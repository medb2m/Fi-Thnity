import Ride from '../models/Ride.js';
import User from '../models/User.js';

/**
 * Create a new ride (request or offer)
 * POST /api/rides
 */
export const createRide = async (req, res) => {
  try {
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

    // Parse departureDate if provided (ISO 8601 string)
    let parsedDepartureDate = departureDate ? new Date(departureDate) : new Date();
    
    // Validate departureDate is not in the past
    if (parsedDepartureDate < new Date()) {
      return res.status(400).json({
        success: false,
        message: 'Departure date cannot be in the past'
      });
    }

    // Validate price for taxi rides
    if (['TAXI', 'TAXI_COLLECTIF'].includes(transportType)) {
      if (!price || price <= 0) {
        return res.status(400).json({
          success: false,
          message: 'Price is required for taxi rides'
        });
      }
    }

    const ride = await Ride.create({
      user: req.user._id,
      firebaseUid: req.firebaseUser.uid,
      rideType,
      transportType,
      origin,
      destination,
      availableSeats: availableSeats || 1,
      notes: notes || '',
      departureDate: parsedDepartureDate,
      price: ['TAXI', 'TAXI_COLLECTIF'].includes(transportType) ? price : null
    });

    // Calculate and save distance
    ride.calculateDistance();
    await ride.save();

    // Populate user info
    await ride.populate('user', 'name photoUrl rating');

    res.status(201).json({
      success: true,
      message: 'Ride created successfully',
      data: ride
    });
  } catch (error) {
    console.error('Create ride error:', error);
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

    const query = {
      status: 'ACTIVE',
      expiresAt: { $gt: new Date() }
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

    const rides = await Ride.find({
      rideType: oppositeType,
      status: 'ACTIVE',
      expiresAt: { $gt: new Date() },
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
