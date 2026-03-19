import Immersion, { ImmersionType } from '@/components/feature/Immersion'
import MusesIconButton from '@/components/ui/MusesIconButton'
import { usePlayerStore } from '@/stores/playerStore'
import {
  Pause as PauseIcon,
  Play as PlayIcon,
  ListVideo as PlayQueueIcon,
} from 'lucide-react-native'
import { useRef } from 'react'
import { Image, Pressable, Text, View } from 'react-native'
import { withUniwind } from 'uniwind'

const StyledImage = withUniwind(Image)

const PlayBar = () => {
  const serverAddress = globalThis.localStorage.getItem('serverAddress') ?? ''
  const currentSongDetail = usePlayerStore((state) => state.currentSongDetail)
  const play = usePlayerStore((state) => state.play)
  const pause = usePlayerStore((state) => state.pause)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const immersionRef = useRef<ImmersionType>(null)
  return (
    <Pressable
      className="h-18 flex-row items-center justify-between p-3 gap-3 bg-white"
      onPress={() => immersionRef.current?.open()}
    >
      <StyledImage
        source={{ uri: `${serverAddress}${currentSongDetail?.cover}` }}
        className="w-18 h-18 rounded-md"
      />
      <View className="flex-1 min-w-0 min-h-0">
        <Text className="text-lg font-bold text-ellipsis overflow-hidden whitespace-nowrap">
          {currentSongDetail?.title}
        </Text>
        <Text className="text-sm text-ellipsis overflow-hidden whitespace-nowrap text-muted">
          {currentSongDetail?.artist}-{currentSongDetail?.album}
        </Text>
      </View>
      <MusesIconButton
        icon={
          isPlaying ? <PauseIcon fill="black" size={20} /> : <PlayIcon fill="black" size={20} />
        }
        onPress={() => (isPlaying ? pause() : play())}
      ></MusesIconButton>
      <MusesIconButton
        icon={<PlayQueueIcon fill="black" size={20} />}
        onPress={() => console.log('play queue')}
      ></MusesIconButton>
      <Immersion ref={immersionRef} />
    </Pressable>
  )
}

export default PlayBar
