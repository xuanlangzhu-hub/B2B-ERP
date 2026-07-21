<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.countNo" placeholder="盘点单号" clearable style="width:180px" />
        <el-select v-model="query.warehouseId" placeholder="盘点仓库" clearable style="width:180px">
          <el-option v-for="item in warehouses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="router.push('/inventory/counts/new')">新增盘点</el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="countNo" label="盘点单号" width="180" />
        <el-table-column prop="countDate" label="盘点日期" width="120" />
        <el-table-column prop="warehouseName" label="盘点仓库" min-width="150" />
        <el-table-column prop="totalBookQuantity" label="账面总数" width="110" align="right" />
        <el-table-column prop="totalActualQuantity" label="实盘总数" width="110" align="right" />
        <el-table-column label="差异总数" width="110" align="right"><template #default="{ row }"><span :class="diffClass(row.totalDiffQuantity)">{{ formatSigned(row.totalDiffQuantity) }}</span></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/inventory/counts/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="primary" @click="start(row)">开始盘点</el-button>
            <el-button v-if="row.status === 'COUNTING'" size="small" type="primary" @click="router.push(`/inventory/counts/${row.id}/edit`)">录入实盘</el-button>
            <el-button v-if="row.status === 'APPROVED'" size="small" type="success" @click="approve(row)">审核入账</el-button>
            <el-button v-if="!['COMPLETED','CANCELLED'].includes(row.status)" size="small" type="danger" @click="cancel(row)">取消</el-button>
            <el-button v-if="['DRAFT','CANCELLED'].includes(row.status)" size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveInventoryCount, cancelInventoryCount, deleteInventoryCount, getInventoryCounts, startInventoryCount } from '@/api/inventory'
import { getWarehouseOptions } from '@/api/masterdata'

const router = useRouter(); const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const warehouses = ref<any[]>([])
const query = reactive({ page: 1, size: 10, countNo: '', warehouseId: '', status: '', dateRange: [] as string[] })
const statuses = [{ label: '草稿', value: 'DRAFT' }, { label: '盘点中', value: 'COUNTING' }, { label: '待审核', value: 'APPROVED' }, { label: '已完成', value: 'COMPLETED' }, { label: '已取消', value: 'CANCELLED' }]
onMounted(async () => { warehouses.value = (await getWarehouseOptions()).data || []; fetchData() })
function statusLabel(status: string) { return statuses.find(item => item.value === status)?.label || status }
function statusType(status: string) { return ({ DRAFT: 'info', COUNTING: 'primary', APPROVED: 'warning', COMPLETED: 'success', CANCELLED: 'danger' } as any)[status] || '' }
function diffClass(value: any) { return Number(value || 0) > 0 ? 'diff-gain' : Number(value || 0) < 0 ? 'diff-loss' : '' }
function formatSigned(value: any) { const number = Number(value || 0); return `${number > 0 ? '+' : ''}${number.toFixed(4).replace(/\.?0+$/, '') || '0'}` }
async function fetchData() { loading.value = true; try { const params: any = { ...query }; if (params.dateRange?.length === 2) [params.startDate, params.endDate] = params.dateRange; delete params.dateRange; const res = await getInventoryCounts(params); tableData.value = res.data.records || []; total.value = res.data.total || 0 } finally { loading.value = false } }
function resetQuery() { Object.assign(query, { page: 1, countNo: '', warehouseId: '', status: '', dateRange: [] }); fetchData() }
async function start(row: any) { await startInventoryCount(row.id); ElMessage.success('盘点已开始'); router.push(`/inventory/counts/${row.id}/edit`) }
async function approve(row: any) { await ElMessageBox.confirm('审核后将按盘盈盘亏调整库存，是否继续？', '审核入账', { type: 'warning' }); await approveInventoryCount(row.id); ElMessage.success('盘点审核入账成功'); fetchData() }
async function cancel(row: any) { await ElMessageBox.confirm('确定取消该盘点单吗？', '取消盘点', { type: 'warning' }); await cancelInventoryCount(row.id); ElMessage.success('盘点单已取消'); fetchData() }
async function remove(row: any) { await ElMessageBox.confirm('删除后无法恢复，是否继续？', '删除盘点单', { type: 'warning' }); await deleteInventoryCount(row.id); ElMessage.success('盘点单已删除'); fetchData() }
</script>

<style scoped>
.toolbar { display:flex; gap:10px; flex-wrap:wrap; }.table-card { margin-top:12px; }.pagination { margin-top:12px; justify-content:flex-end; }.diff-gain { color:#67c23a; font-weight:600; }.diff-loss { color:#f56c6c; font-weight:600; }
</style>
