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
      'apps/**/dist/**',
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
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
)
