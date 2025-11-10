import express from 'express';
import { body } from 'express-validator';
import {
  createRide,
  getRides,
  getRideById,
  getMyRides,
  updateRideStatus,
  deleteRide,
  findMatchingRides
} from '../controllers/rideController.js';
import { verifyFirebaseToken } from '../middleware/auth.js';
import handleValidationErrors from '../middleware/validate.js';

const router = express.Router();

// Create a new ride
router.post(
  '/',
  verifyFirebaseToken,
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
    handleValidationErrors
  ],
  createRide
);

// Get all active rides with filtering
router.get('/', getRides);

// Get user's rides
router.get('/my-rides', verifyFirebaseToken, getMyRides);

// Find matching rides
router.post(
  '/match',
  verifyFirebaseToken,
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

// Get ride by ID
router.get('/:rideId', getRideById);

// Update ride status
router.put(
  '/:rideId/status',
  verifyFirebaseToken,
  [
    body('status').isIn(['ACTIVE', 'MATCHED', 'COMPLETED', 'CANCELLED', 'EXPIRED']),
    handleValidationErrors
  ],
  updateRideStatus
);

// Delete/Cancel a ride
router.delete('/:rideId', verifyFirebaseToken, deleteRide);

export default router;
