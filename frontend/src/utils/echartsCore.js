import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
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
  BarChart,
  LineChart,
  PieChart,
  HeatmapChart,
  ScatterChart,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
  GraphicComponent,
  LegendComponent,
  MarkLineComponent,
  CanvasRenderer,
])

export default echarts
