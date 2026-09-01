/**
 * Muses Full Player — 单一 WebView 整页渲染
 * 1:1 复刻 Capacitor PlayerPage.vue，承载于 WebView 的 #app 内
 * 与 amll.bundle.js (DroidMate main.tsx) 共存：amll 负责歌词与背景，本文件负责信息栏与整页交互
 */
(function() {
  const state = {
    activePanel: 0,
    isTablet: false,
    title: '',
    artist: '',
    coverUrl: '',
    position: 0,
    duration: 0,
    isPlaying: false,
    repeatMode: 0,
    shuffleEnabled: false,
    hasCover: false,
    lines: [],
  };
  // Salt 风格 SVG 图标（与 Compose SaltIconButton 同源，20/24 描边）
  const svgPlay = '<svg viewBox="0 0 24 24" width="24" height="24"><path d="M8 5.14v14l11-7-11-7z" fill="currentColor"/></svg>';
  const svgPause = '<svg viewBox="0 0 24 24" width="24" height="24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" fill="currentColor"/></svg>';
  const svgPrev = '<svg viewBox="0 0 24 24" width="24" height="24"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" fill="currentColor"/></svg>';
  const svgNext = '<svg viewBox="0 0 24 24" width="24" height="24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" fill="currentColor"/></svg>';
  const svgRepeat = '<svg viewBox="0 0 24 24" width="20" height="20"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z" fill="currentColor"/></svg>';
  const svgRepeatOne = '<svg viewBox="0 0 24 24" width="20" height="20"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z" fill="currentColor"/><text x="11.5" y="15.5" text-anchor="middle" font-size="8" font-weight="900" fill="currentColor">1</text></svg>';
  const svgShuffle = '<svg viewBox="0 0 24 24" width="20" height="20"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z" fill="currentColor"/></svg>';
  const svgOrder = '<svg viewBox="0 0 24 24" width="20" height="20"><circle cx="4.5" cy="6" r="2" fill="currentColor"/><circle cx="4.5" cy="12" r="2" fill="currentColor"/><circle cx="4.5" cy="18" r="2" fill="currentColor"/><path d="M8 5h12v2H8zM8 11h12v2H8zM8 17h12v2H8z" fill="currentColor"/></svg>';
  function setRepeatIcon(mode){ var isOne = mode===1; if(window.Android&&window.Android.log) window.Android.log('setRepeatIcon mode='+mode+' isOne='+isOne,'info'); ['btn-repeat','bottom-repeat'].forEach(function(id){ var el=$(id); if(el){ el.innerHTML=isOne?svgRepeatOne:svgRepeat; el.classList.toggle('active', isOne); }}); }
  function setShuffleIcon(enabled){ if(window.Android&&window.Android.log) window.Android.log('setShuffleIcon enabled='+enabled,'info'); ['btn-shuffle','bottom-shuffle'].forEach(function(id){ var el=$(id); if(el){ el.innerHTML=enabled?svgShuffle:svgOrder; el.classList.toggle('active', !!enabled); }}); }
  const svgQueue = '<svg viewBox="0 0 24 24" width="20" height="20"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" fill="currentColor"/></svg>';
  const svgMore = '<svg viewBox="0 0 24 24" width="20" height="20"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" fill="currentColor"/></svg>';

  function $(id) { return document.getElementById(id); }

  function createEl(tag, cls, parent) {
    const el = document.createElement(tag);
    if (cls) el.className = cls;
    if (parent) parent.appendChild(el);
    return el;
  }

  function formatTime(ms) {
    const s = Math.floor(ms / 1000);
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return String(m).padStart(2,'0') + ':' + String(sec).padStart(2,'0');
  }

  function initDom() {
    if(window.Android&&window.Android.log) window.Android.log('full-player.js v6 iconFix visible 1+Order','info');
    const app = document.getElementById('app');
    if (!app) return;
    if (app.querySelector('.player-page')) return;

    const page = createEl('div', 'player-page', app);
    // 背景由 amll.bundle.js 的 BackgroundRender 负责，此处不创建额外背景，仅保留占位
    const headFixed = createEl('div', 'player-page__song-head--fixed', page);
    headFixed.id = 'song-head-fixed';
    const t1 = createEl('div', 'title', headFixed); t1.id = 'head-title';
    const a1 = createEl('div', 'artist', headFixed); a1.id = 'head-artist';

    const panels = createEl('div', 'panels', page);
    panels.id = 'panels';

    // 左侧信息栏
    const info = createEl('div', 'info-panel', panels);
    const songMeta = createEl('div', 'song-meta', info);
    songMeta.id = 'song-meta';
    const t2 = createEl('div', 'title', songMeta); t2.id = 'info-title';
    const a2 = createEl('div', 'artist', songMeta); a2.id = 'info-artist';

    const coverHero = createEl('div', 'cover-hero', info);
    coverHero.id = 'cover-hero';
    const coverImg = createEl('img', '', coverHero);
    coverImg.id = 'cover-img';
    coverImg.alt = '';
    coverImg.style.display = 'none';
    const placeholder = createEl('div', 'placeholder', coverHero);
    placeholder.id = 'cover-placeholder';
    placeholder.innerHTML = '<svg viewBox="0 0 24 24" width="48" height="48"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" fill="rgba(255,255,255,0.8)"/></svg>';
    placeholder.style.display = 'flex';

    const progress = createEl('div', 'progress-range', info);
    const range = createEl('input', '', progress);
    range.type = 'range';
    range.id = 'progress-range';
    range.min = '0';
    range.max = '1000';
    range.value = '0';
    const timeRow = createEl('div', 'player-page__time-row', progress);
    const cur = createEl('span', '', timeRow); cur.id = 'current-time'; cur.textContent = '00:00';
    const dur = createEl('span', '', timeRow); dur.id = 'duration'; dur.textContent = '00:00';

    const controls = createEl('div', 'controls', info);
    controls.id = 'controls';
    const btnPrev = createEl('button', 'btn', controls); btnPrev.id = 'btn-prev'; btnPrev.innerHTML = svgPrev;
    const btnPlay = createEl('button', 'btn play', controls); btnPlay.id = 'btn-play'; btnPlay.innerHTML = svgPlay;
    const btnNext = createEl('button', 'btn', controls); btnNext.id = 'btn-next'; btnNext.innerHTML = svgNext;

    const modeBar = createEl('div', 'mode-bar', info);
    modeBar.id = 'mode-bar';
    const btnRepeat = createEl('button', 'btn', modeBar); btnRepeat.id = 'btn-repeat'; btnRepeat.innerHTML = svgRepeat;
    const btnShuffle = createEl('button', 'btn', modeBar); btnShuffle.id = 'btn-shuffle'; btnShuffle.innerHTML = svgShuffle;
    const btnQueue = createEl('button', 'btn', modeBar); btnQueue.id = 'btn-queue'; btnQueue.innerHTML = svgQueue;
    const btnMore = createEl('button', 'btn', modeBar); btnMore.id = 'btn-more'; btnMore.innerHTML = svgMore;

    // 右侧歌词栏
    const lyric = createEl('div', 'lyric-panel', panels);
    const lyricContainer = createEl('div', '', lyric);
    lyricContainer.id = 'lyric-player-container';
    // 歌词由 amll.bundle.js 注入，此处仅为容器

    // 平板底部条
    const bottomBar = createEl('div', 'player-page__bottom-bar', page);
    bottomBar.id = 'bottom-bar';
    const bProgress = createEl('div', 'progress-range', bottomBar);
    const bRange = createEl('input', '', bProgress);
    bRange.type = 'range';
    bRange.id = 'bottom-progress';
    bRange.min = '0';
    bRange.max = '1000';
    const bTimeRow = createEl('div', 'player-page__time-row', bProgress);
    const bCur = createEl('span', '', bTimeRow); bCur.id = 'bottom-current'; bCur.textContent = '00:00';
    const bDur = createEl('span', '', bTimeRow); bDur.id = 'bottom-duration'; bDur.textContent = '00:00';
    const bControlsRow = createEl('div', 'controls-row', bottomBar);
    const bLeft = createEl('div', 'left', bControlsRow);
    const bRepeat = createEl('button', 'btn', bLeft); bRepeat.id = 'bottom-repeat'; bRepeat.innerHTML = svgRepeat;
    const bShuffle = createEl('button', 'btn', bLeft); bShuffle.id = 'bottom-shuffle'; bShuffle.innerHTML = svgShuffle;
    const bCenter = createEl('div', 'center', bControlsRow);
    const bPrev = createEl('button', 'btn', bCenter); bPrev.id = 'bottom-prev'; bPrev.innerHTML = svgPrev;
    const bPlay = createEl('button', 'btn play', bCenter); bPlay.id = 'bottom-play'; bPlay.innerHTML = svgPlay;
    const bNext = createEl('button', 'btn', bCenter); bNext.id = 'bottom-next'; bNext.innerHTML = svgNext;
    const bRight = createEl('div', 'right', bControlsRow);
    const bQueue = createEl('button', 'btn', bRight); bQueue.id = 'bottom-queue'; bQueue.innerHTML = svgQueue;
    const bMore = createEl('button', 'btn', bRight); bMore.id = 'bottom-more'; bMore.innerHTML = svgMore;

    // 绑定控制回调
    function bindClick(id, action, extra) {
      const el = $(id);
      if (!el) { if(window.Android&&window.Android.log) window.Android.log('bindClick miss '+id,'warn'); return; }
      if(window.Android&&window.Android.log) window.Android.log('bindClick ok '+id+' -> '+action,'info');
      el.addEventListener('click', (e) => {
        if(window.Android&&window.Android.log) window.Android.log('btn click '+id+' '+action,'info');
        e.stopPropagation();
        // 乐观即时反馈，避免 32ms 轮询 + MediaController 主线程调度导致的视觉延迟
        if (action === 'playPause') {
          state.isPlaying = !state.isPlaying;
          ['btn-play','bottom-play'].forEach(pid=>{ const b=$(pid); if(b) b.innerHTML = state.isPlaying ? svgPause : svgPlay; });
        } else if (action === 'toggleRepeat') {
          const isOne = state.repeatMode === 1;
          state.repeatMode = isOne ? 0 : 1;
          setRepeatIcon(state.repeatMode);
        } else if (action === 'toggleShuffle') {
          state.shuffleEnabled = !state.shuffleEnabled;
          setShuffleIcon(state.shuffleEnabled);
        }
        if (window.Android && window.Android.onAction) {
          try { window.Android.onAction(JSON.stringify(Object.assign({action}, extra||{}))); } catch(err){ if(window.Android&&window.Android.log) window.Android.log('onAction err '+err.message,'error'); }
        } else if (window.Android && window.Android.log) {
          window.Android.log('click no onAction ' + action, 'warn');
        }
        // 兼容旧 bridge 的直接方法
        if (action === 'playPause' && window.Android && window.Android.onPlayPause) { try{window.Android.onPlayPause();}catch(e){} }
      });
      // 避免与 panels 横滑冲突
      el.addEventListener('touchstart', (e)=> e.stopPropagation(), {passive:true});
      el.addEventListener('touchmove', (e)=> e.stopPropagation(), {passive:true});
    }
    bindClick('btn-play', 'playPause');
    bindClick('bottom-play', 'playPause');
    bindClick('btn-prev', 'previous');
    bindClick('bottom-prev', 'previous');
    bindClick('btn-next', 'next');
    bindClick('bottom-next', 'next');
    bindClick('btn-repeat', 'toggleRepeat');
    bindClick('bottom-repeat', 'toggleRepeat');
    bindClick('btn-shuffle', 'toggleShuffle');
    bindClick('bottom-shuffle', 'toggleShuffle');
    bindClick('btn-queue', 'openQueue');
    bindClick('bottom-queue', 'openQueue');
    bindClick('btn-more', 'openMore');
    bindClick('bottom-more', 'openMore');
    setRepeatIcon(state.repeatMode);
    setShuffleIcon(state.shuffleEnabled);

    // 进度条
    function bindProgress(inputId) {
      const input = $(inputId);
      if (!input) return;
      let isDragging = false;
      input.addEventListener('input', () => {
        isDragging = true;
        const pct = parseInt(input.value,10)/1000;
        const seekMs = Math.floor(pct * state.duration);
        if (window.Android && window.Android.onAction) {
          // 预览不发，仅抬起发
        }
        // 同步显示
        const curEl = inputId === 'bottom-progress' ? $('bottom-current') : $('current-time');
        if (curEl) curEl.textContent = formatTime(seekMs);
      });
      input.addEventListener('change', () => {
        const pct = parseInt(input.value,10)/1000;
        const seekMs = Math.floor(pct * state.duration);
        if (window.Android && window.Android.onAction) {
          try { window.Android.onAction(JSON.stringify({action:'seekTo', positionMs: seekMs})); } catch(e) {}
        }
        isDragging = false;
      });
      // 避免与 panels 横滑冲突：横向拖动进度条时禁止 panels 拦截
      input.addEventListener('touchstart', (e) => { e.stopPropagation(); }, {passive:true});
    }
    bindProgress('progress-range');
    bindProgress('bottom-progress');

    // 面板横滑（手机）— 底部模式/控制/进度区不参与横滑，避免按钮点击被误判
    let startX = 0, startY = 0, isSwiping = false;
    function isInNoSwipeZone(target) {
      try { return !!(target && target.closest && target.closest('.mode-bar, .controls, .progress-range, .player-page__bottom-bar'));} catch(e){ return false; }
    }
    panels.addEventListener('touchstart', (e) => {
      if (state.isTablet) return;
      if (isInNoSwipeZone(e.target)) return;
      const t = e.touches[0];
      startX = t.clientX; startY = t.clientY;
      isSwiping = false;
      panels.style.transition = 'none';
    }, {passive:true});
    panels.addEventListener('touchmove', (e) => {
      if (state.isTablet) return;
      if (isInNoSwipeZone(e.target)) return;
      const t = e.touches[0];
      const dx = t.clientX - startX;
      const dy = t.clientY - startY;
      if (!isSwiping && Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 10) {
        isSwiping = true;
      }
      if (isSwiping) {
        e.preventDefault();
        const base = state.activePanel === 0 ? 0 : -50;
        const pct = (dx / window.innerWidth) * 50;
        panels.style.transform = 'translateX(' + (base + pct) + '%)';
      }
    }, {passive:false});
    panels.addEventListener('touchend', (e) => {
      if (state.isTablet) return;
      if (isInNoSwipeZone(e.target)) { isSwiping = false; panels.style.transition=''; return; }
      panels.style.transition = '';
      if (isSwiping) {
        const dx = e.changedTouches[0].clientX - startX;
        if (dx < -60 && state.activePanel === 0) window.setActivePanel && window.setActivePanel(1);
        else if (dx > 60 && state.activePanel === 1) window.setActivePanel && window.setActivePanel(0);
        else panels.style.transform = state.activePanel === 0 ? 'translateX(0)' : 'translateX(-50%)';
      }
      isSwiping = false;
    }, {passive:true});

    // 将 amll 的 lyric-player 移入容器（若已创建）
    function moveLyricPlayer() {
      const container = $('lyric-player-container');
      const amllEl = document.querySelector('.amll-lyric-player');
      if (amllEl && container && amllEl.parentElement !== container) {
        container.appendChild(amllEl);
        amllEl.style.position = 'absolute';
        amllEl.style.inset = '0';
      }
    }
    // 轮询移动（amll 创建有延迟）
    const moveTimer = setInterval(() => {
      moveLyricPlayer();
      if (document.querySelector('#lyric-player-container .amll-lyric-player')) clearInterval(moveTimer);
    }, 100);
    setTimeout(() => clearInterval(moveTimer), 5000);
    // 强制高亮行始终居中（alignPosition 0.5），覆盖 bundle 默认 0.35
    (function enforceCenter(){
      let tries=0;
      const iv=setInterval(()=>{
        tries++;
        try {
          const p=window.__amll && window.__amll.player;
          if(p && p.setAlignPosition){ p.setAlignPosition(0.5); if(window.Android&&window.Android.log) window.Android.log('enforceCenter 0.5','info'); clearInterval(iv); }
        } catch(e){}
        if(tries>30) clearInterval(iv);
      },200);
    })();
    // 歌词滚动位置上报（供 Kotlin 判定是否在顶部，顶部时下滑手势放行给外层关闭）
    (function attachLyricScrollReport(){
      const tryAttach = () => {
        const container = document.getElementById('lyric-player-container');
        const amllEl = container ? container.querySelector('.amll-lyric-player') : null;
        const target = amllEl || container;
        if (!target) { setTimeout(tryAttach, 500); return; }
        const report = () => {
          // 仅首行附近（cur 0-1）视为顶部，其余一律视为已滚动，确保中部可跟手
          const isAtTop = (window._amllCurrentIndex||0) <= 1;
          if (window.Android && window.Android.onLyricScroll) { try{ window.Android.onLyricScroll(isAtTop); }catch(e){} }
        };
        // 劫持 updateTime 的 currentIndex 同步
        const prevUpdateTime = window.updateTime;
        if (prevUpdateTime && !prevUpdateTime.__wrappedAtTop) {
          const wrapped = function(t){
            try{ prevUpdateTime(t); }catch(e){}
            try {
              const lines = state.lines||[];
              let cur = 0;
              for(let i=0;i<lines.length;i++){ if(t >= lines[i].startTime) cur = i; else break; }
              window._amllCurrentIndex = cur;
              if (window.Android && window.Android.onLyricScroll) window.Android.onLyricScroll(cur===0);
            } catch(e){}
          };
          wrapped.__wrappedAtTop = true;
          window.updateTime = wrapped;
        }
        // 兜底轮询
        setInterval(report, 400);
        report();
      };
      setTimeout(tryAttach, 800);
    })();

    // 视口与封面尺寸诊断（供 adb logcat -s FullPlayer 抓取）
    function logViewport(tag) {
      try {
        const w = window.innerWidth, h = window.innerHeight;
        const dpr = window.devicePixelRatio || 1;
        const cover = document.querySelector('#cover-hero img') || document.querySelector('#cover-placeholder');
        const cw = cover ? cover.offsetWidth : 0, ch = cover ? cover.offsetHeight : 0;
        const panels = document.getElementById('panels');
        const ph = panels ? panels.clientHeight : 0;
        const info = document.querySelector('.info-panel');
        const ih = info ? info.clientHeight : 0, ish = info ? info.scrollHeight : 0;
        const page = document.querySelector('.player-page');
        const pgh = page ? page.clientHeight : 0;
        if (window.Android && window.Android.log) window.Android.log(tag + ' viewport ' + w + 'x' + h + ' dpr ' + dpr + ' cover ' + cw + 'x' + ch + ' panelsH ' + ph + ' infoH ' + ih + '/' + ish + ' pageH ' + pgh, 'info');
      } catch(e){}
    }
    // 平板判定
    function updateTablet() {
      const w = window.innerWidth, h = window.innerHeight;
      const isTablet = w >= 768 && w > h;
      state.isTablet = isTablet;
      document.querySelector('.player-page')?.classList.toggle('player-page--tablet', isTablet);
      logViewport('updateTablet');
      // 歌词面板在平板始终显示，手机由 activePanel 控制
      if (isTablet) {
        panels.style.transform = 'none';
      } else {
        panels.setAttribute('data-active', String(state.activePanel));
        panels.style.transform = state.activePanel === 0 ? 'translateX(0)' : 'translateX(-50%)';
      }
    }
    window.addEventListener('resize', updateTablet);
    updateTablet();
    setTimeout(() => logViewport('initDom+1s'), 1000);
    setTimeout(() => logViewport('initDom+2s'), 2000);
    const coverImgEl = document.getElementById('cover-img');
    if (coverImgEl) coverImgEl.addEventListener('load', () => setTimeout(() => logViewport('coverLoad'), 100));
  }

  function init() {
    if (!document.getElementById('app')) { setTimeout(init, 50); return; }
    // 等 amll 的 window.AMLLCore 就绪再创 DOM，避免背景与歌词容器时序
    if (!window.AMLLCore && !document.querySelector('.amll-lyric-player')) {
      // amll 未就绪也先创 DOM，歌词容器稍后移动
    }
    initDom();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    setTimeout(init, 0);
  }

  // 对外 API：整页信息更新（Kotlin 侧 1:1 DroidMate 的 update 块）
  window.updateInfo = function(payload) {
    try {
      const p = payload || {};
      if (p.title !== undefined) {
        state.title = String(p.title||'');
        const a = $('head-title'), b = $('info-title');
        if (a) a.textContent = state.title;
        if (b) b.textContent = state.title;
      }
      if (p.artist !== undefined) {
        state.artist = String(p.artist||'');
        const a = $('head-artist'), b = $('info-artist');
        if (a) a.textContent = state.artist;
        if (b) b.textContent = state.artist;
      }
      if (p.coverUrl !== undefined) {
        state.coverUrl = String(p.coverUrl||'');
        const img = $('cover-img'), ph = $('cover-placeholder');
        if (img) {
          if (state.coverUrl) {
            img.src = state.coverUrl;
            img.style.display = 'block';
            if (ph) ph.style.display = 'none';
          } else {
            img.removeAttribute('src');
            img.style.display = 'none';
            if (ph) ph.style.display = 'flex';
          }
        }
      }
      if (p.hasCover !== undefined) state.hasCover = !!p.hasCover;
    } catch(e) { if(window.Android&&window.Android.log) window.Android.log('updateInfo error '+e.message,'error'); }
  };

  window.updateProgress = function(payload) {
    try {
      const p = payload || {};
      if (p.position !== undefined) state.position = Number(p.position)||0;
      if (p.duration !== undefined) state.duration = Number(p.duration)||0;
      if (p.isPlaying !== undefined) state.isPlaying = !!p.isPlaying;
      const pct = state.duration > 0 ? Math.max(0, Math.min(1, state.position / state.duration)) : 0;
      const cur = formatTime(state.position), dur = formatTime(state.duration);
      const els = ['current-time','duration','bottom-current','bottom-duration'];
      const vals = [cur,dur,cur,dur];
      els.forEach((id,i) => { const el=$(id); if(el) el.textContent=vals[i]; });
      ['progress-range','bottom-progress'].forEach(id=>{
        const el=$(id);
        if(el){
          el.value = String(Math.floor(pct*1000));
          el.style.setProperty('--progress', (pct*100)+'%');
        }
      });
      const playEls = ['btn-play','bottom-play'];
      playEls.forEach(id=>{
        const el=$(id);
        if(el) el.innerHTML = state.isPlaying ? svgPause : svgPlay;
      });
      if (p.repeatMode !== undefined) { state.repeatMode = p.repeatMode; setRepeatIcon(state.repeatMode); }
      if (p.shuffleEnabled !== undefined) { state.shuffleEnabled = !!p.shuffleEnabled; setShuffleIcon(state.shuffleEnabled); }
    } catch(e) {}
  };
  // 同步歌词 lines 供滚动位置上报（已移除 meta-window 渲染）
  (function wrapLyrics(){
    if (window.updateLyrics && !window.updateLyrics.__wrapped) {
      const orig = window.updateLyrics;
      const wrapped = function(payload){
        try { orig(payload); } catch(e){}
        state.lines = (payload && payload.lines) ? payload.lines : [];
      };
      wrapped.__wrapped = true;
      window.updateLyrics = wrapped;
    } else if (!window.updateLyrics) {
      setTimeout(wrapLyrics, 100);
    }
  })();

  window.setActivePanel = function(index) {
    state.activePanel = index === 1 ? 1 : 0;
    const panels = $('panels');
    if (panels) {
      panels.setAttribute('data-active', String(state.activePanel));
      if (!state.isTablet) panels.style.transform = state.activePanel === 0 ? 'translateX(0)' : 'translateX(-50%)';
    }
    if (window.Android && window.Android.onPanelChange) {
      try { window.Android.onPanelChange(state.activePanel); } catch(e){}
    }
  };

  // 兼容旧 Kotlin 的 isPageReady 信号：full-player 创 DOM 后也上报
  const origReady = window.Android && window.Android.onPageReady;
  // amll 的 onPageReady 会在 amll.bundle.js 内触发，此处不覆盖，仅确保 panels 状态
  window.addEventListener('load', () => {
    setTimeout(() => {
      const panels = $('panels');
      if (panels) panels.setAttribute('data-active', String(state.activePanel));
    }, 300);
  });

  // 供 Kotlin 查询当前面板（用于 drag-layer 禁止下滑）
  window.getActivePanel = function() { return state.activePanel; };
})();
