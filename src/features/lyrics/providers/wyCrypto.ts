/**
 * 网易云 eapi 参数加密（AES-128-ECB + MD5）。
 * 算法对齐 NeteaseCloudMusicApi / any-listen，独立实现、无宿主依赖。
 */

const EAPI_KEY = new TextEncoder().encode('e82ckenh8dichen8')

// ---- MD5（RFC 1321，小端）----
const md5 = (message: string): string => {
  const bytes = new TextEncoder().encode(message)
  const bitLen = bytes.length * 8
  const withOne = bytes.length + 1
  const paddedLen = ((withOne + 8 + 63) & ~63)
  const buf = new Uint8Array(paddedLen)
  buf.set(bytes)
  buf[bytes.length] = 0x80
  const view = new DataView(buf.buffer)
  view.setUint32(paddedLen - 8, bitLen >>> 0, true)
  view.setUint32(paddedLen - 4, Math.floor(bitLen / 0x100000000), true)

  let a0 = 0x67452301
  let b0 = 0xefcdab89
  let c0 = 0x98badcfe
  let d0 = 0x10325476

  const S = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
  ]
  const K = new Uint32Array(64)
  for (let i = 0; i < 64; i++) {
    K[i] = Math.floor(Math.abs(Math.sin(i + 1)) * 0x100000000) >>> 0
  }

  const rotl = (x: number, n: number) => ((x << n) | (x >>> (32 - n))) >>> 0

  for (let offset = 0; offset < paddedLen; offset += 64) {
    const M = new Uint32Array(16)
    for (let i = 0; i < 16; i++) {
      M[i] = view.getUint32(offset + i * 4, true)
    }
    let A = a0
    let B = b0
    let C = c0
    let D = d0
    for (let i = 0; i < 64; i++) {
      let F = 0
      let g = 0
      if (i < 16) {
        F = (B & C) | (~B & D)
        g = i
      } else if (i < 32) {
        F = (D & B) | (~D & C)
        g = (5 * i + 1) % 16
      } else if (i < 48) {
        F = B ^ C ^ D
        g = (3 * i + 5) % 16
      } else {
        F = C ^ (B | ~D)
        g = (7 * i) % 16
      }
      F = (F + A + K[i] + M[g]) >>> 0
      A = D
      D = C
      C = B
      B = (B + rotl(F, S[i])) >>> 0
    }
    a0 = (a0 + A) >>> 0
    b0 = (b0 + B) >>> 0
    c0 = (c0 + C) >>> 0
    d0 = (d0 + D) >>> 0
  }

  const out = new Uint8Array(16)
  const ov = new DataView(out.buffer)
  ov.setUint32(0, a0, true)
  ov.setUint32(4, b0, true)
  ov.setUint32(8, c0, true)
  ov.setUint32(12, d0, true)
  return Array.from(out, (b) => b.toString(16).padStart(2, '0')).join('')
}

// ---- AES-128-ECB + PKCS7（仅加密）----
const SBOX = new Uint8Array([
  0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
  0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
  0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
  0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
  0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
  0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
  0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
  0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
  0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
  0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
  0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
  0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
  0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
  0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
  0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
  0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16,
])

const RCON = new Uint8Array([0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36])

const xtime = (a: number) => ((a << 1) ^ ((a & 0x80) !== 0 ? 0x1b : 0)) & 0xff

const expandKey = (key: Uint8Array): Uint8Array[] => {
  const w = new Uint8Array(176)
  w.set(key.subarray(0, 16))
  let bytesGenerated = 16
  let rconIter = 1
  const temp = new Uint8Array(4)
  while (bytesGenerated < 176) {
    temp.set(w.subarray(bytesGenerated - 4, bytesGenerated))
    if (bytesGenerated % 16 === 0) {
      const t0 = temp[0]
      temp[0] = SBOX[temp[1]] ^ RCON[rconIter++]
      temp[1] = SBOX[temp[2]]
      temp[2] = SBOX[temp[3]]
      temp[3] = SBOX[t0]
    }
    for (let i = 0; i < 4; i++) {
      w[bytesGenerated] = w[bytesGenerated - 16] ^ temp[i]
      bytesGenerated++
    }
  }
  const rounds: Uint8Array[] = []
  for (let i = 0; i < 11; i++) {
    rounds.push(w.subarray(i * 16, i * 16 + 16))
  }
  return rounds
}

const addRoundKey = (state: Uint8Array, roundKey: Uint8Array) => {
  for (let i = 0; i < 16; i++) {
    state[i] ^= roundKey[i]
  }
}

const subBytes = (state: Uint8Array) => {
  for (let i = 0; i < 16; i++) {
    state[i] = SBOX[state[i]]
  }
}

const shiftRows = (state: Uint8Array) => {
  const t = state.slice()
  state[1] = t[5]
  state[5] = t[9]
  state[9] = t[13]
  state[13] = t[1]
  state[2] = t[10]
  state[6] = t[14]
  state[10] = t[2]
  state[14] = t[6]
  state[3] = t[15]
  state[7] = t[3]
  state[11] = t[7]
  state[15] = t[11]
}

const mixColumns = (state: Uint8Array) => {
  for (let c = 0; c < 4; c++) {
    const i = c * 4
    const a0 = state[i]
    const a1 = state[i + 1]
    const a2 = state[i + 2]
    const a3 = state[i + 3]
    const t = a0 ^ a1 ^ a2 ^ a3
    state[i] ^= t ^ xtime(a0 ^ a1)
    state[i + 1] ^= t ^ xtime(a1 ^ a2)
    state[i + 2] ^= t ^ xtime(a2 ^ a3)
    state[i + 3] ^= t ^ xtime(a3 ^ a0)
  }
}

const encryptBlock = (input: Uint8Array, roundKeys: Uint8Array[]): Uint8Array => {
  const state = input.slice()
  addRoundKey(state, roundKeys[0])
  for (let round = 1; round < 10; round++) {
    subBytes(state)
    shiftRows(state)
    mixColumns(state)
    addRoundKey(state, roundKeys[round])
  }
  subBytes(state)
  shiftRows(state)
  addRoundKey(state, roundKeys[10])
  return state
}

const aes128EcbEncryptPkcs7 = (plain: Uint8Array, key: Uint8Array): Uint8Array => {
  const roundKeys = expandKey(key)
  const pad = 16 - (plain.length % 16)
  const padded = new Uint8Array(plain.length + pad)
  padded.set(plain)
  padded.fill(pad, plain.length)
  const out = new Uint8Array(padded.length)
  for (let i = 0; i < padded.length; i += 16) {
    out.set(encryptBlock(padded.subarray(i, i + 16), roundKeys), i)
  }
  return out
}

const toHexUpper = (bytes: Uint8Array): string =>
  Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('').toUpperCase()

/** 生成 eapi form 字段 params（大写 hex） */
export const buildEapiParams = (apiPath: string, payload: Record<string, unknown>): string => {
  const text = JSON.stringify(payload)
  const message = `nobody${apiPath}use${text}md5forencrypt`
  const digest = md5(message)
  const data = `${apiPath}-36cd479b6b5-${text}-36cd479b6b5-${digest}`
  const encrypted = aes128EcbEncryptPkcs7(new TextEncoder().encode(data), EAPI_KEY)
  return toHexUpper(encrypted)
}

/** 供测试：导出 md5 对照 */
export const __md5ForTest = md5
