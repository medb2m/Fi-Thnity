import CommunityPost from '../models/CommunityPost.js';

/**
 * Create a new community post
 * POST /api/community/posts
 */
export const createPost = async (req, res) => {
  try {
    const { content, postType, location } = req.body;
    
    // Get image URL from uploaded file or from body
    let imageUrl = null;
    if (req.file) {
      // Image was uploaded via multer
      imageUrl = `/uploads/community-posts/${req.file.filename}`;
    } else if (req.body.imageUrl) {
      // Image URL provided directly (for external URLs)
      imageUrl = req.body.imageUrl;
    }

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
 * GET /api/community/posts?postType=GENERAL&sort=score&page=1&limit=20
 */
export const getPosts = async (req, res) => {
  try {
    const { postType, page = 1, limit = 20, sort = 'score' } = req.query;
    const skip = (page - 1) * limit;

    const query = { isActive: true };
    if (postType) query.postType = postType;

    // Determine sort order
    let sortOrder = {};
    if (sort === 'score') {
      sortOrder = { score: -1, createdAt: -1 }; // Sort by score (best posts first), then by date
    } else if (sort === 'new') {
      sortOrder = { createdAt: -1 }; // Sort by newest first
    } else {
      sortOrder = { score: -1, createdAt: -1 }; // Default: best posts first
    }

    const posts = await CommunityPost.find(query)
      .populate('user', 'name photoUrl rating')
      .populate('comments.user', 'name photoUrl')
      .sort(sortOrder)
      .skip(skip)
      .limit(parseInt(limit));

    // Add user's vote status if authenticated
    const userId = req.user?._id;
    const postsWithVoteStatus = posts.map(post => {
      const postObj = post.toObject();
      if (userId) {
        postObj.userVote = post.upvotes.includes(userId) ? 'up' : 
                          post.downvotes.includes(userId) ? 'down' : null;
      } else {
        postObj.userVote = null;
      }
      return postObj;
    });

    const total = await CommunityPost.countDocuments(query);

    res.json({
      success: true,
      data: postsWithVoteStatus,
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
 * Upvote/Downvote a post (Reddit-style)
 * POST /api/community/posts/:postId/vote
 * Body: { vote: 'up' | 'down' | null }
 */
export const votePost = async (req, res) => {
  try {
    const { vote } = req.body; // 'up', 'down', or null to remove vote
    const post = await CommunityPost.findById(req.params.postId);

    if (!post) {
      return res.status(404).json({
        success: false,
        message: 'Post not found'
      });
    }

    const userId = req.user._id;
    const upvoteIndex = post.upvotes.indexOf(userId);
    const downvoteIndex = post.downvotes.indexOf(userId);

    // Remove existing votes
    if (upvoteIndex > -1) {
      post.upvotes.splice(upvoteIndex, 1);
    }
    if (downvoteIndex > -1) {
      post.downvotes.splice(downvoteIndex, 1);
    }

    // Add new vote if specified
    if (vote === 'up') {
      post.upvotes.push(userId);
    } else if (vote === 'down') {
      post.downvotes.push(userId);
    }
    // If vote is null, we've already removed votes above

    await post.save();

    res.json({
      success: true,
      message: vote === 'up' ? 'Post upvoted' : vote === 'down' ? 'Post downvoted' : 'Vote removed',
      data: {
        score: post.score,
        upvotes: post.upvotes.length,
        downvotes: post.downvotes.length,
        userVote: vote
      }
    });
  } catch (error) {
    console.error('Vote post error:', error);
    res.status(500).json({
      success: false,
      message: 'Error voting on post',
      error: error.message
    });
  }
};

/**
 * Like/Unlike a post (deprecated - use votePost instead)
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
