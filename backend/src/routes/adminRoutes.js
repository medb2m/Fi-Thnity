import express from 'express';
import {
  getDashboard,
  getUsers,
  getRides,
  getPosts,
  createUser,
  updateUser,
  deleteUser,
  deleteRide,
  deletePost
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

// Other delete operations
router.post('/rides/:rideId/delete', isAuthenticated, deleteRide);
router.post('/posts/:postId/delete', isAuthenticated, deletePost);

export default router;
