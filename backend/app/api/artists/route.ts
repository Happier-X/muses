import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const search = searchParams.get("search") || "";

    const artists = await prisma.artist.findMany({
      where: search
        ? { name: { contains: search } }
        : undefined,
      include: {
        _count: {
          select: {
            tracks: true,
            albums: true,
          },
        },
      },
      take: 50,
      orderBy: { name: "asc" },
    });

    return NextResponse.json({ artists });
  } catch (error) {
    console.error("获取艺术家列表错误:", error);
    return NextResponse.json(
      { error: "获取失败" },
      { status: 500 }
    );
  }
}
