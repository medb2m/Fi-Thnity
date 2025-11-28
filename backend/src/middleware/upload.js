import multer from 'multer';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Create uploads directories if they don't exist
const profilePicturesDir = path.join(__dirname, '../uploads/profile-pictures');
const communityPostsDir = path.join(__dirname, '../uploads/community-posts');
const chatImagesDir = path.join(__dirname, '../uploads/chat-images');
const chatAudiosDir = path.join(__dirname, '../uploads/chat-audios');
if (!fs.existsSync(profilePicturesDir)) {
  fs.mkdirSync(profilePicturesDir, { recursive: true });
}
if (!fs.existsSync(communityPostsDir)) {
  fs.mkdirSync(communityPostsDir, { recursive: true });
}
if (!fs.existsSync(chatImagesDir)) {
  fs.mkdirSync(chatImagesDir, { recursive: true });
}
if (!fs.existsSync(chatAudiosDir)) {
  fs.mkdirSync(chatAudiosDir, { recursive: true });
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

// File filter for audio files
const audioFileFilter = (req, file, cb) => {
  try {
    const allowedMimes = [
      'audio/mpeg',
      'audio/mp3',
      'audio/wav',
      'audio/x-wav',
      'audio/aac',
      'audio/mp4',
      'audio/m4a',
      'audio/ogg',
      'audio/3gpp',
      'audio/amr'
    ];
    
    const ext = path.extname(file.originalname || '').toLowerCase();
    const allowedExtensions = ['.mp3', '.wav', '.aac', '.m4a', '.ogg', '.3gp', '.amr'];
    
    const mimetype = (file.mimetype || '').toLowerCase();
    const isAllowedMime = allowedMimes.includes(mimetype) || mimetype.startsWith('audio/');
    const isAllowedExt = allowedExtensions.includes(ext);
    
    console.log('🎤 Audio upload attempt:', {
      originalname: file.originalname,
      mimetype: file.mimetype,
      extension: ext,
      isAllowedMime,
      isAllowedExt,
      fieldname: file.fieldname
    });
    
    if (isAllowedMime || isAllowedExt) {
      console.log('✅ Audio file accepted');
      cb(null, true);
    } else {
      const errorMsg = `Invalid audio file type. Received: ${file.mimetype || 'unknown'}, extension: ${ext}`;
      console.log('❌ Audio file rejected:', errorMsg);
      cb(new Error(errorMsg), false);
    }
  } catch (error) {
    console.error('❌ Error in audioFileFilter:', error);
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

// Configure storage for chat images
const chatImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    try {
      if (!fs.existsSync(chatImagesDir)) {
        fs.mkdirSync(chatImagesDir, { recursive: true });
      }
      cb(null, chatImagesDir);
    } catch (error) {
      console.error('Error setting chat image destination:', error);
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    try {
      const userId = req.user?._id?.toString() || 'unknown';
      const timestamp = Date.now();
      const random = Math.random().toString(36).substring(2, 8);
      const ext = path.extname(file.originalname) || '.jpg';
      const filename = `${userId}-${timestamp}-${random}${ext}`;
      cb(null, filename);
    } catch (error) {
      console.error('Error generating chat image filename:', error);
      const fallbackFilename = `chat-${Date.now()}-${Math.random().toString(36).substring(2, 8)}.jpg`;
      cb(null, fallbackFilename);
    }
  }
});

// Configure multer for chat images
const uploadChatImage = multer({
  storage: chatImageStorage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB limit for chat images
  }
});

// Configure storage for chat audios
const chatAudioStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    try {
      if (!fs.existsSync(chatAudiosDir)) {
        fs.mkdirSync(chatAudiosDir, { recursive: true });
      }
      cb(null, chatAudiosDir);
    } catch (error) {
      console.error('Error setting chat audio destination:', error);
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    try {
      const userId = req.user?._id?.toString() || 'unknown';
      const timestamp = Date.now();
      const random = Math.random().toString(36).substring(2, 8);
      const ext = path.extname(file.originalname) || '.m4a';
      const filename = `${userId}-${timestamp}-${random}${ext}`;
      cb(null, filename);
    } catch (error) {
      console.error('Error generating chat audio filename:', error);
      const fallbackFilename = `audio-${Date.now()}-${Math.random().toString(36).substring(2, 8)}.m4a`;
      cb(null, fallbackFilename);
    }
  }
});

// Configure multer for chat audios
const uploadChatAudio = multer({
  storage: chatAudioStorage,
  fileFilter: audioFileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB limit for audio files
  }
});

export default upload;
export { uploadCommunityPost, uploadChatImage, uploadChatAudio };

