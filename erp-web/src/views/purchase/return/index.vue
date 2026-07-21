<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.returnNo" placeholder="退货单号" clearable style="width: 180px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已审核" value="APPROVED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="openCreate">新增退货申请</el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="returnNo" label="退货单号" width="165" />
        <el-table-column prop="returnDate" label="退货日期" width="110" />
        <el-table-column prop="purchaseOrderNo" label="原采购单" width="165" />
        <el-table-column prop="supplierName" label="供应商" min-width="150" />
        <el-table-column prop="warehouseName" label="退货仓库" min-width="130" />
        <el-table-column prop="totalQuantity" label="退货数量" width="100" />
        <el-table-column prop="totalAmount" label="退货金额" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="approve(row)">审核</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="danger" @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status === 'APPROVED'" size="small" type="primary" @click="generateOutbound(row)">生成退货出库</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        background
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @change="fetchData"
      />
    </el-card>

    <el-dialog v-model="createVisible" title="新增采购退货申请" width="920px" @closed="resetForm">
      <el-alert
        title="当前版本支持冲减未付款的退货；若原采购单已全部付款，需要供应商退款，系统会阻止提交。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="9">
            <el-form-item label="原采购单" required>
              <el-select v-model="form.purchaseOrderId" filterable style="width: 100%" placeholder="选择已完成采购单" @change="loadOrderDetail">
                <el-option v-for="order in orderOptions" :key="order.id" :label="`${order.orderNo} / ${order.supplierName || ''}`" :value="order.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="退货日期" required>
              <el-date-picker v-model="form.returnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货仓库" required>
              <el-select v-model="form.warehouseId" style="width: 100%" placeholder="选择仓库">
                <el-option v-for="warehouse in warehouses" :key="warehouse.value" :label="warehouse.label" :value="warehouse.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="退货原因"><el-input v-model="form.returnReason" maxlength="200" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" maxlength="500" /></el-form-item>

        <el-divider content-position="left">选择退货商品</el-divider>
        <el-table :data="form.items" border v-loading="orderLoading">
          <el-table-column prop="productCode" label="商品编码" width="130" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="specification" label="规格" width="110" />
          <el-table-column prop="unitPrice" label="采购单价" width="100" />
          <el-table-column prop="inboundQuantity" label="已入库" width="90" />
          <el-table-column prop="returnedQuantity" label="已退" width="80" />
          <el-table-column prop="maxReturnQuantity" label="可退" width="80" />
          <el-table-column label="本次退货" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.returnQuantity" :min="0" :max="row.maxReturnQuantity" :precision="3" :step="1" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="退货金额" width="110">
            <template #default="{ row }">{{ lineAmount(row) }}</template>
          </el-table-column>
        </el-table>
        <div class="summary">合计数量：{{ totalQuantity }}　合计金额：￥{{ totalAmount }}</div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采购退货详情" width="820px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="退货单号">{{ detail.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="原采购单">{{ detail.purchaseOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="退货仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="退货日期">{{ detail.returnDate }}</el-descriptions-item>
        <el-descriptions-item label="退货原因" :span="2">{{ detail.returnReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items || []" border style="margin-top: 12px">
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="quantity" label="退货数量" width="100" />
        <el-table-column prop="outboundQuantity" label="已出库" width="90" />
        <el-table-column prop="unitPrice" label="单价" width="90" />
        <el-table-column prop="amount" label="金额" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approvePurchaseReturn,
  cancelPurchaseReturn,
  createPurchaseReturn,
  getPurchaseOrder,
  getPurchaseOrders,
  getPurchaseReturn,
  getPurchaseReturns
} from '@/api/purchase'
import { createOutboundFromPurchaseReturn } from '@/api/inventory'
import { getWarehouseOptions } from '@/api/masterdata'

const loading = ref(false)
const submitting = ref(false)
const orderLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const orderOptions = ref<any[]>([])
const warehouses = ref<any[]>([])
const createVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<any>({})
const query = reactive({ page: 1, size: 10, returnNo: '', status: '' })
const form = reactive<any>({ purchaseOrderId: null, returnDate: today(), warehouseId: null, returnReason: '', remark: '', items: [] })

const totalQuantity = computed(() => form.items.reduce((sum: number, item: any) => sum + Number(item.returnQuantity || 0), 0).toFixed(3))
const totalAmount = computed(() => form.items.reduce((sum: number, item: any) => sum + Number(lineAmount(item)), 0).toFixed(2))

onMounted(() => fetchData())

function today() { return new Date().toLocaleDateString('en-CA') }
function statusType(status: string) { return ({ DRAFT: 'info', APPROVED: 'warning', COMPLETED: 'success', CANCELLED: 'danger' } as any)[status] || 'info' }
function statusLabel(status: string) { return ({ DRAFT: '草稿', APPROVED: '已审核', COMPLETED: '已完成', CANCELLED: '已取消' } as any)[status] || status }
function lineAmount(item: any) { return (Number(item.returnQuantity || 0) * Number(item.unitPrice || 0)).toFixed(2) }

async function fetchData() {
  loading.value = true
  try {
    const res = await getPurchaseReturns(query)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

async function openCreate() {
  resetForm()
  createVisible.value = true
  try {
    const [orders, warehouseResult] = await Promise.all([
      getPurchaseOrders({ page: 1, size: 100, status: 'COMPLETED' }),
      getWarehouseOptions()
    ])
    orderOptions.value = orders.data.records || []
    warehouses.value = warehouseResult.data || []
  } catch {
    createVisible.value = false
  }
}

async function loadOrderDetail(orderId: number) {
  if (!orderId) return
  orderLoading.value = true
  try {
    const res = await getPurchaseOrder(orderId)
    const order = res.data
    form.warehouseId = order.warehouseId
    form.items = (order.items || []).map((item: any) => ({
      ...item,
      purchaseOrderItemId: item.id,
      maxReturnQuantity: Math.max(0, Number(item.inboundQuantity || 0) - Number(item.returnedQuantity || 0)),
      returnQuantity: 0
    })).filter((item: any) => item.maxReturnQuantity > 0)
    if (form.items.length === 0) ElMessage.warning('该采购单没有可退商品')
  } finally { orderLoading.value = false }
}

async function submit() {
  const selected = form.items.filter((item: any) => Number(item.returnQuantity) > 0)
  if (!form.purchaseOrderId || !form.returnDate || !form.warehouseId) {
    ElMessage.warning('请选择原采购单、退货日期和退货仓库')
    return
  }
  if (selected.length === 0) {
    ElMessage.warning('请填写至少一项退货数量')
    return
  }
  submitting.value = true
  try {
    await createPurchaseReturn({
      purchaseOrderId: form.purchaseOrderId,
      returnDate: form.returnDate,
      warehouseId: form.warehouseId,
      returnReason: form.returnReason,
      remark: form.remark,
      items: selected.map((item: any) => ({ purchaseOrderItemId: item.purchaseOrderItemId, quantity: item.returnQuantity }))
    })
    ElMessage.success('采购退货申请已保存')
    createVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

async function openDetail(row: any) {
  const res = await getPurchaseReturn(row.id)
  detail.value = res.data
  detailVisible.value = true
}

async function approve(row: any) {
  await ElMessageBox.confirm('审核后即可生成退货出库单，确定审核吗？', '审核退货申请', { type: 'warning' })
  await approvePurchaseReturn(row.id)
  ElMessage.success('审核成功')
  fetchData()
}

async function cancel(row: any) {
  await ElMessageBox.confirm('确定取消这张退货申请吗？', '取消退货申请', { type: 'warning' })
  await cancelPurchaseReturn(row.id)
  ElMessage.success('已取消')
  fetchData()
}

async function generateOutbound(row: any) {
  await createOutboundFromPurchaseReturn(row.id)
  ElMessage.success('退货出库单已生成，请到“出库管理”确认出库')
  fetchData()
}

function resetForm() {
  Object.assign(form, { purchaseOrderId: null, returnDate: today(), warehouseId: null, returnReason: '', remark: '', items: [] })
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
.summary { margin-top: 12px; text-align: right; color: #409eff; font-size: 16px; font-weight: 600; }
</style>
