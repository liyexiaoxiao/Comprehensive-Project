const ISO_LOCAL_DATE_TIME_PATTERN = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/
const SPACE_LOCAL_DATE_TIME_PATTERN = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d+)?$/

export const parseApiDateTime = (value) => {
  if (!value) {
    return new Date(Number.NaN)
  }

  if (value instanceof Date) {
    return new Date(value.getTime())
  }

  if (typeof value === 'number') {
    return new Date(value)
  }

  const normalizedValue = String(value).trim()
  if (!normalizedValue) {
    return new Date(Number.NaN)
  }

  if (ISO_LOCAL_DATE_TIME_PATTERN.test(normalizedValue)) {
    return new Date(normalizedValue)
  }

  if (SPACE_LOCAL_DATE_TIME_PATTERN.test(normalizedValue)) {
    return new Date(normalizedValue.replace(' ', 'T'))
  }

  return new Date(normalizedValue)
}

export const formatApiDateTime = (value, options = {}, fallback = '-') => {
  const date = parseApiDateTime(value)
  if (Number.isNaN(date.getTime())) {
    return fallback
  }
  return date.toLocaleString('zh-CN', {
    hour12: false,
    ...options,
  })
}
