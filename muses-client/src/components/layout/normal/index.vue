<template>
  <div class="size-screen flex flex-col">
    <div class="w-full flex-1 flex">
      <div class="w-50 h-full">
        <SideBar />
      </div>
      <div class="flex-1 h-full flex flex-col">
        <div class="w-full h-10">
          <NavBar />
        </div>
        <div class="w-full flex-1 overflow-y-auto bg-gray-100">
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component :is="Component"></component>
            </KeepAlive>
          </RouterView>
        </div>
      </div>
    </div>
    <div class="w-full h-18">
      <PlayBar />
    </div>
  </div>
</template>
<script setup lang="ts">
import SideBar from "./SideBar.vue";
import NavBar from "./NavBar.vue";
import PlayBar from "./PlayBar.vue";
</script>
