<template>
  <div class="page-wrapper">
    <el-card>
      <div class="page-title">{{ config.title }}</div>
      <div class="toolbar">
        <el-input v-model="query.code" :placeholder="`${config.title}编码`" clearable style="width:180px" />
        <el-input v-model="query.name" :placeholder="`${config.title}名称`" clearable style="width:180px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="openAdd">新增{{ config.title }}</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column :label="`${config.title}编码`" width="170"><template #default="{row}">{{ row[config.codeField] }}</template></el-table-column>
        <el-table-column :label="`${config.title}名称`" min-width="180"><template #default="{row}">{{ row[config.nameField] }}</template></el-table-column>
        <el-table-column v-if="config.type === 'productCategory'" label="上级分类" min-width="150">
          <template #default="{row}">{{ parentName(row.parentId) }}</template>
        </el-table-column>
        <el-table-column v-if="config.type === 'unit'" prop="precisionScale" label="数量小数位" width="120" />
        <el-table-column v-if="config.type === 'customerLevel'" prop="discountRate" label="折扣率" width="110" />
        <el-table-column v-if="config.type === 'customerLevel'" prop="creditLimit" label="默认信用额度" width="140" />
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="155" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog :title="`${editingId ? '编辑' : '新增'}${config.title}`" v-model="dialogVisible" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="`${config.title}编码`" prop="code"><el-input v-model="form.code" /></el-form-item>
        <el-form-item :label="`${config.title}名称`" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="config.type === 'productCategory'" label="上级分类">
          <el-select v-model="form.parentId" clearable style="width:100%" placeholder="不选择表示一级分类">
            <el-option v-for="item in selectableParents" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="config.type === 'unit'" label="数量小数位">
          <el-input-number v-model="form.precisionScale" :min="0" :max="4" :precision="0" />
        </el-form-item>
        <el-form-item v-if="config.type === 'customerLevel'" label="折扣率">
          <el-input-number v-model="form.discountRate" :min="0.0001" :max="1" :precision="4" :step="0.05" />
          <span class="field-tip">1 表示不打折，0.9 表示九折</span>
        </el-form-item>
        <el-form-item v-if="config.type === 'customerLevel'" label="默认信用额度">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" style="width:220px" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" :precision="0" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
          <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
        </el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory, createCustomerCategory, createCustomerLevel, createSupplierCategory, createUnit,
  deleteCategory, deleteCustomerCategory, deleteCustomerLevel, deleteSupplierCategory, deleteUnit,
  getCategories, getCategoryOptions, getCustomerCategories, getCustomerLevels, getSupplierCategories, getUnits,
  updateCategory, updateCustomerCategory, updateCustomerLevel, updateSupplierCategory, updateUnit
} from '@/api/masterdata'

const route = useRoute()
const definitions: Record<string, any> = {
  productCategory: { type:'productCategory', title:'商品分类', codeField:'categoryCode', nameField:'categoryName', list:getCategories, create:createCategory, update:updateCategory, remove:deleteCategory },
  unit: { type:'unit', title:'商品单位', codeField:'unitCode', nameField:'unitName', list:getUnits, create:createUnit, update:updateUnit, remove:deleteUnit },
  customerCategory: { type:'customerCategory', title:'客户分类', codeField:'categoryCode', nameField:'categoryName', list:getCustomerCategories, create:createCustomerCategory, update:updateCustomerCategory, remove:deleteCustomerCategory },
  customerLevel: { type:'customerLevel', title:'客户等级', codeField:'levelCode', nameField:'levelName', list:getCustomerLevels, create:createCustomerLevel, update:updateCustomerLevel, remove:deleteCustomerLevel },
  supplierCategory: { type:'supplierCategory', title:'供应商分类', codeField:'categoryCode', nameField:'categoryName', list:getSupplierCategories, create:createSupplierCategory, update:updateSupplierCategory, remove:deleteSupplierCategory }
}
const config = computed(() => definitions[String(route.meta.configType)] || definitions.productCategory)
const loading=ref(false), submitting=ref(false), dialogVisible=ref(false), editingId=ref<number|null>(null), formRef=ref()
const tableData=ref<any[]>([]), total=ref(0), categoryOptions=ref<any[]>([])
const query=reactive({page:1,size:10,code:'',name:'',status:''})
const form=reactive<any>({})
const rules={code:[{required:true,message:'请输入编码',trigger:'blur'}],name:[{required:true,message:'请输入名称',trigger:'blur'}]}
const selectableParents=computed(()=>categoryOptions.value.filter((item:any)=>item.value!==editingId.value))

onMounted(init)
watch(config, init)
async function init(){query.page=1;query.code='';query.name='';query.status='';await Promise.all([fetchData(),loadParents()])}
async function loadParents(){categoryOptions.value=config.value.type==='productCategory'?(await getCategoryOptions()).data||[]:[]}
function listParams(){
  const base:any={page:query.page,size:query.size,status:query.status}
  if(config.value.type==='productCategory'){base.categoryCode=query.code;base.categoryName=query.name}
  else if(config.value.type==='unit'){base.unitCode=query.code;base.unitName=query.name}
  else {base.code=query.code;base.name=query.name}
  return base
}
async function fetchData(){loading.value=true;try{const res=await config.value.list(listParams());tableData.value=res.data.records;total.value=res.data.total}finally{loading.value=false}}
function parentName(id:any){if(!id||Number(id)===0)return '一级分类';return categoryOptions.value.find((item:any)=>item.value===id)?.label||'-'}
function resetForm(row?:any){Object.assign(form,{code:row?.[config.value.codeField]||'',name:row?.[config.value.nameField]||'',
  parentId:row?.parentId||null,precisionScale:row?.precisionScale??0,discountRate:row?.discountRate??1,
  creditLimit:row?.creditLimit??0,sortNo:row?.sortNo??0,status:row?.status||'ENABLED'})}
function openAdd(){editingId.value=null;resetForm();dialogVisible.value=true}
function openEdit(row:any){editingId.value=row.id;resetForm(row);dialogVisible.value=true}
function payload(){const data:any={[config.value.codeField]:form.code,[config.value.nameField]:form.name,sortNo:form.sortNo,status:form.status}
  if(config.value.type==='productCategory')data.parentId=form.parentId||0
  if(config.value.type==='unit')data.precisionScale=form.precisionScale
  if(config.value.type==='customerLevel'){data.discountRate=form.discountRate;data.creditLimit=form.creditLimit}
  return data}
async function submit(){if(!await formRef.value.validate().catch(()=>false))return;submitting.value=true;try{
  if(editingId.value)await config.value.update(editingId.value,payload());else await config.value.create(payload())
  ElMessage.success('保存成功');dialogVisible.value=false;await Promise.all([fetchData(),loadParents()])
}finally{submitting.value=false}}
async function remove(row:any){await ElMessageBox.confirm('被业务资料使用的数据不能删除，是否继续？','删除确认',{type:'warning'});await config.value.remove(row.id);ElMessage.success('删除成功');await Promise.all([fetchData(),loadParents()])}
</script>

<style scoped>
.page-wrapper{padding:0}.page-title{margin-bottom:12px;font-size:18px;font-weight:600}.toolbar{display:flex;gap:10px;flex-wrap:wrap}.pagination{margin-top:12px;justify-content:flex-end}.field-tip{margin-left:10px;color:#909399;font-size:12px}
</style>
