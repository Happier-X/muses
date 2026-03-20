"use client";

import { Button, ProgressBar, Slider } from "@heroui/react";
import {
  PlayArrowIcon,
  PauseIcon,
  SkipPreviousIcon,
  SkipNextIcon,
  ShuffleIcon,
  RepeatIcon,
  VolumeUpIcon,
  VolumeOffIcon,
} from "./icons";
import { useEffect, useRef, useState } from "react";
import { usePlayer } from "./PlayerContext";

export function MusicPlayer() {
  const {
    currentTrack,
    isPlaying,
    progress,
    volume,
    play,
    pause,
    resume,
    toggle,
    next,
    prev,
    setProgress,
    setVolume,
  } = usePlayer();

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [localProgress, setLocalProgress] = useState(0);
  const [duration, setDuration] = useState(0);

  // 创建 audio 元素
  useEffect(() => {
    if (typeof window !== "undefined" && !audioRef.current) {
      audioRef.current = new Audio();
      audioRef.current.volume = volume / 100;
    }
    return () => {
      if (audioRef.current) {
        audioRef.current.pause();
      }
    };
  }, []);

  // 处理播放/暂停
  useEffect(() => {
    if (!audioRef.current || !currentTrack) return;

    const audio = audioRef.current;
    const streamUrl = `/api/music/stream?id=${currentTrack.id}`;

    if (audio.src !== streamUrl) {
      audio.src = streamUrl;
      audio.load();
    }

    if (isPlaying) {
      audio.play().catch(console.error);
    } else {
      audio.pause();
    }
  }, [isPlaying, currentTrack]);

  // 音量控制
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = volume / 100;
    }
  }, [volume]);

  // 监听音频事件
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const handleTimeUpdate = () => {
      if (audio.duration) {
        const percent = (audio.currentTime / audio.duration) * 100;
        setLocalProgress(percent);
        setProgress(percent);
      }
    };

    const handleLoadedMetadata = () => {
      setDuration(audio.duration);
    };

    const handleEnded = () => {
      next();
    };

    audio.addEventListener("timeupdate", handleTimeUpdate);
    audio.addEventListener("loadedmetadata", handleLoadedMetadata);
    audio.addEventListener("ended", handleEnded);

    return () => {
      audio.removeEventListener("timeupdate", handleTimeUpdate);
      audio.removeEventListener("loadedmetadata", handleLoadedMetadata);
      audio.removeEventListener("ended", handleEnded);
    };
  }, [next, setProgress]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  const currentTime = (localProgress / 100) * duration;
  const displayDuration = duration || currentTrack?.duration || 0;

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-default-100 border-t border-default-200 px-4 py-3 z-50">
      <div className="max-w-screen-2xl mx-auto flex items-center justify-between gap-4">
        {/* 当前播放信息 */}
        <div className="flex items-center gap-3 w-72 min-w-0">
          {currentTrack ? (
            <>
              <div className="w-14 h-14 rounded-md bg-default-200 flex-shrink-0 overflow-hidden shadow-sm">
                {currentTrack.coverUrl ? (
                  <img
                    src={currentTrack.coverUrl}
                    alt={currentTrack.albumName}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-primary flex items-center justify-center">
                    <MusicIcon className="w-6 h-6 text-white" />
                  </div>
                )}
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium truncate">{currentTrack.title}</p>
                <p className="text-xs text-muted-foreground truncate">{currentTrack.artistName}</p>
              </div>
            </>
          ) : (
            <div className="text-muted-foreground text-sm">未播放任何内容</div>
          )}
        </div>

        {/* 播放控制 */}
        <div className="flex-1 max-w-2xl flex flex-col items-center gap-2">
          <div className="flex items-center gap-4">
            <Button isIconOnly variant="light" size="sm">
              <ShuffleIcon className="w-4 h-4" />
            </Button>
            <Button isIconOnly variant="light" size="sm" onPress={prev}>
              <SkipPreviousIcon className="w-5 h-5" />
            </Button>
            <Button
              isIconOnly
              color="primary"
              size="md"
              onPress={toggle}
              isDisabled={!currentTrack}
            >
              {isPlaying ? (
                <PauseIcon className="w-5 h-5" />
              ) : (
                <PlayArrowIcon className="w-5 h-5" />
              )}
            </Button>
            <Button isIconOnly variant="light" size="sm" onPress={next}>
              <SkipNextIcon className="w-5 h-5" />
            </Button>
            <Button isIconOnly variant="light" size="sm">
              <RepeatIcon className="w-4 h-4" />
            </Button>
          </div>

          {/* 进度条 */}
          <div className="w-full flex items-center gap-2 text-xs text-muted-foreground">
            <span className="w-10 text-right">{formatTime(currentTime)}</span>
            <div className="w-full max-w-xl">
              <ProgressBar
                value={localProgress}
                aria-label="播放进度"
                size="sm"
                classNames={{
                  base: "w-full",
                  track: "bg-default-300",
                  indicator: "bg-primary",
                }}
              />
            </div>
            <span className="w-10">{formatTime(displayDuration)}</span>
          </div>
        </div>

        {/* 音量控制 */}
        <div className="flex items-center gap-2 w-36">
          <Button
            isIconOnly
            variant="light"
            size="sm"
            onPress={() => setVolume(volume === 0 ? 75 : 0)}
          >
            {volume === 0 ? (
              <VolumeOffIcon className="w-4 h-4" />
            ) : (
              <VolumeUpIcon className="w-4 h-4" />
            )}
          </Button>
          <Slider
            value={volume}
            onChange={(v) => setVolume(Array.isArray(v) ? v[0] : v)}
            minValue={0}
            maxValue={100}
            size="sm"
            className="flex-1"
            aria-label="音量"
            classNames={{
              base: "flex-1",
              track: "bg-default-300",
              filler: "bg-muted-foreground",
              thumb: "bg-foreground",
            }}
          />
        </div>
      </div>
    </div>
  );
}

function MusicIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" />
    </svg>
  );
}
