<template>
  <!-- HOST-IONIC — app shell；不进入 happier-ui 包心；路由/手势依赖 ion-page -->
  <ion-page>
    <h-nav-bar :fixed="false">
      <template v-if="$slots.start" #left>
        <slot name="start" />
      </template>
      <template #title>
        <slot name="title" />
      </template>
      <template v-if="$slots.end" #right>
        <slot name="end" />
      </template>
    </h-nav-bar>
    <ion-content :fullscreen="fullscreen">
      <slot />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonContent, IonPage } from '@ionic/vue'
import { HNavBar } from 'happier-ui'

withDefaults(defineProps<{
  fullscreen?: boolean
}>(), {
  fullscreen: false,
})
</script>
