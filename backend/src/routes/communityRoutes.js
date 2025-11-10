import express from 'express';
import { body } from 'express-validator';
import {
  createPost,
  getPosts,
  getPostById,
  toggleLike,
  addComment,
  deletePost,
  getMyPosts
} from '../controllers/communityController.js';
import { verifyFirebaseToken, optionalAuth } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

// Create a new post
router.post(
  '/posts',
  verifyFirebaseToken,
  [
    body('content').notEmpty().trim().isLength({ min: 1, max: 500 }),
    body('postType').optional().isIn(['ACCIDENT', 'DELAY', 'ROAD_CLOSURE', 'GENERAL']),
    body('location.latitude').optional().isFloat({ min: -90, max: 90 }),
    body('location.longitude').optional().isFloat({ min: -180, max: 180 }),
    body('imageUrl').optional().isURL(),
    handleValidationErrors
  ],
  createPost
);

// Get all posts (with optional auth for like status)
router.get('/posts', optionalAuth, getPosts);

// Get user's posts
router.get('/my-posts', verifyFirebaseToken, getMyPosts);

// Get post by ID
router.get('/posts/:postId', optionalAuth, getPostById);

// Toggle like on a post
router.post('/posts/:postId/like', verifyFirebaseToken, toggleLike);

// Add a comment to a post
router.post(
  '/posts/:postId/comments',
  verifyFirebaseToken,
  [
    body('content').notEmpty().trim().isLength({ min: 1, max: 200 }),
    handleValidationErrors
  ],
  addComment
);

// Delete a post
router.delete('/posts/:postId', verifyFirebaseToken, deletePost);

export default router;
