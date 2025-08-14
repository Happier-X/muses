<template>
  <div class="size-screen">
    <n-layout class="size-full relative">
      <n-layout-header position="absolute" class="h-10" bordered>
        <TopBar />
      </n-layout-header>
      <n-layout has-sider position="absolute" class="top-10! bottom-18!">
        <n-layout-sider :native-scrollbar="false" bordered>
          <SideBar />
        </n-layout-sider>
        <n-layout-content content-class="overflow-hidden" :native-scrollbar="false">
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component :is="Component"></component>
            </KeepAlive>
          </RouterView>
        </n-layout-content>
      </n-layout>
      <n-layout-footer position="absolute" class="h-18" bordered>
        <PlayBar />
      </n-layout-footer>
    </n-layout>
  </div>
</template>
<script setup lang="ts">
import SideBar from './components/SideBar.vue'
import TopBar from './components/TopBar.vue'
import PlayBar from './components/PlayBar.vue'
</script>
