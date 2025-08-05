/*
  Warnings:

  - A unique constraint covering the columns `[filePath,fileHash]` on the table `Song` will be added. If there are existing duplicate values, this will fail.

*/
-- DropIndex
DROP INDEX "Song_fileHash_key";

-- DropIndex
DROP INDEX "Song_filePath_key";

-- CreateIndex
CREATE UNIQUE INDEX "Song_filePath_fileHash_key" ON "Song"("filePath", "fileHash");
