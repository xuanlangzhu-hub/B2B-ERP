<template>
  <div class="page-wrapper">
    <el-card>
      <el-tabs v-model="activeType" @tab-change="changeType">
        <el-tab-pane label="库存余额" name="BALANCE" />
        <el-tab-pane label="库存预警" name="WARNING" />
        <el-tab-pane label="库存流水" name="MOVEMENT" />
      </el-tabs>
      <div class="toolbar">
        <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:170px">
          <el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-if="activeType === 'MOVEMENT'" v-model="query.productId" placeholder="商品" clearable filterable style="width:190px">
          <el-option v-for="item in productOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <template v-else>
          <el-input v-model="query.productCode" placeholder="商品编码" clearable style="width:150px" />
          <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:160px" />
        </template>
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable style="width:150px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <template v-if="activeType === 'MOVEMENT'">
          <el-select v-model="query.movementType" placeholder="流水类型" clearable style="width:160px">
            <el-option v-for="item in movementTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.direction" placeholder="出入方向" clearable style="width:120px">
            <el-option label="入库" value="IN" /><el-option label="出库" value="OUT" />
          </el-select>
          <el-input v-model="query.sourceNo" placeholder="来源单号" clearable style="width:160px" />
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width:250px" />
        </template>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </el-card>

    <el-row :gutter="12" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="12" :sm="8" :lg="4">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">{{ card.label }}</div><div class="summary-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table v-if="activeType !== 'MOVEMENT'" :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="warehouseName" label="仓库" min-width="130" />
        <el-table-column prop="productCode" label="商品编码" min-width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="specification" label="规格" min-width="110" />
        <el-table-column prop="unitName" label="单位" width="75" />
        <el-table-column prop="quantity" label="库存数量" width="105" align="right" />
        <el-table-column prop="lockedQuantity" label="锁定数量" width="105" align="right" />
        <el-table-column prop="availableQuantity" label="可用数量" width="105" align="right" />
        <el-table-column prop="minStock" label="安全下限" width="100" align="right" />
        <el-table-column prop="avgCostPrice" label="平均成本" width="110" align="right" />
        <el-table-column prop="stockAmount" label="库存金额" width="115" align="right" />
        <el-table-column label="库存状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="row.lowStock ? 'danger' : 'success'">{{ row.lowStock ? '库存预警' : '正常' }}</el-tag></template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="movementNo" label="流水号" width="170" fixed="left" />
        <el-table-column prop="businessDate" label="业务日期" width="105" />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="productCode" label="商品编码" width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column label="类型" width="125"><template #default="{row}">{{ movementLabel(row.movementType) }}</template></el-table-column>
        <el-table-column label="方向" width="75"><template #default="{row}"><el-tag :type="row.direction === 'IN' ? 'success' : 'danger'">{{ row.direction === 'IN' ? '入库' : '出库' }}</el-tag></template></el-table-column>
        <el-table-column prop="quantity" label="数量" width="95" align="right" />
        <el-table-column prop="unitCost" label="单位成本" width="105" align="right" />
        <el-table-column prop="amount" label="成本金额" width="110" align="right" />
        <el-table-column prop="beforeQuantity" label="变动前" width="95" align="right" />
        <el-table-column prop="afterQuantity" label="变动后" width="95" align="right" />
        <el-table-column prop="sourceNo" label="来源单号" width="160" />
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getInventoryReport, getInventoryReportSummary, getInventoryMovementReport, getInventoryMovementSummary } from '@/api/report'
import { getWarehouseOptions, getProductOptions, getCategoryOptions } from '@/api/masterdata'

const activeType = ref('BALANCE'), loading = ref(false), tableData = ref<any[]>([]), total = ref(0)
const dateRange = ref<string[]>([]), warehouseOptions = ref<any[]>([]), productOptions = ref<any[]>([]), categoryOptions = ref<any[]>([])
const query = reactive({ page:1, size:10, warehouseId:'', productId:'', productCode:'', productName:'', categoryId:'', movementType:'', direction:'', sourceNo:'' })
const stockSummary = reactive<any>({ totalQuantity:0,totalStockAmount:0,lowStockCount:0,warehouseCount:0,productCount:0 })
const movementSummary = reactive<any>({ movementCount:0,productCount:0,inboundQuantity:0,outboundQuantity:0,inboundAmount:0,outboundAmount:0 })
const movementTypes = [
  {label:'采购入库',value:'PURCHASE_IN'},{label:'销售出库',value:'SALES_OUT'},{label:'销售退货入库',value:'SALES_RETURN_IN'},
  {label:'采购退货出库',value:'PURCHASE_RETURN_OUT'},{label:'盘点',value:'COUNT'},{label:'库存调整',value:'ADJUST'},
  {label:'调拨入库',value:'TRANSFER_IN'},{label:'调拨出库',value:'TRANSFER_OUT'},{label:'借出',value:'BORROW_OUT'},
  {label:'借出归还',value:'BORROW_OUT_RETURN'},{label:'借入',value:'BORROW_IN'},{label:'借入归还',value:'BORROW_IN_RETURN'}
]
const summaryCards = computed(() => activeType.value === 'MOVEMENT' ? [
  {label:'流水笔数',value:movementSummary.movementCount||0},{label:'涉及商品',value:movementSummary.productCount||0},
  {label:'入库数量',value:quantity(movementSummary.inboundQuantity)},{label:'出库数量',value:quantity(movementSummary.outboundQuantity)},
  {label:'入库金额',value:money(movementSummary.inboundAmount)},{label:'出库金额',value:money(movementSummary.outboundAmount)}
] : [
  {label:'库存总数量',value:quantity(stockSummary.totalQuantity)},{label:'库存总金额',value:money(stockSummary.totalStockAmount)},
  {label:'低库存项',value:stockSummary.lowStockCount||0},{label:'涉及仓库',value:stockSummary.warehouseCount||0},
  {label:'涉及商品',value:stockSummary.productCount||0}
])

onMounted(async()=>{await Promise.all([loadOptions(),fetchData()])})
async function loadOptions(){const [w,p,c]=await Promise.all([getWarehouseOptions(),getProductOptions(),getCategoryOptions()]);warehouseOptions.value=w.data||[];productOptions.value=p.data||[];categoryOptions.value=c.data||[]}
function stockFilters(){return {warehouseId:query.warehouseId||undefined,productCode:query.productCode||undefined,productName:query.productName||undefined,categoryId:query.categoryId||undefined,lowStock:activeType.value==='WARNING'?true:undefined}}
function movementFilters(){return {warehouseId:query.warehouseId||undefined,productId:query.productId||undefined,categoryId:query.categoryId||undefined,movementType:query.movementType||undefined,direction:query.direction||undefined,sourceNo:query.sourceNo||undefined,startDate:dateRange.value?.[0]||undefined,endDate:dateRange.value?.[1]||undefined}}
async function fetchData(){loading.value=true;try{if(activeType.value==='MOVEMENT'){const f=movementFilters();const [l,s]=await Promise.all([getInventoryMovementReport({...f,page:query.page,size:query.size}),getInventoryMovementSummary(f)]);tableData.value=l.data?.records||[];total.value=l.data?.total||0;Object.assign(movementSummary,s.data||{})}else{const f=stockFilters();const [l,s]=await Promise.all([getInventoryReport({...f,page:query.page,size:query.size}),getInventoryReportSummary(f)]);tableData.value=l.data?.records||[];total.value=l.data?.total||0;Object.assign(stockSummary,s.data||{})}}finally{loading.value=false}}
async function fetchList(){loading.value=true;try{const response=activeType.value==='MOVEMENT'?await getInventoryMovementReport({...movementFilters(),page:query.page,size:query.size}):await getInventoryReport({...stockFilters(),page:query.page,size:query.size});tableData.value=response.data?.records||[];total.value=response.data?.total||0}finally{loading.value=false}}
function search(){query.page=1;fetchData()} function changeType(){query.page=1;tableData.value=[];fetchData()}
function resetQuery(){Object.assign(query,{page:1,warehouseId:'',productId:'',productCode:'',productName:'',categoryId:'',movementType:'',direction:'',sourceNo:''});dateRange.value=[];fetchData()}
function movementLabel(type:string){return movementTypes.find(item=>item.value===type)?.label||type}
function money(value:any){return `¥${Number(value||0).toFixed(2)}`} function quantity(value:any){return Number(value||0).toFixed(2)}
</script>

<style scoped>
.page-wrapper{padding:0}.toolbar{display:flex;flex-wrap:wrap;gap:10px}.summary-row{margin-top:12px}.summary-card{margin-bottom:12px;text-align:center}.summary-label{color:#909399;font-size:13px}.summary-value{margin-top:8px;color:#303133;font-size:21px;font-weight:700}.pagination{justify-content:flex-end;margin-top:12px}
</style>
