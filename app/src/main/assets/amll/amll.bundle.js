(function(){if(typeof document!=='undefined'){const style=document.createElement('style');style.innerText=".amll-lyric-player{width:100%;max-width:100%;height:100%;color:var(--amll-lp-color,white);contain:strict;mix-blend-mode:plus-lighter;font-size:var(--amll-lp-font-size,max(max(5vh, 2.5vw), 12px));overflow:hidden}@media screen and (width<=768px){.amll-lyric-player{font-size:var(--amll-lp-font-size,max(8vw, 12px))}}.amll-lyric-player.dom{--amll-lp-line-width-aspect:.8;--amll-lp-line-padding-x:1em;--amll-lp-bg-line-scale:.7;-webkit-user-select:none;user-select:none;box-sizing:content-box;z-index:1;line-height:1.2}@media screen and (width<=768px){.amll-lyric-player{--amll-lp-line-width-aspect:1;--amll-lp-line-padding-x:0}}.FmKaba_lyricLineWrapper{box-sizing:border-box;width:100%;padding:.4em var(--lyric-line-padding-x);will-change:transform, opacity, filter;border-radius:.25em;flex-direction:column;align-items:flex-start;gap:.3em;transition:opacity .4s,filter .4s,background-color .25s;display:flex;position:absolute;top:0;left:0}.FmKaba_lyricLineWrapper:hover{background-color:var(--amll-lp-hover-bg-color,#fff1)}.FmKaba_lyricLineWrapper:active{background-color:var(--amll-lp-hover-bg-color,#ffffff05)}.FmKaba_lyricLine{box-sizing:border-box;width:var(--amll-lp-width,100%);min-width:var(--amll-lp-width,100%);max-width:var(--amll-lp-width,100%);contain:layout style paint;content-visibility:auto;contain-intrinsic-size:auto 40px;backface-visibility:hidden;transform-origin:0;will-change:transform, filter, opacity;height:fit-content;margin:-.2em;padding:.2em;transition:box-shadow .25s;position:relative}.FmKaba_lyricDuetLine{text-align:right;transform-origin:100%}.FmKaba_lyricMainLine{contain:layout style;margin:-1em;padding:1em;transition:opacity .3s .1s}.FmKaba_lyricMainLine span{text-align:start;vertical-align:bottom;display:inline-block}.FmKaba_lyricMainLine .FmKaba_romanWord{padding-inline-end:.3em;font-size:.5em;line-height:1em;display:flex}.FmKaba_lyricMainLine .FmKaba_rubyWord{justify-content:center;min-height:1em;font-size:.5em;line-height:1em;display:flex}.FmKaba_lyricMainLine .FmKaba_wordWithRuby{vertical-align:bottom;flex-direction:column;align-items:center;display:inline-flex}.FmKaba_lyricMainLine .FmKaba_wordBody{flex-direction:column;align-items:center;display:flex}.FmKaba_lyricMainLine>span,.FmKaba_lyricMainLine span.FmKaba_emphasizeWrapper{white-space:pre-wrap;vertical-align:bottom;will-change:transform, text-shadow;contain:layout style;margin:-1em;padding:1em;display:inline-block}:is(.FmKaba_lyricMainLine>span,.FmKaba_lyricMainLine span.FmKaba_emphasizeWrapper).FmKaba_emphasize,:is(.FmKaba_lyricMainLine>span,.FmKaba_lyricMainLine span.FmKaba_emphasizeWrapper) span.FmKaba_emphasize{backface-visibility:hidden;margin:-1em;padding:1em}:is(:is(.FmKaba_lyricMainLine>span,.FmKaba_lyricMainLine span.FmKaba_emphasizeWrapper).FmKaba_emphasize,:is(.FmKaba_lyricMainLine>span,.FmKaba_lyricMainLine span.FmKaba_emphasizeWrapper) span.FmKaba_emphasize)>span{will-change:transform, text-shadow;backface-visibility:hidden;margin:-1em;padding:1em}.FmKaba_lyricBgLine{opacity:.4;font-size:max(calc(1em * var(--amll-lp-bg-line-scale,.7)), 10px);transition:background-color .25s,box-shadow .25s}.FmKaba_lyricBgLine .FmKaba_lyricMainLine{padding:1.2em 1em}.FmKaba_lyricBgLine.FmKaba_active{opacity:.4;transition:background-color .25s,box-shadow .25s}.FmKaba_lyricSubLine{opacity:.3;font-size:max(.5em,10px);line-height:1.5em;transition:opacity .2s .25s}@supports (mix-blend-mode:plus-lighter){.FmKaba_lyricSubLine{opacity:.3}}.FmKaba_bottomLine{cursor:default;padding-top:0;padding-bottom:0;line-height:1.8em}.FmKaba_bottomLine:empty{height:0;margin:0;padding:0;display:none}.FmKaba_bgWrapper{top:100%;left:var(--lyric-line-padding-x);z-index:-1;align-items:inherit;width:calc(100% - var(--lyric-line-padding-x) * 2);visibility:visible;pointer-events:auto;opacity:0;transform-origin:0 0;flex-direction:column;transition:opacity .3s;display:flex;position:absolute}.FmKaba_bgWrapperTop{transform-origin:0 100%;width:100%;margin-top:-999px;position:relative;top:auto;bottom:auto;left:0}.FmKaba_bgWrapperActive{opacity:1;width:100%;position:relative;top:auto;bottom:auto;left:0}.FmKaba_bgWrapperHidden{visibility:hidden;pointer-events:none}.FmKaba_interludeDots{opacity:0;transform-origin:50%;gap:.25em;width:fit-content;height:clamp(.5em,1vh,3em);padding:2.5% .75em;transition:opacity .25s;display:flex;position:absolute;left:0}.FmKaba_interludeDots.FmKaba_enabled{opacity:1}.FmKaba_interludeDots>*{background-color:var(--amll-lp-color,white);aspect-ratio:1;border-radius:50%;width:clamp(.5em,1vh,3em);height:clamp(.5em,1vh,3em);margin-right:4px;display:inline-block}.FmKaba_interludeDots.FmKaba_duet{transform-origin:50%;right:0}.FmKaba_disableSpring>*,.FmKaba_disableSpring .FmKaba_lyricLine{transition:filter .25s,transform .5s,background-color .25s,box-shadow .25s}.FmKaba_tmpDisableTransition{transition:none!important}.amll-lyric-player{--lyric-line-padding-x:1em}@media screen and (width<=500px){.amll-lyric-player{--lyric-line-padding-x:20px}}.amll-lyric-player:hover .FmKaba_lyricLine,.amll-lyric-player:hover .FmKaba_lyricLineWrapper{filter:unset!important}.amll-lyric-player:not(.FmKaba_playing) .FmKaba_bgWrapper{opacity:1;width:100%;position:relative;top:auto;bottom:auto;left:0}.amll-lyric-player.FmKaba_hasDuetLine .FmKaba_lyricLine:not(.FmKaba_lyricDuetLine){padding-right:15%}.amll-lyric-player.FmKaba_hasDuetLine .FmKaba_lyricDuetLine{padding-left:15%}.amll-lyric-player.FmKaba_hasDuetLine .FmKaba_lyricLineWrapper:has(.FmKaba_lyricDuetLine){align-items:flex-end}.amll-lyric-player.FmKaba_hasDuetLine .FmKaba_lyricLineWrapper:has(.FmKaba_lyricDuetLine) .FmKaba_bgWrapper{transform-origin:100% 0}.amll-lyric-player.FmKaba_hasDuetLine .FmKaba_lyricLineWrapper:has(.FmKaba_lyricDuetLine) .FmKaba_bgWrapperTop{transform-origin:100% 100%}:root{--amll-user-font-family:\"SF Pro Display\", \"PingFang SC\", system-ui, -apple-system, \"Segoe UI\", sans-serif;--amll-lp-font-family:var(--amll-user-font-family)}html,body{-webkit-font-smoothing:antialiased;background:0 0;width:100%;height:100%;overflow:hidden;margin:0!important;padding:0!important}#app{width:100%;height:100%;position:relative}.amll-lp-line{will-change:transform, opacity;pointer-events:auto;transform:translateZ(0)}.amll-lyric-player{pointer-events:auto;font-family:var(--amll-lp-font-family);--amll-lp-font-size:max(12px, min(8vw, 32px))}::-webkit-scrollbar{width:0;height:0}\n/*$vite$:1*/\n";document.head.appendChild(style);}})();
var so = Object.create, Xr = Object.defineProperty, io = Object.getOwnPropertyDescriptor, no = Object.getOwnPropertyNames, ao = Object.getPrototypeOf, Tn = Object.prototype.hasOwnProperty, rt = (e, t) => () => (t || (e((t = { exports: {} }).exports, t), e = null), t.exports), oo = (e, t) => {
  let r = {};
  for (var s in e)
    Xr(r, s, {
      get: e[s],
      enumerable: !0
    });
  return t || Xr(r, Symbol.toStringTag, { value: "Module" }), r;
}, ho = (e, t, r, s) => {
  if (t && typeof t == "object" || typeof t == "function")
    for (var i = no(t), n = 0, a = i.length, o; n < a; n++)
      o = i[n], !Tn.call(e, o) && o !== r && Xr(e, o, {
        get: ((h) => t[h]).bind(null, o),
        enumerable: !(s = io(t, o)) || s.enumerable
      });
  return e;
}, wn = (e, t, r) => (r = e != null ? so(ao(e)) : {}, ho(t || !e || !e.__esModule || !Tn.call(e, "default") ? Xr(r, "default", {
  value: e,
  enumerable: !0
}) : r, e));
var Tu = Math.PI / 180, wu = 180 / Math.PI, _i = new Float32Array([
  1,
  0,
  0,
  0,
  0,
  1,
  0,
  0,
  0,
  0,
  1,
  0,
  0,
  0,
  0,
  1
]), xt = class Rt extends Float32Array {
  static BYTE_LENGTH = 16 * Float32Array.BYTES_PER_ELEMENT;
  constructor(...t) {
    switch (t.length) {
      case 16:
        super(t);
        break;
      case 2:
        super(t[0], t[1], 16);
        break;
      case 1:
        const r = t[0];
        typeof r == "number" ? super([
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r,
          r
        ]) : super(r, 0, 16);
        break;
      default:
        super(_i);
    }
  }
  get str() {
    return Rt.str(this);
  }
  copy(t) {
    return this.set(t), this;
  }
  identity() {
    return this.set(_i), this;
  }
  multiply(t) {
    return Rt.multiply(this, this, t);
  }
  mul(t) {
    return this;
  }
  transpose() {
    return Rt.transpose(this, this);
  }
  invert() {
    return Rt.invert(this, this);
  }
  translate(t) {
    return Rt.translate(this, this, t);
  }
  rotate(t, r) {
    return Rt.rotate(this, this, t, r);
  }
  scale(t) {
    return Rt.scale(this, this, t);
  }
  rotateX(t) {
    return Rt.rotateX(this, this, t);
  }
  rotateY(t) {
    return Rt.rotateY(this, this, t);
  }
  rotateZ(t) {
    return Rt.rotateZ(this, this, t);
  }
  perspectiveNO(t, r, s, i) {
    return Rt.perspectiveNO(this, t, r, s, i);
  }
  perspectiveZO(t, r, s, i) {
    return Rt.perspectiveZO(this, t, r, s, i);
  }
  orthoNO(t, r, s, i, n, a) {
    return Rt.orthoNO(this, t, r, s, i, n, a);
  }
  orthoZO(t, r, s, i, n, a) {
    return Rt.orthoZO(this, t, r, s, i, n, a);
  }
  static create() {
    return new Rt();
  }
  static clone(t) {
    return new Rt(t);
  }
  static copy(t, r) {
    return t[0] = r[0], t[1] = r[1], t[2] = r[2], t[3] = r[3], t[4] = r[4], t[5] = r[5], t[6] = r[6], t[7] = r[7], t[8] = r[8], t[9] = r[9], t[10] = r[10], t[11] = r[11], t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15], t;
  }
  static fromValues(...t) {
    return new Rt(...t);
  }
  static set(t, ...r) {
    return t[0] = r[0], t[1] = r[1], t[2] = r[2], t[3] = r[3], t[4] = r[4], t[5] = r[5], t[6] = r[6], t[7] = r[7], t[8] = r[8], t[9] = r[9], t[10] = r[10], t[11] = r[11], t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15], t;
  }
  static identity(t) {
    return t[0] = 1, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = 1, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = 1, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static transpose(t, r) {
    if (t === r) {
      const s = r[1], i = r[2], n = r[3], a = r[6], o = r[7], h = r[11];
      t[1] = r[4], t[2] = r[8], t[3] = r[12], t[4] = s, t[6] = r[9], t[7] = r[13], t[8] = i, t[9] = a, t[11] = r[14], t[12] = n, t[13] = o, t[14] = h;
    } else
      t[0] = r[0], t[1] = r[4], t[2] = r[8], t[3] = r[12], t[4] = r[1], t[5] = r[5], t[6] = r[9], t[7] = r[13], t[8] = r[2], t[9] = r[6], t[10] = r[10], t[11] = r[14], t[12] = r[3], t[13] = r[7], t[14] = r[11], t[15] = r[15];
    return t;
  }
  static invert(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[3], o = r[4], h = r[5], l = r[6], c = r[7], u = r[8], d = r[9], y = r[10], m = r[11], v = r[12], p = r[13], x = r[14], f = r[15], E = s * h - i * o, g = s * l - n * o, A = s * c - a * o, L = i * l - n * h, M = i * c - a * h, C = n * c - a * l, k = u * p - d * v, B = u * x - y * v, w = u * f - m * v, F = d * x - y * p, P = d * f - m * p, G = y * f - m * x;
    let $ = E * G - g * P + A * F + L * w - M * B + C * k;
    return $ ? ($ = 1 / $, t[0] = (h * G - l * P + c * F) * $, t[1] = (n * P - i * G - a * F) * $, t[2] = (p * C - x * M + f * L) * $, t[3] = (y * M - d * C - m * L) * $, t[4] = (l * w - o * G - c * B) * $, t[5] = (s * G - n * w + a * B) * $, t[6] = (x * A - v * C - f * g) * $, t[7] = (u * C - y * A + m * g) * $, t[8] = (o * P - h * w + c * k) * $, t[9] = (i * w - s * P - a * k) * $, t[10] = (v * M - p * A + f * E) * $, t[11] = (d * A - u * M - m * E) * $, t[12] = (h * B - o * F - l * k) * $, t[13] = (s * F - i * B + n * k) * $, t[14] = (p * g - v * L - x * E) * $, t[15] = (u * L - d * g + y * E) * $, t) : null;
  }
  static adjoint(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[3], o = r[4], h = r[5], l = r[6], c = r[7], u = r[8], d = r[9], y = r[10], m = r[11], v = r[12], p = r[13], x = r[14], f = r[15], E = s * h - i * o, g = s * l - n * o, A = s * c - a * o, L = i * l - n * h, M = i * c - a * h, C = n * c - a * l, k = u * p - d * v, B = u * x - y * v, w = u * f - m * v, F = d * x - y * p, P = d * f - m * p, G = y * f - m * x;
    return t[0] = h * G - l * P + c * F, t[1] = n * P - i * G - a * F, t[2] = p * C - x * M + f * L, t[3] = y * M - d * C - m * L, t[4] = l * w - o * G - c * B, t[5] = s * G - n * w + a * B, t[6] = x * A - v * C - f * g, t[7] = u * C - y * A + m * g, t[8] = o * P - h * w + c * k, t[9] = i * w - s * P - a * k, t[10] = v * M - p * A + f * E, t[11] = d * A - u * M - m * E, t[12] = h * B - o * F - l * k, t[13] = s * F - i * B + n * k, t[14] = p * g - v * L - x * E, t[15] = u * L - d * g + y * E, t;
  }
  static determinant(t) {
    const r = t[0], s = t[1], i = t[2], n = t[3], a = t[4], o = t[5], h = t[6], l = t[7], c = t[8], u = t[9], d = t[10], y = t[11], m = t[12], v = t[13], p = t[14], x = t[15], f = r * o - s * a, E = r * h - i * a, g = s * h - i * o, A = c * v - u * m, L = c * p - d * m, M = u * p - d * v, C = r * M - s * L + i * A, k = a * M - o * L + h * A, B = c * g - u * E + d * f, w = m * g - v * E + p * f;
    return l * C - n * k + x * B - y * w;
  }
  static multiply(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = r[3], h = r[4], l = r[5], c = r[6], u = r[7], d = r[8], y = r[9], m = r[10], v = r[11], p = r[12], x = r[13], f = r[14], E = r[15];
    let g = s[0], A = s[1], L = s[2], M = s[3];
    return t[0] = g * i + A * h + L * d + M * p, t[1] = g * n + A * l + L * y + M * x, t[2] = g * a + A * c + L * m + M * f, t[3] = g * o + A * u + L * v + M * E, g = s[4], A = s[5], L = s[6], M = s[7], t[4] = g * i + A * h + L * d + M * p, t[5] = g * n + A * l + L * y + M * x, t[6] = g * a + A * c + L * m + M * f, t[7] = g * o + A * u + L * v + M * E, g = s[8], A = s[9], L = s[10], M = s[11], t[8] = g * i + A * h + L * d + M * p, t[9] = g * n + A * l + L * y + M * x, t[10] = g * a + A * c + L * m + M * f, t[11] = g * o + A * u + L * v + M * E, g = s[12], A = s[13], L = s[14], M = s[15], t[12] = g * i + A * h + L * d + M * p, t[13] = g * n + A * l + L * y + M * x, t[14] = g * a + A * c + L * m + M * f, t[15] = g * o + A * u + L * v + M * E, t;
  }
  static mul(t, r, s) {
    return t;
  }
  static translate(t, r, s) {
    const i = s[0], n = s[1], a = s[2];
    if (r === t)
      t[12] = r[0] * i + r[4] * n + r[8] * a + r[12], t[13] = r[1] * i + r[5] * n + r[9] * a + r[13], t[14] = r[2] * i + r[6] * n + r[10] * a + r[14], t[15] = r[3] * i + r[7] * n + r[11] * a + r[15];
    else {
      const o = r[0], h = r[1], l = r[2], c = r[3], u = r[4], d = r[5], y = r[6], m = r[7], v = r[8], p = r[9], x = r[10], f = r[11];
      t[0] = o, t[1] = h, t[2] = l, t[3] = c, t[4] = u, t[5] = d, t[6] = y, t[7] = m, t[8] = v, t[9] = p, t[10] = x, t[11] = f, t[12] = o * i + u * n + v * a + r[12], t[13] = h * i + d * n + p * a + r[13], t[14] = l * i + y * n + x * a + r[14], t[15] = c * i + m * n + f * a + r[15];
    }
    return t;
  }
  static scale(t, r, s) {
    const i = s[0], n = s[1], a = s[2];
    return t[0] = r[0] * i, t[1] = r[1] * i, t[2] = r[2] * i, t[3] = r[3] * i, t[4] = r[4] * n, t[5] = r[5] * n, t[6] = r[6] * n, t[7] = r[7] * n, t[8] = r[8] * a, t[9] = r[9] * a, t[10] = r[10] * a, t[11] = r[11] * a, t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15], t;
  }
  static rotate(t, r, s, i) {
    let n = i[0], a = i[1], o = i[2], h = Math.sqrt(n * n + a * a + o * o);
    if (h < 1e-6) return null;
    h = 1 / h, n *= h, a *= h, o *= h;
    const l = Math.sin(s), c = Math.cos(s), u = 1 - c, d = r[0], y = r[1], m = r[2], v = r[3], p = r[4], x = r[5], f = r[6], E = r[7], g = r[8], A = r[9], L = r[10], M = r[11], C = n * n * u + c, k = a * n * u + o * l, B = o * n * u - a * l, w = n * a * u - o * l, F = a * a * u + c, P = o * a * u + n * l, G = n * o * u + a * l, $ = a * o * u - n * l, H = o * o * u + c;
    return t[0] = d * C + p * k + g * B, t[1] = y * C + x * k + A * B, t[2] = m * C + f * k + L * B, t[3] = v * C + E * k + M * B, t[4] = d * w + p * F + g * P, t[5] = y * w + x * F + A * P, t[6] = m * w + f * F + L * P, t[7] = v * w + E * F + M * P, t[8] = d * G + p * $ + g * H, t[9] = y * G + x * $ + A * H, t[10] = m * G + f * $ + L * H, t[11] = v * G + E * $ + M * H, r !== t && (t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15]), t;
  }
  static rotateX(t, r, s) {
    let i = Math.sin(s), n = Math.cos(s), a = r[4], o = r[5], h = r[6], l = r[7], c = r[8], u = r[9], d = r[10], y = r[11];
    return r !== t && (t[0] = r[0], t[1] = r[1], t[2] = r[2], t[3] = r[3], t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15]), t[4] = a * n + c * i, t[5] = o * n + u * i, t[6] = h * n + d * i, t[7] = l * n + y * i, t[8] = c * n - a * i, t[9] = u * n - o * i, t[10] = d * n - h * i, t[11] = y * n - l * i, t;
  }
  static rotateY(t, r, s) {
    let i = Math.sin(s), n = Math.cos(s), a = r[0], o = r[1], h = r[2], l = r[3], c = r[8], u = r[9], d = r[10], y = r[11];
    return r !== t && (t[4] = r[4], t[5] = r[5], t[6] = r[6], t[7] = r[7], t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15]), t[0] = a * n - c * i, t[1] = o * n - u * i, t[2] = h * n - d * i, t[3] = l * n - y * i, t[8] = a * i + c * n, t[9] = o * i + u * n, t[10] = h * i + d * n, t[11] = l * i + y * n, t;
  }
  static rotateZ(t, r, s) {
    let i = Math.sin(s), n = Math.cos(s), a = r[0], o = r[1], h = r[2], l = r[3], c = r[4], u = r[5], d = r[6], y = r[7];
    return r !== t && (t[8] = r[8], t[9] = r[9], t[10] = r[10], t[11] = r[11], t[12] = r[12], t[13] = r[13], t[14] = r[14], t[15] = r[15]), t[0] = a * n + c * i, t[1] = o * n + u * i, t[2] = h * n + d * i, t[3] = l * n + y * i, t[4] = c * n - a * i, t[5] = u * n - o * i, t[6] = d * n - h * i, t[7] = y * n - l * i, t;
  }
  static fromTranslation(t, r) {
    return t[0] = 1, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = 1, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = 1, t[11] = 0, t[12] = r[0], t[13] = r[1], t[14] = r[2], t[15] = 1, t;
  }
  static fromScaling(t, r) {
    return t[0] = r[0], t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = r[1], t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = r[2], t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static fromRotation(t, r, s) {
    let i = s[0], n = s[1], a = s[2], o = Math.sqrt(i * i + n * n + a * a);
    if (o < 1e-6) return null;
    o = 1 / o, i *= o, n *= o, a *= o;
    const h = Math.sin(r), l = Math.cos(r), c = 1 - l;
    return t[0] = i * i * c + l, t[1] = n * i * c + a * h, t[2] = a * i * c - n * h, t[3] = 0, t[4] = i * n * c - a * h, t[5] = n * n * c + l, t[6] = a * n * c + i * h, t[7] = 0, t[8] = i * a * c + n * h, t[9] = n * a * c - i * h, t[10] = a * a * c + l, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static fromXRotation(t, r) {
    let s = Math.sin(r), i = Math.cos(r);
    return t[0] = 1, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = i, t[6] = s, t[7] = 0, t[8] = 0, t[9] = -s, t[10] = i, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static fromYRotation(t, r) {
    let s = Math.sin(r), i = Math.cos(r);
    return t[0] = i, t[1] = 0, t[2] = -s, t[3] = 0, t[4] = 0, t[5] = 1, t[6] = 0, t[7] = 0, t[8] = s, t[9] = 0, t[10] = i, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static fromZRotation(t, r) {
    const s = Math.sin(r), i = Math.cos(r);
    return t[0] = i, t[1] = s, t[2] = 0, t[3] = 0, t[4] = -s, t[5] = i, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = 1, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static fromRotationTranslation(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = r[3], h = i + i, l = n + n, c = a + a, u = i * h, d = i * l, y = i * c, m = n * l, v = n * c, p = a * c, x = o * h, f = o * l, E = o * c;
    return t[0] = 1 - (m + p), t[1] = d + E, t[2] = y - f, t[3] = 0, t[4] = d - E, t[5] = 1 - (u + p), t[6] = v + x, t[7] = 0, t[8] = y + f, t[9] = v - x, t[10] = 1 - (u + m), t[11] = 0, t[12] = s[0], t[13] = s[1], t[14] = s[2], t[15] = 1, t;
  }
  static fromQuat2(t, r) {
    const s = -r[0], i = -r[1], n = -r[2], a = r[3], o = r[4], h = r[5], l = r[6], c = r[7];
    let u = s * s + i * i + n * n + a * a;
    return u > 0 ? (Vt[0] = (o * a + c * s + h * n - l * i) * 2 / u, Vt[1] = (h * a + c * i + l * s - o * n) * 2 / u, Vt[2] = (l * a + c * n + o * i - h * s) * 2 / u) : (Vt[0] = (o * a + c * s + h * n - l * i) * 2, Vt[1] = (h * a + c * i + l * s - o * n) * 2, Vt[2] = (l * a + c * n + o * i - h * s) * 2), Rt.fromRotationTranslation(t, r, Vt), t;
  }
  static normalFromMat4(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[3], o = r[4], h = r[5], l = r[6], c = r[7], u = r[8], d = r[9], y = r[10], m = r[11], v = r[12], p = r[13], x = r[14], f = r[15], E = s * h - i * o, g = s * l - n * o, A = s * c - a * o, L = i * l - n * h, M = i * c - a * h, C = n * c - a * l, k = u * p - d * v, B = u * x - y * v, w = u * f - m * v, F = d * x - y * p, P = d * f - m * p, G = y * f - m * x;
    let $ = E * G - g * P + A * F + L * w - M * B + C * k;
    return $ ? ($ = 1 / $, t[0] = (h * G - l * P + c * F) * $, t[1] = (l * w - o * G - c * B) * $, t[2] = (o * P - h * w + c * k) * $, t[3] = 0, t[4] = (n * P - i * G - a * F) * $, t[5] = (s * G - n * w + a * B) * $, t[6] = (i * w - s * P - a * k) * $, t[7] = 0, t[8] = (p * C - x * M + f * L) * $, t[9] = (x * A - v * C - f * g) * $, t[10] = (v * M - p * A + f * E) * $, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t) : null;
  }
  static normalFromMat4Fast(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[4], o = r[5], h = r[6], l = r[8], c = r[9], u = r[10];
    return t[0] = o * u - u * c, t[1] = h * l - l * u, t[2] = a * c - c * l, t[3] = 0, t[4] = c * n - u * i, t[5] = u * s - l * n, t[6] = l * i - c * s, t[7] = 0, t[8] = i * h - n * o, t[9] = n * a - s * h, t[10] = s * o - i * a, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static getTranslation(t, r) {
    return t[0] = r[12], t[1] = r[13], t[2] = r[14], t;
  }
  static getScaling(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[4], o = r[5], h = r[6], l = r[8], c = r[9], u = r[10];
    return t[0] = Math.sqrt(s * s + i * i + n * n), t[1] = Math.sqrt(a * a + o * o + h * h), t[2] = Math.sqrt(l * l + c * c + u * u), t;
  }
  static getRotation(t, r) {
    Rt.getScaling(Vt, r);
    const s = 1 / Vt[0], i = 1 / Vt[1], n = 1 / Vt[2], a = r[0] * s, o = r[1] * i, h = r[2] * n, l = r[4] * s, c = r[5] * i, u = r[6] * n, d = r[8] * s, y = r[9] * i, m = r[10] * n, v = a + c + m;
    let p = 0;
    return v > 0 ? (p = Math.sqrt(v + 1) * 2, t[3] = 0.25 * p, t[0] = (u - y) / p, t[1] = (d - h) / p, t[2] = (o - l) / p) : a > c && a > m ? (p = Math.sqrt(1 + a - c - m) * 2, t[3] = (u - y) / p, t[0] = 0.25 * p, t[1] = (o + l) / p, t[2] = (d + h) / p) : c > m ? (p = Math.sqrt(1 + c - a - m) * 2, t[3] = (d - h) / p, t[0] = (o + l) / p, t[1] = 0.25 * p, t[2] = (u + y) / p) : (p = Math.sqrt(1 + m - a - c) * 2, t[3] = (o - l) / p, t[0] = (d + h) / p, t[1] = (u + y) / p, t[2] = 0.25 * p), t;
  }
  static decompose(t, r, s, i) {
    r[0] = i[12], r[1] = i[13], r[2] = i[14];
    const n = i[0], a = i[1], o = i[2], h = i[4], l = i[5], c = i[6], u = i[8], d = i[9], y = i[10];
    s[0] = Math.sqrt(n * n + a * a + o * o), s[1] = Math.sqrt(h * h + l * l + c * c), s[2] = Math.sqrt(u * u + d * d + y * y);
    const m = 1 / s[0], v = 1 / s[1], p = 1 / s[2], x = n * m, f = a * v, E = o * p, g = h * m, A = l * v, L = c * p, M = u * m, C = d * v, k = y * p, B = x + A + k;
    let w = 0;
    return B > 0 ? (w = Math.sqrt(B + 1) * 2, t[3] = 0.25 * w, t[0] = (L - C) / w, t[1] = (M - E) / w, t[2] = (f - g) / w) : x > A && x > k ? (w = Math.sqrt(1 + x - A - k) * 2, t[3] = (L - C) / w, t[0] = 0.25 * w, t[1] = (f + g) / w, t[2] = (M + E) / w) : A > k ? (w = Math.sqrt(1 + A - x - k) * 2, t[3] = (M - E) / w, t[0] = (f + g) / w, t[1] = 0.25 * w, t[2] = (L + C) / w) : (w = Math.sqrt(1 + k - x - A) * 2, t[3] = (f - g) / w, t[0] = (M + E) / w, t[1] = (L + C) / w, t[2] = 0.25 * w), t;
  }
  static fromRotationTranslationScale(t, r, s, i) {
    const n = r[0], a = r[1], o = r[2], h = r[3], l = n + n, c = a + a, u = o + o, d = n * l, y = n * c, m = n * u, v = a * c, p = a * u, x = o * u, f = h * l, E = h * c, g = h * u, A = i[0], L = i[1], M = i[2];
    return t[0] = (1 - (v + x)) * A, t[1] = (y + g) * A, t[2] = (m - E) * A, t[3] = 0, t[4] = (y - g) * L, t[5] = (1 - (d + x)) * L, t[6] = (p + f) * L, t[7] = 0, t[8] = (m + E) * M, t[9] = (p - f) * M, t[10] = (1 - (d + v)) * M, t[11] = 0, t[12] = s[0], t[13] = s[1], t[14] = s[2], t[15] = 1, t;
  }
  static fromRotationTranslationScaleOrigin(t, r, s, i, n) {
    const a = r[0], o = r[1], h = r[2], l = r[3], c = a + a, u = o + o, d = h + h, y = a * c, m = a * u, v = a * d, p = o * u, x = o * d, f = h * d, E = l * c, g = l * u, A = l * d, L = i[0], M = i[1], C = i[2], k = n[0], B = n[1], w = n[2], F = (1 - (p + f)) * L, P = (m + A) * L, G = (v - g) * L, $ = (m - A) * M, H = (1 - (y + f)) * M, Q = (x + E) * M, _ = (v + g) * C, T = (x - E) * C, b = (1 - (y + p)) * C;
    return t[0] = F, t[1] = P, t[2] = G, t[3] = 0, t[4] = $, t[5] = H, t[6] = Q, t[7] = 0, t[8] = _, t[9] = T, t[10] = b, t[11] = 0, t[12] = s[0] + k - (F * k + $ * B + _ * w), t[13] = s[1] + B - (P * k + H * B + T * w), t[14] = s[2] + w - (G * k + Q * B + b * w), t[15] = 1, t;
  }
  static fromQuat(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[3], o = s + s, h = i + i, l = n + n, c = s * o, u = i * o, d = i * h, y = n * o, m = n * h, v = n * l, p = a * o, x = a * h, f = a * l;
    return t[0] = 1 - d - v, t[1] = u + f, t[2] = y - x, t[3] = 0, t[4] = u - f, t[5] = 1 - c - v, t[6] = m + p, t[7] = 0, t[8] = y + x, t[9] = m - p, t[10] = 1 - c - d, t[11] = 0, t[12] = 0, t[13] = 0, t[14] = 0, t[15] = 1, t;
  }
  static frustumNO(t, r, s, i, n, a, o = 1 / 0) {
    const h = 1 / (s - r), l = 1 / (n - i);
    if (t[0] = a * 2 * h, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = a * 2 * l, t[6] = 0, t[7] = 0, t[8] = (s + r) * h, t[9] = (n + i) * l, t[11] = -1, t[12] = 0, t[13] = 0, t[15] = 0, o != null && o !== 1 / 0) {
      const c = 1 / (a - o);
      t[10] = (o + a) * c, t[14] = 2 * o * a * c;
    } else
      t[10] = -1, t[14] = -2 * a;
    return t;
  }
  static frustum(t, r, s, i, n, a, o = 1 / 0) {
    return t;
  }
  static frustumZO(t, r, s, i, n, a, o = 1 / 0) {
    const h = 1 / (s - r), l = 1 / (n - i);
    if (t[0] = a * 2 * h, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = a * 2 * l, t[6] = 0, t[7] = 0, t[8] = (s + r) * h, t[9] = (n + i) * l, t[11] = -1, t[12] = 0, t[13] = 0, t[15] = 0, o != null && o !== 1 / 0) {
      const c = 1 / (a - o);
      t[10] = o * c, t[14] = o * a * c;
    } else
      t[10] = -1, t[14] = -a;
    return t;
  }
  static perspectiveNO(t, r, s, i, n = 1 / 0) {
    const a = 1 / Math.tan(r / 2);
    if (t[0] = a / s, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = a, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[11] = -1, t[12] = 0, t[13] = 0, t[15] = 0, n != null && n !== 1 / 0) {
      const o = 1 / (i - n);
      t[10] = (n + i) * o, t[14] = 2 * n * i * o;
    } else
      t[10] = -1, t[14] = -2 * i;
    return t;
  }
  static perspective(t, r, s, i, n = 1 / 0) {
    return t;
  }
  static perspectiveZO(t, r, s, i, n = 1 / 0) {
    const a = 1 / Math.tan(r / 2);
    if (t[0] = a / s, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = a, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[11] = -1, t[12] = 0, t[13] = 0, t[15] = 0, n != null && n !== 1 / 0) {
      const o = 1 / (i - n);
      t[10] = n * o, t[14] = n * i * o;
    } else
      t[10] = -1, t[14] = -i;
    return t;
  }
  static perspectiveFromFieldOfView(t, r, s, i) {
    const n = Math.tan(r.upDegrees * Math.PI / 180), a = Math.tan(r.downDegrees * Math.PI / 180), o = Math.tan(r.leftDegrees * Math.PI / 180), h = Math.tan(r.rightDegrees * Math.PI / 180), l = 2 / (o + h), c = 2 / (n + a);
    return t[0] = l, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = c, t[6] = 0, t[7] = 0, t[8] = -((o - h) * l * 0.5), t[9] = (n - a) * c * 0.5, t[10] = i / (s - i), t[11] = -1, t[12] = 0, t[13] = 0, t[14] = i * s / (s - i), t[15] = 0, t;
  }
  static orthoNO(t, r, s, i, n, a, o) {
    const h = 1 / (r - s), l = 1 / (i - n), c = 1 / (a - o);
    return t[0] = -2 * h, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = -2 * l, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = 2 * c, t[11] = 0, t[12] = (r + s) * h, t[13] = (n + i) * l, t[14] = (o + a) * c, t[15] = 1, t;
  }
  static ortho(t, r, s, i, n, a, o) {
    return t;
  }
  static orthoZO(t, r, s, i, n, a, o) {
    const h = 1 / (r - s), l = 1 / (i - n), c = 1 / (a - o);
    return t[0] = -2 * h, t[1] = 0, t[2] = 0, t[3] = 0, t[4] = 0, t[5] = -2 * l, t[6] = 0, t[7] = 0, t[8] = 0, t[9] = 0, t[10] = c, t[11] = 0, t[12] = (r + s) * h, t[13] = (n + i) * l, t[14] = a * c, t[15] = 1, t;
  }
  static lookAt(t, r, s, i) {
    const n = r[0], a = r[1], o = r[2], h = i[0], l = i[1], c = i[2], u = s[0], d = s[1], y = s[2];
    if (Math.abs(n - u) < 1e-6 && Math.abs(a - d) < 1e-6 && Math.abs(o - y) < 1e-6) return Rt.identity(t);
    let m = n - u, v = a - d, p = o - y, x = 1 / Math.sqrt(m * m + v * v + p * p);
    m *= x, v *= x, p *= x;
    let f = l * p - c * v, E = c * m - h * p, g = h * v - l * m;
    x = Math.sqrt(f * f + E * E + g * g), x ? (x = 1 / x, f *= x, E *= x, g *= x) : (f = 0, E = 0, g = 0);
    let A = v * g - p * E, L = p * f - m * g, M = m * E - v * f;
    return x = Math.sqrt(A * A + L * L + M * M), x ? (x = 1 / x, A *= x, L *= x, M *= x) : (A = 0, L = 0, M = 0), t[0] = f, t[1] = A, t[2] = m, t[3] = 0, t[4] = E, t[5] = L, t[6] = v, t[7] = 0, t[8] = g, t[9] = M, t[10] = p, t[11] = 0, t[12] = -(f * n + E * a + g * o), t[13] = -(A * n + L * a + M * o), t[14] = -(m * n + v * a + p * o), t[15] = 1, t;
  }
  static targetTo(t, r, s, i) {
    const n = r[0], a = r[1], o = r[2], h = i[0], l = i[1], c = i[2];
    let u = n - s[0], d = a - s[1], y = o - s[2], m = u * u + d * d + y * y;
    m > 0 && (m = 1 / Math.sqrt(m), u *= m, d *= m, y *= m);
    let v = l * y - c * d, p = c * u - h * y, x = h * d - l * u;
    return m = v * v + p * p + x * x, m > 0 && (m = 1 / Math.sqrt(m), v *= m, p *= m, x *= m), t[0] = v, t[1] = p, t[2] = x, t[3] = 0, t[4] = d * x - y * p, t[5] = y * v - u * x, t[6] = u * p - d * v, t[7] = 0, t[8] = u, t[9] = d, t[10] = y, t[11] = 0, t[12] = n, t[13] = a, t[14] = o, t[15] = 1, t;
  }
  static frob(t) {
    return Math.sqrt(t[0] * t[0] + t[1] * t[1] + t[2] * t[2] + t[3] * t[3] + t[4] * t[4] + t[5] * t[5] + t[6] * t[6] + t[7] * t[7] + t[8] * t[8] + t[9] * t[9] + t[10] * t[10] + t[11] * t[11] + t[12] * t[12] + t[13] * t[13] + t[14] * t[14] + t[15] * t[15]);
  }
  static add(t, r, s) {
    return t[0] = r[0] + s[0], t[1] = r[1] + s[1], t[2] = r[2] + s[2], t[3] = r[3] + s[3], t[4] = r[4] + s[4], t[5] = r[5] + s[5], t[6] = r[6] + s[6], t[7] = r[7] + s[7], t[8] = r[8] + s[8], t[9] = r[9] + s[9], t[10] = r[10] + s[10], t[11] = r[11] + s[11], t[12] = r[12] + s[12], t[13] = r[13] + s[13], t[14] = r[14] + s[14], t[15] = r[15] + s[15], t;
  }
  static subtract(t, r, s) {
    return t[0] = r[0] - s[0], t[1] = r[1] - s[1], t[2] = r[2] - s[2], t[3] = r[3] - s[3], t[4] = r[4] - s[4], t[5] = r[5] - s[5], t[6] = r[6] - s[6], t[7] = r[7] - s[7], t[8] = r[8] - s[8], t[9] = r[9] - s[9], t[10] = r[10] - s[10], t[11] = r[11] - s[11], t[12] = r[12] - s[12], t[13] = r[13] - s[13], t[14] = r[14] - s[14], t[15] = r[15] - s[15], t;
  }
  static sub(t, r, s) {
    return t;
  }
  static multiplyScalar(t, r, s) {
    return t[0] = r[0] * s, t[1] = r[1] * s, t[2] = r[2] * s, t[3] = r[3] * s, t[4] = r[4] * s, t[5] = r[5] * s, t[6] = r[6] * s, t[7] = r[7] * s, t[8] = r[8] * s, t[9] = r[9] * s, t[10] = r[10] * s, t[11] = r[11] * s, t[12] = r[12] * s, t[13] = r[13] * s, t[14] = r[14] * s, t[15] = r[15] * s, t;
  }
  static multiplyScalarAndAdd(t, r, s, i) {
    return t[0] = r[0] + s[0] * i, t[1] = r[1] + s[1] * i, t[2] = r[2] + s[2] * i, t[3] = r[3] + s[3] * i, t[4] = r[4] + s[4] * i, t[5] = r[5] + s[5] * i, t[6] = r[6] + s[6] * i, t[7] = r[7] + s[7] * i, t[8] = r[8] + s[8] * i, t[9] = r[9] + s[9] * i, t[10] = r[10] + s[10] * i, t[11] = r[11] + s[11] * i, t[12] = r[12] + s[12] * i, t[13] = r[13] + s[13] * i, t[14] = r[14] + s[14] * i, t[15] = r[15] + s[15] * i, t;
  }
  static exactEquals(t, r) {
    return t[0] === r[0] && t[1] === r[1] && t[2] === r[2] && t[3] === r[3] && t[4] === r[4] && t[5] === r[5] && t[6] === r[6] && t[7] === r[7] && t[8] === r[8] && t[9] === r[9] && t[10] === r[10] && t[11] === r[11] && t[12] === r[12] && t[13] === r[13] && t[14] === r[14] && t[15] === r[15];
  }
  static equals(t, r) {
    const s = t[0], i = t[1], n = t[2], a = t[3], o = t[4], h = t[5], l = t[6], c = t[7], u = t[8], d = t[9], y = t[10], m = t[11], v = t[12], p = t[13], x = t[14], f = t[15], E = r[0], g = r[1], A = r[2], L = r[3], M = r[4], C = r[5], k = r[6], B = r[7], w = r[8], F = r[9], P = r[10], G = r[11], $ = r[12], H = r[13], Q = r[14], _ = r[15];
    return Math.abs(s - E) <= 1e-6 * Math.max(1, Math.abs(s), Math.abs(E)) && Math.abs(i - g) <= 1e-6 * Math.max(1, Math.abs(i), Math.abs(g)) && Math.abs(n - A) <= 1e-6 * Math.max(1, Math.abs(n), Math.abs(A)) && Math.abs(a - L) <= 1e-6 * Math.max(1, Math.abs(a), Math.abs(L)) && Math.abs(o - M) <= 1e-6 * Math.max(1, Math.abs(o), Math.abs(M)) && Math.abs(h - C) <= 1e-6 * Math.max(1, Math.abs(h), Math.abs(C)) && Math.abs(l - k) <= 1e-6 * Math.max(1, Math.abs(l), Math.abs(k)) && Math.abs(c - B) <= 1e-6 * Math.max(1, Math.abs(c), Math.abs(B)) && Math.abs(u - w) <= 1e-6 * Math.max(1, Math.abs(u), Math.abs(w)) && Math.abs(d - F) <= 1e-6 * Math.max(1, Math.abs(d), Math.abs(F)) && Math.abs(y - P) <= 1e-6 * Math.max(1, Math.abs(y), Math.abs(P)) && Math.abs(m - G) <= 1e-6 * Math.max(1, Math.abs(m), Math.abs(G)) && Math.abs(v - $) <= 1e-6 * Math.max(1, Math.abs(v), Math.abs($)) && Math.abs(p - H) <= 1e-6 * Math.max(1, Math.abs(p), Math.abs(H)) && Math.abs(x - Q) <= 1e-6 * Math.max(1, Math.abs(x), Math.abs(Q)) && Math.abs(f - _) <= 1e-6 * Math.max(1, Math.abs(f), Math.abs(_));
  }
  static str(t) {
    return `Mat4(${t.join(", ")})`;
  }
}, Vt = /* @__PURE__ */ new Float32Array(3);
xt.prototype.mul = xt.prototype.multiply;
xt.sub = xt.subtract;
xt.mul = xt.multiply;
xt.frustum = xt.frustumNO;
xt.perspective = xt.perspectiveNO;
xt.ortho = xt.orthoNO;
var gt = class qt extends Float32Array {
  static BYTE_LENGTH = 3 * Float32Array.BYTES_PER_ELEMENT;
  constructor(...t) {
    switch (t.length) {
      case 3:
        super(t);
        break;
      case 2:
        super(t[0], t[1], 3);
        break;
      case 1: {
        const r = t[0];
        typeof r == "number" ? super([
          r,
          r,
          r
        ]) : super(r, 0, 3);
        break;
      }
      default:
        super(3);
    }
  }
  get x() {
    return this[0];
  }
  set x(t) {
    this[0] = t;
  }
  get y() {
    return this[1];
  }
  set y(t) {
    this[1] = t;
  }
  get z() {
    return this[2];
  }
  set z(t) {
    this[2] = t;
  }
  get r() {
    return this[0];
  }
  set r(t) {
    this[0] = t;
  }
  get g() {
    return this[1];
  }
  set g(t) {
    this[1] = t;
  }
  get b() {
    return this[2];
  }
  set b(t) {
    this[2] = t;
  }
  get magnitude() {
    const t = this[0], r = this[1], s = this[2];
    return Math.sqrt(t * t + r * r + s * s);
  }
  get mag() {
    return this.magnitude;
  }
  get squaredMagnitude() {
    const t = this[0], r = this[1], s = this[2];
    return t * t + r * r + s * s;
  }
  get sqrMag() {
    return this.squaredMagnitude;
  }
  get str() {
    return qt.str(this);
  }
  copy(t) {
    return this.set(t), this;
  }
  add(t) {
    return this[0] += t[0], this[1] += t[1], this[2] += t[2], this;
  }
  subtract(t) {
    return this[0] -= t[0], this[1] -= t[1], this[2] -= t[2], this;
  }
  sub(t) {
    return this;
  }
  multiply(t) {
    return this[0] *= t[0], this[1] *= t[1], this[2] *= t[2], this;
  }
  mul(t) {
    return this;
  }
  divide(t) {
    return this[0] /= t[0], this[1] /= t[1], this[2] /= t[2], this;
  }
  div(t) {
    return this;
  }
  scale(t) {
    return this[0] *= t, this[1] *= t, this[2] *= t, this;
  }
  scaleAndAdd(t, r) {
    return this[0] += t[0] * r, this[1] += t[1] * r, this[2] += t[2] * r, this;
  }
  distance(t) {
    return qt.distance(this, t);
  }
  dist(t) {
    return 0;
  }
  squaredDistance(t) {
    return qt.squaredDistance(this, t);
  }
  sqrDist(t) {
    return 0;
  }
  negate() {
    return this[0] *= -1, this[1] *= -1, this[2] *= -1, this;
  }
  invert() {
    return this[0] = 1 / this[0], this[1] = 1 / this[1], this[2] = 1 / this[2], this;
  }
  abs() {
    return this[0] = Math.abs(this[0]), this[1] = Math.abs(this[1]), this[2] = Math.abs(this[2]), this;
  }
  dot(t) {
    return this[0] * t[0] + this[1] * t[1] + this[2] * t[2];
  }
  normalize() {
    return qt.normalize(this, this);
  }
  static create() {
    return new qt();
  }
  static clone(t) {
    return new qt(t);
  }
  static magnitude(t) {
    let r = t[0], s = t[1], i = t[2];
    return Math.sqrt(r * r + s * s + i * i);
  }
  static mag(t) {
    return 0;
  }
  static length(t) {
    return 0;
  }
  static len(t) {
    return 0;
  }
  static fromValues(t, r, s) {
    return new qt(t, r, s);
  }
  static copy(t, r) {
    return t[0] = r[0], t[1] = r[1], t[2] = r[2], t;
  }
  static set(t, r, s, i) {
    return t[0] = r, t[1] = s, t[2] = i, t;
  }
  static add(t, r, s) {
    return t[0] = r[0] + s[0], t[1] = r[1] + s[1], t[2] = r[2] + s[2], t;
  }
  static subtract(t, r, s) {
    return t[0] = r[0] - s[0], t[1] = r[1] - s[1], t[2] = r[2] - s[2], t;
  }
  static sub(t, r, s) {
    return [
      0,
      0,
      0
    ];
  }
  static multiply(t, r, s) {
    return t[0] = r[0] * s[0], t[1] = r[1] * s[1], t[2] = r[2] * s[2], t;
  }
  static mul(t, r, s) {
    return [
      0,
      0,
      0
    ];
  }
  static divide(t, r, s) {
    return t[0] = r[0] / s[0], t[1] = r[1] / s[1], t[2] = r[2] / s[2], t;
  }
  static div(t, r, s) {
    return [
      0,
      0,
      0
    ];
  }
  static ceil(t, r) {
    return t[0] = Math.ceil(r[0]), t[1] = Math.ceil(r[1]), t[2] = Math.ceil(r[2]), t;
  }
  static floor(t, r) {
    return t[0] = Math.floor(r[0]), t[1] = Math.floor(r[1]), t[2] = Math.floor(r[2]), t;
  }
  static min(t, r, s) {
    return t[0] = Math.min(r[0], s[0]), t[1] = Math.min(r[1], s[1]), t[2] = Math.min(r[2], s[2]), t;
  }
  static max(t, r, s) {
    return t[0] = Math.max(r[0], s[0]), t[1] = Math.max(r[1], s[1]), t[2] = Math.max(r[2], s[2]), t;
  }
  static scale(t, r, s) {
    return t[0] = r[0] * s, t[1] = r[1] * s, t[2] = r[2] * s, t;
  }
  static scaleAndAdd(t, r, s, i) {
    return t[0] = r[0] + s[0] * i, t[1] = r[1] + s[1] * i, t[2] = r[2] + s[2] * i, t;
  }
  static distance(t, r) {
    const s = r[0] - t[0], i = r[1] - t[1], n = r[2] - t[2];
    return Math.sqrt(s * s + i * i + n * n);
  }
  static dist(t, r) {
    return 0;
  }
  static squaredDistance(t, r) {
    const s = r[0] - t[0], i = r[1] - t[1], n = r[2] - t[2];
    return s * s + i * i + n * n;
  }
  static sqrDist(t, r) {
    return 0;
  }
  static squaredLength(t) {
    const r = t[0], s = t[1], i = t[2];
    return r * r + s * s + i * i;
  }
  static sqrLen(t, r) {
    return 0;
  }
  static negate(t, r) {
    return t[0] = -r[0], t[1] = -r[1], t[2] = -r[2], t;
  }
  static inverse(t, r) {
    return t[0] = 1 / r[0], t[1] = 1 / r[1], t[2] = 1 / r[2], t;
  }
  static abs(t, r) {
    return t[0] = Math.abs(r[0]), t[1] = Math.abs(r[1]), t[2] = Math.abs(r[2]), t;
  }
  static normalize(t, r) {
    const s = r[0], i = r[1], n = r[2];
    let a = s * s + i * i + n * n;
    return a > 0 && (a = 1 / Math.sqrt(a)), t[0] = r[0] * a, t[1] = r[1] * a, t[2] = r[2] * a, t;
  }
  static dot(t, r) {
    return t[0] * r[0] + t[1] * r[1] + t[2] * r[2];
  }
  static cross(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = s[0], h = s[1], l = s[2];
    return t[0] = n * l - a * h, t[1] = a * o - i * l, t[2] = i * h - n * o, t;
  }
  static lerp(t, r, s, i) {
    const n = r[0], a = r[1], o = r[2];
    return t[0] = n + i * (s[0] - n), t[1] = a + i * (s[1] - a), t[2] = o + i * (s[2] - o), t;
  }
  static slerp(t, r, s, i) {
    const n = Math.acos(Math.min(Math.max(qt.dot(r, s), -1), 1)), a = Math.sin(n), o = Math.sin((1 - i) * n) / a, h = Math.sin(i * n) / a;
    return t[0] = o * r[0] + h * s[0], t[1] = o * r[1] + h * s[1], t[2] = o * r[2] + h * s[2], t;
  }
  static hermite(t, r, s, i, n, a) {
    const o = a * a, h = o * (2 * a - 3) + 1, l = o * (a - 2) + a, c = o * (a - 1), u = o * (3 - 2 * a);
    return t[0] = r[0] * h + s[0] * l + i[0] * c + n[0] * u, t[1] = r[1] * h + s[1] * l + i[1] * c + n[1] * u, t[2] = r[2] * h + s[2] * l + i[2] * c + n[2] * u, t;
  }
  static bezier(t, r, s, i, n, a) {
    const o = 1 - a, h = o * o, l = a * a, c = h * o, u = 3 * a * h, d = 3 * l * o, y = l * a;
    return t[0] = r[0] * c + s[0] * u + i[0] * d + n[0] * y, t[1] = r[1] * c + s[1] * u + i[1] * d + n[1] * y, t[2] = r[2] * c + s[2] * u + i[2] * d + n[2] * y, t;
  }
  static transformMat4(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = s[3] * i + s[7] * n + s[11] * a + s[15] || 1;
    return t[0] = (s[0] * i + s[4] * n + s[8] * a + s[12]) / o, t[1] = (s[1] * i + s[5] * n + s[9] * a + s[13]) / o, t[2] = (s[2] * i + s[6] * n + s[10] * a + s[14]) / o, t;
  }
  static transformMat3(t, r, s) {
    let i = r[0], n = r[1], a = r[2];
    return t[0] = i * s[0] + n * s[3] + a * s[6], t[1] = i * s[1] + n * s[4] + a * s[7], t[2] = i * s[2] + n * s[5] + a * s[8], t;
  }
  static transformQuat(t, r, s) {
    const i = s[0], n = s[1], a = s[2], o = s[3] * 2, h = r[0], l = r[1], c = r[2], u = n * c - a * l, d = a * h - i * c, y = i * l - n * h, m = (n * y - a * d) * 2, v = (a * u - i * y) * 2, p = (i * d - n * u) * 2;
    return t[0] = h + u * o + m, t[1] = l + d * o + v, t[2] = c + y * o + p, t;
  }
  static rotateX(t, r, s, i) {
    const n = s[1], a = s[2], o = r[1] - n, h = r[2] - a;
    return t[0] = r[0], t[1] = o * Math.cos(i) - h * Math.sin(i) + n, t[2] = o * Math.sin(i) + h * Math.cos(i) + a, t;
  }
  static rotateY(t, r, s, i) {
    const n = s[0], a = s[2], o = r[0] - n, h = r[2] - a;
    return t[0] = h * Math.sin(i) + o * Math.cos(i) + n, t[1] = r[1], t[2] = h * Math.cos(i) - o * Math.sin(i) + a, t;
  }
  static rotateZ(t, r, s, i) {
    const n = s[0], a = s[1], o = r[0] - n, h = r[1] - a;
    return t[0] = o * Math.cos(i) - h * Math.sin(i) + n, t[1] = o * Math.sin(i) + h * Math.cos(i) + a, t[2] = s[2], t;
  }
  static angle(t, r) {
    const s = t[0], i = t[1], n = t[2], a = r[0], o = r[1], h = r[2], l = Math.sqrt((s * s + i * i + n * n) * (a * a + o * o + h * h)), c = l && qt.dot(t, r) / l;
    return Math.acos(Math.min(Math.max(c, -1), 1));
  }
  static zero(t) {
    return t[0] = 0, t[1] = 0, t[2] = 0, t;
  }
  static str(t) {
    return `Vec3(${t.join(", ")})`;
  }
  static exactEquals(t, r) {
    return t[0] === r[0] && t[1] === r[1] && t[2] === r[2];
  }
  static equals(t, r) {
    const s = t[0], i = t[1], n = t[2], a = r[0], o = r[1], h = r[2];
    return Math.abs(s - a) <= 1e-6 * Math.max(1, Math.abs(s), Math.abs(a)) && Math.abs(i - o) <= 1e-6 * Math.max(1, Math.abs(i), Math.abs(o)) && Math.abs(n - h) <= 1e-6 * Math.max(1, Math.abs(n), Math.abs(h));
  }
};
gt.prototype.sub = gt.prototype.subtract;
gt.prototype.mul = gt.prototype.multiply;
gt.prototype.div = gt.prototype.divide;
gt.prototype.dist = gt.prototype.distance;
gt.prototype.sqrDist = gt.prototype.squaredDistance;
gt.sub = gt.subtract;
gt.mul = gt.multiply;
gt.div = gt.divide;
gt.dist = gt.distance;
gt.sqrDist = gt.squaredDistance;
gt.sqrLen = gt.squaredLength;
gt.mag = gt.magnitude;
gt.length = gt.magnitude;
gt.len = gt.magnitude;
var ot = class he extends Float32Array {
  static BYTE_LENGTH = 4 * Float32Array.BYTES_PER_ELEMENT;
  constructor(...t) {
    switch (t.length) {
      case 4:
        super(t);
        break;
      case 2:
        super(t[0], t[1], 4);
        break;
      case 1: {
        const r = t[0];
        typeof r == "number" ? super([
          r,
          r,
          r,
          r
        ]) : super(r, 0, 4);
        break;
      }
      default:
        super(4);
    }
  }
  get x() {
    return this[0];
  }
  set x(t) {
    this[0] = t;
  }
  get y() {
    return this[1];
  }
  set y(t) {
    this[1] = t;
  }
  get z() {
    return this[2];
  }
  set z(t) {
    this[2] = t;
  }
  get w() {
    return this[3];
  }
  set w(t) {
    this[3] = t;
  }
  get r() {
    return this[0];
  }
  set r(t) {
    this[0] = t;
  }
  get g() {
    return this[1];
  }
  set g(t) {
    this[1] = t;
  }
  get b() {
    return this[2];
  }
  set b(t) {
    this[2] = t;
  }
  get a() {
    return this[3];
  }
  set a(t) {
    this[3] = t;
  }
  get magnitude() {
    const t = this[0], r = this[1], s = this[2], i = this[3];
    return Math.sqrt(t * t + r * r + s * s + i * i);
  }
  get mag() {
    return this.magnitude;
  }
  get str() {
    return he.str(this);
  }
  copy(t) {
    return super.set(t), this;
  }
  add(t) {
    return this[0] += t[0], this[1] += t[1], this[2] += t[2], this[3] += t[3], this;
  }
  subtract(t) {
    return this[0] -= t[0], this[1] -= t[1], this[2] -= t[2], this[3] -= t[3], this;
  }
  sub(t) {
    return this;
  }
  multiply(t) {
    return this[0] *= t[0], this[1] *= t[1], this[2] *= t[2], this[3] *= t[3], this;
  }
  mul(t) {
    return this;
  }
  divide(t) {
    return this[0] /= t[0], this[1] /= t[1], this[2] /= t[2], this[3] /= t[3], this;
  }
  div(t) {
    return this;
  }
  scale(t) {
    return this[0] *= t, this[1] *= t, this[2] *= t, this[3] *= t, this;
  }
  scaleAndAdd(t, r) {
    return this[0] += t[0] * r, this[1] += t[1] * r, this[2] += t[2] * r, this[3] += t[3] * r, this;
  }
  distance(t) {
    return he.distance(this, t);
  }
  dist(t) {
    return 0;
  }
  squaredDistance(t) {
    return he.squaredDistance(this, t);
  }
  sqrDist(t) {
    return 0;
  }
  negate() {
    return this[0] *= -1, this[1] *= -1, this[2] *= -1, this[3] *= -1, this;
  }
  invert() {
    return this[0] = 1 / this[0], this[1] = 1 / this[1], this[2] = 1 / this[2], this[3] = 1 / this[3], this;
  }
  abs() {
    return this[0] = Math.abs(this[0]), this[1] = Math.abs(this[1]), this[2] = Math.abs(this[2]), this[3] = Math.abs(this[3]), this;
  }
  dot(t) {
    return this[0] * t[0] + this[1] * t[1] + this[2] * t[2] + this[3] * t[3];
  }
  normalize() {
    return he.normalize(this, this);
  }
  static create() {
    return new he();
  }
  static clone(t) {
    return new he(t);
  }
  static fromValues(t, r, s, i) {
    return new he(t, r, s, i);
  }
  static copy(t, r) {
    return t[0] = r[0], t[1] = r[1], t[2] = r[2], t[3] = r[3], t;
  }
  static set(t, r, s, i, n) {
    return t[0] = r, t[1] = s, t[2] = i, t[3] = n, t;
  }
  static add(t, r, s) {
    return t[0] = r[0] + s[0], t[1] = r[1] + s[1], t[2] = r[2] + s[2], t[3] = r[3] + s[3], t;
  }
  static subtract(t, r, s) {
    return t[0] = r[0] - s[0], t[1] = r[1] - s[1], t[2] = r[2] - s[2], t[3] = r[3] - s[3], t;
  }
  static sub(t, r, s) {
    return t;
  }
  static multiply(t, r, s) {
    return t[0] = r[0] * s[0], t[1] = r[1] * s[1], t[2] = r[2] * s[2], t[3] = r[3] * s[3], t;
  }
  static mul(t, r, s) {
    return t;
  }
  static divide(t, r, s) {
    return t[0] = r[0] / s[0], t[1] = r[1] / s[1], t[2] = r[2] / s[2], t[3] = r[3] / s[3], t;
  }
  static div(t, r, s) {
    return t;
  }
  static ceil(t, r) {
    return t[0] = Math.ceil(r[0]), t[1] = Math.ceil(r[1]), t[2] = Math.ceil(r[2]), t[3] = Math.ceil(r[3]), t;
  }
  static floor(t, r) {
    return t[0] = Math.floor(r[0]), t[1] = Math.floor(r[1]), t[2] = Math.floor(r[2]), t[3] = Math.floor(r[3]), t;
  }
  static min(t, r, s) {
    return t[0] = Math.min(r[0], s[0]), t[1] = Math.min(r[1], s[1]), t[2] = Math.min(r[2], s[2]), t[3] = Math.min(r[3], s[3]), t;
  }
  static max(t, r, s) {
    return t[0] = Math.max(r[0], s[0]), t[1] = Math.max(r[1], s[1]), t[2] = Math.max(r[2], s[2]), t[3] = Math.max(r[3], s[3]), t;
  }
  static round(t, r) {
    return t[0] = Math.round(r[0]), t[1] = Math.round(r[1]), t[2] = Math.round(r[2]), t[3] = Math.round(r[3]), t;
  }
  static scale(t, r, s) {
    return t[0] = r[0] * s, t[1] = r[1] * s, t[2] = r[2] * s, t[3] = r[3] * s, t;
  }
  static scaleAndAdd(t, r, s, i) {
    return t[0] = r[0] + s[0] * i, t[1] = r[1] + s[1] * i, t[2] = r[2] + s[2] * i, t[3] = r[3] + s[3] * i, t;
  }
  static distance(t, r) {
    const s = r[0] - t[0], i = r[1] - t[1], n = r[2] - t[2], a = r[3] - t[3];
    return Math.hypot(s, i, n, a);
  }
  static dist(t, r) {
    return 0;
  }
  static squaredDistance(t, r) {
    const s = r[0] - t[0], i = r[1] - t[1], n = r[2] - t[2], a = r[3] - t[3];
    return s * s + i * i + n * n + a * a;
  }
  static sqrDist(t, r) {
    return 0;
  }
  static magnitude(t) {
    const r = t[0], s = t[1], i = t[2], n = t[3];
    return Math.sqrt(r * r + s * s + i * i + n * n);
  }
  static mag(t) {
    return 0;
  }
  static length(t) {
    return 0;
  }
  static len(t) {
    return 0;
  }
  static squaredLength(t) {
    const r = t[0], s = t[1], i = t[2], n = t[3];
    return r * r + s * s + i * i + n * n;
  }
  static sqrLen(t) {
    return 0;
  }
  static negate(t, r) {
    return t[0] = -r[0], t[1] = -r[1], t[2] = -r[2], t[3] = -r[3], t;
  }
  static inverse(t, r) {
    return t[0] = 1 / r[0], t[1] = 1 / r[1], t[2] = 1 / r[2], t[3] = 1 / r[3], t;
  }
  static abs(t, r) {
    return t[0] = Math.abs(r[0]), t[1] = Math.abs(r[1]), t[2] = Math.abs(r[2]), t[3] = Math.abs(r[3]), t;
  }
  static normalize(t, r) {
    const s = r[0], i = r[1], n = r[2], a = r[3];
    let o = s * s + i * i + n * n + a * a;
    return o > 0 && (o = 1 / Math.sqrt(o)), t[0] = s * o, t[1] = i * o, t[2] = n * o, t[3] = a * o, t;
  }
  static dot(t, r) {
    return t[0] * r[0] + t[1] * r[1] + t[2] * r[2] + t[3] * r[3];
  }
  static cross(t, r, s, i) {
    const n = s[0] * i[1] - s[1] * i[0], a = s[0] * i[2] - s[2] * i[0], o = s[0] * i[3] - s[3] * i[0], h = s[1] * i[2] - s[2] * i[1], l = s[1] * i[3] - s[3] * i[1], c = s[2] * i[3] - s[3] * i[2], u = r[0], d = r[1], y = r[2], m = r[3];
    return t[0] = d * c - y * l + m * h, t[1] = -(u * c) + y * o - m * a, t[2] = u * l - d * o + m * n, t[3] = -(u * h) + d * a - y * n, t;
  }
  static lerp(t, r, s, i) {
    const n = r[0], a = r[1], o = r[2], h = r[3];
    return t[0] = n + i * (s[0] - n), t[1] = a + i * (s[1] - a), t[2] = o + i * (s[2] - o), t[3] = h + i * (s[3] - h), t;
  }
  static transformMat4(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = r[3];
    return t[0] = s[0] * i + s[4] * n + s[8] * a + s[12] * o, t[1] = s[1] * i + s[5] * n + s[9] * a + s[13] * o, t[2] = s[2] * i + s[6] * n + s[10] * a + s[14] * o, t[3] = s[3] * i + s[7] * n + s[11] * a + s[15] * o, t;
  }
  static transformQuat(t, r, s) {
    const i = r[0], n = r[1], a = r[2], o = s[0], h = s[1], l = s[2], c = s[3], u = c * i + h * a - l * n, d = c * n + l * i - o * a, y = c * a + o * n - h * i, m = -o * i - h * n - l * a;
    return t[0] = u * c + m * -o + d * -l - y * -h, t[1] = d * c + m * -h + y * -o - u * -l, t[2] = y * c + m * -l + u * -h - d * -o, t[3] = r[3], t;
  }
  static zero(t) {
    return t[0] = 0, t[1] = 0, t[2] = 0, t[3] = 0, t;
  }
  static str(t) {
    return `Vec4(${t.join(", ")})`;
  }
  static exactEquals(t, r) {
    return t[0] === r[0] && t[1] === r[1] && t[2] === r[2] && t[3] === r[3];
  }
  static equals(t, r) {
    const s = t[0], i = t[1], n = t[2], a = t[3], o = r[0], h = r[1], l = r[2], c = r[3];
    return Math.abs(s - o) <= 1e-6 * Math.max(1, Math.abs(s), Math.abs(o)) && Math.abs(i - h) <= 1e-6 * Math.max(1, Math.abs(i), Math.abs(h)) && Math.abs(n - l) <= 1e-6 * Math.max(1, Math.abs(n), Math.abs(l)) && Math.abs(a - c) <= 1e-6 * Math.max(1, Math.abs(a), Math.abs(c));
  }
};
ot.prototype.sub = ot.prototype.subtract;
ot.prototype.mul = ot.prototype.multiply;
ot.prototype.div = ot.prototype.divide;
ot.prototype.dist = ot.prototype.distance;
ot.prototype.sqrDist = ot.prototype.squaredDistance;
ot.sub = ot.subtract;
ot.mul = ot.multiply;
ot.div = ot.divide;
ot.dist = ot.distance;
ot.sqrDist = ot.squaredDistance;
ot.sqrLen = ot.squaredLength;
ot.mag = ot.magnitude;
ot.length = ot.magnitude;
ot.len = ot.magnitude;
var ft = class le extends Float32Array {
  static BYTE_LENGTH = 2 * Float32Array.BYTES_PER_ELEMENT;
  constructor(...t) {
    switch (t.length) {
      case 2: {
        const r = t[0];
        typeof r == "number" ? super([r, t[1]]) : super(r, t[1], 2);
        break;
      }
      case 1: {
        const r = t[0];
        typeof r == "number" ? super([r, r]) : super(r, 0, 2);
        break;
      }
      default:
        super(2);
    }
  }
  get x() {
    return this[0];
  }
  set x(t) {
    this[0] = t;
  }
  get y() {
    return this[1];
  }
  set y(t) {
    this[1] = t;
  }
  get r() {
    return this[0];
  }
  set r(t) {
    this[0] = t;
  }
  get g() {
    return this[1];
  }
  set g(t) {
    this[1] = t;
  }
  get magnitude() {
    return Math.hypot(this[0], this[1]);
  }
  get mag() {
    return this.magnitude;
  }
  get squaredMagnitude() {
    const t = this[0], r = this[1];
    return t * t + r * r;
  }
  get sqrMag() {
    return this.squaredMagnitude;
  }
  get str() {
    return le.str(this);
  }
  copy(t) {
    return this.set(t), this;
  }
  add(t) {
    return this[0] += t[0], this[1] += t[1], this;
  }
  subtract(t) {
    return this[0] -= t[0], this[1] -= t[1], this;
  }
  sub(t) {
    return this;
  }
  multiply(t) {
    return this[0] *= t[0], this[1] *= t[1], this;
  }
  mul(t) {
    return this;
  }
  divide(t) {
    return this[0] /= t[0], this[1] /= t[1], this;
  }
  div(t) {
    return this;
  }
  scale(t) {
    return this[0] *= t, this[1] *= t, this;
  }
  scaleAndAdd(t, r) {
    return this[0] += t[0] * r, this[1] += t[1] * r, this;
  }
  distance(t) {
    return le.distance(this, t);
  }
  dist(t) {
    return 0;
  }
  squaredDistance(t) {
    return le.squaredDistance(this, t);
  }
  sqrDist(t) {
    return 0;
  }
  negate() {
    return this[0] *= -1, this[1] *= -1, this;
  }
  invert() {
    return this[0] = 1 / this[0], this[1] = 1 / this[1], this;
  }
  abs() {
    return this[0] = Math.abs(this[0]), this[1] = Math.abs(this[1]), this;
  }
  dot(t) {
    return this[0] * t[0] + this[1] * t[1];
  }
  normalize() {
    return le.normalize(this, this);
  }
  static create() {
    return new le();
  }
  static clone(t) {
    return new le(t);
  }
  static fromValues(t, r) {
    return new le(t, r);
  }
  static copy(t, r) {
    return t[0] = r[0], t[1] = r[1], t;
  }
  static set(t, r, s) {
    return t[0] = r, t[1] = s, t;
  }
  static add(t, r, s) {
    return t[0] = r[0] + s[0], t[1] = r[1] + s[1], t;
  }
  static subtract(t, r, s) {
    return t[0] = r[0] - s[0], t[1] = r[1] - s[1], t;
  }
  static sub(t, r, s) {
    return [0, 0];
  }
  static multiply(t, r, s) {
    return t[0] = r[0] * s[0], t[1] = r[1] * s[1], t;
  }
  static mul(t, r, s) {
    return [0, 0];
  }
  static divide(t, r, s) {
    return t[0] = r[0] / s[0], t[1] = r[1] / s[1], t;
  }
  static div(t, r, s) {
    return [0, 0];
  }
  static ceil(t, r) {
    return t[0] = Math.ceil(r[0]), t[1] = Math.ceil(r[1]), t;
  }
  static floor(t, r) {
    return t[0] = Math.floor(r[0]), t[1] = Math.floor(r[1]), t;
  }
  static min(t, r, s) {
    return t[0] = Math.min(r[0], s[0]), t[1] = Math.min(r[1], s[1]), t;
  }
  static max(t, r, s) {
    return t[0] = Math.max(r[0], s[0]), t[1] = Math.max(r[1], s[1]), t;
  }
  static round(t, r) {
    return t[0] = Math.round(r[0]), t[1] = Math.round(r[1]), t;
  }
  static scale(t, r, s) {
    return t[0] = r[0] * s, t[1] = r[1] * s, t;
  }
  static scaleAndAdd(t, r, s, i) {
    return t[0] = r[0] + s[0] * i, t[1] = r[1] + s[1] * i, t;
  }
  static distance(t, r) {
    return Math.hypot(r[0] - t[0], r[1] - t[1]);
  }
  static dist(t, r) {
    return 0;
  }
  static squaredDistance(t, r) {
    const s = r[0] - t[0], i = r[1] - t[1];
    return s * s + i * i;
  }
  static sqrDist(t, r) {
    return 0;
  }
  static magnitude(t) {
    let r = t[0], s = t[1];
    return Math.sqrt(r * r + s * s);
  }
  static mag(t) {
    return 0;
  }
  static length(t) {
    return 0;
  }
  static len(t) {
    return 0;
  }
  static squaredLength(t) {
    const r = t[0], s = t[1];
    return r * r + s * s;
  }
  static sqrLen(t, r) {
    return 0;
  }
  static negate(t, r) {
    return t[0] = -r[0], t[1] = -r[1], t;
  }
  static inverse(t, r) {
    return t[0] = 1 / r[0], t[1] = 1 / r[1], t;
  }
  static abs(t, r) {
    return t[0] = Math.abs(r[0]), t[1] = Math.abs(r[1]), t;
  }
  static normalize(t, r) {
    const s = r[0], i = r[1];
    let n = s * s + i * i;
    return n > 0 && (n = 1 / Math.sqrt(n)), t[0] = r[0] * n, t[1] = r[1] * n, t;
  }
  static dot(t, r) {
    return t[0] * r[0] + t[1] * r[1];
  }
  static cross(t, r, s) {
    const i = r[0] * s[1] - r[1] * s[0];
    return t[0] = t[1] = 0, t[2] = i, t;
  }
  static lerp(t, r, s, i) {
    const n = r[0], a = r[1];
    return t[0] = n + i * (s[0] - n), t[1] = a + i * (s[1] - a), t;
  }
  static transformMat2(t, r, s) {
    const i = r[0], n = r[1];
    return t[0] = s[0] * i + s[2] * n, t[1] = s[1] * i + s[3] * n, t;
  }
  static transformMat2d(t, r, s) {
    const i = r[0], n = r[1];
    return t[0] = s[0] * i + s[2] * n + s[4], t[1] = s[1] * i + s[3] * n + s[5], t;
  }
  static transformMat3(t, r, s) {
    const i = r[0], n = r[1];
    return t[0] = s[0] * i + s[3] * n + s[6], t[1] = s[1] * i + s[4] * n + s[7], t;
  }
  static transformMat4(t, r, s) {
    const i = r[0], n = r[1];
    return t[0] = s[0] * i + s[4] * n + s[12], t[1] = s[1] * i + s[5] * n + s[13], t;
  }
  static rotate(t, r, s, i) {
    const n = r[0] - s[0], a = r[1] - s[1], o = Math.sin(i), h = Math.cos(i);
    return t[0] = n * h - a * o + s[0], t[1] = n * o + a * h + s[1], t;
  }
  static angle(t, r) {
    const s = t[0], i = t[1], n = r[0], a = r[1], o = Math.sqrt(s * s + i * i) * Math.sqrt(n * n + a * a), h = o && (s * n + i * a) / o;
    return Math.acos(Math.min(Math.max(h, -1), 1));
  }
  static zero(t) {
    return t[0] = 0, t[1] = 0, t;
  }
  static exactEquals(t, r) {
    return t[0] === r[0] && t[1] === r[1];
  }
  static equals(t, r) {
    const s = t[0], i = t[1], n = r[0], a = r[1];
    return Math.abs(s - n) <= 1e-6 * Math.max(1, Math.abs(s), Math.abs(n)) && Math.abs(i - a) <= 1e-6 * Math.max(1, Math.abs(i), Math.abs(a));
  }
  static str(t) {
    return `Vec2(${t.join(", ")})`;
  }
};
ft.prototype.sub = ft.prototype.subtract;
ft.prototype.mul = ft.prototype.multiply;
ft.prototype.div = ft.prototype.divide;
ft.prototype.dist = ft.prototype.distance;
ft.prototype.sqrDist = ft.prototype.squaredDistance;
ft.sub = ft.subtract;
ft.mul = ft.multiply;
ft.div = ft.divide;
ft.dist = ft.distance;
ft.sqrDist = ft.squaredDistance;
ft.sqrLen = ft.squaredLength;
ft.mag = ft.magnitude;
ft.length = ft.magnitude;
ft.len = ft.magnitude;
var Me = /* @__PURE__ */ ((e) => (e[e.WEBGL_LEGACY = 0] = "WEBGL_LEGACY", e[e.WEBGL = 1] = "WEBGL", e[e.WEBGL2 = 2] = "WEBGL2", e))(Me || {}), En = /* @__PURE__ */ ((e) => (e[e.UNKNOWN = 0] = "UNKNOWN", e[e.WEBGL = 1] = "WEBGL", e[e.CANVAS = 2] = "CANVAS", e))(En || {}), Rs = /* @__PURE__ */ ((e) => (e[e.COLOR = 16384] = "COLOR", e[e.DEPTH = 256] = "DEPTH", e[e.STENCIL = 1024] = "STENCIL", e))(Rs || {}), st = /* @__PURE__ */ ((e) => (e[e.NORMAL = 0] = "NORMAL", e[e.ADD = 1] = "ADD", e[e.MULTIPLY = 2] = "MULTIPLY", e[e.SCREEN = 3] = "SCREEN", e[e.OVERLAY = 4] = "OVERLAY", e[e.DARKEN = 5] = "DARKEN", e[e.LIGHTEN = 6] = "LIGHTEN", e[e.COLOR_DODGE = 7] = "COLOR_DODGE", e[e.COLOR_BURN = 8] = "COLOR_BURN", e[e.HARD_LIGHT = 9] = "HARD_LIGHT", e[e.SOFT_LIGHT = 10] = "SOFT_LIGHT", e[e.DIFFERENCE = 11] = "DIFFERENCE", e[e.EXCLUSION = 12] = "EXCLUSION", e[e.HUE = 13] = "HUE", e[e.SATURATION = 14] = "SATURATION", e[e.COLOR = 15] = "COLOR", e[e.LUMINOSITY = 16] = "LUMINOSITY", e[e.NORMAL_NPM = 17] = "NORMAL_NPM", e[e.ADD_NPM = 18] = "ADD_NPM", e[e.SCREEN_NPM = 19] = "SCREEN_NPM", e[e.NONE = 20] = "NONE", e[e.SRC_OVER = 0] = "SRC_OVER", e[e.SRC_IN = 21] = "SRC_IN", e[e.SRC_OUT = 22] = "SRC_OUT", e[e.SRC_ATOP = 23] = "SRC_ATOP", e[e.DST_OVER = 24] = "DST_OVER", e[e.DST_IN = 25] = "DST_IN", e[e.DST_OUT = 26] = "DST_OUT", e[e.DST_ATOP = 27] = "DST_ATOP", e[e.ERASE = 26] = "ERASE", e[e.SUBTRACT = 28] = "SUBTRACT", e[e.XOR = 29] = "XOR", e))(st || {}), Wr = /* @__PURE__ */ ((e) => (e[e.POINTS = 0] = "POINTS", e[e.LINES = 1] = "LINES", e[e.LINE_LOOP = 2] = "LINE_LOOP", e[e.LINE_STRIP = 3] = "LINE_STRIP", e[e.TRIANGLES = 4] = "TRIANGLES", e[e.TRIANGLE_STRIP = 5] = "TRIANGLE_STRIP", e[e.TRIANGLE_FAN = 6] = "TRIANGLE_FAN", e))(Wr || {}), X = /* @__PURE__ */ ((e) => (e[e.RGBA = 6408] = "RGBA", e[e.RGB = 6407] = "RGB", e[e.RG = 33319] = "RG", e[e.RED = 6403] = "RED", e[e.RGBA_INTEGER = 36249] = "RGBA_INTEGER", e[e.RGB_INTEGER = 36248] = "RGB_INTEGER", e[e.RG_INTEGER = 33320] = "RG_INTEGER", e[e.RED_INTEGER = 36244] = "RED_INTEGER", e[e.ALPHA = 6406] = "ALPHA", e[e.LUMINANCE = 6409] = "LUMINANCE", e[e.LUMINANCE_ALPHA = 6410] = "LUMINANCE_ALPHA", e[e.DEPTH_COMPONENT = 6402] = "DEPTH_COMPONENT", e[e.DEPTH_STENCIL = 34041] = "DEPTH_STENCIL", e))(X || {}), qe = /* @__PURE__ */ ((e) => (e[e.TEXTURE_2D = 3553] = "TEXTURE_2D", e[e.TEXTURE_CUBE_MAP = 34067] = "TEXTURE_CUBE_MAP", e[e.TEXTURE_2D_ARRAY = 35866] = "TEXTURE_2D_ARRAY", e[e.TEXTURE_CUBE_MAP_POSITIVE_X = 34069] = "TEXTURE_CUBE_MAP_POSITIVE_X", e[e.TEXTURE_CUBE_MAP_NEGATIVE_X = 34070] = "TEXTURE_CUBE_MAP_NEGATIVE_X", e[e.TEXTURE_CUBE_MAP_POSITIVE_Y = 34071] = "TEXTURE_CUBE_MAP_POSITIVE_Y", e[e.TEXTURE_CUBE_MAP_NEGATIVE_Y = 34072] = "TEXTURE_CUBE_MAP_NEGATIVE_Y", e[e.TEXTURE_CUBE_MAP_POSITIVE_Z = 34073] = "TEXTURE_CUBE_MAP_POSITIVE_Z", e[e.TEXTURE_CUBE_MAP_NEGATIVE_Z = 34074] = "TEXTURE_CUBE_MAP_NEGATIVE_Z", e))(qe || {}), ct = /* @__PURE__ */ ((e) => (e[e.UNSIGNED_BYTE = 5121] = "UNSIGNED_BYTE", e[e.UNSIGNED_SHORT = 5123] = "UNSIGNED_SHORT", e[e.UNSIGNED_SHORT_5_6_5 = 33635] = "UNSIGNED_SHORT_5_6_5", e[e.UNSIGNED_SHORT_4_4_4_4 = 32819] = "UNSIGNED_SHORT_4_4_4_4", e[e.UNSIGNED_SHORT_5_5_5_1 = 32820] = "UNSIGNED_SHORT_5_5_5_1", e[e.UNSIGNED_INT = 5125] = "UNSIGNED_INT", e[e.UNSIGNED_INT_10F_11F_11F_REV = 35899] = "UNSIGNED_INT_10F_11F_11F_REV", e[e.UNSIGNED_INT_2_10_10_10_REV = 33640] = "UNSIGNED_INT_2_10_10_10_REV", e[e.UNSIGNED_INT_24_8 = 34042] = "UNSIGNED_INT_24_8", e[e.UNSIGNED_INT_5_9_9_9_REV = 35902] = "UNSIGNED_INT_5_9_9_9_REV", e[e.BYTE = 5120] = "BYTE", e[e.SHORT = 5122] = "SHORT", e[e.INT = 5124] = "INT", e[e.FLOAT = 5126] = "FLOAT", e[e.FLOAT_32_UNSIGNED_INT_24_8_REV = 36269] = "FLOAT_32_UNSIGNED_INT_24_8_REV", e[e.HALF_FLOAT = 36193] = "HALF_FLOAT", e))(ct || {}), q = /* @__PURE__ */ ((e) => (e[e.FLOAT = 0] = "FLOAT", e[e.INT = 1] = "INT", e[e.UINT = 2] = "UINT", e))(q || {}), te = /* @__PURE__ */ ((e) => (e[e.NEAREST = 0] = "NEAREST", e[e.LINEAR = 1] = "LINEAR", e))(te || {}), Js = /* @__PURE__ */ ((e) => (e[e.CLAMP = 33071] = "CLAMP", e[e.REPEAT = 10497] = "REPEAT", e[e.MIRRORED_REPEAT = 33648] = "MIRRORED_REPEAT", e))(Js || {}), Se = /* @__PURE__ */ ((e) => (e[e.OFF = 0] = "OFF", e[e.POW2 = 1] = "POW2", e[e.ON = 2] = "ON", e[e.ON_MANUAL = 3] = "ON_MANUAL", e))(Se || {}), Ce = /* @__PURE__ */ ((e) => (e[e.NPM = 0] = "NPM", e[e.UNPACK = 1] = "UNPACK", e[e.PMA = 2] = "PMA", e[e.NO_PREMULTIPLIED_ALPHA = 0] = "NO_PREMULTIPLIED_ALPHA", e[e.PREMULTIPLY_ON_UPLOAD = 1] = "PREMULTIPLY_ON_UPLOAD", e[e.PREMULTIPLIED_ALPHA = 2] = "PREMULTIPLIED_ALPHA", e))(Ce || {}), Kt = /* @__PURE__ */ ((e) => (e[e.NO = 0] = "NO", e[e.YES = 1] = "YES", e[e.AUTO = 2] = "AUTO", e[e.BLEND = 0] = "BLEND", e[e.CLEAR = 1] = "CLEAR", e[e.BLIT = 2] = "BLIT", e))(Kt || {}), Qs = /* @__PURE__ */ ((e) => (e[e.AUTO = 0] = "AUTO", e[e.MANUAL = 1] = "MANUAL", e))(Qs || {}), kt = /* @__PURE__ */ ((e) => (e.LOW = "lowp", e.MEDIUM = "mediump", e.HIGH = "highp", e))(kt || {}), Ct = /* @__PURE__ */ ((e) => (e[e.NONE = 0] = "NONE", e[e.SCISSOR = 1] = "SCISSOR", e[e.STENCIL = 2] = "STENCIL", e[e.SPRITE = 3] = "SPRITE", e[e.COLOR = 4] = "COLOR", e))(Ct || {}), Mt = /* @__PURE__ */ ((e) => (e[e.NONE = 0] = "NONE", e[e.LOW = 2] = "LOW", e[e.MEDIUM = 4] = "MEDIUM", e[e.HIGH = 8] = "HIGH", e))(Mt || {}), Zt = /* @__PURE__ */ ((e) => (e[e.ELEMENT_ARRAY_BUFFER = 34963] = "ELEMENT_ARRAY_BUFFER", e[e.ARRAY_BUFFER = 34962] = "ARRAY_BUFFER", e[e.UNIFORM_BUFFER = 35345] = "UNIFORM_BUFFER", e))(Zt || {}), lo = {
  createCanvas: (e, t) => {
    const r = document.createElement("canvas");
    return r.width = e, r.height = t, r;
  },
  getCanvasRenderingContext2D: () => CanvasRenderingContext2D,
  getWebGLRenderingContext: () => WebGLRenderingContext,
  getNavigator: () => navigator,
  getBaseUrl: () => document.baseURI ?? window.location.href,
  getFontFaceSet: () => document.fonts,
  fetch: (e, t) => fetch(e, t),
  parseXML: (e) => new DOMParser().parseFromString(e, "text/xml")
}, dt = {
  ADAPTER: lo,
  RESOLUTION: 1,
  CREATE_IMAGE_BITMAP: !1,
  ROUND_PIXELS: !1
}, fs = /iPhone/i, bi = /iPod/i, Ti = /iPad/i, wi = /\biOS-universal(?:.+)Mac\b/i, ps = /\bAndroid(?:.+)Mobile\b/i, Ei = /Android/i, ke = /(?:SD4930UR|\bSilk(?:.+)Mobile\b)/i, _r = /Silk/i, Jt = /Windows Phone/i, Si = /\bWindows(?:.+)ARM\b/i, Ai = /BlackBerry/i, Ii = /BB10/i, Ri = /Opera Mini/i, Mi = /\b(CriOS|Chrome)(?:.+)Mobile/i, Ci = /Mobile(?:.+)Firefox\b/i, Pi = function(e) {
  return typeof e < "u" && e.platform === "MacIntel" && typeof e.maxTouchPoints == "number" && e.maxTouchPoints > 1 && typeof MSStream > "u";
};
function co(e) {
  return function(t) {
    return t.test(e);
  };
}
function Li(e) {
  var t = {
    userAgent: "",
    platform: "",
    maxTouchPoints: 0
  };
  !e && typeof navigator < "u" ? t = {
    userAgent: navigator.userAgent,
    platform: navigator.platform,
    maxTouchPoints: navigator.maxTouchPoints || 0
  } : typeof e == "string" ? t.userAgent = e : e && e.userAgent && (t = {
    userAgent: e.userAgent,
    platform: e.platform,
    maxTouchPoints: e.maxTouchPoints || 0
  });
  var r = t.userAgent, s = r.split("[FBAN");
  typeof s[1] < "u" && (r = s[0]), s = r.split("Twitter"), typeof s[1] < "u" && (r = s[0]);
  var i = co(r), n = {
    apple: {
      phone: i(fs) && !i(Jt),
      ipod: i(bi),
      tablet: !i(fs) && (i(Ti) || Pi(t)) && !i(Jt),
      universal: i(wi),
      device: (i(fs) || i(bi) || i(Ti) || i(wi) || Pi(t)) && !i(Jt)
    },
    amazon: {
      phone: i(ke),
      tablet: !i(ke) && i(_r),
      device: i(ke) || i(_r)
    },
    android: {
      phone: !i(Jt) && i(ke) || !i(Jt) && i(ps),
      tablet: !i(Jt) && !i(ke) && !i(ps) && (i(_r) || i(Ei)),
      device: !i(Jt) && (i(ke) || i(_r) || i(ps) || i(Ei)) || i(/\bokhttp\b/i)
    },
    windows: {
      phone: i(Jt),
      tablet: i(Si),
      device: i(Jt) || i(Si)
    },
    other: {
      blackberry: i(Ai),
      blackberry10: i(Ii),
      opera: i(Ri),
      firefox: i(Ci),
      chrome: i(Mi),
      device: i(Ai) || i(Ii) || i(Ri) || i(Ci) || i(Mi)
    },
    any: !1,
    phone: !1,
    tablet: !1
  };
  return n.any = n.apple.device || n.android.device || n.windows.device || n.other.device, n.phone = n.apple.phone || n.android.phone || n.windows.phone, n.tablet = n.apple.tablet || n.android.tablet || n.windows.tablet, n;
}
var He = (Li.default ?? Li)(globalThis.navigator);
dt.RETINA_PREFIX = /@([0-9\.]+)x/;
dt.FAIL_IF_MAJOR_PERFORMANCE_CAVEAT = !1;
var uo = /* @__PURE__ */ rt(((e, t) => {
  var r = Object.prototype.hasOwnProperty, s = "~";
  function i() {
  }
  Object.create && (i.prototype = /* @__PURE__ */ Object.create(null), new i().__proto__ || (s = !1));
  function n(l, c, u) {
    this.fn = l, this.context = c, this.once = u || !1;
  }
  function a(l, c, u, d, y) {
    if (typeof u != "function") throw new TypeError("The listener must be a function");
    var m = new n(u, d || l, y), v = s ? s + c : c;
    return l._events[v] ? l._events[v].fn ? l._events[v] = [l._events[v], m] : l._events[v].push(m) : (l._events[v] = m, l._eventsCount++), l;
  }
  function o(l, c) {
    --l._eventsCount === 0 ? l._events = new i() : delete l._events[c];
  }
  function h() {
    this._events = new i(), this._eventsCount = 0;
  }
  h.prototype.eventNames = function() {
    var c = [], u, d;
    if (this._eventsCount === 0) return c;
    for (d in u = this._events) r.call(u, d) && c.push(s ? d.slice(1) : d);
    return Object.getOwnPropertySymbols ? c.concat(Object.getOwnPropertySymbols(u)) : c;
  }, h.prototype.listeners = function(c) {
    var u = s ? s + c : c, d = this._events[u];
    if (!d) return [];
    if (d.fn) return [d.fn];
    for (var y = 0, m = d.length, v = new Array(m); y < m; y++) v[y] = d[y].fn;
    return v;
  }, h.prototype.listenerCount = function(c) {
    var u = s ? s + c : c, d = this._events[u];
    return d ? d.fn ? 1 : d.length : 0;
  }, h.prototype.emit = function(c, u, d, y, m, v) {
    var p = s ? s + c : c;
    if (!this._events[p]) return !1;
    var x = this._events[p], f = arguments.length, E, g;
    if (x.fn) {
      switch (x.once && this.removeListener(c, x.fn, void 0, !0), f) {
        case 1:
          return x.fn.call(x.context), !0;
        case 2:
          return x.fn.call(x.context, u), !0;
        case 3:
          return x.fn.call(x.context, u, d), !0;
        case 4:
          return x.fn.call(x.context, u, d, y), !0;
        case 5:
          return x.fn.call(x.context, u, d, y, m), !0;
        case 6:
          return x.fn.call(x.context, u, d, y, m, v), !0;
      }
      for (g = 1, E = new Array(f - 1); g < f; g++) E[g - 1] = arguments[g];
      x.fn.apply(x.context, E);
    } else {
      var A = x.length, L;
      for (g = 0; g < A; g++)
        switch (x[g].once && this.removeListener(c, x[g].fn, void 0, !0), f) {
          case 1:
            x[g].fn.call(x[g].context);
            break;
          case 2:
            x[g].fn.call(x[g].context, u);
            break;
          case 3:
            x[g].fn.call(x[g].context, u, d);
            break;
          case 4:
            x[g].fn.call(x[g].context, u, d, y);
            break;
          default:
            if (!E) for (L = 1, E = new Array(f - 1); L < f; L++) E[L - 1] = arguments[L];
            x[g].fn.apply(x[g].context, E);
        }
    }
    return !0;
  }, h.prototype.on = function(c, u, d) {
    return a(this, c, u, d, !1);
  }, h.prototype.once = function(c, u, d) {
    return a(this, c, u, d, !0);
  }, h.prototype.removeListener = function(c, u, d, y) {
    var m = s ? s + c : c;
    if (!this._events[m]) return this;
    if (!u)
      return o(this, m), this;
    var v = this._events[m];
    if (v.fn)
      v.fn === u && (!y || v.once) && (!d || v.context === d) && o(this, m);
    else {
      for (var p = 0, x = [], f = v.length; p < f; p++) (v[p].fn !== u || y && !v[p].once || d && v[p].context !== d) && x.push(v[p]);
      x.length ? this._events[m] = x.length === 1 ? x[0] : x : o(this, m);
    }
    return this;
  }, h.prototype.removeAllListeners = function(c) {
    var u;
    return c ? (u = s ? s + c : c, this._events[u] && o(this, u)) : (this._events = new i(), this._eventsCount = 0), this;
  }, h.prototype.off = h.prototype.removeListener, h.prototype.addListener = h.prototype.on, h.prefixed = s, h.EventEmitter = h, typeof t < "u" && (t.exports = h);
})), fo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = r, t.exports.default = r;
  function r(_, T, b) {
    b = b || 2;
    var I = T && T.length, S = I ? T[0] * b : _.length, R = s(_, 0, S, b, !0), O = [];
    if (!R || R.next === R.prev) return O;
    var z, V, W, et, it, K, j;
    if (I && (R = c(_, T, R, b)), _.length > 80 * b) {
      z = W = _[0], V = et = _[1];
      for (var Y = b; Y < S; Y += b)
        it = _[Y], K = _[Y + 1], it < z && (z = it), K < V && (V = K), it > W && (W = it), K > et && (et = K);
      j = Math.max(W - z, et - V), j = j !== 0 ? 32767 / j : 0;
    }
    return n(R, O, b, z, V, j, 0), O;
  }
  function s(_, T, b, I, S) {
    var R, O;
    if (S === Q(_, T, b, I) > 0) for (R = T; R < b; R += I) O = G(R, _[R], _[R + 1], O);
    else for (R = b - I; R >= T; R -= I) O = G(R, _[R], _[R + 1], O);
    return O && L(O, O.next) && ($(O), O = O.next), O;
  }
  function i(_, T) {
    if (!_) return _;
    T || (T = _);
    var b = _, I;
    do
      if (I = !1, !b.steiner && (L(b, b.next) || A(b.prev, b, b.next) === 0)) {
        if ($(b), b = T = b.prev, b === b.next) break;
        I = !0;
      } else b = b.next;
    while (I || b !== T);
    return T;
  }
  function n(_, T, b, I, S, R, O) {
    if (_) {
      !O && R && v(_, I, S, R);
      for (var z = _, V, W; _.prev !== _.next; ) {
        if (V = _.prev, W = _.next, R ? o(_, I, S, R) : a(_)) {
          T.push(V.i / b | 0), T.push(_.i / b | 0), T.push(W.i / b | 0), $(_), _ = W.next, z = W.next;
          continue;
        }
        if (_ = W, _ === z) {
          O ? O === 1 ? (_ = h(i(_), T, b), n(_, T, b, I, S, R, 2)) : O === 2 && l(_, T, b, I, S, R) : n(i(_), T, b, I, S, R, 1);
          break;
        }
      }
    }
  }
  function a(_) {
    var T = _.prev, b = _, I = _.next;
    if (A(T, b, I) >= 0) return !1;
    for (var S = T.x, R = b.x, O = I.x, z = T.y, V = b.y, W = I.y, et = S < R ? S < O ? S : O : R < O ? R : O, it = z < V ? z < W ? z : W : V < W ? V : W, K = S > R ? S > O ? S : O : R > O ? R : O, j = z > V ? z > W ? z : W : V > W ? V : W, Y = I.next; Y !== T; ) {
      if (Y.x >= et && Y.x <= K && Y.y >= it && Y.y <= j && E(S, z, R, V, O, W, Y.x, Y.y) && A(Y.prev, Y, Y.next) >= 0) return !1;
      Y = Y.next;
    }
    return !0;
  }
  function o(_, T, b, I) {
    var S = _.prev, R = _, O = _.next;
    if (A(S, R, O) >= 0) return !1;
    for (var z = S.x, V = R.x, W = O.x, et = S.y, it = R.y, K = O.y, j = z < V ? z < W ? z : W : V < W ? V : W, Y = et < it ? et < K ? et : K : it < K ? it : K, tt = z > V ? z > W ? z : W : V > W ? V : W, ht = et > it ? et > K ? et : K : it > K ? it : K, _t = x(j, Y, T, b, I), St = x(tt, ht, T, b, I), J = _.prevZ, nt = _.nextZ; J && J.z >= _t && nt && nt.z <= St; ) {
      if (J.x >= j && J.x <= tt && J.y >= Y && J.y <= ht && J !== S && J !== O && E(z, et, V, it, W, K, J.x, J.y) && A(J.prev, J, J.next) >= 0 || (J = J.prevZ, nt.x >= j && nt.x <= tt && nt.y >= Y && nt.y <= ht && nt !== S && nt !== O && E(z, et, V, it, W, K, nt.x, nt.y) && A(nt.prev, nt, nt.next) >= 0)) return !1;
      nt = nt.nextZ;
    }
    for (; J && J.z >= _t; ) {
      if (J.x >= j && J.x <= tt && J.y >= Y && J.y <= ht && J !== S && J !== O && E(z, et, V, it, W, K, J.x, J.y) && A(J.prev, J, J.next) >= 0) return !1;
      J = J.prevZ;
    }
    for (; nt && nt.z <= St; ) {
      if (nt.x >= j && nt.x <= tt && nt.y >= Y && nt.y <= ht && nt !== S && nt !== O && E(z, et, V, it, W, K, nt.x, nt.y) && A(nt.prev, nt, nt.next) >= 0) return !1;
      nt = nt.nextZ;
    }
    return !0;
  }
  function h(_, T, b) {
    var I = _;
    do {
      var S = I.prev, R = I.next.next;
      !L(S, R) && M(S, I, I.next, R) && w(S, R) && w(R, S) && (T.push(S.i / b | 0), T.push(I.i / b | 0), T.push(R.i / b | 0), $(I), $(I.next), I = _ = R), I = I.next;
    } while (I !== _);
    return i(I);
  }
  function l(_, T, b, I, S, R) {
    var O = _;
    do {
      for (var z = O.next.next; z !== O.prev; ) {
        if (O.i !== z.i && g(O, z)) {
          var V = P(O, z);
          O = i(O, O.next), V = i(V, V.next), n(O, T, b, I, S, R, 0), n(V, T, b, I, S, R, 0);
          return;
        }
        z = z.next;
      }
      O = O.next;
    } while (O !== _);
  }
  function c(_, T, b, I) {
    var S = [], R, O, z, V, W;
    for (R = 0, O = T.length; R < O; R++)
      z = T[R] * I, V = R < O - 1 ? T[R + 1] * I : _.length, W = s(_, z, V, I, !1), W === W.next && (W.steiner = !0), S.push(f(W));
    for (S.sort(u), R = 0; R < S.length; R++) b = d(S[R], b);
    return b;
  }
  function u(_, T) {
    return _.x - T.x;
  }
  function d(_, T) {
    var b = y(_, T);
    if (!b) return T;
    var I = P(b, _);
    return i(I, I.next), i(b, b.next);
  }
  function y(_, T) {
    var b = T, I = _.x, S = _.y, R = -1 / 0, O;
    do {
      if (S <= b.y && S >= b.next.y && b.next.y !== b.y) {
        var z = b.x + (S - b.y) * (b.next.x - b.x) / (b.next.y - b.y);
        if (z <= I && z > R && (R = z, O = b.x < b.next.x ? b : b.next, z === I))
          return O;
      }
      b = b.next;
    } while (b !== T);
    if (!O) return null;
    var V = O, W = O.x, et = O.y, it = 1 / 0, K;
    b = O;
    do
      I >= b.x && b.x >= W && I !== b.x && E(S < et ? I : R, S, W, et, S < et ? R : I, S, b.x, b.y) && (K = Math.abs(S - b.y) / (I - b.x), w(b, _) && (K < it || K === it && (b.x > O.x || b.x === O.x && m(O, b))) && (O = b, it = K)), b = b.next;
    while (b !== V);
    return O;
  }
  function m(_, T) {
    return A(_.prev, _, T.prev) < 0 && A(T.next, _, _.next) < 0;
  }
  function v(_, T, b, I) {
    var S = _;
    do
      S.z === 0 && (S.z = x(S.x, S.y, T, b, I)), S.prevZ = S.prev, S.nextZ = S.next, S = S.next;
    while (S !== _);
    S.prevZ.nextZ = null, S.prevZ = null, p(S);
  }
  function p(_) {
    var T, b, I, S, R, O, z, V, W = 1;
    do {
      for (b = _, _ = null, R = null, O = 0; b; ) {
        for (O++, I = b, z = 0, T = 0; T < W && (z++, I = I.nextZ, !!I); T++)
          ;
        for (V = W; z > 0 || V > 0 && I; )
          z !== 0 && (V === 0 || !I || b.z <= I.z) ? (S = b, b = b.nextZ, z--) : (S = I, I = I.nextZ, V--), R ? R.nextZ = S : _ = S, S.prevZ = R, R = S;
        b = I;
      }
      R.nextZ = null, W *= 2;
    } while (O > 1);
    return _;
  }
  function x(_, T, b, I, S) {
    return _ = (_ - b) * S | 0, T = (T - I) * S | 0, _ = (_ | _ << 8) & 16711935, _ = (_ | _ << 4) & 252645135, _ = (_ | _ << 2) & 858993459, _ = (_ | _ << 1) & 1431655765, T = (T | T << 8) & 16711935, T = (T | T << 4) & 252645135, T = (T | T << 2) & 858993459, T = (T | T << 1) & 1431655765, _ | T << 1;
  }
  function f(_) {
    var T = _, b = _;
    do
      (T.x < b.x || T.x === b.x && T.y < b.y) && (b = T), T = T.next;
    while (T !== _);
    return b;
  }
  function E(_, T, b, I, S, R, O, z) {
    return (S - O) * (T - z) >= (_ - O) * (R - z) && (_ - O) * (I - z) >= (b - O) * (T - z) && (b - O) * (R - z) >= (S - O) * (I - z);
  }
  function g(_, T) {
    return _.next.i !== T.i && _.prev.i !== T.i && !B(_, T) && (w(_, T) && w(T, _) && F(_, T) && (A(_.prev, _, T.prev) || A(_, T.prev, T)) || L(_, T) && A(_.prev, _, _.next) > 0 && A(T.prev, T, T.next) > 0);
  }
  function A(_, T, b) {
    return (T.y - _.y) * (b.x - T.x) - (T.x - _.x) * (b.y - T.y);
  }
  function L(_, T) {
    return _.x === T.x && _.y === T.y;
  }
  function M(_, T, b, I) {
    var S = k(A(_, T, b)), R = k(A(_, T, I)), O = k(A(b, I, _)), z = k(A(b, I, T));
    return !!(S !== R && O !== z || S === 0 && C(_, b, T) || R === 0 && C(_, I, T) || O === 0 && C(b, _, I) || z === 0 && C(b, T, I));
  }
  function C(_, T, b) {
    return T.x <= Math.max(_.x, b.x) && T.x >= Math.min(_.x, b.x) && T.y <= Math.max(_.y, b.y) && T.y >= Math.min(_.y, b.y);
  }
  function k(_) {
    return _ > 0 ? 1 : _ < 0 ? -1 : 0;
  }
  function B(_, T) {
    var b = _;
    do {
      if (b.i !== _.i && b.next.i !== _.i && b.i !== T.i && b.next.i !== T.i && M(b, b.next, _, T)) return !0;
      b = b.next;
    } while (b !== _);
    return !1;
  }
  function w(_, T) {
    return A(_.prev, _, _.next) < 0 ? A(_, T, _.next) >= 0 && A(_, _.prev, T) >= 0 : A(_, T, _.prev) < 0 || A(_, _.next, T) < 0;
  }
  function F(_, T) {
    var b = _, I = !1, S = (_.x + T.x) / 2, R = (_.y + T.y) / 2;
    do
      b.y > R != b.next.y > R && b.next.y !== b.y && S < (b.next.x - b.x) * (R - b.y) / (b.next.y - b.y) + b.x && (I = !I), b = b.next;
    while (b !== _);
    return I;
  }
  function P(_, T) {
    var b = new H(_.i, _.x, _.y), I = new H(T.i, T.x, T.y), S = _.next, R = T.prev;
    return _.next = T, T.prev = _, b.next = S, S.prev = b, I.next = b, b.prev = I, R.next = I, I.prev = R, I;
  }
  function G(_, T, b, I) {
    var S = new H(_, T, b);
    return I ? (S.next = I.next, S.prev = I, I.next.prev = S, I.next = S) : (S.prev = S, S.next = S), S;
  }
  function $(_) {
    _.next.prev = _.prev, _.prev.next = _.next, _.prevZ && (_.prevZ.nextZ = _.nextZ), _.nextZ && (_.nextZ.prevZ = _.prevZ);
  }
  function H(_, T, b) {
    this.i = _, this.x = T, this.y = b, this.prev = null, this.next = null, this.z = 0, this.prevZ = null, this.nextZ = null, this.steiner = !1;
  }
  r.deviation = function(_, T, b, I) {
    var S = T && T.length, R = S ? T[0] * b : _.length, O = Math.abs(Q(_, 0, R, b));
    if (S) for (var z = 0, V = T.length; z < V; z++) {
      var W = T[z] * b, et = z < V - 1 ? T[z + 1] * b : _.length;
      O -= Math.abs(Q(_, W, et, b));
    }
    var it = 0;
    for (z = 0; z < I.length; z += 3) {
      var K = I[z] * b, j = I[z + 1] * b, Y = I[z + 2] * b;
      it += Math.abs((_[K] - _[Y]) * (_[j + 1] - _[K + 1]) - (_[K] - _[j]) * (_[Y + 1] - _[K + 1]));
    }
    return O === 0 && it === 0 ? 0 : Math.abs((it - O) / O);
  };
  function Q(_, T, b, I) {
    for (var S = 0, R = T, O = b - I; R < b; R += I)
      S += (_[O] - _[R]) * (_[R + 1] + _[O + 1]), O = R;
    return S;
  }
  r.flatten = function(_) {
    for (var T = _[0][0].length, b = {
      vertices: [],
      holes: [],
      dimensions: T
    }, I = 0, S = 0; S < _.length; S++) {
      for (var R = 0; R < _[S].length; R++) for (var O = 0; O < T; O++) b.vertices.push(_[S][R][O]);
      S > 0 && (I += _[S - 1].length, b.holes.push(I));
    }
    return b;
  };
})), po = /* @__PURE__ */ rt(((e, t) => {
  (function(r) {
    var s = typeof e == "object" && e && !e.nodeType && e, i = typeof t == "object" && t && !t.nodeType && t, n = typeof globalThis == "object" && globalThis;
    (n.global === n || n.window === n || n.self === n) && (r = n);
    var a, o = 2147483647, h = 36, l = 1, c = 26, u = 38, d = 700, y = 72, m = 128, v = "-", p = /^xn--/, x = /[^\x20-\x7E]/, f = /[\x2E\u3002\uFF0E\uFF61]/g, E = {
      overflow: "Overflow: input needs wider integers to process",
      "not-basic": "Illegal input >= 0x80 (not a basic code point)",
      "invalid-input": "Invalid input"
    }, g = h - l, A = Math.floor, L = String.fromCharCode, M;
    function C(b) {
      throw new RangeError(E[b]);
    }
    function k(b, I) {
      for (var S = b.length, R = []; S--; ) R[S] = I(b[S]);
      return R;
    }
    function B(b, I) {
      var S = b.split("@"), R = "";
      S.length > 1 && (R = S[0] + "@", b = S[1]), b = b.replace(f, ".");
      var O = k(b.split("."), I).join(".");
      return R + O;
    }
    function w(b) {
      for (var I = [], S = 0, R = b.length, O, z; S < R; )
        O = b.charCodeAt(S++), O >= 55296 && O <= 56319 && S < R ? (z = b.charCodeAt(S++), (z & 64512) == 56320 ? I.push(((O & 1023) << 10) + (z & 1023) + 65536) : (I.push(O), S--)) : I.push(O);
      return I;
    }
    function F(b) {
      return k(b, function(I) {
        var S = "";
        return I > 65535 && (I -= 65536, S += L(I >>> 10 & 1023 | 55296), I = 56320 | I & 1023), S += L(I), S;
      }).join("");
    }
    function P(b) {
      return b - 48 < 10 ? b - 22 : b - 65 < 26 ? b - 65 : b - 97 < 26 ? b - 97 : h;
    }
    function G(b, I) {
      return b + 22 + 75 * (b < 26) - ((I != 0) << 5);
    }
    function $(b, I, S) {
      var R = 0;
      for (b = S ? A(b / d) : b >> 1, b += A(b / I); b > g * c >> 1; R += h) b = A(b / g);
      return A(R + (g + 1) * b / (b + u));
    }
    function H(b) {
      var I = [], S = b.length, R, O = 0, z = m, V = y, W = b.lastIndexOf(v), et, it, K, j, Y, tt, ht, _t;
      for (W < 0 && (W = 0), et = 0; et < W; ++et)
        b.charCodeAt(et) >= 128 && C("not-basic"), I.push(b.charCodeAt(et));
      for (it = W > 0 ? W + 1 : 0; it < S; ) {
        for (K = O, j = 1, Y = h; it >= S && C("invalid-input"), tt = P(b.charCodeAt(it++)), (tt >= h || tt > A((o - O) / j)) && C("overflow"), O += tt * j, ht = Y <= V ? l : Y >= V + c ? c : Y - V, !(tt < ht); Y += h)
          _t = h - ht, j > A(o / _t) && C("overflow"), j *= _t;
        R = I.length + 1, V = $(O - K, R, K == 0), A(O / R) > o - z && C("overflow"), z += A(O / R), O %= R, I.splice(O++, 0, z);
      }
      return F(I);
    }
    function Q(b) {
      var I, S, R, O, z, V, W, et, it, K, j, Y = [], tt, ht, _t, St;
      for (b = w(b), tt = b.length, I = m, S = 0, z = y, V = 0; V < tt; ++V)
        j = b[V], j < 128 && Y.push(L(j));
      for (R = O = Y.length, O && Y.push(v); R < tt; ) {
        for (W = o, V = 0; V < tt; ++V)
          j = b[V], j >= I && j < W && (W = j);
        for (ht = R + 1, W - I > A((o - S) / ht) && C("overflow"), S += (W - I) * ht, I = W, V = 0; V < tt; ++V)
          if (j = b[V], j < I && ++S > o && C("overflow"), j == I) {
            for (et = S, it = h; K = it <= z ? l : it >= z + c ? c : it - z, !(et < K); it += h)
              St = et - K, _t = h - K, Y.push(L(G(K + St % _t, 0))), et = A(St / _t);
            Y.push(L(G(et, 0))), z = $(S, ht, R == O), S = 0, ++R;
          }
        ++S, ++I;
      }
      return Y.join("");
    }
    function _(b) {
      return B(b, function(I) {
        return p.test(I) ? H(I.slice(4).toLowerCase()) : I;
      });
    }
    function T(b) {
      return B(b, function(I) {
        return x.test(I) ? "xn--" + Q(I) : I;
      });
    }
    if (a = {
      version: "1.4.1",
      ucs2: {
        decode: w,
        encode: F
      },
      decode: H,
      encode: Q,
      toASCII: T,
      toUnicode: _
    }, typeof define == "function" && typeof define.amd == "object" && define.amd) define("punycode", function() {
      return a;
    });
    else if (s && i)
      if (t.exports == s) i.exports = a;
      else for (M in a) a.hasOwnProperty(M) && (s[M] = a[M]);
    else r.punycode = a;
  })(e);
})), Ke = /* @__PURE__ */ rt(((e, t) => {
  t.exports = TypeError;
})), mo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = {};
})), ts = /* @__PURE__ */ rt(((e, t) => {
  var r = typeof Map == "function" && Map.prototype, s = Object.getOwnPropertyDescriptor && r ? Object.getOwnPropertyDescriptor(Map.prototype, "size") : null, i = r && s && typeof s.get == "function" ? s.get : null, n = r && Map.prototype.forEach, a = typeof Set == "function" && Set.prototype, o = Object.getOwnPropertyDescriptor && a ? Object.getOwnPropertyDescriptor(Set.prototype, "size") : null, h = a && o && typeof o.get == "function" ? o.get : null, l = a && Set.prototype.forEach, c = typeof WeakMap == "function" && WeakMap.prototype ? WeakMap.prototype.has : null, u = typeof WeakSet == "function" && WeakSet.prototype ? WeakSet.prototype.has : null, d = typeof WeakRef == "function" && WeakRef.prototype ? WeakRef.prototype.deref : null, y = Boolean.prototype.valueOf, m = Object.prototype.toString, v = Function.prototype.toString, p = String.prototype.match, x = String.prototype.slice, f = String.prototype.replace, E = String.prototype.toUpperCase, g = String.prototype.toLowerCase, A = RegExp.prototype.test, L = Array.prototype.concat, M = Array.prototype.join, C = Array.prototype.slice, k = Math.floor, B = typeof BigInt == "function" ? BigInt.prototype.valueOf : null, w = Object.getOwnPropertySymbols, F = typeof Symbol == "function" && typeof Symbol.iterator == "symbol" ? Symbol.prototype.toString : null, P = typeof Symbol == "function" && typeof Symbol.iterator == "object", G = typeof Symbol == "function" && Symbol.toStringTag && (typeof Symbol.toStringTag === P || !0) ? Symbol.toStringTag : null, $ = Object.prototype.propertyIsEnumerable, H = (typeof Reflect == "function" ? Reflect.getPrototypeOf : Object.getPrototypeOf) || ([].__proto__ === Array.prototype ? function(N) {
    return N.__proto__;
  } : null);
  function Q(N, U) {
    if (N === 1 / 0 || N === -1 / 0 || N !== N || N && N > -1e3 && N < 1e3 || A.call(/e/, U)) return U;
    var pt = /[0-9](?=(?:[0-9]{3})+(?![0-9]))/g;
    if (typeof N == "number") {
      var bt = N < 0 ? -k(-N) : k(N);
      if (bt !== N) {
        var It = String(bt), lt = x.call(U, It.length + 1);
        return f.call(It, pt, "$&_") + "." + f.call(f.call(lt, /([0-9]{3})/g, "$&_"), /_$/, "");
      }
    }
    return f.call(U, pt, "$&_");
  }
  var _ = mo(), T = _.custom, b = tt(T) ? T : null, I = {
    __proto__: null,
    double: '"',
    single: "'"
  }, S = {
    __proto__: null,
    double: /(["\\])/g,
    single: /(['\\])/g
  };
  t.exports = function N(U, pt, bt, It) {
    var lt = pt || {};
    if (St(lt, "quoteStyle") && !St(I, lt.quoteStyle)) throw new TypeError('option "quoteStyle" must be "single" or "double"');
    if (St(lt, "maxStringLength") && (typeof lt.maxStringLength == "number" ? lt.maxStringLength < 0 && lt.maxStringLength !== 1 / 0 : lt.maxStringLength !== null)) throw new TypeError('option "maxStringLength", if provided, must be a positive integer, Infinity, or `null`');
    var se = St(lt, "customInspect") ? lt.customInspect : !0;
    if (typeof se != "boolean" && se !== "symbol") throw new TypeError("option \"customInspect\", if provided, must be `true`, `false`, or `'symbol'`");
    if (St(lt, "indent") && lt.indent !== null && lt.indent !== "	" && !(parseInt(lt.indent, 10) === lt.indent && lt.indent > 0)) throw new TypeError('option "indent" must be "\\t", an integer > 0, or `null`');
    if (St(lt, "numericSeparator") && typeof lt.numericSeparator != "boolean") throw new TypeError('option "numericSeparator", if provided, must be `true` or `false`');
    var pe = lt.numericSeparator;
    if (typeof U > "u") return "undefined";
    if (U === null) return "null";
    if (typeof U == "boolean") return U ? "true" : "false";
    if (typeof U == "string") return ci(U, lt);
    if (typeof U == "number") {
      if (U === 0) return 1 / 0 / U > 0 ? "0" : "-0";
      var Nt = String(U);
      return pe ? Q(U, Nt) : Nt;
    }
    if (typeof U == "bigint") {
      var ie = String(U) + "n";
      return pe ? Q(U, ie) : ie;
    }
    var as = typeof lt.depth > "u" ? 5 : lt.depth;
    if (typeof bt > "u" && (bt = 0), bt >= as && as > 0 && typeof U == "object") return V(U) ? "[Array]" : "[Object]";
    var Be = eo(lt, bt);
    if (typeof It > "u") It = [];
    else if (Pe(It, U) >= 0) return "[Circular]";
    function $t(Ue, xr, ro) {
      if (xr && (It = C.call(It), It.push(xr)), ro) {
        var xi = { depth: lt.depth };
        return St(lt, "quoteStyle") && (xi.quoteStyle = lt.quoteStyle), N(Ue, xi, bt + 1, It);
      }
      return N(Ue, lt, bt + 1, It);
    }
    if (typeof U == "function" && !et(U)) {
      var di = nt(U), fi = gr(U, $t);
      return "[Function" + (di ? ": " + di : " (anonymous)") + "]" + (fi.length > 0 ? " { " + M.call(fi, ", ") + " }" : "");
    }
    if (tt(U)) {
      var pi = P ? f.call(String(U), /^(Symbol\(.*\))_[^)]*$/, "$1") : F.call(U);
      return typeof U == "object" && !P ? Je(pi) : pi;
    }
    if (Oe(U)) {
      for (var Qe = "<" + g.call(String(U.nodeName)), os = U.attributes || [], vr = 0; vr < os.length; vr++) Qe += " " + os[vr].name + "=" + R(O(os[vr].value), "double", lt);
      return Qe += ">", U.childNodes && U.childNodes.length && (Qe += "..."), Qe += "</" + g.call(String(U.nodeName)) + ">", Qe;
    }
    if (V(U)) {
      if (U.length === 0) return "[]";
      var hs = gr(U, $t);
      return Be && !to(hs) ? "[" + ns(hs, Be) + "]" : "[ " + M.call(hs, ", ") + " ]";
    }
    if (it(U)) {
      var ls = gr(U, $t);
      return !("cause" in Error.prototype) && "cause" in U && !$.call(U, "cause") ? "{ [" + String(U) + "] " + M.call(L.call("[cause]: " + $t(U.cause), ls), ", ") + " }" : ls.length === 0 ? "[" + String(U) + "]" : "{ [" + String(U) + "] " + M.call(ls, ", ") + " }";
    }
    if (typeof U == "object" && se) {
      if (b && typeof U[b] == "function" && _) return _(U, { depth: as - bt });
      if (se !== "symbol" && typeof U.inspect == "function") return U.inspect();
    }
    if (Le(U)) {
      var mi = [];
      return n && n.call(U, function(Ue, xr) {
        mi.push($t(xr, U, !0) + " => " + $t(Ue, U));
      }), ui("Map", i.call(U), mi, Be);
    }
    if (Fe(U)) {
      var yi = [];
      return l && l.call(U, function(Ue) {
        yi.push($t(Ue, U));
      }), ui("Set", h.call(U), yi, Be);
    }
    if (fe(U)) return is("WeakMap");
    if (Ne(U)) return is("WeakSet");
    if (zt(U)) return is("WeakRef");
    if (j(U)) return Je($t(Number(U)));
    if (ht(U)) return Je($t(B.call(U)));
    if (Y(U)) return Je(y.call(U));
    if (K(U)) return Je($t(String(U)));
    if (typeof window < "u" && U === window) return "{ [object Window] }";
    if (typeof globalThis < "u" && U === globalThis || typeof globalThis < "u" && U === globalThis) return "{ [object globalThis] }";
    if (!W(U) && !et(U)) {
      var cs = gr(U, $t), gi = H ? H(U) === Object.prototype : U instanceof Object || U.constructor === Object, us = U instanceof Object ? "" : "null prototype", vi = !gi && G && Object(U) === U && G in U ? x.call(J(U), 8, -1) : us ? "Object" : "", ds = (gi || typeof U.constructor != "function" ? "" : U.constructor.name ? U.constructor.name + " " : "") + (vi || us ? "[" + M.call(L.call([], vi || [], us || []), ": ") + "] " : "");
      return cs.length === 0 ? ds + "{}" : Be ? ds + "{" + ns(cs, Be) + "}" : ds + "{ " + M.call(cs, ", ") + " }";
    }
    return String(U);
  };
  function R(N, U, pt) {
    var bt = I[pt.quoteStyle || U];
    return bt + N + bt;
  }
  function O(N) {
    return f.call(String(N), /"/g, "&quot;");
  }
  function z(N) {
    return !G || !(typeof N == "object" && (G in N || typeof N[G] < "u"));
  }
  function V(N) {
    return J(N) === "[object Array]" && z(N);
  }
  function W(N) {
    return J(N) === "[object Date]" && z(N);
  }
  function et(N) {
    return J(N) === "[object RegExp]" && z(N);
  }
  function it(N) {
    return J(N) === "[object Error]" && z(N);
  }
  function K(N) {
    return J(N) === "[object String]" && z(N);
  }
  function j(N) {
    return J(N) === "[object Number]" && z(N);
  }
  function Y(N) {
    return J(N) === "[object Boolean]" && z(N);
  }
  function tt(N) {
    if (P) return N && typeof N == "object" && N instanceof Symbol;
    if (typeof N == "symbol") return !0;
    if (!N || typeof N != "object" || !F) return !1;
    try {
      return F.call(N), !0;
    } catch {
    }
    return !1;
  }
  function ht(N) {
    if (!N || typeof N != "object" || !B) return !1;
    try {
      return B.call(N), !0;
    } catch {
    }
    return !1;
  }
  var _t = Object.prototype.hasOwnProperty || function(N) {
    return N in this;
  };
  function St(N, U) {
    return _t.call(N, U);
  }
  function J(N) {
    return m.call(N);
  }
  function nt(N) {
    if (N.name) return N.name;
    var U = p.call(v.call(N), /^function\s*([\w$]+)/);
    return U ? U[1] : null;
  }
  function Pe(N, U) {
    if (N.indexOf) return N.indexOf(U);
    for (var pt = 0, bt = N.length; pt < bt; pt++) if (N[pt] === U) return pt;
    return -1;
  }
  function Le(N) {
    if (!i || !N || typeof N != "object") return !1;
    try {
      i.call(N);
      try {
        h.call(N);
      } catch {
        return !0;
      }
      return N instanceof Map;
    } catch {
    }
    return !1;
  }
  function fe(N) {
    if (!c || !N || typeof N != "object") return !1;
    try {
      c.call(N, c);
      try {
        u.call(N, u);
      } catch {
        return !0;
      }
      return N instanceof WeakMap;
    } catch {
    }
    return !1;
  }
  function zt(N) {
    if (!d || !N || typeof N != "object") return !1;
    try {
      return d.call(N), !0;
    } catch {
    }
    return !1;
  }
  function Fe(N) {
    if (!h || !N || typeof N != "object") return !1;
    try {
      h.call(N);
      try {
        i.call(N);
      } catch {
        return !0;
      }
      return N instanceof Set;
    } catch {
    }
    return !1;
  }
  function Ne(N) {
    if (!u || !N || typeof N != "object") return !1;
    try {
      u.call(N, u);
      try {
        c.call(N, c);
      } catch {
        return !0;
      }
      return N instanceof WeakSet;
    } catch {
    }
    return !1;
  }
  function Oe(N) {
    return !N || typeof N != "object" ? !1 : typeof HTMLElement < "u" && N instanceof HTMLElement ? !0 : typeof N.nodeName == "string" && typeof N.getAttribute == "function";
  }
  function ci(N, U) {
    if (N.length > U.maxStringLength) {
      var pt = N.length - U.maxStringLength, bt = "... " + pt + " more character" + (pt > 1 ? "s" : "");
      return ci(x.call(N, 0, U.maxStringLength), U) + bt;
    }
    var It = S[U.quoteStyle || "single"];
    return It.lastIndex = 0, R(f.call(f.call(N, It, "\\$1"), /[\x00-\x1f]/g, Qa), "single", U);
  }
  function Qa(N) {
    var U = N.charCodeAt(0), pt = {
      8: "b",
      9: "t",
      10: "n",
      12: "f",
      13: "r"
    }[U];
    return pt ? "\\" + pt : "\\x" + (U < 16 ? "0" : "") + E.call(U.toString(16));
  }
  function Je(N) {
    return "Object(" + N + ")";
  }
  function is(N) {
    return N + " { ? }";
  }
  function ui(N, U, pt, bt) {
    var It = bt ? ns(pt, bt) : M.call(pt, ", ");
    return N + " (" + U + ") {" + It + "}";
  }
  function to(N) {
    for (var U = 0; U < N.length; U++) if (Pe(N[U], `
`) >= 0) return !1;
    return !0;
  }
  function eo(N, U) {
    var pt;
    if (N.indent === "	") pt = "	";
    else if (typeof N.indent == "number" && N.indent > 0) pt = M.call(Array(N.indent + 1), " ");
    else return null;
    return {
      base: pt,
      prev: M.call(Array(U + 1), pt)
    };
  }
  function ns(N, U) {
    if (N.length === 0) return "";
    var pt = `
` + U.prev + U.base;
    return pt + M.call(N, "," + pt) + `
` + U.prev;
  }
  function gr(N, U) {
    var pt = V(N), bt = [];
    if (pt) {
      bt.length = N.length;
      for (var It = 0; It < N.length; It++) bt[It] = St(N, It) ? U(N[It], N) : "";
    }
    var lt = typeof w == "function" ? w(N) : [], se;
    if (P) {
      se = {};
      for (var pe = 0; pe < lt.length; pe++) se["$" + lt[pe]] = lt[pe];
    }
    for (var Nt in N)
      St(N, Nt) && (pt && String(Number(Nt)) === Nt && Nt < N.length || P && se["$" + Nt] instanceof Symbol || (A.call(/[^\w$]/, Nt) ? bt.push(U(Nt, N) + ": " + U(N[Nt], N)) : bt.push(Nt + ": " + U(N[Nt], N))));
    if (typeof w == "function")
      for (var ie = 0; ie < lt.length; ie++) $.call(N, lt[ie]) && bt.push("[" + U(lt[ie]) + "]: " + U(N[lt[ie]], N));
    return bt;
  }
})), yo = /* @__PURE__ */ rt(((e, t) => {
  var r = ts(), s = Ke(), i = function(l, c, u) {
    for (var d = l, y; (y = d.next) != null; d = y) if (y.key === c)
      return d.next = y.next, u || (y.next = l.next, l.next = y), y;
  }, n = function(l, c) {
    if (l) {
      var u = i(l, c);
      return u && u.value;
    }
  }, a = function(l, c, u) {
    var d = i(l, c);
    d ? d.value = u : l.next = {
      key: c,
      next: l.next,
      value: u
    };
  }, o = function(l, c) {
    return l ? !!i(l, c) : !1;
  }, h = function(l, c) {
    if (l) return i(l, c, !0);
  };
  t.exports = function() {
    var c, u = {
      assert: function(d) {
        if (!u.has(d)) throw new s("Side channel does not contain " + r(d));
      },
      delete: function(d) {
        var y = h(c, d);
        return y && c && !c.next && (c = void 0), !!y;
      },
      get: function(d) {
        return n(c, d);
      },
      has: function(d) {
        return o(c, d);
      },
      set: function(d, y) {
        c || (c = { next: void 0 }), a(c, d, y);
      }
    };
    return u;
  };
})), Sn = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Object;
})), go = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Error;
})), vo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = EvalError;
})), xo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = RangeError;
})), _o = /* @__PURE__ */ rt(((e, t) => {
  t.exports = ReferenceError;
})), bo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = SyntaxError;
})), To = /* @__PURE__ */ rt(((e, t) => {
  t.exports = URIError;
})), wo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.abs;
})), Eo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.floor;
})), So = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.max;
})), Ao = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.min;
})), Io = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.pow;
})), Ro = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Math.round;
})), Mo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Number.isNaN || function(s) {
    return s !== s;
  };
})), Co = /* @__PURE__ */ rt(((e, t) => {
  var r = Mo();
  t.exports = function(i) {
    return r(i) || i === 0 ? i : i < 0 ? -1 : 1;
  };
})), Po = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Object.getOwnPropertyDescriptor;
})), An = /* @__PURE__ */ rt(((e, t) => {
  var r = Po();
  if (r) try {
    r([], "length");
  } catch {
    r = null;
  }
  t.exports = r;
})), In = /* @__PURE__ */ rt(((e, t) => {
  var r = Object.defineProperty || !1;
  if (r) try {
    r({}, "a", { value: 1 });
  } catch {
    r = !1;
  }
  t.exports = r;
})), Lo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = function() {
    if (typeof Symbol != "function" || typeof Object.getOwnPropertySymbols != "function") return !1;
    if (typeof Symbol.iterator == "symbol") return !0;
    var s = {}, i = /* @__PURE__ */ Symbol("test"), n = Object(i);
    if (typeof i == "string" || Object.prototype.toString.call(i) !== "[object Symbol]" || Object.prototype.toString.call(n) !== "[object Symbol]") return !1;
    var a = 42;
    s[i] = a;
    for (var o in s) return !1;
    if (typeof Object.keys == "function" && Object.keys(s).length !== 0 || typeof Object.getOwnPropertyNames == "function" && Object.getOwnPropertyNames(s).length !== 0) return !1;
    var h = Object.getOwnPropertySymbols(s);
    if (h.length !== 1 || h[0] !== i || !Object.prototype.propertyIsEnumerable.call(s, i)) return !1;
    if (typeof Object.getOwnPropertyDescriptor == "function") {
      var l = Object.getOwnPropertyDescriptor(s, i);
      if (l.value !== a || l.enumerable !== !0) return !1;
    }
    return !0;
  };
})), Fo = /* @__PURE__ */ rt(((e, t) => {
  var r = typeof Symbol < "u" && Symbol, s = Lo();
  t.exports = function() {
    return typeof r != "function" || typeof Symbol != "function" || typeof r("foo") != "symbol" || typeof /* @__PURE__ */ Symbol("bar") != "symbol" ? !1 : s();
  };
})), Rn = /* @__PURE__ */ rt(((e, t) => {
  t.exports = typeof Reflect < "u" && Reflect.getPrototypeOf || null;
})), Mn = /* @__PURE__ */ rt(((e, t) => {
  var r = Sn();
  t.exports = r.getPrototypeOf || null;
})), No = /* @__PURE__ */ rt(((e, t) => {
  var r = "Function.prototype.bind called on incompatible ", s = Object.prototype.toString, i = Math.max, n = "[object Function]", a = function(c, u) {
    for (var d = [], y = 0; y < c.length; y += 1) d[y] = c[y];
    for (var m = 0; m < u.length; m += 1) d[m + c.length] = u[m];
    return d;
  }, o = function(c, u) {
    for (var d = [], y = u || 0, m = 0; y < c.length; y += 1, m += 1) d[m] = c[y];
    return d;
  }, h = function(l, c) {
    for (var u = "", d = 0; d < l.length; d += 1)
      u += l[d], d + 1 < l.length && (u += c);
    return u;
  };
  t.exports = function(c) {
    var u = this;
    if (typeof u != "function" || s.apply(u) !== n) throw new TypeError(r + u);
    for (var d = o(arguments, 1), y, m = function() {
      if (this instanceof y) {
        var E = u.apply(this, a(d, arguments));
        return Object(E) === E ? E : this;
      }
      return u.apply(c, a(d, arguments));
    }, v = i(0, u.length - d.length), p = [], x = 0; x < v; x++) p[x] = "$" + x;
    if (y = Function("binder", "return function (" + h(p, ",") + "){ return binder.apply(this,arguments); }")(m), u.prototype) {
      var f = function() {
      };
      f.prototype = u.prototype, y.prototype = new f(), f.prototype = null;
    }
    return y;
  };
})), es = /* @__PURE__ */ rt(((e, t) => {
  var r = No();
  t.exports = Function.prototype.bind || r;
})), ti = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Function.prototype.call;
})), Cn = /* @__PURE__ */ rt(((e, t) => {
  t.exports = Function.prototype.apply;
})), Oo = /* @__PURE__ */ rt(((e, t) => {
  t.exports = typeof Reflect < "u" && Reflect && Reflect.apply;
})), Bo = /* @__PURE__ */ rt(((e, t) => {
  var r = es(), s = Cn(), i = ti(), n = Oo();
  t.exports = n || r.call(i, s);
})), Pn = /* @__PURE__ */ rt(((e, t) => {
  var r = es(), s = Ke(), i = ti(), n = Bo();
  t.exports = function(o) {
    if (o.length < 1 || typeof o[0] != "function") throw new s("a function is required");
    return n(r, i, o);
  };
})), Uo = /* @__PURE__ */ rt(((e, t) => {
  var r = Pn(), s = An(), i;
  try {
    i = [].__proto__ === Array.prototype;
  } catch (h) {
    if (!h || typeof h != "object" || !("code" in h) || h.code !== "ERR_PROTO_ACCESS") throw h;
  }
  var n = !!i && s && s(Object.prototype, "__proto__"), a = Object, o = a.getPrototypeOf;
  t.exports = n && typeof n.get == "function" ? r([n.get]) : typeof o == "function" ? function(l) {
    return o(l == null ? l : a(l));
  } : !1;
})), ko = /* @__PURE__ */ rt(((e, t) => {
  var r = Rn(), s = Mn(), i = Uo();
  t.exports = r ? function(a) {
    return r(a);
  } : s ? function(a) {
    if (!a || typeof a != "object" && typeof a != "function") throw new TypeError("getProto: not an object");
    return s(a);
  } : i ? function(a) {
    return i(a);
  } : null;
})), Do = /* @__PURE__ */ rt(((e, t) => {
  var r = Function.prototype.call, s = Object.prototype.hasOwnProperty, i = es();
  t.exports = i.call(r, s);
})), ei = /* @__PURE__ */ rt(((e, t) => {
  var r, s = Sn(), i = go(), n = vo(), a = xo(), o = _o(), h = bo(), l = Ke(), c = To(), u = wo(), d = Eo(), y = So(), m = Ao(), v = Io(), p = Ro(), x = Co(), f = Function, E = function(K) {
    try {
      return f('"use strict"; return (' + K + ").constructor;")();
    } catch {
    }
  }, g = An(), A = In(), L = function() {
    throw new l();
  }, M = g ? (function() {
    try {
      return arguments.callee, L;
    } catch {
      try {
        return g(arguments, "callee").get;
      } catch {
        return L;
      }
    }
  })() : L, C = Fo()(), k = ko(), B = Mn(), w = Rn(), F = Cn(), P = ti(), G = {}, $ = typeof Uint8Array > "u" || !k ? r : k(Uint8Array), H = {
    __proto__: null,
    "%AggregateError%": typeof AggregateError > "u" ? r : AggregateError,
    "%Array%": Array,
    "%ArrayBuffer%": typeof ArrayBuffer > "u" ? r : ArrayBuffer,
    "%ArrayIteratorPrototype%": C && k ? k([][Symbol.iterator]()) : r,
    "%AsyncFromSyncIteratorPrototype%": r,
    "%AsyncFunction%": G,
    "%AsyncGenerator%": G,
    "%AsyncGeneratorFunction%": G,
    "%AsyncIteratorPrototype%": G,
    "%Atomics%": typeof Atomics > "u" ? r : Atomics,
    "%BigInt%": typeof BigInt > "u" ? r : BigInt,
    "%BigInt64Array%": typeof BigInt64Array > "u" ? r : BigInt64Array,
    "%BigUint64Array%": typeof BigUint64Array > "u" ? r : BigUint64Array,
    "%Boolean%": Boolean,
    "%DataView%": typeof DataView > "u" ? r : DataView,
    "%Date%": Date,
    "%decodeURI%": decodeURI,
    "%decodeURIComponent%": decodeURIComponent,
    "%encodeURI%": encodeURI,
    "%encodeURIComponent%": encodeURIComponent,
    "%Error%": i,
    "%eval%": eval,
    "%EvalError%": n,
    "%Float16Array%": typeof Float16Array > "u" ? r : Float16Array,
    "%Float32Array%": typeof Float32Array > "u" ? r : Float32Array,
    "%Float64Array%": typeof Float64Array > "u" ? r : Float64Array,
    "%FinalizationRegistry%": typeof FinalizationRegistry > "u" ? r : FinalizationRegistry,
    "%Function%": f,
    "%GeneratorFunction%": G,
    "%Int8Array%": typeof Int8Array > "u" ? r : Int8Array,
    "%Int16Array%": typeof Int16Array > "u" ? r : Int16Array,
    "%Int32Array%": typeof Int32Array > "u" ? r : Int32Array,
    "%isFinite%": isFinite,
    "%isNaN%": isNaN,
    "%IteratorPrototype%": C && k ? k(k([][Symbol.iterator]())) : r,
    "%JSON%": typeof JSON == "object" ? JSON : r,
    "%Map%": typeof Map > "u" ? r : Map,
    "%MapIteratorPrototype%": typeof Map > "u" || !C || !k ? r : k((/* @__PURE__ */ new Map())[Symbol.iterator]()),
    "%Math%": Math,
    "%Number%": Number,
    "%Object%": s,
    "%Object.getOwnPropertyDescriptor%": g,
    "%parseFloat%": parseFloat,
    "%parseInt%": parseInt,
    "%Promise%": typeof Promise > "u" ? r : Promise,
    "%Proxy%": typeof Proxy > "u" ? r : Proxy,
    "%RangeError%": a,
    "%ReferenceError%": o,
    "%Reflect%": typeof Reflect > "u" ? r : Reflect,
    "%RegExp%": RegExp,
    "%Set%": typeof Set > "u" ? r : Set,
    "%SetIteratorPrototype%": typeof Set > "u" || !C || !k ? r : k((/* @__PURE__ */ new Set())[Symbol.iterator]()),
    "%SharedArrayBuffer%": typeof SharedArrayBuffer > "u" ? r : SharedArrayBuffer,
    "%String%": String,
    "%StringIteratorPrototype%": C && k ? k(""[Symbol.iterator]()) : r,
    "%Symbol%": C ? Symbol : r,
    "%SyntaxError%": h,
    "%ThrowTypeError%": M,
    "%TypedArray%": $,
    "%TypeError%": l,
    "%Uint8Array%": typeof Uint8Array > "u" ? r : Uint8Array,
    "%Uint8ClampedArray%": typeof Uint8ClampedArray > "u" ? r : Uint8ClampedArray,
    "%Uint16Array%": typeof Uint16Array > "u" ? r : Uint16Array,
    "%Uint32Array%": typeof Uint32Array > "u" ? r : Uint32Array,
    "%URIError%": c,
    "%WeakMap%": typeof WeakMap > "u" ? r : WeakMap,
    "%WeakRef%": typeof WeakRef > "u" ? r : WeakRef,
    "%WeakSet%": typeof WeakSet > "u" ? r : WeakSet,
    "%Function.prototype.call%": P,
    "%Function.prototype.apply%": F,
    "%Object.defineProperty%": A,
    "%Object.getPrototypeOf%": B,
    "%Math.abs%": u,
    "%Math.floor%": d,
    "%Math.max%": y,
    "%Math.min%": m,
    "%Math.pow%": v,
    "%Math.round%": p,
    "%Math.sign%": x,
    "%Reflect.getPrototypeOf%": w
  };
  if (k) try {
    null.error;
  } catch (K) {
    H["%Error.prototype%"] = k(k(K));
  }
  var Q = function K(j) {
    var Y;
    if (j === "%AsyncFunction%") Y = E("async function () {}");
    else if (j === "%GeneratorFunction%") Y = E("function* () {}");
    else if (j === "%AsyncGeneratorFunction%") Y = E("async function* () {}");
    else if (j === "%AsyncGenerator%") {
      var tt = K("%AsyncGeneratorFunction%");
      tt && (Y = tt.prototype);
    } else if (j === "%AsyncIteratorPrototype%") {
      var ht = K("%AsyncGenerator%");
      ht && k && (Y = k(ht.prototype));
    }
    return H[j] = Y, Y;
  }, _ = {
    __proto__: null,
    "%ArrayBufferPrototype%": ["ArrayBuffer", "prototype"],
    "%ArrayPrototype%": ["Array", "prototype"],
    "%ArrayProto_entries%": [
      "Array",
      "prototype",
      "entries"
    ],
    "%ArrayProto_forEach%": [
      "Array",
      "prototype",
      "forEach"
    ],
    "%ArrayProto_keys%": [
      "Array",
      "prototype",
      "keys"
    ],
    "%ArrayProto_values%": [
      "Array",
      "prototype",
      "values"
    ],
    "%AsyncFunctionPrototype%": ["AsyncFunction", "prototype"],
    "%AsyncGenerator%": ["AsyncGeneratorFunction", "prototype"],
    "%AsyncGeneratorPrototype%": [
      "AsyncGeneratorFunction",
      "prototype",
      "prototype"
    ],
    "%BooleanPrototype%": ["Boolean", "prototype"],
    "%DataViewPrototype%": ["DataView", "prototype"],
    "%DatePrototype%": ["Date", "prototype"],
    "%ErrorPrototype%": ["Error", "prototype"],
    "%EvalErrorPrototype%": ["EvalError", "prototype"],
    "%Float32ArrayPrototype%": ["Float32Array", "prototype"],
    "%Float64ArrayPrototype%": ["Float64Array", "prototype"],
    "%FunctionPrototype%": ["Function", "prototype"],
    "%Generator%": ["GeneratorFunction", "prototype"],
    "%GeneratorPrototype%": [
      "GeneratorFunction",
      "prototype",
      "prototype"
    ],
    "%Int8ArrayPrototype%": ["Int8Array", "prototype"],
    "%Int16ArrayPrototype%": ["Int16Array", "prototype"],
    "%Int32ArrayPrototype%": ["Int32Array", "prototype"],
    "%JSONParse%": ["JSON", "parse"],
    "%JSONStringify%": ["JSON", "stringify"],
    "%MapPrototype%": ["Map", "prototype"],
    "%NumberPrototype%": ["Number", "prototype"],
    "%ObjectPrototype%": ["Object", "prototype"],
    "%ObjProto_toString%": [
      "Object",
      "prototype",
      "toString"
    ],
    "%ObjProto_valueOf%": [
      "Object",
      "prototype",
      "valueOf"
    ],
    "%PromisePrototype%": ["Promise", "prototype"],
    "%PromiseProto_then%": [
      "Promise",
      "prototype",
      "then"
    ],
    "%Promise_all%": ["Promise", "all"],
    "%Promise_reject%": ["Promise", "reject"],
    "%Promise_resolve%": ["Promise", "resolve"],
    "%RangeErrorPrototype%": ["RangeError", "prototype"],
    "%ReferenceErrorPrototype%": ["ReferenceError", "prototype"],
    "%RegExpPrototype%": ["RegExp", "prototype"],
    "%SetPrototype%": ["Set", "prototype"],
    "%SharedArrayBufferPrototype%": ["SharedArrayBuffer", "prototype"],
    "%StringPrototype%": ["String", "prototype"],
    "%SymbolPrototype%": ["Symbol", "prototype"],
    "%SyntaxErrorPrototype%": ["SyntaxError", "prototype"],
    "%TypedArrayPrototype%": ["TypedArray", "prototype"],
    "%TypeErrorPrototype%": ["TypeError", "prototype"],
    "%Uint8ArrayPrototype%": ["Uint8Array", "prototype"],
    "%Uint8ClampedArrayPrototype%": ["Uint8ClampedArray", "prototype"],
    "%Uint16ArrayPrototype%": ["Uint16Array", "prototype"],
    "%Uint32ArrayPrototype%": ["Uint32Array", "prototype"],
    "%URIErrorPrototype%": ["URIError", "prototype"],
    "%WeakMapPrototype%": ["WeakMap", "prototype"],
    "%WeakSetPrototype%": ["WeakSet", "prototype"]
  }, T = es(), b = Do(), I = T.call(P, Array.prototype.concat), S = T.call(F, Array.prototype.splice), R = T.call(P, String.prototype.replace), O = T.call(P, String.prototype.slice), z = T.call(P, RegExp.prototype.exec), V = /[^%.[\]]+|\[(?:(-?\d+(?:\.\d+)?)|(["'])((?:(?!\2)[^\\]|\\.)*?)\2)\]|(?=(?:\.|\[\])(?:\.|\[\]|%$))/g, W = /\\(\\)?/g, et = function(j) {
    var Y = O(j, 0, 1), tt = O(j, -1);
    if (Y === "%" && tt !== "%") throw new h("invalid intrinsic syntax, expected closing `%`");
    if (tt === "%" && Y !== "%") throw new h("invalid intrinsic syntax, expected opening `%`");
    var ht = [];
    return R(j, V, function(_t, St, J, nt) {
      ht[ht.length] = J ? R(nt, W, "$1") : St || _t;
    }), ht;
  }, it = function(j, Y) {
    var tt = j, ht;
    if (b(_, tt) && (ht = _[tt], tt = "%" + ht[0] + "%"), b(H, tt)) {
      var _t = H[tt];
      if (_t === G && (_t = Q(tt)), typeof _t > "u" && !Y) throw new l("intrinsic " + j + " exists, but is not available. Please file an issue!");
      return {
        alias: ht,
        name: tt,
        value: _t
      };
    }
    throw new h("intrinsic " + j + " does not exist!");
  };
  t.exports = function(j, Y) {
    if (typeof j != "string" || j.length === 0) throw new l("intrinsic name must be a non-empty string");
    if (arguments.length > 1 && typeof Y != "boolean") throw new l('"allowMissing" argument must be a boolean');
    if (z(/^%?[^%]*%?$/, j) === null) throw new h("`%` may not be present anywhere but at the beginning and end of the intrinsic name");
    var tt = et(j), ht = tt.length > 0 ? tt[0] : "", _t = it("%" + ht + "%", Y), St = _t.name, J = _t.value, nt = !1, Pe = _t.alias;
    Pe && (ht = Pe[0], S(tt, I([0, 1], Pe)));
    for (var Le = 1, fe = !0; Le < tt.length; Le += 1) {
      var zt = tt[Le], Fe = O(zt, 0, 1), Ne = O(zt, -1);
      if ((Fe === '"' || Fe === "'" || Fe === "`" || Ne === '"' || Ne === "'" || Ne === "`") && Fe !== Ne) throw new h("property names with quotes must have matching quotes");
      if ((zt === "constructor" || !fe) && (nt = !0), ht += "." + zt, St = "%" + ht + "%", b(H, St)) J = H[St];
      else if (J != null) {
        if (!(zt in J)) {
          if (!Y) throw new l("base intrinsic for " + j + " exists, but the property is not available.");
          return;
        }
        if (g && Le + 1 >= tt.length) {
          var Oe = g(J, zt);
          fe = !!Oe, fe && "get" in Oe && !("originalValue" in Oe.get) ? J = Oe.get : J = J[zt];
        } else
          fe = b(J, zt), J = J[zt];
        fe && !nt && (H[St] = J);
      }
    }
    return J;
  };
})), Ln = /* @__PURE__ */ rt(((e, t) => {
  var r = ei(), s = Pn(), i = s([r("%String.prototype.indexOf%")]);
  t.exports = function(a, o) {
    var h = r(a, !!o);
    return typeof h == "function" && i(a, ".prototype.") > -1 ? s([h]) : h;
  };
})), Fn = /* @__PURE__ */ rt(((e, t) => {
  var r = ei(), s = Ln(), i = ts(), n = Ke(), a = r("%Map%", !0), o = s("Map.prototype.get", !0), h = s("Map.prototype.set", !0), l = s("Map.prototype.has", !0), c = s("Map.prototype.delete", !0), u = s("Map.prototype.size", !0);
  t.exports = !!a && function() {
    var y, m = {
      assert: function(v) {
        if (!m.has(v)) throw new n("Side channel does not contain " + i(v));
      },
      delete: function(v) {
        if (y) {
          var p = c(y, v);
          return u(y) === 0 && (y = void 0), p;
        }
        return !1;
      },
      get: function(v) {
        if (y) return o(y, v);
      },
      has: function(v) {
        return y ? l(y, v) : !1;
      },
      set: function(v, p) {
        y || (y = new a()), h(y, v, p);
      }
    };
    return m;
  };
})), Go = /* @__PURE__ */ rt(((e, t) => {
  var r = ei(), s = Ln(), i = ts(), n = Fn(), a = Ke(), o = r("%WeakMap%", !0), h = s("WeakMap.prototype.get", !0), l = s("WeakMap.prototype.set", !0), c = s("WeakMap.prototype.has", !0), u = s("WeakMap.prototype.delete", !0);
  t.exports = o ? function() {
    var y, m, v = {
      assert: function(p) {
        if (!v.has(p)) throw new a("Side channel does not contain " + i(p));
      },
      delete: function(p) {
        if (o && p && (typeof p == "object" || typeof p == "function")) {
          if (y) return u(y, p);
        } else if (n && m)
          return m.delete(p);
        return !1;
      },
      get: function(p) {
        return o && p && (typeof p == "object" || typeof p == "function") && y ? h(y, p) : m && m.get(p);
      },
      has: function(p) {
        return o && p && (typeof p == "object" || typeof p == "function") && y ? c(y, p) : !!m && m.has(p);
      },
      set: function(p, x) {
        o && p && (typeof p == "object" || typeof p == "function") ? (y || (y = new o()), l(y, p, x)) : n && (m || (m = n()), m.set(p, x));
      }
    };
    return v;
  } : n;
})), Nn = /* @__PURE__ */ rt(((e, t) => {
  var r = Ke(), s = ts(), i = yo(), n = Fn(), a = Go() || n || i;
  t.exports = function() {
    var h, l = {
      assert: function(c) {
        if (!l.has(c)) throw new r("Side channel does not contain " + (c && Object(c) === c ? "the given object key" : s(c)));
      },
      delete: function(c) {
        return !!h && h.delete(c);
      },
      get: function(c) {
        return h && h.get(c);
      },
      has: function(c) {
        return !!h && h.has(c);
      },
      set: function(c, u) {
        h || (h = a()), h.set(c, u);
      }
    };
    return l;
  };
})), ri = /* @__PURE__ */ rt(((e, t) => {
  var r = String.prototype.replace, s = /%20/g, i = {
    RFC1738: "RFC1738",
    RFC3986: "RFC3986"
  };
  t.exports = {
    default: i.RFC3986,
    formatters: {
      RFC1738: function(n) {
        return r.call(n, s, "+");
      },
      RFC3986: function(n) {
        return String(n);
      }
    },
    RFC1738: i.RFC1738,
    RFC3986: i.RFC3986
  };
})), On = /* @__PURE__ */ rt(((e, t) => {
  var r = ri(), s = Nn(), i = In(), n = Object.prototype.hasOwnProperty, a = Array.isArray, o = s(), h = function(w, F) {
    return o.set(w, F), w;
  }, l = function(w) {
    return o.has(w);
  }, c = function(w) {
    return o.get(w);
  }, u = function(w, F) {
    o.set(w, F);
  }, d = (function() {
    for (var B = [], w = 0; w < 256; ++w) B[B.length] = "%" + ((w < 16 ? "0" : "") + w.toString(16)).toUpperCase();
    return B;
  })(), y = function(w) {
    for (; w.length > 1; ) {
      var F = w.pop(), P = F.obj[F.prop];
      if (a(P)) {
        for (var G = [], $ = 0; $ < P.length; ++$) typeof P[$] < "u" && (G[G.length] = P[$]);
        F.obj[F.prop] = G;
      }
    }
  }, m = function(w, F) {
    for (var P = F && F.plainObjects ? { __proto__: null } : {}, G = 0; G < w.length; ++G) typeof w[G] < "u" && (P[G] = w[G]);
    return P;
  }, v = function(w, F, P) {
    F === "__proto__" && i ? i(w, F, {
      configurable: !0,
      enumerable: !0,
      value: P,
      writable: !0
    }) : w[F] = P;
  }, p = function B(w, F, P) {
    if (!F) return w;
    if (typeof F != "object" && typeof F != "function") {
      if (a(w)) {
        var G = w.length;
        if (P && typeof P.arrayLimit == "number" && G >= P.arrayLimit) {
          if (P.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + P.arrayLimit + " element" + (P.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
          return h(m(w.concat(F), P), G);
        }
        w[G] = F;
      } else if (w && typeof w == "object")
        if (l(w)) {
          var $ = c(w) + 1;
          w[$] = F, u(w, $);
        } else {
          if (P && P.strictMerge) return [w, F];
          (P && (P.plainObjects || P.allowPrototypes) || !n.call(Object.prototype, F)) && (w[F] = !0);
        }
      else return [w, F];
      return w;
    }
    if (!w || typeof w != "object") {
      if (l(F)) {
        for (var H = Object.keys(F), Q = P && P.plainObjects ? {
          __proto__: null,
          0: w
        } : { 0: w }, _ = 0; _ < H.length; _++) {
          var T = parseInt(H[_], 10);
          Q[T + 1] = F[H[_]];
        }
        return h(Q, c(F) + 1);
      }
      var b = [w].concat(F);
      if (P && typeof P.arrayLimit == "number" && b.length > P.arrayLimit) {
        if (P.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + P.arrayLimit + " element" + (P.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
        return h(m(b, P), b.length - 1);
      }
      return b;
    }
    var I = w;
    if (a(w) && !a(F) && (I = m(w, P)), a(w) && a(F)) {
      if (F.forEach(function(S, R) {
        if (n.call(w, R)) {
          var O = w[R];
          O && typeof O == "object" && S && typeof S == "object" ? w[R] = B(O, S, P) : w[w.length] = S;
        } else w[R] = S;
      }), P && typeof P.arrayLimit == "number" && w.length > P.arrayLimit) {
        if (P.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + P.arrayLimit + " element" + (P.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
        return h(m(w, P), w.length - 1);
      }
      return w;
    }
    return Object.keys(F).reduce(function(S, R) {
      var O = F[R];
      if (n.call(S, R) ? v(S, R, B(S[R], O, P)) : v(S, R, O), l(F) && !l(S) && h(S, c(F)), l(S)) {
        var z = parseInt(R, 10);
        String(z) === R && z >= 0 && z > c(S) && u(S, z);
      }
      return S;
    }, I);
  }, x = function(w, F) {
    return Object.keys(F).reduce(function(P, G) {
      return v(P, G, F[G]), P;
    }, w);
  }, f = function(B, w, F) {
    var P = B.replace(/\+/g, " ");
    if (F === "iso-8859-1") return P.replace(/%[0-9a-f]{2}/gi, unescape);
    try {
      return decodeURIComponent(P);
    } catch {
      return P;
    }
  }, E = 1024, g = function(w, F, P, G, $) {
    if (w.length === 0) return w;
    var H = w;
    if (typeof w == "symbol" ? H = Symbol.prototype.toString.call(w) : typeof w != "string" && (H = String(w)), P === "iso-8859-1") return escape(H).replace(/%u[0-9a-f]{4}/gi, function(O) {
      return "%26%23" + parseInt(O.slice(2), 16) + "%3B";
    });
    for (var Q = "", _ = 0; _ < H.length; _ += E) {
      var T = H.length >= E ? H.slice(_, _ + E) : H;
      if (_ + E < H.length) {
        var b = T.charCodeAt(T.length - 1);
        b >= 55296 && b <= 56319 && (T = T.slice(0, -1), _ -= 1);
      }
      for (var I = [], S = 0; S < T.length; ++S) {
        var R = T.charCodeAt(S);
        if (R === 45 || R === 46 || R === 95 || R === 126 || R >= 48 && R <= 57 || R >= 65 && R <= 90 || R >= 97 && R <= 122 || $ === r.RFC1738 && (R === 40 || R === 41)) {
          I[I.length] = T.charAt(S);
          continue;
        }
        if (R < 128) {
          I[I.length] = d[R];
          continue;
        }
        if (R < 2048) {
          I[I.length] = d[192 | R >> 6] + d[128 | R & 63];
          continue;
        }
        if (R < 55296 || R >= 57344) {
          I[I.length] = d[224 | R >> 12] + d[128 | R >> 6 & 63] + d[128 | R & 63];
          continue;
        }
        S += 1, R = 65536 + ((R & 1023) << 10 | T.charCodeAt(S) & 1023), I[I.length] = d[240 | R >> 18] + d[128 | R >> 12 & 63] + d[128 | R >> 6 & 63] + d[128 | R & 63];
      }
      Q += I.join("");
    }
    return Q;
  }, A = function(w) {
    for (var F = [{
      obj: { o: w },
      prop: "o"
    }], P = s(), G = 0; G < F.length; ++G)
      for (var $ = F[G], H = $.obj[$.prop], Q = Object.keys(H), _ = 0; _ < Q.length; ++_) {
        var T = Q[_], b = H[T];
        typeof b == "object" && b !== null && !P.has(b) && (F[F.length] = {
          obj: H,
          prop: T
        }, P.set(b, !0));
      }
    return y(F), w;
  }, L = function(w) {
    return Object.prototype.toString.call(w) === "[object RegExp]";
  }, M = function(w) {
    return !w || typeof w != "object" ? !1 : !!(w.constructor && typeof w.constructor.isBuffer == "function" && w.constructor.isBuffer(w));
  }, C = function(w, F, P, G, $) {
    if (l(w)) {
      if ($) throw new RangeError("Array limit exceeded. Only " + P + " element" + (P === 1 ? "" : "s") + " allowed in an array.");
      for (var H = a(F) ? F : [F], Q = c(w), _ = 0; _ < H.length; ++_)
        Q += 1, w[Q] = H[_];
      return u(w, Q), w;
    }
    var T = [].concat(w, F);
    if (T.length > P) {
      if ($) throw new RangeError("Array limit exceeded. Only " + P + " element" + (P === 1 ? "" : "s") + " allowed in an array.");
      return h(m(T, { plainObjects: G }), T.length - 1);
    }
    return T;
  }, k = function(w, F) {
    if (a(w)) {
      for (var P = [], G = 0; G < w.length; G += 1) P[P.length] = F(w[G]);
      return P;
    }
    return F(w);
  };
  t.exports = {
    arrayToObject: m,
    assign: x,
    combine: C,
    compact: A,
    decode: f,
    encode: g,
    isBuffer: M,
    isOverflow: l,
    isRegExp: L,
    markOverflow: h,
    maybeMap: k,
    merge: p
  };
})), zo = /* @__PURE__ */ rt(((e, t) => {
  var r = Nn(), s = On(), i = ri(), n = Object.prototype.hasOwnProperty, a = {
    brackets: function(f) {
      return f + "[]";
    },
    comma: "comma",
    indices: function(f, E) {
      return f + "[" + E + "]";
    },
    repeat: function(f) {
      return f;
    }
  }, o = Array.isArray, h = Array.prototype.push, l = function(x, f) {
    h.apply(x, o(f) ? f : [f]);
  }, c = Date.prototype.toISOString, u = i.default, d = {
    addQueryPrefix: !1,
    allowDots: !1,
    allowEmptyArrays: !1,
    arrayFormat: "indices",
    charset: "utf-8",
    charsetSentinel: !1,
    commaRoundTrip: !1,
    delimiter: "&",
    depth: 1 / 0,
    encode: !0,
    encodeDotInKeys: !1,
    encoder: s.encode,
    encodeValuesOnly: !1,
    filter: void 0,
    format: u,
    formatter: i.formatters[u],
    indices: !1,
    serializeDate: function(f) {
      return c.call(f);
    },
    skipNulls: !1,
    strictNullHandling: !1
  }, y = function(f) {
    return typeof f == "string" || typeof f == "number" || typeof f == "boolean" || typeof f == "symbol" || typeof f == "bigint";
  }, m = {}, v = function x(f, E, g, A, L, M, C, k, B, w, F, P, G, $, H, Q, _, T, b, I) {
    var S = f;
    if (I > b) throw new RangeError("Input depth exceeded depth option of " + b);
    for (var R = T, O = 0, z = !1; (R = R.get(m)) !== void 0 && !z; ) {
      var V = R.get(f);
      if (O += 1, typeof V < "u") {
        if (V === O) throw new RangeError("Cyclic object value");
        z = !0;
      }
      typeof R.get(m) > "u" && (O = 0);
    }
    if (S = typeof w == "function" ? w(E, S) : S, S instanceof Date ? S = G(S) : g === "comma" && o(S) && (S = s.maybeMap(S, function(nt) {
      return nt instanceof Date ? G(nt) : nt;
    })), S === null) {
      if (M) return H(B && !Q ? B(E, d.encoder, _, "key", $) : E);
      S = "";
    }
    if (y(S) || s.isBuffer(S))
      return B ? [H(Q ? E : B(E, d.encoder, _, "key", $)) + "=" + H(B(S, d.encoder, _, "value", $))] : [H(E) + "=" + H(String(S))];
    var W = [];
    if (typeof S > "u") return W;
    var et;
    if (g === "comma" && o(S))
      Q && B && (S = s.maybeMap(S, function(nt) {
        return nt == null ? nt : B(nt);
      })), et = [{ value: S.length > 0 ? S.join(",") || null : void 0 }];
    else if (o(w)) et = w;
    else {
      var it = Object.keys(S);
      et = F ? it.sort(F) : it;
    }
    var K = k ? String(E).replace(/\./g, "%2E") : String(E), j = A && o(S) && S.length === 1 ? K + "[]" : K;
    if (L && o(S) && S.length === 0 && Object.keys(S).length === 0) return j + "[]";
    for (var Y = 0; Y < et.length; ++Y) {
      var tt = et[Y], ht = typeof tt == "object" && tt && typeof tt.value < "u" ? tt.value : S[tt];
      if (!(C && ht === null)) {
        var _t = P && k ? String(tt).replace(/\./g, "%2E") : String(tt), St = o(S) ? typeof g == "function" ? g(j, _t) : j : j + (P ? "." + _t : "[" + _t + "]");
        T.set(f, O);
        var J = r();
        J.set(m, T), l(W, x(ht, St, g, A, L, M, C, k, g === "comma" && Q && o(S) ? null : B, w, F, P, G, $, H, Q, _, J, b, I + 1));
      }
    }
    return W;
  }, p = function(f) {
    if (!f) return d;
    if (typeof f.allowEmptyArrays < "u" && typeof f.allowEmptyArrays != "boolean") throw new TypeError("`allowEmptyArrays` option can only be `true` or `false`, when provided");
    if (typeof f.encodeDotInKeys < "u" && typeof f.encodeDotInKeys != "boolean") throw new TypeError("`encodeDotInKeys` option can only be `true` or `false`, when provided");
    if (f.encoder !== null && typeof f.encoder < "u" && typeof f.encoder != "function") throw new TypeError("Encoder has to be a function.");
    var E = f.charset || d.charset;
    if (typeof f.charset < "u" && f.charset !== "utf-8" && f.charset !== "iso-8859-1") throw new TypeError("The charset option must be either utf-8, iso-8859-1, or undefined");
    var g = i.default;
    if (typeof f.format < "u") {
      if (!n.call(i.formatters, f.format)) throw new TypeError("Unknown format option provided.");
      g = f.format;
    }
    var A = i.formatters[g], L = d.filter;
    (typeof f.filter == "function" || o(f.filter)) && (L = f.filter);
    var M;
    if (f.arrayFormat in a ? M = f.arrayFormat : "indices" in f ? M = f.indices ? "indices" : "repeat" : M = d.arrayFormat, "commaRoundTrip" in f && typeof f.commaRoundTrip != "boolean") throw new TypeError("`commaRoundTrip` must be a boolean, or absent");
    var C = typeof f.allowDots > "u" ? f.encodeDotInKeys === !0 ? !0 : d.allowDots : !!f.allowDots;
    return {
      addQueryPrefix: typeof f.addQueryPrefix == "boolean" ? f.addQueryPrefix : d.addQueryPrefix,
      allowDots: C,
      allowEmptyArrays: typeof f.allowEmptyArrays == "boolean" ? !!f.allowEmptyArrays : d.allowEmptyArrays,
      arrayFormat: M,
      charset: E,
      charsetSentinel: typeof f.charsetSentinel == "boolean" ? f.charsetSentinel : d.charsetSentinel,
      commaRoundTrip: !!f.commaRoundTrip,
      delimiter: typeof f.delimiter > "u" ? d.delimiter : f.delimiter,
      depth: typeof f.depth == "number" ? f.depth : d.depth,
      encode: typeof f.encode == "boolean" ? f.encode : d.encode,
      encodeDotInKeys: typeof f.encodeDotInKeys == "boolean" ? f.encodeDotInKeys : d.encodeDotInKeys,
      encoder: typeof f.encoder == "function" ? f.encoder : d.encoder,
      encodeValuesOnly: typeof f.encodeValuesOnly == "boolean" ? f.encodeValuesOnly : d.encodeValuesOnly,
      filter: L,
      format: g,
      formatter: A,
      serializeDate: typeof f.serializeDate == "function" ? f.serializeDate : d.serializeDate,
      skipNulls: typeof f.skipNulls == "boolean" ? f.skipNulls : d.skipNulls,
      sort: typeof f.sort == "function" ? f.sort : null,
      strictNullHandling: typeof f.strictNullHandling == "boolean" ? f.strictNullHandling : d.strictNullHandling
    };
  };
  t.exports = function(x, f) {
    var E = x, g = p(f), A, L;
    typeof g.filter == "function" ? (L = g.filter, E = L("", E)) : o(g.filter) && (L = g.filter, A = L);
    var M = [];
    if (typeof E != "object" || E === null) return "";
    var C = a[g.arrayFormat], k = C === "comma" && g.commaRoundTrip;
    A || (A = Object.keys(E)), g.sort && A.sort(g.sort);
    for (var B = r(), w = 0; w < A.length; ++w) {
      var F = A[w];
      if (!(typeof F > "u" || F === null)) {
        var P = E[F];
        g.skipNulls && P === null || l(M, v(P, g.encodeDotInKeys ? String(F).replace(/\./g, "%2E") : String(F), C, k, g.allowEmptyArrays, g.strictNullHandling, g.skipNulls, g.encodeDotInKeys, g.encode ? g.encoder : null, g.filter, g.sort, g.allowDots, g.serializeDate, g.format, g.formatter, g.encodeValuesOnly, g.charset, B, g.depth, 0));
      }
    }
    var G = M.join(g.delimiter), $ = g.addQueryPrefix === !0 ? "?" : "";
    return g.charsetSentinel && (g.charset === "iso-8859-1" ? $ += "utf8=%26%2310003%3B" + g.delimiter : $ += "utf8=%E2%9C%93" + g.delimiter), G.length > 0 ? $ + G : "";
  };
})), $o = /* @__PURE__ */ rt(((e, t) => {
  var r = On(), s = Object.prototype.hasOwnProperty, i = Array.isArray, n = {
    allowDots: !1,
    allowEmptyArrays: !1,
    allowPrototypes: !1,
    allowSparse: !1,
    arrayLimit: 20,
    charset: "utf-8",
    charsetSentinel: !1,
    comma: !1,
    decodeDotInKeys: !1,
    decoder: r.decode,
    delimiter: "&",
    depth: 5,
    duplicates: "combine",
    ignoreQueryPrefix: !1,
    interpretNumericEntities: !1,
    parameterLimit: 1e3,
    parseArrays: !0,
    plainObjects: !1,
    strictDepth: !1,
    strictMerge: !0,
    strictNullHandling: !1,
    throwOnLimitExceeded: !1
  }, a = function(v) {
    return v.replace(/&#(\d+);/g, function(p, x) {
      return String.fromCharCode(parseInt(x, 10));
    });
  }, o = function(v, p, x) {
    if (v && typeof v == "string" && p.comma && v.indexOf(",") > -1) {
      if (p.throwOnLimitExceeded)
        for (var f = 0, E = v.indexOf(","); E > -1; ) {
          if (f += 1, f >= p.arrayLimit) throw new RangeError("Array limit exceeded. Only " + p.arrayLimit + " element" + (p.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
          E = v.indexOf(",", E + 1);
        }
      return v.split(",");
    }
    if (p.throwOnLimitExceeded && x >= p.arrayLimit) throw new RangeError("Array limit exceeded. Only " + p.arrayLimit + " element" + (p.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
    return v;
  }, h = "utf8=%26%2310003%3B", l = "utf8=%E2%9C%93", c = function(p, x) {
    var f = { __proto__: null }, E = x.ignoreQueryPrefix ? p.replace(/^\?/, "") : p;
    E = E.replace(/%5B/gi, "[").replace(/%5D/gi, "]");
    var g = x.parameterLimit === 1 / 0 ? void 0 : x.parameterLimit, A = E.split(x.delimiter, x.throwOnLimitExceeded && typeof g < "u" ? g + 1 : g);
    if (x.throwOnLimitExceeded && typeof g < "u" && A.length > g) throw new RangeError("Parameter limit exceeded. Only " + g + " parameter" + (g === 1 ? "" : "s") + " allowed.");
    var L = -1, M, C = x.charset;
    if (x.charsetSentinel)
      for (M = 0; M < A.length; ++M) A[M].indexOf("utf8=") === 0 && (A[M] === l ? C = "utf-8" : A[M] === h && (C = "iso-8859-1"), L = M, M = A.length);
    for (M = 0; M < A.length; ++M)
      if (M !== L) {
        var k = A[M], B = k.indexOf("]="), w = B === -1 ? k.indexOf("=") : B + 1, F, P;
        if (w === -1 ? (F = x.decoder(k, n.decoder, C, "key"), P = x.strictNullHandling ? null : "") : (F = x.decoder(k.slice(0, w), n.decoder, C, "key"), F !== null && (P = r.maybeMap(o(k.slice(w + 1), x, i(f[F]) ? f[F].length : 0), function($) {
          return x.decoder($, n.decoder, C, "value");
        }))), P && x.interpretNumericEntities && C === "iso-8859-1" && (P = a(String(P))), k.indexOf("[]=") > -1 && (P = i(P) ? [P] : P), x.comma && i(P) && P.length > x.arrayLimit && (P = r.combine([], P, x.arrayLimit, x.plainObjects, x.throwOnLimitExceeded)), F !== null) {
          var G = s.call(f, F);
          G && (x.duplicates === "combine" || k.indexOf("[]=") > -1) ? f[F] = r.combine(f[F], P, x.arrayLimit, x.plainObjects, x.throwOnLimitExceeded) : (!G || x.duplicates === "last") && (f[F] = P);
        }
      }
    return f;
  }, u = function(v, p, x, f) {
    var E = 0;
    if (v.length > 0 && v[v.length - 1] === "[]") {
      var g = v.slice(0, -1).join("");
      E = Array.isArray(p) && p[g] ? p[g].length : 0;
    }
    for (var A = f ? p : o(p, x, E), L = v.length - 1; L >= 0; --L) {
      var M, C = v[L];
      if (C === "[]" && x.parseArrays)
        r.isOverflow(A) ? M = A : M = x.allowEmptyArrays && (A === "" || x.strictNullHandling && A === null) ? [] : r.combine([], A, x.arrayLimit, x.plainObjects, x.throwOnLimitExceeded);
      else {
        M = x.plainObjects ? { __proto__: null } : {};
        var k = C.charAt(0) === "[" && C.charAt(C.length - 1) === "]" ? C.slice(1, -1) : C, B = x.decodeDotInKeys ? k.replace(/%2E/g, ".") : k, w = parseInt(B, 10), F = !isNaN(w) && C !== B && String(w) === B && w >= 0 && x.parseArrays;
        if (!x.parseArrays && B === "") M = { 0: A };
        else if (F && w < x.arrayLimit)
          M = [], M[w] = A;
        else {
          if (F && x.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + x.arrayLimit + " element" + (x.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
          F ? (M[w] = A, r.markOverflow(M, w)) : B !== "__proto__" && (M[B] = A);
        }
      }
      A = M;
    }
    return A;
  }, d = function(p, x) {
    var f = x.allowDots ? p.replace(/\.([^.[]+)/g, "[$1]") : p;
    if (x.depth <= 0)
      return !x.plainObjects && s.call(Object.prototype, f) && !x.allowPrototypes ? void 0 : [f];
    var E = [], g = f.indexOf("["), A = g >= 0 ? f.slice(0, g) : f;
    if (A) {
      if (!x.plainObjects && s.call(Object.prototype, A) && !x.allowPrototypes)
        return;
      E[E.length] = A;
    }
    for (var L = f.length, M = g, C = 0; M >= 0 && C < x.depth; ) {
      for (var k = 1, B = M + 1, w = -1; B < L && w < 0; ) {
        var F = f.charCodeAt(B);
        F === 91 ? k += 1 : F === 93 && (k -= 1, k === 0 && (w = B)), B += 1;
      }
      if (w < 0)
        return E[E.length] = "[" + f.slice(M) + "]", E;
      var P = f.slice(M, w + 1), G = P.slice(1, -1);
      if (!x.plainObjects && s.call(Object.prototype, G) && !x.allowPrototypes) return;
      E[E.length] = P, C += 1, M = f.indexOf("[", w + 1);
    }
    if (M >= 0) {
      if (x.strictDepth === !0) throw new RangeError("Input depth exceeded depth option of " + x.depth + " and strictDepth is true");
      E[E.length] = "[" + f.slice(M) + "]";
    }
    return E;
  }, y = function(p, x, f, E) {
    if (p) {
      var g = d(p, f);
      if (g)
        return u(g, x, f, E);
    }
  }, m = function(p) {
    if (!p) return n;
    if (typeof p.allowEmptyArrays < "u" && typeof p.allowEmptyArrays != "boolean") throw new TypeError("`allowEmptyArrays` option can only be `true` or `false`, when provided");
    if (typeof p.decodeDotInKeys < "u" && typeof p.decodeDotInKeys != "boolean") throw new TypeError("`decodeDotInKeys` option can only be `true` or `false`, when provided");
    if (p.decoder !== null && typeof p.decoder < "u" && typeof p.decoder != "function") throw new TypeError("Decoder has to be a function.");
    if (typeof p.charset < "u" && p.charset !== "utf-8" && p.charset !== "iso-8859-1") throw new TypeError("The charset option must be either utf-8, iso-8859-1, or undefined");
    if (typeof p.throwOnLimitExceeded < "u" && typeof p.throwOnLimitExceeded != "boolean") throw new TypeError("`throwOnLimitExceeded` option must be a boolean");
    var x = typeof p.charset > "u" ? n.charset : p.charset, f = typeof p.duplicates > "u" ? n.duplicates : p.duplicates;
    if (f !== "combine" && f !== "first" && f !== "last") throw new TypeError("The duplicates option must be either combine, first, or last");
    return {
      allowDots: typeof p.allowDots > "u" ? p.decodeDotInKeys === !0 ? !0 : n.allowDots : !!p.allowDots,
      allowEmptyArrays: typeof p.allowEmptyArrays == "boolean" ? !!p.allowEmptyArrays : n.allowEmptyArrays,
      allowPrototypes: typeof p.allowPrototypes == "boolean" ? p.allowPrototypes : n.allowPrototypes,
      allowSparse: typeof p.allowSparse == "boolean" ? p.allowSparse : n.allowSparse,
      arrayLimit: typeof p.arrayLimit == "number" ? p.arrayLimit : n.arrayLimit,
      charset: x,
      charsetSentinel: typeof p.charsetSentinel == "boolean" ? p.charsetSentinel : n.charsetSentinel,
      comma: typeof p.comma == "boolean" ? p.comma : n.comma,
      decodeDotInKeys: typeof p.decodeDotInKeys == "boolean" ? p.decodeDotInKeys : n.decodeDotInKeys,
      decoder: typeof p.decoder == "function" ? p.decoder : n.decoder,
      delimiter: typeof p.delimiter == "string" || r.isRegExp(p.delimiter) ? p.delimiter : n.delimiter,
      depth: typeof p.depth == "number" || p.depth === !1 ? +p.depth : n.depth,
      duplicates: f,
      ignoreQueryPrefix: p.ignoreQueryPrefix === !0,
      interpretNumericEntities: typeof p.interpretNumericEntities == "boolean" ? p.interpretNumericEntities : n.interpretNumericEntities,
      parameterLimit: typeof p.parameterLimit == "number" ? p.parameterLimit : n.parameterLimit,
      parseArrays: p.parseArrays !== !1,
      plainObjects: typeof p.plainObjects == "boolean" ? p.plainObjects : n.plainObjects,
      strictDepth: typeof p.strictDepth == "boolean" ? !!p.strictDepth : n.strictDepth,
      strictMerge: typeof p.strictMerge == "boolean" ? !!p.strictMerge : n.strictMerge,
      strictNullHandling: typeof p.strictNullHandling == "boolean" ? p.strictNullHandling : n.strictNullHandling,
      throwOnLimitExceeded: typeof p.throwOnLimitExceeded == "boolean" ? p.throwOnLimitExceeded : !1
    };
  };
  t.exports = function(v, p) {
    var x = m(p);
    if (v === "" || v === null || typeof v > "u") return x.plainObjects ? { __proto__: null } : {};
    for (var f = typeof v == "string" ? c(v, x) : v, E = x.plainObjects ? { __proto__: null } : {}, g = Object.keys(f), A = 0; A < g.length; ++A) {
      var L = g[A], M = y(L, f[L], x, typeof v == "string");
      E = r.merge(E, M, x);
    }
    return x.allowSparse === !0 ? E : r.compact(E);
  };
})), Vo = /* @__PURE__ */ rt(((e, t) => {
  var r = zo(), s = $o(), i = ri();
  t.exports = {
    formats: i,
    parse: s,
    stringify: r
  };
})), Ho = /* @__PURE__ */ rt(((e) => {
  var t = po();
  function r() {
    this.protocol = null, this.slashes = null, this.auth = null, this.host = null, this.port = null, this.hostname = null, this.hash = null, this.search = null, this.query = null, this.pathname = null, this.path = null, this.href = null;
  }
  var s = /^([a-z0-9.+-]+:)/i, i = /:[0-9]*$/, n = /^(\/\/?(?!\/)[^?\s]*)(\?[^\s]*)?$/, a = [
    "{",
    "}",
    "|",
    "\\",
    "^",
    "`"
  ].concat([
    "<",
    ">",
    '"',
    "`",
    " ",
    "\r",
    `
`,
    "	"
  ]), o = ["'"].concat(a), h = [
    "%",
    "/",
    "?",
    ";",
    "#"
  ].concat(o), l = [
    "/",
    "?",
    "#"
  ], c = 255, u = /^[+a-z0-9A-Z_-]{0,63}$/, d = /^([+a-z0-9A-Z_-]{0,63})(.*)$/, y = {
    javascript: !0,
    "javascript:": !0
  }, m = {
    javascript: !0,
    "javascript:": !0
  }, v = {
    http: !0,
    https: !0,
    ftp: !0,
    gopher: !0,
    file: !0,
    "http:": !0,
    "https:": !0,
    "ftp:": !0,
    "gopher:": !0,
    "file:": !0
  }, p = Vo();
  function x(f, E, g) {
    if (f && typeof f == "object" && f instanceof r) return f;
    var A = new r();
    return A.parse(f, E, g), A;
  }
  r.prototype.parse = function(f, E, g) {
    if (typeof f != "string") throw new TypeError("Parameter 'url' must be a string, not " + typeof f);
    var A = f.indexOf("?"), L = A !== -1 && A < f.indexOf("#") ? "?" : "#", M = f.split(L);
    M[0] = M[0].replace(/\\/g, "/"), f = M.join(L);
    var C = f;
    if (C = C.trim(), !g && f.split("#").length === 1) {
      var k = n.exec(C);
      if (k)
        return this.path = C, this.href = C, this.pathname = k[1], k[2] ? (this.search = k[2], E ? this.query = p.parse(this.search.substr(1)) : this.query = this.search.substr(1)) : E && (this.search = "", this.query = {}), this;
    }
    var B = s.exec(C);
    if (B) {
      B = B[0];
      var w = B.toLowerCase();
      this.protocol = w, C = C.substr(B.length);
    }
    if (g || B || C.match(/^\/\/[^@/]+@[^@/]+/)) {
      var F = C.substr(0, 2) === "//";
      F && !(B && m[B]) && (C = C.substr(2), this.slashes = !0);
    }
    if (!m[B] && (F || B && !v[B])) {
      for (var P = -1, G = 0; G < l.length; G++) {
        var $ = C.indexOf(l[G]);
        $ !== -1 && (P === -1 || $ < P) && (P = $);
      }
      var H, Q;
      P === -1 ? Q = C.lastIndexOf("@") : Q = C.lastIndexOf("@", P), Q !== -1 && (H = C.slice(0, Q), C = C.slice(Q + 1), this.auth = decodeURIComponent(H)), P = -1;
      for (var G = 0; G < h.length; G++) {
        var $ = C.indexOf(h[G]);
        $ !== -1 && (P === -1 || $ < P) && (P = $);
      }
      P === -1 && (P = C.length), this.host = C.slice(0, P), C = C.slice(P), this.parseHost(), this.hostname = this.hostname || "";
      var _ = this.hostname[0] === "[" && this.hostname[this.hostname.length - 1] === "]";
      if (!_)
        for (var T = this.hostname.split(/\./), G = 0, b = T.length; G < b; G++) {
          var I = T[G];
          if (I && !I.match(u)) {
            for (var S = "", R = 0, O = I.length; R < O; R++) I.charCodeAt(R) > 127 ? S += "x" : S += I[R];
            if (!S.match(u)) {
              var z = T.slice(0, G), V = T.slice(G + 1), W = I.match(d);
              W && (z.push(W[1]), V.unshift(W[2])), V.length && (C = "/" + V.join(".") + C), this.hostname = z.join(".");
              break;
            }
          }
        }
      this.hostname.length > c ? this.hostname = "" : this.hostname = this.hostname.toLowerCase(), _ || (this.hostname = t.toASCII(this.hostname));
      var et = this.port ? ":" + this.port : "", it = this.hostname || "";
      this.host = it + et, this.href += this.host, _ && (this.hostname = this.hostname.substr(1, this.hostname.length - 2), C[0] !== "/" && (C = "/" + C));
    }
    if (!y[w]) for (var G = 0, b = o.length; G < b; G++) {
      var K = o[G];
      if (C.indexOf(K) !== -1) {
        var j = encodeURIComponent(K);
        j === K && (j = escape(K)), C = C.split(K).join(j);
      }
    }
    var Y = C.indexOf("#");
    Y !== -1 && (this.hash = C.substr(Y), C = C.slice(0, Y));
    var tt = C.indexOf("?");
    if (tt !== -1 ? (this.search = C.substr(tt), this.query = C.substr(tt + 1), E && (this.query = p.parse(this.query)), C = C.slice(0, tt)) : E && (this.search = "", this.query = {}), C && (this.pathname = C), v[w] && this.hostname && !this.pathname && (this.pathname = "/"), this.pathname || this.search) {
      var et = this.pathname || "", ht = this.search || "";
      this.path = et + ht;
    }
    return this.href = this.format(), this;
  }, r.prototype.format = function() {
    var f = this.auth || "";
    f && (f = encodeURIComponent(f), f = f.replace(/%3A/i, ":"), f += "@");
    var E = this.protocol || "", g = this.pathname || "", A = this.hash || "", L = !1, M = "";
    this.host ? L = f + this.host : this.hostname && (L = f + (this.hostname.indexOf(":") === -1 ? this.hostname : "[" + this.hostname + "]"), this.port && (L += ":" + this.port)), this.query && typeof this.query == "object" && Object.keys(this.query).length && (M = p.stringify(this.query, {
      arrayFormat: "repeat",
      addQueryPrefix: !1
    }));
    var C = this.search || M && "?" + M || "";
    return E && E.substr(-1) !== ":" && (E += ":"), this.slashes || (!E || v[E]) && L !== !1 ? (L = "//" + (L || ""), g && g.charAt(0) !== "/" && (g = "/" + g)) : L || (L = ""), A && A.charAt(0) !== "#" && (A = "#" + A), C && C.charAt(0) !== "?" && (C = "?" + C), g = g.replace(/[?#]/g, function(k) {
      return encodeURIComponent(k);
    }), C = C.replace("#", "%23"), E + L + g + C + A;
  }, r.prototype.resolve = function(f) {
    return this.resolveObject(x(f, !1, !0)).format();
  }, r.prototype.resolveObject = function(f) {
    if (typeof f == "string") {
      var E = new r();
      E.parse(f, !1, !0), f = E;
    }
    for (var g = new r(), A = Object.keys(this), L = 0; L < A.length; L++) {
      var M = A[L];
      g[M] = this[M];
    }
    if (g.hash = f.hash, f.href === "")
      return g.href = g.format(), g;
    if (f.slashes && !f.protocol) {
      for (var C = Object.keys(f), k = 0; k < C.length; k++) {
        var B = C[k];
        B !== "protocol" && (g[B] = f[B]);
      }
      return v[g.protocol] && g.hostname && !g.pathname && (g.pathname = "/", g.path = g.pathname), g.href = g.format(), g;
    }
    if (f.protocol && f.protocol !== g.protocol) {
      if (!v[f.protocol]) {
        for (var w = Object.keys(f), F = 0; F < w.length; F++) {
          var P = w[F];
          g[P] = f[P];
        }
        return g.href = g.format(), g;
      }
      if (g.protocol = f.protocol, !f.host && !m[f.protocol]) {
        for (var T = (f.pathname || "").split("/"); T.length && !(f.host = T.shift()); ) ;
        f.host || (f.host = ""), f.hostname || (f.hostname = ""), T[0] !== "" && T.unshift(""), T.length < 2 && T.unshift(""), g.pathname = T.join("/");
      } else g.pathname = f.pathname;
      return g.search = f.search, g.query = f.query, g.host = f.host || "", g.auth = f.auth, g.hostname = f.hostname || f.host, g.port = f.port, (g.pathname || g.search) && (g.path = (g.pathname || "") + (g.search || "")), g.slashes = g.slashes || f.slashes, g.href = g.format(), g;
    }
    var G = g.pathname && g.pathname.charAt(0) === "/", $ = f.host || f.pathname && f.pathname.charAt(0) === "/", H = $ || G || g.host && f.pathname, Q = H, _ = g.pathname && g.pathname.split("/") || [], T = f.pathname && f.pathname.split("/") || [], b = g.protocol && !v[g.protocol];
    if (b && (g.hostname = "", g.port = null, g.host && (_[0] === "" ? _[0] = g.host : _.unshift(g.host)), g.host = "", f.protocol && (f.hostname = null, f.port = null, f.host && (T[0] === "" ? T[0] = f.host : T.unshift(f.host)), f.host = null), H = H && (T[0] === "" || _[0] === "")), $)
      g.host = f.host || f.host === "" ? f.host : g.host, g.hostname = f.hostname || f.hostname === "" ? f.hostname : g.hostname, g.search = f.search, g.query = f.query, _ = T;
    else if (T.length)
      _ || (_ = []), _.pop(), _ = _.concat(T), g.search = f.search, g.query = f.query;
    else if (f.search != null) {
      if (b) {
        g.host = _.shift(), g.hostname = g.host;
        var I = g.host && g.host.indexOf("@") > 0 ? g.host.split("@") : !1;
        I && (g.auth = I.shift(), g.hostname = I.shift(), g.host = g.hostname);
      }
      return g.search = f.search, g.query = f.query, (g.pathname !== null || g.search !== null) && (g.path = (g.pathname ? g.pathname : "") + (g.search ? g.search : "")), g.href = g.format(), g;
    }
    if (!_.length)
      return g.pathname = null, g.search ? g.path = "/" + g.search : g.path = null, g.href = g.format(), g;
    for (var S = _.slice(-1)[0], R = (g.host || f.host || _.length > 1) && (S === "." || S === "..") || S === "", O = 0, z = _.length; z >= 0; z--)
      S = _[z], S === "." ? _.splice(z, 1) : S === ".." ? (_.splice(z, 1), O++) : O && (_.splice(z, 1), O--);
    if (!H && !Q) for (; O--; ) _.unshift("..");
    H && _[0] !== "" && (!_[0] || _[0].charAt(0) !== "/") && _.unshift(""), R && _.join("/").substr(-1) !== "/" && _.push("");
    var V = _[0] === "" || _[0] && _[0].charAt(0) === "/";
    if (b) {
      g.hostname = V ? "" : _.length ? _.shift() : "", g.host = g.hostname;
      var I = g.host && g.host.indexOf("@") > 0 ? g.host.split("@") : !1;
      I && (g.auth = I.shift(), g.hostname = I.shift(), g.host = g.hostname);
    }
    return H = H || g.host && _.length, H && !V && _.unshift(""), _.length > 0 ? g.pathname = _.join("/") : (g.pathname = null, g.path = null), (g.pathname !== null || g.search !== null) && (g.path = (g.pathname ? g.pathname : "") + (g.search ? g.search : "")), g.auth = f.auth || g.auth, g.slashes = g.slashes || f.slashes, g.href = g.format(), g;
  }, r.prototype.parseHost = function() {
    var f = this.host, E = i.exec(f);
    E && (E = E[0], E !== ":" && (this.port = E.substr(1)), f = f.substr(0, f.length - E.length)), f && (this.hostname = f);
  };
})), rs = /* @__PURE__ */ wn(uo(), 1), Eu = /* @__PURE__ */ wn(fo(), 1), Su = Ho(), Fi = {};
function yt(e, t, r = 3) {
  if (Fi[t]) return;
  let s = (/* @__PURE__ */ new Error()).stack;
  typeof s > "u" ? console.warn("PixiJS Deprecation Warning: ", `${t}
Deprecated since v${e}`) : (s = s.split(`
`).splice(r).join(`
`), console.groupCollapsed ? (console.groupCollapsed("%cPixiJS Deprecation Warning: %c%s", "color:#614108;background:#fffbe6", "font-weight:normal;color:#614108;background:#fffbe6", `${t}
Deprecated since v${e}`), console.warn(s), console.groupEnd()) : (console.warn("PixiJS Deprecation Warning: ", `${t}
Deprecated since v${e}`), console.warn(s))), Fi[t] = !0;
}
var ms;
function Xo() {
  return typeof ms > "u" && (ms = (function() {
    const e = {
      stencil: !0,
      failIfMajorPerformanceCaveat: dt.FAIL_IF_MAJOR_PERFORMANCE_CAVEAT
    };
    try {
      if (!dt.ADAPTER.getWebGLRenderingContext()) return !1;
      const t = dt.ADAPTER.createCanvas();
      let r = t.getContext("webgl", e) || t.getContext("experimental-webgl", e);
      const s = !!r?.getContextAttributes()?.stencil;
      if (r) {
        const i = r.getExtension("WEBGL_lose_context");
        i && i.loseContext();
      }
      return r = null, s;
    } catch {
      return !1;
    }
  })()), ms;
}
var Wo = {
  grad: 0.9,
  turn: 360,
  rad: 360 / (2 * Math.PI)
}, Qt = function(e) {
  return typeof e == "string" ? e.length > 0 : typeof e == "number";
}, Pt = function(e, t, r) {
  return t === void 0 && (t = 0), r === void 0 && (r = Math.pow(10, t)), Math.round(r * e) / r + 0;
}, Dt = function(e, t, r) {
  return t === void 0 && (t = 0), r === void 0 && (r = 1), e > r ? r : e > t ? e : t;
}, Bn = function(e) {
  return (e = isFinite(e) ? e % 360 : 0) > 0 ? e : e + 360;
}, Ni = function(e) {
  return {
    r: Dt(e.r, 0, 255),
    g: Dt(e.g, 0, 255),
    b: Dt(e.b, 0, 255),
    a: Dt(e.a)
  };
}, ys = function(e) {
  return {
    r: Pt(e.r),
    g: Pt(e.g),
    b: Pt(e.b),
    a: Pt(e.a, 3)
  };
}, qo = /^#([0-9a-f]{3,8})$/i, br = function(e) {
  var t = e.toString(16);
  return t.length < 2 ? "0" + t : t;
}, Un = function(e) {
  var t = e.r, r = e.g, s = e.b, i = e.a, n = Math.max(t, r, s), a = n - Math.min(t, r, s), o = a ? n === t ? (r - s) / a : n === r ? 2 + (s - t) / a : 4 + (t - r) / a : 0;
  return {
    h: 60 * (o < 0 ? o + 6 : o),
    s: n ? a / n * 100 : 0,
    v: n / 255 * 100,
    a: i
  };
}, kn = function(e) {
  var t = e.h, r = e.s, s = e.v, i = e.a;
  t = t / 360 * 6, r /= 100, s /= 100;
  var n = Math.floor(t), a = s * (1 - r), o = s * (1 - (t - n) * r), h = s * (1 - (1 - t + n) * r), l = n % 6;
  return {
    r: 255 * [
      s,
      o,
      a,
      a,
      h,
      s
    ][l],
    g: 255 * [
      h,
      s,
      s,
      o,
      a,
      a
    ][l],
    b: 255 * [
      a,
      a,
      h,
      s,
      s,
      o
    ][l],
    a: i
  };
}, Oi = function(e) {
  return {
    h: Bn(e.h),
    s: Dt(e.s, 0, 100),
    l: Dt(e.l, 0, 100),
    a: Dt(e.a)
  };
}, Bi = function(e) {
  return {
    h: Pt(e.h),
    s: Pt(e.s),
    l: Pt(e.l),
    a: Pt(e.a, 3)
  };
}, Ui = function(e) {
  return kn((r = (t = e).s, {
    h: t.h,
    s: (r *= ((s = t.l) < 50 ? s : 100 - s) / 100) > 0 ? 2 * r / (s + r) * 100 : 0,
    v: s + r,
    a: t.a
  }));
  var t, r, s;
}, ur = function(e) {
  return {
    h: (t = Un(e)).h,
    s: (i = (200 - (r = t.s)) * (s = t.v) / 100) > 0 && i < 200 ? r * s / 100 / (i <= 100 ? i : 200 - i) * 100 : 0,
    l: i / 2,
    a: t.a
  };
  var t, r, s, i;
}, jo = /^hsla?\(\s*([+-]?\d*\.?\d+)(deg|rad|grad|turn)?\s*,\s*([+-]?\d*\.?\d+)%\s*,\s*([+-]?\d*\.?\d+)%\s*(?:,\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Yo = /^hsla?\(\s*([+-]?\d*\.?\d+)(deg|rad|grad|turn)?\s+([+-]?\d*\.?\d+)%\s+([+-]?\d*\.?\d+)%\s*(?:\/\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Ko = /^rgba?\(\s*([+-]?\d*\.?\d+)(%)?\s*,\s*([+-]?\d*\.?\d+)(%)?\s*,\s*([+-]?\d*\.?\d+)(%)?\s*(?:,\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Zo = /^rgba?\(\s*([+-]?\d*\.?\d+)(%)?\s+([+-]?\d*\.?\d+)(%)?\s+([+-]?\d*\.?\d+)(%)?\s*(?:\/\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Ms = {
  string: [
    [function(e) {
      var t = qo.exec(e);
      return t ? (e = t[1]).length <= 4 ? {
        r: parseInt(e[0] + e[0], 16),
        g: parseInt(e[1] + e[1], 16),
        b: parseInt(e[2] + e[2], 16),
        a: e.length === 4 ? Pt(parseInt(e[3] + e[3], 16) / 255, 2) : 1
      } : e.length === 6 || e.length === 8 ? {
        r: parseInt(e.substr(0, 2), 16),
        g: parseInt(e.substr(2, 2), 16),
        b: parseInt(e.substr(4, 2), 16),
        a: e.length === 8 ? Pt(parseInt(e.substr(6, 2), 16) / 255, 2) : 1
      } : null : null;
    }, "hex"],
    [function(e) {
      var t = Ko.exec(e) || Zo.exec(e);
      return t ? t[2] !== t[4] || t[4] !== t[6] ? null : Ni({
        r: Number(t[1]) / (t[2] ? 100 / 255 : 1),
        g: Number(t[3]) / (t[4] ? 100 / 255 : 1),
        b: Number(t[5]) / (t[6] ? 100 / 255 : 1),
        a: t[7] === void 0 ? 1 : Number(t[7]) / (t[8] ? 100 : 1)
      }) : null;
    }, "rgb"],
    [function(e) {
      var t = jo.exec(e) || Yo.exec(e);
      if (!t) return null;
      var r, s;
      return Ui(Oi({
        h: (r = t[1], s = t[2], s === void 0 && (s = "deg"), Number(r) * (Wo[s] || 1)),
        s: Number(t[3]),
        l: Number(t[4]),
        a: t[5] === void 0 ? 1 : Number(t[5]) / (t[6] ? 100 : 1)
      }));
    }, "hsl"]
  ],
  object: [
    [function(e) {
      var t = e.r, r = e.g, s = e.b, i = e.a, n = i === void 0 ? 1 : i;
      return Qt(t) && Qt(r) && Qt(s) ? Ni({
        r: Number(t),
        g: Number(r),
        b: Number(s),
        a: Number(n)
      }) : null;
    }, "rgb"],
    [function(e) {
      var t = e.h, r = e.s, s = e.l, i = e.a, n = i === void 0 ? 1 : i;
      return !Qt(t) || !Qt(r) || !Qt(s) ? null : Ui(Oi({
        h: Number(t),
        s: Number(r),
        l: Number(s),
        a: Number(n)
      }));
    }, "hsl"],
    [function(e) {
      var t = e.h, r = e.s, s = e.v, i = e.a, n = i === void 0 ? 1 : i;
      return !Qt(t) || !Qt(r) || !Qt(s) ? null : kn((function(a) {
        return {
          h: Bn(a.h),
          s: Dt(a.s, 0, 100),
          v: Dt(a.v, 0, 100),
          a: Dt(a.a)
        };
      })({
        h: Number(t),
        s: Number(r),
        v: Number(s),
        a: Number(n)
      }));
    }, "hsv"]
  ]
}, ki = function(e, t) {
  for (var r = 0; r < t.length; r++) {
    var s = t[r][0](e);
    if (s) return [s, t[r][1]];
  }
  return [null, void 0];
}, Jo = function(e) {
  return typeof e == "string" ? ki(e.trim(), Ms.string) : typeof e == "object" && e !== null ? ki(e, Ms.object) : [null, void 0];
}, gs = function(e, t) {
  var r = ur(e);
  return {
    h: r.h,
    s: Dt(r.s + 100 * t, 0, 100),
    l: r.l,
    a: r.a
  };
}, vs = function(e) {
  return (299 * e.r + 587 * e.g + 114 * e.b) / 1e3 / 255;
}, Di = function(e, t) {
  var r = ur(e);
  return {
    h: r.h,
    s: r.s,
    l: Dt(r.l + 100 * t, 0, 100),
    a: r.a
  };
}, Cs = (function() {
  function e(t) {
    this.parsed = Jo(t)[0], this.rgba = this.parsed || {
      r: 0,
      g: 0,
      b: 0,
      a: 1
    };
  }
  return e.prototype.isValid = function() {
    return this.parsed !== null;
  }, e.prototype.brightness = function() {
    return Pt(vs(this.rgba), 2);
  }, e.prototype.isDark = function() {
    return vs(this.rgba) < 0.5;
  }, e.prototype.isLight = function() {
    return vs(this.rgba) >= 0.5;
  }, e.prototype.toHex = function() {
    return t = ys(this.rgba), r = t.r, s = t.g, i = t.b, a = (n = t.a) < 1 ? br(Pt(255 * n)) : "", "#" + br(r) + br(s) + br(i) + a;
    var t, r, s, i, n, a;
  }, e.prototype.toRgb = function() {
    return ys(this.rgba);
  }, e.prototype.toRgbString = function() {
    return t = ys(this.rgba), r = t.r, s = t.g, i = t.b, (n = t.a) < 1 ? "rgba(" + r + ", " + s + ", " + i + ", " + n + ")" : "rgb(" + r + ", " + s + ", " + i + ")";
    var t, r, s, i, n;
  }, e.prototype.toHsl = function() {
    return Bi(ur(this.rgba));
  }, e.prototype.toHslString = function() {
    return t = Bi(ur(this.rgba)), r = t.h, s = t.s, i = t.l, (n = t.a) < 1 ? "hsla(" + r + ", " + s + "%, " + i + "%, " + n + ")" : "hsl(" + r + ", " + s + "%, " + i + "%)";
    var t, r, s, i, n;
  }, e.prototype.toHsv = function() {
    return t = Un(this.rgba), {
      h: Pt(t.h),
      s: Pt(t.s),
      v: Pt(t.v),
      a: Pt(t.a, 3)
    };
    var t;
  }, e.prototype.invert = function() {
    return jt({
      r: 255 - (t = this.rgba).r,
      g: 255 - t.g,
      b: 255 - t.b,
      a: t.a
    });
    var t;
  }, e.prototype.saturate = function(t) {
    return t === void 0 && (t = 0.1), jt(gs(this.rgba, t));
  }, e.prototype.desaturate = function(t) {
    return t === void 0 && (t = 0.1), jt(gs(this.rgba, -t));
  }, e.prototype.grayscale = function() {
    return jt(gs(this.rgba, -1));
  }, e.prototype.lighten = function(t) {
    return t === void 0 && (t = 0.1), jt(Di(this.rgba, t));
  }, e.prototype.darken = function(t) {
    return t === void 0 && (t = 0.1), jt(Di(this.rgba, -t));
  }, e.prototype.rotate = function(t) {
    return t === void 0 && (t = 15), this.hue(this.hue() + t);
  }, e.prototype.alpha = function(t) {
    return typeof t == "number" ? jt({
      r: (r = this.rgba).r,
      g: r.g,
      b: r.b,
      a: t
    }) : Pt(this.rgba.a, 3);
    var r;
  }, e.prototype.hue = function(t) {
    var r = ur(this.rgba);
    return typeof t == "number" ? jt({
      h: t,
      s: r.s,
      l: r.l,
      a: r.a
    }) : Pt(r.h);
  }, e.prototype.isEqual = function(t) {
    return this.toHex() === jt(t).toHex();
  }, e;
})(), jt = function(e) {
  return e instanceof Cs ? e : new Cs(e);
}, Gi = [], Qo = function(e) {
  e.forEach(function(t) {
    Gi.indexOf(t) < 0 && (t(Cs, Ms), Gi.push(t));
  });
};
function th(e, t) {
  var r = {
    white: "#ffffff",
    bisque: "#ffe4c4",
    blue: "#0000ff",
    cadetblue: "#5f9ea0",
    chartreuse: "#7fff00",
    chocolate: "#d2691e",
    coral: "#ff7f50",
    antiquewhite: "#faebd7",
    aqua: "#00ffff",
    azure: "#f0ffff",
    whitesmoke: "#f5f5f5",
    papayawhip: "#ffefd5",
    plum: "#dda0dd",
    blanchedalmond: "#ffebcd",
    black: "#000000",
    gold: "#ffd700",
    goldenrod: "#daa520",
    gainsboro: "#dcdcdc",
    cornsilk: "#fff8dc",
    cornflowerblue: "#6495ed",
    burlywood: "#deb887",
    aquamarine: "#7fffd4",
    beige: "#f5f5dc",
    crimson: "#dc143c",
    cyan: "#00ffff",
    darkblue: "#00008b",
    darkcyan: "#008b8b",
    darkgoldenrod: "#b8860b",
    darkkhaki: "#bdb76b",
    darkgray: "#a9a9a9",
    darkgreen: "#006400",
    darkgrey: "#a9a9a9",
    peachpuff: "#ffdab9",
    darkmagenta: "#8b008b",
    darkred: "#8b0000",
    darkorchid: "#9932cc",
    darkorange: "#ff8c00",
    darkslateblue: "#483d8b",
    gray: "#808080",
    darkslategray: "#2f4f4f",
    darkslategrey: "#2f4f4f",
    deeppink: "#ff1493",
    deepskyblue: "#00bfff",
    wheat: "#f5deb3",
    firebrick: "#b22222",
    floralwhite: "#fffaf0",
    ghostwhite: "#f8f8ff",
    darkviolet: "#9400d3",
    magenta: "#ff00ff",
    green: "#008000",
    dodgerblue: "#1e90ff",
    grey: "#808080",
    honeydew: "#f0fff0",
    hotpink: "#ff69b4",
    blueviolet: "#8a2be2",
    forestgreen: "#228b22",
    lawngreen: "#7cfc00",
    indianred: "#cd5c5c",
    indigo: "#4b0082",
    fuchsia: "#ff00ff",
    brown: "#a52a2a",
    maroon: "#800000",
    mediumblue: "#0000cd",
    lightcoral: "#f08080",
    darkturquoise: "#00ced1",
    lightcyan: "#e0ffff",
    ivory: "#fffff0",
    lightyellow: "#ffffe0",
    lightsalmon: "#ffa07a",
    lightseagreen: "#20b2aa",
    linen: "#faf0e6",
    mediumaquamarine: "#66cdaa",
    lemonchiffon: "#fffacd",
    lime: "#00ff00",
    khaki: "#f0e68c",
    mediumseagreen: "#3cb371",
    limegreen: "#32cd32",
    mediumspringgreen: "#00fa9a",
    lightskyblue: "#87cefa",
    lightblue: "#add8e6",
    midnightblue: "#191970",
    lightpink: "#ffb6c1",
    mistyrose: "#ffe4e1",
    moccasin: "#ffe4b5",
    mintcream: "#f5fffa",
    lightslategray: "#778899",
    lightslategrey: "#778899",
    navajowhite: "#ffdead",
    navy: "#000080",
    mediumvioletred: "#c71585",
    powderblue: "#b0e0e6",
    palegoldenrod: "#eee8aa",
    oldlace: "#fdf5e6",
    paleturquoise: "#afeeee",
    mediumturquoise: "#48d1cc",
    mediumorchid: "#ba55d3",
    rebeccapurple: "#663399",
    lightsteelblue: "#b0c4de",
    mediumslateblue: "#7b68ee",
    thistle: "#d8bfd8",
    tan: "#d2b48c",
    orchid: "#da70d6",
    mediumpurple: "#9370db",
    purple: "#800080",
    pink: "#ffc0cb",
    skyblue: "#87ceeb",
    springgreen: "#00ff7f",
    palegreen: "#98fb98",
    red: "#ff0000",
    yellow: "#ffff00",
    slateblue: "#6a5acd",
    lavenderblush: "#fff0f5",
    peru: "#cd853f",
    palevioletred: "#db7093",
    violet: "#ee82ee",
    teal: "#008080",
    slategray: "#708090",
    slategrey: "#708090",
    aliceblue: "#f0f8ff",
    darkseagreen: "#8fbc8f",
    darkolivegreen: "#556b2f",
    greenyellow: "#adff2f",
    seagreen: "#2e8b57",
    seashell: "#fff5ee",
    tomato: "#ff6347",
    silver: "#c0c0c0",
    sienna: "#a0522d",
    lavender: "#e6e6fa",
    lightgreen: "#90ee90",
    orange: "#ffa500",
    orangered: "#ff4500",
    steelblue: "#4682b4",
    royalblue: "#4169e1",
    turquoise: "#40e0d0",
    yellowgreen: "#9acd32",
    salmon: "#fa8072",
    saddlebrown: "#8b4513",
    sandybrown: "#f4a460",
    rosybrown: "#bc8f8f",
    darksalmon: "#e9967a",
    lightgoldenrodyellow: "#fafad2",
    snow: "#fffafa",
    lightgrey: "#d3d3d3",
    lightgray: "#d3d3d3",
    dimgray: "#696969",
    dimgrey: "#696969",
    olivedrab: "#6b8e23",
    olive: "#808000"
  }, s = {};
  for (var i in r) s[r[i]] = i;
  var n = {};
  e.prototype.toName = function(a) {
    if (!(this.rgba.a || this.rgba.r || this.rgba.g || this.rgba.b)) return "transparent";
    var o, h, l = s[this.toHex()];
    if (l) return l;
    if (a?.closest) {
      var c = this.toRgb(), u = 1 / 0, d = "black";
      if (!n.length) for (var y in r) n[y] = new e(r[y]).toRgb();
      for (var m in r) {
        var v = (o = c, h = n[m], Math.pow(o.r - h.r, 2) + Math.pow(o.g - h.g, 2) + Math.pow(o.b - h.b, 2));
        v < u && (u = v, d = m);
      }
      return d;
    }
  }, t.string.push([function(a) {
    var o = a.toLowerCase(), h = o === "transparent" ? "#0000" : r[o];
    return h ? new e(h).toRgb() : null;
  }, "name"]);
}
Qo([th]);
var Ge = class Ur {
  constructor(t = 16777215) {
    this._value = null, this._components = /* @__PURE__ */ new Float32Array(4), this._components.fill(1), this._int = 16777215, this.value = t;
  }
  get red() {
    return this._components[0];
  }
  get green() {
    return this._components[1];
  }
  get blue() {
    return this._components[2];
  }
  get alpha() {
    return this._components[3];
  }
  setValue(t) {
    return this.value = t, this;
  }
  set value(t) {
    if (t instanceof Ur) this._value = this.cloneSource(t._value), this._int = t._int, this._components.set(t._components);
    else {
      if (t === null) throw new Error("Cannot set PIXI.Color#value to null");
      (this._value === null || !this.isSourceEqual(this._value, t)) && (this.normalize(t), this._value = this.cloneSource(t));
    }
  }
  get value() {
    return this._value;
  }
  cloneSource(t) {
    return typeof t == "string" || typeof t == "number" || t instanceof Number || t === null ? t : Array.isArray(t) || ArrayBuffer.isView(t) ? t.slice(0) : typeof t == "object" && t !== null ? { ...t } : t;
  }
  isSourceEqual(t, r) {
    const s = typeof t;
    if (s !== typeof r) return !1;
    if (s === "number" || s === "string" || t instanceof Number) return t === r;
    if (Array.isArray(t) && Array.isArray(r) || ArrayBuffer.isView(t) && ArrayBuffer.isView(r)) return t.length !== r.length ? !1 : t.every((i, n) => i === r[n]);
    if (t !== null && r !== null) {
      const i = Object.keys(t), n = Object.keys(r);
      return i.length !== n.length ? !1 : i.every((a) => t[a] === r[a]);
    }
    return t === r;
  }
  toRgba() {
    const [t, r, s, i] = this._components;
    return {
      r: t,
      g: r,
      b: s,
      a: i
    };
  }
  toRgb() {
    const [t, r, s] = this._components;
    return {
      r: t,
      g: r,
      b: s
    };
  }
  toRgbaString() {
    const [t, r, s] = this.toUint8RgbArray();
    return `rgba(${t},${r},${s},${this.alpha})`;
  }
  toUint8RgbArray(t) {
    const [r, s, i] = this._components;
    return t = t ?? [], t[0] = Math.round(r * 255), t[1] = Math.round(s * 255), t[2] = Math.round(i * 255), t;
  }
  toRgbArray(t) {
    t = t ?? [];
    const [r, s, i] = this._components;
    return t[0] = r, t[1] = s, t[2] = i, t;
  }
  toNumber() {
    return this._int;
  }
  toLittleEndianNumber() {
    const t = this._int;
    return (t >> 16) + (t & 65280) + ((t & 255) << 16);
  }
  multiply(t) {
    const [r, s, i, n] = Ur.temp.setValue(t)._components;
    return this._components[0] *= r, this._components[1] *= s, this._components[2] *= i, this._components[3] *= n, this.refreshInt(), this._value = null, this;
  }
  premultiply(t, r = !0) {
    return r && (this._components[0] *= t, this._components[1] *= t, this._components[2] *= t), this._components[3] = t, this.refreshInt(), this._value = null, this;
  }
  toPremultiplied(t, r = !0) {
    if (t === 1) return (255 << 24) + this._int;
    if (t === 0) return r ? 0 : this._int;
    let s = this._int >> 16 & 255, i = this._int >> 8 & 255, n = this._int & 255;
    return r && (s = s * t + 0.5 | 0, i = i * t + 0.5 | 0, n = n * t + 0.5 | 0), (t * 255 << 24) + (s << 16) + (i << 8) + n;
  }
  toHex() {
    const t = this._int.toString(16);
    return `#${"000000".substring(0, 6 - t.length) + t}`;
  }
  toHexa() {
    const t = Math.round(this._components[3] * 255).toString(16);
    return this.toHex() + "00".substring(0, 2 - t.length) + t;
  }
  setAlpha(t) {
    return this._components[3] = this._clamp(t), this;
  }
  round(t) {
    const [r, s, i] = this._components;
    return this._components[0] = Math.round(r * t) / t, this._components[1] = Math.round(s * t) / t, this._components[2] = Math.round(i * t) / t, this.refreshInt(), this._value = null, this;
  }
  toArray(t) {
    t = t ?? [];
    const [r, s, i, n] = this._components;
    return t[0] = r, t[1] = s, t[2] = i, t[3] = n, t;
  }
  normalize(t) {
    let r, s, i, n;
    if ((typeof t == "number" || t instanceof Number) && t >= 0 && t <= 16777215) {
      const a = t;
      r = (a >> 16 & 255) / 255, s = (a >> 8 & 255) / 255, i = (a & 255) / 255, n = 1;
    } else if ((Array.isArray(t) || t instanceof Float32Array) && t.length >= 3 && t.length <= 4) t = this._clamp(t), [r, s, i, n = 1] = t;
    else if ((t instanceof Uint8Array || t instanceof Uint8ClampedArray) && t.length >= 3 && t.length <= 4) t = this._clamp(t, 0, 255), [r, s, i, n = 255] = t, r /= 255, s /= 255, i /= 255, n /= 255;
    else if (typeof t == "string" || typeof t == "object") {
      if (typeof t == "string") {
        const o = Ur.HEX_PATTERN.exec(t);
        o && (t = `#${o[2]}`);
      }
      const a = jt(t);
      a.isValid() && ({ r, g: s, b: i, a: n } = a.rgba, r /= 255, s /= 255, i /= 255);
    }
    if (r !== void 0) this._components[0] = r, this._components[1] = s, this._components[2] = i, this._components[3] = n, this.refreshInt();
    else throw new Error(`Unable to convert color ${t}`);
  }
  refreshInt() {
    this._clamp(this._components);
    const [t, r, s] = this._components;
    this._int = (t * 255 << 16) + (r * 255 << 8) + (s * 255 | 0);
  }
  _clamp(t, r = 0, s = 1) {
    return typeof t == "number" ? Math.min(Math.max(t, r), s) : (t.forEach((i, n) => {
      t[n] = Math.min(Math.max(i, r), s);
    }), t);
  }
};
Ge.shared = new Ge(), Ge.temp = new Ge(), Ge.HEX_PATTERN = /^(#|0x)?(([a-f0-9]{3}){1,2}([a-f0-9]{2})?)$/i;
var Ae = Ge;
function eh() {
  const e = [], t = [];
  for (let s = 0; s < 32; s++) e[s] = s, t[s] = s;
  e[st.NORMAL_NPM] = st.NORMAL, e[st.ADD_NPM] = st.ADD, e[st.SCREEN_NPM] = st.SCREEN, t[st.NORMAL] = st.NORMAL_NPM, t[st.ADD] = st.ADD_NPM, t[st.SCREEN] = st.SCREEN_NPM;
  const r = [];
  return r.push(t), r.push(e), r;
}
var rh = eh();
function Dn(e) {
  if (e.BYTES_PER_ELEMENT === 4) return e instanceof Float32Array ? "Float32Array" : e instanceof Uint32Array ? "Uint32Array" : "Int32Array";
  if (e.BYTES_PER_ELEMENT === 2) {
    if (e instanceof Uint16Array) return "Uint16Array";
  } else if (e.BYTES_PER_ELEMENT === 1 && e instanceof Uint8Array) return "Uint8Array";
  return null;
}
function qr(e) {
  return e += e === 0 ? 1 : 0, --e, e |= e >>> 1, e |= e >>> 2, e |= e >>> 4, e |= e >>> 8, e |= e >>> 16, e + 1;
}
function zi(e) {
  return !(e & e - 1) && !!e;
}
function $i(e) {
  let t = (e > 65535 ? 1 : 0) << 4;
  e >>>= t;
  let r = (e > 255 ? 1 : 0) << 3;
  return e >>>= r, t |= r, r = (e > 15 ? 1 : 0) << 2, e >>>= r, t |= r, r = (e > 3 ? 1 : 0) << 1, e >>>= r, t |= r, t | e >> 1;
}
function kr(e, t, r) {
  const s = e.length;
  let i;
  if (t >= s || r === 0) return;
  r = t + r > s ? s - t : r;
  const n = s - r;
  for (i = t; i < n; ++i) e[i] = e[i + r];
  e.length = n;
}
function Tr(e) {
  return e === 0 ? 0 : e < 0 ? -1 : 1;
}
var sh = 0;
function fr() {
  return ++sh;
}
var Vi = class {
  constructor(e, t, r, s) {
    this.left = e, this.top = t, this.right = r, this.bottom = s;
  }
  get width() {
    return this.right - this.left;
  }
  get height() {
    return this.bottom - this.top;
  }
  isEmpty() {
    return this.left === this.right || this.top === this.bottom;
  }
};
Vi.EMPTY = new Vi(0, 0, 0, 0);
var Hi = {}, Yt = /* @__PURE__ */ Object.create(null), ne = /* @__PURE__ */ Object.create(null);
function ih(e, t = globalThis.location) {
  if (e.startsWith("data:")) return "";
  t = t || globalThis.location;
  const r = new URL(e, document.baseURI);
  return r.hostname !== t.hostname || r.port !== t.port || r.protocol !== t.protocol ? "anonymous" : "";
}
function Xi(e, t = 1) {
  const r = dt.RETINA_PREFIX?.exec(e);
  return r ? parseFloat(r[1]) : t;
}
var ut = /* @__PURE__ */ ((e) => (e.Renderer = "renderer", e.Application = "application", e.RendererSystem = "renderer-webgl-system", e.RendererPlugin = "renderer-webgl-plugin", e.CanvasRendererSystem = "renderer-canvas-system", e.CanvasRendererPlugin = "renderer-canvas-plugin", e.Asset = "asset", e.LoadParser = "load-parser", e.ResolveParser = "resolve-parser", e.CacheParser = "cache-parser", e.DetectionParser = "detection-parser", e))(ut || {}), Ps = (e) => {
  if (typeof e == "function" || typeof e == "object" && e.extension) {
    if (!e.extension) throw new Error("Extension class must have an extension object");
    e = {
      ...typeof e.extension != "object" ? { type: e.extension } : e.extension,
      ref: e
    };
  }
  if (typeof e == "object") e = { ...e };
  else throw new Error("Invalid extension type");
  return typeof e.type == "string" && (e.type = [e.type]), e;
}, Wi = (e, t) => Ps(e).priority ?? t, mt = {
  _addHandlers: {},
  _removeHandlers: {},
  _queue: {},
  remove(...e) {
    return e.map(Ps).forEach((t) => {
      t.type.forEach((r) => this._removeHandlers[r]?.(t));
    }), this;
  },
  add(...e) {
    return e.map(Ps).forEach((t) => {
      t.type.forEach((r) => {
        const s = this._addHandlers, i = this._queue;
        s[r] ? s[r]?.(t) : (i[r] = i[r] || [], i[r]?.push(t));
      });
    }), this;
  },
  handle(e, t, r) {
    const s = this._addHandlers, i = this._removeHandlers;
    if (s[e] || i[e]) throw new Error(`Extension type ${e} already has a handler`);
    s[e] = t, i[e] = r;
    const n = this._queue;
    return n[e] && (n[e]?.forEach((a) => t(a)), delete n[e]), this;
  },
  handleByMap(e, t) {
    return this.handle(e, (r) => {
      r.name && (t[r.name] = r.ref);
    }, (r) => {
      r.name && delete t[r.name];
    });
  },
  handleByList(e, t, r = -1) {
    return this.handle(e, (s) => {
      t.includes(s.ref) || (t.push(s.ref), t.sort((i, n) => Wi(n, r) - Wi(i, r)));
    }, (s) => {
      const i = t.indexOf(s.ref);
      i !== -1 && t.splice(i, 1);
    });
  }
}, nh = class {
  constructor(e) {
    typeof e == "number" ? this.rawBinaryData = new ArrayBuffer(e) : e instanceof Uint8Array ? this.rawBinaryData = e.buffer : this.rawBinaryData = e, this.uint32View = new Uint32Array(this.rawBinaryData), this.float32View = new Float32Array(this.rawBinaryData);
  }
  get int8View() {
    return this._int8View || (this._int8View = new Int8Array(this.rawBinaryData)), this._int8View;
  }
  get uint8View() {
    return this._uint8View || (this._uint8View = new Uint8Array(this.rawBinaryData)), this._uint8View;
  }
  get int16View() {
    return this._int16View || (this._int16View = new Int16Array(this.rawBinaryData)), this._int16View;
  }
  get uint16View() {
    return this._uint16View || (this._uint16View = new Uint16Array(this.rawBinaryData)), this._uint16View;
  }
  get int32View() {
    return this._int32View || (this._int32View = new Int32Array(this.rawBinaryData)), this._int32View;
  }
  view(e) {
    return this[`${e}View`];
  }
  destroy() {
    this.rawBinaryData = null, this._int8View = null, this._uint8View = null, this._int16View = null, this._uint16View = null, this._int32View = null, this.uint32View = null, this.float32View = null;
  }
  static sizeOf(e) {
    switch (e) {
      case "int8":
      case "uint8":
        return 1;
      case "int16":
      case "uint16":
        return 2;
      case "int32":
      case "uint32":
      case "float32":
        return 4;
      default:
        throw new Error(`${e} isn't a valid view type`);
    }
  }
}, ah = [
  "precision mediump float;",
  "void main(void){",
  "float test = 0.1;",
  "%forloop%",
  "gl_FragColor = vec4(0.0);",
  "}"
].join(`
`);
function oh(e) {
  let t = "";
  for (let r = 0; r < e; ++r) r > 0 && (t += `
else `), r < e - 1 && (t += `if(test == ${r}.0){}`);
  return t;
}
function hh(e, t) {
  if (e === 0) throw new Error("Invalid value of `0` passed to `checkMaxIfStatementsInShader`");
  const r = t.createShader(t.FRAGMENT_SHADER);
  for (; ; ) {
    const s = ah.replace(/%forloop%/gi, oh(e));
    if (t.shaderSource(r, s), t.compileShader(r), !t.getShaderParameter(r, t.COMPILE_STATUS)) e = e / 2 | 0;
    else break;
  }
  return e;
}
var ss = class Gn {
  constructor() {
    this.data = 0, this.blendMode = st.NORMAL, this.polygonOffset = 0, this.blend = !0, this.depthMask = !0;
  }
  get blend() {
    return !!(this.data & 1);
  }
  set blend(t) {
    !!(this.data & 1) !== t && (this.data ^= 1);
  }
  get offsets() {
    return !!(this.data & 2);
  }
  set offsets(t) {
    !!(this.data & 2) !== t && (this.data ^= 2);
  }
  get culling() {
    return !!(this.data & 4);
  }
  set culling(t) {
    !!(this.data & 4) !== t && (this.data ^= 4);
  }
  get depthTest() {
    return !!(this.data & 8);
  }
  set depthTest(t) {
    !!(this.data & 8) !== t && (this.data ^= 8);
  }
  get depthMask() {
    return !!(this.data & 32);
  }
  set depthMask(t) {
    !!(this.data & 32) !== t && (this.data ^= 32);
  }
  get clockwiseFrontFace() {
    return !!(this.data & 16);
  }
  set clockwiseFrontFace(t) {
    !!(this.data & 16) !== t && (this.data ^= 16);
  }
  get blendMode() {
    return this._blendMode;
  }
  set blendMode(t) {
    this.blend = t !== st.NONE, this._blendMode = t;
  }
  get polygonOffset() {
    return this._polygonOffset;
  }
  set polygonOffset(t) {
    this.offsets = !!t, this._polygonOffset = t;
  }
  static for2d() {
    const t = new Gn();
    return t.depthTest = !1, t.blend = !0, t;
  }
};
ss.prototype.toString = function() {
  return `[@pixi/core:State blendMode=${this.blendMode} clockwiseFrontFace=${this.clockwiseFrontFace} culling=${this.culling} depthMask=${this.depthMask} polygonOffset=${this.polygonOffset}]`;
};
var Ls = [];
function zn(e, t) {
  if (!e) return null;
  let r = "";
  if (typeof e == "string") {
    const s = /\.(\w{3,4})(?:$|\?|#)/i.exec(e);
    s && (r = s[1].toLowerCase());
  }
  for (let s = Ls.length - 1; s >= 0; --s) {
    const i = Ls[s];
    if (i.test && i.test(e, r)) return new i(e, t);
  }
  throw new Error("Unrecognized source type to auto-detect Resource");
}
var Wt = class {
  constructor(e) {
    this.items = [], this._name = e, this._aliasCount = 0;
  }
  emit(e, t, r, s, i, n, a, o) {
    if (arguments.length > 8) throw new Error("max arguments reached");
    const { name: h, items: l } = this;
    this._aliasCount++;
    for (let c = 0, u = l.length; c < u; c++) l[c][h](e, t, r, s, i, n, a, o);
    return l === this.items && this._aliasCount--, this;
  }
  ensureNonAliasedItems() {
    this._aliasCount > 0 && this.items.length > 1 && (this._aliasCount = 0, this.items = this.items.slice(0));
  }
  add(e) {
    return e[this._name] && (this.ensureNonAliasedItems(), this.remove(e), this.items.push(e)), this;
  }
  remove(e) {
    const t = this.items.indexOf(e);
    return t !== -1 && (this.ensureNonAliasedItems(), this.items.splice(t, 1)), this;
  }
  contains(e) {
    return this.items.includes(e);
  }
  removeAll() {
    return this.ensureNonAliasedItems(), this.items.length = 0, this;
  }
  destroy() {
    this.removeAll(), this.items.length = 0, this._name = "";
  }
  get empty() {
    return this.items.length === 0;
  }
  get name() {
    return this._name;
  }
};
Object.defineProperties(Wt.prototype, {
  dispatch: { value: Wt.prototype.emit },
  run: { value: Wt.prototype.emit }
});
var pr = class {
  constructor(e = 0, t = 0) {
    this._width = e, this._height = t, this.destroyed = !1, this.internal = !1, this.onResize = new Wt("setRealSize"), this.onUpdate = new Wt("update"), this.onError = new Wt("onError");
  }
  bind(e) {
    this.onResize.add(e), this.onUpdate.add(e), this.onError.add(e), (this._width || this._height) && this.onResize.emit(this._width, this._height);
  }
  unbind(e) {
    this.onResize.remove(e), this.onUpdate.remove(e), this.onError.remove(e);
  }
  resize(e, t) {
    (e !== this._width || t !== this._height) && (this._width = e, this._height = t, this.onResize.emit(e, t));
  }
  get valid() {
    return !!this._width && !!this._height;
  }
  update() {
    this.destroyed || this.onUpdate.emit();
  }
  load() {
    return Promise.resolve(this);
  }
  get width() {
    return this._width;
  }
  get height() {
    return this._height;
  }
  style(e, t, r) {
    return !1;
  }
  dispose() {
  }
  destroy() {
    this.destroyed || (this.destroyed = !0, this.dispose(), this.onError.removeAll(), this.onError = null, this.onResize.removeAll(), this.onResize = null, this.onUpdate.removeAll(), this.onUpdate = null);
  }
  static test(e, t) {
    return !1;
  }
}, $n = class extends pr {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    if (!r || !s) throw new Error("BufferResource width or height invalid");
    super(r, s), this.data = e, this.unpackAlignment = t.unpackAlignment ?? 4;
  }
  upload(e, t, r) {
    const s = e.gl;
    s.pixelStorei(s.UNPACK_ALIGNMENT, this.unpackAlignment), s.pixelStorei(s.UNPACK_PREMULTIPLY_ALPHA_WEBGL, t.alphaMode === Ce.UNPACK);
    const i = t.realWidth, n = t.realHeight;
    return r.width === i && r.height === n ? s.texSubImage2D(t.target, 0, 0, 0, i, n, t.format, r.type, this.data) : (r.width = i, r.height = n, s.texImage2D(t.target, 0, r.internalFormat, i, n, 0, t.format, r.type, this.data)), !0;
  }
  dispose() {
    this.data = null;
  }
  static test(e) {
    return e === null || e instanceof Int8Array || e instanceof Uint8Array || e instanceof Uint8ClampedArray || e instanceof Int16Array || e instanceof Uint16Array || e instanceof Int32Array || e instanceof Uint32Array || e instanceof Float32Array;
  }
}, lh = {
  scaleMode: te.NEAREST,
  alphaMode: Ce.NPM
}, Fs = class ze extends rs.default {
  constructor(t = null, r = null) {
    super(), r = Object.assign({}, ze.defaultOptions, r);
    const { alphaMode: s, mipmap: i, anisotropicLevel: n, scaleMode: a, width: o, height: h, wrapMode: l, format: c, type: u, target: d, resolution: y, resourceOptions: m } = r;
    t && !(t instanceof pr) && (t = zn(t, m), t.internal = !0), this.resolution = y || dt.RESOLUTION, this.width = Math.round((o || 0) * this.resolution) / this.resolution, this.height = Math.round((h || 0) * this.resolution) / this.resolution, this._mipmap = i, this.anisotropicLevel = n, this._wrapMode = l, this._scaleMode = a, this.format = c, this.type = u, this.target = d, this.alphaMode = s, this.uid = fr(), this.touched = 0, this.isPowerOfTwo = !1, this._refreshPOT(), this._glTextures = {}, this.dirtyId = 0, this.dirtyStyleId = 0, this.cacheId = null, this.valid = o > 0 && h > 0, this.textureCacheIds = [], this.destroyed = !1, this.resource = null, this._batchEnabled = 0, this._batchLocation = 0, this.parentTextureArray = null, this.setResource(t);
  }
  get realWidth() {
    return Math.round(this.width * this.resolution);
  }
  get realHeight() {
    return Math.round(this.height * this.resolution);
  }
  get mipmap() {
    return this._mipmap;
  }
  set mipmap(t) {
    this._mipmap !== t && (this._mipmap = t, this.dirtyStyleId++);
  }
  get scaleMode() {
    return this._scaleMode;
  }
  set scaleMode(t) {
    this._scaleMode !== t && (this._scaleMode = t, this.dirtyStyleId++);
  }
  get wrapMode() {
    return this._wrapMode;
  }
  set wrapMode(t) {
    this._wrapMode !== t && (this._wrapMode = t, this.dirtyStyleId++);
  }
  setStyle(t, r) {
    let s;
    return t !== void 0 && t !== this.scaleMode && (this.scaleMode = t, s = !0), r !== void 0 && r !== this.mipmap && (this.mipmap = r, s = !0), s && this.dirtyStyleId++, this;
  }
  setSize(t, r, s) {
    return s = s || this.resolution, this.setRealSize(t * s, r * s, s);
  }
  setRealSize(t, r, s) {
    return this.resolution = s || this.resolution, this.width = Math.round(t) / this.resolution, this.height = Math.round(r) / this.resolution, this._refreshPOT(), this.update(), this;
  }
  _refreshPOT() {
    this.isPowerOfTwo = zi(this.realWidth) && zi(this.realHeight);
  }
  setResolution(t) {
    const r = this.resolution;
    return r === t ? this : (this.resolution = t, this.valid && (this.width = Math.round(this.width * r) / t, this.height = Math.round(this.height * r) / t, this.emit("update", this)), this._refreshPOT(), this);
  }
  setResource(t) {
    if (this.resource === t) return this;
    if (this.resource) throw new Error("Resource can be set only once");
    return t.bind(this), this.resource = t, this;
  }
  update() {
    this.valid ? (this.dirtyId++, this.dirtyStyleId++, this.emit("update", this)) : this.width > 0 && this.height > 0 && (this.valid = !0, this.emit("loaded", this), this.emit("update", this));
  }
  onError(t) {
    this.emit("error", this, t);
  }
  destroy() {
    this.resource && (this.resource.unbind(this), this.resource.internal && this.resource.destroy(), this.resource = null), this.cacheId && (delete ne[this.cacheId], delete Yt[this.cacheId], this.cacheId = null), this.valid = !1, this.dispose(), ze.removeFromCache(this), this.textureCacheIds = null, this.destroyed = !0, this.emit("destroyed", this), this.removeAllListeners();
  }
  dispose() {
    this.emit("dispose", this);
  }
  castToBaseTexture() {
    return this;
  }
  static from(t, r, s = dt.STRICT_TEXTURE_CACHE) {
    const i = typeof t == "string";
    let n = null;
    i ? n = t : (t._pixiId || (t._pixiId = `${r?.pixiIdPrefix || "pixiid"}_${fr()}`), n = t._pixiId);
    let a = ne[n];
    if (i && s && !a) throw new Error(`The cacheId "${n}" does not exist in BaseTextureCache.`);
    return a || (a = new ze(t, r), a.cacheId = n, ze.addToCache(a, n)), a;
  }
  static fromBuffer(t, r, s, i) {
    t = t || new Float32Array(r * s * 4);
    const n = new $n(t, {
      width: r,
      height: s,
      ...i?.resourceOptions
    });
    let a, o;
    return t instanceof Float32Array ? (a = X.RGBA, o = ct.FLOAT) : t instanceof Int32Array ? (a = X.RGBA_INTEGER, o = ct.INT) : t instanceof Uint32Array ? (a = X.RGBA_INTEGER, o = ct.UNSIGNED_INT) : t instanceof Int16Array ? (a = X.RGBA_INTEGER, o = ct.SHORT) : t instanceof Uint16Array ? (a = X.RGBA_INTEGER, o = ct.UNSIGNED_SHORT) : t instanceof Int8Array ? (a = X.RGBA, o = ct.BYTE) : (a = X.RGBA, o = ct.UNSIGNED_BYTE), n.internal = !0, new ze(n, Object.assign({}, lh, {
      type: o,
      format: a
    }, i));
  }
  static addToCache(t, r) {
    r && (t.textureCacheIds.includes(r) || t.textureCacheIds.push(r), ne[r] && ne[r] !== t && console.warn(`BaseTexture added to the cache with an id [${r}] that already had an entry`), ne[r] = t);
  }
  static removeFromCache(t) {
    if (typeof t == "string") {
      const r = ne[t];
      if (r) {
        const s = r.textureCacheIds.indexOf(t);
        return s > -1 && r.textureCacheIds.splice(s, 1), delete ne[t], r;
      }
    } else if (t?.textureCacheIds) {
      for (let r = 0; r < t.textureCacheIds.length; ++r) delete ne[t.textureCacheIds[r]];
      return t.textureCacheIds.length = 0, t;
    }
    return null;
  }
};
Fs.defaultOptions = {
  mipmap: Se.POW2,
  anisotropicLevel: 0,
  scaleMode: te.LINEAR,
  wrapMode: Js.CLAMP,
  alphaMode: Ce.UNPACK,
  target: qe.TEXTURE_2D,
  format: X.RGBA,
  type: ct.UNSIGNED_BYTE
}, Fs._globalBatch = 0;
var vt = Fs, ch = class {
  constructor() {
    this.texArray = null, this.blend = 0, this.type = Wr.TRIANGLES, this.start = 0, this.size = 0, this.data = null;
  }
}, uh = 0, Ut = class Vn {
  constructor(t, r = !0, s = !1) {
    this.data = t || /* @__PURE__ */ new Float32Array(1), this._glBuffers = {}, this._updateID = 0, this.index = s, this.static = r, this.id = uh++, this.disposeRunner = new Wt("disposeBuffer");
  }
  update(t) {
    t instanceof Array && (t = new Float32Array(t)), this.data = t || this.data, this._updateID++;
  }
  dispose() {
    this.disposeRunner.emit(this, !1);
  }
  destroy() {
    this.dispose(), this.data = null;
  }
  set index(t) {
    this.type = t ? Zt.ELEMENT_ARRAY_BUFFER : Zt.ARRAY_BUFFER;
  }
  get index() {
    return this.type === Zt.ELEMENT_ARRAY_BUFFER;
  }
  static from(t) {
    return t instanceof Array && (t = new Float32Array(t)), new Vn(t);
  }
}, qi = class Hn {
  constructor(t, r = 0, s = !1, i = ct.FLOAT, n, a, o, h = 1) {
    this.buffer = t, this.size = r, this.normalized = s, this.type = i, this.stride = n, this.start = a, this.instance = o, this.divisor = h;
  }
  destroy() {
    this.buffer = null;
  }
  static from(t, r, s, i, n) {
    return new Hn(t, r, s, i, n);
  }
}, dh = {
  Float32Array,
  Uint32Array,
  Int32Array,
  Uint8Array
};
function fh(e, t) {
  let r = 0, s = 0;
  const i = {};
  for (let h = 0; h < e.length; h++) s += t[h], r += e[h].length;
  const n = /* @__PURE__ */ new ArrayBuffer(r * 4);
  let a = null, o = 0;
  for (let h = 0; h < e.length; h++) {
    const l = t[h], c = e[h], u = Dn(c);
    i[u] || (i[u] = new dh[u](n)), a = i[u];
    for (let d = 0; d < c.length; d++) {
      const y = (d / l | 0) * s + o, m = d % l;
      a[y + m] = c[d];
    }
    o += l;
  }
  return new Float32Array(n);
}
var ji = {
  5126: 4,
  5123: 2,
  5121: 1
}, ph = 0, mh = {
  Float32Array,
  Uint32Array,
  Int32Array,
  Uint8Array,
  Uint16Array
}, si = class Ns {
  constructor(t = [], r = {}) {
    this.buffers = t, this.indexBuffer = null, this.attributes = r, this.glVertexArrayObjects = {}, this.id = ph++, this.instanced = !1, this.instanceCount = 1, this.disposeRunner = new Wt("disposeGeometry"), this.refCount = 0;
  }
  addAttribute(t, r, s = 0, i = !1, n, a, o, h = !1) {
    if (!r) throw new Error("You must pass a buffer when creating an attribute");
    r instanceof Ut || (r instanceof Array && (r = new Float32Array(r)), r = new Ut(r));
    const l = t.split("|");
    if (l.length > 1) {
      for (let u = 0; u < l.length; u++) this.addAttribute(l[u], r, s, i, n);
      return this;
    }
    let c = this.buffers.indexOf(r);
    return c === -1 && (this.buffers.push(r), c = this.buffers.length - 1), this.attributes[t] = new qi(c, s, i, n, a, o, h), this.instanced = this.instanced || h, this;
  }
  getAttribute(t) {
    return this.attributes[t];
  }
  getBuffer(t) {
    return this.buffers[this.getAttribute(t).buffer];
  }
  addIndex(t) {
    return t instanceof Ut || (t instanceof Array && (t = new Uint16Array(t)), t = new Ut(t)), t.type = Zt.ELEMENT_ARRAY_BUFFER, this.indexBuffer = t, this.buffers.includes(t) || this.buffers.push(t), this;
  }
  getIndex() {
    return this.indexBuffer;
  }
  interleave() {
    if (this.buffers.length === 1 || this.buffers.length === 2 && this.indexBuffer) return this;
    const t = [], r = [], s = new Ut();
    let i;
    for (i in this.attributes) {
      const n = this.attributes[i], a = this.buffers[n.buffer];
      t.push(a.data), r.push(n.size * ji[n.type] / 4), n.buffer = 0;
    }
    for (s.data = fh(t, r), i = 0; i < this.buffers.length; i++) this.buffers[i] !== this.indexBuffer && this.buffers[i].destroy();
    return this.buffers = [s], this.indexBuffer && this.buffers.push(this.indexBuffer), this;
  }
  getSize() {
    for (const t in this.attributes) {
      const r = this.attributes[t];
      return this.buffers[r.buffer].data.length / (r.stride / 4 || r.size);
    }
    return 0;
  }
  dispose() {
    this.disposeRunner.emit(this, !1);
  }
  destroy() {
    this.dispose(), this.buffers = null, this.indexBuffer = null, this.attributes = null;
  }
  clone() {
    const t = new Ns();
    for (let r = 0; r < this.buffers.length; r++) t.buffers[r] = new Ut(this.buffers[r].data.slice(0));
    for (const r in this.attributes) {
      const s = this.attributes[r];
      t.attributes[r] = new qi(s.buffer, s.size, s.normalized, s.type, s.stride, s.start, s.instance);
    }
    return this.indexBuffer && (t.indexBuffer = t.buffers[this.buffers.indexOf(this.indexBuffer)], t.indexBuffer.type = Zt.ELEMENT_ARRAY_BUFFER), t;
  }
  static merge(t) {
    const r = new Ns(), s = [], i = [], n = [];
    let a;
    for (let o = 0; o < t.length; o++) {
      a = t[o];
      for (let h = 0; h < a.buffers.length; h++) i[h] = i[h] || 0, i[h] += a.buffers[h].data.length, n[h] = 0;
    }
    for (let o = 0; o < a.buffers.length; o++) s[o] = new mh[Dn(a.buffers[o].data)](i[o]), r.buffers[o] = new Ut(s[o]);
    for (let o = 0; o < t.length; o++) {
      a = t[o];
      for (let h = 0; h < a.buffers.length; h++) s[h].set(a.buffers[h].data, n[h]), n[h] += a.buffers[h].data.length;
    }
    if (r.attributes = a.attributes, a.indexBuffer) {
      r.indexBuffer = r.buffers[a.buffers.indexOf(a.indexBuffer)], r.indexBuffer.type = Zt.ELEMENT_ARRAY_BUFFER;
      let o = 0, h = 0, l = 0, c = 0;
      for (let u = 0; u < a.buffers.length; u++) if (a.buffers[u] !== a.indexBuffer) {
        c = u;
        break;
      }
      for (const u in a.attributes) {
        const d = a.attributes[u];
        (d.buffer | 0) === c && (h += d.size * ji[d.type] / 4);
      }
      for (let u = 0; u < t.length; u++) {
        const d = t[u].indexBuffer.data;
        for (let y = 0; y < d.length; y++) r.indexBuffer.data[y + l] += o;
        o += t[u].buffers[c].data.length / h, l += d.length;
      }
    }
    return r;
  }
}, yh = class extends si {
  constructor(e = !1) {
    super(), this._buffer = new Ut(null, e, !1), this._indexBuffer = new Ut(null, e, !0), this.addAttribute("aVertexPosition", this._buffer, 2, !1, ct.FLOAT).addAttribute("aTextureCoord", this._buffer, 2, !1, ct.FLOAT).addAttribute("aColor", this._buffer, 4, !0, ct.UNSIGNED_BYTE).addAttribute("aTextureId", this._buffer, 1, !0, ct.FLOAT).addIndex(this._indexBuffer);
  }
}, gh = Math.PI * 2, vh = 180 / Math.PI, xh = Math.PI / 180, Ze = /* @__PURE__ */ ((e) => (e[e.POLY = 0] = "POLY", e[e.RECT = 1] = "RECT", e[e.CIRC = 2] = "CIRC", e[e.ELIP = 3] = "ELIP", e[e.RREC = 4] = "RREC", e))(Ze || {}), Ft = class Xn {
  constructor(t = 0, r = 0) {
    this.x = 0, this.y = 0, this.x = t, this.y = r;
  }
  clone() {
    return new Xn(this.x, this.y);
  }
  copyFrom(t) {
    return this.set(t.x, t.y), this;
  }
  copyTo(t) {
    return t.set(this.x, this.y), t;
  }
  equals(t) {
    return t.x === this.x && t.y === this.y;
  }
  set(t = 0, r = t) {
    return this.x = t, this.y = r, this;
  }
};
Ft.prototype.toString = function() {
  return `[@pixi/math:Point x=${this.x} y=${this.y}]`;
};
var wr = [
  new Ft(),
  new Ft(),
  new Ft(),
  new Ft()
], Et = class Os {
  constructor(t = 0, r = 0, s = 0, i = 0) {
    this.x = Number(t), this.y = Number(r), this.width = Number(s), this.height = Number(i), this.type = Ze.RECT;
  }
  get left() {
    return this.x;
  }
  get right() {
    return this.x + this.width;
  }
  get top() {
    return this.y;
  }
  get bottom() {
    return this.y + this.height;
  }
  static get EMPTY() {
    return new Os(0, 0, 0, 0);
  }
  clone() {
    return new Os(this.x, this.y, this.width, this.height);
  }
  copyFrom(t) {
    return this.x = t.x, this.y = t.y, this.width = t.width, this.height = t.height, this;
  }
  copyTo(t) {
    return t.x = this.x, t.y = this.y, t.width = this.width, t.height = this.height, t;
  }
  contains(t, r) {
    return this.width <= 0 || this.height <= 0 ? !1 : t >= this.x && t < this.x + this.width && r >= this.y && r < this.y + this.height;
  }
  intersects(t, r) {
    if (!r) {
      const C = this.x < t.x ? t.x : this.x;
      if ((this.right > t.right ? t.right : this.right) <= C) return !1;
      const k = this.y < t.y ? t.y : this.y;
      return (this.bottom > t.bottom ? t.bottom : this.bottom) > k;
    }
    const s = this.left, i = this.right, n = this.top, a = this.bottom;
    if (i <= s || a <= n) return !1;
    const o = wr[0].set(t.left, t.top), h = wr[1].set(t.left, t.bottom), l = wr[2].set(t.right, t.top), c = wr[3].set(t.right, t.bottom);
    if (l.x <= o.x || h.y <= o.y) return !1;
    const u = Math.sign(r.a * r.d - r.b * r.c);
    if (u === 0 || (r.apply(o, o), r.apply(h, h), r.apply(l, l), r.apply(c, c), Math.max(o.x, h.x, l.x, c.x) <= s || Math.min(o.x, h.x, l.x, c.x) >= i || Math.max(o.y, h.y, l.y, c.y) <= n || Math.min(o.y, h.y, l.y, c.y) >= a)) return !1;
    const d = u * (h.y - o.y), y = u * (o.x - h.x), m = d * s + y * n, v = d * i + y * n, p = d * s + y * a, x = d * i + y * a;
    if (Math.max(m, v, p, x) <= d * o.x + y * o.y || Math.min(m, v, p, x) >= d * c.x + y * c.y) return !1;
    const f = u * (o.y - l.y), E = u * (l.x - o.x), g = f * s + E * n, A = f * i + E * n, L = f * s + E * a, M = f * i + E * a;
    return !(Math.max(g, A, L, M) <= f * o.x + E * o.y || Math.min(g, A, L, M) >= f * c.x + E * c.y);
  }
  pad(t = 0, r = t) {
    return this.x -= t, this.y -= r, this.width += t * 2, this.height += r * 2, this;
  }
  fit(t) {
    const r = Math.max(this.x, t.x), s = Math.min(this.x + this.width, t.x + t.width), i = Math.max(this.y, t.y), n = Math.min(this.y + this.height, t.y + t.height);
    return this.x = r, this.width = Math.max(s - r, 0), this.y = i, this.height = Math.max(n - i, 0), this;
  }
  ceil(t = 1, r = 1e-3) {
    const s = Math.ceil((this.x + this.width - r) * t) / t, i = Math.ceil((this.y + this.height - r) * t) / t;
    return this.x = Math.floor((this.x + r) * t) / t, this.y = Math.floor((this.y + r) * t) / t, this.width = s - this.x, this.height = i - this.y, this;
  }
  enlarge(t) {
    const r = Math.min(this.x, t.x), s = Math.max(this.x + this.width, t.x + t.width), i = Math.min(this.y, t.y), n = Math.max(this.y + this.height, t.y + t.height);
    return this.x = r, this.width = s - r, this.y = i, this.height = n - i, this;
  }
};
Et.prototype.toString = function() {
  return `[@pixi/math:Rectangle x=${this.x} y=${this.y} width=${this.width} height=${this.height}]`;
};
var _h = class Wn {
  constructor(t = 0, r = 0, s = 0) {
    this.x = t, this.y = r, this.radius = s, this.type = Ze.CIRC;
  }
  clone() {
    return new Wn(this.x, this.y, this.radius);
  }
  contains(t, r) {
    if (this.radius <= 0) return !1;
    const s = this.radius * this.radius;
    let i = this.x - t, n = this.y - r;
    return i *= i, n *= n, i + n <= s;
  }
  getBounds() {
    return new Et(this.x - this.radius, this.y - this.radius, this.radius * 2, this.radius * 2);
  }
};
_h.prototype.toString = function() {
  return `[@pixi/math:Circle x=${this.x} y=${this.y} radius=${this.radius}]`;
};
var bh = class qn {
  constructor(t = 0, r = 0, s = 0, i = 0) {
    this.x = t, this.y = r, this.width = s, this.height = i, this.type = Ze.ELIP;
  }
  clone() {
    return new qn(this.x, this.y, this.width, this.height);
  }
  contains(t, r) {
    if (this.width <= 0 || this.height <= 0) return !1;
    let s = (t - this.x) / this.width, i = (r - this.y) / this.height;
    return s *= s, i *= i, s + i <= 1;
  }
  getBounds() {
    return new Et(this.x - this.width, this.y - this.height, this.width, this.height);
  }
};
bh.prototype.toString = function() {
  return `[@pixi/math:Ellipse x=${this.x} y=${this.y} width=${this.width} height=${this.height}]`;
};
var Th = class jn {
  constructor(...t) {
    let r = Array.isArray(t[0]) ? t[0] : t;
    if (typeof r[0] != "number") {
      const s = [];
      for (let i = 0, n = r.length; i < n; i++) s.push(r[i].x, r[i].y);
      r = s;
    }
    this.points = r, this.type = Ze.POLY, this.closeStroke = !0;
  }
  clone() {
    const t = this.points.slice(), r = new jn(t);
    return r.closeStroke = this.closeStroke, r;
  }
  contains(t, r) {
    let s = !1;
    const i = this.points.length / 2;
    for (let n = 0, a = i - 1; n < i; a = n++) {
      const o = this.points[n * 2], h = this.points[n * 2 + 1], l = this.points[a * 2], c = this.points[a * 2 + 1];
      h > r != c > r && t < (l - o) * ((r - h) / (c - h)) + o && (s = !s);
    }
    return s;
  }
};
Th.prototype.toString = function() {
  return `[@pixi/math:PolygoncloseStroke=${this.closeStroke}points=${this.points.reduce((e, t) => `${e}, ${t}`, "")}]`;
};
var wh = class Yn {
  constructor(t = 0, r = 0, s = 0, i = 0, n = 20) {
    this.x = t, this.y = r, this.width = s, this.height = i, this.radius = n, this.type = Ze.RREC;
  }
  clone() {
    return new Yn(this.x, this.y, this.width, this.height, this.radius);
  }
  contains(t, r) {
    if (this.width <= 0 || this.height <= 0) return !1;
    if (t >= this.x && t <= this.x + this.width && r >= this.y && r <= this.y + this.height) {
      const s = Math.max(0, Math.min(this.radius, Math.min(this.width, this.height) / 2));
      if (r >= this.y + s && r <= this.y + this.height - s || t >= this.x + s && t <= this.x + this.width - s) return !0;
      let i = t - (this.x + s), n = r - (this.y + s);
      const a = s * s;
      if (i * i + n * n <= a || (i = t - (this.x + this.width - s), i * i + n * n <= a) || (n = r - (this.y + this.height - s), i * i + n * n <= a) || (i = t - (this.x + s), i * i + n * n <= a)) return !0;
    }
    return !1;
  }
};
wh.prototype.toString = function() {
  return `[@pixi/math:RoundedRectangle x=${this.x} y=${this.y}width=${this.width} height=${this.height} radius=${this.radius}]`;
};
var Lt = class Dr {
  constructor(t = 1, r = 0, s = 0, i = 1, n = 0, a = 0) {
    this.array = null, this.a = t, this.b = r, this.c = s, this.d = i, this.tx = n, this.ty = a;
  }
  fromArray(t) {
    this.a = t[0], this.b = t[1], this.c = t[3], this.d = t[4], this.tx = t[2], this.ty = t[5];
  }
  set(t, r, s, i, n, a) {
    return this.a = t, this.b = r, this.c = s, this.d = i, this.tx = n, this.ty = a, this;
  }
  toArray(t, r) {
    this.array || (this.array = /* @__PURE__ */ new Float32Array(9));
    const s = r || this.array;
    return t ? (s[0] = this.a, s[1] = this.b, s[2] = 0, s[3] = this.c, s[4] = this.d, s[5] = 0, s[6] = this.tx, s[7] = this.ty, s[8] = 1) : (s[0] = this.a, s[1] = this.c, s[2] = this.tx, s[3] = this.b, s[4] = this.d, s[5] = this.ty, s[6] = 0, s[7] = 0, s[8] = 1), s;
  }
  apply(t, r) {
    r = r || new Ft();
    const s = t.x, i = t.y;
    return r.x = this.a * s + this.c * i + this.tx, r.y = this.b * s + this.d * i + this.ty, r;
  }
  applyInverse(t, r) {
    r = r || new Ft();
    const s = 1 / (this.a * this.d + this.c * -this.b), i = t.x, n = t.y;
    return r.x = this.d * s * i + -this.c * s * n + (this.ty * this.c - this.tx * this.d) * s, r.y = this.a * s * n + -this.b * s * i + (-this.ty * this.a + this.tx * this.b) * s, r;
  }
  translate(t, r) {
    return this.tx += t, this.ty += r, this;
  }
  scale(t, r) {
    return this.a *= t, this.d *= r, this.c *= t, this.b *= r, this.tx *= t, this.ty *= r, this;
  }
  rotate(t) {
    const r = Math.cos(t), s = Math.sin(t), i = this.a, n = this.c, a = this.tx;
    return this.a = i * r - this.b * s, this.b = i * s + this.b * r, this.c = n * r - this.d * s, this.d = n * s + this.d * r, this.tx = a * r - this.ty * s, this.ty = a * s + this.ty * r, this;
  }
  append(t) {
    const r = this.a, s = this.b, i = this.c, n = this.d;
    return this.a = t.a * r + t.b * i, this.b = t.a * s + t.b * n, this.c = t.c * r + t.d * i, this.d = t.c * s + t.d * n, this.tx = t.tx * r + t.ty * i + this.tx, this.ty = t.tx * s + t.ty * n + this.ty, this;
  }
  setTransform(t, r, s, i, n, a, o, h, l) {
    return this.a = Math.cos(o + l) * n, this.b = Math.sin(o + l) * n, this.c = -Math.sin(o - h) * a, this.d = Math.cos(o - h) * a, this.tx = t - (s * this.a + i * this.c), this.ty = r - (s * this.b + i * this.d), this;
  }
  prepend(t) {
    const r = this.tx;
    if (t.a !== 1 || t.b !== 0 || t.c !== 0 || t.d !== 1) {
      const s = this.a, i = this.c;
      this.a = s * t.a + this.b * t.c, this.b = s * t.b + this.b * t.d, this.c = i * t.a + this.d * t.c, this.d = i * t.b + this.d * t.d;
    }
    return this.tx = r * t.a + this.ty * t.c + t.tx, this.ty = r * t.b + this.ty * t.d + t.ty, this;
  }
  decompose(t) {
    const r = this.a, s = this.b, i = this.c, n = this.d, a = t.pivot, o = -Math.atan2(-i, n), h = Math.atan2(s, r), l = Math.abs(o + h);
    return l < 1e-5 || Math.abs(gh - l) < 1e-5 ? (t.rotation = h, t.skew.x = t.skew.y = 0) : (t.rotation = 0, t.skew.x = o, t.skew.y = h), t.scale.x = Math.sqrt(r * r + s * s), t.scale.y = Math.sqrt(i * i + n * n), t.position.x = this.tx + (a.x * r + a.y * i), t.position.y = this.ty + (a.x * s + a.y * n), t;
  }
  invert() {
    const t = this.a, r = this.b, s = this.c, i = this.d, n = this.tx, a = t * i - r * s;
    return this.a = i / a, this.b = -r / a, this.c = -s / a, this.d = t / a, this.tx = (s * this.ty - i * n) / a, this.ty = -(t * this.ty - r * n) / a, this;
  }
  identity() {
    return this.a = 1, this.b = 0, this.c = 0, this.d = 1, this.tx = 0, this.ty = 0, this;
  }
  clone() {
    const t = new Dr();
    return t.a = this.a, t.b = this.b, t.c = this.c, t.d = this.d, t.tx = this.tx, t.ty = this.ty, t;
  }
  copyTo(t) {
    return t.a = this.a, t.b = this.b, t.c = this.c, t.d = this.d, t.tx = this.tx, t.ty = this.ty, t;
  }
  copyFrom(t) {
    return this.a = t.a, this.b = t.b, this.c = t.c, this.d = t.d, this.tx = t.tx, this.ty = t.ty, this;
  }
  static get IDENTITY() {
    return new Dr();
  }
  static get TEMP_MATRIX() {
    return new Dr();
  }
};
Lt.prototype.toString = function() {
  return `[@pixi/math:Matrix a=${this.a} b=${this.b} c=${this.c} d=${this.d} tx=${this.tx} ty=${this.ty}]`;
};
var ye = [
  1,
  1,
  0,
  -1,
  -1,
  -1,
  0,
  1,
  1,
  1,
  0,
  -1,
  -1,
  -1,
  0,
  1
], ge = [
  0,
  1,
  1,
  1,
  0,
  -1,
  -1,
  -1,
  0,
  1,
  1,
  1,
  0,
  -1,
  -1,
  -1
], ve = [
  0,
  -1,
  -1,
  -1,
  0,
  1,
  1,
  1,
  0,
  1,
  1,
  1,
  0,
  -1,
  -1,
  -1
], xe = [
  1,
  1,
  0,
  -1,
  -1,
  -1,
  0,
  1,
  -1,
  -1,
  0,
  1,
  1,
  1,
  0,
  -1
], Bs = [], Kn = [], Er = Math.sign;
function Eh() {
  for (let e = 0; e < 16; e++) {
    const t = [];
    Bs.push(t);
    for (let r = 0; r < 16; r++) {
      const s = Er(ye[e] * ye[r] + ve[e] * ge[r]), i = Er(ge[e] * ye[r] + xe[e] * ge[r]), n = Er(ye[e] * ve[r] + ve[e] * xe[r]), a = Er(ge[e] * ve[r] + xe[e] * xe[r]);
      for (let o = 0; o < 16; o++) if (ye[o] === s && ge[o] === i && ve[o] === n && xe[o] === a) {
        t.push(o);
        break;
      }
    }
  }
  for (let e = 0; e < 16; e++) {
    const t = new Lt();
    t.set(ye[e], ge[e], ve[e], xe[e], 0, 0), Kn.push(t);
  }
}
Eh();
var At = {
  E: 0,
  SE: 1,
  S: 2,
  SW: 3,
  W: 4,
  NW: 5,
  N: 6,
  NE: 7,
  MIRROR_VERTICAL: 8,
  MAIN_DIAGONAL: 10,
  MIRROR_HORIZONTAL: 12,
  REVERSE_DIAGONAL: 14,
  uX: (e) => ye[e],
  uY: (e) => ge[e],
  vX: (e) => ve[e],
  vY: (e) => xe[e],
  inv: (e) => e & 8 ? e & 15 : -e & 7,
  add: (e, t) => Bs[e][t],
  sub: (e, t) => Bs[e][At.inv(t)],
  rotate180: (e) => e ^ 4,
  isVertical: (e) => (e & 3) === 2,
  byDirection: (e, t) => Math.abs(e) * 2 <= Math.abs(t) ? t >= 0 ? At.S : At.N : Math.abs(t) * 2 <= Math.abs(e) ? e > 0 ? At.E : At.W : t > 0 ? e > 0 ? At.SE : At.SW : e > 0 ? At.NE : At.NW,
  matrixAppendRotationInv: (e, t, r = 0, s = 0) => {
    const i = Kn[At.inv(t)];
    i.tx = r, i.ty = s, e.append(i);
  }
}, Xe = class Zn {
  constructor(t, r, s = 0, i = 0) {
    this._x = s, this._y = i, this.cb = t, this.scope = r;
  }
  clone(t = this.cb, r = this.scope) {
    return new Zn(t, r, this._x, this._y);
  }
  set(t = 0, r = t) {
    return (this._x !== t || this._y !== r) && (this._x = t, this._y = r, this.cb.call(this.scope)), this;
  }
  copyFrom(t) {
    return (this._x !== t.x || this._y !== t.y) && (this._x = t.x, this._y = t.y, this.cb.call(this.scope)), this;
  }
  copyTo(t) {
    return t.set(this._x, this._y), t;
  }
  equals(t) {
    return t.x === this._x && t.y === this._y;
  }
  get x() {
    return this._x;
  }
  set x(t) {
    this._x !== t && (this._x = t, this.cb.call(this.scope));
  }
  get y() {
    return this._y;
  }
  set y(t) {
    this._y !== t && (this._y = t, this.cb.call(this.scope));
  }
};
Xe.prototype.toString = function() {
  return `[@pixi/math:ObservablePoint x=${this.x} y=${this.y} scope=${this.scope}]`;
};
var Us = class {
  constructor() {
    this.worldTransform = new Lt(), this.localTransform = new Lt(), this.position = new Xe(this.onChange, this, 0, 0), this.scale = new Xe(this.onChange, this, 1, 1), this.pivot = new Xe(this.onChange, this, 0, 0), this.skew = new Xe(this.updateSkew, this, 0, 0), this._rotation = 0, this._cx = 1, this._sx = 0, this._cy = 0, this._sy = 1, this._localID = 0, this._currentLocalID = 0, this._worldID = 0, this._parentID = 0;
  }
  onChange() {
    this._localID++;
  }
  updateSkew() {
    this._cx = Math.cos(this._rotation + this.skew.y), this._sx = Math.sin(this._rotation + this.skew.y), this._cy = -Math.sin(this._rotation - this.skew.x), this._sy = Math.cos(this._rotation - this.skew.x), this._localID++;
  }
  updateLocalTransform() {
    const e = this.localTransform;
    this._localID !== this._currentLocalID && (e.a = this._cx * this.scale.x, e.b = this._sx * this.scale.x, e.c = this._cy * this.scale.y, e.d = this._sy * this.scale.y, e.tx = this.position.x - (this.pivot.x * e.a + this.pivot.y * e.c), e.ty = this.position.y - (this.pivot.x * e.b + this.pivot.y * e.d), this._currentLocalID = this._localID, this._parentID = -1);
  }
  updateTransform(e) {
    const t = this.localTransform;
    if (this._localID !== this._currentLocalID && (t.a = this._cx * this.scale.x, t.b = this._sx * this.scale.x, t.c = this._cy * this.scale.y, t.d = this._sy * this.scale.y, t.tx = this.position.x - (this.pivot.x * t.a + this.pivot.y * t.c), t.ty = this.position.y - (this.pivot.x * t.b + this.pivot.y * t.d), this._currentLocalID = this._localID, this._parentID = -1), this._parentID !== e._worldID) {
      const r = e.worldTransform, s = this.worldTransform;
      s.a = t.a * r.a + t.b * r.c, s.b = t.a * r.b + t.b * r.d, s.c = t.c * r.a + t.d * r.c, s.d = t.c * r.b + t.d * r.d, s.tx = t.tx * r.a + t.ty * r.c + r.tx, s.ty = t.tx * r.b + t.ty * r.d + r.ty, this._parentID = e._worldID, this._worldID++;
    }
  }
  setFromMatrix(e) {
    e.decompose(this), this._localID++;
  }
  get rotation() {
    return this._rotation;
  }
  set rotation(e) {
    this._rotation !== e && (this._rotation = e, this.updateSkew());
  }
};
Us.IDENTITY = new Us();
var ii = Us;
ii.prototype.toString = function() {
  return `[@pixi/math:Transform position=(${this.position.x}, ${this.position.y}) rotation=${this.rotation} scale=(${this.scale.x}, ${this.scale.y}) skew=(${this.skew.x}, ${this.skew.y}) ]`;
};
var Sh = `varying vec2 vTextureCoord;

uniform sampler2D uSampler;

void main(void){
   gl_FragColor *= texture2D(uSampler, vTextureCoord);
}`, Ah = `attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void){
   gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
   vTextureCoord = aTextureCoord;
}
`;
function Yi(e, t, r) {
  const s = e.createShader(t);
  return e.shaderSource(s, r), e.compileShader(s), s;
}
function xs(e) {
  const t = new Array(e);
  for (let r = 0; r < t.length; r++) t[r] = !1;
  return t;
}
function Jn(e, t) {
  switch (e) {
    case "float":
      return 0;
    case "vec2":
      return new Float32Array(2 * t);
    case "vec3":
      return new Float32Array(3 * t);
    case "vec4":
      return new Float32Array(4 * t);
    case "int":
    case "uint":
    case "sampler2D":
    case "sampler2DArray":
      return 0;
    case "ivec2":
      return new Int32Array(2 * t);
    case "ivec3":
      return new Int32Array(3 * t);
    case "ivec4":
      return new Int32Array(4 * t);
    case "uvec2":
      return new Uint32Array(2 * t);
    case "uvec3":
      return new Uint32Array(3 * t);
    case "uvec4":
      return new Uint32Array(4 * t);
    case "bool":
      return !1;
    case "bvec2":
      return xs(2 * t);
    case "bvec3":
      return xs(3 * t);
    case "bvec4":
      return xs(4 * t);
    case "mat2":
      return new Float32Array([
        1,
        0,
        0,
        1
      ]);
    case "mat3":
      return new Float32Array([
        1,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        1
      ]);
    case "mat4":
      return new Float32Array([
        1,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        1
      ]);
  }
  return null;
}
var je = [
  {
    test: (e) => e.type === "float" && e.size === 1 && !e.isArray,
    code: (e) => `
            if(uv["${e}"] !== ud["${e}"].value)
            {
                ud["${e}"].value = uv["${e}"]
                gl.uniform1f(ud["${e}"].location, uv["${e}"])
            }
            `
  },
  {
    test: (e, t) => (e.type === "sampler2D" || e.type === "samplerCube" || e.type === "sampler2DArray") && e.size === 1 && !e.isArray && (t == null || t.castToBaseTexture !== void 0),
    code: (e) => `t = syncData.textureCount++;

            renderer.texture.bind(uv["${e}"], t);

            if(ud["${e}"].value !== t)
            {
                ud["${e}"].value = t;
                gl.uniform1i(ud["${e}"].location, t);
; // eslint-disable-line max-len
            }`
  },
  {
    test: (e, t) => e.type === "mat3" && e.size === 1 && !e.isArray && t.a !== void 0,
    code: (e) => `
            gl.uniformMatrix3fv(ud["${e}"].location, false, uv["${e}"].toArray(true));
            `,
    codeUbo: (e) => `
                var ${e}_matrix = uv.${e}.toArray(true);

                data[offset] = ${e}_matrix[0];
                data[offset+1] = ${e}_matrix[1];
                data[offset+2] = ${e}_matrix[2];
        
                data[offset + 4] = ${e}_matrix[3];
                data[offset + 5] = ${e}_matrix[4];
                data[offset + 6] = ${e}_matrix[5];
        
                data[offset + 8] = ${e}_matrix[6];
                data[offset + 9] = ${e}_matrix[7];
                data[offset + 10] = ${e}_matrix[8];
            `
  },
  {
    test: (e, t) => e.type === "vec2" && e.size === 1 && !e.isArray && t.x !== void 0,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v.x || cv[1] !== v.y)
                {
                    cv[0] = v.x;
                    cv[1] = v.y;
                    gl.uniform2f(ud["${e}"].location, v.x, v.y);
                }`,
    codeUbo: (e) => `
                v = uv.${e};

                data[offset] = v.x;
                data[offset+1] = v.y;
            `
  },
  {
    test: (e) => e.type === "vec2" && e.size === 1 && !e.isArray,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v[0] || cv[1] !== v[1])
                {
                    cv[0] = v[0];
                    cv[1] = v[1];
                    gl.uniform2f(ud["${e}"].location, v[0], v[1]);
                }
            `
  },
  {
    test: (e, t) => e.type === "vec4" && e.size === 1 && !e.isArray && t.width !== void 0,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v.x || cv[1] !== v.y || cv[2] !== v.width || cv[3] !== v.height)
                {
                    cv[0] = v.x;
                    cv[1] = v.y;
                    cv[2] = v.width;
                    cv[3] = v.height;
                    gl.uniform4f(ud["${e}"].location, v.x, v.y, v.width, v.height)
                }`,
    codeUbo: (e) => `
                    v = uv.${e};

                    data[offset] = v.x;
                    data[offset+1] = v.y;
                    data[offset+2] = v.width;
                    data[offset+3] = v.height;
                `
  },
  {
    test: (e, t) => e.type === "vec4" && e.size === 1 && !e.isArray && t.red !== void 0,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v.red || cv[1] !== v.green || cv[2] !== v.blue || cv[3] !== v.alpha)
                {
                    cv[0] = v.red;
                    cv[1] = v.green;
                    cv[2] = v.blue;
                    cv[3] = v.alpha;
                    gl.uniform4f(ud["${e}"].location, v.red, v.green, v.blue, v.alpha)
                }`,
    codeUbo: (e) => `
                    v = uv.${e};

                    data[offset] = v.red;
                    data[offset+1] = v.green;
                    data[offset+2] = v.blue;
                    data[offset+3] = v.alpha;
                `
  },
  {
    test: (e, t) => e.type === "vec3" && e.size === 1 && !e.isArray && t.red !== void 0,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v.red || cv[1] !== v.green || cv[2] !== v.blue || cv[3] !== v.a)
                {
                    cv[0] = v.red;
                    cv[1] = v.green;
                    cv[2] = v.blue;
    
                    gl.uniform3f(ud["${e}"].location, v.red, v.green, v.blue)
                }`,
    codeUbo: (e) => `
                    v = uv.${e};

                    data[offset] = v.red;
                    data[offset+1] = v.green;
                    data[offset+2] = v.blue;
                `
  },
  {
    test: (e) => e.type === "vec4" && e.size === 1 && !e.isArray,
    code: (e) => `
                cv = ud["${e}"].value;
                v = uv["${e}"];

                if(cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
                {
                    cv[0] = v[0];
                    cv[1] = v[1];
                    cv[2] = v[2];
                    cv[3] = v[3];

                    gl.uniform4f(ud["${e}"].location, v[0], v[1], v[2], v[3])
                }`
  }
], Ih = {
  float: `
    if (cv !== v)
    {
        cu.value = v;
        gl.uniform1f(location, v);
    }`,
  vec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2f(location, v[0], v[1])
    }`,
  vec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3f(location, v[0], v[1], v[2])
    }`,
  vec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4f(location, v[0], v[1], v[2], v[3]);
    }`,
  int: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`,
  ivec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2i(location, v[0], v[1]);
    }`,
  ivec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3i(location, v[0], v[1], v[2]);
    }`,
  ivec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4i(location, v[0], v[1], v[2], v[3]);
    }`,
  uint: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1ui(location, v);
    }`,
  uvec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2ui(location, v[0], v[1]);
    }`,
  uvec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3ui(location, v[0], v[1], v[2]);
    }`,
  uvec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4ui(location, v[0], v[1], v[2], v[3]);
    }`,
  bool: `
    if (cv !== v)
    {
        cu.value = v;
        gl.uniform1i(location, v);
    }`,
  bvec2: `
    if (cv[0] != v[0] || cv[1] != v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2i(location, v[0], v[1]);
    }`,
  bvec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3i(location, v[0], v[1], v[2]);
    }`,
  bvec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4i(location, v[0], v[1], v[2], v[3]);
    }`,
  mat2: "gl.uniformMatrix2fv(location, false, v)",
  mat3: "gl.uniformMatrix3fv(location, false, v)",
  mat4: "gl.uniformMatrix4fv(location, false, v)",
  sampler2D: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`,
  samplerCube: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`,
  sampler2DArray: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`
}, Rh = {
  float: "gl.uniform1fv(location, v)",
  vec2: "gl.uniform2fv(location, v)",
  vec3: "gl.uniform3fv(location, v)",
  vec4: "gl.uniform4fv(location, v)",
  mat4: "gl.uniformMatrix4fv(location, false, v)",
  mat3: "gl.uniformMatrix3fv(location, false, v)",
  mat2: "gl.uniformMatrix2fv(location, false, v)",
  int: "gl.uniform1iv(location, v)",
  ivec2: "gl.uniform2iv(location, v)",
  ivec3: "gl.uniform3iv(location, v)",
  ivec4: "gl.uniform4iv(location, v)",
  uint: "gl.uniform1uiv(location, v)",
  uvec2: "gl.uniform2uiv(location, v)",
  uvec3: "gl.uniform3uiv(location, v)",
  uvec4: "gl.uniform4uiv(location, v)",
  bool: "gl.uniform1iv(location, v)",
  bvec2: "gl.uniform2iv(location, v)",
  bvec3: "gl.uniform3iv(location, v)",
  bvec4: "gl.uniform4iv(location, v)",
  sampler2D: "gl.uniform1iv(location, v)",
  samplerCube: "gl.uniform1iv(location, v)",
  sampler2DArray: "gl.uniform1iv(location, v)"
};
function Mh(e, t) {
  const r = [`
        var v = null;
        var cv = null;
        var cu = null;
        var t = 0;
        var gl = renderer.gl;
    `];
  for (const s in e.uniforms) {
    const i = t[s];
    if (!i) {
      e.uniforms[s]?.group === !0 && (e.uniforms[s].ubo ? r.push(`
                        renderer.shader.syncUniformBufferGroup(uv.${s}, '${s}');
                    `) : r.push(`
                        renderer.shader.syncUniformGroup(uv.${s}, syncData);
                    `));
      continue;
    }
    const n = e.uniforms[s];
    let a = !1;
    for (let o = 0; o < je.length; o++) if (je[o].test(i, n)) {
      r.push(je[o].code(s, n)), a = !0;
      break;
    }
    if (!a) {
      const o = (i.size === 1 && !i.isArray ? Ih : Rh)[i.type].replace("location", `ud["${s}"].location`);
      r.push(`
            cu = ud["${s}"];
            cv = cu.value;
            v = uv["${s}"];
            ${o};`);
    }
  }
  return new Function("ud", "uv", "renderer", "syncData", r.join(`
`));
}
var Qn = {}, Sr = Qn;
function Ch() {
  if (Sr === Qn || Sr?.isContextLost()) {
    const e = dt.ADAPTER.createCanvas();
    let t;
    dt.PREFER_ENV >= Me.WEBGL2 && (t = e.getContext("webgl2", {})), t || (t = e.getContext("webgl", {}) || e.getContext("experimental-webgl", {}), t ? t.getExtension("WEBGL_draw_buffers") : t = null), Sr = t;
  }
  return Sr;
}
var Ar;
function Ph() {
  if (!Ar) {
    Ar = kt.MEDIUM;
    const e = Ch();
    if (e && e.getShaderPrecisionFormat) {
      const t = e.getShaderPrecisionFormat(e.FRAGMENT_SHADER, e.HIGH_FLOAT);
      t && (Ar = t.precision ? kt.HIGH : kt.MEDIUM);
    }
  }
  return Ar;
}
function Ki(e, t) {
  const r = e.getShaderSource(t).split(`
`).map((h, l) => `${l}: ${h}`), s = e.getShaderInfoLog(t), i = s.split(`
`), n = {}, a = i.map((h) => parseFloat(h.replace(/^ERROR\: 0\:([\d]+)\:.*$/, "$1"))).filter((h) => h && !n[h] ? (n[h] = !0, !0) : !1), o = [""];
  a.forEach((h) => {
    r[h - 1] = `%c${r[h - 1]}%c`, o.push("background: #FF0000; color:#FFFFFF; font-size: 10px", "font-size: 10px");
  }), o[0] = r.join(`
`), console.error(s), console.groupCollapsed("click to view full shader code"), console.warn(...o), console.groupEnd();
}
function Lh(e, t, r, s) {
  e.getProgramParameter(t, e.LINK_STATUS) || (e.getShaderParameter(r, e.COMPILE_STATUS) || Ki(e, r), e.getShaderParameter(s, e.COMPILE_STATUS) || Ki(e, s), console.error("PixiJS Error: Could not initialize shader."), e.getProgramInfoLog(t) !== "" && console.warn("PixiJS Warning: gl.getProgramInfoLog()", e.getProgramInfoLog(t)));
}
var Fh = {
  float: 1,
  vec2: 2,
  vec3: 3,
  vec4: 4,
  int: 1,
  ivec2: 2,
  ivec3: 3,
  ivec4: 4,
  uint: 1,
  uvec2: 2,
  uvec3: 3,
  uvec4: 4,
  bool: 1,
  bvec2: 2,
  bvec3: 3,
  bvec4: 4,
  mat2: 4,
  mat3: 9,
  mat4: 16,
  sampler2D: 1
};
function ta(e) {
  return Fh[e];
}
var Ir = null, Zi = {
  FLOAT: "float",
  FLOAT_VEC2: "vec2",
  FLOAT_VEC3: "vec3",
  FLOAT_VEC4: "vec4",
  INT: "int",
  INT_VEC2: "ivec2",
  INT_VEC3: "ivec3",
  INT_VEC4: "ivec4",
  UNSIGNED_INT: "uint",
  UNSIGNED_INT_VEC2: "uvec2",
  UNSIGNED_INT_VEC3: "uvec3",
  UNSIGNED_INT_VEC4: "uvec4",
  BOOL: "bool",
  BOOL_VEC2: "bvec2",
  BOOL_VEC3: "bvec3",
  BOOL_VEC4: "bvec4",
  FLOAT_MAT2: "mat2",
  FLOAT_MAT3: "mat3",
  FLOAT_MAT4: "mat4",
  SAMPLER_2D: "sampler2D",
  INT_SAMPLER_2D: "sampler2D",
  UNSIGNED_INT_SAMPLER_2D: "sampler2D",
  SAMPLER_CUBE: "samplerCube",
  INT_SAMPLER_CUBE: "samplerCube",
  UNSIGNED_INT_SAMPLER_CUBE: "samplerCube",
  SAMPLER_2D_ARRAY: "sampler2DArray",
  INT_SAMPLER_2D_ARRAY: "sampler2DArray",
  UNSIGNED_INT_SAMPLER_2D_ARRAY: "sampler2DArray"
};
function ea(e, t) {
  if (!Ir) {
    const r = Object.keys(Zi);
    Ir = {};
    for (let s = 0; s < r.length; ++s) {
      const i = r[s];
      Ir[e[i]] = Zi[i];
    }
  }
  return Ir[t];
}
function Ji(e, t, r) {
  if (e.substring(0, 9) !== "precision") {
    let s = t;
    return t === kt.HIGH && r !== kt.HIGH && (s = kt.MEDIUM), `precision ${s} float;
${e}`;
  } else if (r !== kt.HIGH && e.substring(0, 15) === "precision highp") return e.replace("precision highp", "precision mediump");
  return e;
}
var tr;
function Nh() {
  if (typeof tr == "boolean") return tr;
  try {
    tr = new Function("param1", "param2", "param3", "return param1[param2] === param3;")({ a: "b" }, "a", "b") === !0;
  } catch {
    tr = !1;
  }
  return tr;
}
var Oh = 0, Rr = {}, ks = class $e {
  constructor(t, r, s = "pixi-shader", i = {}) {
    this.extra = {}, this.id = Oh++, this.vertexSrc = t || $e.defaultVertexSrc, this.fragmentSrc = r || $e.defaultFragmentSrc, this.vertexSrc = this.vertexSrc.trim(), this.fragmentSrc = this.fragmentSrc.trim(), this.extra = i, this.vertexSrc.substring(0, 8) !== "#version" && (s = s.replace(/\s+/g, "-"), Rr[s] ? (Rr[s]++, s += `-${Rr[s]}`) : Rr[s] = 1, this.vertexSrc = `#define SHADER_NAME ${s}
${this.vertexSrc}`, this.fragmentSrc = `#define SHADER_NAME ${s}
${this.fragmentSrc}`, this.vertexSrc = Ji(this.vertexSrc, $e.defaultVertexPrecision, kt.HIGH), this.fragmentSrc = Ji(this.fragmentSrc, $e.defaultFragmentPrecision, Ph())), this.glPrograms = {}, this.syncUniforms = null;
  }
  static get defaultVertexSrc() {
    return Ah;
  }
  static get defaultFragmentSrc() {
    return Sh;
  }
  static from(t, r, s) {
    const i = t + r;
    let n = Hi[i];
    return n || (Hi[i] = n = new $e(t, r, s)), n;
  }
};
ks.defaultVertexPrecision = kt.HIGH, ks.defaultFragmentPrecision = He.apple.device ? kt.HIGH : kt.MEDIUM;
var Ee = ks, Bh = 0, Ye = class Gr {
  constructor(t, r, s) {
    this.group = !0, this.syncUniforms = {}, this.dirtyId = 0, this.id = Bh++, this.static = !!r, this.ubo = !!s, t instanceof Ut ? (this.buffer = t, this.buffer.type = Zt.UNIFORM_BUFFER, this.autoManage = !1, this.ubo = !0) : (this.uniforms = t, this.ubo && (this.buffer = new Ut(/* @__PURE__ */ new Float32Array(1)), this.buffer.type = Zt.UNIFORM_BUFFER, this.autoManage = !0));
  }
  update() {
    this.dirtyId++, !this.autoManage && this.buffer && this.buffer.update();
  }
  add(t, r, s) {
    if (!this.ubo) this.uniforms[t] = new Gr(r, s);
    else throw new Error("[UniformGroup] uniform groups in ubo mode cannot be modified, or have uniform groups nested in them");
  }
  static from(t, r, s) {
    return new Gr(t, r, s);
  }
  static uboFrom(t, r) {
    return new Gr(t, r ?? !0, !0);
  }
}, ra = class sa {
  constructor(t, r) {
    this.uniformBindCount = 0, this.program = t, r ? r instanceof Ye ? this.uniformGroup = r : this.uniformGroup = new Ye(r) : this.uniformGroup = new Ye({}), this.disposeRunner = new Wt("disposeShader");
  }
  checkUniformExists(t, r) {
    if (r.uniforms[t]) return !0;
    for (const s in r.uniforms) {
      const i = r.uniforms[s];
      if (i.group === !0 && this.checkUniformExists(t, i)) return !0;
    }
    return !1;
  }
  destroy() {
    this.uniformGroup = null, this.disposeRunner.emit(this), this.disposeRunner.destroy();
  }
  get uniforms() {
    return this.uniformGroup.uniforms;
  }
  static from(t, r, s) {
    const i = Ee.from(t, r);
    return new sa(i, s);
  }
}, Uh = class {
  constructor(e, t) {
    if (this.vertexSrc = e, this.fragTemplate = t, this.programCache = {}, this.defaultGroupCache = {}, !t.includes("%count%")) throw new Error('Fragment template must contain "%count%".');
    if (!t.includes("%forloop%")) throw new Error('Fragment template must contain "%forloop%".');
  }
  generateShader(e) {
    if (!this.programCache[e]) {
      const r = new Int32Array(e);
      for (let i = 0; i < e; i++) r[i] = i;
      this.defaultGroupCache[e] = Ye.from({ uSamplers: r }, !0);
      let s = this.fragTemplate;
      s = s.replace(/%count%/gi, `${e}`), s = s.replace(/%forloop%/gi, this.generateSampleSrc(e)), this.programCache[e] = new Ee(this.vertexSrc, s);
    }
    const t = {
      tint: new Float32Array([
        1,
        1,
        1,
        1
      ]),
      translationMatrix: new Lt(),
      default: this.defaultGroupCache[e]
    };
    return new ra(this.programCache[e], t);
  }
  generateSampleSrc(e) {
    let t = "";
    t += `
`, t += `
`;
    for (let r = 0; r < e; r++) r > 0 && (t += `
else `), r < e - 1 && (t += `if(vTextureId < ${r}.5)`), t += `
{`, t += `
	color = texture2D(uSamplers[${r}], vTextureCoord);`, t += `
}`;
    return t += `
`, t += `
`, t;
  }
}, kh = class {
  constructor() {
    this.elements = [], this.ids = [], this.count = 0;
  }
  clear() {
    for (let e = 0; e < this.count; e++) this.elements[e] = null;
    this.count = 0;
  }
};
function Dh() {
  return !He.apple.device;
}
function Gh(e) {
  let t = !0;
  const r = dt.ADAPTER.getNavigator();
  if (He.tablet || He.phone) {
    if (He.apple.device) {
      const s = r.userAgent.match(/OS (\d+)_(\d+)?/);
      s && parseInt(s[1], 10) < 11 && (t = !1);
    }
    if (He.android.device) {
      const s = r.userAgent.match(/Android\s([0-9.]*)/);
      s && parseInt(s[1], 10) < 7 && (t = !1);
    }
  }
  return t ? e : 4;
}
var ia = class {
  constructor(e) {
    this.renderer = e;
  }
  flush() {
  }
  destroy() {
    this.renderer = null;
  }
  start() {
  }
  stop() {
    this.flush();
  }
  render(e) {
  }
}, zh = `varying vec2 vTextureCoord;
varying vec4 vColor;
varying float vTextureId;
uniform sampler2D uSamplers[%count%];

void main(void){
    vec4 color;
    %forloop%
    gl_FragColor = color * vColor;
}
`, $h = `precision highp float;
attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;
attribute vec4 aColor;
attribute float aTextureId;

uniform mat3 projectionMatrix;
uniform mat3 translationMatrix;
uniform vec4 tint;

varying vec2 vTextureCoord;
varying vec4 vColor;
varying float vTextureId;

void main(void){
    gl_Position = vec4((projectionMatrix * translationMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);

    vTextureCoord = aTextureCoord;
    vTextureId = aTextureId;
    vColor = aColor * tint;
}
`, ir = class Ht extends ia {
  constructor(t) {
    super(t), this.setShaderGenerator(), this.geometryClass = yh, this.vertexSize = 6, this.state = ss.for2d(), this.size = Ht.defaultBatchSize * 4, this._vertexCount = 0, this._indexCount = 0, this._bufferedElements = [], this._bufferedTextures = [], this._bufferSize = 0, this._shader = null, this._packedGeometries = [], this._packedGeometryPoolSize = 2, this._flushId = 0, this._aBuffers = {}, this._iBuffers = {}, this.maxTextures = 1, this.renderer.on("prerender", this.onPrerender, this), t.runners.contextChange.add(this), this._dcIndex = 0, this._aIndex = 0, this._iIndex = 0, this._attributeBuffer = null, this._indexBuffer = null, this._tempBoundTextures = [];
  }
  static get defaultMaxTextures() {
    return this._defaultMaxTextures = this._defaultMaxTextures ?? Gh(32), this._defaultMaxTextures;
  }
  static set defaultMaxTextures(t) {
    this._defaultMaxTextures = t;
  }
  static get canUploadSameBuffer() {
    return this._canUploadSameBuffer = this._canUploadSameBuffer ?? Dh(), this._canUploadSameBuffer;
  }
  static set canUploadSameBuffer(t) {
    this._canUploadSameBuffer = t;
  }
  get MAX_TEXTURES() {
    return yt("7.1.0", "BatchRenderer#MAX_TEXTURES renamed to BatchRenderer#maxTextures"), this.maxTextures;
  }
  static get defaultVertexSrc() {
    return $h;
  }
  static get defaultFragmentTemplate() {
    return zh;
  }
  setShaderGenerator({ vertex: t = Ht.defaultVertexSrc, fragment: r = Ht.defaultFragmentTemplate } = {}) {
    this.shaderGenerator = new Uh(t, r);
  }
  contextChange() {
    const t = this.renderer.gl;
    dt.PREFER_ENV === Me.WEBGL_LEGACY ? this.maxTextures = 1 : (this.maxTextures = Math.min(t.getParameter(t.MAX_TEXTURE_IMAGE_UNITS), Ht.defaultMaxTextures), this.maxTextures = hh(this.maxTextures, t)), this._shader = this.shaderGenerator.generateShader(this.maxTextures);
    for (let r = 0; r < this._packedGeometryPoolSize; r++) this._packedGeometries[r] = new this.geometryClass();
    this.initFlushBuffers();
  }
  initFlushBuffers() {
    const { _drawCallPool: t, _textureArrayPool: r } = Ht, s = this.size / 4, i = Math.floor(s / this.maxTextures) + 1;
    for (; t.length < s; ) t.push(new ch());
    for (; r.length < i; ) r.push(new kh());
    for (let n = 0; n < this.maxTextures; n++) this._tempBoundTextures[n] = null;
  }
  onPrerender() {
    this._flushId = 0;
  }
  render(t) {
    t._texture.valid && (this._vertexCount + t.vertexData.length / 2 > this.size && this.flush(), this._vertexCount += t.vertexData.length / 2, this._indexCount += t.indices.length, this._bufferedTextures[this._bufferSize] = t._texture.baseTexture, this._bufferedElements[this._bufferSize++] = t);
  }
  buildTexturesAndDrawCalls() {
    const { _bufferedTextures: t, maxTextures: r } = this, s = Ht._textureArrayPool, i = this.renderer.batch, n = this._tempBoundTextures, a = this.renderer.textureGC.count;
    let o = ++vt._globalBatch, h = 0, l = s[0], c = 0;
    i.copyBoundTextures(n, r);
    for (let u = 0; u < this._bufferSize; ++u) {
      const d = t[u];
      t[u] = null, d._batchEnabled !== o && (l.count >= r && (i.boundArray(l, n, o, r), this.buildDrawCalls(l, c, u), c = u, l = s[++h], ++o), d._batchEnabled = o, d.touched = a, l.elements[l.count++] = d);
    }
    l.count > 0 && (i.boundArray(l, n, o, r), this.buildDrawCalls(l, c, this._bufferSize), ++h, ++o);
    for (let u = 0; u < n.length; u++) n[u] = null;
    vt._globalBatch = o;
  }
  buildDrawCalls(t, r, s) {
    const { _bufferedElements: i, _attributeBuffer: n, _indexBuffer: a, vertexSize: o } = this, h = Ht._drawCallPool;
    let l = this._dcIndex, c = this._aIndex, u = this._iIndex, d = h[l];
    d.start = this._iIndex, d.texArray = t;
    for (let y = r; y < s; ++y) {
      const m = i[y], v = m._texture.baseTexture, p = rh[v.alphaMode ? 1 : 0][m.blendMode];
      i[y] = null, r < y && d.blend !== p && (d.size = u - d.start, r = y, d = h[++l], d.texArray = t, d.start = u), this.packInterleavedGeometry(m, n, a, c, u), c += m.vertexData.length / 2 * o, u += m.indices.length, d.blend = p;
    }
    r < s && (d.size = u - d.start, ++l), this._dcIndex = l, this._aIndex = c, this._iIndex = u;
  }
  bindAndClearTexArray(t) {
    const r = this.renderer.texture;
    for (let s = 0; s < t.count; s++) r.bind(t.elements[s], t.ids[s]), t.elements[s] = null;
    t.count = 0;
  }
  updateGeometry() {
    const { _packedGeometries: t, _attributeBuffer: r, _indexBuffer: s } = this;
    Ht.canUploadSameBuffer ? (t[this._flushId]._buffer.update(r.rawBinaryData), t[this._flushId]._indexBuffer.update(s), this.renderer.geometry.updateBuffers()) : (this._packedGeometryPoolSize <= this._flushId && (this._packedGeometryPoolSize++, t[this._flushId] = new this.geometryClass()), t[this._flushId]._buffer.update(r.rawBinaryData), t[this._flushId]._indexBuffer.update(s), this.renderer.geometry.bind(t[this._flushId]), this.renderer.geometry.updateBuffers(), this._flushId++);
  }
  drawBatches() {
    const t = this._dcIndex, { gl: r, state: s } = this.renderer, i = Ht._drawCallPool;
    let n = null;
    for (let a = 0; a < t; a++) {
      const { texArray: o, type: h, size: l, start: c, blend: u } = i[a];
      n !== o && (n = o, this.bindAndClearTexArray(o)), this.state.blendMode = u, s.set(this.state), r.drawElements(h, l, r.UNSIGNED_SHORT, c * 2);
    }
  }
  flush() {
    this._vertexCount !== 0 && (this._attributeBuffer = this.getAttributeBuffer(this._vertexCount), this._indexBuffer = this.getIndexBuffer(this._indexCount), this._aIndex = 0, this._iIndex = 0, this._dcIndex = 0, this.buildTexturesAndDrawCalls(), this.updateGeometry(), this.drawBatches(), this._bufferSize = 0, this._vertexCount = 0, this._indexCount = 0);
  }
  start() {
    this.renderer.state.set(this.state), this.renderer.texture.ensureSamplerType(this.maxTextures), this.renderer.shader.bind(this._shader), Ht.canUploadSameBuffer && this.renderer.geometry.bind(this._packedGeometries[this._flushId]);
  }
  stop() {
    this.flush();
  }
  destroy() {
    for (let t = 0; t < this._packedGeometryPoolSize; t++) this._packedGeometries[t] && this._packedGeometries[t].destroy();
    this.renderer.off("prerender", this.onPrerender, this), this._aBuffers = null, this._iBuffers = null, this._packedGeometries = null, this._attributeBuffer = null, this._indexBuffer = null, this._shader && (this._shader.destroy(), this._shader = null), super.destroy();
  }
  getAttributeBuffer(t) {
    const r = qr(Math.ceil(t / 8)), s = $i(r), i = r * 8;
    this._aBuffers.length <= s && (this._iBuffers.length = s + 1);
    let n = this._aBuffers[i];
    return n || (this._aBuffers[i] = n = new nh(i * this.vertexSize * 4)), n;
  }
  getIndexBuffer(t) {
    const r = qr(Math.ceil(t / 12)), s = $i(r), i = r * 12;
    this._iBuffers.length <= s && (this._iBuffers.length = s + 1);
    let n = this._iBuffers[s];
    return n || (this._iBuffers[s] = n = new Uint16Array(i)), n;
  }
  packInterleavedGeometry(t, r, s, i, n) {
    const { uint32View: a, float32View: o } = r, h = i / this.vertexSize, l = t.uvs, c = t.indices, u = t.vertexData, d = t._texture.baseTexture._batchLocation, y = Math.min(t.worldAlpha, 1), m = Ae.shared.setValue(t._tintRGB).toPremultiplied(y, t._texture.baseTexture.alphaMode > 0);
    for (let v = 0; v < u.length; v += 2) o[i++] = u[v], o[i++] = u[v + 1], o[i++] = l[v], o[i++] = l[v + 1], a[i++] = m, o[i++] = d;
    for (let v = 0; v < c.length; v++) s[n++] = h + c[v];
  }
};
ir.defaultBatchSize = 4096, ir.extension = {
  name: "batch",
  type: ut.RendererPlugin
}, ir._drawCallPool = [], ir._textureArrayPool = [];
var _e = ir;
mt.add(_e);
var Vh = `varying vec2 vTextureCoord;

uniform sampler2D uSampler;

void main(void){
   gl_FragColor = texture2D(uSampler, vTextureCoord);
}
`, Hh = `attribute vec2 aVertexPosition;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

uniform vec4 inputSize;
uniform vec4 outputFrame;

vec4 filterVertexPosition( void )
{
    vec2 position = aVertexPosition * max(outputFrame.zw, vec2(0.)) + outputFrame.xy;

    return vec4((projectionMatrix * vec3(position, 1.0)).xy, 0.0, 1.0);
}

vec2 filterTextureCoord( void )
{
    return aVertexPosition * (outputFrame.zw * inputSize.zw);
}

void main(void)
{
    gl_Position = filterVertexPosition();
    vTextureCoord = filterTextureCoord();
}
`, Ds = class nr extends ra {
  constructor(t, r, s) {
    const i = Ee.from(t || nr.defaultVertexSrc, r || nr.defaultFragmentSrc);
    super(i, s), this.padding = 0, this.resolution = nr.defaultResolution, this.multisample = nr.defaultMultisample, this.enabled = !0, this.autoFit = !0, this.state = new ss();
  }
  apply(t, r, s, i, n) {
    t.applyFilter(this, r, s, i);
  }
  get blendMode() {
    return this.state.blendMode;
  }
  set blendMode(t) {
    this.state.blendMode = t;
  }
  get resolution() {
    return this._resolution;
  }
  set resolution(t) {
    this._resolution = t;
  }
  static get defaultVertexSrc() {
    return Hh;
  }
  static get defaultFragmentSrc() {
    return Vh;
  }
};
Ds.defaultResolution = 1, Ds.defaultMultisample = Mt.NONE;
var Gt = Ds, jr = class {
  constructor() {
    this.clearBeforeRender = !0, this._backgroundColor = new Ae(0), this.alpha = 1;
  }
  init(e) {
    this.clearBeforeRender = e.clearBeforeRender;
    const { backgroundColor: t, background: r, backgroundAlpha: s } = e, i = r ?? t;
    i !== void 0 && (this.color = i), this.alpha = s;
  }
  get color() {
    return this._backgroundColor.value;
  }
  set color(e) {
    this._backgroundColor.setValue(e);
  }
  get alpha() {
    return this._backgroundColor.alpha;
  }
  set alpha(e) {
    this._backgroundColor.setAlpha(e);
  }
  get backgroundColor() {
    return this._backgroundColor;
  }
  destroy() {
  }
};
jr.defaultOptions = {
  backgroundAlpha: 1,
  backgroundColor: 0,
  clearBeforeRender: !0
}, jr.extension = {
  type: [ut.RendererSystem, ut.CanvasRendererSystem],
  name: "background"
};
mt.add(jr);
var na = class {
  constructor(e) {
    this.renderer = e, this.emptyRenderer = new ia(e), this.currentRenderer = this.emptyRenderer;
  }
  setObjectRenderer(e) {
    this.currentRenderer !== e && (this.currentRenderer.stop(), this.currentRenderer = e, this.currentRenderer.start());
  }
  flush() {
    this.setObjectRenderer(this.emptyRenderer);
  }
  reset() {
    this.setObjectRenderer(this.emptyRenderer);
  }
  copyBoundTextures(e, t) {
    const { boundTextures: r } = this.renderer.texture;
    for (let s = t - 1; s >= 0; --s) e[s] = r[s] || null, e[s] && (e[s]._batchLocation = s);
  }
  boundArray(e, t, r, s) {
    const { elements: i, ids: n, count: a } = e;
    let o = 0;
    for (let h = 0; h < a; h++) {
      const l = i[h], c = l._batchLocation;
      if (c >= 0 && c < s && t[c] === l) {
        n[h] = c;
        continue;
      }
      for (; o < s; ) {
        const u = t[o];
        if (u && u._batchEnabled === r && u._batchLocation === o) {
          o++;
          continue;
        }
        n[h] = o, l._batchLocation = o, t[o] = l;
        break;
      }
    }
  }
  destroy() {
    this.renderer = null;
  }
};
na.extension = {
  type: ut.RendererSystem,
  name: "batch"
};
mt.add(na);
var Qi = 0, Yr = class {
  constructor(e) {
    this.renderer = e, this.webGLVersion = 1, this.extensions = {}, this.supports = { uint32Indices: !1 }, this.handleContextLost = this.handleContextLost.bind(this), this.handleContextRestored = this.handleContextRestored.bind(this);
  }
  get isLost() {
    return !this.gl || this.gl.isContextLost();
  }
  contextChange(e) {
    this.gl = e, this.renderer.gl = e, this.renderer.CONTEXT_UID = Qi++;
  }
  init(e) {
    if (e.context) this.initFromContext(e.context);
    else {
      const t = this.renderer.background.alpha < 1, r = e.premultipliedAlpha;
      this.preserveDrawingBuffer = e.preserveDrawingBuffer, this.useContextAlpha = e.useContextAlpha, this.powerPreference = e.powerPreference, this.initFromOptions({
        alpha: t,
        premultipliedAlpha: r,
        antialias: e.antialias,
        stencil: !0,
        preserveDrawingBuffer: e.preserveDrawingBuffer,
        powerPreference: e.powerPreference
      });
    }
  }
  initFromContext(e) {
    this.gl = e, this.validateContext(e), this.renderer.gl = e, this.renderer.CONTEXT_UID = Qi++, this.renderer.runners.contextChange.emit(e);
    const t = this.renderer.view;
    t.addEventListener !== void 0 && (t.addEventListener("webglcontextlost", this.handleContextLost, !1), t.addEventListener("webglcontextrestored", this.handleContextRestored, !1));
  }
  initFromOptions(e) {
    const t = this.createContext(this.renderer.view, e);
    this.initFromContext(t);
  }
  createContext(e, t) {
    let r;
    if (dt.PREFER_ENV >= Me.WEBGL2 && (r = e.getContext("webgl2", t)), r) this.webGLVersion = 2;
    else if (this.webGLVersion = 1, r = e.getContext("webgl", t) || e.getContext("experimental-webgl", t), !r) throw new Error("This browser does not support WebGL. Try using the canvas renderer");
    return this.gl = r, this.getExtensions(), this.gl;
  }
  getExtensions() {
    const { gl: e } = this, t = {
      loseContext: e.getExtension("WEBGL_lose_context"),
      anisotropicFiltering: e.getExtension("EXT_texture_filter_anisotropic"),
      floatTextureLinear: e.getExtension("OES_texture_float_linear"),
      s3tc: e.getExtension("WEBGL_compressed_texture_s3tc"),
      s3tc_sRGB: e.getExtension("WEBGL_compressed_texture_s3tc_srgb"),
      etc: e.getExtension("WEBGL_compressed_texture_etc"),
      etc1: e.getExtension("WEBGL_compressed_texture_etc1"),
      pvrtc: e.getExtension("WEBGL_compressed_texture_pvrtc") || e.getExtension("WEBKIT_WEBGL_compressed_texture_pvrtc"),
      atc: e.getExtension("WEBGL_compressed_texture_atc"),
      astc: e.getExtension("WEBGL_compressed_texture_astc"),
      bptc: e.getExtension("EXT_texture_compression_bptc")
    };
    this.webGLVersion === 1 ? Object.assign(this.extensions, t, {
      drawBuffers: e.getExtension("WEBGL_draw_buffers"),
      depthTexture: e.getExtension("WEBGL_depth_texture"),
      vertexArrayObject: e.getExtension("OES_vertex_array_object") || e.getExtension("MOZ_OES_vertex_array_object") || e.getExtension("WEBKIT_OES_vertex_array_object"),
      uint32ElementIndex: e.getExtension("OES_element_index_uint"),
      floatTexture: e.getExtension("OES_texture_float"),
      floatTextureLinear: e.getExtension("OES_texture_float_linear"),
      textureHalfFloat: e.getExtension("OES_texture_half_float"),
      textureHalfFloatLinear: e.getExtension("OES_texture_half_float_linear")
    }) : this.webGLVersion === 2 && Object.assign(this.extensions, t, { colorBufferFloat: e.getExtension("EXT_color_buffer_float") });
  }
  handleContextLost(e) {
    e.preventDefault(), setTimeout(() => {
      this.gl.isContextLost() && this.extensions.loseContext && this.extensions.loseContext.restoreContext();
    }, 0);
  }
  handleContextRestored() {
    this.renderer.runners.contextChange.emit(this.gl);
  }
  destroy() {
    const e = this.renderer.view;
    this.renderer = null, e.removeEventListener !== void 0 && (e.removeEventListener("webglcontextlost", this.handleContextLost), e.removeEventListener("webglcontextrestored", this.handleContextRestored)), this.gl.useProgram(null), this.extensions.loseContext && this.extensions.loseContext.loseContext();
  }
  postrender() {
    this.renderer.objectRenderer.renderingToScreen && this.gl.flush();
  }
  validateContext(e) {
    const t = e.getContextAttributes(), r = "WebGL2RenderingContext" in globalThis && e instanceof globalThis.WebGL2RenderingContext;
    r && (this.webGLVersion = 2), t && !t.stencil && console.warn("Provided WebGL context does not have a stencil buffer, masks may not render correctly");
    const s = r || !!e.getExtension("OES_element_index_uint");
    this.supports.uint32Indices = s, s || console.warn("Provided WebGL context does not support 32 index buffer, complex graphics may not render correctly");
  }
};
Yr.defaultOptions = {
  context: null,
  antialias: !1,
  premultipliedAlpha: !0,
  preserveDrawingBuffer: !1,
  powerPreference: "default"
}, Yr.extension = {
  type: ut.RendererSystem,
  name: "context"
};
mt.add(Yr);
var Gs = class {
  constructor(e, t) {
    if (this.width = Math.round(e), this.height = Math.round(t), !this.width || !this.height) throw new Error("Framebuffer width or height is zero");
    this.stencil = !1, this.depth = !1, this.dirtyId = 0, this.dirtyFormat = 0, this.dirtySize = 0, this.depthTexture = null, this.colorTextures = [], this.glFramebuffers = {}, this.disposeRunner = new Wt("disposeFramebuffer"), this.multisample = Mt.NONE;
  }
  get colorTexture() {
    return this.colorTextures[0];
  }
  addColorTexture(e = 0, t) {
    return this.colorTextures[e] = t || new vt(null, {
      scaleMode: te.NEAREST,
      resolution: 1,
      mipmap: Se.OFF,
      width: this.width,
      height: this.height
    }), this.dirtyId++, this.dirtyFormat++, this;
  }
  addDepthTexture(e) {
    return this.depthTexture = e || new vt(null, {
      scaleMode: te.NEAREST,
      resolution: 1,
      width: this.width,
      height: this.height,
      mipmap: Se.OFF,
      format: X.DEPTH_COMPONENT,
      type: ct.UNSIGNED_SHORT
    }), this.dirtyId++, this.dirtyFormat++, this;
  }
  enableDepth() {
    return this.depth = !0, this.dirtyId++, this.dirtyFormat++, this;
  }
  enableStencil() {
    return this.stencil = !0, this.dirtyId++, this.dirtyFormat++, this;
  }
  resize(e, t) {
    if (e = Math.round(e), t = Math.round(t), !e || !t) throw new Error("Framebuffer width and height must not be zero");
    if (!(e === this.width && t === this.height)) {
      this.width = e, this.height = t, this.dirtyId++, this.dirtySize++;
      for (let r = 0; r < this.colorTextures.length; r++) {
        const s = this.colorTextures[r], i = s.resolution;
        s.setSize(e / i, t / i);
      }
      if (this.depthTexture) {
        const r = this.depthTexture.resolution;
        this.depthTexture.setSize(e / r, t / r);
      }
    }
  }
  dispose() {
    this.disposeRunner.emit(this, !1);
  }
  destroyDepthTexture() {
    this.depthTexture && (this.depthTexture.destroy(), this.depthTexture = null, ++this.dirtyId, ++this.dirtyFormat);
  }
}, aa = class extends vt {
  constructor(e = {}) {
    typeof e == "number" && (e = {
      width: arguments[0],
      height: arguments[1],
      scaleMode: arguments[2],
      resolution: arguments[3]
    }), e.width = e.width ?? 100, e.height = e.height ?? 100, e.multisample ?? (e.multisample = Mt.NONE), super(null, e), this.mipmap = Se.OFF, this.valid = !0, this._clear = new Ae([
      0,
      0,
      0,
      0
    ]), this.framebuffer = new Gs(this.realWidth, this.realHeight).addColorTexture(0, this), this.framebuffer.multisample = e.multisample, this.maskStack = [], this.filterStack = [{}];
  }
  set clearColor(e) {
    this._clear.setValue(e);
  }
  get clearColor() {
    return this._clear.value;
  }
  get clear() {
    return this._clear;
  }
  get multisample() {
    return this.framebuffer.multisample;
  }
  set multisample(e) {
    this.framebuffer.multisample = e;
  }
  resize(e, t) {
    this.framebuffer.resize(e * this.resolution, t * this.resolution), this.setRealSize(this.framebuffer.width, this.framebuffer.height);
  }
  dispose() {
    this.framebuffer.dispose(), super.dispose();
  }
  destroy() {
    super.destroy(), this.framebuffer.destroyDepthTexture(), this.framebuffer = null;
  }
}, re = class extends pr {
  constructor(e) {
    const t = e, r = t.naturalWidth || t.videoWidth || t.displayWidth || t.width, s = t.naturalHeight || t.videoHeight || t.displayHeight || t.height;
    super(r, s), this.source = e, this.noSubImage = !1;
  }
  static crossOrigin(e, t, r) {
    r === void 0 && !t.startsWith("data:") ? e.crossOrigin = ih(t) : r !== !1 && (e.crossOrigin = typeof r == "string" ? r : "anonymous");
  }
  upload(e, t, r, s) {
    const i = e.gl, n = t.realWidth, a = t.realHeight;
    if (s = s || this.source, typeof HTMLImageElement < "u" && s instanceof HTMLImageElement) {
      if (!s.complete || s.naturalWidth === 0) return !1;
    } else if (typeof HTMLVideoElement < "u" && s instanceof HTMLVideoElement && s.readyState <= 1) return !1;
    return i.pixelStorei(i.UNPACK_PREMULTIPLY_ALPHA_WEBGL, t.alphaMode === Ce.UNPACK), !this.noSubImage && t.target === i.TEXTURE_2D && r.width === n && r.height === a ? i.texSubImage2D(i.TEXTURE_2D, 0, 0, 0, t.format, r.type, s) : (r.width = n, r.height = a, i.texImage2D(t.target, 0, r.internalFormat, t.format, r.type, s)), !0;
  }
  update() {
    if (this.destroyed) return;
    const e = this.source, t = e.naturalWidth || e.videoWidth || e.width, r = e.naturalHeight || e.videoHeight || e.height;
    this.resize(t, r), super.update();
  }
  dispose() {
    this.source = null;
  }
}, oa = class extends re {
  constructor(e, t) {
    if (t = t || {}, typeof e == "string") {
      const r = new Image();
      re.crossOrigin(r, e, t.crossorigin), r.src = e, e = r;
    }
    super(e), !e.complete && this._width && this._height && (this._width = 0, this._height = 0), this.url = e.src, this._process = null, this.preserveBitmap = !1, this.createBitmap = (t.createBitmap ?? dt.CREATE_IMAGE_BITMAP) && !!globalThis.createImageBitmap, this.alphaMode = typeof t.alphaMode == "number" ? t.alphaMode : null, this.bitmap = null, this._load = null, t.autoLoad !== !1 && this.load();
  }
  load(e) {
    return this._load ? this._load : (e !== void 0 && (this.createBitmap = e), this._load = new Promise((t, r) => {
      const s = this.source;
      this.url = s.src;
      const i = () => {
        this.destroyed || (s.onload = null, s.onerror = null, this.update(), this._load = null, this.createBitmap ? t(this.process()) : t(this));
      };
      s.complete && s.src ? i() : (s.onload = i, s.onerror = (n) => {
        r(n), this.onError.emit(n);
      });
    }), this._load);
  }
  process() {
    const e = this.source;
    if (this._process !== null) return this._process;
    if (this.bitmap !== null || !globalThis.createImageBitmap) return Promise.resolve(this);
    const t = globalThis.createImageBitmap, r = !e.crossOrigin || e.crossOrigin === "anonymous";
    return this._process = fetch(e.src, { mode: r ? "cors" : "no-cors" }).then((s) => s.blob()).then((s) => t(s, 0, 0, e.width, e.height, { premultiplyAlpha: this.alphaMode === null || this.alphaMode === Ce.UNPACK ? "premultiply" : "none" })).then((s) => this.destroyed ? Promise.reject() : (this.bitmap = s, this.update(), this._process = null, Promise.resolve(this))), this._process;
  }
  upload(e, t, r) {
    if (typeof this.alphaMode == "number" && (t.alphaMode = this.alphaMode), !this.createBitmap) return super.upload(e, t, r);
    if (!this.bitmap && (this.process(), !this.bitmap)) return !1;
    if (super.upload(e, t, r, this.bitmap), !this.preserveBitmap) {
      let s = !0;
      const i = t._glTextures;
      for (const n in i) {
        const a = i[n];
        if (a !== r && a.dirtyId !== t.dirtyId) {
          s = !1;
          break;
        }
      }
      s && (this.bitmap.close && this.bitmap.close(), this.bitmap = null);
    }
    return !0;
  }
  dispose() {
    this.source.onload = null, this.source.onerror = null, super.dispose(), this.bitmap && (this.bitmap.close(), this.bitmap = null), this._process = null, this._load = null;
  }
  static test(e) {
    return typeof HTMLImageElement < "u" && (typeof e == "string" || e instanceof HTMLImageElement);
  }
}, ni = class {
  constructor() {
    this.x0 = 0, this.y0 = 0, this.x1 = 1, this.y1 = 0, this.x2 = 1, this.y2 = 1, this.x3 = 0, this.y3 = 1, this.uvsFloat32 = /* @__PURE__ */ new Float32Array(8);
  }
  set(e, t, r) {
    const s = t.width, i = t.height;
    if (r) {
      const n = e.width / 2 / s, a = e.height / 2 / i, o = e.x / s + n, h = e.y / i + a;
      r = At.add(r, At.NW), this.x0 = o + n * At.uX(r), this.y0 = h + a * At.uY(r), r = At.add(r, 2), this.x1 = o + n * At.uX(r), this.y1 = h + a * At.uY(r), r = At.add(r, 2), this.x2 = o + n * At.uX(r), this.y2 = h + a * At.uY(r), r = At.add(r, 2), this.x3 = o + n * At.uX(r), this.y3 = h + a * At.uY(r);
    } else this.x0 = e.x / s, this.y0 = e.y / i, this.x1 = (e.x + e.width) / s, this.y1 = e.y / i, this.x2 = (e.x + e.width) / s, this.y2 = (e.y + e.height) / i, this.x3 = e.x / s, this.y3 = (e.y + e.height) / i;
    this.uvsFloat32[0] = this.x0, this.uvsFloat32[1] = this.y0, this.uvsFloat32[2] = this.x1, this.uvsFloat32[3] = this.y1, this.uvsFloat32[4] = this.x2, this.uvsFloat32[5] = this.y2, this.uvsFloat32[6] = this.x3, this.uvsFloat32[7] = this.y3;
  }
};
ni.prototype.toString = function() {
  return `[@pixi/core:TextureUvs x0=${this.x0} y0=${this.y0} x1=${this.x1} y1=${this.y1} x2=${this.x2} y2=${this.y2} x3=${this.x3} y3=${this.y3}]`;
};
var tn = new ni();
function Mr(e) {
  e.destroy = function() {
  }, e.on = function() {
  }, e.once = function() {
  }, e.emit = function() {
  };
}
var We = class wt extends rs.default {
  constructor(t, r, s, i, n, a, o) {
    if (super(), this.noFrame = !1, r || (this.noFrame = !0, r = new Et(0, 0, 1, 1)), t instanceof wt && (t = t.baseTexture), this.baseTexture = t, this._frame = r, this.trim = i, this.valid = !1, this.destroyed = !1, this._uvs = tn, this.uvMatrix = null, this.orig = s || r, this._rotate = Number(n || 0), n === !0) this._rotate = 2;
    else if (this._rotate % 2 !== 0) throw new Error("attempt to use diamond-shaped UVs. If you are sure, set rotation manually");
    this.defaultAnchor = a ? new Ft(a.x, a.y) : new Ft(0, 0), this.defaultBorders = o, this._updateID = 0, this.textureCacheIds = [], t.valid ? this.noFrame ? t.valid && this.onBaseTextureUpdated(t) : this.frame = r : t.once("loaded", this.onBaseTextureUpdated, this), this.noFrame && t.on("update", this.onBaseTextureUpdated, this);
  }
  update() {
    this.baseTexture.resource && this.baseTexture.resource.update();
  }
  onBaseTextureUpdated(t) {
    if (this.noFrame) {
      if (!this.baseTexture.valid) return;
      this._frame.width = t.width, this._frame.height = t.height, this.valid = !0, this.updateUvs();
    } else this.frame = this._frame;
    this.emit("update", this);
  }
  destroy(t) {
    if (this.baseTexture) {
      if (t) {
        const { resource: r } = this.baseTexture;
        r?.url && Yt[r.url] && wt.removeFromCache(r.url), this.baseTexture.destroy();
      }
      this.baseTexture.off("loaded", this.onBaseTextureUpdated, this), this.baseTexture.off("update", this.onBaseTextureUpdated, this), this.baseTexture = null;
    }
    this._frame = null, this._uvs = null, this.trim = null, this.orig = null, this.valid = !1, wt.removeFromCache(this), this.textureCacheIds = null, this.destroyed = !0, this.emit("destroyed", this), this.removeAllListeners();
  }
  clone() {
    const t = this._frame.clone(), r = this._frame === this.orig ? t : this.orig.clone(), s = new wt(this.baseTexture, !this.noFrame && t, r, this.trim?.clone(), this.rotate, this.defaultAnchor, this.defaultBorders);
    return this.noFrame && (s._frame = t), s;
  }
  updateUvs() {
    this._uvs === tn && (this._uvs = new ni()), this._uvs.set(this._frame, this.baseTexture, this.rotate), this._updateID++;
  }
  static from(t, r = {}, s = dt.STRICT_TEXTURE_CACHE) {
    const i = typeof t == "string";
    let n = null;
    i ? n = t : t instanceof vt ? (t.cacheId || (t.cacheId = `${r?.pixiIdPrefix || "pixiid"}-${fr()}`, vt.addToCache(t, t.cacheId)), n = t.cacheId) : (t._pixiId || (t._pixiId = `${r?.pixiIdPrefix || "pixiid"}_${fr()}`), n = t._pixiId);
    let a = Yt[n];
    if (i && s && !a) throw new Error(`The cacheId "${n}" does not exist in TextureCache.`);
    return !a && !(t instanceof vt) ? (r.resolution || (r.resolution = Xi(t)), a = new wt(new vt(t, r)), a.baseTexture.cacheId = n, vt.addToCache(a.baseTexture, n), wt.addToCache(a, n)) : !a && t instanceof vt && (a = new wt(t), wt.addToCache(a, n)), a;
  }
  static fromURL(t, r) {
    const s = Object.assign({ autoLoad: !1 }, r?.resourceOptions), i = wt.from(t, Object.assign({ resourceOptions: s }, r), !1), n = i.baseTexture.resource;
    return i.baseTexture.valid ? Promise.resolve(i) : n.load().then(() => Promise.resolve(i));
  }
  static fromBuffer(t, r, s, i) {
    return new wt(vt.fromBuffer(t, r, s, i));
  }
  static fromLoader(t, r, s, i) {
    const n = new vt(t, Object.assign({
      scaleMode: vt.defaultOptions.scaleMode,
      resolution: Xi(r)
    }, i)), { resource: a } = n;
    a instanceof oa && (a.url = r);
    const o = new wt(n);
    return s || (s = r), vt.addToCache(o.baseTexture, s), wt.addToCache(o, s), s !== r && (vt.addToCache(o.baseTexture, r), wt.addToCache(o, r)), o.baseTexture.valid ? Promise.resolve(o) : new Promise((h) => {
      o.baseTexture.once("loaded", () => h(o));
    });
  }
  static addToCache(t, r) {
    r && (t.textureCacheIds.includes(r) || t.textureCacheIds.push(r), Yt[r] && Yt[r] !== t && console.warn(`Texture added to the cache with an id [${r}] that already had an entry`), Yt[r] = t);
  }
  static removeFromCache(t) {
    if (typeof t == "string") {
      const r = Yt[t];
      if (r) {
        const s = r.textureCacheIds.indexOf(t);
        return s > -1 && r.textureCacheIds.splice(s, 1), delete Yt[t], r;
      }
    } else if (t?.textureCacheIds) {
      for (let r = 0; r < t.textureCacheIds.length; ++r) Yt[t.textureCacheIds[r]] === t && delete Yt[t.textureCacheIds[r]];
      return t.textureCacheIds.length = 0, t;
    }
    return null;
  }
  get resolution() {
    return this.baseTexture.resolution;
  }
  get frame() {
    return this._frame;
  }
  set frame(t) {
    this._frame = t, this.noFrame = !1;
    const { x: r, y: s, width: i, height: n } = t, a = r + i > this.baseTexture.width, o = s + n > this.baseTexture.height;
    if (a || o) {
      const h = a && o ? "and" : "or", l = `X: ${r} + ${i} = ${r + i} > ${this.baseTexture.width}`, c = `Y: ${s} + ${n} = ${s + n} > ${this.baseTexture.height}`;
      throw new Error(`Texture Error: frame does not fit inside the base Texture dimensions: ${l} ${h} ${c}`);
    }
    this.valid = i && n && this.baseTexture.valid, !this.trim && !this.rotate && (this.orig = t), this.valid && this.updateUvs();
  }
  get rotate() {
    return this._rotate;
  }
  set rotate(t) {
    this._rotate = t, this.valid && this.updateUvs();
  }
  get width() {
    return this.orig.width;
  }
  get height() {
    return this.orig.height;
  }
  castToBaseTexture() {
    return this.baseTexture;
  }
  static get EMPTY() {
    return wt._EMPTY || (wt._EMPTY = new wt(new vt()), Mr(wt._EMPTY), Mr(wt._EMPTY.baseTexture)), wt._EMPTY;
  }
  static get WHITE() {
    if (!wt._WHITE) {
      const t = dt.ADAPTER.createCanvas(16, 16), r = t.getContext("2d");
      t.width = 16, t.height = 16, r.fillStyle = "white", r.fillRect(0, 0, 16, 16), wt._WHITE = new wt(vt.from(t)), Mr(wt._WHITE), Mr(wt._WHITE.baseTexture);
    }
    return wt._WHITE;
  }
}, ha = class la extends We {
  constructor(t, r) {
    super(t, r), this.valid = !0, this.filterFrame = null, this.filterPoolKey = null, this.updateUvs();
  }
  get framebuffer() {
    return this.baseTexture.framebuffer;
  }
  get multisample() {
    return this.framebuffer.multisample;
  }
  set multisample(t) {
    this.framebuffer.multisample = t;
  }
  resize(t, r, s = !0) {
    const i = this.baseTexture.resolution, n = Math.round(t * i) / i, a = Math.round(r * i) / i;
    this.valid = n > 0 && a > 0, this._frame.width = this.orig.width = n, this._frame.height = this.orig.height = a, s && this.baseTexture.resize(n, a), this.updateUvs();
  }
  setResolution(t) {
    const { baseTexture: r } = this;
    r.resolution !== t && (r.setResolution(t), this.resize(r.width, r.height, !1));
  }
  static create(t) {
    return new la(new aa(t));
  }
}, ca = class {
  constructor(e) {
    this.texturePool = {}, this.textureOptions = e || {}, this.enableFullScreen = !1, this._pixelsWidth = 0, this._pixelsHeight = 0;
  }
  createTexture(e, t, r = Mt.NONE) {
    const s = new aa(Object.assign({
      width: e,
      height: t,
      resolution: 1,
      multisample: r
    }, this.textureOptions));
    return new ha(s);
  }
  getOptimalTexture(e, t, r = 1, s = Mt.NONE) {
    let i;
    e = Math.max(Math.ceil(e * r - 1e-6), 1), t = Math.max(Math.ceil(t * r - 1e-6), 1), !this.enableFullScreen || e !== this._pixelsWidth || t !== this._pixelsHeight ? (e = qr(e), t = qr(t), i = ((e & 65535) << 16 | t & 65535) >>> 0, s > 1 && (i += s * 4294967296)) : i = s > 1 ? -s : -1, this.texturePool[i] || (this.texturePool[i] = []);
    let n = this.texturePool[i].pop();
    return n || (n = this.createTexture(e, t, s)), n.filterPoolKey = i, n.setResolution(r), n;
  }
  getFilterTexture(e, t, r) {
    const s = this.getOptimalTexture(e.width, e.height, t || e.resolution, r || Mt.NONE);
    return s.filterFrame = e.filterFrame, s;
  }
  returnTexture(e) {
    const t = e.filterPoolKey;
    e.filterFrame = null, this.texturePool[t].push(e);
  }
  returnFilterTexture(e) {
    this.returnTexture(e);
  }
  clear(e) {
    if (e = e !== !1, e) for (const t in this.texturePool) {
      const r = this.texturePool[t];
      if (r) for (let s = 0; s < r.length; s++) r[s].destroy(!0);
    }
    this.texturePool = {};
  }
  setScreenSize(e) {
    if (!(e.width === this._pixelsWidth && e.height === this._pixelsHeight)) {
      this.enableFullScreen = e.width > 0 && e.height > 0;
      for (const t in this.texturePool) {
        if (!(Number(t) < 0)) continue;
        const r = this.texturePool[t];
        if (r) for (let s = 0; s < r.length; s++) r[s].destroy(!0);
        this.texturePool[t] = [];
      }
      this._pixelsWidth = e.width, this._pixelsHeight = e.height;
    }
  }
};
ca.SCREEN_KEY = -1;
var Xh = class extends si {
  constructor() {
    super(), this.addAttribute("aVertexPosition", new Float32Array([
      0,
      0,
      1,
      0,
      1,
      1,
      0,
      1
    ])).addIndex([
      0,
      1,
      3,
      2
    ]);
  }
}, Wh = class extends si {
  constructor() {
    super(), this.vertices = new Float32Array([
      -1,
      -1,
      1,
      -1,
      1,
      1,
      -1,
      1
    ]), this.uvs = new Float32Array([
      0,
      0,
      1,
      0,
      1,
      1,
      0,
      1
    ]), this.vertexBuffer = new Ut(this.vertices), this.uvBuffer = new Ut(this.uvs), this.addAttribute("aVertexPosition", this.vertexBuffer).addAttribute("aTextureCoord", this.uvBuffer).addIndex([
      0,
      1,
      2,
      0,
      2,
      3
    ]);
  }
  map(e, t) {
    let r = 0, s = 0;
    return this.uvs[0] = r, this.uvs[1] = s, this.uvs[2] = r + t.width / e.width, this.uvs[3] = s, this.uvs[4] = r + t.width / e.width, this.uvs[5] = s + t.height / e.height, this.uvs[6] = r, this.uvs[7] = s + t.height / e.height, r = t.x, s = t.y, this.vertices[0] = r, this.vertices[1] = s, this.vertices[2] = r + t.width, this.vertices[3] = s, this.vertices[4] = r + t.width, this.vertices[5] = s + t.height, this.vertices[6] = r, this.vertices[7] = s + t.height, this.invalidate(), this;
  }
  invalidate() {
    return this.vertexBuffer._updateID++, this.uvBuffer._updateID++, this;
  }
}, qh = class {
  constructor() {
    this.renderTexture = null, this.target = null, this.legacy = !1, this.resolution = 1, this.multisample = Mt.NONE, this.sourceFrame = new Et(), this.destinationFrame = new Et(), this.bindingSourceFrame = new Et(), this.bindingDestinationFrame = new Et(), this.filters = [], this.transform = null;
  }
  clear() {
    this.target = null, this.filters = null, this.renderTexture = null;
  }
}, Cr = [
  new Ft(),
  new Ft(),
  new Ft(),
  new Ft()
], _s = new Lt(), ua = class {
  constructor(e) {
    this.renderer = e, this.defaultFilterStack = [{}], this.texturePool = new ca(), this.statePool = [], this.quad = new Xh(), this.quadUv = new Wh(), this.tempRect = new Et(), this.activeState = {}, this.globalUniforms = new Ye({
      outputFrame: new Et(),
      inputSize: /* @__PURE__ */ new Float32Array(4),
      inputPixel: /* @__PURE__ */ new Float32Array(4),
      inputClamp: /* @__PURE__ */ new Float32Array(4),
      resolution: 1,
      filterArea: /* @__PURE__ */ new Float32Array(4),
      filterClamp: /* @__PURE__ */ new Float32Array(4)
    }, !0), this.forceClear = !1, this.useMaxPadding = !1;
  }
  init() {
    this.texturePool.setScreenSize(this.renderer.view);
  }
  push(e, t) {
    const r = this.renderer, s = this.defaultFilterStack, i = this.statePool.pop() || new qh(), n = r.renderTexture;
    let a, o;
    if (n.current) {
      const v = n.current;
      a = v.resolution, o = v.multisample;
    } else a = r.resolution, o = r.multisample;
    let h = t[0].resolution || a, l = t[0].multisample ?? o, c = t[0].padding, u = t[0].autoFit, d = t[0].legacy ?? !0;
    for (let v = 1; v < t.length; v++) {
      const p = t[v];
      h = Math.min(h, p.resolution || a), l = Math.min(l, p.multisample ?? o), c = this.useMaxPadding ? Math.max(c, p.padding) : c + p.padding, u = u && p.autoFit, d = d || (p.legacy ?? !0);
    }
    s.length === 1 && (this.defaultFilterStack[0].renderTexture = n.current), s.push(i), i.resolution = h, i.multisample = l, i.legacy = d, i.target = e, i.sourceFrame.copyFrom(e.filterArea || e.getBounds(!0)), i.sourceFrame.pad(c);
    const y = this.tempRect.copyFrom(n.sourceFrame);
    r.projection.transform && this.transformAABB(_s.copyFrom(r.projection.transform).invert(), y), u ? (i.sourceFrame.fit(y), (i.sourceFrame.width <= 0 || i.sourceFrame.height <= 0) && (i.sourceFrame.width = 0, i.sourceFrame.height = 0)) : i.sourceFrame.intersects(y) || (i.sourceFrame.width = 0, i.sourceFrame.height = 0), this.roundFrame(i.sourceFrame, n.current ? n.current.resolution : r.resolution, n.sourceFrame, n.destinationFrame, r.projection.transform), i.renderTexture = this.getOptimalFilterTexture(i.sourceFrame.width, i.sourceFrame.height, h, l), i.filters = t, i.destinationFrame.width = i.renderTexture.width, i.destinationFrame.height = i.renderTexture.height;
    const m = this.tempRect;
    m.x = 0, m.y = 0, m.width = i.sourceFrame.width, m.height = i.sourceFrame.height, i.renderTexture.filterFrame = i.sourceFrame, i.bindingSourceFrame.copyFrom(n.sourceFrame), i.bindingDestinationFrame.copyFrom(n.destinationFrame), i.transform = r.projection.transform, r.projection.transform = null, n.bind(i.renderTexture, i.sourceFrame, m), r.framebuffer.clear(0, 0, 0, 0);
  }
  pop() {
    const e = this.defaultFilterStack, t = e.pop(), r = t.filters;
    this.activeState = t;
    const s = this.globalUniforms.uniforms;
    s.outputFrame = t.sourceFrame, s.resolution = t.resolution;
    const i = s.inputSize, n = s.inputPixel, a = s.inputClamp;
    if (i[0] = t.destinationFrame.width, i[1] = t.destinationFrame.height, i[2] = 1 / i[0], i[3] = 1 / i[1], n[0] = Math.round(i[0] * t.resolution), n[1] = Math.round(i[1] * t.resolution), n[2] = 1 / n[0], n[3] = 1 / n[1], a[0] = 0.5 * n[2], a[1] = 0.5 * n[3], a[2] = t.sourceFrame.width * i[2] - 0.5 * n[2], a[3] = t.sourceFrame.height * i[3] - 0.5 * n[3], t.legacy) {
      const h = s.filterArea;
      h[0] = t.destinationFrame.width, h[1] = t.destinationFrame.height, h[2] = t.sourceFrame.x, h[3] = t.sourceFrame.y, s.filterClamp = s.inputClamp;
    }
    this.globalUniforms.update();
    const o = e[e.length - 1];
    if (this.renderer.framebuffer.blit(), r.length === 1) r[0].apply(this, t.renderTexture, o.renderTexture, Kt.BLEND, t), this.returnFilterTexture(t.renderTexture);
    else {
      let h = t.renderTexture, l = this.getOptimalFilterTexture(h.width, h.height, t.resolution);
      l.filterFrame = h.filterFrame;
      let c = 0;
      for (c = 0; c < r.length - 1; ++c) {
        c === 1 && t.multisample > 1 && (l = this.getOptimalFilterTexture(h.width, h.height, t.resolution), l.filterFrame = h.filterFrame), r[c].apply(this, h, l, Kt.CLEAR, t);
        const u = h;
        h = l, l = u;
      }
      r[c].apply(this, h, o.renderTexture, Kt.BLEND, t), c > 1 && t.multisample > 1 && this.returnFilterTexture(t.renderTexture), this.returnFilterTexture(h), this.returnFilterTexture(l);
    }
    t.clear(), this.statePool.push(t);
  }
  bindAndClear(e, t = Kt.CLEAR) {
    const { renderTexture: r, state: s } = this.renderer;
    if (e === this.defaultFilterStack[this.defaultFilterStack.length - 1].renderTexture ? this.renderer.projection.transform = this.activeState.transform : this.renderer.projection.transform = null, e?.filterFrame) {
      const n = this.tempRect;
      n.x = 0, n.y = 0, n.width = e.filterFrame.width, n.height = e.filterFrame.height, r.bind(e, e.filterFrame, n);
    } else e !== this.defaultFilterStack[this.defaultFilterStack.length - 1].renderTexture ? r.bind(e) : this.renderer.renderTexture.bind(e, this.activeState.bindingSourceFrame, this.activeState.bindingDestinationFrame);
    const i = s.stateId & 1 || this.forceClear;
    (t === Kt.CLEAR || t === Kt.BLIT && i) && this.renderer.framebuffer.clear(0, 0, 0, 0);
  }
  applyFilter(e, t, r, s) {
    const i = this.renderer;
    i.state.set(e.state), this.bindAndClear(r, s), e.uniforms.uSampler = t, e.uniforms.filterGlobals = this.globalUniforms, i.shader.bind(e), e.legacy = !!e.program.attributeData.aTextureCoord, e.legacy ? (this.quadUv.map(t._frame, t.filterFrame), i.geometry.bind(this.quadUv), i.geometry.draw(Wr.TRIANGLES)) : (i.geometry.bind(this.quad), i.geometry.draw(Wr.TRIANGLE_STRIP));
  }
  calculateSpriteMatrix(e, t) {
    const { sourceFrame: r, destinationFrame: s } = this.activeState, { orig: i } = t._texture, n = e.set(s.width, 0, 0, s.height, r.x, r.y), a = t.worldTransform.copyTo(Lt.TEMP_MATRIX);
    return a.invert(), n.prepend(a), n.scale(1 / i.width, 1 / i.height), n.translate(t.anchor.x, t.anchor.y), n;
  }
  destroy() {
    this.renderer = null, this.texturePool.clear(!1);
  }
  getOptimalFilterTexture(e, t, r = 1, s = Mt.NONE) {
    return this.texturePool.getOptimalTexture(e, t, r, s);
  }
  getFilterTexture(e, t, r) {
    if (typeof e == "number") {
      const i = e;
      e = t, t = i;
    }
    e = e || this.activeState.renderTexture;
    const s = this.texturePool.getOptimalTexture(e.width, e.height, t || e.resolution, r || Mt.NONE);
    return s.filterFrame = e.filterFrame, s;
  }
  returnFilterTexture(e) {
    this.texturePool.returnTexture(e);
  }
  emptyPool() {
    this.texturePool.clear(!0);
  }
  resize() {
    this.texturePool.setScreenSize(this.renderer.view);
  }
  transformAABB(e, t) {
    const r = Cr[0], s = Cr[1], i = Cr[2], n = Cr[3];
    r.set(t.left, t.top), s.set(t.left, t.bottom), i.set(t.right, t.top), n.set(t.right, t.bottom), e.apply(r, r), e.apply(s, s), e.apply(i, i), e.apply(n, n);
    const a = Math.min(r.x, s.x, i.x, n.x), o = Math.min(r.y, s.y, i.y, n.y), h = Math.max(r.x, s.x, i.x, n.x), l = Math.max(r.y, s.y, i.y, n.y);
    t.x = a, t.y = o, t.width = h - a, t.height = l - o;
  }
  roundFrame(e, t, r, s, i) {
    if (!(e.width <= 0 || e.height <= 0 || r.width <= 0 || r.height <= 0)) {
      if (i) {
        const { a: n, b: a, c: o, d: h } = i;
        if ((Math.abs(a) > 1e-4 || Math.abs(o) > 1e-4) && (Math.abs(n) > 1e-4 || Math.abs(h) > 1e-4)) return;
      }
      i = i ? _s.copyFrom(i) : _s.identity(), i.translate(-r.x, -r.y).scale(s.width / r.width, s.height / r.height).translate(s.x, s.y), this.transformAABB(i, e), e.ceil(t), this.transformAABB(i.invert(), e);
    }
  }
};
ua.extension = {
  type: ut.RendererSystem,
  name: "filter"
};
mt.add(ua);
var jh = class {
  constructor(e) {
    this.framebuffer = e, this.stencil = null, this.dirtyId = -1, this.dirtyFormat = -1, this.dirtySize = -1, this.multisample = Mt.NONE, this.msaaBuffer = null, this.blitFramebuffer = null, this.mipLevel = 0;
  }
}, Yh = new Et(), da = class {
  constructor(e) {
    this.renderer = e, this.managedFramebuffers = [], this.unknownFramebuffer = new Gs(10, 10), this.msaaSamples = null;
  }
  contextChange() {
    this.disposeAll(!0);
    const e = this.gl = this.renderer.gl;
    if (this.CONTEXT_UID = this.renderer.CONTEXT_UID, this.current = this.unknownFramebuffer, this.viewport = new Et(), this.hasMRT = !0, this.writeDepthTexture = !0, this.renderer.context.webGLVersion === 1) {
      let t = this.renderer.context.extensions.drawBuffers, r = this.renderer.context.extensions.depthTexture;
      dt.PREFER_ENV === Me.WEBGL_LEGACY && (t = null, r = null), t ? e.drawBuffers = (s) => t.drawBuffersWEBGL(s) : (this.hasMRT = !1, e.drawBuffers = () => {
      }), r || (this.writeDepthTexture = !1);
    } else this.msaaSamples = e.getInternalformatParameter(e.RENDERBUFFER, e.RGBA8, e.SAMPLES);
  }
  bind(e, t, r = 0) {
    const { gl: s } = this;
    if (e) {
      const i = e.glFramebuffers[this.CONTEXT_UID] || this.initFramebuffer(e);
      this.current !== e && (this.current = e, s.bindFramebuffer(s.FRAMEBUFFER, i.framebuffer)), i.mipLevel !== r && (e.dirtyId++, e.dirtyFormat++, i.mipLevel = r), i.dirtyId !== e.dirtyId && (i.dirtyId = e.dirtyId, i.dirtyFormat !== e.dirtyFormat ? (i.dirtyFormat = e.dirtyFormat, i.dirtySize = e.dirtySize, this.updateFramebuffer(e, r)) : i.dirtySize !== e.dirtySize && (i.dirtySize = e.dirtySize, this.resizeFramebuffer(e)));
      for (let n = 0; n < e.colorTextures.length; n++) {
        const a = e.colorTextures[n];
        this.renderer.texture.unbind(a.parentTextureArray || a);
      }
      if (e.depthTexture && this.renderer.texture.unbind(e.depthTexture), t) {
        const n = t.width >> r, a = t.height >> r, o = n / t.width;
        this.setViewport(t.x * o, t.y * o, n, a);
      } else {
        const n = e.width >> r, a = e.height >> r;
        this.setViewport(0, 0, n, a);
      }
    } else this.current && (this.current = null, s.bindFramebuffer(s.FRAMEBUFFER, null)), t ? this.setViewport(t.x, t.y, t.width, t.height) : this.setViewport(0, 0, this.renderer.width, this.renderer.height);
  }
  setViewport(e, t, r, s) {
    const i = this.viewport;
    e = Math.round(e), t = Math.round(t), r = Math.round(r), s = Math.round(s), (i.width !== r || i.height !== s || i.x !== e || i.y !== t) && (i.x = e, i.y = t, i.width = r, i.height = s, this.gl.viewport(e, t, r, s));
  }
  get size() {
    return this.current ? {
      x: 0,
      y: 0,
      width: this.current.width,
      height: this.current.height
    } : {
      x: 0,
      y: 0,
      width: this.renderer.width,
      height: this.renderer.height
    };
  }
  clear(e, t, r, s, i = Rs.COLOR | Rs.DEPTH) {
    const { gl: n } = this;
    n.clearColor(e, t, r, s), n.clear(i);
  }
  initFramebuffer(e) {
    const { gl: t } = this, r = new jh(t.createFramebuffer());
    return r.multisample = this.detectSamples(e.multisample), e.glFramebuffers[this.CONTEXT_UID] = r, this.managedFramebuffers.push(e), e.disposeRunner.add(this), r;
  }
  resizeFramebuffer(e) {
    const { gl: t } = this, r = e.glFramebuffers[this.CONTEXT_UID];
    if (r.stencil) {
      t.bindRenderbuffer(t.RENDERBUFFER, r.stencil);
      let n;
      this.renderer.context.webGLVersion === 1 ? n = t.DEPTH_STENCIL : e.depth && e.stencil ? n = t.DEPTH24_STENCIL8 : e.depth ? n = t.DEPTH_COMPONENT24 : n = t.STENCIL_INDEX8, r.msaaBuffer ? t.renderbufferStorageMultisample(t.RENDERBUFFER, r.multisample, n, e.width, e.height) : t.renderbufferStorage(t.RENDERBUFFER, n, e.width, e.height);
    }
    const s = e.colorTextures;
    let i = s.length;
    t.drawBuffers || (i = Math.min(i, 1));
    for (let n = 0; n < i; n++) {
      const a = s[n], o = a.parentTextureArray || a;
      this.renderer.texture.bind(o, 0), n === 0 && r.msaaBuffer && (t.bindRenderbuffer(t.RENDERBUFFER, r.msaaBuffer), t.renderbufferStorageMultisample(t.RENDERBUFFER, r.multisample, o._glTextures[this.CONTEXT_UID].internalFormat, e.width, e.height));
    }
    e.depthTexture && this.writeDepthTexture && this.renderer.texture.bind(e.depthTexture, 0);
  }
  updateFramebuffer(e, t) {
    const { gl: r } = this, s = e.glFramebuffers[this.CONTEXT_UID], i = e.colorTextures;
    let n = i.length;
    r.drawBuffers || (n = Math.min(n, 1)), s.multisample > 1 && this.canMultisampleFramebuffer(e) ? s.msaaBuffer = s.msaaBuffer || r.createRenderbuffer() : s.msaaBuffer && (r.deleteRenderbuffer(s.msaaBuffer), s.msaaBuffer = null, s.blitFramebuffer && (s.blitFramebuffer.dispose(), s.blitFramebuffer = null));
    const a = [];
    for (let o = 0; o < n; o++) {
      const h = i[o], l = h.parentTextureArray || h;
      this.renderer.texture.bind(l, 0), o === 0 && s.msaaBuffer ? (r.bindRenderbuffer(r.RENDERBUFFER, s.msaaBuffer), r.renderbufferStorageMultisample(r.RENDERBUFFER, s.multisample, l._glTextures[this.CONTEXT_UID].internalFormat, e.width, e.height), r.framebufferRenderbuffer(r.FRAMEBUFFER, r.COLOR_ATTACHMENT0, r.RENDERBUFFER, s.msaaBuffer)) : (r.framebufferTexture2D(r.FRAMEBUFFER, r.COLOR_ATTACHMENT0 + o, h.target, l._glTextures[this.CONTEXT_UID].texture, t), a.push(r.COLOR_ATTACHMENT0 + o));
    }
    if (a.length > 1 && r.drawBuffers(a), e.depthTexture && this.writeDepthTexture) {
      const o = e.depthTexture;
      this.renderer.texture.bind(o, 0), r.framebufferTexture2D(r.FRAMEBUFFER, r.DEPTH_ATTACHMENT, r.TEXTURE_2D, o._glTextures[this.CONTEXT_UID].texture, t);
    }
    if ((e.stencil || e.depth) && !(e.depthTexture && this.writeDepthTexture)) {
      s.stencil = s.stencil || r.createRenderbuffer();
      let o, h;
      this.renderer.context.webGLVersion === 1 ? (o = r.DEPTH_STENCIL_ATTACHMENT, h = r.DEPTH_STENCIL) : e.depth && e.stencil ? (o = r.DEPTH_STENCIL_ATTACHMENT, h = r.DEPTH24_STENCIL8) : e.depth ? (o = r.DEPTH_ATTACHMENT, h = r.DEPTH_COMPONENT24) : (o = r.STENCIL_ATTACHMENT, h = r.STENCIL_INDEX8), r.bindRenderbuffer(r.RENDERBUFFER, s.stencil), s.msaaBuffer ? r.renderbufferStorageMultisample(r.RENDERBUFFER, s.multisample, h, e.width, e.height) : r.renderbufferStorage(r.RENDERBUFFER, h, e.width, e.height), r.framebufferRenderbuffer(r.FRAMEBUFFER, o, r.RENDERBUFFER, s.stencil);
    } else s.stencil && (r.deleteRenderbuffer(s.stencil), s.stencil = null);
  }
  canMultisampleFramebuffer(e) {
    return this.renderer.context.webGLVersion !== 1 && e.colorTextures.length <= 1 && !e.depthTexture;
  }
  detectSamples(e) {
    const { msaaSamples: t } = this;
    let r = Mt.NONE;
    if (e <= 1 || t === null) return r;
    for (let s = 0; s < t.length; s++) if (t[s] <= e) {
      r = t[s];
      break;
    }
    return r === 1 && (r = Mt.NONE), r;
  }
  blit(e, t, r) {
    const { current: s, renderer: i, gl: n, CONTEXT_UID: a } = this;
    if (i.context.webGLVersion !== 2 || !s) return;
    const o = s.glFramebuffers[a];
    if (!o) return;
    if (!e) {
      if (!o.msaaBuffer) return;
      const l = s.colorTextures[0];
      if (!l) return;
      o.blitFramebuffer || (o.blitFramebuffer = new Gs(s.width, s.height), o.blitFramebuffer.addColorTexture(0, l)), e = o.blitFramebuffer, e.colorTextures[0] !== l && (e.colorTextures[0] = l, e.dirtyId++, e.dirtyFormat++), (e.width !== s.width || e.height !== s.height) && (e.width = s.width, e.height = s.height, e.dirtyId++, e.dirtySize++);
    }
    t || (t = Yh, t.width = s.width, t.height = s.height), r || (r = t);
    const h = t.width === r.width && t.height === r.height;
    this.bind(e), n.bindFramebuffer(n.READ_FRAMEBUFFER, o.framebuffer), n.blitFramebuffer(t.left, t.top, t.right, t.bottom, r.left, r.top, r.right, r.bottom, n.COLOR_BUFFER_BIT, h ? n.NEAREST : n.LINEAR), n.bindFramebuffer(n.READ_FRAMEBUFFER, e.glFramebuffers[this.CONTEXT_UID].framebuffer);
  }
  disposeFramebuffer(e, t) {
    const r = e.glFramebuffers[this.CONTEXT_UID], s = this.gl;
    if (!r) return;
    delete e.glFramebuffers[this.CONTEXT_UID];
    const i = this.managedFramebuffers.indexOf(e);
    i >= 0 && this.managedFramebuffers.splice(i, 1), e.disposeRunner.remove(this), t || (s.deleteFramebuffer(r.framebuffer), r.msaaBuffer && s.deleteRenderbuffer(r.msaaBuffer), r.stencil && s.deleteRenderbuffer(r.stencil)), r.blitFramebuffer && this.disposeFramebuffer(r.blitFramebuffer, t);
  }
  disposeAll(e) {
    const t = this.managedFramebuffers;
    this.managedFramebuffers = [];
    for (let r = 0; r < t.length; r++) this.disposeFramebuffer(t[r], e);
  }
  forceStencil() {
    const e = this.current;
    if (!e) return;
    const t = e.glFramebuffers[this.CONTEXT_UID];
    if (!t || t.stencil && e.stencil) return;
    e.stencil = !0;
    const r = e.width, s = e.height, i = this.gl, n = t.stencil = i.createRenderbuffer();
    i.bindRenderbuffer(i.RENDERBUFFER, n);
    let a, o;
    this.renderer.context.webGLVersion === 1 ? (a = i.DEPTH_STENCIL_ATTACHMENT, o = i.DEPTH_STENCIL) : e.depth ? (a = i.DEPTH_STENCIL_ATTACHMENT, o = i.DEPTH24_STENCIL8) : (a = i.STENCIL_ATTACHMENT, o = i.STENCIL_INDEX8), t.msaaBuffer ? i.renderbufferStorageMultisample(i.RENDERBUFFER, t.multisample, o, r, s) : i.renderbufferStorage(i.RENDERBUFFER, o, r, s), i.framebufferRenderbuffer(i.FRAMEBUFFER, a, i.RENDERBUFFER, n);
  }
  reset() {
    this.current = this.unknownFramebuffer, this.viewport = new Et();
  }
  destroy() {
    this.renderer = null;
  }
};
da.extension = {
  type: ut.RendererSystem,
  name: "framebuffer"
};
mt.add(da);
var bs = {
  5126: 4,
  5123: 2,
  5121: 1
}, fa = class {
  constructor(e) {
    this.renderer = e, this._activeGeometry = null, this._activeVao = null, this.hasVao = !0, this.hasInstance = !0, this.canUseUInt32ElementIndex = !1, this.managedGeometries = {};
  }
  contextChange() {
    this.disposeAll(!0);
    const e = this.gl = this.renderer.gl, t = this.renderer.context;
    if (this.CONTEXT_UID = this.renderer.CONTEXT_UID, t.webGLVersion !== 2) {
      let r = this.renderer.context.extensions.vertexArrayObject;
      dt.PREFER_ENV === Me.WEBGL_LEGACY && (r = null), r ? (e.createVertexArray = () => r.createVertexArrayOES(), e.bindVertexArray = (s) => r.bindVertexArrayOES(s), e.deleteVertexArray = (s) => r.deleteVertexArrayOES(s)) : (this.hasVao = !1, e.createVertexArray = () => null, e.bindVertexArray = () => null, e.deleteVertexArray = () => null);
    }
    if (t.webGLVersion !== 2) {
      const r = e.getExtension("ANGLE_instanced_arrays");
      r ? (e.vertexAttribDivisor = (s, i) => r.vertexAttribDivisorANGLE(s, i), e.drawElementsInstanced = (s, i, n, a, o) => r.drawElementsInstancedANGLE(s, i, n, a, o), e.drawArraysInstanced = (s, i, n, a) => r.drawArraysInstancedANGLE(s, i, n, a)) : this.hasInstance = !1;
    }
    this.canUseUInt32ElementIndex = t.webGLVersion === 2 || !!t.extensions.uint32ElementIndex;
  }
  bind(e, t) {
    t = t || this.renderer.shader.shader;
    const { gl: r } = this;
    let s = e.glVertexArrayObjects[this.CONTEXT_UID], i = !1;
    s || (this.managedGeometries[e.id] = e, e.disposeRunner.add(this), e.glVertexArrayObjects[this.CONTEXT_UID] = s = {}, i = !0);
    const n = s[t.program.id] || this.initGeometryVao(e, t, i);
    this._activeGeometry = e, this._activeVao !== n && (this._activeVao = n, this.hasVao ? r.bindVertexArray(n) : this.activateVao(e, t.program)), this.updateBuffers();
  }
  reset() {
    this.unbind();
  }
  updateBuffers() {
    const e = this._activeGeometry, t = this.renderer.buffer;
    for (let r = 0; r < e.buffers.length; r++) {
      const s = e.buffers[r];
      t.update(s);
    }
  }
  checkCompatibility(e, t) {
    const r = e.attributes, s = t.attributeData;
    for (const i in s) if (!r[i]) throw new Error(`shader and geometry incompatible, geometry missing the "${i}" attribute`);
  }
  getSignature(e, t) {
    const r = e.attributes, s = t.attributeData, i = ["g", e.id];
    for (const n in r) s[n] && i.push(n, s[n].location);
    return i.join("-");
  }
  initGeometryVao(e, t, r = !0) {
    const s = this.gl, i = this.CONTEXT_UID, n = this.renderer.buffer, a = t.program;
    a.glPrograms[i] || this.renderer.shader.generateProgram(t), this.checkCompatibility(e, a);
    const o = this.getSignature(e, a), h = e.glVertexArrayObjects[this.CONTEXT_UID];
    let l = h[o];
    if (l) return h[a.id] = l, l;
    const c = e.buffers, u = e.attributes, d = {}, y = {};
    for (const m in c) d[m] = 0, y[m] = 0;
    for (const m in u) !u[m].size && a.attributeData[m] ? u[m].size = a.attributeData[m].size : u[m].size || console.warn(`PIXI Geometry attribute '${m}' size cannot be determined (likely the bound shader does not have the attribute)`), d[u[m].buffer] += u[m].size * bs[u[m].type];
    for (const m in u) {
      const v = u[m], p = v.size;
      v.stride === void 0 && (d[v.buffer] === p * bs[v.type] ? v.stride = 0 : v.stride = d[v.buffer]), v.start === void 0 && (v.start = y[v.buffer], y[v.buffer] += p * bs[v.type]);
    }
    l = s.createVertexArray(), s.bindVertexArray(l);
    for (let m = 0; m < c.length; m++) {
      const v = c[m];
      n.bind(v), r && v._glBuffers[i].refCount++;
    }
    return this.activateVao(e, a), h[a.id] = l, h[o] = l, s.bindVertexArray(null), n.unbind(Zt.ARRAY_BUFFER), l;
  }
  disposeGeometry(e, t) {
    if (!this.managedGeometries[e.id]) return;
    delete this.managedGeometries[e.id];
    const r = e.glVertexArrayObjects[this.CONTEXT_UID], s = this.gl, i = e.buffers, n = this.renderer?.buffer;
    if (e.disposeRunner.remove(this), !!r) {
      if (n) for (let a = 0; a < i.length; a++) {
        const o = i[a]._glBuffers[this.CONTEXT_UID];
        o && (o.refCount--, o.refCount === 0 && !t && n.dispose(i[a], t));
      }
      if (!t) {
        for (const a in r) if (a[0] === "g") {
          const o = r[a];
          this._activeVao === o && this.unbind(), s.deleteVertexArray(o);
        }
      }
      delete e.glVertexArrayObjects[this.CONTEXT_UID];
    }
  }
  disposeAll(e) {
    const t = Object.keys(this.managedGeometries);
    for (let r = 0; r < t.length; r++) this.disposeGeometry(this.managedGeometries[t[r]], e);
  }
  activateVao(e, t) {
    const r = this.gl, s = this.CONTEXT_UID, i = this.renderer.buffer, n = e.buffers, a = e.attributes;
    e.indexBuffer && i.bind(e.indexBuffer);
    let o = null;
    for (const h in a) {
      const l = a[h], c = n[l.buffer], u = c._glBuffers[s];
      if (t.attributeData[h]) {
        o !== u && (i.bind(c), o = u);
        const d = t.attributeData[h].location;
        if (r.enableVertexAttribArray(d), r.vertexAttribPointer(d, l.size, l.type || r.FLOAT, l.normalized, l.stride, l.start), l.instance) if (this.hasInstance) r.vertexAttribDivisor(d, l.divisor);
        else throw new Error("geometry error, GPU Instancing is not supported on this device");
      }
    }
  }
  draw(e, t, r, s) {
    const { gl: i } = this, n = this._activeGeometry;
    if (n.indexBuffer) {
      const a = n.indexBuffer.data.BYTES_PER_ELEMENT, o = a === 2 ? i.UNSIGNED_SHORT : i.UNSIGNED_INT;
      a === 2 || a === 4 && this.canUseUInt32ElementIndex ? n.instanced ? i.drawElementsInstanced(e, t || n.indexBuffer.data.length, o, (r || 0) * a, s || 1) : i.drawElements(e, t || n.indexBuffer.data.length, o, (r || 0) * a) : console.warn("unsupported index buffer type: uint32");
    } else n.instanced ? i.drawArraysInstanced(e, r, t || n.getSize(), s || 1) : i.drawArrays(e, r, t || n.getSize());
    return this;
  }
  unbind() {
    this.gl.bindVertexArray(null), this._activeVao = null, this._activeGeometry = null;
  }
  destroy() {
    this.renderer = null;
  }
};
fa.extension = {
  type: ut.RendererSystem,
  name: "geometry"
};
mt.add(fa);
var en = new Lt(), Kh = class {
  constructor(e, t) {
    this._texture = e, this.mapCoord = new Lt(), this.uClampFrame = /* @__PURE__ */ new Float32Array(4), this.uClampOffset = /* @__PURE__ */ new Float32Array(2), this._textureID = -1, this._updateID = 0, this.clampOffset = 0, this.clampMargin = typeof t > "u" ? 0.5 : t, this.isSimple = !1;
  }
  get texture() {
    return this._texture;
  }
  set texture(e) {
    this._texture = e, this._textureID = -1;
  }
  multiplyUvs(e, t) {
    t === void 0 && (t = e);
    const r = this.mapCoord;
    for (let s = 0; s < e.length; s += 2) {
      const i = e[s], n = e[s + 1];
      t[s] = i * r.a + n * r.c + r.tx, t[s + 1] = i * r.b + n * r.d + r.ty;
    }
    return t;
  }
  update(e) {
    const t = this._texture;
    if (!t || !t.valid || !e && this._textureID === t._updateID) return !1;
    this._textureID = t._updateID, this._updateID++;
    const r = t._uvs;
    this.mapCoord.set(r.x1 - r.x0, r.y1 - r.y0, r.x3 - r.x0, r.y3 - r.y0, r.x0, r.y0);
    const s = t.orig, i = t.trim;
    i && (en.set(s.width / i.width, 0, 0, s.height / i.height, -i.x / i.width, -i.y / i.height), this.mapCoord.append(en));
    const n = t.baseTexture, a = this.uClampFrame, o = this.clampMargin / n.resolution, h = this.clampOffset;
    return a[0] = (t._frame.x + o + h) / n.width, a[1] = (t._frame.y + o + h) / n.height, a[2] = (t._frame.x + t._frame.width - o + h) / n.width, a[3] = (t._frame.y + t._frame.height - o + h) / n.height, this.uClampOffset[0] = h / n.realWidth, this.uClampOffset[1] = h / n.realHeight, this.isSimple = t._frame.width === n.width && t._frame.height === n.height && t.rotate === 0, !0;
  }
}, Zh = `varying vec2 vMaskCoord;
varying vec2 vTextureCoord;

uniform sampler2D uSampler;
uniform sampler2D mask;
uniform float alpha;
uniform float npmAlpha;
uniform vec4 maskClamp;

void main(void)
{
    float clip = step(3.5,
        step(maskClamp.x, vMaskCoord.x) +
        step(maskClamp.y, vMaskCoord.y) +
        step(vMaskCoord.x, maskClamp.z) +
        step(vMaskCoord.y, maskClamp.w));

    vec4 original = texture2D(uSampler, vTextureCoord);
    vec4 masky = texture2D(mask, vMaskCoord);
    float alphaMul = 1.0 - npmAlpha * (1.0 - masky.a);

    original *= (alphaMul * masky.r * alpha * clip);

    gl_FragColor = original;
}
`, Jh = `attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;
uniform mat3 otherMatrix;

varying vec2 vMaskCoord;
varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);

    vTextureCoord = aTextureCoord;
    vMaskCoord = ( otherMatrix * vec3( aTextureCoord, 1.0)  ).xy;
}
`, Qh = class extends Gt {
  constructor(e, t, r) {
    let s = null;
    typeof e != "string" && t === void 0 && r === void 0 && (s = e, e = void 0, t = void 0, r = void 0), super(e || Jh, t || Zh, r), this.maskSprite = s, this.maskMatrix = new Lt();
  }
  get maskSprite() {
    return this._maskSprite;
  }
  set maskSprite(e) {
    this._maskSprite = e, this._maskSprite && (this._maskSprite.renderable = !1);
  }
  apply(e, t, r, s) {
    const i = this._maskSprite, n = i._texture;
    n.valid && (n.uvMatrix || (n.uvMatrix = new Kh(n, 0)), n.uvMatrix.update(), this.uniforms.npmAlpha = n.baseTexture.alphaMode ? 0 : 1, this.uniforms.mask = n, this.uniforms.otherMatrix = e.calculateSpriteMatrix(this.maskMatrix, i).prepend(n.uvMatrix.mapCoord), this.uniforms.alpha = i.worldAlpha, this.uniforms.maskClamp = n.uvMatrix.uClampFrame, e.applyFilter(this, t, r, s));
  }
}, tl = class {
  constructor(e = null) {
    this.type = Ct.NONE, this.autoDetect = !0, this.maskObject = e || null, this.pooled = !1, this.isMaskData = !0, this.resolution = null, this.multisample = Gt.defaultMultisample, this.enabled = !0, this.colorMask = 15, this._filters = null, this._stencilCounter = 0, this._scissorCounter = 0, this._scissorRect = null, this._scissorRectLocal = null, this._colorMask = 15, this._target = null;
  }
  get filter() {
    return this._filters ? this._filters[0] : null;
  }
  set filter(e) {
    e ? this._filters ? this._filters[0] = e : this._filters = [e] : this._filters = null;
  }
  reset() {
    this.pooled && (this.maskObject = null, this.type = Ct.NONE, this.autoDetect = !0), this._target = null, this._scissorRectLocal = null;
  }
  copyCountersOrReset(e) {
    e ? (this._stencilCounter = e._stencilCounter, this._scissorCounter = e._scissorCounter, this._scissorRect = e._scissorRect) : (this._stencilCounter = 0, this._scissorCounter = 0, this._scissorRect = null);
  }
}, pa = class {
  constructor(e) {
    this.renderer = e, this.enableScissor = !0, this.alphaMaskPool = [], this.maskDataPool = [], this.maskStack = [], this.alphaMaskIndex = 0;
  }
  setMaskStack(e) {
    this.maskStack = e, this.renderer.scissor.setMaskStack(e), this.renderer.stencil.setMaskStack(e);
  }
  push(e, t) {
    let r = t;
    if (!r.isMaskData) {
      const i = this.maskDataPool.pop() || new tl();
      i.pooled = !0, i.maskObject = t, r = i;
    }
    const s = this.maskStack.length !== 0 ? this.maskStack[this.maskStack.length - 1] : null;
    if (r.copyCountersOrReset(s), r._colorMask = s ? s._colorMask : 15, r.autoDetect && this.detect(r), r._target = e, r.type !== Ct.SPRITE && this.maskStack.push(r), r.enabled) switch (r.type) {
      case Ct.SCISSOR:
        this.renderer.scissor.push(r);
        break;
      case Ct.STENCIL:
        this.renderer.stencil.push(r);
        break;
      case Ct.SPRITE:
        r.copyCountersOrReset(null), this.pushSpriteMask(r);
        break;
      case Ct.COLOR:
        this.pushColorMask(r);
    }
    r.type === Ct.SPRITE && this.maskStack.push(r);
  }
  pop(e) {
    const t = this.maskStack.pop();
    if (!(!t || t._target !== e)) {
      if (t.enabled) switch (t.type) {
        case Ct.SCISSOR:
          this.renderer.scissor.pop(t);
          break;
        case Ct.STENCIL:
          this.renderer.stencil.pop(t.maskObject);
          break;
        case Ct.SPRITE:
          this.popSpriteMask(t);
          break;
        case Ct.COLOR:
          this.popColorMask(t);
      }
      if (t.reset(), t.pooled && this.maskDataPool.push(t), this.maskStack.length !== 0) {
        const r = this.maskStack[this.maskStack.length - 1];
        r.type === Ct.SPRITE && r._filters && (r._filters[0].maskSprite = r.maskObject);
      }
    }
  }
  detect(e) {
    const t = e.maskObject;
    t ? t.isSprite ? e.type = Ct.SPRITE : this.enableScissor && this.renderer.scissor.testScissor(e) ? e.type = Ct.SCISSOR : e.type = Ct.STENCIL : e.type = Ct.COLOR;
  }
  pushSpriteMask(e) {
    const { maskObject: t } = e, r = e._target;
    let s = e._filters;
    s || (s = this.alphaMaskPool[this.alphaMaskIndex], s || (s = this.alphaMaskPool[this.alphaMaskIndex] = [new Qh()])), s[0].resolution = e.resolution, s[0].multisample = e.multisample, s[0].maskSprite = t;
    const i = r.filterArea;
    r.filterArea = t.getBounds(!0), this.renderer.filter.push(r, s), r.filterArea = i, e._filters || this.alphaMaskIndex++;
  }
  popSpriteMask(e) {
    this.renderer.filter.pop(), e._filters ? e._filters[0].maskSprite = null : (this.alphaMaskIndex--, this.alphaMaskPool[this.alphaMaskIndex][0].maskSprite = null);
  }
  pushColorMask(e) {
    const t = e._colorMask, r = e._colorMask = t & e.colorMask;
    r !== t && this.renderer.gl.colorMask((r & 1) !== 0, (r & 2) !== 0, (r & 4) !== 0, (r & 8) !== 0);
  }
  popColorMask(e) {
    const t = e._colorMask, r = this.maskStack.length > 0 ? this.maskStack[this.maskStack.length - 1]._colorMask : 15;
    r !== t && this.renderer.gl.colorMask((r & 1) !== 0, (r & 2) !== 0, (r & 4) !== 0, (r & 8) !== 0);
  }
  destroy() {
    this.renderer = null;
  }
};
pa.extension = {
  type: ut.RendererSystem,
  name: "mask"
};
mt.add(pa);
var ma = class {
  constructor(e) {
    this.renderer = e, this.maskStack = [], this.glConst = 0;
  }
  getStackLength() {
    return this.maskStack.length;
  }
  setMaskStack(e) {
    const { gl: t } = this.renderer, r = this.getStackLength();
    this.maskStack = e;
    const s = this.getStackLength();
    s !== r && (s === 0 ? t.disable(this.glConst) : (t.enable(this.glConst), this._useCurrent()));
  }
  _useCurrent() {
  }
  destroy() {
    this.renderer = null, this.maskStack = null;
  }
}, rn = new Lt(), sn = [], ya = class zr extends ma {
  constructor(t) {
    super(t), this.glConst = dt.ADAPTER.getWebGLRenderingContext().SCISSOR_TEST;
  }
  getStackLength() {
    const t = this.maskStack[this.maskStack.length - 1];
    return t ? t._scissorCounter : 0;
  }
  calcScissorRect(t) {
    if (t._scissorRectLocal) return;
    const r = t._scissorRect, { maskObject: s } = t, { renderer: i } = this, n = i.renderTexture, a = s.getBounds(!0, sn.pop() ?? new Et());
    this.roundFrameToPixels(a, n.current ? n.current.resolution : i.resolution, n.sourceFrame, n.destinationFrame, i.projection.transform), r && a.fit(r), t._scissorRectLocal = a;
  }
  static isMatrixRotated(t) {
    if (!t) return !1;
    const { a: r, b: s, c: i, d: n } = t;
    return (Math.abs(s) > 1e-4 || Math.abs(i) > 1e-4) && (Math.abs(r) > 1e-4 || Math.abs(n) > 1e-4);
  }
  testScissor(t) {
    const { maskObject: r } = t;
    if (!r.isFastRect || !r.isFastRect() || zr.isMatrixRotated(r.worldTransform) || zr.isMatrixRotated(this.renderer.projection.transform)) return !1;
    this.calcScissorRect(t);
    const s = t._scissorRectLocal;
    return s.width > 0 && s.height > 0;
  }
  roundFrameToPixels(t, r, s, i, n) {
    zr.isMatrixRotated(n) || (n = n ? rn.copyFrom(n) : rn.identity(), n.translate(-s.x, -s.y).scale(i.width / s.width, i.height / s.height).translate(i.x, i.y), this.renderer.filter.transformAABB(n, t), t.fit(i), t.x = Math.round(t.x * r), t.y = Math.round(t.y * r), t.width = Math.round(t.width * r), t.height = Math.round(t.height * r));
  }
  push(t) {
    t._scissorRectLocal || this.calcScissorRect(t);
    const { gl: r } = this.renderer;
    t._scissorRect || r.enable(r.SCISSOR_TEST), t._scissorCounter++, t._scissorRect = t._scissorRectLocal, this._useCurrent();
  }
  pop(t) {
    const { gl: r } = this.renderer;
    t && sn.push(t._scissorRectLocal), this.getStackLength() > 0 ? this._useCurrent() : r.disable(r.SCISSOR_TEST);
  }
  _useCurrent() {
    const t = this.maskStack[this.maskStack.length - 1]._scissorRect;
    let r;
    this.renderer.renderTexture.current ? r = t.y : r = this.renderer.height - t.height - t.y, this.renderer.gl.scissor(t.x, r, t.width, t.height);
  }
};
ya.extension = {
  type: ut.RendererSystem,
  name: "scissor"
};
var el = ya;
mt.add(el);
var ga = class extends ma {
  constructor(e) {
    super(e), this.glConst = dt.ADAPTER.getWebGLRenderingContext().STENCIL_TEST;
  }
  getStackLength() {
    const e = this.maskStack[this.maskStack.length - 1];
    return e ? e._stencilCounter : 0;
  }
  push(e) {
    const t = e.maskObject, { gl: r } = this.renderer, s = e._stencilCounter;
    s === 0 && (this.renderer.framebuffer.forceStencil(), r.clearStencil(0), r.clear(r.STENCIL_BUFFER_BIT), r.enable(r.STENCIL_TEST)), e._stencilCounter++;
    const i = e._colorMask;
    i !== 0 && (e._colorMask = 0, r.colorMask(!1, !1, !1, !1)), r.stencilFunc(r.EQUAL, s, 4294967295), r.stencilOp(r.KEEP, r.KEEP, r.INCR), t.renderable = !0, t.render(this.renderer), this.renderer.batch.flush(), t.renderable = !1, i !== 0 && (e._colorMask = i, r.colorMask((i & 1) !== 0, (i & 2) !== 0, (i & 4) !== 0, (i & 8) !== 0)), this._useCurrent();
  }
  pop(e) {
    const t = this.renderer.gl;
    if (this.getStackLength() === 0) t.disable(t.STENCIL_TEST);
    else {
      const r = this.maskStack.length !== 0 ? this.maskStack[this.maskStack.length - 1] : null, s = r ? r._colorMask : 15;
      s !== 0 && (r._colorMask = 0, t.colorMask(!1, !1, !1, !1)), t.stencilOp(t.KEEP, t.KEEP, t.DECR), e.renderable = !0, e.render(this.renderer), this.renderer.batch.flush(), e.renderable = !1, s !== 0 && (r._colorMask = s, t.colorMask((s & 1) !== 0, (s & 2) !== 0, (s & 4) !== 0, (s & 8) !== 0)), this._useCurrent();
    }
  }
  _useCurrent() {
    const e = this.renderer.gl;
    e.stencilFunc(e.EQUAL, this.getStackLength(), 4294967295), e.stencilOp(e.KEEP, e.KEEP, e.KEEP);
  }
};
ga.extension = {
  type: ut.RendererSystem,
  name: "stencil"
};
mt.add(ga);
var va = class {
  constructor(e) {
    this.renderer = e, this.plugins = {}, Object.defineProperties(this.plugins, {
      extract: {
        enumerable: !1,
        get() {
          return yt("7.0.0", "renderer.plugins.extract has moved to renderer.extract"), e.extract;
        }
      },
      prepare: {
        enumerable: !1,
        get() {
          return yt("7.0.0", "renderer.plugins.prepare has moved to renderer.prepare"), e.prepare;
        }
      },
      interaction: {
        enumerable: !1,
        get() {
          return yt("7.0.0", "renderer.plugins.interaction has been deprecated, use renderer.events"), e.events;
        }
      }
    });
  }
  init() {
    const e = this.rendererPlugins;
    for (const t in e) this.plugins[t] = new e[t](this.renderer);
  }
  destroy() {
    for (const e in this.plugins) this.plugins[e].destroy(), this.plugins[e] = null;
  }
};
va.extension = {
  type: [ut.RendererSystem, ut.CanvasRendererSystem],
  name: "_plugin"
};
mt.add(va);
var xa = class {
  constructor(e) {
    this.renderer = e, this.destinationFrame = null, this.sourceFrame = null, this.defaultFrame = null, this.projectionMatrix = new Lt(), this.transform = null;
  }
  update(e, t, r, s) {
    this.destinationFrame = e || this.destinationFrame || this.defaultFrame, this.sourceFrame = t || this.sourceFrame || e, this.calculateProjection(this.destinationFrame, this.sourceFrame, r, s), this.transform && this.projectionMatrix.append(this.transform);
    const i = this.renderer;
    i.globalUniforms.uniforms.projectionMatrix = this.projectionMatrix, i.globalUniforms.update(), i.shader.shader && i.shader.syncUniformGroup(i.shader.shader.uniforms.globals);
  }
  calculateProjection(e, t, r, s) {
    const i = this.projectionMatrix, n = s ? -1 : 1;
    i.identity(), i.a = 1 / t.width * 2, i.d = n * (1 / t.height * 2), i.tx = -1 - t.x * i.a, i.ty = -n - t.y * i.d;
  }
  setTransform(e) {
  }
  destroy() {
    this.renderer = null;
  }
};
xa.extension = {
  type: ut.RendererSystem,
  name: "projection"
};
mt.add(xa);
var rl = new ii(), nn = new Et(), _a = class {
  constructor(e) {
    this.renderer = e, this._tempMatrix = new Lt();
  }
  generateTexture(e, t) {
    const { region: r, ...s } = t || {}, i = r?.copyTo(nn) || e.getLocalBounds(nn, !0), n = s.resolution || this.renderer.resolution;
    i.width = Math.max(i.width, 1 / n), i.height = Math.max(i.height, 1 / n), s.width = i.width, s.height = i.height, s.resolution = n, s.multisample ?? (s.multisample = this.renderer.multisample);
    const a = ha.create(s);
    this._tempMatrix.tx = -i.x, this._tempMatrix.ty = -i.y;
    const o = e.transform;
    return e.transform = rl, this.renderer.render(e, {
      renderTexture: a,
      transform: this._tempMatrix,
      skipUpdateTransform: !!e.parent,
      blit: !0
    }), e.transform = o, a;
  }
  destroy() {
  }
};
_a.extension = {
  type: [ut.RendererSystem, ut.CanvasRendererSystem],
  name: "textureGenerator"
};
mt.add(_a);
var me = new Et(), er = new Et(), ba = class {
  constructor(e) {
    this.renderer = e, this.defaultMaskStack = [], this.current = null, this.sourceFrame = new Et(), this.destinationFrame = new Et(), this.viewportFrame = new Et();
  }
  contextChange() {
    const e = this.renderer?.gl.getContextAttributes();
    this._rendererPremultipliedAlpha = !!(e && e.alpha && e.premultipliedAlpha);
  }
  bind(e = null, t, r) {
    const s = this.renderer;
    this.current = e;
    let i, n, a;
    e ? (i = e.baseTexture, a = i.resolution, t || (me.width = e.frame.width, me.height = e.frame.height, t = me), r || (er.x = e.frame.x, er.y = e.frame.y, er.width = t.width, er.height = t.height, r = er), n = i.framebuffer) : (a = s.resolution, t || (me.width = s._view.screen.width, me.height = s._view.screen.height, t = me), r || (r = me, r.width = t.width, r.height = t.height));
    const o = this.viewportFrame;
    o.x = r.x * a, o.y = r.y * a, o.width = r.width * a, o.height = r.height * a, e || (o.y = s.view.height - (o.y + o.height)), o.ceil(), this.renderer.framebuffer.bind(n, o), this.renderer.projection.update(r, t, a, !n), e ? this.renderer.mask.setMaskStack(i.maskStack) : this.renderer.mask.setMaskStack(this.defaultMaskStack), this.sourceFrame.copyFrom(t), this.destinationFrame.copyFrom(r);
  }
  clear(e, t) {
    const r = this.current ? this.current.baseTexture.clear : this.renderer.background.backgroundColor, s = Ae.shared.setValue(e || r);
    (this.current && this.current.baseTexture.alphaMode > 0 || !this.current && this._rendererPremultipliedAlpha) && s.premultiply(s.alpha);
    const i = this.destinationFrame, n = this.current ? this.current.baseTexture : this.renderer._view.screen, a = i.width !== n.width || i.height !== n.height;
    if (a) {
      let { x: o, y: h, width: l, height: c } = this.viewportFrame;
      o = Math.round(o), h = Math.round(h), l = Math.round(l), c = Math.round(c), this.renderer.gl.enable(this.renderer.gl.SCISSOR_TEST), this.renderer.gl.scissor(o, h, l, c);
    }
    this.renderer.framebuffer.clear(s.red, s.green, s.blue, s.alpha, t), a && this.renderer.scissor.pop();
  }
  resize() {
    this.bind(null);
  }
  reset() {
    this.bind(null);
  }
  destroy() {
    this.renderer = null;
  }
};
ba.extension = {
  type: ut.RendererSystem,
  name: "renderTexture"
};
mt.add(ba);
var sl = class {
  constructor(e, t) {
    this.program = e, this.uniformData = t, this.uniformGroups = {}, this.uniformDirtyGroups = {}, this.uniformBufferBindings = {};
  }
  destroy() {
    this.uniformData = null, this.uniformGroups = null, this.uniformDirtyGroups = null, this.uniformBufferBindings = null, this.program = null;
  }
};
function il(e, t) {
  const r = {}, s = t.getProgramParameter(e, t.ACTIVE_ATTRIBUTES);
  for (let i = 0; i < s; i++) {
    const n = t.getActiveAttrib(e, i);
    if (n.name.startsWith("gl_")) continue;
    const a = ea(t, n.type), o = {
      type: a,
      name: n.name,
      size: ta(a),
      location: t.getAttribLocation(e, n.name)
    };
    r[n.name] = o;
  }
  return r;
}
function nl(e, t) {
  const r = {}, s = t.getProgramParameter(e, t.ACTIVE_UNIFORMS);
  for (let i = 0; i < s; i++) {
    const n = t.getActiveUniform(e, i), a = n.name.replace(/\[.*?\]$/, ""), o = !!n.name.match(/\[.*?\]$/), h = ea(t, n.type);
    r[a] = {
      name: a,
      index: i,
      type: h,
      size: n.size,
      isArray: o,
      value: Jn(h, n.size)
    };
  }
  return r;
}
function al(e, t) {
  const r = Yi(e, e.VERTEX_SHADER, t.vertexSrc), s = Yi(e, e.FRAGMENT_SHADER, t.fragmentSrc), i = e.createProgram();
  e.attachShader(i, r), e.attachShader(i, s);
  const n = t.extra?.transformFeedbackVaryings;
  if (n && (typeof e.transformFeedbackVaryings != "function" ? console.warn("TransformFeedback is not supported but TransformFeedbackVaryings are given.") : e.transformFeedbackVaryings(i, n.names, n.bufferMode === "separate" ? e.SEPARATE_ATTRIBS : e.INTERLEAVED_ATTRIBS)), e.linkProgram(i), e.getProgramParameter(i, e.LINK_STATUS) || Lh(e, i, r, s), t.attributeData = il(i, e), t.uniformData = nl(i, e), !/^[ \t]*#[ \t]*version[ \t]+300[ \t]+es[ \t]*$/m.test(t.vertexSrc)) {
    const o = Object.keys(t.attributeData);
    o.sort((h, l) => h > l ? 1 : -1);
    for (let h = 0; h < o.length; h++) t.attributeData[o[h]].location = h, e.bindAttribLocation(i, h, o[h]);
    e.linkProgram(i);
  }
  e.deleteShader(r), e.deleteShader(s);
  const a = {};
  for (const o in t.uniformData) {
    const h = t.uniformData[o];
    a[o] = {
      location: e.getUniformLocation(i, o),
      value: Jn(h.type, h.size)
    };
  }
  return new sl(i, a);
}
function ol(e, t, r, s, i) {
  r.buffer.update(i);
}
var hl = {
  float: `
        data[offset] = v;
    `,
  vec2: `
        data[offset] = v[0];
        data[offset+1] = v[1];
    `,
  vec3: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];

    `,
  vec4: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];
        data[offset+3] = v[3];
    `,
  mat2: `
        data[offset] = v[0];
        data[offset+1] = v[1];

        data[offset+4] = v[2];
        data[offset+5] = v[3];
    `,
  mat3: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];

        data[offset + 4] = v[3];
        data[offset + 5] = v[4];
        data[offset + 6] = v[5];

        data[offset + 8] = v[6];
        data[offset + 9] = v[7];
        data[offset + 10] = v[8];
    `,
  mat4: `
        for(var i = 0; i < 16; i++)
        {
            data[offset + i] = v[i];
        }
    `
}, Ta = {
  float: 4,
  vec2: 8,
  vec3: 12,
  vec4: 16,
  int: 4,
  ivec2: 8,
  ivec3: 12,
  ivec4: 16,
  uint: 4,
  uvec2: 8,
  uvec3: 12,
  uvec4: 16,
  bool: 4,
  bvec2: 8,
  bvec3: 12,
  bvec4: 16,
  mat2: 32,
  mat3: 48,
  mat4: 64
};
function ll(e) {
  const t = e.map((n) => ({
    data: n,
    offset: 0,
    dataLen: 0,
    dirty: 0
  }));
  let r = 0, s = 0, i = 0;
  for (let n = 0; n < t.length; n++) {
    const a = t[n];
    if (r = Ta[a.data.type], a.data.size > 1 && (r = Math.max(r, 16) * a.data.size), a.dataLen = r, s % r !== 0 && s < 16) {
      const o = s % r % 16;
      s += o, i += o;
    }
    s + r > 16 ? (i = Math.ceil(i / 16) * 16, a.offset = i, i += r, s = r) : (a.offset = i, s += r, i += r);
  }
  return i = Math.ceil(i / 16) * 16, {
    uboElements: t,
    size: i
  };
}
function cl(e, t) {
  const r = [];
  for (const s in e) t[s] && r.push(t[s]);
  return r.sort((s, i) => s.index - i.index), r;
}
function ul(e, t) {
  if (!e.autoManage) return {
    size: 0,
    syncFunc: ol
  };
  const { uboElements: r, size: s } = ll(cl(e.uniforms, t)), i = [`
    var v = null;
    var v2 = null;
    var cv = null;
    var t = 0;
    var gl = renderer.gl
    var index = 0;
    var data = buffer.data;
    `];
  for (let n = 0; n < r.length; n++) {
    const a = r[n], o = e.uniforms[a.data.name], h = a.data.name;
    let l = !1;
    for (let c = 0; c < je.length; c++) {
      const u = je[c];
      if (u.codeUbo && u.test(a.data, o)) {
        i.push(`offset = ${a.offset / 4};`, je[c].codeUbo(a.data.name, o)), l = !0;
        break;
      }
    }
    if (!l) if (a.data.size > 1) {
      const c = ta(a.data.type), u = Math.max(Ta[a.data.type] / 16, 1), d = c / u, y = (4 - d % 4) % 4;
      i.push(`
                cv = ud.${h}.value;
                v = uv.${h};
                offset = ${a.offset / 4};

                t = 0;

                for(var i=0; i < ${a.data.size * u}; i++)
                {
                    for(var j = 0; j < ${d}; j++)
                    {
                        data[offset++] = v[t++];
                    }
                    offset += ${y};
                }

                `);
    } else {
      const c = hl[a.data.type];
      i.push(`
                cv = ud.${h}.value;
                v = uv.${h};
                offset = ${a.offset / 4};
                ${c};
                `);
    }
  }
  return i.push(`
       renderer.buffer.update(buffer);
    `), {
    size: s,
    syncFunc: new Function("ud", "uv", "renderer", "syncData", "buffer", i.join(`
`))
  };
}
var dl = 0, Pr = {
  textureCount: 0,
  uboCount: 0
}, wa = class {
  constructor(e) {
    this.destroyed = !1, this.renderer = e, this.systemCheck(), this.gl = null, this.shader = null, this.program = null, this.cache = {}, this._uboCache = {}, this.id = dl++;
  }
  systemCheck() {
    if (!Nh()) throw new Error("Current environment does not allow unsafe-eval, please use @pixi/unsafe-eval module to enable support.");
  }
  contextChange(e) {
    this.gl = e, this.reset();
  }
  bind(e, t) {
    e.disposeRunner.add(this), e.uniforms.globals = this.renderer.globalUniforms;
    const r = e.program, s = r.glPrograms[this.renderer.CONTEXT_UID] || this.generateProgram(e);
    return this.shader = e, this.program !== r && (this.program = r, this.gl.useProgram(s.program)), t || (Pr.textureCount = 0, Pr.uboCount = 0, this.syncUniformGroup(e.uniformGroup, Pr)), s;
  }
  setUniforms(e) {
    const t = this.shader.program, r = t.glPrograms[this.renderer.CONTEXT_UID];
    t.syncUniforms(r.uniformData, e, this.renderer);
  }
  syncUniformGroup(e, t) {
    const r = this.getGlProgram();
    (!e.static || e.dirtyId !== r.uniformDirtyGroups[e.id]) && (r.uniformDirtyGroups[e.id] = e.dirtyId, this.syncUniforms(e, r, t));
  }
  syncUniforms(e, t, r) {
    (e.syncUniforms[this.shader.program.id] || this.createSyncGroups(e))(t.uniformData, e.uniforms, this.renderer, r);
  }
  createSyncGroups(e) {
    const t = this.getSignature(e, this.shader.program.uniformData, "u");
    return this.cache[t] || (this.cache[t] = Mh(e, this.shader.program.uniformData)), e.syncUniforms[this.shader.program.id] = this.cache[t], e.syncUniforms[this.shader.program.id];
  }
  syncUniformBufferGroup(e, t) {
    const r = this.getGlProgram();
    if (!e.static || e.dirtyId !== 0 || !r.uniformGroups[e.id]) {
      e.dirtyId = 0;
      const s = r.uniformGroups[e.id] || this.createSyncBufferGroup(e, r, t);
      e.buffer.update(), s(r.uniformData, e.uniforms, this.renderer, Pr, e.buffer);
    }
    this.renderer.buffer.bindBufferBase(e.buffer, r.uniformBufferBindings[t]);
  }
  createSyncBufferGroup(e, t, r) {
    const { gl: s } = this.renderer;
    this.renderer.buffer.bind(e.buffer);
    const i = this.gl.getUniformBlockIndex(t.program, r);
    t.uniformBufferBindings[r] = this.shader.uniformBindCount, s.uniformBlockBinding(t.program, i, this.shader.uniformBindCount), this.shader.uniformBindCount++;
    const n = this.getSignature(e, this.shader.program.uniformData, "ubo");
    let a = this._uboCache[n];
    if (a || (a = this._uboCache[n] = ul(e, this.shader.program.uniformData)), e.autoManage) {
      const o = new Float32Array(a.size / 4);
      e.buffer.update(o);
    }
    return t.uniformGroups[e.id] = a.syncFunc, t.uniformGroups[e.id];
  }
  getSignature(e, t, r) {
    const s = e.uniforms, i = [`${r}-`];
    for (const n in s) i.push(n), t[n] && i.push(t[n].type);
    return i.join("-");
  }
  getGlProgram() {
    return this.shader ? this.shader.program.glPrograms[this.renderer.CONTEXT_UID] : null;
  }
  generateProgram(e) {
    const t = this.gl, r = e.program, s = al(t, r);
    return r.glPrograms[this.renderer.CONTEXT_UID] = s, s;
  }
  reset() {
    this.program = null, this.shader = null;
  }
  disposeShader(e) {
    this.shader === e && (this.shader = null);
  }
  destroy() {
    this.renderer = null, this.destroyed = !0;
  }
};
wa.extension = {
  type: ut.RendererSystem,
  name: "shader"
};
mt.add(wa);
var Kr = class {
  constructor(e) {
    this.renderer = e;
  }
  run(e) {
    const { renderer: t } = this;
    t.runners.init.emit(t.options), e.hello && console.log(`PixiJS 7.4.3 - ${t.rendererLogId} - https://pixijs.com`), t.resize(t.screen.width, t.screen.height);
  }
  destroy() {
  }
};
Kr.defaultOptions = { hello: !1 }, Kr.extension = {
  type: [ut.RendererSystem, ut.CanvasRendererSystem],
  name: "startup"
};
mt.add(Kr);
function fl(e, t = []) {
  return t[st.NORMAL] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.ADD] = [e.ONE, e.ONE], t[st.MULTIPLY] = [
    e.DST_COLOR,
    e.ONE_MINUS_SRC_ALPHA,
    e.ONE,
    e.ONE_MINUS_SRC_ALPHA
  ], t[st.SCREEN] = [
    e.ONE,
    e.ONE_MINUS_SRC_COLOR,
    e.ONE,
    e.ONE_MINUS_SRC_ALPHA
  ], t[st.OVERLAY] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.DARKEN] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.LIGHTEN] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.COLOR_DODGE] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.COLOR_BURN] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.HARD_LIGHT] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.SOFT_LIGHT] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.DIFFERENCE] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.EXCLUSION] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.HUE] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.SATURATION] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.COLOR] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.LUMINOSITY] = [e.ONE, e.ONE_MINUS_SRC_ALPHA], t[st.NONE] = [0, 0], t[st.NORMAL_NPM] = [
    e.SRC_ALPHA,
    e.ONE_MINUS_SRC_ALPHA,
    e.ONE,
    e.ONE_MINUS_SRC_ALPHA
  ], t[st.ADD_NPM] = [
    e.SRC_ALPHA,
    e.ONE,
    e.ONE,
    e.ONE
  ], t[st.SCREEN_NPM] = [
    e.SRC_ALPHA,
    e.ONE_MINUS_SRC_COLOR,
    e.ONE,
    e.ONE_MINUS_SRC_ALPHA
  ], t[st.SRC_IN] = [e.DST_ALPHA, e.ZERO], t[st.SRC_OUT] = [e.ONE_MINUS_DST_ALPHA, e.ZERO], t[st.SRC_ATOP] = [e.DST_ALPHA, e.ONE_MINUS_SRC_ALPHA], t[st.DST_OVER] = [e.ONE_MINUS_DST_ALPHA, e.ONE], t[st.DST_IN] = [e.ZERO, e.SRC_ALPHA], t[st.DST_OUT] = [e.ZERO, e.ONE_MINUS_SRC_ALPHA], t[st.DST_ATOP] = [e.ONE_MINUS_DST_ALPHA, e.SRC_ALPHA], t[st.XOR] = [e.ONE_MINUS_DST_ALPHA, e.ONE_MINUS_SRC_ALPHA], t[st.SUBTRACT] = [
    e.ONE,
    e.ONE,
    e.ONE,
    e.ONE,
    e.FUNC_REVERSE_SUBTRACT,
    e.FUNC_ADD
  ], t;
}
var pl = 0, ml = 1, yl = 2, gl = 3, vl = 4, xl = 5, Ea = class zs {
  constructor() {
    this.gl = null, this.stateId = 0, this.polygonOffset = 0, this.blendMode = st.NONE, this._blendEq = !1, this.map = [], this.map[pl] = this.setBlend, this.map[ml] = this.setOffset, this.map[yl] = this.setCullFace, this.map[gl] = this.setDepthTest, this.map[vl] = this.setFrontFace, this.map[xl] = this.setDepthMask, this.checks = [], this.defaultState = new ss(), this.defaultState.blend = !0;
  }
  contextChange(t) {
    this.gl = t, this.blendModes = fl(t), this.set(this.defaultState), this.reset();
  }
  set(t) {
    if (t = t || this.defaultState, this.stateId !== t.data) {
      let r = this.stateId ^ t.data, s = 0;
      for (; r; ) r & 1 && this.map[s].call(this, !!(t.data & 1 << s)), r = r >> 1, s++;
      this.stateId = t.data;
    }
    for (let r = 0; r < this.checks.length; r++) this.checks[r](this, t);
  }
  forceState(t) {
    t = t || this.defaultState;
    for (let r = 0; r < this.map.length; r++) this.map[r].call(this, !!(t.data & 1 << r));
    for (let r = 0; r < this.checks.length; r++) this.checks[r](this, t);
    this.stateId = t.data;
  }
  setBlend(t) {
    this.updateCheck(zs.checkBlendMode, t), this.gl[t ? "enable" : "disable"](this.gl.BLEND);
  }
  setOffset(t) {
    this.updateCheck(zs.checkPolygonOffset, t), this.gl[t ? "enable" : "disable"](this.gl.POLYGON_OFFSET_FILL);
  }
  setDepthTest(t) {
    this.gl[t ? "enable" : "disable"](this.gl.DEPTH_TEST);
  }
  setDepthMask(t) {
    this.gl.depthMask(t);
  }
  setCullFace(t) {
    this.gl[t ? "enable" : "disable"](this.gl.CULL_FACE);
  }
  setFrontFace(t) {
    this.gl.frontFace(this.gl[t ? "CW" : "CCW"]);
  }
  setBlendMode(t) {
    if (t === this.blendMode) return;
    this.blendMode = t;
    const r = this.blendModes[t], s = this.gl;
    r.length === 2 ? s.blendFunc(r[0], r[1]) : s.blendFuncSeparate(r[0], r[1], r[2], r[3]), r.length === 6 ? (this._blendEq = !0, s.blendEquationSeparate(r[4], r[5])) : this._blendEq && (this._blendEq = !1, s.blendEquationSeparate(s.FUNC_ADD, s.FUNC_ADD));
  }
  setPolygonOffset(t, r) {
    this.gl.polygonOffset(t, r);
  }
  reset() {
    this.gl.pixelStorei(this.gl.UNPACK_FLIP_Y_WEBGL, !1), this.forceState(this.defaultState), this._blendEq = !0, this.blendMode = -1, this.setBlendMode(0);
  }
  updateCheck(t, r) {
    const s = this.checks.indexOf(t);
    r && s === -1 ? this.checks.push(t) : !r && s !== -1 && this.checks.splice(s, 1);
  }
  static checkBlendMode(t, r) {
    t.setBlendMode(r.blendMode);
  }
  static checkPolygonOffset(t, r) {
    t.setPolygonOffset(1, r.polygonOffset);
  }
  destroy() {
    this.gl = null;
  }
};
Ea.extension = {
  type: ut.RendererSystem,
  name: "state"
};
var _l = Ea;
mt.add(_l);
var bl = class extends rs.default {
  constructor() {
    super(...arguments), this.runners = {}, this._systemsHash = {};
  }
  setup(e) {
    this.addRunners(...e.runners);
    const t = (e.priority ?? []).filter((s) => e.systems[s]), r = [...t, ...Object.keys(e.systems).filter((s) => !t.includes(s))];
    for (const s of r) this.addSystem(e.systems[s], s);
  }
  addRunners(...e) {
    e.forEach((t) => {
      this.runners[t] = new Wt(t);
    });
  }
  addSystem(e, t) {
    const r = new e(this);
    if (this[t]) throw new Error(`Whoops! The name "${t}" is already in use`);
    this[t] = r, this._systemsHash[t] = r;
    for (const s in this.runners) this.runners[s].add(r);
    return this;
  }
  emitWithCustomOptions(e, t) {
    const r = Object.keys(this._systemsHash);
    e.items.forEach((s) => {
      const i = r.find((n) => this._systemsHash[n] === s);
      s[e.name](t[i]);
    });
  }
  destroy() {
    Object.values(this.runners).forEach((e) => {
      e.destroy();
    }), this._systemsHash = {};
  }
}, ar = class $r {
  constructor(t) {
    this.renderer = t, this.count = 0, this.checkCount = 0, this.maxIdle = $r.defaultMaxIdle, this.checkCountMax = $r.defaultCheckCountMax, this.mode = $r.defaultMode;
  }
  postrender() {
    this.renderer.objectRenderer.renderingToScreen && (this.count++, this.mode !== Qs.MANUAL && (this.checkCount++, this.checkCount > this.checkCountMax && (this.checkCount = 0, this.run())));
  }
  run() {
    const t = this.renderer.texture, r = t.managedTextures;
    let s = !1;
    for (let i = 0; i < r.length; i++) {
      const n = r[i];
      n.resource && this.count - n.touched > this.maxIdle && (t.destroyTexture(n, !0), r[i] = null, s = !0);
    }
    if (s) {
      let i = 0;
      for (let n = 0; n < r.length; n++) r[n] !== null && (r[i++] = r[n]);
      r.length = i;
    }
  }
  unload(t) {
    const r = this.renderer.texture, s = t._texture;
    s && !s.framebuffer && r.destroyTexture(s);
    for (let i = t.children.length - 1; i >= 0; i--) this.unload(t.children[i]);
  }
  destroy() {
    this.renderer = null;
  }
};
ar.defaultMode = Qs.AUTO, ar.defaultMaxIdle = 3600, ar.defaultCheckCountMax = 600, ar.extension = {
  type: ut.RendererSystem,
  name: "textureGC"
};
var be = ar;
mt.add(be);
var Ts = class {
  constructor(e) {
    this.texture = e, this.width = -1, this.height = -1, this.dirtyId = -1, this.dirtyStyleId = -1, this.mipmap = !1, this.wrapMode = 33071, this.type = ct.UNSIGNED_BYTE, this.internalFormat = X.RGBA, this.samplerType = 0;
  }
};
function Tl(e) {
  let t;
  return "WebGL2RenderingContext" in globalThis && e instanceof globalThis.WebGL2RenderingContext ? t = {
    [e.RGB]: q.FLOAT,
    [e.RGBA]: q.FLOAT,
    [e.ALPHA]: q.FLOAT,
    [e.LUMINANCE]: q.FLOAT,
    [e.LUMINANCE_ALPHA]: q.FLOAT,
    [e.R8]: q.FLOAT,
    [e.R8_SNORM]: q.FLOAT,
    [e.RG8]: q.FLOAT,
    [e.RG8_SNORM]: q.FLOAT,
    [e.RGB8]: q.FLOAT,
    [e.RGB8_SNORM]: q.FLOAT,
    [e.RGB565]: q.FLOAT,
    [e.RGBA4]: q.FLOAT,
    [e.RGB5_A1]: q.FLOAT,
    [e.RGBA8]: q.FLOAT,
    [e.RGBA8_SNORM]: q.FLOAT,
    [e.RGB10_A2]: q.FLOAT,
    [e.RGB10_A2UI]: q.FLOAT,
    [e.SRGB8]: q.FLOAT,
    [e.SRGB8_ALPHA8]: q.FLOAT,
    [e.R16F]: q.FLOAT,
    [e.RG16F]: q.FLOAT,
    [e.RGB16F]: q.FLOAT,
    [e.RGBA16F]: q.FLOAT,
    [e.R32F]: q.FLOAT,
    [e.RG32F]: q.FLOAT,
    [e.RGB32F]: q.FLOAT,
    [e.RGBA32F]: q.FLOAT,
    [e.R11F_G11F_B10F]: q.FLOAT,
    [e.RGB9_E5]: q.FLOAT,
    [e.R8I]: q.INT,
    [e.R8UI]: q.UINT,
    [e.R16I]: q.INT,
    [e.R16UI]: q.UINT,
    [e.R32I]: q.INT,
    [e.R32UI]: q.UINT,
    [e.RG8I]: q.INT,
    [e.RG8UI]: q.UINT,
    [e.RG16I]: q.INT,
    [e.RG16UI]: q.UINT,
    [e.RG32I]: q.INT,
    [e.RG32UI]: q.UINT,
    [e.RGB8I]: q.INT,
    [e.RGB8UI]: q.UINT,
    [e.RGB16I]: q.INT,
    [e.RGB16UI]: q.UINT,
    [e.RGB32I]: q.INT,
    [e.RGB32UI]: q.UINT,
    [e.RGBA8I]: q.INT,
    [e.RGBA8UI]: q.UINT,
    [e.RGBA16I]: q.INT,
    [e.RGBA16UI]: q.UINT,
    [e.RGBA32I]: q.INT,
    [e.RGBA32UI]: q.UINT,
    [e.DEPTH_COMPONENT16]: q.FLOAT,
    [e.DEPTH_COMPONENT24]: q.FLOAT,
    [e.DEPTH_COMPONENT32F]: q.FLOAT,
    [e.DEPTH_STENCIL]: q.FLOAT,
    [e.DEPTH24_STENCIL8]: q.FLOAT,
    [e.DEPTH32F_STENCIL8]: q.FLOAT
  } : t = {
    [e.RGB]: q.FLOAT,
    [e.RGBA]: q.FLOAT,
    [e.ALPHA]: q.FLOAT,
    [e.LUMINANCE]: q.FLOAT,
    [e.LUMINANCE_ALPHA]: q.FLOAT,
    [e.DEPTH_STENCIL]: q.FLOAT
  }, t;
}
function wl(e) {
  let t;
  return "WebGL2RenderingContext" in globalThis && e instanceof globalThis.WebGL2RenderingContext ? t = {
    [ct.UNSIGNED_BYTE]: {
      [X.RGBA]: e.RGBA8,
      [X.RGB]: e.RGB8,
      [X.RG]: e.RG8,
      [X.RED]: e.R8,
      [X.RGBA_INTEGER]: e.RGBA8UI,
      [X.RGB_INTEGER]: e.RGB8UI,
      [X.RG_INTEGER]: e.RG8UI,
      [X.RED_INTEGER]: e.R8UI,
      [X.ALPHA]: e.ALPHA,
      [X.LUMINANCE]: e.LUMINANCE,
      [X.LUMINANCE_ALPHA]: e.LUMINANCE_ALPHA
    },
    [ct.BYTE]: {
      [X.RGBA]: e.RGBA8_SNORM,
      [X.RGB]: e.RGB8_SNORM,
      [X.RG]: e.RG8_SNORM,
      [X.RED]: e.R8_SNORM,
      [X.RGBA_INTEGER]: e.RGBA8I,
      [X.RGB_INTEGER]: e.RGB8I,
      [X.RG_INTEGER]: e.RG8I,
      [X.RED_INTEGER]: e.R8I
    },
    [ct.UNSIGNED_SHORT]: {
      [X.RGBA_INTEGER]: e.RGBA16UI,
      [X.RGB_INTEGER]: e.RGB16UI,
      [X.RG_INTEGER]: e.RG16UI,
      [X.RED_INTEGER]: e.R16UI,
      [X.DEPTH_COMPONENT]: e.DEPTH_COMPONENT16
    },
    [ct.SHORT]: {
      [X.RGBA_INTEGER]: e.RGBA16I,
      [X.RGB_INTEGER]: e.RGB16I,
      [X.RG_INTEGER]: e.RG16I,
      [X.RED_INTEGER]: e.R16I
    },
    [ct.UNSIGNED_INT]: {
      [X.RGBA_INTEGER]: e.RGBA32UI,
      [X.RGB_INTEGER]: e.RGB32UI,
      [X.RG_INTEGER]: e.RG32UI,
      [X.RED_INTEGER]: e.R32UI,
      [X.DEPTH_COMPONENT]: e.DEPTH_COMPONENT24
    },
    [ct.INT]: {
      [X.RGBA_INTEGER]: e.RGBA32I,
      [X.RGB_INTEGER]: e.RGB32I,
      [X.RG_INTEGER]: e.RG32I,
      [X.RED_INTEGER]: e.R32I
    },
    [ct.FLOAT]: {
      [X.RGBA]: e.RGBA32F,
      [X.RGB]: e.RGB32F,
      [X.RG]: e.RG32F,
      [X.RED]: e.R32F,
      [X.DEPTH_COMPONENT]: e.DEPTH_COMPONENT32F
    },
    [ct.HALF_FLOAT]: {
      [X.RGBA]: e.RGBA16F,
      [X.RGB]: e.RGB16F,
      [X.RG]: e.RG16F,
      [X.RED]: e.R16F
    },
    [ct.UNSIGNED_SHORT_5_6_5]: { [X.RGB]: e.RGB565 },
    [ct.UNSIGNED_SHORT_4_4_4_4]: { [X.RGBA]: e.RGBA4 },
    [ct.UNSIGNED_SHORT_5_5_5_1]: { [X.RGBA]: e.RGB5_A1 },
    [ct.UNSIGNED_INT_2_10_10_10_REV]: {
      [X.RGBA]: e.RGB10_A2,
      [X.RGBA_INTEGER]: e.RGB10_A2UI
    },
    [ct.UNSIGNED_INT_10F_11F_11F_REV]: { [X.RGB]: e.R11F_G11F_B10F },
    [ct.UNSIGNED_INT_5_9_9_9_REV]: { [X.RGB]: e.RGB9_E5 },
    [ct.UNSIGNED_INT_24_8]: { [X.DEPTH_STENCIL]: e.DEPTH24_STENCIL8 },
    [ct.FLOAT_32_UNSIGNED_INT_24_8_REV]: { [X.DEPTH_STENCIL]: e.DEPTH32F_STENCIL8 }
  } : t = {
    [ct.UNSIGNED_BYTE]: {
      [X.RGBA]: e.RGBA,
      [X.RGB]: e.RGB,
      [X.ALPHA]: e.ALPHA,
      [X.LUMINANCE]: e.LUMINANCE,
      [X.LUMINANCE_ALPHA]: e.LUMINANCE_ALPHA
    },
    [ct.UNSIGNED_SHORT_5_6_5]: { [X.RGB]: e.RGB },
    [ct.UNSIGNED_SHORT_4_4_4_4]: { [X.RGBA]: e.RGBA },
    [ct.UNSIGNED_SHORT_5_5_5_1]: { [X.RGBA]: e.RGBA }
  }, t;
}
var Sa = class {
  constructor(e) {
    this.renderer = e, this.boundTextures = [], this.currentLocation = -1, this.managedTextures = [], this._unknownBoundTextures = !1, this.unknownTexture = new vt(), this.hasIntegerTextures = !1;
  }
  contextChange() {
    const e = this.gl = this.renderer.gl;
    this.CONTEXT_UID = this.renderer.CONTEXT_UID, this.webGLVersion = this.renderer.context.webGLVersion, this.internalFormats = wl(e), this.samplerTypes = Tl(e);
    const t = e.getParameter(e.MAX_TEXTURE_IMAGE_UNITS);
    this.boundTextures.length = t;
    for (let s = 0; s < t; s++) this.boundTextures[s] = null;
    this.emptyTextures = {};
    const r = new Ts(e.createTexture());
    e.bindTexture(e.TEXTURE_2D, r.texture), e.texImage2D(e.TEXTURE_2D, 0, e.RGBA, 1, 1, 0, e.RGBA, e.UNSIGNED_BYTE, /* @__PURE__ */ new Uint8Array(4)), this.emptyTextures[e.TEXTURE_2D] = r, this.emptyTextures[e.TEXTURE_CUBE_MAP] = new Ts(e.createTexture()), e.bindTexture(e.TEXTURE_CUBE_MAP, this.emptyTextures[e.TEXTURE_CUBE_MAP].texture);
    for (let s = 0; s < 6; s++) e.texImage2D(e.TEXTURE_CUBE_MAP_POSITIVE_X + s, 0, e.RGBA, 1, 1, 0, e.RGBA, e.UNSIGNED_BYTE, null);
    e.texParameteri(e.TEXTURE_CUBE_MAP, e.TEXTURE_MAG_FILTER, e.LINEAR), e.texParameteri(e.TEXTURE_CUBE_MAP, e.TEXTURE_MIN_FILTER, e.LINEAR);
    for (let s = 0; s < this.boundTextures.length; s++) this.bind(null, s);
  }
  bind(e, t = 0) {
    const { gl: r } = this;
    if (e = e?.castToBaseTexture(), e?.valid && !e.parentTextureArray) {
      e.touched = this.renderer.textureGC.count;
      const s = e._glTextures[this.CONTEXT_UID] || this.initTexture(e);
      this.boundTextures[t] !== e && (this.currentLocation !== t && (this.currentLocation = t, r.activeTexture(r.TEXTURE0 + t)), r.bindTexture(e.target, s.texture)), s.dirtyId !== e.dirtyId ? (this.currentLocation !== t && (this.currentLocation = t, r.activeTexture(r.TEXTURE0 + t)), this.updateTexture(e)) : s.dirtyStyleId !== e.dirtyStyleId && this.updateTextureStyle(e), this.boundTextures[t] = e;
    } else this.currentLocation !== t && (this.currentLocation = t, r.activeTexture(r.TEXTURE0 + t)), r.bindTexture(r.TEXTURE_2D, this.emptyTextures[r.TEXTURE_2D].texture), this.boundTextures[t] = null;
  }
  reset() {
    this._unknownBoundTextures = !0, this.hasIntegerTextures = !1, this.currentLocation = -1;
    for (let e = 0; e < this.boundTextures.length; e++) this.boundTextures[e] = this.unknownTexture;
  }
  unbind(e) {
    const { gl: t, boundTextures: r } = this;
    if (this._unknownBoundTextures) {
      this._unknownBoundTextures = !1;
      for (let s = 0; s < r.length; s++) r[s] === this.unknownTexture && this.bind(null, s);
    }
    for (let s = 0; s < r.length; s++) r[s] === e && (this.currentLocation !== s && (t.activeTexture(t.TEXTURE0 + s), this.currentLocation = s), t.bindTexture(e.target, this.emptyTextures[e.target].texture), r[s] = null);
  }
  ensureSamplerType(e) {
    const { boundTextures: t, hasIntegerTextures: r, CONTEXT_UID: s } = this;
    if (r) for (let i = e - 1; i >= 0; --i) {
      const n = t[i];
      n && n._glTextures[s].samplerType !== q.FLOAT && this.renderer.texture.unbind(n);
    }
  }
  initTexture(e) {
    const t = new Ts(this.gl.createTexture());
    return t.dirtyId = -1, e._glTextures[this.CONTEXT_UID] = t, this.managedTextures.push(e), e.on("dispose", this.destroyTexture, this), t;
  }
  initTextureType(e, t) {
    t.internalFormat = this.internalFormats[e.type]?.[e.format] ?? e.format, t.samplerType = this.samplerTypes[t.internalFormat] ?? q.FLOAT, this.webGLVersion === 2 && e.type === ct.HALF_FLOAT ? t.type = this.gl.HALF_FLOAT : t.type = e.type;
  }
  updateTexture(e) {
    const t = e._glTextures[this.CONTEXT_UID];
    if (!t) return;
    const r = this.renderer;
    if (this.initTextureType(e, t), e.resource?.upload(r, e, t)) t.samplerType !== q.FLOAT && (this.hasIntegerTextures = !0);
    else {
      const s = e.realWidth, i = e.realHeight, n = r.gl;
      (t.width !== s || t.height !== i || t.dirtyId < 0) && (t.width = s, t.height = i, n.texImage2D(e.target, 0, t.internalFormat, s, i, 0, e.format, t.type, null));
    }
    e.dirtyStyleId !== t.dirtyStyleId && this.updateTextureStyle(e), t.dirtyId = e.dirtyId;
  }
  destroyTexture(e, t) {
    const { gl: r } = this;
    if (e = e.castToBaseTexture(), e._glTextures[this.CONTEXT_UID] && (this.unbind(e), r.deleteTexture(e._glTextures[this.CONTEXT_UID].texture), e.off("dispose", this.destroyTexture, this), delete e._glTextures[this.CONTEXT_UID], !t)) {
      const s = this.managedTextures.indexOf(e);
      s !== -1 && kr(this.managedTextures, s, 1);
    }
  }
  updateTextureStyle(e) {
    const t = e._glTextures[this.CONTEXT_UID];
    t && ((e.mipmap === Se.POW2 || this.webGLVersion !== 2) && !e.isPowerOfTwo ? t.mipmap = !1 : t.mipmap = e.mipmap >= 1, this.webGLVersion !== 2 && !e.isPowerOfTwo ? t.wrapMode = Js.CLAMP : t.wrapMode = e.wrapMode, e.resource?.style(this.renderer, e, t) || this.setStyle(e, t), t.dirtyStyleId = e.dirtyStyleId);
  }
  setStyle(e, t) {
    const r = this.gl;
    if (t.mipmap && e.mipmap !== Se.ON_MANUAL && r.generateMipmap(e.target), r.texParameteri(e.target, r.TEXTURE_WRAP_S, t.wrapMode), r.texParameteri(e.target, r.TEXTURE_WRAP_T, t.wrapMode), t.mipmap) {
      r.texParameteri(e.target, r.TEXTURE_MIN_FILTER, e.scaleMode === te.LINEAR ? r.LINEAR_MIPMAP_LINEAR : r.NEAREST_MIPMAP_NEAREST);
      const s = this.renderer.context.extensions.anisotropicFiltering;
      if (s && e.anisotropicLevel > 0 && e.scaleMode === te.LINEAR) {
        const i = Math.min(e.anisotropicLevel, r.getParameter(s.MAX_TEXTURE_MAX_ANISOTROPY_EXT));
        r.texParameterf(e.target, s.TEXTURE_MAX_ANISOTROPY_EXT, i);
      }
    } else r.texParameteri(e.target, r.TEXTURE_MIN_FILTER, e.scaleMode === te.LINEAR ? r.LINEAR : r.NEAREST);
    r.texParameteri(e.target, r.TEXTURE_MAG_FILTER, e.scaleMode === te.LINEAR ? r.LINEAR : r.NEAREST);
  }
  destroy() {
    this.renderer = null;
  }
};
Sa.extension = {
  type: ut.RendererSystem,
  name: "texture"
};
mt.add(Sa);
var Aa = class {
  constructor(e) {
    this.renderer = e;
  }
  contextChange() {
    this.gl = this.renderer.gl, this.CONTEXT_UID = this.renderer.CONTEXT_UID;
  }
  bind(e) {
    const { gl: t, CONTEXT_UID: r } = this, s = e._glTransformFeedbacks[r] || this.createGLTransformFeedback(e);
    t.bindTransformFeedback(t.TRANSFORM_FEEDBACK, s);
  }
  unbind() {
    const { gl: e } = this;
    e.bindTransformFeedback(e.TRANSFORM_FEEDBACK, null);
  }
  beginTransformFeedback(e, t) {
    const { gl: r, renderer: s } = this;
    t && s.shader.bind(t), r.beginTransformFeedback(e);
  }
  endTransformFeedback() {
    const { gl: e } = this;
    e.endTransformFeedback();
  }
  createGLTransformFeedback(e) {
    const { gl: t, renderer: r, CONTEXT_UID: s } = this, i = t.createTransformFeedback();
    e._glTransformFeedbacks[s] = i, t.bindTransformFeedback(t.TRANSFORM_FEEDBACK, i);
    for (let n = 0; n < e.buffers.length; n++) {
      const a = e.buffers[n];
      a && (r.buffer.update(a), a._glBuffers[s].refCount++, t.bindBufferBase(t.TRANSFORM_FEEDBACK_BUFFER, n, a._glBuffers[s].buffer || null));
    }
    return t.bindTransformFeedback(t.TRANSFORM_FEEDBACK, null), e.disposeRunner.add(this), i;
  }
  disposeTransformFeedback(e, t) {
    const r = e._glTransformFeedbacks[this.CONTEXT_UID], s = this.gl;
    e.disposeRunner.remove(this);
    const i = this.renderer.buffer;
    if (i) for (let n = 0; n < e.buffers.length; n++) {
      const a = e.buffers[n];
      if (!a) continue;
      const o = a._glBuffers[this.CONTEXT_UID];
      o && (o.refCount--, o.refCount === 0 && !t && i.dispose(a, t));
    }
    r && (t || s.deleteTransformFeedback(r), delete e._glTransformFeedbacks[this.CONTEXT_UID]);
  }
  destroy() {
    this.renderer = null;
  }
};
Aa.extension = {
  type: ut.RendererSystem,
  name: "transformFeedback"
};
mt.add(Aa);
var Zr = class {
  constructor(e) {
    this.renderer = e;
  }
  init(e) {
    this.screen = new Et(0, 0, e.width, e.height), this.element = e.view || dt.ADAPTER.createCanvas(), this.resolution = e.resolution || dt.RESOLUTION, this.autoDensity = !!e.autoDensity;
  }
  resizeView(e, t) {
    this.element.width = Math.round(e * this.resolution), this.element.height = Math.round(t * this.resolution);
    const r = this.element.width / this.resolution, s = this.element.height / this.resolution;
    this.screen.width = r, this.screen.height = s, this.autoDensity && (this.element.style.width = `${r}px`, this.element.style.height = `${s}px`), this.renderer.emit("resize", r, s), this.renderer.runners.resize.emit(this.screen.width, this.screen.height);
  }
  destroy(e) {
    e && this.element.parentNode?.removeChild(this.element), this.renderer = null, this.element = null, this.screen = null;
  }
};
Zr.defaultOptions = {
  width: 800,
  height: 600,
  resolution: void 0,
  autoDensity: !1
}, Zr.extension = {
  type: [ut.RendererSystem, ut.CanvasRendererSystem],
  name: "_view"
};
mt.add(Zr);
dt.PREFER_ENV = Me.WEBGL2;
dt.STRICT_TEXTURE_CACHE = !1;
dt.RENDER_OPTIONS = {
  ...Yr.defaultOptions,
  ...jr.defaultOptions,
  ...Zr.defaultOptions,
  ...Kr.defaultOptions
};
Object.defineProperties(dt, {
  WRAP_MODE: {
    get() {
      return vt.defaultOptions.wrapMode;
    },
    set(e) {
      yt("7.1.0", "settings.WRAP_MODE is deprecated, use BaseTexture.defaultOptions.wrapMode"), vt.defaultOptions.wrapMode = e;
    }
  },
  SCALE_MODE: {
    get() {
      return vt.defaultOptions.scaleMode;
    },
    set(e) {
      yt("7.1.0", "settings.SCALE_MODE is deprecated, use BaseTexture.defaultOptions.scaleMode"), vt.defaultOptions.scaleMode = e;
    }
  },
  MIPMAP_TEXTURES: {
    get() {
      return vt.defaultOptions.mipmap;
    },
    set(e) {
      yt("7.1.0", "settings.MIPMAP_TEXTURES is deprecated, use BaseTexture.defaultOptions.mipmap"), vt.defaultOptions.mipmap = e;
    }
  },
  ANISOTROPIC_LEVEL: {
    get() {
      return vt.defaultOptions.anisotropicLevel;
    },
    set(e) {
      yt("7.1.0", "settings.ANISOTROPIC_LEVEL is deprecated, use BaseTexture.defaultOptions.anisotropicLevel"), vt.defaultOptions.anisotropicLevel = e;
    }
  },
  FILTER_RESOLUTION: {
    get() {
      return yt("7.1.0", "settings.FILTER_RESOLUTION is deprecated, use Filter.defaultResolution"), Gt.defaultResolution;
    },
    set(e) {
      Gt.defaultResolution = e;
    }
  },
  FILTER_MULTISAMPLE: {
    get() {
      return yt("7.1.0", "settings.FILTER_MULTISAMPLE is deprecated, use Filter.defaultMultisample"), Gt.defaultMultisample;
    },
    set(e) {
      Gt.defaultMultisample = e;
    }
  },
  SPRITE_MAX_TEXTURES: {
    get() {
      return _e.defaultMaxTextures;
    },
    set(e) {
      yt("7.1.0", "settings.SPRITE_MAX_TEXTURES is deprecated, use BatchRenderer.defaultMaxTextures"), _e.defaultMaxTextures = e;
    }
  },
  SPRITE_BATCH_SIZE: {
    get() {
      return _e.defaultBatchSize;
    },
    set(e) {
      yt("7.1.0", "settings.SPRITE_BATCH_SIZE is deprecated, use BatchRenderer.defaultBatchSize"), _e.defaultBatchSize = e;
    }
  },
  CAN_UPLOAD_SAME_BUFFER: {
    get() {
      return _e.canUploadSameBuffer;
    },
    set(e) {
      yt("7.1.0", "settings.CAN_UPLOAD_SAME_BUFFER is deprecated, use BatchRenderer.canUploadSameBuffer"), _e.canUploadSameBuffer = e;
    }
  },
  GC_MODE: {
    get() {
      return be.defaultMode;
    },
    set(e) {
      yt("7.1.0", "settings.GC_MODE is deprecated, use TextureGCSystem.defaultMode"), be.defaultMode = e;
    }
  },
  GC_MAX_IDLE: {
    get() {
      return be.defaultMaxIdle;
    },
    set(e) {
      yt("7.1.0", "settings.GC_MAX_IDLE is deprecated, use TextureGCSystem.defaultMaxIdle"), be.defaultMaxIdle = e;
    }
  },
  GC_MAX_CHECK_COUNT: {
    get() {
      return be.defaultCheckCountMax;
    },
    set(e) {
      yt("7.1.0", "settings.GC_MAX_CHECK_COUNT is deprecated, use TextureGCSystem.defaultCheckCountMax"), be.defaultCheckCountMax = e;
    }
  },
  PRECISION_VERTEX: {
    get() {
      return Ee.defaultVertexPrecision;
    },
    set(e) {
      yt("7.1.0", "settings.PRECISION_VERTEX is deprecated, use Program.defaultVertexPrecision"), Ee.defaultVertexPrecision = e;
    }
  },
  PRECISION_FRAGMENT: {
    get() {
      return Ee.defaultFragmentPrecision;
    },
    set(e) {
      yt("7.1.0", "settings.PRECISION_FRAGMENT is deprecated, use Program.defaultFragmentPrecision"), Ee.defaultFragmentPrecision = e;
    }
  }
});
var Jr = /* @__PURE__ */ ((e) => (e[e.INTERACTION = 50] = "INTERACTION", e[e.HIGH = 25] = "HIGH", e[e.NORMAL = 0] = "NORMAL", e[e.LOW = -25] = "LOW", e[e.UTILITY = -50] = "UTILITY", e))(Jr || {}), ws = class {
  constructor(e, t = null, r = 0, s = !1) {
    this.next = null, this.previous = null, this._destroyed = !1, this.fn = e, this.context = t, this.priority = r, this.once = s;
  }
  match(e, t = null) {
    return this.fn === e && this.context === t;
  }
  emit(e) {
    this.fn && (this.context ? this.fn.call(this.context, e) : this.fn(e));
    const t = this.next;
    return this.once && this.destroy(!0), this._destroyed && (this.next = null), t;
  }
  connect(e) {
    this.previous = e, e.next && (e.next.previous = this), this.next = e.next, e.next = this;
  }
  destroy(e = !1) {
    this._destroyed = !0, this.fn = null, this.context = null, this.previous && (this.previous.next = this.next), this.next && (this.next.previous = this.previous);
    const t = this.next;
    return this.next = e ? null : t, this.previous = null, t;
  }
}, Ia = class Bt {
  constructor() {
    this.autoStart = !1, this.deltaTime = 1, this.lastTime = -1, this.speed = 1, this.started = !1, this._requestId = null, this._maxElapsedMS = 100, this._minElapsedMS = 0, this._protected = !1, this._lastFrame = -1, this._head = new ws(null, null, 1 / 0), this.deltaMS = 1 / Bt.targetFPMS, this.elapsedMS = 1 / Bt.targetFPMS, this._tick = (t) => {
      this._requestId = null, this.started && (this.update(t), this.started && this._requestId === null && this._head.next && (this._requestId = requestAnimationFrame(this._tick)));
    };
  }
  _requestIfNeeded() {
    this._requestId === null && this._head.next && (this.lastTime = performance.now(), this._lastFrame = this.lastTime, this._requestId = requestAnimationFrame(this._tick));
  }
  _cancelIfNeeded() {
    this._requestId !== null && (cancelAnimationFrame(this._requestId), this._requestId = null);
  }
  _startIfPossible() {
    this.started ? this._requestIfNeeded() : this.autoStart && this.start();
  }
  add(t, r, s = Jr.NORMAL) {
    return this._addListener(new ws(t, r, s));
  }
  addOnce(t, r, s = Jr.NORMAL) {
    return this._addListener(new ws(t, r, s, !0));
  }
  _addListener(t) {
    let r = this._head.next, s = this._head;
    if (!r) t.connect(s);
    else {
      for (; r; ) {
        if (t.priority > r.priority) {
          t.connect(s);
          break;
        }
        s = r, r = r.next;
      }
      t.previous || t.connect(s);
    }
    return this._startIfPossible(), this;
  }
  remove(t, r) {
    let s = this._head.next;
    for (; s; ) s.match(t, r) ? s = s.destroy() : s = s.next;
    return this._head.next || this._cancelIfNeeded(), this;
  }
  get count() {
    if (!this._head) return 0;
    let t = 0, r = this._head;
    for (; r = r.next; ) t++;
    return t;
  }
  start() {
    this.started || (this.started = !0, this._requestIfNeeded());
  }
  stop() {
    this.started && (this.started = !1, this._cancelIfNeeded());
  }
  destroy() {
    if (!this._protected) {
      this.stop();
      let t = this._head.next;
      for (; t; ) t = t.destroy(!0);
      this._head.destroy(), this._head = null;
    }
  }
  update(t = performance.now()) {
    let r;
    if (t > this.lastTime) {
      if (r = this.elapsedMS = t - this.lastTime, r > this._maxElapsedMS && (r = this._maxElapsedMS), r *= this.speed, this._minElapsedMS) {
        const n = t - this._lastFrame | 0;
        if (n < this._minElapsedMS) return;
        this._lastFrame = t - n % this._minElapsedMS;
      }
      this.deltaMS = r, this.deltaTime = this.deltaMS * Bt.targetFPMS;
      const s = this._head;
      let i = s.next;
      for (; i; ) i = i.emit(this.deltaTime);
      s.next || this._cancelIfNeeded();
    } else this.deltaTime = this.deltaMS = this.elapsedMS = 0;
    this.lastTime = t;
  }
  get FPS() {
    return 1e3 / this.elapsedMS;
  }
  get minFPS() {
    return 1e3 / this._maxElapsedMS;
  }
  set minFPS(t) {
    const r = Math.min(this.maxFPS, t), s = Math.min(Math.max(0, r) / 1e3, Bt.targetFPMS);
    this._maxElapsedMS = 1 / s;
  }
  get maxFPS() {
    return this._minElapsedMS ? Math.round(1e3 / this._minElapsedMS) : 0;
  }
  set maxFPS(t) {
    if (t === 0) this._minElapsedMS = 0;
    else {
      const r = Math.max(this.minFPS, t);
      this._minElapsedMS = 1 / (r / 1e3);
    }
  }
  static get shared() {
    if (!Bt._shared) {
      const t = Bt._shared = new Bt();
      t.autoStart = !0, t._protected = !0;
    }
    return Bt._shared;
  }
  static get system() {
    if (!Bt._system) {
      const t = Bt._system = new Bt();
      t.autoStart = !0, t._protected = !0;
    }
    return Bt._system;
  }
};
Ia.targetFPMS = 0.06;
var ce = Ia;
Object.defineProperties(dt, { TARGET_FPMS: {
  get() {
    return ce.targetFPMS;
  },
  set(e) {
    yt("7.1.0", "settings.TARGET_FPMS is deprecated, use Ticker.targetFPMS"), ce.targetFPMS = e;
  }
} });
var Ra = class {
  static init(e) {
    e = Object.assign({
      autoStart: !0,
      sharedTicker: !1
    }, e), Object.defineProperty(this, "ticker", {
      set(t) {
        this._ticker && this._ticker.remove(this.render, this), this._ticker = t, t && t.add(this.render, this, Jr.LOW);
      },
      get() {
        return this._ticker;
      }
    }), this.stop = () => {
      this._ticker.stop();
    }, this.start = () => {
      this._ticker.start();
    }, this._ticker = null, this.ticker = e.sharedTicker ? ce.shared : new ce(), e.autoStart && this.start();
  }
  static destroy() {
    if (this._ticker) {
      const e = this._ticker;
      this.ticker = null, e.destroy();
    }
  }
};
Ra.extension = ut.Application;
mt.add(Ra);
var Ma = [];
mt.handleByList(ut.Renderer, Ma);
function El(e) {
  for (const t of Ma) if (t.test(e)) return new t(e);
  throw new Error("Unable to auto-detect a suitable renderer.");
}
var Sl = `attribute vec2 aVertexPosition;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

uniform vec4 inputSize;
uniform vec4 outputFrame;

vec4 filterVertexPosition( void )
{
    vec2 position = aVertexPosition * max(outputFrame.zw, vec2(0.)) + outputFrame.xy;

    return vec4((projectionMatrix * vec3(position, 1.0)).xy, 0.0, 1.0);
}

vec2 filterTextureCoord( void )
{
    return aVertexPosition * (outputFrame.zw * inputSize.zw);
}

void main(void)
{
    gl_Position = filterVertexPosition();
    vTextureCoord = filterTextureCoord();
}
`, Al = Sl, Ca = class {
  constructor(e) {
    this.renderer = e;
  }
  contextChange(e) {
    let t;
    if (this.renderer.context.webGLVersion === 1) {
      const r = e.getParameter(e.FRAMEBUFFER_BINDING);
      e.bindFramebuffer(e.FRAMEBUFFER, null), t = e.getParameter(e.SAMPLES), e.bindFramebuffer(e.FRAMEBUFFER, r);
    } else {
      const r = e.getParameter(e.DRAW_FRAMEBUFFER_BINDING);
      e.bindFramebuffer(e.DRAW_FRAMEBUFFER, null), t = e.getParameter(e.SAMPLES), e.bindFramebuffer(e.DRAW_FRAMEBUFFER, r);
    }
    t >= Mt.HIGH ? this.multisample = Mt.HIGH : t >= Mt.MEDIUM ? this.multisample = Mt.MEDIUM : t >= Mt.LOW ? this.multisample = Mt.LOW : this.multisample = Mt.NONE;
  }
  destroy() {
  }
};
Ca.extension = {
  type: ut.RendererSystem,
  name: "_multisample"
};
mt.add(Ca);
var Il = class {
  constructor(e) {
    this.buffer = e || null, this.updateID = -1, this.byteLength = -1, this.refCount = 0;
  }
}, Pa = class {
  constructor(e) {
    this.renderer = e, this.managedBuffers = {}, this.boundBufferBases = {};
  }
  destroy() {
    this.renderer = null;
  }
  contextChange() {
    this.disposeAll(!0), this.gl = this.renderer.gl, this.CONTEXT_UID = this.renderer.CONTEXT_UID;
  }
  bind(e) {
    const { gl: t, CONTEXT_UID: r } = this, s = e._glBuffers[r] || this.createGLBuffer(e);
    t.bindBuffer(e.type, s.buffer);
  }
  unbind(e) {
    const { gl: t } = this;
    t.bindBuffer(e, null);
  }
  bindBufferBase(e, t) {
    const { gl: r, CONTEXT_UID: s } = this;
    if (this.boundBufferBases[t] !== e) {
      const i = e._glBuffers[s] || this.createGLBuffer(e);
      this.boundBufferBases[t] = e, r.bindBufferBase(r.UNIFORM_BUFFER, t, i.buffer);
    }
  }
  bindBufferRange(e, t, r) {
    const { gl: s, CONTEXT_UID: i } = this;
    r = r || 0;
    const n = e._glBuffers[i] || this.createGLBuffer(e);
    s.bindBufferRange(s.UNIFORM_BUFFER, t || 0, n.buffer, r * 256, 256);
  }
  update(e) {
    const { gl: t, CONTEXT_UID: r } = this, s = e._glBuffers[r] || this.createGLBuffer(e);
    if (e._updateID !== s.updateID) if (s.updateID = e._updateID, t.bindBuffer(e.type, s.buffer), s.byteLength >= e.data.byteLength) t.bufferSubData(e.type, 0, e.data);
    else {
      const i = e.static ? t.STATIC_DRAW : t.DYNAMIC_DRAW;
      s.byteLength = e.data.byteLength, t.bufferData(e.type, e.data, i);
    }
  }
  dispose(e, t) {
    if (!this.managedBuffers[e.id]) return;
    delete this.managedBuffers[e.id];
    const r = e._glBuffers[this.CONTEXT_UID], s = this.gl;
    e.disposeRunner.remove(this), r && (t || s.deleteBuffer(r.buffer), delete e._glBuffers[this.CONTEXT_UID]);
  }
  disposeAll(e) {
    const t = Object.keys(this.managedBuffers);
    for (let r = 0; r < t.length; r++) this.dispose(this.managedBuffers[t[r]], e);
  }
  createGLBuffer(e) {
    const { CONTEXT_UID: t, gl: r } = this;
    return e._glBuffers[t] = new Il(r.createBuffer()), this.managedBuffers[e.id] = e, e.disposeRunner.add(this), e._glBuffers[t];
  }
};
Pa.extension = {
  type: ut.RendererSystem,
  name: "buffer"
};
mt.add(Pa);
var La = class {
  constructor(e) {
    this.renderer = e;
  }
  render(e, t) {
    const r = this.renderer;
    let s, i, n, a;
    if (t && (s = t.renderTexture, i = t.clear, n = t.transform, a = t.skipUpdateTransform), this.renderingToScreen = !s, r.runners.prerender.emit(), r.emit("prerender"), r.projection.transform = n, !r.context.isLost) {
      if (s || (this.lastObjectRendered = e), !a) {
        const o = e.enableTempParent();
        e.updateTransform(), e.disableTempParent(o);
      }
      r.renderTexture.bind(s), r.batch.currentRenderer.start(), (i ?? r.background.clearBeforeRender) && r.renderTexture.clear(), e.render(r), r.batch.currentRenderer.flush(), s && (t.blit && r.framebuffer.blit(), s.baseTexture.update()), r.runners.postrender.emit(), r.projection.transform = null, r.emit("postrender");
    }
  }
  destroy() {
    this.renderer = null, this.lastObjectRendered = null;
  }
};
La.extension = {
  type: ut.RendererSystem,
  name: "objectRenderer"
};
mt.add(La);
var Vr = class $s extends bl {
  constructor(t) {
    super(), this.type = En.WEBGL, t = Object.assign({}, dt.RENDER_OPTIONS, t), this.gl = null, this.CONTEXT_UID = 0, this.globalUniforms = new Ye({ projectionMatrix: new Lt() }, !0);
    const r = {
      runners: [
        "init",
        "destroy",
        "contextChange",
        "resolutionChange",
        "reset",
        "update",
        "postrender",
        "prerender",
        "resize"
      ],
      systems: $s.__systems,
      priority: [
        "_view",
        "textureGenerator",
        "background",
        "_plugin",
        "startup",
        "context",
        "state",
        "texture",
        "buffer",
        "geometry",
        "framebuffer",
        "transformFeedback",
        "mask",
        "scissor",
        "stencil",
        "projection",
        "textureGC",
        "filter",
        "renderTexture",
        "batch",
        "objectRenderer",
        "_multisample"
      ]
    };
    this.setup(r), "useContextAlpha" in t && (yt("7.0.0", "options.useContextAlpha is deprecated, use options.premultipliedAlpha and options.backgroundAlpha instead"), t.premultipliedAlpha = t.useContextAlpha && t.useContextAlpha !== "notMultiplied", t.backgroundAlpha = t.useContextAlpha === !1 ? 1 : t.backgroundAlpha), this._plugin.rendererPlugins = $s.__plugins, this.options = t, this.startup.run(this.options);
  }
  static test(t) {
    return t?.forceCanvas ? !1 : Xo();
  }
  render(t, r) {
    this.objectRenderer.render(t, r);
  }
  resize(t, r) {
    this._view.resizeView(t, r);
  }
  reset() {
    return this.runners.reset.emit(), this;
  }
  clear() {
    this.renderTexture.bind(), this.renderTexture.clear();
  }
  destroy(t = !1) {
    this.runners.destroy.items.reverse(), this.emitWithCustomOptions(this.runners.destroy, { _view: t }), super.destroy();
  }
  get plugins() {
    return this._plugin.plugins;
  }
  get multisample() {
    return this._multisample.multisample;
  }
  get width() {
    return this._view.element.width;
  }
  get height() {
    return this._view.element.height;
  }
  get resolution() {
    return this._view.resolution;
  }
  set resolution(t) {
    this._view.resolution = t, this.runners.resolutionChange.emit(t);
  }
  get autoDensity() {
    return this._view.autoDensity;
  }
  get view() {
    return this._view.element;
  }
  get screen() {
    return this._view.screen;
  }
  get lastObjectRendered() {
    return this.objectRenderer.lastObjectRendered;
  }
  get renderingToScreen() {
    return this.objectRenderer.renderingToScreen;
  }
  get rendererLogId() {
    return `WebGL ${this.context.webGLVersion}`;
  }
  get clearBeforeRender() {
    return yt("7.0.0", "renderer.clearBeforeRender has been deprecated, please use renderer.background.clearBeforeRender instead."), this.background.clearBeforeRender;
  }
  get useContextAlpha() {
    return yt("7.0.0", "renderer.useContextAlpha has been deprecated, please use renderer.context.premultipliedAlpha instead."), this.context.useContextAlpha;
  }
  get preserveDrawingBuffer() {
    return yt("7.0.0", "renderer.preserveDrawingBuffer has been deprecated, we cannot truly know this unless pixi created the context"), this.context.preserveDrawingBuffer;
  }
  get backgroundColor() {
    return yt("7.0.0", "renderer.backgroundColor has been deprecated, use renderer.background.color instead."), this.background.color;
  }
  set backgroundColor(t) {
    yt("7.0.0", "renderer.backgroundColor has been deprecated, use renderer.background.color instead."), this.background.color = t;
  }
  get backgroundAlpha() {
    return yt("7.0.0", "renderer.backgroundAlpha has been deprecated, use renderer.background.alpha instead."), this.background.alpha;
  }
  set backgroundAlpha(t) {
    yt("7.0.0", "renderer.backgroundAlpha has been deprecated, use renderer.background.alpha instead."), this.background.alpha = t;
  }
  get powerPreference() {
    return yt("7.0.0", "renderer.powerPreference has been deprecated, we can only know this if pixi creates the context"), this.context.powerPreference;
  }
  generateTexture(t, r) {
    return this.textureGenerator.generateTexture(t, r);
  }
};
Vr.extension = {
  type: ut.Renderer,
  priority: 1
}, Vr.__plugins = {}, Vr.__systems = {};
var ai = Vr;
mt.handleByMap(ut.RendererPlugin, ai.__plugins);
mt.handleByMap(ut.RendererSystem, ai.__systems);
mt.add(ai);
var Fa = class extends pr {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    super(r, s), this.items = [], this.itemDirtyIds = [];
    for (let i = 0; i < e; i++) {
      const n = new vt();
      this.items.push(n), this.itemDirtyIds.push(-2);
    }
    this.length = e, this._load = null, this.baseTexture = null;
  }
  initFromArray(e, t) {
    for (let r = 0; r < this.length; r++) e[r] && (e[r].castToBaseTexture ? this.addBaseTextureAt(e[r].castToBaseTexture(), r) : e[r] instanceof pr ? this.addResourceAt(e[r], r) : this.addResourceAt(zn(e[r], t), r));
  }
  dispose() {
    for (let e = 0, t = this.length; e < t; e++) this.items[e].destroy();
    this.items = null, this.itemDirtyIds = null, this._load = null;
  }
  addResourceAt(e, t) {
    if (!this.items[t]) throw new Error(`Index ${t} is out of bounds`);
    return e.valid && !this.valid && this.resize(e.width, e.height), this.items[t].setResource(e), this;
  }
  bind(e) {
    if (this.baseTexture !== null) throw new Error("Only one base texture per TextureArray is allowed");
    super.bind(e);
    for (let t = 0; t < this.length; t++) this.items[t].parentTextureArray = e, this.items[t].on("update", e.update, e);
  }
  unbind(e) {
    super.unbind(e);
    for (let t = 0; t < this.length; t++) this.items[t].parentTextureArray = null, this.items[t].off("update", e.update, e);
  }
  load() {
    if (this._load) return this._load;
    const e = this.items.map((t) => t.resource).filter((t) => t).map((t) => t.load());
    return this._load = Promise.all(e).then(() => {
      const { realWidth: t, realHeight: r } = this.items[0];
      return this.resize(t, r), this.update(), Promise.resolve(this);
    }), this._load;
  }
}, Rl = class extends Fa {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    let i, n;
    Array.isArray(e) ? (i = e, n = e.length) : n = e, super(n, {
      width: r,
      height: s
    }), i && this.initFromArray(i, t);
  }
  addBaseTextureAt(e, t) {
    if (e.resource) this.addResourceAt(e.resource, t);
    else throw new Error("ArrayResource does not support RenderTexture");
    return this;
  }
  bind(e) {
    super.bind(e), e.target = qe.TEXTURE_2D_ARRAY;
  }
  upload(e, t, r) {
    const { length: s, itemDirtyIds: i, items: n } = this, { gl: a } = e;
    r.dirtyId < 0 && a.texImage3D(a.TEXTURE_2D_ARRAY, 0, r.internalFormat, this._width, this._height, s, 0, t.format, r.type, null);
    for (let o = 0; o < s; o++) {
      const h = n[o];
      i[o] < h.dirtyId && (i[o] = h.dirtyId, h.valid && a.texSubImage3D(a.TEXTURE_2D_ARRAY, 0, 0, 0, o, h.resource.width, h.resource.height, 1, t.format, r.type, h.resource.source));
    }
    return !0;
  }
}, Ml = class extends re {
  constructor(e) {
    super(e);
  }
  static test(e) {
    const { OffscreenCanvas: t } = globalThis;
    return t && e instanceof t ? !0 : globalThis.HTMLCanvasElement && e instanceof HTMLCanvasElement;
  }
}, Na = class or extends Fa {
  constructor(t, r) {
    const { width: s, height: i, autoLoad: n, linkBaseTexture: a } = r || {};
    if (t && t.length !== or.SIDES) throw new Error(`Invalid length. Got ${t.length}, expected 6`);
    super(6, {
      width: s,
      height: i
    });
    for (let o = 0; o < or.SIDES; o++) this.items[o].target = qe.TEXTURE_CUBE_MAP_POSITIVE_X + o;
    this.linkBaseTexture = a !== !1, t && this.initFromArray(t, r), n !== !1 && this.load();
  }
  bind(t) {
    super.bind(t), t.target = qe.TEXTURE_CUBE_MAP;
  }
  addBaseTextureAt(t, r, s) {
    if (s === void 0 && (s = this.linkBaseTexture), !this.items[r]) throw new Error(`Index ${r} is out of bounds`);
    if (!this.linkBaseTexture || t.parentTextureArray || Object.keys(t._glTextures).length > 0) if (t.resource) this.addResourceAt(t.resource, r);
    else throw new Error("CubeResource does not support copying of renderTexture.");
    else t.target = qe.TEXTURE_CUBE_MAP_POSITIVE_X + r, t.parentTextureArray = this.baseTexture, this.items[r] = t;
    return t.valid && !this.valid && this.resize(t.realWidth, t.realHeight), this.items[r] = t, this;
  }
  upload(t, r, s) {
    const i = this.itemDirtyIds;
    for (let n = 0; n < or.SIDES; n++) {
      const a = this.items[n];
      (i[n] < a.dirtyId || s.dirtyId < r.dirtyId) && (a.valid && a.resource ? (a.resource.upload(t, a, s), i[n] = a.dirtyId) : i[n] < -1 && (t.gl.texImage2D(a.target, 0, s.internalFormat, r.realWidth, r.realHeight, 0, r.format, s.type, null), i[n] = -1));
    }
    return !0;
  }
  static test(t) {
    return Array.isArray(t) && t.length === or.SIDES;
  }
};
Na.SIDES = 6;
var Cl = Na, Pl = class hr extends re {
  constructor(t, r) {
    r = r || {};
    let s, i, n;
    typeof t == "string" ? (s = hr.EMPTY, i = t, n = !0) : (s = t, i = null, n = !1), super(s), this.url = i, this.crossOrigin = r.crossOrigin ?? !0, this.alphaMode = typeof r.alphaMode == "number" ? r.alphaMode : null, this.ownsImageBitmap = r.ownsImageBitmap ?? n, this._load = null, r.autoLoad !== !1 && this.load();
  }
  load() {
    return this._load ? this._load : (this._load = new Promise(async (t, r) => {
      if (this.url === null) {
        t(this);
        return;
      }
      try {
        const s = await dt.ADAPTER.fetch(this.url, { mode: this.crossOrigin ? "cors" : "no-cors" });
        if (this.destroyed) return;
        const i = await s.blob();
        if (this.destroyed) return;
        const n = await createImageBitmap(i, { premultiplyAlpha: this.alphaMode === null || this.alphaMode === Ce.UNPACK ? "premultiply" : "none" });
        if (this.destroyed) {
          n.close();
          return;
        }
        this.source = n, this.update(), t(this);
      } catch (s) {
        if (this.destroyed) return;
        r(s), this.onError.emit(s);
      }
    }), this._load);
  }
  upload(t, r, s) {
    return this.source instanceof ImageBitmap ? (typeof this.alphaMode == "number" && (r.alphaMode = this.alphaMode), super.upload(t, r, s)) : (this.load(), !1);
  }
  dispose() {
    this.ownsImageBitmap && this.source instanceof ImageBitmap && this.source.close(), super.dispose(), this._load = null;
  }
  static test(t) {
    return !!globalThis.createImageBitmap && typeof ImageBitmap < "u" && (typeof t == "string" || t instanceof ImageBitmap);
  }
  static get EMPTY() {
    return hr._EMPTY = hr._EMPTY ?? dt.ADAPTER.createCanvas(0, 0), hr._EMPTY;
  }
}, Vs = class Hr extends re {
  constructor(t, r) {
    r = r || {}, super(dt.ADAPTER.createCanvas()), this._width = 0, this._height = 0, this.svg = t, this.scale = r.scale || 1, this._overrideWidth = r.width, this._overrideHeight = r.height, this._resolve = null, this._crossorigin = r.crossorigin, this._load = null, r.autoLoad !== !1 && this.load();
  }
  load() {
    return this._load ? this._load : (this._load = new Promise((t) => {
      if (this._resolve = () => {
        this.update(), t(this);
      }, Hr.SVG_XML.test(this.svg.trim())) {
        if (!btoa) throw new Error("Your browser doesn't support base64 conversions.");
        this.svg = `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(this.svg)))}`;
      }
      this._loadSvg();
    }), this._load);
  }
  _loadSvg() {
    const t = new Image();
    re.crossOrigin(t, this.svg, this._crossorigin), t.src = this.svg, t.onerror = (r) => {
      this._resolve && (t.onerror = null, this.onError.emit(r));
    }, t.onload = () => {
      if (!this._resolve) return;
      const r = t.width, s = t.height;
      if (!r || !s) throw new Error("The SVG image must have width and height defined (in pixels), canvas API needs them.");
      let i = r * this.scale, n = s * this.scale;
      (this._overrideWidth || this._overrideHeight) && (i = this._overrideWidth || this._overrideHeight / s * r, n = this._overrideHeight || this._overrideWidth / r * s), i = Math.round(i), n = Math.round(n);
      const a = this.source;
      a.width = i, a.height = n, a._pixiId = `canvas_${fr()}`, a.getContext("2d").drawImage(t, 0, 0, r, s, 0, 0, i, n), this._resolve(), this._resolve = null;
    };
  }
  static getSize(t) {
    const r = Hr.SVG_SIZE.exec(t), s = {};
    return r && (s[r[1]] = Math.round(parseFloat(r[3])), s[r[5]] = Math.round(parseFloat(r[7]))), s;
  }
  dispose() {
    super.dispose(), this._resolve = null, this._crossorigin = null;
  }
  static test(t, r) {
    return r === "svg" || typeof t == "string" && t.startsWith("data:image/svg+xml") || typeof t == "string" && Hr.SVG_XML.test(t);
  }
};
Vs.SVG_XML = /^(<\?xml[^?]+\?>)?\s*(<!--[^(-->)]*-->)?\s*\<svg/m, Vs.SVG_SIZE = /<svg[^>]*(?:\s(width|height)=('|")(\d*(?:\.\d+)?)(?:px)?('|"))[^>]*(?:\s(width|height)=('|")(\d*(?:\.\d+)?)(?:px)?('|"))[^>]*>/i;
var Ll = Vs, Fl = class extends re {
  constructor(e) {
    super(e);
  }
  static test(e) {
    return !!globalThis.VideoFrame && e instanceof globalThis.VideoFrame;
  }
}, Hs = class Xs extends re {
  constructor(t, r) {
    if (r = r || {}, !(t instanceof HTMLVideoElement)) {
      const s = document.createElement("video");
      r.autoLoad !== !1 && s.setAttribute("preload", "auto"), r.playsinline !== !1 && (s.setAttribute("webkit-playsinline", ""), s.setAttribute("playsinline", "")), r.muted === !0 && (s.setAttribute("muted", ""), s.muted = !0), r.loop === !0 && s.setAttribute("loop", ""), r.autoPlay !== !1 && s.setAttribute("autoplay", ""), typeof t == "string" && (t = [t]);
      const i = t[0].src || t[0];
      re.crossOrigin(s, i, r.crossorigin);
      for (let n = 0; n < t.length; ++n) {
        const a = document.createElement("source");
        let { src: o, mime: h } = t[n];
        if (o = o || t[n], o.startsWith("data:")) h = o.slice(5, o.indexOf(";"));
        else if (!o.startsWith("blob:")) {
          const l = o.split("?").shift().toLowerCase(), c = l.slice(l.lastIndexOf(".") + 1);
          h = h || Xs.MIME_TYPES[c] || `video/${c}`;
        }
        a.src = o, h && (a.type = h), s.appendChild(a);
      }
      t = s;
    }
    super(t), this.noSubImage = !0, this._autoUpdate = !0, this._isConnectedToTicker = !1, this._updateFPS = r.updateFPS || 0, this._msToNextUpdate = 0, this.autoPlay = r.autoPlay !== !1, this._videoFrameRequestCallback = this._videoFrameRequestCallback.bind(this), this._videoFrameRequestCallbackHandle = null, this._load = null, this._resolve = null, this._reject = null, this._onCanPlay = this._onCanPlay.bind(this), this._onError = this._onError.bind(this), this._onPlayStart = this._onPlayStart.bind(this), this._onPlayStop = this._onPlayStop.bind(this), this._onSeeked = this._onSeeked.bind(this), r.autoLoad !== !1 && this.load();
  }
  update(t = 0) {
    if (!this.destroyed) {
      if (this._updateFPS) {
        const r = ce.shared.elapsedMS * this.source.playbackRate;
        this._msToNextUpdate = Math.floor(this._msToNextUpdate - r);
      }
      (!this._updateFPS || this._msToNextUpdate <= 0) && (super.update(), this._msToNextUpdate = this._updateFPS ? Math.floor(1e3 / this._updateFPS) : 0);
    }
  }
  _videoFrameRequestCallback() {
    this.update(), this.destroyed ? this._videoFrameRequestCallbackHandle = null : this._videoFrameRequestCallbackHandle = this.source.requestVideoFrameCallback(this._videoFrameRequestCallback);
  }
  load() {
    if (this._load) return this._load;
    const t = this.source;
    return (t.readyState === t.HAVE_ENOUGH_DATA || t.readyState === t.HAVE_FUTURE_DATA) && t.width && t.height && (t.complete = !0), t.addEventListener("play", this._onPlayStart), t.addEventListener("pause", this._onPlayStop), t.addEventListener("seeked", this._onSeeked), this._isSourceReady() ? this._onCanPlay() : (t.addEventListener("canplay", this._onCanPlay), t.addEventListener("canplaythrough", this._onCanPlay), t.addEventListener("error", this._onError, !0)), this._load = new Promise((r, s) => {
      this.valid ? r(this) : (this._resolve = r, this._reject = s, t.load());
    }), this._load;
  }
  _onError(t) {
    this.source.removeEventListener("error", this._onError, !0), this.onError.emit(t), this._reject && (this._reject(t), this._reject = null, this._resolve = null);
  }
  _isSourcePlaying() {
    const t = this.source;
    return !t.paused && !t.ended;
  }
  _isSourceReady() {
    return this.source.readyState > 2;
  }
  _onPlayStart() {
    this.valid || this._onCanPlay(), this._configureAutoUpdate();
  }
  _onPlayStop() {
    this._configureAutoUpdate();
  }
  _onSeeked() {
    this._autoUpdate && !this._isSourcePlaying() && (this._msToNextUpdate = 0, this.update(), this._msToNextUpdate = 0);
  }
  _onCanPlay() {
    const t = this.source;
    t.removeEventListener("canplay", this._onCanPlay), t.removeEventListener("canplaythrough", this._onCanPlay);
    const r = this.valid;
    this._msToNextUpdate = 0, this.update(), this._msToNextUpdate = 0, !r && this._resolve && (this._resolve(this), this._resolve = null, this._reject = null), this._isSourcePlaying() ? this._onPlayStart() : this.autoPlay && t.play();
  }
  dispose() {
    this._configureAutoUpdate();
    const t = this.source;
    t && (t.removeEventListener("play", this._onPlayStart), t.removeEventListener("pause", this._onPlayStop), t.removeEventListener("seeked", this._onSeeked), t.removeEventListener("canplay", this._onCanPlay), t.removeEventListener("canplaythrough", this._onCanPlay), t.removeEventListener("error", this._onError, !0), t.pause(), t.src = "", t.load()), super.dispose();
  }
  get autoUpdate() {
    return this._autoUpdate;
  }
  set autoUpdate(t) {
    t !== this._autoUpdate && (this._autoUpdate = t, this._configureAutoUpdate());
  }
  get updateFPS() {
    return this._updateFPS;
  }
  set updateFPS(t) {
    t !== this._updateFPS && (this._updateFPS = t, this._configureAutoUpdate());
  }
  _configureAutoUpdate() {
    this._autoUpdate && this._isSourcePlaying() ? !this._updateFPS && this.source.requestVideoFrameCallback ? (this._isConnectedToTicker && (ce.shared.remove(this.update, this), this._isConnectedToTicker = !1, this._msToNextUpdate = 0), this._videoFrameRequestCallbackHandle === null && (this._videoFrameRequestCallbackHandle = this.source.requestVideoFrameCallback(this._videoFrameRequestCallback))) : (this._videoFrameRequestCallbackHandle !== null && (this.source.cancelVideoFrameCallback(this._videoFrameRequestCallbackHandle), this._videoFrameRequestCallbackHandle = null), this._isConnectedToTicker || (ce.shared.add(this.update, this), this._isConnectedToTicker = !0, this._msToNextUpdate = 0)) : (this._videoFrameRequestCallbackHandle !== null && (this.source.cancelVideoFrameCallback(this._videoFrameRequestCallbackHandle), this._videoFrameRequestCallbackHandle = null), this._isConnectedToTicker && (ce.shared.remove(this.update, this), this._isConnectedToTicker = !1, this._msToNextUpdate = 0));
  }
  static test(t, r) {
    return globalThis.HTMLVideoElement && t instanceof HTMLVideoElement || Xs.TYPES.includes(r);
  }
};
Hs.TYPES = [
  "mp4",
  "m4v",
  "webm",
  "ogg",
  "ogv",
  "h264",
  "avi",
  "mov"
], Hs.MIME_TYPES = {
  ogv: "video/ogg",
  mov: "video/quicktime",
  m4v: "video/mp4"
};
var Nl = Hs;
Ls.push(Pl, oa, Ml, Nl, Fl, Ll, $n, Cl, Rl);
var Ws = class {
  constructor() {
    this.minX = 1 / 0, this.minY = 1 / 0, this.maxX = -1 / 0, this.maxY = -1 / 0, this.rect = null, this.updateID = -1;
  }
  isEmpty() {
    return this.minX > this.maxX || this.minY > this.maxY;
  }
  clear() {
    this.minX = 1 / 0, this.minY = 1 / 0, this.maxX = -1 / 0, this.maxY = -1 / 0;
  }
  getRectangle(e) {
    return this.minX > this.maxX || this.minY > this.maxY ? Et.EMPTY : (e = e || new Et(0, 0, 1, 1), e.x = this.minX, e.y = this.minY, e.width = this.maxX - this.minX, e.height = this.maxY - this.minY, e);
  }
  addPoint(e) {
    this.minX = Math.min(this.minX, e.x), this.maxX = Math.max(this.maxX, e.x), this.minY = Math.min(this.minY, e.y), this.maxY = Math.max(this.maxY, e.y);
  }
  addPointMatrix(e, t) {
    const { a: r, b: s, c: i, d: n, tx: a, ty: o } = e, h = r * t.x + i * t.y + a, l = s * t.x + n * t.y + o;
    this.minX = Math.min(this.minX, h), this.maxX = Math.max(this.maxX, h), this.minY = Math.min(this.minY, l), this.maxY = Math.max(this.maxY, l);
  }
  addQuad(e) {
    let t = this.minX, r = this.minY, s = this.maxX, i = this.maxY, n = e[0], a = e[1];
    t = n < t ? n : t, r = a < r ? a : r, s = n > s ? n : s, i = a > i ? a : i, n = e[2], a = e[3], t = n < t ? n : t, r = a < r ? a : r, s = n > s ? n : s, i = a > i ? a : i, n = e[4], a = e[5], t = n < t ? n : t, r = a < r ? a : r, s = n > s ? n : s, i = a > i ? a : i, n = e[6], a = e[7], t = n < t ? n : t, r = a < r ? a : r, s = n > s ? n : s, i = a > i ? a : i, this.minX = t, this.minY = r, this.maxX = s, this.maxY = i;
  }
  addFrame(e, t, r, s, i) {
    this.addFrameMatrix(e.worldTransform, t, r, s, i);
  }
  addFrameMatrix(e, t, r, s, i) {
    const n = e.a, a = e.b, o = e.c, h = e.d, l = e.tx, c = e.ty;
    let u = this.minX, d = this.minY, y = this.maxX, m = this.maxY, v = n * t + o * r + l, p = a * t + h * r + c;
    u = v < u ? v : u, d = p < d ? p : d, y = v > y ? v : y, m = p > m ? p : m, v = n * s + o * r + l, p = a * s + h * r + c, u = v < u ? v : u, d = p < d ? p : d, y = v > y ? v : y, m = p > m ? p : m, v = n * t + o * i + l, p = a * t + h * i + c, u = v < u ? v : u, d = p < d ? p : d, y = v > y ? v : y, m = p > m ? p : m, v = n * s + o * i + l, p = a * s + h * i + c, u = v < u ? v : u, d = p < d ? p : d, y = v > y ? v : y, m = p > m ? p : m, this.minX = u, this.minY = d, this.maxX = y, this.maxY = m;
  }
  addVertexData(e, t, r) {
    let s = this.minX, i = this.minY, n = this.maxX, a = this.maxY;
    for (let o = t; o < r; o += 2) {
      const h = e[o], l = e[o + 1];
      s = h < s ? h : s, i = l < i ? l : i, n = h > n ? h : n, a = l > a ? l : a;
    }
    this.minX = s, this.minY = i, this.maxX = n, this.maxY = a;
  }
  addVertices(e, t, r, s) {
    this.addVerticesMatrix(e.worldTransform, t, r, s);
  }
  addVerticesMatrix(e, t, r, s, i = 0, n = i) {
    const a = e.a, o = e.b, h = e.c, l = e.d, c = e.tx, u = e.ty;
    let d = this.minX, y = this.minY, m = this.maxX, v = this.maxY;
    for (let p = r; p < s; p += 2) {
      const x = t[p], f = t[p + 1], E = a * x + h * f + c, g = l * f + o * x + u;
      d = Math.min(d, E - i), m = Math.max(m, E + i), y = Math.min(y, g - n), v = Math.max(v, g + n);
    }
    this.minX = d, this.minY = y, this.maxX = m, this.maxY = v;
  }
  addBounds(e) {
    const t = this.minX, r = this.minY, s = this.maxX, i = this.maxY;
    this.minX = e.minX < t ? e.minX : t, this.minY = e.minY < r ? e.minY : r, this.maxX = e.maxX > s ? e.maxX : s, this.maxY = e.maxY > i ? e.maxY : i;
  }
  addBoundsMask(e, t) {
    const r = e.minX > t.minX ? e.minX : t.minX, s = e.minY > t.minY ? e.minY : t.minY, i = e.maxX < t.maxX ? e.maxX : t.maxX, n = e.maxY < t.maxY ? e.maxY : t.maxY;
    if (r <= i && s <= n) {
      const a = this.minX, o = this.minY, h = this.maxX, l = this.maxY;
      this.minX = r < a ? r : a, this.minY = s < o ? s : o, this.maxX = i > h ? i : h, this.maxY = n > l ? n : l;
    }
  }
  addBoundsMatrix(e, t) {
    this.addFrameMatrix(t, e.minX, e.minY, e.maxX, e.maxY);
  }
  addBoundsArea(e, t) {
    const r = e.minX > t.x ? e.minX : t.x, s = e.minY > t.y ? e.minY : t.y, i = e.maxX < t.x + t.width ? e.maxX : t.x + t.width, n = e.maxY < t.y + t.height ? e.maxY : t.y + t.height;
    if (r <= i && s <= n) {
      const a = this.minX, o = this.minY, h = this.maxX, l = this.maxY;
      this.minX = r < a ? r : a, this.minY = s < o ? s : o, this.maxX = i > h ? i : h, this.maxY = n > l ? n : l;
    }
  }
  pad(e = 0, t = e) {
    this.isEmpty() || (this.minX -= e, this.maxX += e, this.minY -= t, this.maxY += t);
  }
  addFramePad(e, t, r, s, i, n) {
    e -= i, t -= n, r += i, s += n, this.minX = this.minX < e ? this.minX : e, this.maxX = this.maxX > r ? this.maxX : r, this.minY = this.minY < t ? this.minY : t, this.maxY = this.maxY > s ? this.maxY : s;
  }
}, Qr = class Oa extends rs.default {
  constructor() {
    super(), this.tempDisplayObjectParent = null, this.transform = new ii(), this.alpha = 1, this.visible = !0, this.renderable = !0, this.cullable = !1, this.cullArea = null, this.parent = null, this.worldAlpha = 1, this._lastSortedIndex = 0, this._zIndex = 0, this.filterArea = null, this.filters = null, this._enabledFilters = null, this._bounds = new Ws(), this._localBounds = null, this._boundsID = 0, this._boundsRect = null, this._localBoundsRect = null, this._mask = null, this._maskRefCount = 0, this._destroyed = !1, this.isSprite = !1, this.isMask = !1;
  }
  static mixin(t) {
    const r = Object.keys(t);
    for (let s = 0; s < r.length; ++s) {
      const i = r[s];
      Object.defineProperty(Oa.prototype, i, Object.getOwnPropertyDescriptor(t, i));
    }
  }
  get destroyed() {
    return this._destroyed;
  }
  _recursivePostUpdateTransform() {
    this.parent ? (this.parent._recursivePostUpdateTransform(), this.transform.updateTransform(this.parent.transform)) : this.transform.updateTransform(this._tempDisplayObjectParent.transform);
  }
  updateTransform() {
    this._boundsID++, this.transform.updateTransform(this.parent.transform), this.worldAlpha = this.alpha * this.parent.worldAlpha;
  }
  getBounds(t, r) {
    return t || (this.parent ? (this._recursivePostUpdateTransform(), this.updateTransform()) : (this.parent = this._tempDisplayObjectParent, this.updateTransform(), this.parent = null)), this._bounds.updateID !== this._boundsID && (this.calculateBounds(), this._bounds.updateID = this._boundsID), r || (this._boundsRect || (this._boundsRect = new Et()), r = this._boundsRect), this._bounds.getRectangle(r);
  }
  getLocalBounds(t) {
    t || (this._localBoundsRect || (this._localBoundsRect = new Et()), t = this._localBoundsRect), this._localBounds || (this._localBounds = new Ws());
    const r = this.transform, s = this.parent;
    this.parent = null, this._tempDisplayObjectParent.worldAlpha = s?.worldAlpha ?? 1, this.transform = this._tempDisplayObjectParent.transform;
    const i = this._bounds, n = this._boundsID;
    this._bounds = this._localBounds;
    const a = this.getBounds(!1, t);
    return this.parent = s, this.transform = r, this._bounds = i, this._bounds.updateID += this._boundsID - n, a;
  }
  toGlobal(t, r, s = !1) {
    return s || (this._recursivePostUpdateTransform(), this.parent ? this.displayObjectUpdateTransform() : (this.parent = this._tempDisplayObjectParent, this.displayObjectUpdateTransform(), this.parent = null)), this.worldTransform.apply(t, r);
  }
  toLocal(t, r, s, i) {
    return r && (t = r.toGlobal(t, s, i)), i || (this._recursivePostUpdateTransform(), this.parent ? this.displayObjectUpdateTransform() : (this.parent = this._tempDisplayObjectParent, this.displayObjectUpdateTransform(), this.parent = null)), this.worldTransform.applyInverse(t, s);
  }
  setParent(t) {
    if (!t || !t.addChild) throw new Error("setParent: Argument must be a Container");
    return t.addChild(this), t;
  }
  removeFromParent() {
    this.parent?.removeChild(this);
  }
  setTransform(t = 0, r = 0, s = 1, i = 1, n = 0, a = 0, o = 0, h = 0, l = 0) {
    return this.position.x = t, this.position.y = r, this.scale.x = s || 1, this.scale.y = i || 1, this.rotation = n, this.skew.x = a, this.skew.y = o, this.pivot.x = h, this.pivot.y = l, this;
  }
  destroy(t) {
    this.removeFromParent(), this._destroyed = !0, this.transform = null, this.parent = null, this._bounds = null, this.mask = null, this.cullArea = null, this.filters = null, this.filterArea = null, this.hitArea = null, this.eventMode = "auto", this.interactiveChildren = !1, this.emit("destroyed"), this.removeAllListeners();
  }
  get _tempDisplayObjectParent() {
    return this.tempDisplayObjectParent === null && (this.tempDisplayObjectParent = new Ol()), this.tempDisplayObjectParent;
  }
  enableTempParent() {
    const t = this.parent;
    return this.parent = this._tempDisplayObjectParent, t;
  }
  disableTempParent(t) {
    this.parent = t;
  }
  get x() {
    return this.position.x;
  }
  set x(t) {
    this.transform.position.x = t;
  }
  get y() {
    return this.position.y;
  }
  set y(t) {
    this.transform.position.y = t;
  }
  get worldTransform() {
    return this.transform.worldTransform;
  }
  get localTransform() {
    return this.transform.localTransform;
  }
  get position() {
    return this.transform.position;
  }
  set position(t) {
    this.transform.position.copyFrom(t);
  }
  get scale() {
    return this.transform.scale;
  }
  set scale(t) {
    this.transform.scale.copyFrom(t);
  }
  get pivot() {
    return this.transform.pivot;
  }
  set pivot(t) {
    this.transform.pivot.copyFrom(t);
  }
  get skew() {
    return this.transform.skew;
  }
  set skew(t) {
    this.transform.skew.copyFrom(t);
  }
  get rotation() {
    return this.transform.rotation;
  }
  set rotation(t) {
    this.transform.rotation = t;
  }
  get angle() {
    return this.transform.rotation * vh;
  }
  set angle(t) {
    this.transform.rotation = t * xh;
  }
  get zIndex() {
    return this._zIndex;
  }
  set zIndex(t) {
    this._zIndex !== t && (this._zIndex = t, this.parent && (this.parent.sortDirty = !0));
  }
  get worldVisible() {
    let t = this;
    do {
      if (!t.visible) return !1;
      t = t.parent;
    } while (t);
    return !0;
  }
  get mask() {
    return this._mask;
  }
  set mask(t) {
    if (this._mask !== t) {
      if (this._mask) {
        const r = this._mask.isMaskData ? this._mask.maskObject : this._mask;
        r && (r._maskRefCount--, r._maskRefCount === 0 && (r.renderable = !0, r.isMask = !1));
      }
      if (this._mask = t, this._mask) {
        const r = this._mask.isMaskData ? this._mask.maskObject : this._mask;
        r && (r._maskRefCount === 0 && (r.renderable = !1, r.isMask = !0), r._maskRefCount++);
      }
    }
  }
}, Ol = class extends Qr {
  constructor() {
    super(...arguments), this.sortDirty = null;
  }
};
Qr.prototype.displayObjectUpdateTransform = Qr.prototype.updateTransform;
var Bl = new Lt();
function Ul(e, t) {
  return e.zIndex === t.zIndex ? e._lastSortedIndex - t._lastSortedIndex : e.zIndex - t.zIndex;
}
var Ba = class qs extends Qr {
  constructor() {
    super(), this.children = [], this.sortableChildren = qs.defaultSortableChildren, this.sortDirty = !1;
  }
  onChildrenChange(t) {
  }
  addChild(...t) {
    if (t.length > 1) for (let r = 0; r < t.length; r++) this.addChild(t[r]);
    else {
      const r = t[0];
      r.parent && r.parent.removeChild(r), r.parent = this, this.sortDirty = !0, r.transform._parentID = -1, this.children.push(r), this._boundsID++, this.onChildrenChange(this.children.length - 1), this.emit("childAdded", r, this, this.children.length - 1), r.emit("added", this);
    }
    return t[0];
  }
  addChildAt(t, r) {
    if (r < 0 || r > this.children.length) throw new Error(`${t}addChildAt: The index ${r} supplied is out of bounds ${this.children.length}`);
    return t.parent && t.parent.removeChild(t), t.parent = this, this.sortDirty = !0, t.transform._parentID = -1, this.children.splice(r, 0, t), this._boundsID++, this.onChildrenChange(r), t.emit("added", this), this.emit("childAdded", t, this, r), t;
  }
  swapChildren(t, r) {
    if (t === r) return;
    const s = this.getChildIndex(t), i = this.getChildIndex(r);
    this.children[s] = r, this.children[i] = t, this.onChildrenChange(s < i ? s : i);
  }
  getChildIndex(t) {
    const r = this.children.indexOf(t);
    if (r === -1) throw new Error("The supplied DisplayObject must be a child of the caller");
    return r;
  }
  setChildIndex(t, r) {
    if (r < 0 || r >= this.children.length) throw new Error(`The index ${r} supplied is out of bounds ${this.children.length}`);
    const s = this.getChildIndex(t);
    kr(this.children, s, 1), this.children.splice(r, 0, t), this.onChildrenChange(r);
  }
  getChildAt(t) {
    if (t < 0 || t >= this.children.length) throw new Error(`getChildAt: Index (${t}) does not exist.`);
    return this.children[t];
  }
  removeChild(...t) {
    if (t.length > 1) for (let r = 0; r < t.length; r++) this.removeChild(t[r]);
    else {
      const r = t[0], s = this.children.indexOf(r);
      if (s === -1) return null;
      r.parent = null, r.transform._parentID = -1, kr(this.children, s, 1), this._boundsID++, this.onChildrenChange(s), r.emit("removed", this), this.emit("childRemoved", r, this, s);
    }
    return t[0];
  }
  removeChildAt(t) {
    const r = this.getChildAt(t);
    return r.parent = null, r.transform._parentID = -1, kr(this.children, t, 1), this._boundsID++, this.onChildrenChange(t), r.emit("removed", this), this.emit("childRemoved", r, this, t), r;
  }
  removeChildren(t = 0, r = this.children.length) {
    const s = t, i = r, n = i - s;
    let a;
    if (n > 0 && n <= i) {
      a = this.children.splice(s, n);
      for (let o = 0; o < a.length; ++o) a[o].parent = null, a[o].transform && (a[o].transform._parentID = -1);
      this._boundsID++, this.onChildrenChange(t);
      for (let o = 0; o < a.length; ++o) a[o].emit("removed", this), this.emit("childRemoved", a[o], this, o);
      return a;
    } else if (n === 0 && this.children.length === 0) return [];
    throw new RangeError("removeChildren: numeric values are outside the acceptable range.");
  }
  sortChildren() {
    let t = !1;
    for (let r = 0, s = this.children.length; r < s; ++r) {
      const i = this.children[r];
      i._lastSortedIndex = r, !t && i.zIndex !== 0 && (t = !0);
    }
    t && this.children.length > 1 && this.children.sort(Ul), this.sortDirty = !1;
  }
  updateTransform() {
    this.sortableChildren && this.sortDirty && this.sortChildren(), this._boundsID++, this.transform.updateTransform(this.parent.transform), this.worldAlpha = this.alpha * this.parent.worldAlpha;
    for (let t = 0, r = this.children.length; t < r; ++t) {
      const s = this.children[t];
      s.visible && s.updateTransform();
    }
  }
  calculateBounds() {
    this._bounds.clear(), this._calculateBounds();
    for (let t = 0; t < this.children.length; t++) {
      const r = this.children[t];
      if (!(!r.visible || !r.renderable)) if (r.calculateBounds(), r._mask) {
        const s = r._mask.isMaskData ? r._mask.maskObject : r._mask;
        s ? (s.calculateBounds(), this._bounds.addBoundsMask(r._bounds, s._bounds)) : this._bounds.addBounds(r._bounds);
      } else r.filterArea ? this._bounds.addBoundsArea(r._bounds, r.filterArea) : this._bounds.addBounds(r._bounds);
    }
    this._bounds.updateID = this._boundsID;
  }
  getLocalBounds(t, r = !1) {
    const s = super.getLocalBounds(t);
    if (!r) for (let i = 0, n = this.children.length; i < n; ++i) {
      const a = this.children[i];
      a.visible && a.updateTransform();
    }
    return s;
  }
  _calculateBounds() {
  }
  _renderWithCulling(t) {
    const r = t.renderTexture.sourceFrame;
    if (!(r.width > 0 && r.height > 0)) return;
    let s, i;
    this.cullArea ? (s = this.cullArea, i = this.worldTransform) : this._render !== qs.prototype._render && (s = this.getBounds(!0));
    const n = t.projection.transform;
    if (n && (i ? (i = Bl.copyFrom(i), i.prepend(n)) : i = n), s && r.intersects(s, i)) this._render(t);
    else if (this.cullArea) return;
    for (let a = 0, o = this.children.length; a < o; ++a) {
      const h = this.children[a], l = h.cullable;
      h.cullable = l || !this.cullArea, h.render(t), h.cullable = l;
    }
  }
  render(t) {
    if (!(!this.visible || this.worldAlpha <= 0 || !this.renderable)) if (this._mask || this.filters?.length) this.renderAdvanced(t);
    else if (this.cullable) this._renderWithCulling(t);
    else {
      this._render(t);
      for (let r = 0, s = this.children.length; r < s; ++r) this.children[r].render(t);
    }
  }
  renderAdvanced(t) {
    const r = this.filters, s = this._mask;
    if (r) {
      this._enabledFilters || (this._enabledFilters = []), this._enabledFilters.length = 0;
      for (let n = 0; n < r.length; n++) r[n].enabled && this._enabledFilters.push(r[n]);
    }
    const i = r && this._enabledFilters?.length || s && (!s.isMaskData || s.enabled && (s.autoDetect || s.type !== Ct.NONE));
    if (i && t.batch.flush(), r && this._enabledFilters?.length && t.filter.push(this, this._enabledFilters), s && t.mask.push(this, this._mask), this.cullable) this._renderWithCulling(t);
    else {
      this._render(t);
      for (let n = 0, a = this.children.length; n < a; ++n) this.children[n].render(t);
    }
    i && t.batch.flush(), s && t.mask.pop(this), r && this._enabledFilters?.length && t.filter.pop();
  }
  _render(t) {
  }
  destroy(t) {
    super.destroy(), this.sortDirty = !1;
    const r = typeof t == "boolean" ? t : t?.children, s = this.removeChildren(0, this.children.length);
    if (r) for (let i = 0; i < s.length; ++i) s[i].destroy(t);
  }
  get width() {
    return this.scale.x * this.getLocalBounds().width;
  }
  set width(t) {
    const r = this.getLocalBounds().width;
    r !== 0 ? this.scale.x = t / r : this.scale.x = 1, this._width = t;
  }
  get height() {
    return this.scale.y * this.getLocalBounds().height;
  }
  set height(t) {
    const r = this.getLocalBounds().height;
    r !== 0 ? this.scale.y = t / r : this.scale.y = 1, this._height = t;
  }
};
Ba.defaultSortableChildren = !1;
var Ie = Ba;
Ie.prototype.containerUpdateTransform = Ie.prototype.updateTransform;
Object.defineProperties(dt, { SORTABLE_CHILDREN: {
  get() {
    return Ie.defaultSortableChildren;
  },
  set(e) {
    yt("7.1.0", "settings.SORTABLE_CHILDREN is deprecated, use Container.defaultSortableChildren"), Ie.defaultSortableChildren = e;
  }
} });
var Ua = class js {
  constructor(t) {
    this.stage = new Ie(), t = Object.assign({ forceCanvas: !1 }, t), this.renderer = El(t), js._plugins.forEach((r) => {
      r.init.call(this, t);
    });
  }
  render() {
    this.renderer.render(this.stage);
  }
  get view() {
    return this.renderer?.view;
  }
  get screen() {
    return this.renderer?.screen;
  }
  destroy(t, r) {
    const s = js._plugins.slice(0);
    s.reverse(), s.forEach((i) => {
      i.destroy.call(this);
    }), this.stage.destroy(r), this.stage = null, this.renderer.destroy(t), this.renderer = null;
  }
};
Ua._plugins = [];
var ka = Ua;
mt.handleByList(ut.Application, ka._plugins);
var Da = class {
  static init(e) {
    Object.defineProperty(this, "resizeTo", {
      set(t) {
        globalThis.removeEventListener("resize", this.queueResize), this._resizeTo = t, t && (globalThis.addEventListener("resize", this.queueResize), this.resize());
      },
      get() {
        return this._resizeTo;
      }
    }), this.queueResize = () => {
      this._resizeTo && (this.cancelResize(), this._resizeId = requestAnimationFrame(() => this.resize()));
    }, this.cancelResize = () => {
      this._resizeId && (cancelAnimationFrame(this._resizeId), this._resizeId = null);
    }, this.resize = () => {
      if (!this._resizeTo) return;
      this.cancelResize();
      let t, r;
      if (this._resizeTo === globalThis.window) t = globalThis.innerWidth, r = globalThis.innerHeight;
      else {
        const { clientWidth: s, clientHeight: i } = this._resizeTo;
        t = s, r = i;
      }
      this.renderer.resize(t, r), this.render();
    }, this._resizeId = null, this._resizeTo = null, this.resizeTo = e.resizeTo || null;
  }
  static destroy() {
    globalThis.removeEventListener("resize", this.queueResize), this.cancelResize(), this.cancelResize = null, this.queueResize = null, this.resizeTo = null, this.resize = null;
  }
};
Da.extension = ut.Application;
mt.add(Da);
var kl = {
  5: [
    0.153388,
    0.221461,
    0.250301
  ],
  7: [
    0.071303,
    0.131514,
    0.189879,
    0.214607
  ],
  9: [
    0.028532,
    0.067234,
    0.124009,
    0.179044,
    0.20236
  ],
  11: [
    93e-4,
    0.028002,
    0.065984,
    0.121703,
    0.175713,
    0.198596
  ],
  13: [
    2406e-6,
    9255e-6,
    0.027867,
    0.065666,
    0.121117,
    0.174868,
    0.197641
  ],
  15: [
    489e-6,
    2403e-6,
    9246e-6,
    0.02784,
    0.065602,
    0.120999,
    0.174697,
    0.197448
  ]
}, Dl = [
  "varying vec2 vBlurTexCoords[%size%];",
  "uniform sampler2D uSampler;",
  "void main(void)",
  "{",
  "    gl_FragColor = vec4(0.0);",
  "    %blur%",
  "}"
].join(`
`);
function Gl(e) {
  const t = kl[e], r = t.length;
  let s = Dl, i = "";
  const n = "gl_FragColor += texture2D(uSampler, vBlurTexCoords[%index%]) * %value%;";
  let a;
  for (let o = 0; o < e; o++) {
    let h = n.replace("%index%", o.toString());
    a = o, o >= r && (a = e - o - 1), h = h.replace("%value%", t[a].toString()), i += h, i += `
`;
  }
  return s = s.replace("%blur%", i), s = s.replace("%size%", e.toString()), s;
}
var zl = `
    attribute vec2 aVertexPosition;

    uniform mat3 projectionMatrix;

    uniform float strength;

    varying vec2 vBlurTexCoords[%size%];

    uniform vec4 inputSize;
    uniform vec4 outputFrame;

    vec4 filterVertexPosition( void )
    {
        vec2 position = aVertexPosition * max(outputFrame.zw, vec2(0.)) + outputFrame.xy;

        return vec4((projectionMatrix * vec3(position, 1.0)).xy, 0.0, 1.0);
    }

    vec2 filterTextureCoord( void )
    {
        return aVertexPosition * (outputFrame.zw * inputSize.zw);
    }

    void main(void)
    {
        gl_Position = filterVertexPosition();

        vec2 textureCoord = filterTextureCoord();
        %blur%
    }`;
function $l(e, t) {
  const r = Math.ceil(e / 2);
  let s = zl, i = "", n;
  t ? n = "vBlurTexCoords[%index%] =  textureCoord + vec2(%sampleIndex% * strength, 0.0);" : n = "vBlurTexCoords[%index%] =  textureCoord + vec2(0.0, %sampleIndex% * strength);";
  for (let a = 0; a < e; a++) {
    let o = n.replace("%index%", a.toString());
    o = o.replace("%sampleIndex%", `${a - (r - 1)}.0`), i += o, i += `
`;
  }
  return s = s.replace("%blur%", i), s = s.replace("%size%", e.toString()), s;
}
var an = class extends Gt {
  constructor(e, t = 8, r = 4, s = Gt.defaultResolution, i = 5) {
    const n = $l(i, e), a = Gl(i);
    super(n, a), this.horizontal = e, this.resolution = s, this._quality = 0, this.quality = r, this.blur = t;
  }
  apply(e, t, r, s) {
    if (r ? this.horizontal ? this.uniforms.strength = 1 / r.width * (r.width / t.width) : this.uniforms.strength = 1 / r.height * (r.height / t.height) : this.horizontal ? this.uniforms.strength = 1 / e.renderer.width * (e.renderer.width / t.width) : this.uniforms.strength = 1 / e.renderer.height * (e.renderer.height / t.height), this.uniforms.strength *= this.strength, this.uniforms.strength /= this.passes, this.passes === 1) e.applyFilter(this, t, r, s);
    else {
      const i = e.getFilterTexture(), n = e.renderer;
      let a = t, o = i;
      this.state.blend = !1, e.applyFilter(this, a, o, Kt.CLEAR);
      for (let h = 1; h < this.passes - 1; h++) {
        e.bindAndClear(a, Kt.BLIT), this.uniforms.uSampler = o;
        const l = o;
        o = a, a = l, n.shader.bind(this), n.geometry.draw(5);
      }
      this.state.blend = !0, e.applyFilter(this, o, r, s), e.returnFilterTexture(i);
    }
  }
  get blur() {
    return this.strength;
  }
  set blur(e) {
    this.padding = 1 + Math.abs(e) * 2, this.strength = e;
  }
  get quality() {
    return this._quality;
  }
  set quality(e) {
    this._quality = e, this.passes = e;
  }
}, ae = class extends Gt {
  constructor(e = 8, t = 4, r = Gt.defaultResolution, s = 5) {
    super(), this._repeatEdgePixels = !1, this.blurXFilter = new an(!0, e, t, r, s), this.blurYFilter = new an(!1, e, t, r, s), this.resolution = r, this.quality = t, this.blur = e, this.repeatEdgePixels = !1;
  }
  apply(e, t, r, s) {
    const i = Math.abs(this.blurXFilter.strength), n = Math.abs(this.blurYFilter.strength);
    if (i && n) {
      const a = e.getFilterTexture();
      this.blurXFilter.apply(e, t, a, Kt.CLEAR), this.blurYFilter.apply(e, a, r, s), e.returnFilterTexture(a);
    } else n ? this.blurYFilter.apply(e, t, r, s) : this.blurXFilter.apply(e, t, r, s);
  }
  updatePadding() {
    this._repeatEdgePixels ? this.padding = 0 : this.padding = Math.max(Math.abs(this.blurXFilter.strength), Math.abs(this.blurYFilter.strength)) * 2;
  }
  get blur() {
    return this.blurXFilter.blur;
  }
  set blur(e) {
    this.blurXFilter.blur = this.blurYFilter.blur = e, this.updatePadding();
  }
  get quality() {
    return this.blurXFilter.quality;
  }
  set quality(e) {
    this.blurXFilter.quality = this.blurYFilter.quality = e;
  }
  get blurX() {
    return this.blurXFilter.blur;
  }
  set blurX(e) {
    this.blurXFilter.blur = e, this.updatePadding();
  }
  get blurY() {
    return this.blurYFilter.blur;
  }
  set blurY(e) {
    this.blurYFilter.blur = e, this.updatePadding();
  }
  get blendMode() {
    return this.blurYFilter.blendMode;
  }
  set blendMode(e) {
    this.blurYFilter.blendMode = e;
  }
  get repeatEdgePixels() {
    return this._repeatEdgePixels;
  }
  set repeatEdgePixels(e) {
    this._repeatEdgePixels = e, this.updatePadding();
  }
}, Vl = `attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`, Hl = `uniform float radius;
uniform float strength;
uniform vec2 center;
uniform sampler2D uSampler;
varying vec2 vTextureCoord;

uniform vec4 filterArea;
uniform vec4 filterClamp;
uniform vec2 dimensions;

void main()
{
    vec2 coord = vTextureCoord * filterArea.xy;
    coord -= center * dimensions.xy;
    float distance = length(coord);
    if (distance < radius) {
        float percent = distance / radius;
        if (strength > 0.0) {
            coord *= mix(1.0, smoothstep(0.0, radius / distance, percent), strength * 0.75);
        } else {
            coord *= mix(1.0, pow(percent, 1.0 + strength * 0.75) * radius / distance, 1.0 - percent);
        }
    }
    coord += center * dimensions.xy;
    coord /= filterArea.xy;
    vec2 clampedCoord = clamp(coord, filterClamp.xy, filterClamp.zw);
    vec4 color = texture2D(uSampler, clampedCoord);
    if (coord != clampedCoord) {
        color *= max(0.0, 1.0 - length(coord - clampedCoord));
    }

    gl_FragColor = color;
}
`, Ga = class extends Gt {
  constructor(e) {
    super(Vl, Hl), this.uniforms.dimensions = /* @__PURE__ */ new Float32Array(2), Object.assign(this, Ga.defaults, e);
  }
  apply(e, t, r, s) {
    const { width: i, height: n } = t.filterFrame;
    this.uniforms.dimensions[0] = i, this.uniforms.dimensions[1] = n, e.applyFilter(this, t, r, s);
  }
  get radius() {
    return this.uniforms.radius;
  }
  set radius(e) {
    this.uniforms.radius = e;
  }
  get strength() {
    return this.uniforms.strength;
  }
  set strength(e) {
    this.uniforms.strength = e;
  }
  get center() {
    return this.uniforms.center;
  }
  set center(e) {
    this.uniforms.center = e;
  }
}, lr = Ga;
lr.defaults = {
  center: [0.5, 0.5],
  radius: 100,
  strength: 1
};
var Xl = `varying vec2 vTextureCoord;
uniform sampler2D uSampler;
uniform float m[20];
uniform float uAlpha;

void main(void)
{
    vec4 c = texture2D(uSampler, vTextureCoord);

    if (uAlpha == 0.0) {
        gl_FragColor = c;
        return;
    }

    // Un-premultiply alpha before applying the color matrix. See issue #3539.
    if (c.a > 0.0) {
      c.rgb /= c.a;
    }

    vec4 result;

    result.r = (m[0] * c.r);
        result.r += (m[1] * c.g);
        result.r += (m[2] * c.b);
        result.r += (m[3] * c.a);
        result.r += m[4];

    result.g = (m[5] * c.r);
        result.g += (m[6] * c.g);
        result.g += (m[7] * c.b);
        result.g += (m[8] * c.a);
        result.g += m[9];

    result.b = (m[10] * c.r);
       result.b += (m[11] * c.g);
       result.b += (m[12] * c.b);
       result.b += (m[13] * c.a);
       result.b += m[14];

    result.a = (m[15] * c.r);
       result.a += (m[16] * c.g);
       result.a += (m[17] * c.b);
       result.a += (m[18] * c.a);
       result.a += m[19];

    vec3 rgb = mix(c.rgb, result.rgb, uAlpha);

    // Premultiply alpha again.
    rgb *= result.a;

    gl_FragColor = vec4(rgb, result.a);
}
`, dr = class extends Gt {
  constructor() {
    const e = {
      m: new Float32Array([
        1,
        0,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        0,
        1,
        0
      ]),
      uAlpha: 1
    };
    super(Al, Xl, e), this.alpha = 1;
  }
  _loadMatrix(e, t = !1) {
    let r = e;
    t && (this._multiply(r, this.uniforms.m, e), r = this._colorMatrix(r)), this.uniforms.m = r;
  }
  _multiply(e, t, r) {
    return e[0] = t[0] * r[0] + t[1] * r[5] + t[2] * r[10] + t[3] * r[15], e[1] = t[0] * r[1] + t[1] * r[6] + t[2] * r[11] + t[3] * r[16], e[2] = t[0] * r[2] + t[1] * r[7] + t[2] * r[12] + t[3] * r[17], e[3] = t[0] * r[3] + t[1] * r[8] + t[2] * r[13] + t[3] * r[18], e[4] = t[0] * r[4] + t[1] * r[9] + t[2] * r[14] + t[3] * r[19] + t[4], e[5] = t[5] * r[0] + t[6] * r[5] + t[7] * r[10] + t[8] * r[15], e[6] = t[5] * r[1] + t[6] * r[6] + t[7] * r[11] + t[8] * r[16], e[7] = t[5] * r[2] + t[6] * r[7] + t[7] * r[12] + t[8] * r[17], e[8] = t[5] * r[3] + t[6] * r[8] + t[7] * r[13] + t[8] * r[18], e[9] = t[5] * r[4] + t[6] * r[9] + t[7] * r[14] + t[8] * r[19] + t[9], e[10] = t[10] * r[0] + t[11] * r[5] + t[12] * r[10] + t[13] * r[15], e[11] = t[10] * r[1] + t[11] * r[6] + t[12] * r[11] + t[13] * r[16], e[12] = t[10] * r[2] + t[11] * r[7] + t[12] * r[12] + t[13] * r[17], e[13] = t[10] * r[3] + t[11] * r[8] + t[12] * r[13] + t[13] * r[18], e[14] = t[10] * r[4] + t[11] * r[9] + t[12] * r[14] + t[13] * r[19] + t[14], e[15] = t[15] * r[0] + t[16] * r[5] + t[17] * r[10] + t[18] * r[15], e[16] = t[15] * r[1] + t[16] * r[6] + t[17] * r[11] + t[18] * r[16], e[17] = t[15] * r[2] + t[16] * r[7] + t[17] * r[12] + t[18] * r[17], e[18] = t[15] * r[3] + t[16] * r[8] + t[17] * r[13] + t[18] * r[18], e[19] = t[15] * r[4] + t[16] * r[9] + t[17] * r[14] + t[18] * r[19] + t[19], e;
  }
  _colorMatrix(e) {
    const t = new Float32Array(e);
    return t[4] /= 255, t[9] /= 255, t[14] /= 255, t[19] /= 255, t;
  }
  brightness(e, t) {
    const r = [
      e,
      0,
      0,
      0,
      0,
      0,
      e,
      0,
      0,
      0,
      0,
      0,
      e,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(r, t);
  }
  tint(e, t) {
    const [r, s, i] = Ae.shared.setValue(e).toArray(), n = [
      r,
      0,
      0,
      0,
      0,
      0,
      s,
      0,
      0,
      0,
      0,
      0,
      i,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(n, t);
  }
  greyscale(e, t) {
    const r = [
      e,
      e,
      e,
      0,
      0,
      e,
      e,
      e,
      0,
      0,
      e,
      e,
      e,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(r, t);
  }
  blackAndWhite(e) {
    this._loadMatrix([
      0.3,
      0.6,
      0.1,
      0,
      0,
      0.3,
      0.6,
      0.1,
      0,
      0,
      0.3,
      0.6,
      0.1,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  hue(e, t) {
    e = (e || 0) / 180 * Math.PI;
    const r = Math.cos(e), s = Math.sin(e), i = Math.sqrt, n = 1 / 3, a = i(n), o = [
      r + (1 - r) * n,
      n * (1 - r) - a * s,
      n * (1 - r) + a * s,
      0,
      0,
      n * (1 - r) + a * s,
      r + n * (1 - r),
      n * (1 - r) - a * s,
      0,
      0,
      n * (1 - r) - a * s,
      n * (1 - r) + a * s,
      r + n * (1 - r),
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(o, t);
  }
  contrast(e, t) {
    const r = (e || 0) + 1, s = -0.5 * (r - 1), i = [
      r,
      0,
      0,
      0,
      s,
      0,
      r,
      0,
      0,
      s,
      0,
      0,
      r,
      0,
      s,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(i, t);
  }
  saturate(e = 0, t) {
    const r = e * 2 / 3 + 1, s = (r - 1) * -0.5, i = [
      r,
      s,
      s,
      0,
      0,
      s,
      r,
      s,
      0,
      0,
      s,
      s,
      r,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(i, t);
  }
  desaturate() {
    this.saturate(-1);
  }
  negative(e) {
    this._loadMatrix([
      -1,
      0,
      0,
      1,
      0,
      0,
      -1,
      0,
      1,
      0,
      0,
      0,
      -1,
      1,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  sepia(e) {
    this._loadMatrix([
      0.393,
      0.7689999,
      0.18899999,
      0,
      0,
      0.349,
      0.6859999,
      0.16799999,
      0,
      0,
      0.272,
      0.5339999,
      0.13099999,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  technicolor(e) {
    this._loadMatrix([
      1.9125277891456083,
      -0.8545344976951645,
      -0.09155508482755585,
      0,
      11.793603434377337,
      -0.3087833385928097,
      1.7658908555458428,
      -0.10601743074722245,
      0,
      -70.35205161461398,
      -0.231103377548616,
      -0.7501899197440212,
      1.847597816108189,
      0,
      30.950940869491138,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  polaroid(e) {
    this._loadMatrix([
      1.438,
      -0.062,
      -0.062,
      0,
      0,
      -0.122,
      1.378,
      -0.122,
      0,
      0,
      -0.016,
      -0.016,
      1.483,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  toBGR(e) {
    this._loadMatrix([
      0,
      0,
      1,
      0,
      0,
      0,
      1,
      0,
      0,
      0,
      1,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  kodachrome(e) {
    this._loadMatrix([
      1.1285582396593525,
      -0.3967382283601348,
      -0.03992559172921793,
      0,
      63.72958762196502,
      -0.16404339962244616,
      1.0835251566291304,
      -0.05498805115633132,
      0,
      24.732407896706203,
      -0.16786010706155763,
      -0.5603416277695248,
      1.6014850761964943,
      0,
      35.62982807460946,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  browni(e) {
    this._loadMatrix([
      0.5997023498159715,
      0.34553243048391263,
      -0.2708298674538042,
      0,
      47.43192855600873,
      -0.037703249837783157,
      0.8609577587992641,
      0.15059552388459913,
      0,
      -36.96841498319127,
      0.24113635128153335,
      -0.07441037908422492,
      0.44972182064877153,
      0,
      -7.562075277591283,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  vintage(e) {
    this._loadMatrix([
      0.6279345635605994,
      0.3202183420819367,
      -0.03965408211312453,
      0,
      9.651285835294123,
      0.02578397704808868,
      0.6441188644374771,
      0.03259127616149294,
      0,
      7.462829176470591,
      0.0466055556782719,
      -0.0851232987247891,
      0.5241648018700465,
      0,
      5.159190588235296,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  colorTone(e, t, r, s, i) {
    e = e || 0.2, t = t || 0.15, r = r || 16770432, s = s || 3375104;
    const n = Ae.shared, [a, o, h] = n.setValue(r).toArray(), [l, c, u] = n.setValue(s).toArray(), d = [
      0.3,
      0.59,
      0.11,
      0,
      0,
      a,
      o,
      h,
      e,
      0,
      l,
      c,
      u,
      t,
      0,
      a - l,
      o - c,
      h - u,
      0,
      0
    ];
    this._loadMatrix(d, i);
  }
  night(e, t) {
    e = e || 0.1;
    const r = [
      e * -2,
      -e,
      0,
      0,
      0,
      -e,
      0,
      e,
      0,
      0,
      0,
      e,
      e * 2,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(r, t);
  }
  predator(e, t) {
    const r = [
      11.224130630493164 * e,
      -4.794486999511719 * e,
      -2.8746118545532227 * e,
      0 * e,
      0.40342438220977783 * e,
      -3.6330697536468506 * e,
      9.193157196044922 * e,
      -2.951810836791992 * e,
      0 * e,
      -1.316135048866272 * e,
      -3.2184197902679443 * e,
      -4.2375030517578125 * e,
      7.476448059082031 * e,
      0 * e,
      0.8044459223747253 * e,
      0,
      0,
      0,
      1,
      0
    ];
    this._loadMatrix(r, t);
  }
  lsd(e) {
    this._loadMatrix([
      2,
      -0.4,
      0.5,
      0,
      0,
      -0.5,
      2,
      -0.4,
      0,
      0,
      -0.4,
      -0.5,
      3,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], e);
  }
  reset() {
    this._loadMatrix([
      1,
      0,
      0,
      0,
      0,
      0,
      1,
      0,
      0,
      0,
      0,
      0,
      1,
      0,
      0,
      0,
      0,
      0,
      1,
      0
    ], !1);
  }
  get matrix() {
    return this.uniforms.m;
  }
  set matrix(e) {
    this.uniforms.m = e;
  }
  get alpha() {
    return this.uniforms.uAlpha;
  }
  set alpha(e) {
    this.uniforms.uAlpha = e;
  }
};
dr.prototype.grayscale = dr.prototype.greyscale;
var rr = new Ft(), Wl = new Uint16Array([
  0,
  1,
  2,
  0,
  2,
  3
]), Lr = class za extends Ie {
  constructor(t) {
    super(), this._anchor = new Xe(this._onAnchorUpdate, this, t ? t.defaultAnchor.x : 0, t ? t.defaultAnchor.y : 0), this._texture = null, this._width = 0, this._height = 0, this._tintColor = new Ae(16777215), this._tintRGB = null, this.tint = 16777215, this.blendMode = st.NORMAL, this._cachedTint = 16777215, this.uvs = null, this.texture = t || We.EMPTY, this.vertexData = /* @__PURE__ */ new Float32Array(8), this.vertexTrimmedData = null, this._transformID = -1, this._textureID = -1, this._transformTrimmedID = -1, this._textureTrimmedID = -1, this.indices = Wl, this.pluginName = "batch", this.isSprite = !0, this._roundPixels = dt.ROUND_PIXELS;
  }
  _onTextureUpdate() {
    this._textureID = -1, this._textureTrimmedID = -1, this._cachedTint = 16777215, this._width && (this.scale.x = Tr(this.scale.x) * this._width / this._texture.orig.width), this._height && (this.scale.y = Tr(this.scale.y) * this._height / this._texture.orig.height);
  }
  _onAnchorUpdate() {
    this._transformID = -1, this._transformTrimmedID = -1;
  }
  calculateVertices() {
    const t = this._texture;
    if (this._transformID === this.transform._worldID && this._textureID === t._updateID) return;
    this._textureID !== t._updateID && (this.uvs = this._texture._uvs.uvsFloat32), this._transformID = this.transform._worldID, this._textureID = t._updateID;
    const r = this.transform.worldTransform, s = r.a, i = r.b, n = r.c, a = r.d, o = r.tx, h = r.ty, l = this.vertexData, c = t.trim, u = t.orig, d = this._anchor;
    let y = 0, m = 0, v = 0, p = 0;
    if (c ? (m = c.x - d._x * u.width, y = m + c.width, p = c.y - d._y * u.height, v = p + c.height) : (m = -d._x * u.width, y = m + u.width, p = -d._y * u.height, v = p + u.height), l[0] = s * m + n * p + o, l[1] = a * p + i * m + h, l[2] = s * y + n * p + o, l[3] = a * p + i * y + h, l[4] = s * y + n * v + o, l[5] = a * v + i * y + h, l[6] = s * m + n * v + o, l[7] = a * v + i * m + h, this._roundPixels) {
      const x = dt.RESOLUTION;
      for (let f = 0; f < l.length; ++f) l[f] = Math.round(l[f] * x) / x;
    }
  }
  calculateTrimmedVertices() {
    if (!this.vertexTrimmedData) this.vertexTrimmedData = /* @__PURE__ */ new Float32Array(8);
    else if (this._transformTrimmedID === this.transform._worldID && this._textureTrimmedID === this._texture._updateID) return;
    this._transformTrimmedID = this.transform._worldID, this._textureTrimmedID = this._texture._updateID;
    const t = this._texture, r = this.vertexTrimmedData, s = t.orig, i = this._anchor, n = this.transform.worldTransform, a = n.a, o = n.b, h = n.c, l = n.d, c = n.tx, u = n.ty, d = -i._x * s.width, y = d + s.width, m = -i._y * s.height, v = m + s.height;
    if (r[0] = a * d + h * m + c, r[1] = l * m + o * d + u, r[2] = a * y + h * m + c, r[3] = l * m + o * y + u, r[4] = a * y + h * v + c, r[5] = l * v + o * y + u, r[6] = a * d + h * v + c, r[7] = l * v + o * d + u, this._roundPixels) {
      const p = dt.RESOLUTION;
      for (let x = 0; x < r.length; ++x) r[x] = Math.round(r[x] * p) / p;
    }
  }
  _render(t) {
    this.calculateVertices(), t.batch.setObjectRenderer(t.plugins[this.pluginName]), t.plugins[this.pluginName].render(this);
  }
  _calculateBounds() {
    const t = this._texture.trim, r = this._texture.orig;
    !t || t.width === r.width && t.height === r.height ? (this.calculateVertices(), this._bounds.addQuad(this.vertexData)) : (this.calculateTrimmedVertices(), this._bounds.addQuad(this.vertexTrimmedData));
  }
  getLocalBounds(t) {
    return this.children.length === 0 ? (this._localBounds || (this._localBounds = new Ws()), this._localBounds.minX = this._texture.orig.width * -this._anchor._x, this._localBounds.minY = this._texture.orig.height * -this._anchor._y, this._localBounds.maxX = this._texture.orig.width * (1 - this._anchor._x), this._localBounds.maxY = this._texture.orig.height * (1 - this._anchor._y), t || (this._localBoundsRect || (this._localBoundsRect = new Et()), t = this._localBoundsRect), this._localBounds.getRectangle(t)) : super.getLocalBounds.call(this, t);
  }
  containsPoint(t) {
    this.worldTransform.applyInverse(t, rr);
    const r = this._texture.orig.width, s = this._texture.orig.height, i = -r * this.anchor.x;
    let n = 0;
    return rr.x >= i && rr.x < i + r && (n = -s * this.anchor.y, rr.y >= n && rr.y < n + s);
  }
  destroy(t) {
    if (super.destroy(t), this._texture.off("update", this._onTextureUpdate, this), this._anchor = null, typeof t == "boolean" ? t : t?.texture) {
      const r = typeof t == "boolean" ? t : t?.baseTexture;
      this._texture.destroy(!!r);
    }
    this._texture = null;
  }
  static from(t, r) {
    const s = t instanceof We ? t : We.from(t, r);
    return new za(s);
  }
  set roundPixels(t) {
    this._roundPixels !== t && (this._transformID = -1, this._transformTrimmedID = -1), this._roundPixels = t;
  }
  get roundPixels() {
    return this._roundPixels;
  }
  get width() {
    return Math.abs(this.scale.x) * this._texture.orig.width;
  }
  set width(t) {
    const r = Tr(this.scale.x) || 1;
    this.scale.x = r * t / this._texture.orig.width, this._width = t;
  }
  get height() {
    return Math.abs(this.scale.y) * this._texture.orig.height;
  }
  set height(t) {
    const r = Tr(this.scale.y) || 1;
    this.scale.y = r * t / this._texture.orig.height, this._height = t;
  }
  get anchor() {
    return this._anchor;
  }
  set anchor(t) {
    this._anchor.copyFrom(t);
  }
  get tint() {
    return this._tintColor.value;
  }
  set tint(t) {
    this._tintColor.setValue(t), this._tintRGB = this._tintColor.toLittleEndianNumber();
  }
  get tintValue() {
    return this._tintColor.toNumber();
  }
  get texture() {
    return this._texture;
  }
  set texture(t) {
    this._texture !== t && (this._texture && this._texture.off("update", this._onTextureUpdate, this), this._texture = t || We.EMPTY, this._cachedTint = 16777215, this._textureID = -1, this._textureTrimmedID = -1, t && (t.baseTexture.valid ? this._onTextureUpdate() : t.once("update", this._onTextureUpdate, this)));
  }
}, { defineProperty: ql } = Object, $a = typeof self == "object" ? self : globalThis, on = (e, t) => {
  switch (e) {
    case "Function":
    case "SharedWorker":
    case "Worker":
    case "eval":
    case "setInterval":
    case "setTimeout":
      throw new TypeError("unable to deserialize " + e);
  }
  return new $a[e](t);
}, jl = (e, t) => {
  const r = (i, n) => (e.set(n, i), i), s = (i) => {
    if (e.has(i)) return e.get(i);
    const [n, a] = t[i];
    switch (n) {
      case 0:
      case -1:
        return r(a, i);
      case 1: {
        const o = r([], i);
        for (const h of a) o.push(s(h));
        return o;
      }
      case 2: {
        const o = r({}, i);
        for (const [h, l] of a) {
          const c = s(h), u = s(l);
          c === "__proto__" ? ql(o, c, {
            value: u,
            configurable: !0,
            enumerable: !0,
            writable: !0
          }) : o[c] = u;
        }
        return o;
      }
      case 3:
        return r(new Date(a), i);
      case 4: {
        const { source: o, flags: h } = a;
        return r(new RegExp(o, h), i);
      }
      case 5: {
        const o = r(/* @__PURE__ */ new Map(), i);
        for (const [h, l] of a) o.set(s(h), s(l));
        return o;
      }
      case 6: {
        const o = r(/* @__PURE__ */ new Set(), i);
        for (const h of a) o.add(s(h));
        return o;
      }
      case 7: {
        const { name: o, message: h } = a;
        return r(typeof $a[o] == "function" ? on(o, h) : new Error(h), i);
      }
      case 8:
        return r(BigInt(a), i);
      case "BigInt":
        return r(Object(BigInt(a)), i);
      case "ArrayBuffer":
        return r(new Uint8Array(a).buffer, a);
      case "DataView": {
        const { buffer: o } = new Uint8Array(a);
        return r(new DataView(o), a);
      }
      case "-0":
        return -0;
    }
    return r(on(n, a), i);
  };
  return s;
}, hn = (e) => jl(/* @__PURE__ */ new Map(), e)(0), Te = "", { toString: Yl } = {}, { keys: Kl, is: Zl } = Object, sr = (e) => {
  const t = typeof e;
  if (t !== "object" || !e) return [0, t];
  const r = Yl.call(e).slice(8, -1);
  switch (r) {
    case "Array":
      return [1, Te];
    case "Object":
      return [2, Te];
    case "Date":
      return [3, Te];
    case "RegExp":
      return [4, Te];
    case "Map":
      return [5, Te];
    case "Set":
      return [6, Te];
    case "DataView":
      return [1, r];
  }
  return r.includes("Array") ? [1, r] : e instanceof Error ? [7, e.name || "Error"] : [2, r];
}, Fr = ([e, t]) => e === 0 && (t === "function" || t === "symbol"), Jl = (e, t, r, s) => {
  const i = (a, o) => {
    const h = s.push(a) - 1;
    return r.set(o, h), h;
  }, n = (a) => {
    if (r.has(a)) return r.get(a);
    let [o, h] = sr(a);
    switch (o) {
      case 0: {
        let c = a;
        switch (h) {
          case "bigint":
            o = 8, c = a.toString();
            break;
          case "number":
            if (!a && Zl(a, -0)) return s.push(["-0"]) - 1;
            break;
          case "function":
          case "symbol":
            if (e) throw new TypeError("unable to serialize " + h);
            c = null;
            break;
          case "undefined":
            return i([-1], a);
        }
        return i([o, c], a);
      }
      case 1: {
        if (h) {
          let d = a;
          return h === "DataView" ? d = new Uint8Array(a.buffer) : h === "ArrayBuffer" && (d = new Uint8Array(a)), i([h, [...d]], a);
        }
        const c = [], u = i([o, c], a);
        for (const d of a) c.push(n(d));
        return u;
      }
      case 2: {
        if (h) switch (h) {
          case "BigInt":
            return i([h, a.toString()], a);
          case "Boolean":
          case "Number":
          case "String":
            return i([h, a.valueOf()], a);
        }
        if (t && "toJSON" in a) return n(a.toJSON());
        const c = [], u = i([o, c], a);
        for (const d of Kl(a)) (e || !Fr(sr(a[d]))) && c.push([n(d), n(a[d])]);
        return u;
      }
      case 3:
        return i([o, isNaN(a.getTime()) ? Te : a.toISOString()], a);
      case 4: {
        const { source: c, flags: u } = a;
        return i([o, {
          source: c,
          flags: u
        }], a);
      }
      case 5: {
        const c = [], u = i([o, c], a);
        for (const [d, y] of a) (e || !(Fr(sr(d)) || Fr(sr(y)))) && c.push([n(d), n(y)]);
        return u;
      }
      case 6: {
        const c = [], u = i([o, c], a);
        for (const d of a) (e || !Fr(sr(d))) && c.push(n(d));
        return u;
      }
    }
    const { message: l } = a;
    return i([o, {
      name: h,
      message: l
    }], a);
  };
  return n;
}, ln = (e, { json: t, lossy: r } = {}) => {
  const s = [];
  return Jl(!(t || r), !!t, /* @__PURE__ */ new Map(), s)(e), s;
}, cn = typeof structuredClone == "function" ? (
  /* c8 ignore start */
  (e, t) => t && ("json" in t || "lossy" in t) ? hn(ln(e, t)) : structuredClone(e)
) : (e, t) => hn(ln(e, t));
function un(e) {
  return e;
}
var { cbrt: Es, sqrt: Ss, PI: De } = Math, Ql = (e, t, r, s, i) => {
  const n = t + r * e, a = n ** 2 + s;
  if (a > 0) {
    const c = Ss(a);
    return Es(n + c) + Es(n - c) - i;
  }
  const o = Es(Ss(n * n - a)), h = n ? Math.atan(Ss(-a) / n) : -De / 2;
  let l;
  return r < 0 ? l = (n > 0 ? 2 * De : De) - h : i < 0 ? l = (n > 0 ? 2 * De : -3 * De) + h : l = (n > 0 ? 0 : De) + h, 2 * o * Math.cos(l / 3) - i;
}, tc = (e, t, r, s) => ((t * e + 3 * r) * e + s) * e;
function Va(e, t, r, s) {
  if (!(0 <= e && e <= 1 && 0 <= r && r <= 1)) throw new Error("bezier x values must be in [0, 1] range");
  if (e === t && r === s) return un;
  const i = 6 * (3 * e - 3 * r + 1), n = 6 * (r - 2 * e), a = 3 * e, o = i * i, h = n * n, l = n / i, c = 3 * n * a / o - h * n / (o * i), u = 2 * a / i - h / o, d = u * u * u, y = 3 / i, m = 3 * t - 3 * s + 1, v = s - 2 * t, p = 3 * t, x = i ? Ql : un;
  return function(E) {
    return E === 0 || E === 1 ? E : tc(x(E, c, y, d, l), m, v, p);
  };
}
var oi = /* @__PURE__ */ oo({
  AbstractBaseRenderer: () => Ha,
  BackgroundRender: () => Ec,
  BaseRenderer: () => hi,
  DomLyricPlayer: () => _n,
  LayoutAlignAnchor: () => cr,
  LyricLineMouseEvent: () => Za,
  LyricLineRenderMode: () => ue,
  LyricPlayer: () => _n,
  LyricPlayerBase: () => Ya,
  MaskObsceneWordsMode: () => Ve,
  MeshGradientRenderer: () => bc,
  PixiRenderer: () => wc
}), Ha = class {
};
function dn(e) {
  return Math.max(1, e);
}
var hi = class extends Ha {
  canvas;
  observer;
  flowSpeed = 1;
  currerntRenderScale = 0.75;
  constructor(e) {
    super(), this.canvas = e, this.observer = new ResizeObserver(() => {
      const t = dn(e.clientWidth * window.devicePixelRatio * this.currerntRenderScale), r = dn(e.clientHeight * window.devicePixelRatio * this.currerntRenderScale);
      this.onResize(t, r);
    }), this.observer.observe(e);
  }
  setRenderScale(e) {
    this.currerntRenderScale = e, this.onResize(this.canvas.clientWidth * window.devicePixelRatio * this.currerntRenderScale, this.canvas.clientHeight * window.devicePixelRatio * this.currerntRenderScale);
  }
  onResize(e, t) {
    this.canvas.width = e, this.canvas.height = t;
  }
  setFlowSpeed(e) {
    this.flowSpeed = e;
  }
  dispose() {
    this.observer.disconnect(), this.canvas.remove();
  }
  getElement() {
    return this.canvas;
  }
};
function ec(e) {
  return new Promise((t, r) => {
    const s = document.createElement("img");
    s.onload = () => t(s), s.onerror = r, s.src = e, s.crossOrigin = "anonymous", s.loading = "eager";
  });
}
function rc(e) {
  return new Promise((t, r) => {
    const s = document.createElement("video");
    let i = !1, n = !1, a = !1;
    s.addEventListener("playing", () => {
      i = !0, o();
    }, !0), s.addEventListener("timeupdate", () => {
      n = !0, o();
    }, !0), s.addEventListener("error", (h) => {
      a = !0, r(h);
    }, !0);
    function o() {
      i && n && !a && t(s);
    }
    s.src = e, s.playsInline = !0, s.crossOrigin = "anonymous", s.autoplay = !0, s.loop = !0, s.muted = !0, s.play();
  });
}
function Ys(e, t = !1) {
  return t ? rc(e) : ec(e);
}
function Xa(e) {
  return new Promise((t, r) => {
    (e instanceof HTMLImageElement ? e.complete : e.readyState >= 3) ? t(e) : (e.onload = () => t(e), e.onerror = r);
  });
}
function sc(e, t, r) {
  const s = e.data, i = e.width, n = e.height;
  let a, o, h, l, c, u, d, y, m, v, p, x, f;
  const E = i - 1, g = n - 1, A = t + 1, L = t + A, M = t + 1, C = 1 / (L * (t + M)), k = [], B = [], w = [], F = [], P = [], G = [];
  for (; r-- > 0; ) {
    for (f = x = 0, u = 0; u < n; u++) {
      for (a = s[f] * A, o = s[f + 1] * A, h = s[f + 2] * A, l = s[f + 3] * A, d = 1; d <= t; d++)
        y = f + ((d > E ? E : d) << 2), a += s[y++], o += s[y++], h += s[y++], l += s[y];
      for (c = 0; c < i; c++)
        k[x] = a, B[x] = o, w[x] = h, F[x] = l, u === 0 && (P[c] = Math.min(c + A, E) << 2, G[c] = Math.max(c - t, 0) << 2), m = f + P[c], v = f + G[c], a += s[m++] - s[v++], o += s[m++] - s[v++], h += s[m++] - s[v++], l += s[m] - s[v], x++;
      f += i << 2;
    }
    for (c = 0; c < i; c++) {
      for (p = c, a = k[p] * M, o = B[p] * M, h = w[p] * M, l = F[p] * M, d = 1; d <= t; d++)
        p += d > g ? 0 : i, a += k[p], o += B[p], h += w[p], l += F[p];
      for (x = c << 2, u = 0; u < n; u++)
        s[x] = a * C + 0.5 | 0, s[x + 1] = o * C + 0.5 | 0, s[x + 2] = h * C + 0.5 | 0, s[x + 3] = l * C + 0.5 | 0, c === 0 && (P[u] = Math.min(u + M, g) * i, G[u] = Math.max(u - t, 0) * i), m = c + P[u], v = c + G[u], a += k[m] - k[v], o += B[m] - B[v], h += w[m] - w[v], l += F[m] - F[v], x += i << 2;
    }
  }
}
function de(e, t, r) {
  return Math.min(Math.max(e, t), r);
}
function Xt(e) {
  return de(e, 0, 1);
}
function ee(e) {
  return Math.max(0, e);
}
var D = (e, t, r, s, i = 0, n = 0, a = 1, o = 1) => Object.freeze({
  cx: e,
  cy: t,
  x: r,
  y: s,
  ur: i,
  vr: n,
  up: a,
  vp: o
}), we = (e, t, r) => Object.freeze({
  width: e,
  height: t,
  conf: r
}), fn = [
  we(5, 5, [
    D(0, 0, -1, -1, 0, 0, 1, 1),
    D(1, 0, -0.5, -1, 0, 0, 1, 1),
    D(2, 0, 0, -1, 0, 0, 1, 1),
    D(3, 0, 0.5, -1, 0, 0, 1, 1),
    D(4, 0, 1, -1, 0, 0, 1, 1),
    D(0, 1, -1, -0.5, 0, 0, 1, 1),
    D(1, 1, -0.5, -0.5, 0, 0, 1, 1),
    D(2, 1, -0.0052029684413368305, -0.6131420587090777, 0, 0, 1, 1),
    D(3, 1, 0.5884227308309977, -0.3990805107556692, 0, 0, 1, 1),
    D(4, 1, 1, -0.5, 0, 0, 1, 1),
    D(0, 2, -1, 0, 0, 0, 1, 1),
    D(1, 2, -0.4210024670505933, -0.11895058380429502, 0, 0, 1, 1),
    D(2, 2, -0.1019613423315412, -0.023812118047224606, 0, -47, 0.629, 0.849),
    D(3, 2, 0.40275125660925437, -0.06345314544600389, 0, 0, 1, 1),
    D(4, 2, 1, 0, 0, 0, 1, 1),
    D(0, 3, -1, 0.5, 0, 0, 1, 1),
    D(1, 3, 0.06801958477287173, 0.5205913248960121, -31, -45, 1, 1),
    D(2, 3, 0.21446469120128908, 0.29331610114301043, 6, -56, 0.566, 1.321),
    D(3, 3, 0.5, 0.5, 0, 0, 1, 1),
    D(4, 3, 1, 0.5, 0, 0, 1, 1),
    D(0, 4, -1, 1, 0, 0, 1, 1),
    D(1, 4, -0.31378372841550195, 1, 0, 0, 1, 1),
    D(2, 4, 0.26153633255328046, 1, 0, 0, 1, 1),
    D(3, 4, 0.5, 1, 0, 0, 1, 1),
    D(4, 4, 1, 1, 0, 0, 1, 1)
  ]),
  we(4, 4, [
    D(0, 0, -1, -1, 0, 0, 1, 1),
    D(1, 0, -0.33333333333333337, -1, 0, 0, 1, 1),
    D(2, 0, 0.33333333333333326, -1, 0, 0, 1, 1),
    D(3, 0, 1, -1, 0, 0, 1, 1),
    D(0, 1, -1, -0.04495399932657351, 0, 0, 1, 1),
    D(1, 1, -0.24056117520129328, -0.22465999020104, 0, 0, 1, 1),
    D(2, 1, 0.334758885767489, -0.00531297192779423, 0, 0, 1, 1),
    D(3, 1, 0.9989920470678106, -0.3382976020775408, 8, 0, 0.566, 1.792),
    D(0, 2, -1, 0.33333333333333326, 0, 0, 1, 1),
    D(1, 2, -0.3425497314639411, -27501607956947893e-21, 0, 0, 1, 1),
    D(2, 2, 0.3321437945812673, 0.1981776353859399, 0, 0, 1, 1),
    D(3, 2, 1, 0.0766118180296832, 0, 0, 1, 1),
    D(0, 3, -1, 1, 0, 0, 1, 1),
    D(1, 3, -0.33333333333333337, 1, 0, 0, 1, 1),
    D(2, 3, 0.33333333333333326, 1, 0, 0, 1, 1),
    D(3, 3, 1, 1, 0, 0, 1, 1)
  ]),
  we(4, 4, [
    D(0, 0, -1, -1, 0, 0, 1, 2.075),
    D(1, 0, -0.33333333333333337, -1, 0, 0, 1, 1),
    D(2, 0, 0.33333333333333326, -1, 0, 0, 1, 1),
    D(3, 0, 1, -1, 0, 0, 1, 1),
    D(0, 1, -1, -0.4545779491139603, 0, 0, 1, 1),
    D(1, 1, -0.33333333333333337, -0.33333333333333337, 0, 0, 1, 1),
    D(2, 1, 0.0889403142626457, -0.6025711180694033, -32, 45, 1, 1),
    D(3, 1, 1, -0.33333333333333337, 0, 0, 1, 1),
    D(0, 2, -1, -0.07402408608567845, 1, 0, 1, 0.094),
    D(1, 2, -0.2719422694359541, 0.09775369930903222, 25, -18, 1.321, 0),
    D(2, 2, 0.19877414408395877, 0.4307383294587789, 48, -40, 0.755, 0.975),
    D(3, 2, 1, 0.33333333333333326, -37, 0, 1, 1),
    D(0, 3, -1, 1, 0, 0, 1, 1),
    D(1, 3, -0.33333333333333337, 1, 0, 0, 1, 1),
    D(2, 3, 0.5125850864305672, 1, -20, -18, 0, 1.604),
    D(3, 3, 1, 1, 0, 0, 1, 1)
  ]),
  we(5, 5, [
    D(0, 0, -1, -1, 0, 0, 1, 1),
    D(1, 0, -0.4501953125, -1, 0, 55, 1, 2.075),
    D(2, 0, 0.1953125, -1, 0, 0, 1, 1),
    D(3, 0, 0.4580078125, -1, 0, -25, 1, 1),
    D(4, 0, 1, -1, 0, 0, 1, 1),
    D(0, 1, -1, -0.2514475377525607, -16, 0, 2.327, 0.943),
    D(1, 1, -0.55859375, -0.6609325945787148, 47, 0, 2.358, 0.377),
    D(2, 1, 0.232421875, -0.5244375756366635, -66, -25, 1.855, 1.164),
    D(3, 1, 0.685546875, -0.3753706470552125, 0, 0, 1, 1),
    D(4, 1, 1, -0.6699125300354287, 0, 0, 1, 1),
    D(0, 2, -1, 0.035910396862284255, 0, 0, 1, 1),
    D(1, 2, -0.4921875, 0.005378616309457018, 90, 23, 1, 1.981),
    D(2, 2, 0.021484375, -0.1365043639066228, 0, 42, 1, 1),
    D(3, 2, 0.4765625, 0.05925822904974043, -30, 0, 1.95, 0.44),
    D(4, 2, 1, 0.251428847823418, 0, 0, 1, 1),
    D(0, 3, -1, 0.6968336464764276, -68, 0, 1, 0.786),
    D(1, 3, -0.6904296875, 0.5890744209958608, -68, 0, 1, 1),
    D(2, 3, 0.1845703125, 0.3879238667654693, 61, 0, 1, 1),
    D(3, 3, 0.60546875, 0.4633553246018661, -47, -59, 0.849, 1.73),
    D(4, 3, 1, 0.6214021886400309, -33, 0, 0.377, 1.604),
    D(0, 4, -1, 1, 0, 0, 1, 1),
    D(1, 4, -0.5, 1, 0, -73, 1, 1),
    D(2, 4, -0.3271484375, 1, 0, -24, 0.314, 2.704),
    D(3, 4, 0.5, 1, 0, 0, 1, 1),
    D(4, 4, 1, 1, 0, 0, 1, 1)
  ]),
  we(5, 5, [
    D(0, 0, -1, -1),
    D(1, 0, -0.6393, -1, 0, 0, 1, 2.3884),
    D(2, 0, 0, -1),
    D(3, 0, 0.5, -1),
    D(4, 0, 1, -1),
    D(0, 1, -1, -0.2301),
    D(1, 1, -0.6934, -0.331, 0, -0.7188, 1, 1.063),
    D(2, 1, -82e-4, -0.6814, -0.2583, 0, 1.0964, 1),
    D(3, 1, 0.5836, -0.531, 0.7029, 0, 1.5466, 1),
    D(4, 1, 1, -0.6407),
    D(0, 2, -1, 0.2973, 0, 0, 1.8352, 1),
    D(1, 2, -0.4082, 0.0602),
    D(2, 2, -0.1803, -0.3646, -0.2998, 0, 1.1513, 1),
    D(3, 2, 0.477, -0.1027, 0.8903, -0.1882, 1.0807, 0.8551),
    D(4, 2, 1, -0.2973),
    D(0, 3, -1, 0.7628, 0, 0, 2.3868, 1),
    D(1, 3, -0.2525, 0.4814, -0.8406, -1.6199, 1.4093, 1.2215),
    D(2, 3, 0.3607, 0.2814, -1.0713, -0.0529, 1.0025, 0.7611),
    D(3, 3, 0.4885, 0.623, 0, 0.8184, 1, 1.2876),
    D(4, 3, 1, 0.5),
    D(0, 4, -1, 1),
    D(1, 4, -0.4033, 1),
    D(2, 4, 0.2672, 1),
    D(3, 4, 0.5967, 1),
    D(4, 4, 1, 1)
  ]),
  we(5, 5, [
    D(0, 0, -1, -1),
    D(1, 0, -0.2197, -1),
    D(2, 0, 0.0197, -1),
    D(3, 0, 0.8033, -1),
    D(4, 0, 1, -1),
    D(0, 1, -1, -0.5451),
    D(1, 1, -0.4885, -0.4035, -1.0246, -0.2268, 1.1936, 0.8005),
    D(2, 1, -0.1213, -0.2867, 0, -0.6981, 1, 0.809),
    D(3, 1, 0.3246, -0.5628, 0, -1.2188, 1, 1.044),
    D(4, 1, 1, -0.3292),
    D(0, 2, -1, 0.1416),
    D(1, 2, -0.341, -0.0142, 0, -0.4004, 1, 1.1293),
    D(2, 2, -0.0393, -0.023, 0.2915, -0.373, 1.044, 0.9879),
    D(3, 2, 0.3148, -0.0673, -0.7853, -0.8962, 1.4709, 1.0247),
    D(4, 2, 1, 0.1912),
    D(0, 3, -1, 0.5),
    D(1, 3, -0.2689, 0.2743, 0.3404, -0.5248, 1.0184, 0.4391),
    D(2, 3, 0.0721, 0.269, 0.5302, 0.1244, 0.6723, 0.3225),
    D(3, 3, 0.4148, 0.3894, -0.6977, -0.6783, 0.8094, 0.9247),
    D(4, 3, 1, 0.446),
    D(0, 4, -1, 1),
    D(1, 4, -0.7311, 1),
    D(2, 4, 0.323, 1),
    D(3, 4, 0.6393, 1),
    D(4, 4, 1, 1)
  ])
], Ot = (e, t) => Math.random() * (t - e) + e;
function ic(e, t, r) {
  const s = Xt((r - e) / (t - e));
  return s * s * (3 - 2 * s);
}
function nc(e, t, r, s = 2, i = 0.5, n = 0.1) {
  let a = [], o = i;
  for (let c = 0; c < r; c++) {
    a[c] = [];
    for (let u = 0; u < t; u++) a[c][u] = e[c * t + u];
  }
  const h = [
    [
      1,
      2,
      1
    ],
    [
      2,
      4,
      2
    ],
    [
      1,
      2,
      1
    ]
  ], l = 16;
  for (let c = 0; c < s; c++) {
    const u = [];
    for (let d = 0; d < r; d++) {
      u[d] = [];
      for (let y = 0; y < t; y++) {
        if (y === 0 || y === t - 1 || d === 0 || d === r - 1) {
          u[d][y] = a[d][y];
          continue;
        }
        let m = 0, v = 0, p = 0, x = 0, f = 0, E = 0;
        for (let Q = -1; Q <= 1; Q++) for (let _ = -1; _ <= 1; _++) {
          const T = h[Q + 1][_ + 1], b = a[d + Q][y + _];
          m += b.x * T, v += b.y * T, p += b.ur * T, x += b.vr * T, f += b.up * T, E += b.vp * T;
        }
        const g = m / l, A = v / l, L = p / l, M = x / l, C = f / l, k = E / l, B = a[d][y], w = B.x * (1 - o) + g * o, F = B.y * (1 - o) + A * o, P = B.ur * (1 - o) + L * o, G = B.vr * (1 - o) + M * o, $ = B.up * (1 - o) + C * o, H = B.vp * (1 - o) + k * o;
        u[d][y] = D(y, d, w, F, P, G, $, H);
      }
    }
    a = u, o = Xt(o + n);
  }
  for (let c = 0; c < r; c++) for (let u = 0; u < t; u++) e[c * t + u] = a[c][u];
}
function Nr(e, t) {
  return ac(Math.sin(e * 12.9898 + t * 78.233) * 43758.5453);
}
function ac(e) {
  return e - Math.floor(e);
}
function oc(e, t) {
  const r = Math.floor(e), s = Math.floor(t), i = r + 1, n = s + 1, a = e - r, o = t - s, h = a * a * (3 - 2 * a), l = o * o * (3 - 2 * o), c = Nr(r, s), u = Nr(i, s), d = Nr(r, n), y = Nr(i, n), m = c * (1 - h) + u * h, v = d * (1 - h) + y * h;
  return m * (1 - l) + v * l;
}
function hc(e, t, r, s = 1e-3) {
  const i = e(t + s, r), n = e(t - s, r), a = e(t, r + s), o = e(t, r - s), h = (i - n) / (2 * s), l = (a - o) / (2 * s), c = Math.sqrt(h * h + l * l) || 1;
  return [h / c, l / c];
}
function lc(e, t, r = Ot(0.4, 0.6), s = Ot(0.3, 0.6), i = 0.8, n = Math.floor(Ot(3, 5)), a = Ot(0.2, 0.3), o = Ot(-0.1, -0.05)) {
  const h = e ?? Math.floor(Ot(3, 6)), l = t ?? Math.floor(Ot(3, 6)), c = [], u = h === 1 ? 0 : 2 / (h - 1), d = l === 1 ? 0 : 2 / (l - 1);
  for (let y = 0; y < l; y++) for (let m = 0; m < h; m++) {
    const v = (h === 1 ? 0 : m / (h - 1)) * 2 - 1, p = (l === 1 ? 0 : y / (l - 1)) * 2 - 1, x = m === 0 || m === h - 1 || y === 0 || y === l - 1, f = x ? 0 : Ot(-r * u, r * u), E = x ? 0 : Ot(-r * d, r * d);
    let g = v + f, A = p + E;
    const L = x ? 0 : Ot(-60, 60), M = x ? 0 : Ot(-60, 60), C = x ? 1 : Ot(0.8, 1.2), k = x ? 1 : Ot(0.8, 1.2);
    if (!x) {
      const B = (v + 1) / 2, w = (p + 1) / 2, [F, P] = hc(oc, B, w, 1e-3);
      let G = F * s, $ = P * s;
      const H = ic(0, 1, Math.min(B, 1 - B, w, 1 - w));
      G *= H, $ *= H, g = g * (1 - i) + (g + G) * i, A = A * (1 - i) + (A + $) * i;
    }
    c.push(D(m, y, g, A, L, M, C, k));
  }
  return nc(c, h, l, n, a, o), we(h, l, c);
}
var cc = `precision mediump float;

varying vec3 v_color;
varying vec2 v_uv;
uniform sampler2D u_texture;
uniform float u_volume;
uniform float u_alpha;
uniform float u_sinAngle;
uniform float u_cosAngle;

// 预计算常量
const float INV_255 = 1.0 / 255.0;
const float HALF_INV_255 = 0.5 / 255.0;
const float GRADIENT_NOISE_A = 52.9829189;
const vec2 GRADIENT_NOISE_B = vec2(0.06711056, 0.00583715);

float gradientNoise(in vec2 uv) {
    return fract(GRADIENT_NOISE_A * fract(dot(uv, GRADIENT_NOISE_B)));
}

void main() {
    float volumeEffect = u_volume * 2.0;

    float dither = INV_255 * gradientNoise(gl_FragCoord.xy) - HALF_INV_255;

    vec2 centeredUV = v_uv - vec2(0.2);

    vec2 rotatedUV = vec2(
        u_cosAngle * centeredUV.x - u_sinAngle * centeredUV.y,
        u_sinAngle * centeredUV.x + u_cosAngle * centeredUV.y
    );

    vec2 finalUV = rotatedUV * max(0.001, 1.0 - volumeEffect) + vec2(0.5);
    
    vec4 result = texture2D(u_texture, finalUV);
    
    float alphaVolumeFactor = u_alpha * max(0.5, 1.0 - u_volume * 0.5);
    result.rgb *= v_color * alphaVolumeFactor;
    result.a *= alphaVolumeFactor;
    
    result.rgb += vec3(dither);
    
    float dist = distance(v_uv, vec2(0.5));
    float vignette = smoothstep(0.8, 0.3, dist);
    float mask = 0.6 + vignette * 0.4;
    result.rgb *= mask;
    
    gl_FragColor = result;
}
`, uc = `precision mediump float;

attribute vec2 a_pos;
attribute vec3 a_color;
attribute vec2 a_uv;
varying vec3 v_color;
varying vec2 v_uv;

uniform float u_aspect;

void main() {
    v_color = a_color;
    v_uv = a_uv;
    vec2 pos = a_pos;
    if (u_aspect > 1.0) {
        pos.y *= u_aspect;
    } else {
        pos.x /= u_aspect;
    }
    gl_Position = vec4(pos, 0.0, 1.0);
}
`, dc = `
attribute vec2 a_pos;
varying vec2 v_uv;
void main() {
    gl_Position = vec4(a_pos, 0.0, 1.0);
    v_uv = a_pos * 0.5 + 0.5;
}
`, fc = `
precision mediump float;
varying vec2 v_uv;
uniform sampler2D u_texture;
uniform float u_alpha;
void main() {
    vec4 color = texture2D(u_texture, v_uv);
    gl_FragColor = vec4(color.rgb, color.a * u_alpha);
}
`;
function pc(e) {
  return -(Math.cos(Math.PI * e) - 1) / 2;
}
var pn = class {
  label;
  gl;
  program;
  vertexShader;
  fragmentShader;
  attrs;
  constructor(e, t, r, s = "unknown") {
    this.label = s, this.gl = e, this.vertexShader = this.createShader(e.VERTEX_SHADER, t), this.fragmentShader = this.createShader(e.FRAGMENT_SHADER, r), this.program = this.createProgram();
    const i = e.getProgramParameter(this.program, e.ACTIVE_ATTRIBUTES), n = {};
    for (let a = 0; a < i; a++) {
      const o = e.getActiveAttrib(this.program, a);
      if (!o) continue;
      const h = e.getAttribLocation(this.program, o.name);
      h !== -1 && (n[o.name] = h);
    }
    this.attrs = n;
  }
  createShader(e, t) {
    const r = this.gl, s = r.createShader(e);
    if (!s) throw new Error("Failed to create shader");
    if (r.shaderSource(s, t), r.compileShader(s), !r.getShaderParameter(s, r.COMPILE_STATUS)) throw new Error(`Failed to compile shader for type ${e} "${this.label}": ${r.getShaderInfoLog(s)}`);
    return s;
  }
  createProgram() {
    const e = this.gl, t = e.createProgram();
    if (!t) throw new Error("Failed to create program");
    if (e.attachShader(t, this.vertexShader), e.attachShader(t, this.fragmentShader), e.linkProgram(t), e.validateProgram(t), !e.getProgramParameter(t, e.LINK_STATUS)) {
      const r = e.getProgramInfoLog(t);
      throw e.deleteProgram(t), new Error(`Failed to link program "${this.label}": ${r}`);
    }
    return t;
  }
  use() {
    this.gl.useProgram(this.program);
  }
  notFoundUniforms = /* @__PURE__ */ new Set();
  warnUniformNotFound(e) {
    this.notFoundUniforms.has(e) || (this.notFoundUniforms.add(e), console.warn(`Failed to get uniform location for program "${this.label}": ${e}`));
  }
  setUniform1f(e, t) {
    const r = this.gl, s = r.getUniformLocation(this.program, e);
    s ? r.uniform1f(s, t) : this.warnUniformNotFound(e);
  }
  setUniform2f(e, t, r) {
    const s = this.gl, i = s.getUniformLocation(this.program, e);
    i ? s.uniform2f(i, t, r) : this.warnUniformNotFound(e);
  }
  setUniform1i(e, t) {
    const r = this.gl, s = r.getUniformLocation(this.program, e);
    s ? r.uniform1i(s, t) : this.warnUniformNotFound(e);
  }
  dispose() {
    const e = this.gl;
    e.deleteShader(this.vertexShader), e.deleteShader(this.fragmentShader), e.deleteProgram(this.program);
  }
}, mc = class {
  gl;
  attrPos;
  attrColor;
  attrUV;
  vertexWidth = 0;
  vertexHeight = 0;
  vertexBuffer;
  indexBuffer;
  vertexData;
  indexData;
  vertexIndexLength = 0;
  wireFrame = !1;
  constructor(e, t, r, s) {
    this.gl = e, this.attrPos = t, this.attrColor = r, this.attrUV = s;
    const i = e.createBuffer();
    if (!i) throw new Error("Failed to create vertex buffer");
    this.vertexBuffer = i;
    const n = e.createBuffer();
    if (!n) throw new Error("Failed to create index buffer");
    this.indexBuffer = n, this.bind(), this.vertexData = /* @__PURE__ */ new Float32Array(0), this.indexData = /* @__PURE__ */ new Uint16Array(0), this.resize(2, 2), this.update();
  }
  setWireFrame(e) {
    this.wireFrame = e, this.resize(this.vertexWidth, this.vertexHeight);
  }
  setVertexPos(e, t, r, s) {
    const i = (e + t * this.vertexWidth) * 7;
    if (i >= this.vertexData.length - 1) {
      console.warn("Vertex position out of range", i, this.vertexData.length);
      return;
    }
    this.vertexData[i] = r, this.vertexData[i + 1] = s;
  }
  setVertexColor(e, t, r, s, i) {
    const n = (e + t * this.vertexWidth) * 7 + 2;
    if (n >= this.vertexData.length - 2) {
      console.warn("Vertex color out of range", n, this.vertexData.length);
      return;
    }
    this.vertexData[n] = r, this.vertexData[n + 1] = s, this.vertexData[n + 2] = i;
  }
  setVertexUV(e, t, r, s) {
    const i = (e + t * this.vertexWidth) * 7 + 5;
    if (i >= this.vertexData.length - 1) {
      console.warn("Vertex UV out of range", i, this.vertexData.length);
      return;
    }
    this.vertexData[i] = r, this.vertexData[i + 1] = s;
  }
  setVertexData(e, t, r, s, i, n, a, o, h) {
    const l = (e + t * this.vertexWidth) * 7;
    if (l >= this.vertexData.length - 6) {
      console.warn("Vertex data out of range", l, this.vertexData.length);
      return;
    }
    const c = this.vertexData;
    c[l] = r, c[l + 1] = s, c[l + 2] = i, c[l + 3] = n, c[l + 4] = a, c[l + 5] = o, c[l + 6] = h;
  }
  getVertexIndexLength() {
    return this.vertexIndexLength;
  }
  draw() {
    const e = this.gl;
    this.wireFrame ? e.drawElements(e.LINES, this.vertexIndexLength, e.UNSIGNED_SHORT, 0) : e.drawElements(e.TRIANGLES, this.vertexIndexLength, e.UNSIGNED_SHORT, 0);
  }
  resize(e, t) {
    this.vertexWidth = e, this.vertexHeight = t, this.vertexIndexLength = e * t * 6, this.wireFrame && (this.vertexIndexLength = e * t * 10);
    const r = new Float32Array(e * t * 7), s = new Uint16Array(this.vertexIndexLength);
    this.vertexData = r, this.indexData = s;
    for (let n = 0; n < t; n++) for (let a = 0; a < e; a++) {
      const o = a / (e - 1) * 2 - 1, h = n / (t - 1) * 2 - 1;
      this.setVertexPos(a, n, o || 0, h || 0), this.setVertexColor(a, n, 1, 1, 1), this.setVertexUV(a, n, a / (e - 1), n / (t - 1));
    }
    for (let n = 0; n < t - 1; n++) for (let a = 0; a < e - 1; a++) if (this.wireFrame) {
      const o = (n * e + a) * 10;
      s[o] = n * e + a, s[o + 1] = n * e + a + 1, s[o + 2] = n * e + a + 1, s[o + 3] = (n + 1) * e + a, s[o + 4] = (n + 1) * e + a, s[o + 5] = (n + 1) * e + a + 1, s[o + 6] = (n + 1) * e + a + 1, s[o + 7] = n * e + a + 1, s[o + 8] = n * e + a, s[o + 9] = (n + 1) * e + a;
    } else {
      const o = (n * e + a) * 6;
      s[o] = n * e + a, s[o + 1] = n * e + a + 1, s[o + 2] = (n + 1) * e + a, s[o + 3] = n * e + a + 1, s[o + 4] = (n + 1) * e + a + 1, s[o + 5] = (n + 1) * e + a;
    }
    const i = this.gl;
    i.bindBuffer(i.ELEMENT_ARRAY_BUFFER, this.indexBuffer), i.bufferData(i.ELEMENT_ARRAY_BUFFER, this.indexData, i.STATIC_DRAW);
  }
  bind() {
    const e = this.gl;
    e.bindBuffer(e.ARRAY_BUFFER, this.vertexBuffer), e.bindBuffer(e.ELEMENT_ARRAY_BUFFER, this.indexBuffer), this.attrPos !== void 0 && (e.vertexAttribPointer(this.attrPos, 2, e.FLOAT, !1, 28, 0), e.enableVertexAttribArray(this.attrPos)), this.attrColor !== void 0 && (e.vertexAttribPointer(this.attrColor, 3, e.FLOAT, !1, 28, 8), e.enableVertexAttribArray(this.attrColor)), this.attrUV !== void 0 && (e.vertexAttribPointer(this.attrUV, 2, e.FLOAT, !1, 28, 20), e.enableVertexAttribArray(this.attrUV));
  }
  update() {
    const e = this.gl;
    e.bindBuffer(e.ARRAY_BUFFER, this.vertexBuffer), e.bufferData(e.ARRAY_BUFFER, this.vertexData, e.DYNAMIC_DRAW);
  }
  dispose() {
    this.gl.deleteBuffer(this.vertexBuffer), this.gl.deleteBuffer(this.indexBuffer);
  }
}, yc = class {
  color = gt.fromValues(1, 1, 1);
  location = ft.fromValues(0, 0);
  uTangent = ft.fromValues(0, 0);
  vTangent = ft.fromValues(0, 0);
  _uRot = 0;
  _vRot = 0;
  _uScale = 1;
  _vScale = 1;
  constructor() {
    Object.seal(this);
  }
  get uRot() {
    return this._uRot;
  }
  get vRot() {
    return this._vRot;
  }
  set uRot(e) {
    this._uRot = e, this.updateUTangent();
  }
  set vRot(e) {
    this._vRot = e, this.updateVTangent();
  }
  get uScale() {
    return this._uScale;
  }
  get vScale() {
    return this._vScale;
  }
  set uScale(e) {
    this._uScale = e, this.updateUTangent();
  }
  set vScale(e) {
    this._vScale = e, this.updateVTangent();
  }
  updateUTangent() {
    this.uTangent[0] = Math.cos(this._uRot) * this._uScale, this.uTangent[1] = Math.sin(this._uRot) * this._uScale;
  }
  updateVTangent() {
    this.vTangent[0] = -Math.sin(this._vRot) * this._vScale, this.vTangent[1] = Math.cos(this._vRot) * this._vScale;
  }
}, Wa = xt.fromValues(2, -2, 1, 1, -3, 3, -2, -1, 0, 0, 1, 0, 1, 0, 0, 0), gc = xt.clone(Wa).transpose();
function mn(e, t, r, s, i, n = xt.create()) {
  const a = (l) => l.location[i], o = (l) => l.uTangent[i], h = (l) => l.vTangent[i];
  return n[0] = a(e), n[1] = a(t), n[2] = h(e), n[3] = h(t), n[4] = a(r), n[5] = a(s), n[6] = h(r), n[7] = h(s), n[8] = o(e), n[9] = o(t), n[10] = 0, n[11] = 0, n[12] = o(r), n[13] = o(s), n[14] = 0, n[15] = 0, n;
}
function As(e, t, r, s, i, n = xt.create()) {
  const a = (o) => o.color[i];
  return n.fill(0), n[0] = a(e), n[1] = a(t), n[4] = a(r), n[5] = a(s), n;
}
var vc = class {
  _width = 0;
  _height = 0;
  _data = [];
  constructor(e, t) {
    this.resize(e, t), Object.seal(this);
  }
  resize(e, t) {
    this._width = e, this._height = t, this._data = new Array(e * t).fill(0);
  }
  set(e, t, r) {
    this._data[e + t * this._width] = r;
  }
  get(e, t) {
    return this._data[e + t * this._width];
  }
  get width() {
    return this._width;
  }
  get height() {
    return this._height;
  }
}, xc = class extends mc {
  _subDivisions = 10;
  _controlPoints = new vc(3, 3);
  constructor(e, t, r, s) {
    super(e, t, r, s), this.resizeControlPoints(3, 3), Object.seal(this);
  }
  setWireFrame(e) {
    super.setWireFrame(e), this.updateMesh();
  }
  resetSubdivition(e) {
    this._subDivisions = e, super.resize((this._controlPoints.width - 1) * e, (this._controlPoints.height - 1) * e);
  }
  resizeControlPoints(e, t) {
    if (!(e >= 2 && t >= 2)) throw new Error("Control points must be larger than 3x3 or equal");
    this._controlPoints.resize(e, t);
    for (let r = 0; r < t; r++) for (let s = 0; s < e; s++) {
      const i = new yc();
      i.location.x = s / (e - 1) * 2 - 1, i.location.y = r / (t - 1) * 2 - 1, i.uTangent.x = 2 / (e - 1), i.vTangent.y = 2 / (t - 1), this._controlPoints.set(s, r, i);
    }
    this.resetSubdivition(this._subDivisions);
  }
  getControlPoint(e, t) {
    return this._controlPoints.get(e, t);
  }
  tempX = xt.create();
  tempY = xt.create();
  tempR = xt.create();
  tempG = xt.create();
  tempB = xt.create();
  tempXAcc = xt.create();
  tempYAcc = xt.create();
  tempRAcc = xt.create();
  tempGAcc = xt.create();
  tempBAcc = xt.create();
  tempUx = ot.create();
  tempUy = ot.create();
  tempUr = ot.create();
  tempUg = ot.create();
  tempUb = ot.create();
  precomputeMatrix(e, t) {
    return t.copy(e).transpose(), xt.mul(t, t, Wa), xt.mul(t, gc, t), t;
  }
  updateMesh() {
    const e = this._subDivisions - 1, t = e * (this._controlPoints.height - 1), r = e * (this._controlPoints.width - 1), s = this._controlPoints.width, i = this._controlPoints.height, n = this._subDivisions, a = 1 / e, o = 1 / r, h = 1 / t, l = new Float32Array(n * 4);
    for (let c = 0; c < n; c++) {
      const u = c * a, d = c * 4;
      l[d] = u ** 3, l[d + 1] = u ** 2, l[d + 2] = u, l[d + 3] = 1;
    }
    for (let c = 0; c < s - 1; c++) for (let u = 0; u < i - 1; u++) {
      const d = this._controlPoints.get(c, u), y = this._controlPoints.get(c, u + 1), m = this._controlPoints.get(c + 1, u), v = this._controlPoints.get(c + 1, u + 1);
      mn(d, y, m, v, "x", this.tempX), mn(d, y, m, v, "y", this.tempY), As(d, y, m, v, "r", this.tempR), As(d, y, m, v, "g", this.tempG), As(d, y, m, v, "b", this.tempB), this.precomputeMatrix(this.tempX, this.tempXAcc), this.precomputeMatrix(this.tempY, this.tempYAcc), this.precomputeMatrix(this.tempR, this.tempRAcc), this.precomputeMatrix(this.tempG, this.tempGAcc), this.precomputeMatrix(this.tempB, this.tempBAcc);
      const p = c / (s - 1), x = u / (i - 1), f = u * n, E = c * n;
      for (let g = 0; g < n; g++) {
        const A = f + g, L = g * 4;
        this.tempUx[0] = l[L], this.tempUx[1] = l[L + 1], this.tempUx[2] = l[L + 2], this.tempUx[3] = l[L + 3], ot.transformMat4(this.tempUx, this.tempUx, this.tempXAcc), this.tempUy[0] = l[L], this.tempUy[1] = l[L + 1], this.tempUy[2] = l[L + 2], this.tempUy[3] = l[L + 3], ot.transformMat4(this.tempUy, this.tempUy, this.tempYAcc), this.tempUr[0] = l[L], this.tempUr[1] = l[L + 1], this.tempUr[2] = l[L + 2], this.tempUr[3] = l[L + 3], ot.transformMat4(this.tempUr, this.tempUr, this.tempRAcc), this.tempUg[0] = l[L], this.tempUg[1] = l[L + 1], this.tempUg[2] = l[L + 2], this.tempUg[3] = l[L + 3], ot.transformMat4(this.tempUg, this.tempUg, this.tempGAcc), this.tempUb[0] = l[L], this.tempUb[1] = l[L + 1], this.tempUb[2] = l[L + 2], this.tempUb[3] = l[L + 3], ot.transformMat4(this.tempUb, this.tempUb, this.tempBAcc);
        for (let M = 0; M < n; M++) {
          const C = E + M, k = M * 4, B = l[k], w = l[k + 1], F = l[k + 2], P = l[k + 3], G = B * this.tempUx[0] + w * this.tempUx[1] + F * this.tempUx[2] + P * this.tempUx[3], $ = B * this.tempUy[0] + w * this.tempUy[1] + F * this.tempUy[2] + P * this.tempUy[3], H = B * this.tempUr[0] + w * this.tempUr[1] + F * this.tempUr[2] + P * this.tempUr[3], Q = B * this.tempUg[0] + w * this.tempUg[1] + F * this.tempUg[2] + P * this.tempUg[3], _ = B * this.tempUb[0] + w * this.tempUb[1] + F * this.tempUb[2] + P * this.tempUb[3], T = p + M * o, b = 1 - x - g * h;
          this.setVertexData(A, C, G, $, H, Q, _, T, b);
        }
      }
    }
    this.update();
  }
}, yn = class {
  gl;
  tex;
  constructor(e, t) {
    this.gl = e;
    const r = e.createTexture();
    if (!r) throw new Error("Failed to create texture");
    this.tex = r, e.activeTexture(e.TEXTURE0), e.bindTexture(e.TEXTURE_2D, r), e.texImage2D(e.TEXTURE_2D, 0, e.RGBA, e.RGBA, e.UNSIGNED_BYTE, t), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_MIN_FILTER, e.LINEAR), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_MAG_FILTER, e.LINEAR), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_WRAP_S, e.MIRRORED_REPEAT), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_WRAP_T, e.MIRRORED_REPEAT);
  }
  bind() {
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.tex);
  }
  dispose() {
    this.gl.deleteTexture(this.tex);
  }
};
function _c(e, t) {
  if ("OffscreenCanvas" in window) return new OffscreenCanvas(e, t);
  const r = document.createElement("canvas");
  return r.width = e, r.height = t, r;
}
var bc = class extends hi {
  gl;
  lastFrameTime = 0;
  frameTime = 0;
  lastTickTime = 0;
  smoothedVolume = 0;
  volume = 0;
  tickHandle = 0;
  maxFPS = 60;
  paused = !1;
  staticMode = !1;
  mainProgram;
  quadProgram;
  quadBuffer;
  fbo = null;
  fboTexture = null;
  manualControl = !1;
  reduceImageSizeCanvas = _c(32, 32);
  targetSize = ft.fromValues(0, 0);
  currentSize = ft.fromValues(0, 0);
  isNoCover = !0;
  meshStates = [];
  _disposed = !1;
  frameCount = 0;
  lastFPSUpdate = 0;
  currentFPS = 0;
  enablePerformanceMonitoring = !1;
  setManualControl(e) {
    this.manualControl = e;
  }
  setWireFrame(e) {
    for (const t of this.meshStates) t.mesh.setWireFrame(e);
  }
  getControlPoint(e, t) {
    return this.meshStates[this.meshStates.length - 1]?.mesh?.getControlPoint(e, t);
  }
  resizeControlPoints(e, t) {
    this.meshStates[this.meshStates.length - 1]?.mesh?.resizeControlPoints(e, t);
  }
  resetSubdivition(e) {
    this.meshStates[this.meshStates.length - 1]?.mesh?.resetSubdivition(e);
  }
  onTick(e) {
    if (this.tickHandle = 0, this.paused || this._disposed) return;
    this.updatePerformanceStats(e);
    const t = 1e3 / this.maxFPS, r = e - this.lastTickTime;
    if (r < t) {
      this.requestTick();
      return;
    }
    Number.isNaN(this.lastFrameTime) && (this.lastFrameTime = e);
    const s = e - this.lastFrameTime;
    this.lastFrameTime = e, this.lastTickTime = e - r % t, this.frameTime += s * this.flowSpeed, this.onRedraw(this.frameTime, s) && this.staticMode ? this.staticMode && (this.lastFrameTime = NaN) : this.requestTick();
  }
  checkIfResize() {
    const [e, t] = [this.targetSize.x, this.targetSize.y], [r, s] = [this.currentSize.x, this.currentSize.y];
    if (e !== r || t !== s) {
      super.onResize(e, t);
      const i = this.gl;
      i.bindFramebuffer(i.FRAMEBUFFER, null), i.viewport(0, 0, e, t), this.currentSize.x = e, this.currentSize.y = t, e > 0 && t > 0 && this.updateFBO(e, t);
    }
  }
  updateFBO(e, t) {
    const r = this.gl;
    this.fbo && r.deleteFramebuffer(this.fbo), this.fboTexture && r.deleteTexture(this.fboTexture), this.fboTexture = r.createTexture(), r.bindTexture(r.TEXTURE_2D, this.fboTexture), r.texImage2D(r.TEXTURE_2D, 0, r.RGBA, e, t, 0, r.RGBA, r.UNSIGNED_BYTE, null), r.texParameteri(r.TEXTURE_2D, r.TEXTURE_MIN_FILTER, r.LINEAR), r.texParameteri(r.TEXTURE_2D, r.TEXTURE_MAG_FILTER, r.LINEAR), r.texParameteri(r.TEXTURE_2D, r.TEXTURE_WRAP_S, r.CLAMP_TO_EDGE), r.texParameteri(r.TEXTURE_2D, r.TEXTURE_WRAP_T, r.CLAMP_TO_EDGE), this.fbo = r.createFramebuffer(), r.bindFramebuffer(r.FRAMEBUFFER, this.fbo), r.framebufferTexture2D(r.FRAMEBUFFER, r.COLOR_ATTACHMENT0, r.TEXTURE_2D, this.fboTexture, 0), r.bindFramebuffer(r.FRAMEBUFFER, null);
  }
  onRedraw(e, t) {
    const r = this.meshStates[this.meshStates.length - 1];
    let s = !1;
    const i = t / 500;
    if (r)
      if (r.mesh.bind(), this.manualControl && r.mesh.updateMesh(), this.isNoCover) {
        let o = !1;
        for (let h = this.meshStates.length - 1; h >= 0; h--) {
          const l = this.meshStates[h];
          l.alpha <= -0.1 ? (l.mesh.dispose(), l.texture.dispose(), this.meshStates.splice(h, 1)) : (l.alpha = Math.max(-0.1, l.alpha - i), o = !0);
        }
        s = !o;
      } else {
        if (r.alpha >= 1.1) {
          const o = this.meshStates.splice(0, this.meshStates.length - 1);
          for (const h of o)
            h.mesh.dispose(), h.texture.dispose();
        } else r.alpha = Math.min(1.1, r.alpha + i);
        s = this.meshStates.length === 1 && r.alpha >= 1.1;
      }
    const n = this.gl;
    if (this.checkIfResize(), !this.fbo) return s;
    n.bindFramebuffer(n.FRAMEBUFFER, null), n.clearColor(0, 0, 0, 0), n.clear(n.COLOR_BUFFER_BIT);
    const a = Math.min(1, t / 100);
    this.smoothedVolume += (this.volume - this.smoothedVolume) * a;
    for (const o of this.meshStates) {
      n.bindFramebuffer(n.FRAMEBUFFER, this.fbo), n.disable(n.BLEND), n.clearColor(0, 0, 0, 0), n.clear(n.COLOR_BUFFER_BIT), this.mainProgram.use(), n.activeTexture(n.TEXTURE0);
      const h = e / 1e4;
      this.mainProgram.setUniform1f("u_aspect", this.manualControl ? 1 : this.canvas.width / this.canvas.height), this.mainProgram.setUniform1i("u_texture", 0), this.mainProgram.setUniform1f("u_volume", this.volume), this.mainProgram.setUniform1f("u_alpha", 1);
      const l = (h + this.volume) * 2;
      this.mainProgram.setUniform1f("u_sinAngle", Math.sin(l)), this.mainProgram.setUniform1f("u_cosAngle", Math.cos(l)), o.texture.bind(), o.mesh.bind(), o.mesh.draw(), n.bindFramebuffer(n.FRAMEBUFFER, null), n.enable(n.BLEND), n.blendFuncSeparate(n.SRC_ALPHA, n.ONE_MINUS_SRC_ALPHA, n.ONE, n.ONE_MINUS_SRC_ALPHA), this.quadProgram.use(), this.quadProgram.setUniform1i("u_texture", 0), this.quadProgram.setUniform1f("u_alpha", pc(Xt(o.alpha))), n.activeTexture(n.TEXTURE0), n.bindTexture(n.TEXTURE_2D, this.fboTexture), n.bindBuffer(n.ARRAY_BUFFER, this.quadBuffer);
      const c = this.quadProgram.attrs.a_pos;
      n.vertexAttribPointer(c, 2, n.FLOAT, !1, 0, 0), n.enableVertexAttribArray(c), n.drawArrays(n.TRIANGLES, 0, 6), n.disableVertexAttribArray(c);
    }
    return n.flush(), s;
  }
  onTickBinded = this.onTick.bind(this);
  requestTick() {
    this._disposed || this.tickHandle === 0 && (this.tickHandle = requestAnimationFrame(this.onTickBinded));
  }
  constructor(e) {
    super(e);
    const t = e.getContext("webgl", { antialias: !0 });
    if (!t) throw new Error("WebGL not supported");
    t.getExtension("EXT_color_buffer_float") || console.warn("EXT_color_buffer_float not supported"), t.getExtension("EXT_float_blend") || console.warn("EXT_float_blend not supported"), t.getExtension("OES_texture_float_linear") || console.warn("OES_texture_float_linear not supported"), t.getExtension("OES_texture_float") || console.warn("OES_texture_float not supported"), this.gl = t, t.enable(t.BLEND), t.blendFuncSeparate(t.SRC_ALPHA, t.ONE_MINUS_SRC_ALPHA, t.ONE, t.ONE_MINUS_SRC_ALPHA), t.enable(t.DEPTH_TEST), t.depthFunc(t.ALWAYS), this.mainProgram = new pn(t, uc, cc, "main-program-mg"), this.quadProgram = new pn(t, dc, fc, "quad-program");
    const r = t.createBuffer();
    if (!r) throw new Error("Failed to create quad buffer");
    this.quadBuffer = r, t.bindBuffer(t.ARRAY_BUFFER, this.quadBuffer), t.bufferData(t.ARRAY_BUFFER, new Float32Array([
      -1,
      -1,
      1,
      -1,
      -1,
      1,
      -1,
      1,
      1,
      -1,
      1,
      1
    ]), t.STATIC_DRAW), this.requestTick();
  }
  onResize(e, t) {
    this.targetSize.x = Math.ceil(e), this.targetSize.y = Math.ceil(t), this.requestTick();
  }
  setStaticMode(e) {
    this.staticMode = e, this.lastFrameTime = performance.now(), this.requestTick();
  }
  setFPS(e) {
    this.maxFPS = e;
  }
  pause() {
    this.tickHandle && (cancelAnimationFrame(this.tickHandle), this.tickHandle = 0), this.paused = !0;
  }
  resume() {
    this.paused = !1, this.requestTick();
  }
  async setAlbum(e, t) {
    if (e === void 0 || typeof e == "string" && e.trim().length === 0) {
      this.isNoCover = !0;
      return;
    }
    let r = null, s = null, i = 5;
    for (; !r && i > 0; ) try {
      typeof e == "string" ? !t && "createImageBitmap" in window ? (s = await (await fetch(e)).blob(), r = await Ys(URL.createObjectURL(s), !1)) : r = await Ys(e, t) : r = await Xa(e);
    } catch (d) {
      console.warn(`failed on loading album resource, retrying (${i})`, {
        albumSource: e,
        error: d
      }), i--;
    }
    if (!r) {
      console.error("Failed to load album resource", e), this.isNoCover = !0;
      return;
    }
    this.isNoCover = !1;
    const n = this.reduceImageSizeCanvas, a = n.getContext("2d", { willReadFrequently: !0 });
    if (!a) throw new Error("Failed to create canvas context");
    a.clearRect(0, 0, n.width, n.height);
    const o = r instanceof HTMLVideoElement ? r.videoWidth : r.naturalWidth, h = r instanceof HTMLVideoElement ? r.videoHeight : r.naturalHeight;
    if (o * h === 0) throw new Error("Invalid image size");
    let l = null;
    try {
      "createImageBitmap" in window && (s ? (l = await createImageBitmap(s, {
        resizeWidth: n.width,
        resizeHeight: n.height,
        resizeQuality: "low"
      }), URL.revokeObjectURL(r.src)) : l = await createImageBitmap(r, {
        resizeWidth: n.width,
        resizeHeight: n.height,
        resizeQuality: "low"
      }));
    } catch (d) {
      console.warn("createImageBitmap failed", d);
    }
    l ? (a.drawImage(l, 0, 0), l.close()) : a.drawImage(r, 0, 0, o, h, 0, 0, n.width, n.height);
    const c = a.getImageData(0, 0, n.width, n.height), u = c.data;
    for (let d = 0; d < u.length; d += 4) {
      let y = u[d], m = u[d + 1], v = u[d + 2];
      y = (y - 128) * 0.4 + 128, m = (m - 128) * 0.4 + 128, v = (v - 128) * 0.4 + 128;
      const p = y * 0.3 + m * 0.59 + v * 0.11;
      y = p * -2 + y * 3, m = p * -2 + m * 3, v = p * -2 + v * 3, y = (y - 128) * 1.7 + 128, m = (m - 128) * 1.7 + 128, v = (v - 128) * 1.7 + 128, u[d] = y * 0.75, u[d + 1] = m * 0.75, u[d + 2] = v * 0.75;
    }
    if (sc(c, 2, 4), this.manualControl && this.meshStates.length > 0)
      this.meshStates[0].texture.dispose(), this.meshStates[0].texture = new yn(this.gl, c);
    else {
      const d = new xc(this.gl, this.mainProgram.attrs.a_pos, this.mainProgram.attrs.a_color, this.mainProgram.attrs.a_uv);
      d.resetSubdivition(50);
      const y = Math.random() > 0.8 ? lc(6, 6) : fn[Math.floor(Math.random() * fn.length)];
      d.resizeControlPoints(y.width, y.height);
      const m = 2 / (y.width - 1), v = 2 / (y.height - 1);
      for (const x of y.conf) {
        const f = d.getControlPoint(x.cx, x.cy);
        f.location.x = x.x, f.location.y = x.y, f.uRot = x.ur * Math.PI / 180, f.vRot = x.vr * Math.PI / 180, f.uScale = m * x.up, f.vScale = v * x.vp;
      }
      d.updateMesh();
      const p = {
        mesh: d,
        texture: new yn(this.gl, c),
        alpha: 0
      };
      this.meshStates.push(p);
    }
    this.requestTick();
  }
  setLowFreqVolume(e) {
    this.volume = e / 10;
  }
  setHasLyric(e) {
  }
  dispose() {
    super.dispose(), this.tickHandle && (cancelAnimationFrame(this.tickHandle), this.tickHandle = 0), this._disposed = !0, this.mainProgram.dispose(), this.quadProgram.dispose(), this.gl.deleteBuffer(this.quadBuffer), this.fbo && this.gl.deleteFramebuffer(this.fbo), this.fboTexture && this.gl.deleteTexture(this.fboTexture);
    for (const e of this.meshStates)
      e.mesh.dispose(), e.texture.dispose();
  }
  enablePerformanceMonitor(e) {
    this.enablePerformanceMonitoring = e, e && (this.frameCount = 0, this.lastFPSUpdate = performance.now());
  }
  getCurrentFPS() {
    return this.currentFPS;
  }
  updatePerformanceStats(e) {
    this.enablePerformanceMonitoring && (this.frameCount++, e - this.lastFPSUpdate > 1e3 && (this.currentFPS = this.frameCount, this.frameCount = 0, this.lastFPSUpdate = e));
  }
}, Tc = class extends Ie {
  time = 0;
}, wc = class extends hi {
  canvas;
  app;
  curContainer;
  staticMode = !1;
  lastContainer = /* @__PURE__ */ new Set();
  onTick = (e) => {
    for (const t of this.lastContainer)
      t.alpha = ee(t.alpha - e / 60), t.alpha <= 0 && (this.app.stage.removeChild(t), this.lastContainer.delete(t), t.destroy(!0));
    if (this.curContainer) {
      this.curContainer.alpha = Math.min(1, this.curContainer.alpha + e / 60);
      const [t, r, s, i] = this.curContainer.children, n = Math.max(this.app.screen.width, this.app.screen.height);
      t.position.set(this.app.screen.width / 2, this.app.screen.height / 2), r.position.set(this.app.screen.width / 2.5, this.app.screen.height / 2.5), s.position.set(this.app.screen.width / 2, this.app.screen.height / 2), i.position.set(this.app.screen.width / 2, this.app.screen.height / 2), t.width = n * Math.sqrt(2), t.height = t.width, r.width = n * 0.8, r.height = r.width, s.width = n * 0.5, s.height = s.width, i.width = n * 0.25, i.height = i.width, this.curContainer.time += e * this.flowSpeed, t.rotation += e / 1e3 * this.flowSpeed, r.rotation -= e / 500 * this.flowSpeed, s.rotation += e / 1e3 * this.flowSpeed, i.rotation -= e / 750 * this.flowSpeed, s.x = this.app.screen.width / 2 + this.app.screen.width / 4 * Math.cos(this.curContainer.time / 1e3 * 0.75), s.y = this.app.screen.height / 2 + this.app.screen.width / 4 * Math.cos(this.curContainer.time / 1e3 * 0.75), i.x = this.app.screen.width / 2 + this.app.screen.width / 4 * 0.1 + Math.cos(this.curContainer.time * 6e-3 * 0.75), i.y = this.app.screen.height / 2 + this.app.screen.width / 4 * 0.1 + Math.cos(this.curContainer.time * 6e-3 * 0.75), this.curContainer.alpha >= 1 && this.lastContainer.size === 0 && this.staticMode && this.app.ticker.stop();
    }
  };
  constructor(e) {
    super(e), this.canvas = e, this.app = new ka({
      view: e,
      resizeTo: this.canvas,
      powerPreference: "low-power",
      backgroundAlpha: 1
    }), this.rebuildFilters(), this.app.ticker.maxFPS = 30, this.app.ticker.add(this.onTick), this.app.ticker.start();
  }
  onResize(e, t) {
    super.onResize(e, t), this.app.resize(), this.rebuildFilters();
  }
  setRenderScale(e) {
    super.setRenderScale(e), this.rebuildFilters();
  }
  rebuildFilters() {
    const e = Math.min(this.canvas.width, this.canvas.height), t = Math.max(this.canvas.width, this.canvas.height), r = new dr();
    r.saturate(1.2, !1);
    const s = new dr();
    s.brightness(0.6, !1);
    const i = new dr();
    i.contrast(0.3, !0);
    for (const n of this.app.stage.filters ?? []) n.destroy();
    this.app.stage.filters = [], this.app.stage.filters.push(new ae(5, 1)), this.app.stage.filters.push(new ae(10, 1)), this.app.stage.filters.push(new ae(20, 2)), this.app.stage.filters.push(new ae(40, 2)), this.app.stage.filters.push(new ae(80, 2)), e > 768 && this.app.stage.filters.push(new ae(160, 4)), e > 1536 && this.app.stage.filters.push(new ae(320, 4)), this.app.stage.filters.push(r, s, i), this.app.stage.filters.push(new ae(5, 1)), Math.random() > 0.5 ? (this.app.stage.filters.push(new lr({
      radius: (t + e) / 2,
      strength: 1,
      center: [0.25, 1]
    })), this.app.stage.filters.push(new lr({
      radius: (t + e) / 2,
      strength: 1,
      center: [0.75, 0]
    }))) : (this.app.stage.filters.push(new lr({
      radius: (t + e) / 2,
      strength: 1,
      center: [0.75, 1]
    })), this.app.stage.filters.push(new lr({
      radius: (t + e) / 2,
      strength: 1,
      center: [0.25, 0]
    })));
  }
  setStaticMode(e = !1) {
    this.staticMode = e, this.app.ticker.start();
  }
  setFPS(e) {
    this.app.ticker.maxFPS = e;
  }
  pause() {
    this.app.ticker.stop(), this.app.render();
  }
  resume() {
    this.app.ticker.start();
  }
  setLowFreqVolume(e) {
  }
  setHasLyric(e) {
  }
  async setAlbum(e, t) {
    if (!e || typeof e == "string" && e.trim().length === 0) return;
    let r = null, s = 5, i = null;
    for (; !i?.baseTexture?.resource?.valid && s > 0; ) try {
      typeof e == "string" ? r = await Ys(e, t) : r = await Xa(e), i = We.from(r, { resourceOptions: { autoLoad: !1 } }), await i.baseTexture.resource.load();
    } catch (c) {
      console.warn(`failed on loading album image, retrying (${s})`, e, c), i = null, s--;
    }
    if (!i) return;
    const n = new Tc(), a = new Lr(i), o = new Lr(i), h = new Lr(i), l = new Lr(i);
    a.anchor.set(0.5, 0.5), o.anchor.set(0.5, 0.5), h.anchor.set(0.5, 0.5), l.anchor.set(0.5, 0.5), a.rotation = Math.random() * Math.PI * 2, o.rotation = Math.random() * Math.PI * 2, h.rotation = Math.random() * Math.PI * 2, l.rotation = Math.random() * Math.PI * 2, n.addChild(a, o, h, l), this.curContainer && this.lastContainer.add(this.curContainer), this.curContainer = n, this.app.stage.addChild(n), this.curContainer.alpha = 0, this.app.ticker.start();
  }
  dispose() {
    super.dispose(), this.app.ticker.remove(this.onTick), this.app.destroy(!0);
  }
  getElement() {
    return this.canvas;
  }
}, Ec = class qa {
  element;
  renderer;
  constructor(t, r) {
    this.renderer = t, this.element = r, r.style.pointerEvents = "none", r.style.zIndex = "-1", r.style.contain = "strict";
  }
  static new(t) {
    const r = document.createElement("canvas");
    return new qa(new t(r), r);
  }
  setRenderScale(t) {
    this.renderer.setRenderScale(t);
  }
  setFlowSpeed(t) {
    this.renderer.setFlowSpeed(t);
  }
  setStaticMode(t) {
    this.renderer.setStaticMode(t);
  }
  setFPS(t) {
    this.renderer.setFPS(t);
  }
  pause() {
    this.renderer.pause();
  }
  resume() {
    this.renderer.resume();
  }
  setLowFreqVolume(t) {
    this.renderer.setLowFreqVolume(t);
  }
  setHasLyric(t) {
    this.renderer.setHasLyric(t);
  }
  setAlbum(t, r) {
    return this.renderer.setAlbum(t, r);
  }
  getElement() {
    return this.element;
  }
  dispose() {
    this.renderer.dispose(), this.element.remove();
  }
}, at = {
  active: "FmKaba_active",
  bgWrapper: "FmKaba_bgWrapper",
  bgWrapperActive: "FmKaba_bgWrapperActive",
  bgWrapperHidden: "FmKaba_bgWrapperHidden",
  bgWrapperTop: "FmKaba_bgWrapperTop",
  bottomLine: "FmKaba_bottomLine",
  disableSpring: "FmKaba_disableSpring",
  duet: "FmKaba_duet",
  emphasize: "FmKaba_emphasize",
  emphasizeWrapper: "FmKaba_emphasizeWrapper",
  enabled: "FmKaba_enabled",
  hasDuetLine: "FmKaba_hasDuetLine",
  interludeDots: "FmKaba_interludeDots",
  lyricBgLine: "FmKaba_lyricBgLine",
  lyricDuetLine: "FmKaba_lyricDuetLine",
  lyricLine: "FmKaba_lyricLine",
  lyricLineWrapper: "FmKaba_lyricLineWrapper",
  lyricMainLine: "FmKaba_lyricMainLine",
  lyricSubLine: "FmKaba_lyricSubLine",
  playing: "FmKaba_playing",
  romanWord: "FmKaba_romanWord",
  rubyWord: "FmKaba_rubyWord",
  tmpDisableTransition: "FmKaba_tmpDisableTransition",
  wordBody: "FmKaba_wordBody",
  wordWithRuby: "FmKaba_wordWithRuby"
}, Sc = {
  normalizeSpaces: !0,
  resetLineTimestamps: !0,
  convertExcessiveBackgroundLines: !0,
  syncMainAndBackgroundLines: !0,
  cleanUnintentionalOverlaps: !0,
  tryAdvanceStartTime: !0
};
function Ac(e) {
  for (const t of e) for (const r of t.words) r.word = r.word.replace(/\s+/g, " ");
}
function Ic(e) {
  for (const t of e) if (t.words.length === 1 && t.words[0].startTime === 0 && t.words[0].endTime === 0 && (t.startTime !== 0 || t.endTime !== 0))
    t.words[0].startTime = t.startTime, t.words[0].endTime = t.endTime;
  else if (t.words.length > 0) {
    const r = t.words[0], s = t.words[t.words.length - 1];
    t.startTime = r.startTime, t.endTime = s.endTime;
  }
}
function Rc(e) {
  let t = 0;
  for (const r of e) r.isBG ? (t++, t > 1 && (r.isBG = !1)) : t = 0;
}
function Mc(e) {
  for (let t = e.length - 1; t >= 0; t--) {
    const r = e[t];
    if (r.isBG) continue;
    const s = e[t + 1];
    if (s?.isBG) {
      const i = [...r.words, ...s.words].filter((n) => n.word.trim().length > 0);
      if (i.length > 0) {
        const n = Math.min(...i.map((l) => l.startTime)), a = Math.max(...i.map((l) => l.endTime)), o = Math.min(n, r.startTime, s.startTime), h = Math.max(a, r.endTime, s.endTime);
        r.startTime = o, r.endTime = h, s.startTime = o, s.endTime = h;
      }
    }
  }
}
function Cc(e) {
  for (let t = 0; t < e.length - 1; t++) {
    const r = e[t];
    if (r.isBG) continue;
    let s = t + 1;
    for (; s < e.length && e[s].isBG; ) s++;
    if (s < e.length) {
      const i = e[s], n = r.endTime - i.startTime;
      if (n > 0) {
        const a = (i.endTime - i.startTime) * 0.1;
        if (!(n > 100 && n > a)) {
          r.endTime = i.startTime;
          const o = e[t + 1];
          o?.isBG && (o.endTime = i.startTime);
        }
      }
    }
  }
}
function Pc(e) {
  let i = 0, n = 0, a = 0, o = 0, h = !1;
  for (let l = 0; l < e.length; l++) {
    const c = e[l];
    if (c.isBG) continue;
    const u = c.startTime, d = c.endTime;
    let y = 0, m = 0;
    if (h) if (u >= n)
      y = 600, m = o;
    else {
      y = 400;
      const f = n - i;
      m = i + f * 0.3;
    }
    else
      y = 600, m = 0;
    const v = c.startTime - y, p = Math.max(m, v);
    p < c.startTime && (c.startTime = p);
    const x = e[l + 1];
    x?.isBG && (x.startTime = c.startTime), h && u < o && d > a ? (a = Math.min(a, u), o = Math.max(o, d)) : (a = u, o = d), i = u, n = d, h = !0;
  }
}
function Lc(e, t) {
  const r = {
    ...Sc,
    ...t
  };
  r.normalizeSpaces && Ac(e), r.resetLineTimestamps && Ic(e), r.convertExcessiveBackgroundLines && Rc(e), r.syncMainAndBackgroundLines && Mc(e), r.cleanUnintentionalOverlaps && Cc(e), r.tryAdvanceStartTime && Pc(e);
}
function Fc(e) {
  const t = 2.5949095;
  return e < 0.5 ? (2 * e) ** 2 * (7.189819 * e - t) / 2 : ((2 * e - 2) ** 2 * (3.5949095 * (e * 2 - 2) + t) + 2) / 2;
}
function Nc(e) {
  return e === 1 ? 1 : 1 - 2 ** (-10 * e);
}
var Oc = class {
  element = document.createElement("div");
  dot0 = document.createElement("span");
  dot1 = document.createElement("span");
  dot2 = document.createElement("span");
  left = 0;
  top = 0;
  playing = !0;
  lastStyle = "";
  currentInterlude;
  currentTime = 0;
  targetBreatheDuration = 1500;
  constructor() {
    this.element.className = at.interludeDots, this.element.appendChild(this.dot0), this.element.appendChild(this.dot1), this.element.appendChild(this.dot2);
  }
  getElement() {
    return this.element;
  }
  setTransform(e = this.left, t = this.top) {
    this.left = e, this.top = t, this.update();
  }
  setInterlude(e) {
    this.currentInterlude = e, this.currentTime = e?.[0] ?? 0, e ? this.element.classList.add(at.enabled) : this.element.classList.remove(at.enabled);
  }
  pause() {
    this.playing = !1, this.element.classList.remove(at.playing);
  }
  resume() {
    this.playing = !0, this.element.classList.add(at.playing);
  }
  update(e = 0) {
    if (!this.playing) return;
    this.currentTime += e;
    let t = "";
    if (t += `transform:translate(${this.left.toFixed(2)}px, ${this.top.toFixed(2)}px)`, this.currentInterlude) {
      const r = this.currentInterlude[1] - this.currentInterlude[0], s = this.currentTime - this.currentInterlude[0];
      if (s <= r) {
        const i = r / Math.ceil(r / this.targetBreatheDuration);
        let n = 1, a = 1;
        n *= Math.sin(1.5 * Math.PI - s / i * 2) / 20 + 1, s < 2e3 && (n *= Nc(s / 2e3)), s < 500 ? a = 0 : s < 1e3 && (a *= (s - 500) / 500), r - s < 750 && (n *= 1 - Fc((750 - (r - s)) / 750 / 2)), r - s < 375 && (a *= Xt((r - s) / 375));
        const o = ee(r - 750);
        n = ee(n) * 0.7, t += ` scale(${n})`;
        const h = de(0.25, s * 3 / o * 0.75, 1), l = de(0.25, (s - o / 3) * 3 / o * 0.75, 1), c = de(0.25, (s - o / 3 * 2) * 3 / o * 0.75, 1);
        this.dot0.style.opacity = `${Xt(a * h)}`, this.dot1.style.opacity = `${Xt(a * l)}`, this.dot2.style.opacity = `${Xt(a * c)}`;
      } else
        t += " scale(0)", this.dot0.style.opacity = "0", this.dot1.style.opacity = "0", this.dot2.style.opacity = "0";
      t += ";", this.lastStyle !== t && (this.element.setAttribute("style", t), this.lastStyle = t);
    }
  }
  dispose() {
    this.element.remove();
  }
}, Ks = [], gn = [], Zs = !1;
function Bc() {
  let e = gn.shift();
  for (; e; ) {
    try {
      e.resolve(e.task());
    } catch (t) {
      e.reject(t);
    }
    e = gn.shift();
  }
  for (e = Ks.shift(); e; ) {
    try {
      e.resolve(e.task());
    } catch (t) {
      e.reject(t);
    }
    e = Ks.shift();
  }
  Zs = !1;
}
function Uc() {
  Zs || (Zs = !0, requestAnimationFrame(Bc));
}
function kc(e) {
  const t = {
    task: e,
    resolve: () => {
    },
    reject: () => {
    }
  }, r = new Promise((s, i) => {
    t.resolve = s, t.reject = i;
  });
  return Ks.push(t), Uc(), r;
}
function Dc(e) {
  return (r) => (e(r + 1e-3) - e(r - 1e-3)) / (2 * 1e-3);
}
function vn(e) {
  return Dc(e);
}
var mr = class {
  currentPosition = 0;
  targetPosition = 0;
  currentTime = 0;
  params = {};
  currentSolver;
  getV;
  getV2;
  queueParams;
  queuePosition;
  constructor(e = 0) {
    this.targetPosition = e, this.currentPosition = this.targetPosition, this.currentSolver = () => this.targetPosition, this.getV = () => 0, this.getV2 = () => 0;
  }
  resetSolver() {
    const e = this.getV(this.currentTime);
    this.currentTime = 0, this.currentSolver = Gc(this.currentPosition, e, this.targetPosition, 0, this.params), this.getV = vn(this.currentSolver), this.getV2 = vn(this.getV);
  }
  arrived() {
    return Math.abs(this.targetPosition - this.currentPosition) < 0.01 && this.getV(this.currentTime) < 0.01 && this.getV2(this.currentTime) < 0.01 && this.queueParams === void 0 && this.queuePosition === void 0;
  }
  setPosition(e) {
    this.targetPosition = e, this.currentPosition = e, this.currentSolver = () => this.targetPosition, this.getV = () => 0, this.getV2 = () => 0;
  }
  update(e = 0) {
    this.currentTime += e, this.currentPosition = this.currentSolver(this.currentTime), this.queueParams && (this.queueParams.time -= e, this.queueParams.time <= 0 && this.updateParams({ ...this.queueParams })), this.queuePosition && (this.queuePosition.time -= e, this.queuePosition.time <= 0 && this.setTargetPosition(this.queuePosition.position)), this.arrived() && this.setPosition(this.targetPosition);
  }
  updateParams(e, t = 0) {
    t > 0 ? this.queueParams = {
      ...this.queuePosition ?? {},
      ...e,
      time: t
    } : (this.queuePosition = void 0, this.params = {
      ...this.params,
      ...e
    }, this.resetSolver());
  }
  setTargetPosition(e, t = 0) {
    t > 0 ? this.queuePosition = {
      ...this.queuePosition ?? {},
      position: e,
      time: t
    } : (this.queuePosition = void 0, this.targetPosition = e, this.resetSolver());
  }
  getCurrentPosition() {
    return this.currentPosition;
  }
};
function Gc(e, t, r, s = 0, i) {
  const n = i?.soft ?? !1, a = i?.stiffness ?? 100, o = i?.damping ?? 10, h = i?.mass ?? 1, l = r - e;
  if (n || 1 <= o / (2 * Math.sqrt(a * h))) {
    const m = -Math.sqrt(a / h), v = -m * l - t;
    return (p) => (p -= s, p < 0 ? e : r - (l + p * v) * Math.E ** (p * m));
  }
  const c = Math.sqrt(4 * h * a - o ** 2), u = (o * l - 2 * h * t) / c, d = 0.5 * c / h, y = -(0.5 * o) / h;
  return (m) => (m -= s, m < 0 ? e : r - (Math.cos(m * d) * l + Math.sin(m * d) * u) * Math.E ** (m * y));
}
var zc = class {
  lyricPlayer;
  element = document.createElement("div");
  left = 0;
  top = 0;
  delay = 0;
  lineSize = [0, 0];
  lineTransforms = {
    posX: new mr(0),
    posY: new mr(0)
  };
  isFocused = !1;
  blur = 0;
  constructor(e) {
    this.lyricPlayer = e, this.element.setAttribute("class", `${at.lyricLine} ${at.bottomLine}`), this.element.dataset.bottomLine = "true", this.rebuildStyle();
  }
  async measureSize() {
    return await kc(() => [this.element.clientWidth, this.element.clientHeight]);
  }
  lastStyle = "";
  show() {
    this.rebuildStyle();
  }
  hide() {
    this.rebuildStyle();
  }
  setFocused(e) {
    this.isFocused !== e && (this.isFocused = e, e ? this.element.dataset.focused = "true" : delete this.element.dataset.focused);
  }
  rebuildStyle() {
    let e = `transform:translate(${this.lineTransforms.posX.getCurrentPosition().toFixed(2)}px,${this.lineTransforms.posY.getCurrentPosition().toFixed(2)}px);`;
    !this.lyricPlayer.getEnableSpring() && this.isInSight && (e += `transition-delay:${this.delay}ms;`), e += `filter:blur(${Math.min(5, this.blur)}px);`, e !== this.lastStyle && (this.lastStyle = e, this.element.setAttribute("style", e));
  }
  getElement() {
    return this.element;
  }
  setTransform(e = this.left, t = this.top, r = 0, s = !1, i = 0) {
    this.left = e, this.top = t, this.delay = i * 1e3 | 0, s || !this.lyricPlayer.getEnableSpring() ? (this.blur = Math.min(32, r), s && this.element.classList.add(at.tmpDisableTransition), this.lineTransforms.posX.setPosition(e), this.lineTransforms.posY.setPosition(t), this.lyricPlayer.getEnableSpring() ? this.rebuildStyle() : this.show(), s && requestAnimationFrame(() => {
      this.element.classList.remove(at.tmpDisableTransition);
    })) : (this.blur = Math.min(5, r), this.lineTransforms.posX.setTargetPosition(e, i), this.lineTransforms.posY.setTargetPosition(t, i));
  }
  update(e = 0) {
    this.lyricPlayer.getEnableSpring() && (this.lineTransforms.posX.update(e), this.lineTransforms.posY.update(e), this.isInSight ? this.show() : this.hide());
  }
  get isInSight() {
    const e = this.lineTransforms.posX.getCurrentPosition(), t = this.lineTransforms.posY.getCurrentPosition(), r = e + this.lineSize[0], s = t + this.lineSize[1], i = this.lyricPlayer.size[0], n = this.lyricPlayer.size[1];
    return !(e > i || t > n || r < 0 || s < 0);
  }
  dispose() {
    this.element.remove();
  }
}, Ve = {
  Disabled: "",
  FullMask: "full-mask",
  PartialMask: "partial-mask"
}, ue = {
  SOLID: 0,
  GRADIENT: 1
}, cr = {
  Top: "top",
  Center: "center",
  Bottom: "bottom"
};
function $c(e) {
  const t = e.currentTime + 20, r = e.scrollToIndex, s = e.currentGroups, i = (n) => {
    if (n < -1 || n >= s.length - 1) return;
    const a = n === -1 ? null : s[n], o = s[n + 1], h = a ? a.endTime : 0, l = Math.max(h, o.startTime - 250);
    if (!(l - h < 4e3) && l > t && h < t)
      return {
        startTime: Math.max(h, t),
        endTime: l,
        anchorLineIndex: n,
        isNextDuet: o.mainLine.getLine().isDuet
      };
  };
  return i(r - 1) || i(r) || i(r + 1);
}
function Vc(e) {
  const { enabled: t, currentGroups: r, scrollToIndex: s, isSeeking: i, isInterludeActive: n } = e;
  if (!t || r.length === 0) return { shouldUpdate: !1 };
  if (i || n) return {
    shouldUpdate: !0,
    params: {
      stiffness: 90,
      damping: 15
    }
  };
  const a = r[s], o = r[s - 1];
  if (!a || !o) return { shouldUpdate: !1 };
  const h = a.startTime - o.startTime, l = 100, c = de(h, l, 800), u = 170;
  let d = 1 - (c - l) / 700;
  d = d ** 0.2;
  const y = u + d * 50;
  return {
    shouldUpdate: !0,
    params: {
      stiffness: y,
      damping: Math.sqrt(y) * 2.2
    }
  };
}
function Hc(e) {
  const { groupIndex: t, scrollToIndex: r, latestIndex: s, hasBuffered: i, hidePassedLines: n, isPlaying: a, isNonDynamic: o, enableBlur: h, isUserScrolling: l, isCompact: c, interlude: u } = e, d = i || t >= r && t < s, y = ja({
    enableBlur: h,
    isUserScrolling: l,
    isActive: d,
    itemIndex: t,
    scrollToIndex: r,
    latestIndex: s,
    isCompact: c
  });
  let m;
  return n && t < (u ? u.anchorLineIndex + 1 : r) && a ? m = 1e-4 : i ? m = 0.85 : m = o ? 0.2 : 1, {
    isActive: d,
    targetOpacity: m,
    blurLevel: y
  };
}
function ja(e) {
  const { enableBlur: t, isUserScrolling: r, isActive: s, itemIndex: i, scrollToIndex: n, latestIndex: a, isCompact: o } = e;
  if (!t || r || s) return 0;
  let h = 1;
  return i < n ? h += Math.abs(n - i) + 1 : h += Math.abs(i - Math.max(n, a)), o ? h * 0.8 : h;
}
function Or(e) {
  e.scrollOffset = de(e.scrollOffset, e.scrollBoundary.minOffset, e.scrollBoundary.maxOffset);
}
function Xc(e) {
  e.isScrolled = !1, e.scrollOffset = 0, e.isUserScrolling = !1;
}
function Wc(e, t, r) {
  let s = 0, i = 0, n = 0, a = 0, o = 0, h = 0, l = 0, c = 0;
  e.addEventListener("touchstart", (u) => {
    r.onBeginScroll() && (t.isUserScrolling = !0, u.preventDefault(), s = t.scrollOffset, i = u.touches[0].screenY, o = i, n = u.touches[0].screenX, a = u.touches[0].screenY, h = Date.now(), l = 0, r.onLayout(!0, !0));
  }), e.addEventListener("touchmove", (u) => {
    if (r.onBeginScroll()) {
      u.preventDefault();
      const d = u.touches[0].screenY, y = d - i;
      t.scrollOffset = s - y, Or(t);
      const m = Date.now(), v = m - h;
      v > 0 && (l = (d - o) / v), o = d, h = m, r.onLayout(!0, !0);
    }
  }), e.addEventListener("touchend", (u) => {
    if (r.onBeginScroll()) {
      u.preventDefault();
      const d = u.changedTouches[0], y = Math.abs(d.screenX - n), m = Math.abs(d.screenY - a);
      if (y < 10 && m < 10) {
        const f = document.elementFromPoint(d.clientX, d.clientY);
        f instanceof HTMLElement && r.containsTarget(f) && r.clickTarget(f), t.isUserScrolling = !1, r.onEndScroll();
        return;
      }
      i = 0;
      const v = ++c;
      Math.abs(l) < 0.1 && (l = 0);
      let p = performance.now();
      const x = (f) => {
        if (v !== c) return;
        const E = f - p;
        if (p = f, E <= 0 || E > 100) {
          requestAnimationFrame(x);
          return;
        }
        if (Math.abs(l) > 0.05) {
          t.scrollOffset -= l * E, Or(t);
          const g = 0.95 ** (E / 16);
          l *= g, r.onLayout(!0, !0), requestAnimationFrame(x);
        } else
          t.isUserScrolling = !1, r.onEndScroll();
      };
      requestAnimationFrame(x);
    } else t.isUserScrolling = !1;
  }), e.addEventListener("wheel", (u) => {
    r.onBeginScroll() && (u.preventDefault(), u.deltaMode === u.DOM_DELTA_PIXEL ? (t.scrollOffset += u.deltaY, Or(t), r.onLayout(!0, !1)) : (t.scrollOffset += u.deltaY * 50, Or(t), r.onLayout(!1, !1)));
  }, { passive: !1 });
}
var qc = (e, t) => e.size === t.size && [...e].every((r) => t.has(r));
function jc(e) {
  const { time: t, currentGroups: r, timelineState: { hotGroups: s, bufferedGroups: i } } = e, n = new Set(s), a = /* @__PURE__ */ new Set(), o = /* @__PURE__ */ new Set(), h = /* @__PURE__ */ new Set();
  for (const l of s) {
    const c = r[l];
    (!c || t < c.startTime || c.endTime <= t) && (n.delete(l), o.add(l));
  }
  for (let l = 0; l < r.length; l++) {
    const c = r[l];
    c && c.startTime <= t && c.endTime > t && !n.has(l) && (n.add(l), a.add(l));
  }
  for (const l of i) n.has(l) || h.add(l);
  return {
    nextHotGroups: n,
    addedIds: a,
    removedHotIds: o,
    removedBufferedIds: h
  };
}
function Yc(e, t, r) {
  if (r.size > 0) return Math.min(...r);
  const s = t.findIndex((i) => i.startTime >= e);
  return s === -1 ? t.length : s;
}
function Kc(e) {
  const { timelineState: t, time: r, currentGroups: s, hasBottomContent: i, stateResult: n } = e, { addedIds: a, removedHotIds: o, removedBufferedIds: h } = n, { isSeeking: l } = t;
  t.currentTime = r, t.hotGroups = n.nextHotGroups;
  let c = !1, u = !1;
  const d = [], y = /* @__PURE__ */ new Set();
  if (l) {
    t.bufferedGroups = /* @__PURE__ */ new Set([...t.hotGroups]), t.scrollToIndex = Yc(r, s, t.bufferedGroups);
    for (const m of o) y.add(m);
    for (const m of t.hotGroups) d.push(m);
    for (const m of h) y.add(m);
    u = !0, c = !0;
  } else if (a.size > 0) {
    for (const m of a)
      t.bufferedGroups.add(m), d.push(m);
    for (const m of h)
      t.bufferedGroups.delete(m), y.add(m);
    t.bufferedGroups.size > 0 && (t.scrollToIndex = Math.min(...t.bufferedGroups)), c = !0;
  } else if (h.size > 0 && qc(h, t.bufferedGroups)) {
    for (const m of t.bufferedGroups)
      t.hotGroups.has(m) || (t.bufferedGroups.delete(m), y.add(m));
    c = !0;
  }
  if (t.bufferedGroups.size === 0 && s.length > 0 && r >= s[s.length - 1].endTime) {
    const m = i ? s.length : s.length - 1;
    t.scrollToIndex !== m && (t.scrollToIndex = m, c = !0);
  }
  return t.lastCurrentTime = r, {
    shouldLayout: c,
    shouldResetScroll: u,
    groupsToEnable: d,
    groupsToDisable: [...y]
  };
}
var Ya = class extends EventTarget {
  element = document.createElement("div");
  timelineState = {
    currentTime: 0,
    lastCurrentTime: 0,
    hotGroups: /* @__PURE__ */ new Set(),
    bufferedGroups: /* @__PURE__ */ new Set(),
    scrollToIndex: 0,
    isSeeking: !1,
    isPlaying: !0,
    initialLayoutFinished: !1
  };
  lyricGroupElementMap = /* @__PURE__ */ new WeakMap();
  currentLyricLines = [];
  processedLines = [];
  lyricLinesIndexes = /* @__PURE__ */ new WeakMap();
  isNonDynamic = !1;
  hasDuetLine = !1;
  disableSpring = !1;
  layoutState = {
    interludeDotsSize: [0, 0],
    targetAlignIndex: 0,
    lastInterludeState: !1,
    alignAnchor: cr.Center,
    alignPosition: 0.35,
    overscanPx: 300
  };
  interludeDots = new Oc();
  bottomLine = new zc(this);
  enableBlur = !0;
  enableScale = !0;
  maskObsceneWords = Ve.Disabled;
  maskObsceneWordChar = "*";
  hidePassedLines = !1;
  scrollState = {
    scrollBoundary: {
      minOffset: 0,
      maxOffset: 0
    },
    scrollOffset: 0,
    allowScroll: !0,
    isScrolled: !1,
    isUserScrolling: !1
  };
  currentLyricGroups = [];
  lyricGroupSize = /* @__PURE__ */ new WeakMap();
  size = [0, 0];
  isPageVisible = !0;
  optimizeOptions = {};
  alwaysPostpositionBackground = !1;
  posXSpringParams = {
    mass: 1,
    damping: 10,
    stiffness: 100
  };
  posYSpringParams = {
    mass: 0.9,
    damping: 15,
    stiffness: 90
  };
  scaleSpringParams = {
    mass: 2,
    damping: 25,
    stiffness: 100
  };
  scaleForBGSpringParams = {
    mass: 1,
    damping: 20,
    stiffness: 50
  };
  onPageShow = () => {
    this.isPageVisible = !0, this.setCurrentTime(this.timelineState.currentTime, !0);
  };
  onPageHide = () => {
    this.isPageVisible = !1;
  };
  scrolledHandler;
  resizeObserver = new ResizeObserver(((e) => {
    let t = !1, r = !1;
    for (const s of e) if (s.target === this.element) {
      const i = s.contentRect;
      this.size[0] = i.width, this.size[1] = i.height, r = !0;
    } else if (s.target === this.interludeDots.getElement())
      this.layoutState.interludeDotsSize[0] = s.target.clientWidth, this.layoutState.interludeDotsSize[1] = s.target.clientHeight, t = !0;
    else if (s.target === this.bottomLine.getElement()) {
      const i = [s.target.clientWidth, s.target.clientHeight], n = this.bottomLine.lineSize;
      (i[0] !== n[0] || i[1] !== n[1]) && (this.bottomLine.lineSize = i, t = !0);
    } else {
      const i = this.lyricGroupElementMap.get(s.target);
      if (i) {
        const n = [s.target.clientWidth, s.target.clientHeight], a = this.lyricGroupSize.get(i) ?? [0, 0];
        (n[0] !== a[0] || n[1] !== a[1]) && (this.lyricGroupSize.set(i, n), i.onLineSizeChange(n), t = !0);
      }
    }
    t && this.calcLayout(!0), r && this.onResize();
  }));
  wordFadeWidth = 0.5;
  constructor(e) {
    super(), e && (this.element = e), this.element.classList.add("amll-lyric-player"), this.resizeObserver.observe(this.element), this.resizeObserver.observe(this.interludeDots.getElement()), this.element.appendChild(this.interludeDots.getElement()), this.element.appendChild(this.bottomLine.getElement()), this.interludeDots.setTransform(0, 200), window.addEventListener("pageshow", this.onPageShow), window.addEventListener("pagehide", this.onPageHide), Wc(this.element, this.scrollState, {
      onBeginScroll: () => this.beginScrollHandler(),
      onEndScroll: () => this.endScrollHandler(),
      onLayout: (t, r) => this.calcLayout(t, r),
      containsTarget: (t) => this.element.contains(t),
      clickTarget: (t) => t.click()
    });
  }
  beginScrollHandler() {
    const e = this.scrollState.allowScroll;
    return e && (this.scrollState.isScrolled = !0, clearTimeout(this.scrolledHandler), this.scrolledHandler = setTimeout(() => {
      this.scrollState.isScrolled = !1, this.scrollState.scrollOffset = 0;
    }, 5e3)), e;
  }
  endScrollHandler() {
  }
  setWordFadeWidth(e = 0.5) {
    this.wordFadeWidth = Math.max(1e-4, e);
  }
  setEnableScale(e = !0) {
    this.enableScale = e, this.calcLayout();
  }
  getEnableScale() {
    return this.enableScale;
  }
  getWordFadeWidth() {
    return this.wordFadeWidth;
  }
  setIsSeeking(e) {
    this.timelineState.isSeeking = e;
  }
  setHidePassedLines(e) {
    this.hidePassedLines = e, this.calcLayout();
  }
  setEnableBlur(e) {
    this.enableBlur !== e && (this.enableBlur = e, this.calcLayout());
  }
  setMaskObsceneWords(e) {
    this.maskObsceneWords !== e && (this.maskObsceneWords = e, this.rebuildLyricLines(), this.calcLayout());
  }
  setMaskObsceneWordChar(e) {
    const t = e.charAt(0) || "*";
    this.maskObsceneWordChar !== t && (this.maskObsceneWordChar = t, this.maskObsceneWords !== Ve.Disabled && (this.rebuildLyricLines(), this.calcLayout()));
  }
  rebuildLyricLines() {
    for (const e of this.currentLyricGroups) e.rebuildAllLines();
  }
  processObsceneWord(e) {
    const t = e.word;
    if (!e.obscene || this.maskObsceneWords === Ve.Disabled) return t;
    const r = this.maskObsceneWordChar;
    if (this.maskObsceneWords === Ve.FullMask) return t.replace(/\S/g, r);
    if (this.maskObsceneWords === Ve.PartialMask) {
      const s = t.trim();
      if (s.length <= 2) return t.replace(/\S/g, r);
      const i = t.indexOf(s), n = i + s.length - 1;
      return t.slice(0, i + 1) + t.slice(i + 1, n).replace(/\S/g, r) + t.slice(n);
    }
    return t;
  }
  setAlignAnchor(e) {
    this.layoutState.alignAnchor = e;
  }
  setAlignPosition(e) {
    this.layoutState.alignPosition = e;
  }
  setOverscanPx(e) {
    this.layoutState.overscanPx = ee(e | 0);
  }
  getOverscanPx() {
    return this.layoutState.overscanPx;
  }
  setEnableSpring(e = !0) {
    this.disableSpring = !e, e ? this.element.classList.remove(at.disableSpring) : this.element.classList.add(at.disableSpring), this.calcLayout(!0);
  }
  getEnableSpring() {
    return !this.disableSpring;
  }
  setOptimizeOptions(e) {
    this.optimizeOptions = {
      ...this.optimizeOptions,
      ...e
    };
  }
  setLyricLines(e, t = 0) {
    this.timelineState.initialLayoutFinished = !0, this.timelineState.lastCurrentTime = t, this.timelineState.currentTime = t, this.currentLyricLines = cn(e), this.processedLines = cn(this.currentLyricLines), Lc(this.processedLines, this.optimizeOptions), this.isNonDynamic = !0;
    for (const r of this.processedLines) if (r.words.length > 1) {
      this.isNonDynamic = !1;
      break;
    }
    this.hasDuetLine = this.processedLines.some((r) => r.isDuet);
    for (const r of this.currentLyricGroups) r.dispose();
    this.currentLyricGroups = [], this.interludeDots.setInterlude(void 0), this.timelineState.hotGroups.clear(), this.timelineState.bufferedGroups.clear();
  }
  getIsPlaying() {
    return this.timelineState.isPlaying;
  }
  setCurrentTime(e, t = !1) {
    e = Math.round(e);
    const { timelineState: r } = this;
    if (r.isSeeking = !!t, r.currentTime = e, !r.initialLayoutFinished && !r.isSeeking) return;
    const s = jc({
      time: e,
      currentGroups: this.currentLyricGroups,
      timelineState: r
    }), i = this.bottomLine.getElement().innerHTML.trim().length > 0, n = Kc({
      timelineState: r,
      time: e,
      currentGroups: this.currentLyricGroups,
      hasBottomContent: i,
      stateResult: s
    });
    for (const a of n.groupsToDisable) this.currentLyricGroups[a]?.disable();
    for (const a of n.groupsToEnable) this.currentLyricGroups[a]?.enable();
    n.shouldResetScroll && this.resetScroll(), n.shouldLayout && this.calcLayout();
  }
  async calcLayout(e = !1, t = !1) {
    const r = $c({
      currentTime: this.timelineState.currentTime,
      scrollToIndex: this.timelineState.scrollToIndex,
      currentGroups: this.currentLyricGroups
    }), s = !!r;
    if (this.layoutState.targetAlignIndex !== this.timelineState.scrollToIndex || this.layoutState.lastInterludeState !== s) {
      this.layoutState.lastInterludeState = s;
      const g = Vc({
        enabled: this.getEnableSpring(),
        currentGroups: this.currentLyricGroups,
        scrollToIndex: this.timelineState.scrollToIndex,
        isSeeking: this.timelineState.isSeeking,
        isInterludeActive: s
      });
      g.shouldUpdate && g.params && this.setLinePosYSpringParams(g.params);
    }
    let i = -this.scrollState.scrollOffset;
    const n = this.timelineState.scrollToIndex;
    let a = !1;
    r ? a = r.isNextDuet : this.interludeDots.setInterlude(void 0);
    const o = (this.baseFontSize || 24) * 0.4, h = this.layoutState.interludeDotsSize[1] + o * 2;
    r && r.anchorLineIndex !== -1 && (i -= h);
    const l = this.size[1] / 5, c = this.currentLyricGroups.slice(0, n).reduce((g, A) => g + (this.lyricGroupSize.get(A)?.[1] ?? l), 0);
    this.scrollState.scrollBoundary.minOffset = -c, i -= c, i += this.size[1] * this.layoutState.alignPosition;
    const u = this.currentLyricGroups[n];
    this.layoutState.targetAlignIndex = n;
    const d = n === this.currentLyricGroups.length;
    this.bottomLine.setFocused(d);
    const y = u ? this.lyricGroupSize.get(u)?.[1] ?? l : d ? this.bottomLine.lineSize[1] : 0;
    if (y > 0) switch (this.layoutState.alignAnchor) {
      case cr.Bottom:
        i -= y;
        break;
      case cr.Center:
        i -= y / 2;
        break;
      case cr.Top:
    }
    const m = Math.max(...this.timelineState.bufferedGroups);
    let v = 0, p = e ? 0 : 0.05, x = !1;
    this.currentLyricGroups.forEach((g, A) => {
      const L = this.timelineState.bufferedGroups.has(A), M = r && A === r.anchorLineIndex + 1;
      if (!x && M) {
        x = !0, i += o;
        let k = 0;
        r && a && (k = this.size[0] - this.layoutState.interludeDotsSize[0]), this.interludeDots.setTransform(k, i), r && this.interludeDots.setInterlude([r.startTime, r.endTime]), i += this.layoutState.interludeDotsSize[1], i += o;
      }
      const C = Hc({
        groupIndex: A,
        scrollToIndex: this.timelineState.scrollToIndex,
        latestIndex: m,
        hasBuffered: L,
        hidePassedLines: this.hidePassedLines,
        isPlaying: this.timelineState.isPlaying,
        isNonDynamic: this.isNonDynamic,
        enableBlur: this.enableBlur,
        isUserScrolling: this.scrollState.isUserScrolling,
        isCompact: window.innerWidth <= 1024,
        interlude: r
      });
      g.setTransform(i, t, v, C.isActive, C.targetOpacity, C.blurLevel), i += this.lyricGroupSize.get(g)?.[1] ?? l, i >= 0 && !this.timelineState.isSeeking && (v += p, A >= this.timelineState.scrollToIndex && (p /= 1.05));
    }), this.scrollState.scrollBoundary.maxOffset = i + this.scrollState.scrollOffset - this.size[1] / 2;
    const f = this.currentLyricGroups.length, E = ja({
      enableBlur: this.enableBlur,
      isUserScrolling: this.scrollState.isUserScrolling,
      isActive: d,
      itemIndex: f,
      scrollToIndex: this.timelineState.scrollToIndex,
      latestIndex: m,
      isCompact: window.innerWidth <= 1024
    });
    this.bottomLine.setTransform(0, i, E, t, v);
  }
  setLinePosXSpringParams(e = {}) {
  }
  setLinePosYSpringParams(e = {}) {
    this.posYSpringParams = {
      ...this.posYSpringParams,
      ...e
    }, this.bottomLine.lineTransforms.posY.updateParams(this.posYSpringParams);
    for (const t of this.currentLyricGroups)
      t.posY.updateParams(this.posYSpringParams), t.bgSlideY.updateParams(this.posYSpringParams);
  }
  setLineScaleSpringParams(e = {}) {
    this.scaleSpringParams = {
      ...this.scaleSpringParams,
      ...e
    }, this.scaleForBGSpringParams = {
      ...this.scaleForBGSpringParams,
      ...e
    };
    for (const t of this.currentLyricGroups)
      t.mainLine.lineTransforms.scale.updateParams(this.scaleSpringParams), t.bgLine?.lineTransforms.scale.updateParams(this.scaleForBGSpringParams);
  }
  pause() {
    this.interludeDots.pause(), this.timelineState.isPlaying && (this.timelineState.isPlaying = !1, this.calcLayout());
  }
  resume() {
    this.interludeDots.resume(), this.timelineState.isPlaying || (this.timelineState.isPlaying = !0, this.calcLayout());
  }
  update(e = 0) {
    this.bottomLine.update(e / 1e3), this.interludeDots.update(e);
  }
  onResize() {
  }
  getBottomLineElement() {
    return this.bottomLine.getElement();
  }
  resetScroll() {
    Xc(this.scrollState), clearTimeout(this.scrolledHandler);
  }
  getLyricLines() {
    return this.currentLyricLines;
  }
  getCurrentTime() {
    return this.timelineState.currentTime;
  }
  setAlwaysPostpositionBackground(e) {
    this.alwaysPostpositionBackground !== e && (this.alwaysPostpositionBackground = e, this.rebuildLyricLines(), this.calcLayout());
  }
  getAlwaysPostpositionBackground() {
    return this.alwaysPostpositionBackground;
  }
  getElement() {
    return this.element;
  }
  dispose() {
    this.element.remove(), window.removeEventListener("pageshow", this.onPageShow), window.removeEventListener("pagehide", this.onPageHide);
  }
}, Zc = class {
  mainLine;
  bgLine;
  posY = new mr(0);
  bgSlideY = new mr(-80);
  top = 0;
  delay = 0;
  isActive = !1;
  opacity = 1;
  blur = 0;
  isBgFirst = !1;
  constructor(e, t) {
    this.mainLine = e, this.bgLine = t;
  }
  get startTime() {
    return this.mainLine.getLine().startTime;
  }
  get endTime() {
    return this.mainLine.getLine().endTime;
  }
  onLineSizeChange(e) {
    this.mainLine.onLineSizeChange(e), this.bgLine?.onLineSizeChange(e);
  }
  setTransform(e, t, r, s, i, n) {
    this.top = e, this.delay = r, this.isActive = s, this.opacity = i, this.blur = n, this.setLineTransformations(t, r);
    const a = this.lyricPlayer.getEnableSpring(), o = !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst ? 80 : -80, h = this.lyricPlayer.getIsPlaying(), l = s || !h ? 0 : o;
    t || !a ? (this.posY.setPosition(e), this.bgSlideY.setPosition(l), this.renderStyles()) : (this.posY.setTargetPosition(e, r), this.bgSlideY.setTargetPosition(l, r));
  }
  setLineTransformations(e, t) {
    const r = this.lyricPlayer.getEnableScale(), s = this.lyricPlayer.getIsPlaying(), i = this.isActive ? ue.GRADIENT : ue.SOLID, n = r ? 97 : 100;
    let a = 100;
    !this.isActive && s && (a = n), this.mainLine.setTransform(a, 1, 0, e, t, i);
    let o = 100;
    !this.isActive && s && (o = 75), this.bgLine?.setTransform(o, 1, 0, e, t, i);
  }
  update(e) {
    this.lyricPlayer.getEnableSpring() && (this.posY.update(e), this.bgSlideY.update(e), this.renderStyles()), this.mainLine.update(e), this.bgLine?.update(e);
  }
  rebuildAllLines() {
    this.mainLine.rebuildElement(), this.bgLine?.rebuildElement();
  }
  enable(e, t) {
    this.mainLine.enable(e, t), this.bgLine?.enable(e, t);
  }
  disable() {
    this.mainLine.disable(), this.bgLine?.disable();
  }
  dispose() {
    this.mainLine.dispose(), this.bgLine?.dispose();
  }
}, Jc = class extends Zc {
  lyricPlayer;
  element;
  bgWrapper;
  lastIsActive;
  constructor(e, t) {
    super(t), this.lyricPlayer = e, this.element = document.createElement("div"), this.element.className = at.lyricLineWrapper, this.element.appendChild(t.getElement()), this.posY.setPosition(window.innerHeight * 2), e.resizeObserver.observe(this.element);
  }
  get isInSight() {
    const e = this.posY.getCurrentPosition();
    let t = this.lyricPlayer.lyricGroupSize?.get(this)?.[1];
    (t === void 0 || t === 0) && (t = this.element.clientHeight || 0);
    const r = this.lyricPlayer.size[1], s = this.lyricPlayer.getOverscanPx();
    return !(e > r + t + s || e < -t - s);
  }
  show() {
    if (!this.element.parentElement) {
      const e = this.lyricPlayer.getElement(), t = this.lyricPlayer.currentLyricGroups, r = t.indexOf(this);
      let s = null;
      if (r !== -1) {
        for (let i = r + 1; i < t.length; i++) if (t[i].element.parentElement === e) {
          s = t[i].element;
          break;
        }
      }
      e.insertBefore(this.element, s), this.lyricPlayer.resizeObserver.observe(this.element);
    }
    this.mainLine.show(), this.bgLine?.show();
  }
  hide() {
    this.element.parentElement && (this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove(), this.mainLine.teardownContent(), this.bgLine?.teardownContent());
  }
  update(e) {
    this.isInSight ? this.show() : this.hide(), super.update(e);
  }
  addBgLine(e) {
    this.bgLine && this.bgLine.dispose(), this.bgWrapper && this.bgWrapper.remove(), this.bgLine = e;
    const t = e.getLine().words[0]?.startTime ?? e.getLine().startTime, r = this.mainLine.getLine().words[0]?.startTime ?? this.mainLine.getLine().startTime;
    this.isBgFirst = t < r, this.mainLine.getLine().isDuet && e.getElement().classList.add(at.lyricDuetLine), this.bgWrapper = document.createElement("div"), this.bgWrapper.className = at.bgWrapper, this.bgWrapper.appendChild(e.getElement()), !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst ? (this.bgWrapper.classList.add(at.bgWrapperTop), this.element.insertBefore(this.bgWrapper, this.mainLine.getElement()), this.bgSlideY.setPosition(80)) : this.element.appendChild(this.bgWrapper);
  }
  renderStyles() {
    const e = this.posY.getCurrentPosition().toFixed(1);
    if (this.element.style.transform = `translateY(${e}px)`, this.element.style.opacity = this.opacity.toString(), this.element.style.filter = `blur(${Math.min(5, this.blur)}px)`, this.lyricPlayer.getEnableSpring() || (this.element.style.transitionDelay = `${this.delay}ms`), this.bgWrapper) {
      this.lastIsActive !== this.isActive && (this.lastIsActive = this.isActive, this.bgWrapper.classList.toggle(at.bgWrapperActive, this.isActive));
      const t = this.bgSlideY.getCurrentPosition(), r = t.toFixed(1), s = Xt(1 - Math.abs(t) / 80), i = (0.8 + s * 0.2).toFixed(3);
      this.bgWrapper.style.transform = `translateY(${r}%) scale(${i})`;
      const n = !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst;
      if (n) {
        const o = -(this.bgWrapper.clientHeight || 0) * (1 - s);
        this.bgWrapper.style.marginTop = `${o.toFixed(1)}px`;
      } else this.bgWrapper.style.marginTop = "";
      const a = r === (n ? "80.0" : "-80.0") && !this.isActive;
      this.bgWrapper.classList.toggle(at.bgWrapperHidden, a);
    }
  }
  dispose() {
    super.dispose(), this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove();
  }
}, yr = (e) => /^[\p{Unified_Ideograph}\u0800-\u9FFC]+$/u.test(e), oe = class extends EventTarget {
  top = 0;
  scale = 1;
  blur = 0;
  opacity = 1;
  delay = 0;
  lineTransforms = { scale: new mr(100) };
  static wordSegmenter = typeof Intl < "u" && Intl.Segmenter ? new Intl.Segmenter(void 0, { granularity: "word" }) : null;
  static graphemeSegmenter = typeof Intl < "u" && Intl.Segmenter ? new Intl.Segmenter(void 0, { granularity: "grapheme" }) : null;
  setTransform(e = this.scale, t = this.opacity, r = this.blur, s = !1, i = 0, n = ue.SOLID) {
    this.scale = e, this.opacity = t, this.blur = r, this.delay = i;
  }
  rebuildElement() {
  }
  static shouldEmphasize(e) {
    return yr(e.word) ? e.endTime - e.startTime >= 1e3 : e.endTime - e.startTime >= 1e3 && e.word.trim().length <= 7 && e.word.trim().length > 1;
  }
  dispose() {
  }
}, Qc = 1e3, tu = 0.15, eu = 0.5, ru = 0.4, su = 0.6, iu = /[,.;:!?，。；：！？、）】》」』’”)[\]}>~…]$/;
function nu(e, t, r, s) {
  const i = e.length;
  if (i === 0 || t <= 0) return [];
  const n = /* @__PURE__ */ new Set();
  let a = 0;
  for (const { segment: v, isWordLike: p } of s.segment(r))
    a > 0 && p && [...v].some((x) => yr(x)) && n.add(a), a += v.length;
  const o = new Int32Array(i + 1), h = new Float64Array(i + 1);
  for (let v = 0; v < i; v++)
    o[v + 1] = o[v] + e[v].text.length, h[v + 1] = h[v] + e[v].width;
  if (h[i] <= t) return [];
  const l = new Float64Array(i + 1).fill(Number.POSITIVE_INFINITY), c = new Int32Array(i + 1).fill(-1);
  l[i] = 0;
  const u = (t * tu) ** 2, d = (t * eu) ** 2;
  for (let v = i - 1; v >= 0; v--) for (let p = v + 1; p <= i; p++) {
    const x = h[p] - h[v];
    let f = 0;
    if (x > t) if (p === v + 1) f = (x - t) ** 2 * Qc;
    else continue;
    else f = (t - x) ** 2;
    let E = 0;
    if (p < i) {
      const A = e[p - 1];
      iu.test(A.text) ? E = -((t * su) ** 2) : A.isSpace ? E = -((t * ru) ** 2) : n.has(o[p]) ? E = u : E = d;
    }
    const g = f + E + l[p];
    g < l[v] && (l[v] = g, c[v] = p);
  }
  const y = [];
  let m = 0;
  for (; m < i; )
    m = c[m], m > 0 && m < i && y.push(m);
  return y;
}
var Is = null;
function au() {
  return Is || (Is = document.createElement("canvas").getContext("2d")), Is;
}
var ou = class {
  mainElement;
  isBalancing = !1;
  lastBalancedContainerWidth = -1;
  constructor(e) {
    this.mainElement = e;
  }
  balanceLineBreaks(e, t, r) {
    if (this.isBalancing || !this.mainElement) return;
    const s = getComputedStyle(this.mainElement), i = Number.parseFloat(s.paddingLeft) || 0, n = Number.parseFloat(s.paddingRight) || 0, a = this.mainElement.clientWidth - i - n;
    if (!(a <= 0)) {
      if (e) {
        this.balanceNonDynamicLineBreaks(a, s, r);
        return;
      }
      t && this.balanceDynamicLineBreaks(a, r);
    }
  }
  reset() {
    this.lastBalancedContainerWidth = -1;
  }
  executeLineBalance(e, t, r) {
    const s = this.mainElement.querySelectorAll("br");
    if (e === this.lastBalancedContainerWidth && s.length > 0) return;
    t.resetDOM();
    const i = this.mainElement.style.whiteSpace;
    this.mainElement.style.whiteSpace = "nowrap";
    const n = this.mainElement.parentElement;
    let a = "", o = !1;
    n && (a = n.style.transform, a && a !== "none" && (n.style.transform = "none", o = !0));
    let h = !1;
    try {
      const { childInfos: l, fullText: c } = t.buildChildInfos();
      let u = l.reduce((m, v) => m + v.width, 0);
      if (t.needsCalibration) {
        const m = document.createRange();
        m.selectNodeContents(this.mainElement);
        const v = m.getBoundingClientRect().width;
        if (u > 0 && v > 0) {
          const p = v / u;
          for (const x of l) x.width *= p;
        }
        u = v;
      }
      const d = Math.max(1, e);
      if (u <= d) {
        this.lastBalancedContainerWidth = e;
        return;
      }
      const y = nu(l, d, c, r);
      if (y.length === 0) {
        this.lastBalancedContainerWidth = e;
        return;
      }
      this.isBalancing = !0, h = !0, t.applyBreaks(y, l), this.lastBalancedContainerWidth = e, this.isBalancing = !1;
    } finally {
      this.mainElement.style.whiteSpace = i, o && n && (n.style.transform = a), h && (this.isBalancing = !1);
    }
  }
  balanceDynamicLineBreaks(e, t) {
    const r = [];
    this.executeLineBalance(e, {
      resetDOM: () => {
        this.mainElement.querySelectorAll("br").forEach((s) => {
          s.remove();
        });
      },
      buildChildInfos: () => {
        r.length = 0;
        const s = Array.from(this.mainElement.childNodes), i = [], n = document.createRange();
        for (const a of s) if (a.nodeType === Node.TEXT_NODE) {
          const o = a.textContent ?? "";
          if (o.length === 0) continue;
          n.selectNodeContents(a), i.push({
            width: n.getBoundingClientRect().width,
            text: o,
            isSpace: o.trim().length === 0
          }), r.push(a);
        } else if (a.nodeType === Node.ELEMENT_NODE) {
          const o = a, h = o.getBoundingClientRect(), l = getComputedStyle(o), c = Number.parseFloat(l.marginLeft) || 0, u = Number.parseFloat(l.marginRight) || 0;
          i.push({
            width: ee(h.width + c + u),
            text: o.textContent ?? "",
            isSpace: !1
          }), r.push(a);
        }
        return {
          childInfos: i,
          fullText: i.map((a) => a.text).join("")
        };
      },
      applyBreaks: (s) => {
        for (let i = s.length - 1; i >= 0; i--) {
          const n = s[i];
          n >= 0 && n < r.length && this.mainElement.insertBefore(document.createElement("br"), r[n]);
        }
      },
      needsCalibration: !1
    }, t);
  }
  balanceNonDynamicLineBreaks(e, t, r) {
    const s = this.mainElement.textContent ?? "";
    s.trim().length !== 0 && this.executeLineBalance(e, {
      resetDOM: () => {
        this.mainElement.innerHTML = "", this.mainElement.textContent = s;
      },
      buildChildInfos: () => {
        const i = au();
        if (!i)
          return console.debug("Canvas 2D context is not supported, skipping line balancing"), {
            childInfos: [],
            fullText: s
          };
        i.font = `${t.fontWeight} ${t.fontSize} ${t.fontFamily}`, "letterSpacing" in i && (i.letterSpacing = t.letterSpacing !== "normal" ? t.letterSpacing : "0px"), "wordSpacing" in i && (i.wordSpacing = t.wordSpacing !== "normal" ? t.wordSpacing : "0px");
        const n = [];
        for (const { segment: a } of r.segment(s)) n.push({
          width: i.measureText(a).width,
          text: a,
          isSpace: a.trim().length === 0
        });
        return {
          childInfos: n,
          fullText: s
        };
      },
      applyBreaks: (i, n) => {
        this.mainElement.innerHTML = "";
        const a = new Set(i), o = document.createDocumentFragment();
        for (let h = 0; h < n.length; h++)
          a.has(h) && o.appendChild(document.createElement("br")), o.appendChild(document.createTextNode(n[h].text));
        this.mainElement.appendChild(o);
      },
      needsCalibration: !0
    }, r);
  }
}, hu = /(\s+)/, lu = /\s/g;
function cu(e) {
  const t = [];
  let r = [];
  const s = () => {
    r.length > 0 && (t.push(r.length === 1 ? r[0] : [...r]), r = []);
  }, i = (n) => {
    const a = n.word.trim().length === 0, o = (n.ruby?.length ?? 0) > 0, h = yr(n.word);
    !a && !o && !h ? r.push(n) : (s(), t.push(n));
  };
  for (const n of e) {
    const a = n.word.trim().length === 0, o = n.romanWord ?? "", h = n.obscene ?? !1, l = (n.ruby?.length ?? 0) > 0;
    if (a || l) {
      i({ ...n });
      continue;
    }
    const c = n.word.split(hu).filter((m) => m.length > 0), u = n.word.replace(lu, "").length || 1, d = (n.endTime - n.startTime) / u;
    let y = 0;
    for (const m of c) {
      if (!m.trim()) {
        const v = n.startTime + y * d;
        i({
          word: m,
          romanWord: "",
          startTime: v,
          endTime: v,
          obscene: h
        });
        continue;
      }
      if (yr(m) && m.length > 1 && o.trim().length === 0) {
        const v = m.split("");
        for (const p of v) {
          const x = n.startTime + y * d;
          i({
            word: p,
            romanWord: "",
            startTime: x,
            endTime: x + d,
            obscene: h
          }), y += 1;
        }
      } else {
        const v = m.length, p = n.startTime + y * d;
        i({
          word: m,
          romanWord: o,
          startTime: p,
          endTime: p + v * d,
          obscene: h
        }), y += v;
      }
    }
  }
  return s(), t;
}
function uu() {
  return [
    1,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    1
  ];
}
function du(e, t = 1, r = {
  x: 0,
  y: 0
}) {
  const [s, i] = [r.x, r.y];
  return [
    e[0] * t,
    e[1] * t,
    e[2] * t,
    e[3],
    e[4] * t,
    e[5] * t,
    e[6] * t,
    e[7],
    e[8] * t,
    e[9] * t,
    e[10] * t,
    e[11],
    e[12] - s * t + s,
    e[13] - i * t + i,
    e[14],
    e[15]
  ];
}
function fu(e, t = 4) {
  const r = (s, i) => s.toFixed(t);
  return `matrix3d(${e.map(r).join(", ")})`;
}
var Br = 32, Ka = (e, t) => (r) => Xt((r - e) / (t - e)), li = 0.5, pu = Ka(0, li), mu = Ka(li, 1), yu = Va(0.2, 0.4, 0.58, 1), gu = Va(0.3, 0, 0.58, 1), vu = (e) => (t) => t < e ? yu(pu(t)) : 1 - gu(mu(t));
function xn(e, t = 0, r = "rgba(0,0,0,var(--bright-mask-alpha, 1.0))", s = "rgba(0,0,0,var(--dark-mask-alpha, 1.0))") {
  const i = 2 + e + t, n = e / i, a = (1 - n) / 2;
  return [`linear-gradient(to right,${r} ${a * 100}%,${s} ${(a + n) * 100}%)`, i];
}
var xu = class extends oe {
  lyricPlayer;
  lyricLine;
  element = document.createElement("div");
  splittedWords = [];
  built = !1;
  lineSize = [0, 0];
  renderMode = ue.SOLID;
  currentBrightAlpha = 1;
  currentDarkAlpha = 0.2;
  targetBrightAlpha = 1;
  targetDarkAlpha = 0.2;
  balancer;
  constructor(e, t = {
    words: [],
    translatedLyric: "",
    romanLyric: "",
    startTime: 0,
    endTime: 0,
    isBG: !1,
    isDuet: !1
  }) {
    super(), this.lyricPlayer = e, this.lyricLine = t, this.element.setAttribute("class", at.lyricLine), this.lyricLine.isBG && this.element.classList.add(at.lyricBgLine), this.lyricLine.isDuet && this.element.classList.add(at.lyricDuetLine), this.element.appendChild(document.createElement("div")), this.element.appendChild(document.createElement("div")), this.element.appendChild(document.createElement("div"));
    const r = this.element.children[0], s = this.element.children[1], i = this.element.children[2];
    r.setAttribute("class", at.lyricMainLine), s.setAttribute("class", at.lyricSubLine), i.setAttribute("class", at.lyricSubLine), oe.wordSegmenter && (this.balancer = new ou(r)), this.rebuildStyle();
  }
  areWordsOnSameLine(e, t) {
    if (e?.mainElement && t?.mainElement) {
      const r = e.mainElement, s = t.mainElement, i = r.getBoundingClientRect(), n = s.getBoundingClientRect();
      return Math.abs(i.top - n.top) < 10;
    }
    return !0;
  }
  isEnabled = !1;
  async enable(e = this.lyricPlayer.getCurrentTime(), t = this.lyricPlayer.getIsPlaying()) {
    this.isEnabled = !0, this.element.classList.add(at.active);
    const r = this.element.children[0], s = ee(e - this.lyricLine.startTime);
    for (const i of this.splittedWords) {
      for (const n of i.elementAnimations) {
        n.currentTime = s, n.playbackRate = 1;
        const a = n.effect?.getComputedTiming(), o = Number(a?.duration ?? 0), h = Number(a?.delay ?? 0) + o;
        t && s < h ? n.play() : n.pause();
      }
      for (const n of i.maskAnimations) {
        const a = Math.min(this.totalDuration, s);
        n.currentTime = a, n.playbackRate = 1;
        const o = n.effect?.getComputedTiming(), h = Number(o?.duration ?? 0), l = Number(o?.delay ?? 0) + h;
        t && a < l ? n.play() : n.pause();
      }
    }
    r.classList.add(at.active);
  }
  disable() {
    this.isEnabled = !1, this.element.classList.remove(at.active), this.renderMode = ue.SOLID;
    const e = this.element.children[0];
    for (const t of this.splittedWords) {
      for (const r of t.elementAnimations) (r.id === "float-word" || r.id.includes("emphasize-word-float-only")) && (r.playbackRate = -1, r.play());
      for (const r of t.maskAnimations) r.pause();
    }
    e.classList.remove(at.active);
  }
  lastWord;
  async resume() {
    if (this.isEnabled)
      for (const e of this.splittedWords) {
        for (const t of e.elementAnimations) if (!this.lastWord || this.splittedWords.indexOf(this.lastWord) < this.splittedWords.indexOf(e)) {
          const r = t.effect?.getComputedTiming(), s = r?.duration || 0, i = (r?.delay || 0) + s, n = t.currentTime || 0;
          t.playState !== "finished" && n < i && t.play();
        }
        for (const t of e.maskAnimations) if (!this.lastWord || this.splittedWords.indexOf(this.lastWord) < this.splittedWords.indexOf(e)) {
          const r = t.effect?.getComputedTiming(), s = r?.duration || 0, i = (r?.delay || 0) + s, n = t.currentTime || 0;
          t.playState !== "finished" && n < i && t.play();
        }
      }
  }
  async pause() {
    if (this.isEnabled)
      for (const e of this.splittedWords) {
        for (const t of e.elementAnimations) t.pause();
        for (const t of e.maskAnimations) t.pause();
      }
  }
  setMaskAnimationState(e = 0) {
    const t = e - this.lyricLine.startTime;
    for (const r of this.splittedWords) for (const s of r.maskAnimations)
      s.currentTime = de(t, 0, this.totalDuration), s.playbackRate = 1, t >= 0 && t < this.totalDuration ? s.play() : s.pause();
  }
  getLine() {
    return this.lyricLine;
  }
  lastStyle = "";
  show() {
    this.built || (this.rebuildElement(), this.built = !0, this.updateMaskImageSync());
  }
  rebuildStyle() {
    let e = "";
    e += `transform: scale(${(this.lineTransforms.scale.getCurrentPosition() / 100).toFixed(4)});`, this.lyricPlayer.getEnableSpring() || (e += `transition-delay:${this.delay}ms;`), e += `filter:blur(${Math.min(5, this.blur)}px);`, e !== this.lastStyle && (this.lastStyle = e, this.element.setAttribute("style", e));
  }
  rebuildElement() {
    this.disposeElements();
    const e = this.element.children[0], t = this.element.children[1], r = this.element.children[2];
    if (this.lyricPlayer._getIsNonDynamic()) {
      e.textContent = this.lyricLine.words.map((a) => this.lyricPlayer.processObsceneWord(a)).join(""), this.setSubLinesText(t, r);
      return;
    }
    const s = cu(this.lyricLine.words), i = this.lyricLine.words.some((a) => (a.ruby?.length ?? 0) > 0), n = this.lyricLine.words.some((a) => (a.romanWord?.trim().length ?? 0) > 0);
    e.innerHTML = "";
    for (const a of s) this.buildWord(a, e, i, n);
    this.setSubLinesText(t, r);
  }
  setSubLinesText(e, t) {
    e.textContent = this.lyricLine.translatedLyric, t.textContent = this.lyricLine.romanLyric;
  }
  getRubyCharCount(e) {
    return (e.ruby ?? []).reduce((t, r) => t + r.word.length, 0);
  }
  getRubySegments(e) {
    return (e.ruby ?? []).filter((t) => (t?.word?.trim().length ?? 0) > 0);
  }
  createWord(e, t, r, s) {
    const i = document.createElement("span"), n = [], a = e.romanWord?.trim() ?? "", o = r ? document.createElement("div") : i;
    if (r) {
      const l = document.createElement("div"), c = this.getRubySegments(e);
      for (const u of c) {
        const d = document.createElement("span");
        d.textContent = u.word, d.dataset.startTime = String(u.startTime), d.dataset.endTime = String(u.endTime), l.appendChild(d);
      }
      l.classList.add(at.rubyWord), i.classList.add(at.wordWithRuby), o.classList.add(at.wordBody), i.appendChild(l), i.appendChild(o);
    }
    const h = this.lyricPlayer.processObsceneWord(e);
    if (t) {
      i.classList.add(at.emphasize);
      const l = h.trim();
      if (oe.graphemeSegmenter) for (const { segment: c } of oe.graphemeSegmenter.segment(l)) {
        const u = document.createElement("span");
        u.textContent = c, n.push(u), o.appendChild(u);
      }
      else for (const c of Array.from(l)) {
        const u = document.createElement("span");
        u.textContent = c, n.push(u), o.appendChild(u);
      }
    } else if (s) {
      const l = document.createElement("div");
      l.textContent = h.trim(), o.appendChild(l);
    } else a.length === 0 && (o.textContent = h.trim());
    if (s) {
      const l = document.createElement("div");
      l.textContent = a.length > 0 ? a : " ", l.classList.add(at.romanWord), o.appendChild(l);
    }
    return {
      ...e,
      mainElement: i,
      subElements: n,
      elementAnimations: [this.initFloatAnimation(e, i)],
      maskAnimations: [],
      width: 0,
      height: 0,
      padding: 0,
      shouldEmphasize: t
    };
  }
  buildWord(e, t, r, s) {
    const i = Array.isArray(e) ? e : [e];
    if (i.length === 0) return;
    if (i.every((l) => !l.word.trim())) {
      const l = i.map((c) => c.word).join("");
      t.appendChild(document.createTextNode(l));
      return;
    }
    const n = i.reduce((l, c) => (l.endTime = Math.max(l.endTime, c.endTime), l.startTime = Math.min(l.startTime, c.startTime), l.word += c.word, l), {
      word: "",
      romanWord: "",
      startTime: Number.POSITIVE_INFINITY,
      endTime: Number.NEGATIVE_INFINITY,
      wordType: "normal",
      obscene: !1
    });
    let a = i.some((l) => oe.shouldEmphasize(l));
    yr(n.word) || (a = a || oe.shouldEmphasize(n));
    const o = document.createElement("span");
    o.classList.add(at.emphasizeWrapper);
    const h = [];
    for (const l of i) {
      if (!l.word.trim()) {
        o.appendChild(document.createTextNode(l.word));
        continue;
      }
      const c = this.createWord(l, a, r, s);
      a && h.push(...c.subElements), this.splittedWords.push(c), o.appendChild(c.mainElement);
    }
    if (a && this.splittedWords.length > 0) {
      const l = this.splittedWords[this.splittedWords.length - 1], c = i.reduce((u, d) => u + this.getRubyCharCount(d), 0);
      l.elementAnimations.push(...this.initEmphasizeAnimation(n, h, n.endTime - n.startTime, n.startTime - this.lyricLine.startTime, c));
    }
    t.appendChild(o);
  }
  initFloatAnimation(e, t) {
    const r = e.startTime - this.lyricLine.startTime, s = Math.max(1e3, e.endTime - e.startTime);
    let i = 0.05;
    this.lyricLine.isBG && (i *= 2);
    const n = t.animate([{ transform: "translateY(0px)" }, { transform: `translateY(${-i}em)` }], {
      duration: Number.isFinite(s) ? s : 0,
      delay: Number.isFinite(r) ? r : 0,
      id: "float-word",
      composite: "add",
      fill: "both",
      easing: "ease-out"
    });
    return n.pause(), n;
  }
  initEmphasizeAnimation(e, t, r, s, i) {
    const n = ee(s);
    let a = Math.max(1e3, r);
    const o = i > 0 ? i : Math.max(1, t.length);
    let h = [], l = a / 2e3;
    l = l > 1 ? Math.sqrt(l) : l ** 3;
    let c = a / 3e3;
    c = c > 1 ? Math.sqrt(c) : c ** 3, l *= 0.6, c *= 0.5, this.lyricLine.words.length > 0 && e.word.includes(this.lyricLine.words[this.lyricLine.words.length - 1].word) && (l *= 1.6, c *= 1.5, a *= 1.2), l = Math.min(1.2, l), c = Math.min(0.8, c);
    const u = Number.isFinite(a) ? a : 0, d = vu(li);
    return h = t.flatMap((y, m, v) => {
      const p = n + a / 2.5 / o * m, x = [], f = new Array(Br).fill(0).map((L, M) => {
        const C = (M + 1) / Br, k = d(C), B = d(C) * c, w = du(uu(), 1 + k * 0.1 * l), F = -k * 0.03 * l * (v.length / 2 - m), P = -k * 0.025 * l;
        return {
          offset: C,
          transform: `${fu(w, 4)} translate(${F}em, ${P}em)`,
          textShadow: `0 0 ${Math.min(0.3, c * 0.3)}em rgba(255, 255, 255, ${B})`
        };
      }), E = y.animate(f, {
        duration: u,
        delay: Number.isFinite(p) ? p : 0,
        id: `emphasize-word-${y.textContent}-${m}`,
        iterations: 1,
        composite: "replace",
        fill: "both"
      });
      E.onfinish = () => {
        E.pause();
      }, E.pause(), x.push(E);
      const g = new Array(Br).fill(0).map((L, M) => {
        const C = (M + 1) / Br;
        let k = Math.sin(C * Math.PI);
        return this.lyricLine.isBG && (k *= 2), {
          offset: C,
          transform: `translateY(${-k * 0.05}em)`
        };
      }), A = y.animate(g, {
        duration: u * 1.4,
        delay: Number.isFinite(p) ? p - 400 : 0,
        id: "emphasize-word-float",
        iterations: 1,
        composite: "add",
        fill: "both"
      });
      return A.onfinish = () => {
        A.pause();
      }, A.pause(), x.push(A), x;
    }), h;
  }
  get totalDuration() {
    return this.lyricLine.endTime - this.lyricLine.startTime;
  }
  onLineSizeChange(e) {
    this.updateMaskImageSync();
  }
  updateMaskImageSync() {
    for (const e of this.splittedWords) {
      const t = e.mainElement;
      t ? (e.padding = Number.parseFloat(getComputedStyle(t).paddingLeft), e.width = t.clientWidth - e.padding * 2, e.height = t.clientHeight - e.padding * 2) : (e.width = 0, e.height = 0, e.padding = 0);
    }
    if (this.balancer && oe.wordSegmenter && this.balancer.balanceLineBreaks(this.lyricPlayer._getIsNonDynamic(), this.splittedWords.length > 0, oe.wordSegmenter), this.lyricPlayer.supportMaskImage ? this.generateWebAnimationBasedMaskImage() : this.generateCalcBasedMaskImage(), this.isEnabled) {
      const e = this.lyricPlayer.getIsPlaying?.() ?? !0;
      this.enable(this.lyricPlayer.getCurrentTime(), e);
    }
  }
  generateCalcBasedMaskImage() {
    for (const e of this.splittedWords) {
      const t = e.mainElement;
      if (t) {
        e.width = t.clientWidth, e.height = t.clientHeight;
        const r = e.height * this.lyricPlayer.getWordFadeWidth(), [s, i] = xn(r / e.width), n = `${i * 100}% 100%`;
        this.lyricPlayer.supportMaskImage ? (t.style.maskImage = s, t.style.maskRepeat = "no-repeat", t.style.maskOrigin = "left", t.style.maskSize = n) : (t.style.webkitMaskImage = s, t.style.webkitMaskRepeat = "no-repeat", t.style.webkitMaskOrigin = "left", t.style.webkitMaskSize = n);
        const a = e.width + r, o = `clamp(${-a}px,calc(${-a}px + (var(--amll-player-time) - ${e.startTime})*${a / Math.abs(e.endTime - e.startTime)}px),0px) 0px, left top`;
        t.style.maskPosition = o, t.style.webkitMaskPosition = o;
      }
    }
  }
  generateWebAnimationBasedMaskImage() {
    const e = Math.max(0, ...this.splittedWords.map((t) => t.endTime), this.lyricLine.endTime) - this.lyricLine.startTime;
    this.splittedWords.forEach((t, r) => {
      const s = t.mainElement;
      if (s) {
        const i = t.height * this.lyricPlayer.getWordFadeWidth(), [n, a] = xn(i / (t.width + t.padding * 2)), o = `${a * 100}% 100%`;
        this.lyricPlayer.supportMaskImage ? (s.style.maskImage = n, s.style.maskRepeat = "no-repeat", s.style.maskOrigin = "left", s.style.maskSize = o) : (s.style.webkitMaskImage = n, s.style.webkitMaskRepeat = "no-repeat", s.style.webkitMaskOrigin = "left", s.style.webkitMaskSize = o);
        const h = this.splittedWords.slice(0, r).reduce((f, E) => f + E.width, 0) + (this.splittedWords[0] ? i : 0), l = -(t.width + t.padding * 2 + i), c = (f) => de(f, l, 0);
        let u = -h - t.width - t.padding - i, d = 0;
        const y = [];
        let m = u, v = 0;
        const p = () => {
          const f = u - m, E = Xt(d), g = E - v, A = Math.abs(g / f);
          if (u > l && m < l) {
            const M = Math.abs(m - l) * A, C = `${c(m)}px 0`, k = {
              offset: v + M,
              maskPosition: C
            };
            y.push(k);
          }
          if (u > 0 && m < 0) {
            const M = Math.abs(m) * A, C = `${c(u)}px 0`, k = {
              offset: v + M,
              maskPosition: C
            };
            y.push(k);
          }
          const L = {
            offset: E,
            maskPosition: `${c(u)}px 0`
          };
          y.push(L), m = u, v = E;
        };
        p();
        let x = 0;
        this.splittedWords.forEach((f, E) => {
          {
            const g = f.startTime - this.lyricLine.startTime, A = g - x;
            d += A / e, A > 0 && p(), x = g;
          }
          {
            const g = ee(f.endTime - f.startTime), A = this.getRubySegments(f), L = A.reduce((M, C) => M + C.word.length, 0);
            if (L > 0) {
              const M = f.width / L;
              let C = 0;
              for (const w of A) {
                const F = Number.isFinite(w.startTime) ? w.startTime : f.startTime, P = Number.isFinite(w.endTime) ? w.endTime : f.endTime, G = Math.max(F, f.startTime), $ = Math.min(Math.max(P, G), f.endTime), H = G - this.lyricLine.startTime, Q = H - x;
                d += Q / e, Q > 0 && p(), x = H;
                const _ = ee($ - G) / w.word.length;
                for (let T = 0; T < w.word.length; T++)
                  d += _ / e, u += M, E === 0 && C === 0 && (u += i * 1.5), E === this.splittedWords.length - 1 && C === L - 1 && (u += i * 0.5), _ > 0 && p(), x += _, C++;
              }
              const k = Math.max(f.endTime - this.lyricLine.startTime, x), B = k - x;
              d += B / e, B > 0 && p(), x = k;
            } else {
              const C = f.width / 1, k = g / 1;
              for (let B = 0; B < 1; B++)
                d += k / e, u += C, E === 0 && B === 0 && (u += i * 1.5), E === this.splittedWords.length - 1 && B === 0 && (u += i * 0.5), k > 0 && p(), x += k;
            }
          }
        });
        for (const f of t.maskAnimations) f.cancel();
        try {
          const f = s.animate(y, {
            duration: e || 1,
            id: `fade-word-${t.word}-${r}`,
            fill: "both"
          });
          f.pause(), t.maskAnimations = [f];
        } catch (f) {
          console.warn("应用渐变动画发生错误", y, e, f);
        }
      }
    });
  }
  getElement() {
    return this.element;
  }
  updateMaskAlphaTargets(e) {
    const t = Xt((e - 0.97) / 0.03), r = t * 0.2 + 0.2, s = t * 0.8 + 0.2;
    this.renderMode === ue.SOLID ? (this.targetBrightAlpha = r, this.targetDarkAlpha = r) : (this.targetBrightAlpha = s, this.targetDarkAlpha = r);
  }
  applyAlphaToDom(e) {
    const t = e || 0.016, r = 50, s = 7, i = (o) => 1 - Math.exp(-o * t), n = i(this.targetBrightAlpha > this.currentBrightAlpha ? r : s);
    Math.abs(this.targetBrightAlpha - this.currentBrightAlpha) < 1e-3 ? this.currentBrightAlpha = this.targetBrightAlpha : this.currentBrightAlpha += (this.targetBrightAlpha - this.currentBrightAlpha) * n;
    const a = i(this.targetDarkAlpha > this.currentDarkAlpha ? r : s);
    Math.abs(this.targetDarkAlpha - this.currentDarkAlpha) < 1e-3 ? this.currentDarkAlpha = this.targetDarkAlpha : this.currentDarkAlpha += (this.targetDarkAlpha - this.currentDarkAlpha) * a, this.element.style.setProperty("--bright-mask-alpha", this.currentBrightAlpha.toFixed(3)), this.element.style.setProperty("--dark-mask-alpha", this.currentDarkAlpha.toFixed(3));
  }
  setTransform(e = this.scale, t = 1, r = 0, s = !1, i = 0, n = ue.SOLID) {
    super.setTransform(e, t, r, s, i), this.renderMode = n;
    const a = this.lyricPlayer.getEnableSpring();
    this.top = 0, this.scale = e, this.delay = i * 1e3 | 0;
    const o = this.element.children[0];
    if (o.style.opacity = `${t}`, s || !a) {
      this.blur = Math.min(32, r), this.lineTransforms.scale.setPosition(e), this.rebuildStyle();
      const h = this.lineTransforms.scale.getCurrentPosition();
      this.updateMaskAlphaTargets(h / 100), this.currentBrightAlpha = this.targetBrightAlpha, this.currentDarkAlpha = this.targetDarkAlpha, this.element.style.setProperty("--bright-mask-alpha", String(this.currentBrightAlpha)), this.element.style.setProperty("--dark-mask-alpha", String(this.currentDarkAlpha));
    } else
      this.lineTransforms.scale.setTargetPosition(e), this.blur !== Math.min(5, r) && (this.blur = Math.min(5, r), this.element.style.filter = `blur(${r.toFixed(3)}px)`);
  }
  update(e = 0) {
    if (!this.lyricPlayer.getEnableSpring() || (this.lineTransforms.scale.update(e), this.rebuildStyle(), !this.built)) return;
    const t = this.lineTransforms.scale.getCurrentPosition() / 100;
    this.updateMaskAlphaTargets(t), this.applyAlphaToDom(e);
  }
  _getDebugTargetPos() {
    return `[位移: ${this.top}; 缩放: ${this.scale}; 延时: ${this.delay}]`;
  }
  teardownContent() {
    this.built && (this.disposeElements(), this.built = !1);
  }
  disposeElements() {
    this.balancer?.reset();
    for (const s of this.splittedWords) {
      for (const i of s.elementAnimations) i.cancel();
      for (const i of s.maskAnimations) i.cancel();
      for (const i of s.subElements)
        i.remove(), i.parentNode?.removeChild(i);
      s.elementAnimations = [], s.maskAnimations = [], s.subElements = [], s.mainElement?.parentNode && s.mainElement.parentNode.removeChild(s.mainElement);
    }
    this.splittedWords = [];
    const e = this.element.children[0], t = this.element.children[1], r = this.element.children[2];
    e && (e.innerHTML = ""), t && (t.innerHTML = ""), r && (r.innerHTML = "");
  }
  dispose() {
    this.disposeElements(), this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove();
  }
}, Za = class extends MouseEvent {
  lineIndex;
  line;
  bgLine;
  isPropagationStopped = !1;
  constructor(e, t, r, s) {
    super(`line-${s.type}`, s), this.lineIndex = e, this.line = t, this.bgLine = r;
  }
  stopPropagation() {
    this.isPropagationStopped = !0, super.stopPropagation();
  }
  stopImmediatePropagation() {
    this.isPropagationStopped = !0, super.stopImmediatePropagation();
  }
}, _n = class extends Ya {
  abortController = new AbortController();
  currentLyricGroups = [];
  onResize() {
    const e = getComputedStyle(this.element);
    this._baseFontSize = Number.parseFloat(e.fontSize), this.rebuildStyle();
  }
  supportPlusLighter = CSS.supports("mix-blend-mode", "plus-lighter");
  supportMaskImage = CSS.supports("mask-image", "none");
  innerSize = [0, 0];
  onMouseEventHandler = (e) => {
    const t = e.target;
    if (!(t instanceof Element)) return;
    const r = t.closest(`.${at.lyricLineWrapper}`);
    if (!r) return;
    const s = this.lyricGroupElementMap.get(r);
    if (!s) return;
    const i = s.mainLine, n = s.bgLine, a = new Za(this.lyricLinesIndexes.get(i) ?? -1, i, n, e);
    (!this.dispatchEvent(a) || a.defaultPrevented) && e.preventDefault(), a.isPropagationStopped && (e.stopPropagation(), e.stopImmediatePropagation());
  };
  _getIsNonDynamic() {
    return this.isNonDynamic;
  }
  _baseFontSize = Number.parseFloat(getComputedStyle(this.element).fontSize);
  get baseFontSize() {
    return this._baseFontSize;
  }
  constructor() {
    super(), this.onResize(), this.element.classList.add("amll-lyric-player", "dom"), this.disableSpring && this.element.classList.add(at.disableSpring), this.element.addEventListener("click", this.onMouseEventHandler, { signal: this.abortController.signal }), this.element.addEventListener("contextmenu", this.onMouseEventHandler, { signal: this.abortController.signal });
  }
  rebuildStyle() {
  }
  setWordFadeWidth(e = 0.5) {
    super.setWordFadeWidth(e);
    for (const t of this.currentLyricGroups)
      t.mainLine.updateMaskImageSync(), t.bgLine?.updateMaskImageSync();
  }
  setLyricLines(e, t = 0) {
    super.setLyricLines(e, t), this.hasDuetLine ? this.element.classList.add(at.hasDuetLine) : this.element.classList.remove(at.hasDuetLine), this.supportMaskImage || this.element.style.setProperty("--amll-player-time", `${t}`);
    for (const s of this.currentLyricGroups) s.dispose();
    this.currentLyricGroups = [];
    let r = null;
    for (let s = 0; s < this.processedLines.length; s++) {
      const i = this.processedLines[s], n = new xu(this, i);
      this.lyricLinesIndexes.set(n, s), !i.isBG || !r ? (r = new Jc(this, n), this.currentLyricGroups.push(r), this.lyricGroupElementMap.set(r.element, r)) : r.addBgLine(n);
    }
    this.setLinePosXSpringParams({}), this.setLinePosYSpringParams({}), this.setLineScaleSpringParams({}), this.setCurrentTime(t, !0), this.calcLayout(!0), this.update(0);
  }
  pause() {
    super.pause(), this.element.classList.remove(at.playing), this.interludeDots.pause();
    for (const e of this.currentLyricGroups)
      e.mainLine.pause(), e.bgLine?.pause();
  }
  resume() {
    super.resume(), this.element.classList.add(at.playing), this.interludeDots.resume();
    for (const e of this.currentLyricGroups)
      e.mainLine.resume(), e.bgLine?.resume();
  }
  update(e = 0) {
    if (!this.timelineState.initialLayoutFinished || (super.update(e), this.supportMaskImage || this.element.style.setProperty("--amll-player-time", `${this.timelineState.currentTime}`), !this.isPageVisible)) return;
    const t = e / 1e3;
    for (const r of this.currentLyricGroups) r.update(t);
  }
  dispose() {
    super.dispose(), this.abortController.abort(), this.element.remove();
    for (const e of this.currentLyricGroups) e.dispose();
    this.bottomLine.dispose(), this.interludeDots.dispose();
  }
};
function _u(e, t = "debug") {
  window.Android?.log?.(e, t);
}
var Z = {
  player: null,
  background: null,
  currentTime: -1,
  lyricLines: [],
  albumUri: "",
  isPlaying: !1,
  hasPlaybackState: !1,
  pendingLyricOptions: {}
};
window.AMLLCore = oi;
var Tt = (e, t = "info") => _u(e, t), Re = (e, t) => {
  const r = typeof t == "boolean" ? t ? "1" : "0" : String(t);
  document.documentElement.style.setProperty(e, r);
};
function bu(e, t, r) {
  Object.assign(t.style, {
    position: "absolute",
    inset: "0",
    width: "100%",
    height: "100%",
    pointerEvents: "none",
    zIndex: r
  }), t.parentElement !== e && e.appendChild(t);
}
function Ja(e, t, r) {
  const s = e.BackgroundRender;
  if (!s?.new)
    return Tt("BackgroundRender factory not found on core", "debug"), null;
  const i = {
    mesh: e.MeshGradientRenderer,
    pixi: e.PixiRenderer
  }, n = r && i[r] ? [i[r]] : [e.MeshGradientRenderer, e.PixiRenderer].filter(Boolean);
  for (const a of n) try {
    const o = s.new(a);
    return bu(t, o.getElement(), "-1"), Tt(`Created BackgroundRender with ${a?.name || "renderer"}`, "debug"), o;
  } catch (o) {
    Tt(`BackgroundRender init failed with ${a?.name || "renderer"}: ${o.message}`, "warn");
  }
  return null;
}
function bn() {
  try {
    document.documentElement.style.background = "transparent", document.body.style.background = "transparent";
    const e = document.getElementById("app") || document.createElement("div");
    document.getElementById("app") || (e.id = "app", document.body.appendChild(e)), Object.assign(e.style, {
      position: "relative",
      width: "100%",
      height: "100vh"
    });
    const t = oi, r = t.DomLyricPlayer;
    if (r) try {
      Z.player = new r({
        container: e,
        album: Z.albumUri
      });
      const n = Z.player.getElement?.() || Z.player.element;
      n && (Object.assign(n.style, {
        position: "absolute",
        inset: "0",
        zIndex: "1",
        pointerEvents: "auto"
      }), n.parentElement !== e && e.appendChild(n));
      const a = (o) => {
        const h = o?.detail || {}, l = typeof o?.lineIndex == "number" ? o.lineIndex : typeof h.lineIndex == "number" ? h.lineIndex : -1, c = l >= 0 && Z.lyricLines?.[l] ? Z.lyricLines[l] : void 0, u = (y) => typeof y?.startTime == "number" ? y.startTime : typeof y?.start == "number" ? y.start : void 0, d = u(o) ?? u(h) ?? u(o?.line) ?? u(h.line) ?? u(c);
        Tt(`line-click: index=${l}, start=${d}`, "debug"), d !== void 0 && (window.Android?.onLineClick?.(l, d), Z.player && (Z.player.setCurrentTime ? Z.player.setCurrentTime(d, !1) : Z.player.seek && Z.player.seek(d), Z.player.update?.(0)));
      };
      Z.player.addEventListener?.("line-click", a), n?.addEventListener?.("line-click", a), Tt("Created and attached DomLyricPlayer", "debug");
    } catch (n) {
      Tt(`Failed to instantiate DomLyricPlayer: ${n.message}`, "error");
    }
    Z.background || (Z.background = Ja(t, e)), window.__amll = {
      player: Z.player,
      backgroundRender: Z.background
    };
    let s = performance.now();
    const i = (n) => {
      const a = n - s;
      s = n, Z.player && Z.player.update(a), requestAnimationFrame(i);
    };
    if (requestAnimationFrame(i), Z.lyricLines.length > 0 && Z.player) {
      const n = Z.player, a = n.setLyricLines || n.setLyrics || n.updateLyrics;
      a && (a.call(n, Z.lyricLines), n.calcLayout?.(), n.update?.(0), Tt(`Applied ${Z.lyricLines.length} pending lines to new player`, "info"));
    }
    Tt("AMLL core WebView initialized", "debug"), window.Android?.onPageReady?.(), setTimeout(() => {
      try {
        const n = Z.player?.element;
        if (!n) {
          Tt("DIAG: no player element", "error");
          return;
        }
        const a = n.querySelectorAll(".FmKaba_lyricLine"), o = n.querySelector(".FmKaba_lyricLine.FmKaba_active");
        if (Tt(`DIAG: ${a.length} lines, active=${JSON.stringify(o?.textContent)}`, "info"), o) {
          const h = o.getBoundingClientRect();
          Tt(`DIAG: active rect=${JSON.stringify({
            x: h.x,
            y: h.y,
            w: h.width,
            h: h.height
          })}`, "info"), o.style.setProperty("color", "red", "important"), o.style.setProperty("-webkit-mask-image", "none", "important"), o.style.setProperty("mask-image", "none", "important"), o.style.setProperty("opacity", "1", "important");
          const l = o.querySelectorAll("*");
          l.forEach((c) => {
            c.style.setProperty("color", "red", "important"), c.style.setProperty("-webkit-mask-image", "none", "important"), c.style.setProperty("mask-image", "none", "important"), c.style.setProperty("opacity", "1", "important");
          }), Tt(`DIAG: forced RED on active + ${l.length} descendants`, "info");
        }
      } catch (n) {
        Tt("DIAG error: " + n.message, "error");
      }
    }, 2500);
  } catch (e) {
    Tt(`Initialization error: ${e.message}`, "error");
  }
}
document.readyState === "loading" ? window.addEventListener("DOMContentLoaded", bn) : setTimeout(bn, 0);
window.updateLyrics = (e) => {
  try {
    const t = Array.isArray(e?.lines) ? e.lines : [];
    Z.lyricLines = t, Tt(`updateLyrics: ${t.length} lines`, "debug");
    const r = Z.player;
    if (r) {
      const s = r.setLyricLines || r.setLyrics || r.updateLyrics;
      s ? (s.call(r, t), r.calcLayout?.(), r.update?.(0)) : Tt("playerInstance does not expose lyric setter", "warn");
    }
  } catch (t) {
    Tt(`updateLyrics error: ${t.message}`, "error");
  }
};
window.updateTime = (e) => {
  if (Z.hasPlaybackState && !Z.isPlaying) return;
  const t = Math.trunc(e);
  if (Z.currentTime === t) return;
  Z.currentTime = t;
  const r = Z.player;
  if (r)
    try {
      r.setCurrentTime ? r.setCurrentTime(t, !1) : r.seek && r.seek(t), r.update?.(0);
    } catch (s) {
      Tt(`updateTime error: ${s.message}`, "error");
    }
};
window.updateAlbumArt = async (e) => {
  if (!e || e.trim().length === 0) {
    Z.albumUri = "";
    return;
  }
  try {
    let t = e;
    const r = e.startsWith("file:"), s = e.startsWith("data:"), i = e.startsWith("blob:"), n = e.startsWith("http://") || e.startsWith("https://");
    if (r) {
      const a = await (await fetch(e, { cache: "no-store" })).blob();
      t = await new Promise((o, h) => {
        const l = new FileReader();
        l.onload = () => o(l.result), l.onerror = () => h(l.error), l.readAsDataURL(a);
      });
    } else if (n && !s && !i) {
      const a = `t=${Date.now()}`;
      t = e.includes("?") ? `${e}&${a}` : `${e}?${a}`;
    }
    if (Z.albumUri = t, Z.background?.setAlbum) try {
      await Z.background.setAlbum(Z.albumUri), Z.background.update?.(0);
    } catch (a) {
      Tt(`setAlbum error: ${a.message}. `, "warn");
    }
  } catch (t) {
    Tt(`updateAlbumArt error: ${t.message}`, "error");
  }
};
window.setPaused = (e) => {
  const t = !e;
  if (Z.hasPlaybackState && Z.isPlaying === t) return;
  Z.isPlaying = t, Z.hasPlaybackState = !0;
  const r = Z.player;
  if (r)
    try {
      e ? r.pause?.() : r.resume?.(), r.update?.(0);
    } catch (s) {
      Tt(`setPaused error: ${s.message}`, "error");
    }
};
window.configureLyricMotion = (e) => {
  Z.pendingLyricOptions = {
    ...Z.pendingLyricOptions,
    ...e
  };
  const t = Z.player;
  if (t)
    try {
      const { springPosY: r, enableSpring: s, springScale: i, enableScale: n, enableBlur: a, hidePassedLines: o, wordFadeWidth: h } = e;
      r && t.setLinePosYSpringParams && t.setLinePosYSpringParams(r), s !== void 0 && t.setEnableSpring && t.setEnableSpring(s), i && t.setLineScaleSpringParams && t.setLineScaleSpringParams(i), n !== void 0 && t.setEnableScale && t.setEnableScale(n), a !== void 0 && t.setEnableBlur && t.setEnableBlur(a), o !== void 0 && t.setHidePassedLines && t.setHidePassedLines(o), h !== void 0 && t.setWordFadeWidth && t.setWordFadeWidth(h), t.calcLayout?.(), t.update?.(0);
    } catch (r) {
      Tt(`configureLyricMotion error: ${r.message}`, "error");
    }
};
window.configureBackgroundEffect = (e) => {
  const t = Z.background;
  if (t)
    try {
      e.flowSpeed !== void 0 && t.setFlowSpeed?.(e.flowSpeed), e.renderScale !== void 0 && t.setRenderScale?.(e.renderScale), e.lowFreqVolume !== void 0 && t.setLowFreqVolume?.(e.lowFreqVolume), e.fps !== void 0 && t.setFPS?.(e.fps), e.staticMode !== void 0 && t.setStaticMode?.(e.staticMode), t.update?.(0);
    } catch (r) {
      Tt(`configureBackgroundEffect error: ${r.message}`, "error");
    }
};
window.configureLyricBackground = (e) => {
  try {
    const t = e.renderer === "css-bg";
    !Z.background && !t && (Z.background = Ja(oi, document.getElementById("app"), e.renderer));
    const r = Z.background?.getElement?.();
    if (r && (r.style.display = t ? "none" : "block"), Z.background) {
      const { fps: s, renderScale: i, staticMode: n } = e;
      s !== void 0 && Z.background.setFPS?.(s), i !== void 0 && Z.background.setRenderScale?.(i), n !== void 0 && Z.background.setStaticMode?.(n), Z.background.update?.(0);
    }
    document.body.style.background = t && e.cssProperty ? e.cssProperty : "transparent";
  } catch (t) {
    Tt(`configureLyricBackground error: ${t.message}`, "error");
  }
};
window.setLyricSizePreset = (e) => {
  e !== void 0 && Re("--amll-lp-font-size-preset", e);
};
window.setEnableTranslationLine = (e) => {
  e !== void 0 && Re("--amll-show-translation", e);
};
window.setEnableRomanLine = (e) => {
  e !== void 0 && Re("--amll-show-roman", e);
};
window.setEnableSwapTransRomanLine = (e) => {
  e !== void 0 && Re("--amll-swap-trans-roman", e);
};
window.setAdvanceLyricDynamicLyricTime = (e) => {
  e !== void 0 && (Re("--amll-advance-dynamic-time", e), Z.pendingLyricOptions.advanceDynamicTime = e);
};
window.applyFontSettings = (e) => {
  const { effectiveFamily: t, files: r = [] } = e || {}, s = "amll-dynamic-font-face-style";
  let i = document.getElementById(s);
  i || (i = document.createElement("style"), i.id = s, document.head.appendChild(i));
  let n = "";
  if (Array.isArray(r)) for (const a of r)
    !a?.familyName || !a?.uri || a.uri.startsWith("data:image/svg+xml") || (n += `@font-face{font-family:"${a.familyName}";src:url("${a.uri}");font-display:swap;}`);
  i.textContent = n, Re("--amll-user-font-family", t || ""), Re("--amll-lp-font-family", t ? "var(--amll-user-font-family)" : ""), document.querySelectorAll(".amll-lyric-player").forEach((a) => {
    a.style.fontFamily = t ? "var(--amll-lp-font-family)" : "";
  });
};
window.setRenderMode = (e) => Tt(`setRenderMode: ${e}`, "debug");
window.setLyricPlayerImplementation = (e) => Tt(`setLyricPlayerImplementation: ${e}`, "debug");
window.rebuildLyricsDom = (e) => (Tt(`rebuildLyricsDom: ${e}`, "debug"), Z.player?.calcLayout?.(), Z.player?.update?.(0), !0);
