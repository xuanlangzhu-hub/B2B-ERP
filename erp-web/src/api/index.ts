import request from '@/utils/request'

export function login(username: string, password: string) {
  return request.post('/auth/login', { username, password })
}

export function registerAccount(data: any) {
  return request.post('/auth/register', data)
}

export function getCurrentUser() {
  return request.get('/auth/me')
}
export const getProfile = () => request.get('/auth/profile')
export const updateProfile = (data: any) => request.put('/auth/profile', data)
export const changePassword = (data: any) => request.put('/auth/password', data)
export const getAccessibleStores = () => request.get('/auth/stores')
export const changeDefaultStore = (storeId: number) => request.put(`/auth/default-store/${storeId}`)

// Users
export function getUsers(params: any) {
  return request.get('/users', { params })
}

export function getUser(id: number) {
  return request.get(`/users/${id}`)
}

export function createUser(data: any) {
  return request.post('/users', data)
}

export function updateUser(id: number, data: any) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function resetPassword(id: number) {
  return request.post(`/users/${id}/reset-password`)
}

export function updateUserStatus(id: number, status: string) {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function getUserRoles(id: number) {
  return request.get(`/users/${id}/roles`)
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return request.put(`/users/${id}/roles`, roleIds)
}

// Roles
export function getRoles() {
  return request.get('/roles')
}

export function createRole(data: any) {
  return request.post('/roles', data)
}

export function updateRole(id: number, data: any) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}

// Menus
export function getAllMenus() {
  return request.get('/menus/all')
}

export function getUserMenus() {
  return request.get('/menus')
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return request.put(`/roles/${id}/menus`, menuIds)
}
export const getRoleMenus = (id: number) => request.get(`/roles/${id}/menus`)

// Logs
export function getLogs(params: any) {
  return request.get('/operation-logs', { params })
}
export const getEnterprise = () => request.get('/enterprise/current')
export const updateEnterprise = (data: any) => request.put('/enterprise/current', data)
export const getNotices = (params: any) => request.get('/notices', { params })
export const getUnreadNoticeCount = () => request.get('/notices/unread-count')
export const markNoticeRead = (id: number) => request.post(`/notices/${id}/read`)
export const getNoticeManage = (params: any) => request.get('/notices/manage', { params })
export const createNotice = (data: any) => request.post('/notices', data)
export const updateNotice = (id: number, data: any) => request.put(`/notices/${id}`, data)
export const publishNotice = (id: number) => request.post(`/notices/${id}/publish`)
export const deleteNotice = (id: number) => request.delete(`/notices/${id}`)
