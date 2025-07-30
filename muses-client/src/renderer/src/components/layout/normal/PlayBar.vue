<template>
  <div class="size-full p-3 flex items-center relative">
    <div class="absolute w-full left-0 top-[-1rem] overflow-hidden">
      <ProgressBar />
    </div>
    <div class="size-full">
      <lew-image
        class="h-full! w-unset! aspect-square!"
        src="https://app.tngeek.com/api_sso/open/file/b4c64ae_cat-8321993_1280.webp"
      />
    </div>
    <div class="size-full">
      <PlayControls />
    </div>
    <div class="size-full">
      <QueueControls />
    </div>
  </div>
</template>
<script setup lang="ts">
import ProgressBar from "@renderer/components/common/ProgressBar.vue";
import PlayControls from "@renderer/components/common/PlayControls.vue";
import QueueControls from "@renderer/components/common/QueueControls.vue";
</script>
