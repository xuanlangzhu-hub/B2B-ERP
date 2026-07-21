<template>
  <div class="document-page" v-loading="loading">
    <div class="document-header">
      <div><el-button link @click="router.push(basePath)"><el-icon><ArrowLeft /></el-icon> 返回列表</el-button><h1>{{ title }}详情</h1><el-tag v-if="detail.status" :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></div>
      <div class="header-actions">
        <el-button v-if="detail.status === 'DRAFT'" @click="router.push(`${basePath}/${detail.id}/edit`)">编辑</el-button>
        <el-button v-if="detail.status === 'DRAFT'" type="success" @click="approve">审核</el-button>
        <el-button v-if="detail.status === 'APPROVED'" type="warning" @click="cancel">取消单据</el-button>
      </div>
    </div>
    <el-card class="section-card">
      <template #header><b>单据信息</b></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="业务日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item :label="counterpartyLabel">{{ isSales ? detail.customerName : detail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
        <el-descriptions-item :label="isSales ? '预计送达' : '预计到货'">{{ isSales ? detail.expectedDeliveryDate : detail.expectedArrivalDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结算状态">{{ settlementLabel(detail.settlementStatus) }}</el-descriptions-item>
        <el-descriptions-item v-if="isSales" label="送货地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="isSales ? 1 : 3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card class="section-card">
      <template #header><b>商品明细</b></template>
      <el-table :data="detail.items || []" border>
        <el-table-column prop="lineNo" label="序号" width="65" />
        <el-table-column prop="productCode" label="商品编码" width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="specification" label="规格" min-width="120" />
        <el-table-column prop="quantity" label="数量" width="100" align="right" />
        <el-table-column prop="unitPrice" label="单价" width="110" align="right" />
        <el-table-column prop="discountRate" label="折扣%" width="90" align="right" />
        <el-table-column prop="taxRate" label="税率%" width="90" align="right" />
        <el-table-column label="金额" width="120" align="right"><template #default="{ row }">￥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="130" />
      </el-table>
      <div class="amount-summary">
        <div><span>商品金额</span><b>￥{{ money(detail.totalAmount) }}</b></div>
        <div><span>折扣金额</span><b>-￥{{ money(detail.discountAmount) }}</b></div>
        <div><span>运费</span><b>￥{{ money(detail.freightAmount) }}</b></div>
        <div class="payable"><span>{{ isSales ? '应收金额' : '应付金额' }}</span><b>￥{{ money(isSales ? detail.receivableAmount || detail.payableAmount : detail.payableAmount) }}</b></div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveSalesOrder, cancelSalesOrder, getSalesOrder } from '@/api/sales'
import { approvePurchaseOrder, cancelPurchaseOrder, getPurchaseOrder } from '@/api/purchase'

const route = useRoute(); const router = useRouter(); const loading = ref(false); const detail = ref<any>({})
const isSales = computed(() => route.meta.businessType === 'sales')
const title = computed(() => isSales.value ? '销售单' : '采购单')
const counterpartyLabel = computed(() => isSales.value ? '客户' : '供应商')
const basePath = computed(() => isSales.value ? '/sales/orders' : '/purchase/orders')
onMounted(load)
async function load() { loading.value = true; try { const id = Number(route.params.id); const res = await (isSales.value ? getSalesOrder(id) : getPurchaseOrder(id)); detail.value = res.data } finally { loading.value = false } }
async function approve() { await ElMessageBox.confirm(`确认审核该${title.value}？`, '提示'); await (isSales.value ? approveSalesOrder(detail.value.id) : approvePurchaseOrder(detail.value.id)); ElMessage.success('已审核'); load() }
async function cancel() { await ElMessageBox.confirm(`确认取消该${title.value}？`, '提示'); await (isSales.value ? cancelSalesOrder(detail.value.id) : cancelPurchaseOrder(detail.value.id)); ElMessage.success('已取消'); load() }
function money(value: any) { return Number(value || 0).toFixed(2) }
function statusType(status: string) { return ({ DRAFT: 'info', APPROVED: 'warning', PARTIALLY_INBOUND: 'primary', PARTIALLY_OUTBOUND: 'primary', COMPLETED: 'success', CANCELLED: 'danger' } as any)[status] || 'info' }
function statusLabel(status: string) { return ({ DRAFT: '草稿', APPROVED: '已审核', PARTIALLY_INBOUND: '部分入库', PARTIALLY_OUTBOUND: '部分出库', COMPLETED: '已完成', CANCELLED: '已取消' } as any)[status] || status }
function settlementLabel(status: string) { return ({ UNSETTLED: '未结算', PARTIALLY_SETTLED: '部分结算', SETTLED: '已结算' } as any)[status] || status || '-' }
</script>

<style scoped>
.document-header { min-height: 66px; display: flex; align-items: center; justify-content: space-between; margin: -16px -20px 16px; padding: 0 22px; background: #fff; border-bottom: 1px solid #edf0f5; }
.document-header > div { display: flex; align-items: center; gap: 12px; }
.document-header h1 { margin: 0; font-size: 19px; }
.section-card + .section-card { margin-top: 14px; }
.amount-summary { width: 360px; margin: 22px 0 0 auto; padding: 18px 22px; background: #f8fafc; }
.amount-summary div { display: flex; justify-content: space-between; margin: 9px 0; color: #687386; }
.amount-summary b { color: #273444; }
.amount-summary .payable { margin-top: 14px; padding-top: 14px; border-top: 1px solid #e2e7ee; font-size: 17px; }
.amount-summary .payable b { color: #e66b2f; font-size: 22px; }
</style>
