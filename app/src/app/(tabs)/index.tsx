import PlayBar from '@/components/feature/PlayBar'
import { ScrollView, Text, View } from 'react-native'
import RecommendList from '@/components/feature/RecomendList'
import { useQuery } from '@tanstack/react-query'
import songApi from '@/api/song'
import { withUniwind } from 'uniwind'
import { Image } from 'expo-image'

const StyledImage = withUniwind(Image)

export default function Index() {
  const { data: recommendData } = useQuery({
    queryKey: ['recommend'],
    queryFn: () =>
      songApi.songs({
        page: 1,
        size: 10,
      }),
  })
  const serverAddress = globalThis.localStorage.getItem('serverAddress') ?? ''
  return (
    <View className="flex-1">
      <ScrollView className="flex-1 bg-background">
        <RecommendList
          title="推荐歌曲"
          data={recommendData?.data?.items || []}
          renderItem={({ item, index }) => (
            <View className={index === 0 ? 'mx-4' : 'mr-4'}>
              <StyledImage
                source={{ uri: `${serverAddress}${item.cover}` }}
                className="size-24 rounded-md"
              />
              <Text>{item.title}</Text>
            </View>
          )}
          keyExtractor={(item) => item.id.toString()}
        ></RecommendList>
        <RecommendList
          title="推荐专辑"
          data={recommendData?.data?.items || []}
          renderItem={({ item, index }) => (
            <View className={index === 0 ? 'mx-4' : 'mr-4'}>
              <StyledImage
                source={{ uri: `${serverAddress}${item.cover}` }}
                className="size-24 rounded-md"
              />
              <Text>{item.title}</Text>
            </View>
          )}
          keyExtractor={(item) => item.id.toString()}
        ></RecommendList>
        <RecommendList
          title="推荐艺术家"
          data={recommendData?.data?.items || []}
          renderItem={({ item, index }) => (
            <View className={index === 0 ? 'mx-4' : 'mr-4'}>
              <StyledImage
                source={{ uri: `${serverAddress}${item.cover}` }}
                className="size-24 rounded-md"
              />
              <Text>{item.title}</Text>
            </View>
          )}
          keyExtractor={(item) => item.id.toString()}
        ></RecommendList>
        <RecommendList
          title="推荐歌单"
          data={recommendData?.data?.items || []}
          renderItem={({ item, index }) => (
            <View className={index === 0 ? 'mx-4' : 'mr-4'}>
              <StyledImage
                source={{ uri: `${serverAddress}${item.cover}` }}
                className="size-24 rounded-md"
              />
              <Text>{item.title}</Text>
            </View>
          )}
          keyExtractor={(item) => item.id.toString()}
        ></RecommendList>
      </ScrollView>
      <PlayBar></PlayBar>
    </View>
  )
}
