import PlayBar from '@/components/feature/PlayBar'
import { useRouter } from 'expo-router'
import { Accordion } from 'heroui-native'
import {
  Disc3 as AlbumIcon,
  ChevronRight as ArrowRightIcon,
  MicVocal as ArtistIcon,
  Heart as LikeIcon,
  ListMusic as PlaylistIcon,
  History as RecentPlayIcon,
  Music2 as SongIcon,
} from 'lucide-react-native'
import { ScrollView, Text, View } from 'react-native'
import { withUniwind } from 'uniwind'
const StyledArrowRightIcon = withUniwind(ArrowRightIcon)
export default function Music() {
  const router = useRouter()
  const musicLibraryMenuList = [
    {
      title: '歌曲',
      icon: <SongIcon size={16} />,
      onPress: () =>
        router.navigate({
          pathname: '/songList',
          params: {
            from: 'music',
            title: '歌曲',
          },
        }),
    },
    {
      title: '专辑',
      icon: <AlbumIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '艺术家',
      icon: <ArtistIcon size={16} />,
      onPress: () => alert('开发中'),
    },
  ]
  const myMenuList = [
    {
      title: '歌单',
      icon: <PlaylistIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '喜欢',
      icon: <LikeIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '最近播放',
      icon: <RecentPlayIcon size={16} />,
      onPress: () => alert('开发中'),
    },
  ]
  return (
    <View className="flex-1">
      <ScrollView className="flex-1 bg-background px-4">
        <Text className="font-bold py-2 align-middle">音乐库</Text>
        <Accordion isCollapsible={false} variant="surface" isDividerVisible={false}>
          {musicLibraryMenuList.map((item) => (
            <Accordion.Item key={item.title} value={item.title}>
              <Accordion.Trigger onPress={item.onPress}>
                <View className="flex-row items-center gap-2">
                  {item.icon}
                  <Text>{item.title}</Text>
                </View>
                <Accordion.Indicator>
                  <StyledArrowRightIcon size={16} className="text-muted" />
                </Accordion.Indicator>
              </Accordion.Trigger>
            </Accordion.Item>
          ))}
        </Accordion>
        <Text className="font-bold py-2 align-middle">我的</Text>
        <Accordion isCollapsible={false} variant="surface" isDividerVisible={false}>
          {myMenuList.map((item) => (
            <Accordion.Item key={item.title} value={item.title}>
              <Accordion.Trigger onPress={item.onPress}>
                <View className="flex-row items-center gap-2">
                  {item.icon}
                  <Text>{item.title}</Text>
                </View>
                <Accordion.Indicator>
                  <StyledArrowRightIcon size={16} className="text-muted" />
                </Accordion.Indicator>
              </Accordion.Trigger>
            </Accordion.Item>
          ))}
        </Accordion>
      </ScrollView>
      <PlayBar></PlayBar>
    </View>
  )
}
