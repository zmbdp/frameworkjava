# Excel 导入导出

frameworkJava 基于 Alibaba EasyExcel 封装了 Excel 导入导出能力，提供同步/异步导入、HTTP 响应/输出流导出、单表/多表模板填充、单元格合并、大数值精度保护、JSR-303 数据校验等特性，开箱即用。

## 概述

### EasyExcel 版本

| 依赖            | groupId        | artifactId | 版本    |
|-----------------|----------------|------------|---------|
| EasyExcel       | com.alibaba    | easyexcel  | 3.2.1   |
| Apache XMLBeans | org.apache.xmlbeans | xmlbeans | 3.1.0 |

> EasyExcel 版本在根 `pom.xml` 中统一管理（`easyexcel.version=3.2.1`），`zmbdp-common-excel` 模块通过 dependencyManagement 引入，无需指定版本。

### 模块结构

模块路径：`zmbdp-common/zmbdp-common-excel`

```
src/main/java/com/zmbdp/common/excel/
├── annotation
│   └── CellMerge.java                  // 单元格合并注解
├── converter
│   └── ExcelBigNumberConverter.java    // 大数值转换器（防科学计数法）
├── listener
│   ├── ExcelListener.java              // 监听器接口
│   └── DefaultExcelListener.java       // 默认监听器（JSR-303 校验 + 错误收集）
├── result
│   ├── ExcelResult.java                // 导入结果接口
│   └── DefaultExcelResult.java         // 导入结果默认实现
├── strategy
│   └── CellMergeStrategy.java          // 单元格合并策略
└── util
    └── ExcelUtil.java                  // Excel 工具类（统一入口）
```

### 主要依赖

| 依赖                          | 作用                       |
|-------------------------------|---------------------------|
| `com.alibaba:easyexcel`       | Excel 读写核心库            |
| `org.apache.xmlbeans:xmlbeans`| EasyExcel 解析所需          |
| `jakarta.validation-api`      | JSR-303 数据校验            |
| `jakarta.servlet-api`         | HTTP 响应导出                |
| `commons-collections4`        | 集合工具                    |
| `hutool-all`                  | 通用工具（IdUtil/StrUtil 等）|
| `zmbdp-common-core`           | ValidatorUtil/FileUtil 等   |
| `zmbdp-common-domain`         | ResultCode/CommonConstants  |

## 工具类 ExcelUtil

`ExcelUtil` 是 Excel 模块的统一入口，所有方法均为 `public static`，构造方法已通过 `@NoArgsConstructor(access = AccessLevel.PRIVATE)` 私有化，禁止实例化。

### 方法清单

按职责分为四类，共 13 个 public 方法（外加 1 个辅助方法 `encodingFilename`）：

#### 导入方法（3 个）

| 方法签名                                                                 | 返回值            | 说明                              |
|------------------------------------------------------------------------|------------------|----------------------------------|
| `inputExcel(InputStream is, Class<T> clazz)`                           | `List<T>`        | 同步导入（小数据量，不校验）         |
| `inputExcel(InputStream is, Class<T> clazz, boolean isValidate)`       | `ExcelResult<T>` | 异步导入，支持 JSR-303 校验与错误收集 |
| `inputExcel(InputStream is, Class<T> clazz, ExcelListener<T> listener)`| `ExcelResult<T>` | 异步导入，使用自定义监听器           |

#### 导出方法（4 个）

| 方法签名                                                                                                              | 说明                              |
|--------------------------------------------------------------------------------------------------------------------|----------------------------------|
| `outputExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response)`                         | 导出到 HTTP 响应（不合并）         |
| `outputExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, HttpServletResponse response)`         | 导出到 HTTP 响应（可选合并）       |
| `outputExcel(List<T> list, String sheetName, Class<T> clazz, OutputStream os)`                                      | 导出到输出流（不合并）             |
| `outputExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, OutputStream os)`                      | 导出到输出流（可选合并）           |

#### 模板导出方法（4 个）

| 方法签名                                                                                                              | 说明                                       |
|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| `exportTemplate(List<Object> data, String filename, String templatePath, HttpServletResponse response)`            | 单表多数据模板导出到 HTTP 响应（`{.属性}`）  |
| `exportTemplate(List<Object> data, String templatePath, OutputStream os)`                                          | 单表多数据模板导出到输出流（`{.属性}`）      |
| `exportTemplateMultiList(Map<String, Object> data, String filename, String templatePath, HttpServletResponse response)` | 多表多数据模板导出到 HTTP 响应（`{key.属性}`）|
| `exportTemplateMultiList(Map<String, Object> data, String templatePath, OutputStream os)`                          | 多表多数据模板导出到输出流（`{key.属性}`）   |

#### 表达式转换方法（2 个）

| 方法签名                                                                                | 说明                              |
|---------------------------------------------------------------------------------------|----------------------------------|
| `convertByExp(String propertyValue, String converterExp, String separator)`           | 正向转换：值 → 文本（用于导出）    |
| `reverseByExp(String propertyValue, String converterExp, String separator)`           | 反向转换：文本 → 值（用于导入）    |

#### 辅助方法

| 方法签名                                       | 说明                                       |
|----------------------------------------------|-------------------------------------------|
| `encodingFilename(String filename)`          | 为文件名添加 UUID 前缀并追加 `.xlsx` 扩展名  |

## 导入功能

### 三种导入方式对比

| 方式                    | 校验支持 | 错误收集 | 返回值            | 适用场景                          |
|------------------------|---------|---------|------------------|----------------------------------|
| 同步导入（无监听器）     | 否      | 否      | `List<T>`        | 小数据量（建议 < 1000 行），不需要校验 |
| 默认监听器导入          | 可选    | 是      | `ExcelResult<T>` | 需要校验/错误信息，推荐用于大文件    |
| 自定义监听器导入        | 自定义  | 自定义  | `ExcelResult<T>` | 需要边读边写库、批量处理等特殊逻辑  |

> 三种方式均通过 `EasyExcel.read(is, clazz, listener)` 链式调用实现，输入流都不会被自动关闭，需调用方自行处理。

### 同步导入（小数据量）

一次性读取整个 Excel 文件并转换为对象列表，内部调用 `doReadSync()`：

```java
// 从文件输入流导入
FileInputStream fis = new FileInputStream("users.xlsx");
List<UserDTO> userList = ExcelUtil.inputExcel(fis, UserDTO.class);

// 从 HTTP 请求导入
MultipartFile file = request.getFile("excel");
List<UserDTO> userList = ExcelUtil.inputExcel(file.getInputStream(), UserDTO.class);
```

### 带校验的导入

使用默认监听器 `DefaultExcelListener`，支持 JSR-303 校验和错误信息收集：

```java
ExcelResult<UserDTO> result = ExcelUtil.inputExcel(inputStream, UserDTO.class, true);
List<UserDTO> successList = result.getList();    // 成功导入的数据
List<String> errorList   = result.getErrorList(); // 错误信息列表
String analysis          = result.getAnalysis();  // 导入回执
```

### 自定义监听器导入

实现 `ExcelListener<T>` 接口，可自定义处理逻辑（如实时写库）：

```java
ExcelListener<UserDTO> customListener = new ExcelListener<UserDTO>() {
    @Override
    public void invoke(UserDTO data, AnalysisContext context) {
        // 自定义处理逻辑：例如实时保存到数据库
        userService.save(data);
    }

    @Override
    public ExcelResult<UserDTO> getExcelResult() {
        // 返回自定义结果
        return new ExcelResult<>();
    }
};

ExcelResult<UserDTO> result = ExcelUtil.inputExcel(inputStream, UserDTO.class, customListener);
```

### JSR-303 校验

当 `isValidate=true` 时，`DefaultExcelListener.invoke()` 会调用 `ValidatorUtil.validate(data)` 进行校验：

- 基于 Jakarta Validation（JSR-303/JSR-380）标准
- 使用 Spring 容器中的 `Validator` Bean
- 实体类字段需添加 `@NotBlank`、`@NotNull`、`@Email`、`@Size` 等注解
- 校验失败抛出 `ConstraintViolationException`，由 `onException` 捕获并收集

### 错误信息格式

错误信息由 `DefaultExcelListener.onException()` 收集，分为两类：

| 异常类型                          | 错误信息格式                                          |
|----------------------------------|------------------------------------------------------|
| `ExcelDataConvertException`      | `第X行-第Y列-表头Z: 解析异常`                          |
| `ConstraintViolationException`   | `第X行数据校验异常: 错误消息1, 错误消息2`               |

> 行号、列号均从 0 开始，显示时已 +1 转换为用户可读的位置。
>
> 注意：`onException` 在收集错误信息后会抛出 `ExcelAnalysisException` 终止导入流程。

## 导出功能

### 四种导出方式对比

| 方式                       | 输出目标     | 合并单元格 | 自动列宽 | 大数值保护 |
|---------------------------|------------|----------|---------|----------|
| HTTP 响应（不合并）        | 浏览器下载  | 否       | 是      | 是       |
| HTTP 响应（合并）          | 浏览器下载  | 是       | 是      | 是       |
| 输出流（不合并）           | 自定义流    | 否       | 是      | 是       |
| 输出流（合并）             | 自定义流    | 是       | 是      | 是       |

> HTTP 响应版本内部会调用 `resetResponse()` 设置响应头，然后委托给输出流版本（`merge=false`）执行实际写入。

### 核心实现

四种方式最终都汇聚到 `outputExcel(list, sheetName, clazz, merge, os)`：

```java
ExcelWriterSheetBuilder builder = EasyExcel.write(os, clazz)
        .autoCloseStream(false)
        // 自动适配列宽
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
        // 大数值自动转换 防止失真
        .registerConverter(new ExcelBigNumberConverter())
        .sheet(sheetName);
if (merge) {
    // 合并处理器
    builder.registerWriteHandler(new CellMergeStrategy(list, true));
}
builder.doWrite(list);
```

### 使用示例

```java
// 1. 导出到 HTTP 响应（不合并）
List<UserDTO> userList = userService.findAll();
ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, response);

// 2. 导出到 HTTP 响应（带合并，需实体类字段标注 @CellMerge）
ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);

// 3. 导出到输出流（如写入文件）
try (FileOutputStream fos = new FileOutputStream("users.xlsx")) {
    ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, fos);
}

// 4. 导出到输出流（带合并）
try (FileOutputStream fos = new FileOutputStream("users.xlsx")) {
    ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, fos);
}
```

### 自动列宽

通过注册 `LongestMatchColumnWidthStyleStrategy` 实现，根据单元格内容长度自动调整列宽，无需手动指定。

### 文件名编码（UUID 前缀 + UTF-8）

HTTP 响应导出时通过 `resetResponse()` 处理文件名：

1. 调用 `encodingFilename(sheetName)` 生成最终文件名：`UUID_原始名.xlsx`
   - UUID 使用 `IdUtil.fastSimpleUUID()` 生成（32 位十六进制）
   - 自动追加 `.xlsx` 扩展名
2. 调用 `FileUtil.setAttachmentResponseHeader(response, filename)` 编码文件名（UTF-8）
3. 设置 `Content-Type` 为 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8`

```java
// 文件名示例
String filename = ExcelUtil.encodingFilename("用户列表");
// 结果类似：a1b2c3d4e5f6_用户列表.xlsx
```

## 单元格合并

### @CellMerge 注解

`@CellMerge` 标记需要合并的列，需配合 `@ExcelProperty` 使用，定义在字段上：

| 属性    | 类型 | 默认值 | 说明                                                          |
|---------|------|-------|--------------------------------------------------------------|
| `index` | int  | -1    | 列索引（从 0 开始）；-1 表示使用字段在类中的声明顺序作为列索引   |

注解元信息：

```java
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CellMerge {
    int index() default -1;
}
```

### 合并规则

`CellMergeStrategy` 基于 `AbstractMergeStrategy` 实现，规则如下：

- **连续相同才合并**：只有连续相同值的单元格才会合并，非连续相同值不会合并
- **空值跳过**：`null` 或空字符串的单元格不参与合并，会被跳过
- **至少 2 个才合并**：单个单元格不会合并，至少需要 2 个连续相同值
- **自动居中样式**：合并后的单元格自动应用水平居中 + 垂直居中样式
- **比较方式**：基于值的 `equals()` 方法比较，字段类型需正确实现 `equals`

### 使用示例

```java
// 实体类示例：相同部门的连续单元格会合并
public class UserDTO {
    @ExcelProperty("部门")
    @CellMerge  // 相同部门的连续单元格会自动合并
    private String department;

    @ExcelProperty("姓名")
    private String name;  // 不合并
}

// 导出时启用合并
List<UserDTO> userList = userService.findAll();
ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);
```

指定列索引（字段顺序与 Excel 列顺序不一致时使用）：

```java
public class OrderDTO {
    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("分类")
    @CellMerge(index = 2)  // 指定在第 3 列（索引 2）合并
    private String category;
}
```

### 实现要点

`CellMergeStrategy` 通过反射扫描实体类字段，收集带有 `@CellMerge` 注解的字段及其列索引，遍历数据列表计算连续相同值的范围，生成 `CellRangeAddress` 列表，最后在 `merge()` 方法中应用到 Sheet 上。

合并操作只在第 2 行第 1 列（`cell.getRowIndex()==1 && cell.getColumnIndex()==0`）触发一次，避免重复执行。

## 大数值处理

### ExcelBigNumberConverter

Excel 数值类型最大精度为 15 位，超过会丢失精度。`ExcelBigNumberConverter` 通过实现 EasyExcel 的 `Converter<Long>` 接口解决该问题。

| 方法                      | 作用                                  |
|--------------------------|--------------------------------------|
| `supportJavaTypeKey()`   | 返回 `Long.class`，声明支持 Long 类型  |
| `supportExcelTypeKey()`  | 返回 `STRING`，Excel 中以字符串存储     |
| `convertToJavaData()`    | 导入时将字符串转回 Long（`Convert.toLong`）|
| `convertToExcelData()`   | 导出时按长度判断：>15 位转字符串，≤15 位保持数字 |

### 转换逻辑

```java
public WriteCellData<Object> convertToExcelData(Long object, ...) {
    if (ObjectUtil.isNotNull(object)) {
        String str = Convert.toStr(object);
        if (str.length() > 15) {
            // 超过 15 位，转为字符串格式（防科学计数法/精度丢失）
            return new WriteCellData<>(str);
        }
        // 15 位及以内，保持数字格式
        WriteCellData<Object> cellData = new WriteCellData<>(new BigDecimal(object));
        cellData.setType(CellDataTypeEnum.NUMBER);
        return cellData;
    }
    return new WriteCellData<>("");
}
```

### 自动注册

`ExcelBigNumberConverter` 已在 `ExcelUtil` 的导出方法（`outputExcel` 系列）和模板导出方法（`exportTemplate`、`exportTemplateMultiList`）中通过 `.registerConverter(new ExcelBigNumberConverter())` 自动注册，无需手动配置。

## 监听器

### ExcelListener 接口

扩展 EasyExcel 的 `ReadListener`，新增获取导入结果的方法：

```java
public interface ExcelListener<T> extends ReadListener<T> {
    /**
     * 获取 Excel 导入结果
     */
    ExcelResult<T> getExcelResult();
}
```

### DefaultExcelListener

默认实现，继承 `AnalysisEventListener<T>`，提供以下能力：

| 方法                    | 功能                                                                 |
|------------------------|----------------------------------------------------------------------|
| `invokeHeadMap()`      | 读取表头数据，保存到 `headMap`（列索引 → 表头名称），用于异常提示          |
| `invoke()`             | 读取每行数据；若 `isValidate=true` 调用 `ValidatorUtil.validate(data)`；校验通过则加入成功列表 |
| `onException()`        | 处理 `ExcelDataConvertException` 和 `ConstraintViolationException`，收集错误信息后抛出 `ExcelAnalysisException` |
| `doAfterAllAnalysed()` | 所有数据解析完成回调                                                  |
| `getExcelResult()`     | 返回 `ExcelResult<T>` 实例                                            |

### 字段说明

```java
private Boolean isValidate = Boolean.TRUE;   // 是否启用校验，默认 true
private Map<Integer, String> headMap;        // 表头映射（列索引 → 表头名称）
private ExcelResult<T> excelResult;          // 导入结果对象
```

### 构造方法

```java
// 无参构造（默认启用校验，由 @NoArgsConstructor 提供）
DefaultExcelListener<T> listener = new DefaultExcelListener<>();

// 显式指定是否校验
DefaultExcelListener<T> listener = new DefaultExcelListener<>(true);
```

## 导入结果

### ExcelResult 接口

```java
public interface ExcelResult<T> {
    List<T> getList();         // 成功导入的数据列表
    List<String> getErrorList(); // 错误信息列表
    String getAnalysis();      // 导入回执信息
}
```

### DefaultExcelResult 实现

提供三种构造方式：

| 构造方法                                  | 说明                            |
|-----------------------------------------|--------------------------------|
| `DefaultExcelResult()`                  | 创建空结果，初始化空的 list 和 errorList |
| `DefaultExcelResult(List<T>, List<String>)` | 直接指定成功列表和错误列表        |
| `DefaultExcelResult(ExcelResult<T>)`    | 从其他 ExcelResult 复制          |

### 导入回执 getAnalysis()

根据成功数与错误数生成回执信息：

| 条件                              | 返回值                                  |
|----------------------------------|----------------------------------------|
| 成功数为 0                        | `读取失败，未解析到数据`                  |
| 成功数 > 0 且错误数为 0            | `恭喜您，全部读取成功！共N条`              |
| 成功数 > 0 且错误数 > 0            | 空字符串（调用方应自行组合成功/错误信息）   |

## 模板导出

### 单表多数据模板（{.属性名}）

使用预定义 Excel 模板填充数据，模板中每个对象占一行，使用 `{.属性名}` 格式：

**模板示例：**

```
姓名      | 年龄 | 部门
{.name}   | {.age} | {.dept}
```

**使用示例：**

```java
// 导出到 HTTP 响应
List<Object> data = userList.stream().map(u -> (Object) u).collect(Collectors.toList());
ExcelUtil.exportTemplate(data, "用户列表", "excel/template.xlsx", response);

// 导出到输出流
try (FileOutputStream fos = new FileOutputStream("users.xlsx")) {
    ExcelUtil.exportTemplate(data, "excel/template.xlsx", fos);
}
```

> 注意：`data` 为空时会抛出 `ServiceException(ResultCode.INVALID_PARA)`。
> 模板路径必须是 `resource` 目录下的相对路径（如 `excel/template.xlsx`）。

### 多表多数据模板（{key.属性名}）

支持在一个模板中填充多组数据，使用 `{key.属性名}` 格式，`Map` 的 `key` 对应模板中的 `key`。

**模板示例：**

```
用户列表：
姓名        | 年龄
{users.name} | {users.age}

部门列表：
部门名称
{departments.name}
```

**使用示例：**

```java
Map<String, Object> data = new HashMap<>();
data.put("users", userList);             // Collection 类型
data.put("departments", deptList);       // Collection 类型
data.put("company", companyInfo);        // 单个对象（非 Collection）

ExcelUtil.exportTemplateMultiList(data, "综合报表", "excel/report.xlsx", response);
```

### 实现要点

多表导出的核心逻辑：

```java
for (Map.Entry<String, Object> map : data.entrySet()) {
    // 设置列表后续还有数据
    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
    if (map.getValue() instanceof Collection) {
        // 多表导出必须使用 FillWrapper
        excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
    } else {
        excelWriter.fill(map.getValue(), writeSheet);
    }
}
```

关键点：

- **`FillConfig.forceNewRow=true`**：列表填充时强制新起一行，避免多组列表数据互相覆盖
- **`FillWrapper`**：Collection 类型的多组数据必须用 `FillWrapper` 包装，绑定到模板中的 `key`
- **单个对象**：非 Collection 类型直接 `fill`，不使用 `FillConfig`

## 表达式转换

`convertByExp` 和 `reverseByExp` 用于数据库枚举值与可读文本之间的相互转换，常用于 Excel 导入导出的数据映射。

### 语法

转换表达式格式：`key=value,key=value`，使用逗号分隔多个映射（基于 `CommonConstants.COMMA_SEPARATOR`，即 `,`）。

| 方法            | 表达式格式              | 用途                |
|----------------|------------------------|---------------------|
| `convertByExp` | `值=文本,值=文本`       | 导出时：值 → 文本    |
| `reverseByExp` | `文本=值,文本=值`       | 导入时：文本 → 值    |

### 使用示例

```java
// 正向转换（导出场景）：单个值
String gender = ExcelUtil.convertByExp("0", "0=男,1=女,2=未知", ",");
// 结果：gender = "男"

// 正向转换：多个值（分隔符分隔）
String status = ExcelUtil.convertByExp("0,1", "0=待审核,1=已审核,2=已拒绝", ",");
// 结果：status = "待审核,已审核"

// 反向转换（导入场景）：单个值
String genderCode = ExcelUtil.reverseByExp("男", "男=0,女=1,未知=2", ",");
// 结果：genderCode = "0"

// 反向转换：多个值
String statusCode = ExcelUtil.reverseByExp("待审核,已审核", "待审核=0,已审核=1,已拒绝=2", ",");
// 结果：statusCode = "0,1"
```

### 注意事项

- 找不到对应值时返回空字符串
- 表达式中格式错误（缺少 `=`）的项会被跳过
- 多个值转换时，结果使用相同的分隔符连接
- `separator` 既用于拆分输入值，也用于连接输出结果

## 实体类注解要求

实体类必须使用 EasyExcel 的 `@ExcelProperty` 注解标记字段，用于指定表头名称和列顺序。

### 基本示例

```java
public class UserDTO {
    @ExcelProperty("用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ExcelProperty("年龄")
    @Min(value = 0, message = "年龄不能小于0")
    private Integer age;

    @ExcelProperty("邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ExcelProperty("部门")
    @CellMerge  // 可选：相同部门的连续单元格会合并
    private String department;

    // getter / setter ...
}
```

### 注解搭配说明

| 注解             | 来源                  | 作用                                |
|-----------------|-----------------------|------------------------------------|
| `@ExcelProperty`| EasyExcel             | 标记表头名称、列索引、转换器等         |
| `@CellMerge`    | zmbdp-common-excel    | 标记需要合并的列（可选）              |
| `@NotBlank`/`@NotNull`/`@Email`/`@Size` 等 | Jakarta Validation | 数据校验（导入时由 `DefaultExcelListener` 触发） |

> 导入时 Excel 表头必须与实体类字段对应；模板导出时模板中的占位符必须与字段名匹配。

## 使用示例

### 完整导入流程（带校验）

```java
@PostMapping("/import")
public Result<Void> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
    // 带校验导入
    ExcelResult<UserDTO> result = ExcelUtil.inputExcel(
        file.getInputStream(), UserDTO.class, true);

    List<UserDTO> successList = result.getList();
    List<String> errorList = result.getErrorList();

    if (!errorList.isEmpty()) {
        // 存在错误，返回错误信息给前端
        return Result.fail(String.join("; ", errorList));
    }

    // 全部成功，批量入库
    userService.saveBatch(successList);
    return Result.success(result.getAnalysis());
}
```

### 完整导出流程（带合并）

```java
@GetMapping("/export")
public void exportUsers(HttpServletResponse response) {
    List<UserDTO> userList = userService.findAll();
    // 启用单元格合并（实体类中已标注 @CellMerge）
    ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);
}
```

### 模板导出流程

```java
@GetMapping("/export-template")
public void exportTemplate(HttpServletResponse response) {
    List<Object> data = userService.findAll().stream()
        .map(u -> (Object) u)
        .collect(Collectors.toList());
    // 模板文件位于 resource/excel/user-template.xlsx
    ExcelUtil.exportTemplate(data, "用户导入模板", "excel/user-template.xlsx", response);
}
```

### 多表模板导出流程

```java
@GetMapping("/export-report")
public void exportReport(HttpServletResponse response) {
    Map<String, Object> data = new HashMap<>();
    data.put("users", userService.findAll());
    data.put("departments", deptService.findAll());
    data.put("company", companyService.getInfo());
    ExcelUtil.exportTemplateMultiList(data, "综合报表", "excel/report.xlsx", response);
}
```

## 注意事项

1. **静态方法**：`ExcelUtil` 所有方法均为静态方法，构造方法已私有化，禁止实例化
2. **流不自动关闭**：所有导入/导出方法均设置 `autoCloseStream(false)`，输入/输出流需调用方自行关闭（推荐 try-with-resources）
3. **大数值保护**：导出时 `Long` 类型字段自动应用 `ExcelBigNumberConverter`，超过 15 位转字符串
4. **校验依赖 Spring**：`DefaultExcelListener` 的 JSR-303 校验依赖 `ValidatorUtil`，需 Spring 容器中存在 `Validator` Bean
5. **合并性能**：`CellMergeStrategy` 基于反射扫描字段，大数据量（>10000 行）场景下会增加导出时间，建议谨慎使用
6. **模板路径**：模板文件必须放置在 `resource` 目录下，路径为相对路径（如 `excel/template.xlsx`）
7. **表头对应**：导入时 Excel 表头必须与实体类 `@ExcelProperty` 字段对应
8. **错误终止**：`DefaultExcelListener.onException()` 收集错误后会抛出 `ExcelAnalysisException` 终止导入，后续数据不会被处理
9. **文件名唯一**：HTTP 响应导出会自动为文件名添加 UUID 前缀，避免多用户同时下载时文件名冲突

## 相关文档

- [工具类使用指南](UTILS.md) - 含 `ExcelUtil` 在工具类体系中的位置
