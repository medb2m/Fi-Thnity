import express from 'express';
import { body } from 'express-validator';
import {
  createPost,
  getPosts,
  getPostById,
  toggleLike,
  votePost,
  addComment,
  deletePost,
  getMyPosts
} from '../controllers/communityController.js';
import { authenticate, optionalAuth } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';
import { uploadCommunityPost } from '../middleware/upload.js';

const router = express.Router();

// Create a new post (with optional image upload)
router.post(
  '/posts',
  authenticate,
  uploadCommunityPost.single('image'), // Handle single image upload
  [
    body('content').notEmpty().trim().isLength({ min: 1, max: 500 }),
    body('postType').optional().isIn(['ACCIDENT', 'DELAY', 'ROAD_CLOSURE', 'GENERAL']),
    body('location.latitude').optional().isFloat({ min: -90, max: 90 }),
    body('location.longitude').optional().isFloat({ min: -180, max: 180 }),
    body('imageUrl').optional().isURL(), // For external URLs
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

// Vote on a post (Reddit-style upvote/downvote)
router.post(
  '/posts/:postId/vote',
  authenticate,
  [
    body('vote').optional().isIn(['up', 'down', null]),
    handleValidationErrors
  ],
  votePost
);

// Toggle like on a post (deprecated - use vote instead)
router.post('/posts/:postId/like', verifyFirebaseToken, toggleLike);

// Add a comment to a post
router.post(
  '/posts/:postId/comments',
  authenticate,
  [
    body('content').notEmpty().trim().isLength({ min: 1, max: 200 }),
    handleValidationErrors
  ],
  addComment
);

// Delete a post
router.delete('/posts/:postId', verifyFirebaseToken, deletePost);

export default router;
