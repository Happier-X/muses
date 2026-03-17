import { create } from 'zustand';

interface Song {
  id: number;
  title: string;
  duration: number;
  artist: { id: number; name: string };
  album: { id: number; title: string };
}

interface PlayerState {
  currentSong: Song | null;
  isPlaying: boolean;
  queue: Song[];
  currentIndex: number;
  play: (song: Song, queue?: Song[]) => void;
  pause: () => void;
  resume: () => void;
  next: () => void;
  prev: () => void;
}

export const usePlayerStore = create<PlayerState>((set, get) => ({
  currentSong: null,
  isPlaying: false,
  queue: [],
  currentIndex: 0,

  play: (song, queue) => {
    const newQueue = queue || [song];
    const index = queue ? newQueue.findIndex(s => s.id === song.id) : 0;
    set({ currentSong: song, isPlaying: true, queue: newQueue, currentIndex: index });
  },

  pause: () => set({ isPlaying: false }),
  resume: () => set({ isPlaying: true }),

  next: () => {
    const { queue, currentIndex } = get();
    if (currentIndex < queue.length - 1) {
      const nextIndex = currentIndex + 1;
      set({ currentSong: queue[nextIndex], currentIndex: nextIndex });
    }
  },

  prev: () => {
    const { queue, currentIndex } = get();
    if (currentIndex > 0) {
      const prevIndex = currentIndex - 1;
      set({ currentSong: queue[prevIndex], currentIndex: prevIndex });
    }
  }
}));
