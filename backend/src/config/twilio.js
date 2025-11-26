import twilio from 'twilio';

// Twilio credentials must be set via environment variables
// Never commit secrets to the repository!
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const fromNumber = process.env.TWILIO_PHONE_NUMBER;

// Initialize Twilio client
let twilioClient = null;

const initializeTwilio = () => {
  if (twilioClient) {
    return twilioClient;
  }

  try {
    if (!accountSid || !authToken) {
      console.error('❌ Twilio credentials not configured!');
      console.error('Please set the following environment variables:');
      console.error('  - TWILIO_ACCOUNT_SID');
      console.error('  - TWILIO_AUTH_TOKEN');
      console.error('  - TWILIO_PHONE_NUMBER (optional, defaults to your Twilio number)');
      console.error('');
      console.error('You can set them in your .env file or as environment variables.');
      return null;
    }

    twilioClient = twilio(accountSid, authToken);
    console.log('✅ Twilio client initialized successfully');
    console.log(`   From number: ${fromNumber}`);
    return twilioClient;
  } catch (error) {
    console.error('❌ Error initializing Twilio:', error.message);
    return null;
  }
};

export const getTwilioClient = () => {
  if (!twilioClient) {
    initializeTwilio();
  }
  return twilioClient;
};

export const getTwilioFromNumber = () => {
  return fromNumber;
};

export default initializeTwilio;

