import express from 'express';
import {
  chatWithBot,
  createTicket,
  getUserTickets,
  getTicketById,
  addMessageToTicket
} from '../controllers/supportController.js';
import { authenticate } from '../middleware/auth.js';

const router = express.Router();

// Chat with bot (requires authentication)
router.post('/chat', authenticate, chatWithBot);

// Ticket routes (require authentication)
router.post('/tickets', authenticate, createTicket);
router.get('/tickets', authenticate, getUserTickets);
router.get('/tickets/:ticketId', authenticate, getTicketById);
router.post('/tickets/:ticketId/messages', authenticate, addMessageToTicket);

export default router;

