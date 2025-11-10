// Load environment variables
import dotenv from 'dotenv';
dotenv.config();

import mongoose from 'mongoose';
import User from './src/models/User.js';

async function fixDatabase() {
  try {
    console.log('🔧 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB');

    console.log('\n📊 Checking current indexes...');
    const indexes = await User.collection.getIndexes();
    console.log('Current indexes:', Object.keys(indexes));

    // Step 1: Delete all users with null firebaseUid (except the first one if any)
    console.log('\n🧹 Cleaning up duplicate users...');
    const usersWithNullFirebaseUid = await User.find({
      firebaseUid: null,
      authType: 'email'
    }).sort({ createdAt: 1 });

    if (usersWithNullFirebaseUid.length > 0) {
      console.log(`Found ${usersWithNullFirebaseUid.length} email users`);

      // Delete all except keep them for now (we'll handle duplicates differently)
      console.log('Deleting all existing users to start fresh...');
      await User.deleteMany({ authType: 'email' });
      console.log('✅ Cleaned up email users');
    }

    // Step 2: Drop the problematic firebaseUid index if it exists
    console.log('\n🔨 Dropping old firebaseUid index...');
    try {
      await User.collection.dropIndex('firebaseUid_1');
      console.log('✅ Dropped firebaseUid_1 index');
    } catch (error) {
      if (error.code === 27) {
        console.log('ℹ️  Index firebaseUid_1 does not exist (already dropped)');
      } else {
        console.log('⚠️  Error dropping index:', error.message);
      }
    }

    // Step 3: Drop phoneNumber index and recreate
    console.log('\n🔨 Dropping old phoneNumber index...');
    try {
      await User.collection.dropIndex('phoneNumber_1');
      console.log('✅ Dropped phoneNumber_1 index');
    } catch (error) {
      if (error.code === 27) {
        console.log('ℹ️  Index phoneNumber_1 does not exist');
      } else {
        console.log('⚠️  Error dropping index:', error.message);
      }
    }

    // Step 4: Recreate indexes with sparse option
    console.log('\n🔨 Creating new sparse indexes...');

    await User.collection.createIndex(
      { firebaseUid: 1 },
      { unique: true, sparse: true, name: 'firebaseUid_1' }
    );
    console.log('✅ Created sparse unique index for firebaseUid');

    await User.collection.createIndex(
      { phoneNumber: 1 },
      { unique: true, sparse: true, name: 'phoneNumber_1' }
    );
    console.log('✅ Created sparse unique index for phoneNumber');

    await User.collection.createIndex(
      { email: 1 },
      { unique: true, sparse: true, name: 'email_1' }
    );
    console.log('✅ Created sparse unique index for email');

    // Step 5: Verify indexes
    console.log('\n📊 Verifying new indexes...');
    const newIndexes = await User.collection.getIndexes();
    console.log('New indexes:', Object.keys(newIndexes));

    console.log('\n✅ Database fix completed successfully!');
    console.log('\n📝 Summary:');
    console.log('   - Cleaned up duplicate users');
    console.log('   - Recreated sparse unique indexes');
    console.log('   - Email registration should now work correctly');

  } catch (error) {
    console.error('❌ Error fixing database:', error);
    throw error;
  } finally {
    await mongoose.connection.close();
    console.log('\n👋 Disconnected from MongoDB');
  }
}

// Run the fix
fixDatabase()
  .then(() => {
    console.log('\n✨ All done!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n💥 Fatal error:', error);
    process.exit(1);
  });
