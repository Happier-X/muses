"use client";

import { Avatar } from "@heroui/react";

interface TrackCardProps {
  track: {
    id: string;
    title: string;
    artist: string;
    coverUrl: string;
    duration: number;
  };
  onPlay?: (id: string) => void;
  isPlaying?: boolean;
}

export function TrackCard({ track, onPlay, isPlaying }: TrackCardProps) {
  return (
    <div
      className="group bg-default-100 rounded-xl p-4 hover:bg-default-200 transition-all duration-300 cursor-pointer shadow-sm"
      onClick={() => onPlay?.(track.id)}
    >
      <div className="relative mb-3">
        <div className="aspect-square rounded-lg overflow-hidden bg-default-200">
          <img
            src={track.coverUrl}
            alt={track.title}
            className="w-full h-full object-cover"
          />
        </div>
        <div
          className={`absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity ${
            isPlaying ? "opacity-100 bg-black/30" : ""
          }`}
        >
          <button className="w-12 h-12 rounded-full bg-primary flex items-center justify-center hover:scale-110 transition-transform shadow-lg text-white">
            <svg
              className={`w-6 h-6 ${isPlaying ? "hidden" : ""}`}
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path d="M8 5v14l11-7z" />
            </svg>
            {isPlaying && (
              <div className="flex gap-0.5">
                <span className="w-1 h-4 bg-white animate-pulse" />
                <span className="w-1 h-4 bg-white animate-pulse delay-75" />
                <span className="w-1 h-4 bg-white animate-pulse delay-150" />
              </div>
            )}
          </button>
        </div>
      </div>
      <h3 className="font-medium truncate mb-1">{track.title}</h3>
      <p className="text-sm text-foreground-500 truncate">{track.artist}</p>
    </div>
  );
}

interface PlaylistCardProps {
  playlist: {
    id: string;
    name: string;
    description: string;
    coverUrl: string;
    trackCount: number;
  };
  onClick?: (id: string) => void;
}

export function PlaylistCard({ playlist, onClick }: PlaylistCardProps) {
  return (
    <div
      className="group bg-default-100 rounded-xl p-4 hover:bg-default-200 transition-all duration-300 cursor-pointer shadow-sm"
      onClick={() => onClick?.(playlist.id)}
    >
      <div className="relative mb-3">
        <div className="aspect-square rounded-lg overflow-hidden bg-default-200 shadow-md">
          <img
            src={playlist.coverUrl}
            alt={playlist.name}
            className="w-full h-full object-cover"
          />
        </div>
        <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <button className="w-12 h-12 rounded-full bg-primary flex items-center justify-center hover:scale-110 transition-transform shadow-lg text-white">
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          </button>
        </div>
      </div>
      <h3 className="font-medium truncate mb-1">{playlist.name}</h3>
      <p className="text-sm text-foreground-500 truncate mb-1">
        {playlist.description}
      </p>
      <p className="text-xs text-foreground-400">{playlist.trackCount} 首歌曲</p>
    </div>
  );
}

interface ArtistCardProps {
  artist: {
    id: string;
    name: string;
    avatarUrl: string;
    followers: string;
  };
  onClick?: (id: string) => void;
}

export function ArtistCard({ artist, onClick }: ArtistCardProps) {
  return (
    <div
      className="group bg-default-100 rounded-xl p-4 hover:bg-default-200 transition-all duration-300 cursor-pointer text-center shadow-sm"
      onClick={() => onClick?.(artist.id)}
    >
      <div className="w-full aspect-square mb-3">
        <Avatar
          src={artist.avatarUrl}
          alt={artist.name}
          className="w-full h-full object-cover rounded-full group-hover:ring-4 group-hover:ring-primary/30 transition-all"
        />
      </div>
      <h3 className="font-medium truncate mb-1">{artist.name}</h3>
      <p className="text-sm text-foreground-500">{artist.followers} 粉丝</p>
    </div>
  );
}
