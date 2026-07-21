<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"
          start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-select v-model="query.customerId" placeholder="客户" clearable filterable style="width:180px">
          <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-tab-pane label="销售明细" name="DETAIL" />
        <el-tab-pane label="按商品汇总" name="PRODUCT" />
        <el-tab-pane label="按客户汇总" name="CUSTOMER" />
        <el-tab-pane label="按分类汇总" name="CATEGORY" />
        <el-tab-pane label="销售利润表" name="PROFIT" />
      </el-tabs>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <template v-if="activeType === 'DETAIL'">
          <el-table-column prop="orderNo" label="销售单号" width="150" fixed="left" />
          <el-table-column prop="orderDate" label="销售日期" width="105" />
          <el-table-column prop="customerName" label="客户" min-width="140" />
          <el-table-column prop="productCode" label="商品编码" width="120" />
          <el-table-column prop="productName" label="商品名称" min-width="150" />
          <el-table-column prop="categoryName" label="商品分类" width="110" />
        </template>
        <template v-else-if="activeType === 'CUSTOMER'">
          <el-table-column prop="customerCode" label="客户编码" width="130" fixed="left" />
          <el-table-column prop="customerName" label="客户名称" min-width="180" fixed="left" />
          <el-table-column prop="orderCount" label="销售单数" width="100" align="right" />
        </template>
        <template v-else-if="activeType === 'CATEGORY'">
          <el-table-column prop="categoryName" label="商品分类" min-width="180" fixed="left" />
          <el-table-column prop="orderCount" label="销售单数" width="100" align="right" />
        </template>
        <template v-else>
          <el-table-column prop="productCode" label="商品编码" width="130" fixed="left" />
          <el-table-column prop="productName" label="商品名称" min-width="170" fixed="left" />
          <el-table-column prop="categoryName" label="商品分类" width="120" />
          <el-table-column prop="orderCount" label="销售单数" width="100" align="right" />
        </template>

        <el-table-column prop="grossQuantity" label="销售数量" width="110" align="right">
          <template #default="{ row }">{{ quantity(row.grossQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="returnQuantity" label="退货数量" width="110" align="right">
          <template #default="{ row }">{{ quantity(row.returnQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="netQuantity" label="净销售数量" width="120" align="right">
          <template #default="{ row }">{{ quantity(row.netQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="salesAmount" label="销售金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.salesAmount) }}</template>
        </el-table-column>
        <el-table-column prop="returnAmount" label="退货金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.returnAmount) }}</template>
        </el-table-column>
        <el-table-column prop="netSalesAmount" label="净销售额" width="120" align="right">
          <template #default="{ row }">{{ money(row.netSalesAmount) }}</template>
        </el-table-column>
        <el-table-column prop="costAmount" label="销售成本" width="120" align="right">
          <template #default="{ row }">{{ money(row.costAmount) }}</template>
        </el-table-column>
        <el-table-column prop="profitAmount" label="毛利润" width="120" align="right">
          <template #default="{ row }"><span :class="{ negative: Number(row.profitAmount) < 0 }">{{ money(row.profitAmount) }}</span></template>
        </el-table-column>
        <el-table-column prop="profitRate" label="毛利率" width="100" align="right">
          <template #default="{ row }">{{ Number(row.profitRate || 0).toFixed(2) }}%</template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getSalesReport, getSalesReportSummary } from '@/api/report'
import { getCustomerOptions, getProductOptions, getCategoryOptions } from '@/api/masterdata'

const loading = ref(false)
const activeType = ref('DETAIL')
const dateRange = ref<string[]>([])
const tableData = ref<any[]>([])
const total = ref(0)
const customerOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const categoryOptions = ref<any[]>([])
const query = reactive({ page: 1, size: 10, customerId: '', productId: '', categoryId: '' })
const summary = reactive<any>({ orderCount: 0, totalQuantity: 0, netSalesAmount: 0, returnAmount: 0, costAmount: 0, profitAmount: 0, profitRate: 0 })

const summaryCards = computed(() => [
  { label: '完成销售单', value: summary.orderCount || 0 },
  { label: '净销售数量', value: quantity(summary.totalQuantity) },
  { label: '净销售额', value: money(summary.netSalesAmount) },
  { label: '退货金额', value: money(summary.returnAmount) },
  { label: '毛利润', value: money(summary.profitAmount) },
  { label: '毛利率', value: `${Number(summary.profitRate || 0).toFixed(2)}%` }
])

onMounted(async () => {
  await Promise.all([loadOptions(), fetchData()])
})

function filters() {
  return {
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined,
    customerId: query.customerId || undefined,
    productId: query.productId || undefined,
    categoryId: query.categoryId || undefined
  }
}

async function loadOptions() {
  const [customers, products, categories] = await Promise.all([getCustomerOptions(), getProductOptions(), getCategoryOptions()])
  customerOptions.value = customers.data || []
  productOptions.value = products.data || []
  categoryOptions.value = categories.data || []
}

async function fetchData() {
  loading.value = true
  try {
    const params = filters()
    const [list, totalSummary] = await Promise.all([
      getSalesReport({ ...params, type: activeType.value, page: query.page, size: query.size }),
      getSalesReportSummary(params)
    ])
    tableData.value = list.data?.records || []
    total.value = list.data?.total || 0
    Object.assign(summary, totalSummary.data || {})
  } finally { loading.value = false }
}

async function fetchList() {
  loading.value = true
  try {
    const list = await getSalesReport({ ...filters(), type: activeType.value, page: query.page, size: query.size })
    tableData.value = list.data?.records || []
    total.value = list.data?.total || 0
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function changeType() { query.page = 1; fetchList() }
function resetQuery() {
  query.page = 1; query.customerId = ''; query.productId = ''; query.categoryId = ''; dateRange.value = []
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
.negative { color: #f56c6c; }
</style>
