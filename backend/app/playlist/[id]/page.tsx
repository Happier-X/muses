"use client";

import { Button } from "@heroui/react";
import { MusicPlayer } from "@/components/MusicPlayer";
import { Sidebar } from "@/components/Sidebar";
import { useState } from "react";
import Link from "next/link";

const mockPlaylist = {
  id: "1",
  name: "Today's Top Hits",
  description: "Jung Kook is on top of the Hottest 50!",
  owner: "Muses",
  coverUrl: "https://picsum.photos/seed/playlist1/600/600",
  trackCount: 50,
  playCount: "2.3B",
  createdAt: "2年前",
};

const tracks = [
  { id: "1", title: "Starlight", artist: "The Midnight", album: "Endless Summer", coverUrl: "https://picsum.photos/seed/t1/300/300", duration: 240, addedAt: "3周前" },
  { id: "2", title: "Blinding Lights", artist: "The Weeknd", album: "After Hours", coverUrl: "https://picsum.photos/seed/t2/300/300", duration: 200, addedAt: "1周前" },
  { id: "3", title: "Midnight City", artist: "M83", album: "Hurry Up, We're Dreaming", coverUrl: "https://picsum.photos/seed/t3/300/300", duration: 243, addedAt: "2周前" },
  { id: "4", title: "Electric Feel", artist: "MGMT", album: "Oracular Spectacular", coverUrl: "https://picsum.photos/seed/t4/300/300", duration: 229, addedAt: "5天前" },
  { id: "5", title: "Take On Me", artist: "a-ha", album: "Hunting High and Low", coverUrl: "https://picsum.photos/seed/t5/300/300", duration: 225, addedAt: "1月前" },
  { id: "6", title: "Dreams", artist: "Fleetwood Mac", album: "Rumours", coverUrl: "https://picsum.photos/seed/t6/300/300", duration: 254, addedAt: "2周前" },
  { id: "7", title: "Bohemian Rhapsody", artist: "Queen", album: "A Night at the Opera", coverUrl: "https://picsum.photos/seed/t7/300/300", duration: 354, addedAt: "3月前" },
  { id: "8", title: "Sweet Child O' Mine", artist: "Guns N' Roses", album: "Appetite for Destruction", coverUrl: "https://picsum.photos/seed/t8/300/300", duration: 356, addedAt: "2月前" },
];

export default function PlaylistPage() {
  const [currentTrackId, setCurrentTrackId] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex-1 overflow-y-auto pb-24">
        {/* Header */}
        <div className="bg-gradient-to-b from-primary-100 to-background">
          <div className="flex gap-6 p-8">
            <div className="w-56 h-56 rounded-xl shadow-2xl overflow-hidden flex-shrink-0">
              <img
                src={mockPlaylist.coverUrl}
                alt={mockPlaylist.name}
                className="w-full h-full object-cover"
              />
            </div>
            <div className="flex flex-col justify-end">
              <span className="text-sm font-medium text-foreground-500">歌单</span>
              <h1 className="text-5xl font-bold mb-4">{mockPlaylist.name}</h1>
              <p className="text-foreground-600 mb-2">{mockPlaylist.description}</p>
              <div className="flex items-center gap-2 text-sm text-foreground-500">
                <span>{mockPlaylist.owner}</span>
                <span>•</span>
                <span>{mockPlaylist.trackCount} 首歌曲</span>
                <span>•</span>
                <span>{mockPlaylist.playCount} 次播放</span>
                <span>•</span>
                <span>{mockPlaylist.createdAt}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="px-8 py-4 flex items-center gap-4">
          <Button
            color="primary"
            className="font-bold rounded-full w-14 h-14"
            isIconOnly
            onPress={() => setIsPlaying(!isPlaying)}
          >
            {isPlaying ? (
              <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" />
              </svg>
            ) : (
              <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z" />
              </svg>
            )}
          </Button>
          <Button
            variant="bordered"
            className="border-default-300"
            size="lg"
          >
            <svg className="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
            </svg>
            收藏
          </Button>
          <Button
            variant="light"
            size="lg"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
            </svg>
          </Button>
        </div>

        {/* Track List */}
        <div className="px-8">
          {/* Table Header */}
          <div className="grid grid-cols-12 gap-4 px-4 py-2 text-sm text-foreground-500 border-b border-default-200 sticky top-0 bg-background z-10">
            <span className="col-span-1">#</span>
            <span className="col-span-5">标题</span>
            <span className="col-span-4">专辑</span>
            <span className="col-span-2 text-right">时长</span>
          </div>

          {/* Tracks */}
          <div className="py-2">
            {tracks.map((track, index) => (
              <div
                key={track.id}
                className={`grid grid-cols-12 gap-4 px-4 py-3 rounded-lg cursor-pointer transition-colors group ${
                  currentTrackId === track.id
                    ? "bg-default-100"
                    : "hover:bg-default-50"
                }`}
                onClick={() => {
                  setCurrentTrackId(track.id);
                  setIsPlaying(true);
                }}
              >
                <span className="col-span-1 flex items-center text-foreground-400">
                  <span className="group-hover:hidden">{index + 1}</span>
                  <svg className="hidden group-hover:block w-4 h-4 text-foreground" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M8 5v14l11-7z" />
                  </svg>
                </span>
                <div className="col-span-5 flex items-center gap-3 min-w-0">
                  <div className="w-10 h-10 rounded overflow-hidden flex-shrink-0 shadow-sm">
                    <img src={track.coverUrl} alt={track.title} className="w-full h-full object-cover" />
                  </div>
                  <div className="min-w-0">
                    <p className={`font-medium truncate ${currentTrackId === track.id ? "text-primary" : ""}`}>
                      {track.title}
                    </p>
                    <Link href={`/artist/${track.artist}`} className="text-sm text-foreground-500 hover:underline">
                      {track.artist}
                    </Link>
                  </div>
                </div>
                <span className="col-span-4 flex items-center text-sm text-foreground-500 truncate">
                  {track.album}
                </span>
                <span className="col-span-2 flex items-center justify-end gap-4 text-sm text-foreground-400">
                  <Button
                    isIconOnly
                    variant="light"
                    size="sm"
                    className="text-foreground-400 hover:text-danger opacity-0 group-hover:opacity-100"
                  >
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                  </Button>
                  {formatDuration(track.duration)}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* More tracks placeholder */}
        <div className="px-8 py-8 text-foreground-400">
          <p>还有 {mockPlaylist.trackCount - tracks.length} 首歌曲...</p>
        </div>
      </main>
      <MusicPlayer
        track={
          currentTrackId
            ? {
                ...tracks.find((t) => t.id === currentTrackId)!,
                album: tracks.find((t) => t.id === currentTrackId)!.album,
              }
            : undefined
        }
      />
    </div>
  );
}
