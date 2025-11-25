import User from '../models/User.js';
import Ride from '../models/Ride.js';
import CommunityPost from '../models/CommunityPost.js';

/**
 * Admin Dashboard Home
 * GET /admin
 */
export const getDashboard = async (req, res) => {
  try {
    // Get statistics
    const totalUsers = await User.countDocuments();
    const activeUsers = await User.countDocuments({ isActive: true });
    const totalRides = await Ride.countDocuments();
    const activeRides = await Ride.countDocuments({ status: 'ACTIVE' });
    const totalPosts = await CommunityPost.countDocuments();
    const activePosts = await CommunityPost.countDocuments({ isActive: true });

    // Get recent data
    const recentUsers = await User.find()
      .sort({ createdAt: -1 })
      .limit(5)
      .select('name phoneNumber createdAt');

    const recentRides = await Ride.find()
      .sort({ createdAt: -1 })
      .limit(5)
      .populate('user', 'name')
      .select('rideType transportType status createdAt');

    const recentPosts = await CommunityPost.find()
      .sort({ createdAt: -1 })
      .limit(5)
      .populate('user', 'name')
      .select('content postType likesCount createdAt');

    res.render('admin/dashboard', {
      title: 'Admin Dashboard',
      stats: {
        totalUsers,
        activeUsers,
        totalRides,
        activeRides,
        totalPosts,
        activePosts
      },
      recentUsers,
      recentRides,
      recentPosts
    });
  } catch (error) {
    console.error('Dashboard error:', error);
    res.status(500).send('Error loading dashboard');
  }
};

/**
 * Users Management
 * GET /admin/users
 */
export const getUsers = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = 20;
    const skip = (page - 1) * limit;

    const users = await User.find()
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit);

    const total = await User.countDocuments();

    res.render('admin/users', {
      title: 'Users Management',
      users,
      pagination: {
        page,
        pages: Math.ceil(total / limit),
        total
      },
      message: req.query.message,
      error: req.query.error
    });
  } catch (error) {
    console.error('Get users error:', error);
    res.status(500).json({ success: false, message: 'Error loading users', stack: error.message });
  }
};

/**
 * Rides Management
 * GET /admin/rides
 */
export const getRides = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = 20;
    const skip = (page - 1) * limit;

    const rides = await Ride.find()
      .sort({ createdAt: -1 })
      .populate('user', 'name phoneNumber')
      .skip(skip)
      .limit(limit);

    const total = await Ride.countDocuments();

    res.render('admin/rides', {
      title: 'Rides Management',
      rides,
      pagination: {
        page,
        pages: Math.ceil(total / limit),
        total
      },
      message: req.query.message,
      error: req.query.error
    });
  } catch (error) {
    console.error('Get rides error:', error);
    res.status(500).json({ success: false, message: 'Error loading rides', stack: error.message });
  }
};

/**
 * Community Posts Management
 * GET /admin/posts
 */
export const getPosts = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = 20;
    const skip = (page - 1) * limit;

    const posts = await CommunityPost.find()
      .sort({ createdAt: -1 })
      .populate('user', 'name phoneNumber')
      .skip(skip)
      .limit(limit);

    const total = await CommunityPost.countDocuments();

    res.render('admin/posts', {
      title: 'Community Posts Management',
      posts,
      pagination: {
        page,
        pages: Math.ceil(total / limit),
        total
      },
      message: req.query.message,
      error: req.query.error
    });
  } catch (error) {
    console.error('Get posts error:', error);
    res.status(500).json({ success: false, message: 'Error loading posts', stack: error.message });
  }
};

/**
 * Delete User
 * POST /admin/users/:userId/delete
 */
export const deleteUser = async (req, res) => {
  try {
    await User.findByIdAndDelete(req.params.userId);
    res.redirect('/admin/users?message=User deleted successfully');
  } catch (error) {
    console.error('Delete user error:', error);
    res.redirect('/admin/users?error=Failed to delete user');
  }
};

/**
 * Delete Ride
 * POST /admin/rides/:rideId/delete
 */
export const deleteRide = async (req, res) => {
  try {
    await Ride.findByIdAndUpdate(req.params.rideId, { status: 'CANCELLED' });
    res.redirect('/admin/rides?message=Ride cancelled successfully');
  } catch (error) {
    console.error('Delete ride error:', error);
    res.redirect('/admin/rides?error=Failed to cancel ride');
  }
};

/**
 * Delete Post
 * POST /admin/posts/:postId/delete
 */
export const deletePost = async (req, res) => {
  try {
    await CommunityPost.findByIdAndUpdate(req.params.postId, { isActive: false });
    res.redirect('/admin/posts?message=Post deleted successfully');
  } catch (error) {
    console.error('Delete post error:', error);
    res.redirect('/admin/posts?error=Failed to delete post');
  }
};

/**
 * Create User
 * POST /admin/users/create
 */
export const createUser = async (req, res) => {
  try {
    const { name, email, password, phoneNumber, isActive } = req.body;

    // Check if user already exists
    const existingUser = await User.findOne({ $or: [{ email }, { phoneNumber }] });
    if (existingUser) {
      return res.redirect('/admin/users?error=User with this email or phone already exists');
    }

    // Create new user
    const user = new User({
      name,
      email,
      password, // Will be hashed by the User model pre-save hook
      phoneNumber,
      isActive: isActive === 'true',
      emailVerified: true, // Admin-created users are auto-verified
      isVerified: true
    });

    await user.save();
    res.redirect('/admin/users?message=User created successfully');
  } catch (error) {
    console.error('Create user error:', error);
    res.redirect('/admin/users?error=' + encodeURIComponent(error.message || 'Failed to create user'));
  }
};

/**
 * Update User
 * POST /admin/users/:userId/update
 */
export const updateUser = async (req, res) => {
  try {
    const { name, email, phoneNumber, isActive } = req.body;
    const userId = req.params.userId;

    // Check if email is being changed to one that already exists
    if (email) {
      const existingUser = await User.findOne({ email, _id: { $ne: userId } });
      if (existingUser) {
        return res.redirect('/admin/users?error=Email already in use by another user');
      }
    }

    // Update user
    await User.findByIdAndUpdate(userId, {
      name,
      email,
      phoneNumber,
      isActive: isActive === 'true'
    });

    res.redirect('/admin/users?message=User updated successfully');
  } catch (error) {
    console.error('Update user error:', error);
    res.redirect('/admin/users?error=Failed to update user');
  }
};

/**
 * Create Ride (Admin)
 * POST /admin/rides/create
 */
export const createRide = async (req, res) => {
  try {
    const {
      userId,
      rideType,
      transportType,
      originLatitude,
      originLongitude,
      originAddress,
      destinationLatitude,
      destinationLongitude,
      destinationAddress,
      availableSeats,
      expiresAt,
      notes
    } = req.body;

    // Validate required fields
    if (!userId || !rideType || !transportType || !originAddress || !destinationAddress) {
      return res.redirect('/admin/rides?error=Missing required fields');
    }

    // Validate rideType
    if (!['REQUEST', 'OFFER'].includes(rideType)) {
      return res.redirect('/admin/rides?error=Invalid ride type');
    }

    // Validate transportType
    if (!['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS'].includes(transportType)) {
      return res.redirect('/admin/rides?error=Invalid transport type');
    }

    // Get user
    const user = await User.findById(userId);
    if (!user) {
      return res.redirect('/admin/rides?error=User not found');
    }

    // Create ride
    const ride = await Ride.create({
      user: userId,
      firebaseUid: user.firebaseUid || 'admin-created',
      rideType,
      transportType,
      origin: {
        latitude: parseFloat(originLatitude) || 36.8065, // Default to Tunis
        longitude: parseFloat(originLongitude) || 10.1815,
        address: originAddress
      },
      destination: {
        latitude: parseFloat(destinationLatitude) || 36.8065,
        longitude: parseFloat(destinationLongitude) || 10.1815,
        address: destinationAddress
      },
      availableSeats: parseInt(availableSeats) || 1,
      expiresAt: expiresAt ? new Date(expiresAt) : new Date(Date.now() + 2 * 60 * 60 * 1000), // 2 hours default
      notes: notes || '',
      status: 'ACTIVE'
    });

    // Calculate distance
    ride.calculateDistance();
    await ride.save();

    res.redirect('/admin/rides?message=Ride created successfully');
  } catch (error) {
    console.error('Create ride error:', error);
    res.redirect('/admin/rides?error=' + encodeURIComponent(error.message || 'Failed to create ride'));
  }
};

/**
 * Update Ride (Admin)
 * POST /admin/rides/:rideId/update
 */
export const updateRide = async (req, res) => {
  try {
    const { rideId } = req.params;
    const {
      rideType,
      transportType,
      originAddress,
      destinationAddress,
      availableSeats,
      status,
      expiresAt,
      notes
    } = req.body;

    const ride = await Ride.findById(rideId);
    if (!ride) {
      return res.redirect('/admin/rides?error=Ride not found');
    }

    // Update fields
    if (rideType && ['REQUEST', 'OFFER'].includes(rideType)) {
      ride.rideType = rideType;
    }
    if (transportType && ['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS'].includes(transportType)) {
      ride.transportType = transportType;
    }
    if (originAddress) {
      ride.origin.address = originAddress;
    }
    if (destinationAddress) {
      ride.destination.address = destinationAddress;
    }
    if (availableSeats) {
      const seats = parseInt(availableSeats);
      if (seats >= 0 && seats <= 8) {
        ride.availableSeats = seats;
      }
    }
    if (status && ['ACTIVE', 'MATCHED', 'COMPLETED', 'CANCELLED', 'EXPIRED'].includes(status)) {
      ride.status = status;
    }
    if (expiresAt) {
      ride.expiresAt = new Date(expiresAt);
    }
    if (notes !== undefined) {
      ride.notes = notes;
    }

    // Recalculate distance if origin or destination changed
    if (originAddress || destinationAddress) {
      ride.calculateDistance();
    }

    await ride.save();

    res.redirect('/admin/rides?message=Ride updated successfully');
  } catch (error) {
    console.error('Update ride error:', error);
    res.redirect('/admin/rides?error=Failed to update ride');
  }
};