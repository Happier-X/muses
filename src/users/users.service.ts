import { HttpException, Injectable } from '@nestjs/common';
import { User } from 'generated/prisma/client';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class UsersService {
    constructor(private prisma: PrismaService) { }

    async create(username: string, password: string) {
        const res = await this.findOne(username)
        if (res) {
            throw new HttpException('用户已存在', 400);
        }
        const user = await this.prisma.user.create({
            data: {
                username,
                password,
            },
        });
        const { password: _, ...result } = user;
        return result;
    }

    async findOne(username: string): Promise<User | null> {
        return await this.prisma.user.findUnique({
            where: { username },
        });
    }
}
