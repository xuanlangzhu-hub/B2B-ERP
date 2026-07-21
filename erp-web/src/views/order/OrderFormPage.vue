<template>
  <div class="document-page" v-loading="loading">
    <div class="document-header">
      <div><el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button><h1>{{ pageTitle }}</h1></div>
      <div class="header-actions"><el-button @click="router.push(basePath)">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">保存单据</el-button></div>
    </div>

    <el-card class="section-card">
      <template #header><div class="section-title"><span>基本信息</span><small>请填写单据基础资料</small></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="20">
          <el-col :span="6"><el-form-item :label="counterpartyLabel" prop="counterpartyId"><el-select v-model="form.counterpartyId" filterable style="width:100%" :placeholder="`选择${counterpartyLabel}`"><el-option v-for="item in counterparties" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="业务日期" prop="orderDate"><el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="出入库仓库" prop="warehouseId"><el-select v-model="form.warehouseId" style="width:100%" placeholder="选择仓库"><el-option v-for="item in warehouses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item :label="isSales ? '预计送达日期' : '预计到货日期'"><el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="运费"><el-input-number v-model="form.freightAmount" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col v-if="isSales" :span="18"><el-form-item label="送货地址"><el-input v-model="form.deliveryAddress" placeholder="客户收货地址" /></el-form-item></el-col>
          <el-col :span="isSales ? 24 : 18"><el-form-item label="备注"><el-input v-model="form.remark" placeholder="单据备注" /></el-form-item></el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="section-card detail-card">
      <template #header><div class="detail-head"><div class="section-title"><span>商品明细</span><small>添加本次{{ isSales ? '销售' : '采购' }}的商品</small></div><el-button type="primary" plain @click="addItem">+ 添加商品</el-button></div></template>
      <el-table :data="form.items" border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="商品" min-width="190">
          <template #default="{ row }"><el-select v-model="row.productId" filterable style="width:100%" @change="onProductChange(row)"><el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" /></el-select></template>
        </el-table-column>
        <el-table-column prop="productCode" label="编码" width="110" />
        <el-table-column prop="specification" label="规格" min-width="120" />
        <el-table-column label="数量" width="130"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0.0001" :precision="2" controls-position="right" style="width:100%" @change="calcRow(row)" /></template></el-table-column>
        <el-table-column label="单价" width="130"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" controls-position="right" style="width:100%" @change="calcRow(row)" /></template></el-table-column>
        <el-table-column label="折扣%" width="110"><template #default="{ row }"><el-input-number v-model="row.discountRate" :min="0" :max="100" :precision="1" controls-position="right" style="width:100%" @change="calcRow(row)" /></template></el-table-column>
        <el-table-column label="金额" width="120" align="right"><template #default="{ row }">￥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="备注" min-width="140"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
        <el-table-column label="操作" width="75" fixed="right"><template #default="{ $index }"><el-button link type="danger" @click="removeItem($index)">删除</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!form.items.length" description="暂无商品，请点击右上角添加" :image-size="70" />
      <div class="document-total"><span>商品种类：{{ form.items.length }}</span><span>总数量：{{ totalQuantity }}</span><strong>商品金额：￥{{ money(goodsAmount) }}</strong><strong>应付金额：￥{{ money(payableAmount) }}</strong></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCustomerOptions, getProductOptions, getSupplierOptions, getWarehouseOptions } from '@/api/masterdata'
import { createSalesOrder, getSalesOrder, updateSalesOrder } from '@/api/sales'
import { createPurchaseOrder, getPurchaseOrder, updatePurchaseOrder } from '@/api/purchase'
import { useUserStore } from '@/stores/user'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const type = computed(() => route.meta.businessType as 'sales' | 'purchase')
const isSales = computed(() => type.value === 'sales')
const editingId = computed(() => route.params.id ? Number(route.params.id) : null)
const basePath = computed(() => isSales.value ? '/sales/orders' : '/purchase/orders')
const pageTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${isSales.value ? '销售单' : '采购单'}`)
const counterpartyLabel = computed(() => isSales.value ? '客户' : '供应商')
const loading = ref(false); const submitting = ref(false); const formRef = ref()
const counterparties = ref<any[]>([]); const warehouses = ref<any[]>([]); const products = ref<any[]>([])
const form = reactive<any>({ counterpartyId: null, warehouseId: null, orderDate: today(), expectedDate: '', freightAmount: 0, deliveryAddress: '', remark: '', items: [] })
const rules = { counterpartyId: [{ required: true, message: '请选择往来单位', trigger: 'change' }], warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }], orderDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }] }
const goodsAmount = computed(() => form.items.reduce((sum: number, item: any) => sum + Number(item.amount || 0), 0))
const totalQuantity = computed(() => form.items.reduce((sum: number, item: any) => sum + Number(item.quantity || 0), 0).toFixed(2))
const payableAmount = computed(() => goodsAmount.value + Number(form.freightAmount || 0))

onMounted(async () => {
  loading.value = true
  try {
    const [partners, warehousesRes, productsRes] = await Promise.all([isSales.value ? getCustomerOptions() : getSupplierOptions(), getWarehouseOptions(), getProductOptions()])
    counterparties.value = partners.data || []; warehouses.value = warehousesRes.data || []; products.value = productsRes.data || []
    if (editingId.value) await loadDetail(editingId.value)
    else addItem()
  } finally { loading.value = false }
})
function today() { return new Date().toISOString().slice(0, 10) }
function addItem() { form.items.push({ productId: null, productCode: '', productName: '', specification: '', unitId: null, quantity: 1, unitPrice: 0, discountRate: 0, taxRate: 0, amount: 0, remark: '' }) }
function removeItem(index: number) { form.items.splice(index, 1) }
function onProductChange(row: any) { const product = products.value.find(item => item.value === row.productId); if (!product) return; Object.assign(row, { productCode: product.productCode, productName: product.productName, specification: product.specification, unitId: product.unitId, unitPrice: isSales.value ? Number(product.salePrice || 0) : Number(product.purchasePrice || 0) }); calcRow(row) }
function calcRow(row: any) { const raw = Number(row.quantity || 0) * Number(row.unitPrice || 0); row.amount = +(raw * (1 - Number(row.discountRate || 0) / 100)).toFixed(2) }
async function loadDetail(id: number) { const res = await (isSales.value ? getSalesOrder(id) : getPurchaseOrder(id)); const data = res.data; Object.assign(form, { counterpartyId: isSales.value ? data.customerId : data.supplierId, warehouseId: data.warehouseId, orderDate: data.orderDate, expectedDate: isSales.value ? data.expectedDeliveryDate : data.expectedArrivalDate, freightAmount: Number(data.freightAmount || 0), deliveryAddress: data.deliveryAddress || '', remark: data.remark || '', items: (data.items || []).map((item: any) => ({ ...item, discountRate: Number(item.discountRate || 0), taxRate: Number(item.taxRate || 0), amount: Number(item.amount || 0) })) }) }
async function submit() {
  if (!await formRef.value.validate().catch(() => false)) return
  if (!form.items.length || form.items.some((item: any) => !item.productId || Number(item.quantity) <= 0 || Number(item.unitPrice) < 0)) { ElMessage.warning('请完整填写至少一条商品明细'); return }
  const warehouse = warehouses.value.find(item => item.value === form.warehouseId)
  const storeId = warehouse?.storeId || userStore.userInfo?.defaultStoreId
  if (!storeId) { ElMessage.warning('所选仓库未关联门店'); return }
  const payload: any = { storeId, orderDate: form.orderDate, warehouseId: form.warehouseId, freightAmount: form.freightAmount, remark: form.remark, items: form.items.map((item: any, index: number) => ({ ...item, lineNo: index + 1 })) }
  if (isSales.value) Object.assign(payload, { customerId: form.counterpartyId, expectedDeliveryDate: form.expectedDate || null, deliveryAddress: form.deliveryAddress })
  else Object.assign(payload, { supplierId: form.counterpartyId, expectedArrivalDate: form.expectedDate || null })
  submitting.value = true
  try {
    if (editingId.value) await (isSales.value ? updateSalesOrder(editingId.value, payload) : updatePurchaseOrder(editingId.value, payload))
    else await (isSales.value ? createSalesOrder(payload) : createPurchaseOrder(payload))
    ElMessage.success('单据保存成功'); router.replace(basePath.value)
  } finally { submitting.value = false }
}
function money(value: any) { return Number(value || 0).toFixed(2) }
</script>

<style scoped>
.document-page { padding-bottom: 24px; }
.document-header { min-height: 66px; display: flex; align-items: center; justify-content: space-between; margin: -16px -20px 16px; padding: 0 22px; background: #fff; border-bottom: 1px solid #edf0f5; }
.document-header > div:first-child { display: flex; align-items: center; gap: 14px; }
.document-header h1 { margin: 0; font-size: 19px; font-weight: 600; }
.header-actions { display: flex; gap: 8px; }
.section-card + .section-card { margin-top: 14px; }
.section-title { display: flex; align-items: baseline; gap: 12px; }
.section-title span { font-size: 16px; font-weight: 600; }
.section-title small { color: #a1a8b3; }
.detail-head { display: flex; align-items: center; justify-content: space-between; }
.document-total { display: flex; justify-content: flex-end; align-items: center; gap: 26px; margin-top: 22px; color: #697386; }
.document-total strong { color: #1f2d3d; font-size: 16px; }
.document-total strong:last-child { color: #e66b2f; font-size: 20px; }
</style>
