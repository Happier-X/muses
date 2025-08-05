/*
  Warnings:

  - Added the required column `fileHash` to the `Song` table without a default value. This is not possible if the table is not empty.
  - Added the required column `filePath` to the `Song` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Song" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "title" TEXT NOT NULL,
    "artist" TEXT NOT NULL DEFAULT '',
    "album" TEXT,
    "lyrics" TEXT,
    "releaseDate" DATETIME,
    "duration" INTEGER,
    "filePath" TEXT NOT NULL,
    "fileHash" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);
INSERT INTO "new_Song" ("album", "artist", "createdAt", "duration", "id", "lyrics", "releaseDate", "title", "updatedAt") SELECT "album", "artist", "createdAt", "duration", "id", "lyrics", "releaseDate", "title", "updatedAt" FROM "Song";
DROP TABLE "Song";
ALTER TABLE "new_Song" RENAME TO "Song";
CREATE UNIQUE INDEX "Song_filePath_key" ON "Song"("filePath");
CREATE UNIQUE INDEX "Song_fileHash_key" ON "Song"("fileHash");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;
