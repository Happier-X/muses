"use client";

import { ScanConfig } from "@/components/ScanConfig";
import { MusicPlayer } from "@/components/MusicPlayer";
import { Sidebar } from "@/components/Sidebar";
import { useAuth } from "@/components/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Spinner } from "@heroui/react";

export default function SettingsPage() {
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
          <h1 className="text-2xl font-bold">设置</h1>
        </div>

        <div className="px-8 py-6 space-y-8">
          <ScanConfig />
        </div>
      </main>
      <MusicPlayer />
    </div>
  );
}
