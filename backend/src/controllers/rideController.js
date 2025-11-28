import Ride from '../models/Ride.js';
import User from '../models/User.js';

/**
 * Create a new ride (request or offer)
 * POST /api/rides
 */
export const createRide = async (req, res) => {
  try {
    console.log('🚗 createRide: Starting ride creation');
    console.log('   User ID:', req.user._id);
    console.log('   Request body:', JSON.stringify(req.body, null, 2));

    const {
      rideType,
      transportType,
      origin,
      destination,
      availableSeats,
      notes,
      departureDate,
      price
    } = req.body;

    // Validate required fields
    if (!rideType) {
      console.log('❌ createRide: Missing rideType');
      return res.status(400).json({
        success: false,
        message: 'rideType is required'
      });
    }

    if (!transportType) {
      console.log('❌ createRide: Missing transportType');
      return res.status(400).json({
        success: false,
        message: 'transportType is required'
      });
    }

    if (!origin || !origin.latitude || !origin.longitude || !origin.address) {
      console.log('❌ createRide: Invalid origin');
      return res.status(400).json({
        success: false,
        message: 'origin with latitude, longitude, and address is required'
      });
    }

    if (!destination || !destination.latitude || !destination.longitude || !destination.address) {
      console.log('❌ createRide: Invalid destination');
      return res.status(400).json({
        success: false,
        message: 'destination with latitude, longitude, and address is required'
      });
    }

    // Parse departureDate if provided (ISO 8601 string)
    let parsedDepartureDate;
    if (departureDate) {
      parsedDepartureDate = new Date(departureDate);
      // Check if date parsing failed
      if (isNaN(parsedDepartureDate.getTime())) {
        console.log('❌ createRide: Invalid departureDate format:', departureDate);
        return res.status(400).json({
          success: false,
          message: 'Invalid departureDate format. Expected ISO 8601 format (e.g., 2025-11-26T14:30:00)'
        });
      }
    } else {
      parsedDepartureDate = new Date();
    }
    
    console.log('📅 createRide: Departure date:', parsedDepartureDate.toISOString());
    console.log('📅 createRide: Departure date (local):', parsedDepartureDate.toString());
    
    // Validate departureDate is not in the past (allow 1 minute buffer for clock differences)
    const now = new Date();
    const oneMinuteAgo = new Date(now.getTime() - 60 * 1000);
    if (parsedDepartureDate < oneMinuteAgo) {
      console.log('❌ createRide: Departure date is in the past');
      console.log('   Departure:', parsedDepartureDate.toISOString());
      console.log('   Now:', now.toISOString());
      return res.status(400).json({
        success: false,
        message: 'Departure date cannot be in the past'
      });
    }

    // Validate price for taxi rides
    if (['TAXI', 'TAXI_COLLECTIF'].includes(transportType)) {
      if (!price || price <= 0) {
        console.log('❌ createRide: Price required for taxi rides');
        return res.status(400).json({
          success: false,
          message: 'Price is required for taxi rides'
        });
      }
    }

    // Calculate expiration time (2 hours from departure date)
    const expiresAt = new Date(parsedDepartureDate.getTime() + 2 * 60 * 60 * 1000);
    console.log('⏰ createRide: Expires at:', expiresAt.toISOString());

    // Ensure all numeric values are valid
    const originLat = parseFloat(origin.latitude);
    const originLng = parseFloat(origin.longitude);
    const destLat = parseFloat(destination.latitude);
    const destLng = parseFloat(destination.longitude);
    
    if (isNaN(originLat) || isNaN(originLng) || isNaN(destLat) || isNaN(destLng)) {
      console.log('❌ createRide: Invalid coordinates');
      return res.status(400).json({
        success: false,
        message: 'Invalid latitude or longitude values'
      });
    }

    const rideData = {
      user: req.user._id,
      rideType: rideType.trim().toUpperCase(),
      transportType: transportType.trim().toUpperCase(),
      origin: {
        latitude: originLat,
        longitude: originLng,
        address: origin.address.trim()
      },
      destination: {
        latitude: destLat,
        longitude: destLng,
        address: destination.address.trim()
      },
      availableSeats: availableSeats ? Math.max(1, Math.min(8, parseInt(availableSeats))) : 1,
      notes: notes ? notes.trim().substring(0, 200) : '',
      departureDate: parsedDepartureDate,
      expiresAt: expiresAt,
      price: (() => {
        const normalizedTransportType = transportType.trim().toUpperCase();
        if (['TAXI', 'TAXI_COLLECTIF'].includes(normalizedTransportType)) {
          const parsedPrice = price ? parseFloat(price) : null;
          console.log('💰 createRide: Price for taxi:', parsedPrice);
          return parsedPrice;
        }
        return null;
      })()
    };

    console.log('📝 createRide: Ride data prepared:', JSON.stringify(rideData, null, 2));
    
    const ride = await Ride.create(rideData);
    console.log('✅ createRide: Ride created, ID:', ride._id);

    // Calculate and save distance
    ride.calculateDistance();
    await ride.save();
    console.log('✅ createRide: Distance calculated:', ride.distance, 'km');

    // Populate user info
    await ride.populate('user', 'name photoUrl rating');
    console.log('✅ createRide: User populated');

    res.status(201).json({
      success: true,
      message: 'Ride created successfully',
      data: ride
    });
  } catch (error) {
    console.error('❌ createRide: Error creating ride');
    console.error('   Error name:', error.name);
    console.error('   Error message:', error.message);
    
    // Handle Mongoose validation errors
    if (error.name === 'ValidationError') {
      console.error('   Validation errors:');
      const validationErrors = {};
      for (const field in error.errors) {
        validationErrors[field] = error.errors[field].message;
        console.error(`     - ${field}: ${error.errors[field].message}`);
      }
      
      return res.status(400).json({
        success: false,
        message: 'Ride validation failed',
        errors: validationErrors,
        error: error.message
      });
    }

    // Handle other errors
    console.error('   Stack:', error.stack);
    res.status(500).json({
      success: false,
      message: 'Error creating ride',
      error: error.message
    });
  }
};

/**
 * Get all active rides with filtering
 * GET /api/rides
 */
export const getRides = async (req, res) => {
  try {
    const { rideType, transportType, page = 1, limit = 20 } = req.query;
    const skip = (page - 1) * limit;

    const now = new Date();
    const query = {
      status: 'ACTIVE',
      expiresAt: { $gt: now },
      departureDate: { $gt: now } // Also filter by departure date to exclude past rides
    };

    if (rideType) query.rideType = rideType;
    if (transportType) query.transportType = transportType;

    const rides = await Ride.find(query)
      .populate('user', 'name photoUrl rating')
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(parseInt(limit));

    const total = await Ride.countDocuments(query);

    res.json({
      success: true,
      data: {
        data: rides,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total,
          pages: Math.ceil(total / limit)
        }
      }
    });
  } catch (error) {
    console.error('Get rides error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching rides',
      error: error.message
    });
  }
};

/**
 * Get ride by ID
 * GET /api/rides/:rideId
 */
export const getRideById = async (req, res) => {
  try {
    const ride = await Ride.findById(req.params.rideId)
      .populate('user', 'name photoUrl rating phoneNumber')
      .populate('matchedWith', 'name photoUrl rating phoneNumber');

    if (!ride) {
      return res.status(404).json({
        success: false,
        message: 'Ride not found'
      });
    }

    res.json({
      success: true,
      data: ride
    });
  } catch (error) {
    console.error('Get ride error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching ride',
      error: error.message
    });
  }
};

/**
 * Get user's rides
 * GET /api/rides/my-rides
 */
export const getMyRides = async (req, res) => {
  try {
    const { status } = req.query;
    const query = { user: req.user._id };

    if (status) query.status = status;

    const rides = await Ride.find(query)
      .populate('user', 'name photoUrl rating')
      .populate('matchedWith', 'name photoUrl rating')
      .sort({ createdAt: -1 });

    res.json({
      success: true,
      data: rides
    });
  } catch (error) {
    console.error('Get my rides error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching your rides',
      error: error.message
    });
  }
};

/**
 * Update ride status
 * PUT /api/rides/:rideId/status
 */
export const updateRideStatus = async (req, res) => {
  try {
    const { status } = req.body;

    const ride = await Ride.findById(req.params.rideId);

    if (!ride) {
      return res.status(404).json({
        success: false,
        message: 'Ride not found'
      });
    }

    // Check if user owns the ride
    if (ride.user.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Not authorized to update this ride'
      });
    }

    ride.status = status;
    await ride.save();

    // Update user's total rides if completed
    if (status === 'COMPLETED') {
      await User.findByIdAndUpdate(req.user._id, {
        $inc: { totalRides: 1 }
      });
    }

    res.json({
      success: true,
      message: 'Ride status updated successfully',
      data: ride
    });
  } catch (error) {
    console.error('Update ride status error:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating ride status',
      error: error.message
    });
  }
};

/**
 * Delete/Cancel a ride
 * DELETE /api/rides/:rideId
 */
export const deleteRide = async (req, res) => {
  try {
    const ride = await Ride.findById(req.params.rideId);

    if (!ride) {
      return res.status(404).json({
        success: false,
        message: 'Ride not found'
      });
    }

    // Check if user owns the ride
    if (ride.user.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Not authorized to delete this ride'
      });
    }

    ride.status = 'CANCELLED';
    await ride.save();

    res.json({
      success: true,
      message: 'Ride cancelled successfully'
    });
  } catch (error) {
    console.error('Delete ride error:', error);
    res.status(500).json({
      success: false,
      message: 'Error deleting ride',
      error: error.message
    });
  }
};

/**
 * Find matching rides (basic algorithm)
 * POST /api/rides/match
 */
export const findMatchingRides = async (req, res) => {
  try {
    const { origin, destination, rideType, maxDistance = 2 } = req.body;

    // Find opposite type rides (if looking for offer, find requests and vice versa)
    const oppositeType = rideType === 'REQUEST' ? 'OFFER' : 'REQUEST';
    const now = new Date();

    const rides = await Ride.find({
      rideType: oppositeType,
      status: 'ACTIVE',
      expiresAt: { $gt: now },
      departureDate: { $gt: now }, // Also filter by departure date
      user: { $ne: req.user._id } // Exclude own rides
    }).populate('user', 'name photoUrl rating');

    // Filter by proximity (simple distance calculation)
    const matchingRides = rides.filter(ride => {
      const originDistance = calculateDistance(
        origin.latitude,
        origin.longitude,
        ride.origin.latitude,
        ride.origin.longitude
      );

      const destDistance = calculateDistance(
        destination.latitude,
        destination.longitude,
        ride.destination.latitude,
        ride.destination.longitude
      );

      return originDistance <= maxDistance && destDistance <= maxDistance;
    });

    res.json({
      success: true,
      data: matchingRides,
      count: matchingRides.length
    });
  } catch (error) {
    console.error('Find matching rides error:', error);
    res.status(500).json({
      success: false,
      message: 'Error finding matching rides',
      error: error.message
    });
  }
};

// Helper function to calculate distance between two coordinates
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) *
    Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) *
    Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}
