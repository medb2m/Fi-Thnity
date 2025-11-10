import CommunityPost from '../models/CommunityPost.js';

/**
 * Create a new community post
 * POST /api/community/posts
 */
export const createPost = async (req, res) => {
  try {
    const { content, postType, location, imageUrl } = req.body;

    const post = await CommunityPost.create({
      user: req.user._id,
      firebaseUid: req.firebaseUser.uid,
      content,
      postType: postType || 'GENERAL',
      location,
      imageUrl: imageUrl || null
    });

    await post.populate('user', 'name photoUrl rating');

    res.status(201).json({
      success: true,
      message: 'Post created successfully',
      data: post
    });
  } catch (error) {
    console.error('Create post error:', error);
    res.status(500).json({
      success: false,
      message: 'Error creating post',
      error: error.message
    });
  }
};

/**
 * Get all community posts with filtering
 * GET /api/community/posts
 */
export const getPosts = async (req, res) => {
  try {
    const { postType, page = 1, limit = 20 } = req.query;
    const skip = (page - 1) * limit;

    const query = { isActive: true };
    if (postType) query.postType = postType;

    const posts = await CommunityPost.find(query)
      .populate('user', 'name photoUrl rating')
      .populate('comments.user', 'name photoUrl')
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(parseInt(limit));

    const total = await CommunityPost.countDocuments(query);

    res.json({
      success: true,
      data: posts,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('Get posts error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching posts',
      error: error.message
    });
  }
};

/**
 * Get post by ID
 * GET /api/community/posts/:postId
 */
export const getPostById = async (req, res) => {
  try {
    const post = await CommunityPost.findById(req.params.postId)
      .populate('user', 'name photoUrl rating')
      .populate('comments.user', 'name photoUrl');

    if (!post) {
      return res.status(404).json({
        success: false,
        message: 'Post not found'
      });
    }

    res.json({
      success: true,
      data: post
    });
  } catch (error) {
    console.error('Get post error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching post',
      error: error.message
    });
  }
};

/**
 * Like/Unlike a post
 * POST /api/community/posts/:postId/like
 */
export const toggleLike = async (req, res) => {
  try {
    const post = await CommunityPost.findById(req.params.postId);

    if (!post) {
      return res.status(404).json({
        success: false,
        message: 'Post not found'
      });
    }

    const userIndex = post.likes.indexOf(req.user._id);

    if (userIndex > -1) {
      // Unlike
      post.likes.splice(userIndex, 1);
    } else {
      // Like
      post.likes.push(req.user._id);
    }

    await post.save();

    res.json({
      success: true,
      message: userIndex > -1 ? 'Post unliked' : 'Post liked',
      data: {
        likesCount: post.likesCount,
        isLiked: userIndex === -1
      }
    });
  } catch (error) {
    console.error('Toggle like error:', error);
    res.status(500).json({
      success: false,
      message: 'Error toggling like',
      error: error.message
    });
  }
};

/**
 * Add a comment to a post
 * POST /api/community/posts/:postId/comments
 */
export const addComment = async (req, res) => {
  try {
    const { content } = req.body;

    const post = await CommunityPost.findById(req.params.postId);

    if (!post) {
      return res.status(404).json({
        success: false,
        message: 'Post not found'
      });
    }

    post.comments.push({
      user: req.user._id,
      content,
      createdAt: new Date()
    });

    await post.save();
    await post.populate('comments.user', 'name photoUrl');

    res.status(201).json({
      success: true,
      message: 'Comment added successfully',
      data: post.comments[post.comments.length - 1]
    });
  } catch (error) {
    console.error('Add comment error:', error);
    res.status(500).json({
      success: false,
      message: 'Error adding comment',
      error: error.message
    });
  }
};

/**
 * Delete a post
 * DELETE /api/community/posts/:postId
 */
export const deletePost = async (req, res) => {
  try {
    const post = await CommunityPost.findById(req.params.postId);

    if (!post) {
      return res.status(404).json({
        success: false,
        message: 'Post not found'
      });
    }

    // Check if user owns the post
    if (post.user.toString() !== req.user._id.toString()) {
      return res.status(403).json({
        success: false,
        message: 'Not authorized to delete this post'
      });
    }

    post.isActive = false;
    await post.save();

    res.json({
      success: true,
      message: 'Post deleted successfully'
    });
  } catch (error) {
    console.error('Delete post error:', error);
    res.status(500).json({
      success: false,
      message: 'Error deleting post',
      error: error.message
    });
  }
};

/**
 * Get user's posts
 * GET /api/community/my-posts
 */
export const getMyPosts = async (req, res) => {
  try {
    const posts = await CommunityPost.find({
      user: req.user._id,
      isActive: true
    })
      .sort({ createdAt: -1 })
      .populate('comments.user', 'name photoUrl');

    res.json({
      success: true,
      data: posts
    });
  } catch (error) {
    console.error('Get my posts error:', error);
    res.status(500).json({
      success: false,
      message: 'Error fetching your posts',
      error: error.message
    });
  }
};
