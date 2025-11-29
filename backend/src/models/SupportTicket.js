import mongoose from 'mongoose';

const supportMessageSchema = new mongoose.Schema({
  sender: {
    type: String,
    enum: ['user', 'admin', 'bot'],
    required: true
  },
  senderId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: function() {
      return this.sender === 'user' || this.sender === 'admin';
    }
  },
  content: {
    type: String,
    required: true,
    trim: true,
    maxlength: 5000
  },
  isBot: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

const supportTicketSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true
  },
  subject: {
    type: String,
    required: true,
    trim: true,
    maxlength: 200
  },
  description: {
    type: String,
    required: true,
    trim: true,
    maxlength: 2000
  },
  status: {
    type: String,
    enum: ['open', 'in_progress', 'resolved', 'closed'],
    default: 'open',
    index: true
  },
  priority: {
    type: String,
    enum: ['low', 'medium', 'high', 'urgent'],
    default: 'medium'
  },
  category: {
    type: String,
    enum: ['technical', 'account', 'payment', 'ride_issue', 'other'],
    default: 'other'
  },
  messages: [supportMessageSchema],
  chatbotConversation: [{
    role: {
      type: String,
      enum: ['user', 'assistant', 'system'],
      required: true
    },
    content: {
      type: String,
      required: true
    }
  }],
  assignedTo: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    default: null
  },
  resolvedAt: {
    type: Date,
    default: null
  },
  resolvedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    default: null
  }
}, {
  timestamps: true
});

// Indexes for efficient queries
supportTicketSchema.index({ user: 1, createdAt: -1 });
supportTicketSchema.index({ status: 1, createdAt: -1 });
supportTicketSchema.index({ priority: 1, status: 1 });

// Method to add a message
supportTicketSchema.methods.addMessage = function(sender, senderId, content, isBot = false) {
  this.messages.push({
    sender,
    senderId: senderId || undefined,
    content,
    isBot
  });
  return this.save();
};

// Method to mark as resolved
supportTicketSchema.methods.markAsResolved = function(resolvedBy) {
  this.status = 'resolved';
  this.resolvedAt = new Date();
  this.resolvedBy = resolvedBy;
  return this.save();
};

// Static method to get tickets for a user
supportTicketSchema.statics.getUserTickets = async function(userId, options = {}) {
  const { status, page = 1, limit = 20 } = options;
  const skip = (page - 1) * limit;
  
  const query = { user: userId };
  if (status) {
    query.status = status;
  }
  
  return await this.find(query)
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit)
    .populate('user', 'name email phoneNumber')
    .populate('assignedTo', 'name')
    .populate('resolvedBy', 'name')
    .lean();
};

// Static method to get all tickets (admin)
supportTicketSchema.statics.getAllTickets = async function(options = {}) {
  const { status, priority, page = 1, limit = 50 } = options;
  const skip = (page - 1) * limit;
  
  const query = {};
  if (status) query.status = status;
  if (priority) query.priority = priority;
  
  return await this.find(query)
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit)
    .populate('user', 'name email phoneNumber')
    .populate('assignedTo', 'name')
    .populate('resolvedBy', 'name')
    .lean();
};

const SupportTicket = mongoose.model('SupportTicket', supportTicketSchema);

export default SupportTicket;

