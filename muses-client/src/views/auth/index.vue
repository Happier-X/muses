<template>
  <div
    class="size-screen bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center"
  >
    <el-card class="w-sm flex-col gap-6 rounded-lg">
      <div class="text-center">
        <h2 class="text-lg font-semibold text-gray-800">欢迎!</h2>
      </div>
      <el-form :model="form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="密码" type="password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full" :loading="loading" @click="handleLogin"
            >登录</el-button
          >
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { delayLoadingMiddleware } from '@/api'
import { login } from '@/api/methods/auth'
import { useForm } from 'alova/client'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const { loading, form, send, onSuccess } = useForm((form) => login(form), {
  initialForm: {
    username: '',
    password: ''
  },
  middleware: delayLoadingMiddleware()
})
const router = useRouter()
onSuccess((res: any) => {
  console.log(res)
  if (res.data.statusCode === 201) {
    router.push({
      path: '/app'
    })
    localStorage.setItem('access_token',res.data.access_token)
  } else {
    ElMessage.error(res.data.message)
  }
})
const handleLogin = () => {
  send()
}
</script>
