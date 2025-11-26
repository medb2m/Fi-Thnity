import OTP from '../models/OTP.js';
import User from '../models/User.js';
import { getTwilioClient, getTwilioFromNumber } from '../config/twilio.js';
import jwt from 'jsonwebtoken';

/**
 * Generate JWT token for user
 */
const generateAuthToken = (userId) => {
  return jwt.sign({ userId }, process.env.JWT_SECRET, {
    expiresIn: '30d'
  });
};

/**
 * Send OTP code to phone number
 * POST /api/auth/otp/send
 */
export const sendOTP = async (req, res) => {
  try {
    const { phoneNumber } = req.body;

    if (!phoneNumber) {
      return res.status(400).json({
        success: false,
        message: 'Phone number is required'
      });
    }

    // Validate phone number format (E.164)
    if (!phoneNumber.startsWith('+')) {
      return res.status(400).json({
        success: false,
        message: 'Phone number must be in E.164 format (e.g., +21626204432)'
      });
    }

    // Generate OTP code
    const otp = await OTP.createOTP(phoneNumber);
    console.log(`📱 Generated OTP for ${phoneNumber}: ${otp.code}`);

    // Send OTP via Twilio
    const twilioClient = getTwilioClient();
    if (!twilioClient) {
      return res.status(500).json({
        success: false,
        message: 'SMS service not configured. Please contact administrator.'
      });
    }

    const fromNumber = getTwilioFromNumber();
    const messageBody = `Your Fi Thnity verification code is: ${otp.code}. This code will expire in 10 minutes.`;

    try {
      const message = await twilioClient.messages.create({
        body: messageBody,
        from: fromNumber,
        to: phoneNumber
      });

      console.log(`✅ OTP SMS sent successfully to ${phoneNumber}`);
      console.log(`   Message SID: ${message.sid}`);

      res.json({
        success: true,
        message: 'OTP code sent successfully',
        data: {
          phoneNumber,
          expiresIn: 600, // 10 minutes in seconds
          messageSid: message.sid
          // Don't send the code in response for security
        }
      });
    } catch (twilioError) {
      console.error('❌ Twilio error:', twilioError.message);
      console.error('   Error code:', twilioError.code);

      // Delete the OTP if SMS failed
      await OTP.deleteOne({ _id: otp._id });

      return res.status(500).json({
        success: false,
        message: 'Failed to send OTP code',
        error: twilioError.message,
        code: twilioError.code
      });
    }
  } catch (error) {
    console.error('Send OTP error:', error);
    res.status(500).json({
      success: false,
      message: 'Error sending OTP',
      error: error.message
    });
  }
};

/**
 * Verify OTP code and login/register user
 * POST /api/auth/otp/verify
 */
export const verifyOTP = async (req, res) => {
  try {
    const { phoneNumber, code, name } = req.body;

    if (!phoneNumber || !code) {
      return res.status(400).json({
        success: false,
        message: 'Phone number and OTP code are required'
      });
    }

    // Verify OTP code
    const otp = await OTP.verifyOTP(phoneNumber, code);

    if (!otp) {
      return res.status(400).json({
        success: false,
        message: 'Invalid or expired OTP code'
      });
    }

    console.log(`✅ OTP verified successfully for ${phoneNumber}`);

    // Find or create user
    let user = await User.findOne({ phoneNumber });

    if (!user) {
      // Create new user
      user = await User.create({
        phoneNumber,
        name: name || 'User',
        authType: 'phone',
        isVerified: true
      });
      console.log(`👤 New user created: ${user._id}`);
    } else {
      // Update existing user
      if (name && name.trim() !== '') {
        user.name = name;
      }
      user.isVerified = true;
      await user.save();
      console.log(`👤 User updated: ${user._id}`);
    }

    // Generate auth token
    const authToken = generateAuthToken(user._id);

    res.json({
      success: true,
      message: 'Phone number verified successfully',
      data: {
        user: user.getPublicProfile ? user.getPublicProfile() : user,
        token: authToken
      }
    });
  } catch (error) {
    console.error('Verify OTP error:', error);
    res.status(500).json({
      success: false,
      message: 'Error verifying OTP',
      error: error.message
    });
  }
};

/**
 * Resend OTP code
 * POST /api/auth/otp/resend
 */
export const resendOTP = async (req, res) => {
  try {
    const { phoneNumber } = req.body;

    if (!phoneNumber) {
      return res.status(400).json({
        success: false,
        message: 'Phone number is required'
      });
    }

    // Rate limiting: Check if OTP was sent recently (within last 60 seconds)
    const recentOTP = await OTP.findOne({
      phoneNumber,
      createdAt: { $gt: new Date(Date.now() - 60 * 1000) }
    });

    if (recentOTP) {
      return res.status(429).json({
        success: false,
        message: 'Please wait 60 seconds before requesting a new code'
      });
    }

    // Send new OTP (reuse sendOTP logic)
    return sendOTP(req, res);
  } catch (error) {
    console.error('Resend OTP error:', error);
    res.status(500).json({
      success: false,
      message: 'Error resending OTP',
      error: error.message
    });
  }
};

