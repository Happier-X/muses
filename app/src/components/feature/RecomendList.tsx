import MusesIconButton from '@/components/ui/MusesIconButton'
import { LegendList } from '@legendapp/list'
import { LinearGradient } from 'expo-linear-gradient'
import { ScrollShadow } from 'heroui-native'
import { ChevronRight as ArrowRightIcon } from 'lucide-react-native'
import React from 'react'
import { GestureResponderEvent, ListRenderItem, Text, View } from 'react-native'

type RecommendListProps = {
  title: string
  data: ArrayLike<any>
  keyExtractor: (item: any, index: number) => string
  renderItem: ListRenderItem<any>
  onPressMore?: (event: GestureResponderEvent) => void
}

const RecommendList: React.FC<RecommendListProps> = ({
  title,
  data,
  keyExtractor,
  renderItem,
  onPressMore,
}) => {
  return (
    <>
      <View className="flex-row items-center justify-between px-4">
        <Text className="font-bold py-2 align-middle">{title}</Text>
        <MusesIconButton
          icon={<ArrowRightIcon />}
          onPress={onPressMore && onPressMore}
        ></MusesIconButton>
      </View>
      <ScrollShadow LinearGradientComponent={LinearGradient}>
        <LegendList
          horizontal
          data={data}
          renderItem={renderItem}
          keyExtractor={keyExtractor}
          showsHorizontalScrollIndicator={false}
        />
      </ScrollShadow>
    </>
  )
}

export default RecommendList
