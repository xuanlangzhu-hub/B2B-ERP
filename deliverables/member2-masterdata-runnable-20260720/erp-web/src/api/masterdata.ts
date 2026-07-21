import request from '@/utils/request'

export function getProducts(params: any) {
  return request.get('/products', { params })
}

export function getProductOptions() {
  return request.get('/products/options')
}

export function createProduct(data: any) {
  return request.post('/products', data)
}

export function updateProduct(id: number, data: any) {
  return request.put(`/products/${id}`, data)
}

export function deleteProduct(id: number) {
  return request.delete(`/products/${id}`)
}

export function getCategories(params?: any) {
  return request.get('/product-categories', { params })
}

export function getCategoryOptions() {
  return request.get('/product-categories/options')
}

export function createCategory(data: any) {
  return request.post('/product-categories', data)
}

export function updateCategory(id: number, data: any) {
  return request.put(`/product-categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete(`/product-categories/${id}`)
}

export function getUnits(params?: any) {
  return request.get('/units', { params })
}

export function getUnitOptions() {
  return request.get('/units/options')
}

export function createUnit(data: any) {
  return request.post('/units', data)
}

export function updateUnit(id: number, data: any) {
  return request.put(`/units/${id}`, data)
}

export function deleteUnit(id: number) {
  return request.delete(`/units/${id}`)
}

export function getCustomers(params: any) {
  return request.get('/customers', { params })
}

export function getCustomerOptions() {
  return request.get('/customers/options')
}

export function createCustomer(data: any) {
  return request.post('/customers', data)
}

export function updateCustomer(id: number, data: any) {
  return request.put(`/customers/${id}`, data)
}

export function deleteCustomer(id: number) {
  return request.delete(`/customers/${id}`)
}

export function getSuppliers(params: any) {
  return request.get('/suppliers', { params })
}

export function getSupplierOptions() {
  return request.get('/suppliers/options')
}

export function createSupplier(data: any) {
  return request.post('/suppliers', data)
}

export function updateSupplier(id: number, data: any) {
  return request.put(`/suppliers/${id}`, data)
}

export function deleteSupplier(id: number) {
  return request.delete(`/suppliers/${id}`)
}

export function getWarehouses(params: any) {
  return request.get('/warehouses', { params })
}

export function getWarehouseOptions() {
  return request.get('/warehouses/options')
}

export function createWarehouse(data: any) {
  return request.post('/warehouses', data)
}

export function updateWarehouse(id: number, data: any) {
  return request.put(`/warehouses/${id}`, data)
}

export function deleteWarehouse(id: number) {
  return request.delete(`/warehouses/${id}`)
}
