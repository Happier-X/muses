import { usePlayerStore } from '../stores/player';
import { Play, Pause, SkipBack, SkipForward } from 'lucide-react';

export default function Player() {
  const { currentSong, isPlaying, pause, resume, next, prev } = usePlayerStore();

  if (!currentSong) return null;

  const audioUrl = `/api/songs/${currentSong.id}/stream`;

  return (
    <div className="fixed bottom-0 left-0 right-0 h-24 bg-slate-900 border-t flex items-center px-4 gap-4">
      <audio
        src={audioUrl}
        autoPlay={isPlaying}
        onEnded={next}
        onPause={pause}
        onPlay={resume}
      />

      <div className="flex-1">
        <div className="text-white font-medium">{currentSong.title}</div>
        <div className="text-slate-400 text-sm">{currentSong.artist.name}</div>
      </div>

      <div className="flex items-center gap-2">
        <button onClick={prev} className="text-white p-2">
          <SkipBack className="w-5 h-5" />
        </button>
        <button
          onClick={isPlaying ? pause : resume}
          className="text-white p-2"
        >
          {isPlaying ? <Pause className="w-6 h-6" /> : <Play className="w-6 h-6" />}
        </button>
        <button onClick={next} className="text-white p-2">
          <SkipForward className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
}
