"use client";

import { Input } from "@heroui/react";
import { TrackCard, PlaylistCard, ArtistCard } from "@/components/TrackCard";
import { MusicPlayer } from "@/components/MusicPlayer";
import { Sidebar } from "@/components/Sidebar";
import { useState, useEffect } from "react";
import { useAuth } from "@/components/AuthContext";
import { useRouter } from "next/navigation";
import { Spinner } from "@heroui/react";

const categories = ["全部", "歌曲", "专辑", "艺术家", "播放列表", "播客"];

const mockSearchResults = {
  tracks: [
    { id: "1", title: "Starlight", artist: "The Midnight", coverUrl: "https://picsum.photos/seed/s1/300/300", duration: 240 },
    { id: "2", title: "Blinding Lights", artist: "The Weeknd", coverUrl: "https://picsum.photos/seed/s2/300/300", duration: 200 },
    { id: "3", title: "Midnight City", artist: "M83", coverUrl: "https://picsum.photos/seed/s3/300/300", duration: 243 },
  ],
  playlists: [
    { id: "1", name: "Chill Vibes", description: "Perfect for relaxation", coverUrl: "https://picsum.photos/seed/sp1/300/300", trackCount: 75 },
    { id: "2", name: "Workout Mix", description: "High energy tracks", coverUrl: "https://picsum.photos/seed/sp2/300/300", trackCount: 45 },
  ],
  artists: [
    { id: "1", name: "The Weeknd", avatarUrl: "https://picsum.photos/seed/sa1/300/300", followers: "85.2M" },
    { id: "2", name: "Drake", avatarUrl: "https://picsum.photos/seed/sa2/300/300", followers: "78.1M" },
  ],
};

export default function SearchPage() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [query, setQuery] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner size="lg" color="primary" />
      </div>
    );
  }

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex-1 overflow-y-auto pb-24">
        <div className="sticky top-0 z-10 bg-background/80 backdrop-blur-lg border-b border-default-200 px-8 py-4">
          <Input
            placeholder="搜索歌曲、专辑、艺术家..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            classNames={{
              base: "max-w-2xl",
              inputWrapper: "bg-default-100 border-default-200 hover:bg-default-200",
              input: "placeholder:text-foreground-400",
            }}
            startContent={
              <svg className="w-5 h-5 text-foreground-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            }
          />
        </div>

        <div className="px-8 py-6 space-y-8">
          {/* 分类标签 */}
          <div className="flex gap-3 flex-wrap">
            {categories.map((cat) => (
              <button
                key={cat}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                  cat === "全部"
                    ? "bg-primary text-white"
                    : "bg-default-100 hover:bg-default-200 text-foreground-600"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* 热门搜索 */}
          {!query && (
            <section>
              <h2 className="text-xl font-bold mb-4">热门搜索</h2>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                {["周杰伦", "Taylor Swift", "健身音乐", "中文流行", "R&B", "爵士蓝调", "轻音乐", "电子舞曲"].map((term, i) => (
                  <button
                    key={term}
                    className="bg-default-100 hover:bg-default-200 rounded-xl p-4 text-left transition-all flex items-center gap-3 shadow-sm"
                  >
                    <span className={`font-bold ${i < 3 ? "text-primary" : "text-foreground-400"}`}>
                      {i + 1}
                    </span>
                    <span>{term}</span>
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* 搜索结果 */}
          {query && (
            <>
              {/* 歌曲结果 */}
              <section>
                <h2 className="text-xl font-bold mb-4">歌曲</h2>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6 gap-4">
                  {mockSearchResults.tracks.map((track) => (
                    <TrackCard key={track.id} track={track} />
                  ))}
                </div>
              </section>

              {/* 歌手结果 */}
              <section>
                <h2 className="text-xl font-bold mb-4">歌手</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
                  {mockSearchResults.artists.map((artist) => (
                    <ArtistCard key={artist.id} artist={artist} />
                  ))}
                </div>
              </section>

              {/* 播放列表结果 */}
              <section>
                <h2 className="text-xl font-bold mb-4">播放列表</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
                  {mockSearchResults.playlists.map((playlist) => (
                    <PlaylistCard key={playlist.id} playlist={playlist} />
                  ))}
                </div>
              </section>
            </>
          )}
        </div>
      </main>
      <MusicPlayer />
    </div>
  );
}
