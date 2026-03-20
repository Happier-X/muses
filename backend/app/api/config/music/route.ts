import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAuth } from "@/lib/auth";

export async function GET() {
  try {
    const config = await prisma.scanConfig.findUnique({
      where: { id: "default" },
    });

    return NextResponse.json({
      musicFolder: config?.musicFolder || "",
      scanStatus: config?.scanStatus || "idle",
      lastScanAt: config?.lastScanAt,
    });
  } catch (error) {
    console.error("获取配置错误:", error);
    return NextResponse.json(
      { musicFolder: "", scanStatus: "idle" },
      { status: 500 }
    );
  }
}

export async function PUT(request: NextRequest) {
  // 验证登录
  const authError = await requireAuth(request);
  if (authError) return authError;

  try {
    const body = await request.json();
    const { musicFolder } = body;

    if (!musicFolder) {
      return NextResponse.json(
        { error: "请提供音乐文件夹路径" },
        { status: 400 }
      );
    }

    const config = await prisma.scanConfig.upsert({
      where: { id: "default" },
      create: {
        id: "default",
        musicFolder,
      },
      update: {
        musicFolder,
      },
    });

    return NextResponse.json({
      musicFolder: config.musicFolder,
      scanStatus: config.scanStatus,
    });
  } catch (error) {
    console.error("保存配置错误:", error);
    return NextResponse.json(
      { error: "保存失败" },
      { status: 500 }
    );
  }
}
