<template>
  <div class="size-screen flex items-center justify-center">
    <div class="w-1/3 h-1/2 shadow-lg p-4 flex items-center justify-center">
      <lew-form
        class="!min-w-0"
        width="80%"
        ref="formRef"
        :options="options"
        @mounted="setForm"
        @change="change"
      />
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { LewMessage } from 'lew-ui'
import * as Yup from 'yup'


const form = ref({} as any)
const formRef = ref()

function submit() {
  LewMessage.request({ loadingMessage: '处理中···' }, () => {
    return new Promise<any>((resolve, reject) => {
      formRef.value
        .validate()
        .then((vail: boolean) => {
          if (vail) {
            form.value = formRef.value.getForm()
            resolve({
              content: '加载成功！',
              duration: 1000,
              type: 'success',
            })
          }
          else {
            resolve({
              content: '请完善表单',
              duration: 1000,
              type: 'warning',
            })
          }
        })
        .catch((err: any) => {
          reject(err)
        })
    })
  })
}
const options = ref([
  {
    field: 'input',
    label: '用户名',
    as: 'input',
    rule: Yup.string().required('请输入用户名'),
  },
  {
    field: 'password',
    label: '密码',
    as: 'input',
    rule: Yup.string().min(6).required('请输入密码'),
    props: {
      type:'password'
    },
  },
  {
    as: 'button',
    props: {
      text: '提交',
      request: submit,
    },
  },
])

function setForm() {
  // 设置表单
  formRef.value.setForm({
    size: 'medium',
  })
}

function resetForm() {
  // 重置表单
  formRef.value.setForm({ size: 'medium' })
}

function change() {
  // 获取表单
  form.value = formRef.value.getForm()
}

onMounted(() => {
  setForm()
})
</script>
