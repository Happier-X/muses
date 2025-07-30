import { defineConfig } from 'unocss'

export default defineConfig({
    rules:[
        ['drag',{'-webkit-app-region':'drag'}],
        ['no-drag',{'-webkit-app-region':'no-drag'}]
    ]
})
