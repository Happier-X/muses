import { Router } from 'express';
import { listSongs, getSong, scanLibrary } from '../controllers/songs.js';
import { streamSong } from '../services/stream.js';

const router = Router();

router.get('/', listSongs);
router.get('/:id', (req, res) => getSong(res, parseInt(req.params.id)));
router.get('/:id/stream', async (req, res) => {
  const id = parseInt(req.params.id);
  const acceptEncoding = req.headers['accept-encoding'] as string | undefined;
  const maxBitrate = req.headers['max-bitrate'] ? parseInt(req.headers['max-bitrate'] as string) : undefined;

  await streamSong(id, res, { acceptEncoding, maxBitrate });
});
router.post('/scan', scanLibrary);

export default router;
