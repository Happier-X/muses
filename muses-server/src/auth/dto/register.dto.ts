import { MinLength, MaxLength, IsNotEmpty } from "class-validator";

export class RegisterDto {
    @IsNotEmpty()
    @MinLength(3)
    @MaxLength(20)
    public readonly username: string;

    @IsNotEmpty()
    @MinLength(8)
    @MaxLength(20)
    public readonly password: string;
}