import { BadRequestException, Injectable } from "@nestjs/common";
import { PassportStrategy } from "@nestjs/passport";
import { AuthService } from "../auth.service";
import { Strategy } from "passport-local";

@Injectable()
export class LocalStrategy extends PassportStrategy(Strategy) {
    constructor(private readonly authService: AuthService) {
        super();
    }

    async validate(username: string, password: string) {
        const user = await this.authService.validateUser(username, password);
        if (!user) {
            throw new BadRequestException('用户名或密码错误');
        }
        return user;
    }
}