<template>
  <div class="main-layout">
    <aside class="primary-sidebar">
      <div class="brand-mark">ERP</div>
      <nav class="module-nav">
        <button
          v-for="menu in menuList"
          :key="menu.id"
          type="button"
          class="module-item"
          :class="{ active: activeRootId === menu.id }"
          @click="selectRoot(menu)"
        >
          <el-icon :size="21"><component :is="menu.icon || 'Grid'" /></el-icon>
          <span>{{ moduleLabel(menu.menuName) }}</span>
        </button>
      </nav>
    </aside>

    <aside class="secondary-sidebar">
      <div class="secondary-title">{{ moduleLabel(activeRoot?.menuName) }}</div>
      <el-menu class="secondary-menu" :default-active="activeMenu" router>
        <template v-if="activeRoot?.children?.length">
          <el-menu-item
            v-for="child in activeRoot.children"
            :key="child.id"
            :index="menuPath(activeRoot.routePath, child.routePath)"
          >
            <span>{{ child.menuName }}</span>
          </el-menu-item>
        </template>
        <el-menu-item v-else-if="activeRoot" :index="menuPath(activeRoot.routePath)">
          <span>{{ activeRoot.menuName }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="page-shell">
      <header class="top-header">
        <div class="welcome-text">欢迎登录{{ currentStoreName ? currentStoreName + '！' : 'ERP系统！' }}</div>
        <div class="header-right">
          <el-select
            v-if="stores.length"
            v-model="currentStore"
            class="store-select"
            placeholder="选择门店"
            @change="switchStore"
          >
            <el-option v-for="store in stores" :key="store.id" :label="store.storeName" :value="store.id" />
          </el-select>

          <el-popover placement="bottom" :width="360" trigger="click" @show="loadNotices">
            <template #reference>
              <el-badge :value="unread" :hidden="!unread" class="notice-badge">
                <el-button circle text><el-icon size="20"><Bell /></el-icon></el-button>
              </el-badge>
            </template>
            <div class="notice-head">
              <b>消息通知</b>
              <el-link v-if="canManageNotice" type="primary" @click="router.push('/system/notices')">管理通知</el-link>
            </div>
            <el-empty v-if="!notices.length" description="暂无通知" :image-size="60" />
            <div
              v-for="notice in notices"
              :key="notice.id"
              class="notice-item"
              :class="{ unread: !notice.read }"
              @click="readNotice(notice)"
            >
              <div class="notice-title">{{ notice.noticeTitle }}</div>
              <div class="notice-time">{{ notice.publishedAt }}</div>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="34">{{ (userStore.userInfo?.realName || 'U').slice(0, 1) }}</el-avatar>
              <span>{{ userStore.userInfo?.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-main">
        <div class="page-breadcrumb">
          <el-icon><HomeFilled /></el-icon>
          <span>{{ route.meta.title || activeRoot?.menuName || '首页' }}</span>
        </div>
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  changeDefaultStore,
  getAccessibleStores,
  getNotices,
  getUnreadNoticeCount,
  getUserMenus,
  markNoticeRead
} from '@/api'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const menuList = ref<any[]>([])
const activeRootId = ref<number>()
const stores = ref<any[]>([])
const currentStore = ref<number>()
const unread = ref(0)
const notices = ref<any[]>([])

const activeRoot = computed(() => menuList.value.find((menu) => menu.id === activeRootId.value))
const activeMenu = computed(() => {
  if (!activeRoot.value) return route.path
  const child = activeRoot.value.children?.find((item: any) => {
    const path = menuPath(activeRoot.value.routePath, item.routePath)
    return route.path === path || route.path.startsWith(`${path}/`)
  })
  return child ? menuPath(activeRoot.value.routePath, child.routePath) : menuPath(activeRoot.value.routePath)
})
const currentStoreName = computed(() => stores.value.find((store) => store.id === currentStore.value)?.storeName || '')
const canManageNotice = computed(() =>
  userStore.permissions.includes('*:*:*') || userStore.permissions.includes('system:notice:list')
)

const moduleLabels: Record<string, string> = {
  '基础资料': '资料',
  '销售管理': '销售',
  '采购管理': '采购',
  '库存管理': '库存',
  '资金管理': '资金',
  '报表中心': '报表',
  '系统设置': '设置'
}

function moduleLabel(name?: string) {
  if (!name) return '首页'
  return moduleLabels[name] || name
}

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) await userStore.fetchUserInfo()
  await Promise.all([loadMenus(), loadStores(), loadUnread()])
})

watch(() => route.path, syncActiveRoot)

function menuPath(...parts: Array<string | number | null | undefined>) {
  const path = parts
    .filter((part) => part !== null && part !== undefined)
    .map((part) => String(part).replace(/^\/+|\/+$/g, ''))
    .filter(Boolean)
    .join('/')
  return `/${path}`
}

function hasPage(...parts: Array<string | number | null | undefined>) {
  return router.resolve(menuPath(...parts)).name !== undefined
}

async function loadMenus() {
  const menus = (await getUserMenus()).data || []
  const map = new Map<number, any>()
  const tree: any[] = []
  menus.forEach((menu: any) => map.set(menu.id, { ...menu, children: [] }))
  map.forEach((menu: any) => {
    if (menu.parentId && map.has(menu.parentId)) map.get(menu.parentId).children.push(menu)
    else tree.push(menu)
  })
  tree.forEach((menu) => {
    menu.children = menu.children.filter((child: any) => hasPage(menu.routePath, child.routePath))
  })
  menuList.value = tree.filter((menu) => menu.children.length || hasPage(menu.routePath))
  syncActiveRoot()
}

function syncActiveRoot() {
  const matched = menuList.value.find((menu) => {
    const rootPath = menuPath(menu.routePath)
    return route.path === rootPath || route.path.startsWith(`${rootPath}/`)
  })
  if (matched) activeRootId.value = matched.id
}

function selectRoot(menu: any) {
  activeRootId.value = menu.id
  if (menu.children?.length) {
    const firstChild = menu.children[0]
    router.push(menuPath(menu.routePath, firstChild.routePath))
  } else {
    router.push(menuPath(menu.routePath))
  }
}

async function loadStores() {
  stores.value = (await getAccessibleStores()).data || []
  currentStore.value = userStore.userInfo?.defaultStoreId || stores.value[0]?.id
}

async function switchStore(id: number) {
  await changeDefaultStore(id)
  await userStore.fetchUserInfo()
  ElMessage.success('已切换门店')
}

async function loadUnread() {
  unread.value = (await getUnreadNoticeCount()).data?.count || 0
}

async function loadNotices() {
  notices.value = (await getNotices({ page: 1, size: 6 })).data?.records || []
}

async function readNotice(notice: any) {
  if (!notice.read) {
    await markNoticeRead(notice.id)
    notice.read = true
    await loadUnread()
  }
  ElMessage.info(notice.noticeContent)
}

function handleCommand(command: string) {
  if (command === 'profile') router.push('/profile')
  else userStore.logout()
}
</script>

<style scoped>
.main-layout { display: flex; width: 100%; height: 100vh; min-height: 0; overflow: hidden; background: #f4f7fd; }
.primary-sidebar { width: 64px; flex: 0 0 64px; height: 100%; overflow-y: auto; overflow-x: hidden; background: #151b3b; color: #fff; }
.brand-mark { width: 42px; height: 42px; margin: 12px auto 14px; display: grid; place-items: center; border-radius: 50%; background: #fff; color: #2489f6; font-size: 12px; font-weight: 700; }
.module-nav { display: flex; flex-direction: column; }
.module-item { width: 64px; min-height: 72px; padding: 9px 2px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; border: 0; color: #fff; background: transparent; cursor: pointer; font: inherit; font-size: 13px; }
.module-item:hover { background: rgb(255 255 255 / 8%); }
.module-item.active { background: #1687f8; }
.secondary-sidebar { width: 176px; flex: 0 0 176px; height: 100%; overflow-y: auto; overflow-x: hidden; background: #fff; border-right: 1px solid #edf0f5; }
.secondary-title { height: 64px; display: flex; align-items: center; padding: 0 22px; color: #4b4f58; font-size: 16px; border-bottom: 1px solid #f0f1f4; }
.secondary-menu { border-right: 0; padding-top: 8px; }
.secondary-menu :deep(.el-menu-item) { height: 44px; margin: 0; padding-left: 30px !important; color: #555b66; }
.secondary-menu :deep(.el-menu-item.is-active) { color: #1687f8; background: #eef6ff; border-right: 3px solid #1687f8; }
.page-shell { min-width: 0; flex: 1; height: 100%; display: flex; flex-direction: column; }
.top-header { height: 64px; flex: 0 0 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: #fff; border-bottom: 1px solid #eef0f4; }
.welcome-text { color: #30343b; font-size: 18px; }
.header-right, .user-entry { display: flex; align-items: center; }
.header-right { gap: 16px; }
.user-entry { gap: 8px; color: #454a53; cursor: pointer; }
.store-select { width: 180px; }
.notice-badge { display: flex; }
.content-main { flex: 1; min-height: 0; padding: 16px 20px 24px; overflow: auto; background: #f4f7fd; }
.page-breadcrumb { height: 28px; margin-bottom: 10px; display: flex; align-items: center; gap: 7px; color: #1687f8; font-size: 14px; }
.notice-head { display: flex; justify-content: space-between; padding-bottom: 10px; border-bottom: 1px solid #eee; }
.notice-item { padding: 11px 4px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.notice-item.unread .notice-title { font-weight: 600; color: #303133; }
.notice-item.unread .notice-title::before { content: ''; display: inline-block; width: 6px; height: 6px; margin-right: 7px; border-radius: 50%; background: #409eff; }
.notice-title { color: #606266; }
.notice-time { margin-top: 5px; color: #a8abb2; font-size: 12px; }
@media (max-width: 1280px) {
  .secondary-sidebar { width: 156px; flex-basis: 156px; }
  .store-select { width: 150px; }
  .welcome-text { font-size: 16px; }
}
</style>
