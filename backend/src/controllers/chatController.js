import Conversation from '../models/Conversation.js';
import Message from '../models/Message.js';
import User from '../models/User.js';
import { createNotification } from './notificationController.js';

/**
 * Get all conversations for the authenticated user
 * GET /api/chat/conversations
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
        total: formattedConversations.length
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
 * Get or create a conversation with another user
 * POST /api/chat/conversations
 */
export const getOrCreateConversation = async (req, res) => {
  try {
    const { otherUserId } = req.body;
    const currentUserId = req.user._id;

    if (!otherUserId) {
      return res.status(400).json({
        success: false,
        message: 'Other user ID is required'
      });
    }

    if (otherUserId === currentUserId.toString()) {
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

    // Try to find existing conversation
    let conversation = await Conversation.findBetweenUsers(currentUserId, otherUserId);

    // Create new conversation if it doesn't exist
    if (!conversation) {
      conversation = await Conversation.create({
        participants: [currentUserId, otherUserId],
        unreadCount: new Map([
          [currentUserId.toString(), 0],
          [otherUserId, 0]
        ])
      });

      await conversation.populate('participants', 'name photoUrl');
    }

    // Format response
    const otherParticipant = conversation.participants.find(
      p => p._id.toString() !== currentUserId.toString()
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
        lastMessage: conversation.lastMessage,
        lastMessageTime: conversation.lastMessageTime,
        unreadCount: conversation.unreadCount?.get(currentUserId.toString()) || 0,
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
 * GET /api/chat/conversations/:conversationId/messages
 */
export const getMessages = async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { page = 1, limit = 50 } = req.query;
    const userId = req.user._id;

    // Verify user is part of this conversation
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
    const messages = await Message.findByConversation(conversationId, {
      page: parseInt(page),
      limit: parseInt(limit),
      userId
    });

    // Reverse to show oldest first
    messages.reverse();

    res.json({
      success: true,
      data: messages,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total: messages.length
      }
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
    const { content, messageType = 'TEXT', imageUrl, location } = req.body;
    const userId = req.user._id;

    // Validate content
    if (!content || content.trim().length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Message content is required'
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
      content: content.trim(),
      messageType,
      imageUrl,
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
        await createNotification(
          participantId,
          'MESSAGE',
          'New message',
          `${senderName}: ${content.trim().substring(0, 50)}${content.length > 50 ? '...' : ''}`,
          {
            conversationId: conversationId.toString(),
            messageId: message._id.toString(),
            senderId: userId.toString(),
            senderName: senderName
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
 * Mark messages as read
 * PUT /api/chat/conversations/:conversationId/read
 */
export const markAsRead = async (req, res) => {
  try {
    const { conversationId } = req.params;
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

    // Mark all unread messages as read
    await Message.markAsRead(conversationId, userId);

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
 * Get list of users (for starting new conversations)
 * GET /api/chat/users
 */
export const getUsers = async (req, res) => {
  try {
    const { search = '', page = 1, limit = 20 } = req.query;
    const currentUserId = req.user._id;

    const query = {
      _id: { $ne: currentUserId } // Exclude current user
    };

    if (search) {
      query.name = { $regex: search, $options: 'i' };
    }

    const users = await User.find(query)
      .select('name photoUrl')
      .sort({ name: 1 })
      .skip((parseInt(page) - 1) * parseInt(limit))
      .limit(parseInt(limit));

    const total = await User.countDocuments(query);

    res.json({
      success: true,
      data: users,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / parseInt(limit))
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
 * Delete a conversation
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

    // Mark all messages in this conversation as deleted for this user
    await Message.updateMany(
      { conversation: conversationId },
      { $addToSet: { deletedFor: userId } }
    );

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

