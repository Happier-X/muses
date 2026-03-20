import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1");
    const limit = parseInt(searchParams.get("limit") || "50");
    const search = searchParams.get("search") || "";
    const artistId = searchParams.get("artistId");
    const albumId = searchParams.get("albumId");

    const where: Record<string, unknown> = {};

    if (search) {
      where.OR = [
        { title: { contains: search } },
        { artistName: { contains: search } },
        { albumName: { contains: search } },
      ];
    }

    if (artistId) {
      where.artistId = artistId;
    }

    if (albumId) {
      where.albumId = albumId;
    }

    const [tracks, total] = await Promise.all([
      prisma.track.findMany({
        where,
        include: {
          artist: {
            select: {
              id: true,
              name: true,
              avatar: true,
            },
          },
          album: {
            select: {
              id: true,
              title: true,
              cover: true,
            },
          },
        },
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: "desc" },
      }),
      prisma.track.count({ where }),
    ]);

    return NextResponse.json({
      tracks,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    });
  } catch (error) {
    console.error("获取音乐列表错误:", error);
    return NextResponse.json(
      { error: "获取失败" },
      { status: 500 }
    );
  }
}
