import express from 'express';
import { body } from 'express-validator';
import {
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  getFriends,
  getInvitations,
  getFriendStatus
} from '../controllers/friendController.js';
import { authenticate } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

// Send a friend request
router.post(
  '/request',
  authenticate,
  [
    body('recipientId').notEmpty().withMessage('Recipient ID is required'),
    handleValidationErrors
  ],
  sendFriendRequest
);

// Accept a friend request
router.put('/accept/:requestId', authenticate, acceptFriendRequest);

// Reject a friend request
router.put('/reject/:requestId', authenticate, rejectFriendRequest);

// Get all friends
router.get('/', authenticate, getFriends);

// Get pending invitations
router.get('/invitations', authenticate, getInvitations);

// Get friend status with a specific user
router.get('/status/:userId', authenticate, getFriendStatus);

export default router;

