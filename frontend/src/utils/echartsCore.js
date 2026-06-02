import * as echarts from 'echarts/core'
import { HeatmapChart, LineChart, PieChart } from 'echarts/charts'
import {
  GraphicComponent,
  GridComponent,
  LegendComponent,
  MarkLineComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  PieChart,
  HeatmapChart,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
  GraphicComponent,
  LegendComponent,
  MarkLineComponent,
  CanvasRenderer,
])

export default echarts
