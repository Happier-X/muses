<!-- <template>
    <n-flex class="py-5" align="center" size="large">
        <img
            :src="songList[0]?.cover"
            class="size-40 aspect-square rounded-xl shadow-md object-cover" />
        <n-flex vertical align="start">
            <n-h3 class="font-bold m-0!">全部歌曲</n-h3>
            <n-flex class="text-xs text-gray-500">
                <span>共 {{ total }} 首</span>
            </n-flex>
            <n-flex>
                <n-button
                    secondary
                    type="primary"
                    round
                    size="small"
                    :focusable="false">
                    <template #icon>
                        <PlayIcon class="size-4" />
                    </template>
                    播放
                </n-button>
            </n-flex>
        </n-flex>
    </n-flex>
    <n-data-table :columns="columns" :data="songList" striped />
</template>
<script setup lang="ts">
import { NFlex, NH3, NButton, NDataTable } from 'naive-ui'
import subsonicApi from '@/api/subsonic'
import { ref, onMounted, h } from 'vue'
import { Play as PlayIcon } from 'lucide-vue-next'
import MusicInfo from '@/components/common/MusicInfo.vue'

// 歌曲总数
const total = ref(0)
// 歌曲列表
const songList = ref<any[]>([])
/**
 * 获取歌曲列表
 */
const getSongList = async () => {
    let current = 0
    const size = 100
    let hasMoreSongs = true
    try {
        songList.value = [] // 清空现有歌曲列表
        while (hasMoreSongs) {
            const res: any = await subsonicApi.search({
                query: '',
                songCount: size,
                songOffset: current * size
            })
            if (res.searchResult2?.song && res.searchResult2.song.length > 0) {
                const songs = res.searchResult2.song
                await Promise.all(
                    songs.map(async (item: any) => {
                        item.cover = await subsonicApi.getCoverById({
                            id: item.id
                        })
                        return item
                    })
                )
                songList.value.push(...songs)
                if (res.searchResult2.song.length < size) {
                    hasMoreSongs = false
                }
            } else {
                hasMoreSongs = false
            }
            current++
        }
        total.value = songList.value.length
    } catch (error) {
        console.error('获取歌曲列表失败:', error)
    }
}
onMounted(async () => {
    await getSongList()
})
const columns = ref([
    {
        title: '歌曲',
        key: 'song',
        render(row: any) {
            return h(
                'div',
                { class: 'h-12' },
                h(MusicInfo, {
                    currentSongInfo: row
                })
            )
        }
    },
    {
        title: '专辑',
        key: 'album',
        ellipsis: {
            tooltip: true
        }
    },
    {
        title: '时长',
        key: 'duration',
        render(row: any) {
            const minutes = Math.floor(row.duration / 60)
            const seconds = row.duration % 60
            return `${minutes}:${seconds < 10 ? '0' + seconds : seconds}`
        }
    }
])
</script> -->
<template>
    123
</template>
<script setup>
</script>
<style scoped>
</style>