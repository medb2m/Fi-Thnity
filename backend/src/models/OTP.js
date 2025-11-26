import mongoose from 'mongoose';

const otpSchema = new mongoose.Schema({
  phoneNumber: {
    type: String,
    required: true,
    index: true
  },
  code: {
    type: String,
    required: true,
    index: true
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true,
    expires: 600 // Auto-delete after 10 minutes (MongoDB TTL index)
  },
  verified: {
    type: Boolean,
    default: false
  },
  attempts: {
    type: Number,
    default: 0
  },
  maxAttempts: {
    type: Number,
    default: 5
  }
}, {
  timestamps: true
});

// Generate 6-digit OTP code
otpSchema.statics.generateCode = function() {
  return Math.floor(100000 + Math.random() * 900000).toString();
};

// Create OTP for phone number
otpSchema.statics.createOTP = async function(phoneNumber) {
  // Invalidate any existing OTPs for this phone number
  await this.updateMany(
    { phoneNumber, verified: false },
    { verified: true } // Mark as used/invalid
  );

  // Generate new OTP
  const code = this.generateCode();
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes

  const otp = await this.create({
    phoneNumber,
    code,
    expiresAt
  });

  return otp;
};

// Verify OTP code
otpSchema.statics.verifyOTP = async function(phoneNumber, code) {
  const otp = await this.findOne({
    phoneNumber,
    code,
    verified: false,
    expiresAt: { $gt: new Date() },
    attempts: { $lt: 5 } // Max 5 attempts
  });

  if (!otp) {
    // Increment attempts for failed verification
    await this.updateOne(
      { phoneNumber, code, verified: false },
      { $inc: { attempts: 1 } }
    );
    return null;
  }

  // Mark as verified
  otp.verified = true;
  await otp.save();

  return otp;
};

// Clean up expired OTPs (MongoDB TTL will handle this, but this is a backup)
otpSchema.statics.cleanupExpired = async function() {
  const result = await this.deleteMany({
    $or: [
      { expiresAt: { $lt: new Date() } },
      { verified: true, createdAt: { $lt: new Date(Date.now() - 24 * 60 * 60 * 1000) } } // 24 hours old
    ]
  });

  console.log(`🗑️  Cleaned up ${result.deletedCount} expired OTPs`);
  return result.deletedCount;
};

const OTP = mongoose.model('OTP', otpSchema);

export default OTP;

