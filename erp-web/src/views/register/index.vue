<template>
  <div class="auth-page">
    <div class="auth-panel">
      <section class="brand-side">
        <div class="brand-shade"></div>
        <div class="brand-content">
          <div class="brand-logo">B2B</div>
          <h1>免费创建企业账套</h1>
          <p>两步完成注册，系统会自动创建默认门店、仓库和管理员权限。</p>
          <ul>
            <li>进销存业务完整闭环</li>
            <li>多门店与多角色协同</li>
            <li>库存与资金流水自动生成</li>
          </ul>
        </div>
        <div class="copyright">© 2026 B2B ERP 实训项目</div>
      </section>

      <section class="form-side">
        <div class="form-box">
          <h2>{{ step === 0 ? '注册账号' : '完善资料' }}</h2>
          <el-steps :active="step" finish-status="success" simple class="register-steps">
            <el-step title="创建账号" />
            <el-step title="企业资料" />
          </el-steps>

          <el-form v-if="step === 0" ref="accountFormRef" :model="form" :rules="accountRules" label-position="top" size="large">
            <el-form-item label="登录邮箱" prop="username"><el-input v-model.trim="form.username" placeholder="name@example.com" /></el-form-item>
            <el-form-item label="登录密码" prop="password"><el-input v-model="form.password" type="password" show-password placeholder="至少8位字符" /></el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" @keyup.enter="nextStep" /></el-form-item>
            <el-button class="submit-button" type="primary" @click="nextStep">下一步</el-button>
          </el-form>

          <el-form v-else ref="profileFormRef" :model="form" :rules="profileRules" label-position="top" size="large">
            <el-row :gutter="14">
              <el-col :span="12"><el-form-item label="企业名称" prop="enterpriseName"><el-input v-model.trim="form.enterpriseName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="联系人" prop="realName"><el-input v-model.trim="form.realName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="联系电话" prop="phone"><el-input v-model.trim="form.phone" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="默认门店" prop="storeName"><el-input v-model.trim="form.storeName" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="企业地址"><el-input v-model.trim="form.enterpriseAddress" /></el-form-item>
            <el-form-item label="门店地址"><el-input v-model.trim="form.storeAddress" /></el-form-item>
            <div class="button-row">
              <el-button @click="step = 0">上一步</el-button>
              <el-button type="primary" :loading="submitting" @click="submitRegister">完成注册</el-button>
            </div>
          </el-form>
          <div class="switch-entry">已有账号？<router-link to="/login">返回登录</router-link></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerAccount } from '@/api'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)
const accountFormRef = ref()
const profileFormRef = ref()
const form = reactive({ username: '', password: '', confirmPassword: '', enterpriseName: '', realName: '', phone: '', enterpriseAddress: '', storeName: '', storeAddress: '' })
const validateConfirm = (_rule: any, value: string, callback: (error?: Error) => void) => value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
const accountRules = {
  username: [{ required: true, message: '请输入登录邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 8, max: 64, message: '密码长度应为8到64位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
}
const profileRules = {
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入默认门店名称', trigger: 'blur' }]
}

async function nextStep() {
  if (await accountFormRef.value.validate().catch(() => false)) step.value = 1
}
async function submitRegister() {
  if (!await profileFormRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    await registerAccount(form)
    ElMessage.success('注册成功，请使用新账号登录')
    router.replace({ path: '/login', query: { username: form.username } })
  } finally { submitting.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: grid; place-items: center; padding: 28px; background: linear-gradient(rgb(11 34 68 / 58%), rgb(11 34 68 / 58%)), url('@/assets/images/login-city.png') center / cover no-repeat; }
.auth-panel { width: min(1180px, calc(100vw - 70px)); min-height: 690px; display: grid; grid-template-columns: 44% 56%; overflow: hidden; background: #fff; box-shadow: 0 22px 70px rgb(6 25 52 / 38%); }
.brand-side { position: relative; min-height: 690px; overflow: hidden; color: #fff; background: url('@/assets/images/login-city.png') center / cover no-repeat; }
.brand-shade { position: absolute; inset: 0; background: linear-gradient(145deg, rgb(8 73 146 / 94%), rgb(9 63 126 / 76%)); }
.brand-content { position: relative; z-index: 1; padding: 58px 48px; }
.brand-logo { width: 68px; height: 68px; display: grid; place-items: center; margin-bottom: 24px; border: 2px solid rgb(255 255 255 / 78%); border-radius: 18px; font-size: 23px; font-weight: 700; }
.brand-content h1 { margin: 0 0 18px; font-size: 32px; font-weight: 500; }
.brand-content p { max-width: 360px; color: rgb(255 255 255 / 78%); line-height: 1.8; }
.brand-content ul { margin: 44px 0 0; padding: 0; list-style: none; }
.brand-content li { margin: 18px 0; color: rgb(255 255 255 / 86%); }
.brand-content li::before { content: '✓'; margin-right: 12px; color: #8fd3ff; }
.copyright { position: absolute; z-index: 1; bottom: 32px; left: 48px; color: rgb(255 255 255 / 62%); font-size: 13px; }
.form-side { display: grid; place-items: center; padding: 38px 64px; }
.form-box { width: 100%; max-width: 540px; }
.form-box h2 { margin: 0 0 22px; color: #075aa7; text-align: center; font-size: 30px; }
.register-steps { margin-bottom: 28px; background: #f6f9fd; }
.form-box :deep(.el-form-item) { margin-bottom: 17px; }
.form-box :deep(.el-input__wrapper) { min-height: 44px; border-radius: 2px; }
.submit-button { width: 100%; height: 48px; border-radius: 2px; background: #075aa7; }
.button-row { display: flex; justify-content: flex-end; gap: 10px; }
.button-row .el-button { min-width: 110px; height: 44px; }
.switch-entry { margin-top: 22px; color: #808894; text-align: center; }
.switch-entry a { color: #075aa7; text-decoration: none; }
@media (max-height: 800px) { .auth-page { padding: 16px 28px; } .auth-panel, .brand-side { min-height: calc(100vh - 32px); } }
</style>
