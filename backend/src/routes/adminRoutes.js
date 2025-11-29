import express from 'express';
import {
  getDashboard,
  getUsers,
  getRides,
  getPosts,
  createUser,
  updateUser,
  deleteUser,
  createRide,
  updateRide,
  deleteRide,
  deletePost,
  getSupportTickets,
  getSupportTicketDetail,
  addSupportMessage,
  resolveSupportTicket,
  updateTicketStatus
} from '../controllers/adminController.js';

const router = express.Router();

// Simple session middleware (basic authentication)
const isAuthenticated = (req, res, next) => {
  if (req.session?.isAdmin) {
    return next();
  }
  res.redirect('/admin/login');
};

// Login page
router.get('/login', (req, res) => {
  res.render('admin/login', { title: 'Admin Login', error: null });
});

// Login POST
router.post('/login', (req, res) => {
  const { username, password } = req.body;

  if (
    username === process.env.ADMIN_USERNAME &&
    password === process.env.ADMIN_PASSWORD
  ) {
    req.session = { isAdmin: true };
    res.redirect('/admin');
  } else {
    res.render('admin/login', {
      title: 'Admin Login',
      error: 'Invalid credentials'
    });
  }
});

// Logout
router.get('/logout', (req, res) => {
  req.session = null;
  res.redirect('/admin/login');
});

// Protected admin routes
router.get('/', isAuthenticated, getDashboard);
router.get('/users', isAuthenticated, getUsers);
router.get('/rides', isAuthenticated, getRides);
router.get('/posts', isAuthenticated, getPosts);

// User CRUD operations
router.post('/users/create', isAuthenticated, createUser);
router.post('/users/:userId/update', isAuthenticated, updateUser);
router.post('/users/:userId/delete', isAuthenticated, deleteUser);

// Ride CRUD operations
router.post('/rides/create', isAuthenticated, createRide);
router.post('/rides/:rideId/update', isAuthenticated, updateRide);
router.post('/rides/:rideId/delete', isAuthenticated, deleteRide);

// Other delete operations
router.post('/posts/:postId/delete', isAuthenticated, deletePost);

// Support Tickets routes
router.get('/support', isAuthenticated, getSupportTickets);
router.get('/support/:ticketId', isAuthenticated, getSupportTicketDetail);
router.post('/support/:ticketId/message', isAuthenticated, addSupportMessage);
router.post('/support/:ticketId/resolve', isAuthenticated, resolveSupportTicket);
router.post('/support/:ticketId/status', isAuthenticated, updateTicketStatus);

export default router;
