<template>
  <div class="page-wrapper">
    <el-card class="filter-card">
      <div class="toolbar">
        <el-input v-model="query.orderNo" placeholder="单号" clearable style="width:170px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="router.push(`${basePath}/new`)">新增{{ title }}</el-button>
      </div>
    </el-card>

    <el-card class="list-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="单号" min-width="155" />
        <el-table-column prop="orderDate" label="日期" width="115" />
        <el-table-column :prop="counterpartyNameField" :label="counterpartyLabel" min-width="170" />
        <el-table-column prop="warehouseName" label="仓库" min-width="140" />
        <el-table-column prop="totalQuantity" label="数量" width="100" align="right" />
        <el-table-column prop="totalAmount" label="总金额" width="120" align="right">
          <template #default="{ row }">￥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="245" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`${basePath}/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="router.push(`${basePath}/${row.id}/edit`)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="approve(row)">审核</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="remove(row)">删除</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="warning" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveSalesOrder, cancelSalesOrder, deleteSalesOrder, getSalesOrders } from '@/api/sales'
import { approvePurchaseOrder, cancelPurchaseOrder, deletePurchaseOrder, getPurchaseOrders } from '@/api/purchase'

const props = defineProps<{ type: 'sales' | 'purchase' }>()
const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const query = reactive({ page: 1, size: 10, orderNo: '', status: '' })
const statuses = [
  { label: '草稿', value: 'DRAFT' }, { label: '已审核', value: 'APPROVED' },
  { label: '部分执行', value: props.type === 'sales' ? 'PARTIALLY_OUTBOUND' : 'PARTIALLY_INBOUND' },
  { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' }
]
const isSales = computed(() => props.type === 'sales')
const title = computed(() => isSales.value ? '销售单' : '采购单')
const basePath = computed(() => isSales.value ? '/sales/orders' : '/purchase/orders')
const counterpartyLabel = computed(() => isSales.value ? '客户' : '供应商')
const counterpartyNameField = computed(() => isSales.value ? 'customerName' : 'supplierName')

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) [params.startDate, params.endDate] = dateRange.value
    const res = await (isSales.value ? getSalesOrders(params) : getPurchaseOrders(params))
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function resetQuery() { Object.assign(query, { page: 1, orderNo: '', status: '' }); dateRange.value = []; fetchData() }
async function approve(row: any) {
  await ElMessageBox.confirm(`确认审核该${title.value}？`, '提示')
  await (isSales.value ? approveSalesOrder(row.id) : approvePurchaseOrder(row.id))
  ElMessage.success('已审核'); fetchData()
}
async function cancel(row: any) {
  await ElMessageBox.confirm(`确认取消该${title.value}？`, '提示')
  await (isSales.value ? cancelSalesOrder(row.id) : cancelPurchaseOrder(row.id))
  ElMessage.success('已取消'); fetchData()
}
async function remove(row: any) {
  await ElMessageBox.confirm('删除后不可恢复，是否继续？', '提示', { type: 'warning' })
  await (isSales.value ? deleteSalesOrder(row.id) : deletePurchaseOrder(row.id))
  ElMessage.success('已删除'); fetchData()
}
function money(value: any) { return Number(value || 0).toFixed(2) }
function statusType(status: string) { return ({ DRAFT: 'info', APPROVED: 'warning', PARTIALLY_INBOUND: 'primary', PARTIALLY_OUTBOUND: 'primary', COMPLETED: 'success', CANCELLED: 'danger' } as any)[status] || 'info' }
function statusLabel(status: string) { return ({ DRAFT: '草稿', APPROVED: '已审核', PARTIALLY_INBOUND: '部分入库', PARTIALLY_OUTBOUND: '部分出库', COMPLETED: '已完成', CANCELLED: '已取消' } as any)[status] || status }
</script>

<style scoped>
.list-card { margin-top: 14px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
