import express from 'express';
import { body } from 'express-validator';
import multer from 'multer';
import {
  createPost,
  getPosts,
  getPostById,
  toggleLike,
  votePost,
  addComment,
  updateComment,
  deleteComment,
  deletePost,
  updatePost,
  getMyPosts
} from '../controllers/communityController.js';
import { authenticate, optionalAuth } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';
import { uploadCommunityPost } from '../middleware/upload.js';

const router = express.Router();

// Test endpoint to verify server is receiving requests
router.post('/posts/test', (req, res) => {
  console.log('✅ Test endpoint hit');
  res.json({ success: true, message: 'Server is receiving POST requests' });
});

// Create a new post (with optional image upload)
router.post(
  '/posts',
  // Log incoming request
  (req, res, next) => {
    console.log('📥 POST /api/community/posts - Request received');
    console.log('   Content-Type:', req.headers['content-type']);
    console.log('   Content-Length:', req.headers['content-length']);
    console.log('   User-Agent:', req.headers['user-agent']);
    next();
  },
  authenticate,
  // Multer must run first to parse multipart/form-data
  // Wrap in try-catch to handle errors gracefully
  (req, res, next) => {
    console.log('📤 Starting multer upload processing...');
    uploadCommunityPost.single('image')(req, res, (err) => {
      if (err) {
        console.error('❌ Multer upload error:', err);
        console.error('   Error type:', err.constructor.name);
        console.error('   Error code:', err.code);
        console.error('   Error message:', err.message);
        // Handle multer-specific errors
        if (err instanceof multer.MulterError) {
          if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({
              success: false,
              message: 'File too large. Maximum size is 10MB.'
            });
          }
          return res.status(400).json({
            success: false,
            message: 'File upload error: ' + err.message
          });
        }
        // Handle other errors (like fileFilter errors)
        return res.status(400).json({
          success: false,
          message: err.message || 'Error uploading file'
        });
      }
      console.log('✅ Multer upload successful');
      console.log('   File:', req.file ? req.file.filename : 'No file');
      console.log('   Body content length:', req.body.content?.length);
      // No error, continue to next middleware
      next();
    });
  },
  // Validation runs after multer has parsed the form data
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
router.get('/my-posts', authenticate, getMyPosts);

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
router.post('/posts/:postId/like', authenticate, toggleLike);

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

// Update a comment
router.put(
  '/posts/:postId/comments/:commentId',
  authenticate,
  [
    body('content').notEmpty().trim().isLength({ min: 1, max: 200 }),
    handleValidationErrors
  ],
  updateComment
);

// Delete a comment
router.delete(
  '/posts/:postId/comments/:commentId',
  authenticate,
  deleteComment
);

// Update a post
router.put(
  '/posts/:postId',
  authenticate,
  (req, res, next) => {
    uploadCommunityPost.single('image')(req, res, (err) => {
      if (err) {
        if (err instanceof multer.MulterError) {
          if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({
              success: false,
              message: 'File too large. Maximum size is 10MB.'
            });
          }
          return res.status(400).json({
            success: false,
            message: 'File upload error: ' + err.message
          });
        }
        return res.status(400).json({
          success: false,
          message: err.message || 'Error uploading file'
        });
      }
      next();
    });
  },
  [
    body('content').optional().trim().isLength({ min: 1, max: 500 }),
    body('removeImage').optional().isIn(['true', 'false']),
    handleValidationErrors
  ],
  updatePost
);

// Delete a post
router.delete('/posts/:postId', authenticate, deletePost);

export default router;
