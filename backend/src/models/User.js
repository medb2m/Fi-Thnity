import mongoose from 'mongoose';
import bcrypt from 'bcryptjs';

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
    default: ''
  },
  timestamp: {
    type: Date,
    default: Date.now
  }
}, { _id: false });

const userSchema = new mongoose.Schema({
  // Firebase Authentication (for mobile app)
  firebaseUid: {
    type: String,
    sparse: true,
    unique: true,
    index: true
  },
  phoneNumber: {
    type: String,
    sparse: true,
    unique: true,
    index: true
  },

  // Email Authentication (for web app)
  email: {
    type: String,
    sparse: true,
    unique: true,
    lowercase: true,
    trim: true,
    index: true
  },
  password: {
    type: String,
    select: false // Don't return password by default
  },
  emailVerified: {
    type: Boolean,
    default: false
  },

  // Authentication type
  authType: {
    type: String,
    enum: ['firebase', 'email'],
    required: true,
    default: 'firebase'
  },

  // Common fields
  name: {
    type: String,
    required: true,
    trim: true,
    minlength: 2,
    maxlength: 50
  },
  photoUrl: {
    type: String,
    default: null
  },
  bio: {
    type: String,
    maxlength: 150,
    default: ''
  },
  rating: {
    type: Number,
    default: 5.0,
    min: 0,
    max: 5
  },
  totalRides: {
    type: Number,
    default: 0
  },
  isVerified: {
    type: Boolean,
    default: false
  },
  currentLocation: locationSchema,
  isActive: {
    type: Boolean,
    default: true
  },
  lastSeen: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  toJSON: { virtuals: true },
  toObject: { virtuals: true }
});

// Virtual for user's active rides
userSchema.virtual('activeRides', {
  ref: 'Ride',
  localField: '_id',
  foreignField: 'user',
  match: { status: 'ACTIVE' }
});

// Hash password before saving (only for email auth users)
userSchema.pre('save', async function(next) {
  this.lastSeen = new Date();

  // Only hash password if it's modified and user is email auth
  if (this.authType === 'email' && this.password && this.isModified('password')) {
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
  }

  next();
});

// Method to compare password
userSchema.methods.comparePassword = async function(candidatePassword) {
  if (!this.password) {
    return false;
  }
  return await bcrypt.compare(candidatePassword, this.password);
};

// Method to get public profile
userSchema.methods.getPublicProfile = function() {
  const user = this.toObject();
  delete user.password;
  delete user.__v;
  return user;
};

// Indexes for performance
userSchema.index({ isActive: 1, lastSeen: -1 });
userSchema.index({ 'currentLocation.latitude': 1, 'currentLocation.longitude': 1 });
userSchema.index({ email: 1, emailVerified: 1 });

const User = mongoose.model('User', userSchema);

export default User;
