import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getCurrentUser } from '@/api'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const permissions = ref<string[]>([])

  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName
    }
    await fetchUserInfo()
    router.push('/')
  }

  async function fetchUserInfo() {
    const res = await getCurrentUser()
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      enterpriseId: res.data.enterpriseId,
      defaultStoreId: res.data.defaultStoreId
    }
    permissions.value = res.data.permissions || []
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  return { token, userInfo, permissions, login, fetchUserInfo, logout }
})
