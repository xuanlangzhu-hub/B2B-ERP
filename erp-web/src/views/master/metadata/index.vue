<template>
  <div class="page-wrapper">
    <el-card>
      <div class="page-title">{{ title }}</div>
      <div class="toolbar">
        <el-input v-if="isAttribute" v-model="query.code" placeholder="属性编码" clearable style="width:180px" />
        <el-input v-model="query.name" :placeholder="isAttribute ? '属性名称' : '标签名称'" clearable style="width:180px" />
        <el-select v-if="isAttribute" v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="openAdd">新增{{ title }}</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <template v-if="isAttribute">
          <el-table-column prop="attributeCode" label="属性编码" width="160" />
          <el-table-column prop="attributeName" label="属性名称" width="160" />
          <el-table-column label="输入类型" width="110"><template #default="{row}">{{ inputTypeLabel(row.inputType) }}</template></el-table-column>
          <el-table-column label="可选值" min-width="250"><template #default="{row}">
            <el-tag v-for="value in row.values || []" :key="value.id" class="value-tag">{{ value.valueName }}</el-tag>
            <span v-if="row.inputType !== 'SELECT'" class="muted">用户填写</span>
          </template></el-table-column>
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column prop="status" label="状态" width="90"><template #default="{row}">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
          </template></el-table-column>
        </template>
        <template v-else>
          <el-table-column prop="tagName" label="标签名称" min-width="200" />
          <el-table-column label="标签颜色" width="180"><template #default="{row}">
            <el-tag :color="row.tagColor" effect="dark">{{ row.tagColor }}</el-tag>
          </template></el-table-column>
        </template>
        <el-table-column label="操作" width="155" fixed="right"><template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total,prev,pager,next" :total="total"
        v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog v-if="isAttribute" :title="`${editingId ? '编辑' : '新增'}商品属性`" v-model="dialogVisible"
      width="min(760px,94vw)" top="6vh" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="attributeRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="属性编码" prop="attributeCode"><el-input v-model="form.attributeCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="属性名称" prop="attributeName"><el-input v-model="form.attributeName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="输入类型"><el-select v-model="form.inputType" style="width:100%">
            <el-option label="下拉选择" value="SELECT" /><el-option label="文本" value="TEXT" /><el-option label="数字" value="NUMBER" />
          </el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" :precision="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
          </el-select></el-form-item></el-col>
        </el-row>
        <template v-if="form.inputType === 'SELECT'">
          <div class="section-title"><span>属性可选值</span><el-button type="primary" link @click="addValue">+ 添加值</el-button></div>
          <el-table :data="form.values" border max-height="320">
            <el-table-column label="值编码" min-width="150"><template #default="{row}"><el-input v-model="row.valueCode" /></template></el-table-column>
            <el-table-column label="值名称" min-width="150"><template #default="{row}"><el-input v-model="row.valueName" /></template></el-table-column>
            <el-table-column label="排序" width="120"><template #default="{row}"><el-input-number v-model="row.sortNo" :min="0" :precision="0" style="width:95px" /></template></el-table-column>
            <el-table-column label="状态" width="120"><template #default="{row}"><el-select v-model="row.status">
              <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
            </el-select></template></el-table-column>
            <el-table-column label="操作" width="70"><template #default="{$index}"><el-button type="danger" link @click="form.values.splice($index,1)">删除</el-button></template></el-table-column>
          </el-table>
        </template>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-else :title="`${editingId ? '编辑' : '新增'}${title}`" v-model="dialogVisible" width="440px">
      <el-form ref="formRef" :model="form" :rules="tagRules" label-width="90px">
        <el-form-item label="标签名称" prop="tagName"><el-input v-model="form.tagName" maxlength="50" /></el-form-item>
        <el-form-item label="标签颜色"><el-color-picker v-model="form.tagColor" show-alpha /><span class="color-value">{{ form.tagColor }}</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCustomerTag, createProductAttribute, createProductTag, deleteCustomerTag, deleteProductAttribute,
  deleteProductTag, getCustomerTags, getProductAttributes, getProductTags, updateCustomerTag,
  updateProductAttribute, updateProductTag } from '@/api/masterdata'

const route=useRoute(), type=computed(()=>String(route.meta.metadataType||'attribute'))
const isAttribute=computed(()=>type.value==='attribute')
const title=computed(()=>({attribute:'商品属性',productTag:'商品标签',customerTag:'客户标签'} as Record<string,string>)[type.value])
const definitions:Record<string,any>={attribute:{list:getProductAttributes,create:createProductAttribute,update:updateProductAttribute,remove:deleteProductAttribute},
  productTag:{list:getProductTags,create:createProductTag,update:updateProductTag,remove:deleteProductTag},
  customerTag:{list:getCustomerTags,create:createCustomerTag,update:updateCustomerTag,remove:deleteCustomerTag}}
const loading=ref(false),submitting=ref(false),dialogVisible=ref(false),editingId=ref<number|null>(null),formRef=ref()
const tableData=ref<any[]>([]),total=ref(0),query=reactive({page:1,size:10,code:'',name:'',status:''}),form=reactive<any>({})
const attributeRules={attributeCode:[{required:true,message:'请输入属性编码',trigger:'blur'}],attributeName:[{required:true,message:'请输入属性名称',trigger:'blur'}]}
const tagRules={tagName:[{required:true,message:'请输入标签名称',trigger:'blur'}]}
onMounted(init);watch(type,init)
function inputTypeLabel(value:string){return({SELECT:'下拉选择',TEXT:'文本',NUMBER:'数字'} as Record<string,string>)[value]||value}
async function init(){query.page=1;query.code='';query.name='';query.status='';await fetchData()}
async function fetchData(){loading.value=true;try{const params:any={page:query.page,size:query.size,name:query.name};if(isAttribute.value){params.code=query.code;params.status=query.status}
  const res=await definitions[type.value].list(params);tableData.value=res.data.records;total.value=res.data.total}finally{loading.value=false}}
function resetForm(row?:any){if(isAttribute.value)Object.assign(form,{attributeCode:row?.attributeCode||'',attributeName:row?.attributeName||'',inputType:row?.inputType||'SELECT',sortNo:row?.sortNo??0,status:row?.status||'ENABLED',values:(row?.values||[]).map((v:any)=>({...v}))});
  else Object.assign(form,{tagName:row?.tagName||'',tagColor:row?.tagColor||(type.value==='productTag'?'#409EFF':'#67C23A')})}
function openAdd(){editingId.value=null;resetForm();if(isAttribute.value&&form.values.length===0)addValue();dialogVisible.value=true}
function openEdit(row:any){editingId.value=row.id;resetForm(row);dialogVisible.value=true}
function addValue(){form.values.push({valueCode:'',valueName:'',sortNo:form.values.length,status:'ENABLED'})}
async function submit(){if(!await formRef.value.validate().catch(()=>false))return;if(isAttribute.value&&form.inputType==='SELECT'&&form.values.some((v:any)=>!v.valueCode?.trim()||!v.valueName?.trim()))return ElMessage.warning('请完整填写属性值编码和名称');submitting.value=true;try{
  if(editingId.value)await definitions[type.value].update(editingId.value,form);else await definitions[type.value].create(form);ElMessage.success('保存成功');dialogVisible.value=false;fetchData()}finally{submitting.value=false}}
async function remove(row:any){await ElMessageBox.confirm('已经被商品或客户使用的数据不能删除，是否继续？','删除确认',{type:'warning'});await definitions[type.value].remove(row.id);ElMessage.success('删除成功');fetchData()}
</script>

<style scoped>
.page-wrapper{padding:0}.page-title{margin-bottom:12px;font-size:18px;font-weight:600}.toolbar{display:flex;gap:10px;flex-wrap:wrap}.pagination{margin-top:12px;justify-content:flex-end}.section-title{display:flex;justify-content:space-between;align-items:center;margin:4px 0 10px;font-weight:600}.value-tag{margin-right:5px}.muted{color:#909399}.color-value{margin-left:10px;color:#606266}
</style>
