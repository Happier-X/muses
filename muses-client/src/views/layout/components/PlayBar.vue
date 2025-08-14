<template>
  <n-flex align="center" :wrap="false" class="size-full p-3 relative">
    <div class="absolute w-full left-0 top-[-0.8rem] overflow-hidden">
      <ProgressBar />
    </div>
    <div class="size-full">
      <n-image
        class="h-full aspect-square rounded-lg"
        preview-disabled
        object-fit="cover"
        src="https://app.tngeek.com/api_sso/open/file/b4c64ae_cat-8321993_1280.webp"
      />
    </div>
    <div class="size-full">
      <PlayControls />
    </div>
    <div class="size-full">
      <QueueControls />
    </div>
  </n-flex>
</template>
<script setup lang="ts">
import ProgressBar from "@/components/common/ProgressBar.vue";
import PlayControls from "@/components/common/PlayControls.vue";
import QueueControls from "@/components/common/QueueControls.vue";
</script>
