"use client";

import { useState, useEffect } from "react";
import { Button, Input } from "@heroui/react";
import { useAuth } from "./AuthContext";

export function ScanConfig() {
  const { token } = useAuth();
  const [folderPath, setFolderPath] = useState("");
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<{
    musicFolder: string;
    scanStatus: string;
    lastScanAt: string | null;
  } | null>(null);
  const [scanResult, setScanResult] = useState<{
    success: boolean;
    total: number;
    added: number;
    errors: string[];
  } | null>(null);

  // 获取请求头
  const getHeaders = () => {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
    return headers;
  };

  // 加载配置
  const loadConfig = async () => {
    try {
      const res = await fetch("/api/config/music");
      const data = await res.json();
      setStatus(data);
      setFolderPath(data.musicFolder || "");
    } catch (error) {
      console.error("加载配置失败:", error);
    }
  };

  // 保存配置
  const saveConfig = async () => {
    try {
      await fetch("/api/config/music", {
        method: "PUT",
        headers: getHeaders(),
        body: JSON.stringify({ musicFolder: folderPath }),
      });
      await loadConfig();
    } catch (error) {
      console.error("保存配置失败:", error);
    }
  };

  // 扫描音乐
  const scanMusic = async () => {
    setLoading(true);
    setScanResult(null);
    try {
      const res = await fetch("/api/music/scan", {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify({ folderPath }),
      });
      const data = await res.json();
      setScanResult(data);
      await loadConfig();
    } catch (error) {
      console.error("扫描失败:", error);
      setScanResult({ success: false, total: 0, added: 0, errors: ["扫描失败"] });
    } finally {
      setLoading(false);
    }
  };

  // 组件挂载时加载配置
  useEffect(() => {
    loadConfig();
  }, []);

  return (
    <div className="max-w-xl mx-auto bg-default-100 rounded-xl p-6">
      <h2 className="text-xl font-bold mb-4">音乐文件夹配置</h2>

      <div className="space-y-4">
        <Input
          label="音乐文件夹路径"
          placeholder="例如: D:/音乐 或 /volume1/music"
          value={folderPath}
          onChange={(e) => setFolderPath(e.target.value)}
          description="在 NAS 上通常是 /volume1/music 或类似的路径"
          variant="bordered"
        />

        <div className="flex gap-2">
          <Button color="primary" onPress={saveConfig}>
            保存路径
          </Button>
          <Button
            onPress={scanMusic}
            isLoading={loading}
            isDisabled={!folderPath}
          >
            扫描音乐
          </Button>
          <Button variant="bordered" onPress={loadConfig}>
            刷新状态
          </Button>
        </div>

        {status && (
          <div className="text-sm text-muted-foreground space-y-1">
            <p>当前路径: {status.musicFolder || "未设置"}</p>
            <p>扫描状态: {status.scanStatus}</p>
            {status.lastScanAt && (
              <p>上次扫描: {new Date(status.lastScanAt).toLocaleString()}</p>
            )}
          </div>
        )}

        {scanResult && (
          <div
            className={`p-4 rounded-lg ${
              scanResult.success ? "bg-success-100" : "bg-danger-100"
            }`}
          >
            <p className="font-medium">
              {scanResult.success ? "扫描完成" : "扫描失败"}
            </p>
            <p className="text-sm">
              总文件: {scanResult.total} | 新增: {scanResult.added}
            </p>
            {scanResult.errors.length > 0 && (
              <details className="mt-2">
                <summary className="cursor-pointer text-sm">
                  查看错误 ({scanResult.errors.length})
                </summary>
                <ul className="text-xs mt-1 space-y-1">
                  {scanResult.errors.slice(0, 10).map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </details>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
