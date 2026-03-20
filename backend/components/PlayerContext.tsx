"use client";

import { createContext, useContext, useState, useCallback, ReactNode } from "react";

interface Track {
  id: string;
  title: string;
  artistName: string;
  albumName: string;
  duration: number;
  coverUrl?: string;
  audioUrl: string;
}

interface PlayerContextType {
  currentTrack: Track | null;
  isPlaying: boolean;
  progress: number;
  volume: number;
  playlist: Track[];
  currentIndex: number;
  play: (track: Track, playlist?: Track[]) => void;
  pause: () => void;
  resume: () => void;
  toggle: () => void;
  next: () => void;
  prev: () => void;
  setProgress: (progress: number) => void;
  setVolume: (volume: number) => void;
}

const PlayerContext = createContext<PlayerContextType | null>(null);

export function PlayerProvider({ children }: { children: ReactNode }) {
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [volume, setVolume] = useState(75);
  const [playlist, setPlaylist] = useState<Track[]>([]);
  const [currentIndex, setCurrentIndex] = useState(-1);

  const play = useCallback((track: Track, newPlaylist?: Track[]) => {
    setCurrentTrack(track);
    setIsPlaying(true);
    setProgress(0);
    if (newPlaylist) {
      setPlaylist(newPlaylist);
      const index = newPlaylist.findIndex((t) => t.id === track.id);
      setCurrentIndex(index);
    }
  }, []);

  const pause = useCallback(() => setIsPlaying(false), []);
  const resume = useCallback(() => setIsPlaying(true), []);

  const toggle = useCallback(() => {
    setIsPlaying((prev) => !prev);
  }, []);

  const next = useCallback(() => {
    if (playlist.length > 0 && currentIndex < playlist.length - 1) {
      const nextTrack = playlist[currentIndex + 1];
      setCurrentTrack(nextTrack);
      setCurrentIndex((prev) => prev + 1);
      setProgress(0);
    }
  }, [playlist, currentIndex]);

  const prev = useCallback(() => {
    if (playlist.length > 0 && currentIndex > 0) {
      const prevTrack = playlist[currentIndex - 1];
      setCurrentTrack(prevTrack);
      setCurrentIndex((prev) => prev - 1);
      setProgress(0);
    }
  }, [playlist, currentIndex]);

  return (
    <PlayerContext.Provider
      value={{
        currentTrack,
        isPlaying,
        progress,
        volume,
        playlist,
        currentIndex,
        play,
        pause,
        resume,
        toggle,
        next,
        prev,
        setProgress,
        setVolume,
      }}
    >
      {children}
    </PlayerContext.Provider>
  );
}

export function usePlayer() {
  const context = useContext(PlayerContext);
  if (!context) {
    throw new Error("usePlayer must be used within PlayerProvider");
  }
  return context;
}
