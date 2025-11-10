import express from 'express';
import { body } from 'express-validator';
import {
  getProfile,
  updateProfile,
  updateLocation,
  getUserStats,
  getUserById,
  getAllUsers,
  firebaseRegisterOrUpdate
} from '../controllers/userController.js';
import { verifyFirebaseToken } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

// Get current user profile
router.get('/profile', verifyFirebaseToken, getProfile);

// Update user profile
router.put(
  '/profile',
  verifyFirebaseToken,
  [
    body('name').optional().trim().isLength({ min: 2, max: 50 }),
    body('bio').optional().trim().isLength({ max: 150 }),
    body('photoUrl').optional().isURL(),
    handleValidationErrors
  ],
  updateProfile
);

// Update user location
router.put(
  '/location',
  verifyFirebaseToken,
  [
    body('latitude').isFloat({ min: -90, max: 90 }),
    body('longitude').isFloat({ min: -180, max: 180 }),
    body('address').optional().trim(),
    handleValidationErrors
  ],
  updateLocation
);

// Get user statistics
router.get('/stats', verifyFirebaseToken, getUserStats);

// Get user by ID (public profile)
router.get('/:userId', getUserById);

// Get all users (for admin)
router.get('/', getAllUsers);

// Add route for Firebase phone registration/sync (before export default)
router.post(
  '/firebase',
  verifyFirebaseToken,
  [
    body('firebaseUid').notEmpty(),
    body('phoneNumber').notEmpty(),
    body('name').optional().trim().default('User'),
    // email and photoUrl are optional
    handleValidationErrors
  ],
  firebaseRegisterOrUpdate
);

export default router;
