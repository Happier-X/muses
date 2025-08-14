<template>
  <n-flex align="center" justify="center" class="size-full">
    <n-button quaternary circle :focusable="false">
      <template #icon>
        <n-icon class="i-mingcute-skip-previous-line"> </n-icon>
      </template>
    </n-button>
    <n-button quaternary circle :focusable="false">
      <template #icon>
        <n-icon class="i-mingcute-play-line"> </n-icon>
      </template>
    </n-button>
    <n-button quaternary circle :focusable="false">
      <template #icon>
        <n-icon class="i-mingcute-skip-forward-line"> </n-icon>
      </template>
    </n-button>
  </n-flex>
</template>
<script setup lang="ts"></script>
