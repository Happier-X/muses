-- CreateTable
CREATE TABLE "Song" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "title" TEXT NOT NULL,
    "artist" TEXT,
    "album" TEXT,
    "albumArtist" TEXT,
    "year" INTEGER,
    "track" INTEGER,
    "genre" TEXT,
    "duration" REAL,
    "bitrate" INTEGER,
    "sampleRate" INTEGER,
    "format" TEXT NOT NULL,
    "filePath" TEXT NOT NULL,
    "fileSize" INTEGER,
    "coverPath" TEXT,
    "playCount" INTEGER NOT NULL DEFAULT 0,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "Song_filePath_key" ON "Song"("filePath");
