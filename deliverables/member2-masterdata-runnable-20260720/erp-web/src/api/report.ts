import request from '@/utils/request'

export function getSalesReport(params: any) {
  return request.get('/reports/sales', { params })
}

export function getPurchaseReport(params: any) {
  return request.get('/reports/purchase', { params })
}

export function getInventoryReport(params: any) {
  return request.get('/reports/inventory', { params })
}
