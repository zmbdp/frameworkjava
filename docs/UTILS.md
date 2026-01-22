# 工具类使用指南

FrameworkJava 提供了 **23 个工具类**，覆盖加密、JSON、Excel、邮件、分页、流处理等常用场景，开箱即用，无需重复造轮子。

## 工具类分类

### 核心工具类（17 个）

#### zmbdp-common-core 模块（14 个）

| 工具类 | 功能说明 | 主要方法 |
|--------|---------|---------|
| `AESUtil` | AES 加密/解密 | `encrypt()`, `decrypt()` |
| `BeanCopyUtil` | Bean 属性拷贝 | `copyProperties()`, `copyListProperties()` |
| `ExcelUtil` | Excel 导入/导出 | `read()`, `write()` |
| `FileUtil` | 文件操作 | `read()`, `write()`, `delete()` |
| `JsonUtil` | JSON 处理 | `toJson()`, `parseObject()`, `parseArray()` |
| `MailUtil` | 邮件发送 | `sendText()`, `sendHtml()` |
| `PageUtil` | 分页处理 | `startPage()`, `getPage()` |
| `ServletUtil` | Servlet 工具 | `getRequest()`, `getResponse()`, `getParameter()` |
| `StreamUtil` | 流处理 | `toInputStream()`, `toByteArray()` |
| `StringUtil` | 字符串处理 | `isEmpty()`, `isBlank()`, `trim()` |
| `ThreadUtil` | 线程工具 | `sleep()`, `waitFor()` |
| `TimestampUtil` | 时间戳处理 | `getCurrentTimestamp()`, `format()` |
| `ValidatorUtil` | 数据校验 | `validate()`, `validateObject()` |
| `VerifyUtil` | 格式验证 | `checkPhone()`, `checkEmail()`, `checkIdCard()` |

#### 其他模块（3 个）

| 工具类 | 模块 | 功能说明 |
|--------|------|---------|
| `CacheUtil` | zmbdp-common-cache | 三级缓存工具（布隆过滤器 + Caffeine + Redis） |
| `JwtUtil` | zmbdp-common-security | JWT Token 创建、解析、信息提取 |
| `SecurityUtil` | zmbdp-common-security | Token 提取和处理 |

### Excel 工具类（6 个）

| 工具类 | 功能说明 |
|--------|---------|
| `CellMergeStrategy` | 单元格合并策略 |
| `DefaultExcelListener` | 默认 Excel 监听器（用于导入） |
| `DefaultExcelResult` | 默认 Excel 结果处理 |
| `ExcelBigNumberConverter` | Excel 大数字转换器 |
| `ExcelListener` | Excel 监听器接口 |
| `ExcelResult` | Excel 结果接口 |

## 快速索引

### 按功能分类

#### 数据处理
- **Bean 拷贝**：`BeanCopyUtil` - DTO/Entity/VO 转换
- **JSON 处理**：`JsonUtil` - JSON 序列化/反序列化
- **流处理**：`StreamUtil` - 流转换和处理

#### 文件操作
- **文件操作**：`FileUtil` - 文件读写、删除
- **Excel 处理**：`ExcelUtil` + Excel 工具类 - Excel 导入导出

#### 字符串与验证
- **字符串处理**：`StringUtil` - 字符串工具方法
- **格式验证**：`VerifyUtil` - 手机号、邮箱、身份证等格式验证
- **数据校验**：`ValidatorUtil` - 对象数据校验

#### 加密与安全
- **AES 加密**：`AESUtil` - AES 加密/解密
- **JWT 处理**：`JwtUtil` - JWT Token 创建和解析
- **安全工具**：`SecurityUtil` - Token 提取和处理

#### Web 相关
- **Servlet 工具**：`ServletUtil` - 获取请求、响应等
- **分页处理**：`PageUtil` - 分页参数处理

#### 其他工具
- **邮件发送**：`MailUtil` - 邮件发送
- **线程工具**：`ThreadUtil` - 线程等待、休眠
- **时间戳**：`TimestampUtil` - 时间戳处理
- **缓存工具**：`CacheUtil` - 三级缓存操作

## 使用示例

### Bean 拷贝

```java
// 单个对象拷贝
UserEntity entity = userService.findById(1L);
UserDTO dto = BeanCopyUtil.copyProperties(entity, UserDTO::new);

// List 集合拷贝
List<UserEntity> entityList = userService.findAll();
List<UserDTO> dtoList = BeanCopyUtil.copyListProperties(entityList, UserDTO::new);
```

### JSON 处理

```java
// 对象转 JSON
String json = JsonUtil.toJson(user);

// JSON 转对象
User user = JsonUtil.parseObject(json, User.class);

// JSON 转 List
List<User> users = JsonUtil.parseArray(json, User.class);
```

### Excel 导入导出

```java
// 导出 Excel
List<User> users = userService.findAll();
ExcelUtil.write("用户列表.xlsx", User.class, users);

// 导入 Excel
List<User> users = ExcelUtil.read("用户列表.xlsx", User.class);
```

### 格式验证

```java
// 验证手机号
boolean isValid = VerifyUtil.checkPhone("13800138000");

// 验证邮箱
boolean isValid = VerifyUtil.checkEmail("user@example.com");

// 验证身份证
boolean isValid = VerifyUtil.checkIdCard("110101199001011234");
```

### JWT 处理

```java
// 创建 Token
Map<String, Object> claims = new HashMap<>();
claims.put("user_id", "123");
String token = JwtUtil.createToken(claims, secretKey);

// 解析 Token
Claims parsedClaims = JwtUtil.parseToken(token, secretKey);
String userId = JwtUtil.getUserId(token, secretKey);
```

### 三级缓存

```java
// 查询（自动处理三级缓存）
String key = "user:123";
User user = CacheUtil.getL2Cache(
    redisService, 
    bloomFilterService, 
    key, 
    User.class, 
    caffeineCache
);

// 写入（自动写入三级缓存）
CacheUtil.setL2Cache(
    redisService, 
    bloomFilterService, 
    key, 
    user, 
    caffeineCache, 
    300L, 
    TimeUnit.SECONDS
);
```

### 邮件发送

```java
// 发送文本邮件
MailUtil.sendText("recipient@example.com", "主题", "内容");

// 发送 HTML 邮件
MailUtil.sendHtml("recipient@example.com", "主题", "<h1>HTML 内容</h1>");
```

### AES 加密

```java
// 加密
String encrypted = AESUtil.encrypt("原始数据", "密钥");

// 解密
String decrypted = AESUtil.decrypt(encrypted, "密钥");
```

## 注意事项

1. **工具类都是静态方法**：所有工具类都使用静态方法，无需实例化
2. **空值处理**：大部分工具类都做了空值处理，不会抛出 `NullPointerException`
3. **异常处理**：工具类内部已处理常见异常，使用更安全
4. **线程安全**：所有工具类都是线程安全的，可以在多线程环境下使用

## 扩展建议

如果需要新增工具类：

1. 在 `zmbdp-common-core/src/main/java/com/zmbdp/common/core/utils/` 下创建
2. 使用 `@NoArgsConstructor(access = AccessLevel.PRIVATE)` 防止实例化
3. 所有方法使用 `public static` 修饰
4. 添加完整的 Javadoc 注释
5. 更新本文档的工具类列表

## 相关文档

- [项目结构与模块职责](PROJECT_STRUCTURE.md)
- [三级缓存架构](CACHE_ARCHITECTURE.md) - `CacheUtil` 使用说明
- [分布式幂等性](IDEMPOTENT.md) - 幂等性相关工具
