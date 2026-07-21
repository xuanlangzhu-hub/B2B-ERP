<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.inboundNo" placeholder="入库单号" clearable style="width:180px" />
        <el-input v-model="query.sourceNo" placeholder="采购/退货单号" clearable style="width:180px" />
        <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:180px">
          <el-option v-for="w in warehouseOptions" :key="w.value" :label="w.label" :value="w.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="handleOpenCreate">新增入库</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="inboundNo" label="入库单号" width="160" />
        <el-table-column prop="inboundDate" label="入库日期" width="120" />
        <el-table-column prop="inboundType" label="入库类型" width="120">
          <template #default="{ row }">{{ inboundTypeLabel(row.inboundType) }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="sourceNo" label="来源单号" width="160" />
        <el-table-column prop="totalQuantity" label="总数量" width="100" />
        <el-table-column prop="totalAmount" label="总金额" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewDetail(row)">查看详情</el-button>
            <el-button size="small" type="success" v-if="row.status === 'DRAFT'" @click="handleConfirm(row)">确认入库</el-button>
            <el-button size="small" type="danger" v-if="row.status === 'DRAFT'" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog title="新增入库" v-model="createDialogVisible" width="700px">
      <el-table :data="purchaseOrderOptions" v-loading="optionsLoading" border stripe @selection-change="handleSelectPurchase"
        max-height="400">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="orderNo" label="采购单号" width="160" />
        <el-table-column prop="orderDate" label="采购日期" width="120" />
        <el-table-column prop="supplierName" label="供应商" width="160" />
        <el-table-column prop="totalAmount" label="采购金额" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateInbound" :disabled="!selectedPurchaseRow" :loading="creating">确定入库</el-button>
      </template>
    </el-dialog>

    <el-dialog title="入库详情" v-model="detailDialogVisible" width="700px">
      <el-table :data="detailItems" v-loading="detailLoading" border stripe>
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" width="160" />
        <el-table-column prop="specification" label="规格" width="100" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitCost" label="成本单价" width="100" />
        <el-table-column prop="amount" label="金额" width="120" />
      </el-table>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInbounds, getInbound, confirmInbound, createInboundFromPurchase, cancelInbound } from '@/api/inventory'
import { getInboundOptions } from '@/api/purchase'
import { getWarehouseOptions } from '@/api/masterdata'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseOptions = ref<any[]>([])

const query = reactive({
  page: 1, size: 10,
  inboundNo: '', sourceNo: '', warehouseId: '', status: '', dateRange: [] as string[]
})

function inboundTypeLabel(type: string) {
  return ({ PURCHASE: '采购入库', SALES_RETURN: '销售退货入库' } as Record<string, string>)[type] || type
}

function statusType(status: string) {
  const map: Record<string, string> = { DRAFT: 'info', CONFIRMED: 'success', CANCELLED: 'danger' }
  return map[status] || ''
}
function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', CONFIRMED: '已确认', CANCELLED: '已取消' }
  return map[status] || status
}

onMounted(() => { fetchData(); loadWarehouses() })

async function loadWarehouses() {
  try {
    const res = await getWarehouseOptions()
    warehouseOptions.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0]
      params.endDate = params.dateRange[1]
    }
    delete params.dateRange
    const res = await getInbounds(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function handleConfirm(row: any) {
  await ElMessageBox.confirm('确定确认该入库单吗？确认后库存将增加。', '确认入库', { type: 'warning' })
  await confirmInbound(row.id)
  ElMessage.success('入库确认成功')
  fetchData()
}

async function handleCancel(row: any) {
  await ElMessageBox.confirm('确定取消该入库单吗？', '确认取消', { type: 'warning' })
  await cancelInbound(row.id)
  ElMessage.success('已取消')
  fetchData()
}

const createDialogVisible = ref(false)
const optionsLoading = ref(false)
const creating = ref(false)
const purchaseOrderOptions = ref<any[]>([])
const selectedPurchaseRow = ref<any>(null)

async function handleOpenCreate() {
  createDialogVisible.value = true
  optionsLoading.value = true
  selectedPurchaseRow.value = null
  try {
    const res = await getInboundOptions()
    purchaseOrderOptions.value = res.data || []
  } finally { optionsLoading.value = false }
}

function handleSelectPurchase(rows: any[]) {
  selectedPurchaseRow.value = rows.length > 0 ? rows[0] : null
}

async function handleCreateInbound() {
  if (!selectedPurchaseRow.value) return
  creating.value = true
  try {
    await createInboundFromPurchase(selectedPurchaseRow.value.id)
    ElMessage.success('入库单创建成功')
    createDialogVisible.value = false
    fetchData()
  } finally { creating.value = false }
}

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailItems = ref<any[]>([])

async function handleViewDetail(row: any) {
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const res = await getInbound(row.id)
    detailItems.value = res.data.items || []
  } finally { detailLoading.value = false }
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
</style>
