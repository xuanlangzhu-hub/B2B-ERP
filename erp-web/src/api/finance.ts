import request from '@/utils/request'

export function getReceipts(params: any) { return request.get('/receipts', { params }) }
export function createReceipt(data: any) { return request.post('/receipts', data) }
export function confirmReceipt(id: number) { return request.post(`/receipts/${id}/confirm`) }
export function cancelReceipt(id: number) { return request.post(`/receipts/${id}/cancel`) }

export function getPayments(params: any) { return request.get('/payments', { params }) }
export function createPayment(data: any) { return request.post('/payments', data) }
export function confirmPayment(id: number) { return request.post(`/payments/${id}/confirm`) }
export function cancelPayment(id: number) { return request.post(`/payments/${id}/cancel`) }

export function getCapitalFlows(params: any) { return request.get('/capital-flows', { params }) }

export function getAccounts(params: any) { return request.get('/accounts', { params }) }
export function getAccountOptions() { return request.get('/accounts/options') }
export function createAccount(data: any) { return request.post('/accounts', data) }
export function updateAccount(id: number, data: any) { return request.put(`/accounts/${id}`, data) }
export function deleteAccount(id: number) { return request.delete(`/accounts/${id}`) }

export function getReceivables(params: any) { return request.get('/receivables', { params }) }
export function getPayables(params: any) { return request.get('/payables', { params }) }

export function getOtherTransactions(params: any) { return request.get('/other-transactions', { params }) }
export function createOtherTransaction(data: any) { return request.post('/other-transactions', data) }
export function updateOtherTransaction(id: number, data: any) { return request.put(`/other-transactions/${id}`, data) }
export function confirmOtherTransaction(id: number) { return request.post(`/other-transactions/${id}/confirm`) }
export function cancelOtherTransaction(id: number) { return request.post(`/other-transactions/${id}/cancel`) }
export function deleteOtherTransaction(id: number) { return request.delete(`/other-transactions/${id}`) }
