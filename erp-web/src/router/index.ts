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
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
      meta: { title: '注册', public: true }
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
          path: 'master/stores',
          name: 'Stores',
          component: () => import('@/views/master/store/index.vue'),
          meta: { title: '门店管理' }
        },
        {
          path: 'master/product-categories',
          name: 'ProductCategories',
          component: () => import('@/views/master/config/index.vue'),
          meta: { title: '商品分类', configType: 'productCategory' }
        },
        {
          path: 'master/units',
          name: 'ProductUnits',
          component: () => import('@/views/master/config/index.vue'),
          meta: { title: '商品单位', configType: 'unit' }
        },
        {
          path: 'master/customer-categories',
          name: 'CustomerCategories',
          component: () => import('@/views/master/config/index.vue'),
          meta: { title: '客户分类', configType: 'customerCategory' }
        },
        {
          path: 'master/customer-levels',
          name: 'CustomerLevels',
          component: () => import('@/views/master/config/index.vue'),
          meta: { title: '客户等级', configType: 'customerLevel' }
        },
        {
          path: 'master/supplier-categories',
          name: 'SupplierCategories',
          component: () => import('@/views/master/config/index.vue'),
          meta: { title: '供应商分类', configType: 'supplierCategory' }
        },
        {
          path: 'master/product-attributes',
          name: 'ProductAttributes',
          component: () => import('@/views/master/metadata/index.vue'),
          meta: { title: '商品属性', metadataType: 'attribute' }
        },
        {
          path: 'master/product-tags',
          name: 'ProductTags',
          component: () => import('@/views/master/metadata/index.vue'),
          meta: { title: '商品标签', metadataType: 'productTag' }
        },
        {
          path: 'master/customer-tags',
          name: 'CustomerTags',
          component: () => import('@/views/master/metadata/index.vue'),
          meta: { title: '客户标签', metadataType: 'customerTag' }
        },
        {
          path: 'sales/orders',
          name: 'SalesOrders',
          component: () => import('@/views/sales/order/index.vue'),
          meta: { title: '销售单' }
        },
        {
          path: 'sales/orders/new', name: 'SalesOrderCreate', component: () => import('@/views/order/OrderFormPage.vue'),
          meta: { title: '新增销售单', businessType: 'sales' }
        },
        {
          path: 'sales/orders/:id/edit', name: 'SalesOrderEdit', component: () => import('@/views/order/OrderFormPage.vue'),
          meta: { title: '编辑销售单', businessType: 'sales' }
        },
        {
          path: 'sales/orders/:id', name: 'SalesOrderDetail', component: () => import('@/views/order/OrderDetailPage.vue'),
          meta: { title: '销售单详情', businessType: 'sales' }
        },
        {
          path: 'sales/returns',
          name: 'SalesReturns',
          component: () => import('@/views/sales/return/index.vue'),
          meta: { title: '销售退货申请单' }
        },
        { path: 'sales/returns/new', name: 'SalesReturnCreate', component: () => import('@/views/order/ReturnFormPage.vue'), meta: { title: '新增销售退货申请', businessType: 'sales' } },
        { path: 'sales/returns/:id', name: 'SalesReturnDetail', component: () => import('@/views/order/ReturnDetailPage.vue'), meta: { title: '销售退货详情', businessType: 'sales' } },
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
          path: 'purchase/orders/new', name: 'PurchaseOrderCreate', component: () => import('@/views/order/OrderFormPage.vue'),
          meta: { title: '新增采购单', businessType: 'purchase' }
        },
        {
          path: 'purchase/orders/:id/edit', name: 'PurchaseOrderEdit', component: () => import('@/views/order/OrderFormPage.vue'),
          meta: { title: '编辑采购单', businessType: 'purchase' }
        },
        {
          path: 'purchase/orders/:id', name: 'PurchaseOrderDetail', component: () => import('@/views/order/OrderDetailPage.vue'),
          meta: { title: '采购单详情', businessType: 'purchase' }
        },
        {
          path: 'purchase/returns',
          name: 'PurchaseReturns',
          component: () => import('@/views/purchase/return/index.vue'),
          meta: { title: '采购退货申请单' }
        },
        { path: 'purchase/returns/new', name: 'PurchaseReturnCreate', component: () => import('@/views/order/ReturnFormPage.vue'), meta: { title: '新增采购退货申请', businessType: 'purchase' } },
        { path: 'purchase/returns/:id', name: 'PurchaseReturnDetail', component: () => import('@/views/order/ReturnDetailPage.vue'), meta: { title: '采购退货详情', businessType: 'purchase' } },
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
          path: 'inventory/counts',
          name: 'InventoryCounts',
          component: () => import('@/views/inventory/count/index.vue'),
          meta: { title: '库存盘点' }
        },
        {
          path: 'inventory/counts/new',
          name: 'InventoryCountCreate',
          component: () => import('@/views/inventory/count/form.vue'),
          meta: { title: '新增盘点单' }
        },
        {
          path: 'inventory/counts/:id/edit',
          name: 'InventoryCountEdit',
          component: () => import('@/views/inventory/count/form.vue'),
          meta: { title: '录入实盘数量' }
        },
        {
          path: 'inventory/counts/:id',
          name: 'InventoryCountDetail',
          component: () => import('@/views/inventory/count/detail.vue'),
          meta: { title: '盘点单详情' }
        },
        {
          path: 'inventory/transfers',
          name: 'InventoryTransfers',
          component: () => import('@/views/inventory/transfer/index.vue'),
          meta: { title: '库存调拨' }
        },
        {
          path: 'inventory/transfers/new',
          name: 'InventoryTransferCreate',
          component: () => import('@/views/inventory/transfer/form.vue'),
          meta: { title: '新增调拨单' }
        },
        {
          path: 'inventory/transfers/:id',
          name: 'InventoryTransferDetail',
          component: () => import('@/views/inventory/transfer/detail.vue'),
          meta: { title: '调拨单详情' }
        },
        {
          path: 'inventory/adjustments',
          name: 'InventoryAdjustments',
          component: () => import('@/views/inventory/adjustment/index.vue'),
          meta: { title: '商品报损与调整' }
        },
        { path: 'inventory/adjustments/new', name: 'InventoryAdjustmentCreate', component: () => import('@/views/inventory/adjustment/form.vue'), meta: { title: '新增库存调整' } },
        { path: 'inventory/adjustments/:id', name: 'InventoryAdjustmentDetail', component: () => import('@/views/inventory/adjustment/detail.vue'), meta: { title: '库存调整详情' } },
        {
          path: 'inventory/borrow-outs',
          name: 'InventoryBorrowOuts',
          component: () => import('@/views/inventory/borrow/index.vue'),
          meta: { title: '借出管理', borrowType: 'BORROW_OUT' }
        },
        { path: 'inventory/borrow-outs/new', name: 'InventoryBorrowOutCreate', component: () => import('@/views/inventory/borrow/form.vue'), meta: { title: '新增借出单', borrowType: 'BORROW_OUT' } },
        { path: 'inventory/borrow-outs/:id', name: 'InventoryBorrowOutDetail', component: () => import('@/views/inventory/borrow/detail.vue'), meta: { title: '借出详情', borrowType: 'BORROW_OUT' } },
        {
          path: 'inventory/borrow-ins',
          name: 'InventoryBorrowIns',
          component: () => import('@/views/inventory/borrow/index.vue'),
          meta: { title: '借入管理', borrowType: 'BORROW_IN' }
        },
        { path: 'inventory/borrow-ins/new', name: 'InventoryBorrowInCreate', component: () => import('@/views/inventory/borrow/form.vue'), meta: { title: '新增借入单', borrowType: 'BORROW_IN' } },
        { path: 'inventory/borrow-ins/:id', name: 'InventoryBorrowInDetail', component: () => import('@/views/inventory/borrow/detail.vue'), meta: { title: '借入详情', borrowType: 'BORROW_IN' } },
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
          path: 'finance/other-transactions',
          name: 'FinanceOtherTransactions',
          component: () => import('@/views/finance/other/index.vue'),
          meta: { title: '其他收支' }
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
          path: 'reports/finance',
          name: 'ReportsFinance',
          component: () => import('@/views/report/finance/index.vue'),
          meta: { title: '财务报表' }
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
        },
        {
          path: 'system/enterprise',
          name: 'SystemEnterprise',
          component: () => import('@/views/system/enterprise/index.vue'),
          meta: { title: '企业信息' }
        },
        {
          path: 'system/notices',
          name: 'SystemNotices',
          component: () => import('@/views/system/notice/index.vue'),
          meta: { title: '消息通知' }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/system/profile/index.vue'),
          meta: { title: '个人信息' }
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
  const publicRoute = to.path === '/login' || to.meta.public === true
  if (!publicRoute && !token) {
    next('/login')
  } else if (publicRoute && token) {
    next('/')
  } else {
    next()
  }
})

export default router
