<!-- <template>
    <NFlex vertical>
        <NFlex vertical :size="0">
            <NFlex justify="space-between" align="center">
                <NH3 prefix="bar" class="font-bold mt-5!">推荐歌曲</NH3>
                <NButton text :focusable="false">查看更多</NButton>
            </NFlex>
            <RecommendSongList :list="songList" />
        </NFlex>
        <NFlex vertical :size="0">
            <NFlex justify="space-between" align="center">
                <NH3 prefix="bar" class="font-bold mt-5!">推荐艺术家</NH3>
                <NButton text :focusable="false">查看更多</NButton>
            </NFlex>
            <RecommendArtistList :list="artistsList" />
        </NFlex>
        <NFlex vertical :size="0">
            <NFlex justify="space-between" align="center">
                <NH3 prefix="bar" class="font-bold mt-5!">推荐专辑</NH3>
                <NButton text :focusable="false">查看更多</NButton>
            </NFlex>
            <RecommendSongList :list="songList" />
        </NFlex>
    </NFlex>
</template>

<script setup lang="ts">
import subsonicApi from '@/api/subsonic'
import RecommendArtistList from '@/views/home/components/RecommendArtistList.vue'
import RecommendSongList from '@/views/home/components/RecommendSongList.vue'
import { onMounted, ref } from 'vue'
import { NH3, NButton, NFlex } from 'naive-ui'

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
// 艺术家列表
const artistsList = ref<any[]>([])
/**
 * 获取艺术家列表
 */
const getArtistsList = async () => {
    try {
        const res: any = await subsonicApi.getArtistsIndexesList()
        res.indexes.index.forEach((item: any) => {
            if (item.artist && item.artist.length > 0) {
                artistsList.value.push(...item.artist)
            }
        })
        console.log('艺术家列表:', artistsList.value)
        const res2 = await subsonicApi.getArtistInfo({
            id: artistsList.value[0].id
        })
        console.log('艺术家信息:', res2)
    } catch (error) {
        console.error('获取艺术家列表失败:', error)
    }
}
onMounted(() => {
    getSongList()
    getArtistsList()
})
</script> -->
<template>
    111
</template>
<script setup>
</script>
<style scoped>
</style>