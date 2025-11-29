import { WebSocketServer } from 'ws';
import jwt from 'jsonwebtoken';
import User from '../models/User.js';

/**
 * WebSocket server for real-time notifications
 * Clients connect and receive notifications as they are created
 */
class NotificationServer {
  constructor(httpServer) {
    this.wss = new WebSocketServer({ 
      server: httpServer, 
      path: '/ws/notifications',
      perMessageDeflate: false, // Disable compression to avoid protocol issues
      clientTracking: true,
      maxPayload: 100 * 1024 * 1024 // 100MB max payload
    });
    
    // Map of userId -> Set of WebSocket connections for that user
    this.userConnections = new Map();
    
    this.setupWebSocket();
  }

  setupWebSocket() {
    this.wss.on('connection', (ws, req) => {
      console.log('New WebSocket connection for notifications');
      console.log('Request URL:', req.url);
      console.log('Request headers:', JSON.stringify(req.headers, null, 2));
      
      // Set up ping/pong for connection keep-alive
      ws.isAlive = true;
      ws.on('pong', () => {
        ws.isAlive = true;
      });
      
      // Authenticate connection using JWT token from query params
      let token = null;
      try {
        const url = new URL(req.url, `http://${req.headers.host}`);
        token = url.searchParams.get('token');
        console.log('Token from URL params:', token ? `${token.substring(0, 20)}...` : 'null');
      } catch (error) {
        console.error('Error parsing URL:', error);
        // Fallback: try to extract token manually from query string
        const queryString = req.url.split('?')[1];
        if (queryString) {
          const params = new URLSearchParams(queryString);
          token = params.get('token');
          console.log('Token from manual parsing:', token ? `${token.substring(0, 20)}...` : 'null');
        }
      }
      
      if (!token || token.trim() === '') {
        console.log('Notification WebSocket: No token provided, closing connection');
        ws.close(1008, 'Authentication required');
        return;
      }

      // Verify JWT token
      try {
        console.log('Notification WebSocket: Verifying token (length: ' + token.length + ')...');
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'default-secret-change-in-production');
        console.log('Notification WebSocket: Token decoded successfully');
        console.log('Decoded token payload:', JSON.stringify(decoded, null, 2));
        const userId = decoded.userId || decoded.id;
        
        if (!userId) {
          console.log('Notification WebSocket: Invalid token, no userId found in decoded token');
          ws.close(1008, 'Invalid token');
          return;
        }
        
        console.log('Notification WebSocket: Authenticated user ID:', userId);

        // Add connection to user's connection set
        if (!this.userConnections.has(userId)) {
          this.userConnections.set(userId, new Set());
        }
        this.userConnections.get(userId).add(ws);
        
        console.log(`Notification WebSocket: User ${userId} connected (${this.userConnections.get(userId).size} connections)`);

        // Send connection confirmation
        ws.send(JSON.stringify({
          type: 'connected',
          message: 'Connected to notification server'
        }));

        // Handle disconnection
        ws.on('close', () => {
          const userSet = this.userConnections.get(userId);
          if (userSet) {
            userSet.delete(ws);
            if (userSet.size === 0) {
              this.userConnections.delete(userId);
            }
            console.log(`Notification WebSocket: User ${userId} disconnected (${userSet?.size || 0} connections remaining)`);
          }
        });

        ws.on('error', (error) => {
          console.error('Notification WebSocket error:', error);
        });

      } catch (error) {
        console.error('Notification WebSocket: Token verification failed', error);
        ws.close(1008, 'Invalid token');
      }
    });

    // Set up heartbeat interval to check for dead connections
    this.heartbeatInterval = setInterval(() => {
      this.wss.clients.forEach((ws) => {
        if (ws.isAlive === false) {
          console.log('Terminating dead notification WebSocket connection');
          return ws.terminate();
        }
        ws.isAlive = false;
        ws.ping();
      });
    }, 30000); // Every 30 seconds

    console.log('Notification WebSocket server started on /ws/notifications');
  }

  /**
   * Send notification to a specific user
   * @param {string} userId - User ID to send notification to
   * @param {object} notification - Notification object
   */
  sendNotificationToUser(userId, notification) {
    const connections = this.userConnections.get(userId);
    
    if (!connections || connections.size === 0) {
      console.log(`Notification WebSocket: User ${userId} has no active connections`);
      return false;
    }

    const message = JSON.stringify({
      type: 'notification',
      data: notification
    });

    let sentCount = 0;
    connections.forEach((ws) => {
      if (ws.readyState === 1) { // WebSocket.OPEN
        try {
          ws.send(message);
          sentCount++;
        } catch (error) {
          console.error(`Notification WebSocket: Error sending to user ${userId}:`, error);
        }
      }
    });

    console.log(`Notification WebSocket: Sent notification to user ${userId} (${sentCount}/${connections.size} connections)`);
    return sentCount > 0;
  }

  /**
   * Broadcast notification to all connected users
   * @param {object} notification - Notification object
   */
  broadcastNotification(notification) {
    const message = JSON.stringify({
      type: 'notification',
      data: notification
    });

    let sentCount = 0;
    this.userConnections.forEach((connections, userId) => {
      connections.forEach((ws) => {
        if (ws.readyState === 1) { // WebSocket.OPEN
          try {
            ws.send(message);
            sentCount++;
          } catch (error) {
            console.error(`Notification WebSocket: Error broadcasting to user ${userId}:`, error);
          }
        }
      });
    });

    console.log(`Notification WebSocket: Broadcast notification to ${sentCount} connections`);
    return sentCount;
  }

  /**
   * Get number of active connections for a user
   */
  getUserConnectionCount(userId) {
    const connections = this.userConnections.get(userId);
    return connections ? connections.size : 0;
  }

  /**
   * Get total number of connected users
   */
  getTotalConnectedUsers() {
    return this.userConnections.size;
  }
}

export default NotificationServer;

