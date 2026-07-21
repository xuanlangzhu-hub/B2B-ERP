<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:180px">
          <el-option v-for="w in warehouseOptions" :key="w.value" :label="w.label" :value="w.value" />
        </el-select>
        <el-input v-model="query.productCode" placeholder="商品编码" clearable style="width:180px" />
        <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:180px" />
        <el-checkbox v-model="query.lowStock" border>仅显示低库存</el-checkbox>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" width="160" />
        <el-table-column prop="specification" label="规格" width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="quantity" label="库存数量" width="100" />
        <el-table-column prop="lockedQuantity" label="锁定数量" width="100" />
        <el-table-column prop="availableQuantity" label="可用数量" width="100" />
        <el-table-column prop="minStock" label="最低库存" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.availableQuantity <= row.minStock ? 'danger' : 'success'" size="small">
              {{ row.availableQuantity <= row.minStock ? '低库存' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getStocks } from '@/api/inventory'
import { getWarehouseOptions } from '@/api/masterdata'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const warehouseOptions = ref<any[]>([])

const query = reactive({
  page: 1, size: 10,
  warehouseId: '', productCode: '', productName: '', lowStock: false
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
    const res = await getStocks(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
</style>
