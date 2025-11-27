import jwt from 'jsonwebtoken';
import User from '../models/User.js';

/**
 * Unified authentication middleware
 * Handles JWT tokens (email auth and OTP phone auth)
 */
export const authenticate = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No token provided. Authorization header must be "Bearer <token>"'
      });
    }

    const token = authHeader.split('Bearer ')[1];

    // Verify JWT token
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await User.findById(decoded.userId);

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'User not found'
        });
      }

      // Email verification is optional - users can use app features regardless of verification status
      // The verification status is tracked but doesn't block access

      req.user = user;
      req.authType = decoded.authType || 'jwt';
      return next();
    } catch (jwtError) {
      console.error('JWT verification error:', jwtError.message);
      return res.status(401).json({
        success: false,
        message: 'Invalid or expired token',
        error: jwtError.message
      });
    }
  } catch (error) {
    console.error('Authentication error:', error);
    return res.status(401).json({
      success: false,
      message: 'Authentication failed',
      error: error.message
    });
  }
};

/**
 * Optional authentication - doesn't fail if no token
 * Useful for endpoints that have different behavior for authenticated users
 */
export const optionalAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return next(); // Continue without auth
    }

    const token = authHeader.split('Bearer ')[1];
    
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await User.findById(decoded.userId);
      req.user = user;
    } catch (error) {
      // Silently fail and continue without auth
    }

    next();
  } catch (error) {
    // Silently fail and continue without auth
    next();
  }
};

/**
 * Admin authentication middleware
 * For admin panel access
 */
export const verifyAdmin = (req, res, next) => {
  const { username, password } = req.body;

  if (
    username === process.env.ADMIN_USERNAME &&
    password === process.env.ADMIN_PASSWORD
  ) {
    req.isAdmin = true;
    return next();
  }

  res.status(401).json({
    success: false,
    message: 'Invalid admin credentials'
  });
};
