# v1.3.0 版本更新日志

**发布日期**: 2025-01-XX

## 更新概览

本次更新主要新增了 Excel 导入导出功能和邮件发送功能，并对工具类库进行了增强，提供了更完善的业务开发能力。

## 新增功能

### Excel 导入导出功能

新增了完整的 Excel 处理能力，主要包括以下组件：

- **ExcelUtil** - Excel 工具类，提供完整的导入导出能力
- **ExcelBigNumberConverter** - 大数值转换器，解决 Excel 中超过 15 位数值精度丢失问题
- **CellMergeStrategy** - 单元格合并策略，支持基于注解的自动单元格合并
- **CellMerge** - 注解，用于标记需要合并的列字段
- **DefaultExcelListener** - 默认导入监听器，支持数据校验和错误收集
- **ExcelListener** / **ExcelResult** - 导入监听器和结果接口，提供灵活的导入结果处理
- **FileUtil** - 文件工具类，支持文件下载响应头设置和文件名编码处理

主要特性：

- 大数值处理：自动将超过 15 位的数值转换为字符串格式，防止精度丢失
- 单元格合并：基于 `@CellMerge` 注解自动合并相同值的单元格
- 数据校验：基于 Jakarta Validation 框架提供完整的数据校验能力
- 错误收集：导入过程中自动收集校验错误，提供详细的错误反馈
- 模板导出：支持单表和多表模板导出
- 自动列宽：导出时自动适配列宽，提升用户体验

#### 使用示例
```java
// 导出 Excel
ExcelUtil.exportExcel(response, "用户列表", UserDTO.class, userList, true);

// 导入 Excel
DefaultExcelListener<UserDTO> listener = new DefaultExcelListener<>(true);
EasyExcel.read(inputStream, UserDTO.class, listener).sheet().doRead();
ExcelResult<UserDTO> result = listener.getExcelResult();
```

### 邮件发送功能

新增了邮件发送功能，主要包括以下组件：

- **MailUtil** - 邮件工具类，基于 Spring JavaMailSender + Jakarta Mail
- **MailConfig** - 邮件配置类，支持 Spring 容器自动配置
- **MailAccount** - 邮件账号配置类，兼容原有 API

主要特性：

- 多格式支持：支持文本和 HTML 格式邮件发送
- 收件人管理：支持抄送（CC）、密送（BCC）功能
- 附件支持：支持单个或多个附件发送
- 内嵌图片：支持通过 cid 方式内嵌图片到 HTML 邮件中
- 自动格式识别：自动识别图片格式（JPEG、PNG、GIF、BMP、WebP）
- 灵活配置：支持自定义 MailAccount 或使用全局配置
- 线程安全：提供线程安全的邮件账号配置，避免并发问题
- 动态刷新：支持 Nacos 配置动态刷新（`@RefreshScope`）

#### 使用示例
```java
// 发送 HTML 邮件
MailUtil.sendHtml("user@example.com", "邮件标题", "<h1>邮件内容</h1>");

// 发送带附件的邮件
MailUtil.send("user@example.com", "标题", "内容", false, new File("附件.pdf"));

// 发送带内嵌图片的 HTML 邮件
Map<String, InputStream> images = new HashMap<>();
images.put("logo", new FileInputStream("logo.png"));
MailUtil.sendHtml("user@example.com", "标题", "<img src='cid:logo'>", images);
```

### 工具类增强

对现有工具类进行了增强：

**ThreadUtil 线程工具类**
- 新增线程休眠方法，支持中断状态恢复
- 增强线程池管理功能

**ValidatorUtil 校验工具类**
- 基于 Jakarta Validation 框架提供数据校验能力
- 支持单个对象和集合对象的批量校验
- 提供详细的校验错误信息收集

**StreamUtil 流工具类**
- 提供丰富的集合和流处理方法
- 支持复杂数据结构的转换和处理

**FileUtil 文件工具类**
- 支持文件下载响应头设置
- 支持文件名编码处理（解决中文文件名乱码问题）
- 支持文件大小和类型校验

## 代码质量提升

### Bug 修复

修复了若干潜在 bug，包括：
- 数组越界风险
- 空指针异常问题
- 邮件内嵌图片流重复读取问题（改用 `ByteArrayResource`）
- Excel 大数值精度丢失问题
- 单元格合并样式应用不完整的问题

### 代码优化

- 优化了代码逻辑，提高了代码健壮性
- 统一了代码风格和注释格式
- 完善了工具类的 Javadoc 文档，提高代码可读性和可维护性
- 优化了异常处理机制，提供更清晰的错误信息

### 文档更新

- 重新生成了 SDK 文档，包含所有新增功能的接口说明
- 更新了 README.md，添加了 Excel 和邮件功能的使用说明

## 依赖变更

**新增依赖**
- `spring-boot-starter-mail` - Spring Boot 邮件支持
- `xmlbeans` (3.1.0) - Excel 处理所需依赖

**依赖调整**
- 移除了 `easyexcel` 对 `poi-ooxml-schemas` 的排除，确保 Excel 功能正常工作

## 影响范围

**功能模块**
- Excel 导入导出：所有需要进行 Excel 处理的业务模块
- 邮件发送：所有需要邮件功能的业务模块
- 数据校验：使用数据校验功能的业务模块
- 工具类：使用对象拷贝、流处理等操作的场景

**配置变更**
- 邮件配置：需要在 Nacos 中配置 `mail.isEnabled=true` 以启用邮件功能
- 文件上传：`bootstrap.yml` 中已配置文件上传大小限制（50MB/100MB）

**SDK 文档**
- 建议替换旧版本 SDK 文档，以便获取最新的接口说明

## 升级指南

### 兼容性说明

本次更新为向下兼容更新，现有代码无需修改即可使用。保持了原有 API 的兼容性，`MailAccount` 类保持原有接口。

### 升级步骤

**1. 依赖更新**
```bash
# 拉取最新代码
git pull origin master

# 重新编译
mvn clean install -DskipTests
```

**2. 邮件功能配置（如需要）**

在 Nacos 配置中心添加邮件配置（`share-mail-{env}.yaml`）：
```yaml
mail:
  isEnabled: true
  from: your-email@example.com
  user: your-email@example.com
  pass: your-email-password
  host: smtp.example.com
  port: 465
  ssl-enable: true
```

**3. Excel 功能使用（如需要）**
- 确保已引入 `easyexcel` 相关依赖（如果尚未引入）
- 使用 Jakarta Validation 的项目需要确保依赖版本兼容

**4. SDK 文档更新**
- 建议替换旧版本 SDK 文档（`javapro/javadoc` 目录）

### 注意事项

- 邮件功能需要在 Spring 容器中配置 `MailAccount` Bean（通过 `mail.isEnabled=true` 启用）
- Excel 导入导出功能需要引入 EasyExcel 相关依赖（如果尚未引入）
- 使用 Jakarta Validation 的项目需要确保依赖版本兼容
- 无需额外配置，直接升级即可使用新增功能

## 详细变更列表

### 新增文件
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/ExcelUtil.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/MailUtil.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/FileUtil.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/ValidatorUtil.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/StreamUtil.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/ExcelBigNumberConverter.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/CellMergeStrategy.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/DefaultExcelListener.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/DefaultExcelResult.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/ExcelListener.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/excel/ExcelResult.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/annotation/excel/CellMerge.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/config/MailConfig.java`
- `zmbdp-common-core/src/main/java/com/zmbdp/common/core/config/MailAccount.java`

### 修改文件
- `pom.xml` - 添加 `xmlbeans` 依赖，移除 `poi-ooxml-schemas` 排除
- `zmbdp-common-core/pom.xml` - 添加 `spring-boot-starter-mail` 和 `xmlbeans` 依赖
- `zmbdp-common-domain/src/main/java/com/zmbdp/common/domain/domain/ResultCode.java` - 新增 Excel 和邮件相关错误码
- `zmbdp-mstemplate-service/src/main/resources/bootstrap.yml` - 添加文件上传大小限制配置
- `zmbdp-mstemplate-service/src/main/java/com/zmbdp/mstemplate/service/test/TestExcelController.java` - 新增 Excel 测试接口
- `zmbdp-mstemplate-service/src/main/java/com/zmbdp/mstemplate/service/test/TestMailController.java` - 新增邮件测试接口

## 致谢

感谢所有为本次更新做出贡献的开发者，特别感谢 Spring 团队、EasyExcel 团队和 Jakarta Mail 团队提供的优秀框架支持。

## 技术支持

如有问题或建议，请通过以下方式联系：
- GitHub Issues: [提交 Issue](https://github.com/zmbdp/frameworkjava/issues)
- 邮箱: JavaFH@163.com
