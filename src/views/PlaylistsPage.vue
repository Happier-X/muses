<template>
  <div class="m-page">
    <h-nav-bar title="歌单" :fixed="false">
      <template #right>
        <h-button variant="ghost" is-icon-only shape="square" aria-label="新建歌单" @click="openCreateAlert">
          <h-icon :icon="addOutline" />
        </h-button>
      </template>
    </h-nav-bar>
    <div class="m-content">
      <div class="md:max-w-[var(--muses-content-max-width)] md:mx-auto">
        <h-empty
          v-if="playlists.length === 0"
          title="还没有歌单"
          description="点右上角新建，或在歌曲页「更多」加入歌单。"
        />

        <div v-else class="pb-[calc(var(--muses-mini-player-height)+var(--muses-space-lg))] md:pb-[calc(var(--muses-mini-player-height)+var(--muses-space-lg)+env(safe-area-inset-bottom,0px))]">
          <div
            v-for="item in listRows"
            :key="item.id"
            class="flex items-center gap-3 py-2 px-3 mb-1 cursor-pointer"
            role="button"
            tabindex="0"
            @click="openDetail(item.id)"
            @keyup.enter="openDetail(item.id)"
          >
            <m-cover :size="48" radius="sm" alt="">
              <template #placeholder>
                <h-icon :icon="list" aria-hidden="true" />
              </template>
            </m-cover>
            <div class="flex-1 min-w-0">
              <h2 class="m-0 text-[length:var(--muses-font-title)] font-semibold truncate">{{ item.name }}</h2>
              <p class="m-0 text-[length:var(--muses-font-body-sm)] text-[color:var(--h-color-ink-muted)]">{{ item.validCount }} 首</p>
            </div>
            <h-button
              variant="ghost"
              is-icon-only
              shape="square"
              aria-label="更多歌单操作"
              class="m-0"
              @click.stop="openPlaylistActions(item.id)"
            >
              <h-icon :icon="ellipsisVertical" />
            </h-button>
          </div>
        </div>
      </div>

      <h-bottom-sheet v-model="isActionsOpen" title="歌单操作" @close="onActionsDismiss">
        <div class="flex flex-col gap-[var(--muses-space-xs)] pb-[var(--muses-space-lg)] px-[var(--muses-space-lg)]">
          <button :class="actionSheetItemClass" type="button" @click="handleRename">重命名</button>
          <button :class="[actionSheetItemClass, actionSheetDestructiveClass]" type="button" @click="handleDelete">删除</button>
          <button :class="[actionSheetItemClass, actionSheetCancelClass]" type="button" @click="isActionsOpen = false">取消</button>
        </div>
      </h-bottom-sheet>

      <h-dialog v-model="isNameAlertOpen" :title="nameAlertHeader">
        <h-input v-model="nameInput" placeholder="歌单名称" maxlength="80" />
        <template #actions>
          <h-button variant="ghost" @click="isNameAlertOpen = false">取消</h-button>
          <h-button variant="primary" @click="onNameConfirm">确定</h-button>
        </template>
      </h-dialog>

      <h-dialog v-model="isDeleteAlertOpen" title="删除歌单">
        <p>{{ deleteMessage }}</p>
        <template #actions>
          <h-button variant="ghost" @click="isDeleteAlertOpen = false">取消</h-button>
          <h-button variant="danger" @click="onDeleteConfirm">删除</h-button>
        </template>
      </h-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { addOutline, ellipsisVertical, list } from '@/icons'
import { HBottomSheet, HButton, HDialog, HEmpty, HIcon, HInput, HNavBar, MCover } from '@/components/ui'
import { actionSheetCancelClass, actionSheetDestructiveClass, actionSheetItemClass } from '@/theme/action-sheet'
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
  nameInput.value = ''
}

const nameInput = ref('')

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

const handleRename = () => {
  isActionsOpen.value = false
  nameAlertMode.value = 'rename'
  const current = activePlaylistId.value
    ? playlists.value.find((p) => p.id === activePlaylistId.value)?.name ?? ''
    : ''
  nameInput.value = current
  isNameAlertOpen.value = true
}

const handleDelete = () => {
  isActionsOpen.value = false
  isDeleteAlertOpen.value = true
}

const nameAlertHeader = computed(() => (nameAlertMode.value === 'create' ? '新建歌单' : '重命名歌单'))

const onNameConfirm = () => {
  const name = nameInput.value.trim()
  if (!name) return
  if (nameAlertMode.value === 'create') {
    const created = createPlaylist(name)
    if (created) refresh()
  } else if (activePlaylistId.value) {
    const ok = renamePlaylist(activePlaylistId.value, name)
    if (ok) refresh()
  }
  isNameAlertOpen.value = false
}

const deleteMessage = computed(() => {
  const name = playlists.value.find((p) => p.id === activePlaylistId.value)?.name ?? '该歌单'
  return `确定删除「${name}」？此操作不可撤销。`
})

const onDeleteConfirm = () => {
  if (activePlaylistId.value) {
    deletePlaylist(activePlaylistId.value)
    refresh()
  }
  isDeleteAlertOpen.value = false
}

onMounted(() => {
  refresh()
  window.addEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.addEventListener(SONGS_UPDATED_EVENT, refresh)
})

onUnmounted(() => {
  window.removeEventListener(PLAYLISTS_UPDATED_EVENT, refresh)
  window.removeEventListener(SONGS_UPDATED_EVENT, refresh)
})
</script>
