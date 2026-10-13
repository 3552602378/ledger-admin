<template>
  <div class="oper-log-page">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filter.username"
        placeholder="操作人"
        clearable
        style="width: 130px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-input
        v-model="filter.title"
        placeholder="模块标题"
        clearable
        style="width: 130px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="filter.businessType" placeholder="业务类型" clearable style="width: 120px" @change="handleSearch">
        <el-option v-for="(label, value) in BUSINESS_TYPE" :key="value" :label="label" :value="Number(value)" />
      </el-select>
      <el-select v-model="filter.status" placeholder="状态" clearable style="width: 100px" @change="handleSearch">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 240px"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="createTime" label="操作时间" width="170" />
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="title" label="模块" width="100" />
      <el-table-column label="业务类型" width="90">
        <template #default="{ row }">
          <el-tag :type="BIZ_TAG[row.businessType] || 'info'">
            {{ BUSINESS_TYPE[String(row.businessType)] || row.businessType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="请求" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.requestMethod }} {{ row.requestUrl }}
        </template>
      </el-table-column>
      <el-table-column prop="requestParams" label="请求参数" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseResult" label="错误信息" min-width="160" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">
          {{ row.costTime }} ms
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadList"
        @current-change="loadList"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listOperLogs, type OperLogVO } from '@/api/operlog'

const BUSINESS_TYPE: Record<string, string> = {
  '1': '新增',
  '2': '修改',
  '3': '删除',
  '4': '导出',
  '5': '登录',
}
const BIZ_TAG: Record<number, 'success' | 'primary' | 'danger' | 'warning' | 'info'> = {
  1: 'success',
  2: 'primary',
  3: 'danger',
  4: 'warning',
  5: 'info',
}

const loading = ref(false)
const rows = ref<OperLogVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const dateRange = ref<[string, string] | null>(null)

const filter = reactive<{
  username: string
  title: string
  businessType: number | ''
  status: number | ''
}>({ username: '', title: '', businessType: '', status: '' })

async function loadList() {
  loading.value = true
  try {
    const res: any = await listOperLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: filter.username || undefined,
      title: filter.title || undefined,
      businessType: filter.businessType === '' ? undefined : filter.businessType,
      status: filter.status === '' ? undefined : filter.status,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    })
    rows.value = (res.data.rows ?? []) as OperLogVO[]
    total.value = Number(res.data.total ?? 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  filter.username = ''
  filter.title = ''
  filter.businessType = ''
  filter.status = ''
  dateRange.value = null
  handleSearch()
}

onMounted(loadList)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
