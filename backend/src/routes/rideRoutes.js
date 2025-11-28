import express from 'express';
import { body } from 'express-validator';
import {
  createRide,
  getRides,
  getRideById,
  getMyRides,
  updateRide,
  updateRideStatus,
  deleteRide,
  findMatchingRides,
  addUserToRide
} from '../controllers/rideController.js';
import { authenticate } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

// Create a new ride
router.post(
  '/',
  authenticate,
  [
    body('rideType').isIn(['REQUEST', 'OFFER']),
    body('transportType').isIn(['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS']),
    body('origin.latitude').isFloat({ min: -90, max: 90 }),
    body('origin.longitude').isFloat({ min: -180, max: 180 }),
    body('origin.address').notEmpty().trim(),
    body('destination.latitude').isFloat({ min: -90, max: 90 }),
    body('destination.longitude').isFloat({ min: -180, max: 180 }),
    body('destination.address').notEmpty().trim(),
    body('availableSeats').optional().isInt({ min: 0, max: 8 }),
    body('notes').optional().trim().isLength({ max: 200 }),
    body('departureDate').optional().isISO8601().toDate(),
    body('price').optional().isFloat({ min: 0, max: 1000 }),
    handleValidationErrors
  ],
  createRide
);

// Get all active rides with filtering
router.get('/', getRides);

// Get user's rides
router.get('/my-rides', authenticate, getMyRides);

// Find matching rides
router.post(
  '/match',
  authenticate,
  [
    body('origin.latitude').isFloat({ min: -90, max: 90 }),
    body('origin.longitude').isFloat({ min: -180, max: 180 }),
    body('destination.latitude').isFloat({ min: -90, max: 90 }),
    body('destination.longitude').isFloat({ min: -180, max: 180 }),
    body('rideType').isIn(['REQUEST', 'OFFER']),
    body('maxDistance').optional().isFloat({ min: 0.1, max: 50 }),
    handleValidationErrors
  ],
  findMatchingRides
);

// Add user to ride offer (must be before /:rideId route)
router.put(
  '/:rideId/add-user',
  authenticate,
  [
    body('userId').notEmpty().withMessage('userId is required'),
    handleValidationErrors
  ],
  addUserToRide
);

// Update a ride (must be before /:rideId/status route)
router.put(
  '/:rideId',
  authenticate,
  [
    body('transportType').optional().isIn(['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS']),
    body('origin.latitude').optional().isFloat({ min: -90, max: 90 }),
    body('origin.longitude').optional().isFloat({ min: -180, max: 180 }),
    body('origin.address').optional().notEmpty().trim(),
    body('destination.latitude').optional().isFloat({ min: -90, max: 90 }),
    body('destination.longitude').optional().isFloat({ min: -180, max: 180 }),
    body('destination.address').optional().notEmpty().trim(),
    body('availableSeats').optional().isInt({ min: 1, max: 8 }),
    body('notes').optional().trim().isLength({ max: 200 }),
    body('departureDate').optional().isISO8601().toDate(),
    body('price').optional().isFloat({ min: 0, max: 1000 }),
    handleValidationErrors
  ],
  updateRide
);

// Update ride status
router.put(
  '/:rideId/status',
  authenticate,
  [
    body('status').isIn(['ACTIVE', 'MATCHED', 'COMPLETED', 'CANCELLED', 'EXPIRED']),
    handleValidationErrors
  ],
  updateRideStatus
);

// Get ride by ID
router.get('/:rideId', getRideById);

// Delete/Cancel a ride
router.delete('/:rideId', authenticate, deleteRide);

export default router;
