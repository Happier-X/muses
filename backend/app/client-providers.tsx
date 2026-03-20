"use client";

import { PlayerProvider } from "@/components/PlayerContext";
import { AuthProvider } from "@/components/AuthContext";

export function ClientProviders({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <PlayerProvider>{children}</PlayerProvider>
    </AuthProvider>
  );
}
