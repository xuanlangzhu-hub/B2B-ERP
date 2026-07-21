import request from '@/utils/request'

export function getInbounds(params: any) { return request.get('/inbounds', { params }) }
export function getInbound(id: number) { return request.get(`/inbounds/${id}`) }
export function createInboundFromPurchase(purchaseOrderId: number) { return request.post(`/inbounds/from-purchase/${purchaseOrderId}`) }
export function createInboundFromSalesReturn(returnId: number) { return request.post(`/inbounds/from-sales-return/${returnId}`) }
export function confirmInbound(id: number) { return request.post(`/inbounds/${id}/confirm`) }
export function getOutbounds(params: any) { return request.get('/outbounds', { params }) }
export function getOutbound(id: number) { return request.get(`/outbounds/${id}`) }
export function createOutboundFromSales(salesOrderId: number) { return request.post(`/outbounds/from-sales/${salesOrderId}`) }
export function createOutboundFromPurchaseReturn(returnId: number) { return request.post(`/outbounds/from-purchase-return/${returnId}`) }
export function confirmOutbound(id: number) { return request.post(`/outbounds/${id}/confirm`) }
export function getStocks(params: any) { return request.get('/stocks', { params }) }
export function getStockMovements(params: any) { return request.get('/stock-movements', { params }) }
export function cancelInbound(id: number) { return request.post(`/inbounds/${id}/cancel`) }
export function cancelOutbound(id: number) { return request.post(`/outbounds/${id}/cancel`) }
export function getDashboardSummary() { return request.get('/dashboard/summary') }

export function getInventoryCounts(params: any) { return request.get('/inventory-counts', { params }) }
export function getInventoryCount(id: number) { return request.get(`/inventory-counts/${id}`) }
export function createInventoryCount(data: any) { return request.post('/inventory-counts', data) }
export function updateInventoryCount(id: number, data: any) { return request.put(`/inventory-counts/${id}`, data) }
export function startInventoryCount(id: number) { return request.post(`/inventory-counts/${id}/start`) }
export function submitInventoryCount(id: number) { return request.post(`/inventory-counts/${id}/submit`) }
export function approveInventoryCount(id: number) { return request.post(`/inventory-counts/${id}/approve`) }
export function cancelInventoryCount(id: number) { return request.post(`/inventory-counts/${id}/cancel`) }
export function deleteInventoryCount(id: number) { return request.delete(`/inventory-counts/${id}`) }

export function getInventoryTransfers(params: any) { return request.get('/inventory-transfers', { params }) }
export function getInventoryTransfer(id: number) { return request.get(`/inventory-transfers/${id}`) }
export function createInventoryTransfer(data: any) { return request.post('/inventory-transfers', data) }
export function approveInventoryTransfer(id: number) { return request.post(`/inventory-transfers/${id}/approve`) }
export function completeInventoryTransfer(id: number) { return request.post(`/inventory-transfers/${id}/complete`) }
export function cancelInventoryTransfer(id: number) { return request.post(`/inventory-transfers/${id}/cancel`) }
export function deleteInventoryTransfer(id: number) { return request.delete(`/inventory-transfers/${id}`) }

export function getInventoryAdjustments(params: any) { return request.get('/inventory-adjustments', { params }) }
export function getInventoryAdjustment(id: number) { return request.get(`/inventory-adjustments/${id}`) }
export function createInventoryAdjustment(data: any) { return request.post('/inventory-adjustments', data) }
export function approveInventoryAdjustment(id: number) { return request.post(`/inventory-adjustments/${id}/approve`) }
export function cancelInventoryAdjustment(id: number) { return request.post(`/inventory-adjustments/${id}/cancel`) }
export function deleteInventoryAdjustment(id: number) { return request.delete(`/inventory-adjustments/${id}`) }

export function getInventoryBorrows(params: any) { return request.get('/inventory-borrows', { params }) }
export function getInventoryBorrow(id: number) { return request.get(`/inventory-borrows/${id}`) }
export function createInventoryBorrow(data: any) { return request.post('/inventory-borrows', data) }
export function approveInventoryBorrow(id: number) { return request.post(`/inventory-borrows/${id}/approve`) }
export function returnInventoryBorrow(id: number, data: any) { return request.post(`/inventory-borrows/${id}/return`, data) }
export function cancelInventoryBorrow(id: number) { return request.post(`/inventory-borrows/${id}/cancel`) }
export function deleteInventoryBorrow(id: number) { return request.delete(`/inventory-borrows/${id}`) }
