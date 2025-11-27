import express from 'express';
import { body } from 'express-validator';
import {
  getProfile,
  updateProfile,
  uploadProfilePicture,
  updateLocation,
  getUserStats,
  getUserById,
  getAllUsers,
} from '../controllers/userController.js';
import { authenticate } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';
import upload from '../middleware/upload.js';

const router = express.Router();

// Get current user profile
router.get('/profile', authenticate, getProfile);

// Upload profile picture
router.post(
  '/profile/upload-picture',
  authenticate,
  upload.single('picture'),
  uploadProfilePicture
);

// Update user profile
router.put(
  '/profile',
  authenticate,
  [
    body('name').optional().trim().isLength({ min: 2, max: 50 }),
    body('bio').optional().trim().isLength({ max: 150 }),
    body('photoUrl').optional().isURL(),
    body('email').optional().isEmail().normalizeEmail(),
    body('phoneNumber').optional().isMobilePhone('any', { strictMode: false }),
    handleValidationErrors
  ],
  updateProfile
);

// Update user location
router.put(
  '/location',
  authenticate,
  [
    body('latitude').isFloat({ min: -90, max: 90 }),
    body('longitude').isFloat({ min: -180, max: 180 }),
    body('address').optional().trim(),
    handleValidationErrors
  ],
  updateLocation
);

// Get user statistics
router.get('/stats', authenticate, getUserStats);

// Get user by ID (public profile)
router.get('/:userId', getUserById);

// Get all users (for admin)
router.get('/', getAllUsers);

// Firebase route removed - using OTP authentication instead

export default router;
