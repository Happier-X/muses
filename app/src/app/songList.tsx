import songApi from '@/api/song'
import MusesIconButton from '@/components/ui/MusesIconButton'
import { usePlayerStore } from '@/stores/playerStore'
import { LegendList } from '@legendapp/list'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Image } from 'expo-image'
import { EllipsisVertical as MoreIcon } from 'lucide-react-native'
import { GestureResponderEvent, Pressable, Text, View } from 'react-native'
import { withUniwind } from 'uniwind'

type SongListItemProps = {
  cover: string
  title: string
  album: string
  artist: string
  onPress?: (event: GestureResponderEvent) => void
}
const StyledImage = withUniwind(Image)
const SongListItem: React.FC<SongListItemProps> = ({ cover, title, album, artist, onPress }) => {
  const serverAddress = globalThis.localStorage.getItem('serverAddress') ?? ''
  return (
    <Pressable onPress={onPress} className="flex-row items-center justify-between p-3 gap-3">
      <StyledImage source={{ uri: `${serverAddress}${cover}` }} className="size-12 rounded-md" />
      <View className="flex-1">
        <Text className="font-bold text-lg">{title}</Text>
        <Text className="text-sm text-muted">
          {artist}-{album}
        </Text>
      </View>
      <MusesIconButton icon={<MoreIcon />} onPress={() => console.log('more')}></MusesIconButton>
    </Pressable>
  )
}

const LoadingIndicator = () => (
  <View className="flex items-center justify-center">
    <Text className="font-bold text-center">加载中...</Text>
  </View>
)

const NoMoreData = () => (
  <View className="flex items-center justify-center">
    <Text className="font-bold text-center">没有更多数据了</Text>
  </View>
)

export default function SongList() {
  const loadSong = usePlayerStore((state) => state.loadSong)
  const play = usePlayerStore((state) => state.play)
  const setPlayQueue = usePlayerStore((state) => state.setPlayQueue)
  const {
    data: songsData,
    isLoading,
    error,
    isFetchingNextPage,
    fetchNextPage,
    hasNextPage,
  } = useInfiniteQuery({
    queryKey: ['songs'],
    queryFn: ({ pageParam = 1 }) => songApi.songs({ page: pageParam, size: 30 }),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      const currentPage = lastPage.data.pagination.page
      const totalItems = lastPage.data.pagination.total
      const loadedItems = allPages.flatMap((p) => p.data.items).length
      return loadedItems < totalItems ? currentPage + 1 : undefined
    },
    staleTime: 1000 * 60 * 5,
  })

  const allSongs = songsData?.pages.flatMap((page) => page.data.items) ?? []
  const allSongIds = allSongs.map((item) => item.id)

  const onEndReached = () => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage()
    }
  }

  if (isLoading) return <Text>Loading...</Text>
  if (error) return <Text>Error: {error.message}</Text>

  return (
    <LegendList
      className="flex-1"
      data={allSongs}
      renderItem={({ item }) => (
        <SongListItem
          cover={item.cover}
          title={item.title}
          artist={item.artist}
          album={item.album}
          onPress={() => {
            loadSong(item.id)
            play()
            setPlayQueue(allSongIds)
          }}
        />
      )}
      keyExtractor={(item) => item.id.toString()}
      showsVerticalScrollIndicator={false}
      recycleItems
      onEndReached={onEndReached}
      onEndReachedThreshold={0.1}
      ListFooterComponent={() => {
        if (isFetchingNextPage) {
          return <LoadingIndicator />
        }
        if (!hasNextPage && allSongs.length > 0) {
          return <NoMoreData />
        }
        return null
      }}
    />
  )
}
