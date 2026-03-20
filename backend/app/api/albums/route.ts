import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const search = searchParams.get("search") || "";
    const artistId = searchParams.get("artistId");

    const albums = await prisma.album.findMany({
      where: {
        ...(search && { title: { contains: search } }),
        ...(artistId && { artistId }),
      },
      include: {
        artist: {
          select: {
            id: true,
            name: true,
            avatar: true,
          },
        },
        _count: {
          select: {
            tracks: true,
          },
        },
      },
      take: 50,
      orderBy: { releaseDate: "desc" },
    });

    return NextResponse.json({ albums });
  } catch (error) {
    console.error("获取专辑列表错误:", error);
    return NextResponse.json(
      { error: "获取失败" },
      { status: 500 }
    );
  }
}
