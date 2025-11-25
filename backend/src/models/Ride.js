import mongoose from 'mongoose';

const locationSchema = new mongoose.Schema({
  latitude: {
    type: Number,
    required: true
  },
  longitude: {
    type: Number,
    required: true
  },
  address: {
    type: String,
    required: true
  }
}, { _id: false });

const rideSchema = new mongoose.Schema({
  // User reference (following User.js pattern)
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true
  },
  // Firebase UID for mobile app authentication (following User.js pattern)
  firebaseUid: {
    type: String,
    required: true,
    index: true,
    sparse: true
  },
  // Ride type: REQUEST (user needs a ride) or OFFER (user offers a ride)
  rideType: {
    type: String,
    enum: ['REQUEST', 'OFFER'],
    required: true,
    index: true
  },
  // Transport type (matching mobile VehicleType: PERSONAL_CAR -> PRIVATE_CAR, TAXI -> TAXI)
  transportType: {
    type: String,
    enum: ['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS'],
    required: true,
    index: true
  },
  origin: {
    type: locationSchema,
    required: true
  },
  destination: {
    type: locationSchema,
    required: true
  },
  availableSeats: {
    type: Number,
    default: 1,
    min: 0,
    max: 8
  },
  // Departure date and time (when the ride will start)
  departureDate: {
    type: Date,
    required: true,
    index: true
  },
  // Price per person in TND (only for TAXI and TAXI_COLLECTIF, null for PRIVATE_CAR)
  // Matching mobile: price is only set for VehicleType.TAXI
  price: {
    type: Number,
    default: null,
    min: 0,
    max: 1000
  },
  status: {
    type: String,
    enum: ['ACTIVE', 'MATCHED', 'COMPLETED', 'CANCELLED', 'EXPIRED'],
    default: 'ACTIVE',
    index: true
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true
  },
  matchedWith: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    default: null
  },
  distance: {
    type: Number, // in kilometers
    default: null
  },
  notes: {
    type: String,
    maxlength: 200,
    default: ''
  }
}, {
  timestamps: true,
  toJSON: { virtuals: true },
  toObject: { virtuals: true }
});

// Virtual for checking if ride is expired
rideSchema.virtual('isExpired').get(function() {
  return new Date() > this.expiresAt;
});

// Virtual for checking if shareable
rideSchema.virtual('isShareable').get(function() {
  return ['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR'].includes(this.transportType);
});

// Auto-set expiration time (2 hours from departure date, or 2 hours from creation if no departure date)
rideSchema.pre('save', function(next) {
  if (this.isNew && !this.expiresAt) {
    // If departureDate is set, expire 2 hours after departure
    // Otherwise, expire 2 hours from now
    const baseDate = this.departureDate || new Date();
    this.expiresAt = new Date(baseDate.getTime() + 2 * 60 * 60 * 1000); // 2 hours
  }
  next();
});

// Validate that price is set for taxi rides
rideSchema.pre('save', function(next) {
  if (['TAXI', 'TAXI_COLLECTIF'].includes(this.transportType)) {
    if (this.price == null || this.price <= 0) {
      return next(new Error('Price is required for taxi rides'));
    }
  } else {
    // For non-taxi rides, price should be null
    this.price = null;
  }
  next();
});

// Calculate distance between origin and destination
rideSchema.methods.calculateDistance = function() {
  const R = 6371; // Earth's radius in km
  const dLat = (this.destination.latitude - this.origin.latitude) * Math.PI / 180;
  const dLon = (this.destination.longitude - this.origin.longitude) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(this.origin.latitude * Math.PI / 180) *
            Math.cos(this.destination.latitude * Math.PI / 180) *
            Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  this.distance = R * c;
  return this.distance;
};

// Indexes for performance
rideSchema.index({ status: 1, expiresAt: 1 });
rideSchema.index({ rideType: 1, status: 1 });
rideSchema.index({ transportType: 1, status: 1 });
rideSchema.index({ departureDate: 1, status: 1 });
rideSchema.index({ 'origin.latitude': 1, 'origin.longitude': 1 });
rideSchema.index({ 'destination.latitude': 1, 'destination.longitude': 1 });
rideSchema.index({ createdAt: -1 });
rideSchema.index({ user: 1, status: 1 });

const Ride = mongoose.model('Ride', rideSchema);

export default Ride;
