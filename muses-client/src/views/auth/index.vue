<template>
  <div
    class="size-screen bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center"
  >
    <n-card class="w-sm flex-col gap-6 rounded-lg" hoverable>
      <div class="text-center">
        <h2 class="text-lg font-semibold text-gray-800">欢迎!</h2>
      </div>
      <n-form :model="form">
        <n-form-item>
          <n-input v-model:value="form.username" placeholder="用户名" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.password" placeholder="密码" type="password" />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" class="w-full" :loading="loading" @click="handleLogin"
            >登录</n-button
          >
        </n-form-item>
      </n-form>
    </n-card>
  </div>
</template>
<script setup lang="ts">
import { delayLoadingMiddleware } from '@/api'
import { login } from '@/api/methods/auth'
import { useForm } from 'alova/client'

const { loading, form, send } = useForm((form) => login(form), {
  initialForm: {
    username: '',
    password: ''
  },
  middleware: delayLoadingMiddleware()
})
const handleLogin = () => {
  send()
}
</script>
