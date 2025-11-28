import Conversation from '../models/Conversation.js';
import Message from '../models/Message.js';
import User from '../models/User.js';
import { createNotification } from './notificationController.js';

/**
 * Get all conversations for the authenticated user
 * GET /api/chat/conversations?page=1&limit=20
 */
export const getConversations = async (req, res) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const userId = req.user._id;

    const conversations = await Conversation.findByUser(userId, {
      page: parseInt(page),
      limit: parseInt(limit)
    });

    // Add unread count for current user and format response
    const formattedConversations = conversations.map(conv => {
      const otherParticipant = conv.participants.find(
        p => p._id.toString() !== userId.toString()
      );

      return {
        _id: conv._id,
        otherUser: {
          _id: otherParticipant._id,
          name: otherParticipant.name,
          photoUrl: otherParticipant.photoUrl
        },
        lastMessage: conv.lastMessage ? {
          _id: conv.lastMessage._id,
          content: conv.lastMessage.content,
          sender: conv.lastMessage.sender,
          createdAt: conv.lastMessage.createdAt,
          messageType: conv.lastMessage.messageType
        } : null,
        lastMessageTime: conv.lastMessageTime,
        unreadCount: conv.unreadCount?.get(userId.toString()) || 0,
        createdAt: conv.createdAt,
        updatedAt: conv.updatedAt
      };
    });

    res.json({
      success: true,
      data: formattedConversations,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total: await Conversation.countDocuments({ participants: userId })
      }
    });
  } catch (error) {
    console.error('Get conversations error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching conversations',
      error: error.message
    });
  }
};

/**
 * Get or create conversation with another user
 * POST /api/chat/conversations
 */
export const getOrCreateConversation = async (req, res) => {
  try {
    const { otherUserId } = req.body;
    const userId = req.user._id;

    if (!otherUserId) {
      return res.status(400).json({
        success: false,
        message: 'otherUserId is required'
      });
    }

    if (otherUserId === userId.toString()) {
      return res.status(400).json({
        success: false,
        message: 'Cannot create conversation with yourself'
      });
    }

    // Check if other user exists
    const otherUser = await User.findById(otherUserId);
    if (!otherUser) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Find existing conversation
    let conversation = await Conversation.findOne({
      participants: { $all: [userId, otherUserId] },
      $expr: { $eq: [{ $size: '$participants' }, 2] }
    }).populate('participants', 'name photoUrl');

    // Create new conversation if doesn't exist
    if (!conversation) {
      conversation = await Conversation.create({
        participants: [userId, otherUserId],
        unreadCount: new Map()
      });
      await conversation.populate('participants', 'name photoUrl');
    }

    // Format response
    const otherParticipant = conversation.participants.find(
      p => p._id.toString() !== userId.toString()
    );

    res.json({
      success: true,
      data: {
        _id: conversation._id,
        otherUser: {
          _id: otherParticipant._id,
          name: otherParticipant.name,
          photoUrl: otherParticipant.photoUrl
        },
        lastMessage: null,
        lastMessageTime: conversation.createdAt,
        unreadCount: conversation.unreadCount?.get(userId.toString()) || 0,
        createdAt: conversation.createdAt,
        updatedAt: conversation.updatedAt
      }
    });
  } catch (error) {
    console.error('Get or create conversation error:', error);
    res.status(500).json({
      success: false,
      message: 'Error creating conversation',
      error: error.message
    });
  }
};

/**
 * Get messages in a conversation
 * GET /api/chat/conversations/:conversationId/messages?page=1&limit=50
 */
export const getMessages = async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { page = 1, limit = 50 } = req.query;
    const userId = req.user._id;

    // Verify conversation exists and user is a participant
    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    const isParticipant = conversation.participants.some(
      p => p.toString() === userId.toString()
    );

    if (!isParticipant) {
      return res.status(403).json({
        success: false,
        message: 'You are not a participant in this conversation'
      });
    }

    // Get messages
    const skip = (parseInt(page) - 1) * parseInt(limit);
    const messages = await Message.find({ conversation: conversationId })
      .populate('sender', 'name photoUrl')
      .sort({ createdAt: -1 })
      .limit(parseInt(limit))
      .skip(skip)
      .lean();

    // Reverse to show oldest first
    messages.reverse();

    res.json({
      success: true,
      data: messages
    });
  } catch (error) {
    console.error('Get messages error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching messages',
      error: error.message
    });
  }
};

/**
 * Send a message
 * POST /api/chat/conversations/:conversationId/messages
 */
export const sendMessage = async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { content, messageType = 'TEXT', imageUrl, audioUrl, audioDuration, location } = req.body;
    const userId = req.user._id;

    // Validate content - allow empty for image and audio messages
    if (messageType === 'TEXT' && (!content || content.trim().length === 0)) {
      return res.status(400).json({
        success: false,
        message: 'Message content is required for text messages'
      });
    }

    // Validate image messages
    if (messageType === 'IMAGE' && !imageUrl) {
      return res.status(400).json({
        success: false,
        message: 'Image URL is required for image messages'
      });
    }

    // Validate audio messages
    if (messageType === 'AUDIO' && !audioUrl) {
      return res.status(400).json({
        success: false,
        message: 'Audio URL is required for audio messages'
      });
    }

    // Verify conversation exists and user is a participant
    const conversation = await Conversation.findById(conversationId).populate('participants', 'name photoUrl');
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    const isParticipant = conversation.participants.some(
      p => p._id.toString() === userId.toString()
    );

    if (!isParticipant) {
      return res.status(403).json({
        success: false,
        message: 'You are not a participant in this conversation'
      });
    }

    // Create message
    const message = await Message.create({
      conversation: conversationId,
      sender: userId,
      content: content?.trim() || '',
      messageType,
      imageUrl,
      audioUrl,
      audioDuration,
      location,
      status: 'SENT'
    });

    await message.populate('sender', 'name photoUrl');

    // Update conversation's last message
    conversation.lastMessage = message._id;
    conversation.lastMessageTime = message.createdAt;

    // Increment unread count for other participants
    conversation.participants.forEach(participant => {
      const participantId = participant._id.toString();
      if (participantId !== userId.toString()) {
        const currentCount = conversation.unreadCount?.get(participantId) || 0;
        conversation.unreadCount.set(participantId, currentCount + 1);
      }
    });

    await conversation.save();

    // Create notifications for other participants
    const sender = conversation.participants.find(p => p._id.toString() === userId.toString());
    const senderName = sender?.name || 'Someone';
    
    conversation.participants.forEach(async (participant) => {
      const participantId = participant._id.toString();
      if (participantId !== userId.toString()) {
        // Create notification for this participant
        let notificationMessage;
        if (messageType === 'IMAGE') {
          notificationMessage = `${senderName} sent a photo`;
        } else if (messageType === 'AUDIO') {
          notificationMessage = `${senderName} sent a voice message`;
        } else {
          notificationMessage = `${senderName}: ${content.trim().substring(0, 50)}${content.length > 50 ? '...' : ''}`;
        }
        
        await createNotification(
          participantId,
          'MESSAGE',
          'New message',
          notificationMessage,
          {
            conversationId: conversationId.toString(),
            messageId: message._id.toString(),
            senderId: userId.toString(),
            senderName: senderName,
            messageType: messageType
          }
        );
      }
    });

    // TODO: Emit socket event for real-time updates
    // io.to(conversationId).emit('new-message', message);

    res.status(201).json({
      success: true,
      message: 'Message sent successfully',
      data: message
    });
  } catch (error) {
    console.error('Send message error:', error);
    res.status(500).json({
      success: false,
      message: 'Error sending message',
      error: error.message
    });
  }
};

/**
 * Upload chat image
 * POST /api/chat/upload-image
 */
export const uploadChatImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No image file provided'
      });
    }

    const imageUrl = `/uploads/chat-images/${req.file.filename}`;

    res.json({
      success: true,
      message: 'Image uploaded successfully',
      data: {
        imageUrl: imageUrl
      }
    });
  } catch (error) {
    console.error('Upload chat image error:', error);
    res.status(500).json({
      success: false,
      message: 'Error uploading image',
      error: error.message
    });
  }
};

/**
 * Upload chat audio
 * POST /api/chat/upload-audio
 */
export const uploadChatAudio = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No audio file provided'
      });
    }

    const audioUrl = `/uploads/chat-audios/${req.file.filename}`;

    res.json({
      success: true,
      message: 'Audio uploaded successfully',
      data: {
        audioUrl: audioUrl
      }
    });
  } catch (error) {
    console.error('Upload chat audio error:', error);
    res.status(500).json({
      success: false,
      message: 'Error uploading audio',
      error: error.message
    });
  }
};

/**
 * Mark messages as read
 * PUT /api/chat/conversations/:conversationId/read
 */
export const markAsRead = async (req, res) => {
  try {
    const { conversationId } = req.params;
    const userId = req.user._id;

    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    const isParticipant = conversation.participants.some(
      p => p.toString() === userId.toString()
    );

    if (!isParticipant) {
      return res.status(403).json({
        success: false,
        message: 'You are not a participant in this conversation'
      });
    }

    // Reset unread count for this user
    conversation.unreadCount.set(userId.toString(), 0);
    await conversation.save();

    res.json({
      success: true,
      message: 'Messages marked as read'
    });
  } catch (error) {
    console.error('Mark as read error:', error);
    res.status(500).json({
      success: false,
      message: 'Error marking messages as read',
      error: error.message
    });
  }
};

/**
 * Delete conversation (soft delete)
 * DELETE /api/chat/conversations/:conversationId
 */
export const deleteConversation = async (req, res) => {
  try {
    const { conversationId } = req.params;
    const userId = req.user._id;

    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({
        success: false,
        message: 'Conversation not found'
      });
    }

    const isParticipant = conversation.participants.some(
      p => p.toString() === userId.toString()
    );

    if (!isParticipant) {
      return res.status(403).json({
        success: false,
        message: 'You are not a participant in this conversation'
      });
    }

    // Soft delete - remove user from participants
    conversation.participants = conversation.participants.filter(
      p => p.toString() !== userId.toString()
    );
    await conversation.save();

    res.json({
      success: true,
      message: 'Conversation deleted successfully'
    });
  } catch (error) {
    console.error('Delete conversation error:', error);
    res.status(500).json({
      success: false,
      message: 'Error deleting conversation',
      error: error.message
    });
  }
};

/**
 * Get users list (for starting new chats)
 * GET /api/chat/users?search=&page=1&limit=20
 */
export const getUsers = async (req, res) => {
  try {
    const { search = '', page = 1, limit = 20 } = req.query;
    const userId = req.user._id;

    const query = {
      _id: { $ne: userId } // Exclude current user
    };

    if (search && search.trim().length > 0) {
      query.$or = [
        { name: { $regex: search.trim(), $options: 'i' } },
        { email: { $regex: search.trim(), $options: 'i' } }
      ];
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    const users = await User.find(query)
      .select('name email photoUrl')
      .limit(parseInt(limit))
      .skip(skip)
      .lean();

    res.json({
      success: true,
      data: users,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total: await User.countDocuments(query)
      }
    });
  } catch (error) {
    console.error('Get users error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching users',
      error: error.message
    });
  }
};

/**
 * Get unread conversation count (number of conversations with unread messages)
 * GET /api/chat/conversations/unread-count
 */
export const getUnreadConversationCount = async (req, res) => {
  try {
    const userId = req.user._id;

    // Find all conversations where the user has unread messages
    const conversations = await Conversation.find({
      participants: userId
    });

    // Count conversations where unreadCount > 0 for this user
    let unreadConversationCount = 0;
    for (const conv of conversations) {
      const unreadCount = conv.unreadCount?.get(userId.toString()) || 0;
      if (unreadCount > 0) {
        unreadConversationCount++;
      }
    }

    res.json({
      success: true,
      data: {
        unreadConversationCount
      }
    });
  } catch (error) {
    console.error('Get unread conversation count error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching unread conversation count',
      error: error.message
    });
  }
};
