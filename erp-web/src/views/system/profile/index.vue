<template>
  <div class="page-wrapper profile-grid">
    <el-card>
      <template #header><span class="card-title">个人信息</span></template>
      <el-form ref="profileRef" :model="profile" :rules="profileRules" label-width="90px">
        <el-form-item label="登录账号"><el-input v-model="profile.username" disabled /></el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="profile.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profile.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
        <el-form-item label="头像地址"><el-input v-model="profile.avatarUrl" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveProfile">保存资料</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card>
      <template #header><span class="card-title">修改密码</span></template>
      <el-alert title="修改成功后请使用新密码重新登录" type="info" :closable="false" style="margin-bottom:18px" />
      <el-form ref="passwordRef" :model="password" :rules="passwordRules" label-width="100px">
        <el-form-item label="当前密码" prop="currentPassword"><el-input v-model="password.currentPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="password.confirmPassword" type="password" show-password /></el-form-item>
        <el-form-item><el-button type="primary" @click="savePassword">修改密码</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api'
import { useUserStore } from '@/stores/user'
const userStore = useUserStore(); const profileRef = ref(); const passwordRef = ref()
const profile = reactive<any>({ username: '', realName: '', phone: '', email: '', avatarUrl: '' })
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const profileRules = { realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }
const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ validator: (_: any, value: string, callback: any) => value === password.newPassword ? callback() : callback(new Error('两次密码不一致')), trigger: 'blur' }]
}
onMounted(async () => Object.assign(profile, (await getProfile()).data || {}))
async function saveProfile() { if (!await profileRef.value.validate().catch(() => false)) return; await updateProfile(profile); await userStore.fetchUserInfo(); ElMessage.success('个人资料已保存') }
async function savePassword() { if (!await passwordRef.value.validate().catch(() => false)) return; await changePassword(password); ElMessage.success('密码修改成功，请重新登录'); userStore.logout() }
</script>
<style scoped>.profile-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }.card-title { font-size:17px; font-weight:600; }</style>
