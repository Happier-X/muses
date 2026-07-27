<template>
  <div class="text-center absolute left-0 right-0 top-1/2 -translate-y-1/2">
    <strong class="text-[20px] leading-[26px]">{{ name }}</strong>
    <p class="text-[16px] leading-[22px] text-[#8c8c8c] m-0">Explore <a class="no-underline" target="_blank" rel="noopener noreferrer" href="https://ionicframework.com/docs/components">UI Components</a></p>
  </div>
</template>

<script setup lang="ts">
defineProps({
  name: String,
});
</script>
