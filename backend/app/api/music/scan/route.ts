import { NextRequest, NextResponse } from "next/server";
import { scanMusicFolder } from "@/lib/scanner";
import { requireAuth } from "@/lib/auth";

export async function POST(request: NextRequest) {
  // 验证登录
  const authError = await requireAuth(request);
  if (authError) return authError;

  try {
    const body = await request.json();
    const { folderPath } = body;

    if (!folderPath) {
      return NextResponse.json(
        { error: "请提供音乐文件夹路径" },
        { status: 400 }
      );
    }

    const result = await scanMusicFolder(folderPath);

    return NextResponse.json(result);
  } catch (error) {
    console.error("扫描音乐错误:", error);
    return NextResponse.json(
      { error: "扫描失败" },
      { status: 500 }
    );
  }
}
