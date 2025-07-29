<template>
  <div class="size-screen flex flex-col">
    <div class="w-screen h-[calc(100%-4.5rem)] flex">
      <div class="w-50 h-full">
        <SideBar />
      </div>
      <div class="w-[calc(100%-12.5rem)] h-full flex flex-col">
        <div class="w-full h-10">
          <NavBar />
        </div>
        <div
          class="w-full h-[calc(100%-2.5rem)] bg-[--lew-bgcolor-2] overflow-hidden p-2"
        >
          <RouterView v-slot="{ Component }">
            <KeepAlive>
              <component
                :is="Component"
                class="size-full overflow-y-auto lew-scrollbar-hover"
              ></component>
            </KeepAlive>
          </RouterView>
        </div>
      </div>
    </div>
    <div class="w-screen h-18">
      <PlayBar />
    </div>
  </div>
</template>
<script setup lang="ts">
import SideBar from "./SideBar.vue";
import NavBar from "./NavBar.vue";
import PlayBar from "./PlayBar.vue";
</script>
