import cron from 'node-cron';
import Ride from '../models/Ride.js';

/**
 * Delete expired rides (rides where departureDate has passed)
 * This job runs every 30 minutes to clean up expired rides
 */
export const deleteExpiredRides = async () => {
  try {
    const now = new Date();
    
    // Find and delete rides where departureDate is in the past
    // We delete rides that are ACTIVE and have passed their departure time
    // This automatically removes offers and requests that are no longer valid
    const result = await Ride.deleteMany({
      status: { $in: ['ACTIVE', 'EXPIRED'] },
      departureDate: { $lt: now }
    });

    if (result.deletedCount > 0) {
      console.log(`✅ Cleanup: Deleted ${result.deletedCount} expired ride(s) (departure date passed)`);
    } else {
      console.log('✅ Cleanup: No expired rides to delete');
    }
  } catch (error) {
    console.error('❌ Error in deleteExpiredRides job:', error);
  }
};

/**
 * Initialize scheduled jobs
 */
export const initializeScheduledJobs = () => {
  // Run cleanup immediately on server start
  console.log('🕐 Running initial cleanup: deleteExpiredRides');
  deleteExpiredRides();
  
  // Run every 30 minutes: '*/30 * * * *'
  // Run every hour: '0 * * * *'
  // Run every 15 minutes: '*/15 * * * *'
  
  // Delete expired rides every 30 minutes
  cron.schedule('*/30 * * * *', async () => {
    console.log('🕐 Running scheduled job: deleteExpiredRides');
    await deleteExpiredRides();
  });

  console.log('✅ Scheduled jobs initialized');
  console.log('   - deleteExpiredRides: runs every 30 minutes');
};

