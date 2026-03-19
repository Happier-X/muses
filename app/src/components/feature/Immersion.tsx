import MusesIconButton from '@/components/ui/MusesIconButton'
import { useBottomSheetBackHandler } from '@/hooks/useBottomSheetBackHandler'
import { usePlayerStore } from '@/stores/playerStore'
import parseTime from '@/utils/parseTime'
import { BottomSheetModal, BottomSheetView } from '@gorhom/bottom-sheet'
import Slider from '@react-native-community/slider'
import { BlurView } from 'expo-blur'
import { Image } from 'expo-image'
import {
  Repeat as ListLoopIcon,
  Ellipsis as MoreIcon,
  ListOrdered as OrderPlayIcon,
  Pause as PauseIcon,
  Play as PlayIcon,
  SkipForward as PlayNextIcon,
  SkipBack as PlayPreviousIcon,
  ListVideo as PlayQueueIcon,
  Shuffle as RandomPlayIcon,
  Repeat1 as SingleLoopIcon,
} from 'lucide-react-native'
import React, { forwardRef, useImperativeHandle, useRef, useState } from 'react'
import { Text, View } from 'react-native'
import { withUniwind } from 'uniwind'

export type ImmersionType = {
  open: () => void
}

const StyledImage = withUniwind(Image)

const Immersion = forwardRef<ImmersionType>((props, ref) => {
  const bottomSheetModalRef = useRef<BottomSheetModal>(null)
  useImperativeHandle(ref, () => ({
    open: () => bottomSheetModalRef.current?.present(),
  }))
  const { handleSheetPositionChange } = useBottomSheetBackHandler(bottomSheetModalRef)
  const serverAddress = globalThis.localStorage.getItem('serverAddress') ?? ''
  const currentSongDetail = usePlayerStore((state) => state.currentSongDetail)
  const play = usePlayerStore((state) => state.play)
  const pause = usePlayerStore((state) => state.pause)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const playNext = usePlayerStore((state) => state.playNext)
  const playPrevious = usePlayerStore((state) => state.playPrevious)
  const loopMode = usePlayerStore((state) => state.loopMode)
  const playMode = usePlayerStore((state) => state.playMode)
  const changeLoopMode = usePlayerStore((state) => state.changeLoopMode)
  const changePlayMode = usePlayerStore((state) => state.changePlayMode)
  const currentTime = usePlayerStore((state) => state.currentTime)
  const duration = usePlayerStore((state) => state.duration)
  const seekTo = usePlayerStore((state) => state.seekTo)
  const [tempCurrentTime, setTempCurrentTime] = useState(0)
  const [isDragging, setIsDragging] = useState(false)

  return (
    <BottomSheetModal
      ref={bottomSheetModalRef}
      enableDynamicSizing
      snapPoints={['100%']}
      activeOffsetY={[-1, 1]}
      failOffsetX={[-5, 5]}
      enablePanDownToClose={true}
      enableHandlePanningGesture={false}
      handleComponent={null}
      onChange={handleSheetPositionChange}
    >
      <BottomSheetView className="h-full px-[8%] items-center justify-evenly">
        <StyledImage
          source={{ uri: `${serverAddress}${currentSongDetail?.cover}` }}
          className="absolute top-0 left-0 bottom-0 right-0"
        ></StyledImage>
        <BlurView
          className="absolute top-0 left-0 bottom-0 right-0"
          intensity={80}
          tint="light"
          experimentalBlurMethod="dimezisBlurView"
        ></BlurView>
        <View className="self-start">
          <Text>{currentSongDetail?.title}</Text>
          <Text>{currentSongDetail?.artist}</Text>
        </View>
        <StyledImage
          source={{ uri: `${serverAddress}${currentSongDetail?.cover}` }}
          className="w-full aspect-square rounded-xl"
        />
        <View>
          <Text>lyric</Text>
        </View>
        <View className="w-full">
          <Slider
            className="h-10"
            minimumValue={0}
            maximumValue={duration}
            value={currentTime}
            step={0.01}
            onSlidingStart={() => setIsDragging(true)}
            onSlidingComplete={(value) => {
              setIsDragging(false)
              seekTo(value)
            }}
            onValueChange={(value) => setTempCurrentTime(value)}
          />
          <View className="flex-row justify-between">
            <View className="flex-row gap-2">
              <Text>{parseTime(currentTime)}</Text>
              {isDragging && (
                <Text className="bg-gray p-1 rounded-md">{parseTime(tempCurrentTime)}</Text>
              )}
            </View>
            <Text>{parseTime(duration)}</Text>
          </View>
        </View>
        <View className="w-full flex-row items-center justify-around">
          <MusesIconButton
            icon={<PlayPreviousIcon fill="black" size={20} />}
            onPress={() => playPrevious()}
          ></MusesIconButton>
          <MusesIconButton
            icon={
              isPlaying ? <PauseIcon fill="black" size={20} /> : <PlayIcon fill="black" size={20} />
            }
            onPress={() => (isPlaying ? pause() : play())}
          ></MusesIconButton>
          <MusesIconButton
            icon={<PlayNextIcon fill="black" size={20} />}
            onPress={() => playNext()}
          ></MusesIconButton>
        </View>
        <View className="w-full flex-row items-center justify-between">
          <MusesIconButton
            icon={
              loopMode === 'listLoop' ? <ListLoopIcon size={20} /> : <SingleLoopIcon size={20} />
            }
            onPress={() => changeLoopMode()}
          ></MusesIconButton>
          <MusesIconButton
            icon={
              playMode === 'orderPlay' ? <OrderPlayIcon size={20} /> : <RandomPlayIcon size={20} />
            }
            onPress={() => changePlayMode()}
          ></MusesIconButton>
          <MusesIconButton
            icon={<PlayQueueIcon size={20} />}
            onPress={() => console.log('play queue')}
          ></MusesIconButton>
          <MusesIconButton
            icon={<MoreIcon size={20} />}
            onPress={() => console.log('more')}
          ></MusesIconButton>
        </View>
      </BottomSheetView>
    </BottomSheetModal>
  )
})

export default Immersion
