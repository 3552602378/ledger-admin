<template>
  <div class="dashboard">
    <div class="chart-card">
      <div class="chart-title">近 12 个月收支</div>
      <div ref="monthRef" class="chart" />
    </div>
    <div class="chart-card">
      <div class="chart-title">近 7 日收支</div>
      <div ref="dayRef" class="chart" />
    </div>
    <div class="chart-card">
      <div class="chart-title">分类金额占比</div>
      <div ref="pieRef" class="chart" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, type DashboardStats } from '@/api/dashboard'

const monthRef = ref<HTMLElement>()
const dayRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()

let monthChart: echarts.ECharts | null = null
let dayChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

async function load() {
  const res: any = await getDashboardStats()
  if (!res.data) return
  const stats = res.data as DashboardStats

  const lineBase = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
  }

  monthChart?.setOption({
    ...lineBase,
    xAxis: { type: 'category', data: stats.months },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: stats.monthIncome },
      { name: '支出', type: 'line', smooth: true, data: stats.monthExpense },
    ],
  })

  dayChart?.setOption({
    ...lineBase,
    xAxis: { type: 'category', data: stats.days },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: stats.dayIncome },
      { name: '支出', type: 'line', smooth: true, data: stats.dayExpense },
    ],
  })

  pieChart?.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 10 },
    series: [
      {
        name: '分类金额',
        type: 'pie',
        radius: ['40%', '65%'],
        data: stats.categories.map((c) => ({ name: c.name, value: c.amount })),
      },
    ],
  })
}

function resize() {
  monthChart?.resize()
  dayChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  monthChart = echarts.init(monthRef.value!)
  dayChart = echarts.init(dayRef.value!)
  pieChart = echarts.init(pieRef.value!)
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  monthChart?.dispose()
  dayChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.chart-card:last-child {
  grid-column: 1 / -1;
}
.chart-title {
  font-weight: 600;
  margin-bottom: 12px;
}
.chart {
  height: 320px;
}
</style>