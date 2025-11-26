import admin from 'firebase-admin';
import { readFileSync, existsSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let firebaseApp = null;

const initializeFirebase = () => {
  // Skip if already initialized
  if (firebaseApp) {
    return firebaseApp;
  }

  try {
    // Try multiple possible paths for the service account file
    const possiblePaths = [
      join(__dirname, '../../firebase-service-account.json'), // Relative to config folder
      join(process.cwd(), 'firebase-service-account.json'), // Root of project
      '/opt/fi-thnity/backend/firebase-service-account.json', // Production path
      process.env.FIREBASE_SERVICE_ACCOUNT_PATH // Custom path from env
    ].filter(Boolean); // Remove undefined values

    let serviceAccount = null;
    let loadedFrom = null;

    // Try to load from file first
    for (const serviceAccountPath of possiblePaths) {
      try {
        if (serviceAccountPath && existsSync(serviceAccountPath)) {
          const fileContent = readFileSync(serviceAccountPath, 'utf8');
          serviceAccount = JSON.parse(fileContent);

          // Validate that it's actually an Admin SDK service account
          if (!serviceAccount.private_key || !serviceAccount.client_email) {
            console.warn(`⚠️  ${serviceAccountPath} is not a valid Admin SDK service account.`);
            console.warn('⚠️  It appears to be a google-services.json (Android client config).');
            serviceAccount = null;
            continue;
          }

          loadedFrom = serviceAccountPath;
          break;
        }
      } catch (fileError) {
        // Try next path
        continue;
      }
    }

    // If file loading failed, try environment variables
    if (!serviceAccount) {
      if (process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PROJECT_ID) {
        serviceAccount = {
          projectId: process.env.FIREBASE_PROJECT_ID,
          privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
          clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        };
        loadedFrom = 'environment variables';
      }
    }

    // Initialize Firebase if we have credentials
    if (serviceAccount) {
      const databaseURL = process.env.FIREBASE_DATABASE_URL || 
                         `https://${(serviceAccount.projectId || process.env.FIREBASE_PROJECT_ID)}-default-rtdb.europe-west1.firebasedatabase.app`;

      try {
        // Check if Firebase is already initialized
        const existingApp = admin.apps.find(app => app.name === '[DEFAULT]');
        if (existingApp) {
          firebaseApp = existingApp;
          console.log(`✅ Firebase Admin already initialized, reusing existing instance`);
        } else {
          firebaseApp = admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            databaseURL: databaseURL
          });
          console.log(`✅ Firebase Admin initialized successfully`);
        }
        console.log(`   Loaded from: ${loadedFrom}`);
        console.log(`   Database URL: ${databaseURL}`);
        return firebaseApp;
      } catch (initError) {
        // If initialization fails (e.g., already initialized), try to get existing app
        if (initError.code === 'app/duplicate-app') {
          firebaseApp = admin.app();
          console.log(`✅ Firebase Admin already initialized, using existing instance`);
          return firebaseApp;
        }
        throw initError;
      }
    } else {
      console.error('❌ Firebase Admin SDK not configured!');
      console.error('❌ Phone authentication and Firebase features will NOT work.');
      console.error('');
      console.error('📋 Configuration Options:');
      console.error('');
      console.error('Option 1: Service Account File');
      console.error('  1. Go to Firebase Console: https://console.firebase.google.com');
      console.error('  2. Select your project: fi-thnity-11a68');
      console.error('  3. Go to Project Settings → Service Accounts');
      console.error('  4. Click "Generate New Private Key"');
      console.error('  5. Save the file as: backend/firebase-service-account.json');
      console.error('');
      console.error('Option 2: Environment Variables');
      console.error('  Set these environment variables:');
      console.error('  - FIREBASE_PROJECT_ID');
      console.error('  - FIREBASE_PRIVATE_KEY');
      console.error('  - FIREBASE_CLIENT_EMAIL');
      console.error('');
      console.error('Tried paths:');
      possiblePaths.forEach(path => console.error(`  - ${path}`));
      console.error('');
      return null;
    }
  } catch (error) {
    console.error('❌ Error initializing Firebase Admin:', error.message);
    console.error('Stack:', error.stack);
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
    // Try to initialize one more time
    initializeFirebase();
    if (!firebaseApp) {
      throw new Error('Firebase Admin SDK is not initialized. Please configure firebase-service-account.json or set FIREBASE_PRIVATE_KEY, FIREBASE_CLIENT_EMAIL, and FIREBASE_PROJECT_ID environment variables.');
    }
  }
  return admin.auth();
};
export const getFirebaseDB = () => {
  if (!firebaseApp) {
    throw new Error('Firebase Admin SDK is not initialized. Please configure firebase-service-account.json');
  }
  return admin.database();
};
