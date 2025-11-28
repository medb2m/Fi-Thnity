import express from 'express';
import { body } from 'express-validator';
import {
  getConversations,
  getOrCreateConversation,
  getMessages,
  sendMessage,
  markAsRead,
  getUsers,
  deleteConversation,
  getUnreadConversationCount,
  uploadChatImage
} from '../controllers/chatController.js';
import { authenticate } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';
import { uploadChatImage as uploadChatImageMiddleware } from '../middleware/upload.js';

const router = express.Router();

// All chat routes require authentication
router.use(authenticate);

// Get all conversations for current user
router.get('/conversations', getConversations);

// Get unread conversation count
router.get('/conversations/unread-count', getUnreadConversationCount);

// Get or create conversation with another user
router.post(
  '/conversations',
  [
    body('otherUserId').notEmpty().isMongoId(),
    handleValidationErrors
  ],
  getOrCreateConversation
);

// Get messages in a conversation
router.get('/conversations/:conversationId/messages', getMessages);

// Send a message
router.post(
  '/conversations/:conversationId/messages',
  [
    body('content').optional().trim().isLength({ max: 5000 }),
    body('messageType').optional().isIn(['TEXT', 'IMAGE', 'LOCATION']),
    body('imageUrl').optional().isURL(),
    body('location.latitude').optional().isFloat({ min: -90, max: 90 }),
    body('location.longitude').optional().isFloat({ min: -180, max: 180 }),
    handleValidationErrors
  ],
  sendMessage
);

// Mark messages as read
router.put('/conversations/:conversationId/read', markAsRead);

// Delete conversation (soft delete)
router.delete('/conversations/:conversationId', deleteConversation);

// Get users list (for starting new chats)
router.get('/users', getUsers);

// Upload chat image
router.post(
  '/upload-image',
  uploadChatImageMiddleware.single('image'),
  uploadChatImage
);

export default router;

