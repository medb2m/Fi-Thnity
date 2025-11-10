import mongoose from 'mongoose';
import crypto from 'crypto';

const verificationTokenSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true
  },
  token: {
    type: String,
    required: true,
    unique: true,
    index: true
  },
  type: {
    type: String,
    enum: ['email-verification', 'password-reset'],
    required: true
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true
  },
  used: {
    type: Boolean,
    default: false
  },
  usedAt: {
    type: Date,
    default: null
  }
}, {
  timestamps: true
});

// Generate random token
verificationTokenSchema.statics.generateToken = function() {
  return crypto.randomBytes(32).toString('hex');
};

// Create verification token for user
verificationTokenSchema.statics.createEmailVerificationToken = async function(userId) {
  const token = this.generateToken();
  const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 hours

  const verificationToken = await this.create({
    user: userId,
    token,
    type: 'email-verification',
    expiresAt
  });

  return verificationToken.token;
};

// Create password reset token
verificationTokenSchema.statics.createPasswordResetToken = async function(userId) {
  const token = this.generateToken();
  const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

  const verificationToken = await this.create({
    user: userId,
    token,
    type: 'password-reset',
    expiresAt
  });

  return verificationToken.token;
};

// Verify token
verificationTokenSchema.statics.verifyToken = async function(token, type) {
  const verificationToken = await this.findOne({
    token,
    type,
    used: false,
    expiresAt: { $gt: new Date() }
  }).populate('user');

  if (!verificationToken) {
    return null;
  }

  return verificationToken;
};

// Mark token as used
verificationTokenSchema.methods.markAsUsed = async function() {
  this.used = true;
  this.usedAt = new Date();
  await this.save();
};

// Clean up expired tokens (can be run as a cron job)
verificationTokenSchema.statics.cleanupExpired = async function() {
  const result = await this.deleteMany({
    $or: [
      { expiresAt: { $lt: new Date() } },
      { used: true, usedAt: { $lt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) } } // 7 days old
    ]
  });

  console.log(`🗑️  Cleaned up ${result.deletedCount} expired tokens`);
  return result.deletedCount;
};

const VerificationToken = mongoose.model('VerificationToken', verificationTokenSchema);

export default VerificationToken;
