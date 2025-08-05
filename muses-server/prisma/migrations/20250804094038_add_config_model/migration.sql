/*
  Warnings:

  - You are about to alter the column `lyrics` on the `Song` table. The data in that column could be lost. The data in that column will be cast from `String` to `Json`.

*/
-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Song" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "title" TEXT NOT NULL,
    "artist" TEXT NOT NULL,
    "album" TEXT NOT NULL,
    "lyrics" JSONB NOT NULL,
    "duration" INTEGER NOT NULL,
    "filePath" TEXT NOT NULL,
    "fileHash" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);
INSERT INTO "new_Song" ("album", "artist", "createdAt", "duration", "fileHash", "filePath", "id", "lyrics", "title", "updatedAt") SELECT "album", "artist", "createdAt", "duration", "fileHash", "filePath", "id", "lyrics", "title", "updatedAt" FROM "Song";
DROP TABLE "Song";
ALTER TABLE "new_Song" RENAME TO "Song";
CREATE UNIQUE INDEX "Song_filePath_key" ON "Song"("filePath");
CREATE UNIQUE INDEX "Song_fileHash_key" ON "Song"("fileHash");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;
