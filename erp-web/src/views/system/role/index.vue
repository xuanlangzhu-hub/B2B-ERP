<template>
  <div class="page-wrapper">
    <el-card><div class="toolbar"><el-button type="primary" @click="openAdd">新增角色</el-button></div></el-card>
    <el-card><el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="roleCode" label="角色编码" width="150"/><el-table-column prop="roleName" label="角色名称" width="150"/>
      <el-table-column prop="dataScope" label="数据范围" width="120"><template #default="{row}">{{ scopeNames[row.dataScope]||row.dataScope }}</template></el-table-column>
      <el-table-column prop="sortNo" label="排序" width="80"/><el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='ENABLED'?'success':'danger'">{{row.status==='ENABLED'?'启用':'禁用'}}</el-tag></template></el-table-column>
      <el-table-column prop="remark" label="备注"/><el-table-column label="操作" width="220" fixed="right"><template #default="{row}"><el-button size="small" @click="openEdit(row)">编辑</el-button><el-button size="small" type="primary" @click="openPermission(row)">权限</el-button><el-button size="small" type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table></el-card>
    <el-dialog v-model="dialogVisible" :title="editingId?'编辑角色':'新增角色'" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="角色编码" prop="roleCode"><el-input v-model="form.roleCode" :disabled="!!editingId"/></el-form-item><el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName"/></el-form-item>
      <el-form-item label="数据范围"><el-select v-model="form.dataScope" style="width:100%"><el-option v-for="(name,key) in scopeNames" :key="key" :label="name" :value="key"/></el-select></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0"/></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea"/></el-form-item>
    </el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
    <el-dialog v-model="permissionVisible" title="分配菜单权限" width="560px"><el-alert :title="`当前角色：${currentRole?.roleName||''}`" :closable="false" style="margin-bottom:14px"/><el-tree ref="treeRef" :data="menuTree" node-key="id" show-checkbox default-expand-all :props="{label:'menuName',children:'children'}"/><template #footer><el-button @click="permissionVisible=false">取消</el-button><el-button type="primary" @click="savePermission">保存权限</el-button></template></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref, nextTick } from 'vue'; import { ElMessage, ElMessageBox } from 'element-plus'; import { getRoles,createRole,updateRole,deleteRole,getAllMenus,getRoleMenus,assignRoleMenus } from '@/api'
const loading=ref(false),rows=ref<any[]>([]),dialogVisible=ref(false),editingId=ref<number|null>(null),formRef=ref(),permissionVisible=ref(false),currentRole=ref<any>(),treeRef=ref(),menuTree=ref<any[]>([])
const scopeNames:any={ALL:'全部数据',STORE:'本门店',WAREHOUSE:'本仓库',SELF:'仅本人'}; const form=reactive({roleCode:'',roleName:'',dataScope:'SELF',sortNo:0,remark:''}); const rules={roleCode:[{required:true,message:'请输入角色编码',trigger:'blur'}],roleName:[{required:true,message:'请输入角色名称',trigger:'blur'}]}
onMounted(load); async function load(){loading.value=true;try{rows.value=(await getRoles()).data||[]}finally{loading.value=false}} function openAdd(){editingId.value=null;Object.assign(form,{roleCode:'',roleName:'',dataScope:'SELF',sortNo:0,remark:''});dialogVisible.value=true} function openEdit(row:any){editingId.value=row.id;Object.assign(form,row);dialogVisible.value=true}
async function save(){if(!await formRef.value.validate().catch(()=>false))return;editingId.value?await updateRole(editingId.value,form):await createRole(form);ElMessage.success('保存成功');dialogVisible.value=false;load()} async function remove(row:any){await ElMessageBox.confirm('确定删除该角色吗？','确认',{type:'warning'});await deleteRole(row.id);ElMessage.success('删除成功');load()}
function buildTree(items:any[]){const map=new Map();items.forEach(i=>map.set(i.id,{...i,children:[]}));const roots:any[]=[];map.forEach(i=>i.parentId&&map.has(i.parentId)?map.get(i.parentId).children.push(i):roots.push(i));return roots}
async function openPermission(row:any){currentRole.value=row;const [menus,checked]=await Promise.all([getAllMenus(),getRoleMenus(row.id)]);menuTree.value=buildTree(menus.data||[]);permissionVisible.value=true;await nextTick();treeRef.value.setCheckedKeys(checked.data||[])}
async function savePermission(){const checked=treeRef.value.getCheckedKeys();const half=treeRef.value.getHalfCheckedKeys();await assignRoleMenus(currentRole.value.id,[...checked,...half]);ElMessage.success('权限已保存');permissionVisible.value=false}
</script>
