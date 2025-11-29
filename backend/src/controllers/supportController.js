import SupportTicket from '../models/SupportTicket.js';
import User from '../models/User.js';

const GROQ_API = 'https://api.groq.com/openai/v1/chat/completions';
const GROQ_API_KEY = process.env.GROQ_API_KEY;

/**
 * System prompt for the support chatbot
 */
const SYSTEM_PROMPT = `You are a helpful support assistant for Fi Thnity, a carpooling app in Tunisia.

Your role is to:
1. Help users with common questions and issues
2. Identify if a user has a real issue that needs human support
3. Be friendly, professional, and concise

COMMON ISSUES YOU CAN HELP WITH:
- App navigation and features
- How to create/join rides
- Profile settings
- General questions about the app

ISSUES THAT NEED HUMAN SUPPORT:
- Account problems (login, verification, banned account)
- Payment or billing issues
- Technical bugs or app crashes
- Safety concerns or reports
- Disputes with other users
- Data privacy concerns
- Feature requests

When a user has an issue that needs human support, respond with:
"I understand your issue. Let me create a support ticket for you so our team can help you directly."

Then the system will create a ticket automatically.

Be concise and helpful. Respond in the same language the user uses.`;

/**
 * Chat with chatbot
 * POST /api/support/chat
 */
export const chatWithBot = async (req, res) => {
  try {
    const { messages, ticketId } = req.body;
    const userId = req.user?._id?.toString();

    if (!messages || messages.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Messages are required'
      });
    }

    const lastMessage = messages[messages.length - 1];
    if (!lastMessage || lastMessage.role !== 'user') {
      return res.status(400).json({
        success: false,
        message: 'Last message must be from user'
      });
    }

    if (!GROQ_API_KEY) {
      return res.status(500).json({
        success: false,
        message: 'Chatbot service not configured'
      });
    }

    // Prepare messages with system prompt
    const groqMessages = [
      { role: 'system', content: SYSTEM_PROMPT },
      ...messages.map(m => ({
        role: m.role,
        content: m.content
      }))
    ];

    // Call Groq API
    const response = await fetch(GROQ_API, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${GROQ_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: 'llama-3.1-8b-instant',
        messages: groqMessages,
        max_tokens: 300,
        temperature: 0.7,
        top_p: 0.9
      })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(`Groq API error: ${response.statusText}`);
    }

    const data = await response.json();
    if (!data.choices || !data.choices[0] || !data.choices[0].message) {
      throw new Error('Invalid response from Groq API');
    }

    const botResponse = data.choices[0].message.content;

    // Check if bot suggests creating a ticket
    const shouldCreateTicket = botResponse.toLowerCase().includes('support ticket') ||
                               botResponse.toLowerCase().includes('create a ticket') ||
                               botResponse.toLowerCase().includes('our team can help');

    // If there's a ticketId, update the ticket with the conversation
    if (ticketId && userId) {
      const ticket = await SupportTicket.findById(ticketId);
      if (ticket && ticket.user.toString() === userId) {
        ticket.chatbotConversation.push(
          { role: 'user', content: lastMessage.content },
          { role: 'assistant', content: botResponse }
        );
        await ticket.save();
      }
    }

    res.json({
      success: true,
      data: {
        message: botResponse,
        shouldCreateTicket,
        usage: data.usage
      }
    });
  } catch (error) {
    console.error('Chat API error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to generate response',
      details: error.message
    });
  }
};

/**
 * Create a support ticket
 * POST /api/support/tickets
 */
export const createTicket = async (req, res) => {
  try {
    const userId = req.user?._id?.toString();
    const { subject, description, category, chatbotConversation } = req.body;

    if (!userId) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required'
      });
    }

    if (!subject || !description) {
      return res.status(400).json({
        success: false,
        message: 'Subject and description are required'
      });
    }

    // Determine priority based on category
    let priority = 'medium';
    if (category === 'technical' || category === 'payment') {
      priority = 'high';
    }

    const ticket = new SupportTicket({
      user: userId,
      subject,
      description,
      category: category || 'other',
      priority,
      chatbotConversation: chatbotConversation || [],
      messages: [{
        sender: 'user',
        senderId: userId,
        content: description,
        isBot: false
      }]
    });

    await ticket.save();
    await ticket.populate('user', 'name email phoneNumber');

    res.status(201).json({
      success: true,
      data: ticket
    });
  } catch (error) {
    console.error('Create ticket error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to create ticket',
      details: error.message
    });
  }
};

/**
 * Get user's tickets
 * GET /api/support/tickets
 */
export const getUserTickets = async (req, res) => {
  try {
    const userId = req.user?._id?.toString();
    const { status, page = 1, limit = 20 } = req.query;

    if (!userId) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required'
      });
    }

    const tickets = await SupportTicket.getUserTickets(userId, {
      status,
      page: parseInt(page),
      limit: parseInt(limit)
    });

    res.json({
      success: true,
      data: {
        tickets,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: tickets.length
        }
      }
    });
  } catch (error) {
    console.error('Get tickets error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to get tickets',
      details: error.message
    });
  }
};

/**
 * Get ticket by ID
 * GET /api/support/tickets/:ticketId
 */
export const getTicketById = async (req, res) => {
  try {
    const userId = req.user?._id?.toString();
    const { ticketId } = req.params;

    const ticket = await SupportTicket.findById(ticketId)
      .populate('user', 'name email phoneNumber')
      .populate('assignedTo', 'name')
      .populate('resolvedBy', 'name');

    if (!ticket) {
      return res.status(404).json({
        success: false,
        message: 'Ticket not found'
      });
    }

    // Check if user owns the ticket or is admin
    const isAdmin = req.user?.role === 'admin';
    if (ticket.user._id.toString() !== userId && !isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    res.json({
      success: true,
      data: ticket
    });
  } catch (error) {
    console.error('Get ticket error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to get ticket',
      details: error.message
    });
  }
};

/**
 * Add message to ticket
 * POST /api/support/tickets/:ticketId/messages
 */
export const addMessageToTicket = async (req, res) => {
  try {
    const userId = req.user?._id?.toString();
    const { ticketId } = req.params;
    const { content } = req.body;

    if (!userId) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required'
      });
    }

    if (!content || content.trim().length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Message content is required'
      });
    }

    const ticket = await SupportTicket.findById(ticketId);
    if (!ticket) {
      return res.status(404).json({
        success: false,
        message: 'Ticket not found'
      });
    }

    // Check if user owns the ticket or is admin
    const isAdmin = req.user?.role === 'admin';
    if (ticket.user.toString() !== userId && !isAdmin) {
      return res.status(403).json({
        success: false,
        message: 'Access denied'
      });
    }

    // Determine sender type
    const sender = isAdmin ? 'admin' : 'user';
    
    // Update ticket status if it was resolved
    if (ticket.status === 'resolved' && sender === 'user') {
      ticket.status = 'open';
      ticket.resolvedAt = null;
      ticket.resolvedBy = null;
    } else if (ticket.status === 'open' && sender === 'admin') {
      ticket.status = 'in_progress';
      if (!ticket.assignedTo) {
        ticket.assignedTo = userId;
      }
    }

    await ticket.addMessage(sender, userId, content.trim(), false);
    await ticket.populate('user', 'name email phoneNumber');
    await ticket.populate('assignedTo', 'name');

    res.json({
      success: true,
      data: ticket
    });
  } catch (error) {
    console.error('Add message error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to add message',
      details: error.message
    });
  }
};

