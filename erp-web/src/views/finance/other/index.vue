<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar">
      <el-select v-model="query.transactionType" placeholder="收支类型" clearable style="width:130px"><el-option label="其他收入" value="INCOME"/><el-option label="其他支出" value="EXPENSE"/></el-select>
      <el-input v-model="query.category" placeholder="收支类别" clearable style="width:160px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:130px"><el-option label="草稿" value="DRAFT"/><el-option label="已确认" value="CONFIRMED"/><el-option label="已取消" value="CANCELLED"/></el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" @click="search">查询</el-button><el-button type="success" @click="openDialog()">新增收支</el-button>
    </div></el-card>
    <el-card class="table-card"><el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="transactionNo" label="单号" width="175"/><el-table-column prop="transactionDate" label="日期" width="110"/>
      <el-table-column label="类型" width="100"><template #default="{row}"><el-tag :type="row.transactionType==='INCOME'?'success':'danger'">{{row.transactionType==='INCOME'?'收入':'支出'}}</el-tag></template></el-table-column>
      <el-table-column prop="category" label="收支类别" width="140"/><el-table-column prop="accountName" label="资金账户" width="140"/>
      <el-table-column label="金额" width="120" align="right"><template #default="{row}">{{money(row.amount)}}</template></el-table-column>
      <el-table-column prop="counterparty" label="往来单位" min-width="140"/><el-table-column label="状态" width="100"><template #default="{row}">{{statusLabel(row.status)}}</template></el-table-column>
      <el-table-column label="操作" width="220" fixed="right"><template #default="{row}">
        <el-button v-if="row.status==='DRAFT'" link type="primary" @click="openDialog(row)">编辑</el-button>
        <el-button v-if="row.status==='DRAFT'" link type="success" @click="confirmRow(row)">确认</el-button>
        <el-button v-if="row.status!=='CANCELLED'" link type="warning" @click="cancelRow(row)">取消</el-button>
        <el-button v-if="row.status!=='CONFIRMED'" link type="danger" @click="removeRow(row)">删除</el-button>
      </template></el-table-column>
    </el-table><el-pagination class="pagination" background layout="total,prev,pager,next" :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData"/></el-card>

    <el-dialog v-model="dialogVisible" :title="editingId?'编辑其他收支':'新增其他收支'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="业务日期" prop="transactionDate"><el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="收支类型" prop="transactionType"><el-radio-group v-model="form.transactionType"><el-radio value="INCOME">其他收入</el-radio><el-radio value="EXPENSE">其他支出</el-radio></el-radio-group></el-form-item>
        <el-form-item label="收支类别" prop="category"><el-input v-model="form.category" placeholder="如：利息收入、办公费用"/></el-form-item>
        <el-form-item label="资金账户" prop="accountId"><el-select v-model="form.accountId" style="width:100%"><el-option v-for="item in accountOptions" :key="item.value" :label="`${item.label}（余额 ${money(item.balance)}）`" :value="item.value"/></el-select></el-form-item>
        <el-form-item label="金额" prop="amount"><el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width:100%"/></el-form-item>
        <el-form-item label="往来单位"><el-input v-model="form.counterparty"/></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea"/></el-form-item>
      </el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onMounted,reactive,ref} from 'vue';import {ElMessage,ElMessageBox} from 'element-plus'
import {getOtherTransactions,createOtherTransaction,updateOtherTransaction,confirmOtherTransaction,cancelOtherTransaction,deleteOtherTransaction,getAccountOptions} from '@/api/finance'
const loading=ref(false),rows=ref<any[]>([]),total=ref(0),dateRange=ref<string[]>([]),accountOptions=ref<any[]>([]),dialogVisible=ref(false),editingId=ref<number|null>(null),formRef=ref()
const query=reactive({page:1,size:10,transactionType:'',category:'',status:''}),form=reactive<any>({transactionDate:today(),transactionType:'INCOME',category:'',accountId:null,amount:0,counterparty:'',remark:''})
const rules={transactionDate:[{required:true,message:'请选择日期'}],transactionType:[{required:true,message:'请选择类型'}],category:[{required:true,message:'请输入类别'}],accountId:[{required:true,message:'请选择账户'}],amount:[{required:true,message:'请输入金额'}]}
onMounted(async()=>{const r=await getAccountOptions();accountOptions.value=r.data||[];fetchData()})
async function fetchData(){loading.value=true;try{const p:any={...query};if(dateRange.value?.length===2){p.startDate=dateRange.value[0];p.endDate=dateRange.value[1]}const r=await getOtherTransactions(p);rows.value=r.data?.records||[];total.value=r.data?.total||0}finally{loading.value=false}}
function search(){query.page=1;fetchData()} function openDialog(row?:any){editingId.value=row?.id||null;Object.assign(form,row?{transactionDate:row.transactionDate,transactionType:row.transactionType,category:row.category,accountId:row.accountId,amount:row.amount,counterparty:row.counterparty,remark:row.remark}:{transactionDate:today(),transactionType:'INCOME',category:'',accountId:null,amount:0,counterparty:'',remark:''});dialogVisible.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;if(editingId.value)await updateOtherTransaction(editingId.value,form);else await createOtherTransaction(form);ElMessage.success('草稿已保存');dialogVisible.value=false;fetchData()}
async function confirmRow(row:any){await ElMessageBox.confirm('确认后将立即更新账户余额，是否继续？','确认入账',{type:'warning'});await confirmOtherTransaction(row.id);ElMessage.success('已确认入账');fetchData()}
async function cancelRow(row:any){await ElMessageBox.confirm('已确认单据会生成反向流水，是否继续？','取消单据',{type:'warning'});await cancelOtherTransaction(row.id);ElMessage.success('单据已取消');fetchData()}
async function removeRow(row:any){await ElMessageBox.confirm('确定删除该单据？','删除确认',{type:'warning'});await deleteOtherTransaction(row.id);ElMessage.success('已删除');fetchData()}
function statusLabel(v:string){return({DRAFT:'草稿',CONFIRMED:'已确认',CANCELLED:'已取消'} as Record<string,string>)[v]||v}function money(v:any){return `¥${Number(v||0).toFixed(2)}`}function today(){return new Date().toLocaleDateString('sv-SE')}
</script>
<style scoped>.page-wrapper{padding:0}.toolbar{display:flex;flex-wrap:wrap;gap:10px}.table-card{margin-top:12px}.pagination{justify-content:flex-end;margin-top:12px}</style>
