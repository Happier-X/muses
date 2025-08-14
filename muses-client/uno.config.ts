import presetIcons from '@unocss/preset-icons'
import { defineConfig, presetWind4 } from 'unocss'

export default defineConfig({
  rules: [
    ['drag', { '-webkit-app-region': 'drag' }],
    ['no-drag', { '-webkit-app-region': 'no-drag' }]
  ],
  presets: [
    presetWind4(),
    presetIcons({
      extraProperties: {
        display: 'inline-block',
        'vertical-align': 'middle'
      }
    })
  ]
})
