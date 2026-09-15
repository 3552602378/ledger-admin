<template>
  <div class="user-page">
    <!-- 工具条 -->
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索用户名/昵称"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button v-if="hasPerm('system:user:add')" type="success" @click="openDialog()">
        新增用户
      </el-button>
    </div>

    <!-- 用户表格 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-if="hasPerm('system:user:edit')"
            :model-value="row.status === 1"
            :disabled="row.id === currentUserId"
            @change="handleStatusChange(row)"
          />
          <span v-else>{{ row.status === 1 ? '启用' : '禁用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="hasPerm('system:user:edit')"
            link
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('system:user:reset')"
            link
            type="warning"
            @click="handleResetPassword(row)"
          >
            重置密码
          </el-button>
          <el-button
            v-if="hasPerm('system:user:delete') && row.id !== currentUserId"
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
    <el-pagination
      class="pagination"
      background
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      @change="loadList"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="不填则默认 123456" show-password />
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
  listUsers,
  addUser,
  updateUser,
  updateUserStatus,
  resetUserPassword,
  deleteUser,
  type UserVO,
} from '@/api/user'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'

const permissionStore = usePermissionStore()
const userStore = useUserStore()
const hasPerm = permissionStore.hasPerm
const currentUserId = userStore.userInfo?.id

const loading = ref(false)
const list = ref<UserVO[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

async function loadList() {
  loading.value = true
  try {
    const res: any = await listUsers({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
    })
    list.value = res.data.rows
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

// 启用/禁用
async function handleStatusChange(row: UserVO) {
  const next = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '已启用' : '已禁用')
  } catch {
    /* 拦截器已提示 */
  }
}

// 新增/编辑
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive<any>({
  id: undefined,
  username: '',
  nickname: '',
  password: '',
  status: 1,
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ min: 6, message: '密码至少 6 位', trigger: 'blur' }],
}

function openDialog(row?: UserVO) {
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, nickname: row.nickname, status: row.status, password: '' })
  } else {
    Object.assign(form, { id: undefined, username: '', nickname: '', password: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form.id, { username: form.username, nickname: form.nickname, status: form.status })
    } else {
      await addUser({ username: form.username, nickname: form.nickname, status: form.status, password: form.password || undefined })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// 重置密码
async function handleResetPassword(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确定重置用户「${row.username}」的密码吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  const res: any = await resetUserPassword(row.id)
  ElMessage.success(`密码已重置为：${res.data}`)
}

// 删除
async function handleDelete(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  if (list.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.user-page {
  padding: 4px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>