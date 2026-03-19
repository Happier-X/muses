import PlayBar from '@/components/feature/PlayBar'
import MusesIconButton from '@/components/ui/MusesIconButton'
import { BottomSheetModalProvider } from '@gorhom/bottom-sheet'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Stack, usePathname, useRouter } from 'expo-router'
import { ChevronLeft as ArrowLeftIcon } from 'lucide-react-native'
import { GestureHandlerRootView } from 'react-native-gesture-handler'
import { StatusBar } from 'expo-status-bar'
import '../../global.css'
import { HeroUINativeProvider } from 'heroui-native'

type SongListRouteParams = {
  from: string
  title?: string
}

const queryClient = new QueryClient()

export default function Layout() {
  const router = useRouter()
  const pathname = usePathname()
  return (
    <HeroUINativeProvider>
      <QueryClientProvider client={queryClient}>
        <GestureHandlerRootView style={{ flex: 1 }}>
          <BottomSheetModalProvider>
            <Stack
              screenOptions={{
                headerTitleAlign: 'center',
                animation: 'slide_from_right',
                headerLeft: () => (
                  <MusesIconButton icon={<ArrowLeftIcon />} onPress={() => router.back()} />
                ),
                headerBackVisible: false,
              }}
            >
              <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
              <Stack.Screen name="auth" options={{ headerShown: false }} />
              <Stack.Screen
                name="songList"
                options={({ route }) => ({ title: (route.params as SongListRouteParams).title })}
              />
            </Stack>
            {!(
              pathname === '/' ||
              pathname === '/music' ||
              pathname === '/my' ||
              pathname.startsWith('/auth')
            ) && <PlayBar />}
            <StatusBar animated />
          </BottomSheetModalProvider>
        </GestureHandlerRootView>
      </QueryClientProvider>
    </HeroUINativeProvider>
  )
}
