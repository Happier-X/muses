import { Router } from 'express';
import { authMiddleware } from '../middleware/auth.js';
import { listFavorites, addFavorite, removeFavorite } from '../controllers/favorites.js';

const router = Router();

router.use(authMiddleware);
router.get('/', listFavorites);
router.post('/:songId', addFavorite);
router.delete('/:songId', removeFavorite);

export default router;
