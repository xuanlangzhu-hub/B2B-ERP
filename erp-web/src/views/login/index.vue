<template>
  <div class="auth-page">
    <div class="auth-panel">
      <section class="brand-side">
        <div class="brand-shade"></div>
        <div class="brand-content">
          <div class="brand-logo">B2B</div>
          <h1>云进销存连锁版</h1>
          <p>销售 · 采购 · 库存 · 资金一体化管理</p>
        </div>
        <div class="copyright">© 2026 B2B ERP 实训项目</div>
      </section>

      <section class="form-side">
        <div class="form-box">
          <h2>欢迎登录</h2>
          <p class="form-subtitle">登录您的企业工作台</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
            <el-form-item label="账号" prop="username">
              <el-input v-model.trim="form.username" placeholder="请输入注册邮箱或用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <div class="form-tools">
              <span>忘记密码请联系企业管理员</span>
            </div>
            <el-button class="submit-button" type="primary" :loading="loading" @click="handleLogin">登录</el-button>
          </el-form>
          <div class="switch-entry">没有账号？<router-link to="/register">现在去注册</router-link></div>
          <div class="demo-account">演示账号：admin　密码：Admin@123456</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: String(route.query.username || 'admin'),
  password: route.query.username ? '' : 'Admin@123456'
})
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: grid; place-items: center; padding: 32px; background: linear-gradient(rgb(11 34 68 / 52%), rgb(11 34 68 / 52%)), url('@/assets/images/login-city.png') center / cover no-repeat; }
.auth-panel { width: min(1180px, calc(100vw - 80px)); min-height: 660px; display: grid; grid-template-columns: 52% 48%; overflow: hidden; background: #fff; box-shadow: 0 22px 70px rgb(6 25 52 / 38%); }
.brand-side { position: relative; min-height: 660px; overflow: hidden; color: #fff; background: url('@/assets/images/login-city.png') center / cover no-repeat; }
.brand-shade { position: absolute; inset: 0; background: linear-gradient(145deg, rgb(8 73 146 / 92%), rgb(9 63 126 / 72%)); }
.brand-content { position: relative; z-index: 1; padding: 58px 54px; }
.brand-logo { width: 70px; height: 70px; display: grid; place-items: center; margin-bottom: 24px; border: 2px solid rgb(255 255 255 / 78%); border-radius: 18px; font-size: 24px; font-weight: 700; letter-spacing: 2px; }
.brand-content h1 { margin: 0 0 16px; font-size: 38px; font-weight: 500; letter-spacing: 2px; }
.brand-content p { color: rgb(255 255 255 / 76%); font-size: 16px; letter-spacing: 1px; }
.copyright { position: absolute; z-index: 1; bottom: 34px; left: 54px; color: rgb(255 255 255 / 62%); font-size: 13px; }
.form-side { display: grid; place-items: center; padding: 52px 76px; }
.form-box { width: 100%; max-width: 400px; }
.form-box h2 { margin: 0; color: #075aa7; text-align: center; font-size: 32px; font-weight: 600; }
.form-subtitle { margin: 10px 0 40px; color: #a1a8b3; text-align: center; }
.form-box :deep(.el-form-item__label) { color: #5d6673; }
.form-box :deep(.el-input__wrapper) { min-height: 48px; border-radius: 2px; box-shadow: 0 0 0 1px #dce2ea inset; }
.form-tools { margin: -5px 0 22px; color: #9aa3af; text-align: right; font-size: 13px; }
.submit-button { width: 100%; height: 50px; border-radius: 2px; background: #075aa7; font-size: 17px; }
.switch-entry { margin-top: 22px; color: #808894; text-align: center; }
.switch-entry a { color: #075aa7; text-decoration: none; }
.demo-account { margin-top: 34px; padding: 12px; color: #8a94a3; text-align: center; font-size: 12px; background: #f7f9fc; }
@media (max-width: 1200px) { .auth-panel { grid-template-columns: 46% 54%; } .form-side { padding: 44px 56px; } }
@media (max-height: 800px) { .auth-page { padding: 16px 32px; } }
</style>
