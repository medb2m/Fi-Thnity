import User from '../models/User.js';
import Ride from '../models/Ride.js';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Get current user profile
 * GET /api/users/profile
 */
export const getProfile = async (req, res) => {
  try {
    const user = await User.findById(req.user._id).select('-__v');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      data: user
    });
  } catch (error) {
    console.error('Get profile error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching profile',
      error: error.message
    });
  }
};

/**
 * Upload profile picture
 * POST /api/users/profile/upload-picture
 */
export const uploadProfilePicture = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file uploaded'
      });
    }

    // Get the uploaded file path
    const filePath = `/uploads/profile-pictures/${req.file.filename}`;
    const photoUrl = `${req.protocol}://${req.get('host')}${filePath}`;

    // Get current user to delete old picture if exists
    const currentUser = await User.findById(req.user._id);
    if (currentUser && currentUser.photoUrl) {
      // Extract filename from old photoUrl
      const oldPath = currentUser.photoUrl.split('/uploads/profile-pictures/')[1];
      if (oldPath) {
        const oldFilePath = path.join(__dirname, '../uploads/profile-pictures', oldPath);
        // Delete old file if it exists
        if (fs.existsSync(oldFilePath)) {
          fs.unlinkSync(oldFilePath);
        }
      }
    }

    // Update user with new photoUrl
    const user = await User.findByIdAndUpdate(
      req.user._id,
      { $set: { photoUrl } },
      { new: true, runValidators: true }
    ).select('-__v');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'Profile picture uploaded successfully',
      data: {
        photoUrl: user.photoUrl
      }
    });
  } catch (error) {
    console.error('Upload profile picture error:', error);
    res.status(500).json({
      success: false,
      message: 'Error uploading profile picture',
      error: error.message
    });
  }
};

/**
 * Update user profile
 * PUT /api/users/profile
 */
export const updateProfile = async (req, res) => {
  try {
    const { name, bio, photoUrl, currentLocation } = req.body;

    const updates = {};
    if (name) updates.name = name;
    if (bio !== undefined) updates.bio = bio;
    if (photoUrl !== undefined) updates.photoUrl = photoUrl;
    if (currentLocation) updates.currentLocation = currentLocation;

    const user = await User.findByIdAndUpdate(
      req.user._id,
      { $set: updates },
      { new: true, runValidators: true }
    ).select('-__v');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      message: 'Profile updated successfully',
      data: user
    });
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating profile',
      error: error.message
    });
  }
};

/**
 * Update user location
 * PUT /api/users/location
 */
export const updateLocation = async (req, res) => {
  try {
    const { latitude, longitude, address } = req.body;

    const user = await User.findByIdAndUpdate(
      req.user._id,
      {
        $set: {
          currentLocation: {
            latitude,
            longitude,
            address: address || '',
            timestamp: new Date()
          }
        }
      },
      { new: true }
    ).select('currentLocation');

    res.json({
      success: true,
      message: 'Location updated successfully',
      data: user.currentLocation
    });
  } catch (error) {
    console.error('Update location error:', error);
    res.status(500).json({
      success: false,
      message: 'Error updating location',
      error: error.message
    });
  }
};

/**
 * Get user statistics
 * GET /api/users/stats
 */
export const getUserStats = async (req, res) => {
  try {
    const user = await User.findById(req.user._id);

    const activeRides = await Ride.countDocuments({
      user: req.user._id,
      status: 'ACTIVE'
    });

    const completedRides = await Ride.countDocuments({
      user: req.user._id,
      status: 'COMPLETED'
    });

    res.json({
      success: true,
      data: {
        rating: user.rating,
        totalRides: user.totalRides,
        activeRides,
        completedRides,
        isVerified: user.isVerified
      }
    });
  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching statistics',
      error: error.message
    });
  }
};

/**
 * Get user by ID (public profile)
 * GET /api/users/:userId
 */
export const getUserById = async (req, res) => {
  try {
    const user = await User.findById(req.params.userId)
      .select('name photoUrl rating totalRides isVerified');

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    res.json({
      success: true,
      data: user
    });
  } catch (error) {
    console.error('Get user error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching user',
      error: error.message
    });
  }
};

/**
 * Get all users (admin only - used in admin panel)
 * GET /api/users
 */
export const getAllUsers = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const skip = (page - 1) * limit;

    const users = await User.find()
      .select('-__v')
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit);

    const total = await User.countDocuments();

    res.json({
      success: true,
      data: users,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('Get all users error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching users',
      error: error.message
    });
  }
};

// Firebase registration removed - using OTP authentication instead
