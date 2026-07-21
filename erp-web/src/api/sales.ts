import request from '@/utils/request'

export function getSalesOrders(params: any) { return request.get('/sales-orders', { params }) }
export function getSalesOrder(id: number) { return request.get(`/sales-orders/${id}`) }
export function createSalesOrder(data: any) { return request.post('/sales-orders', data) }
export function updateSalesOrder(id: number, data: any) { return request.put(`/sales-orders/${id}`, data) }
export function deleteSalesOrder(id: number) { return request.delete(`/sales-orders/${id}`) }
export function approveSalesOrder(id: number) { return request.post(`/sales-orders/${id}/approve`) }
export function cancelSalesOrder(id: number) { return request.post(`/sales-orders/${id}/cancel`) }
export function getOutboundOptions() { return request.get('/sales-orders/outbound-options') }
export function getReceipts(params: any) { return request.get('/receipts', { params }) }
export function createReceipt(data: any) { return request.post('/receipts', data) }
export function getSalesReturns(params: any) { return request.get('/sales-returns', { params }) }
export function getSalesReturn(id: number) { return request.get(`/sales-returns/${id}`) }
export function createSalesReturn(data: any) { return request.post('/sales-returns', data) }
export function approveSalesReturn(id: number) { return request.post(`/sales-returns/${id}/approve`) }
export function cancelSalesReturn(id: number) { return request.post(`/sales-returns/${id}/cancel`) }
