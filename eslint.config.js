import pluginVue from 'eslint-plugin-vue'
import { withVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'

const isProduction = process.env.NODE_ENV === 'production'

export default withVueTs(
  {
    rootDir: import.meta.dirname,
    scriptLangs: ['ts', 'js'],
  },
  {
    ignores: [
      'dist/**',
      'android/**',
      'ios/**',
      'coverage/**',
      'node_modules/**',
      '.pi/**',
      '.trellis/**',
      '.DS_Store',
      '.env.local',
      '.env.*.local',
      'npm-debug.log*',
      'yarn-debug.log*',
      'yarn-error.log*',
      'pnpm-debug.log*',
      '.idea/**',
      '.vscode/**',
      '**/*.suo',
      '**/*.ntvs*',
      '**/*.njsproj',
      '**/*.sln',
      '**/*.sw?',
    ],
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  {
    rules: {
      'no-console': isProduction ? 'warn' : 'off',
      'no-debugger': isProduction ? 'warn' : 'off',
      'vue/no-deprecated-slot-attribute': 'off',
      // 防回归：模板使用未导入的组件会被渲染为原生自定义元素（子内容无条件显示，
      // 曾导致歌单页弹层内容裸显在文档流，见 .trellis/tasks/08-06-playlist-page-fix）
      'vue/no-undef-components': 'error',
      '@typescript-eslint/no-explicit-any': 'off',
      // 项目约定：统一使用 ref，禁用 reactive（见 .trellis/spec/frontend/state-management.md）
      'no-restricted-imports': ['error', {
        paths: [{
          name: 'vue',
          importNames: ['reactive'],
          message: '禁止使用 reactive()，统一使用 ref（见 .trellis/spec/frontend/state-management.md）',
        }],
      }],
    },
  },
)
