import request from '@/utils/request'

export function getProducts(params: any) {
  return request.get('/products', { params })
}

export function getProduct(id: number) { return request.get(`/products/${id}`) }

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

export function getCustomer(id: number) {
  return request.get(`/customers/${id}`)
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

export function getStores(params: any) { return request.get('/stores', { params }) }
export function getStoreOptions() { return request.get('/stores/options') }
export function createStore(data: any) { return request.post('/stores', data) }
export function updateStore(id: number, data: any) { return request.put(`/stores/${id}`, data) }
export function deleteStore(id: number) { return request.delete(`/stores/${id}`) }

export function getCustomerCategories(params: any) { return request.get('/customer-categories', { params }) }
export function getCustomerCategoryOptions() { return request.get('/customer-categories/options') }
export function createCustomerCategory(data: any) { return request.post('/customer-categories', data) }
export function updateCustomerCategory(id: number, data: any) { return request.put(`/customer-categories/${id}`, data) }
export function deleteCustomerCategory(id: number) { return request.delete(`/customer-categories/${id}`) }

export function getCustomerLevels(params: any) { return request.get('/customer-levels', { params }) }
export function getCustomerLevelOptions() { return request.get('/customer-levels/options') }
export function createCustomerLevel(data: any) { return request.post('/customer-levels', data) }
export function updateCustomerLevel(id: number, data: any) { return request.put(`/customer-levels/${id}`, data) }
export function deleteCustomerLevel(id: number) { return request.delete(`/customer-levels/${id}`) }

export function getSupplierCategories(params: any) { return request.get('/supplier-categories', { params }) }
export function getSupplierCategoryOptions() { return request.get('/supplier-categories/options') }
export function createSupplierCategory(data: any) { return request.post('/supplier-categories', data) }
export function updateSupplierCategory(id: number, data: any) { return request.put(`/supplier-categories/${id}`, data) }
export function deleteSupplierCategory(id: number) { return request.delete(`/supplier-categories/${id}`) }

export function getProductAttributes(params: any) { return request.get('/product-attributes', { params }) }
export function createProductAttribute(data: any) { return request.post('/product-attributes', data) }
export function updateProductAttribute(id: number, data: any) { return request.put(`/product-attributes/${id}`, data) }
export function deleteProductAttribute(id: number) { return request.delete(`/product-attributes/${id}`) }
export function getProductTags(params: any) { return request.get('/product-tags', { params }) }
export function createProductTag(data: any) { return request.post('/product-tags', data) }
export function updateProductTag(id: number, data: any) { return request.put(`/product-tags/${id}`, data) }
export function deleteProductTag(id: number) { return request.delete(`/product-tags/${id}`) }
export function getCustomerTags(params: any) { return request.get('/customer-tags', { params }) }
export function getCustomerTagOptions() { return request.get('/customer-tags/options') }
export function createCustomerTag(data: any) { return request.post('/customer-tags', data) }
export function updateCustomerTag(id: number, data: any) { return request.put(`/customer-tags/${id}`, data) }
export function deleteCustomerTag(id: number) { return request.delete(`/customer-tags/${id}`) }
export function getProductMetadataOptions() { return request.get('/product-metadata/options') }
