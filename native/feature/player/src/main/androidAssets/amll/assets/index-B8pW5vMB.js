(function() {
  const e = document.createElement("link").relList;
  if (e && e.supports && e.supports("modulepreload")) return;
  for (const s of document.querySelectorAll('link[rel="modulepreload"]')) r(s);
  new MutationObserver((s) => {
    for (const n of s) if (n.type === "childList") for (const a of n.addedNodes) a.tagName === "LINK" && a.rel === "modulepreload" && r(a);
  }).observe(document, { childList: true, subtree: true });
  function t(s) {
    const n = {};
    return s.integrity && (n.integrity = s.integrity), s.referrerPolicy && (n.referrerPolicy = s.referrerPolicy), s.crossOrigin === "use-credentials" ? n.credentials = "include" : s.crossOrigin === "anonymous" ? n.credentials = "omit" : n.credentials = "same-origin", n;
  }
  function r(s) {
    if (s.ep) return;
    s.ep = true;
    const n = t(s);
    fetch(s.href, n);
  }
})();
const be = 1e-6, Bn = new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);
class oe extends Float32Array {
  static BYTE_LENGTH = 16 * Float32Array.BYTES_PER_ELEMENT;
  constructor(...e) {
    switch (e.length) {
      case 16:
        super(e);
        break;
      case 2:
        super(e[0], e[1], 16);
        break;
      case 1:
        const t = e[0];
        typeof t == "number" ? super([t, t, t, t, t, t, t, t, t, t, t, t, t, t, t, t]) : super(t, 0, 16);
        break;
      default:
        super(Bn);
        break;
    }
  }
  get str() {
    return oe.str(this);
  }
  copy(e) {
    return this.set(e), this;
  }
  identity() {
    return this.set(Bn), this;
  }
  multiply(e) {
    return oe.multiply(this, this, e);
  }
  mul(e) {
    return this;
  }
  transpose() {
    return oe.transpose(this, this);
  }
  invert() {
    return oe.invert(this, this);
  }
  translate(e) {
    return oe.translate(this, this, e);
  }
  rotate(e, t) {
    return oe.rotate(this, this, e, t);
  }
  scale(e) {
    return oe.scale(this, this, e);
  }
  rotateX(e) {
    return oe.rotateX(this, this, e);
  }
  rotateY(e) {
    return oe.rotateY(this, this, e);
  }
  rotateZ(e) {
    return oe.rotateZ(this, this, e);
  }
  perspectiveNO(e, t, r, s) {
    return oe.perspectiveNO(this, e, t, r, s);
  }
  perspectiveZO(e, t, r, s) {
    return oe.perspectiveZO(this, e, t, r, s);
  }
  orthoNO(e, t, r, s, n, a) {
    return oe.orthoNO(this, e, t, r, s, n, a);
  }
  orthoZO(e, t, r, s, n, a) {
    return oe.orthoZO(this, e, t, r, s, n, a);
  }
  static create() {
    return new oe();
  }
  static clone(e) {
    return new oe(e);
  }
  static copy(e, t) {
    return e[0] = t[0], e[1] = t[1], e[2] = t[2], e[3] = t[3], e[4] = t[4], e[5] = t[5], e[6] = t[6], e[7] = t[7], e[8] = t[8], e[9] = t[9], e[10] = t[10], e[11] = t[11], e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15], e;
  }
  static fromValues(...e) {
    return new oe(...e);
  }
  static set(e, ...t) {
    return e[0] = t[0], e[1] = t[1], e[2] = t[2], e[3] = t[3], e[4] = t[4], e[5] = t[5], e[6] = t[6], e[7] = t[7], e[8] = t[8], e[9] = t[9], e[10] = t[10], e[11] = t[11], e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15], e;
  }
  static identity(e) {
    return e[0] = 1, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = 1, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 1, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static transpose(e, t) {
    if (e === t) {
      const r = t[1], s = t[2], n = t[3], a = t[6], o = t[7], h = t[11];
      e[1] = t[4], e[2] = t[8], e[3] = t[12], e[4] = r, e[6] = t[9], e[7] = t[13], e[8] = s, e[9] = a, e[11] = t[14], e[12] = n, e[13] = o, e[14] = h;
    } else e[0] = t[0], e[1] = t[4], e[2] = t[8], e[3] = t[12], e[4] = t[1], e[5] = t[5], e[6] = t[9], e[7] = t[13], e[8] = t[2], e[9] = t[6], e[10] = t[10], e[11] = t[14], e[12] = t[3], e[13] = t[7], e[14] = t[11], e[15] = t[15];
    return e;
  }
  static invert(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[3], o = t[4], h = t[5], l = t[6], c = t[7], u = t[8], m = t[9], y = t[10], d = t[11], p = t[12], f = t[13], _ = t[14], T = t[15], I = r * h - s * o, k = r * l - n * o, g = r * c - a * o, M = s * l - n * h, v = s * c - a * h, O = n * c - a * l, E = u * f - m * p, N = u * _ - y * p, R = u * T - d * p, U = m * _ - y * f, H = m * T - d * f, G = y * T - d * _;
    let z = I * G - k * H + g * U + M * R - v * N + O * E;
    return z ? (z = 1 / z, e[0] = (h * G - l * H + c * U) * z, e[1] = (n * H - s * G - a * U) * z, e[2] = (f * O - _ * v + T * M) * z, e[3] = (y * v - m * O - d * M) * z, e[4] = (l * R - o * G - c * N) * z, e[5] = (r * G - n * R + a * N) * z, e[6] = (_ * g - p * O - T * k) * z, e[7] = (u * O - y * g + d * k) * z, e[8] = (o * H - h * R + c * E) * z, e[9] = (s * R - r * H - a * E) * z, e[10] = (p * v - f * g + T * I) * z, e[11] = (m * g - u * v - d * I) * z, e[12] = (h * N - o * U - l * E) * z, e[13] = (r * U - s * N + n * E) * z, e[14] = (f * k - p * M - _ * I) * z, e[15] = (u * M - m * k + y * I) * z, e) : null;
  }
  static adjoint(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[3], o = t[4], h = t[5], l = t[6], c = t[7], u = t[8], m = t[9], y = t[10], d = t[11], p = t[12], f = t[13], _ = t[14], T = t[15], I = r * h - s * o, k = r * l - n * o, g = r * c - a * o, M = s * l - n * h, v = s * c - a * h, O = n * c - a * l, E = u * f - m * p, N = u * _ - y * p, R = u * T - d * p, U = m * _ - y * f, H = m * T - d * f, G = y * T - d * _;
    return e[0] = h * G - l * H + c * U, e[1] = n * H - s * G - a * U, e[2] = f * O - _ * v + T * M, e[3] = y * v - m * O - d * M, e[4] = l * R - o * G - c * N, e[5] = r * G - n * R + a * N, e[6] = _ * g - p * O - T * k, e[7] = u * O - y * g + d * k, e[8] = o * H - h * R + c * E, e[9] = s * R - r * H - a * E, e[10] = p * v - f * g + T * I, e[11] = m * g - u * v - d * I, e[12] = h * N - o * U - l * E, e[13] = r * U - s * N + n * E, e[14] = f * k - p * M - _ * I, e[15] = u * M - m * k + y * I, e;
  }
  static determinant(e) {
    const t = e[0], r = e[1], s = e[2], n = e[3], a = e[4], o = e[5], h = e[6], l = e[7], c = e[8], u = e[9], m = e[10], y = e[11], d = e[12], p = e[13], f = e[14], _ = e[15], T = t * o - r * a, I = t * h - s * a, k = r * h - s * o, g = c * p - u * d, M = c * f - m * d, v = u * f - m * p, O = t * v - r * M + s * g, E = a * v - o * M + h * g, N = c * k - u * I + m * T, R = d * k - p * I + f * T;
    return l * O - n * E + _ * N - y * R;
  }
  static multiply(e, t, r) {
    const s = t[0], n = t[1], a = t[2], o = t[3], h = t[4], l = t[5], c = t[6], u = t[7], m = t[8], y = t[9], d = t[10], p = t[11], f = t[12], _ = t[13], T = t[14], I = t[15];
    let k = r[0], g = r[1], M = r[2], v = r[3];
    return e[0] = k * s + g * h + M * m + v * f, e[1] = k * n + g * l + M * y + v * _, e[2] = k * a + g * c + M * d + v * T, e[3] = k * o + g * u + M * p + v * I, k = r[4], g = r[5], M = r[6], v = r[7], e[4] = k * s + g * h + M * m + v * f, e[5] = k * n + g * l + M * y + v * _, e[6] = k * a + g * c + M * d + v * T, e[7] = k * o + g * u + M * p + v * I, k = r[8], g = r[9], M = r[10], v = r[11], e[8] = k * s + g * h + M * m + v * f, e[9] = k * n + g * l + M * y + v * _, e[10] = k * a + g * c + M * d + v * T, e[11] = k * o + g * u + M * p + v * I, k = r[12], g = r[13], M = r[14], v = r[15], e[12] = k * s + g * h + M * m + v * f, e[13] = k * n + g * l + M * y + v * _, e[14] = k * a + g * c + M * d + v * T, e[15] = k * o + g * u + M * p + v * I, e;
  }
  static mul(e, t, r) {
    return e;
  }
  static translate(e, t, r) {
    const s = r[0], n = r[1], a = r[2];
    if (t === e) e[12] = t[0] * s + t[4] * n + t[8] * a + t[12], e[13] = t[1] * s + t[5] * n + t[9] * a + t[13], e[14] = t[2] * s + t[6] * n + t[10] * a + t[14], e[15] = t[3] * s + t[7] * n + t[11] * a + t[15];
    else {
      const o = t[0], h = t[1], l = t[2], c = t[3], u = t[4], m = t[5], y = t[6], d = t[7], p = t[8], f = t[9], _ = t[10], T = t[11];
      e[0] = o, e[1] = h, e[2] = l, e[3] = c, e[4] = u, e[5] = m, e[6] = y, e[7] = d, e[8] = p, e[9] = f, e[10] = _, e[11] = T, e[12] = o * s + u * n + p * a + t[12], e[13] = h * s + m * n + f * a + t[13], e[14] = l * s + y * n + _ * a + t[14], e[15] = c * s + d * n + T * a + t[15];
    }
    return e;
  }
  static scale(e, t, r) {
    const s = r[0], n = r[1], a = r[2];
    return e[0] = t[0] * s, e[1] = t[1] * s, e[2] = t[2] * s, e[3] = t[3] * s, e[4] = t[4] * n, e[5] = t[5] * n, e[6] = t[6] * n, e[7] = t[7] * n, e[8] = t[8] * a, e[9] = t[9] * a, e[10] = t[10] * a, e[11] = t[11] * a, e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15], e;
  }
  static rotate(e, t, r, s) {
    let n = s[0], a = s[1], o = s[2], h = Math.sqrt(n * n + a * a + o * o);
    if (h < be) return null;
    h = 1 / h, n *= h, a *= h, o *= h;
    const l = Math.sin(r), c = Math.cos(r), u = 1 - c, m = t[0], y = t[1], d = t[2], p = t[3], f = t[4], _ = t[5], T = t[6], I = t[7], k = t[8], g = t[9], M = t[10], v = t[11], O = n * n * u + c, E = a * n * u + o * l, N = o * n * u - a * l, R = n * a * u - o * l, U = a * a * u + c, H = o * a * u + n * l, G = n * o * u + a * l, z = a * o * u - n * l, b = o * o * u + c;
    return e[0] = m * O + f * E + k * N, e[1] = y * O + _ * E + g * N, e[2] = d * O + T * E + M * N, e[3] = p * O + I * E + v * N, e[4] = m * R + f * U + k * H, e[5] = y * R + _ * U + g * H, e[6] = d * R + T * U + M * H, e[7] = p * R + I * U + v * H, e[8] = m * G + f * z + k * b, e[9] = y * G + _ * z + g * b, e[10] = d * G + T * z + M * b, e[11] = p * G + I * z + v * b, t !== e && (e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15]), e;
  }
  static rotateX(e, t, r) {
    let s = Math.sin(r), n = Math.cos(r), a = t[4], o = t[5], h = t[6], l = t[7], c = t[8], u = t[9], m = t[10], y = t[11];
    return t !== e && (e[0] = t[0], e[1] = t[1], e[2] = t[2], e[3] = t[3], e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15]), e[4] = a * n + c * s, e[5] = o * n + u * s, e[6] = h * n + m * s, e[7] = l * n + y * s, e[8] = c * n - a * s, e[9] = u * n - o * s, e[10] = m * n - h * s, e[11] = y * n - l * s, e;
  }
  static rotateY(e, t, r) {
    let s = Math.sin(r), n = Math.cos(r), a = t[0], o = t[1], h = t[2], l = t[3], c = t[8], u = t[9], m = t[10], y = t[11];
    return t !== e && (e[4] = t[4], e[5] = t[5], e[6] = t[6], e[7] = t[7], e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15]), e[0] = a * n - c * s, e[1] = o * n - u * s, e[2] = h * n - m * s, e[3] = l * n - y * s, e[8] = a * s + c * n, e[9] = o * s + u * n, e[10] = h * s + m * n, e[11] = l * s + y * n, e;
  }
  static rotateZ(e, t, r) {
    let s = Math.sin(r), n = Math.cos(r), a = t[0], o = t[1], h = t[2], l = t[3], c = t[4], u = t[5], m = t[6], y = t[7];
    return t !== e && (e[8] = t[8], e[9] = t[9], e[10] = t[10], e[11] = t[11], e[12] = t[12], e[13] = t[13], e[14] = t[14], e[15] = t[15]), e[0] = a * n + c * s, e[1] = o * n + u * s, e[2] = h * n + m * s, e[3] = l * n + y * s, e[4] = c * n - a * s, e[5] = u * n - o * s, e[6] = m * n - h * s, e[7] = y * n - l * s, e;
  }
  static fromTranslation(e, t) {
    return e[0] = 1, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = 1, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 1, e[11] = 0, e[12] = t[0], e[13] = t[1], e[14] = t[2], e[15] = 1, e;
  }
  static fromScaling(e, t) {
    return e[0] = t[0], e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = t[1], e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = t[2], e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static fromRotation(e, t, r) {
    let s = r[0], n = r[1], a = r[2], o = Math.sqrt(s * s + n * n + a * a);
    if (o < be) return null;
    o = 1 / o, s *= o, n *= o, a *= o;
    const h = Math.sin(t), l = Math.cos(t), c = 1 - l;
    return e[0] = s * s * c + l, e[1] = n * s * c + a * h, e[2] = a * s * c - n * h, e[3] = 0, e[4] = s * n * c - a * h, e[5] = n * n * c + l, e[6] = a * n * c + s * h, e[7] = 0, e[8] = s * a * c + n * h, e[9] = n * a * c - s * h, e[10] = a * a * c + l, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static fromXRotation(e, t) {
    let r = Math.sin(t), s = Math.cos(t);
    return e[0] = 1, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = s, e[6] = r, e[7] = 0, e[8] = 0, e[9] = -r, e[10] = s, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static fromYRotation(e, t) {
    let r = Math.sin(t), s = Math.cos(t);
    return e[0] = s, e[1] = 0, e[2] = -r, e[3] = 0, e[4] = 0, e[5] = 1, e[6] = 0, e[7] = 0, e[8] = r, e[9] = 0, e[10] = s, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static fromZRotation(e, t) {
    const r = Math.sin(t), s = Math.cos(t);
    return e[0] = s, e[1] = r, e[2] = 0, e[3] = 0, e[4] = -r, e[5] = s, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 1, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static fromRotationTranslation(e, t, r) {
    const s = t[0], n = t[1], a = t[2], o = t[3], h = s + s, l = n + n, c = a + a, u = s * h, m = s * l, y = s * c, d = n * l, p = n * c, f = a * c, _ = o * h, T = o * l, I = o * c;
    return e[0] = 1 - (d + f), e[1] = m + I, e[2] = y - T, e[3] = 0, e[4] = m - I, e[5] = 1 - (u + f), e[6] = p + _, e[7] = 0, e[8] = y + T, e[9] = p - _, e[10] = 1 - (u + d), e[11] = 0, e[12] = r[0], e[13] = r[1], e[14] = r[2], e[15] = 1, e;
  }
  static fromQuat2(e, t) {
    const r = -t[0], s = -t[1], n = -t[2], a = t[3], o = t[4], h = t[5], l = t[6], c = t[7];
    let u = r * r + s * s + n * n + a * a;
    return u > 0 ? (ke[0] = (o * a + c * r + h * n - l * s) * 2 / u, ke[1] = (h * a + c * s + l * r - o * n) * 2 / u, ke[2] = (l * a + c * n + o * s - h * r) * 2 / u) : (ke[0] = (o * a + c * r + h * n - l * s) * 2, ke[1] = (h * a + c * s + l * r - o * n) * 2, ke[2] = (l * a + c * n + o * s - h * r) * 2), oe.fromRotationTranslation(e, t, ke), e;
  }
  static normalFromMat4(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[3], o = t[4], h = t[5], l = t[6], c = t[7], u = t[8], m = t[9], y = t[10], d = t[11], p = t[12], f = t[13], _ = t[14], T = t[15], I = r * h - s * o, k = r * l - n * o, g = r * c - a * o, M = s * l - n * h, v = s * c - a * h, O = n * c - a * l, E = u * f - m * p, N = u * _ - y * p, R = u * T - d * p, U = m * _ - y * f, H = m * T - d * f, G = y * T - d * _;
    let z = I * G - k * H + g * U + M * R - v * N + O * E;
    return z ? (z = 1 / z, e[0] = (h * G - l * H + c * U) * z, e[1] = (l * R - o * G - c * N) * z, e[2] = (o * H - h * R + c * E) * z, e[3] = 0, e[4] = (n * H - s * G - a * U) * z, e[5] = (r * G - n * R + a * N) * z, e[6] = (s * R - r * H - a * E) * z, e[7] = 0, e[8] = (f * O - _ * v + T * M) * z, e[9] = (_ * g - p * O - T * k) * z, e[10] = (p * v - f * g + T * I) * z, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e) : null;
  }
  static normalFromMat4Fast(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[4], o = t[5], h = t[6], l = t[8], c = t[9], u = t[10];
    return e[0] = o * u - u * c, e[1] = h * l - l * u, e[2] = a * c - c * l, e[3] = 0, e[4] = c * n - u * s, e[5] = u * r - l * n, e[6] = l * s - c * r, e[7] = 0, e[8] = s * h - n * o, e[9] = n * a - r * h, e[10] = r * o - s * a, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static getTranslation(e, t) {
    return e[0] = t[12], e[1] = t[13], e[2] = t[14], e;
  }
  static getScaling(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[4], o = t[5], h = t[6], l = t[8], c = t[9], u = t[10];
    return e[0] = Math.sqrt(r * r + s * s + n * n), e[1] = Math.sqrt(a * a + o * o + h * h), e[2] = Math.sqrt(l * l + c * c + u * u), e;
  }
  static getRotation(e, t) {
    oe.getScaling(ke, t);
    const r = 1 / ke[0], s = 1 / ke[1], n = 1 / ke[2], a = t[0] * r, o = t[1] * s, h = t[2] * n, l = t[4] * r, c = t[5] * s, u = t[6] * n, m = t[8] * r, y = t[9] * s, d = t[10] * n, p = a + c + d;
    let f = 0;
    return p > 0 ? (f = Math.sqrt(p + 1) * 2, e[3] = 0.25 * f, e[0] = (u - y) / f, e[1] = (m - h) / f, e[2] = (o - l) / f) : a > c && a > d ? (f = Math.sqrt(1 + a - c - d) * 2, e[3] = (u - y) / f, e[0] = 0.25 * f, e[1] = (o + l) / f, e[2] = (m + h) / f) : c > d ? (f = Math.sqrt(1 + c - a - d) * 2, e[3] = (m - h) / f, e[0] = (o + l) / f, e[1] = 0.25 * f, e[2] = (u + y) / f) : (f = Math.sqrt(1 + d - a - c) * 2, e[3] = (o - l) / f, e[0] = (m + h) / f, e[1] = (u + y) / f, e[2] = 0.25 * f), e;
  }
  static decompose(e, t, r, s) {
    t[0] = s[12], t[1] = s[13], t[2] = s[14];
    const n = s[0], a = s[1], o = s[2], h = s[4], l = s[5], c = s[6], u = s[8], m = s[9], y = s[10];
    r[0] = Math.sqrt(n * n + a * a + o * o), r[1] = Math.sqrt(h * h + l * l + c * c), r[2] = Math.sqrt(u * u + m * m + y * y);
    const d = 1 / r[0], p = 1 / r[1], f = 1 / r[2], _ = n * d, T = a * p, I = o * f, k = h * d, g = l * p, M = c * f, v = u * d, O = m * p, E = y * f, N = _ + g + E;
    let R = 0;
    return N > 0 ? (R = Math.sqrt(N + 1) * 2, e[3] = 0.25 * R, e[0] = (M - O) / R, e[1] = (v - I) / R, e[2] = (T - k) / R) : _ > g && _ > E ? (R = Math.sqrt(1 + _ - g - E) * 2, e[3] = (M - O) / R, e[0] = 0.25 * R, e[1] = (T + k) / R, e[2] = (v + I) / R) : g > E ? (R = Math.sqrt(1 + g - _ - E) * 2, e[3] = (v - I) / R, e[0] = (T + k) / R, e[1] = 0.25 * R, e[2] = (M + O) / R) : (R = Math.sqrt(1 + E - _ - g) * 2, e[3] = (T - k) / R, e[0] = (v + I) / R, e[1] = (M + O) / R, e[2] = 0.25 * R), e;
  }
  static fromRotationTranslationScale(e, t, r, s) {
    const n = t[0], a = t[1], o = t[2], h = t[3], l = n + n, c = a + a, u = o + o, m = n * l, y = n * c, d = n * u, p = a * c, f = a * u, _ = o * u, T = h * l, I = h * c, k = h * u, g = s[0], M = s[1], v = s[2];
    return e[0] = (1 - (p + _)) * g, e[1] = (y + k) * g, e[2] = (d - I) * g, e[3] = 0, e[4] = (y - k) * M, e[5] = (1 - (m + _)) * M, e[6] = (f + T) * M, e[7] = 0, e[8] = (d + I) * v, e[9] = (f - T) * v, e[10] = (1 - (m + p)) * v, e[11] = 0, e[12] = r[0], e[13] = r[1], e[14] = r[2], e[15] = 1, e;
  }
  static fromRotationTranslationScaleOrigin(e, t, r, s, n) {
    const a = t[0], o = t[1], h = t[2], l = t[3], c = a + a, u = o + o, m = h + h, y = a * c, d = a * u, p = a * m, f = o * u, _ = o * m, T = h * m, I = l * c, k = l * u, g = l * m, M = s[0], v = s[1], O = s[2], E = n[0], N = n[1], R = n[2], U = (1 - (f + T)) * M, H = (d + g) * M, G = (p - k) * M, z = (d - g) * v, b = (1 - (y + T)) * v, w = (_ + I) * v, x = (p + k) * O, C = (_ - I) * O, S = (1 - (y + f)) * O;
    return e[0] = U, e[1] = H, e[2] = G, e[3] = 0, e[4] = z, e[5] = b, e[6] = w, e[7] = 0, e[8] = x, e[9] = C, e[10] = S, e[11] = 0, e[12] = r[0] + E - (U * E + z * N + x * R), e[13] = r[1] + N - (H * E + b * N + C * R), e[14] = r[2] + R - (G * E + w * N + S * R), e[15] = 1, e;
  }
  static fromQuat(e, t) {
    const r = t[0], s = t[1], n = t[2], a = t[3], o = r + r, h = s + s, l = n + n, c = r * o, u = s * o, m = s * h, y = n * o, d = n * h, p = n * l, f = a * o, _ = a * h, T = a * l;
    return e[0] = 1 - m - p, e[1] = u + T, e[2] = y - _, e[3] = 0, e[4] = u - T, e[5] = 1 - c - p, e[6] = d + f, e[7] = 0, e[8] = y + _, e[9] = d - f, e[10] = 1 - c - m, e[11] = 0, e[12] = 0, e[13] = 0, e[14] = 0, e[15] = 1, e;
  }
  static frustumNO(e, t, r, s, n, a, o = 1 / 0) {
    const h = 1 / (r - t), l = 1 / (n - s);
    if (e[0] = a * 2 * h, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = a * 2 * l, e[6] = 0, e[7] = 0, e[8] = (r + t) * h, e[9] = (n + s) * l, e[11] = -1, e[12] = 0, e[13] = 0, e[15] = 0, o != null && o !== 1 / 0) {
      const c = 1 / (a - o);
      e[10] = (o + a) * c, e[14] = 2 * o * a * c;
    } else e[10] = -1, e[14] = -2 * a;
    return e;
  }
  static frustum(e, t, r, s, n, a, o = 1 / 0) {
    return e;
  }
  static frustumZO(e, t, r, s, n, a, o = 1 / 0) {
    const h = 1 / (r - t), l = 1 / (n - s);
    if (e[0] = a * 2 * h, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = a * 2 * l, e[6] = 0, e[7] = 0, e[8] = (r + t) * h, e[9] = (n + s) * l, e[11] = -1, e[12] = 0, e[13] = 0, e[15] = 0, o != null && o !== 1 / 0) {
      const c = 1 / (a - o);
      e[10] = o * c, e[14] = o * a * c;
    } else e[10] = -1, e[14] = -a;
    return e;
  }
  static perspectiveNO(e, t, r, s, n = 1 / 0) {
    const a = 1 / Math.tan(t / 2);
    if (e[0] = a / r, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = a, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[11] = -1, e[12] = 0, e[13] = 0, e[15] = 0, n != null && n !== 1 / 0) {
      const o = 1 / (s - n);
      e[10] = (n + s) * o, e[14] = 2 * n * s * o;
    } else e[10] = -1, e[14] = -2 * s;
    return e;
  }
  static perspective(e, t, r, s, n = 1 / 0) {
    return e;
  }
  static perspectiveZO(e, t, r, s, n = 1 / 0) {
    const a = 1 / Math.tan(t / 2);
    if (e[0] = a / r, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = a, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[11] = -1, e[12] = 0, e[13] = 0, e[15] = 0, n != null && n !== 1 / 0) {
      const o = 1 / (s - n);
      e[10] = n * o, e[14] = n * s * o;
    } else e[10] = -1, e[14] = -s;
    return e;
  }
  static perspectiveFromFieldOfView(e, t, r, s) {
    const n = Math.tan(t.upDegrees * Math.PI / 180), a = Math.tan(t.downDegrees * Math.PI / 180), o = Math.tan(t.leftDegrees * Math.PI / 180), h = Math.tan(t.rightDegrees * Math.PI / 180), l = 2 / (o + h), c = 2 / (n + a);
    return e[0] = l, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = c, e[6] = 0, e[7] = 0, e[8] = -((o - h) * l * 0.5), e[9] = (n - a) * c * 0.5, e[10] = s / (r - s), e[11] = -1, e[12] = 0, e[13] = 0, e[14] = s * r / (r - s), e[15] = 0, e;
  }
  static orthoNO(e, t, r, s, n, a, o) {
    const h = 1 / (t - r), l = 1 / (s - n), c = 1 / (a - o);
    return e[0] = -2 * h, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = -2 * l, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = 2 * c, e[11] = 0, e[12] = (t + r) * h, e[13] = (n + s) * l, e[14] = (o + a) * c, e[15] = 1, e;
  }
  static ortho(e, t, r, s, n, a, o) {
    return e;
  }
  static orthoZO(e, t, r, s, n, a, o) {
    const h = 1 / (t - r), l = 1 / (s - n), c = 1 / (a - o);
    return e[0] = -2 * h, e[1] = 0, e[2] = 0, e[3] = 0, e[4] = 0, e[5] = -2 * l, e[6] = 0, e[7] = 0, e[8] = 0, e[9] = 0, e[10] = c, e[11] = 0, e[12] = (t + r) * h, e[13] = (n + s) * l, e[14] = a * c, e[15] = 1, e;
  }
  static lookAt(e, t, r, s) {
    const n = t[0], a = t[1], o = t[2], h = s[0], l = s[1], c = s[2], u = r[0], m = r[1], y = r[2];
    if (Math.abs(n - u) < be && Math.abs(a - m) < be && Math.abs(o - y) < be) return oe.identity(e);
    let d = n - u, p = a - m, f = o - y, _ = 1 / Math.sqrt(d * d + p * p + f * f);
    d *= _, p *= _, f *= _;
    let T = l * f - c * p, I = c * d - h * f, k = h * p - l * d;
    _ = Math.sqrt(T * T + I * I + k * k), _ ? (_ = 1 / _, T *= _, I *= _, k *= _) : (T = 0, I = 0, k = 0);
    let g = p * k - f * I, M = f * T - d * k, v = d * I - p * T;
    return _ = Math.sqrt(g * g + M * M + v * v), _ ? (_ = 1 / _, g *= _, M *= _, v *= _) : (g = 0, M = 0, v = 0), e[0] = T, e[1] = g, e[2] = d, e[3] = 0, e[4] = I, e[5] = M, e[6] = p, e[7] = 0, e[8] = k, e[9] = v, e[10] = f, e[11] = 0, e[12] = -(T * n + I * a + k * o), e[13] = -(g * n + M * a + v * o), e[14] = -(d * n + p * a + f * o), e[15] = 1, e;
  }
  static targetTo(e, t, r, s) {
    const n = t[0], a = t[1], o = t[2], h = s[0], l = s[1], c = s[2];
    let u = n - r[0], m = a - r[1], y = o - r[2], d = u * u + m * m + y * y;
    d > 0 && (d = 1 / Math.sqrt(d), u *= d, m *= d, y *= d);
    let p = l * y - c * m, f = c * u - h * y, _ = h * m - l * u;
    return d = p * p + f * f + _ * _, d > 0 && (d = 1 / Math.sqrt(d), p *= d, f *= d, _ *= d), e[0] = p, e[1] = f, e[2] = _, e[3] = 0, e[4] = m * _ - y * f, e[5] = y * p - u * _, e[6] = u * f - m * p, e[7] = 0, e[8] = u, e[9] = m, e[10] = y, e[11] = 0, e[12] = n, e[13] = a, e[14] = o, e[15] = 1, e;
  }
  static frob(e) {
    return Math.sqrt(e[0] * e[0] + e[1] * e[1] + e[2] * e[2] + e[3] * e[3] + e[4] * e[4] + e[5] * e[5] + e[6] * e[6] + e[7] * e[7] + e[8] * e[8] + e[9] * e[9] + e[10] * e[10] + e[11] * e[11] + e[12] * e[12] + e[13] * e[13] + e[14] * e[14] + e[15] * e[15]);
  }
  static add(e, t, r) {
    return e[0] = t[0] + r[0], e[1] = t[1] + r[1], e[2] = t[2] + r[2], e[3] = t[3] + r[3], e[4] = t[4] + r[4], e[5] = t[5] + r[5], e[6] = t[6] + r[6], e[7] = t[7] + r[7], e[8] = t[8] + r[8], e[9] = t[9] + r[9], e[10] = t[10] + r[10], e[11] = t[11] + r[11], e[12] = t[12] + r[12], e[13] = t[13] + r[13], e[14] = t[14] + r[14], e[15] = t[15] + r[15], e;
  }
  static subtract(e, t, r) {
    return e[0] = t[0] - r[0], e[1] = t[1] - r[1], e[2] = t[2] - r[2], e[3] = t[3] - r[3], e[4] = t[4] - r[4], e[5] = t[5] - r[5], e[6] = t[6] - r[6], e[7] = t[7] - r[7], e[8] = t[8] - r[8], e[9] = t[9] - r[9], e[10] = t[10] - r[10], e[11] = t[11] - r[11], e[12] = t[12] - r[12], e[13] = t[13] - r[13], e[14] = t[14] - r[14], e[15] = t[15] - r[15], e;
  }
  static sub(e, t, r) {
    return e;
  }
  static multiplyScalar(e, t, r) {
    return e[0] = t[0] * r, e[1] = t[1] * r, e[2] = t[2] * r, e[3] = t[3] * r, e[4] = t[4] * r, e[5] = t[5] * r, e[6] = t[6] * r, e[7] = t[7] * r, e[8] = t[8] * r, e[9] = t[9] * r, e[10] = t[10] * r, e[11] = t[11] * r, e[12] = t[12] * r, e[13] = t[13] * r, e[14] = t[14] * r, e[15] = t[15] * r, e;
  }
  static multiplyScalarAndAdd(e, t, r, s) {
    return e[0] = t[0] + r[0] * s, e[1] = t[1] + r[1] * s, e[2] = t[2] + r[2] * s, e[3] = t[3] + r[3] * s, e[4] = t[4] + r[4] * s, e[5] = t[5] + r[5] * s, e[6] = t[6] + r[6] * s, e[7] = t[7] + r[7] * s, e[8] = t[8] + r[8] * s, e[9] = t[9] + r[9] * s, e[10] = t[10] + r[10] * s, e[11] = t[11] + r[11] * s, e[12] = t[12] + r[12] * s, e[13] = t[13] + r[13] * s, e[14] = t[14] + r[14] * s, e[15] = t[15] + r[15] * s, e;
  }
  static exactEquals(e, t) {
    return e[0] === t[0] && e[1] === t[1] && e[2] === t[2] && e[3] === t[3] && e[4] === t[4] && e[5] === t[5] && e[6] === t[6] && e[7] === t[7] && e[8] === t[8] && e[9] === t[9] && e[10] === t[10] && e[11] === t[11] && e[12] === t[12] && e[13] === t[13] && e[14] === t[14] && e[15] === t[15];
  }
  static equals(e, t) {
    const r = e[0], s = e[1], n = e[2], a = e[3], o = e[4], h = e[5], l = e[6], c = e[7], u = e[8], m = e[9], y = e[10], d = e[11], p = e[12], f = e[13], _ = e[14], T = e[15], I = t[0], k = t[1], g = t[2], M = t[3], v = t[4], O = t[5], E = t[6], N = t[7], R = t[8], U = t[9], H = t[10], G = t[11], z = t[12], b = t[13], w = t[14], x = t[15];
    return Math.abs(r - I) <= be * Math.max(1, Math.abs(r), Math.abs(I)) && Math.abs(s - k) <= be * Math.max(1, Math.abs(s), Math.abs(k)) && Math.abs(n - g) <= be * Math.max(1, Math.abs(n), Math.abs(g)) && Math.abs(a - M) <= be * Math.max(1, Math.abs(a), Math.abs(M)) && Math.abs(o - v) <= be * Math.max(1, Math.abs(o), Math.abs(v)) && Math.abs(h - O) <= be * Math.max(1, Math.abs(h), Math.abs(O)) && Math.abs(l - E) <= be * Math.max(1, Math.abs(l), Math.abs(E)) && Math.abs(c - N) <= be * Math.max(1, Math.abs(c), Math.abs(N)) && Math.abs(u - R) <= be * Math.max(1, Math.abs(u), Math.abs(R)) && Math.abs(m - U) <= be * Math.max(1, Math.abs(m), Math.abs(U)) && Math.abs(y - H) <= be * Math.max(1, Math.abs(y), Math.abs(H)) && Math.abs(d - G) <= be * Math.max(1, Math.abs(d), Math.abs(G)) && Math.abs(p - z) <= be * Math.max(1, Math.abs(p), Math.abs(z)) && Math.abs(f - b) <= be * Math.max(1, Math.abs(f), Math.abs(b)) && Math.abs(_ - w) <= be * Math.max(1, Math.abs(_), Math.abs(w)) && Math.abs(T - x) <= be * Math.max(1, Math.abs(T), Math.abs(x));
  }
  static str(e) {
    return `Mat4(${e.join(", ")})`;
  }
}
const ke = new Float32Array(3);
oe.prototype.mul = oe.prototype.multiply;
oe.sub = oe.subtract;
oe.mul = oe.multiply;
oe.frustum = oe.frustumNO;
oe.perspective = oe.perspectiveNO;
oe.ortho = oe.orthoNO;
var Lt = ((i) => (i[i.WEBGL_LEGACY = 0] = "WEBGL_LEGACY", i[i.WEBGL = 1] = "WEBGL", i[i.WEBGL2 = 2] = "WEBGL2", i))(Lt || {}), Mo = ((i) => (i[i.UNKNOWN = 0] = "UNKNOWN", i[i.WEBGL = 1] = "WEBGL", i[i.CANVAS = 2] = "CANVAS", i))(Mo || {}), Bs = ((i) => (i[i.COLOR = 16384] = "COLOR", i[i.DEPTH = 256] = "DEPTH", i[i.STENCIL = 1024] = "STENCIL", i))(Bs || {}), te = ((i) => (i[i.NORMAL = 0] = "NORMAL", i[i.ADD = 1] = "ADD", i[i.MULTIPLY = 2] = "MULTIPLY", i[i.SCREEN = 3] = "SCREEN", i[i.OVERLAY = 4] = "OVERLAY", i[i.DARKEN = 5] = "DARKEN", i[i.LIGHTEN = 6] = "LIGHTEN", i[i.COLOR_DODGE = 7] = "COLOR_DODGE", i[i.COLOR_BURN = 8] = "COLOR_BURN", i[i.HARD_LIGHT = 9] = "HARD_LIGHT", i[i.SOFT_LIGHT = 10] = "SOFT_LIGHT", i[i.DIFFERENCE = 11] = "DIFFERENCE", i[i.EXCLUSION = 12] = "EXCLUSION", i[i.HUE = 13] = "HUE", i[i.SATURATION = 14] = "SATURATION", i[i.COLOR = 15] = "COLOR", i[i.LUMINOSITY = 16] = "LUMINOSITY", i[i.NORMAL_NPM = 17] = "NORMAL_NPM", i[i.ADD_NPM = 18] = "ADD_NPM", i[i.SCREEN_NPM = 19] = "SCREEN_NPM", i[i.NONE = 20] = "NONE", i[i.SRC_OVER = 0] = "SRC_OVER", i[i.SRC_IN = 21] = "SRC_IN", i[i.SRC_OUT = 22] = "SRC_OUT", i[i.SRC_ATOP = 23] = "SRC_ATOP", i[i.DST_OVER = 24] = "DST_OVER", i[i.DST_IN = 25] = "DST_IN", i[i.DST_OUT = 26] = "DST_OUT", i[i.DST_ATOP = 27] = "DST_ATOP", i[i.ERASE = 26] = "ERASE", i[i.SUBTRACT = 28] = "SUBTRACT", i[i.XOR = 29] = "XOR", i))(te || {}), Yr = ((i) => (i[i.POINTS = 0] = "POINTS", i[i.LINES = 1] = "LINES", i[i.LINE_LOOP = 2] = "LINE_LOOP", i[i.LINE_STRIP = 3] = "LINE_STRIP", i[i.TRIANGLES = 4] = "TRIANGLES", i[i.TRIANGLE_STRIP = 5] = "TRIANGLE_STRIP", i[i.TRIANGLE_FAN = 6] = "TRIANGLE_FAN", i))(Yr || {}), W = ((i) => (i[i.RGBA = 6408] = "RGBA", i[i.RGB = 6407] = "RGB", i[i.RG = 33319] = "RG", i[i.RED = 6403] = "RED", i[i.RGBA_INTEGER = 36249] = "RGBA_INTEGER", i[i.RGB_INTEGER = 36248] = "RGB_INTEGER", i[i.RG_INTEGER = 33320] = "RG_INTEGER", i[i.RED_INTEGER = 36244] = "RED_INTEGER", i[i.ALPHA = 6406] = "ALPHA", i[i.LUMINANCE = 6409] = "LUMINANCE", i[i.LUMINANCE_ALPHA = 6410] = "LUMINANCE_ALPHA", i[i.DEPTH_COMPONENT = 6402] = "DEPTH_COMPONENT", i[i.DEPTH_STENCIL = 34041] = "DEPTH_STENCIL", i))(W || {}), jt = ((i) => (i[i.TEXTURE_2D = 3553] = "TEXTURE_2D", i[i.TEXTURE_CUBE_MAP = 34067] = "TEXTURE_CUBE_MAP", i[i.TEXTURE_2D_ARRAY = 35866] = "TEXTURE_2D_ARRAY", i[i.TEXTURE_CUBE_MAP_POSITIVE_X = 34069] = "TEXTURE_CUBE_MAP_POSITIVE_X", i[i.TEXTURE_CUBE_MAP_NEGATIVE_X = 34070] = "TEXTURE_CUBE_MAP_NEGATIVE_X", i[i.TEXTURE_CUBE_MAP_POSITIVE_Y = 34071] = "TEXTURE_CUBE_MAP_POSITIVE_Y", i[i.TEXTURE_CUBE_MAP_NEGATIVE_Y = 34072] = "TEXTURE_CUBE_MAP_NEGATIVE_Y", i[i.TEXTURE_CUBE_MAP_POSITIVE_Z = 34073] = "TEXTURE_CUBE_MAP_POSITIVE_Z", i[i.TEXTURE_CUBE_MAP_NEGATIVE_Z = 34074] = "TEXTURE_CUBE_MAP_NEGATIVE_Z", i))(jt || {}), se = ((i) => (i[i.UNSIGNED_BYTE = 5121] = "UNSIGNED_BYTE", i[i.UNSIGNED_SHORT = 5123] = "UNSIGNED_SHORT", i[i.UNSIGNED_SHORT_5_6_5 = 33635] = "UNSIGNED_SHORT_5_6_5", i[i.UNSIGNED_SHORT_4_4_4_4 = 32819] = "UNSIGNED_SHORT_4_4_4_4", i[i.UNSIGNED_SHORT_5_5_5_1 = 32820] = "UNSIGNED_SHORT_5_5_5_1", i[i.UNSIGNED_INT = 5125] = "UNSIGNED_INT", i[i.UNSIGNED_INT_10F_11F_11F_REV = 35899] = "UNSIGNED_INT_10F_11F_11F_REV", i[i.UNSIGNED_INT_2_10_10_10_REV = 33640] = "UNSIGNED_INT_2_10_10_10_REV", i[i.UNSIGNED_INT_24_8 = 34042] = "UNSIGNED_INT_24_8", i[i.UNSIGNED_INT_5_9_9_9_REV = 35902] = "UNSIGNED_INT_5_9_9_9_REV", i[i.BYTE = 5120] = "BYTE", i[i.SHORT = 5122] = "SHORT", i[i.INT = 5124] = "INT", i[i.FLOAT = 5126] = "FLOAT", i[i.FLOAT_32_UNSIGNED_INT_24_8_REV = 36269] = "FLOAT_32_UNSIGNED_INT_24_8_REV", i[i.HALF_FLOAT = 36193] = "HALF_FLOAT", i))(se || {}), X = ((i) => (i[i.FLOAT = 0] = "FLOAT", i[i.INT = 1] = "INT", i[i.UINT = 2] = "UINT", i))(X || {}), Ze = ((i) => (i[i.NEAREST = 0] = "NEAREST", i[i.LINEAR = 1] = "LINEAR", i))(Ze || {}), on = ((i) => (i[i.CLAMP = 33071] = "CLAMP", i[i.REPEAT = 10497] = "REPEAT", i[i.MIRRORED_REPEAT = 33648] = "MIRRORED_REPEAT", i))(on || {}), It = ((i) => (i[i.OFF = 0] = "OFF", i[i.POW2 = 1] = "POW2", i[i.ON = 2] = "ON", i[i.ON_MANUAL = 3] = "ON_MANUAL", i))(It || {}), Pt = ((i) => (i[i.NPM = 0] = "NPM", i[i.UNPACK = 1] = "UNPACK", i[i.PMA = 2] = "PMA", i[i.NO_PREMULTIPLIED_ALPHA = 0] = "NO_PREMULTIPLIED_ALPHA", i[i.PREMULTIPLY_ON_UPLOAD = 1] = "PREMULTIPLY_ON_UPLOAD", i[i.PREMULTIPLIED_ALPHA = 2] = "PREMULTIPLIED_ALPHA", i))(Pt || {}), He = ((i) => (i[i.NO = 0] = "NO", i[i.YES = 1] = "YES", i[i.AUTO = 2] = "AUTO", i[i.BLEND = 0] = "BLEND", i[i.CLEAR = 1] = "CLEAR", i[i.BLIT = 2] = "BLIT", i))(He || {}), hn = ((i) => (i[i.AUTO = 0] = "AUTO", i[i.MANUAL = 1] = "MANUAL", i))(hn || {}), Pe = ((i) => (i.LOW = "lowp", i.MEDIUM = "mediump", i.HIGH = "highp", i))(Pe || {}), Ee = ((i) => (i[i.NONE = 0] = "NONE", i[i.SCISSOR = 1] = "SCISSOR", i[i.STENCIL = 2] = "STENCIL", i[i.SPRITE = 3] = "SPRITE", i[i.COLOR = 4] = "COLOR", i))(Ee || {}), _e = ((i) => (i[i.NONE = 0] = "NONE", i[i.LOW = 2] = "LOW", i[i.MEDIUM = 4] = "MEDIUM", i[i.HIGH = 8] = "HIGH", i))(_e || {}), We = ((i) => (i[i.ELEMENT_ARRAY_BUFFER = 34963] = "ELEMENT_ARRAY_BUFFER", i[i.ARRAY_BUFFER = 34962] = "ARRAY_BUFFER", i[i.UNIFORM_BUFFER = 35345] = "UNIFORM_BUFFER", i))(We || {});
const sl = { createCanvas: (i, e) => {
  const t = document.createElement("canvas");
  return t.width = i, t.height = e, t;
}, getCanvasRenderingContext2D: () => CanvasRenderingContext2D, getWebGLRenderingContext: () => WebGLRenderingContext, getNavigator: () => navigator, getBaseUrl: () => document.baseURI ?? window.location.href, getFontFaceSet: () => document.fonts, fetch: (i, e) => fetch(i, e), parseXML: (i) => new DOMParser().parseFromString(i, "text/xml") }, he = { ADAPTER: sl, RESOLUTION: 1, CREATE_IMAGE_BITMAP: false, ROUND_PIXELS: false };
var Ei = /iPhone/i, kn = /iPod/i, Un = /iPad/i, Dn = /\biOS-universal(?:.+)Mac\b/i, wi = /\bAndroid(?:.+)Mobile\b/i, Gn = /Android/i, Dt = /(?:SD4930UR|\bSilk(?:.+)Mobile\b)/i, Ir = /Silk/i, je = /Windows Phone/i, zn = /\bWindows(?:.+)ARM\b/i, $n = /BlackBerry/i, Hn = /BB10/i, Vn = /Opera Mini/i, Wn = /\b(CriOS|Chrome)(?:.+)Mobile/i, Xn = /Mobile(?:.+)Firefox\b/i, qn = function(i) {
  return typeof i < "u" && i.platform === "MacIntel" && typeof i.maxTouchPoints == "number" && i.maxTouchPoints > 1 && typeof MSStream > "u";
};
function nl(i) {
  return function(e) {
    return e.test(i);
  };
}
function jn(i) {
  var e = { userAgent: "", platform: "", maxTouchPoints: 0 };
  !i && typeof navigator < "u" ? e = { userAgent: navigator.userAgent, platform: navigator.platform, maxTouchPoints: navigator.maxTouchPoints || 0 } : typeof i == "string" ? e.userAgent = i : i && i.userAgent && (e = { userAgent: i.userAgent, platform: i.platform, maxTouchPoints: i.maxTouchPoints || 0 });
  var t = e.userAgent, r = t.split("[FBAN");
  typeof r[1] < "u" && (t = r[0]), r = t.split("Twitter"), typeof r[1] < "u" && (t = r[0]);
  var s = nl(t), n = { apple: { phone: s(Ei) && !s(je), ipod: s(kn), tablet: !s(Ei) && (s(Un) || qn(e)) && !s(je), universal: s(Dn), device: (s(Ei) || s(kn) || s(Un) || s(Dn) || qn(e)) && !s(je) }, amazon: { phone: s(Dt), tablet: !s(Dt) && s(Ir), device: s(Dt) || s(Ir) }, android: { phone: !s(je) && s(Dt) || !s(je) && s(wi), tablet: !s(je) && !s(Dt) && !s(wi) && (s(Ir) || s(Gn)), device: !s(je) && (s(Dt) || s(Ir) || s(wi) || s(Gn)) || s(/\bokhttp\b/i) }, windows: { phone: s(je), tablet: s(zn), device: s(je) || s(zn) }, other: { blackberry: s($n), blackberry10: s(Hn), opera: s(Vn), firefox: s(Xn), chrome: s(Wn), device: s($n) || s(Hn) || s(Vn) || s(Xn) || s(Wn) }, any: false, phone: false, tablet: false };
  return n.any = n.apple.device || n.android.device || n.windows.device || n.other.device, n.phone = n.apple.phone || n.android.phone || n.windows.phone, n.tablet = n.apple.tablet || n.android.tablet || n.windows.tablet, n;
}
const al = jn.default ?? jn, Vt = al(globalThis.navigator);
he.RETINA_PREFIX = /@([0-9\.]+)x/;
he.FAIL_IF_MAJOR_PERFORMANCE_CAVEAT = false;
var Kr = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : {};
function ol(i) {
  return i && i.__esModule && Object.prototype.hasOwnProperty.call(i, "default") ? i.default : i;
}
function hl(i) {
  if (Object.prototype.hasOwnProperty.call(i, "__esModule")) return i;
  var e = i.default;
  if (typeof e == "function") {
    var t = function r() {
      var s = false;
      try {
        s = this instanceof r;
      } catch {
      }
      return s ? Reflect.construct(e, arguments, this.constructor) : e.apply(this, arguments);
    };
    t.prototype = e.prototype;
  } else t = {};
  return Object.defineProperty(t, "__esModule", { value: true }), Object.keys(i).forEach(function(r) {
    var s = Object.getOwnPropertyDescriptor(i, r);
    Object.defineProperty(t, r, s.get ? s : { enumerable: true, get: function() {
      return i[r];
    } });
  }), t;
}
var Si = { exports: {} }, Yn;
function ll() {
  return Yn || (Yn = 1, (function(i) {
    var e = Object.prototype.hasOwnProperty, t = "~";
    function r() {
    }
    Object.create && (r.prototype = /* @__PURE__ */ Object.create(null), new r().__proto__ || (t = false));
    function s(h, l, c) {
      this.fn = h, this.context = l, this.once = c || false;
    }
    function n(h, l, c, u, m) {
      if (typeof c != "function") throw new TypeError("The listener must be a function");
      var y = new s(c, u || h, m), d = t ? t + l : l;
      return h._events[d] ? h._events[d].fn ? h._events[d] = [h._events[d], y] : h._events[d].push(y) : (h._events[d] = y, h._eventsCount++), h;
    }
    function a(h, l) {
      --h._eventsCount === 0 ? h._events = new r() : delete h._events[l];
    }
    function o() {
      this._events = new r(), this._eventsCount = 0;
    }
    o.prototype.eventNames = function() {
      var l = [], c, u;
      if (this._eventsCount === 0) return l;
      for (u in c = this._events) e.call(c, u) && l.push(t ? u.slice(1) : u);
      return Object.getOwnPropertySymbols ? l.concat(Object.getOwnPropertySymbols(c)) : l;
    }, o.prototype.listeners = function(l) {
      var c = t ? t + l : l, u = this._events[c];
      if (!u) return [];
      if (u.fn) return [u.fn];
      for (var m = 0, y = u.length, d = new Array(y); m < y; m++) d[m] = u[m].fn;
      return d;
    }, o.prototype.listenerCount = function(l) {
      var c = t ? t + l : l, u = this._events[c];
      return u ? u.fn ? 1 : u.length : 0;
    }, o.prototype.emit = function(l, c, u, m, y, d) {
      var p = t ? t + l : l;
      if (!this._events[p]) return false;
      var f = this._events[p], _ = arguments.length, T, I;
      if (f.fn) {
        switch (f.once && this.removeListener(l, f.fn, void 0, true), _) {
          case 1:
            return f.fn.call(f.context), true;
          case 2:
            return f.fn.call(f.context, c), true;
          case 3:
            return f.fn.call(f.context, c, u), true;
          case 4:
            return f.fn.call(f.context, c, u, m), true;
          case 5:
            return f.fn.call(f.context, c, u, m, y), true;
          case 6:
            return f.fn.call(f.context, c, u, m, y, d), true;
        }
        for (I = 1, T = new Array(_ - 1); I < _; I++) T[I - 1] = arguments[I];
        f.fn.apply(f.context, T);
      } else {
        var k = f.length, g;
        for (I = 0; I < k; I++) switch (f[I].once && this.removeListener(l, f[I].fn, void 0, true), _) {
          case 1:
            f[I].fn.call(f[I].context);
            break;
          case 2:
            f[I].fn.call(f[I].context, c);
            break;
          case 3:
            f[I].fn.call(f[I].context, c, u);
            break;
          case 4:
            f[I].fn.call(f[I].context, c, u, m);
            break;
          default:
            if (!T) for (g = 1, T = new Array(_ - 1); g < _; g++) T[g - 1] = arguments[g];
            f[I].fn.apply(f[I].context, T);
        }
      }
      return true;
    }, o.prototype.on = function(l, c, u) {
      return n(this, l, c, u, false);
    }, o.prototype.once = function(l, c, u) {
      return n(this, l, c, u, true);
    }, o.prototype.removeListener = function(l, c, u, m) {
      var y = t ? t + l : l;
      if (!this._events[y]) return this;
      if (!c) return a(this, y), this;
      var d = this._events[y];
      if (d.fn) d.fn === c && (!m || d.once) && (!u || d.context === u) && a(this, y);
      else {
        for (var p = 0, f = [], _ = d.length; p < _; p++) (d[p].fn !== c || m && !d[p].once || u && d[p].context !== u) && f.push(d[p]);
        f.length ? this._events[y] = f.length === 1 ? f[0] : f : a(this, y);
      }
      return this;
    }, o.prototype.removeAllListeners = function(l) {
      var c;
      return l ? (c = t ? t + l : l, this._events[c] && a(this, c)) : (this._events = new r(), this._eventsCount = 0), this;
    }, o.prototype.off = o.prototype.removeListener, o.prototype.addListener = o.prototype.on, o.prefixed = t, o.EventEmitter = o, i.exports = o;
  })(Si)), Si.exports;
}
var cl = ll();
const oi = ol(cl);
var Rr = { exports: {} }, Kn;
function ul() {
  if (Kn) return Rr.exports;
  Kn = 1, Rr.exports = i, Rr.exports.default = i;
  function i(b, w, x) {
    x = x || 2;
    var C = w && w.length, S = C ? w[0] * x : b.length, A = e(b, 0, S, x, true), L = [];
    if (!A || A.next === A.prev) return L;
    var F, D, $, q, j, K, J;
    if (C && (A = h(b, w, A, x)), b.length > 80 * x) {
      F = $ = b[0], D = q = b[1];
      for (var V = x; V < S; V += x) j = b[V], K = b[V + 1], j < F && (F = j), K < D && (D = K), j > $ && ($ = j), K > q && (q = K);
      J = Math.max($ - F, q - D), J = J !== 0 ? 32767 / J : 0;
    }
    return r(A, L, x, F, D, J, 0), L;
  }
  function e(b, w, x, C, S) {
    var A, L;
    if (S === z(b, w, x, C) > 0) for (A = w; A < x; A += C) L = U(A, b[A], b[A + 1], L);
    else for (A = x - C; A >= w; A -= C) L = U(A, b[A], b[A + 1], L);
    return L && k(L, L.next) && (H(L), L = L.next), L;
  }
  function t(b, w) {
    if (!b) return b;
    w || (w = b);
    var x = b, C;
    do
      if (C = false, !x.steiner && (k(x, x.next) || I(x.prev, x, x.next) === 0)) {
        if (H(x), x = w = x.prev, x === x.next) break;
        C = true;
      } else x = x.next;
    while (C || x !== w);
    return w;
  }
  function r(b, w, x, C, S, A, L) {
    if (b) {
      !L && A && y(b, C, S, A);
      for (var F = b, D, $; b.prev !== b.next; ) {
        if (D = b.prev, $ = b.next, A ? n(b, C, S, A) : s(b)) {
          w.push(D.i / x | 0), w.push(b.i / x | 0), w.push($.i / x | 0), H(b), b = $.next, F = $.next;
          continue;
        }
        if (b = $, b === F) {
          L ? L === 1 ? (b = a(t(b), w, x), r(b, w, x, C, S, A, 2)) : L === 2 && o(b, w, x, C, S, A) : r(t(b), w, x, C, S, A, 1);
          break;
        }
      }
    }
  }
  function s(b) {
    var w = b.prev, x = b, C = b.next;
    if (I(w, x, C) >= 0) return false;
    for (var S = w.x, A = x.x, L = C.x, F = w.y, D = x.y, $ = C.y, q = S < A ? S < L ? S : L : A < L ? A : L, j = F < D ? F < $ ? F : $ : D < $ ? D : $, K = S > A ? S > L ? S : L : A > L ? A : L, J = F > D ? F > $ ? F : $ : D > $ ? D : $, V = C.next; V !== w; ) {
      if (V.x >= q && V.x <= K && V.y >= j && V.y <= J && _(S, F, A, D, L, $, V.x, V.y) && I(V.prev, V, V.next) >= 0) return false;
      V = V.next;
    }
    return true;
  }
  function n(b, w, x, C) {
    var S = b.prev, A = b, L = b.next;
    if (I(S, A, L) >= 0) return false;
    for (var F = S.x, D = A.x, $ = L.x, q = S.y, j = A.y, K = L.y, J = F < D ? F < $ ? F : $ : D < $ ? D : $, V = q < j ? q < K ? q : K : j < K ? j : K, Q = F > D ? F > $ ? F : $ : D > $ ? D : $, ee = q > j ? q > K ? q : K : j > K ? j : K, ae = p(J, V, w, x, C), le = p(Q, ee, w, x, C), Y = b.prevZ, Z = b.nextZ; Y && Y.z >= ae && Z && Z.z <= le; ) {
      if (Y.x >= J && Y.x <= Q && Y.y >= V && Y.y <= ee && Y !== S && Y !== L && _(F, q, D, j, $, K, Y.x, Y.y) && I(Y.prev, Y, Y.next) >= 0 || (Y = Y.prevZ, Z.x >= J && Z.x <= Q && Z.y >= V && Z.y <= ee && Z !== S && Z !== L && _(F, q, D, j, $, K, Z.x, Z.y) && I(Z.prev, Z, Z.next) >= 0)) return false;
      Z = Z.nextZ;
    }
    for (; Y && Y.z >= ae; ) {
      if (Y.x >= J && Y.x <= Q && Y.y >= V && Y.y <= ee && Y !== S && Y !== L && _(F, q, D, j, $, K, Y.x, Y.y) && I(Y.prev, Y, Y.next) >= 0) return false;
      Y = Y.prevZ;
    }
    for (; Z && Z.z <= le; ) {
      if (Z.x >= J && Z.x <= Q && Z.y >= V && Z.y <= ee && Z !== S && Z !== L && _(F, q, D, j, $, K, Z.x, Z.y) && I(Z.prev, Z, Z.next) >= 0) return false;
      Z = Z.nextZ;
    }
    return true;
  }
  function a(b, w, x) {
    var C = b;
    do {
      var S = C.prev, A = C.next.next;
      !k(S, A) && g(S, C, C.next, A) && E(S, A) && E(A, S) && (w.push(S.i / x | 0), w.push(C.i / x | 0), w.push(A.i / x | 0), H(C), H(C.next), C = b = A), C = C.next;
    } while (C !== b);
    return t(C);
  }
  function o(b, w, x, C, S, A) {
    var L = b;
    do {
      for (var F = L.next.next; F !== L.prev; ) {
        if (L.i !== F.i && T(L, F)) {
          var D = R(L, F);
          L = t(L, L.next), D = t(D, D.next), r(L, w, x, C, S, A, 0), r(D, w, x, C, S, A, 0);
          return;
        }
        F = F.next;
      }
      L = L.next;
    } while (L !== b);
  }
  function h(b, w, x, C) {
    var S = [], A, L, F, D, $;
    for (A = 0, L = w.length; A < L; A++) F = w[A] * C, D = A < L - 1 ? w[A + 1] * C : b.length, $ = e(b, F, D, C, false), $ === $.next && ($.steiner = true), S.push(f($));
    for (S.sort(l), A = 0; A < S.length; A++) x = c(S[A], x);
    return x;
  }
  function l(b, w) {
    return b.x - w.x;
  }
  function c(b, w) {
    var x = u(b, w);
    if (!x) return w;
    var C = R(x, b);
    return t(C, C.next), t(x, x.next);
  }
  function u(b, w) {
    var x = w, C = b.x, S = b.y, A = -1 / 0, L;
    do {
      if (S <= x.y && S >= x.next.y && x.next.y !== x.y) {
        var F = x.x + (S - x.y) * (x.next.x - x.x) / (x.next.y - x.y);
        if (F <= C && F > A && (A = F, L = x.x < x.next.x ? x : x.next, F === C)) return L;
      }
      x = x.next;
    } while (x !== w);
    if (!L) return null;
    var D = L, $ = L.x, q = L.y, j = 1 / 0, K;
    x = L;
    do
      C >= x.x && x.x >= $ && C !== x.x && _(S < q ? C : A, S, $, q, S < q ? A : C, S, x.x, x.y) && (K = Math.abs(S - x.y) / (C - x.x), E(x, b) && (K < j || K === j && (x.x > L.x || x.x === L.x && m(L, x))) && (L = x, j = K)), x = x.next;
    while (x !== D);
    return L;
  }
  function m(b, w) {
    return I(b.prev, b, w.prev) < 0 && I(w.next, b, b.next) < 0;
  }
  function y(b, w, x, C) {
    var S = b;
    do
      S.z === 0 && (S.z = p(S.x, S.y, w, x, C)), S.prevZ = S.prev, S.nextZ = S.next, S = S.next;
    while (S !== b);
    S.prevZ.nextZ = null, S.prevZ = null, d(S);
  }
  function d(b) {
    var w, x, C, S, A, L, F, D, $ = 1;
    do {
      for (x = b, b = null, A = null, L = 0; x; ) {
        for (L++, C = x, F = 0, w = 0; w < $ && (F++, C = C.nextZ, !!C); w++) ;
        for (D = $; F > 0 || D > 0 && C; ) F !== 0 && (D === 0 || !C || x.z <= C.z) ? (S = x, x = x.nextZ, F--) : (S = C, C = C.nextZ, D--), A ? A.nextZ = S : b = S, S.prevZ = A, A = S;
        x = C;
      }
      A.nextZ = null, $ *= 2;
    } while (L > 1);
    return b;
  }
  function p(b, w, x, C, S) {
    return b = (b - x) * S | 0, w = (w - C) * S | 0, b = (b | b << 8) & 16711935, b = (b | b << 4) & 252645135, b = (b | b << 2) & 858993459, b = (b | b << 1) & 1431655765, w = (w | w << 8) & 16711935, w = (w | w << 4) & 252645135, w = (w | w << 2) & 858993459, w = (w | w << 1) & 1431655765, b | w << 1;
  }
  function f(b) {
    var w = b, x = b;
    do
      (w.x < x.x || w.x === x.x && w.y < x.y) && (x = w), w = w.next;
    while (w !== b);
    return x;
  }
  function _(b, w, x, C, S, A, L, F) {
    return (S - L) * (w - F) >= (b - L) * (A - F) && (b - L) * (C - F) >= (x - L) * (w - F) && (x - L) * (A - F) >= (S - L) * (C - F);
  }
  function T(b, w) {
    return b.next.i !== w.i && b.prev.i !== w.i && !O(b, w) && (E(b, w) && E(w, b) && N(b, w) && (I(b.prev, b, w.prev) || I(b, w.prev, w)) || k(b, w) && I(b.prev, b, b.next) > 0 && I(w.prev, w, w.next) > 0);
  }
  function I(b, w, x) {
    return (w.y - b.y) * (x.x - w.x) - (w.x - b.x) * (x.y - w.y);
  }
  function k(b, w) {
    return b.x === w.x && b.y === w.y;
  }
  function g(b, w, x, C) {
    var S = v(I(b, w, x)), A = v(I(b, w, C)), L = v(I(x, C, b)), F = v(I(x, C, w));
    return !!(S !== A && L !== F || S === 0 && M(b, x, w) || A === 0 && M(b, C, w) || L === 0 && M(x, b, C) || F === 0 && M(x, w, C));
  }
  function M(b, w, x) {
    return w.x <= Math.max(b.x, x.x) && w.x >= Math.min(b.x, x.x) && w.y <= Math.max(b.y, x.y) && w.y >= Math.min(b.y, x.y);
  }
  function v(b) {
    return b > 0 ? 1 : b < 0 ? -1 : 0;
  }
  function O(b, w) {
    var x = b;
    do {
      if (x.i !== b.i && x.next.i !== b.i && x.i !== w.i && x.next.i !== w.i && g(x, x.next, b, w)) return true;
      x = x.next;
    } while (x !== b);
    return false;
  }
  function E(b, w) {
    return I(b.prev, b, b.next) < 0 ? I(b, w, b.next) >= 0 && I(b, b.prev, w) >= 0 : I(b, w, b.prev) < 0 || I(b, b.next, w) < 0;
  }
  function N(b, w) {
    var x = b, C = false, S = (b.x + w.x) / 2, A = (b.y + w.y) / 2;
    do
      x.y > A != x.next.y > A && x.next.y !== x.y && S < (x.next.x - x.x) * (A - x.y) / (x.next.y - x.y) + x.x && (C = !C), x = x.next;
    while (x !== b);
    return C;
  }
  function R(b, w) {
    var x = new G(b.i, b.x, b.y), C = new G(w.i, w.x, w.y), S = b.next, A = w.prev;
    return b.next = w, w.prev = b, x.next = S, S.prev = x, C.next = x, x.prev = C, A.next = C, C.prev = A, C;
  }
  function U(b, w, x, C) {
    var S = new G(b, w, x);
    return C ? (S.next = C.next, S.prev = C, C.next.prev = S, C.next = S) : (S.prev = S, S.next = S), S;
  }
  function H(b) {
    b.next.prev = b.prev, b.prev.next = b.next, b.prevZ && (b.prevZ.nextZ = b.nextZ), b.nextZ && (b.nextZ.prevZ = b.prevZ);
  }
  function G(b, w, x) {
    this.i = b, this.x = w, this.y = x, this.prev = null, this.next = null, this.z = 0, this.prevZ = null, this.nextZ = null, this.steiner = false;
  }
  i.deviation = function(b, w, x, C) {
    var S = w && w.length, A = S ? w[0] * x : b.length, L = Math.abs(z(b, 0, A, x));
    if (S) for (var F = 0, D = w.length; F < D; F++) {
      var $ = w[F] * x, q = F < D - 1 ? w[F + 1] * x : b.length;
      L -= Math.abs(z(b, $, q, x));
    }
    var j = 0;
    for (F = 0; F < C.length; F += 3) {
      var K = C[F] * x, J = C[F + 1] * x, V = C[F + 2] * x;
      j += Math.abs((b[K] - b[V]) * (b[J + 1] - b[K + 1]) - (b[K] - b[J]) * (b[V + 1] - b[K + 1]));
    }
    return L === 0 && j === 0 ? 0 : Math.abs((j - L) / L);
  };
  function z(b, w, x, C) {
    for (var S = 0, A = w, L = x - C; A < x; A += C) S += (b[L] - b[A]) * (b[A + 1] + b[L + 1]), L = A;
    return S;
  }
  return i.flatten = function(b) {
    for (var w = b[0][0].length, x = { vertices: [], holes: [], dimensions: w }, C = 0, S = 0; S < b.length; S++) {
      for (var A = 0; A < b[S].length; A++) for (var L = 0; L < w; L++) x.vertices.push(b[S][A][L]);
      S > 0 && (C += b[S - 1].length, x.holes.push(C));
    }
    return x;
  }, Rr.exports;
}
ul();
var ft = {}, hr = { exports: {} };
var dl = hr.exports, Zn;
function fl() {
  return Zn || (Zn = 1, (function(i, e) {
    (function(t) {
      var r = e && !e.nodeType && e, s = i && !i.nodeType && i, n = typeof Kr == "object" && Kr;
      (n.global === n || n.window === n || n.self === n) && (t = n);
      var a, o = 2147483647, h = 36, l = 1, c = 26, u = 38, m = 700, y = 72, d = 128, p = "-", f = /^xn--/, _ = /[^\x20-\x7E]/, T = /[\x2E\u3002\uFF0E\uFF61]/g, I = { overflow: "Overflow: input needs wider integers to process", "not-basic": "Illegal input >= 0x80 (not a basic code point)", "invalid-input": "Invalid input" }, k = h - l, g = Math.floor, M = String.fromCharCode, v;
      function O(S) {
        throw new RangeError(I[S]);
      }
      function E(S, A) {
        for (var L = S.length, F = []; L--; ) F[L] = A(S[L]);
        return F;
      }
      function N(S, A) {
        var L = S.split("@"), F = "";
        L.length > 1 && (F = L[0] + "@", S = L[1]), S = S.replace(T, ".");
        var D = S.split("."), $ = E(D, A).join(".");
        return F + $;
      }
      function R(S) {
        for (var A = [], L = 0, F = S.length, D, $; L < F; ) D = S.charCodeAt(L++), D >= 55296 && D <= 56319 && L < F ? ($ = S.charCodeAt(L++), ($ & 64512) == 56320 ? A.push(((D & 1023) << 10) + ($ & 1023) + 65536) : (A.push(D), L--)) : A.push(D);
        return A;
      }
      function U(S) {
        return E(S, function(A) {
          var L = "";
          return A > 65535 && (A -= 65536, L += M(A >>> 10 & 1023 | 55296), A = 56320 | A & 1023), L += M(A), L;
        }).join("");
      }
      function H(S) {
        return S - 48 < 10 ? S - 22 : S - 65 < 26 ? S - 65 : S - 97 < 26 ? S - 97 : h;
      }
      function G(S, A) {
        return S + 22 + 75 * (S < 26) - ((A != 0) << 5);
      }
      function z(S, A, L) {
        var F = 0;
        for (S = L ? g(S / m) : S >> 1, S += g(S / A); S > k * c >> 1; F += h) S = g(S / k);
        return g(F + (k + 1) * S / (S + u));
      }
      function b(S) {
        var A = [], L = S.length, F, D = 0, $ = d, q = y, j, K, J, V, Q, ee, ae, le, Y;
        for (j = S.lastIndexOf(p), j < 0 && (j = 0), K = 0; K < j; ++K) S.charCodeAt(K) >= 128 && O("not-basic"), A.push(S.charCodeAt(K));
        for (J = j > 0 ? j + 1 : 0; J < L; ) {
          for (V = D, Q = 1, ee = h; J >= L && O("invalid-input"), ae = H(S.charCodeAt(J++)), (ae >= h || ae > g((o - D) / Q)) && O("overflow"), D += ae * Q, le = ee <= q ? l : ee >= q + c ? c : ee - q, !(ae < le); ee += h) Y = h - le, Q > g(o / Y) && O("overflow"), Q *= Y;
          F = A.length + 1, q = z(D - V, F, V == 0), g(D / F) > o - $ && O("overflow"), $ += g(D / F), D %= F, A.splice(D++, 0, $);
        }
        return U(A);
      }
      function w(S) {
        var A, L, F, D, $, q, j, K, J, V, Q, ee = [], ae, le, Y, Z;
        for (S = R(S), ae = S.length, A = d, L = 0, $ = y, q = 0; q < ae; ++q) Q = S[q], Q < 128 && ee.push(M(Q));
        for (F = D = ee.length, D && ee.push(p); F < ae; ) {
          for (j = o, q = 0; q < ae; ++q) Q = S[q], Q >= A && Q < j && (j = Q);
          for (le = F + 1, j - A > g((o - L) / le) && O("overflow"), L += (j - A) * le, A = j, q = 0; q < ae; ++q) if (Q = S[q], Q < A && ++L > o && O("overflow"), Q == A) {
            for (K = L, J = h; V = J <= $ ? l : J >= $ + c ? c : J - $, !(K < V); J += h) Z = K - V, Y = h - V, ee.push(M(G(V + Z % Y, 0))), K = g(Z / Y);
            ee.push(M(G(K, 0))), $ = z(L, le, F == D), L = 0, ++F;
          }
          ++L, ++A;
        }
        return ee.join("");
      }
      function x(S) {
        return N(S, function(A) {
          return f.test(A) ? b(A.slice(4).toLowerCase()) : A;
        });
      }
      function C(S) {
        return N(S, function(A) {
          return _.test(A) ? "xn--" + w(A) : A;
        });
      }
      if (a = { version: "1.4.1", ucs2: { decode: R, encode: U }, decode: b, encode: w, toASCII: C, toUnicode: x }, r && s) if (i.exports == r) s.exports = a;
      else for (v in a) a.hasOwnProperty(v) && (r[v] = a[v]);
      else t.punycode = a;
    })(dl);
  })(hr, hr.exports)), hr.exports;
}
var Ai, Jn;
function Qt() {
  return Jn || (Jn = 1, Ai = TypeError), Ai;
}
const pl = {}, ml = Object.freeze(Object.defineProperty({ __proto__: null, default: pl }, Symbol.toStringTag, { value: "Module" })), yl = hl(ml);
var Ii, Qn;
function hi() {
  if (Qn) return Ii;
  Qn = 1;
  var i = typeof Map == "function" && Map.prototype, e = Object.getOwnPropertyDescriptor && i ? Object.getOwnPropertyDescriptor(Map.prototype, "size") : null, t = i && e && typeof e.get == "function" ? e.get : null, r = i && Map.prototype.forEach, s = typeof Set == "function" && Set.prototype, n = Object.getOwnPropertyDescriptor && s ? Object.getOwnPropertyDescriptor(Set.prototype, "size") : null, a = s && n && typeof n.get == "function" ? n.get : null, o = s && Set.prototype.forEach, h = typeof WeakMap == "function" && WeakMap.prototype, l = h ? WeakMap.prototype.has : null, c = typeof WeakSet == "function" && WeakSet.prototype, u = c ? WeakSet.prototype.has : null, m = typeof WeakRef == "function" && WeakRef.prototype, y = m ? WeakRef.prototype.deref : null, d = Boolean.prototype.valueOf, p = Object.prototype.toString, f = Function.prototype.toString, _ = String.prototype.match, T = String.prototype.slice, I = String.prototype.replace, k = String.prototype.toUpperCase, g = String.prototype.toLowerCase, M = RegExp.prototype.test, v = Array.prototype.concat, O = Array.prototype.join, E = Array.prototype.slice, N = Math.floor, R = typeof BigInt == "function" ? BigInt.prototype.valueOf : null, U = Object.getOwnPropertySymbols, H = typeof Symbol == "function" && typeof Symbol.iterator == "symbol" ? Symbol.prototype.toString : null, G = typeof Symbol == "function" && typeof Symbol.iterator == "object", z = typeof Symbol == "function" && Symbol.toStringTag && (typeof Symbol.toStringTag === G || true) ? Symbol.toStringTag : null, b = Object.prototype.propertyIsEnumerable, w = (typeof Reflect == "function" ? Reflect.getPrototypeOf : Object.getPrototypeOf) || ([].__proto__ === Array.prototype ? function(P) {
    return P.__proto__;
  } : null);
  function x(P, B) {
    if (P === 1 / 0 || P === -1 / 0 || P !== P || P && P > -1e3 && P < 1e3 || M.call(/e/, B)) return B;
    var ue = /[0-9](?=(?:[0-9]{3})+(?![0-9]))/g;
    if (typeof P == "number") {
      var ye = P < 0 ? -N(-P) : N(P);
      if (ye !== P) {
        var ge = String(ye), re = T.call(B, ge.length + 1);
        return I.call(ge, ue, "$&_") + "." + I.call(I.call(re, /([0-9]{3})/g, "$&_"), /_$/, "");
      }
    }
    return I.call(B, ue, "$&_");
  }
  var C = yl, S = C.custom, A = le(S) ? S : null, L = { __proto__: null, double: '"', single: "'" }, F = { __proto__: null, double: /(["\\])/g, single: /(['\\])/g };
  Ii = function P(B, ue, ye, ge) {
    var re = ue || {};
    if (Te(re, "quoteStyle") && !Te(L, re.quoteStyle)) throw new TypeError('option "quoteStyle" must be "single" or "double"');
    if (Te(re, "maxStringLength") && (typeof re.maxStringLength == "number" ? re.maxStringLength < 0 && re.maxStringLength !== 1 / 0 : re.maxStringLength !== null)) throw new TypeError('option "maxStringLength", if provided, must be a positive integer, Infinity, or `null`');
    var tt = Te(re, "customInspect") ? re.customInspect : true;
    if (typeof tt != "boolean" && tt !== "symbol") throw new TypeError("option \"customInspect\", if provided, must be `true`, `false`, or `'symbol'`");
    if (Te(re, "indent") && re.indent !== null && re.indent !== "	" && !(parseInt(re.indent, 10) === re.indent && re.indent > 0)) throw new TypeError('option "indent" must be "\\t", an integer > 0, or `null`');
    if (Te(re, "numericSeparator") && typeof re.numericSeparator != "boolean") throw new TypeError('option "numericSeparator", if provided, must be `true` or `false`');
    var dt = re.numericSeparator;
    if (typeof B > "u") return "undefined";
    if (B === null) return "null";
    if (typeof B == "boolean") return B ? "true" : "false";
    if (typeof B == "string") return An(B, re);
    if (typeof B == "number") {
      if (B === 0) return 1 / 0 / B > 0 ? "0" : "-0";
      var Me = String(B);
      return dt ? x(B, Me) : Me;
    }
    if (typeof B == "bigint") {
      var rt = String(B) + "n";
      return dt ? x(B, rt) : rt;
    }
    var yi = typeof re.depth > "u" ? 5 : re.depth;
    if (typeof ye > "u" && (ye = 0), ye >= yi && yi > 0 && typeof B == "object") return j(B) ? "[Array]" : "[Object]";
    var kt = tl(re, ye);
    if (typeof ge > "u") ge = [];
    else if (et(ge, B) >= 0) return "[Circular]";
    function Be(Ut, Ar, il) {
      if (Ar && (ge = E.call(ge), ge.push(Ar)), il) {
        var On = { depth: re.depth };
        return Te(re, "quoteStyle") && (On.quoteStyle = re.quoteStyle), P(Ut, On, ye + 1, ge);
      }
      return P(Ut, re, ye + 1, ge);
    }
    if (typeof B == "function" && !J(B)) {
      var Rn = Ft(B), Cn = wr(B, Be);
      return "[Function" + (Rn ? ": " + Rn : " (anonymous)") + "]" + (Cn.length > 0 ? " { " + O.call(Cn, ", ") + " }" : "");
    }
    if (le(B)) {
      var Mn = G ? I.call(String(B), /^(Symbol\(.*\))_[^)]*$/, "$1") : H.call(B);
      return typeof B == "object" && !G ? tr(Mn) : Mn;
    }
    if (Jh(B)) {
      for (var rr = "<" + g.call(String(B.nodeName)), gi = B.attributes || [], Sr = 0; Sr < gi.length; Sr++) rr += " " + gi[Sr].name + "=" + D($(gi[Sr].value), "double", re);
      return rr += ">", B.childNodes && B.childNodes.length && (rr += "..."), rr += "</" + g.call(String(B.nodeName)) + ">", rr;
    }
    if (j(B)) {
      if (B.length === 0) return "[]";
      var vi = wr(B, Be);
      return kt && !el(vi) ? "[" + mi(vi, kt) + "]" : "[ " + O.call(vi, ", ") + " ]";
    }
    if (V(B)) {
      var xi = wr(B, Be);
      return !("cause" in Error.prototype) && "cause" in B && !b.call(B, "cause") ? "{ [" + String(B) + "] " + O.call(v.call("[cause]: " + Be(B.cause), xi), ", ") + " }" : xi.length === 0 ? "[" + String(B) + "]" : "{ [" + String(B) + "] " + O.call(xi, ", ") + " }";
    }
    if (typeof B == "object" && tt) {
      if (A && typeof B[A] == "function" && C) return C(B, { depth: yi - ye });
      if (tt !== "symbol" && typeof B.inspect == "function") return B.inspect();
    }
    if (Oe(B)) {
      var Ln = [];
      return r && r.call(B, function(Ut, Ar) {
        Ln.push(Be(Ar, B, true) + " => " + Be(Ut, B));
      }), In("Map", t.call(B), Ln, kt);
    }
    if (Bt(B)) {
      var Pn = [];
      return o && o.call(B, function(Ut) {
        Pn.push(Be(Ut, B));
      }), In("Set", a.call(B), Pn, kt);
    }
    if (Nt(B)) return pi("WeakMap");
    if (Zh(B)) return pi("WeakSet");
    if (Ot(B)) return pi("WeakRef");
    if (ee(B)) return tr(Be(Number(B)));
    if (Y(B)) return tr(Be(R.call(B)));
    if (ae(B)) return tr(d.call(B));
    if (Q(B)) return tr(Be(String(B)));
    if (typeof window < "u" && B === window) return "{ [object Window] }";
    if (typeof globalThis < "u" && B === globalThis || typeof Kr < "u" && B === Kr) return "{ [object globalThis] }";
    if (!K(B) && !J(B)) {
      var bi = wr(B, Be), Fn = w ? w(B) === Object.prototype : B instanceof Object || B.constructor === Object, _i = B instanceof Object ? "" : "null prototype", Nn = !Fn && z && Object(B) === B && z in B ? T.call(Re(B), 8, -1) : _i ? "Object" : "", rl = Fn || typeof B.constructor != "function" ? "" : B.constructor.name ? B.constructor.name + " " : "", Ti = rl + (Nn || _i ? "[" + O.call(v.call([], Nn || [], _i || []), ": ") + "] " : "");
      return bi.length === 0 ? Ti + "{}" : kt ? Ti + "{" + mi(bi, kt) + "}" : Ti + "{ " + O.call(bi, ", ") + " }";
    }
    return String(B);
  };
  function D(P, B, ue) {
    var ye = ue.quoteStyle || B, ge = L[ye];
    return ge + P + ge;
  }
  function $(P) {
    return I.call(String(P), /"/g, "&quot;");
  }
  function q(P) {
    return !z || !(typeof P == "object" && (z in P || typeof P[z] < "u"));
  }
  function j(P) {
    return Re(P) === "[object Array]" && q(P);
  }
  function K(P) {
    return Re(P) === "[object Date]" && q(P);
  }
  function J(P) {
    return Re(P) === "[object RegExp]" && q(P);
  }
  function V(P) {
    return Re(P) === "[object Error]" && q(P);
  }
  function Q(P) {
    return Re(P) === "[object String]" && q(P);
  }
  function ee(P) {
    return Re(P) === "[object Number]" && q(P);
  }
  function ae(P) {
    return Re(P) === "[object Boolean]" && q(P);
  }
  function le(P) {
    if (G) return P && typeof P == "object" && P instanceof Symbol;
    if (typeof P == "symbol") return true;
    if (!P || typeof P != "object" || !H) return false;
    try {
      return H.call(P), true;
    } catch {
    }
    return false;
  }
  function Y(P) {
    if (!P || typeof P != "object" || !R) return false;
    try {
      return R.call(P), true;
    } catch {
    }
    return false;
  }
  var Z = Object.prototype.hasOwnProperty || function(P) {
    return P in this;
  };
  function Te(P, B) {
    return Z.call(P, B);
  }
  function Re(P) {
    return p.call(P);
  }
  function Ft(P) {
    if (P.name) return P.name;
    var B = _.call(f.call(P), /^function\s*([\w$]+)/);
    return B ? B[1] : null;
  }
  function et(P, B) {
    if (P.indexOf) return P.indexOf(B);
    for (var ue = 0, ye = P.length; ue < ye; ue++) if (P[ue] === B) return ue;
    return -1;
  }
  function Oe(P) {
    if (!t || !P || typeof P != "object") return false;
    try {
      t.call(P);
      try {
        a.call(P);
      } catch {
        return true;
      }
      return P instanceof Map;
    } catch {
    }
    return false;
  }
  function Nt(P) {
    if (!l || !P || typeof P != "object") return false;
    try {
      l.call(P, l);
      try {
        u.call(P, u);
      } catch {
        return true;
      }
      return P instanceof WeakMap;
    } catch {
    }
    return false;
  }
  function Ot(P) {
    if (!y || !P || typeof P != "object") return false;
    try {
      return y.call(P), true;
    } catch {
    }
    return false;
  }
  function Bt(P) {
    if (!a || !P || typeof P != "object") return false;
    try {
      a.call(P);
      try {
        t.call(P);
      } catch {
        return true;
      }
      return P instanceof Set;
    } catch {
    }
    return false;
  }
  function Zh(P) {
    if (!u || !P || typeof P != "object") return false;
    try {
      u.call(P, u);
      try {
        l.call(P, l);
      } catch {
        return true;
      }
      return P instanceof WeakSet;
    } catch {
    }
    return false;
  }
  function Jh(P) {
    return !P || typeof P != "object" ? false : typeof HTMLElement < "u" && P instanceof HTMLElement ? true : typeof P.nodeName == "string" && typeof P.getAttribute == "function";
  }
  function An(P, B) {
    if (P.length > B.maxStringLength) {
      var ue = P.length - B.maxStringLength, ye = "... " + ue + " more character" + (ue > 1 ? "s" : "");
      return An(T.call(P, 0, B.maxStringLength), B) + ye;
    }
    var ge = F[B.quoteStyle || "single"];
    ge.lastIndex = 0;
    var re = I.call(I.call(P, ge, "\\$1"), /[\x00-\x1f]/g, Qh);
    return D(re, "single", B);
  }
  function Qh(P) {
    var B = P.charCodeAt(0), ue = { 8: "b", 9: "t", 10: "n", 12: "f", 13: "r" }[B];
    return ue ? "\\" + ue : "\\x" + (B < 16 ? "0" : "") + k.call(B.toString(16));
  }
  function tr(P) {
    return "Object(" + P + ")";
  }
  function pi(P) {
    return P + " { ? }";
  }
  function In(P, B, ue, ye) {
    var ge = ye ? mi(ue, ye) : O.call(ue, ", ");
    return P + " (" + B + ") {" + ge + "}";
  }
  function el(P) {
    for (var B = 0; B < P.length; B++) if (et(P[B], `
`) >= 0) return false;
    return true;
  }
  function tl(P, B) {
    var ue;
    if (P.indent === "	") ue = "	";
    else if (typeof P.indent == "number" && P.indent > 0) ue = O.call(Array(P.indent + 1), " ");
    else return null;
    return { base: ue, prev: O.call(Array(B + 1), ue) };
  }
  function mi(P, B) {
    if (P.length === 0) return "";
    var ue = `
` + B.prev + B.base;
    return ue + O.call(P, "," + ue) + `
` + B.prev;
  }
  function wr(P, B) {
    var ue = j(P), ye = [];
    if (ue) {
      ye.length = P.length;
      for (var ge = 0; ge < P.length; ge++) ye[ge] = Te(P, ge) ? B(P[ge], P) : "";
    }
    var re = typeof U == "function" ? U(P) : [], tt;
    if (G) {
      tt = {};
      for (var dt = 0; dt < re.length; dt++) tt["$" + re[dt]] = re[dt];
    }
    for (var Me in P) Te(P, Me) && (ue && String(Number(Me)) === Me && Me < P.length || G && tt["$" + Me] instanceof Symbol || (M.call(/[^\w$]/, Me) ? ye.push(B(Me, P) + ": " + B(P[Me], P)) : ye.push(Me + ": " + B(P[Me], P))));
    if (typeof U == "function") for (var rt = 0; rt < re.length; rt++) b.call(P, re[rt]) && ye.push("[" + B(re[rt]) + "]: " + B(P[re[rt]], P));
    return ye;
  }
  return Ii;
}
var Ri, ea;
function gl() {
  if (ea) return Ri;
  ea = 1;
  var i = hi(), e = Qt(), t = function(o, h, l) {
    for (var c = o, u; (u = c.next) != null; c = u) if (u.key === h) return c.next = u.next, l || (u.next = o.next, o.next = u), u;
  }, r = function(o, h) {
    if (o) {
      var l = t(o, h);
      return l && l.value;
    }
  }, s = function(o, h, l) {
    var c = t(o, h);
    c ? c.value = l : o.next = { key: h, next: o.next, value: l };
  }, n = function(o, h) {
    return o ? !!t(o, h) : false;
  }, a = function(o, h) {
    if (o) return t(o, h, true);
  };
  return Ri = function() {
    var h, l = { assert: function(c) {
      if (!l.has(c)) throw new e("Side channel does not contain " + i(c));
    }, delete: function(c) {
      var u = a(h, c);
      return u && h && !h.next && (h = void 0), !!u;
    }, get: function(c) {
      return r(h, c);
    }, has: function(c) {
      return n(h, c);
    }, set: function(c, u) {
      h || (h = { next: void 0 }), s(h, c, u);
    } };
    return l;
  }, Ri;
}
var Ci, ta;
function Lo() {
  return ta || (ta = 1, Ci = Object), Ci;
}
var Mi, ra;
function vl() {
  return ra || (ra = 1, Mi = Error), Mi;
}
var Li, ia;
function xl() {
  return ia || (ia = 1, Li = EvalError), Li;
}
var Pi, sa;
function bl() {
  return sa || (sa = 1, Pi = RangeError), Pi;
}
var Fi, na;
function _l() {
  return na || (na = 1, Fi = ReferenceError), Fi;
}
var Ni, aa;
function Tl() {
  return aa || (aa = 1, Ni = SyntaxError), Ni;
}
var Oi, oa;
function El() {
  return oa || (oa = 1, Oi = URIError), Oi;
}
var Bi, ha;
function wl() {
  return ha || (ha = 1, Bi = Math.abs), Bi;
}
var ki, la;
function Sl() {
  return la || (la = 1, ki = Math.floor), ki;
}
var Ui, ca;
function Al() {
  return ca || (ca = 1, Ui = Math.max), Ui;
}
var Di, ua;
function Il() {
  return ua || (ua = 1, Di = Math.min), Di;
}
var Gi, da;
function Rl() {
  return da || (da = 1, Gi = Math.pow), Gi;
}
var zi, fa;
function Cl() {
  return fa || (fa = 1, zi = Math.round), zi;
}
var $i, pa;
function Ml() {
  return pa || (pa = 1, $i = Number.isNaN || function(e) {
    return e !== e;
  }), $i;
}
var Hi, ma;
function Ll() {
  if (ma) return Hi;
  ma = 1;
  var i = Ml();
  return Hi = function(t) {
    return i(t) || t === 0 ? t : t < 0 ? -1 : 1;
  }, Hi;
}
var Vi, ya;
function Pl() {
  return ya || (ya = 1, Vi = Object.getOwnPropertyDescriptor), Vi;
}
var Wi, ga;
function Po() {
  if (ga) return Wi;
  ga = 1;
  var i = Pl();
  if (i) try {
    i([], "length");
  } catch {
    i = null;
  }
  return Wi = i, Wi;
}
var Xi, va;
function Fo() {
  if (va) return Xi;
  va = 1;
  var i = Object.defineProperty || false;
  if (i) try {
    i({}, "a", { value: 1 });
  } catch {
    i = false;
  }
  return Xi = i, Xi;
}
var qi, xa;
function Fl() {
  return xa || (xa = 1, qi = function() {
    if (typeof Symbol != "function" || typeof Object.getOwnPropertySymbols != "function") return false;
    if (typeof Symbol.iterator == "symbol") return true;
    var e = {}, t = /* @__PURE__ */ Symbol("test"), r = Object(t);
    if (typeof t == "string" || Object.prototype.toString.call(t) !== "[object Symbol]" || Object.prototype.toString.call(r) !== "[object Symbol]") return false;
    var s = 42;
    e[t] = s;
    for (var n in e) return false;
    if (typeof Object.keys == "function" && Object.keys(e).length !== 0 || typeof Object.getOwnPropertyNames == "function" && Object.getOwnPropertyNames(e).length !== 0) return false;
    var a = Object.getOwnPropertySymbols(e);
    if (a.length !== 1 || a[0] !== t || !Object.prototype.propertyIsEnumerable.call(e, t)) return false;
    if (typeof Object.getOwnPropertyDescriptor == "function") {
      var o = Object.getOwnPropertyDescriptor(e, t);
      if (o.value !== s || o.enumerable !== true) return false;
    }
    return true;
  }), qi;
}
var ji, ba;
function Nl() {
  if (ba) return ji;
  ba = 1;
  var i = typeof Symbol < "u" && Symbol, e = Fl();
  return ji = function() {
    return typeof i != "function" || typeof Symbol != "function" || typeof i("foo") != "symbol" || typeof /* @__PURE__ */ Symbol("bar") != "symbol" ? false : e();
  }, ji;
}
var Yi, _a;
function No() {
  return _a || (_a = 1, Yi = typeof Reflect < "u" && Reflect.getPrototypeOf || null), Yi;
}
var Ki, Ta;
function Oo() {
  if (Ta) return Ki;
  Ta = 1;
  var i = Lo();
  return Ki = i.getPrototypeOf || null, Ki;
}
var Zi, Ea;
function Ol() {
  if (Ea) return Zi;
  Ea = 1;
  var i = "Function.prototype.bind called on incompatible ", e = Object.prototype.toString, t = Math.max, r = "[object Function]", s = function(h, l) {
    for (var c = [], u = 0; u < h.length; u += 1) c[u] = h[u];
    for (var m = 0; m < l.length; m += 1) c[m + h.length] = l[m];
    return c;
  }, n = function(h, l) {
    for (var c = [], u = l, m = 0; u < h.length; u += 1, m += 1) c[m] = h[u];
    return c;
  }, a = function(o, h) {
    for (var l = "", c = 0; c < o.length; c += 1) l += o[c], c + 1 < o.length && (l += h);
    return l;
  };
  return Zi = function(h) {
    var l = this;
    if (typeof l != "function" || e.apply(l) !== r) throw new TypeError(i + l);
    for (var c = n(arguments, 1), u, m = function() {
      if (this instanceof u) {
        var _ = l.apply(this, s(c, arguments));
        return Object(_) === _ ? _ : this;
      }
      return l.apply(h, s(c, arguments));
    }, y = t(0, l.length - c.length), d = [], p = 0; p < y; p++) d[p] = "$" + p;
    if (u = Function("binder", "return function (" + a(d, ",") + "){ return binder.apply(this,arguments); }")(m), l.prototype) {
      var f = function() {
      };
      f.prototype = l.prototype, u.prototype = new f(), f.prototype = null;
    }
    return u;
  }, Zi;
}
var Ji, wa;
function li() {
  if (wa) return Ji;
  wa = 1;
  var i = Ol();
  return Ji = Function.prototype.bind || i, Ji;
}
var Qi, Sa;
function ln() {
  return Sa || (Sa = 1, Qi = Function.prototype.call), Qi;
}
var es, Aa;
function Bo() {
  return Aa || (Aa = 1, es = Function.prototype.apply), es;
}
var ts, Ia;
function Bl() {
  return Ia || (Ia = 1, ts = typeof Reflect < "u" && Reflect && Reflect.apply), ts;
}
var rs, Ra;
function kl() {
  if (Ra) return rs;
  Ra = 1;
  var i = li(), e = Bo(), t = ln(), r = Bl();
  return rs = r || i.call(t, e), rs;
}
var is, Ca;
function ko() {
  if (Ca) return is;
  Ca = 1;
  var i = li(), e = Qt(), t = ln(), r = kl();
  return is = function(n) {
    if (n.length < 1 || typeof n[0] != "function") throw new e("a function is required");
    return r(i, t, n);
  }, is;
}
var ss, Ma;
function Ul() {
  if (Ma) return ss;
  Ma = 1;
  var i = ko(), e = Po(), t;
  try {
    t = [].__proto__ === Array.prototype;
  } catch (a) {
    if (!a || typeof a != "object" || !("code" in a) || a.code !== "ERR_PROTO_ACCESS") throw a;
  }
  var r = !!t && e && e(Object.prototype, "__proto__"), s = Object, n = s.getPrototypeOf;
  return ss = r && typeof r.get == "function" ? i([r.get]) : typeof n == "function" ? function(o) {
    return n(o == null ? o : s(o));
  } : false, ss;
}
var ns, La;
function Dl() {
  if (La) return ns;
  La = 1;
  var i = No(), e = Oo(), t = Ul();
  return ns = i ? function(s) {
    return i(s);
  } : e ? function(s) {
    if (!s || typeof s != "object" && typeof s != "function") throw new TypeError("getProto: not an object");
    return e(s);
  } : t ? function(s) {
    return t(s);
  } : null, ns;
}
var as, Pa;
function Gl() {
  if (Pa) return as;
  Pa = 1;
  var i = Function.prototype.call, e = Object.prototype.hasOwnProperty, t = li();
  return as = t.call(i, e), as;
}
var os, Fa;
function cn() {
  if (Fa) return os;
  Fa = 1;
  var i, e = Lo(), t = vl(), r = xl(), s = bl(), n = _l(), a = Tl(), o = Qt(), h = El(), l = wl(), c = Sl(), u = Al(), m = Il(), y = Rl(), d = Cl(), p = Ll(), f = Function, _ = function(J) {
    try {
      return f('"use strict"; return (' + J + ").constructor;")();
    } catch {
    }
  }, T = Po(), I = Fo(), k = function() {
    throw new o();
  }, g = T ? (function() {
    try {
      return arguments.callee, k;
    } catch {
      try {
        return T(arguments, "callee").get;
      } catch {
        return k;
      }
    }
  })() : k, M = Nl()(), v = Dl(), O = Oo(), E = No(), N = Bo(), R = ln(), U = {}, H = typeof Uint8Array > "u" || !v ? i : v(Uint8Array), G = { __proto__: null, "%AggregateError%": typeof AggregateError > "u" ? i : AggregateError, "%Array%": Array, "%ArrayBuffer%": typeof ArrayBuffer > "u" ? i : ArrayBuffer, "%ArrayIteratorPrototype%": M && v ? v([][Symbol.iterator]()) : i, "%AsyncFromSyncIteratorPrototype%": i, "%AsyncFunction%": U, "%AsyncGenerator%": U, "%AsyncGeneratorFunction%": U, "%AsyncIteratorPrototype%": U, "%Atomics%": typeof Atomics > "u" ? i : Atomics, "%BigInt%": typeof BigInt > "u" ? i : BigInt, "%BigInt64Array%": typeof BigInt64Array > "u" ? i : BigInt64Array, "%BigUint64Array%": typeof BigUint64Array > "u" ? i : BigUint64Array, "%Boolean%": Boolean, "%DataView%": typeof DataView > "u" ? i : DataView, "%Date%": Date, "%decodeURI%": decodeURI, "%decodeURIComponent%": decodeURIComponent, "%encodeURI%": encodeURI, "%encodeURIComponent%": encodeURIComponent, "%Error%": t, "%eval%": eval, "%EvalError%": r, "%Float16Array%": typeof Float16Array > "u" ? i : Float16Array, "%Float32Array%": typeof Float32Array > "u" ? i : Float32Array, "%Float64Array%": typeof Float64Array > "u" ? i : Float64Array, "%FinalizationRegistry%": typeof FinalizationRegistry > "u" ? i : FinalizationRegistry, "%Function%": f, "%GeneratorFunction%": U, "%Int8Array%": typeof Int8Array > "u" ? i : Int8Array, "%Int16Array%": typeof Int16Array > "u" ? i : Int16Array, "%Int32Array%": typeof Int32Array > "u" ? i : Int32Array, "%isFinite%": isFinite, "%isNaN%": isNaN, "%IteratorPrototype%": M && v ? v(v([][Symbol.iterator]())) : i, "%JSON%": typeof JSON == "object" ? JSON : i, "%Map%": typeof Map > "u" ? i : Map, "%MapIteratorPrototype%": typeof Map > "u" || !M || !v ? i : v((/* @__PURE__ */ new Map())[Symbol.iterator]()), "%Math%": Math, "%Number%": Number, "%Object%": e, "%Object.getOwnPropertyDescriptor%": T, "%parseFloat%": parseFloat, "%parseInt%": parseInt, "%Promise%": typeof Promise > "u" ? i : Promise, "%Proxy%": typeof Proxy > "u" ? i : Proxy, "%RangeError%": s, "%ReferenceError%": n, "%Reflect%": typeof Reflect > "u" ? i : Reflect, "%RegExp%": RegExp, "%Set%": typeof Set > "u" ? i : Set, "%SetIteratorPrototype%": typeof Set > "u" || !M || !v ? i : v((/* @__PURE__ */ new Set())[Symbol.iterator]()), "%SharedArrayBuffer%": typeof SharedArrayBuffer > "u" ? i : SharedArrayBuffer, "%String%": String, "%StringIteratorPrototype%": M && v ? v(""[Symbol.iterator]()) : i, "%Symbol%": M ? Symbol : i, "%SyntaxError%": a, "%ThrowTypeError%": g, "%TypedArray%": H, "%TypeError%": o, "%Uint8Array%": typeof Uint8Array > "u" ? i : Uint8Array, "%Uint8ClampedArray%": typeof Uint8ClampedArray > "u" ? i : Uint8ClampedArray, "%Uint16Array%": typeof Uint16Array > "u" ? i : Uint16Array, "%Uint32Array%": typeof Uint32Array > "u" ? i : Uint32Array, "%URIError%": h, "%WeakMap%": typeof WeakMap > "u" ? i : WeakMap, "%WeakRef%": typeof WeakRef > "u" ? i : WeakRef, "%WeakSet%": typeof WeakSet > "u" ? i : WeakSet, "%Function.prototype.call%": R, "%Function.prototype.apply%": N, "%Object.defineProperty%": I, "%Object.getPrototypeOf%": O, "%Math.abs%": l, "%Math.floor%": c, "%Math.max%": u, "%Math.min%": m, "%Math.pow%": y, "%Math.round%": d, "%Math.sign%": p, "%Reflect.getPrototypeOf%": E };
  if (v) try {
    null.error;
  } catch (J) {
    var z = v(v(J));
    G["%Error.prototype%"] = z;
  }
  var b = function J(V) {
    var Q;
    if (V === "%AsyncFunction%") Q = _("async function () {}");
    else if (V === "%GeneratorFunction%") Q = _("function* () {}");
    else if (V === "%AsyncGeneratorFunction%") Q = _("async function* () {}");
    else if (V === "%AsyncGenerator%") {
      var ee = J("%AsyncGeneratorFunction%");
      ee && (Q = ee.prototype);
    } else if (V === "%AsyncIteratorPrototype%") {
      var ae = J("%AsyncGenerator%");
      ae && v && (Q = v(ae.prototype));
    }
    return G[V] = Q, Q;
  }, w = { __proto__: null, "%ArrayBufferPrototype%": ["ArrayBuffer", "prototype"], "%ArrayPrototype%": ["Array", "prototype"], "%ArrayProto_entries%": ["Array", "prototype", "entries"], "%ArrayProto_forEach%": ["Array", "prototype", "forEach"], "%ArrayProto_keys%": ["Array", "prototype", "keys"], "%ArrayProto_values%": ["Array", "prototype", "values"], "%AsyncFunctionPrototype%": ["AsyncFunction", "prototype"], "%AsyncGenerator%": ["AsyncGeneratorFunction", "prototype"], "%AsyncGeneratorPrototype%": ["AsyncGeneratorFunction", "prototype", "prototype"], "%BooleanPrototype%": ["Boolean", "prototype"], "%DataViewPrototype%": ["DataView", "prototype"], "%DatePrototype%": ["Date", "prototype"], "%ErrorPrototype%": ["Error", "prototype"], "%EvalErrorPrototype%": ["EvalError", "prototype"], "%Float32ArrayPrototype%": ["Float32Array", "prototype"], "%Float64ArrayPrototype%": ["Float64Array", "prototype"], "%FunctionPrototype%": ["Function", "prototype"], "%Generator%": ["GeneratorFunction", "prototype"], "%GeneratorPrototype%": ["GeneratorFunction", "prototype", "prototype"], "%Int8ArrayPrototype%": ["Int8Array", "prototype"], "%Int16ArrayPrototype%": ["Int16Array", "prototype"], "%Int32ArrayPrototype%": ["Int32Array", "prototype"], "%JSONParse%": ["JSON", "parse"], "%JSONStringify%": ["JSON", "stringify"], "%MapPrototype%": ["Map", "prototype"], "%NumberPrototype%": ["Number", "prototype"], "%ObjectPrototype%": ["Object", "prototype"], "%ObjProto_toString%": ["Object", "prototype", "toString"], "%ObjProto_valueOf%": ["Object", "prototype", "valueOf"], "%PromisePrototype%": ["Promise", "prototype"], "%PromiseProto_then%": ["Promise", "prototype", "then"], "%Promise_all%": ["Promise", "all"], "%Promise_reject%": ["Promise", "reject"], "%Promise_resolve%": ["Promise", "resolve"], "%RangeErrorPrototype%": ["RangeError", "prototype"], "%ReferenceErrorPrototype%": ["ReferenceError", "prototype"], "%RegExpPrototype%": ["RegExp", "prototype"], "%SetPrototype%": ["Set", "prototype"], "%SharedArrayBufferPrototype%": ["SharedArrayBuffer", "prototype"], "%StringPrototype%": ["String", "prototype"], "%SymbolPrototype%": ["Symbol", "prototype"], "%SyntaxErrorPrototype%": ["SyntaxError", "prototype"], "%TypedArrayPrototype%": ["TypedArray", "prototype"], "%TypeErrorPrototype%": ["TypeError", "prototype"], "%Uint8ArrayPrototype%": ["Uint8Array", "prototype"], "%Uint8ClampedArrayPrototype%": ["Uint8ClampedArray", "prototype"], "%Uint16ArrayPrototype%": ["Uint16Array", "prototype"], "%Uint32ArrayPrototype%": ["Uint32Array", "prototype"], "%URIErrorPrototype%": ["URIError", "prototype"], "%WeakMapPrototype%": ["WeakMap", "prototype"], "%WeakSetPrototype%": ["WeakSet", "prototype"] }, x = li(), C = Gl(), S = x.call(R, Array.prototype.concat), A = x.call(N, Array.prototype.splice), L = x.call(R, String.prototype.replace), F = x.call(R, String.prototype.slice), D = x.call(R, RegExp.prototype.exec), $ = /[^%.[\]]+|\[(?:(-?\d+(?:\.\d+)?)|(["'])((?:(?!\2)[^\\]|\\.)*?)\2)\]|(?=(?:\.|\[\])(?:\.|\[\]|%$))/g, q = /\\(\\)?/g, j = function(V) {
    var Q = F(V, 0, 1), ee = F(V, -1);
    if (Q === "%" && ee !== "%") throw new a("invalid intrinsic syntax, expected closing `%`");
    if (ee === "%" && Q !== "%") throw new a("invalid intrinsic syntax, expected opening `%`");
    var ae = [];
    return L(V, $, function(le, Y, Z, Te) {
      ae[ae.length] = Z ? L(Te, q, "$1") : Y || le;
    }), ae;
  }, K = function(V, Q) {
    var ee = V, ae;
    if (C(w, ee) && (ae = w[ee], ee = "%" + ae[0] + "%"), C(G, ee)) {
      var le = G[ee];
      if (le === U && (le = b(ee)), typeof le > "u" && !Q) throw new o("intrinsic " + V + " exists, but is not available. Please file an issue!");
      return { alias: ae, name: ee, value: le };
    }
    throw new a("intrinsic " + V + " does not exist!");
  };
  return os = function(V, Q) {
    if (typeof V != "string" || V.length === 0) throw new o("intrinsic name must be a non-empty string");
    if (arguments.length > 1 && typeof Q != "boolean") throw new o('"allowMissing" argument must be a boolean');
    if (D(/^%?[^%]*%?$/, V) === null) throw new a("`%` may not be present anywhere but at the beginning and end of the intrinsic name");
    var ee = j(V), ae = ee.length > 0 ? ee[0] : "", le = K("%" + ae + "%", Q), Y = le.name, Z = le.value, Te = false, Re = le.alias;
    Re && (ae = Re[0], A(ee, S([0, 1], Re)));
    for (var Ft = 1, et = true; Ft < ee.length; Ft += 1) {
      var Oe = ee[Ft], Nt = F(Oe, 0, 1), Ot = F(Oe, -1);
      if ((Nt === '"' || Nt === "'" || Nt === "`" || Ot === '"' || Ot === "'" || Ot === "`") && Nt !== Ot) throw new a("property names with quotes must have matching quotes");
      if ((Oe === "constructor" || !et) && (Te = true), ae += "." + Oe, Y = "%" + ae + "%", C(G, Y)) Z = G[Y];
      else if (Z != null) {
        if (!(Oe in Z)) {
          if (!Q) throw new o("base intrinsic for " + V + " exists, but the property is not available.");
          return;
        }
        if (T && Ft + 1 >= ee.length) {
          var Bt = T(Z, Oe);
          et = !!Bt, et && "get" in Bt && !("originalValue" in Bt.get) ? Z = Bt.get : Z = Z[Oe];
        } else et = C(Z, Oe), Z = Z[Oe];
        et && !Te && (G[Y] = Z);
      }
    }
    return Z;
  }, os;
}
var hs, Na;
function Uo() {
  if (Na) return hs;
  Na = 1;
  var i = cn(), e = ko(), t = e([i("%String.prototype.indexOf%")]);
  return hs = function(s, n) {
    var a = i(s, !!n);
    return typeof a == "function" && t(s, ".prototype.") > -1 ? e([a]) : a;
  }, hs;
}
var ls, Oa;
function Do() {
  if (Oa) return ls;
  Oa = 1;
  var i = cn(), e = Uo(), t = hi(), r = Qt(), s = i("%Map%", true), n = e("Map.prototype.get", true), a = e("Map.prototype.set", true), o = e("Map.prototype.has", true), h = e("Map.prototype.delete", true), l = e("Map.prototype.size", true);
  return ls = !!s && function() {
    var u, m = { assert: function(y) {
      if (!m.has(y)) throw new r("Side channel does not contain " + t(y));
    }, delete: function(y) {
      if (u) {
        var d = h(u, y);
        return l(u) === 0 && (u = void 0), d;
      }
      return false;
    }, get: function(y) {
      if (u) return n(u, y);
    }, has: function(y) {
      return u ? o(u, y) : false;
    }, set: function(y, d) {
      u || (u = new s()), a(u, y, d);
    } };
    return m;
  }, ls;
}
var cs, Ba;
function zl() {
  if (Ba) return cs;
  Ba = 1;
  var i = cn(), e = Uo(), t = hi(), r = Do(), s = Qt(), n = i("%WeakMap%", true), a = e("WeakMap.prototype.get", true), o = e("WeakMap.prototype.set", true), h = e("WeakMap.prototype.has", true), l = e("WeakMap.prototype.delete", true);
  return cs = n ? function() {
    var u, m, y = { assert: function(d) {
      if (!y.has(d)) throw new s("Side channel does not contain " + t(d));
    }, delete: function(d) {
      if (n && d && (typeof d == "object" || typeof d == "function")) {
        if (u) return l(u, d);
      } else if (r && m) return m.delete(d);
      return false;
    }, get: function(d) {
      return n && d && (typeof d == "object" || typeof d == "function") && u ? a(u, d) : m && m.get(d);
    }, has: function(d) {
      return n && d && (typeof d == "object" || typeof d == "function") && u ? h(u, d) : !!m && m.has(d);
    }, set: function(d, p) {
      n && d && (typeof d == "object" || typeof d == "function") ? (u || (u = new n()), o(u, d, p)) : r && (m || (m = r()), m.set(d, p));
    } };
    return y;
  } : r, cs;
}
var us, ka;
function Go() {
  if (ka) return us;
  ka = 1;
  var i = Qt(), e = hi(), t = gl(), r = Do(), s = zl(), n = s || r || t;
  return us = function() {
    var o, h = { assert: function(l) {
      if (!h.has(l)) {
        var c = l && Object(l) === l ? "the given object key" : e(l);
        throw new i("Side channel does not contain " + c);
      }
    }, delete: function(l) {
      return !!o && o.delete(l);
    }, get: function(l) {
      return o && o.get(l);
    }, has: function(l) {
      return !!o && o.has(l);
    }, set: function(l, c) {
      o || (o = n()), o.set(l, c);
    } };
    return h;
  }, us;
}
var ds, Ua;
function un() {
  if (Ua) return ds;
  Ua = 1;
  var i = String.prototype.replace, e = /%20/g, t = { RFC1738: "RFC1738", RFC3986: "RFC3986" };
  return ds = { default: t.RFC3986, formatters: { RFC1738: function(r) {
    return i.call(r, e, "+");
  }, RFC3986: function(r) {
    return String(r);
  } }, RFC1738: t.RFC1738, RFC3986: t.RFC3986 }, ds;
}
var fs, Da;
function zo() {
  if (Da) return fs;
  Da = 1;
  var i = un(), e = Go(), t = Fo(), r = Object.prototype.hasOwnProperty, s = Array.isArray, n = e(), a = function(E, N) {
    return n.set(E, N), E;
  }, o = function(E) {
    return n.has(E);
  }, h = function(E) {
    return n.get(E);
  }, l = function(E, N) {
    n.set(E, N);
  }, c = (function() {
    for (var O = [], E = 0; E < 256; ++E) O[O.length] = "%" + ((E < 16 ? "0" : "") + E.toString(16)).toUpperCase();
    return O;
  })(), u = function(E) {
    for (; E.length > 1; ) {
      var N = E.pop(), R = N.obj[N.prop];
      if (s(R)) {
        for (var U = [], H = 0; H < R.length; ++H) typeof R[H] < "u" && (U[U.length] = R[H]);
        N.obj[N.prop] = U;
      }
    }
  }, m = function(E, N) {
    for (var R = N && N.plainObjects ? { __proto__: null } : {}, U = 0; U < E.length; ++U) typeof E[U] < "u" && (R[U] = E[U]);
    return R;
  }, y = function(E, N, R) {
    N === "__proto__" && t ? t(E, N, { configurable: true, enumerable: true, value: R, writable: true }) : E[N] = R;
  }, d = function O(E, N, R) {
    if (!N) return E;
    if (typeof N != "object" && typeof N != "function") {
      if (s(E)) {
        var U = E.length;
        if (R && typeof R.arrayLimit == "number" && U >= R.arrayLimit) {
          if (R.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + R.arrayLimit + " element" + (R.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
          return a(m(E.concat(N), R), U);
        }
        E[U] = N;
      } else if (E && typeof E == "object") if (o(E)) {
        var H = h(E) + 1;
        E[H] = N, l(E, H);
      } else {
        if (R && R.strictMerge) return [E, N];
        (R && (R.plainObjects || R.allowPrototypes) || !r.call(Object.prototype, N)) && (E[N] = true);
      }
      else return [E, N];
      return E;
    }
    if (!E || typeof E != "object") {
      if (o(N)) {
        for (var G = Object.keys(N), z = R && R.plainObjects ? { __proto__: null, 0: E } : { 0: E }, b = 0; b < G.length; b++) {
          var w = parseInt(G[b], 10);
          z[w + 1] = N[G[b]];
        }
        return a(z, h(N) + 1);
      }
      var x = [E].concat(N);
      if (R && typeof R.arrayLimit == "number" && x.length > R.arrayLimit) {
        if (R.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + R.arrayLimit + " element" + (R.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
        return a(m(x, R), x.length - 1);
      }
      return x;
    }
    var C = E;
    if (s(E) && !s(N) && (C = m(E, R)), s(E) && s(N)) {
      if (N.forEach(function(S, A) {
        if (r.call(E, A)) {
          var L = E[A];
          L && typeof L == "object" && S && typeof S == "object" ? E[A] = O(L, S, R) : E[E.length] = S;
        } else E[A] = S;
      }), R && typeof R.arrayLimit == "number" && E.length > R.arrayLimit) {
        if (R.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + R.arrayLimit + " element" + (R.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
        return a(m(E, R), E.length - 1);
      }
      return E;
    }
    return Object.keys(N).reduce(function(S, A) {
      var L = N[A];
      if (r.call(S, A) ? y(S, A, O(S[A], L, R)) : y(S, A, L), o(N) && !o(S) && a(S, h(N)), o(S)) {
        var F = parseInt(A, 10);
        String(F) === A && F >= 0 && F > h(S) && l(S, F);
      }
      return S;
    }, C);
  }, p = function(E, N) {
    return Object.keys(N).reduce(function(R, U) {
      return y(R, U, N[U]), R;
    }, E);
  }, f = function(O, E, N) {
    var R = O.replace(/\+/g, " ");
    if (N === "iso-8859-1") return R.replace(/%[0-9a-f]{2}/gi, unescape);
    try {
      return decodeURIComponent(R);
    } catch {
      return R;
    }
  }, _ = 1024, T = function(E, N, R, U, H) {
    if (E.length === 0) return E;
    var G = E;
    if (typeof E == "symbol" ? G = Symbol.prototype.toString.call(E) : typeof E != "string" && (G = String(E)), R === "iso-8859-1") return escape(G).replace(/%u[0-9a-f]{4}/gi, function(L) {
      return "%26%23" + parseInt(L.slice(2), 16) + "%3B";
    });
    for (var z = "", b = 0; b < G.length; b += _) {
      var w = G.length >= _ ? G.slice(b, b + _) : G;
      if (b + _ < G.length) {
        var x = w.charCodeAt(w.length - 1);
        x >= 55296 && x <= 56319 && (w = w.slice(0, -1), b -= 1);
      }
      for (var C = [], S = 0; S < w.length; ++S) {
        var A = w.charCodeAt(S);
        if (A === 45 || A === 46 || A === 95 || A === 126 || A >= 48 && A <= 57 || A >= 65 && A <= 90 || A >= 97 && A <= 122 || H === i.RFC1738 && (A === 40 || A === 41)) {
          C[C.length] = w.charAt(S);
          continue;
        }
        if (A < 128) {
          C[C.length] = c[A];
          continue;
        }
        if (A < 2048) {
          C[C.length] = c[192 | A >> 6] + c[128 | A & 63];
          continue;
        }
        if (A < 55296 || A >= 57344) {
          C[C.length] = c[224 | A >> 12] + c[128 | A >> 6 & 63] + c[128 | A & 63];
          continue;
        }
        S += 1, A = 65536 + ((A & 1023) << 10 | w.charCodeAt(S) & 1023), C[C.length] = c[240 | A >> 18] + c[128 | A >> 12 & 63] + c[128 | A >> 6 & 63] + c[128 | A & 63];
      }
      z += C.join("");
    }
    return z;
  }, I = function(E) {
    for (var N = [{ obj: { o: E }, prop: "o" }], R = e(), U = 0; U < N.length; ++U) for (var H = N[U], G = H.obj[H.prop], z = Object.keys(G), b = 0; b < z.length; ++b) {
      var w = z[b], x = G[w];
      typeof x == "object" && x !== null && !R.has(x) && (N[N.length] = { obj: G, prop: w }, R.set(x, true));
    }
    return u(N), E;
  }, k = function(E) {
    return Object.prototype.toString.call(E) === "[object RegExp]";
  }, g = function(E) {
    return !E || typeof E != "object" ? false : !!(E.constructor && E.constructor.isBuffer && E.constructor.isBuffer(E));
  }, M = function(E, N, R, U, H) {
    if (o(E)) {
      if (H) throw new RangeError("Array limit exceeded. Only " + R + " element" + (R === 1 ? "" : "s") + " allowed in an array.");
      var G = h(E) + 1;
      return E[G] = N, l(E, G), E;
    }
    var z = [].concat(E, N);
    if (z.length > R) {
      if (H) throw new RangeError("Array limit exceeded. Only " + R + " element" + (R === 1 ? "" : "s") + " allowed in an array.");
      return a(m(z, { plainObjects: U }), z.length - 1);
    }
    return z;
  }, v = function(E, N) {
    if (s(E)) {
      for (var R = [], U = 0; U < E.length; U += 1) R[R.length] = N(E[U]);
      return R;
    }
    return N(E);
  };
  return fs = { arrayToObject: m, assign: p, combine: M, compact: I, decode: f, encode: T, isBuffer: g, isOverflow: o, isRegExp: k, markOverflow: a, maybeMap: v, merge: d }, fs;
}
var ps, Ga;
function $l() {
  if (Ga) return ps;
  Ga = 1;
  var i = Go(), e = zo(), t = un(), r = Object.prototype.hasOwnProperty, s = { brackets: function(f) {
    return f + "[]";
  }, comma: "comma", indices: function(f, _) {
    return f + "[" + _ + "]";
  }, repeat: function(f) {
    return f;
  } }, n = Array.isArray, a = Array.prototype.push, o = function(p, f) {
    a.apply(p, n(f) ? f : [f]);
  }, h = Date.prototype.toISOString, l = t.default, c = { addQueryPrefix: false, allowDots: false, allowEmptyArrays: false, arrayFormat: "indices", charset: "utf-8", charsetSentinel: false, commaRoundTrip: false, delimiter: "&", encode: true, encodeDotInKeys: false, encoder: e.encode, encodeValuesOnly: false, filter: void 0, format: l, formatter: t.formatters[l], indices: false, serializeDate: function(f) {
    return h.call(f);
  }, skipNulls: false, strictNullHandling: false }, u = function(f) {
    return typeof f == "string" || typeof f == "number" || typeof f == "boolean" || typeof f == "symbol" || typeof f == "bigint";
  }, m = {}, y = function p(f, _, T, I, k, g, M, v, O, E, N, R, U, H, G, z, b, w) {
    for (var x = f, C = w, S = 0, A = false; (C = C.get(m)) !== void 0 && !A; ) {
      var L = C.get(f);
      if (S += 1, typeof L < "u") {
        if (L === S) throw new RangeError("Cyclic object value");
        A = true;
      }
      typeof C.get(m) > "u" && (S = 0);
    }
    if (typeof E == "function" ? x = E(_, x) : x instanceof Date ? x = U(x) : T === "comma" && n(x) && (x = e.maybeMap(x, function(Y) {
      return Y instanceof Date ? U(Y) : Y;
    })), x === null) {
      if (g) return G(O && !z ? O(_, c.encoder, b, "key", H) : _);
      x = "";
    }
    if (u(x) || e.isBuffer(x)) {
      if (O) {
        var F = z ? _ : O(_, c.encoder, b, "key", H);
        return [G(F) + "=" + G(O(x, c.encoder, b, "value", H))];
      }
      return [G(_) + "=" + G(String(x))];
    }
    var D = [];
    if (typeof x > "u") return D;
    var $;
    if (T === "comma" && n(x)) z && O && (x = e.maybeMap(x, function(Y) {
      return Y == null ? Y : O(Y);
    })), $ = [{ value: x.length > 0 ? x.join(",") || null : void 0 }];
    else if (n(E)) $ = E;
    else {
      var q = Object.keys(x);
      $ = N ? q.sort(N) : q;
    }
    var j = v ? String(_).replace(/\./g, "%2E") : String(_), K = I && n(x) && x.length === 1 ? j + "[]" : j;
    if (k && n(x) && x.length === 0) return K + "[]";
    for (var J = 0; J < $.length; ++J) {
      var V = $[J], Q = typeof V == "object" && V && typeof V.value < "u" ? V.value : x[V];
      if (!(M && Q === null)) {
        var ee = R && v ? String(V).replace(/\./g, "%2E") : String(V), ae = n(x) ? typeof T == "function" ? T(K, ee) : K : K + (R ? "." + ee : "[" + ee + "]");
        w.set(f, S);
        var le = i();
        le.set(m, w), o(D, p(Q, ae, T, I, k, g, M, v, T === "comma" && z && n(x) ? null : O, E, N, R, U, H, G, z, b, le));
      }
    }
    return D;
  }, d = function(f) {
    if (!f) return c;
    if (typeof f.allowEmptyArrays < "u" && typeof f.allowEmptyArrays != "boolean") throw new TypeError("`allowEmptyArrays` option can only be `true` or `false`, when provided");
    if (typeof f.encodeDotInKeys < "u" && typeof f.encodeDotInKeys != "boolean") throw new TypeError("`encodeDotInKeys` option can only be `true` or `false`, when provided");
    if (f.encoder !== null && typeof f.encoder < "u" && typeof f.encoder != "function") throw new TypeError("Encoder has to be a function.");
    var _ = f.charset || c.charset;
    if (typeof f.charset < "u" && f.charset !== "utf-8" && f.charset !== "iso-8859-1") throw new TypeError("The charset option must be either utf-8, iso-8859-1, or undefined");
    var T = t.default;
    if (typeof f.format < "u") {
      if (!r.call(t.formatters, f.format)) throw new TypeError("Unknown format option provided.");
      T = f.format;
    }
    var I = t.formatters[T], k = c.filter;
    (typeof f.filter == "function" || n(f.filter)) && (k = f.filter);
    var g;
    if (f.arrayFormat in s ? g = f.arrayFormat : "indices" in f ? g = f.indices ? "indices" : "repeat" : g = c.arrayFormat, "commaRoundTrip" in f && typeof f.commaRoundTrip != "boolean") throw new TypeError("`commaRoundTrip` must be a boolean, or absent");
    var M = typeof f.allowDots > "u" ? f.encodeDotInKeys === true ? true : c.allowDots : !!f.allowDots;
    return { addQueryPrefix: typeof f.addQueryPrefix == "boolean" ? f.addQueryPrefix : c.addQueryPrefix, allowDots: M, allowEmptyArrays: typeof f.allowEmptyArrays == "boolean" ? !!f.allowEmptyArrays : c.allowEmptyArrays, arrayFormat: g, charset: _, charsetSentinel: typeof f.charsetSentinel == "boolean" ? f.charsetSentinel : c.charsetSentinel, commaRoundTrip: !!f.commaRoundTrip, delimiter: typeof f.delimiter > "u" ? c.delimiter : f.delimiter, encode: typeof f.encode == "boolean" ? f.encode : c.encode, encodeDotInKeys: typeof f.encodeDotInKeys == "boolean" ? f.encodeDotInKeys : c.encodeDotInKeys, encoder: typeof f.encoder == "function" ? f.encoder : c.encoder, encodeValuesOnly: typeof f.encodeValuesOnly == "boolean" ? f.encodeValuesOnly : c.encodeValuesOnly, filter: k, format: T, formatter: I, serializeDate: typeof f.serializeDate == "function" ? f.serializeDate : c.serializeDate, skipNulls: typeof f.skipNulls == "boolean" ? f.skipNulls : c.skipNulls, sort: typeof f.sort == "function" ? f.sort : null, strictNullHandling: typeof f.strictNullHandling == "boolean" ? f.strictNullHandling : c.strictNullHandling };
  };
  return ps = function(p, f) {
    var _ = p, T = d(f), I, k;
    typeof T.filter == "function" ? (k = T.filter, _ = k("", _)) : n(T.filter) && (k = T.filter, I = k);
    var g = [];
    if (typeof _ != "object" || _ === null) return "";
    var M = s[T.arrayFormat], v = M === "comma" && T.commaRoundTrip;
    I || (I = Object.keys(_)), T.sort && I.sort(T.sort);
    for (var O = i(), E = 0; E < I.length; ++E) {
      var N = I[E];
      if (!(typeof N > "u" || N === null)) {
        var R = _[N];
        T.skipNulls && R === null || o(g, y(R, N, M, v, T.allowEmptyArrays, T.strictNullHandling, T.skipNulls, T.encodeDotInKeys, T.encode ? T.encoder : null, T.filter, T.sort, T.allowDots, T.serializeDate, T.format, T.formatter, T.encodeValuesOnly, T.charset, O));
      }
    }
    var U = g.join(T.delimiter), H = T.addQueryPrefix === true ? "?" : "";
    return T.charsetSentinel && (T.charset === "iso-8859-1" ? H += "utf8=%26%2310003%3B" + T.delimiter : H += "utf8=%E2%9C%93" + T.delimiter), U.length > 0 ? H + U : "";
  }, ps;
}
var ms, za;
function Hl() {
  if (za) return ms;
  za = 1;
  var i = zo(), e = Object.prototype.hasOwnProperty, t = Array.isArray, r = { allowDots: false, allowEmptyArrays: false, allowPrototypes: false, allowSparse: false, arrayLimit: 20, charset: "utf-8", charsetSentinel: false, comma: false, decodeDotInKeys: false, decoder: i.decode, delimiter: "&", depth: 5, duplicates: "combine", ignoreQueryPrefix: false, interpretNumericEntities: false, parameterLimit: 1e3, parseArrays: true, plainObjects: false, strictDepth: false, strictMerge: true, strictNullHandling: false, throwOnLimitExceeded: false }, s = function(y) {
    return y.replace(/&#(\d+);/g, function(d, p) {
      return String.fromCharCode(parseInt(p, 10));
    });
  }, n = function(y, d, p, f) {
    if (y && typeof y == "string" && d.comma && y.indexOf(",") > -1) {
      if (f && d.throwOnLimitExceeded) for (var _ = 0, T = y.indexOf(","); T > -1; ) {
        if (_ += 1, _ >= d.arrayLimit) throw new RangeError("Array limit exceeded. Only " + d.arrayLimit + " element" + (d.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
        T = y.indexOf(",", T + 1);
      }
      return y.split(",");
    }
    if (d.throwOnLimitExceeded && p >= d.arrayLimit) throw new RangeError("Array limit exceeded. Only " + d.arrayLimit + " element" + (d.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
    return y;
  }, a = "utf8=%26%2310003%3B", o = "utf8=%E2%9C%93", h = function(d, p) {
    var f = { __proto__: null }, _ = p.ignoreQueryPrefix ? d.replace(/^\?/, "") : d;
    _ = _.replace(/%5B/gi, "[").replace(/%5D/gi, "]");
    var T = p.parameterLimit === 1 / 0 ? void 0 : p.parameterLimit, I = _.split(p.delimiter, p.throwOnLimitExceeded && typeof T < "u" ? T + 1 : T);
    if (p.throwOnLimitExceeded && typeof T < "u" && I.length > T) throw new RangeError("Parameter limit exceeded. Only " + T + " parameter" + (T === 1 ? "" : "s") + " allowed.");
    var k = -1, g, M = p.charset;
    if (p.charsetSentinel) for (g = 0; g < I.length; ++g) I[g].indexOf("utf8=") === 0 && (I[g] === o ? M = "utf-8" : I[g] === a && (M = "iso-8859-1"), k = g, g = I.length);
    for (g = 0; g < I.length; ++g) if (g !== k) {
      var v = I[g], O = v.indexOf("]="), E = O === -1 ? v.indexOf("=") : O + 1, N, R;
      if (E === -1 ? (N = p.decoder(v, r.decoder, M, "key"), R = p.strictNullHandling ? null : "") : (N = p.decoder(v.slice(0, E), r.decoder, M, "key"), N !== null && (R = i.maybeMap(n(v.slice(E + 1), p, t(f[N]) ? f[N].length : 0, v.indexOf("[]=") === -1), function(H) {
        return p.decoder(H, r.decoder, M, "value");
      }))), R && p.interpretNumericEntities && M === "iso-8859-1" && (R = s(String(R))), v.indexOf("[]=") > -1 && (R = t(R) ? [R] : R), p.comma && t(R) && R.length > p.arrayLimit && (R = i.combine([], R, p.arrayLimit, p.plainObjects, p.throwOnLimitExceeded)), N !== null) {
        var U = e.call(f, N);
        U && (p.duplicates === "combine" || v.indexOf("[]=") > -1) ? f[N] = i.combine(f[N], R, p.arrayLimit, p.plainObjects, p.throwOnLimitExceeded) : (!U || p.duplicates === "last") && (f[N] = R);
      }
    }
    return f;
  }, l = function(y, d, p, f) {
    var _ = 0;
    if (y.length > 0 && y[y.length - 1] === "[]") {
      var T = y.slice(0, -1).join("");
      _ = Array.isArray(d) && d[T] ? d[T].length : 0;
    }
    for (var I = f ? d : n(d, p, _), k = y.length - 1; k >= 0; --k) {
      var g, M = y[k];
      if (M === "[]" && p.parseArrays) i.isOverflow(I) ? g = I : g = p.allowEmptyArrays && (I === "" || p.strictNullHandling && I === null) ? [] : i.combine([], I, p.arrayLimit, p.plainObjects, p.throwOnLimitExceeded);
      else {
        g = p.plainObjects ? { __proto__: null } : {};
        var v = M.charAt(0) === "[" && M.charAt(M.length - 1) === "]" ? M.slice(1, -1) : M, O = p.decodeDotInKeys ? v.replace(/%2E/g, ".") : v, E = parseInt(O, 10), N = !isNaN(E) && M !== O && String(E) === O && E >= 0 && p.parseArrays;
        if (!p.parseArrays && O === "") g = { 0: I };
        else if (N && E < p.arrayLimit) g = [], g[E] = I;
        else {
          if (N && p.throwOnLimitExceeded) throw new RangeError("Array limit exceeded. Only " + p.arrayLimit + " element" + (p.arrayLimit === 1 ? "" : "s") + " allowed in an array.");
          N ? (g[E] = I, i.markOverflow(g, E)) : O !== "__proto__" && (g[O] = I);
        }
      }
      I = g;
    }
    return I;
  }, c = function(d, p) {
    var f = p.allowDots ? d.replace(/\.([^.[]+)/g, "[$1]") : d;
    if (p.depth <= 0) return !p.plainObjects && e.call(Object.prototype, f) && !p.allowPrototypes ? void 0 : [f];
    var _ = [], T = f.indexOf("["), I = T >= 0 ? f.slice(0, T) : f;
    if (I) {
      if (!p.plainObjects && e.call(Object.prototype, I) && !p.allowPrototypes) return;
      _[_.length] = I;
    }
    for (var k = f.length, g = T, M = 0; g >= 0 && M < p.depth; ) {
      for (var v = 1, O = g + 1, E = -1; O < k && E < 0; ) {
        var N = f.charCodeAt(O);
        N === 91 ? v += 1 : N === 93 && (v -= 1, v === 0 && (E = O)), O += 1;
      }
      if (E < 0) return _[_.length] = "[" + f.slice(g) + "]", _;
      var R = f.slice(g, E + 1), U = R.slice(1, -1);
      if (!p.plainObjects && e.call(Object.prototype, U) && !p.allowPrototypes) return;
      _[_.length] = R, M += 1, g = f.indexOf("[", E + 1);
    }
    if (g >= 0) {
      if (p.strictDepth === true) throw new RangeError("Input depth exceeded depth option of " + p.depth + " and strictDepth is true");
      _[_.length] = "[" + f.slice(g) + "]";
    }
    return _;
  }, u = function(d, p, f, _) {
    if (d) {
      var T = c(d, f);
      if (T) return l(T, p, f, _);
    }
  }, m = function(d) {
    if (!d) return r;
    if (typeof d.allowEmptyArrays < "u" && typeof d.allowEmptyArrays != "boolean") throw new TypeError("`allowEmptyArrays` option can only be `true` or `false`, when provided");
    if (typeof d.decodeDotInKeys < "u" && typeof d.decodeDotInKeys != "boolean") throw new TypeError("`decodeDotInKeys` option can only be `true` or `false`, when provided");
    if (d.decoder !== null && typeof d.decoder < "u" && typeof d.decoder != "function") throw new TypeError("Decoder has to be a function.");
    if (typeof d.charset < "u" && d.charset !== "utf-8" && d.charset !== "iso-8859-1") throw new TypeError("The charset option must be either utf-8, iso-8859-1, or undefined");
    if (typeof d.throwOnLimitExceeded < "u" && typeof d.throwOnLimitExceeded != "boolean") throw new TypeError("`throwOnLimitExceeded` option must be a boolean");
    var p = typeof d.charset > "u" ? r.charset : d.charset, f = typeof d.duplicates > "u" ? r.duplicates : d.duplicates;
    if (f !== "combine" && f !== "first" && f !== "last") throw new TypeError("The duplicates option must be either combine, first, or last");
    var _ = typeof d.allowDots > "u" ? d.decodeDotInKeys === true ? true : r.allowDots : !!d.allowDots;
    return { allowDots: _, allowEmptyArrays: typeof d.allowEmptyArrays == "boolean" ? !!d.allowEmptyArrays : r.allowEmptyArrays, allowPrototypes: typeof d.allowPrototypes == "boolean" ? d.allowPrototypes : r.allowPrototypes, allowSparse: typeof d.allowSparse == "boolean" ? d.allowSparse : r.allowSparse, arrayLimit: typeof d.arrayLimit == "number" ? d.arrayLimit : r.arrayLimit, charset: p, charsetSentinel: typeof d.charsetSentinel == "boolean" ? d.charsetSentinel : r.charsetSentinel, comma: typeof d.comma == "boolean" ? d.comma : r.comma, decodeDotInKeys: typeof d.decodeDotInKeys == "boolean" ? d.decodeDotInKeys : r.decodeDotInKeys, decoder: typeof d.decoder == "function" ? d.decoder : r.decoder, delimiter: typeof d.delimiter == "string" || i.isRegExp(d.delimiter) ? d.delimiter : r.delimiter, depth: typeof d.depth == "number" || d.depth === false ? +d.depth : r.depth, duplicates: f, ignoreQueryPrefix: d.ignoreQueryPrefix === true, interpretNumericEntities: typeof d.interpretNumericEntities == "boolean" ? d.interpretNumericEntities : r.interpretNumericEntities, parameterLimit: typeof d.parameterLimit == "number" ? d.parameterLimit : r.parameterLimit, parseArrays: d.parseArrays !== false, plainObjects: typeof d.plainObjects == "boolean" ? d.plainObjects : r.plainObjects, strictDepth: typeof d.strictDepth == "boolean" ? !!d.strictDepth : r.strictDepth, strictMerge: typeof d.strictMerge == "boolean" ? !!d.strictMerge : r.strictMerge, strictNullHandling: typeof d.strictNullHandling == "boolean" ? d.strictNullHandling : r.strictNullHandling, throwOnLimitExceeded: typeof d.throwOnLimitExceeded == "boolean" ? d.throwOnLimitExceeded : false };
  };
  return ms = function(y, d) {
    var p = m(d);
    if (y === "" || y === null || typeof y > "u") return p.plainObjects ? { __proto__: null } : {};
    for (var f = typeof y == "string" ? h(y, p) : y, _ = p.plainObjects ? { __proto__: null } : {}, T = Object.keys(f), I = 0; I < T.length; ++I) {
      var k = T[I], g = u(k, f[k], p, typeof y == "string");
      _ = i.merge(_, g, p);
    }
    return p.allowSparse === true ? _ : i.compact(_);
  }, ms;
}
var ys, $a;
function Vl() {
  if ($a) return ys;
  $a = 1;
  var i = $l(), e = Hl(), t = un();
  return ys = { formats: t, parse: e, stringify: i }, ys;
}
var Ha;
function Wl() {
  if (Ha) return ft;
  Ha = 1;
  var i = fl();
  function e() {
    this.protocol = null, this.slashes = null, this.auth = null, this.host = null, this.port = null, this.hostname = null, this.hash = null, this.search = null, this.query = null, this.pathname = null, this.path = null, this.href = null;
  }
  var t = /^([a-z0-9.+-]+:)/i, r = /:[0-9]*$/, s = /^(\/\/?(?!\/)[^?\s]*)(\?[^\s]*)?$/, n = ["<", ">", '"', "`", " ", "\r", `
`, "	"], a = ["{", "}", "|", "\\", "^", "`"].concat(n), o = ["'"].concat(a), h = ["%", "/", "?", ";", "#"].concat(o), l = ["/", "?", "#"], c = 255, u = /^[+a-z0-9A-Z_-]{0,63}$/, m = /^([+a-z0-9A-Z_-]{0,63})(.*)$/, y = { javascript: true, "javascript:": true }, d = { javascript: true, "javascript:": true }, p = { http: true, https: true, ftp: true, gopher: true, file: true, "http:": true, "https:": true, "ftp:": true, "gopher:": true, "file:": true }, f = Vl();
  function _(g, M, v) {
    if (g && typeof g == "object" && g instanceof e) return g;
    var O = new e();
    return O.parse(g, M, v), O;
  }
  e.prototype.parse = function(g, M, v) {
    if (typeof g != "string") throw new TypeError("Parameter 'url' must be a string, not " + typeof g);
    var O = g.indexOf("?"), E = O !== -1 && O < g.indexOf("#") ? "?" : "#", N = g.split(E), R = /\\/g;
    N[0] = N[0].replace(R, "/"), g = N.join(E);
    var U = g;
    if (U = U.trim(), !v && g.split("#").length === 1) {
      var H = s.exec(U);
      if (H) return this.path = U, this.href = U, this.pathname = H[1], H[2] ? (this.search = H[2], M ? this.query = f.parse(this.search.substr(1)) : this.query = this.search.substr(1)) : M && (this.search = "", this.query = {}), this;
    }
    var G = t.exec(U);
    if (G) {
      G = G[0];
      var z = G.toLowerCase();
      this.protocol = z, U = U.substr(G.length);
    }
    if (v || G || U.match(/^\/\/[^@/]+@[^@/]+/)) {
      var b = U.substr(0, 2) === "//";
      b && !(G && d[G]) && (U = U.substr(2), this.slashes = true);
    }
    if (!d[G] && (b || G && !p[G])) {
      for (var w = -1, x = 0; x < l.length; x++) {
        var C = U.indexOf(l[x]);
        C !== -1 && (w === -1 || C < w) && (w = C);
      }
      var S, A;
      w === -1 ? A = U.lastIndexOf("@") : A = U.lastIndexOf("@", w), A !== -1 && (S = U.slice(0, A), U = U.slice(A + 1), this.auth = decodeURIComponent(S)), w = -1;
      for (var x = 0; x < h.length; x++) {
        var C = U.indexOf(h[x]);
        C !== -1 && (w === -1 || C < w) && (w = C);
      }
      w === -1 && (w = U.length), this.host = U.slice(0, w), U = U.slice(w), this.parseHost(), this.hostname = this.hostname || "";
      var L = this.hostname[0] === "[" && this.hostname[this.hostname.length - 1] === "]";
      if (!L) for (var F = this.hostname.split(/\./), x = 0, D = F.length; x < D; x++) {
        var $ = F[x];
        if ($ && !$.match(u)) {
          for (var q = "", j = 0, K = $.length; j < K; j++) $.charCodeAt(j) > 127 ? q += "x" : q += $[j];
          if (!q.match(u)) {
            var J = F.slice(0, x), V = F.slice(x + 1), Q = $.match(m);
            Q && (J.push(Q[1]), V.unshift(Q[2])), V.length && (U = "/" + V.join(".") + U), this.hostname = J.join(".");
            break;
          }
        }
      }
      this.hostname.length > c ? this.hostname = "" : this.hostname = this.hostname.toLowerCase(), L || (this.hostname = i.toASCII(this.hostname));
      var ee = this.port ? ":" + this.port : "", ae = this.hostname || "";
      this.host = ae + ee, this.href += this.host, L && (this.hostname = this.hostname.substr(1, this.hostname.length - 2), U[0] !== "/" && (U = "/" + U));
    }
    if (!y[z]) for (var x = 0, D = o.length; x < D; x++) {
      var le = o[x];
      if (U.indexOf(le) !== -1) {
        var Y = encodeURIComponent(le);
        Y === le && (Y = escape(le)), U = U.split(le).join(Y);
      }
    }
    var Z = U.indexOf("#");
    Z !== -1 && (this.hash = U.substr(Z), U = U.slice(0, Z));
    var Te = U.indexOf("?");
    if (Te !== -1 ? (this.search = U.substr(Te), this.query = U.substr(Te + 1), M && (this.query = f.parse(this.query)), U = U.slice(0, Te)) : M && (this.search = "", this.query = {}), U && (this.pathname = U), p[z] && this.hostname && !this.pathname && (this.pathname = "/"), this.pathname || this.search) {
      var ee = this.pathname || "", Re = this.search || "";
      this.path = ee + Re;
    }
    return this.href = this.format(), this;
  };
  function T(g) {
    return typeof g == "string" && (g = _(g)), g instanceof e ? g.format() : e.prototype.format.call(g);
  }
  e.prototype.format = function() {
    var g = this.auth || "";
    g && (g = encodeURIComponent(g), g = g.replace(/%3A/i, ":"), g += "@");
    var M = this.protocol || "", v = this.pathname || "", O = this.hash || "", E = false, N = "";
    this.host ? E = g + this.host : this.hostname && (E = g + (this.hostname.indexOf(":") === -1 ? this.hostname : "[" + this.hostname + "]"), this.port && (E += ":" + this.port)), this.query && typeof this.query == "object" && Object.keys(this.query).length && (N = f.stringify(this.query, { arrayFormat: "repeat", addQueryPrefix: false }));
    var R = this.search || N && "?" + N || "";
    return M && M.substr(-1) !== ":" && (M += ":"), this.slashes || (!M || p[M]) && E !== false ? (E = "//" + (E || ""), v && v.charAt(0) !== "/" && (v = "/" + v)) : E || (E = ""), O && O.charAt(0) !== "#" && (O = "#" + O), R && R.charAt(0) !== "?" && (R = "?" + R), v = v.replace(/[?#]/g, function(U) {
      return encodeURIComponent(U);
    }), R = R.replace("#", "%23"), M + E + v + R + O;
  };
  function I(g, M) {
    return _(g, false, true).resolve(M);
  }
  e.prototype.resolve = function(g) {
    return this.resolveObject(_(g, false, true)).format();
  };
  function k(g, M) {
    return g ? _(g, false, true).resolveObject(M) : M;
  }
  return e.prototype.resolveObject = function(g) {
    if (typeof g == "string") {
      var M = new e();
      M.parse(g, false, true), g = M;
    }
    for (var v = new e(), O = Object.keys(this), E = 0; E < O.length; E++) {
      var N = O[E];
      v[N] = this[N];
    }
    if (v.hash = g.hash, g.href === "") return v.href = v.format(), v;
    if (g.slashes && !g.protocol) {
      for (var R = Object.keys(g), U = 0; U < R.length; U++) {
        var H = R[U];
        H !== "protocol" && (v[H] = g[H]);
      }
      return p[v.protocol] && v.hostname && !v.pathname && (v.pathname = "/", v.path = v.pathname), v.href = v.format(), v;
    }
    if (g.protocol && g.protocol !== v.protocol) {
      if (!p[g.protocol]) {
        for (var G = Object.keys(g), z = 0; z < G.length; z++) {
          var b = G[z];
          v[b] = g[b];
        }
        return v.href = v.format(), v;
      }
      if (v.protocol = g.protocol, !g.host && !d[g.protocol]) {
        for (var D = (g.pathname || "").split("/"); D.length && !(g.host = D.shift()); ) ;
        g.host || (g.host = ""), g.hostname || (g.hostname = ""), D[0] !== "" && D.unshift(""), D.length < 2 && D.unshift(""), v.pathname = D.join("/");
      } else v.pathname = g.pathname;
      if (v.search = g.search, v.query = g.query, v.host = g.host || "", v.auth = g.auth, v.hostname = g.hostname || g.host, v.port = g.port, v.pathname || v.search) {
        var w = v.pathname || "", x = v.search || "";
        v.path = w + x;
      }
      return v.slashes = v.slashes || g.slashes, v.href = v.format(), v;
    }
    var C = v.pathname && v.pathname.charAt(0) === "/", S = g.host || g.pathname && g.pathname.charAt(0) === "/", A = S || C || v.host && g.pathname, L = A, F = v.pathname && v.pathname.split("/") || [], D = g.pathname && g.pathname.split("/") || [], $ = v.protocol && !p[v.protocol];
    if ($ && (v.hostname = "", v.port = null, v.host && (F[0] === "" ? F[0] = v.host : F.unshift(v.host)), v.host = "", g.protocol && (g.hostname = null, g.port = null, g.host && (D[0] === "" ? D[0] = g.host : D.unshift(g.host)), g.host = null), A = A && (D[0] === "" || F[0] === "")), S) v.host = g.host || g.host === "" ? g.host : v.host, v.hostname = g.hostname || g.hostname === "" ? g.hostname : v.hostname, v.search = g.search, v.query = g.query, F = D;
    else if (D.length) F || (F = []), F.pop(), F = F.concat(D), v.search = g.search, v.query = g.query;
    else if (g.search != null) {
      if ($) {
        v.host = F.shift(), v.hostname = v.host;
        var q = v.host && v.host.indexOf("@") > 0 ? v.host.split("@") : false;
        q && (v.auth = q.shift(), v.hostname = q.shift(), v.host = v.hostname);
      }
      return v.search = g.search, v.query = g.query, (v.pathname !== null || v.search !== null) && (v.path = (v.pathname ? v.pathname : "") + (v.search ? v.search : "")), v.href = v.format(), v;
    }
    if (!F.length) return v.pathname = null, v.search ? v.path = "/" + v.search : v.path = null, v.href = v.format(), v;
    for (var j = F.slice(-1)[0], K = (v.host || g.host || F.length > 1) && (j === "." || j === "..") || j === "", J = 0, V = F.length; V >= 0; V--) j = F[V], j === "." ? F.splice(V, 1) : j === ".." ? (F.splice(V, 1), J++) : J && (F.splice(V, 1), J--);
    if (!A && !L) for (; J--; J) F.unshift("..");
    A && F[0] !== "" && (!F[0] || F[0].charAt(0) !== "/") && F.unshift(""), K && F.join("/").substr(-1) !== "/" && F.push("");
    var Q = F[0] === "" || F[0] && F[0].charAt(0) === "/";
    if ($) {
      v.hostname = Q ? "" : F.length ? F.shift() : "", v.host = v.hostname;
      var q = v.host && v.host.indexOf("@") > 0 ? v.host.split("@") : false;
      q && (v.auth = q.shift(), v.hostname = q.shift(), v.host = v.hostname);
    }
    return A = A || v.host && F.length, A && !Q && F.unshift(""), F.length > 0 ? v.pathname = F.join("/") : (v.pathname = null, v.path = null), (v.pathname !== null || v.search !== null) && (v.path = (v.pathname ? v.pathname : "") + (v.search ? v.search : "")), v.auth = g.auth || v.auth, v.slashes = v.slashes || g.slashes, v.href = v.format(), v;
  }, e.prototype.parseHost = function() {
    var g = this.host, M = r.exec(g);
    M && (M = M[0], M !== ":" && (this.port = M.substr(1)), g = g.substr(0, g.length - M.length)), g && (this.hostname = g);
  }, ft.parse = _, ft.resolve = I, ft.resolveObject = k, ft.format = T, ft.Url = e, ft;
}
Wl();
const Va = {};
function fe(i, e, t = 3) {
  if (Va[e]) return;
  let r = new Error().stack;
  typeof r > "u" ? console.warn("PixiJS Deprecation Warning: ", `${e}
Deprecated since v${i}`) : (r = r.split(`
`).splice(t).join(`
`), console.groupCollapsed ? (console.groupCollapsed("%cPixiJS Deprecation Warning: %c%s", "color:#614108;background:#fffbe6", "font-weight:normal;color:#614108;background:#fffbe6", `${e}
Deprecated since v${i}`), console.warn(r), console.groupEnd()) : (console.warn("PixiJS Deprecation Warning: ", `${e}
Deprecated since v${i}`), console.warn(r))), Va[e] = true;
}
let gs;
function Xl() {
  return typeof gs > "u" && (gs = (function() {
    const i = { stencil: true, failIfMajorPerformanceCaveat: he.FAIL_IF_MAJOR_PERFORMANCE_CAVEAT };
    try {
      if (!he.ADAPTER.getWebGLRenderingContext()) return false;
      const e = he.ADAPTER.createCanvas();
      let t = e.getContext("webgl", i) || e.getContext("experimental-webgl", i);
      const r = !!t?.getContextAttributes()?.stencil;
      if (t) {
        const s = t.getExtension("WEBGL_lose_context");
        s && s.loseContext();
      }
      return t = null, r;
    } catch {
      return false;
    }
  })()), gs;
}
var ql = { grad: 0.9, turn: 360, rad: 360 / (2 * Math.PI) }, Ye = function(i) {
  return typeof i == "string" ? i.length > 0 : typeof i == "number";
}, Se = function(i, e, t) {
  return e === void 0 && (e = 0), t === void 0 && (t = Math.pow(10, e)), Math.round(t * i) / t + 0;
}, Fe = function(i, e, t) {
  return e === void 0 && (e = 0), t === void 0 && (t = 1), i > t ? t : i > e ? i : e;
}, $o = function(i) {
  return (i = isFinite(i) ? i % 360 : 0) > 0 ? i : i + 360;
}, Wa = function(i) {
  return { r: Fe(i.r, 0, 255), g: Fe(i.g, 0, 255), b: Fe(i.b, 0, 255), a: Fe(i.a) };
}, vs = function(i) {
  return { r: Se(i.r), g: Se(i.g), b: Se(i.b), a: Se(i.a, 3) };
}, jl = /^#([0-9a-f]{3,8})$/i, Cr = function(i) {
  var e = i.toString(16);
  return e.length < 2 ? "0" + e : e;
}, Ho = function(i) {
  var e = i.r, t = i.g, r = i.b, s = i.a, n = Math.max(e, t, r), a = n - Math.min(e, t, r), o = a ? n === e ? (t - r) / a : n === t ? 2 + (r - e) / a : 4 + (e - t) / a : 0;
  return { h: 60 * (o < 0 ? o + 6 : o), s: n ? a / n * 100 : 0, v: n / 255 * 100, a: s };
}, Vo = function(i) {
  var e = i.h, t = i.s, r = i.v, s = i.a;
  e = e / 360 * 6, t /= 100, r /= 100;
  var n = Math.floor(e), a = r * (1 - t), o = r * (1 - (e - n) * t), h = r * (1 - (1 - e + n) * t), l = n % 6;
  return { r: 255 * [r, o, a, a, h, r][l], g: 255 * [h, r, r, o, a, a][l], b: 255 * [a, a, h, r, r, o][l], a: s };
}, Xa = function(i) {
  return { h: $o(i.h), s: Fe(i.s, 0, 100), l: Fe(i.l, 0, 100), a: Fe(i.a) };
}, qa = function(i) {
  return { h: Se(i.h), s: Se(i.s), l: Se(i.l), a: Se(i.a, 3) };
}, ja = function(i) {
  return Vo((t = (e = i).s, { h: e.h, s: (t *= ((r = e.l) < 50 ? r : 100 - r) / 100) > 0 ? 2 * t / (r + t) * 100 : 0, v: r + t, a: e.a }));
  var e, t, r;
}, pr = function(i) {
  return { h: (e = Ho(i)).h, s: (s = (200 - (t = e.s)) * (r = e.v) / 100) > 0 && s < 200 ? t * r / 100 / (s <= 100 ? s : 200 - s) * 100 : 0, l: s / 2, a: e.a };
  var e, t, r, s;
}, Yl = /^hsla?\(\s*([+-]?\d*\.?\d+)(deg|rad|grad|turn)?\s*,\s*([+-]?\d*\.?\d+)%\s*,\s*([+-]?\d*\.?\d+)%\s*(?:,\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Kl = /^hsla?\(\s*([+-]?\d*\.?\d+)(deg|rad|grad|turn)?\s+([+-]?\d*\.?\d+)%\s+([+-]?\d*\.?\d+)%\s*(?:\/\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Zl = /^rgba?\(\s*([+-]?\d*\.?\d+)(%)?\s*,\s*([+-]?\d*\.?\d+)(%)?\s*,\s*([+-]?\d*\.?\d+)(%)?\s*(?:,\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, Jl = /^rgba?\(\s*([+-]?\d*\.?\d+)(%)?\s+([+-]?\d*\.?\d+)(%)?\s+([+-]?\d*\.?\d+)(%)?\s*(?:\/\s*([+-]?\d*\.?\d+)(%)?\s*)?\)$/i, ks = { string: [[function(i) {
  var e = jl.exec(i);
  return e ? (i = e[1]).length <= 4 ? { r: parseInt(i[0] + i[0], 16), g: parseInt(i[1] + i[1], 16), b: parseInt(i[2] + i[2], 16), a: i.length === 4 ? Se(parseInt(i[3] + i[3], 16) / 255, 2) : 1 } : i.length === 6 || i.length === 8 ? { r: parseInt(i.substr(0, 2), 16), g: parseInt(i.substr(2, 2), 16), b: parseInt(i.substr(4, 2), 16), a: i.length === 8 ? Se(parseInt(i.substr(6, 2), 16) / 255, 2) : 1 } : null : null;
}, "hex"], [function(i) {
  var e = Zl.exec(i) || Jl.exec(i);
  return e ? e[2] !== e[4] || e[4] !== e[6] ? null : Wa({ r: Number(e[1]) / (e[2] ? 100 / 255 : 1), g: Number(e[3]) / (e[4] ? 100 / 255 : 1), b: Number(e[5]) / (e[6] ? 100 / 255 : 1), a: e[7] === void 0 ? 1 : Number(e[7]) / (e[8] ? 100 : 1) }) : null;
}, "rgb"], [function(i) {
  var e = Yl.exec(i) || Kl.exec(i);
  if (!e) return null;
  var t, r, s = Xa({ h: (t = e[1], r = e[2], r === void 0 && (r = "deg"), Number(t) * (ql[r] || 1)), s: Number(e[3]), l: Number(e[4]), a: e[5] === void 0 ? 1 : Number(e[5]) / (e[6] ? 100 : 1) });
  return ja(s);
}, "hsl"]], object: [[function(i) {
  var e = i.r, t = i.g, r = i.b, s = i.a, n = s === void 0 ? 1 : s;
  return Ye(e) && Ye(t) && Ye(r) ? Wa({ r: Number(e), g: Number(t), b: Number(r), a: Number(n) }) : null;
}, "rgb"], [function(i) {
  var e = i.h, t = i.s, r = i.l, s = i.a, n = s === void 0 ? 1 : s;
  if (!Ye(e) || !Ye(t) || !Ye(r)) return null;
  var a = Xa({ h: Number(e), s: Number(t), l: Number(r), a: Number(n) });
  return ja(a);
}, "hsl"], [function(i) {
  var e = i.h, t = i.s, r = i.v, s = i.a, n = s === void 0 ? 1 : s;
  if (!Ye(e) || !Ye(t) || !Ye(r)) return null;
  var a = (function(o) {
    return { h: $o(o.h), s: Fe(o.s, 0, 100), v: Fe(o.v, 0, 100), a: Fe(o.a) };
  })({ h: Number(e), s: Number(t), v: Number(r), a: Number(n) });
  return Vo(a);
}, "hsv"]] }, Ya = function(i, e) {
  for (var t = 0; t < e.length; t++) {
    var r = e[t][0](i);
    if (r) return [r, e[t][1]];
  }
  return [null, void 0];
}, Ql = function(i) {
  return typeof i == "string" ? Ya(i.trim(), ks.string) : typeof i == "object" && i !== null ? Ya(i, ks.object) : [null, void 0];
}, xs = function(i, e) {
  var t = pr(i);
  return { h: t.h, s: Fe(t.s + 100 * e, 0, 100), l: t.l, a: t.a };
}, bs = function(i) {
  return (299 * i.r + 587 * i.g + 114 * i.b) / 1e3 / 255;
}, Ka = function(i, e) {
  var t = pr(i);
  return { h: t.h, s: t.s, l: Fe(t.l + 100 * e, 0, 100), a: t.a };
}, Us = (function() {
  function i(e) {
    this.parsed = Ql(e)[0], this.rgba = this.parsed || { r: 0, g: 0, b: 0, a: 1 };
  }
  return i.prototype.isValid = function() {
    return this.parsed !== null;
  }, i.prototype.brightness = function() {
    return Se(bs(this.rgba), 2);
  }, i.prototype.isDark = function() {
    return bs(this.rgba) < 0.5;
  }, i.prototype.isLight = function() {
    return bs(this.rgba) >= 0.5;
  }, i.prototype.toHex = function() {
    return e = vs(this.rgba), t = e.r, r = e.g, s = e.b, a = (n = e.a) < 1 ? Cr(Se(255 * n)) : "", "#" + Cr(t) + Cr(r) + Cr(s) + a;
    var e, t, r, s, n, a;
  }, i.prototype.toRgb = function() {
    return vs(this.rgba);
  }, i.prototype.toRgbString = function() {
    return e = vs(this.rgba), t = e.r, r = e.g, s = e.b, (n = e.a) < 1 ? "rgba(" + t + ", " + r + ", " + s + ", " + n + ")" : "rgb(" + t + ", " + r + ", " + s + ")";
    var e, t, r, s, n;
  }, i.prototype.toHsl = function() {
    return qa(pr(this.rgba));
  }, i.prototype.toHslString = function() {
    return e = qa(pr(this.rgba)), t = e.h, r = e.s, s = e.l, (n = e.a) < 1 ? "hsla(" + t + ", " + r + "%, " + s + "%, " + n + ")" : "hsl(" + t + ", " + r + "%, " + s + "%)";
    var e, t, r, s, n;
  }, i.prototype.toHsv = function() {
    return e = Ho(this.rgba), { h: Se(e.h), s: Se(e.s), v: Se(e.v), a: Se(e.a, 3) };
    var e;
  }, i.prototype.invert = function() {
    return ze({ r: 255 - (e = this.rgba).r, g: 255 - e.g, b: 255 - e.b, a: e.a });
    var e;
  }, i.prototype.saturate = function(e) {
    return e === void 0 && (e = 0.1), ze(xs(this.rgba, e));
  }, i.prototype.desaturate = function(e) {
    return e === void 0 && (e = 0.1), ze(xs(this.rgba, -e));
  }, i.prototype.grayscale = function() {
    return ze(xs(this.rgba, -1));
  }, i.prototype.lighten = function(e) {
    return e === void 0 && (e = 0.1), ze(Ka(this.rgba, e));
  }, i.prototype.darken = function(e) {
    return e === void 0 && (e = 0.1), ze(Ka(this.rgba, -e));
  }, i.prototype.rotate = function(e) {
    return e === void 0 && (e = 15), this.hue(this.hue() + e);
  }, i.prototype.alpha = function(e) {
    return typeof e == "number" ? ze({ r: (t = this.rgba).r, g: t.g, b: t.b, a: e }) : Se(this.rgba.a, 3);
    var t;
  }, i.prototype.hue = function(e) {
    var t = pr(this.rgba);
    return typeof e == "number" ? ze({ h: e, s: t.s, l: t.l, a: t.a }) : Se(t.h);
  }, i.prototype.isEqual = function(e) {
    return this.toHex() === ze(e).toHex();
  }, i;
})(), ze = function(i) {
  return i instanceof Us ? i : new Us(i);
}, Za = [], ec = function(i) {
  i.forEach(function(e) {
    Za.indexOf(e) < 0 && (e(Us, ks), Za.push(e));
  });
};
function tc(i, e) {
  var t = { white: "#ffffff", bisque: "#ffe4c4", blue: "#0000ff", cadetblue: "#5f9ea0", chartreuse: "#7fff00", chocolate: "#d2691e", coral: "#ff7f50", antiquewhite: "#faebd7", aqua: "#00ffff", azure: "#f0ffff", whitesmoke: "#f5f5f5", papayawhip: "#ffefd5", plum: "#dda0dd", blanchedalmond: "#ffebcd", black: "#000000", gold: "#ffd700", goldenrod: "#daa520", gainsboro: "#dcdcdc", cornsilk: "#fff8dc", cornflowerblue: "#6495ed", burlywood: "#deb887", aquamarine: "#7fffd4", beige: "#f5f5dc", crimson: "#dc143c", cyan: "#00ffff", darkblue: "#00008b", darkcyan: "#008b8b", darkgoldenrod: "#b8860b", darkkhaki: "#bdb76b", darkgray: "#a9a9a9", darkgreen: "#006400", darkgrey: "#a9a9a9", peachpuff: "#ffdab9", darkmagenta: "#8b008b", darkred: "#8b0000", darkorchid: "#9932cc", darkorange: "#ff8c00", darkslateblue: "#483d8b", gray: "#808080", darkslategray: "#2f4f4f", darkslategrey: "#2f4f4f", deeppink: "#ff1493", deepskyblue: "#00bfff", wheat: "#f5deb3", firebrick: "#b22222", floralwhite: "#fffaf0", ghostwhite: "#f8f8ff", darkviolet: "#9400d3", magenta: "#ff00ff", green: "#008000", dodgerblue: "#1e90ff", grey: "#808080", honeydew: "#f0fff0", hotpink: "#ff69b4", blueviolet: "#8a2be2", forestgreen: "#228b22", lawngreen: "#7cfc00", indianred: "#cd5c5c", indigo: "#4b0082", fuchsia: "#ff00ff", brown: "#a52a2a", maroon: "#800000", mediumblue: "#0000cd", lightcoral: "#f08080", darkturquoise: "#00ced1", lightcyan: "#e0ffff", ivory: "#fffff0", lightyellow: "#ffffe0", lightsalmon: "#ffa07a", lightseagreen: "#20b2aa", linen: "#faf0e6", mediumaquamarine: "#66cdaa", lemonchiffon: "#fffacd", lime: "#00ff00", khaki: "#f0e68c", mediumseagreen: "#3cb371", limegreen: "#32cd32", mediumspringgreen: "#00fa9a", lightskyblue: "#87cefa", lightblue: "#add8e6", midnightblue: "#191970", lightpink: "#ffb6c1", mistyrose: "#ffe4e1", moccasin: "#ffe4b5", mintcream: "#f5fffa", lightslategray: "#778899", lightslategrey: "#778899", navajowhite: "#ffdead", navy: "#000080", mediumvioletred: "#c71585", powderblue: "#b0e0e6", palegoldenrod: "#eee8aa", oldlace: "#fdf5e6", paleturquoise: "#afeeee", mediumturquoise: "#48d1cc", mediumorchid: "#ba55d3", rebeccapurple: "#663399", lightsteelblue: "#b0c4de", mediumslateblue: "#7b68ee", thistle: "#d8bfd8", tan: "#d2b48c", orchid: "#da70d6", mediumpurple: "#9370db", purple: "#800080", pink: "#ffc0cb", skyblue: "#87ceeb", springgreen: "#00ff7f", palegreen: "#98fb98", red: "#ff0000", yellow: "#ffff00", slateblue: "#6a5acd", lavenderblush: "#fff0f5", peru: "#cd853f", palevioletred: "#db7093", violet: "#ee82ee", teal: "#008080", slategray: "#708090", slategrey: "#708090", aliceblue: "#f0f8ff", darkseagreen: "#8fbc8f", darkolivegreen: "#556b2f", greenyellow: "#adff2f", seagreen: "#2e8b57", seashell: "#fff5ee", tomato: "#ff6347", silver: "#c0c0c0", sienna: "#a0522d", lavender: "#e6e6fa", lightgreen: "#90ee90", orange: "#ffa500", orangered: "#ff4500", steelblue: "#4682b4", royalblue: "#4169e1", turquoise: "#40e0d0", yellowgreen: "#9acd32", salmon: "#fa8072", saddlebrown: "#8b4513", sandybrown: "#f4a460", rosybrown: "#bc8f8f", darksalmon: "#e9967a", lightgoldenrodyellow: "#fafad2", snow: "#fffafa", lightgrey: "#d3d3d3", lightgray: "#d3d3d3", dimgray: "#696969", dimgrey: "#696969", olivedrab: "#6b8e23", olive: "#808000" }, r = {};
  for (var s in t) r[t[s]] = s;
  var n = {};
  i.prototype.toName = function(a) {
    if (!(this.rgba.a || this.rgba.r || this.rgba.g || this.rgba.b)) return "transparent";
    var o, h, l = r[this.toHex()];
    if (l) return l;
    if (a?.closest) {
      var c = this.toRgb(), u = 1 / 0, m = "black";
      if (!n.length) for (var y in t) n[y] = new i(t[y]).toRgb();
      for (var d in t) {
        var p = (o = c, h = n[d], Math.pow(o.r - h.r, 2) + Math.pow(o.g - h.g, 2) + Math.pow(o.b - h.b, 2));
        p < u && (u = p, m = d);
      }
      return m;
    }
  }, e.string.push([function(a) {
    var o = a.toLowerCase(), h = o === "transparent" ? "#0000" : t[o];
    return h ? new i(h).toRgb() : null;
  }, "name"]);
}
ec([tc]);
const zt = class Hr {
  constructor(e = 16777215) {
    this._value = null, this._components = new Float32Array(4), this._components.fill(1), this._int = 16777215, this.value = e;
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
  setValue(e) {
    return this.value = e, this;
  }
  set value(e) {
    if (e instanceof Hr) this._value = this.cloneSource(e._value), this._int = e._int, this._components.set(e._components);
    else {
      if (e === null) throw new Error("Cannot set PIXI.Color#value to null");
      (this._value === null || !this.isSourceEqual(this._value, e)) && (this.normalize(e), this._value = this.cloneSource(e));
    }
  }
  get value() {
    return this._value;
  }
  cloneSource(e) {
    return typeof e == "string" || typeof e == "number" || e instanceof Number || e === null ? e : Array.isArray(e) || ArrayBuffer.isView(e) ? e.slice(0) : typeof e == "object" && e !== null ? { ...e } : e;
  }
  isSourceEqual(e, t) {
    const r = typeof e;
    if (r !== typeof t) return false;
    if (r === "number" || r === "string" || e instanceof Number) return e === t;
    if (Array.isArray(e) && Array.isArray(t) || ArrayBuffer.isView(e) && ArrayBuffer.isView(t)) return e.length !== t.length ? false : e.every((s, n) => s === t[n]);
    if (e !== null && t !== null) {
      const s = Object.keys(e), n = Object.keys(t);
      return s.length !== n.length ? false : s.every((a) => e[a] === t[a]);
    }
    return e === t;
  }
  toRgba() {
    const [e, t, r, s] = this._components;
    return { r: e, g: t, b: r, a: s };
  }
  toRgb() {
    const [e, t, r] = this._components;
    return { r: e, g: t, b: r };
  }
  toRgbaString() {
    const [e, t, r] = this.toUint8RgbArray();
    return `rgba(${e},${t},${r},${this.alpha})`;
  }
  toUint8RgbArray(e) {
    const [t, r, s] = this._components;
    return e = e ?? [], e[0] = Math.round(t * 255), e[1] = Math.round(r * 255), e[2] = Math.round(s * 255), e;
  }
  toRgbArray(e) {
    e = e ?? [];
    const [t, r, s] = this._components;
    return e[0] = t, e[1] = r, e[2] = s, e;
  }
  toNumber() {
    return this._int;
  }
  toLittleEndianNumber() {
    const e = this._int;
    return (e >> 16) + (e & 65280) + ((e & 255) << 16);
  }
  multiply(e) {
    const [t, r, s, n] = Hr.temp.setValue(e)._components;
    return this._components[0] *= t, this._components[1] *= r, this._components[2] *= s, this._components[3] *= n, this.refreshInt(), this._value = null, this;
  }
  premultiply(e, t = true) {
    return t && (this._components[0] *= e, this._components[1] *= e, this._components[2] *= e), this._components[3] = e, this.refreshInt(), this._value = null, this;
  }
  toPremultiplied(e, t = true) {
    if (e === 1) return (255 << 24) + this._int;
    if (e === 0) return t ? 0 : this._int;
    let r = this._int >> 16 & 255, s = this._int >> 8 & 255, n = this._int & 255;
    return t && (r = r * e + 0.5 | 0, s = s * e + 0.5 | 0, n = n * e + 0.5 | 0), (e * 255 << 24) + (r << 16) + (s << 8) + n;
  }
  toHex() {
    const e = this._int.toString(16);
    return `#${"000000".substring(0, 6 - e.length) + e}`;
  }
  toHexa() {
    const e = Math.round(this._components[3] * 255).toString(16);
    return this.toHex() + "00".substring(0, 2 - e.length) + e;
  }
  setAlpha(e) {
    return this._components[3] = this._clamp(e), this;
  }
  round(e) {
    const [t, r, s] = this._components;
    return this._components[0] = Math.round(t * e) / e, this._components[1] = Math.round(r * e) / e, this._components[2] = Math.round(s * e) / e, this.refreshInt(), this._value = null, this;
  }
  toArray(e) {
    e = e ?? [];
    const [t, r, s, n] = this._components;
    return e[0] = t, e[1] = r, e[2] = s, e[3] = n, e;
  }
  normalize(e) {
    let t, r, s, n;
    if ((typeof e == "number" || e instanceof Number) && e >= 0 && e <= 16777215) {
      const a = e;
      t = (a >> 16 & 255) / 255, r = (a >> 8 & 255) / 255, s = (a & 255) / 255, n = 1;
    } else if ((Array.isArray(e) || e instanceof Float32Array) && e.length >= 3 && e.length <= 4) e = this._clamp(e), [t, r, s, n = 1] = e;
    else if ((e instanceof Uint8Array || e instanceof Uint8ClampedArray) && e.length >= 3 && e.length <= 4) e = this._clamp(e, 0, 255), [t, r, s, n = 255] = e, t /= 255, r /= 255, s /= 255, n /= 255;
    else if (typeof e == "string" || typeof e == "object") {
      if (typeof e == "string") {
        const o = Hr.HEX_PATTERN.exec(e);
        o && (e = `#${o[2]}`);
      }
      const a = ze(e);
      a.isValid() && ({ r: t, g: r, b: s, a: n } = a.rgba, t /= 255, r /= 255, s /= 255);
    }
    if (t !== void 0) this._components[0] = t, this._components[1] = r, this._components[2] = s, this._components[3] = n, this.refreshInt();
    else throw new Error(`Unable to convert color ${e}`);
  }
  refreshInt() {
    this._clamp(this._components);
    const [e, t, r] = this._components;
    this._int = (e * 255 << 16) + (t * 255 << 8) + (r * 255 | 0);
  }
  _clamp(e, t = 0, r = 1) {
    return typeof e == "number" ? Math.min(Math.max(e, t), r) : (e.forEach((s, n) => {
      e[n] = Math.min(Math.max(s, t), r);
    }), e);
  }
};
zt.shared = new zt(), zt.temp = new zt(), zt.HEX_PATTERN = /^(#|0x)?(([a-f0-9]{3}){1,2}([a-f0-9]{2})?)$/i;
let Rt = zt;
function rc() {
  const i = [], e = [];
  for (let r = 0; r < 32; r++) i[r] = r, e[r] = r;
  i[te.NORMAL_NPM] = te.NORMAL, i[te.ADD_NPM] = te.ADD, i[te.SCREEN_NPM] = te.SCREEN, e[te.NORMAL] = te.NORMAL_NPM, e[te.ADD] = te.ADD_NPM, e[te.SCREEN] = te.SCREEN_NPM;
  const t = [];
  return t.push(e), t.push(i), t;
}
const ic = rc();
function Wo(i) {
  if (i.BYTES_PER_ELEMENT === 4) return i instanceof Float32Array ? "Float32Array" : i instanceof Uint32Array ? "Uint32Array" : "Int32Array";
  if (i.BYTES_PER_ELEMENT === 2) {
    if (i instanceof Uint16Array) return "Uint16Array";
  } else if (i.BYTES_PER_ELEMENT === 1 && i instanceof Uint8Array) return "Uint8Array";
  return null;
}
function Zr(i) {
  return i += i === 0 ? 1 : 0, --i, i |= i >>> 1, i |= i >>> 2, i |= i >>> 4, i |= i >>> 8, i |= i >>> 16, i + 1;
}
function Ja(i) {
  return !(i & i - 1) && !!i;
}
function Qa(i) {
  let e = (i > 65535 ? 1 : 0) << 4;
  i >>>= e;
  let t = (i > 255 ? 1 : 0) << 3;
  return i >>>= t, e |= t, t = (i > 15 ? 1 : 0) << 2, i >>>= t, e |= t, t = (i > 3 ? 1 : 0) << 1, i >>>= t, e |= t, e | i >> 1;
}
function Vr(i, e, t) {
  const r = i.length;
  let s;
  if (e >= r || t === 0) return;
  t = e + t > r ? r - e : t;
  const n = r - t;
  for (s = e; s < n; ++s) i[s] = i[s + t];
  i.length = n;
}
function Mr(i) {
  return i === 0 ? 0 : i < 0 ? -1 : 1;
}
let sc = 0;
function gr() {
  return ++sc;
}
const eo = {}, $e = /* @__PURE__ */ Object.create(null), it = /* @__PURE__ */ Object.create(null);
function nc(i, e = globalThis.location) {
  if (i.startsWith("data:")) return "";
  e = e || globalThis.location;
  const t = new URL(i, document.baseURI);
  return t.hostname !== e.hostname || t.port !== e.port || t.protocol !== e.protocol ? "anonymous" : "";
}
function to(i, e = 1) {
  const t = he.RETINA_PREFIX?.exec(i);
  return t ? parseFloat(t[1]) : e;
}
var ne = ((i) => (i.Renderer = "renderer", i.Application = "application", i.RendererSystem = "renderer-webgl-system", i.RendererPlugin = "renderer-webgl-plugin", i.CanvasRendererSystem = "renderer-canvas-system", i.CanvasRendererPlugin = "renderer-canvas-plugin", i.Asset = "asset", i.LoadParser = "load-parser", i.ResolveParser = "resolve-parser", i.CacheParser = "cache-parser", i.DetectionParser = "detection-parser", i))(ne || {});
const Ds = (i) => {
  if (typeof i == "function" || typeof i == "object" && i.extension) {
    if (!i.extension) throw new Error("Extension class must have an extension object");
    i = { ...typeof i.extension != "object" ? { type: i.extension } : i.extension, ref: i };
  }
  if (typeof i == "object") i = { ...i };
  else throw new Error("Invalid extension type");
  return typeof i.type == "string" && (i.type = [i.type]), i;
}, ro = (i, e) => Ds(i).priority ?? e, de = { _addHandlers: {}, _removeHandlers: {}, _queue: {}, remove(...i) {
  return i.map(Ds).forEach((e) => {
    e.type.forEach((t) => this._removeHandlers[t]?.(e));
  }), this;
}, add(...i) {
  return i.map(Ds).forEach((e) => {
    e.type.forEach((t) => {
      const r = this._addHandlers, s = this._queue;
      r[t] ? r[t]?.(e) : (s[t] = s[t] || [], s[t]?.push(e));
    });
  }), this;
}, handle(i, e, t) {
  const r = this._addHandlers, s = this._removeHandlers;
  if (r[i] || s[i]) throw new Error(`Extension type ${i} already has a handler`);
  r[i] = e, s[i] = t;
  const n = this._queue;
  return n[i] && (n[i]?.forEach((a) => e(a)), delete n[i]), this;
}, handleByMap(i, e) {
  return this.handle(i, (t) => {
    t.name && (e[t.name] = t.ref);
  }, (t) => {
    t.name && delete e[t.name];
  });
}, handleByList(i, e, t = -1) {
  return this.handle(i, (r) => {
    e.includes(r.ref) || (e.push(r.ref), e.sort((s, n) => ro(n, t) - ro(s, t)));
  }, (r) => {
    const s = e.indexOf(r.ref);
    s !== -1 && e.splice(s, 1);
  });
} };
class ac {
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
}
const oc = ["precision mediump float;", "void main(void){", "float test = 0.1;", "%forloop%", "gl_FragColor = vec4(0.0);", "}"].join(`
`);
function hc(i) {
  let e = "";
  for (let t = 0; t < i; ++t) t > 0 && (e += `
else `), t < i - 1 && (e += `if(test == ${t}.0){}`);
  return e;
}
function lc(i, e) {
  if (i === 0) throw new Error("Invalid value of `0` passed to `checkMaxIfStatementsInShader`");
  const t = e.createShader(e.FRAGMENT_SHADER);
  for (; ; ) {
    const r = oc.replace(/%forloop%/gi, hc(i));
    if (e.shaderSource(t, r), e.compileShader(t), !e.getShaderParameter(t, e.COMPILE_STATUS)) i = i / 2 | 0;
    else break;
  }
  return i;
}
const _s = 0, Ts = 1, Es = 2, ws = 3, Ss = 4, As = 5;
class er {
  constructor() {
    this.data = 0, this.blendMode = te.NORMAL, this.polygonOffset = 0, this.blend = true, this.depthMask = true;
  }
  get blend() {
    return !!(this.data & 1 << _s);
  }
  set blend(e) {
    !!(this.data & 1 << _s) !== e && (this.data ^= 1 << _s);
  }
  get offsets() {
    return !!(this.data & 1 << Ts);
  }
  set offsets(e) {
    !!(this.data & 1 << Ts) !== e && (this.data ^= 1 << Ts);
  }
  get culling() {
    return !!(this.data & 1 << Es);
  }
  set culling(e) {
    !!(this.data & 1 << Es) !== e && (this.data ^= 1 << Es);
  }
  get depthTest() {
    return !!(this.data & 1 << ws);
  }
  set depthTest(e) {
    !!(this.data & 1 << ws) !== e && (this.data ^= 1 << ws);
  }
  get depthMask() {
    return !!(this.data & 1 << As);
  }
  set depthMask(e) {
    !!(this.data & 1 << As) !== e && (this.data ^= 1 << As);
  }
  get clockwiseFrontFace() {
    return !!(this.data & 1 << Ss);
  }
  set clockwiseFrontFace(e) {
    !!(this.data & 1 << Ss) !== e && (this.data ^= 1 << Ss);
  }
  get blendMode() {
    return this._blendMode;
  }
  set blendMode(e) {
    this.blend = e !== te.NONE, this._blendMode = e;
  }
  get polygonOffset() {
    return this._polygonOffset;
  }
  set polygonOffset(e) {
    this.offsets = !!e, this._polygonOffset = e;
  }
  static for2d() {
    const e = new er();
    return e.depthTest = false, e.blend = true, e;
  }
}
er.prototype.toString = function() {
  return `[@pixi/core:State blendMode=${this.blendMode} clockwiseFrontFace=${this.clockwiseFrontFace} culling=${this.culling} depthMask=${this.depthMask} polygonOffset=${this.polygonOffset}]`;
};
const Gs = [];
function Xo(i, e) {
  if (!i) return null;
  let t = "";
  if (typeof i == "string") {
    const r = /\.(\w{3,4})(?:$|\?|#)/i.exec(i);
    r && (t = r[1].toLowerCase());
  }
  for (let r = Gs.length - 1; r >= 0; --r) {
    const s = Gs[r];
    if (s.test && s.test(i, t)) return new s(i, e);
  }
  throw new Error("Unrecognized source type to auto-detect Resource");
}
class De {
  constructor(e) {
    this.items = [], this._name = e, this._aliasCount = 0;
  }
  emit(e, t, r, s, n, a, o, h) {
    if (arguments.length > 8) throw new Error("max arguments reached");
    const { name: l, items: c } = this;
    this._aliasCount++;
    for (let u = 0, m = c.length; u < m; u++) c[u][l](e, t, r, s, n, a, o, h);
    return c === this.items && this._aliasCount--, this;
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
}
Object.defineProperties(De.prototype, { dispatch: { value: De.prototype.emit }, run: { value: De.prototype.emit } });
class vr {
  constructor(e = 0, t = 0) {
    this._width = e, this._height = t, this.destroyed = false, this.internal = false, this.onResize = new De("setRealSize"), this.onUpdate = new De("update"), this.onError = new De("onError");
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
    return false;
  }
  dispose() {
  }
  destroy() {
    this.destroyed || (this.destroyed = true, this.dispose(), this.onError.removeAll(), this.onError = null, this.onResize.removeAll(), this.onResize = null, this.onUpdate.removeAll(), this.onUpdate = null);
  }
  static test(e, t) {
    return false;
  }
}
class qo extends vr {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    if (!r || !s) throw new Error("BufferResource width or height invalid");
    super(r, s), this.data = e, this.unpackAlignment = t.unpackAlignment ?? 4;
  }
  upload(e, t, r) {
    const s = e.gl;
    s.pixelStorei(s.UNPACK_ALIGNMENT, this.unpackAlignment), s.pixelStorei(s.UNPACK_PREMULTIPLY_ALPHA_WEBGL, t.alphaMode === Pt.UNPACK);
    const n = t.realWidth, a = t.realHeight;
    return r.width === n && r.height === a ? s.texSubImage2D(t.target, 0, 0, 0, n, a, t.format, r.type, this.data) : (r.width = n, r.height = a, s.texImage2D(t.target, 0, r.internalFormat, n, a, 0, t.format, r.type, this.data)), true;
  }
  dispose() {
    this.data = null;
  }
  static test(e) {
    return e === null || e instanceof Int8Array || e instanceof Uint8Array || e instanceof Uint8ClampedArray || e instanceof Int16Array || e instanceof Uint16Array || e instanceof Int32Array || e instanceof Uint32Array || e instanceof Float32Array;
  }
}
const cc = { scaleMode: Ze.NEAREST, alphaMode: Pt.NPM }, zs = class $t extends oi {
  constructor(e = null, t = null) {
    super(), t = Object.assign({}, $t.defaultOptions, t);
    const { alphaMode: r, mipmap: s, anisotropicLevel: n, scaleMode: a, width: o, height: h, wrapMode: l, format: c, type: u, target: m, resolution: y, resourceOptions: d } = t;
    e && !(e instanceof vr) && (e = Xo(e, d), e.internal = true), this.resolution = y || he.RESOLUTION, this.width = Math.round((o || 0) * this.resolution) / this.resolution, this.height = Math.round((h || 0) * this.resolution) / this.resolution, this._mipmap = s, this.anisotropicLevel = n, this._wrapMode = l, this._scaleMode = a, this.format = c, this.type = u, this.target = m, this.alphaMode = r, this.uid = gr(), this.touched = 0, this.isPowerOfTwo = false, this._refreshPOT(), this._glTextures = {}, this.dirtyId = 0, this.dirtyStyleId = 0, this.cacheId = null, this.valid = o > 0 && h > 0, this.textureCacheIds = [], this.destroyed = false, this.resource = null, this._batchEnabled = 0, this._batchLocation = 0, this.parentTextureArray = null, this.setResource(e);
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
  set mipmap(e) {
    this._mipmap !== e && (this._mipmap = e, this.dirtyStyleId++);
  }
  get scaleMode() {
    return this._scaleMode;
  }
  set scaleMode(e) {
    this._scaleMode !== e && (this._scaleMode = e, this.dirtyStyleId++);
  }
  get wrapMode() {
    return this._wrapMode;
  }
  set wrapMode(e) {
    this._wrapMode !== e && (this._wrapMode = e, this.dirtyStyleId++);
  }
  setStyle(e, t) {
    let r;
    return e !== void 0 && e !== this.scaleMode && (this.scaleMode = e, r = true), t !== void 0 && t !== this.mipmap && (this.mipmap = t, r = true), r && this.dirtyStyleId++, this;
  }
  setSize(e, t, r) {
    return r = r || this.resolution, this.setRealSize(e * r, t * r, r);
  }
  setRealSize(e, t, r) {
    return this.resolution = r || this.resolution, this.width = Math.round(e) / this.resolution, this.height = Math.round(t) / this.resolution, this._refreshPOT(), this.update(), this;
  }
  _refreshPOT() {
    this.isPowerOfTwo = Ja(this.realWidth) && Ja(this.realHeight);
  }
  setResolution(e) {
    const t = this.resolution;
    return t === e ? this : (this.resolution = e, this.valid && (this.width = Math.round(this.width * t) / e, this.height = Math.round(this.height * t) / e, this.emit("update", this)), this._refreshPOT(), this);
  }
  setResource(e) {
    if (this.resource === e) return this;
    if (this.resource) throw new Error("Resource can be set only once");
    return e.bind(this), this.resource = e, this;
  }
  update() {
    this.valid ? (this.dirtyId++, this.dirtyStyleId++, this.emit("update", this)) : this.width > 0 && this.height > 0 && (this.valid = true, this.emit("loaded", this), this.emit("update", this));
  }
  onError(e) {
    this.emit("error", this, e);
  }
  destroy() {
    this.resource && (this.resource.unbind(this), this.resource.internal && this.resource.destroy(), this.resource = null), this.cacheId && (delete it[this.cacheId], delete $e[this.cacheId], this.cacheId = null), this.valid = false, this.dispose(), $t.removeFromCache(this), this.textureCacheIds = null, this.destroyed = true, this.emit("destroyed", this), this.removeAllListeners();
  }
  dispose() {
    this.emit("dispose", this);
  }
  castToBaseTexture() {
    return this;
  }
  static from(e, t, r = he.STRICT_TEXTURE_CACHE) {
    const s = typeof e == "string";
    let n = null;
    if (s) n = e;
    else {
      if (!e._pixiId) {
        const o = t?.pixiIdPrefix || "pixiid";
        e._pixiId = `${o}_${gr()}`;
      }
      n = e._pixiId;
    }
    let a = it[n];
    if (s && r && !a) throw new Error(`The cacheId "${n}" does not exist in BaseTextureCache.`);
    return a || (a = new $t(e, t), a.cacheId = n, $t.addToCache(a, n)), a;
  }
  static fromBuffer(e, t, r, s) {
    e = e || new Float32Array(t * r * 4);
    const n = new qo(e, { width: t, height: r, ...s?.resourceOptions });
    let a, o;
    return e instanceof Float32Array ? (a = W.RGBA, o = se.FLOAT) : e instanceof Int32Array ? (a = W.RGBA_INTEGER, o = se.INT) : e instanceof Uint32Array ? (a = W.RGBA_INTEGER, o = se.UNSIGNED_INT) : e instanceof Int16Array ? (a = W.RGBA_INTEGER, o = se.SHORT) : e instanceof Uint16Array ? (a = W.RGBA_INTEGER, o = se.UNSIGNED_SHORT) : e instanceof Int8Array ? (a = W.RGBA, o = se.BYTE) : (a = W.RGBA, o = se.UNSIGNED_BYTE), n.internal = true, new $t(n, Object.assign({}, cc, { type: o, format: a }, s));
  }
  static addToCache(e, t) {
    t && (e.textureCacheIds.includes(t) || e.textureCacheIds.push(t), it[t] && it[t] !== e && console.warn(`BaseTexture added to the cache with an id [${t}] that already had an entry`), it[t] = e);
  }
  static removeFromCache(e) {
    if (typeof e == "string") {
      const t = it[e];
      if (t) {
        const r = t.textureCacheIds.indexOf(e);
        return r > -1 && t.textureCacheIds.splice(r, 1), delete it[e], t;
      }
    } else if (e?.textureCacheIds) {
      for (let t = 0; t < e.textureCacheIds.length; ++t) delete it[e.textureCacheIds[t]];
      return e.textureCacheIds.length = 0, e;
    }
    return null;
  }
};
zs.defaultOptions = { mipmap: It.POW2, anisotropicLevel: 0, scaleMode: Ze.LINEAR, wrapMode: on.CLAMP, alphaMode: Pt.UNPACK, target: jt.TEXTURE_2D, format: W.RGBA, type: se.UNSIGNED_BYTE }, zs._globalBatch = 0;
let pe = zs;
class uc {
  constructor() {
    this.texArray = null, this.blend = 0, this.type = Yr.TRIANGLES, this.start = 0, this.size = 0, this.data = null;
  }
}
let dc = 0;
class Ie {
  constructor(e, t = true, r = false) {
    this.data = e || new Float32Array(1), this._glBuffers = {}, this._updateID = 0, this.index = r, this.static = t, this.id = dc++, this.disposeRunner = new De("disposeBuffer");
  }
  update(e) {
    e instanceof Array && (e = new Float32Array(e)), this.data = e || this.data, this._updateID++;
  }
  dispose() {
    this.disposeRunner.emit(this, false);
  }
  destroy() {
    this.dispose(), this.data = null;
  }
  set index(e) {
    this.type = e ? We.ELEMENT_ARRAY_BUFFER : We.ARRAY_BUFFER;
  }
  get index() {
    return this.type === We.ELEMENT_ARRAY_BUFFER;
  }
  static from(e) {
    return e instanceof Array && (e = new Float32Array(e)), new Ie(e);
  }
}
class Jr {
  constructor(e, t = 0, r = false, s = se.FLOAT, n, a, o, h = 1) {
    this.buffer = e, this.size = t, this.normalized = r, this.type = s, this.stride = n, this.start = a, this.instance = o, this.divisor = h;
  }
  destroy() {
    this.buffer = null;
  }
  static from(e, t, r, s, n) {
    return new Jr(e, t, r, s, n);
  }
}
const fc = { Float32Array, Uint32Array, Int32Array, Uint8Array };
function pc(i, e) {
  let t = 0, r = 0;
  const s = {};
  for (let h = 0; h < i.length; h++) r += e[h], t += i[h].length;
  const n = new ArrayBuffer(t * 4);
  let a = null, o = 0;
  for (let h = 0; h < i.length; h++) {
    const l = e[h], c = i[h], u = Wo(c);
    s[u] || (s[u] = new fc[u](n)), a = s[u];
    for (let m = 0; m < c.length; m++) {
      const y = (m / l | 0) * r + o, d = m % l;
      a[y + d] = c[m];
    }
    o += l;
  }
  return new Float32Array(n);
}
const io = { 5126: 4, 5123: 2, 5121: 1 };
let mc = 0;
const yc = { Float32Array, Uint32Array, Int32Array, Uint8Array, Uint16Array };
class Zt {
  constructor(e = [], t = {}) {
    this.buffers = e, this.indexBuffer = null, this.attributes = t, this.glVertexArrayObjects = {}, this.id = mc++, this.instanced = false, this.instanceCount = 1, this.disposeRunner = new De("disposeGeometry"), this.refCount = 0;
  }
  addAttribute(e, t, r = 0, s = false, n, a, o, h = false) {
    if (!t) throw new Error("You must pass a buffer when creating an attribute");
    t instanceof Ie || (t instanceof Array && (t = new Float32Array(t)), t = new Ie(t));
    const l = e.split("|");
    if (l.length > 1) {
      for (let u = 0; u < l.length; u++) this.addAttribute(l[u], t, r, s, n);
      return this;
    }
    let c = this.buffers.indexOf(t);
    return c === -1 && (this.buffers.push(t), c = this.buffers.length - 1), this.attributes[e] = new Jr(c, r, s, n, a, o, h), this.instanced = this.instanced || h, this;
  }
  getAttribute(e) {
    return this.attributes[e];
  }
  getBuffer(e) {
    return this.buffers[this.getAttribute(e).buffer];
  }
  addIndex(e) {
    return e instanceof Ie || (e instanceof Array && (e = new Uint16Array(e)), e = new Ie(e)), e.type = We.ELEMENT_ARRAY_BUFFER, this.indexBuffer = e, this.buffers.includes(e) || this.buffers.push(e), this;
  }
  getIndex() {
    return this.indexBuffer;
  }
  interleave() {
    if (this.buffers.length === 1 || this.buffers.length === 2 && this.indexBuffer) return this;
    const e = [], t = [], r = new Ie();
    let s;
    for (s in this.attributes) {
      const n = this.attributes[s], a = this.buffers[n.buffer];
      e.push(a.data), t.push(n.size * io[n.type] / 4), n.buffer = 0;
    }
    for (r.data = pc(e, t), s = 0; s < this.buffers.length; s++) this.buffers[s] !== this.indexBuffer && this.buffers[s].destroy();
    return this.buffers = [r], this.indexBuffer && this.buffers.push(this.indexBuffer), this;
  }
  getSize() {
    for (const e in this.attributes) {
      const t = this.attributes[e];
      return this.buffers[t.buffer].data.length / (t.stride / 4 || t.size);
    }
    return 0;
  }
  dispose() {
    this.disposeRunner.emit(this, false);
  }
  destroy() {
    this.dispose(), this.buffers = null, this.indexBuffer = null, this.attributes = null;
  }
  clone() {
    const e = new Zt();
    for (let t = 0; t < this.buffers.length; t++) e.buffers[t] = new Ie(this.buffers[t].data.slice(0));
    for (const t in this.attributes) {
      const r = this.attributes[t];
      e.attributes[t] = new Jr(r.buffer, r.size, r.normalized, r.type, r.stride, r.start, r.instance);
    }
    return this.indexBuffer && (e.indexBuffer = e.buffers[this.buffers.indexOf(this.indexBuffer)], e.indexBuffer.type = We.ELEMENT_ARRAY_BUFFER), e;
  }
  static merge(e) {
    const t = new Zt(), r = [], s = [], n = [];
    let a;
    for (let o = 0; o < e.length; o++) {
      a = e[o];
      for (let h = 0; h < a.buffers.length; h++) s[h] = s[h] || 0, s[h] += a.buffers[h].data.length, n[h] = 0;
    }
    for (let o = 0; o < a.buffers.length; o++) r[o] = new yc[Wo(a.buffers[o].data)](s[o]), t.buffers[o] = new Ie(r[o]);
    for (let o = 0; o < e.length; o++) {
      a = e[o];
      for (let h = 0; h < a.buffers.length; h++) r[h].set(a.buffers[h].data, n[h]), n[h] += a.buffers[h].data.length;
    }
    if (t.attributes = a.attributes, a.indexBuffer) {
      t.indexBuffer = t.buffers[a.buffers.indexOf(a.indexBuffer)], t.indexBuffer.type = We.ELEMENT_ARRAY_BUFFER;
      let o = 0, h = 0, l = 0, c = 0;
      for (let u = 0; u < a.buffers.length; u++) if (a.buffers[u] !== a.indexBuffer) {
        c = u;
        break;
      }
      for (const u in a.attributes) {
        const m = a.attributes[u];
        (m.buffer | 0) === c && (h += m.size * io[m.type] / 4);
      }
      for (let u = 0; u < e.length; u++) {
        const m = e[u].indexBuffer.data;
        for (let y = 0; y < m.length; y++) t.indexBuffer.data[y + l] += o;
        o += e[u].buffers[c].data.length / h, l += m.length;
      }
    }
    return t;
  }
}
class gc extends Zt {
  constructor(e = false) {
    super(), this._buffer = new Ie(null, e, false), this._indexBuffer = new Ie(null, e, true), this.addAttribute("aVertexPosition", this._buffer, 2, false, se.FLOAT).addAttribute("aTextureCoord", this._buffer, 2, false, se.FLOAT).addAttribute("aColor", this._buffer, 4, true, se.UNSIGNED_BYTE).addAttribute("aTextureId", this._buffer, 1, true, se.FLOAT).addIndex(this._indexBuffer);
  }
}
const vc = Math.PI * 2, xc = 180 / Math.PI, bc = Math.PI / 180;
var jo = ((i) => (i[i.POLY = 0] = "POLY", i[i.RECT = 1] = "RECT", i[i.CIRC = 2] = "CIRC", i[i.ELIP = 3] = "ELIP", i[i.RREC = 4] = "RREC", i))(jo || {});
class Ae {
  constructor(e = 0, t = 0) {
    this.x = 0, this.y = 0, this.x = e, this.y = t;
  }
  clone() {
    return new Ae(this.x, this.y);
  }
  copyFrom(e) {
    return this.set(e.x, e.y), this;
  }
  copyTo(e) {
    return e.set(this.x, this.y), e;
  }
  equals(e) {
    return e.x === this.x && e.y === this.y;
  }
  set(e = 0, t = e) {
    return this.x = e, this.y = t, this;
  }
}
Ae.prototype.toString = function() {
  return `[@pixi/math:Point x=${this.x} y=${this.y}]`;
};
const Lr = [new Ae(), new Ae(), new Ae(), new Ae()];
class me {
  constructor(e = 0, t = 0, r = 0, s = 0) {
    this.x = Number(e), this.y = Number(t), this.width = Number(r), this.height = Number(s), this.type = jo.RECT;
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
    return new me(0, 0, 0, 0);
  }
  clone() {
    return new me(this.x, this.y, this.width, this.height);
  }
  copyFrom(e) {
    return this.x = e.x, this.y = e.y, this.width = e.width, this.height = e.height, this;
  }
  copyTo(e) {
    return e.x = this.x, e.y = this.y, e.width = this.width, e.height = this.height, e;
  }
  contains(e, t) {
    return this.width <= 0 || this.height <= 0 ? false : e >= this.x && e < this.x + this.width && t >= this.y && t < this.y + this.height;
  }
  intersects(e, t) {
    if (!t) {
      const O = this.x < e.x ? e.x : this.x;
      if ((this.right > e.right ? e.right : this.right) <= O) return false;
      const E = this.y < e.y ? e.y : this.y;
      return (this.bottom > e.bottom ? e.bottom : this.bottom) > E;
    }
    const r = this.left, s = this.right, n = this.top, a = this.bottom;
    if (s <= r || a <= n) return false;
    const o = Lr[0].set(e.left, e.top), h = Lr[1].set(e.left, e.bottom), l = Lr[2].set(e.right, e.top), c = Lr[3].set(e.right, e.bottom);
    if (l.x <= o.x || h.y <= o.y) return false;
    const u = Math.sign(t.a * t.d - t.b * t.c);
    if (u === 0 || (t.apply(o, o), t.apply(h, h), t.apply(l, l), t.apply(c, c), Math.max(o.x, h.x, l.x, c.x) <= r || Math.min(o.x, h.x, l.x, c.x) >= s || Math.max(o.y, h.y, l.y, c.y) <= n || Math.min(o.y, h.y, l.y, c.y) >= a)) return false;
    const m = u * (h.y - o.y), y = u * (o.x - h.x), d = m * r + y * n, p = m * s + y * n, f = m * r + y * a, _ = m * s + y * a;
    if (Math.max(d, p, f, _) <= m * o.x + y * o.y || Math.min(d, p, f, _) >= m * c.x + y * c.y) return false;
    const T = u * (o.y - l.y), I = u * (l.x - o.x), k = T * r + I * n, g = T * s + I * n, M = T * r + I * a, v = T * s + I * a;
    return !(Math.max(k, g, M, v) <= T * o.x + I * o.y || Math.min(k, g, M, v) >= T * c.x + I * c.y);
  }
  pad(e = 0, t = e) {
    return this.x -= e, this.y -= t, this.width += e * 2, this.height += t * 2, this;
  }
  fit(e) {
    const t = Math.max(this.x, e.x), r = Math.min(this.x + this.width, e.x + e.width), s = Math.max(this.y, e.y), n = Math.min(this.y + this.height, e.y + e.height);
    return this.x = t, this.width = Math.max(r - t, 0), this.y = s, this.height = Math.max(n - s, 0), this;
  }
  ceil(e = 1, t = 1e-3) {
    const r = Math.ceil((this.x + this.width - t) * e) / e, s = Math.ceil((this.y + this.height - t) * e) / e;
    return this.x = Math.floor((this.x + t) * e) / e, this.y = Math.floor((this.y + t) * e) / e, this.width = r - this.x, this.height = s - this.y, this;
  }
  enlarge(e) {
    const t = Math.min(this.x, e.x), r = Math.max(this.x + this.width, e.x + e.width), s = Math.min(this.y, e.y), n = Math.max(this.y + this.height, e.y + e.height);
    return this.x = t, this.width = r - t, this.y = s, this.height = n - s, this;
  }
}
me.prototype.toString = function() {
  return `[@pixi/math:Rectangle x=${this.x} y=${this.y} width=${this.width} height=${this.height}]`;
};
class we {
  constructor(e = 1, t = 0, r = 0, s = 1, n = 0, a = 0) {
    this.array = null, this.a = e, this.b = t, this.c = r, this.d = s, this.tx = n, this.ty = a;
  }
  fromArray(e) {
    this.a = e[0], this.b = e[1], this.c = e[3], this.d = e[4], this.tx = e[2], this.ty = e[5];
  }
  set(e, t, r, s, n, a) {
    return this.a = e, this.b = t, this.c = r, this.d = s, this.tx = n, this.ty = a, this;
  }
  toArray(e, t) {
    this.array || (this.array = new Float32Array(9));
    const r = t || this.array;
    return e ? (r[0] = this.a, r[1] = this.b, r[2] = 0, r[3] = this.c, r[4] = this.d, r[5] = 0, r[6] = this.tx, r[7] = this.ty, r[8] = 1) : (r[0] = this.a, r[1] = this.c, r[2] = this.tx, r[3] = this.b, r[4] = this.d, r[5] = this.ty, r[6] = 0, r[7] = 0, r[8] = 1), r;
  }
  apply(e, t) {
    t = t || new Ae();
    const r = e.x, s = e.y;
    return t.x = this.a * r + this.c * s + this.tx, t.y = this.b * r + this.d * s + this.ty, t;
  }
  applyInverse(e, t) {
    t = t || new Ae();
    const r = 1 / (this.a * this.d + this.c * -this.b), s = e.x, n = e.y;
    return t.x = this.d * r * s + -this.c * r * n + (this.ty * this.c - this.tx * this.d) * r, t.y = this.a * r * n + -this.b * r * s + (-this.ty * this.a + this.tx * this.b) * r, t;
  }
  translate(e, t) {
    return this.tx += e, this.ty += t, this;
  }
  scale(e, t) {
    return this.a *= e, this.d *= t, this.c *= e, this.b *= t, this.tx *= e, this.ty *= t, this;
  }
  rotate(e) {
    const t = Math.cos(e), r = Math.sin(e), s = this.a, n = this.c, a = this.tx;
    return this.a = s * t - this.b * r, this.b = s * r + this.b * t, this.c = n * t - this.d * r, this.d = n * r + this.d * t, this.tx = a * t - this.ty * r, this.ty = a * r + this.ty * t, this;
  }
  append(e) {
    const t = this.a, r = this.b, s = this.c, n = this.d;
    return this.a = e.a * t + e.b * s, this.b = e.a * r + e.b * n, this.c = e.c * t + e.d * s, this.d = e.c * r + e.d * n, this.tx = e.tx * t + e.ty * s + this.tx, this.ty = e.tx * r + e.ty * n + this.ty, this;
  }
  setTransform(e, t, r, s, n, a, o, h, l) {
    return this.a = Math.cos(o + l) * n, this.b = Math.sin(o + l) * n, this.c = -Math.sin(o - h) * a, this.d = Math.cos(o - h) * a, this.tx = e - (r * this.a + s * this.c), this.ty = t - (r * this.b + s * this.d), this;
  }
  prepend(e) {
    const t = this.tx;
    if (e.a !== 1 || e.b !== 0 || e.c !== 0 || e.d !== 1) {
      const r = this.a, s = this.c;
      this.a = r * e.a + this.b * e.c, this.b = r * e.b + this.b * e.d, this.c = s * e.a + this.d * e.c, this.d = s * e.b + this.d * e.d;
    }
    return this.tx = t * e.a + this.ty * e.c + e.tx, this.ty = t * e.b + this.ty * e.d + e.ty, this;
  }
  decompose(e) {
    const t = this.a, r = this.b, s = this.c, n = this.d, a = e.pivot, o = -Math.atan2(-s, n), h = Math.atan2(r, t), l = Math.abs(o + h);
    return l < 1e-5 || Math.abs(vc - l) < 1e-5 ? (e.rotation = h, e.skew.x = e.skew.y = 0) : (e.rotation = 0, e.skew.x = o, e.skew.y = h), e.scale.x = Math.sqrt(t * t + r * r), e.scale.y = Math.sqrt(s * s + n * n), e.position.x = this.tx + (a.x * t + a.y * s), e.position.y = this.ty + (a.x * r + a.y * n), e;
  }
  invert() {
    const e = this.a, t = this.b, r = this.c, s = this.d, n = this.tx, a = e * s - t * r;
    return this.a = s / a, this.b = -t / a, this.c = -r / a, this.d = e / a, this.tx = (r * this.ty - s * n) / a, this.ty = -(e * this.ty - t * n) / a, this;
  }
  identity() {
    return this.a = 1, this.b = 0, this.c = 0, this.d = 1, this.tx = 0, this.ty = 0, this;
  }
  clone() {
    const e = new we();
    return e.a = this.a, e.b = this.b, e.c = this.c, e.d = this.d, e.tx = this.tx, e.ty = this.ty, e;
  }
  copyTo(e) {
    return e.a = this.a, e.b = this.b, e.c = this.c, e.d = this.d, e.tx = this.tx, e.ty = this.ty, e;
  }
  copyFrom(e) {
    return this.a = e.a, this.b = e.b, this.c = e.c, this.d = e.d, this.tx = e.tx, this.ty = e.ty, this;
  }
  static get IDENTITY() {
    return new we();
  }
  static get TEMP_MATRIX() {
    return new we();
  }
}
we.prototype.toString = function() {
  return `[@pixi/math:Matrix a=${this.a} b=${this.b} c=${this.c} d=${this.d} tx=${this.tx} ty=${this.ty}]`;
};
const mt = [1, 1, 0, -1, -1, -1, 0, 1, 1, 1, 0, -1, -1, -1, 0, 1], yt = [0, 1, 1, 1, 0, -1, -1, -1, 0, 1, 1, 1, 0, -1, -1, -1], gt = [0, -1, -1, -1, 0, 1, 1, 1, 0, 1, 1, 1, 0, -1, -1, -1], vt = [1, 1, 0, -1, -1, -1, 0, 1, -1, -1, 0, 1, 1, 1, 0, -1], $s = [], Yo = [], Pr = Math.sign;
function _c() {
  for (let i = 0; i < 16; i++) {
    const e = [];
    $s.push(e);
    for (let t = 0; t < 16; t++) {
      const r = Pr(mt[i] * mt[t] + gt[i] * yt[t]), s = Pr(yt[i] * mt[t] + vt[i] * yt[t]), n = Pr(mt[i] * gt[t] + gt[i] * vt[t]), a = Pr(yt[i] * gt[t] + vt[i] * vt[t]);
      for (let o = 0; o < 16; o++) if (mt[o] === r && yt[o] === s && gt[o] === n && vt[o] === a) {
        e.push(o);
        break;
      }
    }
  }
  for (let i = 0; i < 16; i++) {
    const e = new we();
    e.set(mt[i], yt[i], gt[i], vt[i], 0, 0), Yo.push(e);
  }
}
_c();
const xe = { E: 0, SE: 1, S: 2, SW: 3, W: 4, NW: 5, N: 6, NE: 7, MIRROR_VERTICAL: 8, MAIN_DIAGONAL: 10, MIRROR_HORIZONTAL: 12, REVERSE_DIAGONAL: 14, uX: (i) => mt[i], uY: (i) => yt[i], vX: (i) => gt[i], vY: (i) => vt[i], inv: (i) => i & 8 ? i & 15 : -i & 7, add: (i, e) => $s[i][e], sub: (i, e) => $s[i][xe.inv(e)], rotate180: (i) => i ^ 4, isVertical: (i) => (i & 3) === 2, byDirection: (i, e) => Math.abs(i) * 2 <= Math.abs(e) ? e >= 0 ? xe.S : xe.N : Math.abs(e) * 2 <= Math.abs(i) ? i > 0 ? xe.E : xe.W : e > 0 ? i > 0 ? xe.SE : xe.SW : i > 0 ? xe.NE : xe.NW, matrixAppendRotationInv: (i, e, t = 0, r = 0) => {
  const s = Yo[xe.inv(e)];
  s.tx = t, s.ty = r, i.append(s);
} };
class at {
  constructor(e, t, r = 0, s = 0) {
    this._x = r, this._y = s, this.cb = e, this.scope = t;
  }
  clone(e = this.cb, t = this.scope) {
    return new at(e, t, this._x, this._y);
  }
  set(e = 0, t = e) {
    return (this._x !== e || this._y !== t) && (this._x = e, this._y = t, this.cb.call(this.scope)), this;
  }
  copyFrom(e) {
    return (this._x !== e.x || this._y !== e.y) && (this._x = e.x, this._y = e.y, this.cb.call(this.scope)), this;
  }
  copyTo(e) {
    return e.set(this._x, this._y), e;
  }
  equals(e) {
    return e.x === this._x && e.y === this._y;
  }
  get x() {
    return this._x;
  }
  set x(e) {
    this._x !== e && (this._x = e, this.cb.call(this.scope));
  }
  get y() {
    return this._y;
  }
  set y(e) {
    this._y !== e && (this._y = e, this.cb.call(this.scope));
  }
}
at.prototype.toString = function() {
  return `[@pixi/math:ObservablePoint x=${this.x} y=${this.y} scope=${this.scope}]`;
};
const Hs = class {
  constructor() {
    this.worldTransform = new we(), this.localTransform = new we(), this.position = new at(this.onChange, this, 0, 0), this.scale = new at(this.onChange, this, 1, 1), this.pivot = new at(this.onChange, this, 0, 0), this.skew = new at(this.updateSkew, this, 0, 0), this._rotation = 0, this._cx = 1, this._sx = 0, this._cy = 0, this._sy = 1, this._localID = 0, this._currentLocalID = 0, this._worldID = 0, this._parentID = 0;
  }
  onChange() {
    this._localID++;
  }
  updateSkew() {
    this._cx = Math.cos(this._rotation + this.skew.y), this._sx = Math.sin(this._rotation + this.skew.y), this._cy = -Math.sin(this._rotation - this.skew.x), this._sy = Math.cos(this._rotation - this.skew.x), this._localID++;
  }
  updateLocalTransform() {
    const i = this.localTransform;
    this._localID !== this._currentLocalID && (i.a = this._cx * this.scale.x, i.b = this._sx * this.scale.x, i.c = this._cy * this.scale.y, i.d = this._sy * this.scale.y, i.tx = this.position.x - (this.pivot.x * i.a + this.pivot.y * i.c), i.ty = this.position.y - (this.pivot.x * i.b + this.pivot.y * i.d), this._currentLocalID = this._localID, this._parentID = -1);
  }
  updateTransform(i) {
    const e = this.localTransform;
    if (this._localID !== this._currentLocalID && (e.a = this._cx * this.scale.x, e.b = this._sx * this.scale.x, e.c = this._cy * this.scale.y, e.d = this._sy * this.scale.y, e.tx = this.position.x - (this.pivot.x * e.a + this.pivot.y * e.c), e.ty = this.position.y - (this.pivot.x * e.b + this.pivot.y * e.d), this._currentLocalID = this._localID, this._parentID = -1), this._parentID !== i._worldID) {
      const t = i.worldTransform, r = this.worldTransform;
      r.a = e.a * t.a + e.b * t.c, r.b = e.a * t.b + e.b * t.d, r.c = e.c * t.a + e.d * t.c, r.d = e.c * t.b + e.d * t.d, r.tx = e.tx * t.a + e.ty * t.c + t.tx, r.ty = e.tx * t.b + e.ty * t.d + t.ty, this._parentID = i._worldID, this._worldID++;
    }
  }
  setFromMatrix(i) {
    i.decompose(this), this._localID++;
  }
  get rotation() {
    return this._rotation;
  }
  set rotation(i) {
    this._rotation !== i && (this._rotation = i, this.updateSkew());
  }
};
Hs.IDENTITY = new Hs();
let dn = Hs;
dn.prototype.toString = function() {
  return `[@pixi/math:Transform position=(${this.position.x}, ${this.position.y}) rotation=${this.rotation} scale=(${this.scale.x}, ${this.scale.y}) skew=(${this.skew.x}, ${this.skew.y}) ]`;
};
var Tc = `varying vec2 vTextureCoord;

uniform sampler2D uSampler;

void main(void){
   gl_FragColor *= texture2D(uSampler, vTextureCoord);
}`, Ec = `attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void){
   gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
   vTextureCoord = aTextureCoord;
}
`;
function so(i, e, t) {
  const r = i.createShader(e);
  return i.shaderSource(r, t), i.compileShader(r), r;
}
function Is(i) {
  const e = new Array(i);
  for (let t = 0; t < e.length; t++) e[t] = false;
  return e;
}
function Ko(i, e) {
  switch (i) {
    case "float":
      return 0;
    case "vec2":
      return new Float32Array(2 * e);
    case "vec3":
      return new Float32Array(3 * e);
    case "vec4":
      return new Float32Array(4 * e);
    case "int":
    case "uint":
    case "sampler2D":
    case "sampler2DArray":
      return 0;
    case "ivec2":
      return new Int32Array(2 * e);
    case "ivec3":
      return new Int32Array(3 * e);
    case "ivec4":
      return new Int32Array(4 * e);
    case "uvec2":
      return new Uint32Array(2 * e);
    case "uvec3":
      return new Uint32Array(3 * e);
    case "uvec4":
      return new Uint32Array(4 * e);
    case "bool":
      return false;
    case "bvec2":
      return Is(2 * e);
    case "bvec3":
      return Is(3 * e);
    case "bvec4":
      return Is(4 * e);
    case "mat2":
      return new Float32Array([1, 0, 0, 1]);
    case "mat3":
      return new Float32Array([1, 0, 0, 0, 1, 0, 0, 0, 1]);
    case "mat4":
      return new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]);
  }
  return null;
}
const Yt = [{ test: (i) => i.type === "float" && i.size === 1 && !i.isArray, code: (i) => `
            if(uv["${i}"] !== ud["${i}"].value)
            {
                ud["${i}"].value = uv["${i}"]
                gl.uniform1f(ud["${i}"].location, uv["${i}"])
            }
            ` }, { test: (i, e) => (i.type === "sampler2D" || i.type === "samplerCube" || i.type === "sampler2DArray") && i.size === 1 && !i.isArray && (e == null || e.castToBaseTexture !== void 0), code: (i) => `t = syncData.textureCount++;

            renderer.texture.bind(uv["${i}"], t);

            if(ud["${i}"].value !== t)
            {
                ud["${i}"].value = t;
                gl.uniform1i(ud["${i}"].location, t);
; // eslint-disable-line max-len
            }` }, { test: (i, e) => i.type === "mat3" && i.size === 1 && !i.isArray && e.a !== void 0, code: (i) => `
            gl.uniformMatrix3fv(ud["${i}"].location, false, uv["${i}"].toArray(true));
            `, codeUbo: (i) => `
                var ${i}_matrix = uv.${i}.toArray(true);

                data[offset] = ${i}_matrix[0];
                data[offset+1] = ${i}_matrix[1];
                data[offset+2] = ${i}_matrix[2];
        
                data[offset + 4] = ${i}_matrix[3];
                data[offset + 5] = ${i}_matrix[4];
                data[offset + 6] = ${i}_matrix[5];
        
                data[offset + 8] = ${i}_matrix[6];
                data[offset + 9] = ${i}_matrix[7];
                data[offset + 10] = ${i}_matrix[8];
            ` }, { test: (i, e) => i.type === "vec2" && i.size === 1 && !i.isArray && e.x !== void 0, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v.x || cv[1] !== v.y)
                {
                    cv[0] = v.x;
                    cv[1] = v.y;
                    gl.uniform2f(ud["${i}"].location, v.x, v.y);
                }`, codeUbo: (i) => `
                v = uv.${i};

                data[offset] = v.x;
                data[offset+1] = v.y;
            ` }, { test: (i) => i.type === "vec2" && i.size === 1 && !i.isArray, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v[0] || cv[1] !== v[1])
                {
                    cv[0] = v[0];
                    cv[1] = v[1];
                    gl.uniform2f(ud["${i}"].location, v[0], v[1]);
                }
            ` }, { test: (i, e) => i.type === "vec4" && i.size === 1 && !i.isArray && e.width !== void 0, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v.x || cv[1] !== v.y || cv[2] !== v.width || cv[3] !== v.height)
                {
                    cv[0] = v.x;
                    cv[1] = v.y;
                    cv[2] = v.width;
                    cv[3] = v.height;
                    gl.uniform4f(ud["${i}"].location, v.x, v.y, v.width, v.height)
                }`, codeUbo: (i) => `
                    v = uv.${i};

                    data[offset] = v.x;
                    data[offset+1] = v.y;
                    data[offset+2] = v.width;
                    data[offset+3] = v.height;
                ` }, { test: (i, e) => i.type === "vec4" && i.size === 1 && !i.isArray && e.red !== void 0, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v.red || cv[1] !== v.green || cv[2] !== v.blue || cv[3] !== v.alpha)
                {
                    cv[0] = v.red;
                    cv[1] = v.green;
                    cv[2] = v.blue;
                    cv[3] = v.alpha;
                    gl.uniform4f(ud["${i}"].location, v.red, v.green, v.blue, v.alpha)
                }`, codeUbo: (i) => `
                    v = uv.${i};

                    data[offset] = v.red;
                    data[offset+1] = v.green;
                    data[offset+2] = v.blue;
                    data[offset+3] = v.alpha;
                ` }, { test: (i, e) => i.type === "vec3" && i.size === 1 && !i.isArray && e.red !== void 0, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v.red || cv[1] !== v.green || cv[2] !== v.blue || cv[3] !== v.a)
                {
                    cv[0] = v.red;
                    cv[1] = v.green;
                    cv[2] = v.blue;
    
                    gl.uniform3f(ud["${i}"].location, v.red, v.green, v.blue)
                }`, codeUbo: (i) => `
                    v = uv.${i};

                    data[offset] = v.red;
                    data[offset+1] = v.green;
                    data[offset+2] = v.blue;
                ` }, { test: (i) => i.type === "vec4" && i.size === 1 && !i.isArray, code: (i) => `
                cv = ud["${i}"].value;
                v = uv["${i}"];

                if(cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
                {
                    cv[0] = v[0];
                    cv[1] = v[1];
                    cv[2] = v[2];
                    cv[3] = v[3];

                    gl.uniform4f(ud["${i}"].location, v[0], v[1], v[2], v[3])
                }` }], wc = { float: `
    if (cv !== v)
    {
        cu.value = v;
        gl.uniform1f(location, v);
    }`, vec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2f(location, v[0], v[1])
    }`, vec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3f(location, v[0], v[1], v[2])
    }`, vec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4f(location, v[0], v[1], v[2], v[3]);
    }`, int: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`, ivec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2i(location, v[0], v[1]);
    }`, ivec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3i(location, v[0], v[1], v[2]);
    }`, ivec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4i(location, v[0], v[1], v[2], v[3]);
    }`, uint: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1ui(location, v);
    }`, uvec2: `
    if (cv[0] !== v[0] || cv[1] !== v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2ui(location, v[0], v[1]);
    }`, uvec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3ui(location, v[0], v[1], v[2]);
    }`, uvec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4ui(location, v[0], v[1], v[2], v[3]);
    }`, bool: `
    if (cv !== v)
    {
        cu.value = v;
        gl.uniform1i(location, v);
    }`, bvec2: `
    if (cv[0] != v[0] || cv[1] != v[1])
    {
        cv[0] = v[0];
        cv[1] = v[1];

        gl.uniform2i(location, v[0], v[1]);
    }`, bvec3: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];

        gl.uniform3i(location, v[0], v[1], v[2]);
    }`, bvec4: `
    if (cv[0] !== v[0] || cv[1] !== v[1] || cv[2] !== v[2] || cv[3] !== v[3])
    {
        cv[0] = v[0];
        cv[1] = v[1];
        cv[2] = v[2];
        cv[3] = v[3];

        gl.uniform4i(location, v[0], v[1], v[2], v[3]);
    }`, mat2: "gl.uniformMatrix2fv(location, false, v)", mat3: "gl.uniformMatrix3fv(location, false, v)", mat4: "gl.uniformMatrix4fv(location, false, v)", sampler2D: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`, samplerCube: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }`, sampler2DArray: `
    if (cv !== v)
    {
        cu.value = v;

        gl.uniform1i(location, v);
    }` }, Sc = { float: "gl.uniform1fv(location, v)", vec2: "gl.uniform2fv(location, v)", vec3: "gl.uniform3fv(location, v)", vec4: "gl.uniform4fv(location, v)", mat4: "gl.uniformMatrix4fv(location, false, v)", mat3: "gl.uniformMatrix3fv(location, false, v)", mat2: "gl.uniformMatrix2fv(location, false, v)", int: "gl.uniform1iv(location, v)", ivec2: "gl.uniform2iv(location, v)", ivec3: "gl.uniform3iv(location, v)", ivec4: "gl.uniform4iv(location, v)", uint: "gl.uniform1uiv(location, v)", uvec2: "gl.uniform2uiv(location, v)", uvec3: "gl.uniform3uiv(location, v)", uvec4: "gl.uniform4uiv(location, v)", bool: "gl.uniform1iv(location, v)", bvec2: "gl.uniform2iv(location, v)", bvec3: "gl.uniform3iv(location, v)", bvec4: "gl.uniform4iv(location, v)", sampler2D: "gl.uniform1iv(location, v)", samplerCube: "gl.uniform1iv(location, v)", sampler2DArray: "gl.uniform1iv(location, v)" };
function Ac(i, e) {
  const t = [`
        var v = null;
        var cv = null;
        var cu = null;
        var t = 0;
        var gl = renderer.gl;
    `];
  for (const r in i.uniforms) {
    const s = e[r];
    if (!s) {
      i.uniforms[r]?.group === true && (i.uniforms[r].ubo ? t.push(`
                        renderer.shader.syncUniformBufferGroup(uv.${r}, '${r}');
                    `) : t.push(`
                        renderer.shader.syncUniformGroup(uv.${r}, syncData);
                    `));
      continue;
    }
    const n = i.uniforms[r];
    let a = false;
    for (let o = 0; o < Yt.length; o++) if (Yt[o].test(s, n)) {
      t.push(Yt[o].code(r, n)), a = true;
      break;
    }
    if (!a) {
      const o = (s.size === 1 && !s.isArray ? wc : Sc)[s.type].replace("location", `ud["${r}"].location`);
      t.push(`
            cu = ud["${r}"];
            cv = cu.value;
            v = uv["${r}"];
            ${o};`);
    }
  }
  return new Function("ud", "uv", "renderer", "syncData", t.join(`
`));
}
const Zo = {};
let Fr = Zo;
function Ic() {
  if (Fr === Zo || Fr?.isContextLost()) {
    const i = he.ADAPTER.createCanvas();
    let e;
    he.PREFER_ENV >= Lt.WEBGL2 && (e = i.getContext("webgl2", {})), e || (e = i.getContext("webgl", {}) || i.getContext("experimental-webgl", {}), e ? e.getExtension("WEBGL_draw_buffers") : e = null), Fr = e;
  }
  return Fr;
}
let Nr;
function Rc() {
  if (!Nr) {
    Nr = Pe.MEDIUM;
    const i = Ic();
    if (i && i.getShaderPrecisionFormat) {
      const e = i.getShaderPrecisionFormat(i.FRAGMENT_SHADER, i.HIGH_FLOAT);
      e && (Nr = e.precision ? Pe.HIGH : Pe.MEDIUM);
    }
  }
  return Nr;
}
function no(i, e) {
  const t = i.getShaderSource(e).split(`
`).map((l, c) => `${c}: ${l}`), r = i.getShaderInfoLog(e), s = r.split(`
`), n = {}, a = s.map((l) => parseFloat(l.replace(/^ERROR\: 0\:([\d]+)\:.*$/, "$1"))).filter((l) => l && !n[l] ? (n[l] = true, true) : false), o = [""];
  a.forEach((l) => {
    t[l - 1] = `%c${t[l - 1]}%c`, o.push("background: #FF0000; color:#FFFFFF; font-size: 10px", "font-size: 10px");
  });
  const h = t.join(`
`);
  o[0] = h, console.error(r), console.groupCollapsed("click to view full shader code"), console.warn(...o), console.groupEnd();
}
function Cc(i, e, t, r) {
  i.getProgramParameter(e, i.LINK_STATUS) || (i.getShaderParameter(t, i.COMPILE_STATUS) || no(i, t), i.getShaderParameter(r, i.COMPILE_STATUS) || no(i, r), console.error("PixiJS Error: Could not initialize shader."), i.getProgramInfoLog(e) !== "" && console.warn("PixiJS Warning: gl.getProgramInfoLog()", i.getProgramInfoLog(e)));
}
const Mc = { float: 1, vec2: 2, vec3: 3, vec4: 4, int: 1, ivec2: 2, ivec3: 3, ivec4: 4, uint: 1, uvec2: 2, uvec3: 3, uvec4: 4, bool: 1, bvec2: 2, bvec3: 3, bvec4: 4, mat2: 4, mat3: 9, mat4: 16, sampler2D: 1 };
function Jo(i) {
  return Mc[i];
}
let Or = null;
const ao = { FLOAT: "float", FLOAT_VEC2: "vec2", FLOAT_VEC3: "vec3", FLOAT_VEC4: "vec4", INT: "int", INT_VEC2: "ivec2", INT_VEC3: "ivec3", INT_VEC4: "ivec4", UNSIGNED_INT: "uint", UNSIGNED_INT_VEC2: "uvec2", UNSIGNED_INT_VEC3: "uvec3", UNSIGNED_INT_VEC4: "uvec4", BOOL: "bool", BOOL_VEC2: "bvec2", BOOL_VEC3: "bvec3", BOOL_VEC4: "bvec4", FLOAT_MAT2: "mat2", FLOAT_MAT3: "mat3", FLOAT_MAT4: "mat4", SAMPLER_2D: "sampler2D", INT_SAMPLER_2D: "sampler2D", UNSIGNED_INT_SAMPLER_2D: "sampler2D", SAMPLER_CUBE: "samplerCube", INT_SAMPLER_CUBE: "samplerCube", UNSIGNED_INT_SAMPLER_CUBE: "samplerCube", SAMPLER_2D_ARRAY: "sampler2DArray", INT_SAMPLER_2D_ARRAY: "sampler2DArray", UNSIGNED_INT_SAMPLER_2D_ARRAY: "sampler2DArray" };
function Qo(i, e) {
  if (!Or) {
    const t = Object.keys(ao);
    Or = {};
    for (let r = 0; r < t.length; ++r) {
      const s = t[r];
      Or[i[s]] = ao[s];
    }
  }
  return Or[e];
}
function oo(i, e, t) {
  if (i.substring(0, 9) !== "precision") {
    let r = e;
    return e === Pe.HIGH && t !== Pe.HIGH && (r = Pe.MEDIUM), `precision ${r} float;
${i}`;
  } else if (t !== Pe.HIGH && i.substring(0, 15) === "precision highp") return i.replace("precision highp", "precision mediump");
  return i;
}
let ir;
function Lc() {
  if (typeof ir == "boolean") return ir;
  try {
    ir = new Function("param1", "param2", "param3", "return param1[param2] === param3;")({ a: "b" }, "a", "b") === true;
  } catch {
    ir = false;
  }
  return ir;
}
let Pc = 0;
const Br = {}, Vs = class Ht {
  constructor(e, t, r = "pixi-shader", s = {}) {
    this.extra = {}, this.id = Pc++, this.vertexSrc = e || Ht.defaultVertexSrc, this.fragmentSrc = t || Ht.defaultFragmentSrc, this.vertexSrc = this.vertexSrc.trim(), this.fragmentSrc = this.fragmentSrc.trim(), this.extra = s, this.vertexSrc.substring(0, 8) !== "#version" && (r = r.replace(/\s+/g, "-"), Br[r] ? (Br[r]++, r += `-${Br[r]}`) : Br[r] = 1, this.vertexSrc = `#define SHADER_NAME ${r}
${this.vertexSrc}`, this.fragmentSrc = `#define SHADER_NAME ${r}
${this.fragmentSrc}`, this.vertexSrc = oo(this.vertexSrc, Ht.defaultVertexPrecision, Pe.HIGH), this.fragmentSrc = oo(this.fragmentSrc, Ht.defaultFragmentPrecision, Rc())), this.glPrograms = {}, this.syncUniforms = null;
  }
  static get defaultVertexSrc() {
    return Ec;
  }
  static get defaultFragmentSrc() {
    return Tc;
  }
  static from(e, t, r) {
    const s = e + t;
    let n = eo[s];
    return n || (eo[s] = n = new Ht(e, t, r)), n;
  }
};
Vs.defaultVertexPrecision = Pe.HIGH, Vs.defaultFragmentPrecision = Vt.apple.device ? Pe.HIGH : Pe.MEDIUM;
let Tt = Vs, Fc = 0;
class Xe {
  constructor(e, t, r) {
    this.group = true, this.syncUniforms = {}, this.dirtyId = 0, this.id = Fc++, this.static = !!t, this.ubo = !!r, e instanceof Ie ? (this.buffer = e, this.buffer.type = We.UNIFORM_BUFFER, this.autoManage = false, this.ubo = true) : (this.uniforms = e, this.ubo && (this.buffer = new Ie(new Float32Array(1)), this.buffer.type = We.UNIFORM_BUFFER, this.autoManage = true));
  }
  update() {
    this.dirtyId++, !this.autoManage && this.buffer && this.buffer.update();
  }
  add(e, t, r) {
    if (!this.ubo) this.uniforms[e] = new Xe(t, r);
    else throw new Error("[UniformGroup] uniform groups in ubo mode cannot be modified, or have uniform groups nested in them");
  }
  static from(e, t, r) {
    return new Xe(e, t, r);
  }
  static uboFrom(e, t) {
    return new Xe(e, t ?? true, true);
  }
}
class ci {
  constructor(e, t) {
    this.uniformBindCount = 0, this.program = e, t ? t instanceof Xe ? this.uniformGroup = t : this.uniformGroup = new Xe(t) : this.uniformGroup = new Xe({}), this.disposeRunner = new De("disposeShader");
  }
  checkUniformExists(e, t) {
    if (t.uniforms[e]) return true;
    for (const r in t.uniforms) {
      const s = t.uniforms[r];
      if (s.group === true && this.checkUniformExists(e, s)) return true;
    }
    return false;
  }
  destroy() {
    this.uniformGroup = null, this.disposeRunner.emit(this), this.disposeRunner.destroy();
  }
  get uniforms() {
    return this.uniformGroup.uniforms;
  }
  static from(e, t, r) {
    const s = Tt.from(e, t);
    return new ci(s, r);
  }
}
class Nc {
  constructor(e, t) {
    if (this.vertexSrc = e, this.fragTemplate = t, this.programCache = {}, this.defaultGroupCache = {}, !t.includes("%count%")) throw new Error('Fragment template must contain "%count%".');
    if (!t.includes("%forloop%")) throw new Error('Fragment template must contain "%forloop%".');
  }
  generateShader(e) {
    if (!this.programCache[e]) {
      const r = new Int32Array(e);
      for (let n = 0; n < e; n++) r[n] = n;
      this.defaultGroupCache[e] = Xe.from({ uSamplers: r }, true);
      let s = this.fragTemplate;
      s = s.replace(/%count%/gi, `${e}`), s = s.replace(/%forloop%/gi, this.generateSampleSrc(e)), this.programCache[e] = new Tt(this.vertexSrc, s);
    }
    const t = { tint: new Float32Array([1, 1, 1, 1]), translationMatrix: new we(), default: this.defaultGroupCache[e] };
    return new ci(this.programCache[e], t);
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
}
class Oc {
  constructor() {
    this.elements = [], this.ids = [], this.count = 0;
  }
  clear() {
    for (let e = 0; e < this.count; e++) this.elements[e] = null;
    this.count = 0;
  }
}
function Bc() {
  return !Vt.apple.device;
}
function kc(i) {
  let e = true;
  const t = he.ADAPTER.getNavigator();
  if (Vt.tablet || Vt.phone) {
    if (Vt.apple.device) {
      const r = t.userAgent.match(/OS (\d+)_(\d+)?/);
      r && parseInt(r[1], 10) < 11 && (e = false);
    }
    if (Vt.android.device) {
      const r = t.userAgent.match(/Android\s([0-9.]*)/);
      r && parseInt(r[1], 10) < 7 && (e = false);
    }
  }
  return e ? i : 4;
}
class eh {
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
}
var Uc = `varying vec2 vTextureCoord;
varying vec4 vColor;
varying float vTextureId;
uniform sampler2D uSamplers[%count%];

void main(void){
    vec4 color;
    %forloop%
    gl_FragColor = color * vColor;
}
`, Dc = `precision highp float;
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
`;
const lr = class Ue extends eh {
  constructor(e) {
    super(e), this.setShaderGenerator(), this.geometryClass = gc, this.vertexSize = 6, this.state = er.for2d(), this.size = Ue.defaultBatchSize * 4, this._vertexCount = 0, this._indexCount = 0, this._bufferedElements = [], this._bufferedTextures = [], this._bufferSize = 0, this._shader = null, this._packedGeometries = [], this._packedGeometryPoolSize = 2, this._flushId = 0, this._aBuffers = {}, this._iBuffers = {}, this.maxTextures = 1, this.renderer.on("prerender", this.onPrerender, this), e.runners.contextChange.add(this), this._dcIndex = 0, this._aIndex = 0, this._iIndex = 0, this._attributeBuffer = null, this._indexBuffer = null, this._tempBoundTextures = [];
  }
  static get defaultMaxTextures() {
    return this._defaultMaxTextures = this._defaultMaxTextures ?? kc(32), this._defaultMaxTextures;
  }
  static set defaultMaxTextures(e) {
    this._defaultMaxTextures = e;
  }
  static get canUploadSameBuffer() {
    return this._canUploadSameBuffer = this._canUploadSameBuffer ?? Bc(), this._canUploadSameBuffer;
  }
  static set canUploadSameBuffer(e) {
    this._canUploadSameBuffer = e;
  }
  get MAX_TEXTURES() {
    return fe("7.1.0", "BatchRenderer#MAX_TEXTURES renamed to BatchRenderer#maxTextures"), this.maxTextures;
  }
  static get defaultVertexSrc() {
    return Dc;
  }
  static get defaultFragmentTemplate() {
    return Uc;
  }
  setShaderGenerator({ vertex: e = Ue.defaultVertexSrc, fragment: t = Ue.defaultFragmentTemplate } = {}) {
    this.shaderGenerator = new Nc(e, t);
  }
  contextChange() {
    const e = this.renderer.gl;
    he.PREFER_ENV === Lt.WEBGL_LEGACY ? this.maxTextures = 1 : (this.maxTextures = Math.min(e.getParameter(e.MAX_TEXTURE_IMAGE_UNITS), Ue.defaultMaxTextures), this.maxTextures = lc(this.maxTextures, e)), this._shader = this.shaderGenerator.generateShader(this.maxTextures);
    for (let t = 0; t < this._packedGeometryPoolSize; t++) this._packedGeometries[t] = new this.geometryClass();
    this.initFlushBuffers();
  }
  initFlushBuffers() {
    const { _drawCallPool: e, _textureArrayPool: t } = Ue, r = this.size / 4, s = Math.floor(r / this.maxTextures) + 1;
    for (; e.length < r; ) e.push(new uc());
    for (; t.length < s; ) t.push(new Oc());
    for (let n = 0; n < this.maxTextures; n++) this._tempBoundTextures[n] = null;
  }
  onPrerender() {
    this._flushId = 0;
  }
  render(e) {
    e._texture.valid && (this._vertexCount + e.vertexData.length / 2 > this.size && this.flush(), this._vertexCount += e.vertexData.length / 2, this._indexCount += e.indices.length, this._bufferedTextures[this._bufferSize] = e._texture.baseTexture, this._bufferedElements[this._bufferSize++] = e);
  }
  buildTexturesAndDrawCalls() {
    const { _bufferedTextures: e, maxTextures: t } = this, r = Ue._textureArrayPool, s = this.renderer.batch, n = this._tempBoundTextures, a = this.renderer.textureGC.count;
    let o = ++pe._globalBatch, h = 0, l = r[0], c = 0;
    s.copyBoundTextures(n, t);
    for (let u = 0; u < this._bufferSize; ++u) {
      const m = e[u];
      e[u] = null, m._batchEnabled !== o && (l.count >= t && (s.boundArray(l, n, o, t), this.buildDrawCalls(l, c, u), c = u, l = r[++h], ++o), m._batchEnabled = o, m.touched = a, l.elements[l.count++] = m);
    }
    l.count > 0 && (s.boundArray(l, n, o, t), this.buildDrawCalls(l, c, this._bufferSize), ++h, ++o);
    for (let u = 0; u < n.length; u++) n[u] = null;
    pe._globalBatch = o;
  }
  buildDrawCalls(e, t, r) {
    const { _bufferedElements: s, _attributeBuffer: n, _indexBuffer: a, vertexSize: o } = this, h = Ue._drawCallPool;
    let l = this._dcIndex, c = this._aIndex, u = this._iIndex, m = h[l];
    m.start = this._iIndex, m.texArray = e;
    for (let y = t; y < r; ++y) {
      const d = s[y], p = d._texture.baseTexture, f = ic[p.alphaMode ? 1 : 0][d.blendMode];
      s[y] = null, t < y && m.blend !== f && (m.size = u - m.start, t = y, m = h[++l], m.texArray = e, m.start = u), this.packInterleavedGeometry(d, n, a, c, u), c += d.vertexData.length / 2 * o, u += d.indices.length, m.blend = f;
    }
    t < r && (m.size = u - m.start, ++l), this._dcIndex = l, this._aIndex = c, this._iIndex = u;
  }
  bindAndClearTexArray(e) {
    const t = this.renderer.texture;
    for (let r = 0; r < e.count; r++) t.bind(e.elements[r], e.ids[r]), e.elements[r] = null;
    e.count = 0;
  }
  updateGeometry() {
    const { _packedGeometries: e, _attributeBuffer: t, _indexBuffer: r } = this;
    Ue.canUploadSameBuffer ? (e[this._flushId]._buffer.update(t.rawBinaryData), e[this._flushId]._indexBuffer.update(r), this.renderer.geometry.updateBuffers()) : (this._packedGeometryPoolSize <= this._flushId && (this._packedGeometryPoolSize++, e[this._flushId] = new this.geometryClass()), e[this._flushId]._buffer.update(t.rawBinaryData), e[this._flushId]._indexBuffer.update(r), this.renderer.geometry.bind(e[this._flushId]), this.renderer.geometry.updateBuffers(), this._flushId++);
  }
  drawBatches() {
    const e = this._dcIndex, { gl: t, state: r } = this.renderer, s = Ue._drawCallPool;
    let n = null;
    for (let a = 0; a < e; a++) {
      const { texArray: o, type: h, size: l, start: c, blend: u } = s[a];
      n !== o && (n = o, this.bindAndClearTexArray(o)), this.state.blendMode = u, r.set(this.state), t.drawElements(h, l, t.UNSIGNED_SHORT, c * 2);
    }
  }
  flush() {
    this._vertexCount !== 0 && (this._attributeBuffer = this.getAttributeBuffer(this._vertexCount), this._indexBuffer = this.getIndexBuffer(this._indexCount), this._aIndex = 0, this._iIndex = 0, this._dcIndex = 0, this.buildTexturesAndDrawCalls(), this.updateGeometry(), this.drawBatches(), this._bufferSize = 0, this._vertexCount = 0, this._indexCount = 0);
  }
  start() {
    this.renderer.state.set(this.state), this.renderer.texture.ensureSamplerType(this.maxTextures), this.renderer.shader.bind(this._shader), Ue.canUploadSameBuffer && this.renderer.geometry.bind(this._packedGeometries[this._flushId]);
  }
  stop() {
    this.flush();
  }
  destroy() {
    for (let e = 0; e < this._packedGeometryPoolSize; e++) this._packedGeometries[e] && this._packedGeometries[e].destroy();
    this.renderer.off("prerender", this.onPrerender, this), this._aBuffers = null, this._iBuffers = null, this._packedGeometries = null, this._attributeBuffer = null, this._indexBuffer = null, this._shader && (this._shader.destroy(), this._shader = null), super.destroy();
  }
  getAttributeBuffer(e) {
    const t = Zr(Math.ceil(e / 8)), r = Qa(t), s = t * 8;
    this._aBuffers.length <= r && (this._iBuffers.length = r + 1);
    let n = this._aBuffers[s];
    return n || (this._aBuffers[s] = n = new ac(s * this.vertexSize * 4)), n;
  }
  getIndexBuffer(e) {
    const t = Zr(Math.ceil(e / 12)), r = Qa(t), s = t * 12;
    this._iBuffers.length <= r && (this._iBuffers.length = r + 1);
    let n = this._iBuffers[r];
    return n || (this._iBuffers[r] = n = new Uint16Array(s)), n;
  }
  packInterleavedGeometry(e, t, r, s, n) {
    const { uint32View: a, float32View: o } = t, h = s / this.vertexSize, l = e.uvs, c = e.indices, u = e.vertexData, m = e._texture.baseTexture._batchLocation, y = Math.min(e.worldAlpha, 1), d = Rt.shared.setValue(e._tintRGB).toPremultiplied(y, e._texture.baseTexture.alphaMode > 0);
    for (let p = 0; p < u.length; p += 2) o[s++] = u[p], o[s++] = u[p + 1], o[s++] = l[p], o[s++] = l[p + 1], a[s++] = d, o[s++] = m;
    for (let p = 0; p < c.length; p++) r[n++] = h + c[p];
  }
};
lr.defaultBatchSize = 4096, lr.extension = { name: "batch", type: ne.RendererPlugin }, lr._drawCallPool = [], lr._textureArrayPool = [];
let xt = lr;
de.add(xt);
var Gc = `varying vec2 vTextureCoord;

uniform sampler2D uSampler;

void main(void){
   gl_FragColor = texture2D(uSampler, vTextureCoord);
}
`, zc = `attribute vec2 aVertexPosition;

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
`;
const Ws = class cr extends ci {
  constructor(e, t, r) {
    const s = Tt.from(e || cr.defaultVertexSrc, t || cr.defaultFragmentSrc);
    super(s, r), this.padding = 0, this.resolution = cr.defaultResolution, this.multisample = cr.defaultMultisample, this.enabled = true, this.autoFit = true, this.state = new er();
  }
  apply(e, t, r, s, n) {
    e.applyFilter(this, t, r, s);
  }
  get blendMode() {
    return this.state.blendMode;
  }
  set blendMode(e) {
    this.state.blendMode = e;
  }
  get resolution() {
    return this._resolution;
  }
  set resolution(e) {
    this._resolution = e;
  }
  static get defaultVertexSrc() {
    return zc;
  }
  static get defaultFragmentSrc() {
    return Gc;
  }
};
Ws.defaultResolution = 1, Ws.defaultMultisample = _e.NONE;
let Ne = Ws;
class Qr {
  constructor() {
    this.clearBeforeRender = true, this._backgroundColor = new Rt(0), this.alpha = 1;
  }
  init(e) {
    this.clearBeforeRender = e.clearBeforeRender;
    const { backgroundColor: t, background: r, backgroundAlpha: s } = e, n = r ?? t;
    n !== void 0 && (this.color = n), this.alpha = s;
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
}
Qr.defaultOptions = { backgroundAlpha: 1, backgroundColor: 0, clearBeforeRender: true }, Qr.extension = { type: [ne.RendererSystem, ne.CanvasRendererSystem], name: "background" };
de.add(Qr);
class th {
  constructor(e) {
    this.renderer = e, this.emptyRenderer = new eh(e), this.currentRenderer = this.emptyRenderer;
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
    const { elements: n, ids: a, count: o } = e;
    let h = 0;
    for (let l = 0; l < o; l++) {
      const c = n[l], u = c._batchLocation;
      if (u >= 0 && u < s && t[u] === c) {
        a[l] = u;
        continue;
      }
      for (; h < s; ) {
        const m = t[h];
        if (m && m._batchEnabled === r && m._batchLocation === h) {
          h++;
          continue;
        }
        a[l] = h, c._batchLocation = h, t[h] = c;
        break;
      }
    }
  }
  destroy() {
    this.renderer = null;
  }
}
th.extension = { type: ne.RendererSystem, name: "batch" };
de.add(th);
let ho = 0;
class ei {
  constructor(e) {
    this.renderer = e, this.webGLVersion = 1, this.extensions = {}, this.supports = { uint32Indices: false }, this.handleContextLost = this.handleContextLost.bind(this), this.handleContextRestored = this.handleContextRestored.bind(this);
  }
  get isLost() {
    return !this.gl || this.gl.isContextLost();
  }
  contextChange(e) {
    this.gl = e, this.renderer.gl = e, this.renderer.CONTEXT_UID = ho++;
  }
  init(e) {
    if (e.context) this.initFromContext(e.context);
    else {
      const t = this.renderer.background.alpha < 1, r = e.premultipliedAlpha;
      this.preserveDrawingBuffer = e.preserveDrawingBuffer, this.useContextAlpha = e.useContextAlpha, this.powerPreference = e.powerPreference, this.initFromOptions({ alpha: t, premultipliedAlpha: r, antialias: e.antialias, stencil: true, preserveDrawingBuffer: e.preserveDrawingBuffer, powerPreference: e.powerPreference });
    }
  }
  initFromContext(e) {
    this.gl = e, this.validateContext(e), this.renderer.gl = e, this.renderer.CONTEXT_UID = ho++, this.renderer.runners.contextChange.emit(e);
    const t = this.renderer.view;
    t.addEventListener !== void 0 && (t.addEventListener("webglcontextlost", this.handleContextLost, false), t.addEventListener("webglcontextrestored", this.handleContextRestored, false));
  }
  initFromOptions(e) {
    const t = this.createContext(this.renderer.view, e);
    this.initFromContext(t);
  }
  createContext(e, t) {
    let r;
    if (he.PREFER_ENV >= Lt.WEBGL2 && (r = e.getContext("webgl2", t)), r) this.webGLVersion = 2;
    else if (this.webGLVersion = 1, r = e.getContext("webgl", t) || e.getContext("experimental-webgl", t), !r) throw new Error("This browser does not support WebGL. Try using the canvas renderer");
    return this.gl = r, this.getExtensions(), this.gl;
  }
  getExtensions() {
    const { gl: e } = this, t = { loseContext: e.getExtension("WEBGL_lose_context"), anisotropicFiltering: e.getExtension("EXT_texture_filter_anisotropic"), floatTextureLinear: e.getExtension("OES_texture_float_linear"), s3tc: e.getExtension("WEBGL_compressed_texture_s3tc"), s3tc_sRGB: e.getExtension("WEBGL_compressed_texture_s3tc_srgb"), etc: e.getExtension("WEBGL_compressed_texture_etc"), etc1: e.getExtension("WEBGL_compressed_texture_etc1"), pvrtc: e.getExtension("WEBGL_compressed_texture_pvrtc") || e.getExtension("WEBKIT_WEBGL_compressed_texture_pvrtc"), atc: e.getExtension("WEBGL_compressed_texture_atc"), astc: e.getExtension("WEBGL_compressed_texture_astc"), bptc: e.getExtension("EXT_texture_compression_bptc") };
    this.webGLVersion === 1 ? Object.assign(this.extensions, t, { drawBuffers: e.getExtension("WEBGL_draw_buffers"), depthTexture: e.getExtension("WEBGL_depth_texture"), vertexArrayObject: e.getExtension("OES_vertex_array_object") || e.getExtension("MOZ_OES_vertex_array_object") || e.getExtension("WEBKIT_OES_vertex_array_object"), uint32ElementIndex: e.getExtension("OES_element_index_uint"), floatTexture: e.getExtension("OES_texture_float"), floatTextureLinear: e.getExtension("OES_texture_float_linear"), textureHalfFloat: e.getExtension("OES_texture_half_float"), textureHalfFloatLinear: e.getExtension("OES_texture_half_float_linear") }) : this.webGLVersion === 2 && Object.assign(this.extensions, t, { colorBufferFloat: e.getExtension("EXT_color_buffer_float") });
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
}
ei.defaultOptions = { context: null, antialias: false, premultipliedAlpha: true, preserveDrawingBuffer: false, powerPreference: "default" }, ei.extension = { type: ne.RendererSystem, name: "context" };
de.add(ei);
class Xs {
  constructor(e, t) {
    if (this.width = Math.round(e), this.height = Math.round(t), !this.width || !this.height) throw new Error("Framebuffer width or height is zero");
    this.stencil = false, this.depth = false, this.dirtyId = 0, this.dirtyFormat = 0, this.dirtySize = 0, this.depthTexture = null, this.colorTextures = [], this.glFramebuffers = {}, this.disposeRunner = new De("disposeFramebuffer"), this.multisample = _e.NONE;
  }
  get colorTexture() {
    return this.colorTextures[0];
  }
  addColorTexture(e = 0, t) {
    return this.colorTextures[e] = t || new pe(null, { scaleMode: Ze.NEAREST, resolution: 1, mipmap: It.OFF, width: this.width, height: this.height }), this.dirtyId++, this.dirtyFormat++, this;
  }
  addDepthTexture(e) {
    return this.depthTexture = e || new pe(null, { scaleMode: Ze.NEAREST, resolution: 1, width: this.width, height: this.height, mipmap: It.OFF, format: W.DEPTH_COMPONENT, type: se.UNSIGNED_SHORT }), this.dirtyId++, this.dirtyFormat++, this;
  }
  enableDepth() {
    return this.depth = true, this.dirtyId++, this.dirtyFormat++, this;
  }
  enableStencil() {
    return this.stencil = true, this.dirtyId++, this.dirtyFormat++, this;
  }
  resize(e, t) {
    if (e = Math.round(e), t = Math.round(t), !e || !t) throw new Error("Framebuffer width and height must not be zero");
    if (!(e === this.width && t === this.height)) {
      this.width = e, this.height = t, this.dirtyId++, this.dirtySize++;
      for (let r = 0; r < this.colorTextures.length; r++) {
        const s = this.colorTextures[r], n = s.resolution;
        s.setSize(e / n, t / n);
      }
      if (this.depthTexture) {
        const r = this.depthTexture.resolution;
        this.depthTexture.setSize(e / r, t / r);
      }
    }
  }
  dispose() {
    this.disposeRunner.emit(this, false);
  }
  destroyDepthTexture() {
    this.depthTexture && (this.depthTexture.destroy(), this.depthTexture = null, ++this.dirtyId, ++this.dirtyFormat);
  }
}
class rh extends pe {
  constructor(e = {}) {
    if (typeof e == "number") {
      const t = arguments[0], r = arguments[1], s = arguments[2], n = arguments[3];
      e = { width: t, height: r, scaleMode: s, resolution: n };
    }
    e.width = e.width ?? 100, e.height = e.height ?? 100, e.multisample ?? (e.multisample = _e.NONE), super(null, e), this.mipmap = It.OFF, this.valid = true, this._clear = new Rt([0, 0, 0, 0]), this.framebuffer = new Xs(this.realWidth, this.realHeight).addColorTexture(0, this), this.framebuffer.multisample = e.multisample, this.maskStack = [], this.filterStack = [{}];
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
}
class Qe extends vr {
  constructor(e) {
    const t = e, r = t.naturalWidth || t.videoWidth || t.displayWidth || t.width, s = t.naturalHeight || t.videoHeight || t.displayHeight || t.height;
    super(r, s), this.source = e, this.noSubImage = false;
  }
  static crossOrigin(e, t, r) {
    r === void 0 && !t.startsWith("data:") ? e.crossOrigin = nc(t) : r !== false && (e.crossOrigin = typeof r == "string" ? r : "anonymous");
  }
  upload(e, t, r, s) {
    const n = e.gl, a = t.realWidth, o = t.realHeight;
    if (s = s || this.source, typeof HTMLImageElement < "u" && s instanceof HTMLImageElement) {
      if (!s.complete || s.naturalWidth === 0) return false;
    } else if (typeof HTMLVideoElement < "u" && s instanceof HTMLVideoElement && s.readyState <= 1) return false;
    return n.pixelStorei(n.UNPACK_PREMULTIPLY_ALPHA_WEBGL, t.alphaMode === Pt.UNPACK), !this.noSubImage && t.target === n.TEXTURE_2D && r.width === a && r.height === o ? n.texSubImage2D(n.TEXTURE_2D, 0, 0, 0, t.format, r.type, s) : (r.width = a, r.height = o, n.texImage2D(t.target, 0, r.internalFormat, t.format, r.type, s)), true;
  }
  update() {
    if (this.destroyed) return;
    const e = this.source, t = e.naturalWidth || e.videoWidth || e.width, r = e.naturalHeight || e.videoHeight || e.height;
    this.resize(t, r), super.update();
  }
  dispose() {
    this.source = null;
  }
}
class ih extends Qe {
  constructor(e, t) {
    if (t = t || {}, typeof e == "string") {
      const r = new Image();
      Qe.crossOrigin(r, e, t.crossorigin), r.src = e, e = r;
    }
    super(e), !e.complete && this._width && this._height && (this._width = 0, this._height = 0), this.url = e.src, this._process = null, this.preserveBitmap = false, this.createBitmap = (t.createBitmap ?? he.CREATE_IMAGE_BITMAP) && !!globalThis.createImageBitmap, this.alphaMode = typeof t.alphaMode == "number" ? t.alphaMode : null, this.bitmap = null, this._load = null, t.autoLoad !== false && this.load();
  }
  load(e) {
    return this._load ? this._load : (e !== void 0 && (this.createBitmap = e), this._load = new Promise((t, r) => {
      const s = this.source;
      this.url = s.src;
      const n = () => {
        this.destroyed || (s.onload = null, s.onerror = null, this.update(), this._load = null, this.createBitmap ? t(this.process()) : t(this));
      };
      s.complete && s.src ? n() : (s.onload = n, s.onerror = (a) => {
        r(a), this.onError.emit(a);
      });
    }), this._load);
  }
  process() {
    const e = this.source;
    if (this._process !== null) return this._process;
    if (this.bitmap !== null || !globalThis.createImageBitmap) return Promise.resolve(this);
    const t = globalThis.createImageBitmap, r = !e.crossOrigin || e.crossOrigin === "anonymous";
    return this._process = fetch(e.src, { mode: r ? "cors" : "no-cors" }).then((s) => s.blob()).then((s) => t(s, 0, 0, e.width, e.height, { premultiplyAlpha: this.alphaMode === null || this.alphaMode === Pt.UNPACK ? "premultiply" : "none" })).then((s) => this.destroyed ? Promise.reject() : (this.bitmap = s, this.update(), this._process = null, Promise.resolve(this))), this._process;
  }
  upload(e, t, r) {
    if (typeof this.alphaMode == "number" && (t.alphaMode = this.alphaMode), !this.createBitmap) return super.upload(e, t, r);
    if (!this.bitmap && (this.process(), !this.bitmap)) return false;
    if (super.upload(e, t, r, this.bitmap), !this.preserveBitmap) {
      let s = true;
      const n = t._glTextures;
      for (const a in n) {
        const o = n[a];
        if (o !== r && o.dirtyId !== t.dirtyId) {
          s = false;
          break;
        }
      }
      s && (this.bitmap.close && this.bitmap.close(), this.bitmap = null);
    }
    return true;
  }
  dispose() {
    this.source.onload = null, this.source.onerror = null, super.dispose(), this.bitmap && (this.bitmap.close(), this.bitmap = null), this._process = null, this._load = null;
  }
  static test(e) {
    return typeof HTMLImageElement < "u" && (typeof e == "string" || e instanceof HTMLImageElement);
  }
}
class fn {
  constructor() {
    this.x0 = 0, this.y0 = 0, this.x1 = 1, this.y1 = 0, this.x2 = 1, this.y2 = 1, this.x3 = 0, this.y3 = 1, this.uvsFloat32 = new Float32Array(8);
  }
  set(e, t, r) {
    const s = t.width, n = t.height;
    if (r) {
      const a = e.width / 2 / s, o = e.height / 2 / n, h = e.x / s + a, l = e.y / n + o;
      r = xe.add(r, xe.NW), this.x0 = h + a * xe.uX(r), this.y0 = l + o * xe.uY(r), r = xe.add(r, 2), this.x1 = h + a * xe.uX(r), this.y1 = l + o * xe.uY(r), r = xe.add(r, 2), this.x2 = h + a * xe.uX(r), this.y2 = l + o * xe.uY(r), r = xe.add(r, 2), this.x3 = h + a * xe.uX(r), this.y3 = l + o * xe.uY(r);
    } else this.x0 = e.x / s, this.y0 = e.y / n, this.x1 = (e.x + e.width) / s, this.y1 = e.y / n, this.x2 = (e.x + e.width) / s, this.y2 = (e.y + e.height) / n, this.x3 = e.x / s, this.y3 = (e.y + e.height) / n;
    this.uvsFloat32[0] = this.x0, this.uvsFloat32[1] = this.y0, this.uvsFloat32[2] = this.x1, this.uvsFloat32[3] = this.y1, this.uvsFloat32[4] = this.x2, this.uvsFloat32[5] = this.y2, this.uvsFloat32[6] = this.x3, this.uvsFloat32[7] = this.y3;
  }
}
fn.prototype.toString = function() {
  return `[@pixi/core:TextureUvs x0=${this.x0} y0=${this.y0} x1=${this.x1} y1=${this.y1} x2=${this.x2} y2=${this.y2} x3=${this.x3} y3=${this.y3}]`;
};
const lo = new fn();
function kr(i) {
  i.destroy = function() {
  }, i.on = function() {
  }, i.once = function() {
  }, i.emit = function() {
  };
}
class ce extends oi {
  constructor(e, t, r, s, n, a, o) {
    if (super(), this.noFrame = false, t || (this.noFrame = true, t = new me(0, 0, 1, 1)), e instanceof ce && (e = e.baseTexture), this.baseTexture = e, this._frame = t, this.trim = s, this.valid = false, this.destroyed = false, this._uvs = lo, this.uvMatrix = null, this.orig = r || t, this._rotate = Number(n || 0), n === true) this._rotate = 2;
    else if (this._rotate % 2 !== 0) throw new Error("attempt to use diamond-shaped UVs. If you are sure, set rotation manually");
    this.defaultAnchor = a ? new Ae(a.x, a.y) : new Ae(0, 0), this.defaultBorders = o, this._updateID = 0, this.textureCacheIds = [], e.valid ? this.noFrame ? e.valid && this.onBaseTextureUpdated(e) : this.frame = t : e.once("loaded", this.onBaseTextureUpdated, this), this.noFrame && e.on("update", this.onBaseTextureUpdated, this);
  }
  update() {
    this.baseTexture.resource && this.baseTexture.resource.update();
  }
  onBaseTextureUpdated(e) {
    if (this.noFrame) {
      if (!this.baseTexture.valid) return;
      this._frame.width = e.width, this._frame.height = e.height, this.valid = true, this.updateUvs();
    } else this.frame = this._frame;
    this.emit("update", this);
  }
  destroy(e) {
    if (this.baseTexture) {
      if (e) {
        const { resource: t } = this.baseTexture;
        t?.url && $e[t.url] && ce.removeFromCache(t.url), this.baseTexture.destroy();
      }
      this.baseTexture.off("loaded", this.onBaseTextureUpdated, this), this.baseTexture.off("update", this.onBaseTextureUpdated, this), this.baseTexture = null;
    }
    this._frame = null, this._uvs = null, this.trim = null, this.orig = null, this.valid = false, ce.removeFromCache(this), this.textureCacheIds = null, this.destroyed = true, this.emit("destroyed", this), this.removeAllListeners();
  }
  clone() {
    const e = this._frame.clone(), t = this._frame === this.orig ? e : this.orig.clone(), r = new ce(this.baseTexture, !this.noFrame && e, t, this.trim?.clone(), this.rotate, this.defaultAnchor, this.defaultBorders);
    return this.noFrame && (r._frame = e), r;
  }
  updateUvs() {
    this._uvs === lo && (this._uvs = new fn()), this._uvs.set(this._frame, this.baseTexture, this.rotate), this._updateID++;
  }
  static from(e, t = {}, r = he.STRICT_TEXTURE_CACHE) {
    const s = typeof e == "string";
    let n = null;
    if (s) n = e;
    else if (e instanceof pe) {
      if (!e.cacheId) {
        const o = t?.pixiIdPrefix || "pixiid";
        e.cacheId = `${o}-${gr()}`, pe.addToCache(e, e.cacheId);
      }
      n = e.cacheId;
    } else {
      if (!e._pixiId) {
        const o = t?.pixiIdPrefix || "pixiid";
        e._pixiId = `${o}_${gr()}`;
      }
      n = e._pixiId;
    }
    let a = $e[n];
    if (s && r && !a) throw new Error(`The cacheId "${n}" does not exist in TextureCache.`);
    return !a && !(e instanceof pe) ? (t.resolution || (t.resolution = to(e)), a = new ce(new pe(e, t)), a.baseTexture.cacheId = n, pe.addToCache(a.baseTexture, n), ce.addToCache(a, n)) : !a && e instanceof pe && (a = new ce(e), ce.addToCache(a, n)), a;
  }
  static fromURL(e, t) {
    const r = Object.assign({ autoLoad: false }, t?.resourceOptions), s = ce.from(e, Object.assign({ resourceOptions: r }, t), false), n = s.baseTexture.resource;
    return s.baseTexture.valid ? Promise.resolve(s) : n.load().then(() => Promise.resolve(s));
  }
  static fromBuffer(e, t, r, s) {
    return new ce(pe.fromBuffer(e, t, r, s));
  }
  static fromLoader(e, t, r, s) {
    const n = new pe(e, Object.assign({ scaleMode: pe.defaultOptions.scaleMode, resolution: to(t) }, s)), { resource: a } = n;
    a instanceof ih && (a.url = t);
    const o = new ce(n);
    return r || (r = t), pe.addToCache(o.baseTexture, r), ce.addToCache(o, r), r !== t && (pe.addToCache(o.baseTexture, t), ce.addToCache(o, t)), o.baseTexture.valid ? Promise.resolve(o) : new Promise((h) => {
      o.baseTexture.once("loaded", () => h(o));
    });
  }
  static addToCache(e, t) {
    t && (e.textureCacheIds.includes(t) || e.textureCacheIds.push(t), $e[t] && $e[t] !== e && console.warn(`Texture added to the cache with an id [${t}] that already had an entry`), $e[t] = e);
  }
  static removeFromCache(e) {
    if (typeof e == "string") {
      const t = $e[e];
      if (t) {
        const r = t.textureCacheIds.indexOf(e);
        return r > -1 && t.textureCacheIds.splice(r, 1), delete $e[e], t;
      }
    } else if (e?.textureCacheIds) {
      for (let t = 0; t < e.textureCacheIds.length; ++t) $e[e.textureCacheIds[t]] === e && delete $e[e.textureCacheIds[t]];
      return e.textureCacheIds.length = 0, e;
    }
    return null;
  }
  get resolution() {
    return this.baseTexture.resolution;
  }
  get frame() {
    return this._frame;
  }
  set frame(e) {
    this._frame = e, this.noFrame = false;
    const { x: t, y: r, width: s, height: n } = e, a = t + s > this.baseTexture.width, o = r + n > this.baseTexture.height;
    if (a || o) {
      const h = a && o ? "and" : "or", l = `X: ${t} + ${s} = ${t + s} > ${this.baseTexture.width}`, c = `Y: ${r} + ${n} = ${r + n} > ${this.baseTexture.height}`;
      throw new Error(`Texture Error: frame does not fit inside the base Texture dimensions: ${l} ${h} ${c}`);
    }
    this.valid = s && n && this.baseTexture.valid, !this.trim && !this.rotate && (this.orig = e), this.valid && this.updateUvs();
  }
  get rotate() {
    return this._rotate;
  }
  set rotate(e) {
    this._rotate = e, this.valid && this.updateUvs();
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
    return ce._EMPTY || (ce._EMPTY = new ce(new pe()), kr(ce._EMPTY), kr(ce._EMPTY.baseTexture)), ce._EMPTY;
  }
  static get WHITE() {
    if (!ce._WHITE) {
      const e = he.ADAPTER.createCanvas(16, 16), t = e.getContext("2d");
      e.width = 16, e.height = 16, t.fillStyle = "white", t.fillRect(0, 0, 16, 16), ce._WHITE = new ce(pe.from(e)), kr(ce._WHITE), kr(ce._WHITE.baseTexture);
    }
    return ce._WHITE;
  }
}
class ui extends ce {
  constructor(e, t) {
    super(e, t), this.valid = true, this.filterFrame = null, this.filterPoolKey = null, this.updateUvs();
  }
  get framebuffer() {
    return this.baseTexture.framebuffer;
  }
  get multisample() {
    return this.framebuffer.multisample;
  }
  set multisample(e) {
    this.framebuffer.multisample = e;
  }
  resize(e, t, r = true) {
    const s = this.baseTexture.resolution, n = Math.round(e * s) / s, a = Math.round(t * s) / s;
    this.valid = n > 0 && a > 0, this._frame.width = this.orig.width = n, this._frame.height = this.orig.height = a, r && this.baseTexture.resize(n, a), this.updateUvs();
  }
  setResolution(e) {
    const { baseTexture: t } = this;
    t.resolution !== e && (t.setResolution(e), this.resize(t.width, t.height, false));
  }
  static create(e) {
    return new ui(new rh(e));
  }
}
class sh {
  constructor(e) {
    this.texturePool = {}, this.textureOptions = e || {}, this.enableFullScreen = false, this._pixelsWidth = 0, this._pixelsHeight = 0;
  }
  createTexture(e, t, r = _e.NONE) {
    const s = new rh(Object.assign({ width: e, height: t, resolution: 1, multisample: r }, this.textureOptions));
    return new ui(s);
  }
  getOptimalTexture(e, t, r = 1, s = _e.NONE) {
    let n;
    e = Math.max(Math.ceil(e * r - 1e-6), 1), t = Math.max(Math.ceil(t * r - 1e-6), 1), !this.enableFullScreen || e !== this._pixelsWidth || t !== this._pixelsHeight ? (e = Zr(e), t = Zr(t), n = ((e & 65535) << 16 | t & 65535) >>> 0, s > 1 && (n += s * 4294967296)) : n = s > 1 ? -s : -1, this.texturePool[n] || (this.texturePool[n] = []);
    let a = this.texturePool[n].pop();
    return a || (a = this.createTexture(e, t, s)), a.filterPoolKey = n, a.setResolution(r), a;
  }
  getFilterTexture(e, t, r) {
    const s = this.getOptimalTexture(e.width, e.height, t || e.resolution, r || _e.NONE);
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
    if (e = e !== false, e) for (const t in this.texturePool) {
      const r = this.texturePool[t];
      if (r) for (let s = 0; s < r.length; s++) r[s].destroy(true);
    }
    this.texturePool = {};
  }
  setScreenSize(e) {
    if (!(e.width === this._pixelsWidth && e.height === this._pixelsHeight)) {
      this.enableFullScreen = e.width > 0 && e.height > 0;
      for (const t in this.texturePool) {
        if (!(Number(t) < 0)) continue;
        const r = this.texturePool[t];
        if (r) for (let s = 0; s < r.length; s++) r[s].destroy(true);
        this.texturePool[t] = [];
      }
      this._pixelsWidth = e.width, this._pixelsHeight = e.height;
    }
  }
}
sh.SCREEN_KEY = -1;
class $c extends Zt {
  constructor() {
    super(), this.addAttribute("aVertexPosition", new Float32Array([0, 0, 1, 0, 1, 1, 0, 1])).addIndex([0, 1, 3, 2]);
  }
}
class Hc extends Zt {
  constructor() {
    super(), this.vertices = new Float32Array([-1, -1, 1, -1, 1, 1, -1, 1]), this.uvs = new Float32Array([0, 0, 1, 0, 1, 1, 0, 1]), this.vertexBuffer = new Ie(this.vertices), this.uvBuffer = new Ie(this.uvs), this.addAttribute("aVertexPosition", this.vertexBuffer).addAttribute("aTextureCoord", this.uvBuffer).addIndex([0, 1, 2, 0, 2, 3]);
  }
  map(e, t) {
    let r = 0, s = 0;
    return this.uvs[0] = r, this.uvs[1] = s, this.uvs[2] = r + t.width / e.width, this.uvs[3] = s, this.uvs[4] = r + t.width / e.width, this.uvs[5] = s + t.height / e.height, this.uvs[6] = r, this.uvs[7] = s + t.height / e.height, r = t.x, s = t.y, this.vertices[0] = r, this.vertices[1] = s, this.vertices[2] = r + t.width, this.vertices[3] = s, this.vertices[4] = r + t.width, this.vertices[5] = s + t.height, this.vertices[6] = r, this.vertices[7] = s + t.height, this.invalidate(), this;
  }
  invalidate() {
    return this.vertexBuffer._updateID++, this.uvBuffer._updateID++, this;
  }
}
class Vc {
  constructor() {
    this.renderTexture = null, this.target = null, this.legacy = false, this.resolution = 1, this.multisample = _e.NONE, this.sourceFrame = new me(), this.destinationFrame = new me(), this.bindingSourceFrame = new me(), this.bindingDestinationFrame = new me(), this.filters = [], this.transform = null;
  }
  clear() {
    this.target = null, this.filters = null, this.renderTexture = null;
  }
}
const Ur = [new Ae(), new Ae(), new Ae(), new Ae()], Rs = new we();
class nh {
  constructor(e) {
    this.renderer = e, this.defaultFilterStack = [{}], this.texturePool = new sh(), this.statePool = [], this.quad = new $c(), this.quadUv = new Hc(), this.tempRect = new me(), this.activeState = {}, this.globalUniforms = new Xe({ outputFrame: new me(), inputSize: new Float32Array(4), inputPixel: new Float32Array(4), inputClamp: new Float32Array(4), resolution: 1, filterArea: new Float32Array(4), filterClamp: new Float32Array(4) }, true), this.forceClear = false, this.useMaxPadding = false;
  }
  init() {
    this.texturePool.setScreenSize(this.renderer.view);
  }
  push(e, t) {
    const r = this.renderer, s = this.defaultFilterStack, n = this.statePool.pop() || new Vc(), a = r.renderTexture;
    let o, h;
    if (a.current) {
      const f = a.current;
      o = f.resolution, h = f.multisample;
    } else o = r.resolution, h = r.multisample;
    let l = t[0].resolution || o, c = t[0].multisample ?? h, u = t[0].padding, m = t[0].autoFit, y = t[0].legacy ?? true;
    for (let f = 1; f < t.length; f++) {
      const _ = t[f];
      l = Math.min(l, _.resolution || o), c = Math.min(c, _.multisample ?? h), u = this.useMaxPadding ? Math.max(u, _.padding) : u + _.padding, m = m && _.autoFit, y = y || (_.legacy ?? true);
    }
    s.length === 1 && (this.defaultFilterStack[0].renderTexture = a.current), s.push(n), n.resolution = l, n.multisample = c, n.legacy = y, n.target = e, n.sourceFrame.copyFrom(e.filterArea || e.getBounds(true)), n.sourceFrame.pad(u);
    const d = this.tempRect.copyFrom(a.sourceFrame);
    r.projection.transform && this.transformAABB(Rs.copyFrom(r.projection.transform).invert(), d), m ? (n.sourceFrame.fit(d), (n.sourceFrame.width <= 0 || n.sourceFrame.height <= 0) && (n.sourceFrame.width = 0, n.sourceFrame.height = 0)) : n.sourceFrame.intersects(d) || (n.sourceFrame.width = 0, n.sourceFrame.height = 0), this.roundFrame(n.sourceFrame, a.current ? a.current.resolution : r.resolution, a.sourceFrame, a.destinationFrame, r.projection.transform), n.renderTexture = this.getOptimalFilterTexture(n.sourceFrame.width, n.sourceFrame.height, l, c), n.filters = t, n.destinationFrame.width = n.renderTexture.width, n.destinationFrame.height = n.renderTexture.height;
    const p = this.tempRect;
    p.x = 0, p.y = 0, p.width = n.sourceFrame.width, p.height = n.sourceFrame.height, n.renderTexture.filterFrame = n.sourceFrame, n.bindingSourceFrame.copyFrom(a.sourceFrame), n.bindingDestinationFrame.copyFrom(a.destinationFrame), n.transform = r.projection.transform, r.projection.transform = null, a.bind(n.renderTexture, n.sourceFrame, p), r.framebuffer.clear(0, 0, 0, 0);
  }
  pop() {
    const e = this.defaultFilterStack, t = e.pop(), r = t.filters;
    this.activeState = t;
    const s = this.globalUniforms.uniforms;
    s.outputFrame = t.sourceFrame, s.resolution = t.resolution;
    const n = s.inputSize, a = s.inputPixel, o = s.inputClamp;
    if (n[0] = t.destinationFrame.width, n[1] = t.destinationFrame.height, n[2] = 1 / n[0], n[3] = 1 / n[1], a[0] = Math.round(n[0] * t.resolution), a[1] = Math.round(n[1] * t.resolution), a[2] = 1 / a[0], a[3] = 1 / a[1], o[0] = 0.5 * a[2], o[1] = 0.5 * a[3], o[2] = t.sourceFrame.width * n[2] - 0.5 * a[2], o[3] = t.sourceFrame.height * n[3] - 0.5 * a[3], t.legacy) {
      const l = s.filterArea;
      l[0] = t.destinationFrame.width, l[1] = t.destinationFrame.height, l[2] = t.sourceFrame.x, l[3] = t.sourceFrame.y, s.filterClamp = s.inputClamp;
    }
    this.globalUniforms.update();
    const h = e[e.length - 1];
    if (this.renderer.framebuffer.blit(), r.length === 1) r[0].apply(this, t.renderTexture, h.renderTexture, He.BLEND, t), this.returnFilterTexture(t.renderTexture);
    else {
      let l = t.renderTexture, c = this.getOptimalFilterTexture(l.width, l.height, t.resolution);
      c.filterFrame = l.filterFrame;
      let u = 0;
      for (u = 0; u < r.length - 1; ++u) {
        u === 1 && t.multisample > 1 && (c = this.getOptimalFilterTexture(l.width, l.height, t.resolution), c.filterFrame = l.filterFrame), r[u].apply(this, l, c, He.CLEAR, t);
        const m = l;
        l = c, c = m;
      }
      r[u].apply(this, l, h.renderTexture, He.BLEND, t), u > 1 && t.multisample > 1 && this.returnFilterTexture(t.renderTexture), this.returnFilterTexture(l), this.returnFilterTexture(c);
    }
    t.clear(), this.statePool.push(t);
  }
  bindAndClear(e, t = He.CLEAR) {
    const { renderTexture: r, state: s } = this.renderer;
    if (e === this.defaultFilterStack[this.defaultFilterStack.length - 1].renderTexture ? this.renderer.projection.transform = this.activeState.transform : this.renderer.projection.transform = null, e?.filterFrame) {
      const a = this.tempRect;
      a.x = 0, a.y = 0, a.width = e.filterFrame.width, a.height = e.filterFrame.height, r.bind(e, e.filterFrame, a);
    } else e !== this.defaultFilterStack[this.defaultFilterStack.length - 1].renderTexture ? r.bind(e) : this.renderer.renderTexture.bind(e, this.activeState.bindingSourceFrame, this.activeState.bindingDestinationFrame);
    const n = s.stateId & 1 || this.forceClear;
    (t === He.CLEAR || t === He.BLIT && n) && this.renderer.framebuffer.clear(0, 0, 0, 0);
  }
  applyFilter(e, t, r, s) {
    const n = this.renderer;
    n.state.set(e.state), this.bindAndClear(r, s), e.uniforms.uSampler = t, e.uniforms.filterGlobals = this.globalUniforms, n.shader.bind(e), e.legacy = !!e.program.attributeData.aTextureCoord, e.legacy ? (this.quadUv.map(t._frame, t.filterFrame), n.geometry.bind(this.quadUv), n.geometry.draw(Yr.TRIANGLES)) : (n.geometry.bind(this.quad), n.geometry.draw(Yr.TRIANGLE_STRIP));
  }
  calculateSpriteMatrix(e, t) {
    const { sourceFrame: r, destinationFrame: s } = this.activeState, { orig: n } = t._texture, a = e.set(s.width, 0, 0, s.height, r.x, r.y), o = t.worldTransform.copyTo(we.TEMP_MATRIX);
    return o.invert(), a.prepend(o), a.scale(1 / n.width, 1 / n.height), a.translate(t.anchor.x, t.anchor.y), a;
  }
  destroy() {
    this.renderer = null, this.texturePool.clear(false);
  }
  getOptimalFilterTexture(e, t, r = 1, s = _e.NONE) {
    return this.texturePool.getOptimalTexture(e, t, r, s);
  }
  getFilterTexture(e, t, r) {
    if (typeof e == "number") {
      const n = e;
      e = t, t = n;
    }
    e = e || this.activeState.renderTexture;
    const s = this.texturePool.getOptimalTexture(e.width, e.height, t || e.resolution, r || _e.NONE);
    return s.filterFrame = e.filterFrame, s;
  }
  returnFilterTexture(e) {
    this.texturePool.returnTexture(e);
  }
  emptyPool() {
    this.texturePool.clear(true);
  }
  resize() {
    this.texturePool.setScreenSize(this.renderer.view);
  }
  transformAABB(e, t) {
    const r = Ur[0], s = Ur[1], n = Ur[2], a = Ur[3];
    r.set(t.left, t.top), s.set(t.left, t.bottom), n.set(t.right, t.top), a.set(t.right, t.bottom), e.apply(r, r), e.apply(s, s), e.apply(n, n), e.apply(a, a);
    const o = Math.min(r.x, s.x, n.x, a.x), h = Math.min(r.y, s.y, n.y, a.y), l = Math.max(r.x, s.x, n.x, a.x), c = Math.max(r.y, s.y, n.y, a.y);
    t.x = o, t.y = h, t.width = l - o, t.height = c - h;
  }
  roundFrame(e, t, r, s, n) {
    if (!(e.width <= 0 || e.height <= 0 || r.width <= 0 || r.height <= 0)) {
      if (n) {
        const { a, b: o, c: h, d: l } = n;
        if ((Math.abs(o) > 1e-4 || Math.abs(h) > 1e-4) && (Math.abs(a) > 1e-4 || Math.abs(l) > 1e-4)) return;
      }
      n = n ? Rs.copyFrom(n) : Rs.identity(), n.translate(-r.x, -r.y).scale(s.width / r.width, s.height / r.height).translate(s.x, s.y), this.transformAABB(n, e), e.ceil(t), this.transformAABB(n.invert(), e);
    }
  }
}
nh.extension = { type: ne.RendererSystem, name: "filter" };
de.add(nh);
class Wc {
  constructor(e) {
    this.framebuffer = e, this.stencil = null, this.dirtyId = -1, this.dirtyFormat = -1, this.dirtySize = -1, this.multisample = _e.NONE, this.msaaBuffer = null, this.blitFramebuffer = null, this.mipLevel = 0;
  }
}
const Xc = new me();
class ah {
  constructor(e) {
    this.renderer = e, this.managedFramebuffers = [], this.unknownFramebuffer = new Xs(10, 10), this.msaaSamples = null;
  }
  contextChange() {
    this.disposeAll(true);
    const e = this.gl = this.renderer.gl;
    if (this.CONTEXT_UID = this.renderer.CONTEXT_UID, this.current = this.unknownFramebuffer, this.viewport = new me(), this.hasMRT = true, this.writeDepthTexture = true, this.renderer.context.webGLVersion === 1) {
      let t = this.renderer.context.extensions.drawBuffers, r = this.renderer.context.extensions.depthTexture;
      he.PREFER_ENV === Lt.WEBGL_LEGACY && (t = null, r = null), t ? e.drawBuffers = (s) => t.drawBuffersWEBGL(s) : (this.hasMRT = false, e.drawBuffers = () => {
      }), r || (this.writeDepthTexture = false);
    } else this.msaaSamples = e.getInternalformatParameter(e.RENDERBUFFER, e.RGBA8, e.SAMPLES);
  }
  bind(e, t, r = 0) {
    const { gl: s } = this;
    if (e) {
      const n = e.glFramebuffers[this.CONTEXT_UID] || this.initFramebuffer(e);
      this.current !== e && (this.current = e, s.bindFramebuffer(s.FRAMEBUFFER, n.framebuffer)), n.mipLevel !== r && (e.dirtyId++, e.dirtyFormat++, n.mipLevel = r), n.dirtyId !== e.dirtyId && (n.dirtyId = e.dirtyId, n.dirtyFormat !== e.dirtyFormat ? (n.dirtyFormat = e.dirtyFormat, n.dirtySize = e.dirtySize, this.updateFramebuffer(e, r)) : n.dirtySize !== e.dirtySize && (n.dirtySize = e.dirtySize, this.resizeFramebuffer(e)));
      for (let a = 0; a < e.colorTextures.length; a++) {
        const o = e.colorTextures[a];
        this.renderer.texture.unbind(o.parentTextureArray || o);
      }
      if (e.depthTexture && this.renderer.texture.unbind(e.depthTexture), t) {
        const a = t.width >> r, o = t.height >> r, h = a / t.width;
        this.setViewport(t.x * h, t.y * h, a, o);
      } else {
        const a = e.width >> r, o = e.height >> r;
        this.setViewport(0, 0, a, o);
      }
    } else this.current && (this.current = null, s.bindFramebuffer(s.FRAMEBUFFER, null)), t ? this.setViewport(t.x, t.y, t.width, t.height) : this.setViewport(0, 0, this.renderer.width, this.renderer.height);
  }
  setViewport(e, t, r, s) {
    const n = this.viewport;
    e = Math.round(e), t = Math.round(t), r = Math.round(r), s = Math.round(s), (n.width !== r || n.height !== s || n.x !== e || n.y !== t) && (n.x = e, n.y = t, n.width = r, n.height = s, this.gl.viewport(e, t, r, s));
  }
  get size() {
    return this.current ? { x: 0, y: 0, width: this.current.width, height: this.current.height } : { x: 0, y: 0, width: this.renderer.width, height: this.renderer.height };
  }
  clear(e, t, r, s, n = Bs.COLOR | Bs.DEPTH) {
    const { gl: a } = this;
    a.clearColor(e, t, r, s), a.clear(n);
  }
  initFramebuffer(e) {
    const { gl: t } = this, r = new Wc(t.createFramebuffer());
    return r.multisample = this.detectSamples(e.multisample), e.glFramebuffers[this.CONTEXT_UID] = r, this.managedFramebuffers.push(e), e.disposeRunner.add(this), r;
  }
  resizeFramebuffer(e) {
    const { gl: t } = this, r = e.glFramebuffers[this.CONTEXT_UID];
    if (r.stencil) {
      t.bindRenderbuffer(t.RENDERBUFFER, r.stencil);
      let a;
      this.renderer.context.webGLVersion === 1 ? a = t.DEPTH_STENCIL : e.depth && e.stencil ? a = t.DEPTH24_STENCIL8 : e.depth ? a = t.DEPTH_COMPONENT24 : a = t.STENCIL_INDEX8, r.msaaBuffer ? t.renderbufferStorageMultisample(t.RENDERBUFFER, r.multisample, a, e.width, e.height) : t.renderbufferStorage(t.RENDERBUFFER, a, e.width, e.height);
    }
    const s = e.colorTextures;
    let n = s.length;
    t.drawBuffers || (n = Math.min(n, 1));
    for (let a = 0; a < n; a++) {
      const o = s[a], h = o.parentTextureArray || o;
      this.renderer.texture.bind(h, 0), a === 0 && r.msaaBuffer && (t.bindRenderbuffer(t.RENDERBUFFER, r.msaaBuffer), t.renderbufferStorageMultisample(t.RENDERBUFFER, r.multisample, h._glTextures[this.CONTEXT_UID].internalFormat, e.width, e.height));
    }
    e.depthTexture && this.writeDepthTexture && this.renderer.texture.bind(e.depthTexture, 0);
  }
  updateFramebuffer(e, t) {
    const { gl: r } = this, s = e.glFramebuffers[this.CONTEXT_UID], n = e.colorTextures;
    let a = n.length;
    r.drawBuffers || (a = Math.min(a, 1)), s.multisample > 1 && this.canMultisampleFramebuffer(e) ? s.msaaBuffer = s.msaaBuffer || r.createRenderbuffer() : s.msaaBuffer && (r.deleteRenderbuffer(s.msaaBuffer), s.msaaBuffer = null, s.blitFramebuffer && (s.blitFramebuffer.dispose(), s.blitFramebuffer = null));
    const o = [];
    for (let h = 0; h < a; h++) {
      const l = n[h], c = l.parentTextureArray || l;
      this.renderer.texture.bind(c, 0), h === 0 && s.msaaBuffer ? (r.bindRenderbuffer(r.RENDERBUFFER, s.msaaBuffer), r.renderbufferStorageMultisample(r.RENDERBUFFER, s.multisample, c._glTextures[this.CONTEXT_UID].internalFormat, e.width, e.height), r.framebufferRenderbuffer(r.FRAMEBUFFER, r.COLOR_ATTACHMENT0, r.RENDERBUFFER, s.msaaBuffer)) : (r.framebufferTexture2D(r.FRAMEBUFFER, r.COLOR_ATTACHMENT0 + h, l.target, c._glTextures[this.CONTEXT_UID].texture, t), o.push(r.COLOR_ATTACHMENT0 + h));
    }
    if (o.length > 1 && r.drawBuffers(o), e.depthTexture && this.writeDepthTexture) {
      const h = e.depthTexture;
      this.renderer.texture.bind(h, 0), r.framebufferTexture2D(r.FRAMEBUFFER, r.DEPTH_ATTACHMENT, r.TEXTURE_2D, h._glTextures[this.CONTEXT_UID].texture, t);
    }
    if ((e.stencil || e.depth) && !(e.depthTexture && this.writeDepthTexture)) {
      s.stencil = s.stencil || r.createRenderbuffer();
      let h, l;
      this.renderer.context.webGLVersion === 1 ? (h = r.DEPTH_STENCIL_ATTACHMENT, l = r.DEPTH_STENCIL) : e.depth && e.stencil ? (h = r.DEPTH_STENCIL_ATTACHMENT, l = r.DEPTH24_STENCIL8) : e.depth ? (h = r.DEPTH_ATTACHMENT, l = r.DEPTH_COMPONENT24) : (h = r.STENCIL_ATTACHMENT, l = r.STENCIL_INDEX8), r.bindRenderbuffer(r.RENDERBUFFER, s.stencil), s.msaaBuffer ? r.renderbufferStorageMultisample(r.RENDERBUFFER, s.multisample, l, e.width, e.height) : r.renderbufferStorage(r.RENDERBUFFER, l, e.width, e.height), r.framebufferRenderbuffer(r.FRAMEBUFFER, h, r.RENDERBUFFER, s.stencil);
    } else s.stencil && (r.deleteRenderbuffer(s.stencil), s.stencil = null);
  }
  canMultisampleFramebuffer(e) {
    return this.renderer.context.webGLVersion !== 1 && e.colorTextures.length <= 1 && !e.depthTexture;
  }
  detectSamples(e) {
    const { msaaSamples: t } = this;
    let r = _e.NONE;
    if (e <= 1 || t === null) return r;
    for (let s = 0; s < t.length; s++) if (t[s] <= e) {
      r = t[s];
      break;
    }
    return r === 1 && (r = _e.NONE), r;
  }
  blit(e, t, r) {
    const { current: s, renderer: n, gl: a, CONTEXT_UID: o } = this;
    if (n.context.webGLVersion !== 2 || !s) return;
    const h = s.glFramebuffers[o];
    if (!h) return;
    if (!e) {
      if (!h.msaaBuffer) return;
      const c = s.colorTextures[0];
      if (!c) return;
      h.blitFramebuffer || (h.blitFramebuffer = new Xs(s.width, s.height), h.blitFramebuffer.addColorTexture(0, c)), e = h.blitFramebuffer, e.colorTextures[0] !== c && (e.colorTextures[0] = c, e.dirtyId++, e.dirtyFormat++), (e.width !== s.width || e.height !== s.height) && (e.width = s.width, e.height = s.height, e.dirtyId++, e.dirtySize++);
    }
    t || (t = Xc, t.width = s.width, t.height = s.height), r || (r = t);
    const l = t.width === r.width && t.height === r.height;
    this.bind(e), a.bindFramebuffer(a.READ_FRAMEBUFFER, h.framebuffer), a.blitFramebuffer(t.left, t.top, t.right, t.bottom, r.left, r.top, r.right, r.bottom, a.COLOR_BUFFER_BIT, l ? a.NEAREST : a.LINEAR), a.bindFramebuffer(a.READ_FRAMEBUFFER, e.glFramebuffers[this.CONTEXT_UID].framebuffer);
  }
  disposeFramebuffer(e, t) {
    const r = e.glFramebuffers[this.CONTEXT_UID], s = this.gl;
    if (!r) return;
    delete e.glFramebuffers[this.CONTEXT_UID];
    const n = this.managedFramebuffers.indexOf(e);
    n >= 0 && this.managedFramebuffers.splice(n, 1), e.disposeRunner.remove(this), t || (s.deleteFramebuffer(r.framebuffer), r.msaaBuffer && s.deleteRenderbuffer(r.msaaBuffer), r.stencil && s.deleteRenderbuffer(r.stencil)), r.blitFramebuffer && this.disposeFramebuffer(r.blitFramebuffer, t);
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
    e.stencil = true;
    const r = e.width, s = e.height, n = this.gl, a = t.stencil = n.createRenderbuffer();
    n.bindRenderbuffer(n.RENDERBUFFER, a);
    let o, h;
    this.renderer.context.webGLVersion === 1 ? (o = n.DEPTH_STENCIL_ATTACHMENT, h = n.DEPTH_STENCIL) : e.depth ? (o = n.DEPTH_STENCIL_ATTACHMENT, h = n.DEPTH24_STENCIL8) : (o = n.STENCIL_ATTACHMENT, h = n.STENCIL_INDEX8), t.msaaBuffer ? n.renderbufferStorageMultisample(n.RENDERBUFFER, t.multisample, h, r, s) : n.renderbufferStorage(n.RENDERBUFFER, h, r, s), n.framebufferRenderbuffer(n.FRAMEBUFFER, o, n.RENDERBUFFER, a);
  }
  reset() {
    this.current = this.unknownFramebuffer, this.viewport = new me();
  }
  destroy() {
    this.renderer = null;
  }
}
ah.extension = { type: ne.RendererSystem, name: "framebuffer" };
de.add(ah);
const Cs = { 5126: 4, 5123: 2, 5121: 1 };
class oh {
  constructor(e) {
    this.renderer = e, this._activeGeometry = null, this._activeVao = null, this.hasVao = true, this.hasInstance = true, this.canUseUInt32ElementIndex = false, this.managedGeometries = {};
  }
  contextChange() {
    this.disposeAll(true);
    const e = this.gl = this.renderer.gl, t = this.renderer.context;
    if (this.CONTEXT_UID = this.renderer.CONTEXT_UID, t.webGLVersion !== 2) {
      let r = this.renderer.context.extensions.vertexArrayObject;
      he.PREFER_ENV === Lt.WEBGL_LEGACY && (r = null), r ? (e.createVertexArray = () => r.createVertexArrayOES(), e.bindVertexArray = (s) => r.bindVertexArrayOES(s), e.deleteVertexArray = (s) => r.deleteVertexArrayOES(s)) : (this.hasVao = false, e.createVertexArray = () => null, e.bindVertexArray = () => null, e.deleteVertexArray = () => null);
    }
    if (t.webGLVersion !== 2) {
      const r = e.getExtension("ANGLE_instanced_arrays");
      r ? (e.vertexAttribDivisor = (s, n) => r.vertexAttribDivisorANGLE(s, n), e.drawElementsInstanced = (s, n, a, o, h) => r.drawElementsInstancedANGLE(s, n, a, o, h), e.drawArraysInstanced = (s, n, a, o) => r.drawArraysInstancedANGLE(s, n, a, o)) : this.hasInstance = false;
    }
    this.canUseUInt32ElementIndex = t.webGLVersion === 2 || !!t.extensions.uint32ElementIndex;
  }
  bind(e, t) {
    t = t || this.renderer.shader.shader;
    const { gl: r } = this;
    let s = e.glVertexArrayObjects[this.CONTEXT_UID], n = false;
    s || (this.managedGeometries[e.id] = e, e.disposeRunner.add(this), e.glVertexArrayObjects[this.CONTEXT_UID] = s = {}, n = true);
    const a = s[t.program.id] || this.initGeometryVao(e, t, n);
    this._activeGeometry = e, this._activeVao !== a && (this._activeVao = a, this.hasVao ? r.bindVertexArray(a) : this.activateVao(e, t.program)), this.updateBuffers();
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
    for (const n in s) if (!r[n]) throw new Error(`shader and geometry incompatible, geometry missing the "${n}" attribute`);
  }
  getSignature(e, t) {
    const r = e.attributes, s = t.attributeData, n = ["g", e.id];
    for (const a in r) s[a] && n.push(a, s[a].location);
    return n.join("-");
  }
  initGeometryVao(e, t, r = true) {
    const s = this.gl, n = this.CONTEXT_UID, a = this.renderer.buffer, o = t.program;
    o.glPrograms[n] || this.renderer.shader.generateProgram(t), this.checkCompatibility(e, o);
    const h = this.getSignature(e, o), l = e.glVertexArrayObjects[this.CONTEXT_UID];
    let c = l[h];
    if (c) return l[o.id] = c, c;
    const u = e.buffers, m = e.attributes, y = {}, d = {};
    for (const p in u) y[p] = 0, d[p] = 0;
    for (const p in m) !m[p].size && o.attributeData[p] ? m[p].size = o.attributeData[p].size : m[p].size || console.warn(`PIXI Geometry attribute '${p}' size cannot be determined (likely the bound shader does not have the attribute)`), y[m[p].buffer] += m[p].size * Cs[m[p].type];
    for (const p in m) {
      const f = m[p], _ = f.size;
      f.stride === void 0 && (y[f.buffer] === _ * Cs[f.type] ? f.stride = 0 : f.stride = y[f.buffer]), f.start === void 0 && (f.start = d[f.buffer], d[f.buffer] += _ * Cs[f.type]);
    }
    c = s.createVertexArray(), s.bindVertexArray(c);
    for (let p = 0; p < u.length; p++) {
      const f = u[p];
      a.bind(f), r && f._glBuffers[n].refCount++;
    }
    return this.activateVao(e, o), l[o.id] = c, l[h] = c, s.bindVertexArray(null), a.unbind(We.ARRAY_BUFFER), c;
  }
  disposeGeometry(e, t) {
    if (!this.managedGeometries[e.id]) return;
    delete this.managedGeometries[e.id];
    const r = e.glVertexArrayObjects[this.CONTEXT_UID], s = this.gl, n = e.buffers, a = this.renderer?.buffer;
    if (e.disposeRunner.remove(this), !!r) {
      if (a) for (let o = 0; o < n.length; o++) {
        const h = n[o]._glBuffers[this.CONTEXT_UID];
        h && (h.refCount--, h.refCount === 0 && !t && a.dispose(n[o], t));
      }
      if (!t) {
        for (const o in r) if (o[0] === "g") {
          const h = r[o];
          this._activeVao === h && this.unbind(), s.deleteVertexArray(h);
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
    const r = this.gl, s = this.CONTEXT_UID, n = this.renderer.buffer, a = e.buffers, o = e.attributes;
    e.indexBuffer && n.bind(e.indexBuffer);
    let h = null;
    for (const l in o) {
      const c = o[l], u = a[c.buffer], m = u._glBuffers[s];
      if (t.attributeData[l]) {
        h !== m && (n.bind(u), h = m);
        const y = t.attributeData[l].location;
        if (r.enableVertexAttribArray(y), r.vertexAttribPointer(y, c.size, c.type || r.FLOAT, c.normalized, c.stride, c.start), c.instance) if (this.hasInstance) r.vertexAttribDivisor(y, c.divisor);
        else throw new Error("geometry error, GPU Instancing is not supported on this device");
      }
    }
  }
  draw(e, t, r, s) {
    const { gl: n } = this, a = this._activeGeometry;
    if (a.indexBuffer) {
      const o = a.indexBuffer.data.BYTES_PER_ELEMENT, h = o === 2 ? n.UNSIGNED_SHORT : n.UNSIGNED_INT;
      o === 2 || o === 4 && this.canUseUInt32ElementIndex ? a.instanced ? n.drawElementsInstanced(e, t || a.indexBuffer.data.length, h, (r || 0) * o, s || 1) : n.drawElements(e, t || a.indexBuffer.data.length, h, (r || 0) * o) : console.warn("unsupported index buffer type: uint32");
    } else a.instanced ? n.drawArraysInstanced(e, r, t || a.getSize(), s || 1) : n.drawArrays(e, r, t || a.getSize());
    return this;
  }
  unbind() {
    this.gl.bindVertexArray(null), this._activeVao = null, this._activeGeometry = null;
  }
  destroy() {
    this.renderer = null;
  }
}
oh.extension = { type: ne.RendererSystem, name: "geometry" };
de.add(oh);
const co = new we();
class qc {
  constructor(e, t) {
    this._texture = e, this.mapCoord = new we(), this.uClampFrame = new Float32Array(4), this.uClampOffset = new Float32Array(2), this._textureID = -1, this._updateID = 0, this.clampOffset = 0, this.clampMargin = typeof t > "u" ? 0.5 : t, this.isSimple = false;
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
      const n = e[s], a = e[s + 1];
      t[s] = n * r.a + a * r.c + r.tx, t[s + 1] = n * r.b + a * r.d + r.ty;
    }
    return t;
  }
  update(e) {
    const t = this._texture;
    if (!t || !t.valid || !e && this._textureID === t._updateID) return false;
    this._textureID = t._updateID, this._updateID++;
    const r = t._uvs;
    this.mapCoord.set(r.x1 - r.x0, r.y1 - r.y0, r.x3 - r.x0, r.y3 - r.y0, r.x0, r.y0);
    const s = t.orig, n = t.trim;
    n && (co.set(s.width / n.width, 0, 0, s.height / n.height, -n.x / n.width, -n.y / n.height), this.mapCoord.append(co));
    const a = t.baseTexture, o = this.uClampFrame, h = this.clampMargin / a.resolution, l = this.clampOffset;
    return o[0] = (t._frame.x + h + l) / a.width, o[1] = (t._frame.y + h + l) / a.height, o[2] = (t._frame.x + t._frame.width - h + l) / a.width, o[3] = (t._frame.y + t._frame.height - h + l) / a.height, this.uClampOffset[0] = l / a.realWidth, this.uClampOffset[1] = l / a.realHeight, this.isSimple = t._frame.width === a.width && t._frame.height === a.height && t.rotate === 0, true;
  }
}
var jc = `varying vec2 vMaskCoord;
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
`, Yc = `attribute vec2 aVertexPosition;
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
`;
class Kc extends Ne {
  constructor(e, t, r) {
    let s = null;
    typeof e != "string" && t === void 0 && r === void 0 && (s = e, e = void 0, t = void 0, r = void 0), super(e || Yc, t || jc, r), this.maskSprite = s, this.maskMatrix = new we();
  }
  get maskSprite() {
    return this._maskSprite;
  }
  set maskSprite(e) {
    this._maskSprite = e, this._maskSprite && (this._maskSprite.renderable = false);
  }
  apply(e, t, r, s) {
    const n = this._maskSprite, a = n._texture;
    a.valid && (a.uvMatrix || (a.uvMatrix = new qc(a, 0)), a.uvMatrix.update(), this.uniforms.npmAlpha = a.baseTexture.alphaMode ? 0 : 1, this.uniforms.mask = a, this.uniforms.otherMatrix = e.calculateSpriteMatrix(this.maskMatrix, n).prepend(a.uvMatrix.mapCoord), this.uniforms.alpha = n.worldAlpha, this.uniforms.maskClamp = a.uvMatrix.uClampFrame, e.applyFilter(this, t, r, s));
  }
}
class Zc {
  constructor(e = null) {
    this.type = Ee.NONE, this.autoDetect = true, this.maskObject = e || null, this.pooled = false, this.isMaskData = true, this.resolution = null, this.multisample = Ne.defaultMultisample, this.enabled = true, this.colorMask = 15, this._filters = null, this._stencilCounter = 0, this._scissorCounter = 0, this._scissorRect = null, this._scissorRectLocal = null, this._colorMask = 15, this._target = null;
  }
  get filter() {
    return this._filters ? this._filters[0] : null;
  }
  set filter(e) {
    e ? this._filters ? this._filters[0] = e : this._filters = [e] : this._filters = null;
  }
  reset() {
    this.pooled && (this.maskObject = null, this.type = Ee.NONE, this.autoDetect = true), this._target = null, this._scissorRectLocal = null;
  }
  copyCountersOrReset(e) {
    e ? (this._stencilCounter = e._stencilCounter, this._scissorCounter = e._scissorCounter, this._scissorRect = e._scissorRect) : (this._stencilCounter = 0, this._scissorCounter = 0, this._scissorRect = null);
  }
}
class hh {
  constructor(e) {
    this.renderer = e, this.enableScissor = true, this.alphaMaskPool = [], this.maskDataPool = [], this.maskStack = [], this.alphaMaskIndex = 0;
  }
  setMaskStack(e) {
    this.maskStack = e, this.renderer.scissor.setMaskStack(e), this.renderer.stencil.setMaskStack(e);
  }
  push(e, t) {
    let r = t;
    if (!r.isMaskData) {
      const n = this.maskDataPool.pop() || new Zc();
      n.pooled = true, n.maskObject = t, r = n;
    }
    const s = this.maskStack.length !== 0 ? this.maskStack[this.maskStack.length - 1] : null;
    if (r.copyCountersOrReset(s), r._colorMask = s ? s._colorMask : 15, r.autoDetect && this.detect(r), r._target = e, r.type !== Ee.SPRITE && this.maskStack.push(r), r.enabled) switch (r.type) {
      case Ee.SCISSOR:
        this.renderer.scissor.push(r);
        break;
      case Ee.STENCIL:
        this.renderer.stencil.push(r);
        break;
      case Ee.SPRITE:
        r.copyCountersOrReset(null), this.pushSpriteMask(r);
        break;
      case Ee.COLOR:
        this.pushColorMask(r);
        break;
    }
    r.type === Ee.SPRITE && this.maskStack.push(r);
  }
  pop(e) {
    const t = this.maskStack.pop();
    if (!(!t || t._target !== e)) {
      if (t.enabled) switch (t.type) {
        case Ee.SCISSOR:
          this.renderer.scissor.pop(t);
          break;
        case Ee.STENCIL:
          this.renderer.stencil.pop(t.maskObject);
          break;
        case Ee.SPRITE:
          this.popSpriteMask(t);
          break;
        case Ee.COLOR:
          this.popColorMask(t);
          break;
      }
      if (t.reset(), t.pooled && this.maskDataPool.push(t), this.maskStack.length !== 0) {
        const r = this.maskStack[this.maskStack.length - 1];
        r.type === Ee.SPRITE && r._filters && (r._filters[0].maskSprite = r.maskObject);
      }
    }
  }
  detect(e) {
    const t = e.maskObject;
    t ? t.isSprite ? e.type = Ee.SPRITE : this.enableScissor && this.renderer.scissor.testScissor(e) ? e.type = Ee.SCISSOR : e.type = Ee.STENCIL : e.type = Ee.COLOR;
  }
  pushSpriteMask(e) {
    const { maskObject: t } = e, r = e._target;
    let s = e._filters;
    s || (s = this.alphaMaskPool[this.alphaMaskIndex], s || (s = this.alphaMaskPool[this.alphaMaskIndex] = [new Kc()])), s[0].resolution = e.resolution, s[0].multisample = e.multisample, s[0].maskSprite = t;
    const n = r.filterArea;
    r.filterArea = t.getBounds(true), this.renderer.filter.push(r, s), r.filterArea = n, e._filters || this.alphaMaskIndex++;
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
}
hh.extension = { type: ne.RendererSystem, name: "mask" };
de.add(hh);
class lh {
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
}
const uo = new we(), fo = [], ch = class Wr extends lh {
  constructor(e) {
    super(e), this.glConst = he.ADAPTER.getWebGLRenderingContext().SCISSOR_TEST;
  }
  getStackLength() {
    const e = this.maskStack[this.maskStack.length - 1];
    return e ? e._scissorCounter : 0;
  }
  calcScissorRect(e) {
    if (e._scissorRectLocal) return;
    const t = e._scissorRect, { maskObject: r } = e, { renderer: s } = this, n = s.renderTexture, a = r.getBounds(true, fo.pop() ?? new me());
    this.roundFrameToPixels(a, n.current ? n.current.resolution : s.resolution, n.sourceFrame, n.destinationFrame, s.projection.transform), t && a.fit(t), e._scissorRectLocal = a;
  }
  static isMatrixRotated(e) {
    if (!e) return false;
    const { a: t, b: r, c: s, d: n } = e;
    return (Math.abs(r) > 1e-4 || Math.abs(s) > 1e-4) && (Math.abs(t) > 1e-4 || Math.abs(n) > 1e-4);
  }
  testScissor(e) {
    const { maskObject: t } = e;
    if (!t.isFastRect || !t.isFastRect() || Wr.isMatrixRotated(t.worldTransform) || Wr.isMatrixRotated(this.renderer.projection.transform)) return false;
    this.calcScissorRect(e);
    const r = e._scissorRectLocal;
    return r.width > 0 && r.height > 0;
  }
  roundFrameToPixels(e, t, r, s, n) {
    Wr.isMatrixRotated(n) || (n = n ? uo.copyFrom(n) : uo.identity(), n.translate(-r.x, -r.y).scale(s.width / r.width, s.height / r.height).translate(s.x, s.y), this.renderer.filter.transformAABB(n, e), e.fit(s), e.x = Math.round(e.x * t), e.y = Math.round(e.y * t), e.width = Math.round(e.width * t), e.height = Math.round(e.height * t));
  }
  push(e) {
    e._scissorRectLocal || this.calcScissorRect(e);
    const { gl: t } = this.renderer;
    e._scissorRect || t.enable(t.SCISSOR_TEST), e._scissorCounter++, e._scissorRect = e._scissorRectLocal, this._useCurrent();
  }
  pop(e) {
    const { gl: t } = this.renderer;
    e && fo.push(e._scissorRectLocal), this.getStackLength() > 0 ? this._useCurrent() : t.disable(t.SCISSOR_TEST);
  }
  _useCurrent() {
    const e = this.maskStack[this.maskStack.length - 1]._scissorRect;
    let t;
    this.renderer.renderTexture.current ? t = e.y : t = this.renderer.height - e.height - e.y, this.renderer.gl.scissor(e.x, t, e.width, e.height);
  }
};
ch.extension = { type: ne.RendererSystem, name: "scissor" };
let Jc = ch;
de.add(Jc);
class uh extends lh {
  constructor(e) {
    super(e), this.glConst = he.ADAPTER.getWebGLRenderingContext().STENCIL_TEST;
  }
  getStackLength() {
    const e = this.maskStack[this.maskStack.length - 1];
    return e ? e._stencilCounter : 0;
  }
  push(e) {
    const t = e.maskObject, { gl: r } = this.renderer, s = e._stencilCounter;
    s === 0 && (this.renderer.framebuffer.forceStencil(), r.clearStencil(0), r.clear(r.STENCIL_BUFFER_BIT), r.enable(r.STENCIL_TEST)), e._stencilCounter++;
    const n = e._colorMask;
    n !== 0 && (e._colorMask = 0, r.colorMask(false, false, false, false)), r.stencilFunc(r.EQUAL, s, 4294967295), r.stencilOp(r.KEEP, r.KEEP, r.INCR), t.renderable = true, t.render(this.renderer), this.renderer.batch.flush(), t.renderable = false, n !== 0 && (e._colorMask = n, r.colorMask((n & 1) !== 0, (n & 2) !== 0, (n & 4) !== 0, (n & 8) !== 0)), this._useCurrent();
  }
  pop(e) {
    const t = this.renderer.gl;
    if (this.getStackLength() === 0) t.disable(t.STENCIL_TEST);
    else {
      const r = this.maskStack.length !== 0 ? this.maskStack[this.maskStack.length - 1] : null, s = r ? r._colorMask : 15;
      s !== 0 && (r._colorMask = 0, t.colorMask(false, false, false, false)), t.stencilOp(t.KEEP, t.KEEP, t.DECR), e.renderable = true, e.render(this.renderer), this.renderer.batch.flush(), e.renderable = false, s !== 0 && (r._colorMask = s, t.colorMask((s & 1) !== 0, (s & 2) !== 0, (s & 4) !== 0, (s & 8) !== 0)), this._useCurrent();
    }
  }
  _useCurrent() {
    const e = this.renderer.gl;
    e.stencilFunc(e.EQUAL, this.getStackLength(), 4294967295), e.stencilOp(e.KEEP, e.KEEP, e.KEEP);
  }
}
uh.extension = { type: ne.RendererSystem, name: "stencil" };
de.add(uh);
class dh {
  constructor(e) {
    this.renderer = e, this.plugins = {}, Object.defineProperties(this.plugins, { extract: { enumerable: false, get() {
      return fe("7.0.0", "renderer.plugins.extract has moved to renderer.extract"), e.extract;
    } }, prepare: { enumerable: false, get() {
      return fe("7.0.0", "renderer.plugins.prepare has moved to renderer.prepare"), e.prepare;
    } }, interaction: { enumerable: false, get() {
      return fe("7.0.0", "renderer.plugins.interaction has been deprecated, use renderer.events"), e.events;
    } } });
  }
  init() {
    const e = this.rendererPlugins;
    for (const t in e) this.plugins[t] = new e[t](this.renderer);
  }
  destroy() {
    for (const e in this.plugins) this.plugins[e].destroy(), this.plugins[e] = null;
  }
}
dh.extension = { type: [ne.RendererSystem, ne.CanvasRendererSystem], name: "_plugin" };
de.add(dh);
class fh {
  constructor(e) {
    this.renderer = e, this.destinationFrame = null, this.sourceFrame = null, this.defaultFrame = null, this.projectionMatrix = new we(), this.transform = null;
  }
  update(e, t, r, s) {
    this.destinationFrame = e || this.destinationFrame || this.defaultFrame, this.sourceFrame = t || this.sourceFrame || e, this.calculateProjection(this.destinationFrame, this.sourceFrame, r, s), this.transform && this.projectionMatrix.append(this.transform);
    const n = this.renderer;
    n.globalUniforms.uniforms.projectionMatrix = this.projectionMatrix, n.globalUniforms.update(), n.shader.shader && n.shader.syncUniformGroup(n.shader.shader.uniforms.globals);
  }
  calculateProjection(e, t, r, s) {
    const n = this.projectionMatrix, a = s ? -1 : 1;
    n.identity(), n.a = 1 / t.width * 2, n.d = a * (1 / t.height * 2), n.tx = -1 - t.x * n.a, n.ty = -a - t.y * n.d;
  }
  setTransform(e) {
  }
  destroy() {
    this.renderer = null;
  }
}
fh.extension = { type: ne.RendererSystem, name: "projection" };
de.add(fh);
const Qc = new dn(), po = new me();
class ph {
  constructor(e) {
    this.renderer = e, this._tempMatrix = new we();
  }
  generateTexture(e, t) {
    const { region: r, ...s } = t || {}, n = r?.copyTo(po) || e.getLocalBounds(po, true), a = s.resolution || this.renderer.resolution;
    n.width = Math.max(n.width, 1 / a), n.height = Math.max(n.height, 1 / a), s.width = n.width, s.height = n.height, s.resolution = a, s.multisample ?? (s.multisample = this.renderer.multisample);
    const o = ui.create(s);
    this._tempMatrix.tx = -n.x, this._tempMatrix.ty = -n.y;
    const h = e.transform;
    return e.transform = Qc, this.renderer.render(e, { renderTexture: o, transform: this._tempMatrix, skipUpdateTransform: !!e.parent, blit: true }), e.transform = h, o;
  }
  destroy() {
  }
}
ph.extension = { type: [ne.RendererSystem, ne.CanvasRendererSystem], name: "textureGenerator" };
de.add(ph);
const pt = new me(), sr = new me();
class mh {
  constructor(e) {
    this.renderer = e, this.defaultMaskStack = [], this.current = null, this.sourceFrame = new me(), this.destinationFrame = new me(), this.viewportFrame = new me();
  }
  contextChange() {
    const e = this.renderer?.gl.getContextAttributes();
    this._rendererPremultipliedAlpha = !!(e && e.alpha && e.premultipliedAlpha);
  }
  bind(e = null, t, r) {
    const s = this.renderer;
    this.current = e;
    let n, a, o;
    e ? (n = e.baseTexture, o = n.resolution, t || (pt.width = e.frame.width, pt.height = e.frame.height, t = pt), r || (sr.x = e.frame.x, sr.y = e.frame.y, sr.width = t.width, sr.height = t.height, r = sr), a = n.framebuffer) : (o = s.resolution, t || (pt.width = s._view.screen.width, pt.height = s._view.screen.height, t = pt), r || (r = pt, r.width = t.width, r.height = t.height));
    const h = this.viewportFrame;
    h.x = r.x * o, h.y = r.y * o, h.width = r.width * o, h.height = r.height * o, e || (h.y = s.view.height - (h.y + h.height)), h.ceil(), this.renderer.framebuffer.bind(a, h), this.renderer.projection.update(r, t, o, !a), e ? this.renderer.mask.setMaskStack(n.maskStack) : this.renderer.mask.setMaskStack(this.defaultMaskStack), this.sourceFrame.copyFrom(t), this.destinationFrame.copyFrom(r);
  }
  clear(e, t) {
    const r = this.current ? this.current.baseTexture.clear : this.renderer.background.backgroundColor, s = Rt.shared.setValue(e || r);
    (this.current && this.current.baseTexture.alphaMode > 0 || !this.current && this._rendererPremultipliedAlpha) && s.premultiply(s.alpha);
    const n = this.destinationFrame, a = this.current ? this.current.baseTexture : this.renderer._view.screen, o = n.width !== a.width || n.height !== a.height;
    if (o) {
      let { x: h, y: l, width: c, height: u } = this.viewportFrame;
      h = Math.round(h), l = Math.round(l), c = Math.round(c), u = Math.round(u), this.renderer.gl.enable(this.renderer.gl.SCISSOR_TEST), this.renderer.gl.scissor(h, l, c, u);
    }
    this.renderer.framebuffer.clear(s.red, s.green, s.blue, s.alpha, t), o && this.renderer.scissor.pop();
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
}
mh.extension = { type: ne.RendererSystem, name: "renderTexture" };
de.add(mh);
class eu {
  constructor(e, t) {
    this.program = e, this.uniformData = t, this.uniformGroups = {}, this.uniformDirtyGroups = {}, this.uniformBufferBindings = {};
  }
  destroy() {
    this.uniformData = null, this.uniformGroups = null, this.uniformDirtyGroups = null, this.uniformBufferBindings = null, this.program = null;
  }
}
function tu(i, e) {
  const t = {}, r = e.getProgramParameter(i, e.ACTIVE_ATTRIBUTES);
  for (let s = 0; s < r; s++) {
    const n = e.getActiveAttrib(i, s);
    if (n.name.startsWith("gl_")) continue;
    const a = Qo(e, n.type), o = { type: a, name: n.name, size: Jo(a), location: e.getAttribLocation(i, n.name) };
    t[n.name] = o;
  }
  return t;
}
function ru(i, e) {
  const t = {}, r = e.getProgramParameter(i, e.ACTIVE_UNIFORMS);
  for (let s = 0; s < r; s++) {
    const n = e.getActiveUniform(i, s), a = n.name.replace(/\[.*?\]$/, ""), o = !!n.name.match(/\[.*?\]$/), h = Qo(e, n.type);
    t[a] = { name: a, index: s, type: h, size: n.size, isArray: o, value: Ko(h, n.size) };
  }
  return t;
}
function iu(i, e) {
  const t = so(i, i.VERTEX_SHADER, e.vertexSrc), r = so(i, i.FRAGMENT_SHADER, e.fragmentSrc), s = i.createProgram();
  i.attachShader(s, t), i.attachShader(s, r);
  const n = e.extra?.transformFeedbackVaryings;
  if (n && (typeof i.transformFeedbackVaryings != "function" ? console.warn("TransformFeedback is not supported but TransformFeedbackVaryings are given.") : i.transformFeedbackVaryings(s, n.names, n.bufferMode === "separate" ? i.SEPARATE_ATTRIBS : i.INTERLEAVED_ATTRIBS)), i.linkProgram(s), i.getProgramParameter(s, i.LINK_STATUS) || Cc(i, s, t, r), e.attributeData = tu(s, i), e.uniformData = ru(s, i), !/^[ \t]*#[ \t]*version[ \t]+300[ \t]+es[ \t]*$/m.test(e.vertexSrc)) {
    const o = Object.keys(e.attributeData);
    o.sort((h, l) => h > l ? 1 : -1);
    for (let h = 0; h < o.length; h++) e.attributeData[o[h]].location = h, i.bindAttribLocation(s, h, o[h]);
    i.linkProgram(s);
  }
  i.deleteShader(t), i.deleteShader(r);
  const a = {};
  for (const o in e.uniformData) {
    const h = e.uniformData[o];
    a[o] = { location: i.getUniformLocation(s, o), value: Ko(h.type, h.size) };
  }
  return new eu(s, a);
}
function su(i, e, t, r, s) {
  t.buffer.update(s);
}
const nu = { float: `
        data[offset] = v;
    `, vec2: `
        data[offset] = v[0];
        data[offset+1] = v[1];
    `, vec3: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];

    `, vec4: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];
        data[offset+3] = v[3];
    `, mat2: `
        data[offset] = v[0];
        data[offset+1] = v[1];

        data[offset+4] = v[2];
        data[offset+5] = v[3];
    `, mat3: `
        data[offset] = v[0];
        data[offset+1] = v[1];
        data[offset+2] = v[2];

        data[offset + 4] = v[3];
        data[offset + 5] = v[4];
        data[offset + 6] = v[5];

        data[offset + 8] = v[6];
        data[offset + 9] = v[7];
        data[offset + 10] = v[8];
    `, mat4: `
        for(var i = 0; i < 16; i++)
        {
            data[offset + i] = v[i];
        }
    ` }, yh = { float: 4, vec2: 8, vec3: 12, vec4: 16, int: 4, ivec2: 8, ivec3: 12, ivec4: 16, uint: 4, uvec2: 8, uvec3: 12, uvec4: 16, bool: 4, bvec2: 8, bvec3: 12, bvec4: 16, mat2: 32, mat3: 48, mat4: 64 };
function au(i) {
  const e = i.map((n) => ({ data: n, offset: 0, dataLen: 0, dirty: 0 }));
  let t = 0, r = 0, s = 0;
  for (let n = 0; n < e.length; n++) {
    const a = e[n];
    if (t = yh[a.data.type], a.data.size > 1 && (t = Math.max(t, 16) * a.data.size), a.dataLen = t, r % t !== 0 && r < 16) {
      const o = r % t % 16;
      r += o, s += o;
    }
    r + t > 16 ? (s = Math.ceil(s / 16) * 16, a.offset = s, s += t, r = t) : (a.offset = s, r += t, s += t);
  }
  return s = Math.ceil(s / 16) * 16, { uboElements: e, size: s };
}
function ou(i, e) {
  const t = [];
  for (const r in i) e[r] && t.push(e[r]);
  return t.sort((r, s) => r.index - s.index), t;
}
function hu(i, e) {
  if (!i.autoManage) return { size: 0, syncFunc: su };
  const t = ou(i.uniforms, e), { uboElements: r, size: s } = au(t), n = [`
    var v = null;
    var v2 = null;
    var cv = null;
    var t = 0;
    var gl = renderer.gl
    var index = 0;
    var data = buffer.data;
    `];
  for (let a = 0; a < r.length; a++) {
    const o = r[a], h = i.uniforms[o.data.name], l = o.data.name;
    let c = false;
    for (let u = 0; u < Yt.length; u++) {
      const m = Yt[u];
      if (m.codeUbo && m.test(o.data, h)) {
        n.push(`offset = ${o.offset / 4};`, Yt[u].codeUbo(o.data.name, h)), c = true;
        break;
      }
    }
    if (!c) if (o.data.size > 1) {
      const u = Jo(o.data.type), m = Math.max(yh[o.data.type] / 16, 1), y = u / m, d = (4 - y % 4) % 4;
      n.push(`
                cv = ud.${l}.value;
                v = uv.${l};
                offset = ${o.offset / 4};

                t = 0;

                for(var i=0; i < ${o.data.size * m}; i++)
                {
                    for(var j = 0; j < ${y}; j++)
                    {
                        data[offset++] = v[t++];
                    }
                    offset += ${d};
                }

                `);
    } else {
      const u = nu[o.data.type];
      n.push(`
                cv = ud.${l}.value;
                v = uv.${l};
                offset = ${o.offset / 4};
                ${u};
                `);
    }
  }
  return n.push(`
       renderer.buffer.update(buffer);
    `), { size: s, syncFunc: new Function("ud", "uv", "renderer", "syncData", "buffer", n.join(`
`)) };
}
let lu = 0;
const Dr = { textureCount: 0, uboCount: 0 };
class gh {
  constructor(e) {
    this.destroyed = false, this.renderer = e, this.systemCheck(), this.gl = null, this.shader = null, this.program = null, this.cache = {}, this._uboCache = {}, this.id = lu++;
  }
  systemCheck() {
    if (!Lc()) throw new Error("Current environment does not allow unsafe-eval, please use @pixi/unsafe-eval module to enable support.");
  }
  contextChange(e) {
    this.gl = e, this.reset();
  }
  bind(e, t) {
    e.disposeRunner.add(this), e.uniforms.globals = this.renderer.globalUniforms;
    const r = e.program, s = r.glPrograms[this.renderer.CONTEXT_UID] || this.generateProgram(e);
    return this.shader = e, this.program !== r && (this.program = r, this.gl.useProgram(s.program)), t || (Dr.textureCount = 0, Dr.uboCount = 0, this.syncUniformGroup(e.uniformGroup, Dr)), s;
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
    return this.cache[t] || (this.cache[t] = Ac(e, this.shader.program.uniformData)), e.syncUniforms[this.shader.program.id] = this.cache[t], e.syncUniforms[this.shader.program.id];
  }
  syncUniformBufferGroup(e, t) {
    const r = this.getGlProgram();
    if (!e.static || e.dirtyId !== 0 || !r.uniformGroups[e.id]) {
      e.dirtyId = 0;
      const s = r.uniformGroups[e.id] || this.createSyncBufferGroup(e, r, t);
      e.buffer.update(), s(r.uniformData, e.uniforms, this.renderer, Dr, e.buffer);
    }
    this.renderer.buffer.bindBufferBase(e.buffer, r.uniformBufferBindings[t]);
  }
  createSyncBufferGroup(e, t, r) {
    const { gl: s } = this.renderer;
    this.renderer.buffer.bind(e.buffer);
    const n = this.gl.getUniformBlockIndex(t.program, r);
    t.uniformBufferBindings[r] = this.shader.uniformBindCount, s.uniformBlockBinding(t.program, n, this.shader.uniformBindCount), this.shader.uniformBindCount++;
    const a = this.getSignature(e, this.shader.program.uniformData, "ubo");
    let o = this._uboCache[a];
    if (o || (o = this._uboCache[a] = hu(e, this.shader.program.uniformData)), e.autoManage) {
      const h = new Float32Array(o.size / 4);
      e.buffer.update(h);
    }
    return t.uniformGroups[e.id] = o.syncFunc, t.uniformGroups[e.id];
  }
  getSignature(e, t, r) {
    const s = e.uniforms, n = [`${r}-`];
    for (const a in s) n.push(a), t[a] && n.push(t[a].type);
    return n.join("-");
  }
  getGlProgram() {
    return this.shader ? this.shader.program.glPrograms[this.renderer.CONTEXT_UID] : null;
  }
  generateProgram(e) {
    const t = this.gl, r = e.program, s = iu(t, r);
    return r.glPrograms[this.renderer.CONTEXT_UID] = s, s;
  }
  reset() {
    this.program = null, this.shader = null;
  }
  disposeShader(e) {
    this.shader === e && (this.shader = null);
  }
  destroy() {
    this.renderer = null, this.destroyed = true;
  }
}
gh.extension = { type: ne.RendererSystem, name: "shader" };
de.add(gh);
class ti {
  constructor(e) {
    this.renderer = e;
  }
  run(e) {
    const { renderer: t } = this;
    t.runners.init.emit(t.options), e.hello && console.log(`PixiJS 7.4.3 - ${t.rendererLogId} - https://pixijs.com`), t.resize(t.screen.width, t.screen.height);
  }
  destroy() {
  }
}
ti.defaultOptions = { hello: false }, ti.extension = { type: [ne.RendererSystem, ne.CanvasRendererSystem], name: "startup" };
de.add(ti);
function cu(i, e = []) {
  return e[te.NORMAL] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.ADD] = [i.ONE, i.ONE], e[te.MULTIPLY] = [i.DST_COLOR, i.ONE_MINUS_SRC_ALPHA, i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.SCREEN] = [i.ONE, i.ONE_MINUS_SRC_COLOR, i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.OVERLAY] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.DARKEN] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.LIGHTEN] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.COLOR_DODGE] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.COLOR_BURN] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.HARD_LIGHT] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.SOFT_LIGHT] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.DIFFERENCE] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.EXCLUSION] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.HUE] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.SATURATION] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.COLOR] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.LUMINOSITY] = [i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.NONE] = [0, 0], e[te.NORMAL_NPM] = [i.SRC_ALPHA, i.ONE_MINUS_SRC_ALPHA, i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.ADD_NPM] = [i.SRC_ALPHA, i.ONE, i.ONE, i.ONE], e[te.SCREEN_NPM] = [i.SRC_ALPHA, i.ONE_MINUS_SRC_COLOR, i.ONE, i.ONE_MINUS_SRC_ALPHA], e[te.SRC_IN] = [i.DST_ALPHA, i.ZERO], e[te.SRC_OUT] = [i.ONE_MINUS_DST_ALPHA, i.ZERO], e[te.SRC_ATOP] = [i.DST_ALPHA, i.ONE_MINUS_SRC_ALPHA], e[te.DST_OVER] = [i.ONE_MINUS_DST_ALPHA, i.ONE], e[te.DST_IN] = [i.ZERO, i.SRC_ALPHA], e[te.DST_OUT] = [i.ZERO, i.ONE_MINUS_SRC_ALPHA], e[te.DST_ATOP] = [i.ONE_MINUS_DST_ALPHA, i.SRC_ALPHA], e[te.XOR] = [i.ONE_MINUS_DST_ALPHA, i.ONE_MINUS_SRC_ALPHA], e[te.SUBTRACT] = [i.ONE, i.ONE, i.ONE, i.ONE, i.FUNC_REVERSE_SUBTRACT, i.FUNC_ADD], e;
}
const uu = 0, du = 1, fu = 2, pu = 3, mu = 4, yu = 5, vh = class qs {
  constructor() {
    this.gl = null, this.stateId = 0, this.polygonOffset = 0, this.blendMode = te.NONE, this._blendEq = false, this.map = [], this.map[uu] = this.setBlend, this.map[du] = this.setOffset, this.map[fu] = this.setCullFace, this.map[pu] = this.setDepthTest, this.map[mu] = this.setFrontFace, this.map[yu] = this.setDepthMask, this.checks = [], this.defaultState = new er(), this.defaultState.blend = true;
  }
  contextChange(e) {
    this.gl = e, this.blendModes = cu(e), this.set(this.defaultState), this.reset();
  }
  set(e) {
    if (e = e || this.defaultState, this.stateId !== e.data) {
      let t = this.stateId ^ e.data, r = 0;
      for (; t; ) t & 1 && this.map[r].call(this, !!(e.data & 1 << r)), t = t >> 1, r++;
      this.stateId = e.data;
    }
    for (let t = 0; t < this.checks.length; t++) this.checks[t](this, e);
  }
  forceState(e) {
    e = e || this.defaultState;
    for (let t = 0; t < this.map.length; t++) this.map[t].call(this, !!(e.data & 1 << t));
    for (let t = 0; t < this.checks.length; t++) this.checks[t](this, e);
    this.stateId = e.data;
  }
  setBlend(e) {
    this.updateCheck(qs.checkBlendMode, e), this.gl[e ? "enable" : "disable"](this.gl.BLEND);
  }
  setOffset(e) {
    this.updateCheck(qs.checkPolygonOffset, e), this.gl[e ? "enable" : "disable"](this.gl.POLYGON_OFFSET_FILL);
  }
  setDepthTest(e) {
    this.gl[e ? "enable" : "disable"](this.gl.DEPTH_TEST);
  }
  setDepthMask(e) {
    this.gl.depthMask(e);
  }
  setCullFace(e) {
    this.gl[e ? "enable" : "disable"](this.gl.CULL_FACE);
  }
  setFrontFace(e) {
    this.gl.frontFace(this.gl[e ? "CW" : "CCW"]);
  }
  setBlendMode(e) {
    if (e === this.blendMode) return;
    this.blendMode = e;
    const t = this.blendModes[e], r = this.gl;
    t.length === 2 ? r.blendFunc(t[0], t[1]) : r.blendFuncSeparate(t[0], t[1], t[2], t[3]), t.length === 6 ? (this._blendEq = true, r.blendEquationSeparate(t[4], t[5])) : this._blendEq && (this._blendEq = false, r.blendEquationSeparate(r.FUNC_ADD, r.FUNC_ADD));
  }
  setPolygonOffset(e, t) {
    this.gl.polygonOffset(e, t);
  }
  reset() {
    this.gl.pixelStorei(this.gl.UNPACK_FLIP_Y_WEBGL, false), this.forceState(this.defaultState), this._blendEq = true, this.blendMode = -1, this.setBlendMode(0);
  }
  updateCheck(e, t) {
    const r = this.checks.indexOf(e);
    t && r === -1 ? this.checks.push(e) : !t && r !== -1 && this.checks.splice(r, 1);
  }
  static checkBlendMode(e, t) {
    e.setBlendMode(t.blendMode);
  }
  static checkPolygonOffset(e, t) {
    e.setPolygonOffset(1, t.polygonOffset);
  }
  destroy() {
    this.gl = null;
  }
};
vh.extension = { type: ne.RendererSystem, name: "state" };
let gu = vh;
de.add(gu);
class vu extends oi {
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
      this.runners[t] = new De(t);
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
      const n = r.find((a) => this._systemsHash[a] === s);
      s[e.name](t[n]);
    });
  }
  destroy() {
    Object.values(this.runners).forEach((e) => {
      e.destroy();
    }), this._systemsHash = {};
  }
}
const ur = class Xr {
  constructor(e) {
    this.renderer = e, this.count = 0, this.checkCount = 0, this.maxIdle = Xr.defaultMaxIdle, this.checkCountMax = Xr.defaultCheckCountMax, this.mode = Xr.defaultMode;
  }
  postrender() {
    this.renderer.objectRenderer.renderingToScreen && (this.count++, this.mode !== hn.MANUAL && (this.checkCount++, this.checkCount > this.checkCountMax && (this.checkCount = 0, this.run())));
  }
  run() {
    const e = this.renderer.texture, t = e.managedTextures;
    let r = false;
    for (let s = 0; s < t.length; s++) {
      const n = t[s];
      n.resource && this.count - n.touched > this.maxIdle && (e.destroyTexture(n, true), t[s] = null, r = true);
    }
    if (r) {
      let s = 0;
      for (let n = 0; n < t.length; n++) t[n] !== null && (t[s++] = t[n]);
      t.length = s;
    }
  }
  unload(e) {
    const t = this.renderer.texture, r = e._texture;
    r && !r.framebuffer && t.destroyTexture(r);
    for (let s = e.children.length - 1; s >= 0; s--) this.unload(e.children[s]);
  }
  destroy() {
    this.renderer = null;
  }
};
ur.defaultMode = hn.AUTO, ur.defaultMaxIdle = 3600, ur.defaultCheckCountMax = 600, ur.extension = { type: ne.RendererSystem, name: "textureGC" };
let bt = ur;
de.add(bt);
class Ms {
  constructor(e) {
    this.texture = e, this.width = -1, this.height = -1, this.dirtyId = -1, this.dirtyStyleId = -1, this.mipmap = false, this.wrapMode = 33071, this.type = se.UNSIGNED_BYTE, this.internalFormat = W.RGBA, this.samplerType = 0;
  }
}
function xu(i) {
  let e;
  return "WebGL2RenderingContext" in globalThis && i instanceof globalThis.WebGL2RenderingContext ? e = { [i.RGB]: X.FLOAT, [i.RGBA]: X.FLOAT, [i.ALPHA]: X.FLOAT, [i.LUMINANCE]: X.FLOAT, [i.LUMINANCE_ALPHA]: X.FLOAT, [i.R8]: X.FLOAT, [i.R8_SNORM]: X.FLOAT, [i.RG8]: X.FLOAT, [i.RG8_SNORM]: X.FLOAT, [i.RGB8]: X.FLOAT, [i.RGB8_SNORM]: X.FLOAT, [i.RGB565]: X.FLOAT, [i.RGBA4]: X.FLOAT, [i.RGB5_A1]: X.FLOAT, [i.RGBA8]: X.FLOAT, [i.RGBA8_SNORM]: X.FLOAT, [i.RGB10_A2]: X.FLOAT, [i.RGB10_A2UI]: X.FLOAT, [i.SRGB8]: X.FLOAT, [i.SRGB8_ALPHA8]: X.FLOAT, [i.R16F]: X.FLOAT, [i.RG16F]: X.FLOAT, [i.RGB16F]: X.FLOAT, [i.RGBA16F]: X.FLOAT, [i.R32F]: X.FLOAT, [i.RG32F]: X.FLOAT, [i.RGB32F]: X.FLOAT, [i.RGBA32F]: X.FLOAT, [i.R11F_G11F_B10F]: X.FLOAT, [i.RGB9_E5]: X.FLOAT, [i.R8I]: X.INT, [i.R8UI]: X.UINT, [i.R16I]: X.INT, [i.R16UI]: X.UINT, [i.R32I]: X.INT, [i.R32UI]: X.UINT, [i.RG8I]: X.INT, [i.RG8UI]: X.UINT, [i.RG16I]: X.INT, [i.RG16UI]: X.UINT, [i.RG32I]: X.INT, [i.RG32UI]: X.UINT, [i.RGB8I]: X.INT, [i.RGB8UI]: X.UINT, [i.RGB16I]: X.INT, [i.RGB16UI]: X.UINT, [i.RGB32I]: X.INT, [i.RGB32UI]: X.UINT, [i.RGBA8I]: X.INT, [i.RGBA8UI]: X.UINT, [i.RGBA16I]: X.INT, [i.RGBA16UI]: X.UINT, [i.RGBA32I]: X.INT, [i.RGBA32UI]: X.UINT, [i.DEPTH_COMPONENT16]: X.FLOAT, [i.DEPTH_COMPONENT24]: X.FLOAT, [i.DEPTH_COMPONENT32F]: X.FLOAT, [i.DEPTH_STENCIL]: X.FLOAT, [i.DEPTH24_STENCIL8]: X.FLOAT, [i.DEPTH32F_STENCIL8]: X.FLOAT } : e = { [i.RGB]: X.FLOAT, [i.RGBA]: X.FLOAT, [i.ALPHA]: X.FLOAT, [i.LUMINANCE]: X.FLOAT, [i.LUMINANCE_ALPHA]: X.FLOAT, [i.DEPTH_STENCIL]: X.FLOAT }, e;
}
function bu(i) {
  let e;
  return "WebGL2RenderingContext" in globalThis && i instanceof globalThis.WebGL2RenderingContext ? e = { [se.UNSIGNED_BYTE]: { [W.RGBA]: i.RGBA8, [W.RGB]: i.RGB8, [W.RG]: i.RG8, [W.RED]: i.R8, [W.RGBA_INTEGER]: i.RGBA8UI, [W.RGB_INTEGER]: i.RGB8UI, [W.RG_INTEGER]: i.RG8UI, [W.RED_INTEGER]: i.R8UI, [W.ALPHA]: i.ALPHA, [W.LUMINANCE]: i.LUMINANCE, [W.LUMINANCE_ALPHA]: i.LUMINANCE_ALPHA }, [se.BYTE]: { [W.RGBA]: i.RGBA8_SNORM, [W.RGB]: i.RGB8_SNORM, [W.RG]: i.RG8_SNORM, [W.RED]: i.R8_SNORM, [W.RGBA_INTEGER]: i.RGBA8I, [W.RGB_INTEGER]: i.RGB8I, [W.RG_INTEGER]: i.RG8I, [W.RED_INTEGER]: i.R8I }, [se.UNSIGNED_SHORT]: { [W.RGBA_INTEGER]: i.RGBA16UI, [W.RGB_INTEGER]: i.RGB16UI, [W.RG_INTEGER]: i.RG16UI, [W.RED_INTEGER]: i.R16UI, [W.DEPTH_COMPONENT]: i.DEPTH_COMPONENT16 }, [se.SHORT]: { [W.RGBA_INTEGER]: i.RGBA16I, [W.RGB_INTEGER]: i.RGB16I, [W.RG_INTEGER]: i.RG16I, [W.RED_INTEGER]: i.R16I }, [se.UNSIGNED_INT]: { [W.RGBA_INTEGER]: i.RGBA32UI, [W.RGB_INTEGER]: i.RGB32UI, [W.RG_INTEGER]: i.RG32UI, [W.RED_INTEGER]: i.R32UI, [W.DEPTH_COMPONENT]: i.DEPTH_COMPONENT24 }, [se.INT]: { [W.RGBA_INTEGER]: i.RGBA32I, [W.RGB_INTEGER]: i.RGB32I, [W.RG_INTEGER]: i.RG32I, [W.RED_INTEGER]: i.R32I }, [se.FLOAT]: { [W.RGBA]: i.RGBA32F, [W.RGB]: i.RGB32F, [W.RG]: i.RG32F, [W.RED]: i.R32F, [W.DEPTH_COMPONENT]: i.DEPTH_COMPONENT32F }, [se.HALF_FLOAT]: { [W.RGBA]: i.RGBA16F, [W.RGB]: i.RGB16F, [W.RG]: i.RG16F, [W.RED]: i.R16F }, [se.UNSIGNED_SHORT_5_6_5]: { [W.RGB]: i.RGB565 }, [se.UNSIGNED_SHORT_4_4_4_4]: { [W.RGBA]: i.RGBA4 }, [se.UNSIGNED_SHORT_5_5_5_1]: { [W.RGBA]: i.RGB5_A1 }, [se.UNSIGNED_INT_2_10_10_10_REV]: { [W.RGBA]: i.RGB10_A2, [W.RGBA_INTEGER]: i.RGB10_A2UI }, [se.UNSIGNED_INT_10F_11F_11F_REV]: { [W.RGB]: i.R11F_G11F_B10F }, [se.UNSIGNED_INT_5_9_9_9_REV]: { [W.RGB]: i.RGB9_E5 }, [se.UNSIGNED_INT_24_8]: { [W.DEPTH_STENCIL]: i.DEPTH24_STENCIL8 }, [se.FLOAT_32_UNSIGNED_INT_24_8_REV]: { [W.DEPTH_STENCIL]: i.DEPTH32F_STENCIL8 } } : e = { [se.UNSIGNED_BYTE]: { [W.RGBA]: i.RGBA, [W.RGB]: i.RGB, [W.ALPHA]: i.ALPHA, [W.LUMINANCE]: i.LUMINANCE, [W.LUMINANCE_ALPHA]: i.LUMINANCE_ALPHA }, [se.UNSIGNED_SHORT_5_6_5]: { [W.RGB]: i.RGB }, [se.UNSIGNED_SHORT_4_4_4_4]: { [W.RGBA]: i.RGBA }, [se.UNSIGNED_SHORT_5_5_5_1]: { [W.RGBA]: i.RGBA } }, e;
}
class xh {
  constructor(e) {
    this.renderer = e, this.boundTextures = [], this.currentLocation = -1, this.managedTextures = [], this._unknownBoundTextures = false, this.unknownTexture = new pe(), this.hasIntegerTextures = false;
  }
  contextChange() {
    const e = this.gl = this.renderer.gl;
    this.CONTEXT_UID = this.renderer.CONTEXT_UID, this.webGLVersion = this.renderer.context.webGLVersion, this.internalFormats = bu(e), this.samplerTypes = xu(e);
    const t = e.getParameter(e.MAX_TEXTURE_IMAGE_UNITS);
    this.boundTextures.length = t;
    for (let s = 0; s < t; s++) this.boundTextures[s] = null;
    this.emptyTextures = {};
    const r = new Ms(e.createTexture());
    e.bindTexture(e.TEXTURE_2D, r.texture), e.texImage2D(e.TEXTURE_2D, 0, e.RGBA, 1, 1, 0, e.RGBA, e.UNSIGNED_BYTE, new Uint8Array(4)), this.emptyTextures[e.TEXTURE_2D] = r, this.emptyTextures[e.TEXTURE_CUBE_MAP] = new Ms(e.createTexture()), e.bindTexture(e.TEXTURE_CUBE_MAP, this.emptyTextures[e.TEXTURE_CUBE_MAP].texture);
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
    this._unknownBoundTextures = true, this.hasIntegerTextures = false, this.currentLocation = -1;
    for (let e = 0; e < this.boundTextures.length; e++) this.boundTextures[e] = this.unknownTexture;
  }
  unbind(e) {
    const { gl: t, boundTextures: r } = this;
    if (this._unknownBoundTextures) {
      this._unknownBoundTextures = false;
      for (let s = 0; s < r.length; s++) r[s] === this.unknownTexture && this.bind(null, s);
    }
    for (let s = 0; s < r.length; s++) r[s] === e && (this.currentLocation !== s && (t.activeTexture(t.TEXTURE0 + s), this.currentLocation = s), t.bindTexture(e.target, this.emptyTextures[e.target].texture), r[s] = null);
  }
  ensureSamplerType(e) {
    const { boundTextures: t, hasIntegerTextures: r, CONTEXT_UID: s } = this;
    if (r) for (let n = e - 1; n >= 0; --n) {
      const a = t[n];
      a && a._glTextures[s].samplerType !== X.FLOAT && this.renderer.texture.unbind(a);
    }
  }
  initTexture(e) {
    const t = new Ms(this.gl.createTexture());
    return t.dirtyId = -1, e._glTextures[this.CONTEXT_UID] = t, this.managedTextures.push(e), e.on("dispose", this.destroyTexture, this), t;
  }
  initTextureType(e, t) {
    t.internalFormat = this.internalFormats[e.type]?.[e.format] ?? e.format, t.samplerType = this.samplerTypes[t.internalFormat] ?? X.FLOAT, this.webGLVersion === 2 && e.type === se.HALF_FLOAT ? t.type = this.gl.HALF_FLOAT : t.type = e.type;
  }
  updateTexture(e) {
    const t = e._glTextures[this.CONTEXT_UID];
    if (!t) return;
    const r = this.renderer;
    if (this.initTextureType(e, t), e.resource?.upload(r, e, t)) t.samplerType !== X.FLOAT && (this.hasIntegerTextures = true);
    else {
      const s = e.realWidth, n = e.realHeight, a = r.gl;
      (t.width !== s || t.height !== n || t.dirtyId < 0) && (t.width = s, t.height = n, a.texImage2D(e.target, 0, t.internalFormat, s, n, 0, e.format, t.type, null));
    }
    e.dirtyStyleId !== t.dirtyStyleId && this.updateTextureStyle(e), t.dirtyId = e.dirtyId;
  }
  destroyTexture(e, t) {
    const { gl: r } = this;
    if (e = e.castToBaseTexture(), e._glTextures[this.CONTEXT_UID] && (this.unbind(e), r.deleteTexture(e._glTextures[this.CONTEXT_UID].texture), e.off("dispose", this.destroyTexture, this), delete e._glTextures[this.CONTEXT_UID], !t)) {
      const s = this.managedTextures.indexOf(e);
      s !== -1 && Vr(this.managedTextures, s, 1);
    }
  }
  updateTextureStyle(e) {
    const t = e._glTextures[this.CONTEXT_UID];
    t && ((e.mipmap === It.POW2 || this.webGLVersion !== 2) && !e.isPowerOfTwo ? t.mipmap = false : t.mipmap = e.mipmap >= 1, this.webGLVersion !== 2 && !e.isPowerOfTwo ? t.wrapMode = on.CLAMP : t.wrapMode = e.wrapMode, e.resource?.style(this.renderer, e, t) || this.setStyle(e, t), t.dirtyStyleId = e.dirtyStyleId);
  }
  setStyle(e, t) {
    const r = this.gl;
    if (t.mipmap && e.mipmap !== It.ON_MANUAL && r.generateMipmap(e.target), r.texParameteri(e.target, r.TEXTURE_WRAP_S, t.wrapMode), r.texParameteri(e.target, r.TEXTURE_WRAP_T, t.wrapMode), t.mipmap) {
      r.texParameteri(e.target, r.TEXTURE_MIN_FILTER, e.scaleMode === Ze.LINEAR ? r.LINEAR_MIPMAP_LINEAR : r.NEAREST_MIPMAP_NEAREST);
      const s = this.renderer.context.extensions.anisotropicFiltering;
      if (s && e.anisotropicLevel > 0 && e.scaleMode === Ze.LINEAR) {
        const n = Math.min(e.anisotropicLevel, r.getParameter(s.MAX_TEXTURE_MAX_ANISOTROPY_EXT));
        r.texParameterf(e.target, s.TEXTURE_MAX_ANISOTROPY_EXT, n);
      }
    } else r.texParameteri(e.target, r.TEXTURE_MIN_FILTER, e.scaleMode === Ze.LINEAR ? r.LINEAR : r.NEAREST);
    r.texParameteri(e.target, r.TEXTURE_MAG_FILTER, e.scaleMode === Ze.LINEAR ? r.LINEAR : r.NEAREST);
  }
  destroy() {
    this.renderer = null;
  }
}
xh.extension = { type: ne.RendererSystem, name: "texture" };
de.add(xh);
class bh {
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
    const { gl: t, renderer: r, CONTEXT_UID: s } = this, n = t.createTransformFeedback();
    e._glTransformFeedbacks[s] = n, t.bindTransformFeedback(t.TRANSFORM_FEEDBACK, n);
    for (let a = 0; a < e.buffers.length; a++) {
      const o = e.buffers[a];
      o && (r.buffer.update(o), o._glBuffers[s].refCount++, t.bindBufferBase(t.TRANSFORM_FEEDBACK_BUFFER, a, o._glBuffers[s].buffer || null));
    }
    return t.bindTransformFeedback(t.TRANSFORM_FEEDBACK, null), e.disposeRunner.add(this), n;
  }
  disposeTransformFeedback(e, t) {
    const r = e._glTransformFeedbacks[this.CONTEXT_UID], s = this.gl;
    e.disposeRunner.remove(this);
    const n = this.renderer.buffer;
    if (n) for (let a = 0; a < e.buffers.length; a++) {
      const o = e.buffers[a];
      if (!o) continue;
      const h = o._glBuffers[this.CONTEXT_UID];
      h && (h.refCount--, h.refCount === 0 && !t && n.dispose(o, t));
    }
    r && (t || s.deleteTransformFeedback(r), delete e._glTransformFeedbacks[this.CONTEXT_UID]);
  }
  destroy() {
    this.renderer = null;
  }
}
bh.extension = { type: ne.RendererSystem, name: "transformFeedback" };
de.add(bh);
class ri {
  constructor(e) {
    this.renderer = e;
  }
  init(e) {
    this.screen = new me(0, 0, e.width, e.height), this.element = e.view || he.ADAPTER.createCanvas(), this.resolution = e.resolution || he.RESOLUTION, this.autoDensity = !!e.autoDensity;
  }
  resizeView(e, t) {
    this.element.width = Math.round(e * this.resolution), this.element.height = Math.round(t * this.resolution);
    const r = this.element.width / this.resolution, s = this.element.height / this.resolution;
    this.screen.width = r, this.screen.height = s, this.autoDensity && (this.element.style.width = `${r}px`, this.element.style.height = `${s}px`), this.renderer.emit("resize", r, s), this.renderer.runners.resize.emit(this.screen.width, this.screen.height);
  }
  destroy(e) {
    e && this.element.parentNode?.removeChild(this.element), this.renderer = null, this.element = null, this.screen = null;
  }
}
ri.defaultOptions = { width: 800, height: 600, resolution: void 0, autoDensity: false }, ri.extension = { type: [ne.RendererSystem, ne.CanvasRendererSystem], name: "_view" };
de.add(ri);
he.PREFER_ENV = Lt.WEBGL2;
he.STRICT_TEXTURE_CACHE = false;
he.RENDER_OPTIONS = { ...ei.defaultOptions, ...Qr.defaultOptions, ...ri.defaultOptions, ...ti.defaultOptions };
Object.defineProperties(he, { WRAP_MODE: { get() {
  return pe.defaultOptions.wrapMode;
}, set(i) {
  fe("7.1.0", "settings.WRAP_MODE is deprecated, use BaseTexture.defaultOptions.wrapMode"), pe.defaultOptions.wrapMode = i;
} }, SCALE_MODE: { get() {
  return pe.defaultOptions.scaleMode;
}, set(i) {
  fe("7.1.0", "settings.SCALE_MODE is deprecated, use BaseTexture.defaultOptions.scaleMode"), pe.defaultOptions.scaleMode = i;
} }, MIPMAP_TEXTURES: { get() {
  return pe.defaultOptions.mipmap;
}, set(i) {
  fe("7.1.0", "settings.MIPMAP_TEXTURES is deprecated, use BaseTexture.defaultOptions.mipmap"), pe.defaultOptions.mipmap = i;
} }, ANISOTROPIC_LEVEL: { get() {
  return pe.defaultOptions.anisotropicLevel;
}, set(i) {
  fe("7.1.0", "settings.ANISOTROPIC_LEVEL is deprecated, use BaseTexture.defaultOptions.anisotropicLevel"), pe.defaultOptions.anisotropicLevel = i;
} }, FILTER_RESOLUTION: { get() {
  return fe("7.1.0", "settings.FILTER_RESOLUTION is deprecated, use Filter.defaultResolution"), Ne.defaultResolution;
}, set(i) {
  Ne.defaultResolution = i;
} }, FILTER_MULTISAMPLE: { get() {
  return fe("7.1.0", "settings.FILTER_MULTISAMPLE is deprecated, use Filter.defaultMultisample"), Ne.defaultMultisample;
}, set(i) {
  Ne.defaultMultisample = i;
} }, SPRITE_MAX_TEXTURES: { get() {
  return xt.defaultMaxTextures;
}, set(i) {
  fe("7.1.0", "settings.SPRITE_MAX_TEXTURES is deprecated, use BatchRenderer.defaultMaxTextures"), xt.defaultMaxTextures = i;
} }, SPRITE_BATCH_SIZE: { get() {
  return xt.defaultBatchSize;
}, set(i) {
  fe("7.1.0", "settings.SPRITE_BATCH_SIZE is deprecated, use BatchRenderer.defaultBatchSize"), xt.defaultBatchSize = i;
} }, CAN_UPLOAD_SAME_BUFFER: { get() {
  return xt.canUploadSameBuffer;
}, set(i) {
  fe("7.1.0", "settings.CAN_UPLOAD_SAME_BUFFER is deprecated, use BatchRenderer.canUploadSameBuffer"), xt.canUploadSameBuffer = i;
} }, GC_MODE: { get() {
  return bt.defaultMode;
}, set(i) {
  fe("7.1.0", "settings.GC_MODE is deprecated, use TextureGCSystem.defaultMode"), bt.defaultMode = i;
} }, GC_MAX_IDLE: { get() {
  return bt.defaultMaxIdle;
}, set(i) {
  fe("7.1.0", "settings.GC_MAX_IDLE is deprecated, use TextureGCSystem.defaultMaxIdle"), bt.defaultMaxIdle = i;
} }, GC_MAX_CHECK_COUNT: { get() {
  return bt.defaultCheckCountMax;
}, set(i) {
  fe("7.1.0", "settings.GC_MAX_CHECK_COUNT is deprecated, use TextureGCSystem.defaultCheckCountMax"), bt.defaultCheckCountMax = i;
} }, PRECISION_VERTEX: { get() {
  return Tt.defaultVertexPrecision;
}, set(i) {
  fe("7.1.0", "settings.PRECISION_VERTEX is deprecated, use Program.defaultVertexPrecision"), Tt.defaultVertexPrecision = i;
} }, PRECISION_FRAGMENT: { get() {
  return Tt.defaultFragmentPrecision;
}, set(i) {
  fe("7.1.0", "settings.PRECISION_FRAGMENT is deprecated, use Program.defaultFragmentPrecision"), Tt.defaultFragmentPrecision = i;
} } });
var ii = ((i) => (i[i.INTERACTION = 50] = "INTERACTION", i[i.HIGH = 25] = "HIGH", i[i.NORMAL = 0] = "NORMAL", i[i.LOW = -25] = "LOW", i[i.UTILITY = -50] = "UTILITY", i))(ii || {});
class Ls {
  constructor(e, t = null, r = 0, s = false) {
    this.next = null, this.previous = null, this._destroyed = false, this.fn = e, this.context = t, this.priority = r, this.once = s;
  }
  match(e, t = null) {
    return this.fn === e && this.context === t;
  }
  emit(e) {
    this.fn && (this.context ? this.fn.call(this.context, e) : this.fn(e));
    const t = this.next;
    return this.once && this.destroy(true), this._destroyed && (this.next = null), t;
  }
  connect(e) {
    this.previous = e, e.next && (e.next.previous = this), this.next = e.next, e.next = this;
  }
  destroy(e = false) {
    this._destroyed = true, this.fn = null, this.context = null, this.previous && (this.previous.next = this.next), this.next && (this.next.previous = this.previous);
    const t = this.next;
    return this.next = e ? null : t, this.previous = null, t;
  }
}
const _h = class Le {
  constructor() {
    this.autoStart = false, this.deltaTime = 1, this.lastTime = -1, this.speed = 1, this.started = false, this._requestId = null, this._maxElapsedMS = 100, this._minElapsedMS = 0, this._protected = false, this._lastFrame = -1, this._head = new Ls(null, null, 1 / 0), this.deltaMS = 1 / Le.targetFPMS, this.elapsedMS = 1 / Le.targetFPMS, this._tick = (e) => {
      this._requestId = null, this.started && (this.update(e), this.started && this._requestId === null && this._head.next && (this._requestId = requestAnimationFrame(this._tick)));
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
  add(e, t, r = ii.NORMAL) {
    return this._addListener(new Ls(e, t, r));
  }
  addOnce(e, t, r = ii.NORMAL) {
    return this._addListener(new Ls(e, t, r, true));
  }
  _addListener(e) {
    let t = this._head.next, r = this._head;
    if (!t) e.connect(r);
    else {
      for (; t; ) {
        if (e.priority > t.priority) {
          e.connect(r);
          break;
        }
        r = t, t = t.next;
      }
      e.previous || e.connect(r);
    }
    return this._startIfPossible(), this;
  }
  remove(e, t) {
    let r = this._head.next;
    for (; r; ) r.match(e, t) ? r = r.destroy() : r = r.next;
    return this._head.next || this._cancelIfNeeded(), this;
  }
  get count() {
    if (!this._head) return 0;
    let e = 0, t = this._head;
    for (; t = t.next; ) e++;
    return e;
  }
  start() {
    this.started || (this.started = true, this._requestIfNeeded());
  }
  stop() {
    this.started && (this.started = false, this._cancelIfNeeded());
  }
  destroy() {
    if (!this._protected) {
      this.stop();
      let e = this._head.next;
      for (; e; ) e = e.destroy(true);
      this._head.destroy(), this._head = null;
    }
  }
  update(e = performance.now()) {
    let t;
    if (e > this.lastTime) {
      if (t = this.elapsedMS = e - this.lastTime, t > this._maxElapsedMS && (t = this._maxElapsedMS), t *= this.speed, this._minElapsedMS) {
        const n = e - this._lastFrame | 0;
        if (n < this._minElapsedMS) return;
        this._lastFrame = e - n % this._minElapsedMS;
      }
      this.deltaMS = t, this.deltaTime = this.deltaMS * Le.targetFPMS;
      const r = this._head;
      let s = r.next;
      for (; s; ) s = s.emit(this.deltaTime);
      r.next || this._cancelIfNeeded();
    } else this.deltaTime = this.deltaMS = this.elapsedMS = 0;
    this.lastTime = e;
  }
  get FPS() {
    return 1e3 / this.elapsedMS;
  }
  get minFPS() {
    return 1e3 / this._maxElapsedMS;
  }
  set minFPS(e) {
    const t = Math.min(this.maxFPS, e), r = Math.min(Math.max(0, t) / 1e3, Le.targetFPMS);
    this._maxElapsedMS = 1 / r;
  }
  get maxFPS() {
    return this._minElapsedMS ? Math.round(1e3 / this._minElapsedMS) : 0;
  }
  set maxFPS(e) {
    if (e === 0) this._minElapsedMS = 0;
    else {
      const t = Math.max(this.minFPS, e);
      this._minElapsedMS = 1 / (t / 1e3);
    }
  }
  static get shared() {
    if (!Le._shared) {
      const e = Le._shared = new Le();
      e.autoStart = true, e._protected = true;
    }
    return Le._shared;
  }
  static get system() {
    if (!Le._system) {
      const e = Le._system = new Le();
      e.autoStart = true, e._protected = true;
    }
    return Le._system;
  }
};
_h.targetFPMS = 0.06;
let ot = _h;
Object.defineProperties(he, { TARGET_FPMS: { get() {
  return ot.targetFPMS;
}, set(i) {
  fe("7.1.0", "settings.TARGET_FPMS is deprecated, use Ticker.targetFPMS"), ot.targetFPMS = i;
} } });
class Th {
  static init(e) {
    e = Object.assign({ autoStart: true, sharedTicker: false }, e), Object.defineProperty(this, "ticker", { set(t) {
      this._ticker && this._ticker.remove(this.render, this), this._ticker = t, t && t.add(this.render, this, ii.LOW);
    }, get() {
      return this._ticker;
    } }), this.stop = () => {
      this._ticker.stop();
    }, this.start = () => {
      this._ticker.start();
    }, this._ticker = null, this.ticker = e.sharedTicker ? ot.shared : new ot(), e.autoStart && this.start();
  }
  static destroy() {
    if (this._ticker) {
      const e = this._ticker;
      this.ticker = null, e.destroy();
    }
  }
}
Th.extension = ne.Application;
de.add(Th);
const Eh = [];
de.handleByList(ne.Renderer, Eh);
function _u(i) {
  for (const e of Eh) if (e.test(i)) return new e(i);
  throw new Error("Unable to auto-detect a suitable renderer.");
}
var Tu = `attribute vec2 aVertexPosition;

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
`;
const Eu = Tu;
class wh {
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
    t >= _e.HIGH ? this.multisample = _e.HIGH : t >= _e.MEDIUM ? this.multisample = _e.MEDIUM : t >= _e.LOW ? this.multisample = _e.LOW : this.multisample = _e.NONE;
  }
  destroy() {
  }
}
wh.extension = { type: ne.RendererSystem, name: "_multisample" };
de.add(wh);
class wu {
  constructor(e) {
    this.buffer = e || null, this.updateID = -1, this.byteLength = -1, this.refCount = 0;
  }
}
class Sh {
  constructor(e) {
    this.renderer = e, this.managedBuffers = {}, this.boundBufferBases = {};
  }
  destroy() {
    this.renderer = null;
  }
  contextChange() {
    this.disposeAll(true), this.gl = this.renderer.gl, this.CONTEXT_UID = this.renderer.CONTEXT_UID;
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
      const n = e._glBuffers[s] || this.createGLBuffer(e);
      this.boundBufferBases[t] = e, r.bindBufferBase(r.UNIFORM_BUFFER, t, n.buffer);
    }
  }
  bindBufferRange(e, t, r) {
    const { gl: s, CONTEXT_UID: n } = this;
    r = r || 0;
    const a = e._glBuffers[n] || this.createGLBuffer(e);
    s.bindBufferRange(s.UNIFORM_BUFFER, t || 0, a.buffer, r * 256, 256);
  }
  update(e) {
    const { gl: t, CONTEXT_UID: r } = this, s = e._glBuffers[r] || this.createGLBuffer(e);
    if (e._updateID !== s.updateID) if (s.updateID = e._updateID, t.bindBuffer(e.type, s.buffer), s.byteLength >= e.data.byteLength) t.bufferSubData(e.type, 0, e.data);
    else {
      const n = e.static ? t.STATIC_DRAW : t.DYNAMIC_DRAW;
      s.byteLength = e.data.byteLength, t.bufferData(e.type, e.data, n);
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
    return e._glBuffers[t] = new wu(r.createBuffer()), this.managedBuffers[e.id] = e, e.disposeRunner.add(this), e._glBuffers[t];
  }
}
Sh.extension = { type: ne.RendererSystem, name: "buffer" };
de.add(Sh);
class Ah {
  constructor(e) {
    this.renderer = e;
  }
  render(e, t) {
    const r = this.renderer;
    let s, n, a, o;
    if (t && (s = t.renderTexture, n = t.clear, a = t.transform, o = t.skipUpdateTransform), this.renderingToScreen = !s, r.runners.prerender.emit(), r.emit("prerender"), r.projection.transform = a, !r.context.isLost) {
      if (s || (this.lastObjectRendered = e), !o) {
        const h = e.enableTempParent();
        e.updateTransform(), e.disableTempParent(h);
      }
      r.renderTexture.bind(s), r.batch.currentRenderer.start(), (n ?? r.background.clearBeforeRender) && r.renderTexture.clear(), e.render(r), r.batch.currentRenderer.flush(), s && (t.blit && r.framebuffer.blit(), s.baseTexture.update()), r.runners.postrender.emit(), r.projection.transform = null, r.emit("postrender");
    }
  }
  destroy() {
    this.renderer = null, this.lastObjectRendered = null;
  }
}
Ah.extension = { type: ne.RendererSystem, name: "objectRenderer" };
de.add(Ah);
const qr = class js extends vu {
  constructor(e) {
    super(), this.type = Mo.WEBGL, e = Object.assign({}, he.RENDER_OPTIONS, e), this.gl = null, this.CONTEXT_UID = 0, this.globalUniforms = new Xe({ projectionMatrix: new we() }, true);
    const t = { runners: ["init", "destroy", "contextChange", "resolutionChange", "reset", "update", "postrender", "prerender", "resize"], systems: js.__systems, priority: ["_view", "textureGenerator", "background", "_plugin", "startup", "context", "state", "texture", "buffer", "geometry", "framebuffer", "transformFeedback", "mask", "scissor", "stencil", "projection", "textureGC", "filter", "renderTexture", "batch", "objectRenderer", "_multisample"] };
    this.setup(t), "useContextAlpha" in e && (fe("7.0.0", "options.useContextAlpha is deprecated, use options.premultipliedAlpha and options.backgroundAlpha instead"), e.premultipliedAlpha = e.useContextAlpha && e.useContextAlpha !== "notMultiplied", e.backgroundAlpha = e.useContextAlpha === false ? 1 : e.backgroundAlpha), this._plugin.rendererPlugins = js.__plugins, this.options = e, this.startup.run(this.options);
  }
  static test(e) {
    return e?.forceCanvas ? false : Xl();
  }
  render(e, t) {
    this.objectRenderer.render(e, t);
  }
  resize(e, t) {
    this._view.resizeView(e, t);
  }
  reset() {
    return this.runners.reset.emit(), this;
  }
  clear() {
    this.renderTexture.bind(), this.renderTexture.clear();
  }
  destroy(e = false) {
    this.runners.destroy.items.reverse(), this.emitWithCustomOptions(this.runners.destroy, { _view: e }), super.destroy();
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
  set resolution(e) {
    this._view.resolution = e, this.runners.resolutionChange.emit(e);
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
    return fe("7.0.0", "renderer.clearBeforeRender has been deprecated, please use renderer.background.clearBeforeRender instead."), this.background.clearBeforeRender;
  }
  get useContextAlpha() {
    return fe("7.0.0", "renderer.useContextAlpha has been deprecated, please use renderer.context.premultipliedAlpha instead."), this.context.useContextAlpha;
  }
  get preserveDrawingBuffer() {
    return fe("7.0.0", "renderer.preserveDrawingBuffer has been deprecated, we cannot truly know this unless pixi created the context"), this.context.preserveDrawingBuffer;
  }
  get backgroundColor() {
    return fe("7.0.0", "renderer.backgroundColor has been deprecated, use renderer.background.color instead."), this.background.color;
  }
  set backgroundColor(e) {
    fe("7.0.0", "renderer.backgroundColor has been deprecated, use renderer.background.color instead."), this.background.color = e;
  }
  get backgroundAlpha() {
    return fe("7.0.0", "renderer.backgroundAlpha has been deprecated, use renderer.background.alpha instead."), this.background.alpha;
  }
  set backgroundAlpha(e) {
    fe("7.0.0", "renderer.backgroundAlpha has been deprecated, use renderer.background.alpha instead."), this.background.alpha = e;
  }
  get powerPreference() {
    return fe("7.0.0", "renderer.powerPreference has been deprecated, we can only know this if pixi creates the context"), this.context.powerPreference;
  }
  generateTexture(e, t) {
    return this.textureGenerator.generateTexture(e, t);
  }
};
qr.extension = { type: ne.Renderer, priority: 1 }, qr.__plugins = {}, qr.__systems = {};
let pn = qr;
de.handleByMap(ne.RendererPlugin, pn.__plugins);
de.handleByMap(ne.RendererSystem, pn.__systems);
de.add(pn);
class Ih extends vr {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    super(r, s), this.items = [], this.itemDirtyIds = [];
    for (let n = 0; n < e; n++) {
      const a = new pe();
      this.items.push(a), this.itemDirtyIds.push(-2);
    }
    this.length = e, this._load = null, this.baseTexture = null;
  }
  initFromArray(e, t) {
    for (let r = 0; r < this.length; r++) e[r] && (e[r].castToBaseTexture ? this.addBaseTextureAt(e[r].castToBaseTexture(), r) : e[r] instanceof vr ? this.addResourceAt(e[r], r) : this.addResourceAt(Xo(e[r], t), r));
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
}
class Su extends Ih {
  constructor(e, t) {
    const { width: r, height: s } = t || {};
    let n, a;
    Array.isArray(e) ? (n = e, a = e.length) : a = e, super(a, { width: r, height: s }), n && this.initFromArray(n, t);
  }
  addBaseTextureAt(e, t) {
    if (e.resource) this.addResourceAt(e.resource, t);
    else throw new Error("ArrayResource does not support RenderTexture");
    return this;
  }
  bind(e) {
    super.bind(e), e.target = jt.TEXTURE_2D_ARRAY;
  }
  upload(e, t, r) {
    const { length: s, itemDirtyIds: n, items: a } = this, { gl: o } = e;
    r.dirtyId < 0 && o.texImage3D(o.TEXTURE_2D_ARRAY, 0, r.internalFormat, this._width, this._height, s, 0, t.format, r.type, null);
    for (let h = 0; h < s; h++) {
      const l = a[h];
      n[h] < l.dirtyId && (n[h] = l.dirtyId, l.valid && o.texSubImage3D(o.TEXTURE_2D_ARRAY, 0, 0, 0, h, l.resource.width, l.resource.height, 1, t.format, r.type, l.resource.source));
    }
    return true;
  }
}
class Au extends Qe {
  constructor(e) {
    super(e);
  }
  static test(e) {
    const { OffscreenCanvas: t } = globalThis;
    return t && e instanceof t ? true : globalThis.HTMLCanvasElement && e instanceof HTMLCanvasElement;
  }
}
const Rh = class dr extends Ih {
  constructor(e, t) {
    const { width: r, height: s, autoLoad: n, linkBaseTexture: a } = t || {};
    if (e && e.length !== dr.SIDES) throw new Error(`Invalid length. Got ${e.length}, expected 6`);
    super(6, { width: r, height: s });
    for (let o = 0; o < dr.SIDES; o++) this.items[o].target = jt.TEXTURE_CUBE_MAP_POSITIVE_X + o;
    this.linkBaseTexture = a !== false, e && this.initFromArray(e, t), n !== false && this.load();
  }
  bind(e) {
    super.bind(e), e.target = jt.TEXTURE_CUBE_MAP;
  }
  addBaseTextureAt(e, t, r) {
    if (r === void 0 && (r = this.linkBaseTexture), !this.items[t]) throw new Error(`Index ${t} is out of bounds`);
    if (!this.linkBaseTexture || e.parentTextureArray || Object.keys(e._glTextures).length > 0) if (e.resource) this.addResourceAt(e.resource, t);
    else throw new Error("CubeResource does not support copying of renderTexture.");
    else e.target = jt.TEXTURE_CUBE_MAP_POSITIVE_X + t, e.parentTextureArray = this.baseTexture, this.items[t] = e;
    return e.valid && !this.valid && this.resize(e.realWidth, e.realHeight), this.items[t] = e, this;
  }
  upload(e, t, r) {
    const s = this.itemDirtyIds;
    for (let n = 0; n < dr.SIDES; n++) {
      const a = this.items[n];
      (s[n] < a.dirtyId || r.dirtyId < t.dirtyId) && (a.valid && a.resource ? (a.resource.upload(e, a, r), s[n] = a.dirtyId) : s[n] < -1 && (e.gl.texImage2D(a.target, 0, r.internalFormat, t.realWidth, t.realHeight, 0, t.format, r.type, null), s[n] = -1));
    }
    return true;
  }
  static test(e) {
    return Array.isArray(e) && e.length === dr.SIDES;
  }
};
Rh.SIDES = 6;
let Iu = Rh;
class Wt extends Qe {
  constructor(e, t) {
    t = t || {};
    let r, s, n;
    typeof e == "string" ? (r = Wt.EMPTY, s = e, n = true) : (r = e, s = null, n = false), super(r), this.url = s, this.crossOrigin = t.crossOrigin ?? true, this.alphaMode = typeof t.alphaMode == "number" ? t.alphaMode : null, this.ownsImageBitmap = t.ownsImageBitmap ?? n, this._load = null, t.autoLoad !== false && this.load();
  }
  load() {
    return this._load ? this._load : (this._load = new Promise(async (e, t) => {
      if (this.url === null) {
        e(this);
        return;
      }
      try {
        const r = await he.ADAPTER.fetch(this.url, { mode: this.crossOrigin ? "cors" : "no-cors" });
        if (this.destroyed) return;
        const s = await r.blob();
        if (this.destroyed) return;
        const n = await createImageBitmap(s, { premultiplyAlpha: this.alphaMode === null || this.alphaMode === Pt.UNPACK ? "premultiply" : "none" });
        if (this.destroyed) {
          n.close();
          return;
        }
        this.source = n, this.update(), e(this);
      } catch (r) {
        if (this.destroyed) return;
        t(r), this.onError.emit(r);
      }
    }), this._load);
  }
  upload(e, t, r) {
    return this.source instanceof ImageBitmap ? (typeof this.alphaMode == "number" && (t.alphaMode = this.alphaMode), super.upload(e, t, r)) : (this.load(), false);
  }
  dispose() {
    this.ownsImageBitmap && this.source instanceof ImageBitmap && this.source.close(), super.dispose(), this._load = null;
  }
  static test(e) {
    return !!globalThis.createImageBitmap && typeof ImageBitmap < "u" && (typeof e == "string" || e instanceof ImageBitmap);
  }
  static get EMPTY() {
    return Wt._EMPTY = Wt._EMPTY ?? he.ADAPTER.createCanvas(0, 0), Wt._EMPTY;
  }
}
const Ys = class jr extends Qe {
  constructor(e, t) {
    t = t || {}, super(he.ADAPTER.createCanvas()), this._width = 0, this._height = 0, this.svg = e, this.scale = t.scale || 1, this._overrideWidth = t.width, this._overrideHeight = t.height, this._resolve = null, this._crossorigin = t.crossorigin, this._load = null, t.autoLoad !== false && this.load();
  }
  load() {
    return this._load ? this._load : (this._load = new Promise((e) => {
      if (this._resolve = () => {
        this.update(), e(this);
      }, jr.SVG_XML.test(this.svg.trim())) {
        if (!btoa) throw new Error("Your browser doesn't support base64 conversions.");
        this.svg = `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(this.svg)))}`;
      }
      this._loadSvg();
    }), this._load);
  }
  _loadSvg() {
    const e = new Image();
    Qe.crossOrigin(e, this.svg, this._crossorigin), e.src = this.svg, e.onerror = (t) => {
      this._resolve && (e.onerror = null, this.onError.emit(t));
    }, e.onload = () => {
      if (!this._resolve) return;
      const t = e.width, r = e.height;
      if (!t || !r) throw new Error("The SVG image must have width and height defined (in pixels), canvas API needs them.");
      let s = t * this.scale, n = r * this.scale;
      (this._overrideWidth || this._overrideHeight) && (s = this._overrideWidth || this._overrideHeight / r * t, n = this._overrideHeight || this._overrideWidth / t * r), s = Math.round(s), n = Math.round(n);
      const a = this.source;
      a.width = s, a.height = n, a._pixiId = `canvas_${gr()}`, a.getContext("2d").drawImage(e, 0, 0, t, r, 0, 0, s, n), this._resolve(), this._resolve = null;
    };
  }
  static getSize(e) {
    const t = jr.SVG_SIZE.exec(e), r = {};
    return t && (r[t[1]] = Math.round(parseFloat(t[3])), r[t[5]] = Math.round(parseFloat(t[7]))), r;
  }
  dispose() {
    super.dispose(), this._resolve = null, this._crossorigin = null;
  }
  static test(e, t) {
    return t === "svg" || typeof e == "string" && e.startsWith("data:image/svg+xml") || typeof e == "string" && jr.SVG_XML.test(e);
  }
};
Ys.SVG_XML = /^(<\?xml[^?]+\?>)?\s*(<!--[^(-->)]*-->)?\s*\<svg/m, Ys.SVG_SIZE = /<svg[^>]*(?:\s(width|height)=('|")(\d*(?:\.\d+)?)(?:px)?('|"))[^>]*(?:\s(width|height)=('|")(\d*(?:\.\d+)?)(?:px)?('|"))[^>]*>/i;
let Ru = Ys;
class Cu extends Qe {
  constructor(e) {
    super(e);
  }
  static test(e) {
    return !!globalThis.VideoFrame && e instanceof globalThis.VideoFrame;
  }
}
const Ks = class Zs extends Qe {
  constructor(e, t) {
    if (t = t || {}, !(e instanceof HTMLVideoElement)) {
      const r = document.createElement("video");
      t.autoLoad !== false && r.setAttribute("preload", "auto"), t.playsinline !== false && (r.setAttribute("webkit-playsinline", ""), r.setAttribute("playsinline", "")), t.muted === true && (r.setAttribute("muted", ""), r.muted = true), t.loop === true && r.setAttribute("loop", ""), t.autoPlay !== false && r.setAttribute("autoplay", ""), typeof e == "string" && (e = [e]);
      const s = e[0].src || e[0];
      Qe.crossOrigin(r, s, t.crossorigin);
      for (let n = 0; n < e.length; ++n) {
        const a = document.createElement("source");
        let { src: o, mime: h } = e[n];
        if (o = o || e[n], o.startsWith("data:")) h = o.slice(5, o.indexOf(";"));
        else if (!o.startsWith("blob:")) {
          const l = o.split("?").shift().toLowerCase(), c = l.slice(l.lastIndexOf(".") + 1);
          h = h || Zs.MIME_TYPES[c] || `video/${c}`;
        }
        a.src = o, h && (a.type = h), r.appendChild(a);
      }
      e = r;
    }
    super(e), this.noSubImage = true, this._autoUpdate = true, this._isConnectedToTicker = false, this._updateFPS = t.updateFPS || 0, this._msToNextUpdate = 0, this.autoPlay = t.autoPlay !== false, this._videoFrameRequestCallback = this._videoFrameRequestCallback.bind(this), this._videoFrameRequestCallbackHandle = null, this._load = null, this._resolve = null, this._reject = null, this._onCanPlay = this._onCanPlay.bind(this), this._onError = this._onError.bind(this), this._onPlayStart = this._onPlayStart.bind(this), this._onPlayStop = this._onPlayStop.bind(this), this._onSeeked = this._onSeeked.bind(this), t.autoLoad !== false && this.load();
  }
  update(e = 0) {
    if (!this.destroyed) {
      if (this._updateFPS) {
        const t = ot.shared.elapsedMS * this.source.playbackRate;
        this._msToNextUpdate = Math.floor(this._msToNextUpdate - t);
      }
      (!this._updateFPS || this._msToNextUpdate <= 0) && (super.update(), this._msToNextUpdate = this._updateFPS ? Math.floor(1e3 / this._updateFPS) : 0);
    }
  }
  _videoFrameRequestCallback() {
    this.update(), this.destroyed ? this._videoFrameRequestCallbackHandle = null : this._videoFrameRequestCallbackHandle = this.source.requestVideoFrameCallback(this._videoFrameRequestCallback);
  }
  load() {
    if (this._load) return this._load;
    const e = this.source;
    return (e.readyState === e.HAVE_ENOUGH_DATA || e.readyState === e.HAVE_FUTURE_DATA) && e.width && e.height && (e.complete = true), e.addEventListener("play", this._onPlayStart), e.addEventListener("pause", this._onPlayStop), e.addEventListener("seeked", this._onSeeked), this._isSourceReady() ? this._onCanPlay() : (e.addEventListener("canplay", this._onCanPlay), e.addEventListener("canplaythrough", this._onCanPlay), e.addEventListener("error", this._onError, true)), this._load = new Promise((t, r) => {
      this.valid ? t(this) : (this._resolve = t, this._reject = r, e.load());
    }), this._load;
  }
  _onError(e) {
    this.source.removeEventListener("error", this._onError, true), this.onError.emit(e), this._reject && (this._reject(e), this._reject = null, this._resolve = null);
  }
  _isSourcePlaying() {
    const e = this.source;
    return !e.paused && !e.ended;
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
    const e = this.source;
    e.removeEventListener("canplay", this._onCanPlay), e.removeEventListener("canplaythrough", this._onCanPlay);
    const t = this.valid;
    this._msToNextUpdate = 0, this.update(), this._msToNextUpdate = 0, !t && this._resolve && (this._resolve(this), this._resolve = null, this._reject = null), this._isSourcePlaying() ? this._onPlayStart() : this.autoPlay && e.play();
  }
  dispose() {
    this._configureAutoUpdate();
    const e = this.source;
    e && (e.removeEventListener("play", this._onPlayStart), e.removeEventListener("pause", this._onPlayStop), e.removeEventListener("seeked", this._onSeeked), e.removeEventListener("canplay", this._onCanPlay), e.removeEventListener("canplaythrough", this._onCanPlay), e.removeEventListener("error", this._onError, true), e.pause(), e.src = "", e.load()), super.dispose();
  }
  get autoUpdate() {
    return this._autoUpdate;
  }
  set autoUpdate(e) {
    e !== this._autoUpdate && (this._autoUpdate = e, this._configureAutoUpdate());
  }
  get updateFPS() {
    return this._updateFPS;
  }
  set updateFPS(e) {
    e !== this._updateFPS && (this._updateFPS = e, this._configureAutoUpdate());
  }
  _configureAutoUpdate() {
    this._autoUpdate && this._isSourcePlaying() ? !this._updateFPS && this.source.requestVideoFrameCallback ? (this._isConnectedToTicker && (ot.shared.remove(this.update, this), this._isConnectedToTicker = false, this._msToNextUpdate = 0), this._videoFrameRequestCallbackHandle === null && (this._videoFrameRequestCallbackHandle = this.source.requestVideoFrameCallback(this._videoFrameRequestCallback))) : (this._videoFrameRequestCallbackHandle !== null && (this.source.cancelVideoFrameCallback(this._videoFrameRequestCallbackHandle), this._videoFrameRequestCallbackHandle = null), this._isConnectedToTicker || (ot.shared.add(this.update, this), this._isConnectedToTicker = true, this._msToNextUpdate = 0)) : (this._videoFrameRequestCallbackHandle !== null && (this.source.cancelVideoFrameCallback(this._videoFrameRequestCallbackHandle), this._videoFrameRequestCallbackHandle = null), this._isConnectedToTicker && (ot.shared.remove(this.update, this), this._isConnectedToTicker = false, this._msToNextUpdate = 0));
  }
  static test(e, t) {
    return globalThis.HTMLVideoElement && e instanceof HTMLVideoElement || Zs.TYPES.includes(t);
  }
};
Ks.TYPES = ["mp4", "m4v", "webm", "ogg", "ogv", "h264", "avi", "mov"], Ks.MIME_TYPES = { ogv: "video/ogg", mov: "video/quicktime", m4v: "video/mp4" };
let Mu = Ks;
Gs.push(Wt, ih, Au, Mu, Cu, Ru, qo, Iu, Su);
class Js {
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
    return this.minX > this.maxX || this.minY > this.maxY ? me.EMPTY : (e = e || new me(0, 0, 1, 1), e.x = this.minX, e.y = this.minY, e.width = this.maxX - this.minX, e.height = this.maxY - this.minY, e);
  }
  addPoint(e) {
    this.minX = Math.min(this.minX, e.x), this.maxX = Math.max(this.maxX, e.x), this.minY = Math.min(this.minY, e.y), this.maxY = Math.max(this.maxY, e.y);
  }
  addPointMatrix(e, t) {
    const { a: r, b: s, c: n, d: a, tx: o, ty: h } = e, l = r * t.x + n * t.y + o, c = s * t.x + a * t.y + h;
    this.minX = Math.min(this.minX, l), this.maxX = Math.max(this.maxX, l), this.minY = Math.min(this.minY, c), this.maxY = Math.max(this.maxY, c);
  }
  addQuad(e) {
    let t = this.minX, r = this.minY, s = this.maxX, n = this.maxY, a = e[0], o = e[1];
    t = a < t ? a : t, r = o < r ? o : r, s = a > s ? a : s, n = o > n ? o : n, a = e[2], o = e[3], t = a < t ? a : t, r = o < r ? o : r, s = a > s ? a : s, n = o > n ? o : n, a = e[4], o = e[5], t = a < t ? a : t, r = o < r ? o : r, s = a > s ? a : s, n = o > n ? o : n, a = e[6], o = e[7], t = a < t ? a : t, r = o < r ? o : r, s = a > s ? a : s, n = o > n ? o : n, this.minX = t, this.minY = r, this.maxX = s, this.maxY = n;
  }
  addFrame(e, t, r, s, n) {
    this.addFrameMatrix(e.worldTransform, t, r, s, n);
  }
  addFrameMatrix(e, t, r, s, n) {
    const a = e.a, o = e.b, h = e.c, l = e.d, c = e.tx, u = e.ty;
    let m = this.minX, y = this.minY, d = this.maxX, p = this.maxY, f = a * t + h * r + c, _ = o * t + l * r + u;
    m = f < m ? f : m, y = _ < y ? _ : y, d = f > d ? f : d, p = _ > p ? _ : p, f = a * s + h * r + c, _ = o * s + l * r + u, m = f < m ? f : m, y = _ < y ? _ : y, d = f > d ? f : d, p = _ > p ? _ : p, f = a * t + h * n + c, _ = o * t + l * n + u, m = f < m ? f : m, y = _ < y ? _ : y, d = f > d ? f : d, p = _ > p ? _ : p, f = a * s + h * n + c, _ = o * s + l * n + u, m = f < m ? f : m, y = _ < y ? _ : y, d = f > d ? f : d, p = _ > p ? _ : p, this.minX = m, this.minY = y, this.maxX = d, this.maxY = p;
  }
  addVertexData(e, t, r) {
    let s = this.minX, n = this.minY, a = this.maxX, o = this.maxY;
    for (let h = t; h < r; h += 2) {
      const l = e[h], c = e[h + 1];
      s = l < s ? l : s, n = c < n ? c : n, a = l > a ? l : a, o = c > o ? c : o;
    }
    this.minX = s, this.minY = n, this.maxX = a, this.maxY = o;
  }
  addVertices(e, t, r, s) {
    this.addVerticesMatrix(e.worldTransform, t, r, s);
  }
  addVerticesMatrix(e, t, r, s, n = 0, a = n) {
    const o = e.a, h = e.b, l = e.c, c = e.d, u = e.tx, m = e.ty;
    let y = this.minX, d = this.minY, p = this.maxX, f = this.maxY;
    for (let _ = r; _ < s; _ += 2) {
      const T = t[_], I = t[_ + 1], k = o * T + l * I + u, g = c * I + h * T + m;
      y = Math.min(y, k - n), p = Math.max(p, k + n), d = Math.min(d, g - a), f = Math.max(f, g + a);
    }
    this.minX = y, this.minY = d, this.maxX = p, this.maxY = f;
  }
  addBounds(e) {
    const t = this.minX, r = this.minY, s = this.maxX, n = this.maxY;
    this.minX = e.minX < t ? e.minX : t, this.minY = e.minY < r ? e.minY : r, this.maxX = e.maxX > s ? e.maxX : s, this.maxY = e.maxY > n ? e.maxY : n;
  }
  addBoundsMask(e, t) {
    const r = e.minX > t.minX ? e.minX : t.minX, s = e.minY > t.minY ? e.minY : t.minY, n = e.maxX < t.maxX ? e.maxX : t.maxX, a = e.maxY < t.maxY ? e.maxY : t.maxY;
    if (r <= n && s <= a) {
      const o = this.minX, h = this.minY, l = this.maxX, c = this.maxY;
      this.minX = r < o ? r : o, this.minY = s < h ? s : h, this.maxX = n > l ? n : l, this.maxY = a > c ? a : c;
    }
  }
  addBoundsMatrix(e, t) {
    this.addFrameMatrix(t, e.minX, e.minY, e.maxX, e.maxY);
  }
  addBoundsArea(e, t) {
    const r = e.minX > t.x ? e.minX : t.x, s = e.minY > t.y ? e.minY : t.y, n = e.maxX < t.x + t.width ? e.maxX : t.x + t.width, a = e.maxY < t.y + t.height ? e.maxY : t.y + t.height;
    if (r <= n && s <= a) {
      const o = this.minX, h = this.minY, l = this.maxX, c = this.maxY;
      this.minX = r < o ? r : o, this.minY = s < h ? s : h, this.maxX = n > l ? n : l, this.maxY = a > c ? a : c;
    }
  }
  pad(e = 0, t = e) {
    this.isEmpty() || (this.minX -= e, this.maxX += e, this.minY -= t, this.maxY += t);
  }
  addFramePad(e, t, r, s, n, a) {
    e -= n, t -= a, r += n, s += a, this.minX = this.minX < e ? this.minX : e, this.maxX = this.maxX > r ? this.maxX : r, this.minY = this.minY < t ? this.minY : t, this.maxY = this.maxY > s ? this.maxY : s;
  }
}
class Jt extends oi {
  constructor() {
    super(), this.tempDisplayObjectParent = null, this.transform = new dn(), this.alpha = 1, this.visible = true, this.renderable = true, this.cullable = false, this.cullArea = null, this.parent = null, this.worldAlpha = 1, this._lastSortedIndex = 0, this._zIndex = 0, this.filterArea = null, this.filters = null, this._enabledFilters = null, this._bounds = new Js(), this._localBounds = null, this._boundsID = 0, this._boundsRect = null, this._localBoundsRect = null, this._mask = null, this._maskRefCount = 0, this._destroyed = false, this.isSprite = false, this.isMask = false;
  }
  static mixin(e) {
    const t = Object.keys(e);
    for (let r = 0; r < t.length; ++r) {
      const s = t[r];
      Object.defineProperty(Jt.prototype, s, Object.getOwnPropertyDescriptor(e, s));
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
  getBounds(e, t) {
    return e || (this.parent ? (this._recursivePostUpdateTransform(), this.updateTransform()) : (this.parent = this._tempDisplayObjectParent, this.updateTransform(), this.parent = null)), this._bounds.updateID !== this._boundsID && (this.calculateBounds(), this._bounds.updateID = this._boundsID), t || (this._boundsRect || (this._boundsRect = new me()), t = this._boundsRect), this._bounds.getRectangle(t);
  }
  getLocalBounds(e) {
    e || (this._localBoundsRect || (this._localBoundsRect = new me()), e = this._localBoundsRect), this._localBounds || (this._localBounds = new Js());
    const t = this.transform, r = this.parent;
    this.parent = null, this._tempDisplayObjectParent.worldAlpha = r?.worldAlpha ?? 1, this.transform = this._tempDisplayObjectParent.transform;
    const s = this._bounds, n = this._boundsID;
    this._bounds = this._localBounds;
    const a = this.getBounds(false, e);
    return this.parent = r, this.transform = t, this._bounds = s, this._bounds.updateID += this._boundsID - n, a;
  }
  toGlobal(e, t, r = false) {
    return r || (this._recursivePostUpdateTransform(), this.parent ? this.displayObjectUpdateTransform() : (this.parent = this._tempDisplayObjectParent, this.displayObjectUpdateTransform(), this.parent = null)), this.worldTransform.apply(e, t);
  }
  toLocal(e, t, r, s) {
    return t && (e = t.toGlobal(e, r, s)), s || (this._recursivePostUpdateTransform(), this.parent ? this.displayObjectUpdateTransform() : (this.parent = this._tempDisplayObjectParent, this.displayObjectUpdateTransform(), this.parent = null)), this.worldTransform.applyInverse(e, r);
  }
  setParent(e) {
    if (!e || !e.addChild) throw new Error("setParent: Argument must be a Container");
    return e.addChild(this), e;
  }
  removeFromParent() {
    this.parent?.removeChild(this);
  }
  setTransform(e = 0, t = 0, r = 1, s = 1, n = 0, a = 0, o = 0, h = 0, l = 0) {
    return this.position.x = e, this.position.y = t, this.scale.x = r || 1, this.scale.y = s || 1, this.rotation = n, this.skew.x = a, this.skew.y = o, this.pivot.x = h, this.pivot.y = l, this;
  }
  destroy(e) {
    this.removeFromParent(), this._destroyed = true, this.transform = null, this.parent = null, this._bounds = null, this.mask = null, this.cullArea = null, this.filters = null, this.filterArea = null, this.hitArea = null, this.eventMode = "auto", this.interactiveChildren = false, this.emit("destroyed"), this.removeAllListeners();
  }
  get _tempDisplayObjectParent() {
    return this.tempDisplayObjectParent === null && (this.tempDisplayObjectParent = new Lu()), this.tempDisplayObjectParent;
  }
  enableTempParent() {
    const e = this.parent;
    return this.parent = this._tempDisplayObjectParent, e;
  }
  disableTempParent(e) {
    this.parent = e;
  }
  get x() {
    return this.position.x;
  }
  set x(e) {
    this.transform.position.x = e;
  }
  get y() {
    return this.position.y;
  }
  set y(e) {
    this.transform.position.y = e;
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
  set position(e) {
    this.transform.position.copyFrom(e);
  }
  get scale() {
    return this.transform.scale;
  }
  set scale(e) {
    this.transform.scale.copyFrom(e);
  }
  get pivot() {
    return this.transform.pivot;
  }
  set pivot(e) {
    this.transform.pivot.copyFrom(e);
  }
  get skew() {
    return this.transform.skew;
  }
  set skew(e) {
    this.transform.skew.copyFrom(e);
  }
  get rotation() {
    return this.transform.rotation;
  }
  set rotation(e) {
    this.transform.rotation = e;
  }
  get angle() {
    return this.transform.rotation * xc;
  }
  set angle(e) {
    this.transform.rotation = e * bc;
  }
  get zIndex() {
    return this._zIndex;
  }
  set zIndex(e) {
    this._zIndex !== e && (this._zIndex = e, this.parent && (this.parent.sortDirty = true));
  }
  get worldVisible() {
    let e = this;
    do {
      if (!e.visible) return false;
      e = e.parent;
    } while (e);
    return true;
  }
  get mask() {
    return this._mask;
  }
  set mask(e) {
    if (this._mask !== e) {
      if (this._mask) {
        const t = this._mask.isMaskData ? this._mask.maskObject : this._mask;
        t && (t._maskRefCount--, t._maskRefCount === 0 && (t.renderable = true, t.isMask = false));
      }
      if (this._mask = e, this._mask) {
        const t = this._mask.isMaskData ? this._mask.maskObject : this._mask;
        t && (t._maskRefCount === 0 && (t.renderable = false, t.isMask = true), t._maskRefCount++);
      }
    }
  }
}
class Lu extends Jt {
  constructor() {
    super(...arguments), this.sortDirty = null;
  }
}
Jt.prototype.displayObjectUpdateTransform = Jt.prototype.updateTransform;
const Pu = new we();
function Fu(i, e) {
  return i.zIndex === e.zIndex ? i._lastSortedIndex - e._lastSortedIndex : i.zIndex - e.zIndex;
}
const Ch = class Qs extends Jt {
  constructor() {
    super(), this.children = [], this.sortableChildren = Qs.defaultSortableChildren, this.sortDirty = false;
  }
  onChildrenChange(e) {
  }
  addChild(...e) {
    if (e.length > 1) for (let t = 0; t < e.length; t++) this.addChild(e[t]);
    else {
      const t = e[0];
      t.parent && t.parent.removeChild(t), t.parent = this, this.sortDirty = true, t.transform._parentID = -1, this.children.push(t), this._boundsID++, this.onChildrenChange(this.children.length - 1), this.emit("childAdded", t, this, this.children.length - 1), t.emit("added", this);
    }
    return e[0];
  }
  addChildAt(e, t) {
    if (t < 0 || t > this.children.length) throw new Error(`${e}addChildAt: The index ${t} supplied is out of bounds ${this.children.length}`);
    return e.parent && e.parent.removeChild(e), e.parent = this, this.sortDirty = true, e.transform._parentID = -1, this.children.splice(t, 0, e), this._boundsID++, this.onChildrenChange(t), e.emit("added", this), this.emit("childAdded", e, this, t), e;
  }
  swapChildren(e, t) {
    if (e === t) return;
    const r = this.getChildIndex(e), s = this.getChildIndex(t);
    this.children[r] = t, this.children[s] = e, this.onChildrenChange(r < s ? r : s);
  }
  getChildIndex(e) {
    const t = this.children.indexOf(e);
    if (t === -1) throw new Error("The supplied DisplayObject must be a child of the caller");
    return t;
  }
  setChildIndex(e, t) {
    if (t < 0 || t >= this.children.length) throw new Error(`The index ${t} supplied is out of bounds ${this.children.length}`);
    const r = this.getChildIndex(e);
    Vr(this.children, r, 1), this.children.splice(t, 0, e), this.onChildrenChange(t);
  }
  getChildAt(e) {
    if (e < 0 || e >= this.children.length) throw new Error(`getChildAt: Index (${e}) does not exist.`);
    return this.children[e];
  }
  removeChild(...e) {
    if (e.length > 1) for (let t = 0; t < e.length; t++) this.removeChild(e[t]);
    else {
      const t = e[0], r = this.children.indexOf(t);
      if (r === -1) return null;
      t.parent = null, t.transform._parentID = -1, Vr(this.children, r, 1), this._boundsID++, this.onChildrenChange(r), t.emit("removed", this), this.emit("childRemoved", t, this, r);
    }
    return e[0];
  }
  removeChildAt(e) {
    const t = this.getChildAt(e);
    return t.parent = null, t.transform._parentID = -1, Vr(this.children, e, 1), this._boundsID++, this.onChildrenChange(e), t.emit("removed", this), this.emit("childRemoved", t, this, e), t;
  }
  removeChildren(e = 0, t = this.children.length) {
    const r = e, s = t, n = s - r;
    let a;
    if (n > 0 && n <= s) {
      a = this.children.splice(r, n);
      for (let o = 0; o < a.length; ++o) a[o].parent = null, a[o].transform && (a[o].transform._parentID = -1);
      this._boundsID++, this.onChildrenChange(e);
      for (let o = 0; o < a.length; ++o) a[o].emit("removed", this), this.emit("childRemoved", a[o], this, o);
      return a;
    } else if (n === 0 && this.children.length === 0) return [];
    throw new RangeError("removeChildren: numeric values are outside the acceptable range.");
  }
  sortChildren() {
    let e = false;
    for (let t = 0, r = this.children.length; t < r; ++t) {
      const s = this.children[t];
      s._lastSortedIndex = t, !e && s.zIndex !== 0 && (e = true);
    }
    e && this.children.length > 1 && this.children.sort(Fu), this.sortDirty = false;
  }
  updateTransform() {
    this.sortableChildren && this.sortDirty && this.sortChildren(), this._boundsID++, this.transform.updateTransform(this.parent.transform), this.worldAlpha = this.alpha * this.parent.worldAlpha;
    for (let e = 0, t = this.children.length; e < t; ++e) {
      const r = this.children[e];
      r.visible && r.updateTransform();
    }
  }
  calculateBounds() {
    this._bounds.clear(), this._calculateBounds();
    for (let e = 0; e < this.children.length; e++) {
      const t = this.children[e];
      if (!(!t.visible || !t.renderable)) if (t.calculateBounds(), t._mask) {
        const r = t._mask.isMaskData ? t._mask.maskObject : t._mask;
        r ? (r.calculateBounds(), this._bounds.addBoundsMask(t._bounds, r._bounds)) : this._bounds.addBounds(t._bounds);
      } else t.filterArea ? this._bounds.addBoundsArea(t._bounds, t.filterArea) : this._bounds.addBounds(t._bounds);
    }
    this._bounds.updateID = this._boundsID;
  }
  getLocalBounds(e, t = false) {
    const r = super.getLocalBounds(e);
    if (!t) for (let s = 0, n = this.children.length; s < n; ++s) {
      const a = this.children[s];
      a.visible && a.updateTransform();
    }
    return r;
  }
  _calculateBounds() {
  }
  _renderWithCulling(e) {
    const t = e.renderTexture.sourceFrame;
    if (!(t.width > 0 && t.height > 0)) return;
    let r, s;
    this.cullArea ? (r = this.cullArea, s = this.worldTransform) : this._render !== Qs.prototype._render && (r = this.getBounds(true));
    const n = e.projection.transform;
    if (n && (s ? (s = Pu.copyFrom(s), s.prepend(n)) : s = n), r && t.intersects(r, s)) this._render(e);
    else if (this.cullArea) return;
    for (let a = 0, o = this.children.length; a < o; ++a) {
      const h = this.children[a], l = h.cullable;
      h.cullable = l || !this.cullArea, h.render(e), h.cullable = l;
    }
  }
  render(e) {
    if (!(!this.visible || this.worldAlpha <= 0 || !this.renderable)) if (this._mask || this.filters?.length) this.renderAdvanced(e);
    else if (this.cullable) this._renderWithCulling(e);
    else {
      this._render(e);
      for (let t = 0, r = this.children.length; t < r; ++t) this.children[t].render(e);
    }
  }
  renderAdvanced(e) {
    const t = this.filters, r = this._mask;
    if (t) {
      this._enabledFilters || (this._enabledFilters = []), this._enabledFilters.length = 0;
      for (let n = 0; n < t.length; n++) t[n].enabled && this._enabledFilters.push(t[n]);
    }
    const s = t && this._enabledFilters?.length || r && (!r.isMaskData || r.enabled && (r.autoDetect || r.type !== Ee.NONE));
    if (s && e.batch.flush(), t && this._enabledFilters?.length && e.filter.push(this, this._enabledFilters), r && e.mask.push(this, this._mask), this.cullable) this._renderWithCulling(e);
    else {
      this._render(e);
      for (let n = 0, a = this.children.length; n < a; ++n) this.children[n].render(e);
    }
    s && e.batch.flush(), r && e.mask.pop(this), t && this._enabledFilters?.length && e.filter.pop();
  }
  _render(e) {
  }
  destroy(e) {
    super.destroy(), this.sortDirty = false;
    const t = typeof e == "boolean" ? e : e?.children, r = this.removeChildren(0, this.children.length);
    if (t) for (let s = 0; s < r.length; ++s) r[s].destroy(e);
  }
  get width() {
    return this.scale.x * this.getLocalBounds().width;
  }
  set width(e) {
    const t = this.getLocalBounds().width;
    t !== 0 ? this.scale.x = e / t : this.scale.x = 1, this._width = e;
  }
  get height() {
    return this.scale.y * this.getLocalBounds().height;
  }
  set height(e) {
    const t = this.getLocalBounds().height;
    t !== 0 ? this.scale.y = e / t : this.scale.y = 1, this._height = e;
  }
};
Ch.defaultSortableChildren = false;
let Ct = Ch;
Ct.prototype.containerUpdateTransform = Ct.prototype.updateTransform;
Object.defineProperties(he, { SORTABLE_CHILDREN: { get() {
  return Ct.defaultSortableChildren;
}, set(i) {
  fe("7.1.0", "settings.SORTABLE_CHILDREN is deprecated, use Container.defaultSortableChildren"), Ct.defaultSortableChildren = i;
} } });
const Mh = class en {
  constructor(e) {
    this.stage = new Ct(), e = Object.assign({ forceCanvas: false }, e), this.renderer = _u(e), en._plugins.forEach((t) => {
      t.init.call(this, e);
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
  destroy(e, t) {
    const r = en._plugins.slice(0);
    r.reverse(), r.forEach((s) => {
      s.destroy.call(this);
    }), this.stage.destroy(t), this.stage = null, this.renderer.destroy(e), this.renderer = null;
  }
};
Mh._plugins = [];
let Lh = Mh;
de.handleByList(ne.Application, Lh._plugins);
class Ph {
  static init(e) {
    Object.defineProperty(this, "resizeTo", { set(t) {
      globalThis.removeEventListener("resize", this.queueResize), this._resizeTo = t, t && (globalThis.addEventListener("resize", this.queueResize), this.resize());
    }, get() {
      return this._resizeTo;
    } }), this.queueResize = () => {
      this._resizeTo && (this.cancelResize(), this._resizeId = requestAnimationFrame(() => this.resize()));
    }, this.cancelResize = () => {
      this._resizeId && (cancelAnimationFrame(this._resizeId), this._resizeId = null);
    }, this.resize = () => {
      if (!this._resizeTo) return;
      this.cancelResize();
      let t, r;
      if (this._resizeTo === globalThis.window) t = globalThis.innerWidth, r = globalThis.innerHeight;
      else {
        const { clientWidth: s, clientHeight: n } = this._resizeTo;
        t = s, r = n;
      }
      this.renderer.resize(t, r), this.render();
    }, this._resizeId = null, this._resizeTo = null, this.resizeTo = e.resizeTo || null;
  }
  static destroy() {
    globalThis.removeEventListener("resize", this.queueResize), this.cancelResize(), this.cancelResize = null, this.queueResize = null, this.resizeTo = null, this.resize = null;
  }
}
Ph.extension = ne.Application;
de.add(Ph);
const Nu = { 5: [0.153388, 0.221461, 0.250301], 7: [0.071303, 0.131514, 0.189879, 0.214607], 9: [0.028532, 0.067234, 0.124009, 0.179044, 0.20236], 11: [93e-4, 0.028002, 0.065984, 0.121703, 0.175713, 0.198596], 13: [2406e-6, 9255e-6, 0.027867, 0.065666, 0.121117, 0.174868, 0.197641], 15: [489e-6, 2403e-6, 9246e-6, 0.02784, 0.065602, 0.120999, 0.174697, 0.197448] }, Ou = ["varying vec2 vBlurTexCoords[%size%];", "uniform sampler2D uSampler;", "void main(void)", "{", "    gl_FragColor = vec4(0.0);", "    %blur%", "}"].join(`
`);
function Bu(i) {
  const e = Nu[i], t = e.length;
  let r = Ou, s = "";
  const n = "gl_FragColor += texture2D(uSampler, vBlurTexCoords[%index%]) * %value%;";
  let a;
  for (let o = 0; o < i; o++) {
    let h = n.replace("%index%", o.toString());
    a = o, o >= t && (a = i - o - 1), h = h.replace("%value%", e[a].toString()), s += h, s += `
`;
  }
  return r = r.replace("%blur%", s), r = r.replace("%size%", i.toString()), r;
}
const ku = `
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
function Uu(i, e) {
  const t = Math.ceil(i / 2);
  let r = ku, s = "", n;
  e ? n = "vBlurTexCoords[%index%] =  textureCoord + vec2(%sampleIndex% * strength, 0.0);" : n = "vBlurTexCoords[%index%] =  textureCoord + vec2(0.0, %sampleIndex% * strength);";
  for (let a = 0; a < i; a++) {
    let o = n.replace("%index%", a.toString());
    o = o.replace("%sampleIndex%", `${a - (t - 1)}.0`), s += o, s += `
`;
  }
  return r = r.replace("%blur%", s), r = r.replace("%size%", i.toString()), r;
}
class mo extends Ne {
  constructor(e, t = 8, r = 4, s = Ne.defaultResolution, n = 5) {
    const a = Uu(n, e), o = Bu(n);
    super(a, o), this.horizontal = e, this.resolution = s, this._quality = 0, this.quality = r, this.blur = t;
  }
  apply(e, t, r, s) {
    if (r ? this.horizontal ? this.uniforms.strength = 1 / r.width * (r.width / t.width) : this.uniforms.strength = 1 / r.height * (r.height / t.height) : this.horizontal ? this.uniforms.strength = 1 / e.renderer.width * (e.renderer.width / t.width) : this.uniforms.strength = 1 / e.renderer.height * (e.renderer.height / t.height), this.uniforms.strength *= this.strength, this.uniforms.strength /= this.passes, this.passes === 1) e.applyFilter(this, t, r, s);
    else {
      const n = e.getFilterTexture(), a = e.renderer;
      let o = t, h = n;
      this.state.blend = false, e.applyFilter(this, o, h, He.CLEAR);
      for (let l = 1; l < this.passes - 1; l++) {
        e.bindAndClear(o, He.BLIT), this.uniforms.uSampler = h;
        const c = h;
        h = o, o = c, a.shader.bind(this), a.geometry.draw(5);
      }
      this.state.blend = true, e.applyFilter(this, h, r, s), e.returnFilterTexture(n);
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
}
class st extends Ne {
  constructor(e = 8, t = 4, r = Ne.defaultResolution, s = 5) {
    super(), this._repeatEdgePixels = false, this.blurXFilter = new mo(true, e, t, r, s), this.blurYFilter = new mo(false, e, t, r, s), this.resolution = r, this.quality = t, this.blur = e, this.repeatEdgePixels = false;
  }
  apply(e, t, r, s) {
    const n = Math.abs(this.blurXFilter.strength), a = Math.abs(this.blurYFilter.strength);
    if (n && a) {
      const o = e.getFilterTexture();
      this.blurXFilter.apply(e, t, o, He.CLEAR), this.blurYFilter.apply(e, o, r, s), e.returnFilterTexture(o);
    } else a ? this.blurYFilter.apply(e, t, r, s) : this.blurXFilter.apply(e, t, r, s);
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
}
var Du = `attribute vec2 aVertexPosition;
attribute vec2 aTextureCoord;

uniform mat3 projectionMatrix;

varying vec2 vTextureCoord;

void main(void)
{
    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);
    vTextureCoord = aTextureCoord;
}`, Gu = `uniform float radius;
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
`;
const Fh = class extends Ne {
  constructor(i) {
    super(Du, Gu), this.uniforms.dimensions = new Float32Array(2), Object.assign(this, Fh.defaults, i);
  }
  apply(i, e, t, r) {
    const { width: s, height: n } = e.filterFrame;
    this.uniforms.dimensions[0] = s, this.uniforms.dimensions[1] = n, i.applyFilter(this, e, t, r);
  }
  get radius() {
    return this.uniforms.radius;
  }
  set radius(i) {
    this.uniforms.radius = i;
  }
  get strength() {
    return this.uniforms.strength;
  }
  set strength(i) {
    this.uniforms.strength = i;
  }
  get center() {
    return this.uniforms.center;
  }
  set center(i) {
    this.uniforms.center = i;
  }
};
let fr = Fh;
fr.defaults = { center: [0.5, 0.5], radius: 100, strength: 1 };
var zu = `varying vec2 vTextureCoord;
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
`;
class mr extends Ne {
  constructor() {
    const e = { m: new Float32Array([1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0]), uAlpha: 1 };
    super(Eu, zu, e), this.alpha = 1;
  }
  _loadMatrix(e, t = false) {
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
    const r = [e, 0, 0, 0, 0, 0, e, 0, 0, 0, 0, 0, e, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(r, t);
  }
  tint(e, t) {
    const [r, s, n] = Rt.shared.setValue(e).toArray(), a = [r, 0, 0, 0, 0, 0, s, 0, 0, 0, 0, 0, n, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(a, t);
  }
  greyscale(e, t) {
    const r = [e, e, e, 0, 0, e, e, e, 0, 0, e, e, e, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(r, t);
  }
  blackAndWhite(e) {
    const t = [0.3, 0.6, 0.1, 0, 0, 0.3, 0.6, 0.1, 0, 0, 0.3, 0.6, 0.1, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  hue(e, t) {
    e = (e || 0) / 180 * Math.PI;
    const r = Math.cos(e), s = Math.sin(e), n = Math.sqrt, a = 1 / 3, o = n(a), h = r + (1 - r) * a, l = a * (1 - r) - o * s, c = a * (1 - r) + o * s, u = a * (1 - r) + o * s, m = r + a * (1 - r), y = a * (1 - r) - o * s, d = a * (1 - r) - o * s, p = a * (1 - r) + o * s, f = r + a * (1 - r), _ = [h, l, c, 0, 0, u, m, y, 0, 0, d, p, f, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(_, t);
  }
  contrast(e, t) {
    const r = (e || 0) + 1, s = -0.5 * (r - 1), n = [r, 0, 0, 0, s, 0, r, 0, 0, s, 0, 0, r, 0, s, 0, 0, 0, 1, 0];
    this._loadMatrix(n, t);
  }
  saturate(e = 0, t) {
    const r = e * 2 / 3 + 1, s = (r - 1) * -0.5, n = [r, s, s, 0, 0, s, r, s, 0, 0, s, s, r, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(n, t);
  }
  desaturate() {
    this.saturate(-1);
  }
  negative(e) {
    const t = [-1, 0, 0, 1, 0, 0, -1, 0, 1, 0, 0, 0, -1, 1, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  sepia(e) {
    const t = [0.393, 0.7689999, 0.18899999, 0, 0, 0.349, 0.6859999, 0.16799999, 0, 0, 0.272, 0.5339999, 0.13099999, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  technicolor(e) {
    const t = [1.9125277891456083, -0.8545344976951645, -0.09155508482755585, 0, 11.793603434377337, -0.3087833385928097, 1.7658908555458428, -0.10601743074722245, 0, -70.35205161461398, -0.231103377548616, -0.7501899197440212, 1.847597816108189, 0, 30.950940869491138, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  polaroid(e) {
    const t = [1.438, -0.062, -0.062, 0, 0, -0.122, 1.378, -0.122, 0, 0, -0.016, -0.016, 1.483, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  toBGR(e) {
    const t = [0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  kodachrome(e) {
    const t = [1.1285582396593525, -0.3967382283601348, -0.03992559172921793, 0, 63.72958762196502, -0.16404339962244616, 1.0835251566291304, -0.05498805115633132, 0, 24.732407896706203, -0.16786010706155763, -0.5603416277695248, 1.6014850761964943, 0, 35.62982807460946, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  browni(e) {
    const t = [0.5997023498159715, 0.34553243048391263, -0.2708298674538042, 0, 47.43192855600873, -0.037703249837783157, 0.8609577587992641, 0.15059552388459913, 0, -36.96841498319127, 0.24113635128153335, -0.07441037908422492, 0.44972182064877153, 0, -7.562075277591283, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  vintage(e) {
    const t = [0.6279345635605994, 0.3202183420819367, -0.03965408211312453, 0, 9.651285835294123, 0.02578397704808868, 0.6441188644374771, 0.03259127616149294, 0, 7.462829176470591, 0.0466055556782719, -0.0851232987247891, 0.5241648018700465, 0, 5.159190588235296, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  colorTone(e, t, r, s, n) {
    e = e || 0.2, t = t || 0.15, r = r || 16770432, s = s || 3375104;
    const a = Rt.shared, [o, h, l] = a.setValue(r).toArray(), [c, u, m] = a.setValue(s).toArray(), y = [0.3, 0.59, 0.11, 0, 0, o, h, l, e, 0, c, u, m, t, 0, o - c, h - u, l - m, 0, 0];
    this._loadMatrix(y, n);
  }
  night(e, t) {
    e = e || 0.1;
    const r = [e * -2, -e, 0, 0, 0, -e, 0, e, 0, 0, 0, e, e * 2, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(r, t);
  }
  predator(e, t) {
    const r = [11.224130630493164 * e, -4.794486999511719 * e, -2.8746118545532227 * e, 0 * e, 0.40342438220977783 * e, -3.6330697536468506 * e, 9.193157196044922 * e, -2.951810836791992 * e, 0 * e, -1.316135048866272 * e, -3.2184197902679443 * e, -4.2375030517578125 * e, 7.476448059082031 * e, 0 * e, 0.8044459223747253 * e, 0, 0, 0, 1, 0];
    this._loadMatrix(r, t);
  }
  lsd(e) {
    const t = [2, -0.4, 0.5, 0, 0, -0.5, 2, -0.4, 0, 0, -0.4, -0.5, 3, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(t, e);
  }
  reset() {
    const e = [1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0];
    this._loadMatrix(e, false);
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
}
mr.prototype.grayscale = mr.prototype.greyscale;
const nr = new Ae(), $u = new Uint16Array([0, 1, 2, 0, 2, 3]);
class Xt extends Ct {
  constructor(e) {
    super(), this._anchor = new at(this._onAnchorUpdate, this, e ? e.defaultAnchor.x : 0, e ? e.defaultAnchor.y : 0), this._texture = null, this._width = 0, this._height = 0, this._tintColor = new Rt(16777215), this._tintRGB = null, this.tint = 16777215, this.blendMode = te.NORMAL, this._cachedTint = 16777215, this.uvs = null, this.texture = e || ce.EMPTY, this.vertexData = new Float32Array(8), this.vertexTrimmedData = null, this._transformID = -1, this._textureID = -1, this._transformTrimmedID = -1, this._textureTrimmedID = -1, this.indices = $u, this.pluginName = "batch", this.isSprite = true, this._roundPixels = he.ROUND_PIXELS;
  }
  _onTextureUpdate() {
    this._textureID = -1, this._textureTrimmedID = -1, this._cachedTint = 16777215, this._width && (this.scale.x = Mr(this.scale.x) * this._width / this._texture.orig.width), this._height && (this.scale.y = Mr(this.scale.y) * this._height / this._texture.orig.height);
  }
  _onAnchorUpdate() {
    this._transformID = -1, this._transformTrimmedID = -1;
  }
  calculateVertices() {
    const e = this._texture;
    if (this._transformID === this.transform._worldID && this._textureID === e._updateID) return;
    this._textureID !== e._updateID && (this.uvs = this._texture._uvs.uvsFloat32), this._transformID = this.transform._worldID, this._textureID = e._updateID;
    const t = this.transform.worldTransform, r = t.a, s = t.b, n = t.c, a = t.d, o = t.tx, h = t.ty, l = this.vertexData, c = e.trim, u = e.orig, m = this._anchor;
    let y = 0, d = 0, p = 0, f = 0;
    if (c ? (d = c.x - m._x * u.width, y = d + c.width, f = c.y - m._y * u.height, p = f + c.height) : (d = -m._x * u.width, y = d + u.width, f = -m._y * u.height, p = f + u.height), l[0] = r * d + n * f + o, l[1] = a * f + s * d + h, l[2] = r * y + n * f + o, l[3] = a * f + s * y + h, l[4] = r * y + n * p + o, l[5] = a * p + s * y + h, l[6] = r * d + n * p + o, l[7] = a * p + s * d + h, this._roundPixels) {
      const _ = he.RESOLUTION;
      for (let T = 0; T < l.length; ++T) l[T] = Math.round(l[T] * _) / _;
    }
  }
  calculateTrimmedVertices() {
    if (!this.vertexTrimmedData) this.vertexTrimmedData = new Float32Array(8);
    else if (this._transformTrimmedID === this.transform._worldID && this._textureTrimmedID === this._texture._updateID) return;
    this._transformTrimmedID = this.transform._worldID, this._textureTrimmedID = this._texture._updateID;
    const e = this._texture, t = this.vertexTrimmedData, r = e.orig, s = this._anchor, n = this.transform.worldTransform, a = n.a, o = n.b, h = n.c, l = n.d, c = n.tx, u = n.ty, m = -s._x * r.width, y = m + r.width, d = -s._y * r.height, p = d + r.height;
    if (t[0] = a * m + h * d + c, t[1] = l * d + o * m + u, t[2] = a * y + h * d + c, t[3] = l * d + o * y + u, t[4] = a * y + h * p + c, t[5] = l * p + o * y + u, t[6] = a * m + h * p + c, t[7] = l * p + o * m + u, this._roundPixels) {
      const f = he.RESOLUTION;
      for (let _ = 0; _ < t.length; ++_) t[_] = Math.round(t[_] * f) / f;
    }
  }
  _render(e) {
    this.calculateVertices(), e.batch.setObjectRenderer(e.plugins[this.pluginName]), e.plugins[this.pluginName].render(this);
  }
  _calculateBounds() {
    const e = this._texture.trim, t = this._texture.orig;
    !e || e.width === t.width && e.height === t.height ? (this.calculateVertices(), this._bounds.addQuad(this.vertexData)) : (this.calculateTrimmedVertices(), this._bounds.addQuad(this.vertexTrimmedData));
  }
  getLocalBounds(e) {
    return this.children.length === 0 ? (this._localBounds || (this._localBounds = new Js()), this._localBounds.minX = this._texture.orig.width * -this._anchor._x, this._localBounds.minY = this._texture.orig.height * -this._anchor._y, this._localBounds.maxX = this._texture.orig.width * (1 - this._anchor._x), this._localBounds.maxY = this._texture.orig.height * (1 - this._anchor._y), e || (this._localBoundsRect || (this._localBoundsRect = new me()), e = this._localBoundsRect), this._localBounds.getRectangle(e)) : super.getLocalBounds.call(this, e);
  }
  containsPoint(e) {
    this.worldTransform.applyInverse(e, nr);
    const t = this._texture.orig.width, r = this._texture.orig.height, s = -t * this.anchor.x;
    let n = 0;
    return nr.x >= s && nr.x < s + t && (n = -r * this.anchor.y, nr.y >= n && nr.y < n + r);
  }
  destroy(e) {
    if (super.destroy(e), this._texture.off("update", this._onTextureUpdate, this), this._anchor = null, typeof e == "boolean" ? e : e?.texture) {
      const t = typeof e == "boolean" ? e : e?.baseTexture;
      this._texture.destroy(!!t);
    }
    this._texture = null;
  }
  static from(e, t) {
    const r = e instanceof ce ? e : ce.from(e, t);
    return new Xt(r);
  }
  set roundPixels(e) {
    this._roundPixels !== e && (this._transformID = -1, this._transformTrimmedID = -1), this._roundPixels = e;
  }
  get roundPixels() {
    return this._roundPixels;
  }
  get width() {
    return Math.abs(this.scale.x) * this._texture.orig.width;
  }
  set width(e) {
    const t = Mr(this.scale.x) || 1;
    this.scale.x = t * e / this._texture.orig.width, this._width = e;
  }
  get height() {
    return Math.abs(this.scale.y) * this._texture.orig.height;
  }
  set height(e) {
    const t = Mr(this.scale.y) || 1;
    this.scale.y = t * e / this._texture.orig.height, this._height = e;
  }
  get anchor() {
    return this._anchor;
  }
  set anchor(e) {
    this._anchor.copyFrom(e);
  }
  get tint() {
    return this._tintColor.value;
  }
  set tint(e) {
    this._tintColor.setValue(e), this._tintRGB = this._tintColor.toLittleEndianNumber();
  }
  get tintValue() {
    return this._tintColor.toNumber();
  }
  get texture() {
    return this._texture;
  }
  set texture(e) {
    this._texture !== e && (this._texture && this._texture.off("update", this._onTextureUpdate, this), this._texture = e || ce.EMPTY, this._cachedTint = 16777215, this._textureID = -1, this._textureTrimmedID = -1, e && (e.baseTexture.valid ? this._onTextureUpdate() : e.once("update", this._onTextureUpdate, this)));
  }
}
const Nh = -1, di = 0, yr = 1, si = 2, mn = 3, yn = 4, gn = 5, vn = 6, Oh = 7, Bh = 8, kh = typeof self == "object" ? self : globalThis, yo = (i, e) => {
  switch (i) {
    case "Function":
    case "SharedWorker":
    case "Worker":
    case "eval":
    case "setInterval":
    case "setTimeout":
      throw new TypeError("unable to deserialize " + i);
  }
  return new kh[i](e);
}, Hu = (i, e) => {
  const t = (s, n) => (i.set(n, s), s), r = (s) => {
    if (i.has(s)) return i.get(s);
    const [n, a] = e[s];
    switch (n) {
      case di:
      case Nh:
        return t(a, s);
      case yr: {
        const o = t([], s);
        for (const h of a) o.push(r(h));
        return o;
      }
      case si: {
        const o = t({}, s);
        for (const [h, l] of a) o[r(h)] = r(l);
        return o;
      }
      case mn:
        return t(new Date(a), s);
      case yn: {
        const { source: o, flags: h } = a;
        return t(new RegExp(o, h), s);
      }
      case gn: {
        const o = t(/* @__PURE__ */ new Map(), s);
        for (const [h, l] of a) o.set(r(h), r(l));
        return o;
      }
      case vn: {
        const o = t(/* @__PURE__ */ new Set(), s);
        for (const h of a) o.add(r(h));
        return o;
      }
      case Oh: {
        const { name: o, message: h } = a;
        return t(typeof kh[o] == "function" ? yo(o, h) : new Error(h), s);
      }
      case Bh:
        return t(BigInt(a), s);
      case "BigInt":
        return t(Object(BigInt(a)), s);
      case "ArrayBuffer":
        return t(new Uint8Array(a).buffer, a);
      case "DataView": {
        const { buffer: o } = new Uint8Array(a);
        return t(new DataView(o), a);
      }
    }
    return t(yo(n, a), s);
  };
  return r;
}, go = (i) => Hu(/* @__PURE__ */ new Map(), i)(0), _t = "", { toString: Vu } = {}, { keys: Wu } = Object, ar = (i) => {
  const e = typeof i;
  if (e !== "object" || !i) return [di, e];
  const t = Vu.call(i).slice(8, -1);
  switch (t) {
    case "Array":
      return [yr, _t];
    case "Object":
      return [si, _t];
    case "Date":
      return [mn, _t];
    case "RegExp":
      return [yn, _t];
    case "Map":
      return [gn, _t];
    case "Set":
      return [vn, _t];
    case "DataView":
      return [yr, t];
  }
  return t.includes("Array") ? [yr, t] : i instanceof Error ? [Oh, i.name || "Error"] : [si, t];
}, Gr = ([i, e]) => i === di && (e === "function" || e === "symbol"), Xu = (i, e, t, r) => {
  const s = (a, o) => {
    const h = r.push(a) - 1;
    return t.set(o, h), h;
  }, n = (a) => {
    if (t.has(a)) return t.get(a);
    let [o, h] = ar(a);
    switch (o) {
      case di: {
        let c = a;
        switch (h) {
          case "bigint":
            o = Bh, c = a.toString();
            break;
          case "function":
          case "symbol":
            if (i) throw new TypeError("unable to serialize " + h);
            c = null;
            break;
          case "undefined":
            return s([Nh], a);
        }
        return s([o, c], a);
      }
      case yr: {
        if (h) {
          let m = a;
          return h === "DataView" ? m = new Uint8Array(a.buffer) : h === "ArrayBuffer" && (m = new Uint8Array(a)), s([h, [...m]], a);
        }
        const c = [], u = s([o, c], a);
        for (const m of a) c.push(n(m));
        return u;
      }
      case si: {
        if (h) switch (h) {
          case "BigInt":
            return s([h, a.toString()], a);
          case "Boolean":
          case "Number":
          case "String":
            return s([h, a.valueOf()], a);
        }
        if (e && "toJSON" in a) return n(a.toJSON());
        const c = [], u = s([o, c], a);
        for (const m of Wu(a)) (i || !Gr(ar(a[m]))) && c.push([n(m), n(a[m])]);
        return u;
      }
      case mn:
        return s([o, isNaN(a.getTime()) ? _t : a.toISOString()], a);
      case yn: {
        const { source: c, flags: u } = a;
        return s([o, { source: c, flags: u }], a);
      }
      case gn: {
        const c = [], u = s([o, c], a);
        for (const [m, y] of a) (i || !(Gr(ar(m)) || Gr(ar(y)))) && c.push([n(m), n(y)]);
        return u;
      }
      case vn: {
        const c = [], u = s([o, c], a);
        for (const m of a) (i || !Gr(ar(m))) && c.push(n(m));
        return u;
      }
    }
    const { message: l } = a;
    return s([o, { name: h, message: l }], a);
  };
  return n;
}, vo = (i, { json: e, lossy: t } = {}) => {
  const r = [];
  return Xu(!(e || t), !!e, /* @__PURE__ */ new Map(), r)(i), r;
}, xo = typeof structuredClone == "function" ? (i, e) => e && ("json" in e || "lossy" in e) ? go(vo(i, e)) : structuredClone(i) : (i, e) => go(vo(i, e));
function bo(i) {
  return i;
}
const { cbrt: Ps, sqrt: Fs, PI: Gt } = Math, qu = (i, e, t, r, s) => {
  const n = e + t * i, a = n ** 2 + r;
  if (a > 0) {
    const c = Fs(a);
    return Ps(n + c) + Ps(n - c) - s;
  }
  const o = Ps(Fs(n * n - a)), h = n ? Math.atan(Fs(-a) / n) : -Gt / 2;
  let l;
  return t < 0 ? l = (n > 0 ? 2 * Gt : Gt) - h : s < 0 ? l = (n > 0 ? 2 * Gt : -3 * Gt) + h : l = (n > 0 ? 0 : Gt) + h, 2 * o * Math.cos(l / 3) - s;
}, ju = (i, e, t, r) => ((e * i + 3 * t) * i + r) * i;
function Uh(i, e, t, r) {
  if (!(0 <= i && i <= 1 && 0 <= t && t <= 1)) throw new Error("bezier x values must be in [0, 1] range");
  if (i === e && t === r) return bo;
  const s = 6 * (3 * i - 3 * t + 1), n = 6 * (t - 2 * i), a = 3 * i, o = s * s, h = n * n, l = n / s, c = 3 * n * a / o - h * n / (o * s), u = 2 * a / s - h / o, m = u * u * u, y = 3 / s, d = 3 * e - 3 * r + 1, p = r - 2 * e, f = 3 * e, _ = s ? qu : bo;
  return function(I) {
    return I === 0 || I === 1 ? I : ju(_(I, c, y, m, l), d, p, f);
  };
}
var Yu = class {
};
function _o(i) {
  return Math.max(1, i);
}
var Ku = class extends Yu {
  canvas;
  observer;
  flowSpeed = 1;
  currerntRenderScale = 0.75;
  constructor(i) {
    super(), this.canvas = i, this.observer = new ResizeObserver(() => {
      const e = _o(i.clientWidth * window.devicePixelRatio * this.currerntRenderScale), t = _o(i.clientHeight * window.devicePixelRatio * this.currerntRenderScale);
      this.onResize(e, t);
    }), this.observer.observe(i);
  }
  setRenderScale(i) {
    this.currerntRenderScale = i, this.onResize(this.canvas.clientWidth * window.devicePixelRatio * this.currerntRenderScale, this.canvas.clientHeight * window.devicePixelRatio * this.currerntRenderScale);
  }
  onResize(i, e) {
    this.canvas.width = i, this.canvas.height = e;
  }
  setFlowSpeed(i) {
    this.flowSpeed = i;
  }
  dispose() {
    this.observer.disconnect(), this.canvas.remove();
  }
  getElement() {
    return this.canvas;
  }
};
function Zu(i) {
  return new Promise((e, t) => {
    const r = document.createElement("img");
    r.onload = () => e(r), r.onerror = t, r.src = i, r.crossOrigin = "anonymous", r.loading = "eager";
  });
}
function Ju(i) {
  return new Promise((e, t) => {
    const r = document.createElement("video");
    let s = false, n = false, a = false;
    r.addEventListener("playing", () => {
      s = true, o();
    }, true), r.addEventListener("timeupdate", () => {
      n = true, o();
    }, true), r.addEventListener("error", (h) => {
      a = true, t(h);
    }, true);
    function o() {
      s && n && !a && e(r);
    }
    r.src = i, r.playsInline = true, r.crossOrigin = "anonymous", r.autoplay = true, r.loop = true, r.muted = true, r.play();
  });
}
function Qu(i, e = false) {
  return e ? Ju(i) : Zu(i);
}
function ed(i) {
  return new Promise((e, t) => {
    (i instanceof HTMLImageElement ? i.complete : i.readyState >= 3) ? e(i) : (i.onload = () => e(i), i.onerror = t);
  });
}
function lt(i, e, t) {
  return Math.min(Math.max(i, e), t);
}
function ht(i) {
  return lt(i, 0, 1);
}
function Je(i) {
  return Math.max(0, i);
}
const td = oe.fromValues(2, -2, 1, 1, -3, 3, -2, -1, 0, 0, 1, 0, 1, 0, 0, 0);
oe.clone(td).transpose();
var rd = class extends Ct {
  time = 0;
}, id = class extends Ku {
  canvas;
  app;
  curContainer;
  staticMode = false;
  lastContainer = /* @__PURE__ */ new Set();
  onTick = (i) => {
    for (const e of this.lastContainer) e.alpha = Je(e.alpha - i / 60), e.alpha <= 0 && (this.app.stage.removeChild(e), this.lastContainer.delete(e), e.destroy(true));
    if (this.curContainer) {
      this.curContainer.alpha = Math.min(1, this.curContainer.alpha + i / 60);
      const [e, t, r, s] = this.curContainer.children, n = Math.max(this.app.screen.width, this.app.screen.height);
      e.position.set(this.app.screen.width / 2, this.app.screen.height / 2), t.position.set(this.app.screen.width / 2.5, this.app.screen.height / 2.5), r.position.set(this.app.screen.width / 2, this.app.screen.height / 2), s.position.set(this.app.screen.width / 2, this.app.screen.height / 2), e.width = n * Math.sqrt(2), e.height = e.width, t.width = n * 0.8, t.height = t.width, r.width = n * 0.5, r.height = r.width, s.width = n * 0.25, s.height = s.width, this.curContainer.time += i * this.flowSpeed, e.rotation += i / 1e3 * this.flowSpeed, t.rotation -= i / 500 * this.flowSpeed, r.rotation += i / 1e3 * this.flowSpeed, s.rotation -= i / 750 * this.flowSpeed, r.x = this.app.screen.width / 2 + this.app.screen.width / 4 * Math.cos(this.curContainer.time / 1e3 * 0.75), r.y = this.app.screen.height / 2 + this.app.screen.width / 4 * Math.cos(this.curContainer.time / 1e3 * 0.75), s.x = this.app.screen.width / 2 + this.app.screen.width / 4 * 0.1 + Math.cos(this.curContainer.time * 6e-3 * 0.75), s.y = this.app.screen.height / 2 + this.app.screen.width / 4 * 0.1 + Math.cos(this.curContainer.time * 6e-3 * 0.75), this.curContainer.alpha >= 1 && this.lastContainer.size === 0 && this.staticMode && this.app.ticker.stop();
    }
  };
  constructor(i) {
    super(i), this.canvas = i, this.app = new Lh({ view: i, resizeTo: this.canvas, powerPreference: "low-power", backgroundAlpha: 1 }), this.rebuildFilters(), this.app.ticker.maxFPS = 30, this.app.ticker.add(this.onTick), this.app.ticker.start();
  }
  onResize(i, e) {
    super.onResize(i, e), this.app.resize(), this.rebuildFilters();
  }
  setRenderScale(i) {
    super.setRenderScale(i), this.rebuildFilters();
  }
  rebuildFilters() {
    const i = Math.min(this.canvas.width, this.canvas.height), e = Math.max(this.canvas.width, this.canvas.height), t = new mr();
    t.saturate(1.2, false);
    const r = new mr();
    r.brightness(0.6, false);
    const s = new mr();
    s.contrast(0.3, true);
    for (const n of this.app.stage.filters ?? []) n.destroy();
    this.app.stage.filters = [], this.app.stage.filters.push(new st(5, 1)), this.app.stage.filters.push(new st(10, 1)), this.app.stage.filters.push(new st(20, 2)), this.app.stage.filters.push(new st(40, 2)), this.app.stage.filters.push(new st(80, 2)), i > 768 && this.app.stage.filters.push(new st(160, 4)), i > 768 * 2 && this.app.stage.filters.push(new st(320, 4)), this.app.stage.filters.push(t, r, s), this.app.stage.filters.push(new st(5, 1)), Math.random() > 0.5 ? (this.app.stage.filters.push(new fr({ radius: (e + i) / 2, strength: 1, center: [0.25, 1] })), this.app.stage.filters.push(new fr({ radius: (e + i) / 2, strength: 1, center: [0.75, 0] }))) : (this.app.stage.filters.push(new fr({ radius: (e + i) / 2, strength: 1, center: [0.75, 1] })), this.app.stage.filters.push(new fr({ radius: (e + i) / 2, strength: 1, center: [0.25, 0] })));
  }
  setStaticMode(i = false) {
    this.staticMode = i, this.app.ticker.start();
  }
  setFPS(i) {
    this.app.ticker.maxFPS = i;
  }
  pause() {
    this.app.ticker.stop(), this.app.render();
  }
  resume() {
    this.app.ticker.start();
  }
  setLowFreqVolume(i) {
  }
  setHasLyric(i) {
  }
  async setAlbum(i, e) {
    if (!i || typeof i == "string" && i.trim().length === 0) return;
    let t = null, r = 5, s = null;
    for (; !s?.baseTexture?.resource?.valid && r > 0; ) try {
      typeof i == "string" ? t = await Qu(i, e) : t = await ed(i), s = ce.from(t, { resourceOptions: { autoLoad: false } }), await s.baseTexture.resource.load();
    } catch (c) {
      console.warn(`failed on loading album image, retrying (${r})`, i, c), s = null, r--;
    }
    if (!s) return;
    const n = new rd(), a = new Xt(s), o = new Xt(s), h = new Xt(s), l = new Xt(s);
    a.anchor.set(0.5, 0.5), o.anchor.set(0.5, 0.5), h.anchor.set(0.5, 0.5), l.anchor.set(0.5, 0.5), a.rotation = Math.random() * Math.PI * 2, o.rotation = Math.random() * Math.PI * 2, h.rotation = Math.random() * Math.PI * 2, l.rotation = Math.random() * Math.PI * 2, n.addChild(a, o, h, l), this.curContainer && this.lastContainer.add(this.curContainer), this.curContainer = n, this.app.stage.addChild(n), this.curContainer.alpha = 0, this.app.ticker.start();
  }
  dispose() {
    super.dispose(), this.app.ticker.remove(this.onTick), this.app.destroy(true);
  }
  getElement() {
    return this.canvas;
  }
}, sd = class Dh {
  element;
  renderer;
  constructor(e, t) {
    this.renderer = e, this.element = t, t.style.pointerEvents = "none", t.style.zIndex = "-1", t.style.contain = "strict";
  }
  static new(e) {
    const t = document.createElement("canvas");
    return new Dh(new e(t), t);
  }
  setRenderScale(e) {
    this.renderer.setRenderScale(e);
  }
  setFlowSpeed(e) {
    this.renderer.setFlowSpeed(e);
  }
  setStaticMode(e) {
    this.renderer.setStaticMode(e);
  }
  setFPS(e) {
    this.renderer.setFPS(e);
  }
  pause() {
    this.renderer.pause();
  }
  resume() {
    this.renderer.resume();
  }
  setLowFreqVolume(e) {
    this.renderer.setLowFreqVolume(e);
  }
  setHasLyric(e) {
    this.renderer.setHasLyric(e);
  }
  setAlbum(e, t) {
    return this.renderer.setAlbum(e, t);
  }
  getElement() {
    return this.element;
  }
  dispose() {
    this.renderer.dispose(), this.element.remove();
  }
}, ie = { active: "FmKaba_active", bgWrapper: "FmKaba_bgWrapper", bgWrapperActive: "FmKaba_bgWrapperActive", bgWrapperHidden: "FmKaba_bgWrapperHidden", bgWrapperTop: "FmKaba_bgWrapperTop", bottomLine: "FmKaba_bottomLine", disableSpring: "FmKaba_disableSpring", emphasize: "FmKaba_emphasize", emphasizeWrapper: "FmKaba_emphasizeWrapper", enabled: "FmKaba_enabled", hasDuetLine: "FmKaba_hasDuetLine", interludeDots: "FmKaba_interludeDots", lyricBgLine: "FmKaba_lyricBgLine", lyricDuetLine: "FmKaba_lyricDuetLine", lyricLine: "FmKaba_lyricLine", lyricLineWrapper: "FmKaba_lyricLineWrapper", lyricMainLine: "FmKaba_lyricMainLine", lyricSubLine: "FmKaba_lyricSubLine", playing: "FmKaba_playing", romanWord: "FmKaba_romanWord", rubyWord: "FmKaba_rubyWord", tmpDisableTransition: "FmKaba_tmpDisableTransition", wordBody: "FmKaba_wordBody", wordWithRuby: "FmKaba_wordWithRuby" };
const nd = { normalizeSpaces: true, resetLineTimestamps: true, convertExcessiveBackgroundLines: true, syncMainAndBackgroundLines: true, cleanUnintentionalOverlaps: true, tryAdvanceStartTime: true };
function ad(i) {
  for (const e of i) for (const t of e.words) t.word = t.word.replace(/\s+/g, " ");
}
function od(i) {
  for (const e of i) if (e.words.length === 1 && e.words[0].startTime === 0 && e.words[0].endTime === 0 && (e.startTime !== 0 || e.endTime !== 0)) e.words[0].startTime = e.startTime, e.words[0].endTime = e.endTime;
  else if (e.words.length > 0) {
    const t = e.words[0], r = e.words[e.words.length - 1];
    e.startTime = t.startTime, e.endTime = r.endTime;
  }
}
function hd(i) {
  let e = 0;
  for (const t of i) t.isBG ? (e++, e > 1 && (t.isBG = false)) : e = 0;
}
function ld(i) {
  for (let e = i.length - 1; e >= 0; e--) {
    const t = i[e];
    if (t.isBG) continue;
    const r = i[e + 1];
    if (r?.isBG) {
      const s = [...t.words, ...r.words].filter((n) => n.word.trim().length > 0);
      if (s.length > 0) {
        const n = Math.min(...s.map((l) => l.startTime)), a = Math.max(...s.map((l) => l.endTime)), o = Math.min(n, t.startTime, r.startTime), h = Math.max(a, t.endTime, r.endTime);
        t.startTime = o, t.endTime = h, r.startTime = o, r.endTime = h;
      }
    }
  }
}
function cd(i) {
  for (let e = 0; e < i.length - 1; e++) {
    const t = i[e];
    if (t.isBG) continue;
    let r = e + 1;
    for (; r < i.length && i[r].isBG; ) r++;
    if (r < i.length) {
      const s = i[r], n = t.endTime - s.startTime;
      if (n > 0) {
        const a = (s.endTime - s.startTime) * 0.1;
        if (!(n > 100 && n > a)) {
          t.endTime = s.startTime;
          const o = i[e + 1];
          o?.isBG && (o.endTime = s.startTime);
        }
      }
    }
  }
}
function ud(i) {
  let s = 0, n = 0, a = 0, o = 0, h = false;
  for (let l = 0; l < i.length; l++) {
    const c = i[l];
    if (c.isBG) continue;
    const u = c.startTime, m = c.endTime;
    let y = 0, d = 0;
    if (h) if (u >= n) y = 600, d = o;
    else {
      y = 400;
      const T = n - s;
      d = s + T * 0.3;
    }
    else y = 600, d = 0;
    const p = c.startTime - y, f = Math.max(d, p);
    f < c.startTime && (c.startTime = f);
    const _ = i[l + 1];
    _?.isBG && (_.startTime = c.startTime), h && u < o && m > a ? (a = Math.min(a, u), o = Math.max(o, m)) : (a = u, o = m), s = u, n = m, h = true;
  }
}
function dd(i, e) {
  const t = { ...nd, ...e };
  t.normalizeSpaces && ad(i), t.resetLineTimestamps && od(i), t.convertExcessiveBackgroundLines && hd(i), t.syncMainAndBackgroundLines && ld(i), t.cleanUnintentionalOverlaps && cd(i), t.tryAdvanceStartTime && ud(i);
}
function fd(i) {
  const e = 2.5949095;
  return i < 0.5 ? (2 * i) ** 2 * ((e + 1) * 2 * i - e) / 2 : ((2 * i - 2) ** 2 * ((e + 1) * (i * 2 - 2) + e) + 2) / 2;
}
function pd(i) {
  return i === 1 ? 1 : 1 - 2 ** (-10 * i);
}
var md = class {
  element = document.createElement("div");
  dot0 = document.createElement("span");
  dot1 = document.createElement("span");
  dot2 = document.createElement("span");
  left = 0;
  top = 0;
  playing = true;
  lastStyle = "";
  currentInterlude;
  currentTime = 0;
  targetBreatheDuration = 1500;
  constructor() {
    this.element.className = ie.interludeDots, this.element.appendChild(this.dot0), this.element.appendChild(this.dot1), this.element.appendChild(this.dot2);
  }
  getElement() {
    return this.element;
  }
  setTransform(i = this.left, e = this.top) {
    this.left = i, this.top = e, this.update();
  }
  setInterlude(i) {
    this.currentInterlude = i, this.currentTime = i?.[0] ?? 0, i ? this.element.classList.add(ie.enabled) : this.element.classList.remove(ie.enabled);
  }
  pause() {
    this.playing = false, this.element.classList.remove(ie.playing);
  }
  resume() {
    this.playing = true, this.element.classList.add(ie.playing);
  }
  update(i = 0) {
    if (!this.playing) return;
    this.currentTime += i;
    let e = "";
    if (e += `transform:translate(${this.left.toFixed(2)}px, ${this.top.toFixed(2)}px)`, this.currentInterlude) {
      const t = this.currentInterlude[1] - this.currentInterlude[0], r = this.currentTime - this.currentInterlude[0];
      if (r <= t) {
        const s = t / Math.ceil(t / this.targetBreatheDuration);
        let n = 1, a = 1;
        n *= Math.sin(1.5 * Math.PI - r / s * 2) / 20 + 1, r < 2e3 && (n *= pd(r / 2e3)), r < 500 ? a = 0 : r < 1e3 && (a *= (r - 500) / 500), t - r < 750 && (n *= 1 - fd((750 - (t - r)) / 750 / 2)), t - r < 375 && (a *= ht((t - r) / 375));
        const o = Je(t - 750);
        n = Je(n) * 0.7, e += ` scale(${n})`;
        const h = lt(0.25, r * 3 / o * 0.75, 1), l = lt(0.25, (r - o / 3) * 3 / o * 0.75, 1), c = lt(0.25, (r - o / 3 * 2) * 3 / o * 0.75, 1);
        this.dot0.style.opacity = `${ht(a * h)}`, this.dot1.style.opacity = `${ht(a * l)}`, this.dot2.style.opacity = `${ht(a * c)}`;
      } else e += " scale(0)", this.dot0.style.opacity = "0", this.dot1.style.opacity = "0", this.dot2.style.opacity = "0";
      e += ";", this.lastStyle !== e && (this.element.setAttribute("style", e), this.lastStyle = e);
    }
  }
  dispose() {
    this.element.remove();
  }
};
const tn = [], To = [];
let rn = false;
function yd() {
  let i = To.shift();
  for (; i; ) {
    try {
      i.resolve(i.task());
    } catch (e) {
      i.reject(e);
    }
    i = To.shift();
  }
  for (i = tn.shift(); i; ) {
    try {
      i.resolve(i.task());
    } catch (e) {
      i.reject(e);
    }
    i = tn.shift();
  }
  rn = false;
}
function gd() {
  rn || (rn = true, requestAnimationFrame(yd));
}
function vd(i) {
  const e = { task: i, resolve: () => {
  }, reject: () => {
  } }, t = new Promise((r, s) => {
    e.resolve = r, e.reject = s;
  });
  return tn.push(e), gd(), t;
}
function xd(i) {
  return (t) => (i(t + 1e-3) - i(t - 1e-3)) / (2 * 1e-3);
}
function Eo(i) {
  return xd(i);
}
var xr = class {
  currentPosition = 0;
  targetPosition = 0;
  currentTime = 0;
  params = {};
  currentSolver;
  getV;
  getV2;
  queueParams;
  queuePosition;
  constructor(i = 0) {
    this.targetPosition = i, this.currentPosition = this.targetPosition, this.currentSolver = () => this.targetPosition, this.getV = () => 0, this.getV2 = () => 0;
  }
  resetSolver() {
    const i = this.getV(this.currentTime);
    this.currentTime = 0, this.currentSolver = bd(this.currentPosition, i, this.targetPosition, 0, this.params), this.getV = Eo(this.currentSolver), this.getV2 = Eo(this.getV);
  }
  arrived() {
    return Math.abs(this.targetPosition - this.currentPosition) < 0.01 && this.getV(this.currentTime) < 0.01 && this.getV2(this.currentTime) < 0.01 && this.queueParams === void 0 && this.queuePosition === void 0;
  }
  setPosition(i) {
    this.targetPosition = i, this.currentPosition = i, this.currentSolver = () => this.targetPosition, this.getV = () => 0, this.getV2 = () => 0;
  }
  update(i = 0) {
    this.currentTime += i, this.currentPosition = this.currentSolver(this.currentTime), this.queueParams && (this.queueParams.time -= i, this.queueParams.time <= 0 && this.updateParams({ ...this.queueParams })), this.queuePosition && (this.queuePosition.time -= i, this.queuePosition.time <= 0 && this.setTargetPosition(this.queuePosition.position)), this.arrived() && this.setPosition(this.targetPosition);
  }
  updateParams(i, e = 0) {
    e > 0 ? this.queueParams = { ...this.queuePosition ?? {}, ...i, time: e } : (this.queuePosition = void 0, this.params = { ...this.params, ...i }, this.resetSolver());
  }
  setTargetPosition(i, e = 0) {
    e > 0 ? this.queuePosition = { ...this.queuePosition ?? {}, position: i, time: e } : (this.queuePosition = void 0, this.targetPosition = i, this.resetSolver());
  }
  getCurrentPosition() {
    return this.currentPosition;
  }
};
function bd(i, e, t, r = 0, s) {
  const n = s?.soft ?? false, a = s?.stiffness ?? 100, o = s?.damping ?? 10, h = s?.mass ?? 1, l = t - i;
  if (n || 1 <= o / (2 * Math.sqrt(a * h))) {
    const d = -Math.sqrt(a / h), p = -d * l - e;
    return (f) => (f -= r, f < 0 ? i : t - (l + f * p) * Math.E ** (f * d));
  }
  const c = Math.sqrt(4 * h * a - o ** 2), u = (o * l - 2 * h * e) / c, m = 0.5 * c / h, y = -(0.5 * o) / h;
  return (d) => (d -= r, d < 0 ? i : t - (Math.cos(d * m) * l + Math.sin(d * m) * u) * Math.E ** (d * y));
}
var _d = class {
  lyricPlayer;
  element = document.createElement("div");
  left = 0;
  top = 0;
  delay = 0;
  lineSize = [0, 0];
  lineTransforms = { posX: new xr(0), posY: new xr(0) };
  isFocused = false;
  blur = 0;
  constructor(i) {
    this.lyricPlayer = i, this.element.setAttribute("class", `${ie.lyricLine} ${ie.bottomLine}`), this.element.dataset.bottomLine = "true", this.rebuildStyle();
  }
  async measureSize() {
    return await vd(() => [this.element.clientWidth, this.element.clientHeight]);
  }
  lastStyle = "";
  show() {
    this.rebuildStyle();
  }
  hide() {
    this.rebuildStyle();
  }
  setFocused(i) {
    this.isFocused !== i && (this.isFocused = i, i ? this.element.dataset.focused = "true" : delete this.element.dataset.focused);
  }
  rebuildStyle() {
    let i = `transform:translate(${this.lineTransforms.posX.getCurrentPosition().toFixed(2)}px,${this.lineTransforms.posY.getCurrentPosition().toFixed(2)}px);`;
    !this.lyricPlayer.getEnableSpring() && this.isInSight && (i += `transition-delay:${this.delay}ms;`), i += `filter:blur(${Math.min(5, this.blur)}px);`, i !== this.lastStyle && (this.lastStyle = i, this.element.setAttribute("style", i));
  }
  getElement() {
    return this.element;
  }
  setTransform(i = this.left, e = this.top, t = 0, r = false, s = 0) {
    this.left = i, this.top = e, this.delay = s * 1e3 | 0, r || !this.lyricPlayer.getEnableSpring() ? (this.blur = Math.min(32, t), r && this.element.classList.add(ie.tmpDisableTransition), this.lineTransforms.posX.setPosition(i), this.lineTransforms.posY.setPosition(e), this.lyricPlayer.getEnableSpring() ? this.rebuildStyle() : this.show(), r && requestAnimationFrame(() => {
      this.element.classList.remove(ie.tmpDisableTransition);
    })) : (this.blur = Math.min(5, t), this.lineTransforms.posX.setTargetPosition(i, s), this.lineTransforms.posY.setTargetPosition(e, s));
  }
  update(i = 0) {
    this.lyricPlayer.getEnableSpring() && (this.lineTransforms.posX.update(i), this.lineTransforms.posY.update(i), this.isInSight ? this.show() : this.hide());
  }
  get isInSight() {
    const i = this.lineTransforms.posX.getCurrentPosition(), e = this.lineTransforms.posY.getCurrentPosition(), t = i + this.lineSize[0], r = e + this.lineSize[1], s = this.lyricPlayer.size[0], n = this.lyricPlayer.size[1];
    return !(i > s || e > n || t < 0 || r < 0);
  }
  dispose() {
    this.element.remove();
  }
};
const or = { Disabled: "", FullMask: "full-mask", PartialMask: "partial-mask" }, Et = { SOLID: 0, GRADIENT: 1 }, Ns = { Center: "center", Bottom: "bottom" };
function Td(i) {
  const e = i.currentTime + 20, t = i.scrollToIndex, r = i.currentGroups, s = (n) => {
    if (n < -1 || n >= r.length - 1) return;
    const a = n === -1 ? null : r[n], o = r[n + 1], h = a ? a.endTime : 0, l = Math.max(h, o.startTime - 250);
    if (!(l - h < 4e3) && l > e && h < e) return { startTime: Math.max(h, e), endTime: l, anchorLineIndex: n, isNextDuet: o.mainLine.getLine().isDuet };
  };
  return s(t - 1) || s(t) || s(t + 1);
}
function Ed(i) {
  const { enabled: e, currentGroups: t, scrollToIndex: r, isSeeking: s, isInterludeActive: n } = i;
  if (!e || t.length === 0) return { shouldUpdate: false };
  if (s || n) return { shouldUpdate: true, params: { stiffness: 90, damping: 15 } };
  const a = t[r], o = t[r - 1];
  if (!a || !o) return { shouldUpdate: false };
  const h = a.startTime - o.startTime, l = 100, c = 800, u = lt(h, l, c), m = 220, y = 170;
  let d = 1 - (u - l) / (c - l);
  d = d ** 0.2;
  const p = y + d * (m - y);
  return { shouldUpdate: true, params: { stiffness: p, damping: Math.sqrt(p) * 2.2 } };
}
function wd(i) {
  const { groupIndex: e, scrollToIndex: t, latestIndex: r, hasBuffered: s, hidePassedLines: n, isPlaying: a, isNonDynamic: o, enableBlur: h, isUserScrolling: l, isCompact: c, interlude: u } = i, m = s || e >= t && e < r, y = Gh({ enableBlur: h, isUserScrolling: l, isActive: m, itemIndex: e, scrollToIndex: t, latestIndex: r, isCompact: c });
  let d;
  return n && e < (u ? u.anchorLineIndex + 1 : t) && a ? d = 1e-4 : s ? d = 0.85 : d = o ? 0.2 : 1, { isActive: m, targetOpacity: d, blurLevel: y };
}
function Gh(i) {
  const { enableBlur: e, isUserScrolling: t, isActive: r, itemIndex: s, scrollToIndex: n, latestIndex: a, isCompact: o } = i;
  if (!e || t || r) return 0;
  let h = 1;
  return s < n ? h += Math.abs(n - s) + 1 : h += Math.abs(s - Math.max(n, a)), o ? h * 0.8 : h;
}
function zr(i) {
  i.scrollOffset = lt(i.scrollOffset, i.scrollBoundary.minOffset, i.scrollBoundary.maxOffset);
}
function Sd(i) {
  i.isScrolled = false, i.scrollOffset = 0, i.isUserScrolling = false;
}
function Ad(i, e, t) {
  let r = 0, s = 0, n = 0, a = 0, o = 0, h = 0, l = 0, c = 0;
  i.addEventListener("touchstart", (u) => {
    t.onBeginScroll() && (e.isUserScrolling = true, u.preventDefault(), r = e.scrollOffset, s = u.touches[0].screenY, o = s, n = u.touches[0].screenX, a = u.touches[0].screenY, h = Date.now(), l = 0, t.onLayout(true, true));
  }), i.addEventListener("touchmove", (u) => {
    if (t.onBeginScroll()) {
      u.preventDefault();
      const m = u.touches[0].screenY, y = m - s;
      e.scrollOffset = r - y, zr(e);
      const d = Date.now(), p = d - h;
      p > 0 && (l = (m - o) / p), o = m, h = d, t.onLayout(true, true);
    }
  }), i.addEventListener("touchend", (u) => {
    if (t.onBeginScroll()) {
      u.preventDefault();
      const m = u.changedTouches[0], y = Math.abs(m.screenX - n), d = Math.abs(m.screenY - a);
      if (y < 10 && d < 10) {
        const T = document.elementFromPoint(m.clientX, m.clientY);
        T instanceof HTMLElement && t.containsTarget(T) && t.clickTarget(T), e.isUserScrolling = false, t.onEndScroll();
        return;
      }
      s = 0;
      const p = ++c;
      Math.abs(l) < 0.1 && (l = 0);
      let f = performance.now();
      const _ = (T) => {
        if (p !== c) return;
        const I = T - f;
        if (f = T, I <= 0 || I > 100) {
          requestAnimationFrame(_);
          return;
        }
        if (Math.abs(l) > 0.05) {
          e.scrollOffset -= l * I, zr(e);
          const k = 0.95 ** (I / 16);
          l *= k, t.onLayout(true, true), requestAnimationFrame(_);
        } else e.isUserScrolling = false, t.onEndScroll();
      };
      requestAnimationFrame(_);
    } else e.isUserScrolling = false;
  }), i.addEventListener("wheel", (u) => {
    t.onBeginScroll() && (u.preventDefault(), u.deltaMode === u.DOM_DELTA_PIXEL ? (e.scrollOffset += u.deltaY, zr(e), t.onLayout(true, false)) : (e.scrollOffset += u.deltaY * 50, zr(e), t.onLayout(false, false)));
  }, { passive: false });
}
const Id = (i, e) => i.size === e.size && [...i].every((t) => e.has(t));
function Rd(i) {
  const { time: e, currentGroups: t, timelineState: { hotGroups: r, bufferedGroups: s } } = i, n = new Set(r), a = /* @__PURE__ */ new Set(), o = /* @__PURE__ */ new Set(), h = /* @__PURE__ */ new Set();
  for (const l of r) {
    const c = t[l];
    (!c || e < c.startTime || c.endTime <= e) && (n.delete(l), o.add(l));
  }
  for (let l = 0; l < t.length; l++) {
    const c = t[l];
    c && c.startTime <= e && c.endTime > e && !n.has(l) && (n.add(l), a.add(l));
  }
  for (const l of s) n.has(l) || h.add(l);
  return { nextHotGroups: n, addedIds: a, removedHotIds: o, removedBufferedIds: h };
}
function Cd(i, e, t) {
  if (t.size > 0) return Math.min(...t);
  const r = e.findIndex((s) => s.startTime >= i);
  return r === -1 ? e.length : r;
}
function Md(i) {
  const { timelineState: e, time: t, currentGroups: r, hasBottomContent: s, stateResult: n } = i, { addedIds: a, removedHotIds: o, removedBufferedIds: h } = n, { isSeeking: l } = e;
  e.currentTime = t, e.hotGroups = n.nextHotGroups;
  let c = false, u = false;
  const m = [], y = /* @__PURE__ */ new Set();
  if (l) {
    e.bufferedGroups = /* @__PURE__ */ new Set([...e.hotGroups]), e.scrollToIndex = Cd(t, r, e.bufferedGroups);
    for (const d of o) y.add(d);
    for (const d of e.hotGroups) m.push(d);
    for (const d of h) y.add(d);
    u = true, c = true;
  } else if (a.size > 0) {
    for (const d of a) e.bufferedGroups.add(d), m.push(d);
    for (const d of h) e.bufferedGroups.delete(d), y.add(d);
    e.bufferedGroups.size > 0 && (e.scrollToIndex = Math.min(...e.bufferedGroups)), c = true;
  } else if (h.size > 0 && Id(h, e.bufferedGroups)) {
    for (const d of e.bufferedGroups) e.hotGroups.has(d) || (e.bufferedGroups.delete(d), y.add(d));
    c = true;
  }
  if (e.bufferedGroups.size === 0 && r.length > 0 && t >= r[r.length - 1].endTime) {
    const d = s ? r.length : r.length - 1;
    e.scrollToIndex !== d && (e.scrollToIndex = d, c = true);
  }
  return e.lastCurrentTime = t, { shouldLayout: c, shouldResetScroll: u, groupsToEnable: m, groupsToDisable: [...y] };
}
var Ld = class extends EventTarget {
  element = document.createElement("div");
  timelineState = { currentTime: 0, lastCurrentTime: 0, hotGroups: /* @__PURE__ */ new Set(), bufferedGroups: /* @__PURE__ */ new Set(), scrollToIndex: 0, isSeeking: false, isPlaying: true, initialLayoutFinished: false };
  lyricGroupElementMap = /* @__PURE__ */ new WeakMap();
  currentLyricLines = [];
  processedLines = [];
  lyricLinesIndexes = /* @__PURE__ */ new WeakMap();
  isNonDynamic = false;
  hasDuetLine = false;
  disableSpring = false;
  layoutState = { interludeDotsSize: [0, 0], targetAlignIndex: 0, lastInterludeState: false, alignAnchor: Ns.Center, alignPosition: 0.35, overscanPx: 300 };
  interludeDots = new md();
  bottomLine = new _d(this);
  enableBlur = true;
  enableScale = true;
  maskObsceneWords = or.Disabled;
  maskObsceneWordChar = "*";
  hidePassedLines = false;
  scrollState = { scrollBoundary: { minOffset: 0, maxOffset: 0 }, scrollOffset: 0, allowScroll: true, isScrolled: false, isUserScrolling: false };
  currentLyricGroups = [];
  lyricGroupSize = /* @__PURE__ */ new WeakMap();
  size = [0, 0];
  isPageVisible = true;
  optimizeOptions = {};
  alwaysPostpositionBackground = false;
  posXSpringParams = { mass: 1, damping: 10, stiffness: 100 };
  posYSpringParams = { mass: 0.9, damping: 15, stiffness: 90 };
  scaleSpringParams = { mass: 2, damping: 25, stiffness: 100 };
  scaleForBGSpringParams = { mass: 1, damping: 20, stiffness: 50 };
  onPageShow = () => {
    this.isPageVisible = true, this.setCurrentTime(this.timelineState.currentTime, true);
  };
  onPageHide = () => {
    this.isPageVisible = false;
  };
  scrolledHandler;
  resizeObserver = new ResizeObserver(((i) => {
    let e = false, t = false;
    for (const r of i) if (r.target === this.element) {
      const s = r.contentRect;
      this.size[0] = s.width, this.size[1] = s.height, t = true;
    } else if (r.target === this.interludeDots.getElement()) this.layoutState.interludeDotsSize[0] = r.target.clientWidth, this.layoutState.interludeDotsSize[1] = r.target.clientHeight, e = true;
    else if (r.target === this.bottomLine.getElement()) {
      const s = [r.target.clientWidth, r.target.clientHeight], n = this.bottomLine.lineSize;
      (s[0] !== n[0] || s[1] !== n[1]) && (this.bottomLine.lineSize = s, e = true);
    } else {
      const s = this.lyricGroupElementMap.get(r.target);
      if (s) {
        const n = [r.target.clientWidth, r.target.clientHeight], a = this.lyricGroupSize.get(s) ?? [0, 0];
        (n[0] !== a[0] || n[1] !== a[1]) && (this.lyricGroupSize.set(s, n), s.onLineSizeChange(n), e = true);
      }
    }
    e && this.calcLayout(true), t && this.onResize();
  }));
  wordFadeWidth = 0.5;
  constructor(i) {
    super(), i && (this.element = i), this.element.classList.add("amll-lyric-player"), this.resizeObserver.observe(this.element), this.resizeObserver.observe(this.interludeDots.getElement()), this.element.appendChild(this.interludeDots.getElement()), this.element.appendChild(this.bottomLine.getElement()), this.interludeDots.setTransform(0, 200), window.addEventListener("pageshow", this.onPageShow), window.addEventListener("pagehide", this.onPageHide), Ad(this.element, this.scrollState, { onBeginScroll: () => this.beginScrollHandler(), onEndScroll: () => this.endScrollHandler(), onLayout: (e, t) => this.calcLayout(e, t), containsTarget: (e) => this.element.contains(e), clickTarget: (e) => e.click() });
  }
  beginScrollHandler() {
    const i = this.scrollState.allowScroll;
    return i && (this.scrollState.isScrolled = true, clearTimeout(this.scrolledHandler), this.scrolledHandler = setTimeout(() => {
      this.scrollState.isScrolled = false, this.scrollState.scrollOffset = 0;
    }, 5e3)), i;
  }
  endScrollHandler() {
  }
  setWordFadeWidth(i = 0.5) {
    this.wordFadeWidth = Math.max(1e-4, i);
  }
  setEnableScale(i = true) {
    this.enableScale = i, this.calcLayout();
  }
  getEnableScale() {
    return this.enableScale;
  }
  getWordFadeWidth() {
    return this.wordFadeWidth;
  }
  setIsSeeking(i) {
    this.timelineState.isSeeking = i;
  }
  setHidePassedLines(i) {
    this.hidePassedLines = i, this.calcLayout();
  }
  setEnableBlur(i) {
    this.enableBlur !== i && (this.enableBlur = i, this.calcLayout());
  }
  setMaskObsceneWords(i) {
    this.maskObsceneWords !== i && (this.maskObsceneWords = i, this.rebuildLyricLines(), this.calcLayout());
  }
  setMaskObsceneWordChar(i) {
    const e = i.charAt(0) || "*";
    this.maskObsceneWordChar !== e && (this.maskObsceneWordChar = e, this.maskObsceneWords !== or.Disabled && (this.rebuildLyricLines(), this.calcLayout()));
  }
  rebuildLyricLines() {
    for (const i of this.currentLyricGroups) i.rebuildAllLines();
  }
  processObsceneWord(i) {
    const e = i.word;
    if (!i.obscene || this.maskObsceneWords === or.Disabled) return e;
    const t = this.maskObsceneWordChar;
    if (this.maskObsceneWords === or.FullMask) return e.replace(/\S/g, t);
    if (this.maskObsceneWords === or.PartialMask) {
      const r = e.trim();
      if (r.length <= 2) return e.replace(/\S/g, t);
      const s = e.indexOf(r), n = s + r.length - 1;
      return e.slice(0, s + 1) + e.slice(s + 1, n).replace(/\S/g, t) + e.slice(n);
    }
    return e;
  }
  setAlignAnchor(i) {
    this.layoutState.alignAnchor = i;
  }
  setAlignPosition(i) {
    this.layoutState.alignPosition = i;
  }
  setOverscanPx(i) {
    this.layoutState.overscanPx = Je(i | 0);
  }
  getOverscanPx() {
    return this.layoutState.overscanPx;
  }
  setEnableSpring(i = true) {
    this.disableSpring = !i, i ? this.element.classList.remove(ie.disableSpring) : this.element.classList.add(ie.disableSpring), this.calcLayout(true);
  }
  getEnableSpring() {
    return !this.disableSpring;
  }
  setOptimizeOptions(i) {
    this.optimizeOptions = { ...this.optimizeOptions, ...i };
  }
  setLyricLines(i, e = 0) {
    this.timelineState.initialLayoutFinished = true, this.timelineState.lastCurrentTime = e, this.timelineState.currentTime = e, this.currentLyricLines = xo(i), this.processedLines = xo(this.currentLyricLines), dd(this.processedLines, this.optimizeOptions), this.isNonDynamic = true;
    for (const t of this.processedLines) if (t.words.length > 1) {
      this.isNonDynamic = false;
      break;
    }
    this.hasDuetLine = this.processedLines.some((t) => t.isDuet);
    for (const t of this.currentLyricGroups) t.dispose();
    this.currentLyricGroups = [], this.interludeDots.setInterlude(void 0), this.timelineState.hotGroups.clear(), this.timelineState.bufferedGroups.clear();
  }
  getIsPlaying() {
    return this.timelineState.isPlaying;
  }
  setCurrentTime(i, e = false) {
    i = Math.round(i);
    const { timelineState: t } = this;
    if (t.isSeeking = !!e, t.currentTime = i, !t.initialLayoutFinished && !t.isSeeking) return;
    const r = Rd({ time: i, currentGroups: this.currentLyricGroups, timelineState: t }), s = this.bottomLine.getElement().innerHTML.trim().length > 0, n = Md({ timelineState: t, time: i, currentGroups: this.currentLyricGroups, hasBottomContent: s, stateResult: r });
    for (const a of n.groupsToDisable) this.currentLyricGroups[a]?.disable();
    for (const a of n.groupsToEnable) this.currentLyricGroups[a]?.enable();
    n.shouldResetScroll && this.resetScroll(), n.shouldLayout && this.calcLayout();
  }
  async calcLayout(i = false, e = false) {
    const t = Td({ currentTime: this.timelineState.currentTime, scrollToIndex: this.timelineState.scrollToIndex, currentGroups: this.currentLyricGroups }), r = !!t;
    if (this.layoutState.targetAlignIndex !== this.timelineState.scrollToIndex || this.layoutState.lastInterludeState !== r) {
      this.layoutState.lastInterludeState = r;
      const k = Ed({ enabled: this.getEnableSpring(), currentGroups: this.currentLyricGroups, scrollToIndex: this.timelineState.scrollToIndex, isSeeking: this.timelineState.isSeeking, isInterludeActive: r });
      k.shouldUpdate && k.params && this.setLinePosYSpringParams(k.params);
    }
    let s = -this.scrollState.scrollOffset;
    const n = this.timelineState.scrollToIndex;
    let a = false;
    t ? a = t.isNextDuet : this.interludeDots.setInterlude(void 0);
    const o = (this.baseFontSize || 24) * 0.4, h = this.layoutState.interludeDotsSize[1] + o * 2;
    t && t.anchorLineIndex !== -1 && (s -= h);
    const l = this.size[1] / 5, c = this.currentLyricGroups.slice(0, n).reduce((k, g) => k + (this.lyricGroupSize.get(g)?.[1] ?? l), 0);
    this.scrollState.scrollBoundary.minOffset = -c, s -= c, s += this.size[1] * this.layoutState.alignPosition;
    const u = this.currentLyricGroups[n];
    this.layoutState.targetAlignIndex = n;
    const m = n === this.currentLyricGroups.length;
    this.bottomLine.setFocused(m);
    const y = u ? this.lyricGroupSize.get(u)?.[1] ?? l : m ? this.bottomLine.lineSize[1] : 0;
    if (y > 0) switch (this.layoutState.alignAnchor) {
      case Ns.Bottom:
        s -= y;
        break;
      case Ns.Center:
        s -= y / 2;
        break;
    }
    const d = Math.max(...this.timelineState.bufferedGroups);
    let p = 0, f = i ? 0 : 0.05, _ = false;
    this.currentLyricGroups.forEach((k, g) => {
      const M = this.timelineState.bufferedGroups.has(g), v = t && g === t.anchorLineIndex + 1;
      if (!_ && v) {
        _ = true, s += o;
        let E = 0;
        t && a && (E = this.size[0] - this.layoutState.interludeDotsSize[0]), this.interludeDots.setTransform(E, s), t && this.interludeDots.setInterlude([t.startTime, t.endTime]), s += this.layoutState.interludeDotsSize[1], s += o;
      }
      const O = wd({ groupIndex: g, scrollToIndex: this.timelineState.scrollToIndex, latestIndex: d, hasBuffered: M, hidePassedLines: this.hidePassedLines, isPlaying: this.timelineState.isPlaying, isNonDynamic: this.isNonDynamic, enableBlur: this.enableBlur, isUserScrolling: this.scrollState.isUserScrolling, isCompact: window.innerWidth <= 1024, interlude: t });
      k.setTransform(s, e, p, O.isActive, O.targetOpacity, O.blurLevel), s += this.lyricGroupSize.get(k)?.[1] ?? l, s >= 0 && !this.timelineState.isSeeking && (p += f, g >= this.timelineState.scrollToIndex && (f /= 1.05));
    }), this.scrollState.scrollBoundary.maxOffset = s + this.scrollState.scrollOffset - this.size[1] / 2;
    const T = this.currentLyricGroups.length, I = Gh({ enableBlur: this.enableBlur, isUserScrolling: this.scrollState.isUserScrolling, isActive: m, itemIndex: T, scrollToIndex: this.timelineState.scrollToIndex, latestIndex: d, isCompact: window.innerWidth <= 1024 });
    this.bottomLine.setTransform(0, s, I, e, p);
  }
  setLinePosXSpringParams(i = {}) {
  }
  setLinePosYSpringParams(i = {}) {
    this.posYSpringParams = { ...this.posYSpringParams, ...i }, this.bottomLine.lineTransforms.posY.updateParams(this.posYSpringParams);
    for (const e of this.currentLyricGroups) e.posY.updateParams(this.posYSpringParams), e.bgSlideY.updateParams(this.posYSpringParams);
  }
  setLineScaleSpringParams(i = {}) {
    this.scaleSpringParams = { ...this.scaleSpringParams, ...i }, this.scaleForBGSpringParams = { ...this.scaleForBGSpringParams, ...i };
    for (const e of this.currentLyricGroups) e.mainLine.lineTransforms.scale.updateParams(this.scaleSpringParams), e.bgLine?.lineTransforms.scale.updateParams(this.scaleForBGSpringParams);
  }
  pause() {
    this.interludeDots.pause(), this.timelineState.isPlaying && (this.timelineState.isPlaying = false, this.calcLayout());
  }
  resume() {
    this.interludeDots.resume(), this.timelineState.isPlaying || (this.timelineState.isPlaying = true, this.calcLayout());
  }
  update(i = 0) {
    this.bottomLine.update(i / 1e3), this.interludeDots.update(i);
  }
  onResize() {
  }
  getBottomLineElement() {
    return this.bottomLine.getElement();
  }
  resetScroll() {
    Sd(this.scrollState), clearTimeout(this.scrolledHandler);
  }
  getLyricLines() {
    return this.currentLyricLines;
  }
  getCurrentTime() {
    return this.timelineState.currentTime;
  }
  setAlwaysPostpositionBackground(i) {
    this.alwaysPostpositionBackground !== i && (this.alwaysPostpositionBackground = i, this.rebuildLyricLines(), this.calcLayout());
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
}, Pd = class {
  mainLine;
  bgLine;
  posY = new xr(0);
  bgSlideY = new xr(-80);
  top = 0;
  delay = 0;
  isActive = false;
  opacity = 1;
  blur = 0;
  isBgFirst = false;
  constructor(i, e) {
    this.mainLine = i, this.bgLine = e;
  }
  get startTime() {
    return this.mainLine.getLine().startTime;
  }
  get endTime() {
    return this.mainLine.getLine().endTime;
  }
  onLineSizeChange(i) {
    this.mainLine.onLineSizeChange(i), this.bgLine?.onLineSizeChange(i);
  }
  setTransform(i, e, t, r, s, n) {
    this.top = i, this.delay = t, this.isActive = r, this.opacity = s, this.blur = n, this.setLineTransformations(e, t);
    const a = this.lyricPlayer.getEnableSpring(), o = !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst ? 80 : -80, h = this.lyricPlayer.getIsPlaying(), l = r || !h ? 0 : o;
    e || !a ? (this.posY.setPosition(i), this.bgSlideY.setPosition(l), this.renderStyles()) : (this.posY.setTargetPosition(i, t), this.bgSlideY.setTargetPosition(l, t));
  }
  setLineTransformations(i, e) {
    const t = this.lyricPlayer.getEnableScale(), r = this.lyricPlayer.getIsPlaying(), s = this.isActive ? Et.GRADIENT : Et.SOLID, n = t ? 97 : 100;
    let a = 100;
    !this.isActive && r && (a = n), this.mainLine.setTransform(a, 1, 0, i, e, s);
    let o = 100;
    !this.isActive && r && (o = 75), this.bgLine?.setTransform(o, 1, 0, i, e, s);
  }
  update(i) {
    this.lyricPlayer.getEnableSpring() && (this.posY.update(i), this.bgSlideY.update(i), this.renderStyles()), this.mainLine.update(i), this.bgLine?.update(i);
  }
  rebuildAllLines() {
    this.mainLine.rebuildElement(), this.bgLine?.rebuildElement();
  }
  enable(i, e) {
    this.mainLine.enable(i, e), this.bgLine?.enable(i, e);
  }
  disable() {
    this.mainLine.disable(), this.bgLine?.disable();
  }
  dispose() {
    this.mainLine.dispose(), this.bgLine?.dispose();
  }
}, Fd = class extends Pd {
  lyricPlayer;
  element;
  bgWrapper;
  lastIsActive;
  constructor(i, e) {
    super(e), this.lyricPlayer = i, this.element = document.createElement("div"), this.element.className = ie.lyricLineWrapper, this.element.appendChild(e.getElement()), this.posY.setPosition(window.innerHeight * 2), i.resizeObserver.observe(this.element);
  }
  get isInSight() {
    const i = this.posY.getCurrentPosition();
    let e = this.lyricPlayer.lyricGroupSize?.get(this)?.[1];
    (e === void 0 || e === 0) && (e = this.element.clientHeight || 0);
    const t = this.lyricPlayer.size[1], r = this.lyricPlayer.getOverscanPx();
    return !(i > t + e + r || i < -e - r);
  }
  show() {
    if (!this.element.parentElement) {
      const i = this.lyricPlayer.getElement(), e = this.lyricPlayer.currentLyricGroups, t = e.indexOf(this);
      let r = null;
      if (t !== -1) {
        for (let s = t + 1; s < e.length; s++) if (e[s].element.parentElement === i) {
          r = e[s].element;
          break;
        }
      }
      i.insertBefore(this.element, r), this.lyricPlayer.resizeObserver.observe(this.element);
    }
    this.mainLine.show(), this.bgLine?.show();
  }
  hide() {
    this.element.parentElement && (this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove(), this.mainLine.teardownContent(), this.bgLine?.teardownContent());
  }
  update(i) {
    this.isInSight ? this.show() : this.hide(), super.update(i);
  }
  addBgLine(i) {
    this.bgLine && this.bgLine.dispose(), this.bgWrapper && this.bgWrapper.remove(), this.bgLine = i;
    const e = i.getLine().words[0]?.startTime ?? i.getLine().startTime, t = this.mainLine.getLine().words[0]?.startTime ?? this.mainLine.getLine().startTime;
    this.isBgFirst = e < t, this.mainLine.getLine().isDuet && i.getElement().classList.add(ie.lyricDuetLine), this.bgWrapper = document.createElement("div"), this.bgWrapper.className = ie.bgWrapper, this.bgWrapper.appendChild(i.getElement()), !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst ? (this.bgWrapper.classList.add(ie.bgWrapperTop), this.element.insertBefore(this.bgWrapper, this.mainLine.getElement()), this.bgSlideY.setPosition(80)) : this.element.appendChild(this.bgWrapper);
  }
  renderStyles() {
    const i = this.posY.getCurrentPosition().toFixed(1);
    if (this.element.style.transform = `translateY(${i}px)`, this.element.style.opacity = this.opacity.toString(), this.element.style.filter = `blur(${Math.min(5, this.blur)}px)`, this.lyricPlayer.getEnableSpring() || (this.element.style.transitionDelay = `${this.delay}ms`), this.bgWrapper) {
      this.lastIsActive !== this.isActive && (this.lastIsActive = this.isActive, this.bgWrapper.classList.toggle(ie.bgWrapperActive, this.isActive));
      const e = this.bgSlideY.getCurrentPosition(), t = e.toFixed(1), r = ht(1 - Math.abs(e) / 80), s = (0.8 + r * 0.2).toFixed(3);
      this.bgWrapper.style.transform = `translateY(${t}%) scale(${s})`;
      const n = !this.lyricPlayer.getAlwaysPostpositionBackground() && this.isBgFirst;
      if (n) {
        const o = -(this.bgWrapper.clientHeight || 0) * (1 - r);
        this.bgWrapper.style.marginTop = `${o.toFixed(1)}px`;
      } else this.bgWrapper.style.marginTop = "";
      const a = t === (n ? "80.0" : "-80.0") && !this.isActive;
      this.bgWrapper.classList.toggle(ie.bgWrapperHidden, a);
    }
  }
  dispose() {
    super.dispose(), this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove();
  }
};
const br = (i) => /^[\p{Unified_Ideograph}\u0800-\u9FFC]+$/u.test(i);
var nt = class extends EventTarget {
  top = 0;
  scale = 1;
  blur = 0;
  opacity = 1;
  delay = 0;
  lineTransforms = { scale: new xr(100) };
  static wordSegmenter = typeof Intl < "u" && Intl.Segmenter ? new Intl.Segmenter(void 0, { granularity: "word" }) : null;
  static graphemeSegmenter = typeof Intl < "u" && Intl.Segmenter ? new Intl.Segmenter(void 0, { granularity: "grapheme" }) : null;
  setTransform(i = this.scale, e = this.opacity, t = this.blur, r = false, s = 0, n = Et.SOLID) {
    this.scale = i, this.opacity = e, this.blur = t, this.delay = s;
  }
  rebuildElement() {
  }
  static shouldEmphasize(i) {
    return br(i.word) ? i.endTime - i.startTime >= 1e3 : i.endTime - i.startTime >= 1e3 && i.word.trim().length <= 7 && i.word.trim().length > 1;
  }
  dispose() {
  }
};
const Nd = 1e3, Od = 0.15, Bd = 0.5, kd = 0.4, Ud = 0.6, Dd = /[,.;:!?，。；：！？、）】》」』’”)[\]}>~…]$/;
function Gd(i, e, t, r) {
  const s = i.length;
  if (s === 0 || e <= 0) return [];
  const n = /* @__PURE__ */ new Set();
  let a = 0;
  for (const { segment: p, isWordLike: f } of r.segment(t)) a > 0 && f && [...p].some((_) => br(_)) && n.add(a), a += p.length;
  const o = new Int32Array(s + 1), h = new Float64Array(s + 1);
  for (let p = 0; p < s; p++) o[p + 1] = o[p] + i[p].text.length, h[p + 1] = h[p] + i[p].width;
  if (h[s] <= e) return [];
  const l = new Float64Array(s + 1).fill(Number.POSITIVE_INFINITY), c = new Int32Array(s + 1).fill(-1);
  l[s] = 0;
  const u = (e * Od) ** 2, m = (e * Bd) ** 2;
  for (let p = s - 1; p >= 0; p--) for (let f = p + 1; f <= s; f++) {
    const _ = h[f] - h[p];
    let T = 0;
    if (_ > e) if (f === p + 1) T = (_ - e) ** 2 * Nd;
    else continue;
    else T = (e - _) ** 2;
    let I = 0;
    if (f < s) {
      const g = i[f - 1];
      Dd.test(g.text) ? I = -((e * Ud) ** 2) : g.isSpace ? I = -((e * kd) ** 2) : n.has(o[f]) ? I = u : I = m;
    }
    const k = T + I + l[f];
    k < l[p] && (l[p] = k, c[p] = f);
  }
  const y = [];
  let d = 0;
  for (; d < s; ) d = c[d], d > 0 && d < s && y.push(d);
  return y;
}
let Os = null;
function zd() {
  return Os || (Os = document.createElement("canvas").getContext("2d")), Os;
}
var $d = class {
  mainElement;
  isBalancing = false;
  lastBalancedContainerWidth = -1;
  constructor(i) {
    this.mainElement = i;
  }
  balanceLineBreaks(i, e, t) {
    if (this.isBalancing || !this.mainElement) return;
    const r = getComputedStyle(this.mainElement), s = Number.parseFloat(r.paddingLeft) || 0, n = Number.parseFloat(r.paddingRight) || 0, a = this.mainElement.clientWidth - s - n;
    if (!(a <= 0)) {
      if (i) {
        this.balanceNonDynamicLineBreaks(a, r, t);
        return;
      }
      e && this.balanceDynamicLineBreaks(a, t);
    }
  }
  reset() {
    this.lastBalancedContainerWidth = -1;
  }
  executeLineBalance(i, e, t) {
    const r = this.mainElement.querySelectorAll("br");
    if (i === this.lastBalancedContainerWidth && r.length > 0) return;
    e.resetDOM();
    const s = this.mainElement.style.whiteSpace;
    this.mainElement.style.whiteSpace = "nowrap";
    const n = this.mainElement.parentElement;
    let a = "", o = false;
    n && (a = n.style.transform, a && a !== "none" && (n.style.transform = "none", o = true));
    let h = false;
    try {
      const { childInfos: l, fullText: c } = e.buildChildInfos();
      let u = l.reduce((d, p) => d + p.width, 0);
      if (e.needsCalibration) {
        const d = document.createRange();
        d.selectNodeContents(this.mainElement);
        const p = d.getBoundingClientRect().width;
        if (u > 0 && p > 0) {
          const f = p / u;
          for (const _ of l) _.width *= f;
        }
        u = p;
      }
      const m = Math.max(1, i);
      if (u <= m) {
        this.lastBalancedContainerWidth = i;
        return;
      }
      const y = Gd(l, m, c, t);
      if (y.length === 0) {
        this.lastBalancedContainerWidth = i;
        return;
      }
      this.isBalancing = true, h = true, e.applyBreaks(y, l), this.lastBalancedContainerWidth = i, this.isBalancing = false;
    } finally {
      this.mainElement.style.whiteSpace = s, o && n && (n.style.transform = a), h && (this.isBalancing = false);
    }
  }
  balanceDynamicLineBreaks(i, e) {
    const t = [];
    this.executeLineBalance(i, { resetDOM: () => {
      this.mainElement.querySelectorAll("br").forEach((r) => {
        r.remove();
      });
    }, buildChildInfos: () => {
      t.length = 0;
      const r = Array.from(this.mainElement.childNodes), s = [], n = document.createRange();
      for (const a of r) if (a.nodeType === Node.TEXT_NODE) {
        const o = a.textContent ?? "";
        if (o.length === 0) continue;
        n.selectNodeContents(a), s.push({ width: n.getBoundingClientRect().width, text: o, isSpace: o.trim().length === 0 }), t.push(a);
      } else if (a.nodeType === Node.ELEMENT_NODE) {
        const o = a, h = o.getBoundingClientRect(), l = getComputedStyle(o), c = Number.parseFloat(l.marginLeft) || 0, u = Number.parseFloat(l.marginRight) || 0;
        s.push({ width: Je(h.width + c + u), text: o.textContent ?? "", isSpace: false }), t.push(a);
      }
      return { childInfos: s, fullText: s.map((a) => a.text).join("") };
    }, applyBreaks: (r) => {
      for (let s = r.length - 1; s >= 0; s--) {
        const n = r[s];
        n >= 0 && n < t.length && this.mainElement.insertBefore(document.createElement("br"), t[n]);
      }
    }, needsCalibration: false }, e);
  }
  balanceNonDynamicLineBreaks(i, e, t) {
    const r = this.mainElement.textContent ?? "";
    r.trim().length !== 0 && this.executeLineBalance(i, { resetDOM: () => {
      this.mainElement.innerHTML = "", this.mainElement.textContent = r;
    }, buildChildInfos: () => {
      const s = zd();
      if (!s) return console.debug("Canvas 2D context is not supported, skipping line balancing"), { childInfos: [], fullText: r };
      s.font = `${e.fontWeight} ${e.fontSize} ${e.fontFamily}`, "letterSpacing" in s && (s.letterSpacing = e.letterSpacing !== "normal" ? e.letterSpacing : "0px"), "wordSpacing" in s && (s.wordSpacing = e.wordSpacing !== "normal" ? e.wordSpacing : "0px");
      const n = [];
      for (const { segment: a } of t.segment(r)) n.push({ width: s.measureText(a).width, text: a, isSpace: a.trim().length === 0 });
      return { childInfos: n, fullText: r };
    }, applyBreaks: (s, n) => {
      this.mainElement.innerHTML = "";
      const a = new Set(s), o = document.createDocumentFragment();
      for (let h = 0; h < n.length; h++) a.has(h) && o.appendChild(document.createElement("br")), o.appendChild(document.createTextNode(n[h].text));
      this.mainElement.appendChild(o);
    }, needsCalibration: true }, t);
  }
};
const Hd = /(\s+)/, Vd = /\s/g;
function Wd(i) {
  const e = [];
  let t = [];
  const r = () => {
    t.length > 0 && (e.push(t.length === 1 ? t[0] : [...t]), t = []);
  }, s = (n) => {
    const a = n.word.trim().length === 0, o = (n.ruby?.length ?? 0) > 0, h = br(n.word);
    !a && !o && !h ? t.push(n) : (r(), e.push(n));
  };
  for (const n of i) {
    const a = n.word.trim().length === 0, o = n.romanWord ?? "", h = n.obscene ?? false, l = (n.ruby?.length ?? 0) > 0;
    if (a || l) {
      s({ ...n });
      continue;
    }
    const c = n.word.split(Hd).filter((d) => d.length > 0), u = n.word.replace(Vd, "").length || 1, m = (n.endTime - n.startTime) / u;
    let y = 0;
    for (const d of c) {
      if (!d.trim()) {
        const p = n.startTime + y * m;
        s({ word: d, romanWord: "", startTime: p, endTime: p, obscene: h });
        continue;
      }
      if (br(d) && d.length > 1 && o.trim().length === 0) {
        const p = d.split("");
        for (const f of p) {
          const _ = n.startTime + y * m;
          s({ word: f, romanWord: "", startTime: _, endTime: _ + m, obscene: h }), y += 1;
        }
      } else {
        const p = d.length, f = n.startTime + y * m;
        s({ word: d, romanWord: o, startTime: f, endTime: f + p * m, obscene: h }), y += p;
      }
    }
  }
  return r(), e;
}
function Xd() {
  return [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];
}
function qd(i, e = 1, t = { x: 0, y: 0 }) {
  const [r, s] = [t.x, t.y];
  return [i[0] * e, i[1] * e, i[2] * e, i[3], i[4] * e, i[5] * e, i[6] * e, i[7], i[8] * e, i[9] * e, i[10] * e, i[11], i[12] - r * e + r, i[13] - s * e + s, i[14], i[15]];
}
function jd(i, e = 4) {
  const t = (r, s) => r.toFixed(e);
  return `matrix3d(${i.map(t).join(", ")})`;
}
const $r = 32, zh = (i, e) => (t) => ht((t - i) / (e - i)), xn = 0.5, Yd = zh(0, xn), Kd = zh(xn, 1), Zd = Uh(0.2, 0.4, 0.58, 1), Jd = Uh(0.3, 0, 0.58, 1), Qd = (i) => (e) => e < i ? Zd(Yd(e)) : 1 - Jd(Kd(e));
function wo(i, e = 0, t = "rgba(0,0,0,var(--bright-mask-alpha, 1.0))", r = "rgba(0,0,0,var(--dark-mask-alpha, 1.0))") {
  const s = 2 + i + e, n = i / s, a = (1 - n) / 2;
  return [`linear-gradient(to right,${t} ${a * 100}%,${r} ${(a + n) * 100}%)`, s];
}
var ef = class extends nt {
  lyricPlayer;
  lyricLine;
  element = document.createElement("div");
  splittedWords = [];
  built = false;
  lineSize = [0, 0];
  renderMode = Et.SOLID;
  currentBrightAlpha = 1;
  currentDarkAlpha = 0.2;
  targetBrightAlpha = 1;
  targetDarkAlpha = 0.2;
  balancer;
  constructor(i, e = { words: [], translatedLyric: "", romanLyric: "", startTime: 0, endTime: 0, isBG: false, isDuet: false }) {
    super(), this.lyricPlayer = i, this.lyricLine = e, this.element.setAttribute("class", ie.lyricLine), this.lyricLine.isBG && this.element.classList.add(ie.lyricBgLine), this.lyricLine.isDuet && this.element.classList.add(ie.lyricDuetLine), this.element.appendChild(document.createElement("div")), this.element.appendChild(document.createElement("div")), this.element.appendChild(document.createElement("div"));
    const t = this.element.children[0], r = this.element.children[1], s = this.element.children[2];
    t.setAttribute("class", ie.lyricMainLine), r.setAttribute("class", ie.lyricSubLine), s.setAttribute("class", ie.lyricSubLine), nt.wordSegmenter && (this.balancer = new $d(t)), this.rebuildStyle();
  }
  areWordsOnSameLine(i, e) {
    if (i?.mainElement && e?.mainElement) {
      const t = i.mainElement, r = e.mainElement, s = t.getBoundingClientRect(), n = r.getBoundingClientRect();
      return Math.abs(s.top - n.top) < 10;
    }
    return true;
  }
  isEnabled = false;
  async enable(i = this.lyricPlayer.getCurrentTime(), e = this.lyricPlayer.getIsPlaying()) {
    this.isEnabled = true, this.element.classList.add(ie.active);
    const t = this.element.children[0], r = Je(i - this.lyricLine.startTime);
    for (const s of this.splittedWords) {
      for (const n of s.elementAnimations) {
        n.currentTime = r, n.playbackRate = 1;
        const a = n.effect?.getComputedTiming(), o = Number(a?.duration ?? 0), h = Number(a?.delay ?? 0) + o;
        e && r < h ? n.play() : n.pause();
      }
      for (const n of s.maskAnimations) {
        const a = Math.min(this.totalDuration, r);
        n.currentTime = a, n.playbackRate = 1;
        const o = n.effect?.getComputedTiming(), h = Number(o?.duration ?? 0), l = Number(o?.delay ?? 0) + h;
        e && a < l ? n.play() : n.pause();
      }
    }
    t.classList.add(ie.active);
  }
  disable() {
    this.isEnabled = false, this.element.classList.remove(ie.active), this.renderMode = Et.SOLID;
    const i = this.element.children[0];
    for (const e of this.splittedWords) {
      for (const t of e.elementAnimations) (t.id === "float-word" || t.id.includes("emphasize-word-float-only")) && (t.playbackRate = -1, t.play());
      for (const t of e.maskAnimations) t.pause();
    }
    i.classList.remove(ie.active);
  }
  lastWord;
  async resume() {
    if (this.isEnabled) for (const i of this.splittedWords) {
      for (const e of i.elementAnimations) if (!this.lastWord || this.splittedWords.indexOf(this.lastWord) < this.splittedWords.indexOf(i)) {
        const t = e.effect?.getComputedTiming(), r = t?.duration || 0, s = (t?.delay || 0) + r, n = e.currentTime || 0;
        e.playState !== "finished" && n < s && e.play();
      }
      for (const e of i.maskAnimations) if (!this.lastWord || this.splittedWords.indexOf(this.lastWord) < this.splittedWords.indexOf(i)) {
        const t = e.effect?.getComputedTiming(), r = t?.duration || 0, s = (t?.delay || 0) + r, n = e.currentTime || 0;
        e.playState !== "finished" && n < s && e.play();
      }
    }
  }
  async pause() {
    if (this.isEnabled) for (const i of this.splittedWords) {
      for (const e of i.elementAnimations) e.pause();
      for (const e of i.maskAnimations) e.pause();
    }
  }
  setMaskAnimationState(i = 0) {
    const e = i - this.lyricLine.startTime;
    for (const t of this.splittedWords) for (const r of t.maskAnimations) r.currentTime = lt(e, 0, this.totalDuration), r.playbackRate = 1, e >= 0 && e < this.totalDuration ? r.play() : r.pause();
  }
  getLine() {
    return this.lyricLine;
  }
  lastStyle = "";
  show() {
    this.built || (this.rebuildElement(), this.built = true, this.updateMaskImageSync());
  }
  rebuildStyle() {
    let i = "";
    i += `transform: scale(${(this.lineTransforms.scale.getCurrentPosition() / 100).toFixed(4)});`, this.lyricPlayer.getEnableSpring() || (i += `transition-delay:${this.delay}ms;`), i += `filter:blur(${Math.min(5, this.blur)}px);`, i !== this.lastStyle && (this.lastStyle = i, this.element.setAttribute("style", i));
  }
  rebuildElement() {
    this.disposeElements();
    const i = this.element.children[0], e = this.element.children[1], t = this.element.children[2];
    if (this.lyricPlayer._getIsNonDynamic()) {
      i.textContent = this.lyricLine.words.map((a) => this.lyricPlayer.processObsceneWord(a)).join(""), this.setSubLinesText(e, t);
      return;
    }
    const r = Wd(this.lyricLine.words), s = this.lyricLine.words.some((a) => (a.ruby?.length ?? 0) > 0), n = this.lyricLine.words.some((a) => (a.romanWord?.trim().length ?? 0) > 0);
    i.innerHTML = "";
    for (const a of r) this.buildWord(a, i, s, n);
    this.setSubLinesText(e, t);
  }
  setSubLinesText(i, e) {
    i.textContent = this.lyricLine.translatedLyric, e.textContent = this.lyricLine.romanLyric;
  }
  getRubyCharCount(i) {
    return (i.ruby ?? []).reduce((e, t) => e + t.word.length, 0);
  }
  getRubySegments(i) {
    return (i.ruby ?? []).filter((e) => (e?.word?.trim().length ?? 0) > 0);
  }
  createWord(i, e, t, r) {
    const s = document.createElement("span"), n = [], a = i.romanWord?.trim() ?? "", o = t ? document.createElement("div") : s;
    if (t) {
      const l = document.createElement("div"), c = this.getRubySegments(i);
      for (const u of c) {
        const m = document.createElement("span");
        m.textContent = u.word, m.dataset.startTime = String(u.startTime), m.dataset.endTime = String(u.endTime), l.appendChild(m);
      }
      l.classList.add(ie.rubyWord), s.classList.add(ie.wordWithRuby), o.classList.add(ie.wordBody), s.appendChild(l), s.appendChild(o);
    }
    const h = this.lyricPlayer.processObsceneWord(i);
    if (e) {
      s.classList.add(ie.emphasize);
      const l = h.trim();
      if (nt.graphemeSegmenter) for (const { segment: c } of nt.graphemeSegmenter.segment(l)) {
        const u = document.createElement("span");
        u.textContent = c, n.push(u), o.appendChild(u);
      }
      else for (const c of Array.from(l)) {
        const u = document.createElement("span");
        u.textContent = c, n.push(u), o.appendChild(u);
      }
    } else if (r) {
      const l = document.createElement("div");
      l.textContent = h.trim(), o.appendChild(l);
    } else a.length === 0 && (o.textContent = h.trim());
    if (r) {
      const l = document.createElement("div");
      l.textContent = a.length > 0 ? a : "\xA0", l.classList.add(ie.romanWord), o.appendChild(l);
    }
    return { ...i, mainElement: s, subElements: n, elementAnimations: [this.initFloatAnimation(i, s)], maskAnimations: [], width: 0, height: 0, padding: 0, shouldEmphasize: e };
  }
  buildWord(i, e, t, r) {
    const s = Array.isArray(i) ? i : [i];
    if (s.length === 0) return;
    if (s.every((l) => !l.word.trim())) {
      const l = s.map((c) => c.word).join("");
      e.appendChild(document.createTextNode(l));
      return;
    }
    const n = s.reduce((l, c) => (l.endTime = Math.max(l.endTime, c.endTime), l.startTime = Math.min(l.startTime, c.startTime), l.word += c.word, l), { word: "", romanWord: "", startTime: Number.POSITIVE_INFINITY, endTime: Number.NEGATIVE_INFINITY, wordType: "normal", obscene: false });
    let a = s.some((l) => nt.shouldEmphasize(l));
    br(n.word) || (a = a || nt.shouldEmphasize(n));
    const o = document.createElement("span");
    o.classList.add(ie.emphasizeWrapper);
    const h = [];
    for (const l of s) {
      if (!l.word.trim()) {
        o.appendChild(document.createTextNode(l.word));
        continue;
      }
      const c = this.createWord(l, a, t, r);
      a && h.push(...c.subElements), this.splittedWords.push(c), o.appendChild(c.mainElement);
    }
    if (a && this.splittedWords.length > 0) {
      const l = this.splittedWords[this.splittedWords.length - 1], c = s.reduce((u, m) => u + this.getRubyCharCount(m), 0);
      l.elementAnimations.push(...this.initEmphasizeAnimation(n, h, n.endTime - n.startTime, n.startTime - this.lyricLine.startTime, c));
    }
    e.appendChild(o);
  }
  initFloatAnimation(i, e) {
    const t = i.startTime - this.lyricLine.startTime, r = Math.max(1e3, i.endTime - i.startTime);
    let s = 0.05;
    this.lyricLine.isBG && (s *= 2);
    const n = e.animate([{ transform: "translateY(0px)" }, { transform: `translateY(${-s}em)` }], { duration: Number.isFinite(r) ? r : 0, delay: Number.isFinite(t) ? t : 0, id: "float-word", composite: "add", fill: "both", easing: "ease-out" });
    return n.pause(), n;
  }
  initEmphasizeAnimation(i, e, t, r, s) {
    const n = Je(r);
    let a = Math.max(1e3, t);
    const o = s > 0 ? s : Math.max(1, e.length);
    let h = [], l = a / 2e3;
    l = l > 1 ? Math.sqrt(l) : l ** 3;
    let c = a / 3e3;
    c = c > 1 ? Math.sqrt(c) : c ** 3, l *= 0.6, c *= 0.5, this.lyricLine.words.length > 0 && i.word.includes(this.lyricLine.words[this.lyricLine.words.length - 1].word) && (l *= 1.6, c *= 1.5, a *= 1.2), l = Math.min(1.2, l), c = Math.min(0.8, c);
    const u = Number.isFinite(a) ? a : 0, m = Qd(xn);
    return h = e.flatMap((y, d, p) => {
      const f = n + a / 2.5 / o * d, _ = [], T = new Array($r).fill(0).map((M, v) => {
        const O = (v + 1) / $r, E = m(O), N = m(O) * c, R = qd(Xd(), 1 + E * 0.1 * l), U = -E * 0.03 * l * (p.length / 2 - d), H = -E * 0.025 * l;
        return { offset: O, transform: `${jd(R, 4)} translate(${U}em, ${H}em)`, textShadow: `0 0 ${Math.min(0.3, c * 0.3)}em rgba(255, 255, 255, ${N})` };
      }), I = y.animate(T, { duration: u, delay: Number.isFinite(f) ? f : 0, id: `emphasize-word-${y.textContent}-${d}`, iterations: 1, composite: "replace", fill: "both" });
      I.onfinish = () => {
        I.pause();
      }, I.pause(), _.push(I);
      const k = new Array($r).fill(0).map((M, v) => {
        const O = (v + 1) / $r;
        let E = Math.sin(O * Math.PI);
        return this.lyricLine.isBG && (E *= 2), { offset: O, transform: `translateY(${-E * 0.05}em)` };
      }), g = y.animate(k, { duration: u * 1.4, delay: Number.isFinite(f) ? f - 400 : 0, id: "emphasize-word-float", iterations: 1, composite: "add", fill: "both" });
      return g.onfinish = () => {
        g.pause();
      }, g.pause(), _.push(g), _;
    }), h;
  }
  get totalDuration() {
    return this.lyricLine.endTime - this.lyricLine.startTime;
  }
  onLineSizeChange(i) {
    this.updateMaskImageSync();
  }
  updateMaskImageSync() {
    for (const i of this.splittedWords) {
      const e = i.mainElement;
      e ? (i.padding = Number.parseFloat(getComputedStyle(e).paddingLeft), i.width = e.clientWidth - i.padding * 2, i.height = e.clientHeight - i.padding * 2) : (i.width = 0, i.height = 0, i.padding = 0);
    }
    if (this.balancer && nt.wordSegmenter && this.balancer.balanceLineBreaks(this.lyricPlayer._getIsNonDynamic(), this.splittedWords.length > 0, nt.wordSegmenter), this.lyricPlayer.supportMaskImage ? this.generateWebAnimationBasedMaskImage() : this.generateCalcBasedMaskImage(), this.isEnabled) {
      const i = this.lyricPlayer.getIsPlaying?.() ?? true;
      this.enable(this.lyricPlayer.getCurrentTime(), i);
    }
  }
  generateCalcBasedMaskImage() {
    for (const i of this.splittedWords) {
      const e = i.mainElement;
      if (e) {
        i.width = e.clientWidth, i.height = e.clientHeight;
        const t = i.height * this.lyricPlayer.getWordFadeWidth(), [r, s] = wo(t / i.width), n = `${s * 100}% 100%`;
        this.lyricPlayer.supportMaskImage ? (e.style.maskImage = r, e.style.maskRepeat = "no-repeat", e.style.maskOrigin = "left", e.style.maskSize = n) : (e.style.webkitMaskImage = r, e.style.webkitMaskRepeat = "no-repeat", e.style.webkitMaskOrigin = "left", e.style.webkitMaskSize = n);
        const a = i.width + t, o = `clamp(${-a}px,calc(${-a}px + (var(--amll-player-time) - ${i.startTime})*${a / Math.abs(i.endTime - i.startTime)}px),0px) 0px, left top`;
        e.style.maskPosition = o, e.style.webkitMaskPosition = o;
      }
    }
  }
  generateWebAnimationBasedMaskImage() {
    const i = Math.max(0, ...this.splittedWords.map((e) => e.endTime), this.lyricLine.endTime) - this.lyricLine.startTime;
    this.splittedWords.forEach((e, t) => {
      const r = e.mainElement;
      if (r) {
        const s = e.height * this.lyricPlayer.getWordFadeWidth(), [n, a] = wo(s / (e.width + e.padding * 2)), o = `${a * 100}% 100%`;
        this.lyricPlayer.supportMaskImage ? (r.style.maskImage = n, r.style.maskRepeat = "no-repeat", r.style.maskOrigin = "left", r.style.maskSize = o) : (r.style.webkitMaskImage = n, r.style.webkitMaskRepeat = "no-repeat", r.style.webkitMaskOrigin = "left", r.style.webkitMaskSize = o);
        const h = this.splittedWords.slice(0, t).reduce((T, I) => T + I.width, 0) + (this.splittedWords[0] ? s : 0), l = -(e.width + e.padding * 2 + s), c = (T) => lt(T, l, 0);
        let u = -h - e.width - e.padding - s, m = 0;
        const y = [];
        let d = u, p = 0;
        const f = () => {
          const T = u - d, I = ht(m), k = I - p, g = Math.abs(k / T);
          if (u > l && d < l) {
            const v = Math.abs(d - l) * g, O = `${c(d)}px 0`, E = { offset: p + v, maskPosition: O };
            y.push(E);
          }
          if (u > 0 && d < 0) {
            const v = Math.abs(d) * g, O = `${c(u)}px 0`, E = { offset: p + v, maskPosition: O };
            y.push(E);
          }
          const M = { offset: I, maskPosition: `${c(u)}px 0` };
          y.push(M), d = u, p = I;
        };
        f();
        let _ = 0;
        this.splittedWords.forEach((T, I) => {
          {
            const k = T.startTime - this.lyricLine.startTime, g = k - _;
            m += g / i, g > 0 && f(), _ = k;
          }
          {
            const k = Je(T.endTime - T.startTime), g = this.getRubySegments(T), M = g.reduce((v, O) => v + O.word.length, 0);
            if (M > 0) {
              const v = T.width / M;
              let O = 0;
              for (const R of g) {
                const U = Number.isFinite(R.startTime) ? R.startTime : T.startTime, H = Number.isFinite(R.endTime) ? R.endTime : T.endTime, G = Math.max(U, T.startTime), z = Math.min(Math.max(H, G), T.endTime), b = G - this.lyricLine.startTime, w = b - _;
                m += w / i, w > 0 && f(), _ = b;
                const x = Je(z - G) / R.word.length;
                for (let C = 0; C < R.word.length; C++) m += x / i, u += v, I === 0 && O === 0 && (u += s * 1.5), I === this.splittedWords.length - 1 && O === M - 1 && (u += s * 0.5), x > 0 && f(), _ += x, O++;
              }
              const E = Math.max(T.endTime - this.lyricLine.startTime, _), N = E - _;
              m += N / i, N > 0 && f(), _ = E;
            } else {
              const O = T.width / 1, E = k / 1;
              for (let N = 0; N < 1; N++) m += E / i, u += O, I === 0 && N === 0 && (u += s * 1.5), I === this.splittedWords.length - 1 && N === 0 && (u += s * 0.5), E > 0 && f(), _ += E;
            }
          }
        });
        for (const T of e.maskAnimations) T.cancel();
        try {
          const T = r.animate(y, { duration: i || 1, id: `fade-word-${e.word}-${t}`, fill: "both" });
          T.pause(), e.maskAnimations = [T];
        } catch (T) {
          console.warn("\u5E94\u7528\u6E10\u53D8\u52A8\u753B\u53D1\u751F\u9519\u8BEF", y, i, T);
        }
      }
    });
  }
  getElement() {
    return this.element;
  }
  updateMaskAlphaTargets(i) {
    const e = ht((i - 0.97) / 0.03), t = e * 0.2 + 0.2, r = e * 0.8 + 0.2;
    this.renderMode === Et.SOLID ? (this.targetBrightAlpha = t, this.targetDarkAlpha = t) : (this.targetBrightAlpha = r, this.targetDarkAlpha = t);
  }
  applyAlphaToDom(i) {
    const e = i || 0.016, t = 50, r = 7, s = (o) => 1 - Math.exp(-o * e), n = s(this.targetBrightAlpha > this.currentBrightAlpha ? t : r);
    Math.abs(this.targetBrightAlpha - this.currentBrightAlpha) < 1e-3 ? this.currentBrightAlpha = this.targetBrightAlpha : this.currentBrightAlpha += (this.targetBrightAlpha - this.currentBrightAlpha) * n;
    const a = s(this.targetDarkAlpha > this.currentDarkAlpha ? t : r);
    Math.abs(this.targetDarkAlpha - this.currentDarkAlpha) < 1e-3 ? this.currentDarkAlpha = this.targetDarkAlpha : this.currentDarkAlpha += (this.targetDarkAlpha - this.currentDarkAlpha) * a, this.element.style.setProperty("--bright-mask-alpha", this.currentBrightAlpha.toFixed(3)), this.element.style.setProperty("--dark-mask-alpha", this.currentDarkAlpha.toFixed(3));
  }
  setTransform(i = this.scale, e = 1, t = 0, r = false, s = 0, n = Et.SOLID) {
    super.setTransform(i, e, t, r, s), this.renderMode = n;
    const a = this.lyricPlayer.getEnableSpring();
    this.top = 0, this.scale = i, this.delay = s * 1e3 | 0;
    const o = this.element.children[0];
    if (o.style.opacity = `${e}`, r || !a) {
      this.blur = Math.min(32, t), this.lineTransforms.scale.setPosition(i), this.rebuildStyle();
      const h = this.lineTransforms.scale.getCurrentPosition();
      this.updateMaskAlphaTargets(h / 100), this.currentBrightAlpha = this.targetBrightAlpha, this.currentDarkAlpha = this.targetDarkAlpha, this.element.style.setProperty("--bright-mask-alpha", String(this.currentBrightAlpha)), this.element.style.setProperty("--dark-mask-alpha", String(this.currentDarkAlpha));
    } else this.lineTransforms.scale.setTargetPosition(i), this.blur !== Math.min(5, t) && (this.blur = Math.min(5, t), this.element.style.filter = `blur(${t.toFixed(3)}px)`);
  }
  update(i = 0) {
    if (!this.lyricPlayer.getEnableSpring() || (this.lineTransforms.scale.update(i), this.rebuildStyle(), !this.built)) return;
    const e = this.lineTransforms.scale.getCurrentPosition() / 100;
    this.updateMaskAlphaTargets(e), this.applyAlphaToDom(i);
  }
  _getDebugTargetPos() {
    return `[\u4F4D\u79FB: ${this.top}; \u7F29\u653E: ${this.scale}; \u5EF6\u65F6: ${this.delay}]`;
  }
  teardownContent() {
    this.built && (this.disposeElements(), this.built = false);
  }
  disposeElements() {
    this.balancer?.reset();
    for (const r of this.splittedWords) {
      for (const s of r.elementAnimations) s.cancel();
      for (const s of r.maskAnimations) s.cancel();
      for (const s of r.subElements) s.remove(), s.parentNode?.removeChild(s);
      r.elementAnimations = [], r.maskAnimations = [], r.subElements = [], r.mainElement?.parentNode && r.mainElement.parentNode.removeChild(r.mainElement);
    }
    this.splittedWords = [];
    const i = this.element.children[0], e = this.element.children[1], t = this.element.children[2];
    i && (i.innerHTML = ""), e && (e.innerHTML = ""), t && (t.innerHTML = "");
  }
  dispose() {
    this.disposeElements(), this.lyricPlayer.resizeObserver.unobserve(this.element), this.element.remove();
  }
}, tf = class extends MouseEvent {
  lineIndex;
  line;
  bgLine;
  isPropagationStopped = false;
  constructor(i, e, t, r) {
    super(`line-${r.type}`, r), this.lineIndex = i, this.line = e, this.bgLine = t;
  }
  stopPropagation() {
    this.isPropagationStopped = true, super.stopPropagation();
  }
  stopImmediatePropagation() {
    this.isPropagationStopped = true, super.stopImmediatePropagation();
  }
}, rf = class extends Ld {
  abortController = new AbortController();
  currentLyricGroups = [];
  onResize() {
    const i = getComputedStyle(this.element);
    this._baseFontSize = Number.parseFloat(i.fontSize), this.rebuildStyle();
  }
  supportPlusLighter = CSS.supports("mix-blend-mode", "plus-lighter");
  supportMaskImage = CSS.supports("mask-image", "none");
  innerSize = [0, 0];
  onMouseEventHandler = (i) => {
    const e = i.target;
    if (!(e instanceof Element)) return;
    const t = e.closest(`.${ie.lyricLineWrapper}`);
    if (!t) return;
    const r = this.lyricGroupElementMap.get(t);
    if (!r) return;
    const s = r.mainLine, n = r.bgLine, a = new tf(this.lyricLinesIndexes.get(s) ?? -1, s, n, i);
    (!this.dispatchEvent(a) || a.defaultPrevented) && i.preventDefault(), a.isPropagationStopped && (i.stopPropagation(), i.stopImmediatePropagation());
  };
  _getIsNonDynamic() {
    return this.isNonDynamic;
  }
  _baseFontSize = Number.parseFloat(getComputedStyle(this.element).fontSize);
  get baseFontSize() {
    return this._baseFontSize;
  }
  constructor() {
    super(), this.onResize(), this.element.classList.add("amll-lyric-player", "dom"), this.disableSpring && this.element.classList.add(ie.disableSpring), this.element.addEventListener("click", this.onMouseEventHandler, { signal: this.abortController.signal }), this.element.addEventListener("contextmenu", this.onMouseEventHandler, { signal: this.abortController.signal });
  }
  rebuildStyle() {
  }
  setWordFadeWidth(i = 0.5) {
    super.setWordFadeWidth(i);
    for (const e of this.currentLyricGroups) e.mainLine.updateMaskImageSync(), e.bgLine?.updateMaskImageSync();
  }
  setLyricLines(i, e = 0) {
    super.setLyricLines(i, e), this.hasDuetLine ? this.element.classList.add(ie.hasDuetLine) : this.element.classList.remove(ie.hasDuetLine), this.supportMaskImage || this.element.style.setProperty("--amll-player-time", `${e}`);
    for (const r of this.currentLyricGroups) r.dispose();
    this.currentLyricGroups = [];
    let t = null;
    for (let r = 0; r < this.processedLines.length; r++) {
      const s = this.processedLines[r], n = new ef(this, s);
      this.lyricLinesIndexes.set(n, r), !s.isBG || !t ? (t = new Fd(this, n), this.currentLyricGroups.push(t), this.lyricGroupElementMap.set(t.element, t)) : t.addBgLine(n);
    }
    this.setLinePosXSpringParams({}), this.setLinePosYSpringParams({}), this.setLineScaleSpringParams({}), this.setCurrentTime(e, true), this.calcLayout(true), this.update(0);
  }
  pause() {
    super.pause(), this.element.classList.remove(ie.playing), this.interludeDots.pause();
    for (const i of this.currentLyricGroups) i.mainLine.pause(), i.bgLine?.pause();
  }
  resume() {
    super.resume(), this.element.classList.add(ie.playing), this.interludeDots.resume();
    for (const i of this.currentLyricGroups) i.mainLine.resume(), i.bgLine?.resume();
  }
  update(i = 0) {
    if (!this.timelineState.initialLayoutFinished || (super.update(i), this.supportMaskImage || this.element.style.setProperty("--amll-player-time", `${this.timelineState.currentTime}`), !this.isPageVisible)) return;
    const e = i / 1e3;
    for (const t of this.currentLyricGroups) t.update(e);
  }
  dispose() {
    super.dispose(), this.abortController.abort(), this.element.remove();
    for (const i of this.currentLyricGroups) i.dispose();
    this.bottomLine.dispose(), this.interludeDots.dispose();
  }
};
function ve(i) {
  const e = document.getElementById(i);
  if (!e) throw new Error(`[amll-web] missing element #${i}`);
  return e;
}
const sf = document.getElementById("background-layer"), nf = document.getElementById("lyric-layer"), af = document.getElementById("empty-state"), Ce = ve("player-ui"), of = ve("pp-panels"), hf = ve("pp-title"), So = ve("pp-artist"), Ao = ve("pp-cover"), lf = ve("pp-cover-placeholder"), Ke = ve("pp-meta-window"), Io = ve("pp-meta-empty"), bn = ve("pp-track-value"), cf = ve("pp-time-cur"), uf = ve("pp-time-dur"), df = ve("pp-buffer-hint"), $h = ve("pp-btn-prev"), sn = ve("pp-btn-play"), Hh = ve("pp-btn-next"), nn = ve("pp-btn-repeat"), Vh = ve("pp-btn-shuffle"), ff = ve("pp-btn-queue"), pf = ve("pp-btn-more"), fi = ve("pp-fab-translate"), _r = ve("pp-fabs"), _n = ve("pp-fab-play"), Tn = ve("lyric-slot"), mf = ve("pp-lyric-empty"), St = ve("pp-progress");
function Ge(i) {
  return `<svg viewBox="0 0 24 24" aria-hidden="true">${i}</svg>`;
}
const Ve = { play: Ge('<polygon points="6 3 20 12 6 21 6 3" />'), pause: Ge('<rect x="14" y="4" width="4" height="16" rx="1" /><rect x="6" y="4" width="4" height="16" rx="1" />'), previous: Ge('<polygon points="19 20 9 12 19 4 19 20" /><line x1="5" y1="19" x2="5" y2="5" />'), next: Ge('<polygon points="5 4 15 12 5 20 5 4" /><line x1="19" y1="5" x2="19" y2="19" />'), repeat: Ge('<path d="m17 2 4 4-4 4" /><path d="M3 11v-1a4 4 0 0 1 4-4h14" /><path d="m7 22-4-4 4-4" /><path d="M21 13v1a4 4 0 0 1-4 4H3" />'), repeatOne: Ge('<path d="m17 2 4 4-4 4" /><path d="M3 11v-1a4 4 0 0 1 4-4h14" /><path d="m7 22-4-4 4-4" /><path d="M21 13v1a4 4 0 0 1-4 4H3" /><path d="M11 10h1v4" />'), shuffle: Ge('<path d="M2 18h1.4c1.3 0 2.5-.6 3.3-1.7l6.1-8.6c.8-1.1 2-1.7 3.3-1.7H22" /><path d="m18 2 4 4-4 4" /><path d="M2 6h1.9c1.5 0 2.9.9 3.6 2.2" /><path d="M22 18h-5.9c-1.3 0-2.6-.7-3.3-1.8l-.5-.8" /><path d="m18 14 4 4-4 4" />'), queue: Ge('<path d="M21 15V6" /><circle cx="18.5" cy="15.5" r="2.5" /><path d="M12 12H3" /><path d="M16 6H3" /><path d="M12 18H3" />'), more: Ge('<circle cx="12" cy="5" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="12" cy="19" r="1" />'), translate: Ge('<path d="m5 8 6 6" /><path d="m4 14 6-6 2-3" /><path d="M2 5h12" /><path d="M7 2h1" /><path d="m22 22-5-10-5 10" /><path d="M14 18h6" />') };
$h.innerHTML = Ve.previous;
Hh.innerHTML = Ve.next;
fi.innerHTML = Ve.translate;
_n.innerHTML = Ve.play;
const ni = document.createElement("canvas");
Object.assign(ni.style, { position: "absolute", inset: "0", width: "100%", height: "100%" });
sf.appendChild(ni);
const Mt = new sd(new id(ni), ni);
Mt.setRenderScale(Math.min(window.devicePixelRatio || 1, 2));
const Er = new rf();
Object.assign(Er.getElement().style, { position: "absolute", inset: "0", width: "100%", height: "100%" });
Tn.appendChild(Er.getElement());
nf.style.display = "none";
let En = false, an = performance.now();
function Wh(i) {
  const e = i - an;
  an = i, En || Er.update(e), requestAnimationFrame(Wh);
}
requestAnimationFrame(Wh);
function wn() {
  Mt.getElement().width = window.innerWidth, Mt.getElement().height = window.innerHeight;
}
window.addEventListener("resize", wn);
new ResizeObserver(wn).observe(document.body);
wn();
function qe(i) {
  try {
    window.nativeBridge?.onAction(JSON.stringify(i));
  } catch (e) {
    console.warn("[amll-web] postAction failed", e);
  }
}
function Xh(i) {
  const e = Math.max(0, Math.floor(i / 1e3));
  return `${Math.floor(e / 60)}:${String(e % 60).padStart(2, "0")}`;
}
let ct = [], ai = 0, qt = -2;
function yf(i) {
  return i?.words.map((e) => e.word).join("") ?? "";
}
function Ro(i) {
  let e = -1;
  for (let t = 0; t < ct.length && (ct[t]?.startTime ?? Number.MAX_SAFE_INTEGER) <= i; t++) e = t;
  return e;
}
function Co(i) {
  Ke.replaceChildren();
  for (let e = -2; e <= 2; e++) {
    const t = document.createElement("p");
    t.className = e === 0 ? "pp-meta-line pp-meta-current" : "pp-meta-line", t.textContent = yf(ct[i + e]), Ke.appendChild(t);
  }
}
function qh() {
  if (ct.length === 0) return;
  const i = Ro(ai);
  if (i === qt) return;
  const e = i === qt + 1 && qt >= -1;
  if (qt = i, !e) {
    Co(i);
    return;
  }
  Ke.style.transition = "none", Ke.style.transform = "translateY(0px)", Ke.offsetWidth, Ke.style.transition = "transform 0.4s cubic-bezier(0.32, 0.72, 0, 1)", Ke.style.transform = "", window.setTimeout(() => {
    Ke.style.transition = "none", Co(Ro(ai));
  }, 420);
}
const gf = 3e3;
let wt = null;
function Sn() {
  wt !== null && window.clearTimeout(wt), wt = window.setTimeout(() => {
    _r.classList.remove("is-visible"), _r.setAttribute("aria-hidden", "true"), wt = null;
  }, gf);
}
function jh() {
  Kt === 1 && (_r.classList.add("is-visible"), _r.setAttribute("aria-hidden", "false"), Sn());
}
function vf() {
  wt !== null && window.clearTimeout(wt), wt = null, _r.classList.remove("is-visible");
}
let ut = null, Kt = 0;
function xf() {
  of.style.transform = `translateX(-${Kt * 50}%)`;
}
function bf(i, e) {
  const t = e > 0 ? Math.min(1, Math.max(0, i / e)) : 0;
  bn.style.width = `${t * 100}%`, cf.textContent = Xh(i);
}
function _f() {
  sn.innerHTML = ut?.isPlaying ? Ve.pause : Ve.play, _n.innerHTML = ut?.isPlaying ? Ve.pause : Ve.play, sn.setAttribute("aria-label", ut?.isPlaying ? "\u6682\u505C\u64AD\u653E" : "\u64AD\u653E");
}
window.updatePlayerState = (i) => {
  try {
    const e = JSON.parse(i);
    ut = e, typeof e.insetTopPx == "number" && Ce.style.setProperty("--pp-safe-top", `${e.insetTopPx}px`), typeof e.insetBottomPx == "number" && Ce.style.setProperty("--pp-safe-bottom", `${e.insetBottomPx}px`);
    const t = e.title.length > 0;
    if (Ce.hidden = !t, af.hidden = t, !t) return;
    hf.textContent = e.title;
    const r = e.artist?.trim() ?? "";
    So.textContent = r, So.hidden = r.length === 0, e.coverUrl !== null && e.coverUrl !== void 0 && (Ao.src = e.coverUrl, Ao.hidden = false, lf.hidden = true), _f(), ai = e.positionMs, bf(e.positionMs, e.durationMs), uf.textContent = e.durationMs > 0 ? Xh(e.durationMs) : "--:--", df.hidden = !e.buffering, nn.innerHTML = e.repeatMode === "one" ? Ve.repeatOne : Ve.repeat, nn.classList.toggle("pp-active", e.repeatMode !== "off"), Vh.classList.toggle("pp-active", e.shuffleEnabled);
    const s = ct.length > 0;
    mf.hidden = s, fi.hidden = !e.hasTranslation;
  } catch (e) {
    console.error("[amll-web] updatePlayerState parse failed", e);
  }
};
let Tf = "";
window.updateLyrics = (i) => {
  try {
    const e = JSON.parse(i);
    Tf = e.songId, ct = Array.isArray(e.lines) ? [...e.lines] : [], Er.setLyricLines(e.lines ?? [], 0), Mt.setHasLyric(ct.length > 0), ct.length === 0 ? (Ke.replaceChildren(), Io.hidden = false, qt = -2) : (Io.hidden = true, qt = -2, qh()), e.coverUrl !== null && Mt.setAlbum(e.coverUrl).catch((t) => {
      console.warn("[amll-web] setAlbum failed", t);
    });
  } catch (e) {
    console.error("[amll-web] updateLyrics parse failed", e);
  }
};
window.updatePosition = (i) => {
  Er.setCurrentTime(i), ai = i, qh();
};
window.pauseRender = () => {
  En = true, Mt.pause();
};
window.resumeRender = () => {
  an = performance.now(), En = false, Mt.resume();
};
sn.addEventListener("click", () => qe({ action: "playPause" }));
$h.addEventListener("click", () => qe({ action: "previous" }));
Hh.addEventListener("click", () => qe({ action: "next" }));
ff.addEventListener("click", () => qe({ action: "openQueue" }));
pf.addEventListener("click", () => qe({ action: "openEditMeta" }));
fi.addEventListener("click", () => qe({ action: "toggleTranslation" }));
nn.addEventListener("click", () => {
  const i = ut?.repeatMode === "one" ? "all" : "one";
  qe({ action: "setRepeatMode", mode: i });
});
Vh.addEventListener("click", () => qe({ action: "setShuffle", enabled: !(ut?.shuffleEnabled ?? false) }));
let At = null;
St.addEventListener("pointerdown", (i) => {
  (ut?.durationMs ?? 0) <= 0 || (St.setPointerCapture(i.pointerId), At = Math.min(1, Math.max(0, i.clientX / St.clientWidth)), bn.style.width = `${At * 100}%`);
});
St.addEventListener("pointermove", (i) => {
  At !== null && (At = Math.min(1, Math.max(0, i.clientX / St.clientWidth)), bn.style.width = `${At * 100}%`);
});
function Yh(i) {
  const e = At;
  if (At = null, e === null || !i) return;
  const t = ut?.durationMs ?? 0;
  t <= 0 || qe({ action: "seekTo", positionMs: Math.round(e * t) });
}
St.addEventListener("pointerup", () => Yh(true));
St.addEventListener("pointercancel", () => Yh(false));
let Tr = null;
const Ef = 120, wf = 8;
function Sf() {
  return window.innerWidth * 0.4;
}
Ce.addEventListener("touchstart", (i) => {
  if ((i.target instanceof Element ? i.target : null)?.closest("#pp-progress")) {
    Tr = null;
    return;
  }
  const t = i.changedTouches[0];
  t && (Tr = { startX: t.clientX, startY: t.clientY, direction: "none", dy: 0, dx: 0 });
}, { passive: true });
Ce.addEventListener("touchmove", (i) => {
  const e = Tr, t = i.changedTouches[0];
  !e || !t || (i.preventDefault(), e.dx = t.clientX - e.startX, e.dy = t.clientY - e.startY, e.direction === "none" && Math.max(Math.abs(e.dx), Math.abs(e.dy)) > wf && (e.direction = Math.abs(e.dy) > Math.abs(e.dx) ? "vertical" : "horizontal", e.direction === "vertical" && (Ce.style.transition = "none")), e.direction === "vertical" && Kt === 0 && (Ce.style.transform = `translateY(${Math.max(0, e.dy)}px)`));
}, { passive: false });
function Kh(i) {
  const e = Tr;
  if (Tr = null, !(!e || e.direction === "none")) {
    if (e.direction === "vertical") {
      if (Kt !== 0 || i) {
        Ce.style.transform = "", Ce.style.transition = "";
        return;
      }
      if (e.dy >= Ef) {
        qe({ action: "close" });
        return;
      }
      Ce.style.transition = "transform 0.22s ease-out", Ce.style.transform = "", window.setTimeout(() => {
        Ce.style.transition = "";
      }, 240);
      return;
    }
    !i && Math.abs(e.dx) > Sf() && (Kt = e.dx < 0 ? 1 : 0, xf(), Kt !== 1 && vf());
  }
}
Tn.addEventListener("touchend", () => jh(), { passive: true });
Tn.addEventListener("scroll", () => jh(), { passive: true });
fi.addEventListener("click", () => Sn());
_n.addEventListener("click", () => Sn());
Ce.addEventListener("touchend", () => Kh(false));
Ce.addEventListener("touchcancel", () => Kh(true));
if (window.nativeBridge) try {
  window.nativeBridge.onAction(JSON.stringify({ action: "ready" }));
} catch (i) {
  console.error("[amll-web] ready handshake failed", i);
}
