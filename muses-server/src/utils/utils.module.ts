import { Global, Module } from '@nestjs/common'
import { UtilsService } from './utils.service';

@Global()
@Module({
    exports:[UtilsService],
    providers:[UtilsService]
})
export class UtilsModule { }