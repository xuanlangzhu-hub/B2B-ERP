<template>
  <div class="main-layout">
    <el-container>
      <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
        <div class="logo">
          <span v-if="!collapsed">B2B进销存ERP</span>
          <span v-else>ERP</span>
        </div>
        <el-menu :default-active="activeMenu" :collapse="collapsed" router background-color="#304156"
          text-color="#bfcbd9" active-text-color="#409EFF">
          <template v-for="menu in menuList" :key="menu.id">
            <el-sub-menu v-if="menu.children && menu.children.length" :index="menuPath(menu.routePath || menu.id)">
              <template #title>
                <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
                <span>{{ menu.menuName }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.id" :index="menuPath(menu.routePath, child.routePath)">
                {{ child.menuName }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menuPath(menu.routePath)">
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="collapsed = !collapsed">
              <Fold v-if="!collapsed" /><Expand v-else />
            </el-icon>
          </div>
          <div class="header-right">
            <span class="user-name">{{ userStore.userInfo?.realName }}</span>
            <el-button text @click="userStore.logout()">退出</el-button>
          </div>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserMenus } from '@/api/index'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)

const menuList = ref<any[]>([])
onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  await loadMenus()
})

function menuPath(...parts: Array<string | number | null | undefined>) {
  const path = parts
    .filter((part) => part !== null && part !== undefined && String(part).length > 0)
    .map((part) => String(part).replace(/^\/+|\/+$/g, ''))
    .filter(Boolean)
    .join('/')
  return `/${path}`
}

function hasPage(...parts: Array<string | number | null | undefined>) {
  return router.resolve(menuPath(...parts)).name !== undefined
}

async function loadMenus() {
  try {
    const res = await getUserMenus()
    const menus = res.data || []
    const tree: any[] = []
    const map = new Map<number, any>()
    menus.forEach((m: any) => {
      m.children = []
      map.set(m.id, m)
    })
    menus.forEach((m: any) => {
      if (m.parentId && map.has(m.parentId)) {
        map.get(m.parentId).children.push(m)
      } else if (!m.parentId || m.parentId === 0) {
        tree.push(m)
      }
    })
    tree.forEach((menu: any) => {
      menu.children = menu.children.filter((child: any) => hasPage(menu.routePath, child.routePath))
    })
    menuList.value = tree.filter((menu: any) => menu.children.length > 0 || hasPage(menu.routePath))
  } catch { /* ignore */ }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  min-height: 0;
  overflow: hidden;
}
.main-layout > .el-container {
  height: 100%;
  min-width: 0;
}
.main-layout > .el-container > .el-container {
  height: 100%;
  min-width: 0;
}
.sidebar {
  height: 100%;
  background: #304156;
  transition: width .3s;
  overflow-x: hidden;
  overflow-y: auto;
  flex-shrink: 0;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}
.header-left { display: flex; align-items: center; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-name { color: #666; }
.el-main {
  height: calc(100vh - 60px);
  min-width: 0;
  background: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
</style>
