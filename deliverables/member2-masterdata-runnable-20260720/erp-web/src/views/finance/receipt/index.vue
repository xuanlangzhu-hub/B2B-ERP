<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.receiptNo" placeholder="收款单号" clearable style="width:160px" />
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="handleAdd">新增收款</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-alert title="收款单确认后才会增加账户余额，并自动核销该客户最早的未结应收。" type="info" :closable="false" show-icon style="margin-bottom:12px" />
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="receiptNo" label="单号" width="160" />
        <el-table-column prop="receiptDate" label="日期" width="110" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="accountName" label="收款账户" min-width="130" />
        <el-table-column prop="receiptAmount" label="金额" width="100" />
        <el-table-column prop="allocatedAmount" label="已核销" width="100" />
        <el-table-column prop="unallocatedAmount" label="未分配" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status !== 'CANCELLED'" link type="danger" @click="handleCancel(row)">{{ row.status === 'CONFIRMED' ? '冲销' : '取消' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog title="新增收款草稿" v-model="dialogVisible" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="客户"><el-select v-model="form.customerId" filterable style="width:100%"><el-option v-for="c in customers" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item>
        <el-form-item label="收款账户"><el-select v-model="form.accountId" style="width:100%"><el-option v-for="a in accounts" :key="a.value" :label="`${a.label}（余额 ${a.balance}）`" :value="a.value" /></el-select></el-form-item>
        <el-form-item label="收款金额"><el-input-number v-model="form.receiptAmount" :min="0.01" :precision="2" style="width:100%" controls-position="right" /></el-form-item>
        <el-form-item label="收款方式"><el-select v-model="form.paymentMethod" style="width:100%"><el-option label="现金" value="CASH" /><el-option label="银行转账" value="BANK" /><el-option label="微信" value="WECHAT" /><el-option label="支付宝" value="ALIPAY" /><el-option label="其他" value="OTHER" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReceipts, createReceipt, confirmReceipt, cancelReceipt, getAccountOptions } from '@/api/finance'
import { getCustomerOptions } from '@/api/masterdata'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, receiptNo: '' })
const dateRange = ref<string[]>([])
const userStore = useUserStore()
const dialogVisible = ref(false)
const form = reactive({ storeId: null as number | null, customerId: null as number | null, accountId: null as number | null, receiptAmount: 0, paymentMethod: 'BANK', receiptDate: '', remark: '' })
const customers = ref<any[]>([])
const accounts = ref<any[]>([])

onMounted(async () => {
  await Promise.all([loadOptions(), fetchData()])
})

async function loadOptions() {
  const [customerRes, accountRes] = await Promise.all([getCustomerOptions(), getAccountOptions()])
  customers.value = customerRes.data || []
  accounts.value = accountRes.data || []
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) [params.startDate, params.endDate] = dateRange.value
    const res = await getReceipts(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleAdd() {
  Object.assign(form, { storeId: userStore.userInfo?.defaultStoreId || null, customerId: null, accountId: accounts.value[0]?.value || null, receiptAmount: 0, paymentMethod: 'BANK', receiptDate: new Date().toISOString().slice(0, 10), remark: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.storeId) return ElMessage.warning('当前用户未设置默认门店')
  if (!form.customerId || !form.accountId || !form.receiptDate || form.receiptAmount <= 0) return ElMessage.warning('请完整填写客户、账户、日期和金额')
  await createReceipt(form)
  ElMessage.success('收款草稿已保存')
  dialogVisible.value = false
  fetchData()
}

async function handleConfirm(row: any) {
  await ElMessageBox.confirm(`确认收款 ${row.receiptAmount} 元并自动核销应收？`, '确认收款', { type: 'warning' })
  await confirmReceipt(row.id)
  ElMessage.success('收款已确认并记账')
  await Promise.all([fetchData(), loadOptions()])
}

async function handleCancel(row: any) {
  const action = row.status === 'CONFIRMED' ? '冲销这笔已确认收款' : '取消这张收款草稿'
  await ElMessageBox.confirm(`确定${action}？`, '操作确认', { type: 'warning' })
  await cancelReceipt(row.id)
  ElMessage.success(row.status === 'CONFIRMED' ? '收款已冲销' : '收款单已取消')
  await Promise.all([fetchData(), loadOptions()])
}

function statusText(status: string) { return ({ DRAFT: '草稿', CONFIRMED: '已确认', CANCELLED: '已取消' } as any)[status] || status }
function statusType(status: string) { return status === 'CONFIRMED' ? 'success' : status === 'CANCELLED' ? 'danger' : 'info' }
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
