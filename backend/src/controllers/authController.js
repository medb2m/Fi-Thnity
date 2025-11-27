import jwt from 'jsonwebtoken';
import User from '../models/User.js';
import VerificationToken from '../models/VerificationToken.js';
import {
  sendVerificationEmail,
  sendPasswordResetEmail,
  sendWelcomeEmail
} from '../config/email.js';

/**
 * Generate JWT token for user
 */
const generateAuthToken = (userId) => {
  return jwt.sign({ userId }, process.env.JWT_SECRET, {
    expiresIn: '30d'
  });
};

/**
 * Register new user with email
 * POST /api/auth/register
 */
export const register = async (req, res) => {
  try {
    const { name, email, password } = req.body;

    // Check if user already exists
    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: 'Email already registered'
      });
    }

    // Create user
    const user = await User.create({
      name,
      email,
      password,
      authType: 'email',
      emailVerified: false
    });

    // Generate verification token
    const token = await VerificationToken.createEmailVerificationToken(user._id);

    // Send verification email
    let emailSent = false;
    let emailError = null;
    try {
      await sendVerificationEmail(email, name, token);
      emailSent = true;
      console.log(`✅ Verification email sent successfully to ${email}`);
    } catch (error) {
      emailError = error.message;
      console.error('❌ Email sending failed:', error);
      // Continue anyway - user is created, they can request resend later
    }

    // Generate JWT token for immediate login
    const jwtToken = generateAuthToken(user._id);

    res.status(201).json({
      success: true,
      message: emailSent
        ? 'Registration successful! Please check your email to verify your account.'
        : 'Registration successful! However, we could not send the verification email. Please use the "Resend Verification" option.',
      data: {
        user: user.getPublicProfile(),
        token: jwtToken,
        emailVerified: false,
        needsVerification: !user.emailVerified,
        emailSent,
        ...(emailError && { emailError })
      }
    });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({
      success: false,
      message: 'Error during registration',
      error: error.message
    });
  }
};

/**
 * Verify email address
 * GET /api/auth/verify-email?token=xxx
 * Renders an HTML page instead of JSON
 */
export const verifyEmail = async (req, res) => {
  try {
    const { token } = req.query;

    if (!token) {
      return res.status(400).render('email-verification', {
        status: 'error',
        errorMessage: 'Verification token is required. Please check your email for the complete verification link.',
        userName: null,
        authToken: null,
        token: null
      });
    }

    // Verify token
    const verificationToken = await VerificationToken.verifyToken(token, 'email-verification');

    if (!verificationToken) {
      return res.status(400).render('email-verification', {
        status: 'error',
        errorMessage: 'Invalid or expired verification token. The link may have expired (valid for 24 hours) or has already been used.',
        userName: null,
        authToken: null,
        token: token.substring(0, 10) + '...' // Show partial token for debugging
      });
    }

    // Update user
    const user = verificationToken.user;
    user.emailVerified = true;
    user.isVerified = true;
    await user.save();

    // Mark token as used
    await verificationToken.markAsUsed();

    // Send welcome email
    try {
      await sendWelcomeEmail(user.email, user.name);
    } catch (emailError) {
      console.error('Welcome email failed:', emailError);
      // Don't fail the verification if welcome email fails
    }

    // Generate auth token
    const authToken = generateAuthToken(user._id);

    // Render success page
    return res.render('email-verification', {
      status: 'success',
      userName: user.name || user.email,
      authToken: authToken,
      errorMessage: null,
      token: null
    });
  } catch (error) {
    console.error('Email verification error:', error);
    return res.status(500).render('email-verification', {
      status: 'error',
      errorMessage: 'An error occurred while verifying your email. Please try again or contact support.',
      userName: null,
      authToken: null,
      token: req.query.token ? req.query.token.substring(0, 10) + '...' : null
    });
  }
};

/**
 * Resend verification email (public - requires email in body)
 * POST /api/auth/resend-verification
 */
export const resendVerification = async (req, res) => {
  try {
    const { email } = req.body;

    const user = await User.findOne({ email, authType: 'email' });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    if (user.emailVerified) {
      return res.status(400).json({
        success: false,
        message: 'Email already verified'
      });
    }

    // Generate new token
    const token = await VerificationToken.createEmailVerificationToken(user._id);

    // Send verification email
    await sendVerificationEmail(email, user.name, token);

    res.json({
      success: true,
      message: 'Verification email sent successfully'
    });
  } catch (error) {
    console.error('Resend verification error:', error);
    res.status(500).json({
      success: false,
      message: 'Error sending verification email',
      error: error.message
    });
  }
};

/**
 * Resend verification email (authenticated - uses current user)
 * POST /api/users/resend-verification
 */
export const resendVerificationAuthenticated = async (req, res) => {
  try {
    const user = req.user;

    if (!user.email) {
      return res.status(400).json({
        success: false,
        message: 'No email address associated with this account'
      });
    }

    if (user.emailVerified) {
      return res.status(400).json({
        success: false,
        message: 'Email already verified'
      });
    }

    // Generate new token
    const token = await VerificationToken.createEmailVerificationToken(user._id);

    // Send verification email
    await sendVerificationEmail(user.email, user.name, token);

    res.json({
      success: true,
      message: 'Verification email sent successfully'
    });
  } catch (error) {
    console.error('Resend verification error:', error);
    res.status(500).json({
      success: false,
      message: 'Error sending verification email',
      error: error.message
    });
  }
};

/**
 * Login with email and password
 * POST /api/auth/login
 */
export const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    // Find user and include password field
    const user = await User.findOne({ email, authType: 'email' }).select('+password');

    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password'
      });
    }

    // Verify password first
    const isPasswordValid = await user.comparePassword(password);

    if (!isPasswordValid) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password'
      });
    }

    // Generate token
    const token = generateAuthToken(user._id);

    // Always allow login, but include verification status
    const message = user.emailVerified
      ? 'Login successful'
      : 'Login successful. Please verify your email to access all features.';

    res.json({
      success: true,
      message,
      data: {
        user: user.getPublicProfile(),
        token,
        emailVerified: user.emailVerified,
        needsVerification: !user.emailVerified
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({
      success: false,
      message: 'Error during login',
      error: error.message
    });
  }
};

/**
 * Forgot password - send reset email
 * POST /api/auth/forgot-password
 */
export const forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;

    const user = await User.findOne({ email, authType: 'email' });

    if (!user) {
      // Don't reveal if email exists or not
      return res.json({
        success: true,
        message: 'If the email exists, a password reset link has been sent'
      });
    }

    // Generate reset token
    const token = await VerificationToken.createPasswordResetToken(user._id);

    // Send reset email
    await sendPasswordResetEmail(email, user.name, token);

    res.json({
      success: true,
      message: 'Password reset link sent to your email'
    });
  } catch (error) {
    console.error('Forgot password error:', error);
    res.status(500).json({
      success: false,
      message: 'Error processing password reset request',
      error: error.message
    });
  }
};

/**
 * Reset password with token
 * POST /api/auth/reset-password
 */
export const resetPassword = async (req, res) => {
  try {
    const { token, newPassword } = req.body;

    // Verify token
    const verificationToken = await VerificationToken.verifyToken(token, 'password-reset');

    if (!verificationToken) {
      return res.status(400).json({
        success: false,
        message: 'Invalid or expired reset token'
      });
    }

    // Update password
    const user = verificationToken.user;
    user.password = newPassword;
    await user.save();

    // Mark token as used
    await verificationToken.markAsUsed();

    res.json({
      success: true,
      message: 'Password reset successful. You can now login with your new password.'
    });
  } catch (error) {
    console.error('Reset password error:', error);
    res.status(500).json({
      success: false,
      message: 'Error resetting password',
      error: error.message
    });
  }
};

/**
 * Get current user (for both email and phone/OTP auth)
 * GET /api/auth/me
 */
export const getCurrentUser = async (req, res) => {
  try {
    res.json({
      success: true,
      data: req.user.getPublicProfile()
    });
  } catch (error) {
    console.error('Get current user error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching user data',
      error: error.message
    });
  }
};
