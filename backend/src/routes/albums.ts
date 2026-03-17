import { Router } from 'express';
import { listAlbums, getAlbum } from '../controllers/albums.js';

const router = Router();

router.get('/', listAlbums);
router.get('/:id', (req, res) => getAlbum(res, parseInt(req.params.id)));

export default router;
