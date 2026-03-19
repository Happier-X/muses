import request from '@/utils/request'

const songApi = {
  songs: (params: { page: number; size: number }) =>
    request('/songs', { method: 'GET', params }),
  getSongDetail: (songId: string) =>
    request('/songs/detail', { method: 'GET', params: { songId } }),
}

export default songApi
