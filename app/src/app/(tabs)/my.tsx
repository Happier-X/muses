import PlayBar from '@/components/feature/PlayBar'
import { useRouter } from 'expo-router'
import { Accordion, Avatar, Chip, Surface } from 'heroui-native'
import {
  ChevronRight as ArrowRightIcon,
  LogIn as LoginIcon,
  FolderSearch as ScanSongIcon,
  Settings as SettingsIcon,
  UserCog as UserManageIcon,
} from 'lucide-react-native'
import { ScrollView, Text, View } from 'react-native'
import { withUniwind } from 'uniwind'

const StyledArrowRightIcon = withUniwind(ArrowRightIcon)

export default function My() {
  const router = useRouter()
  const menuList = [
    {
      title: '扫描歌曲',
      icon: <ScanSongIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '用户管理',
      icon: <UserManageIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '设置',
      icon: <SettingsIcon size={16} />,
      onPress: () => alert('开发中'),
    },
    {
      title: '登录',
      icon: <LoginIcon size={16} />,
      onPress: () =>
        router.navigate({
          pathname: '/auth',
        }),
    },
  ]
  return (
    <View className="flex-1">
      <ScrollView className="flex-1 bg-background px-4" contentContainerClassName="gap-4">
        <Surface className="flex-row gap-4 items-center mt-4">
          <Avatar size="lg" alt="avatar">
            <Avatar.Fallback>admin</Avatar.Fallback>
          </Avatar>
          <View className="gap-2">
            <Text>admin</Text>
            <Chip size="sm">管理员</Chip>
          </View>
        </Surface>
        <Accordion isCollapsible={false} variant="surface" isDividerVisible={false}>
          {menuList.map((item) => (
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
