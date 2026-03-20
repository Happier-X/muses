-- AlterTable
ALTER TABLE "albums" ADD COLUMN "artistName" TEXT;
ALTER TABLE "albums" ADD COLUMN "genre" TEXT;
ALTER TABLE "albums" ADD COLUMN "year" INTEGER;

-- AlterTable
ALTER TABLE "artists" ADD COLUMN "genre" TEXT;

-- AlterTable
ALTER TABLE "tracks" ADD COLUMN "albumName" TEXT;
ALTER TABLE "tracks" ADD COLUMN "artistName" TEXT;
ALTER TABLE "tracks" ADD COLUMN "discNumber" INTEGER;
ALTER TABLE "tracks" ADD COLUMN "filePath" TEXT;
ALTER TABLE "tracks" ADD COLUMN "genre" TEXT;
ALTER TABLE "tracks" ADD COLUMN "trackNumber" INTEGER;
ALTER TABLE "tracks" ADD COLUMN "year" INTEGER;

-- CreateTable
CREATE TABLE "scan_configs" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "musicFolder" TEXT NOT NULL,
    "lastScanAt" DATETIME,
    "scanStatus" TEXT NOT NULL DEFAULT 'idle',
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);
