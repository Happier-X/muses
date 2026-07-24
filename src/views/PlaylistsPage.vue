<template>
  <ion-page>
    <h-nav-bar title="歌单" :fixed="false">
      <template #right>
        <ion-button fill="clear" aria-label="新建歌单" @click="openCreateAlert">
          <h-icon slot="icon-only" :icon="addOutline" aria-hidden="true" />
        </ion-button>
      </template>
    </h-nav-bar>
    <ion-content :fullscreen="false">
      <div class="tablet-content-limit">
        <h-empty
          v-if="playlists.length === 0"
          title="还没有歌单"
          description="点右上角新建，或在歌曲页「更多」加入歌单。"
        />

        <ion-list v-else>
          <ion-item
            v-for="item in listRows"
            :key="item.id"
            button
            :detail="false"
            lines="none"
            class="playlist-item"
            @click="openDetail(item.id)"
          >
            <m-cover slot="start" :size="48" radius="sm" alt="">
              <template #placeholder>
                <h-icon :icon="list" aria-hidden="true" />
              </template>
            </m-cover>
            <ion-label>
              <h2>{{ item.name }}</h2>
              <p>{{ item.validCount }} 首</p>
            </ion-label>
            <ion-button
              slot="end"
              fill="clear"
              class="more-button"
              aria-label="更多歌单操作"
              @click.stop="openPlaylistActions(item.id)"
            >
              <h-icon slot="icon-only" :icon="ellipsisVertical" aria-hidden="true" />
            </ion-button>
          </ion-item>
        </ion-list>
      </div>

      <ion-action-sheet
        :is-open="isActionsOpen"
        header="歌单操作"
        :buttons="actionButtons"
        @didDismiss="onActionsDismiss"
      />

      <ion-alert
        :is-open="isNameAlertOpen"
        :header="nameAlertHeader"
        :inputs="nameAlertInputs"
        :buttons="nameAlertButtons"
        @didDismiss="isNameAlertOpen = false"
      />

      <ion-alert
        :is-open="isDeleteAlertOpen"
        header="删除歌单"
        :message="deleteMessage"
        :buttons="deleteAlertButtons"
        @didDismiss="isDeleteAlertOpen = false"
      />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  IonActionSheet,
  IonAlert,
  IonButton,
  IonContent,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  onIonViewWillEnter,
  type ActionSheetButton,
  type AlertButton,
  type AlertInput,
} from '@ionic/vue'
import { addOutline, ellipsisVertical, list } from '@/icons'
import { HEmpty, HIcon, HNavBar, MCover } from '@/components/ui'
import { loadSongs, SONGS_UPDATED_EVENT } from '@/features/library/storage'
import {
  countValidSongs,
  createPlaylist,
  deletePlaylist,
  loadPlaylists,
  PLAYLISTS_UPDATED_EVENT,
  renamePlaylist,
  type Playlist,
} from '@/features/playlist'

const router = useRouter()
const playlists = ref<Playlist[]>([])
const songsTick = ref(0)
const activePlaylistId = ref<string | null>(null)
const isActionsOpen = ref(false)
const isNameAlertOpen = ref(false)
const nameAlertMode = ref<'create' | 'rename'>('create')
const isDeleteAlertOpen = ref(false)

const refresh = () => {
  playlists.value = loadPlaylists().slice().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  songsTick.value += 1
}

const listRows = computed(() => {
  void songsTick.value
  const songs = loadSongs()
  return playlists.value.map((playlist) => ({
    ...playlist,
    validCount: countValidSongs(playlist, songs),
  }))
})

const openDetail = (id: string) => {
  void router.push(`/tabs/playlists/${encodeURIComponent(id)}`)
}

const openCreateAlert = () => {
  nameAlertMode.value = 'create'
  activePlaylistId.value = null
  isNameAlertOpen.value = true
}

const openPlaylistActions = (id: string) => {
  activePlaylistId.value = id
  isActionsOpen.value = true
}

const onActionsDismiss = () => {
  isActionsOpen.value = false
}

const actionButtons = computed<ActionSheetButton[]>(() => [
  {
    text: '重命名',
    handler: () => {
      nameAlertMode.value = 'rename'
      isNameAlertOpen.value = true
    },
  },
  {
    text: '删除',
    role: 'destructive',
    handler: () => {
      isDeleteAlertOpen.value = true
    },
  },
  { text: '取消', role: 'cancel' },
])

const nameAlertHeader = computed(() => (nameAlertMode.value === 'create' ? '新建歌单' : '重命名歌单'))

const nameAlertInputs = computed<AlertInput[]>(() => {
  const current = activePlaylistId.value
    ? playlists.value.find((p) => p.id === activePlaylistId.value)?.name ?? ''
    : ''
  return [
    {
      name: 'name',
      type: 'text',
      placeholder: '歌单名称',
      value: nameAlertMode.value === 'rename' ? current : '',
      attributes: { maxlength: 80 },
    },
  ]
})

const nameAlertButtons = computed<AlertButton[]>(() => [
  { text: '取消', role: 'cancel' },
  {
    text: '确定',
    handler: (data: { name?: string }) => {
      const name = typeof data?.name === 'string' ? data.name : ''
      if (nameAlertMode.value === 'create') {
        const created = createPlaylist(name)
        if (!created) {
          return false
        }
        refresh()
        return true
      }
      if (!activePlaylistId.value) {
        return false
      }
      const ok = renamePlaylist(activePlaylistId.value, name)
      if (ok) {
        refresh()
      }
      return ok
    },
  },
])

const deleteMessage = computed(() => {
  const name = playlists.value.find((p) => p.id === activePlaylistId.value)?.name ?? '该歌单'
  return `确定删除「${name}」？此操作不可撤销。`
})

const deleteAlertButtons = computed<AlertButton[]>(() => [
  { text: '取消', role: 'cancel' },
  {
    text: '删除',
    role: 'destructive',
    handler: () => {
      if (activePlaylistId.value) {
        deletePlaylist(activePlaylistId.value)
        refresh()
      }
    },
  },
])

onMounted(() => {
  refresh()
  window.addEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.addEventListener(SONGS_UPDATED_EVENT, refresh)
})

onUnmounted(() => {
  window.removeEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.removeEventListener(SONGS_UPDATED_EVENT, refresh)
})

onIonViewWillEnter(() => {
  refresh()
})
</script>

<style scoped>
.playlist-item {
  --padding-start: 12px;
  --inner-padding-end: 4px;
  margin-bottom: 4px;
}

.more-button {
  margin: 0;
}

@media (min-width: 768px) {
  .tablet-content-limit {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
</style>
