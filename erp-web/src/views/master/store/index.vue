<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.storeCode" placeholder="门店编码" clearable style="width:180px" />
        <el-input v-model="query.storeName" placeholder="门店名称" clearable style="width:180px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="openAdd">新增门店</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="storeCode" label="门店编码" width="140" />
        <el-table-column prop="storeName" label="门店名称" min-width="170" />
        <el-table-column prop="managerName" label="负责人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="155" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog :title="editingId ? '编辑门店' : '新增门店'" v-model="dialogVisible" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="门店编码" prop="storeCode"><el-input v-model="form.storeCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="门店名称" prop="storeName"><el-input v-model="form.storeName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.managerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
          </el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createStore, deleteStore, getStores, updateStore } from '@/api/masterdata'

const loading = ref(false), submitting = ref(false), dialogVisible = ref(false)
const tableData = ref<any[]>([]), total = ref(0), editingId = ref<number | null>(null), formRef = ref()
const query = reactive({ page: 1, size: 10, storeCode: '', storeName: '', status: '' })
const form = reactive<any>({})
const rules = {
  storeCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }]
}
onMounted(fetchData)
async function fetchData() {
  loading.value = true
  try { const res = await getStores(query); tableData.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
function resetForm(row?: any) {
  Object.assign(form, { storeCode: row?.storeCode || '', storeName: row?.storeName || '',
    managerName: row?.managerName || '', contactPhone: row?.contactPhone || '', address: row?.address || '',
    status: row?.status || 'ENABLED', remark: row?.remark || '' })
}
function openAdd() { editingId.value = null; resetForm(); dialogVisible.value = true }
function openEdit(row: any) { editingId.value = row.id; resetForm(row); dialogVisible.value = true }
async function submit() {
  if (!await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    if (editingId.value) await updateStore(editingId.value, form); else await createStore(form)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}
async function remove(row: any) {
  await ElMessageBox.confirm('已关联仓库或员工的门店不能删除，是否继续？', '删除门店', { type: 'warning' })
  await deleteStore(row.id); ElMessage.success('删除成功'); fetchData()
}
</script>

<style scoped>
.page-wrapper { padding: 0; }.toolbar { display:flex;gap:10px;flex-wrap:wrap }.pagination { margin-top:12px;justify-content:flex-end }
</style>
