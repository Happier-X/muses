import { Controller, Get, Param, Query } from '@nestjs/common';
import { SongsService } from './songs.service';

@Controller('songs')
export class SongsController {
    constructor(private readonly songsService: SongsService) { }

    @Get('scan')
    scanAllSongs(@Query('scanAll') scanAll: boolean) {
        return this.songsService.scanAllSongs(scanAll);
    }

    @Get('stream/:id')
    getStreamById(@Param('id') id: string) {
        return this.songsService.getStreamById(Number(id));
    }

    @Get('list')
    getSongsList() {
        return this.songsService.getSongsList();
    }
}
