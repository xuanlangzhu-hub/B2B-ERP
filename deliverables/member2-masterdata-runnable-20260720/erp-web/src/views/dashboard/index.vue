<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{color:card.color}">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card>
          <template #header>核心业务链路</template>
          <el-steps :active="1" align-center>
            <el-step title="基础资料" description="商品/客户/供应商/仓库" />
            <el-step title="采购入库" description="采购单 → 入库 → 应付" />
            <el-step title="销售出库" description="销售单 → 出库 → 应收" />
            <el-step title="资金结算" description="收款 / 付款" />
          </el-steps>
          <div style="margin-top:20px;text-align:center;color:#666">
            演示账号: admin / Admin@123456 | sales / purchase / warehouse / finance
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>快捷入口</template>
          <div style="display:flex;flex-direction:column;gap:8px">
            <el-button @click="$router.push('/sales/orders')">新增销售单</el-button>
            <el-button @click="$router.push('/purchase/orders')">新增采购单</el-button>
            <el-button @click="$router.push('/inventory/stock')">查看库存</el-button>
            <el-button @click="$router.push('/finance/flows')">资金流水</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardSummary } from '@/api/inventory'

const cards = ref([
  { label: '商品数量', value: '--', color: '#409EFF' },
  { label: '客户数量', value: '--', color: '#67C23A' },
  { label: '供应商数量', value: '--', color: '#E6A23C' },
  { label: '仓库数量', value: '--', color: '#F56C6C' },
  { label: '采购金额(本月)', value: '--', color: '#409EFF' },
  { label: '销售金额(本月)', value: '--', color: '#67C23A' }
])

onMounted(async () => {
  try {
    const res = await getDashboardSummary()
    const d = res.data || {}
    cards.value[0].value = d.productCount ?? '--'
    cards.value[1].value = d.customerCount ?? '--'
    cards.value[2].value = d.supplierCount ?? '--'
    cards.value[3].value = d.warehouseCount ?? '--'
    cards.value[4].value = d.purchaseAmount ? '¥' + Number(d.purchaseAmount).toFixed(2) : '--'
    cards.value[5].value = d.salesAmount ? '¥' + Number(d.salesAmount).toFixed(2) : '--'
  } catch { /* ignore */ }
})
</script>

<style scoped>
.stat-card { text-align: center; cursor: pointer; }
.stat-label { font-size: 14px; color: #999; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; }
</style>
