"use client";

import { useState, useEffect } from "react";
import { Input, Button } from "@heroui/react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthContext";
import { Spinner } from "@heroui/react";

export default function LoginPage() {
  const router = useRouter();
  const { user, loading: authLoading, login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!authLoading && user) {
      router.push("/");
    }
  }, [user, authLoading, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.error || "登录失败");
        return;
      }

      login(data.user, data.token);
      router.push("/");
    } catch {
      setError("网络错误，请重试");
    } finally {
      setSubmitting(false);
    }
  };

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner size="lg" color="primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-primary-100 to-background p-4">
      <div className="w-full max-w-md bg-default-100 rounded-2xl p-8 shadow-lg">
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-2 mb-4">
            <div className="w-12 h-12 rounded-full bg-primary flex items-center justify-center">
              <span className="text-2xl font-bold text-white">M</span>
            </div>
          </Link>
          <h1 className="text-2xl font-bold">登录 Muses</h1>
          <p className="text-muted-foreground mt-2">欢迎回来！</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-danger-100 text-danger rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="w-full">
            <Input
              label="用户名"
              placeholder="输入用户名"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              variant="bordered"
              className="w-full"
            />
          </div>

          <div className="w-full">
            <Input
              label="密码"
              type="password"
              placeholder="输入密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              variant="bordered"
              className="w-full"
            />
          </div>

          <Button
            type="submit"
            color="primary"
            className="w-full font-medium"
            isLoading={submitting}
          >
            登录
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          还没有账号？{" "}
          <Link href="/register" className="text-primary font-medium hover:underline">
            注册
          </Link>
        </p>
      </div>
    </div>
  );
}
