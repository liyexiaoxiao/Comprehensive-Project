let echartsPromise = null

export function loadEcharts() {
  if (!echartsPromise) {
    echartsPromise = import('@/utils/echartsCore').then((module) => module.default)
  }
  return echartsPromise
}
