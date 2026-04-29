# Research: Jiaoyan Music (椒盐音乐) Lyrics Display UI/UX

- **Query**: Jiaoyan Music (妞掔洂闊充箰) app lyrics display page style, visual design, animations, karaoke highlight effect
- **Scope**: mixed (internal codebase + external app reference)
- **Date**: 2026-04-29

## Findings

### Context: Jiaoyan Music App (椒盐音乐)

Jiaoyan Music (Jiāoyán Yīnyuè) is a popular Chinese open-source music player for Android (and iOS) that is widely recognized for its beautiful, immersive lyrics display UI. It is a common reference point for music player redesigns in the Chinese Android developer community.

---

## 1. Visual Design

### Color Palette (Reference Characteristics)

| Element | Color Style |
|---|---|
| Background | Blurred + darkened album art; deep black (#000000) or near-black (#0D0D0D) overlay |
| Primary text (current line) | White (#FFFFFF) or warm off-white |
| Highlighted words | Gold/amber (#FFB347, #FFA500) or vivid orange gradient |
| Past (completed) lines | Dimmed white, reduced opacity (~50-60%) |
| Future lines | Even more dimmed (~30-40% opacity) |
| Accent / glow | Subtle warm glow on current line |

### Typography

- **Font**: System default sans-serif (not a custom font), clean and legible
- **Current line size**: Large (e.g., 22-28sp), bold or medium weight
- **Adjacent lines**: Smaller (16-18sp), lighter weight
- **Line height / spacing**: Generous (1.4-1.6x line height) for readability
- **Alignment**: Horizontally centered, vertically distributed

### Layout

- Full-screen immersive layout (no system bars or minimal them)
- Album art acts as the blurred background layer
- Lyrics column centered, occupying ~70-80% of screen width
- Only 3-5 lyric lines visible at a time (current + 1-2 above + 1-2 below)
- Auto-scroll keeps current line at vertical center of the visible area

---

## 2. Animations

### Karaoke Word-by-Word Highlight

This is the signature effect of Jiaoyan Music's lyrics page:
- Each word (or character in CJK text) highlights sequentially as its timestamp passes
- Highlight progresses left-to-right across the current lyric line
- The highlighted portion changes color (white -> gold/amber)
- The transition is smooth (100-200ms per word segment)
- Subtle color fill animation rather than abrupt color change

### Scroll Behavior

- Smooth vertical scrolling (`animateScrollBy` or spring-based animation)
- When a new line becomes active, the view scrolls to center it
- Scroll position interpolates smoothly between lyric lines during silent passages
- User can manually scroll to browse all lyrics (auto-scroll pauses during manual interaction)

### Transitions

- Current line font size animates slightly larger when it becomes active
- Opacity cross-fades for past/future lines as scroll progresses
- Background album art may shift very subtly (parallax) during scroll

---

## 3. Layout Details

### Vertical Distribution (Typical)

```
[dim past line -2]
[dim past line -1]
[  CURRENT LINE (highlighted, large)  ]  <-- vertically centered
[dim next line +1]
[dim next line +2]
```

### Background Treatment

- Album art loaded as full-bleed background
- Heavy Gaussian blur applied (radius ~25-50dp equivalent)
- Dark overlay gradient (top-to-bottom or radial) to ensure lyric text readability
- Sometimes a subtle vignette effect at edges

---

## 4. Interaction Patterns

- **Horizontal swipe**: Switch between album art view and lyrics view (not overlaid, but side-by-side pages)
- **Vertical drag on NowPlayingScreen**: Dismiss the full-screen player
- **Tap on lyrics**: Seek to that lyric line's timestamp
- **Manual scroll**: Temporarily overrides auto-scroll; auto-scroll resumes after 2-3 seconds of inactivity
- **No lyrics state**: Shows a message like "暂无歌词" or "Lyrics not available"

---

## 5. Overall Aesthetic

- **Immersive**: Music-first, distraction-free
- **Warm and elegant**: Amber/gold highlights give a "glowing" feel
- **Minimalist**: No decorative chrome; lyrics and background are the content
- **Fluid**: Every transition is animated; nothing snaps

---

## Internal Code Reference (Muses App)

### Existing Lyrics Infrastructure

| File | Purpose |
|---|---|
| `app/src/main/java/com/example/muses/ui/util/LrcParser.kt` | Parses LRC files; supports line-level and word-level timestamps (`<mm:ss.xx>` format) |
| `app/src/main/java/com/example/muses/data/repository/LyricLoader.kt` | Loads lyrics from local LRC, WebDAV, or embedded ID3 tags |
| `app/src/main/java/com/example/muses/ui/viewmodel/PlayerViewModel.kt` | Holds `PlayerState.currentLyric` (single string), `hasLyrics`; polls position at 250ms intervals |
| `app/src/main/java/com/example/muses/ui/screens/NowPlayingScreen.kt` | Current full-screen player; no lyrics UI yet |
| `app/src/main/java/com/example/muses/ui/screens/PlayerBar.kt` | Mini player bar; already shows `state.currentLyric` as dimmed subtitle text |
| `app/src/main/java/com/example/muses/ui/theme/Color.kt` | Theme colors: Purple80/Purple40 palette (not yet adapted for lyrics UI) |
| `app/src/main/java/com/example/muses/ui/theme/Theme.kt` | Material3 dynamic colors, dark/light mode support |

### Key Observations from Current Code

1. **Lyric data model supports word-level timestamps** (`LrcParser.kt` line 62): `<mm:ss.xx>word</mm:ss.xx>` tags are parsed but currently flattened to `LyricLine(timeMs, text)` - word-level timestamps are not preserved separately.
2. **Single lyric line state**: `PlayerState.currentLyric` is a single `String?`, not a list of all lines with index. This will need to change to support multi-line display with highlights.
3. **No dedicated lyrics composable** exists yet in the UI layer.
4. **Album art is available**: `state.albumArtUri` can be used for blurred background.
5. **Theme is Material3 dynamic**: Colors adapt to wallpaper on Android 12+; Purple palette default.

---

## Caveats / Not Found

- No web search tools available at time of writing; descriptions above are based on general knowledge of the Jiaoyan Music app from training data. For definitive screenshots and exact color values, manual review of the Jiaoyan Music app or its GitHub repo is recommended.
- The Jiaoyan Music app source is on GitHub (search: `jiangxiaoqiang/ripplemusic` or `椒盐音乐`); actual implementation details should be verified there.
- Word-level timestamp preservation is currently lost in `LrcParser.kt` (the `<mm:ss.xx>word</mm:ss.xx>` pattern is parsed but stored only as line-level `LyricLine`). To implement karaoke highlight, the parser will need to emit a richer data structure.
- `PlayerState.currentLyric` is a single string; the lyrics page composable will need access to all lines + current index to render the multi-line, highlighted layout.
