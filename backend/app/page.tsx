"use client";

import { TrackCard, PlaylistCard, ArtistCard } from "@/components/TrackCard";
import { MusicPlayer } from "@/components/MusicPlayer";
import { Sidebar } from "@/components/Sidebar";
import { Avatar, Button, Spinner } from "@heroui/react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { usePlayer } from "@/components/PlayerContext";
import { useAuth } from "@/components/AuthContext";
import { useRouter } from "next/navigation";

interface Track {
  id: string;
  title: string;
  artistName: string;
  albumName: string;
  duration: number;
  coverUrl?: string;
  audioUrl: string;
}

interface Artist {
  id: string;
  name: string;
  avatar?: string;
}

interface Album {
  id: string;
  title: string;
  cover?: string;
  artist?: Artist;
}

export default function HomePage() {
  const { play } = usePlayer();
  const { user, loading } = useAuth();
  const router = useRouter();
  const [tracks, setTracks] = useState<Track[]>([]);
  const [artists, setArtists] = useState<Artist[]>([]);
  const [albums, setAlbums] = useState<Album[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  // 未登录重定向到登录页
  useEffect(() => {
    if (!loading && !user) {
      router.push("/login");
    }
  }, [user, loading, router]);

  useEffect(() => {
    async function fetchData() {
      if (!user) return;

      try {
        const [tracksRes, artistsRes, albumsRes] = await Promise.all([
          fetch("/api/music?limit=12"),
          fetch("/api/artists?limit=6"),
          fetch("/api/albums?limit=6"),
        ]);

        const tracksData = await tracksRes.json();
        const artistsData = await artistsRes.json();
        const albumsData = await albumsRes.json();

        setTracks(tracksData.tracks || []);
        setArtists(artistsData.artists || []);
        setAlbums(albumsData.albums || []);
      } catch (error) {
        console.error("获取数据失败:", error);
      } finally {
        setDataLoading(false);
      }
    }

    fetchData();
  }, [user]);

  const handlePlayTrack = (track: Track) => {
    play(track, tracks);
  };

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
      <main className="flex-1 overflow-y-auto pb-24 bg-gradient-to-b from-primary-100 via-background to-background">
        {/* Header */}
        <header className="sticky top-0 z-10 bg-background/80 backdrop-blur-lg border-b border-default-200 px-8 py-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold">欢迎回来</h1>
            <div className="flex items-center gap-4">
              <Avatar
                src="https://picsum.photos/seed/user/100/100"
                size="sm"
                className="cursor-pointer"
              />
            </div>
          </div>
        </header>

        <div className="px-8 py-6 space-y-10">
          {/* 歌曲 */}
          <section>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">全部歌曲</h2>
              <Button variant="light" size="sm" color="foreground">
                显示全部
              </Button>
            </div>
            {tracks.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                {tracks.map((track) => (
                  <TrackCard
                    key={track.id}
                    track={{
                      ...track,
                      artist: track.artistName,
                      coverUrl: track.coverUrl || `https://picsum.photos/seed/${track.id}/300/300`,
                    }}
                    onPlay={() => handlePlayTrack(track)}
                  />
                ))}
              </div>
            ) : (
              <EmptyState
                title="暂无歌曲"
                description="请先在设置页面配置音乐文件夹并扫描"
                actionLabel="去设置"
                actionHref="/settings"
              />
            )}
          </section>

          {/* 艺术家 */}
          {artists.length > 0 && (
            <section>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold">艺术家</h2>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                {artists.map((artist) => (
                  <ArtistCard
                    key={artist.id}
                    artist={{
                      id: artist.id,
                      name: artist.name,
                      avatarUrl: artist.avatar || `https://picsum.photos/seed/${artist.id}/300/300`,
                      followers: "0",
                    }}
                  />
                ))}
              </div>
            </section>
          )}

          {/* 专辑 */}
          {albums.length > 0 && (
            <section>
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold">专辑</h2>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                {albums.map((album) => (
                  <PlaylistCard
                    key={album.id}
                    playlist={{
                      id: album.id,
                      name: album.title,
                      description: album.artist?.name || "",
                      coverUrl: album.cover || `https://picsum.photos/seed/${album.id}/300/300`,
                      trackCount: 0,
                    }}
                  />
                ))}
              </div>
            </section>
          )}
        </div>
      </main>

      <MusicPlayer />
    </div>
  );
}

function EmptyState({
  title,
  description,
  actionLabel,
  actionHref,
}: {
  title: string;
  description: string;
  actionLabel: string;
  actionHref: string;
}) {
  return (
    <div className="max-w-xl mx-auto bg-default-100 rounded-xl p-8 text-center">
      <h3 className="text-xl font-bold mb-2">{title}</h3>
      <p className="text-muted-foreground mb-4">{description}</p>
      <Link href={actionHref}>
        <Button color="primary">{actionLabel}</Button>
      </Link>
    </div>
  );
}
