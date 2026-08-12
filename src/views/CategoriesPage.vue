<template>
  <div class="m-page categories-page">
    <m-navbar right-class="categories-page__right">
      <template #title>音乐库</template>
      <template #right>
        <m-button
          v-if="segment === 'playlists'"
          component="button"
          variant="clear"
          rounded
          class="categories-page__add-btn"
          aria-label="新建歌单"
          @click="playlistsRef?.openCreateAlert()"
        >
          <component :is="add" aria-hidden="true" class="categories-page__add-icon" />
        </m-button>
      </template>
      <template #subnavbar>
        <div class="categories-page__segment-wrap">
          <m-segmented strong rounded>
            <m-segmented-button
              v-for="item in segments"
              :key="item.value"
              :active="segment === item.value"
              @click="segment = item.value"
            >
              {{ item.label }}
            </m-segmented-button>
          </m-segmented>
        </div>
      </template>
    </m-navbar>
    <!-- 子页面区：三个分类页 v-show 切换（各自无 navbar，仅内容） -->
    <div class="categories-page__content">
      <AlbumsPage v-show="segment === 'albums'" />
      <ArtistsPage v-show="segment === 'artists'" />
      <PlaylistsPage ref="playlistsRef" v-show="segment === 'playlists'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MButton, MNavbar, MSegmented, MSegmentedButton } from '@/components/ui'
import { add } from '@/icons'
import { getCategoriesSegment, setCategoriesSegment, type CategoriesSegment } from '@/features/player/categoriesSegment'
import AlbumsPage from './AlbumsPage.vue'
import ArtistsPage from './ArtistsPage.vue'
import PlaylistsPage from './PlaylistsPage.vue'

const segments: Array<{ value: CategoriesSegment; label: string }> = [
  { value: 'albums', label: '专辑' },
  { value: 'artists', label: '艺术家' },
  { value: 'playlists', label: '歌单' },
]

const segment = ref<CategoriesSegment>(getCategoriesSegment())
watch(segment, (value) => setCategoriesSegment(value))

const playlistsRef = ref<InstanceType<typeof PlaylistsPage> | null>(null)
</script>

<style scoped lang="scss">
.categories-page {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;

  /* right 区：32px 高（原 rightClass="!h-8"） */
  &__right {
    height: 32px;
  }

  &__add-btn {
    width: 32px;
    height: 32px;
    padding: 0;
    flex: 0 0 32px;

    &:active {
      background-color: rgba(0, 122, 255, 0.15);
    }
  }

  &__add-icon {
    width: 16px;
    height: 16px;
  }

  &__segment-wrap {
    flex: 1;
    min-width: 0;
  }

  &__content {
    position: relative;
    flex: 1;
    min-height: 0;
  }
}
</style>