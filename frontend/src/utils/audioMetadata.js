const TEXT_DECODER_LATIN1 = new TextDecoder('latin1')
const TEXT_DECODER_UTF8 = new TextDecoder('utf-8')
const TEXT_DECODER_UTF16 = new TextDecoder('utf-16')
const TEXT_DECODER_UTF16BE = new TextDecoder('utf-16be')

const bytesToBase64 = (bytes) => {
  let binary = ''
  const chunkSize = 0x8000
  for (let index = 0; index < bytes.length; index += chunkSize) {
    const chunk = bytes.subarray(index, index + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return window.btoa(binary)
}

const readSyncSafeInteger = (bytes, offset) =>
  ((bytes[offset] & 0x7f) << 21)
  | ((bytes[offset + 1] & 0x7f) << 14)
  | ((bytes[offset + 2] & 0x7f) << 7)
  | (bytes[offset + 3] & 0x7f)

const readFrameSize = (bytes, offset, version) => {
  if (version === 4) {
    return readSyncSafeInteger(bytes, offset)
  }
  return ((bytes[offset] & 0xff) << 24)
    | ((bytes[offset + 1] & 0xff) << 16)
    | ((bytes[offset + 2] & 0xff) << 8)
    | (bytes[offset + 3] & 0xff)
}

const trimZeroChars = (value) => String(value || '').replace(/\u0000+$/g, '').trim()

const decodeTextBytes = (encoding, bytes) => {
  if (!bytes?.length) return ''

  try {
    if (encoding === 0) return trimZeroChars(TEXT_DECODER_LATIN1.decode(bytes))
    if (encoding === 3) return trimZeroChars(TEXT_DECODER_UTF8.decode(bytes))
    if (encoding === 2) return trimZeroChars(TEXT_DECODER_UTF16BE.decode(bytes))
    return trimZeroChars(TEXT_DECODER_UTF16.decode(bytes))
  } catch {
    return ''
  }
}

const findTerminatorIndex = (bytes, encoding, startIndex) => {
  if (encoding === 1 || encoding === 2) {
    for (let index = startIndex; index < bytes.length - 1; index += 1) {
      if (bytes[index] === 0 && bytes[index + 1] === 0) {
        return index
      }
    }
    return bytes.length
  }

  for (let index = startIndex; index < bytes.length; index += 1) {
    if (bytes[index] === 0) {
      return index
    }
  }
  return bytes.length
}

const decodeApicFrame = (frameBytes) => {
  if (!frameBytes?.length) return ''

  const encoding = frameBytes[0] ?? 0
  const mimeEnd = findTerminatorIndex(frameBytes, 0, 1)
  const mimeType = trimZeroChars(TEXT_DECODER_LATIN1.decode(frameBytes.slice(1, mimeEnd))) || 'image/jpeg'
  const pictureTypeIndex = Math.min(mimeEnd + 1, frameBytes.length)
  const descriptionStart = Math.min(pictureTypeIndex + 1, frameBytes.length)
  const descriptionEnd = findTerminatorIndex(frameBytes, encoding, descriptionStart)
  const imageStart = Math.min(
    descriptionEnd + ((encoding === 1 || encoding === 2) ? 2 : 1),
    frameBytes.length,
  )
  const imageBytes = frameBytes.slice(imageStart)

  if (!imageBytes.length) return ''
  return `data:${mimeType};base64,${bytesToBase64(imageBytes)}`
}

const decodePicFrame = (frameBytes) => {
  if (!frameBytes?.length || frameBytes.length < 5) return ''

  const encoding = frameBytes[0] ?? 0
  const format = trimZeroChars(TEXT_DECODER_LATIN1.decode(frameBytes.slice(1, 4))).toLowerCase()
  const mimeType = format === 'png' ? 'image/png' : 'image/jpeg'
  const descriptionStart = 5
  const descriptionEnd = findTerminatorIndex(frameBytes, encoding, descriptionStart)
  const imageStart = Math.min(
    descriptionEnd + ((encoding === 1 || encoding === 2) ? 2 : 1),
    frameBytes.length,
  )
  const imageBytes = frameBytes.slice(imageStart)

  if (!imageBytes.length) return ''
  return `data:${mimeType};base64,${bytesToBase64(imageBytes)}`
}

const parseId3v2Metadata = (bytes) => {
  if (!bytes?.length || bytes.length < 10) {
    return { title: '', artist: '', coverUrl: '' }
  }

  if (TEXT_DECODER_LATIN1.decode(bytes.slice(0, 3)) !== 'ID3') {
    return { title: '', artist: '', coverUrl: '' }
  }

  const version = bytes[3]
  const tagSize = readSyncSafeInteger(bytes, 6)
  const endOffset = Math.min(bytes.length, 10 + tagSize)
  let offset = 10

  if (bytes[5] & 0x40) {
    const extHeaderSize = version === 4
      ? readSyncSafeInteger(bytes, offset)
      : readFrameSize(bytes, offset, 3)
    offset += extHeaderSize
  }

  const metadata = { title: '', artist: '', coverUrl: '' }

  while (offset < endOffset) {
    const headerSize = version === 2 ? 6 : 10
    if (offset + headerSize > endOffset) break

    const frameId = trimZeroChars(TEXT_DECODER_LATIN1.decode(bytes.slice(offset, offset + (version === 2 ? 3 : 4))))
    if (!frameId) break

    const frameSize = version === 2
      ? ((bytes[offset + 3] & 0xff) << 16) | ((bytes[offset + 4] & 0xff) << 8) | (bytes[offset + 5] & 0xff)
      : readFrameSize(bytes, offset + 4, version)

    if (!frameSize || frameSize < 0) break

    const frameDataStart = offset + headerSize
    const frameDataEnd = Math.min(frameDataStart + frameSize, endOffset)
    const frameBytes = bytes.slice(frameDataStart, frameDataEnd)

    if ((frameId === 'TIT2' || frameId === 'TT2') && !metadata.title) {
      metadata.title = decodeTextBytes(frameBytes[0], frameBytes.slice(1))
    } else if ((frameId === 'TPE1' || frameId === 'TP1') && !metadata.artist) {
      metadata.artist = decodeTextBytes(frameBytes[0], frameBytes.slice(1))
    } else if (frameId === 'APIC' && !metadata.coverUrl) {
      metadata.coverUrl = decodeApicFrame(frameBytes)
    } else if (frameId === 'PIC' && !metadata.coverUrl) {
      metadata.coverUrl = decodePicFrame(frameBytes)
    }

    offset = frameDataEnd
  }

  return metadata
}

export const readAudioTagMetadata = async (file) => {
  if (!file) {
    return { title: '', artist: '', coverUrl: '' }
  }

  try {
    const buffer = await file.arrayBuffer()
    return parseId3v2Metadata(new Uint8Array(buffer))
  } catch {
    return { title: '', artist: '', coverUrl: '' }
  }
}
