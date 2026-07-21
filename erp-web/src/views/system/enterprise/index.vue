<template>
  <div class="page-wrapper enterprise-page">
    <el-card>
      <template #header><span class="card-title">企业信息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width:760px">
        <el-form-item label="企业编码"><el-input v-model="form.enterpriseCode" disabled /></el-form-item>
        <el-form-item label="企业名称" prop="enterpriseName"><el-input v-model="form.enterpriseName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="企业地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="Logo 地址"><el-input v-model="form.logoUrl" placeholder="可填写图片地址" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="4" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="save">保存企业信息</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnterprise, updateEnterprise } from '@/api'
const formRef = ref(); const saving = ref(false)
const form = reactive<any>({ enterpriseCode: '', enterpriseName: '', contactName: '', contactPhone: '', address: '', logoUrl: '', remark: '' })
const rules = { enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }] }
onMounted(async () => Object.assign(form, (await getEnterprise()).data || {}))
async function save() { if (!await formRef.value.validate().catch(() => false)) return; saving.value = true; try { await updateEnterprise(form); ElMessage.success('企业信息已保存') } finally { saving.value = false } }
</script>
<style scoped>.card-title { font-size: 17px; font-weight: 600; }</style>
