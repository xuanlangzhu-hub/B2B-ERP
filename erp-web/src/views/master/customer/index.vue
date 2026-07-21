<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.customerCode" placeholder="客户编码" clearable style="width:180px" />
        <el-input v-model="query.customerName" placeholder="客户名称" clearable style="width:180px" />
        <el-input v-model="query.contactPhone" placeholder="联系电话" clearable style="width:180px" />
        <el-select v-model="query.categoryId" placeholder="客户分类" clearable style="width:150px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.levelId" placeholder="客户等级" clearable style="width:150px">
          <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="handleAdd">新增</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="customerCode" label="客户编码" width="130" />
        <el-table-column prop="customerName" label="客户名称" width="180" />
        <el-table-column prop="categoryName" label="客户分类" width="120" />
        <el-table-column prop="levelName" label="客户等级" width="120" />
        <el-table-column label="客户标签" min-width="150">
          <template #default="{ row }">{{ (row.tagNames || []).join('、') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="180" />
        <el-table-column prop="creditLimit" label="信用额度" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码" prop="customerCode">
              <el-input v-model="form.customerCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户分类">
              <el-select v-model="form.categoryId" clearable style="width:100%">
                <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="form.levelId" clearable style="width:100%">
                <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户标签">
              <el-select v-model="form.tagIds" multiple clearable style="width:100%" placeholder="请选择客户标签">
                <el-option v-for="item in tagOptions" :key="item.id" :label="item.tagName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="信用额度">
              <el-input-number v-model="form.creditLimit" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账期(天)">
              <el-input-number v-model="form.paymentDays" :precision="0" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="启用" value="ENABLED" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCustomers, getCustomer, createCustomer, updateCustomer, deleteCustomer,
  getCustomerCategoryOptions, getCustomerLevelOptions, getCustomerTagOptions
} from '@/api/masterdata'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, customerCode: '', customerName: '', contactPhone: '', categoryId: '', levelId: '', status: '' })
const categoryOptions = ref<any[]>([])
const levelOptions = ref<any[]>([])
const tagOptions = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增客户')
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({
  customerCode: '',
  customerName: '',
  categoryId: '',
  levelId: '',
  tagIds: [] as number[],
  contactName: '',
  contactPhone: '',
  email: '',
  address: '',
  creditLimit: 0,
  paymentDays: 0,
  status: 'ENABLED',
  remark: ''
})
const formRules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

onMounted(async () => {
  const [categories, levels, tags] = await Promise.all([
    getCustomerCategoryOptions(), getCustomerLevelOptions(), getCustomerTagOptions()
  ])
  categoryOptions.value = categories.data || []
  levelOptions.value = levels.data || []
  tagOptions.value = tags.data || []
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getCustomers(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增客户'
  Object.assign(form, {
    customerCode: '', customerName: '', categoryId: '', levelId: '', tagIds: [], contactName: '', contactPhone: '', email: '',
    address: '', creditLimit: 0, paymentDays: 0, status: 'ENABLED', remark: ''
  })
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  const res = await getCustomer(row.id)
  const detail = res.data
  editingId.value = row.id
  dialogTitle.value = '编辑客户'
  Object.assign(form, {
    customerCode: detail.customerCode, customerName: detail.customerName,
    categoryId: detail.categoryId || '', levelId: detail.levelId || '', tagIds: detail.tagIds || [],
    contactName: detail.contactName, contactPhone: detail.contactPhone, email: detail.email,
    address: detail.address, creditLimit: detail.creditLimit, paymentDays: detail.paymentDays,
    status: detail.status, remark: detail.remark
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingId.value) {
      await updateCustomer(editingId.value, form)
    } else {
      await createCustomer(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该客户吗？', '确认', { type: 'warning' })
  await deleteCustomer(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
