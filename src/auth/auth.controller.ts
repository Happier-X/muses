import { Controller, UseGuards, Post, Req, Body } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { Request } from 'express';
import { LocalAuthGuard } from './guard/local-auth.guard';
import { Public } from './decorator/auth.decorator';

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
}
