<template>
  <div class="m-list-input">
    <label v-if="label || $slots.label" class="m-list-input__label">
      <slot name="label">{{ label }}</slot>
    </label>
    <div class="m-list-input__wrap">
      <!-- 自定义 input（Sources 表单等页面自控样式） -->
      <slot v-if="$slots.input" name="input" />
      <input
        v-else
        class="m-list-input__input"
        :type="type"
        :value="modelValue ?? value"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        :inputmode="inputmode"
        @input="onInput"
        @change="onChange"
        @focus="onFocus"
        @blur="onBlur"
      />
      <span
        v-if="clearButton && (textLength > 0)"
        class="m-list-input__clear"
        role="button"
        tabindex="0"
        aria-label="清除"
        @click="onClear"
        @keyup.enter="onClear"
      >
        <svg
          width="28"
          height="28"
          viewBox="0 0 28 28"
          fill="currentcolor"
          aria-hidden="true"
        >
          <path
            d="M14,0 C21.7319865,0 28,6.2680135 28,14 C28,21.7319865 21.7319865,28 14,28 C6.2680135,28 0,21.7319865 0,14 C0,6.2680135 6.2680135,0 14,0 Z M18.9393398,6.93933983 L14,11.8786797 L9.06066017,6.93933983 L6.93933983,9.06066017 L11.8786797,14 L6.93933983,18.9393398 L9.06066017,21.0606602 L14,16.1213203 L18.9393398,21.0606602 L21.0606602,18.9393398 L16.1213203,14 L21.0606602,9.06066017 L18.9393398,6.93933983 Z"
          />
        </svg>
      </span>
    </div>
    <div v-if="(error && error !== true) || $slots.error" class="m-list-input__error">
      <slot name="error">{{ error }}</slot>
    </div>
    <div
      v-else-if="(info && !error) || $slots.info"
      class="m-list-input__info"
    >
      <slot name="info">{{ info }}</slot>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * APP-ONLY：MListInput —— 表单行输入（替代 k-list-input，iOS 布局）
 * label 左（12px 次级色）+ input 右（h-10 16px）；error 红 12px / info 半透明 12px。
 * 支持自定义 #input slot（页面表单最常用）与内置 input（重命名弹层）。
 * clear-button 清除钮在文本非空时显示（iOS 圆形 ×）。
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label?: string
    /** Konsta 时代 :value + @input 用法 */
    value?: string | number
    modelValue?: string | number
    type?: string
    placeholder?: string
    disabled?: boolean
    error?: string | boolean
    info?: string
    clearButton?: boolean
    maxlength?: string | number
    inputmode?: 'search' | 'text' | 'none' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal'
  }>(),
  {
    label: undefined,
    value: undefined,
    modelValue: undefined,
    type: 'text',
    placeholder: undefined,
    disabled: false,
    error: undefined,
    info: undefined,
    clearButton: false,
    maxlength: undefined,
    inputmode: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  input: [event: Event]
  change: [event: Event]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
}>()

const currentValue = computed(() => props.modelValue ?? props.value ?? '')
const textLength = computed(() => String(currentValue.value).length)

function onInput(event: Event) {
  emit('input', event)
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function onChange(event: Event) {
  emit('change', event)
}

function onFocus(event: FocusEvent) {
  emit('focus', event)
}

function onBlur(event: FocusEvent) {
  emit('blur', event)
}

function onClear() {
  emit('input', new Event('input', { bubbles: true }))
  emit('update:modelValue', '')
}
</script>

<style scoped lang="scss">
.m-list-input {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  box-sizing: border-box;
  width: 100%;
  padding: 12px calc(16px + var(--m-safe-area-right, 0px)) 12px
    calc(16px + var(--m-safe-area-left, 0px));
  border: 1px solid var(--m-hairline);
  border-radius: var(--m-radius-card);
  background-color: var(--m-surface-1);
  color: var(--m-text);

  &__label {
    flex: 0 0 auto;
    font-size: 12px;
    line-height: 1.4;
    color: var(--m-text-2);
    margin-right: 8px;
  }

  &__wrap {
    position: relative;
    flex: 1 1 auto;
    min-width: 0;
    margin: -10px 0;
  }

  &__input {
    display: block;
    width: 100%;
    height: 40px;
    box-sizing: border-box;
    appearance: none;
    background: transparent;
    border: none;
    outline: none;
    font-size: 16px;
    line-height: 40px;
    color: var(--m-text);
    font-family: inherit;

    &::placeholder {
      color: var(--m-text-3);
    }

    &:disabled {
      opacity: 0.5;
    }
  }

  &__error,
  &__info {
    flex: 0 0 100%;
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.4;
  }

  &__error {
    position: relative;
    z-index: 10;
    color: var(--m-danger);
  }

  &__info {
    opacity: 0.5;
    color: var(--m-text);
  }

  &__clear {
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 14px;
    height: 14px;
    cursor: pointer;
    opacity: 0.45;
    color: var(--m-text);
    display: flex;
    align-items: center;
    justify-content: center;

    &:active {
      opacity: 0.3;
    }

    svg {
      width: 14px;
      height: 14px;
      display: block;
    }
  }
}
</style>