# 实现清单：remove-ionic-residue

- [x] 删除 `ionic.config.json`
- [x] `package.json` 去掉 `ionic:build` / `ionic:serve`
- [x] `index.html` 标题改为 Muses
- [x] `vite.config.ts` 去掉 Ionic manualChunks / 注释中性化
- [x] `src` 误导注释（tailwind.css / variables.css / PlayerPage 等）中性化
- [x] `npm run lint` + `npm run build`
- [x] 勾选 prd AC

## 验证

```bash
rg -n -i "ionic|ionicons|@ionic" --glob "!node_modules/**" --glob "!dist/**" --glob "!package-lock.json" --glob "!changelog/**" --glob "!.trellis/**"
npm run lint
npm run build
```
