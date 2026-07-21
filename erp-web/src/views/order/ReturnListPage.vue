<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar"><el-input v-model="query.returnNo" placeholder="退货单号" clearable style="width:180px" /><el-select v-model="query.status" placeholder="状态" clearable style="width:140px"><el-option label="草稿" value="DRAFT" /><el-option label="已审核" value="APPROVED" /><el-option label="已完成" value="COMPLETED" /><el-option label="已取消" value="CANCELLED" /></el-select><el-button type="primary" @click="fetchData">查询</el-button><el-button @click="reset">重置</el-button><el-button type="success" @click="router.push(`${basePath}/new`)">新增退货申请</el-button></div></el-card>
    <el-card class="list-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="returnNo" label="退货单号" width="165" /><el-table-column prop="returnDate" label="退货日期" width="115" />
        <el-table-column :prop="sourceNoField" :label="`原${sourceLabel}`" width="165" /><el-table-column :prop="partnerNameField" :label="partnerLabel" min-width="160" />
        <el-table-column prop="warehouseName" label="退货仓库" min-width="140" /><el-table-column prop="totalQuantity" label="退货数量" width="105" align="right" />
        <el-table-column label="退货金额" width="120" align="right"><template #default="{ row }">￥{{ money(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="95"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="280" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="router.push(`${basePath}/${row.id}`)">查看</el-button><el-button v-if="row.status === 'DRAFT'" link type="success" @click="approve(row)">审核</el-button><el-button v-if="row.status === 'DRAFT'" link type="danger" @click="cancel(row)">取消</el-button><el-button v-if="row.status === 'APPROVED'" link type="primary" @click="generate(row)">生成退货{{ isSales ? '入库' : '出库' }}</el-button></template></el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" background layout="total,prev,pager,next" @change="fetchData" />
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveSalesReturn, cancelSalesReturn, getSalesReturns } from '@/api/sales'
import { approvePurchaseReturn, cancelPurchaseReturn, getPurchaseReturns } from '@/api/purchase'
import { createInboundFromSalesReturn, createOutboundFromPurchaseReturn } from '@/api/inventory'
const props = defineProps<{ type: 'sales' | 'purchase' }>(); const router = useRouter(); const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const query = reactive({ page: 1, size: 10, returnNo: '', status: '' })
const isSales = computed(() => props.type === 'sales'); const basePath = computed(() => isSales.value ? '/sales/returns' : '/purchase/returns'); const sourceLabel = computed(() => isSales.value ? '销售单' : '采购单'); const sourceNoField = computed(() => isSales.value ? 'salesOrderNo' : 'purchaseOrderNo'); const partnerLabel = computed(() => isSales.value ? '客户' : '供应商'); const partnerNameField = computed(() => isSales.value ? 'customerName' : 'supplierName')
onMounted(fetchData)
async function fetchData() { loading.value = true; try { const res = await (isSales.value ? getSalesReturns(query) : getPurchaseReturns(query)); tableData.value = res.data.records || []; total.value = res.data.total || 0 } finally { loading.value = false } }
function reset() { Object.assign(query, { page: 1, returnNo: '', status: '' }); fetchData() }
async function approve(row: any) { await ElMessageBox.confirm(`审核后可生成退货${isSales.value ? '入库' : '出库'}单，确定审核吗？`, '审核退货申请'); await (isSales.value ? approveSalesReturn(row.id) : approvePurchaseReturn(row.id)); ElMessage.success('审核成功'); fetchData() }
async function cancel(row: any) { await ElMessageBox.confirm('确定取消这张退货申请吗？', '取消退货申请'); await (isSales.value ? cancelSalesReturn(row.id) : cancelPurchaseReturn(row.id)); ElMessage.success('已取消'); fetchData() }
async function generate(row: any) { await (isSales.value ? createInboundFromSalesReturn(row.id) : createOutboundFromPurchaseReturn(row.id)); ElMessage.success(`退货${isSales.value ? '入库' : '出库'}单已生成`); fetchData() }
function money(v: any) { return Number(v || 0).toFixed(2) } function statusType(s: string) { return ({ DRAFT: 'info', APPROVED: 'warning', COMPLETED: 'success', CANCELLED: 'danger' } as any)[s] || 'info' } function statusLabel(s: string) { return ({ DRAFT: '草稿', APPROVED: '已审核', COMPLETED: '已完成', CANCELLED: '已取消' } as any)[s] || s }
</script>
<style scoped>.list-card{margin-top:14px}.pagination{margin-top:16px;justify-content:flex-end}</style>
