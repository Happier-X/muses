import { alova } from '..'

export const scan = (scanAll?: boolean) => {
  return alova.Get('/songs/scan', {
    params: {
        scanAll
    }
  })
}

export const songList = () => {
  return alova.Get('/songs/list')
}

export const stream = (id:string)=>{
    return alova.Get('/songs/stream', {
        params: {
            id
        }
    })
}