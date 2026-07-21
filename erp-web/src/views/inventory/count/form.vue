<template>
  <div class="document-page" v-loading="loading">
    <div class="document-header">
      <div><el-button link @click="router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button><h1>{{ editing ? '录入实盘数量' : '新增库存盘点' }}</h1></div>
      <div class="header-actions"><el-button @click="router.push('/inventory/counts')">取消</el-button><el-button v-if="editing" :loading="saving" @click="save(false)">保存</el-button><el-button type="primary" :loading="saving" @click="editing ? save(true) : create()">{{ editing ? '保存并提交审核' : '创建并开始盘点' }}</el-button></div>
    </div>

    <el-card class="section-card">
      <template #header><div class="section-title"><span>基本信息</span><small>{{ editing ? '盘点开始后仓库和日期不可更改' : '选择需要盘点的仓库' }}</small></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="20">
          <el-col :span="6"><el-form-item label="盘点日期" prop="countDate"><el-date-picker v-model="form.countDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled="editing" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="盘点仓库" prop="warehouseId"><el-select v-model="form.warehouseId" style="width:100%" placeholder="选择仓库" :disabled="editing"><el-option v-for="item in warehouses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col v-if="editing" :span="6"><el-form-item label="盘点单号"><el-input :model-value="detail?.countNo" disabled /></el-form-item></el-col>
          <el-col v-if="editing" :span="6"><el-form-item label="状态"><el-input value="盘点中" disabled /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <el-alert v-if="!editing" title="创建后系统会把该仓库当前库存作为账面数量。盘点期间请尽量暂停该仓库出入库。" type="warning" :closable="false" show-icon />
    </el-card>

    <el-card v-if="editing" class="section-card">
      <template #header><div class="section-title"><span>盘点明细</span><small>有盘盈或盘亏时必须填写差异原因</small></div></template>
      <el-table :data="detail?.items || []" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="170" />
        <el-table-column prop="specification" label="规格" min-width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="bookQuantity" label="账面数量" width="120" align="right" />
        <el-table-column label="实盘数量" width="180"><template #default="{ row }"><el-input-number v-model="row.actualQuantity" :min="0" :precision="4" controls-position="right" style="width:150px" /></template></el-table-column>
        <el-table-column label="差异" width="110" align="right"><template #default="{ row }"><span :class="diffClass(diff(row))">{{ signed(diff(row)) }}</span></template></el-table-column>
        <el-table-column label="差异原因" min-width="190"><template #default="{ row }"><el-input v-model="row.reason" :placeholder="diff(row) ? '必填' : '无差异'" maxlength="500" /></template></el-table-column>
      </el-table>
      <div class="summary"><span>账面总数：{{ totalBook }}</span><span>实盘总数：{{ totalActual }}</span><strong :class="diffClass(totalDiff)">盘点差异：{{ signed(totalDiff) }}</strong></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createInventoryCount, getInventoryCount, startInventoryCount, submitInventoryCount, updateInventoryCount } from '@/api/inventory'
import { getWarehouseOptions } from '@/api/masterdata'

const route = useRoute(); const router = useRouter(); const id = computed(() => route.params.id ? Number(route.params.id) : null); const editing = computed(() => !!id.value)
const loading = ref(false); const saving = ref(false); const formRef = ref(); const warehouses = ref<any[]>([]); const detail = ref<any>(null)
const form = reactive<any>({ countDate: today(), warehouseId: null, remark: '' })
const rules = { countDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }], warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }] }
const totalBook = computed(() => sum('bookQuantity')); const totalActual = computed(() => sum('actualQuantity')); const totalDiff = computed(() => totalActual.value - totalBook.value)
onMounted(async () => { loading.value = true; try { warehouses.value = (await getWarehouseOptions()).data || []; if (id.value) { detail.value = (await getInventoryCount(id.value)).data; Object.assign(form, { countDate: detail.value.countDate, warehouseId: detail.value.warehouseId, remark: detail.value.remark || '' }) } } finally { loading.value = false } })
function today() { return new Date().toISOString().slice(0, 10) }
function sum(field: string) { return Number((detail.value?.items || []).reduce((result: number, item: any) => result + Number(item[field] || 0), 0).toFixed(4)) }
function diff(row: any) { return Number(row.actualQuantity || 0) - Number(row.bookQuantity || 0) }
function signed(value: any) { const number = Number(value || 0); return `${number > 0 ? '+' : ''}${number.toFixed(4).replace(/\.?0+$/, '') || '0'}` }
function diffClass(value: any) { return Number(value || 0) > 0 ? 'diff-gain' : Number(value || 0) < 0 ? 'diff-loss' : '' }
async function create() { if (!await formRef.value.validate().catch(() => false)) return; saving.value = true; try { const res = await createInventoryCount(form); await startInventoryCount(res.data.id); ElMessage.success('盘点单已创建，请录入实盘数量'); router.replace(`/inventory/counts/${res.data.id}/edit`) } finally { saving.value = false } }
async function save(thenSubmit: boolean) { if (!detail.value || !id.value) return; const missing = detail.value.items.some((item: any) => diff(item) !== 0 && !item.reason?.trim()); if (missing) { ElMessage.warning('有盘盈或盘亏的商品必须填写差异原因'); return } saving.value = true; try { await updateInventoryCount(id.value, { remark: form.remark, items: detail.value.items.map((item: any) => ({ id: item.id, actualQuantity: item.actualQuantity, reason: item.reason })) }); if (thenSubmit) { await submitInventoryCount(id.value); ElMessage.success('盘点单已提交审核'); router.replace(`/inventory/counts/${id.value}`) } else ElMessage.success('实盘数量已保存') } finally { saving.value = false } }
</script>

<style scoped>
.document-page { padding-bottom:24px; }.document-header { min-height:66px; display:flex; align-items:center; justify-content:space-between; margin:-16px -20px 16px; padding:0 22px; background:#fff; border-bottom:1px solid #edf0f5; }.document-header>div:first-child,.header-actions,.section-title,.summary { display:flex; align-items:center; }.document-header>div:first-child { gap:14px; }.document-header h1 { margin:0; font-size:19px; }.header-actions { gap:8px; }.section-card+.section-card { margin-top:14px; }.section-title { gap:12px; }.section-title span { font-size:16px; font-weight:600; }.section-title small { color:#a1a8b3; }.summary { justify-content:flex-end; gap:32px; margin-top:20px; }.summary strong { font-size:17px; }.diff-gain { color:#67c23a; font-weight:600; }.diff-loss { color:#f56c6c; font-weight:600; }
</style>
