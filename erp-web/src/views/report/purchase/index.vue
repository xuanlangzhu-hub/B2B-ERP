<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"
          start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-select v-model="query.supplierId" placeholder="供应商" clearable filterable style="width:180px">
          <el-option v-for="item in supplierOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.productId" placeholder="商品" clearable filterable style="width:180px">
          <el-option v-for="item in productOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable style="width:160px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-row :gutter="12" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="12" :sm="8" :lg="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-tabs v-model="activeType" @tab-change="changeType">
        <el-tab-pane label="采购明细" name="DETAIL" />
        <el-tab-pane label="按商品汇总" name="PRODUCT" />
        <el-tab-pane label="按供应商汇总" name="SUPPLIER" />
        <el-tab-pane label="按分类汇总" name="CATEGORY" />
      </el-tabs>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <template v-if="activeType === 'DETAIL'">
          <el-table-column prop="orderNo" label="采购单号" width="150" fixed="left" />
          <el-table-column prop="orderDate" label="采购日期" width="105" />
          <el-table-column prop="supplierName" label="供应商" min-width="150" />
          <el-table-column prop="productCode" label="商品编码" width="120" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="categoryName" label="商品分类" width="110" />
        </template>
        <template v-else-if="activeType === 'SUPPLIER'">
          <el-table-column prop="supplierCode" label="供应商编码" width="140" fixed="left" />
          <el-table-column prop="supplierName" label="供应商名称" min-width="190" fixed="left" />
          <el-table-column prop="orderCount" label="采购单数" width="100" align="right" />
        </template>
        <template v-else-if="activeType === 'CATEGORY'">
          <el-table-column prop="categoryName" label="商品分类" min-width="190" fixed="left" />
          <el-table-column prop="orderCount" label="采购单数" width="100" align="right" />
        </template>
        <template v-else>
          <el-table-column prop="productCode" label="商品编码" width="130" fixed="left" />
          <el-table-column prop="productName" label="商品名称" min-width="180" fixed="left" />
          <el-table-column prop="categoryName" label="商品分类" width="120" />
          <el-table-column prop="orderCount" label="采购单数" width="100" align="right" />
        </template>

        <el-table-column prop="grossQuantity" label="采购数量" width="110" align="right">
          <template #default="{ row }">{{ quantity(row.grossQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="returnQuantity" label="退货数量" width="110" align="right">
          <template #default="{ row }">{{ quantity(row.returnQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="netQuantity" label="净采购数量" width="120" align="right">
          <template #default="{ row }">{{ quantity(row.netQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="purchaseAmount" label="采购金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.purchaseAmount) }}</template>
        </el-table-column>
        <el-table-column prop="returnAmount" label="退货金额" width="130" align="right">
          <template #default="{ row }">{{ money(row.returnAmount) }}</template>
        </el-table-column>
        <el-table-column prop="netPurchaseAmount" label="净采购额" width="130" align="right">
          <template #default="{ row }">{{ money(row.netPurchaseAmount) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getPurchaseReport, getPurchaseReportSummary } from '@/api/report'
import { getSupplierOptions, getProductOptions, getCategoryOptions } from '@/api/masterdata'

const loading = ref(false)
const activeType = ref('DETAIL')
const dateRange = ref<string[]>([])
const tableData = ref<any[]>([])
const total = ref(0)
const supplierOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const categoryOptions = ref<any[]>([])
const query = reactive({ page: 1, size: 10, supplierId: '', productId: '', categoryId: '' })
const summary = reactive<any>({ orderCount: 0, supplierCount: 0, productCount: 0, totalQuantity: 0, grossPurchaseAmount: 0, returnAmount: 0, netPurchaseAmount: 0 })

const summaryCards = computed(() => [
  { label: '完成采购单', value: summary.orderCount || 0 },
  { label: '净采购数量', value: quantity(summary.totalQuantity) },
  { label: '采购总额', value: money(summary.grossPurchaseAmount) },
  { label: '退货金额', value: money(summary.returnAmount) },
  { label: '净采购额', value: money(summary.netPurchaseAmount) },
  { label: '涉及供应商', value: summary.supplierCount || 0 }
])

onMounted(async () => { await Promise.all([loadOptions(), fetchData()]) })

function filters() {
  return {
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined,
    supplierId: query.supplierId || undefined,
    productId: query.productId || undefined,
    categoryId: query.categoryId || undefined
  }
}

async function loadOptions() {
  const [suppliers, products, categories] = await Promise.all([getSupplierOptions(), getProductOptions(), getCategoryOptions()])
  supplierOptions.value = suppliers.data || []
  productOptions.value = products.data || []
  categoryOptions.value = categories.data || []
}

async function fetchData() {
  loading.value = true
  try {
    const params = filters()
    const [list, totalSummary] = await Promise.all([
      getPurchaseReport({ ...params, type: activeType.value, page: query.page, size: query.size }),
      getPurchaseReportSummary(params)
    ])
    tableData.value = list.data?.records || []
    total.value = list.data?.total || 0
    Object.assign(summary, totalSummary.data || {})
  } finally { loading.value = false }
}

async function fetchList() {
  loading.value = true
  try {
    const list = await getPurchaseReport({ ...filters(), type: activeType.value, page: query.page, size: query.size })
    tableData.value = list.data?.records || []
    total.value = list.data?.total || 0
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function changeType() { query.page = 1; fetchList() }
function resetQuery() {
  query.page = 1; query.supplierId = ''; query.productId = ''; query.categoryId = ''; dateRange.value = []
  fetchData()
}
function money(value: any) { return `¥${Number(value || 0).toFixed(2)}` }
function quantity(value: any) { return Number(value || 0).toFixed(2) }
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; }
.summary-row { margin-top: 12px; }
.summary-card { margin-bottom: 12px; text-align: center; }
.summary-label { color: #909399; font-size: 13px; }
.summary-value { margin-top: 8px; color: #303133; font-size: 21px; font-weight: 700; }
.pagination { justify-content: flex-end; margin-top: 12px; }
</style>
