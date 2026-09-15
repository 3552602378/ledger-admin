<template>
  <div class="record-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card income">
        <div class="stat-label">收入</div>
        <div class="stat-value">+{{ fmt(statistics.totalIncome) }}</div>
      </div>
      <div class="stat-card expense">
        <div class="stat-label">支出</div>
        <div class="stat-value">-{{ fmt(statistics.totalExpense) }}</div>
      </div>
      <div class="stat-card balance">
        <div class="stat-label">结余</div>
        <div class="stat-value" :class="statistics.balance >= 0 ? 'p' : 'n'">
          {{ fmt(statistics.balance) }}
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="filter.dateType" @change="handleSearch">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="today">今天</el-radio-button>
        <el-radio-button value="week">本周</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
        <el-radio-button value="custom">自定义</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-if="filter.dateType === 'custom'"
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 240px"
      />
      <el-select v-model="filter.type" placeholder="收支类型" clearable style="width: 120px" @change="handleSearch">
        <el-option label="收入" value="income" />
        <el-option label="支出" value="expense" />
      </el-select>
      <el-select
        v-model="filter.categoryId"
        placeholder="分类"
        clearable
        filterable
        style="width: 150px"
        @change="handleSearch"
      >
        <el-option v-for="c in typeCategories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input
        v-model="filter.remark"
        placeholder="备注搜索"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <span style="flex: 1" />
      <el-button v-if="hasPerm('finance:record:add')" type="success" @click="openDrawer()">
        新增收支
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="recordDate" label="日期" width="120" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'income' ? 'success' : 'warning'">
            {{ row.type === 'income' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="130" />
      <el-table-column label="金额" width="140">
        <template #default="{ row }">
          <span :class="row.type === 'income' ? 'amt-income' : 'amt-expense'">
            {{ row.type === 'income' ? '+' : '-' }}{{ fmt(row.amount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="hasPerm('finance:record:edit')"
            link
            type="primary"
            @click="openDrawer(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('finance:record:delete')"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
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

    <!-- 新增/编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="form.id ? '编辑收支' : '新增收支'"
      size="420px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" @change="form.categoryId = undefined">
            <el-radio value="income">收入</el-radio>
            <el-radio value="expense">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" filterable style="width: 100%">
            <el-option v-for="c in drawerCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="日期" prop="recordDate">
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRecords,
  getStatistics,
  addRecord,
  updateRecord,
  deleteRecord,
  type RecordVO,
} from '@/api/record'
import { listCategories, type CategoryVO } from '@/api/category'
import { usePermissionStore } from '@/stores/permission'

const permissionStore = usePermissionStore()
const hasPerm = permissionStore.hasPerm

// 分页
const rows = ref<RecordVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 统计
const statistics = reactive({ totalIncome: 0, totalExpense: 0, balance: 0 })

// 筛选
const filter = reactive<any>({
  dateType: '',
  type: '',
  categoryId: undefined,
  remark: '',
})
const dateRange = ref<[string, string]>()

// 分类（过滤当前类型）
const categories = ref<CategoryVO[]>([])
const typeCategories = computed(() =>
  filter.type ? categories.value.filter((c) => c.type === filter.type) : categories.value,
)
// 抽屉内分类按表单所选类型过滤，避免与顶部筛选栏类型耦合
const drawerCategories = computed(() =>
  form.type ? categories.value.filter((c) => c.type === form.type) : categories.value,
)

function fmt(v: number | string | undefined): string {
  return Number(v ?? 0).toFixed(2)
}

function buildParams(withType = true) {
  const p: any = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filter.dateType) p.dateType = filter.dateType
  if (filter.dateType === 'custom' && dateRange.value && dateRange.value.length === 2) {
    p.startDate = dateRange.value[0]
    p.endDate = dateRange.value[1]
  }
  if (withType && filter.type) p.type = filter.type
  if (filter.categoryId != null && filter.categoryId !== '') p.categoryId = filter.categoryId
  if (filter.remark) p.remark = filter.remark
  return p
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await listRecords(buildParams())
    if (res.data) {
      rows.value = res.data.rows
      total.value = Number(res.data.total)
    }
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  const res: any = await getStatistics(buildParams(false))
  if (res.data) {
    statistics.totalIncome = res.data.totalIncome
    statistics.totalExpense = res.data.totalExpense
    statistics.balance = res.data.balance
  }
}

async function refresh() {
  pageNum.value = 1
  await Promise.all([loadList(), loadStatistics()])
}

function handleSearch() {
  pageNum.value = 1
  loadList()
  loadStatistics()
}

function handleReset() {
  filter.dateType = ''
  filter.type = ''
  filter.categoryId = undefined
  filter.remark = ''
  dateRange.value = undefined
  handleSearch()
}

// 抽屉
const drawerVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive<any>({ id: undefined, type: 'expense', categoryId: undefined, amount: 1, recordDate: '', remark: '' })

const formRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

function openDrawer(row?: RecordVO) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      type: row.type,
      categoryId: row.categoryId,
      amount: row.amount,
      recordDate: row.recordDate,
      remark: row.remark ?? '',
    })
  } else {
    const d = new Date()
    const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    Object.assign(form, { id: undefined, type: 'expense', categoryId: undefined, amount: 1, recordDate: today, remark: '' })
  }
  drawerVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      type: form.type,
      categoryId: form.categoryId,
      amount: form.amount,
      recordDate: form.recordDate,
      remark: form.remark,
    }
    if (form.id) {
      await updateRecord(form.id, payload)
    } else {
      await addRecord(payload)
    }
    ElMessage.success('保存成功')
    drawerVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RecordVO) {
  try {
    await ElMessageBox.confirm(`确定删除这笔记录吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteRecord(row.id)
  ElMessage.success('删除成功')
  refresh()
}

// 当前登录用户无 finance:record:add 时，抽屉里的新增按钮也需要权限（已用 hasPerm 控制入口）
watch(
  () => filter.type,
  () => {
    if (
      filter.categoryId != null &&
      filter.categoryId !== '' &&
      !typeCategories.value.some((c) => c.id === filter.categoryId)
    ) {
      filter.categoryId = undefined
    }
  },
)

async function init() {
  const res: any = await listCategories()
  categories.value = res.data
  await refresh()
}

onMounted(init)
</script>

<style scoped>
.record-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.stat-card {
  padding: 18px 22px;
  border-radius: 8px;
  color: #fff;
}
.stat-card.income {
  background: linear-gradient(135deg, #67c23a, #4eb30d);
}
.stat-card.expense {
  background: linear-gradient(135deg, #f56c6c, #e63e3e);
}
.stat-card.balance {
  background: linear-gradient(135deg, #409eff, #1e7bd6);
}
.stat-label {
  font-size: 13px;
  opacity: 0.85;
}
.stat-value {
  font-size: 26px;
  font-weight: 600;
  margin-top: 6px;
}
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border-radius: 8px;
}
.pager {
  display: flex;
  justify-content: flex-end;
}
.amt-income {
  color: #67c23a;
  font-weight: 600;
}
.amt-expense {
  color: #f56c6c;
  font-weight: 600;
}
.p {
  color: #fff;
}
.n {
  color: #ffd2d2;
}
</style>