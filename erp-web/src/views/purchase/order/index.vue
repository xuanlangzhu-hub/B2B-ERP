<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.orderNo" placeholder="单号" clearable style="width:160px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:130px">
          <el-option label="草稿" value="DRAFT" /><el-option label="已审核" value="APPROVED" />
          <el-option label="已完成" value="COMPLETED" /><el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="handleAdd">新增</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="单号" width="150" />
        <el-table-column prop="orderDate" label="日期" width="110" />
        <el-table-column prop="supplierName" label="供应商" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="totalAmount" label="总金额" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleDetail(row)">查看</el-button>
            <el-button v-if="row.status==='DRAFT'" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status==='DRAFT'" size="small" type="success" @click="handleApprove(row)">审核</el-button>
            <el-button v-if="row.status==='DRAFT'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status==='APPROVED'" size="small" type="warning" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="800px" @close="resetForm">
      <el-form :model="form" ref="formRef" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="供应商"><el-select v-model="form.supplierId" placeholder="选择供应商" filterable style="width:100%"><el-option v-for="c in suppliers" :key="c.value" :label="c.label" :value="c.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="仓库"><el-select v-model="form.warehouseId" placeholder="选择仓库" style="width:100%"><el-option v-for="w in warehouses" :key="w.value" :label="w.label" :value="w.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务日期"><el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-divider content-position="left">商品明细</el-divider>
        <el-table :data="form.items" border>
          <el-table-column label="商品" width="160">
            <template #default="{ row }"><el-select v-model="row.productId" filterable @change="onProductChange(row)"><el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" /></el-select></template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" controls-position="right" size="small" @change="calcRowAmount(row)" /></template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" controls-position="right" size="small" @change="calcRowAmount(row)" /></template>
          </el-table-column>
          <el-table-column label="金额" width="120"><template #default="{ row }">{{ row.amount }}</template></el-table-column>
          <el-table-column label="操作" width="80"><template #default="{ $index }"><el-button size="small" type="danger" @click="form.items.splice($index,1)">删除</el-button></template></el-table-column>
        </el-table>
        <el-button style="margin-top:8px" @click="addItem">+ 添加商品</el-button>
        <el-divider />
        <div style="text-align:right;font-size:16px">总金额: <b style="color:#409EFF">￥{{ totalAmount.toFixed(2) }}</b></div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="采购单详情" v-model="detailVisible" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items" border style="margin-top:12px">
        <el-table-column prop="productName" label="商品" /><el-table-column prop="quantity" label="数量" />
        <el-table-column prop="unitPrice" label="单价" /><el-table-column prop="amount" label="金额" />
      </el-table>
      <div style="text-align:right;font-size:16px;margin-top:8px">总金额: <b>￥{{ detail.totalAmount }}</b></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPurchaseOrders, getPurchaseOrder, createPurchaseOrder, updatePurchaseOrder, deletePurchaseOrder, approvePurchaseOrder, cancelPurchaseOrder } from '@/api/purchase'
import { getSupplierOptions } from '@/api/masterdata'
import { getWarehouseOptions } from '@/api/masterdata'
import { getProductOptions } from '@/api/masterdata'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false); const submitting = ref(false)
const tableData = ref<any[]>([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, orderNo: '', status: '' })
const dateRange = ref<any[]>([])

const dialogVisible = ref(false); const dialogTitle = ref('新增采购单'); const editingId = ref<number|null>(null)
const formRef = ref(); const form = reactive({ supplierId: null, warehouseId: null, orderDate: '', remark: '', items: [] as any[] } as any)
const suppliers = ref<any[]>([]); const warehouses = ref<any[]>([]); const products = ref<any[]>([])

const detailVisible = ref(false); const detail = ref<any>({})

const totalAmount = computed(() => form.items.reduce((s:number, i:any) => s + (i.amount || 0), 0))

onMounted(async () => { fetchData(); await loadOptions() })

async function loadOptions() {
  const [s, w, p] = await Promise.all([getSupplierOptions(), getWarehouseOptions(), getProductOptions()])
  suppliers.value = s.data || []; warehouses.value = w.data || []
  products.value = p.data || []
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) { params.startDate = dateRange.value[0]; params.endDate = dateRange.value[1] }
    const res = await getPurchaseOrders(params)
    tableData.value = res.data.records; total.value = res.data.total
  } finally { loading.value = false }
}

function statusType(s: string) { return { DRAFT:'info', APPROVED:'warning', COMPLETED:'success', CANCELLED:'danger' }[s] || 'info' }
function statusLabel(s: string) { return { DRAFT:'草稿', APPROVED:'已审核', COMPLETED:'已完成', CANCELLED:'已取消' }[s] || s }

function handleAdd() { editingId.value = null; dialogTitle.value = '新增采购单'; resetForm(); dialogVisible.value = true }
function addItem() { form.items.push({ productId: null, quantity: 0, unitPrice: 0, amount: 0, unitId: null }) }
function onProductChange(row: any) { const p = products.value.find(x => x.value === row.productId); if (p) { row.productCode = p.productCode; row.productName = p.productName; row.unitPrice = p.purchasePrice || 0; row.unitId = p.unitId; calcRowAmount(row) } }
function calcRowAmount(row: any) { row.amount = +(row.quantity * row.unitPrice).toFixed(2) }

async function handleSubmit() {
  if (!form.supplierId || !form.warehouseId || !form.orderDate) { ElMessage.warning('请填写供应商、仓库和业务日期'); return }
  if (form.items.length === 0) { ElMessage.warning('请添加商品明细'); return }
  if (form.items.some((item: any) => !item.productId || !item.quantity || item.quantity <= 0 || item.unitPrice == null || item.unitPrice < 0)) {
    ElMessage.warning('请完整填写商品、数量和单价'); return
  }
  const warehouse = warehouses.value.find((item: any) => item.value === form.warehouseId)
  const storeId = warehouse?.storeId || userStore.userInfo?.defaultStoreId
  if (!storeId) { ElMessage.warning('请先为仓库关联门店或设置用户默认门店'); return }
  submitting.value = true
  try {
    const payload = {
      ...form,
      storeId,
      totalAmount: totalAmount.value,
      items: form.items.map((item: any, index: number) => ({ ...item, lineNo: index + 1 }))
    }
    if (editingId.value) { await updatePurchaseOrder(editingId.value, payload) }
    else { await createPurchaseOrder(payload) }
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

async function handleEdit(row: any) {
  const res = await getPurchaseOrder(row.id)
  const d = res.data; editingId.value = row.id; dialogTitle.value = '编辑采购单'
  Object.assign(form, { supplierId: d.supplierId, warehouseId: d.warehouseId, orderDate: d.orderDate, remark: d.remark, items: d.items || [] })
  dialogVisible.value = true
}

async function handleDetail(row: any) { const r = await getPurchaseOrder(row.id); detail.value = r.data; detailVisible.value = true }

async function handleApprove(row: any) { await ElMessageBox.confirm('确认审核该采购单？'); await approvePurchaseOrder(row.id); ElMessage.success('已审核'); fetchData() }
async function handleCancel(row: any) { await ElMessageBox.confirm('确认取消该采购单？'); await cancelPurchaseOrder(row.id); ElMessage.success('已取消'); fetchData() }
async function handleDelete(row: any) { await ElMessageBox.confirm('确认删除？'); await deletePurchaseOrder(row.id); ElMessage.success('已删除'); fetchData() }

function resetForm() { Object.assign(form, { supplierId: null, warehouseId: null, orderDate: new Date().toISOString().slice(0,10), remark: '', items: [] }) }
</script>

<style scoped>.page-wrapper{padding:0}.toolbar{display:flex;gap:10px;flex-wrap:wrap}</style>
