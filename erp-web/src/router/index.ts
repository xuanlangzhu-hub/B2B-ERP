import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '首页', icon: 'HomeFilled' }
        },
        {
          path: 'master/products',
          name: 'Products',
          component: () => import('@/views/master/product/index.vue'),
          meta: { title: '商品管理' }
        },
        {
          path: 'master/customers',
          name: 'Customers',
          component: () => import('@/views/master/customer/index.vue'),
          meta: { title: '客户管理' }
        },
        {
          path: 'master/suppliers',
          name: 'Suppliers',
          component: () => import('@/views/master/supplier/index.vue'),
          meta: { title: '供应商管理' }
        },
        {
          path: 'master/warehouses',
          name: 'Warehouses',
          component: () => import('@/views/master/warehouse/index.vue'),
          meta: { title: '仓库管理' }
        },
        {
          path: 'sales/orders',
          name: 'SalesOrders',
          component: () => import('@/views/sales/order/index.vue'),
          meta: { title: '销售单' }
        },
        {
          path: 'sales/returns',
          name: 'SalesReturns',
          component: () => import('@/views/sales/return/index.vue'),
          meta: { title: '销售退货申请单' }
        },
        {
          path: 'sales/receipts',
          name: 'SalesReceipts',
          component: () => import('@/views/sales/receipt/index.vue'),
          meta: { title: '收款单' }
        },
        {
          path: 'sales/report',
          name: 'SalesReport',
          component: () => import('@/views/sales/report/index.vue'),
          meta: { title: '销售报表' }
        },
        {
          path: 'purchase/orders',
          name: 'PurchaseOrders',
          component: () => import('@/views/purchase/order/index.vue'),
          meta: { title: '采购单' }
        },
        {
          path: 'purchase/returns',
          name: 'PurchaseReturns',
          component: () => import('@/views/purchase/return/index.vue'),
          meta: { title: '采购退货申请单' }
        },
        {
          path: 'purchase/payments',
          name: 'PurchasePayments',
          component: () => import('@/views/purchase/payment/index.vue'),
          meta: { title: '付款单' }
        },
        {
          path: 'purchase/report',
          name: 'PurchaseReport',
          component: () => import('@/views/purchase/report/index.vue'),
          meta: { title: '采购报表' }
        },
        {
          path: 'inventory/stock',
          name: 'InventoryStock',
          component: () => import('@/views/inventory/stock/index.vue'),
          meta: { title: '库存查询' }
        },
        {
          path: 'inventory/inbounds',
          name: 'InventoryInbounds',
          component: () => import('@/views/inventory/inbound/index.vue'),
          meta: { title: '入库管理' }
        },
        {
          path: 'inventory/outbounds',
          name: 'InventoryOutbounds',
          component: () => import('@/views/inventory/outbound/index.vue'),
          meta: { title: '出库管理' }
        },
        {
          path: 'inventory/movements',
          name: 'InventoryMovements',
          component: () => import('@/views/inventory/movement/index.vue'),
          meta: { title: '库存流水' }
        },
        {
          path: 'finance/receipts',
          name: 'FinanceReceipts',
          component: () => import('@/views/finance/receipt/index.vue'),
          meta: { title: '收款单' }
        },
        {
          path: 'finance/payments',
          name: 'FinancePayments',
          component: () => import('@/views/finance/payment/index.vue'),
          meta: { title: '付款单' }
        },
        {
          path: 'finance/flows',
          name: 'FinanceFlows',
          component: () => import('@/views/finance/flow/index.vue'),
          meta: { title: '资金流水' }
        },
        {
          path: 'finance/accounting',
          name: 'FinanceAccounting',
          component: () => import('@/views/finance/accounting/index.vue'),
          meta: { title: '账户与往来' }
        },
        {
          path: 'reports/sales',
          name: 'ReportsSales',
          component: () => import('@/views/report/sales/index.vue'),
          meta: { title: '销售报表' }
        },
        {
          path: 'reports/purchase',
          name: 'ReportsPurchase',
          component: () => import('@/views/report/purchase/index.vue'),
          meta: { title: '采购报表' }
        },
        {
          path: 'reports/inventory',
          name: 'ReportsInventory',
          component: () => import('@/views/report/inventory/index.vue'),
          meta: { title: '库存报表' }
        },
        {
          path: 'system/users',
          name: 'SystemUsers',
          component: () => import('@/views/system/user/index.vue'),
          meta: { title: '员工管理' }
        },
        {
          path: 'system/roles',
          name: 'SystemRoles',
          component: () => import('@/views/system/role/index.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'system/logs',
          name: 'SystemLogs',
          component: () => import('@/views/system/log/index.vue'),
          meta: { title: '操作日志' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
