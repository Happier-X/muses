<template>
  <div
    class="w-full h-full flex items-center justify-between px-2"
    data-tauri-drag-region
  >
    <div class="flex items-center gap-1">
      <lew-button type="text" single-icon size="small" @click="handleBack">
        <BackIcon class="size-4" />
      </lew-button>
      <lew-button type="text" single-icon size="small" @click="handleForward">
        <ForwardIcon class="size-4" />
      </lew-button>
    </div>
    <div class="flex items-center gap-1">
      <lew-button type="text" single-icon size="small" @click="handleMinimize">
        <MinimizeIcon class="size-4" />
      </lew-button>
      <lew-button
        type="text"
        single-icon
        size="small"
        class="scale-x-[-1]"
        @click="handleToggleScreenSize"
      >
        <component
          :is="isMaximized ? RestoreIcon : MaximizeIcon"
          class="size-4"
        />
      </lew-button>
      <lew-button type="text" single-icon size="small" @click="handleClose">
        <CloseIcon class="size-4" />
      </lew-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useWindowControl } from "@/composables/useWindowControl";
import {
  ChevronLeft as BackIcon,
  X as CloseIcon,
  ChevronRight as ForwardIcon,
  Square as MaximizeIcon,
  Minus as MinimizeIcon,
  Copy as RestoreIcon,
} from "lucide-vue-next";
import { useRouter } from "vue-router";

const { isMaximized, handleMinimize, handleToggleScreenSize, handleClose } =
  useWindowControl();
const router = useRouter();
const handleBack = () => {
  router.back();
};
const handleForward = () => {
  router.forward();
};
</script>
