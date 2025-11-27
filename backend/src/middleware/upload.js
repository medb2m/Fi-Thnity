import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Create uploads directories if they don't exist
const profilePicturesDir = path.join(__dirname, '../uploads/profile-pictures');
const communityPostsDir = path.join(__dirname, '../uploads/community-posts');
if (!fs.existsSync(profilePicturesDir)) {
  fs.mkdirSync(profilePicturesDir, { recursive: true });
}
if (!fs.existsSync(communityPostsDir)) {
  fs.mkdirSync(communityPostsDir, { recursive: true });
}

// Configure storage for profile pictures
const profileStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, profilePicturesDir);
  },
  filename: (req, file, cb) => {
    // Generate unique filename: userId-timestamp.extension
    const userId = req.user?._id?.toString() || 'unknown';
    const timestamp = Date.now();
    const ext = path.extname(file.originalname);
    const filename = `${userId}-${timestamp}${ext}`;
    cb(null, filename);
  }
});

// Configure storage for community posts
const communityPostStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    try {
      // Ensure directory exists
      if (!fs.existsSync(communityPostsDir)) {
        fs.mkdirSync(communityPostsDir, { recursive: true });
        console.log('Created community posts directory:', communityPostsDir);
      }
      cb(null, communityPostsDir);
    } catch (error) {
      console.error('Error setting destination:', error);
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    try {
      // Generate unique filename: userId-timestamp-random.extension
      const userId = req.user?._id?.toString() || 'unknown';
      const timestamp = Date.now();
      const random = Math.random().toString(36).substring(2, 8);
      const ext = path.extname(file.originalname) || '.jpg'; // Default to .jpg if no extension
      const filename = `${userId}-${timestamp}-${random}${ext}`;
      console.log('Saving community post image as:', filename);
      cb(null, filename);
    } catch (error) {
      console.error('Error generating filename:', error);
      // Don't fail the upload if filename generation fails, use a fallback
      const fallbackFilename = `post-${Date.now()}-${Math.random().toString(36).substring(2, 8)}.jpg`;
      console.log('Using fallback filename:', fallbackFilename);
      cb(null, fallbackFilename);
    }
  }
});

// File filter - only allow images
const fileFilter = (req, file, cb) => {
  try {
    const allowedMimes = [
      'image/jpeg', 
      'image/jpg', 
      'image/png', 
      'image/gif', 
      'image/webp',
      'image/pjpeg' // Some Android devices send this
    ];
    
    // Get file extension
    const ext = path.extname(file.originalname || '').toLowerCase();
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
    
    // Check MIME type (case-insensitive)
    const mimetype = (file.mimetype || '').toLowerCase();
    const isAllowedMime = allowedMimes.includes(mimetype) || mimetype.startsWith('image/');
    
    // Check file extension as fallback (for cases where MIME type might be missing or generic)
    const isAllowedExt = allowedExtensions.includes(ext);
    
    // Log for debugging
    console.log('📎 File upload attempt:', {
      originalname: file.originalname,
      mimetype: file.mimetype,
      extension: ext,
      isAllowedMime,
      isAllowedExt,
      fieldname: file.fieldname
    });
    
    // Accept if either MIME type or extension is valid
    // Also accept if mimetype starts with 'image/' (more lenient for Android)
    if (isAllowedMime || isAllowedExt) {
      console.log('✅ File accepted');
      cb(null, true);
    } else {
      const errorMsg = `Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed. Received: ${file.mimetype || 'unknown'}, extension: ${ext}`;
      console.log('❌ File rejected:', errorMsg);
      cb(new Error(errorMsg), false);
    }
  } catch (error) {
    console.error('❌ Error in fileFilter:', error);
    cb(error, false);
  }
};

// Configure multer for profile pictures
const upload = multer({
  storage: profileStorage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  }
});

// Configure multer for community posts
const uploadCommunityPost = multer({
  storage: communityPostStorage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB limit for posts
  }
});

export default upload;
export { uploadCommunityPost };

