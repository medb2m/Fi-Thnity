import { WebSocketServer } from 'ws';

/**
 * WebSocket server for real-time vehicle location tracking
 * Handles location updates from vehicles and broadcasts to all map viewers
 */
class VehicleLocationServer {
  constructor(server) {
    this.wss = new WebSocketServer({ 
      server,
      path: '/ws/vehicle-location',
      perMessageDeflate: false, // Disable compression to avoid protocol issues
      clientTracking: true,
      maxPayload: 100 * 1024 * 1024 // 100MB max payload
    });
    
    // Store active vehicle positions in memory
    // In production, use Redis for scalability
    this.vehiclePositions = new Map(); // vehicleId -> position data
    this.clients = new Set(); // All connected clients (map viewers)
    this.vehicleClients = new Map(); // vehicleId -> WebSocket (vehicle sharing location)
    
    this.setupWebSocket();
  }
  
  setupWebSocket() {
    this.wss.on('connection', (ws, req) => {
      console.log('New WebSocket connection for vehicle location');
      
      // Set up ping/pong for connection keep-alive
      ws.isAlive = true;
      ws.on('pong', () => {
        ws.isAlive = true;
      });
      
      ws.on('message', (message) => {
        try {
          const data = JSON.parse(message.toString());
          this.handleMessage(ws, data);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
          ws.send(JSON.stringify({
            event: 'error',
            message: 'Invalid message format'
          }));
        }
      });
      
      ws.on('close', () => {
        console.log('WebSocket connection closed');
        this.handleDisconnect(ws);
      });
      
      ws.on('error', (error) => {
        console.error('WebSocket error:', error);
        this.handleDisconnect(ws);
      });
      
      // Send current vehicle positions to new client
      this.sendCurrentPositions(ws);
    });
    
    // Set up heartbeat interval to check for dead connections
    this.heartbeatInterval = setInterval(() => {
      this.wss.clients.forEach((ws) => {
        if (ws.isAlive === false) {
          console.log('Terminating dead WebSocket connection');
          return ws.terminate();
        }
        ws.isAlive = false;
        ws.ping();
      });
    }, 30000); // Every 30 seconds
    
    console.log('Vehicle Location WebSocket server started on /ws/vehicle-location');
  }
  
  handleMessage(ws, data) {
    const { event, data: payload } = data;
    
    switch (event) {
      case 'update_location':
        this.handleLocationUpdate(ws, payload);
        break;
      
      case 'subscribe':
        // Client wants to receive vehicle positions
        this.clients.add(ws);
        this.sendCurrentPositions(ws);
        break;
      
      default:
        console.log('Unknown event:', event);
    }
  }
  
  handleLocationUpdate(ws, locationData) {
    const {
      vehicleId,
      type,
      lat,
      lng,
      speed,
      bearing,
      timestamp
    } = locationData;
    
    if (!vehicleId || !lat || !lng) {
      ws.send(JSON.stringify({
        event: 'error',
        message: 'Missing required fields: vehicleId, lat, lng'
      }));
      return;
    }
    
    // Store vehicle position
    const position = {
      vehicleId,
      type: type || 'CAR',
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      speed: parseFloat(speed) || 0,
      bearing: parseFloat(bearing) || 0,
      timestamp: timestamp || Date.now(),
      lastUpdate: Date.now()
    };
    
    this.vehiclePositions.set(vehicleId, position);
    this.vehicleClients.set(vehicleId, ws);
    
    // Broadcast to all map viewers
    this.broadcastVehiclePosition(position);
    
    // Send acknowledgment
    ws.send(JSON.stringify({
      event: 'location_ack',
      vehicleId,
      timestamp: Date.now()
    }));
  }
  
  broadcastVehiclePosition(position) {
    const message = JSON.stringify({
      event: 'vehicle_position',
      data: {
        vehicleId: position.vehicleId,
        type: position.type,
        lat: position.lat,
        lng: position.lng,
        speed: position.speed,
        bearing: position.bearing,
        timestamp: position.timestamp
      }
    });
    
    // Send to all map viewer clients
    this.clients.forEach((client) => {
      if (client.readyState === 1) { // WebSocket.OPEN
        try {
          client.send(message);
        } catch (error) {
          console.error('Error sending to client:', error);
          this.clients.delete(client);
        }
      }
    });
  }
  
  sendCurrentPositions(ws) {
    if (this.vehiclePositions.size === 0) return;
    
    // Send all current vehicle positions
    this.vehiclePositions.forEach((position) => {
      const message = JSON.stringify({
        event: 'vehicle_position',
        data: {
          vehicleId: position.vehicleId,
          type: position.type,
          lat: position.lat,
          lng: position.lng,
          speed: position.speed,
          bearing: position.bearing,
          timestamp: position.timestamp
        }
      });
      
      if (ws.readyState === 1) {
        ws.send(message);
      }
    });
  }
  
  handleDisconnect(ws) {
    // Remove from clients
    this.clients.delete(ws);
    
    // Find and remove vehicle if this was a vehicle client
    let vehicleIdToRemove = null;
    this.vehicleClients.forEach((vehicleWs, vehicleId) => {
      if (vehicleWs === ws) {
        vehicleIdToRemove = vehicleId;
      }
    });
    
    if (vehicleIdToRemove) {
      this.vehicleClients.delete(vehicleIdToRemove);
      this.vehiclePositions.delete(vehicleIdToRemove);
      
      // Notify clients that vehicle stopped sharing
      this.broadcastVehicleRemoved(vehicleIdToRemove);
    }
  }
  
  broadcastVehicleRemoved(vehicleId) {
    const message = JSON.stringify({
      event: 'vehicle_removed',
      data: { vehicleId }
    });
    
    this.clients.forEach((client) => {
      if (client.readyState === 1) {
        try {
          client.send(message);
        } catch (error) {
          console.error('Error sending removal to client:', error);
        }
      }
    });
  }
  
  // Cleanup old positions (vehicles that haven't updated in 30 seconds)
  startCleanupTimer() {
    setInterval(() => {
      const now = Date.now();
      const vehiclesToRemove = [];
      
      this.vehiclePositions.forEach((position, vehicleId) => {
        if (now - position.lastUpdate > 30000) { // 30 seconds
          vehiclesToRemove.push(vehicleId);
        }
      });
      
      vehiclesToRemove.forEach((vehicleId) => {
        this.vehiclePositions.delete(vehicleId);
        this.vehicleClients.delete(vehicleId);
        this.broadcastVehicleRemoved(vehicleId);
      });
    }, 10000); // Check every 10 seconds
  }
  
  // Get all active vehicles (for API endpoint)
  getActiveVehicles() {
    return Array.from(this.vehiclePositions.values());
  }
}

export default VehicleLocationServer;

