<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:180px">
          <el-option v-for="w in warehouseOptions" :key="w.value" :label="w.label" :value="w.value" />
        </el-select>
        <el-input v-model="query.productId" placeholder="商品ID" clearable style="width:140px" />
        <el-select v-model="query.movementType" placeholder="变动类型" clearable style="width:180px">
          <el-option label="采购入库" value="PURCHASE_IN" />
          <el-option label="销售出库" value="SALES_OUT" />
          <el-option label="采购退货" value="PURCHASE_RETURN" />
          <el-option label="销售退货" value="SALES_RETURN" />
          <el-option label="盘点入库" value="CHECK_IN" />
          <el-option label="盘点出库" value="CHECK_OUT" />
          <el-option label="调拨入库" value="TRANSFER_IN" />
          <el-option label="调拨出库" value="TRANSFER_OUT" />
        </el-select>
        <el-input v-model="query.sourceNo" placeholder="来源单号" clearable style="width:180px" />
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="movementNo" label="流水号" width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="productName" label="商品" width="160" />
        <el-table-column prop="movementType" label="变动类型" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ row.movementType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'" size="small">
              {{ row.direction === 'IN' ? '入库' : '出库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="beforeQuantity" label="变动前" width="100" />
        <el-table-column prop="afterQuantity" label="变动后" width="100" />
        <el-table-column prop="sourceNo" label="来源单号" width="160" />
        <el-table-column prop="businessDate" label="业务日期" width="120" />
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getStockMovements } from '@/api/inventory'
import { getWarehouseOptions } from '@/api/masterdata'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseOptions = ref<any[]>([])

const query = reactive({
  page: 1, size: 10,
  warehouseId: '', productId: '', movementType: '', sourceNo: '', dateRange: [] as string[]
})

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
    const res = await getStockMovements(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
</style>
