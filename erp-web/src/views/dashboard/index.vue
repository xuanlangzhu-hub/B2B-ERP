<template>
  <div class="dashboard-page">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="8" :md="6" :lg="6" :xl="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-note">{{ card.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="dashboard-row">
      <el-col :xs="24" :lg="15">
        <el-card class="full-height-card">
          <template #header>核心业务链路</template>
          <el-steps :active="2" align-center finish-status="success">
            <el-step title="基础资料" description="商品、客户、供应商、仓库" />
            <el-step title="采购入库" description="采购单 → 入库 → 应付" />
            <el-step title="销售出库" description="销售单 → 出库 → 应收" />
            <el-step title="资金结算" description="收款、付款、资金流水" />
          </el-steps>
          <div class="demo-tip">演示账号：admin / Admin@123456</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card class="full-height-card">
          <template #header>待处理库存业务</template>
          <div class="quick-actions">
            <el-button @click="$router.push('/inventory/inbounds')">
              待确认入库 <el-tag type="warning" size="small">{{ summary.pendingInboundCount }}</el-tag>
            </el-button>
            <el-button @click="$router.push('/inventory/outbounds')">
              待确认出库 <el-tag type="warning" size="small">{{ summary.pendingOutboundCount }}</el-tag>
            </el-button>
            <el-button @click="$router.push('/inventory/stock')">查看实时库存</el-button>
            <el-button @click="$router.push('/reports/inventory')">查看库存报表</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="dashboard-row">
      <template #header>
        <div class="card-header">
          <span>低库存预警（前 5 条）</span>
          <el-button link type="primary" @click="$router.push('/inventory/stock?lowStock=true')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="summary.lowStockItems" border stripe empty-text="暂无低库存商品">
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="productCode" label="商品编码" min-width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="specification" label="规格" min-width="120" />
        <el-table-column label="可用库存" width="120">
          <template #default="{ row }">
            <span class="danger-text">{{ formatQuantity(row.availableQuantity) }} {{ row.unitName || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最低库存" width="120">
          <template #default="{ row }">{{ formatQuantity(row.minStock) }} {{ row.unitName || '' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { getDashboardSummary } from '@/api/inventory'

const summary = reactive<any>({
  productCount: 0,
  customerCount: 0,
  supplierCount: 0,
  warehouseCount: 0,
  inventoryAmount: 0,
  lowStockCount: 0,
  purchaseAmount: 0,
  salesAmount: 0,
  purchaseOrderCount: 0,
  salesOrderCount: 0,
  pendingInboundCount: 0,
  pendingOutboundCount: 0,
  lowStockItems: []
})

const cards = computed(() => [
  { key: 'product', label: '商品数量', value: summary.productCount, note: '在库商品资料', color: '#409EFF' },
  { key: 'customer', label: '客户数量', value: summary.customerCount, note: '当前企业客户', color: '#67C23A' },
  { key: 'supplier', label: '供应商数量', value: summary.supplierCount, note: '当前企业供应商', color: '#E6A23C' },
  { key: 'warehouse', label: '仓库数量', value: summary.warehouseCount, note: '当前企业仓库', color: '#909399' },
  { key: 'inventory', label: '库存金额', value: formatMoney(summary.inventoryAmount), note: '按移动平均成本', color: '#7A5AF8' },
  { key: 'low-stock', label: '低库存预警', value: summary.lowStockCount, note: '可用量低于最低库存', color: '#F56C6C' },
  { key: 'purchase', label: '采购金额（本月）', value: formatMoney(summary.purchaseAmount), note: `${summary.purchaseOrderCount} 张已完成采购单`, color: '#409EFF' },
  { key: 'sales', label: '销售金额（本月）', value: formatMoney(summary.salesAmount), note: `${summary.salesOrderCount} 张已完成销售单`, color: '#67C23A' },
  { key: 'inbound', label: '待确认入库', value: summary.pendingInboundCount, note: '草稿入库单', color: '#E6A23C' },
  { key: 'outbound', label: '待确认出库', value: summary.pendingOutboundCount, note: '草稿出库单', color: '#F56C6C' }
])

onMounted(async () => {
  try {
    const response = await getDashboardSummary()
    Object.assign(summary, response.data || {})
    summary.lowStockItems = response.data?.lowStockItems || []
  } catch {
    summary.lowStockItems = []
  }
})

function formatMoney(value: unknown) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatQuantity(value: unknown) {
  return Number(value ?? 0).toFixed(2)
}
</script>

<style scoped>
.dashboard-page { padding: 0; }
.stat-card { margin-bottom: 16px; text-align: center; }
.stat-label { margin-bottom: 8px; color: #909399; font-size: 14px; }
.stat-value { font-size: 27px; font-weight: 700; line-height: 36px; }
.stat-note { min-height: 18px; margin-top: 6px; color: #b1b3b8; font-size: 12px; }
.dashboard-row { margin-top: 0; margin-bottom: 16px; }
.full-height-card { height: 100%; }
.demo-tip { margin-top: 24px; color: #909399; text-align: center; }
.quick-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.quick-actions .el-button { width: 100%; margin: 0; }
.quick-actions .el-tag { margin-left: 6px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.danger-text { color: #f56c6c; font-weight: 600; }
@media (max-width: 1199px) {
  .full-height-card { margin-bottom: 16px; }
}
@media (max-width: 640px) {
  .quick-actions { grid-template-columns: 1fr; }
  .stat-value { font-size: 22px; }
}
</style>
