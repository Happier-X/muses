import { Controller, Get, Param } from '@nestjs/common';
import { SongsService } from './songs.service';

@Controller('songs')
export class SongsController {
    constructor(private readonly songsService: SongsService) { }

    @Get('scan')
    scanAllSongs() {
        return this.songsService.scanAllSongs();
    }

    @Get('stream/:id')
    getStreamById(@Param('id') id: string) {
        return this.songsService.getStreamById(Number(id));
    }
}
