import { Router } from 'express';
import { listArtists, getArtist } from '../controllers/artists.js';

const router = Router();

router.get('/', listArtists);
router.get('/:id', (req, res) => getArtist(res, parseInt(req.params.id)));

export default router;
