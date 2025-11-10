import jwt from 'jsonwebtoken';
import { getFirebaseAuth } from '../config/firebase.js';
import User from '../models/User.js';

/**
 * Middleware to verify Firebase ID token
 * Expects Authorization header with "Bearer <token>"
 */
export const verifyFirebaseToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No token provided. Authorization header must be "Bearer <token>"'
      });
    }

    const idToken = authHeader.split('Bearer ')[1];

    // Verify the token with Firebase Admin
    const decodedToken = await getFirebaseAuth().verifyIdToken(idToken);

    // Attach Firebase user info to request
    req.firebaseUser = {
      uid: decodedToken.uid,
      phone: decodedToken.phone_number,
      email: decodedToken.email
    };

    // Try to find user in database
    let user = await User.findOne({ firebaseUid: decodedToken.uid });

    // If user doesn't exist in our DB, create them
    if (!user) {
      user = await User.create({
        firebaseUid: decodedToken.uid,
        name: decodedToken.name || 'User',
        phoneNumber: decodedToken.phone_number || '',
        photoUrl: decodedToken.picture || null
      });
    }

    // Attach user to request
    req.user = user;

    next();
  } catch (error) {
    console.error('Firebase token verification error:', error);

    if (error.code === 'auth/id-token-expired') {
      return res.status(401).json({
        success: false,
        message: 'Token expired. Please login again.'
      });
    }

    if (error.code === 'auth/argument-error') {
      return res.status(401).json({
        success: false,
        message: 'Invalid token format'
      });
    }

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

    const idToken = authHeader.split('Bearer ')[1];
    const decodedToken = await getFirebaseAuth().verifyIdToken(idToken);

    req.firebaseUser = {
      uid: decodedToken.uid,
      phone: decodedToken.phone_number,
      email: decodedToken.email
    };

    const user = await User.findOne({ firebaseUid: decodedToken.uid });
    req.user = user;

    next();
  } catch (error) {
    // Silently fail and continue without auth
    next();
  }
};

/**
 * Unified authentication middleware
 * Handles both JWT (email auth) and Firebase tokens (mobile auth)
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

    // Try JWT first (email auth)
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      const user = await User.findById(decoded.userId);

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'User not found'
        });
      }

      if (user.authType === 'email' && !user.emailVerified) {
        return res.status(403).json({
          success: false,
          message: 'Please verify your email before accessing this resource'
        });
      }

      req.user = user;
      req.authType = 'jwt';
      return next();
    } catch (jwtError) {
      // If JWT fails, try Firebase token (mobile auth)
      try {
        const decodedToken = await getFirebaseAuth().verifyIdToken(token);

        req.firebaseUser = {
          uid: decodedToken.uid,
          phone: decodedToken.phone_number,
          email: decodedToken.email
        };

        let user = await User.findOne({ firebaseUid: decodedToken.uid });

        // Create user if doesn't exist
        if (!user) {
          user = await User.create({
            firebaseUid: decodedToken.uid,
            name: decodedToken.name || 'User',
            phoneNumber: decodedToken.phone_number || '',
            photoUrl: decodedToken.picture || null,
            authType: 'firebase'
          });
        }

        req.user = user;
        req.authType = 'firebase';
        return next();
      } catch (firebaseError) {
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired token',
          error: firebaseError.message
        });
      }
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
