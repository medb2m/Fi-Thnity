import Friend from '../models/Friend.js';
import User from '../models/User.js';
import { createNotification } from './notificationController.js';

/**
 * Send a friend request
 * POST /api/friends/request
 */
export const sendFriendRequest = async (req, res) => {
  try {
    const { recipientId } = req.body;
    const requesterId = req.user._id;

    if (requesterId.toString() === recipientId) {
      return res.status(400).json({
        success: false,
        message: 'You cannot send a friend request to yourself'
      });
    }

    // Check if recipient exists
    const recipient = await User.findById(recipientId);
    if (!recipient) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }

    // Check if friend request already exists
    const existingRequest = await Friend.findOne({
      $or: [
        { requester: requesterId, recipient: recipientId },
        { requester: recipientId, recipient: requesterId }
      ]
    });

    if (existingRequest) {
      if (existingRequest.status === 'PENDING') {
        return res.status(400).json({
          success: false,
          message: 'Friend request already pending'
        });
      } else if (existingRequest.status === 'ACCEPTED') {
        return res.status(400).json({
          success: false,
          message: 'You are already friends'
        });
      }
    }

    // Create new friend request
    const friendRequest = new Friend({
      requester: requesterId,
      recipient: recipientId,
      status: 'PENDING'
    });

    await friendRequest.save();

    // Create notification for recipient
    const requesterName = req.user.name || 'Someone';
    await createNotification(
      recipientId,
      'FRIEND_REQUEST',
      'New friend request',
      `${requesterName} wants to be your friend`,
      {
        requesterId: requesterId.toString(),
        requesterName: requesterName,
        friendRequestId: friendRequest._id.toString()
      }
    );

    res.status(201).json({
      success: true,
      message: 'Friend request sent successfully',
      data: friendRequest
    });
  } catch (error) {
    console.error('Send friend request error:', error);
    res.status(500).json({
      success: false,
      message: 'Error sending friend request',
      error: error.message
    });
  }
};

/**
 * Accept a friend request
 * PUT /api/friends/accept/:requestId
 */
export const acceptFriendRequest = async (req, res) => {
  try {
    const { requestId } = req.params;
    const userId = req.user._id;

    const friendRequest = await Friend.findById(requestId);

    if (!friendRequest) {
      return res.status(404).json({
        success: false,
        message: 'Friend request not found'
      });
    }

    // Check if user is the recipient
    if (friendRequest.recipient.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'You can only accept friend requests sent to you'
      });
    }

    // Check if already accepted
    if (friendRequest.status === 'ACCEPTED') {
      return res.status(400).json({
        success: false,
        message: 'Friend request already accepted'
      });
    }

    // Update status
    friendRequest.status = 'ACCEPTED';
    friendRequest.acceptedAt = new Date();
    await friendRequest.save();

    // Create notification for requester
    const recipientName = req.user.name || 'Someone';
    await createNotification(
      friendRequest.requester,
      'FRIEND_REQUEST_ACCEPTED',
      'Friend request accepted',
      `${recipientName} accepted your friend request`,
      {
        recipientId: userId.toString(),
        recipientName: recipientName,
        friendRequestId: friendRequest._id.toString()
      }
    );

    // Populate user info
    await friendRequest.populate('requester', 'name photoUrl');
    await friendRequest.populate('recipient', 'name photoUrl');

    res.json({
      success: true,
      message: 'Friend request accepted',
      data: friendRequest
    });
  } catch (error) {
    console.error('Accept friend request error:', error);
    res.status(500).json({
      success: false,
      message: 'Error accepting friend request',
      error: error.message
    });
  }
};

/**
 * Reject a friend request
 * PUT /api/friends/reject/:requestId
 */
export const rejectFriendRequest = async (req, res) => {
  try {
    const { requestId } = req.params;
    const userId = req.user._id;

    const friendRequest = await Friend.findById(requestId);

    if (!friendRequest) {
      return res.status(404).json({
        success: false,
        message: 'Friend request not found'
      });
    }

    // Check if user is the recipient
    if (friendRequest.recipient.toString() !== userId.toString()) {
      return res.status(403).json({
        success: false,
        message: 'You can only reject friend requests sent to you'
      });
    }

    // Update status
    friendRequest.status = 'REJECTED';
    await friendRequest.save();

    res.json({
      success: true,
      message: 'Friend request rejected',
      data: friendRequest
    });
  } catch (error) {
    console.error('Reject friend request error:', error);
    res.status(500).json({
      success: false,
      message: 'Error rejecting friend request',
      error: error.message
    });
  }
};

/**
 * Get all friends
 * GET /api/friends
 */
export const getFriends = async (req, res) => {
  try {
    const userId = req.user._id;

    const friends = await Friend.find({
      $or: [
        { requester: userId, status: 'ACCEPTED' },
        { recipient: userId, status: 'ACCEPTED' }
      ]
    })
      .populate('requester', 'name photoUrl')
      .populate('recipient', 'name photoUrl')
      .sort({ acceptedAt: -1 });

    // Map to return friend user info (not the requester/recipient structure)
    const friendsList = friends.map(friend => {
      const friendUser = friend.requester._id.toString() === userId.toString()
        ? friend.recipient
        : friend.requester;
      
      return {
        _id: friend._id,
        user: friendUser,
        acceptedAt: friend.acceptedAt,
        createdAt: friend.createdAt
      };
    });

    res.json({
      success: true,
      data: friendsList
    });
  } catch (error) {
    console.error('Get friends error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching friends',
      error: error.message
    });
  }
};

/**
 * Get pending friend requests (invitations)
 * GET /api/friends/invitations
 */
export const getInvitations = async (req, res) => {
  try {
    const userId = req.user._id;

    const invitations = await Friend.find({
      recipient: userId,
      status: 'PENDING'
    })
      .populate('requester', 'name photoUrl')
      .sort({ createdAt: -1 });

    const invitationsList = invitations.map(invitation => ({
      _id: invitation._id,
      user: invitation.requester,
      createdAt: invitation.createdAt
    }));

    res.json({
      success: true,
      data: invitationsList
    });
  } catch (error) {
    console.error('Get invitations error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching invitations',
      error: error.message
    });
  }
};

/**
 * Get friend status between two users
 * GET /api/friends/status/:userId
 */
export const getFriendStatus = async (req, res) => {
  try {
    const { userId } = req.params;
    const currentUserId = req.user._id;

    if (currentUserId.toString() === userId) {
      return res.json({
        success: true,
        data: { status: 'SELF' }
      });
    }

    const friendRequest = await Friend.findOne({
      $or: [
        { requester: currentUserId, recipient: userId },
        { requester: userId, recipient: currentUserId }
      ]
    });

    if (!friendRequest) {
      return res.json({
        success: true,
        data: { status: 'NONE' }
      });
    }

    res.json({
      success: true,
      data: {
        status: friendRequest.status,
        isRequester: friendRequest.requester.toString() === currentUserId.toString(),
        requestId: friendRequest._id.toString()
      }
    });
  } catch (error) {
    console.error('Get friend status error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching friend status',
      error: error.message
    });
  }
};

