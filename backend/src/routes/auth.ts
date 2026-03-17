import { Router, Response } from 'express';
import { register, login, getMe } from '../controllers/auth.js';
import { authMiddleware, AuthRequest } from '../middleware/auth.js';

const router = Router();

router.post('/register', register);
router.post('/login', login);
router.get('/me', authMiddleware, (req: AuthRequest, res: Response) => getMe(req, res));

export default router;
