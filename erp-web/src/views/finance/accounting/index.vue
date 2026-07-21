<template>
  <div class="page-wrapper">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="loadActiveTab">
        <el-tab-pane label="资金账户" name="accounts">
          <div class="toolbar">
            <el-input v-model="accountQuery.keyword" placeholder="账户编码或名称" clearable style="width:220px" />
            <el-button type="primary" @click="fetchAccounts">查询</el-button>
            <el-button type="success" @click="openAccountDialog()">新增账户</el-button>
          </div>
          <el-table :data="accounts" v-loading="loading" border stripe style="margin-top:12px">
            <el-table-column prop="accountCode" label="账户编码" width="130" />
            <el-table-column prop="accountName" label="账户名称" min-width="150" />
            <el-table-column prop="accountType" label="类型" width="100" />
            <el-table-column prop="bankName" label="开户行" min-width="140" />
            <el-table-column prop="openingBalance" label="期初余额" width="110" />
            <el-table-column prop="currentBalance" label="当前余额" width="110" />
            <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="130"><template #default="{row}"><el-button link type="primary" @click="openAccountDialog(row)">编辑</el-button><el-button link type="danger" @click="removeAccount(row)">删除</el-button></template></el-table-column>
          </el-table>
          <el-pagination class="pagination" background layout="total,prev,pager,next" :total="accountTotal" v-model:current-page="accountQuery.page" v-model:page-size="accountQuery.size" @change="fetchAccounts" />
        </el-tab-pane>

        <el-tab-pane label="客户应收" name="receivables">
          <div class="toolbar"><el-input v-model="receivableQuery.sourceNo" placeholder="销售单号" clearable style="width:180px" /><el-select v-model="receivableQuery.status" placeholder="结算状态" clearable style="width:160px"><el-option label="未结清" value="UNSETTLED" /><el-option label="部分结清" value="PARTIALLY_SETTLED" /><el-option label="已结清" value="SETTLED" /></el-select><el-button type="primary" @click="fetchReceivables">查询</el-button></div>
          <el-table :data="receivables" v-loading="loading" border stripe style="margin-top:12px">
            <el-table-column prop="receivableNo" label="应收单号" width="170" /><el-table-column prop="sourceNo" label="销售单号" width="160" /><el-table-column prop="customerName" label="客户" min-width="160" /><el-table-column prop="businessDate" label="业务日期" width="110" /><el-table-column prop="originalAmount" label="原始应收" width="110" /><el-table-column prop="receivedAmount" label="已收" width="100" /><el-table-column prop="outstandingAmount" label="未收" width="100" /><el-table-column prop="status" label="状态" width="110"><template #default="{row}">{{ settlementText(row.status) }}</template></el-table-column>
          </el-table>
          <el-pagination class="pagination" background layout="total,prev,pager,next" :total="receivableTotal" v-model:current-page="receivableQuery.page" v-model:page-size="receivableQuery.size" @change="fetchReceivables" />
        </el-tab-pane>

        <el-tab-pane label="供应商应付" name="payables">
          <div class="toolbar"><el-input v-model="payableQuery.sourceNo" placeholder="采购单号" clearable style="width:180px" /><el-select v-model="payableQuery.status" placeholder="结算状态" clearable style="width:160px"><el-option label="未结清" value="UNSETTLED" /><el-option label="部分结清" value="PARTIALLY_SETTLED" /><el-option label="已结清" value="SETTLED" /></el-select><el-button type="primary" @click="fetchPayables">查询</el-button></div>
          <el-table :data="payables" v-loading="loading" border stripe style="margin-top:12px">
            <el-table-column prop="payableNo" label="应付单号" width="170" /><el-table-column prop="sourceNo" label="采购单号" width="160" /><el-table-column prop="supplierName" label="供应商" min-width="160" /><el-table-column prop="businessDate" label="业务日期" width="110" /><el-table-column prop="originalAmount" label="原始应付" width="110" /><el-table-column prop="paidAmount" label="已付" width="100" /><el-table-column prop="outstandingAmount" label="未付" width="100" /><el-table-column prop="status" label="状态" width="110"><template #default="{row}">{{ settlementText(row.status) }}</template></el-table-column>
          </el-table>
          <el-pagination class="pagination" background layout="total,prev,pager,next" :total="payableTotal" v-model:current-page="payableQuery.page" v-model:page-size="payableQuery.size" @change="fetchPayables" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog :title="accountForm.id ? '编辑账户' : '新增账户'" v-model="accountDialog" width="520px">
      <el-form :model="accountForm" label-width="100px">
        <el-form-item label="账户编码"><el-input v-model="accountForm.accountCode" /></el-form-item>
        <el-form-item label="账户名称"><el-input v-model="accountForm.accountName" /></el-form-item>
        <el-form-item label="账户类型"><el-select v-model="accountForm.accountType" style="width:100%"><el-option label="现金" value="CASH" /><el-option label="银行" value="BANK" /><el-option label="线上账户" value="ONLINE" /><el-option label="其他" value="OTHER" /></el-select></el-form-item>
        <el-form-item label="开户行"><el-input v-model="accountForm.bankName" /></el-form-item>
        <el-form-item label="账号"><el-input v-model="accountForm.accountNumber" /></el-form-item>
        <el-form-item label="期初余额"><el-input-number v-model="accountForm.openingBalance" :precision="2" style="width:100%" :disabled="!!accountForm.id" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="accountForm.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="accountForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="accountDialog=false">取消</el-button><el-button type="primary" @click="saveAccount">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAccounts, createAccount, updateAccount, deleteAccount, getReceivables, getPayables } from '@/api/finance'

const activeTab = ref('accounts')
const loading = ref(false)
const accounts = ref<any[]>([]), receivables = ref<any[]>([]), payables = ref<any[]>([])
const accountTotal = ref(0), receivableTotal = ref(0), payableTotal = ref(0)
const accountQuery = reactive({ page: 1, size: 10, keyword: '' })
const receivableQuery = reactive({ page: 1, size: 10, sourceNo: '', status: '' })
const payableQuery = reactive({ page: 1, size: 10, sourceNo: '', status: '' })
const accountDialog = ref(false)
const accountForm = reactive<any>({ id: null, accountCode: '', accountName: '', accountType: 'BANK', bankName: '', accountNumber: '', openingBalance: 0, status: 'ENABLED', remark: '' })

onMounted(fetchAccounts)
function loadActiveTab() { if (activeTab.value === 'accounts') fetchAccounts(); else if (activeTab.value === 'receivables') fetchReceivables(); else fetchPayables() }
async function fetchAccounts() { loading.value = true; try { const r = await getAccounts(accountQuery); accounts.value = r.data.records; accountTotal.value = r.data.total } finally { loading.value = false } }
async function fetchReceivables() { loading.value = true; try { const r = await getReceivables(receivableQuery); receivables.value = r.data.records; receivableTotal.value = r.data.total } finally { loading.value = false } }
async function fetchPayables() { loading.value = true; try { const r = await getPayables(payableQuery); payables.value = r.data.records; payableTotal.value = r.data.total } finally { loading.value = false } }

function openAccountDialog(row?: any) {
  Object.assign(accountForm, row ? { ...row } : { id: null, accountCode: '', accountName: '', accountType: 'BANK', bankName: '', accountNumber: '', openingBalance: 0, status: 'ENABLED', remark: '' })
  accountDialog.value = true
}

async function saveAccount() {
  if (!accountForm.accountCode || !accountForm.accountName || !accountForm.accountType) return ElMessage.warning('请填写账户编码、名称和类型')
  if (accountForm.id) await updateAccount(accountForm.id, accountForm); else await createAccount(accountForm)
  ElMessage.success('账户已保存')
  accountDialog.value = false
  fetchAccounts()
}

async function removeAccount(row: any) {
  await ElMessageBox.confirm(`确定删除账户“${row.accountName}”？已有流水的账户不能删除。`, '删除确认', { type: 'warning' })
  await deleteAccount(row.id)
  ElMessage.success('账户已删除')
  fetchAccounts()
}

function settlementText(status: string) { return ({ UNSETTLED: '未结清', PARTIALLY_SETTLED: '部分结清', SETTLED: '已结清', CANCELLED: '已取消' } as any)[status] || status }
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
