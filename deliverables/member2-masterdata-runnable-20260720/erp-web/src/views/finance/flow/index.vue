<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-select v-model="query.flowType" placeholder="流水类型" clearable style="width:160px">
          <el-option label="收款" value="RECEIPT" />
          <el-option label="付款" value="PAYMENT" />
          <el-option label="退款" value="REFUND" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="flowNo" label="流水号" width="160" />
        <el-table-column prop="flowDate" label="日期" width="110" />
        <el-table-column prop="flowType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.flowType === 'RECEIPT' ? 'success' : row.flowType === 'PAYMENT' ? 'danger' : 'warning'" size="small">
              {{ row.flowType === 'RECEIPT' ? '收款' : row.flowType === 'PAYMENT' ? '付款' : '退款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'" size="small">
              {{ row.direction === 'IN' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="beforeBalance" label="变动前余额" width="120" />
        <el-table-column prop="afterBalance" label="变动后余额" width="120" />
        <el-table-column prop="counterpartyName" label="对方名称" width="160" />
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getCapitalFlows } from '@/api/finance'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<any[]>([])

const query = reactive({
  page: 1, size: 10,
  flowType: ''
})

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getCapitalFlows(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
