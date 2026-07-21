<template>
  <div class="page-wrapper">
    <el-card>
      <el-tabs v-model="activeType" @tab-change="changeType">
        <el-tab-pane label="客户对账单" name="CUSTOMER" />
        <el-tab-pane label="供应商对账单" name="SUPPLIER" />
      </el-tabs>
      <div class="toolbar">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"
          start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-select v-if="activeType==='CUSTOMER'" v-model="query.partnerId" placeholder="客户" clearable filterable style="width:210px">
          <el-option v-for="item in customerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-else v-model="query.partnerId" placeholder="供应商" clearable filterable style="width:210px">
          <el-option v-for="item in supplierOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button><el-button @click="resetQuery">本月</el-button>
      </div>
    </el-card>

    <el-row :gutter="12" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="12" :sm="8" :lg="4">
        <el-card shadow="hover" class="summary-card"><div class="summary-label">{{card.label}}</div><div class="summary-value">{{card.value}}</div></el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="partnerCode" :label="activeType==='CUSTOMER'?'客户编码':'供应商编码'" width="140" />
        <el-table-column prop="partnerName" :label="activeType==='CUSTOMER'?'客户名称':'供应商名称'" min-width="180" />
        <el-table-column label="期初余额" width="125" align="right"><template #default="{row}">{{money(row.openingBalance)}}</template></el-table-column>
        <el-table-column :label="activeType==='CUSTOMER'?'本期销售':'本期采购'" width="125" align="right"><template #default="{row}">{{money(row.increaseAmount)}}</template></el-table-column>
        <el-table-column label="本期退货" width="125" align="right"><template #default="{row}">{{money(row.returnAmount)}}</template></el-table-column>
        <el-table-column :label="activeType==='CUSTOMER'?'本期收款':'本期付款'" width="125" align="right"><template #default="{row}">{{money(row.paymentAmount)}}</template></el-table-column>
        <el-table-column label="期末余额" width="130" align="right"><template #default="{row}"><strong>{{money(row.closingBalance)}}</strong></template></el-table-column>
        <el-table-column label="操作" width="90" fixed="right"><template #default="{row}"><el-button link type="primary" @click="openLedger(row)">明细</el-button></template></el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchList" />
    </el-card>

    <el-dialog v-model="ledgerVisible" :title="`${currentPartner?.partnerName||''} 往来明细`" width="min(960px,95vw)" top="5vh">
      <el-table :data="ledgerRows" v-loading="ledgerLoading" border stripe max-height="560">
        <el-table-column prop="businessDate" label="业务日期" width="110" />
        <el-table-column label="业务类型" width="110"><template #default="{row}">{{eventLabel(row.eventType)}}</template></el-table-column>
        <el-table-column prop="sourceNo" label="来源单号" width="175" />
        <el-table-column label="增加" width="120" align="right"><template #default="{row}">{{money(row.increaseAmount)}}</template></el-table-column>
        <el-table-column label="减少" width="120" align="right"><template #default="{row}">{{money(row.decreaseAmount)}}</template></el-table-column>
        <el-table-column label="余额" width="125" align="right"><template #default="{row}">{{money(row.balance)}}</template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="ledgerTotal"
        v-model:current-page="ledgerQuery.page" v-model:page-size="ledgerQuery.size" @change="fetchLedger" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed,onMounted,reactive,ref } from 'vue'
import { getCustomerStatements,getCustomerStatementSummary,getCustomerLedger,getSupplierStatements,getSupplierStatementSummary,getSupplierLedger } from '@/api/report'
import { getCustomerOptions,getSupplierOptions } from '@/api/masterdata'

const activeType=ref('CUSTOMER'),loading=ref(false),tableData=ref<any[]>([]),total=ref(0),customerOptions=ref<any[]>([]),supplierOptions=ref<any[]>([])
const dateRange=ref<string[]>(monthRange()),query=reactive({page:1,size:10,partnerId:''}),summary=reactive<any>({partnerCount:0,openingBalance:0,increaseAmount:0,returnAmount:0,paymentAmount:0,closingBalance:0})
const ledgerVisible=ref(false),ledgerLoading=ref(false),ledgerRows=ref<any[]>([]),ledgerTotal=ref(0),ledgerQuery=reactive({page:1,size:10}),currentPartner=ref<any>(null)
const summaryCards=computed(()=>[
  {label:activeType.value==='CUSTOMER'?'客户数':'供应商数',value:summary.partnerCount||0},{label:'期初余额',value:money(summary.openingBalance)},
  {label:activeType.value==='CUSTOMER'?'本期销售':'本期采购',value:money(summary.increaseAmount)},{label:'本期退货',value:money(summary.returnAmount)},
  {label:activeType.value==='CUSTOMER'?'本期收款':'本期付款',value:money(summary.paymentAmount)},{label:'期末余额',value:money(summary.closingBalance)}
])
onMounted(async()=>{const [c,s]=await Promise.all([getCustomerOptions(),getSupplierOptions()]);customerOptions.value=c.data||[];supplierOptions.value=s.data||[];fetchData()})
function params(){return {startDate:dateRange.value?.[0],endDate:dateRange.value?.[1],[activeType.value==='CUSTOMER'?'customerId':'supplierId']:query.partnerId||undefined}}
async function fetchData(){loading.value=true;try{const p=params();const [list,sum]=activeType.value==='CUSTOMER'?await Promise.all([getCustomerStatements({...p,page:query.page,size:query.size}),getCustomerStatementSummary(p)]):await Promise.all([getSupplierStatements({...p,page:query.page,size:query.size}),getSupplierStatementSummary(p)]);tableData.value=list.data?.records||[];total.value=list.data?.total||0;Object.assign(summary,sum.data||{})}finally{loading.value=false}}
async function fetchList(){loading.value=true;try{const p=params();const list=activeType.value==='CUSTOMER'?await getCustomerStatements({...p,page:query.page,size:query.size}):await getSupplierStatements({...p,page:query.page,size:query.size});tableData.value=list.data?.records||[];total.value=list.data?.total||0}finally{loading.value=false}}
function search(){query.page=1;fetchData()} function changeType(){query.page=1;query.partnerId='';fetchData()} function resetQuery(){dateRange.value=monthRange();query.partnerId='';query.page=1;fetchData()}
async function openLedger(row:any){currentPartner.value=row;ledgerQuery.page=1;ledgerVisible.value=true;await fetchLedger()}
async function fetchLedger(){if(!currentPartner.value)return;ledgerLoading.value=true;try{const p={startDate:dateRange.value?.[0],endDate:dateRange.value?.[1],[activeType.value==='CUSTOMER'?'customerId':'supplierId']:currentPartner.value.partnerId,page:ledgerQuery.page,size:ledgerQuery.size};const r=activeType.value==='CUSTOMER'?await getCustomerLedger(p):await getSupplierLedger(p);ledgerRows.value=r.data?.records||[];ledgerTotal.value=r.data?.total||0}finally{ledgerLoading.value=false}}
function eventLabel(type:string){return({SALE:'销售',PURCHASE:'采购',RETURN:'退货',RECEIPT:'收款',PAYMENT:'付款'} as Record<string,string>)[type]||type}
function money(v:any){return `¥${Number(v||0).toFixed(2)}`} function monthRange(){const now=new Date(),y=now.getFullYear(),m=String(now.getMonth()+1).padStart(2,'0'),last=new Date(y,now.getMonth()+1,0).getDate();return [`${y}-${m}-01`,`${y}-${m}-${String(last).padStart(2,'0')}`]}
</script>

<style scoped>
.page-wrapper{padding:0}.toolbar{display:flex;flex-wrap:wrap;gap:10px}.summary-row{margin-top:12px}.summary-card{margin-bottom:12px;text-align:center}.summary-label{color:#909399;font-size:13px}.summary-value{margin-top:8px;color:#303133;font-size:20px;font-weight:700}.pagination{justify-content:flex-end;margin-top:12px}
</style>
