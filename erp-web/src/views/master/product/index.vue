<template>
  <div class="page-wrapper">
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.productCode" placeholder="商品编码" clearable style="width:180px" />
        <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:180px" />
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable style="width:180px">
          <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button type="success" @click="handleAdd">新增</el-button>
      </div>
    </el-card>
    <el-card style="margin-top:12px">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="productName" label="商品名称" width="160" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="specification" label="规格" width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column label="标签" min-width="150"><template #default="{row}">
          <el-tag v-for="name in row.tagNames || []" :key="name" size="small" class="tag-item">{{ name }}</el-tag>
        </template></el-table-column>
        <el-table-column prop="purchasePrice" label="采购价" width="100" />
        <el-table-column prop="salePrice" label="销售价" width="100" />
        <el-table-column prop="minStock" label="最低库存" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px;justify-content:flex-end" background layout="total,prev,pager,next"
        :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="fetchData" />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="min(820px,94vw)" top="4vh">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品编码" prop="productCode">
              <el-input v-model="form.productCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="form.productName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条形码">
              <el-input v-model="form.barcode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="unitId">
              <el-select v-model="form.unitId" placeholder="请选择单位" style="width:100%">
                <el-option v-for="u in unitOptions" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品标签">
              <el-select v-model="form.tagIds" multiple clearable style="width:100%" placeholder="可选择多个标签">
                <el-option v-for="tag in productTags" :key="tag.id" :label="tag.tagName" :value="tag.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-for="attribute in productAttributes" :key="attribute.id" :span="12">
            <el-form-item :label="attribute.attributeName">
              <el-select v-if="attribute.inputType === 'SELECT'" v-model="selectionFor(attribute.id).attributeValueId"
                clearable style="width:100%" placeholder="请选择">
                <el-option v-for="value in attribute.values || []" :key="value.id" :label="value.valueName" :value="value.id" />
              </el-select>
              <el-input-number v-else-if="attribute.inputType === 'NUMBER'" v-model="selectionFor(attribute.id).customValue"
                controls-position="right" style="width:100%" />
              <el-input v-else v-model="selectionFor(attribute.id).customValue" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.specification" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.modelNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购价">
              <el-input-number v-model="form.purchasePrice" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价">
              <el-input-number v-model="form.salePrice" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低库存">
              <el-input-number v-model="form.minStock" :precision="0" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="启用" value="ENABLED" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProducts, getProduct, createProduct, updateProduct, deleteProduct, getCategoryOptions, getUnitOptions, getProductMetadataOptions } from '@/api/masterdata'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, productCode: '', productName: '', categoryId: '', status: '' })

const categoryOptions = ref<any[]>([])
const unitOptions = ref<any[]>([])
const productTags = ref<any[]>([])
const productAttributes = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({
  productCode: '',
  productName: '',
  barcode: '',
  categoryId: null as number | null,
  unitId: null as number | null,
  brand: '',
  specification: '',
  modelNo: '',
  purchasePrice: 0,
  salePrice: 0,
  minStock: 0,
  tagIds: [] as number[],
  attributes: [] as any[],
  status: 'ENABLED',
  remark: ''
})
const formRules = {
  productCode: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

onMounted(() => { fetchData(); loadOptions() })

async function loadOptions() {
  try {
    const [catRes, unitRes, metadataRes] = await Promise.all([getCategoryOptions(), getUnitOptions(), getProductMetadataOptions()])
    categoryOptions.value = catRes.data || []
    unitOptions.value = unitRes.data || []
    productTags.value = metadataRes.data?.tags || []
    productAttributes.value = metadataRes.data?.attributes || []
  } catch { /* ignore */ }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getProducts(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增商品'
  Object.assign(form, {
    productCode: '', productName: '', barcode: '', categoryId: null, unitId: null,
    brand: '', specification: '', modelNo: '', purchasePrice: 0, salePrice: 0,
    minStock: 0, tagIds: [], attributes: emptySelections(), status: 'ENABLED', remark: ''
  })
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  const detail = (await getProduct(row.id)).data
  editingId.value = row.id
  dialogTitle.value = '编辑商品'
  Object.assign(form, {
    productCode: detail.productCode, productName: detail.productName, barcode: detail.barcode,
    categoryId: detail.categoryId, unitId: detail.unitId, brand: detail.brand,
    specification: detail.specification, modelNo: detail.modelNo,
    purchasePrice: detail.purchasePrice, salePrice: detail.salePrice,
    minStock: detail.minStock, tagIds: detail.tagIds || [], attributes: mergeSelections(detail.attributes || []),
    status: detail.status, remark: detail.remark
  })
  dialogVisible.value = true
}

function emptySelections() { return productAttributes.value.map((attribute: any) => ({ attributeId: attribute.id, attributeValueId: null, customValue: null })) }
function mergeSelections(saved: any[]) {
  return productAttributes.value.map((attribute: any) => {
    const current = saved.find((item: any) => item.attributeId === attribute.id)
    return current ? { ...current } : { attributeId: attribute.id, attributeValueId: null, customValue: null }
  })
}
function selectionFor(attributeId: number) {
  let selection = form.attributes.find((item: any) => item.attributeId === attributeId)
  if (!selection) { selection = { attributeId, attributeValueId: null, customValue: null }; form.attributes.push(selection) }
  return selection
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, form)
    } else {
      await createProduct(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该商品吗？', '确认', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.page-wrapper { padding: 0; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
.tag-item { margin-right: 4px; }
</style>
