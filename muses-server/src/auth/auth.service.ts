import { HttpStatus, Injectable } from '@nestjs/common';
import { UsersService } from 'src/users/users.service';
import * as argon2 from 'argon2';
import { RegisterDto } from './dto/register.dto';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class AuthService {
  constructor(
    private usersService: UsersService,
    private jwtService: JwtService,
    private prisma: PrismaService,
  ) {}

  async register(registerObj: RegisterDto) {
    const password = await argon2.hash(registerObj.password);
    return await this.usersService.create(registerObj.username, password);
  }

  async validateUser(username: string, password: string): Promise<any> {
    const user = await this.usersService.findOne(username);
    if (!user) {
      return null;
    }
    const isPasswordValid = await argon2.verify(user.password, password);
    if (!isPasswordValid) {
      return null;
    } else {
      const { password, ...result } = user;
      return result;
    }
  }

  async login(user: any) {
    const payload = { username: user.username, sub: user.id };
    return {
      statusCode: HttpStatus.CREATED,
      access_token: this.jwtService.sign(payload),
      refresh_token: this.jwtService.sign(payload, {
        expiresIn: '14d',
      }),
    };
  }

  async refreshToken(refreshToken: string) {
    const { sub: userId } = this.jwtService.verify(refreshToken);
    const user = await this.prisma.user.findUnique({
      where: {
        id: userId,
      },
    });
    if (user) {
      const payload = { username: user.username, sub: user.id };
      return {
        statusCode: HttpStatus.CREATED,
        access_token: this.jwtService.sign(payload),
        refresh_token: this.jwtService.sign(payload, {
          expiresIn: '14d',
        }),
      };
    }
    return {
      statusCode: HttpStatus.UNAUTHORIZED,
      message: 'refreshToken失效，请重新登录',
    };
  }
}
