import admin from 'firebase-admin';
import { readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let firebaseApp = null;

const initializeFirebase = () => {
  try {
    // Try to load service account from file (recommended for production)
    const serviceAccountPath = join(__dirname, '../../firebase-service-account.json');

    try {
      const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));

      // Validate that it's actually an Admin SDK service account (not google-services.json)
      if (!serviceAccount.private_key || !serviceAccount.client_email) {
        console.warn('⚠️  firebase-service-account.json is not a valid Admin SDK service account.');
        console.warn('⚠️  It appears to be a google-services.json (Android client config).');
        console.warn('⚠️  Please download the Admin SDK service account from Firebase Console.');
        throw new Error('Invalid service account file');
      }

      firebaseApp = admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.europe-west1.firebasedatabase.app`
      });

      console.log('✅ Firebase Admin initialized with service account file');
    } catch (fileError) {
      // Fallback to environment variables if file doesn't exist
      if (process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
        firebaseApp = admin.initializeApp({
          credential: admin.credential.cert({
            projectId: process.env.FIREBASE_PROJECT_ID,
            privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
            clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
          }),
          databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.europe-west1.firebasedatabase.app`
        });

        console.log('✅ Firebase Admin initialized with environment variables');
      } else {
        console.error('❌ Firebase Admin SDK not configured!');
        console.error('❌ Phone authentication will NOT work.');
        console.error('');
        console.error('📋 To fix this, follow these steps:');
        console.error('1. Go to Firebase Console: https://console.firebase.google.com');
        console.error('2. Select your project: fi-thnity-11a68');
        console.error('3. Go to Project Settings → Service Accounts');
        console.error('4. Click "Generate New Private Key"');
        console.error('5. Save the file as: backend/firebase-service-account.json');
        console.error('6. Restart the server');
        console.error('');
      }
    }

    return firebaseApp;
  } catch (error) {
    console.error('❌ Error initializing Firebase Admin:', error.message);
    return null;
  }
};

export default initializeFirebase;
export const getFirebaseApp = () => {
  if (!firebaseApp) {
    throw new Error('Firebase Admin SDK is not initialized. Please configure firebase-service-account.json');
  }
  return firebaseApp;
};
export const getFirebaseAuth = () => {
  if (!firebaseApp) {
    throw new Error('Firebase Admin SDK is not initialized. Please configure firebase-service-account.json');
  }
  return admin.auth();
};
export const getFirebaseDB = () => {
  if (!firebaseApp) {
    throw new Error('Firebase Admin SDK is not initialized. Please configure firebase-service-account.json');
  }
  return admin.database();
};
