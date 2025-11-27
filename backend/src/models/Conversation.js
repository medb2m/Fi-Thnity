import mongoose from 'mongoose';

const conversationSchema = new mongoose.Schema({
  participants: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  }],
  lastMessage: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Message'
  },
  lastMessageTime: {
    type: Date,
    default: Date.now
  },
  unreadCount: {
    type: Map,
    of: Number,
    default: new Map()
  }
}, {
  timestamps: true
});

// Index for finding conversations by participants
conversationSchema.index({ participants: 1 });

// Virtual for the other participant (from current user's perspective)
conversationSchema.virtual('otherParticipant').get(function() {
  return this.participants.find(p => p._id.toString() !== this._currentUserId);
});

// Method to get conversation between two users
conversationSchema.statics.findBetweenUsers = async function(userId1, userId2) {
  return await this.findOne({
    participants: { $all: [userId1, userId2], $size: 2 }
  }).populate('participants', 'name photoUrl')
    .populate('lastMessage');
};

// Method to get all conversations for a user
conversationSchema.statics.findByUser = async function(userId, options = {}) {
  const { page = 1, limit = 20 } = options;
  const skip = (page - 1) * limit;

  return await this.find({
    participants: userId
  })
    .sort({ lastMessageTime: -1 })
    .skip(skip)
    .limit(limit)
    .populate('participants', 'name photoUrl')
    .populate('lastMessage');
};

const Conversation = mongoose.model('Conversation', conversationSchema);

export default Conversation;

