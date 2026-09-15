<template>
  <div class="category-page">
    <div class="toolbar">
      <el-radio-group v-model="activeType" @change="loadList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="income">收入分类</el-radio-button>
        <el-radio-button value="expense">支出分类</el-radio-button>
      </el-radio-group>
      <span style="flex: 1" />
      <el-button v-if="hasPerm('finance:category:add')" type="success" @click="openDialog()">
        新增分类
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="row.type === 'income' ? 'success' : 'warning'">
            {{ row.type === 'income' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="分类名称" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="hasPerm('finance:category:edit')"
            link
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('finance:category:delete')"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="440px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="income">收入</el-radio>
            <el-radio value="expense">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCategories,
  addCategory,
  updateCategory,
  deleteCategory,
  type CategoryVO,
} from '@/api/category'
import { usePermissionStore } from '@/stores/permission'

const permissionStore = usePermissionStore()
const hasPerm = permissionStore.hasPerm

const loading = ref(false)
const list = ref<CategoryVO[]>([])
const activeType = ref('')

async function loadList() {
  loading.value = true
  try {
    const res: any = await listCategories(activeType.value || undefined)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

// 新增/编辑
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive<any>({
  id: undefined,
  type: 'income',
  name: '',
  status: 1,
})

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openDialog(row?: CategoryVO) {
  if (row) {
    Object.assign(form, { id: row.id, type: row.type, name: row.name, status: row.status })
  } else {
    Object.assign(form, { id: undefined, type: activeType.value === 'expense' ? 'expense' : 'income', name: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { type: form.type, name: form.name, status: form.status }
    if (form.id) {
      await updateCategory(form.id, payload)
    } else {
      await addCategory(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: CategoryVO) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.category-page {
  padding: 4px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>