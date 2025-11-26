import express from 'express';
import { body } from 'express-validator';
import {
  register,
  verifyEmail,
  resendVerification,
  login,
  forgotPassword,
  resetPassword,
  getCurrentUser
} from '../controllers/authController.js';
import {
  sendOTP,
  verifyOTP,
  resendOTP
} from '../controllers/otpController.js';
import { authenticate } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

/**
 * Register new user
 * POST /api/auth/register
 */
router.post(
  '/register',
  [
    body('name')
      .trim()
      .isLength({ min: 2, max: 50 })
      .withMessage('Name must be between 2 and 50 characters'),
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Please provide a valid email address'),
    body('password')
      .isLength({ min: 6 })
      .withMessage('Password must be at least 6 characters long')
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
      .withMessage('Password must contain at least one uppercase letter, one lowercase letter, and one number'),
    handleValidationErrors
  ],
  register
);

/**
 * Verify email address
 * GET /api/auth/verify-email?token=xxx
 */
router.get('/verify-email', verifyEmail);

/**
 * Resend verification email
 * POST /api/auth/resend-verification
 */
router.post(
  '/resend-verification',
  [
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Please provide a valid email address'),
    handleValidationErrors
  ],
  resendVerification
);

/**
 * Login with email and password
 * POST /api/auth/login
 */
router.post(
  '/login',
  [
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Please provide a valid email address'),
    body('password')
      .notEmpty()
      .withMessage('Password is required'),
    handleValidationErrors
  ],
  login
);

/**
 * Forgot password - send reset email
 * POST /api/auth/forgot-password
 */
router.post(
  '/forgot-password',
  [
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Please provide a valid email address'),
    handleValidationErrors
  ],
  forgotPassword
);

/**
 * Reset password with token
 * POST /api/auth/reset-password
 */
router.post(
  '/reset-password',
  [
    body('token')
      .notEmpty()
      .withMessage('Reset token is required'),
    body('newPassword')
      .isLength({ min: 6 })
      .withMessage('Password must be at least 6 characters long')
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
      .withMessage('Password must contain at least one uppercase letter, one lowercase letter, and one number'),
    handleValidationErrors
  ],
  resetPassword
);

/**
 * Get current authenticated user
 * GET /api/auth/me
 */
router.get('/me', authenticate, getCurrentUser);

/**
 * Phone OTP Authentication Routes
 */

/**
 * Send OTP code to phone number
 * POST /api/auth/otp/send
 */
router.post(
  '/otp/send',
  [
    body('phoneNumber')
      .notEmpty()
      .withMessage('Phone number is required')
      .matches(/^\+[1-9]\d{1,14}$/)
      .withMessage('Phone number must be in E.164 format (e.g., +21626204432)'),
    handleValidationErrors
  ],
  sendOTP
);

// Debug route to verify OTP routes are loaded
router.get('/otp/test', (req, res) => {
  res.json({ success: true, message: 'OTP routes are working!' });
});

/**
 * Verify OTP code and login/register
 * POST /api/auth/otp/verify
 */
router.post(
  '/otp/verify',
  [
    body('phoneNumber')
      .notEmpty()
      .withMessage('Phone number is required')
      .matches(/^\+[1-9]\d{1,14}$/)
      .withMessage('Phone number must be in E.164 format (e.g., +21626204432)'),
    body('code')
      .notEmpty()
      .withMessage('OTP code is required')
      .isLength({ min: 6, max: 6 })
      .withMessage('OTP code must be 6 digits')
      .matches(/^\d{6}$/)
      .withMessage('OTP code must contain only digits'),
    body('name')
      .optional()
      .trim()
      .isLength({ min: 2, max: 50 })
      .withMessage('Name must be between 2 and 50 characters'),
    handleValidationErrors
  ],
  verifyOTP
);

/**
 * Resend OTP code
 * POST /api/auth/otp/resend
 */
router.post(
  '/otp/resend',
  [
    body('phoneNumber')
      .notEmpty()
      .withMessage('Phone number is required')
      .matches(/^\+[1-9]\d{1,14}$/)
      .withMessage('Phone number must be in E.164 format (e.g., +21626204432)'),
    handleValidationErrors
  ],
  resendOTP
);

export default router;
