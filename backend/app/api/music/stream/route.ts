import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import * as fs from "fs";
import * as path from "path";

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get("id");

    if (!id) {
      return NextResponse.json(
        { error: "请提供曲目 ID" },
        { status: 400 }
      );
    }

    const track = await prisma.track.findUnique({
      where: { id },
    });

    if (!track) {
      return NextResponse.json(
        { error: "曲目不存在" },
        { status: 404 }
      );
    }

    const filePath = track.filePath || track.audioUrl;

    if (!fs.existsSync(filePath)) {
      return NextResponse.json(
        { error: "音频文件不存在" },
        { status: 404 }
      );
    }

    const stat = fs.statSync(filePath);
    const fileSize = stat.size;
    const range = request.headers.get("range");

    if (range) {
      // 流式传输
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunkSize = end - start + 1;

      const file = fs.createReadStream(filePath, { start, end });
      const chunks: Buffer[] = [];

      for await (const chunk of file) {
        chunks.push(Buffer.from(chunk));
      }

      const buffer = Buffer.concat(chunks);

      return new NextResponse(buffer, {
        status: 206,
        headers: {
          "Content-Range": `bytes ${start}-${end}/${fileSize}`,
          "Accept-Ranges": "bytes",
          "Content-Length": String(chunkSize),
          "Content-Type": "audio/mpeg",
        },
      });
    }

    // 完整文件下载
    const fileBuffer = fs.readFileSync(filePath);
    const ext = path.extname(filePath).toLowerCase();
    const contentTypes: Record<string, string> = {
      ".mp3": "audio/mpeg",
      ".flac": "audio/flac",
      ".wav": "audio/wav",
      ".m4a": "audio/mp4",
      ".ogg": "audio/ogg",
      ".aac": "audio/aac",
      ".wma": "audio/x-ms-wma",
    };

    return new NextResponse(fileBuffer, {
      status: 200,
      headers: {
        "Content-Length": String(fileSize),
        "Content-Type": contentTypes[ext] || "audio/mpeg",
        "Content-Disposition": `inline; filename="${track.title}.${ext.slice(1)}"`,
      },
    });
  } catch (error) {
    console.error("播放音乐错误:", error);
    return NextResponse.json(
      { error: "播放失败" },
      { status: 500 }
    );
  }
}
