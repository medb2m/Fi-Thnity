import express from 'express';
import {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  broadcastNotification
} from '../controllers/notificationController.js';
import { authenticate } from '../middleware/auth.js';

const router = express.Router();

// All notification routes require authentication
router.use(authenticate);

// Get all notifications
router.get('/', getNotifications);

// Get unread count
router.get('/unread-count', getUnreadCount);

// Mark notification as read
router.put('/:notificationId/read', markAsRead);

// Mark all notifications as read
router.put('/read-all', markAllAsRead);

// Delete notification
router.delete('/:notificationId', deleteNotification);

// Broadcast notification to all users (for public transport searches)
router.post('/broadcast', broadcastNotification);

export default router;

