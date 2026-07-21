<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar"><el-input v-model="query.receiptNo" placeholder="收款单号" clearable style="width:160px"/><el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"/><el-button type="primary" @click="fetchData">查询</el-button><el-button type="success" @click="handleAdd">新增收款</el-button></div></el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="receiptNo" label="单号" width="150"/><el-table-column prop="receiptDate" label="日期" width="110"/>
        <el-table-column prop="customerName" label="客户" width="160"/>
        <el-table-column prop="receiptAmount" label="收款金额" width="100"/><el-table-column prop="paymentMethod" label="方式" width="80"/>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='CONFIRMED'?'success':'info'" size="small">{{row.status==='CONFIRMED'?'已确认':'草稿'}}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170"/>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData"/>
    </el-card>

    <el-dialog title="新增收款" v-model="dialogVisible" width="500px">
      <el-form :model="form" ref="formRef" label-width="100px">
        <el-form-item label="客户"><el-select v-model="form.customerId" filterable style="width:100%"><el-option v-for="c in customers" :key="c.value" :label="c.label" :value="c.value"/></el-select></el-form-item>
        <el-form-item label="收款金额"><el-input-number v-model="form.receiptAmount" :min="0" :precision="2" style="width:100%" controls-position="right"/></el-form-item>
        <el-form-item label="收款方式"><el-select v-model="form.paymentMethod" style="width:100%"><el-option label="现金" value="CASH"/><el-option label="银行转账" value="BANK"/><el-option label="微信" value="WECHAT"/><el-option label="支付宝" value="ALIPAY"/></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReceipts, createReceipt } from '@/api/sales'
import { getCustomerOptions } from '@/api/masterdata'
import { useUserStore } from '@/stores/user'

const loading=ref(false);const tableData=ref<any[]>([]);const total=ref(0)
const query=reactive({page:1,size:10,receiptNo:''}); const dateRange=ref<any[]>([])
const userStore=useUserStore()
const dialogVisible=ref(false);const form=reactive({storeId:null as number|null,customerId:null,receiptAmount:0,paymentMethod:'BANK',receiptDate:'',remark:''});const formRef=ref()
const customers=ref<any[]>([])

onMounted(async()=>{fetchData();const r=await getCustomerOptions();customers.value=r.data||[]})

async function fetchData(){loading.value=true;try{const r=await getReceipts(query);tableData.value=r.data.records;total.value=r.data.total}finally{loading.value=false}}
function handleAdd(){Object.assign(form,{storeId:userStore.userInfo?.defaultStoreId||null,customerId:null,receiptAmount:0,paymentMethod:'BANK',receiptDate:new Date().toISOString().slice(0,10),remark:''});dialogVisible.value=true}
async function handleSubmit(){if(!form.storeId){ElMessage.warning('当前用户未设置默认门店');return}if(!form.customerId||!form.receiptDate||form.receiptAmount<=0){ElMessage.warning('请完整填写客户、日期和收款金额');return}await createReceipt(form);ElMessage.success('收款成功');dialogVisible.value=false;fetchData()}
</script>
<style scoped>.page-wrapper{padding:0}.toolbar{display:flex;gap:10px;flex-wrap:wrap}</style>
