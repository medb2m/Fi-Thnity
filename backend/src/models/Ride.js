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
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true
  },
  firebaseUid: {
    type: String,
    required: true,
    index: true
  },
  rideType: {
    type: String,
    enum: ['REQUEST', 'OFFER'],
    required: true,
    index: true
  },
  transportType: {
    type: String,
    enum: ['TAXI', 'TAXI_COLLECTIF', 'PRIVATE_CAR', 'METRO', 'BUS'],
    required: true
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

// Auto-set expiration time (2 hours from creation)
rideSchema.pre('save', function(next) {
  if (this.isNew && !this.expiresAt) {
    this.expiresAt = new Date(Date.now() + 2 * 60 * 60 * 1000); // 2 hours
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
rideSchema.index({ 'origin.latitude': 1, 'origin.longitude': 1 });
rideSchema.index({ 'destination.latitude': 1, 'destination.longitude': 1 });
rideSchema.index({ createdAt: -1 });

const Ride = mongoose.model('Ride', rideSchema);

export default Ride;
