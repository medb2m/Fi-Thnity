// Load environment variables FIRST before any other imports
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

// ES Module dirname equivalent
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load .env file from backend directory
dotenv.config({ path: path.join(__dirname, '../.env') });

import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import cookieSession from 'cookie-session';
import multer from 'multer';
// Import configurations
import connectDB from './config/database.js';
import initializeTwilio from './config/twilio.js';
import './config/email.js'; // Initialize email service

// Import scheduled jobs
import { initializeScheduledJobs } from './jobs/scheduledJobs.js';

// Import routes
import authRoutes from './routes/authRoutes.js';
import userRoutes from './routes/userRoutes.js';
import rideRoutes from './routes/rideRoutes.js';
import communityRoutes from './routes/communityRoutes.js';
import chatRoutes from './routes/chatRoutes.js';
import notificationRoutes from './routes/notificationRoutes.js';
import adminRoutes from './routes/adminRoutes.js';

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 3000;

// Increase timeout for file uploads (5 minutes)
app.use((req, res, next) => {
  req.setTimeout(5 * 60 * 1000); // 5 minutes
  res.setTimeout(5 * 60 * 1000); // 5 minutes
  next();
});

// Connect to MongoDB
connectDB();

// Initialize Twilio
const twilioInitialized = initializeTwilio();
if (!twilioInitialized) {
  console.warn('⚠️  WARNING: Twilio not initialized!');
  console.warn('⚠️  OTP/SMS endpoints will fail until Twilio is configured.');
}

// Initialize scheduled jobs (after DB connection)
initializeScheduledJobs();

// Middleware
// Configure Helmet for HTTP (adjust for production with HTTPS)
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-hashes'"],
      scriptSrcAttr: ["'unsafe-inline'"], // Allow inline event handlers (onclick, etc.)
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'"],
      fontSrc: ["'self'", "data:"],
      objectSrc: ["'none'"],
      mediaSrc: ["'self'"],
      frameSrc: ["'none'"],
      formAction: ["'self'"], // Allow form submissions to same origin
      upgradeInsecureRequests: null // Disable for HTTP (enable for HTTPS)
    }
  },
  crossOriginOpenerPolicy: false, // Disable COOP for HTTP (enable for HTTPS)
  crossOriginResourcePolicy: { policy: "cross-origin" }, // Allow cross-origin resources
  originAgentCluster: false // Disable origin-keyed agent cluster for HTTP
}));
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));
app.use(morgan('dev')); // Logging

// Request logging middleware (before body parsing)
app.use((req, res, next) => {
  if (req.path.includes('/api/community/posts') && req.method === 'POST') {
    console.log('🔍 Incoming POST to /api/community/posts');
    console.log('   URL:', req.url);
    console.log('   Path:', req.path);
    console.log('   Headers:', {
      'content-type': req.headers['content-type'],
      'content-length': req.headers['content-length'],
      'authorization': req.headers['authorization'] ? 'Present' : 'Missing',
      'user-agent': req.headers['user-agent']?.substring(0, 50)
    });
    // Log request start time
    req._startTime = Date.now();
  }
  next();
});

// Error handler for uncaught errors during request parsing
app.use((err, req, res, next) => {
  if (req.path.includes('/api/community/posts') && req.method === 'POST') {
    console.error('❌ Error during request parsing:', err);
    console.error('   Error stack:', err.stack);
  }
  next(err);
});

// Increase body size limits for file uploads
app.use(express.json({ limit: '50mb' })); // Parse JSON bodies
app.use(express.urlencoded({ extended: true, limit: '50mb' })); // Parse URL-encoded bodies

// Session middleware for admin panel
app.use(cookieSession({
  name: 'admin-session',
  secret: process.env.JWT_SECRET || 'default-secret-change-in-production',
  maxAge: 24 * 60 * 60 * 1000 // 24 hours
}));

// Static files
const publicPath = path.join(__dirname, 'public');
console.log('📁 Static files directory:', publicPath);
app.use(express.static(publicPath));

// Serve uploaded profile pictures
const uploadsPath = path.join(__dirname, 'uploads');
console.log('📁 Uploads directory:', uploadsPath);
app.use('/uploads', express.static(uploadsPath));

// Explicit route for logo (fallback)
app.get('/fithnity_logo.png', (req, res) => {
  const logoPath = path.join(__dirname, 'public', 'fithnity_logo.png');
  res.sendFile(logoPath, (err) => {
    if (err) {
      console.error('❌ Error serving logo:', err);
      res.status(404).json({ error: 'Logo not found' });
    }
  });
});

// View engine setup (for admin panel)
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    environment: process.env.NODE_ENV
  });
});

// Multer error handler (must be before routes to catch multer errors)
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    console.error('❌ Multer error:', err);
    console.error('   Error code:', err.code);
    console.error('   Error field:', err.field);
    console.error('   Request path:', req.path);
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
  // Pass other errors to the next error handler
  next(err);
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/rides', rideRoutes);
app.use('/api/community', communityRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/notifications', notificationRoutes);

// Debug: Log registered routes
console.log('📋 Registered API routes:');
console.log('   POST /api/auth/otp/send');
console.log('   POST /api/auth/otp/verify');
console.log('   POST /api/auth/otp/resend');

// Admin Panel Routes
app.use('/admin', adminRoutes);

// Root endpoint
app.get('/', (req, res) => {
  res.json({
    message: 'Welcome to Fi Thnity API',
    version: '1.0.0',
    endpoints: {
      health: '/health',
      api: {
        auth: '/api/auth',
        users: '/api/users',
        rides: '/api/rides',
        community: '/api/community'
      },
      admin: '/admin'
    }
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Route not found',
    path: req.originalUrl
  });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('Error:', err);

  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal Server Error',
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
});

// Start server - explicitly bind to 0.0.0.0 for IPv4 support
app.listen(PORT, '0.0.0.0', () => {
  console.log(`
╔═══════════════════════════════════════════════╗
║                                               ║
║   🚗 Fi Thnity Backend Server                ║
║                                               ║
║   🌍 Environment: ${process.env.NODE_ENV?.padEnd(19) || 'development'.padEnd(19)}    ║
║   🚀 Server running on port ${PORT.toString().padEnd(14)}    ║
║   📍 URL: http://localhost:${PORT}              ║
║   🔧 Admin Panel: http://localhost:${PORT}/admin  ║
║                                               ║
╚═══════════════════════════════════════════════╝
  `);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received. Shutting down gracefully...');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('\nSIGINT received. Shutting down gracefully...');
  process.exit(0);
});
