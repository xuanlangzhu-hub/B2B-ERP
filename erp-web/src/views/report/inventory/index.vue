<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width: 180px">
          <el-option v-for="warehouse in warehouseOptions" :key="warehouse.value" :label="warehouse.label" :value="warehouse.value" />
        </el-select>
        <el-input v-model="query.productCode" placeholder="商品编码" clearable style="width: 180px" />
        <el-input v-model="query.productName" placeholder="商品名称" clearable style="width: 180px" />
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
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="productCode" label="商品编码" min-width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="specification" label="规格" min-width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="quantity" label="库存数量" width="110" align="right" />
        <el-table-column prop="availableQuantity" label="可用数量" width="110" align="right" />
        <el-table-column prop="avgCostPrice" label="平均成本价" width="120" align="right" />
        <el-table-column prop="stockAmount" label="库存金额" width="120" align="right" />
        <el-table-column label="库存状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.lowStock" type="danger">库存预警</el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        @change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getInventoryReport, getInventoryReportSummary } from '@/api/report'
import { getWarehouseOptions } from '@/api/masterdata'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseOptions = ref<any[]>([])
const summary = reactive<any>({
  totalQuantity: 0,
  totalStockAmount: 0,
  lowStockCount: 0,
  warehouseCount: 0,
  productCount: 0
})
const query = reactive({
  page: 1,
  size: 10,
  warehouseId: '',
  productCode: '',
  productName: ''
})

const summaryCards = computed(() => [
  { label: '库存总数量', value: Number(summary.totalQuantity ?? 0).toFixed(2) },
  { label: '库存总金额', value: `¥${Number(summary.totalStockAmount ?? 0).toFixed(2)}` },
  { label: '低库存项', value: summary.lowStockCount ?? 0 },
  { label: '涉及仓库', value: summary.warehouseCount ?? 0 },
  { label: '涉及商品', value: summary.productCount ?? 0 }
])

onMounted(() => {
  fetchData()
  loadWarehouses()
})

async function loadWarehouses() {
  try {
    const response = await getWarehouseOptions()
    warehouseOptions.value = response.data || []
  } catch {
    warehouseOptions.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const filters = {
      warehouseId: query.warehouseId || undefined,
      productCode: query.productCode || undefined,
      productName: query.productName || undefined
    }
    const [listResponse, summaryResponse] = await Promise.all([
      getInventoryReport({ ...filters, page: query.page, size: query.size }),
      getInventoryReportSummary(filters)
    ])
    tableData.value = listResponse.data?.records || []
    total.value = listResponse.data?.total || 0
    Object.assign(summary, summaryResponse.data || {})
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchData()
}

function resetQuery() {
  query.page = 1
  query.warehouseId = ''
  query.productCode = ''
  query.productName = ''
  fetchData()
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; }
.summary-row { margin: 12px 0 0; }
.summary-card { margin-bottom: 12px; text-align: center; }
.summary-label { color: #909399; font-size: 13px; }
.summary-value { margin-top: 8px; color: #303133; font-size: 22px; font-weight: 700; }
.pagination { justify-content: flex-end; margin-top: 12px; }
</style>
