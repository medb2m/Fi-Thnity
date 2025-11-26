import nodemailer from 'nodemailer';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

// ES Module dirname equivalent
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Ensure environment variables are loaded
dotenv.config({ path: path.join(__dirname, '../../.env') });

// Log email configuration for debugging
console.log('📧 Email Configuration:');
console.log('   Host:', process.env.EMAIL_HOST || 'NOT SET');
console.log('   Port:', process.env.EMAIL_PORT || 'NOT SET');
console.log('   Secure:', process.env.EMAIL_SECURE || 'NOT SET');
console.log('   User:', process.env.EMAIL_USER || 'NOT SET');

// Create transporter
const transporter = nodemailer.createTransport({
  host: process.env.EMAIL_HOST || 'smtp.hostinger.com',
  port: parseInt(process.env.EMAIL_PORT || '465'),
  secure: process.env.EMAIL_SECURE === 'true',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASSWORD
  }
});

// Verify connection
transporter.verify((error, success) => {
  if (error) {
    console.error('❌ Email service error:', error.message);
  } else {
    console.log('✅ Email service ready');
  }
});

/**
 * Get server base URL (IP or domain)
 */
const getServerUrl = () => {
  // Priority: SERVER_URL > SERVER_IP:PORT > default server IP
  if (process.env.SERVER_URL) {
    return process.env.SERVER_URL.replace(/\/$/, ''); // Remove trailing slash
  }
  
  // Use SERVER_IP from env, or default to production server IP
  const serverIP = process.env.SERVER_IP || '72.61.145.239';
  const port = process.env.PORT || '9090';
  
  return `http://${serverIP}:${port}`;
};

/**
 * Send verification email
 */
export const sendVerificationEmail = async (email, name, token) => {
  const serverUrl = getServerUrl();
  const verificationUrl = `${serverUrl}/api/auth/verify-email?token=${token}`;

  const mailOptions = {
    from: process.env.EMAIL_FROM,
    to: email,
    subject: '🚗 Verify Your Fi Thnity Account',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background: #006D9C; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
          .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
          .button { display: inline-block; padding: 12px 30px; background: #006D9C; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
          .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>🚗 Fi Thnity</h1>
            <p>Save Time, Save Tunisia</p>
          </div>
          <div class="content">
            <h2>Welcome, ${name}! 👋</h2>
            <p>Thank you for registering with Fi Thnity, Tunisia's community-driven carpooling platform.</p>
            <p>To complete your registration and start connecting with fellow travelers, please verify your email address by clicking the button below:</p>
            <center>
              <a href="${verificationUrl}" class="button">Verify Email Address</a>
            </center>
            <p>Or copy and paste this link in your browser:</p>
            <p style="background: #fff; padding: 10px; border-radius: 4px; word-break: break-all;">
              ${verificationUrl}
            </p>
            <p><strong>This link will expire in 24 hours.</strong></p>
            <p>If you didn't create an account with Fi Thnity, please ignore this email.</p>
          </div>
          <div class="footer">
            <p>Fi Thnity - On My Way 🇹🇳</p>
            <p>This is an automated email, please do not reply.</p>
          </div>
        </div>
      </body>
      </html>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log(`✅ Verification email sent to ${email}`);
    return true;
  } catch (error) {
    console.error('❌ Error sending verification email:', error);
    throw error;
  }
};

/**
 * Send password reset email
 */
export const sendPasswordResetEmail = async (email, name, token) => {
  const resetUrl = `${process.env.FRONTEND_URL}/reset-password?token=${token}`;

  const mailOptions = {
    from: process.env.EMAIL_FROM,
    to: email,
    subject: '🔒 Reset Your Fi Thnity Password',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background: #006D9C; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
          .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
          .button { display: inline-block; padding: 12px 30px; background: #D62828; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
          .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>🚗 Fi Thnity</h1>
            <p>Save Time, Save Tunisia</p>
          </div>
          <div class="content">
            <h2>Password Reset Request</h2>
            <p>Hello ${name},</p>
            <p>We received a request to reset your password. Click the button below to create a new password:</p>
            <center>
              <a href="${resetUrl}" class="button">Reset Password</a>
            </center>
            <p>Or copy and paste this link in your browser:</p>
            <p style="background: #fff; padding: 10px; border-radius: 4px; word-break: break-all;">
              ${resetUrl}
            </p>
            <p><strong>This link will expire in 1 hour.</strong></p>
            <p>If you didn't request a password reset, please ignore this email. Your password will remain unchanged.</p>
          </div>
          <div class="footer">
            <p>Fi Thnity - On My Way 🇹🇳</p>
            <p>This is an automated email, please do not reply.</p>
          </div>
        </div>
      </body>
      </html>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log(`✅ Password reset email sent to ${email}`);
    return true;
  } catch (error) {
    console.error('❌ Error sending password reset email:', error);
    throw error;
  }
};

/**
 * Send welcome email
 */
export const sendWelcomeEmail = async (email, name) => {
  const mailOptions = {
    from: process.env.EMAIL_FROM,
    to: email,
    subject: '🎉 Welcome to Fi Thnity!',
    html: `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background: linear-gradient(135deg, #006D9C 0%, #FFD54F 100%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
          .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
          .feature { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #006D9C; }
          .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>🚗 Welcome to Fi Thnity!</h1>
            <p>Your account has been verified</p>
          </div>
          <div class="content">
            <h2>Hello ${name}! 🎉</h2>
            <p>Your email has been successfully verified. You're now part of the Fi Thnity community!</p>

            <h3>What's Next?</h3>

            <div class="feature">
              <strong>🚖 Share Your Journey</strong>
              <p>Post ride offers when you're traveling and help others reach their destination.</p>
            </div>

            <div class="feature">
              <strong>🗺️ Find Rides</strong>
              <p>Browse available rides or post requests when you need a lift.</p>
            </div>

            <div class="feature">
              <strong>👥 Join the Community</strong>
              <p>Share traffic updates, road closures, and stay connected with fellow travelers.</p>
            </div>

            <p>Together, we're making transportation easier and more accessible for all Tunisians while keeping our planet safe! 🇹🇳</p>

            <p style="margin-top: 30px;">
              <strong>Happy travels!</strong><br>
              The Fi Thnity Team
            </p>
          </div>
          <div class="footer">
            <p>Fi Thnity - On My Way (يقيرط يف)</p>
            <p>Save Time, Save Tunisia 🇹🇳</p>
          </div>
        </div>
      </body>
      </html>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log(`✅ Welcome email sent to ${email}`);
    return true;
  } catch (error) {
    console.error('❌ Error sending welcome email:', error);
    // Don't throw error for welcome email - it's not critical
    return false;
  }
};

export default transporter;
