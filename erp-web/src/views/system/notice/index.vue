<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar">
      <el-input v-model="query.title" placeholder="通知标题" clearable style="width:220px" />
      <el-select v-model="query.status" placeholder="发布状态" clearable style="width:140px"><el-option label="草稿" value="DRAFT"/><el-option label="已发布" value="PUBLISHED"/></el-select>
      <el-button type="primary" @click="load">查询</el-button><el-button type="success" @click="openAdd">新增通知</el-button>
    </div></el-card>
    <el-card>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="noticeTitle" label="标题" min-width="220" /><el-table-column prop="noticeType" label="类型" width="100" />
        <el-table-column prop="publishStatus" label="状态" width="100"><template #default="{row}"><el-tag :type="row.publishStatus==='PUBLISHED'?'success':'info'">{{ row.publishStatus==='PUBLISHED'?'已发布':'草稿' }}</el-tag></template></el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170" /><el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right"><template #default="{row}">
          <el-button size="small" @click="preview=row; previewVisible=true">查看</el-button><el-button v-if="row.publishStatus==='DRAFT'" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.publishStatus==='DRAFT'" size="small" type="success" @click="publish(row)">发布</el-button><el-button v-if="row.publishStatus==='DRAFT'" size="small" type="danger" @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination style="margin-top:14px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" v-model:current-page="query.page" @change="load" />
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editingId?'编辑通知':'新增通知'" width="620px"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="标题" prop="noticeTitle"><el-input v-model="form.noticeTitle" /></el-form-item><el-form-item label="类型"><el-select v-model="form.noticeType"><el-option label="系统通知" value="SYSTEM"/><el-option label="业务提醒" value="BUSINESS"/></el-select></el-form-item>
      <el-form-item label="内容" prop="noticeContent"><el-input v-model="form.noticeContent" type="textarea" :rows="8" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template></el-dialog>
    <el-dialog v-model="previewVisible" title="通知详情" width="620px"><h3>{{ preview.noticeTitle }}</h3><div class="notice-meta">{{ preview.publishedAt || preview.createdAt }}</div><div class="notice-content">{{ preview.noticeContent }}</div></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'; import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticeManage, createNotice, updateNotice, publishNotice, deleteNotice } from '@/api'
const loading=ref(false), rows=ref<any[]>([]), total=ref(0), dialogVisible=ref(false), previewVisible=ref(false), editingId=ref<number|null>(null), formRef=ref(), preview=ref<any>({})
const query=reactive({page:1,size:10,title:'',status:''}); const form=reactive({noticeTitle:'',noticeType:'SYSTEM',noticeContent:''}); const rules={noticeTitle:[{required:true,message:'请输入标题',trigger:'blur'}],noticeContent:[{required:true,message:'请输入内容',trigger:'blur'}]}
onMounted(load); async function load(){loading.value=true;try{const r=await getNoticeManage(query);rows.value=r.data.records||[];total.value=r.data.total||0}finally{loading.value=false}}
function openAdd(){editingId.value=null;Object.assign(form,{noticeTitle:'',noticeType:'SYSTEM',noticeContent:''});dialogVisible.value=true} function openEdit(row:any){editingId.value=row.id;Object.assign(form,row);dialogVisible.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;editingId.value?await updateNotice(editingId.value,form):await createNotice(form);ElMessage.success('保存成功');dialogVisible.value=false;load()}
async function publish(row:any){await ElMessageBox.confirm('发布后将不能修改，确定发布吗？','确认',{type:'warning'});await publishNotice(row.id);ElMessage.success('发布成功');load()} async function remove(row:any){await ElMessageBox.confirm('确定删除该草稿吗？','确认',{type:'warning'});await deleteNotice(row.id);ElMessage.success('删除成功');load()}
</script>
<style scoped>.notice-meta{color:#909399;margin:8px 0 20px}.notice-content{white-space:pre-wrap;line-height:1.8}</style>
