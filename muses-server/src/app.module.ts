import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { SongsModule } from './songs/songs.module';
import { UtilsModule } from './utils/utils.module';

@Module({
  imports: [ConfigModule.forRoot(), PrismaModule, AuthModule, UsersModule, SongsModule,UtilsModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
