<template>
  <div class="m-page playlists-page">
    <div class="m-content">
      <m-empty
        v-if="playlists.length === 0"
        title="还没有歌单"
        description="点右上角新建，或在歌曲页「更多」加入歌单。"
        :icon="list"
      />

      <div v-else class="playlists-page__list">
        <div
          v-for="item in listRows"
          :key="item.id"
          class="playlists-page__row"
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
          <div class="playlists-page__row-info">
            <h2 class="playlists-page__row-title">{{ item.name }}</h2>
            <p class="playlists-page__row-meta">{{ item.validCount }} 首</p>
          </div>
          <m-button
            component="button"
            size="small"
            variant="clear"
            rounded
            class="playlists-page__more-btn"
            aria-label="更多歌单操作"
            @click.stop="openPlaylistActions(item.id)"
          >
            <component :is="ellipsisVertical" aria-hidden="true" class="playlists-page__more-icon" />
          </m-button>
        </div>
      </div>

      <m-actions :opened="isActionsOpen" @backdropclick="isActionsOpen = false">
        <m-actions-group>
          <m-actions-label>歌单操作</m-actions-label>
          <m-actions-button @click="handleRename">重命名</m-actions-button>
          <m-actions-button bold danger @click="handleDelete">删除</m-actions-button>
          <m-actions-button @click="isActionsOpen = false">取消</m-actions-button>
        </m-actions-group>
      </m-actions>

      <m-dialog :opened="isNameAlertOpen" :title="nameAlertHeader">
        <m-list inset>
          <m-list-input
            label="歌单名称"
            type="text"
            :value="nameInput"
            placeholder="歌单名称"
            clear-button
            @input="onNameInput"
          />
        </m-list>
        <template #buttons>
          <m-dialog-button @click="isNameAlertOpen = false">取消</m-dialog-button>
          <m-dialog-button strong @click="onNameConfirm">确定</m-dialog-button>
        </template>
      </m-dialog>

      <m-dialog :opened="isDeleteAlertOpen" title="删除歌单">
        <p class="playlists-page__delete-message">{{ deleteMessage }}</p>
        <template #buttons>
          <m-dialog-button @click="isDeleteAlertOpen = false">取消</m-dialog-button>
          <m-dialog-button strong danger @click="onDeleteConfirm">删除</m-dialog-button>
        </template>
      </m-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ellipsisVertical, list } from '@/icons'
import {
  MActions, MActionsButton, MActionsGroup, MActionsLabel,
  MButton, MDialog, MDialogButton, MList, MListInput, MCover, MEmpty,
} from '@/components/ui'
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

// 新建入口上移分类页 navbar（歌单段），暴露打开方法供父组件调用
defineExpose({ openCreateAlert })
</script>

<style scoped lang="scss">
.playlists-page {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  &__list {
    padding-bottom: var(--m-content-pb);

    @media (min-width: 768px) {
      padding-bottom: var(--m-content-pb-md);
    }
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    margin-bottom: 4px;
    cursor: pointer;

    &:active {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  &__row-info {
    flex: 1;
    min-width: 0;
  }

  &__row-title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--m-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-meta {
    margin: 0;
    font-size: 13px;
    line-height: 1.35;
    color: var(--m-text-2);
  }

  &__more-btn {
    width: 32px;
    height: 32px;
    padding: 0;
    flex: 0 0 32px;
  }

  &__more-icon {
    width: 16px;
    height: 16px;
  }

  &__delete-message {
    margin: 0;
    text-align: center;
    font-size: 15px;
    line-height: 1.4;
    color: var(--m-text-2);
  }
}
</style>