import { Router } from 'express';
import { listSongs, getSong, scanLibrary } from '../controllers/songs.js';

const router = Router();

router.get('/', listSongs);
router.get('/:id', (req, res) => getSong(res, parseInt(req.params.id)));
router.post('/scan', scanLibrary);

export default router;
