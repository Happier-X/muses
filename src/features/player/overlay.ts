import { ref } from 'vue'

export const playerOverlayVisible = ref(false)
export const queueOverlayVisible = ref(false)

export const openPlayerOverlay = () => {
  playerOverlayVisible.value = true
}

export const closePlayerOverlay = () => {
  playerOverlayVisible.value = false
}

export const openQueueOverlay = () => {
  queueOverlayVisible.value = true
}

export const closeQueueOverlay = () => {
  queueOverlayVisible.value = false
}
