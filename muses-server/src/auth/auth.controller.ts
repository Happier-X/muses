import { Body, Controller, Get, Post, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { AuthService } from './auth.service';
import { Public } from './decorator/auth.decorator';
import { RegisterDto } from './dto/register.dto';
import { LocalAuthGuard } from './guard/local-auth.guard';

@Controller('auth')
export class AuthController {
    constructor(private readonly authService: AuthService) { }

    @Public()
    @UseGuards(LocalAuthGuard)
    @Post('login')
    login(@Req() req: Request) {
        return this.authService.login(req.user);
    }

    @Public()
    @Post('register')
    register(@Body() registerObj: RegisterDto) {
        return this.authService.register(registerObj);
    }

    @Get('refreshToken')
    refreshToken(@Req() req: Request) {
        return this.authService.refreshToken(req.headers.refreshToken as string);
    }
}
