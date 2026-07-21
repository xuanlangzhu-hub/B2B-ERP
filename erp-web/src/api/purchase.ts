import request from '@/utils/request'

export function getPurchaseOrders(params: any) { return request.get('/purchase-orders', { params }) }
export function getPurchaseOrder(id: number) { return request.get(`/purchase-orders/${id}`) }
export function createPurchaseOrder(data: any) { return request.post('/purchase-orders', data) }
export function updatePurchaseOrder(id: number, data: any) { return request.put(`/purchase-orders/${id}`, data) }
export function deletePurchaseOrder(id: number) { return request.delete(`/purchase-orders/${id}`) }
export function approvePurchaseOrder(id: number) { return request.post(`/purchase-orders/${id}/approve`) }
export function cancelPurchaseOrder(id: number) { return request.post(`/purchase-orders/${id}/cancel`) }
export function getInboundOptions() { return request.get('/purchase-orders/inbound-options') }
export function getPayments(params: any) { return request.get('/payments', { params }) }
export function createPayment(data: any) { return request.post('/payments', data) }
export function getPurchaseReturns(params: any) { return request.get('/purchase-returns', { params }) }
export function getPurchaseReturn(id: number) { return request.get(`/purchase-returns/${id}`) }
export function createPurchaseReturn(data: any) { return request.post('/purchase-returns', data) }
export function approvePurchaseReturn(id: number) { return request.post(`/purchase-returns/${id}/approve`) }
export function cancelPurchaseReturn(id: number) { return request.post(`/purchase-returns/${id}/cancel`) }
