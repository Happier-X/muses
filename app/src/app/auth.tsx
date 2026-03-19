import authApi from '@/api/auth'
import MusesIconButton from '@/components/ui/MusesIconButton'
import { useForm } from '@tanstack/react-form'
import { useMutation } from '@tanstack/react-query'
import { useRouter } from 'expo-router'
import { Button, Surface, TextField } from 'heroui-native'
import {
  Eye as EyeIcon,
  EyeOff as EyeOffIcon,
  Lock as PasswordIcon,
  Server as ServerAddressIcon,
  User as UsernameIcon,
} from 'lucide-react-native'
import { useState } from 'react'
import { Text, View } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { z } from 'zod'

export default function Auth() {
  const insets = useSafeAreaInsets()
  const router = useRouter()
  const form = useForm({
    defaultValues: {
      serverAddress: globalThis.localStorage.getItem('serverAddress') ?? '',
      username: globalThis.localStorage.getItem('username') ?? '',
      password: '',
    },
    onSubmit: ({ value }) => {
      globalThis.localStorage.setItem('serverAddress', value.serverAddress)
      globalThis.localStorage.setItem('username', value.username)
      loginMutate({
        username: value.username,
        password: value.password,
      })
    },
  })
  const { mutate: loginMutate } = useMutation({
    mutationFn: (body: LoginBody) => authApi.login(body),
    onSuccess: (res) => {
      if (res.code == 200) {
        globalThis.localStorage.setItem('accessToken', res.data.accessToken)
        globalThis.localStorage.setItem('refreshToken', res.data.refreshToken)
        router.replace('(tabs)')
      }
    },
    onError: (err) => {
      console.error('登录失败', err)
    },
  })
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  return (
    <View
      className="flex-1 bg-background px-8 items-center justify-center"
      style={{ paddingTop: insets.top }}
    >
      <Surface className="w-full gap-4">
        <Text className="text-3xl font-bold text-center">Muses</Text>
        <form.Field
          name="serverAddress"
          validators={{
            onBlur: z.url('请输入正确的服务器地址').nonempty('服务器地址不能为空'),
          }}
        >
          {(field) => (
            <>
              <TextField isInvalid={!field.state.meta.isValid}>
                <TextField.Label>服务器地址</TextField.Label>
                <TextField.Input
                  placeholder="请输入服务器地址"
                  value={field.state.value}
                  onChangeText={field.handleChange}
                >
                  <TextField.InputStartContent>
                    <ServerAddressIcon size={16} />
                  </TextField.InputStartContent>
                </TextField.Input>
                <TextField.ErrorMessage>
                  {field.state.meta.errors.join(', ')}
                </TextField.ErrorMessage>
              </TextField>
            </>
          )}
        </form.Field>
        <form.Field
          name="username"
          validators={{
            onBlur: z.string().trim().nonempty('用户名不能为空'),
          }}
        >
          {(field) => (
            <>
              <TextField isInvalid={!field.state.meta.isValid}>
                <TextField.Label>用户名</TextField.Label>
                <TextField.Input
                  placeholder="请输入用户名"
                  value={field.state.value}
                  onChangeText={field.handleChange}
                >
                  <TextField.InputStartContent>
                    <UsernameIcon size={16} />
                  </TextField.InputStartContent>
                </TextField.Input>
                <TextField.ErrorMessage>
                  {field.state.meta.errors.join(', ')}
                </TextField.ErrorMessage>
              </TextField>
            </>
          )}
        </form.Field>
        <form.Field
          name="password"
          validators={{
            onBlur: z.string().trim().nonempty('密码不能为空'),
          }}
        >
          {(field) => (
            <>
              <TextField isInvalid={!field.state.meta.isValid}>
                <TextField.Label>密码</TextField.Label>
                <TextField.Input
                  placeholder="请输入密码"
                  value={field.state.value}
                  onChangeText={field.handleChange}
                  secureTextEntry={!isPasswordVisible}
                >
                  <TextField.InputStartContent className="pointer-events-none">
                    <PasswordIcon size={16} />
                  </TextField.InputStartContent>
                  <TextField.InputEndContent>
                    <MusesIconButton
                      icon={isPasswordVisible ? <EyeOffIcon size={16} /> : <EyeIcon size={16} />}
                      onPress={() => setIsPasswordVisible(!isPasswordVisible)}
                    ></MusesIconButton>
                  </TextField.InputEndContent>
                </TextField.Input>
                <TextField.ErrorMessage>
                  {field.state.meta.errors.join(', ')}
                </TextField.ErrorMessage>
              </TextField>
            </>
          )}
        </form.Field>
        <Button onPress={form.handleSubmit}>登录</Button>
      </Surface>
    </View>
  )
}
