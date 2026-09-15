<template>
  <div class="menu-page">
    <div class="toolbar">
      <el-button v-if="hasPerm('system:menu:add')" type="success" @click="openDialog()">
        新增菜单
      </el-button>
    </div>

    <el-table
      :data="tree"
      v-loading="loading"
      border
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
    >
      <el-table-column prop="menuName" label="菜单名称" min-width="180" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.menuType)">{{ typeText(row.menuType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限标识/路径" min-width="200">
        <template #default="{ row }">
          <span v-if="row.menuType === 3">{{ row.perms }}</span>
          <span v-else>{{ row.path || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.menuType !== 3 && hasPerm('system:menu:add')"
            link
            type="primary"
            @click="openDialog(undefined, row.id)"
          >
            新增子级
          </el-button>
          <el-button
            v-if="hasPerm('system:menu:edit')"
            link
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('system:menu:delete')"
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', children: 'children' }"
            check-strictly
            node-key="id"
            placeholder="不选则为根目录"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" label="路由路径">
          <el-input v-model="form.path" placeholder="如 system/user" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" label="组件">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 3" label="权限标识">
          <el-input v-model="form.perms" placeholder="如 system:user:add" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMenuTree,
  addMenu,
  updateMenu,
  deleteMenu,
  type MenuNode,
} from '@/api/menu'
import { usePermissionStore } from '@/stores/permission'

const permissionStore = usePermissionStore()
const hasPerm = permissionStore.hasPerm

const loading = ref(false)
const tree = ref<MenuNode[]>([])

async function loadTree() {
  loading.value = true
  try {
    const res: any = await getMenuTree()
    tree.value = res.data
  } finally {
    loading.value = false
  }
}

function typeText(type: number) {
  return type === 1 ? '目录' : type === 2 ? '菜单' : '按钮'
}
function typeTag(type: number) {
  return type === 1 ? 'primary' : type === 2 ? 'success' : 'warning'
}

// 上级菜单选项（允许挂到目录/菜单，不含按钮）
const parentOptions = computed<MenuNode[]>(() => {
  const strip = (nodes: MenuNode[]): MenuNode[] =>
    nodes
      .filter((n) => n.menuType !== 3 && n.id !== form.id)
      .map((n) => ({ ...n, children: n.children ? strip(n.children) : undefined }))
  return strip(tree.value)
})

// 新增/编辑
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive<any>({
  id: undefined,
  parentId: undefined,
  menuName: '',
  menuType: 1,
  path: '',
  component: '',
  perms: '',
  icon: '',
  sortOrder: 0,
  status: 1,
})

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
}

function openDialog(row?: MenuNode, parentId?: number) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      parentId: row.parentId || undefined,
      menuName: row.menuName,
      menuType: row.menuType,
      path: row.path || '',
      component: row.component || '',
      perms: row.perms || '',
      icon: row.icon || '',
      sortOrder: row.sortOrder ?? 0,
      status: row.status ?? 1,
    })
  } else {
    Object.assign(form, {
      id: undefined,
      parentId,
      menuName: '',
      menuType: 1,
      path: '',
      component: '',
      perms: '',
      icon: '',
      sortOrder: 0,
      status: 1,
    })
  }
  dialogVisible.value = true
}

// 切换菜单类型时清理不适用的字段
watch(
  () => form.menuType,
  (t) => {
    if (t === 3) form.perms = form.perms || ''
    else form.component = form.component || ''
  },
)

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId || 0,
      menuName: form.menuName,
      menuType: form.menuType,
      path: form.menuType === 3 ? (form.path || '') : form.path,
      component: form.menuType === 2 ? form.component : form.component || '',
      perms: form.perms || '',
      icon: form.icon || '',
      sortOrder: form.sortOrder ?? 0,
      status: form.status,
    }
    if (form.id) {
      await updateMenu(form.id, payload)
    } else {
      await addMenu(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTree()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: MenuNode) {
  if (row.children && row.children.length) {
    ElMessage.warning('存在子菜单，不能删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

onMounted(loadTree)
</script>

<style scoped>
.menu-page {
  padding: 4px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>