"use client";

import { Tabs, Tab } from "@heroui/react";
import { TrackCard, PlaylistCard } from "@/components/TrackCard";
import { MusicPlayer } from "@/components/MusicPlayer";
import { Sidebar } from "@/components/Sidebar";
import { Button } from "@heroui/react";
import { useAuth } from "@/components/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Spinner } from "@heroui/react";

const likedTracks = [
  { id: "1", title: "Starlight", artist: "The Midnight", coverUrl: "https://picsum.photos/seed/l1/300/300", duration: 240 },
  { id: "2", title: "Blinding Lights", artist: "The Weeknd", coverUrl: "https://picsum.photos/seed/l2/300/300", duration: 200 },
  { id: "3", title: "Midnight City", artist: "M83", coverUrl: "https://picsum.photos/seed/l3/300/300", duration: 243 },
  { id: "4", title: "Electric Feel", artist: "MGMT", coverUrl: "https://picsum.photos/seed/l4/300/300", duration: 229 },
  { id: "5", title: "Dreams", artist: "Fleetwood Mac", coverUrl: "https://picsum.photos/seed/l5/300/300", duration: 254 },
];

const recentPlayed = [
  { id: "1", title: "Today's Top Hits", description: "Jung Kook is on top!", coverUrl: "https://picsum.photos/seed/rp1/300/300", trackCount: 50 },
  { id: "2", title: "Chill Vibes", description: "Perfect for relaxation", coverUrl: "https://picsum.photos/seed/rp2/300/300", trackCount: 75 },
  { id: "3", title: "Workout Motivation", description: "High energy tracks", coverUrl: "https://picsum.photos/seed/rp3/300/300", trackCount: 60 },
  { id: "4", title: "Late Night Jazz", description: "Smooth jazz", coverUrl: "https://picsum.photos/seed/rp4/300/300", trackCount: 40 },
];

const userPlaylists = [
  { id: "1", name: "我喜欢的音乐", description: "私人的", coverUrl: "https://picsum.photos/seed/up1/300/300", trackCount: 128 },
  { id: "2", name: "入睡音乐", description: "助眠放松", coverUrl: "https://picsum.photos/seed/up2/300/300", trackCount: 45 },
  { id: "3", name: "工作专注", description: "提高效率", coverUrl: "https://picsum.photos/seed/up3/300/300", trackCount: 32 },
  { id: "4", name: "运动健身", description: "燃脂必备", coverUrl: "https://picsum.photos/seed/up4/300/300", trackCount: 56 },
];

export default function LibraryPage() {
  const { user, loading } = useAuth();
  const router = useRouter();

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
      <main className="flex-1 overflow-y-auto pb-24 bg-gradient-to-b from-primary-50 via-background to-background">
        <div className="sticky top-0 z-10 bg-background/80 backdrop-blur-lg border-b border-default-200 px-8 py-4">
          <h1 className="text-2xl font-bold">音乐库</h1>
        </div>

        <div className="px-8 py-6">
          <Tabs
            aria-label="Library tabs"
            color="primary"
          >
            <Tab key="playlists" title="播放列表">
              <div className="py-6 space-y-8">
                {/* 用户创建 */}
                <section>
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-xl font-bold">创建的歌单</h2>
                    <Button
                      isIconOnly
                      variant="light"
                      size="sm"
                    >
                      <PlusIcon className="w-5 h-5" />
                    </Button>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
                    {userPlaylists.map((playlist) => (
                      <PlaylistCard key={playlist.id} playlist={playlist} />
                    ))}
                  </div>
                </section>

                {/* 最近播放 */}
                <section>
                  <h2 className="text-xl font-bold mb-4">最近播放</h2>
                  <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
                    {recentPlayed.map((playlist) => (
                      <PlaylistCard key={playlist.id} playlist={playlist} />
                    ))}
                  </div>
                </section>
              </div>
            </Tab>

            <Tab key="liked" title="收藏">
              <div className="py-6">
                {/* 收藏封面 */}
                <div className="flex gap-6 mb-8 p-6 bg-gradient-to-br from-primary-100 to-secondary-100 rounded-xl shadow-sm">
                  <div className="w-48 h-48 bg-gradient-to-br from-primary to-secondary rounded-lg shadow-lg flex items-center justify-center">
                    <svg className="w-20 h-20 text-white" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                  </div>
                  <div className="flex flex-col justify-end">
                    <span className="text-sm font-medium text-foreground-500">歌单</span>
                    <h2 className="text-4xl font-bold mb-2">我喜欢的音乐</h2>
                    <p className="text-foreground-500">{likedTracks.length} 首歌曲</p>
                    <Button color="primary" className="font-bold mt-4 w-fit px-8">
                      播放全部
                    </Button>
                  </div>
                </div>

                {/* 收藏歌曲列表 */}
                <div className="space-y-1">
                  {likedTracks.map((track, index) => (
                    <div
                      key={track.id}
                      className="flex items-center gap-4 p-3 rounded-lg hover:bg-default-100 group cursor-pointer transition-colors"
                    >
                      <span className="w-6 text-center text-foreground-400 group-hover:hidden">
                        {index + 1}
                      </span>
                      <button className="hidden group-hover:block w-6 text-center">
                        <svg className="w-4 h-4 mx-auto text-foreground" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M8 5v14l11-7z" />
                        </svg>
                      </button>
                      <div className="w-12 h-12 rounded overflow-hidden flex-shrink-0 shadow-sm">
                        <img src={track.coverUrl} alt={track.title} className="w-full h-full object-cover" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium truncate">{track.title}</p>
                        <p className="text-sm text-foreground-500 truncate">{track.artist}</p>
                      </div>
                      <span className="text-sm text-foreground-400">
                        {Math.floor(track.duration / 60)}:{(track.duration % 60).toString().padStart(2, "0")}
                      </span>
                      <Button
                        isIconOnly
                        variant="light"
                        size="sm"
                        className="text-foreground-400 hover:text-danger opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                        </svg>
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            </Tab>

            <Tab key="albums" title="专辑">
              <div className="py-6">
                <p className="text-foreground-500">暂无收藏专辑</p>
              </div>
            </Tab>

            <Tab key="artists" title="关注">
              <div className="py-6">
                <p className="text-foreground-500">暂无关注歌手</p>
              </div>
            </Tab>
          </Tabs>
        </div>
      </main>
      <MusicPlayer />
    </div>
  );
}

function PlusIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
    </svg>
  );
}
