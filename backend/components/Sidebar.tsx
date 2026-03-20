"use client";

import { Button, Avatar } from "@heroui/react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "./AuthContext";

const navItems = [
  { href: "/", label: "首页", icon: HomeIcon },
  { href: "/search", label: "搜索", icon: SearchIcon },
  { href: "/library", label: "音乐库", icon: LibraryIcon },
  { href: "/settings", label: "设置", icon: SettingsIcon },
  { href: "/api-docs", label: "API 文档", icon: ApiIcon },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <aside className="w-64 bg-default-100 flex flex-col h-full border-r border-default-200">
      {/* Logo */}
      <div className="p-6">
        <Link href="/" className="flex items-center gap-2">
          <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center">
            <span className="text-2xl font-bold text-white">M</span>
          </div>
          <span className="text-xl font-bold">Muses</span>
        </Link>
      </div>

      {/* 用户信息 / 登录 */}
      <div className="px-3 mb-4">
        {user ? (
          <div className="flex items-center gap-3 p-2 bg-default-200 rounded-lg">
            <Avatar name={user.username} size="sm" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{user.username}</p>
            </div>
            <Button isIconOnly variant="light" size="sm" onPress={handleLogout}>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </Button>
          </div>
        ) : (
          <div className="flex gap-2">
            <Link href="/login" className="flex-1">
              <Button variant="bordered" className="w-full" size="sm">
                登录
              </Button>
            </Link>
            <Link href="/register" className="flex-1">
              <Button color="primary" className="w-full" size="sm">
                注册
              </Button>
            </Link>
          </div>
        )}
      </div>

      {/* 导航 */}
      <nav className="px-3 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;
          return (
            <Link key={item.href} href={item.href}>
              <Button
                className={`w-full justify-start h-12 px-4 ${
                  isActive
                    ? "bg-default-200 text-foreground"
                    : "text-foreground-600 hover:text-foreground hover:bg-default-200"
                }`}
                startContent={<Icon className="w-5 h-5" />}
              >
                {item.label}
              </Button>
            </Link>
          );
        })}
      </nav>

      {/* 播放列表 */}
      <div className="flex-1 overflow-hidden flex flex-col mt-6">
        <div className="px-6 py-2 flex items-center justify-between">
          <span className="text-sm font-semibold text-foreground-500">播放列表</span>
          <Button
            isIconOnly
            variant="light"
            size="sm"
            className="text-foreground-500 hover:text-foreground"
          >
            <PlusIcon className="w-4 h-4" />
          </Button>
        </div>
        <div className="flex-1 overflow-y-auto px-3 space-y-1">
          {["我喜欢的音乐", "入睡音乐", "工作专注", "运动健身", "周末派对"].map(
            (name, i) => (
              <Link key={name} href={`/playlist/${i + 1}`}>
                <Button
                  className={`w-full justify-start h-10 px-4 text-sm truncate ${
                    pathname === `/playlist/${i + 1}`
                      ? "bg-default-200 text-foreground"
                      : "text-foreground-600 hover:text-foreground hover:bg-default-200"
                  }`}
                >
                  {name}
                </Button>
              </Link>
            )
          )}
        </div>
      </div>
    </aside>
  );
}

// Icons
function HomeIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  );
}

function SearchIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
    </svg>
  );
}

function LibraryIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
    </svg>
  );
}

function SettingsIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  );
}

function PlusIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
    </svg>
  );
}

function ApiIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
    </svg>
  );
}
