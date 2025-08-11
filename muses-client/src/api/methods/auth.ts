import { alova } from '..'

export const login = (data:any)=>{
    return alova.Post('/auth/login',{
        username:data.username,
        password:data.password
    })
}
