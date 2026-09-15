<template>
  <div class="role-page">
    <div class="toolbar">
      <el-button v-if="hasPerm('system:role:add')" type="success" @click="openDialog()">
        新增角色
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" min-width="120" />
      <el-table-column prop="roleCode" label="角色编码" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="hasPerm('system:role:edit')"
            link
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('system:role:view')"
            link
            type="warning"
            @click="openPermDrawer(row)"
          >
            权限分配
          </el-button>
          <el-button
            v-if="hasPerm('system:role:delete')"
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 tester" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配抽屉 -->
    <el-drawer v-model="permDrawerVisible" :title="`权限分配：${permRole.roleName}`" size="420px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="permDrawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingPerm" @click="handleSavePerm">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRoles,
  addRole,
  updateRole,
  deleteRole,
  getRoleMenus,
  type RoleVO,
} from '@/api/role'
import { getMenuTree, type MenuNode } from '@/api/menu'
import { usePermissionStore } from '@/stores/permission'

const permissionStore = usePermissionStore()
const hasPerm = permissionStore.hasPerm

const loading = ref(false)
const list = ref<RoleVO[]>([])

async function loadList() {
  loading.value = true
  try {
    const res: any = await listRoles()
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
  roleName: '',
  roleCode: '',
  status: 1,
  remark: '',
})

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function openDialog(row?: RoleVO) {
  if (row) {
    Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, status: row.status, remark: row.remark || '' })
  } else {
    Object.assign(form, { id: undefined, roleName: '', roleCode: '', status: 1, remark: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { roleName: form.roleName, roleCode: form.roleCode, status: form.status, remark: form.remark, menuIds: [] }
    if (form.id) {
      await updateRole(form.id, payload)
    } else {
      await addRole(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// 删除
async function handleDelete(row: RoleVO) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// 权限分配
const permDrawerVisible = ref(false)
const savingPerm = ref(false)
const menuTree = ref<MenuNode[]>([])
const treeRef = ref()
const permRole = ref<RoleVO>({ id: 0, roleName: '', roleCode: '', status: 1 })

async function openPermDrawer(row: RoleVO) {
  permRole.value = row
  permDrawerVisible.value = true
  if (!menuTree.value.length) {
    const res: any = await getMenuTree()
    menuTree.value = res.data
  }
  const res: any = await getRoleMenus(row.id)
  await nextTick()
  treeRef.value?.setCheckedKeys(res.data)
}

async function handleSavePerm() {
  const checked = treeRef.value.getCheckedKeys() as number[]
  const halfChecked = treeRef.value.getHalfCheckedKeys() as number[]
  savingPerm.value = true
  try {
    const payload = {
      roleName: permRole.value.roleName,
      roleCode: permRole.value.roleCode,
      status: permRole.value.status,
      remark: permRole.value.remark,
      menuIds: [...checked, ...halfChecked],
    }
    await updateRole(permRole.value.id, payload)
    ElMessage.success('权限保存成功')
    permDrawerVisible.value = false
  } finally {
    savingPerm.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.role-page {
  padding: 4px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.drawer-footer {
  text-align: right;
}
</style>