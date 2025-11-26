import mongoose from 'mongoose';

const locationSchema = new mongoose.Schema({
  latitude: {
    type: Number,
    required: true
  },
  longitude: {
    type: Number,
    required: true
  },
  address: {
    type: String,
    default: ''
  }
}, { _id: false });

const communityPostSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true
  },
  firebaseUid: {
    type: String,
    required: true,
    index: true
  },
  content: {
    type: String,
    required: true,
    maxlength: 500
  },
  postType: {
    type: String,
    enum: ['ACCIDENT', 'DELAY', 'ROAD_CLOSURE', 'GENERAL'],
    default: 'GENERAL',
    index: true
  },
  location: locationSchema,
  imageUrl: {
    type: String,
    default: null
  },
  // Reddit-style voting system
  upvotes: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  downvotes: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  score: {
    type: Number,
    default: 0,
    index: true
  },
  // Keep likes for backward compatibility (deprecated, use score instead)
  likes: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  likesCount: {
    type: Number,
    default: 0
  },
  comments: [{
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true
    },
    content: {
      type: String,
      required: true,
      maxlength: 200
    },
    createdAt: {
      type: Date,
      default: Date.now
    }
  }],
  commentsCount: {
    type: Number,
    default: 0
  },
  isActive: {
    type: Boolean,
    default: true
  }
}, {
  timestamps: true,
  toJSON: { virtuals: true },
  toObject: { virtuals: true }
});

// Virtual for time ago
communityPostSchema.virtual('timeAgo').get(function() {
  const seconds = Math.floor((new Date() - this.createdAt) / 1000);

  let interval = seconds / 31536000;
  if (interval > 1) return Math.floor(interval) + ' years ago';

  interval = seconds / 2592000;
  if (interval > 1) return Math.floor(interval) + ' months ago';

  interval = seconds / 86400;
  if (interval > 1) return Math.floor(interval) + ' days ago';

  interval = seconds / 3600;
  if (interval > 1) return Math.floor(interval) + ' hours ago';

  interval = seconds / 60;
  if (interval > 1) return Math.floor(interval) + ' minutes ago';

  return Math.floor(seconds) + ' seconds ago';
});

// Update counts when arrays change
communityPostSchema.pre('save', function(next) {
  this.likesCount = this.likes?.length || 0;
  this.commentsCount = this.comments?.length || 0;
  // Calculate score: upvotes - downvotes
  const upvoteCount = this.upvotes?.length || 0;
  const downvoteCount = this.downvotes?.length || 0;
  this.score = upvoteCount - downvoteCount;
  next();
});

// Indexes for performance
communityPostSchema.index({ postType: 1, isActive: 1, createdAt: -1 });
communityPostSchema.index({ postType: 1, isActive: 1, score: -1 }); // For sorting by score
communityPostSchema.index({ 'location.latitude': 1, 'location.longitude': 1 });

const CommunityPost = mongoose.model('CommunityPost', communityPostSchema);

export default CommunityPost;
