import { Tabs } from 'expo-router'
import { House as HouseIcon, Music4 as MusicIcon, User as MyIcon } from 'lucide-react-native'

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerTitleAlign: 'center',
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: '首页',
          tabBarIcon: ({ color }) => <HouseIcon color={color} />,
        }}
      />
      <Tabs.Screen
        name="music"
        options={{
          title: '音乐',
          tabBarIcon: ({ color }) => <MusicIcon color={color} />,
        }}
      />
      <Tabs.Screen
        name="my"
        options={{
          title: '我的',
          tabBarIcon: ({ color }) => <MyIcon color={color} />,
        }}
      />
    </Tabs>
  )
}
