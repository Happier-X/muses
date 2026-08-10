<template>
  <div class="m-page">
    <k-navbar center-title>
      <template #title>歌单</template>
      <template #right>
        <k-button component="button" clear rounded class="size-8" aria-label="新建歌单" @click="openCreateAlert">
          <component :is="addOutline" aria-hidden="true" class="size-4" />
        </k-button>
      </template>
    </k-navbar>
    <div class="m-content">
      <div class="md:max-w-[720px] md:mx-auto">
        <m-empty
          v-if="playlists.length === 0"
          title="还没有歌单"
          description="点右上角新建，或在歌曲页「更多」加入歌单。"
          :icon="list"
        />

        <div v-else class="pb-[calc(64px+16px)] md:pb-[calc(64px+16px+var(--safe-area-inset-bottom,env(safe-area-inset-bottom,0px)))]">
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
                <component :is="list" aria-hidden="true" />
              </template>
            </m-cover>
            <div class="flex-1 min-w-0">
              <h2 class="m-0 text-[17px] font-semibold leading-[1.3] text-black dark:text-white truncate">{{ item.name }}</h2>
              <p class="m-0 text-[13px] text-black/55 dark:text-white/55">{{ item.validCount }} 首</p>
            </div>
            <k-button
              component="button"
              small
              rounded
              class="m-0 size-8"
              aria-label="更多歌单操作"
              @click.stop="openPlaylistActions(item.id)"
            >
              <component :is="ellipsisVertical" aria-hidden="true" class="size-4" />
            </k-button>
          </div>
        </div>
      </div>

      <k-actions :opened="isActionsOpen" @backdropclick="isActionsOpen = false">
        <k-actions-group>
          <k-actions-label>歌单操作</k-actions-label>
          <k-actions-button @click="handleRename">重命名</k-actions-button>
          <k-actions-button bold :colors="{ textIos: 'text-[#ff3b30]', activeBgIos: 'active:bg-[#ff3b30]/10' }" @click="handleDelete">删除</k-actions-button>
        </k-actions-group>
        <k-actions-group>
          <k-actions-button @click="isActionsOpen = false">取消</k-actions-button>
        </k-actions-group>
      </k-actions>

      <k-dialog :opened="isNameAlertOpen" :title="nameAlertHeader">
        <k-list inset>
          <k-list-input
            label="歌单名称"
            type="text"
            :value="nameInput"
            placeholder="歌单名称"
            clear-button
            @input="onNameInput"
          />
        </k-list>
        <template #buttons>
          <k-dialog-button @click="isNameAlertOpen = false">取消</k-dialog-button>
          <k-dialog-button strong @click="onNameConfirm">确定</k-dialog-button>
        </template>
      </k-dialog>

      <k-dialog :opened="isDeleteAlertOpen" title="删除歌单">
        <p class="m-0 text-center text-black/55 dark:text-white/55 text-[15px] leading-[1.4]">{{ deleteMessage }}</p>
        <template #buttons>
          <k-dialog-button @click="isDeleteAlertOpen = false">取消</k-dialog-button>
          <k-dialog-button strong :colors="{ fillBgIos: 'bg-[#ff3b30] active:bg-[#e03428]' }" @click="onDeleteConfirm">删除</k-dialog-button>
        </template>
      </k-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { addOutline, ellipsisVertical, list } from '@/icons'
import { kActions, kActionsButton, kActionsGroup, kActionsLabel, kButton, kDialog, kDialogButton, kList, kListInput, kNavbar, MCover, MEmpty } from '@/components/ui'
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

const onNameInput = (e: Event): void => {
  nameInput.value = (e.target as HTMLInputElement).value
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
