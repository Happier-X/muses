<template>
  <RouterView />
  <n-message-provider>
    <Content/>
  </n-message-provider>
</template>
<script setup lang="ts">
import Content from '@/components/Content.vue'
</script>
