<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar"><el-date-picker v-model="dateRange" type="daterange" range-separator="至" value-format="YYYY-MM-DD"/><el-button type="primary" @click="fetchData">查询</el-button></div></el-card>
    <el-card style="margin-top:12px">
      <div style="display:flex;gap:16px;margin-bottom:12px"><el-statistic title="采购单数" :value="summary.orderCount"/><el-statistic title="采购总额" :value="summary.totalAmount" prefix="¥"/><el-statistic title="已付金额" :value="summary.paidAmount" prefix="¥"/></div>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="单号" width="150"/><el-table-column prop="orderDate" label="日期" width="110"/>
        <el-table-column prop="supplierName" label="供应商"/><el-table-column prop="productName" label="商品"/>
        <el-table-column prop="quantity" label="数量" width="80"/><el-table-column prop="unitPrice" label="单价" width="80"/><el-table-column prop="amount" label="金额" width="100"/>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData"/>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPurchaseOrders } from '@/api/purchase'
const loading=ref(false);const tableData=ref<any[]>([]);const total=ref(0)
const query=reactive({page:1,size:10});const dateRange=ref<any[]>([])
const summary=reactive({orderCount:0,totalAmount:0,paidAmount:0})
onMounted(fetchData)
async function fetchData(){loading.value=true;try{const r=await getPurchaseOrders(query);tableData.value=r.data.records;total.value=r.data.total;summary.orderCount=r.data.total;summary.totalAmount=r.data.records?.reduce((s:number,i:any)=>s+(i.totalAmount||0),0)||0;summary.paidAmount=r.data.records?.reduce((s:number,i:any)=>s+(i.paidAmount||0),0)||0}finally{loading.value=false}}
</script>
<style scoped>.page-wrapper{padding:0}.toolbar{display:flex;gap:10px;flex-wrap:wrap}</style>
