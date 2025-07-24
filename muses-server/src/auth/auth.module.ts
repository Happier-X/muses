import { Module } from '@nestjs/common';
import { AuthService } from './auth.service';
import { UsersModule } from 'src/users/users.module';
import { PassportModule } from '@nestjs/passport';
import { LocalStrategy } from './strategy/local.strategy';
import { AuthController } from './auth.controller';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { JwtModule } from '@nestjs/jwt';
import { JwtStrategy } from './strategy/jwt.strategy';
import { APP_GUARD } from '@nestjs/core';
import { JwtAuthGuard } from './guard/jwt-auth.guard';
import { RolesGuard } from './guard/roles.guard';

@Module({
  imports: [UsersModule, PassportModule, ConfigModule.forRoot({ isGlobal: true }), JwtModule.registerAsync({
    inject: [ConfigService],
    useFactory: ((config: ConfigService) => {
      const secret = config.get('JWT_SECRET')
      return {
        secret,
        signOptions: {
          expiresIn: '1d'
        }
      }
    })
  })],
  providers: [AuthService, LocalStrategy, JwtStrategy, {
    provide: APP_GUARD,
    useClass: JwtAuthGuard
  },{
    provide:APP_GUARD,
    useClass: RolesGuard
  }],
  controllers: [AuthController]
})
export class AuthModule { }
