import type { InjectionKey, Ref } from 'vue'

export interface NavigationDrawerContext {
  expanded: Readonly<Ref<boolean>>
  open: (trigger?: HTMLElement | null) => void
}

export const navigationDrawerKey: InjectionKey<NavigationDrawerContext> = Symbol('navigation-drawer')
