import request from '@/utils/request'

const queueApi = {
  getNextQueueItem: (currentSongId: string, playMode: string) =>
    request('/play-queue/next', { method: 'GET', params: { currentSongId, playMode } }),
  getPreviousQueueItem: (currentSongId: string, playMode: string) =>
    request('/play-queue/previous', { method: 'GET', params: { currentSongId, playMode } }),
}

export default queueApi
