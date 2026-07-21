import request from '@/utils/request'

export function getSalesReport(params: any) {
  return request.get('/reports/sales', { params })
}

export function getSalesReportSummary(params: any) {
  return request.get('/reports/sales/summary', { params })
}

export function getPurchaseReport(params: any) {
  return request.get('/reports/purchase', { params })
}

export function getPurchaseReportSummary(params: any) {
  return request.get('/reports/purchase/summary', { params })
}

export function getInventoryReport(params: any) {
  return request.get('/reports/inventory', { params })
}

export function getInventoryReportSummary(params: any) {
  return request.get('/reports/inventory/summary', { params })
}

export function getInventoryMovementReport(params: any) {
  return request.get('/reports/inventory/movements', { params })
}

export function getInventoryMovementSummary(params: any) {
  return request.get('/reports/inventory/movements/summary', { params })
}

export function getCustomerStatements(params: any) { return request.get('/reports/finance/customers', { params }) }
export function getCustomerStatementSummary(params: any) { return request.get('/reports/finance/customers/summary', { params }) }
export function getCustomerLedger(params: any) { return request.get('/reports/finance/customers/ledger', { params }) }
export function getSupplierStatements(params: any) { return request.get('/reports/finance/suppliers', { params }) }
export function getSupplierStatementSummary(params: any) { return request.get('/reports/finance/suppliers/summary', { params }) }
export function getSupplierLedger(params: any) { return request.get('/reports/finance/suppliers/ledger', { params }) }
