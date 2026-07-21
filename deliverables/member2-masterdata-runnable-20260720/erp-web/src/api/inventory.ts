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
