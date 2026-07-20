# 消息与验证码服务

frameworkJava 提供统一的验证码发送与校验能力，基于策略模式封装短信、邮件两种发送渠道，并通过 Redis 实现验证码存储、每日发送次数限制与固定验证码机制，业务代码无感知切换开发/生产环境。

## 一、概述

消息与验证码服务位于 `zmbdp-common-message` 模块，对外提供统一的验证码发送、获取、校验、删除能力，业务方只需调用 `CaptchaService` 即可完成完整的验证码流程，无需关心底层发送渠道差异。

**核心能力：**

- 验证码生成与发送（手机号自动走短信、邮箱自动走邮件）
- Redis 存储 + 过期自动清理
- 每日发送次数限制 + 每分钟频繁发送限流（IP + 账号双维度）
- 开发环境固定验证码机制（不实际发送，避免短信/邮件成本）
- 配置热更新（`@RefreshScope` + Nacos）

**核心类清单：**

| 类型 | 类名 | 说明 |
| --- | --- | --- |
| Service | `CaptchaService` | 验证码服务入口，提供 sendCode/getCode/checkCode/deleteCode |
| Router | `CaptchaSenderRouter` | 策略路由器，根据账号格式选择发送策略 |
| Interface | `ICaptchaSenderStrategy` | 发送策略接口（supports + sendCode） |
| Impl | `AliSmsServiceStrategy` | 阿里云短信发送策略（手机号） |
| Impl | `EmailCodeServiceStrategy` | 邮件验证码发送策略（邮箱） |
| Config | `AliSmsConfig` | 阿里云短信客户端 Bean 配置 |
| Config | `MailCodeProperties` | 邮件标题/内容模板列表配置（`@ConfigurationProperties(prefix = "mail.code")`） |
| Constants | `MessageConstants` | Redis Key 前缀、默认验证码、成功响应码等常量 |

所有核心类通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动装配，引入 `zmbdp-common-message` 依赖即可使用。

## 二、架构设计

### 2.1 策略模式分层

frameworkJava 采用**策略模式 + 路由器**实现验证码发送渠道的解耦，调用链如下：

```
业务层 (UserService / Controller)
  │
  ▼
CaptchaService.sendCode(account)
  │  ① 校验每日发送次数
  │  ② 生成验证码（固定 or 随机）
  │  ③ 调用路由器发送
  │  ④ 写入 Redis（验证码 + 次数）
  ▼
CaptchaSenderRouter.sendCode(account, code)
  │  遍历 List<ICaptchaSenderStrategy>
  │  调用 supports(account) 找到第一个匹配的策略
  ▼
ICaptchaSenderStrategy 实现类
  ├── AliSmsServiceStrategy   (手机号 → 阿里云短信)
  └── EmailCodeServiceStrategy (邮箱   → SMTP 邮件)
```

### 2.2 路由器设计

`CaptchaSenderRouter` 通过 Spring 自动注入收集所有 `ICaptchaSenderStrategy` 实现，发送时按注入顺序遍历，**找到第一个 `supports(account)` 返回 true 的策略**即调用其 `sendCode`，结果直接透传；若所有策略都不支持，抛出 `IllegalArgumentException`。

```java
@Autowired
private List<ICaptchaSenderStrategy> captchaSenderStrategies;

public boolean sendCode(String account, String code) {
    for (ICaptchaSenderStrategy sender : captchaSenderStrategies) {
        if (sender.supports(account)) {
            return sender.sendCode(account, code);
        }
    }
    throw new IllegalArgumentException("不支持的账号类型: " + account);
}
```

路由器只负责路由和调用，不处理业务逻辑，发送失败只返回 `false`，不抛异常。

### 2.3 策略接口

`ICaptchaSenderStrategy` 定义两个方法：

```java
public interface ICaptchaSenderStrategy {
    // 是否支持当前账号类型（手机号 / 邮箱 / 其他）
    boolean supports(String account);
    // 发送验证码，返回是否发送成功（失败不抛异常）
    boolean sendCode(String account, String code);
}
```

每个实现类自行判断是否支持当前账号格式，路由器据此完成自动选择。

## 三、验证码服务（CaptchaService）

`CaptchaService` 是业务方唯一需要接触的入口，标注 `@Service` 和 `@RefreshScope`，配置项支持 Nacos 热更新。

### 3.1 sendCode：发送验证码

```java
@RateLimit(
        limit = 1,                              // 时间窗口内阈值
        windowSec = 60,                          // 限流时间窗口（秒）
        dimensions = RateLimitDimension.BOTH,    // IP + 账号双维度
        ipHeaderName = "X-Send-Code-IP",         // IP 限流时使用的 HTTP 头
        message = "操作过于频繁，请稍后重试"
)
public String sendCode(String account) {
    // ① 校验每日发送次数
    String limitCacheKey = MessageConstants.CAPTCHA_CODE_TIMES_KEY + account;
    Integer times = redisService.getCacheObject(limitCacheKey, Integer.class);
    times = times == null ? 0 : times;
    if (times >= sendLimit) {
        throw new ServiceException(ResultCode.SEND_MSG_FAILED);
    }

    // ② 生成验证码（固定 or 随机）
    String verifyCode = shouldUseFixedCode(account)
            ? MessageConstants.DEFAULT_CAPTCHA_CODE
            : VerifyUtil.generateVerifyCode(MessageConstants.DEFAULT_CAPTCHA_LENGTH, captChaType);

    // ③ 调用路由器发送
    if (sendMessage) {
        boolean result = captchaSenderRouter.sendCode(account, verifyCode);
        if (!result && !shouldIgnoreSendFailure(account)) {
            throw new ServiceException(ResultCode.SEND_MSG_FAILED);
        }
    }
    // ④ 写入 Redis（验证码 + 发送次数）
    redisService.setCacheObject(codeKey, verifyCode, accountCodeExpiration, TimeUnit.MINUTES);
    long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
            LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
    redisService.setCacheObject(limitCacheKey, times + 1, seconds, TimeUnit.SECONDS);
    return verifyCode;
}
```

**关键行为：**

- 返回值即为生成的验证码（便于测试场景或前端直接展示）
- 每日次数超限抛 `ServiceException(SEND_MSG_FAILED)`
- 频繁发送由 `@RateLimit` 注解拦截，触发限流抛 `ServiceException(REQUEST_TOO_FREQUENT)`
- 使用固定验证码时，发送失败不会抛异常（`shouldIgnoreSendFailure` 直接复用 `shouldUseFixedCode`）

### 3.2 getCode：获取缓存中的验证码

```java
public String getCode(String account) {
    String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + account;
    return redisService.getCacheObject(cacheKey, String.class);
}
```

主要用于测试场景查询当前 Redis 中的验证码；验证码不存在或已过期返回 `null`。

### 3.3 checkCode：校验验证码

```java
public boolean checkCode(String account, String code) {
    String cacheCode = getCode(account);
    if (code == null || StringUtil.isEmpty(code) || cacheCode == null || StringUtil.isEmpty(cacheCode)) {
        throw new ServiceException(ResultCode.INVALID_CODE);
    }
    return cacheCode.equals(code);
}
```

- 账号或验证码为空、缓存中无验证码 → 抛 `ServiceException(INVALID_CODE)`
- 比较区分大小写
- **不会自动删除**验证码，调用方需自行调用 `deleteCode` 保证一次性使用

### 3.4 deleteCode：删除验证码

```java
public boolean deleteCode(String account) {
    String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + account;
    return redisService.deleteObject(cacheKey);
}
```

验证成功后应立即调用，防止验证码被重复使用。

## 四、限流机制

### 4.1 @RateLimit 注解（频繁发送限制）

`sendCode` 方法上的 `@RateLimit` 注解基于 **AOP + Redis** 实现，参数如下：

| 参数 | 值 | 含义 |
| --- | --- | --- |
| `limit` | `1` | 时间窗口内允许的请求数 |
| `windowSec` | `60` | 时间窗口大小（秒），即 1 分钟 |
| `dimensions` | `BOTH` | IP + 账号双维度限流，任一维度超限即拒绝 |
| `ipHeaderName` | `X-Send-Code-IP` | IP 限流时使用的 HTTP 头名称 |
| `message` | 操作过于频繁，请稍后重试 | 触发限流的提示语 |

**双维度限流说明：**

- 已登录场景：IP 与账号（userId）同时计数，任一超限即拒绝
- 未登录场景：账号维度自动退化为 IP 限流，且 `identityKey == ipKey`，只计数一次，避免重复扣减
- 无 HTTP 请求上下文（如定时任务、单元测试、内部直接调用）：跳过限流，直接放行

### 4.2 每日发送次数限制

通过 `captcha.send-limit` 控制单个账号每日最大发送次数（默认 50），超过后抛 `ServiceException(SEND_MSG_FAILED)`。计数 Key 为 `captcha:times:{account}`，过期时间为「当前时刻到次日 0 点」的秒数。

## 五、固定验证码机制

为了在开发/测试环境避免实际发送短信和邮件，`CaptchaService` 提供「固定验证码」机制。固定验证码值为 `MessageConstants.DEFAULT_CAPTCHA_CODE = "123456"`。

`shouldUseFixedCode(account)` 在以下**三种场景**返回 `true`，使用固定验证码：

| 场景 | 触发条件 | 说明 |
| --- | --- | --- |
| ① 总开关关闭 | `captcha.send-message = false` | 完全不发短信/邮件，使用固定验证码 |
| ② 短信通道关闭 | 账号为手机号且 `sms.send-message = false` | 手机号走固定验证码 |
| ③ 邮件通道关闭 | 账号为邮箱且 `mail.send-message = false` | 邮箱走固定验证码 |

```java
private boolean shouldUseFixedCode(String account) {
    if (!sendMessage) {                                            // ① 总开关关闭
        return true;
    }
    if (VerifyUtil.checkPhone(account) && !smsSendMessage) {       // ② 手机号 + 短信通道关闭
        return true;
    }
    return VerifyUtil.checkEmail(account) && !mailSendMessage;     // ③ 邮箱 + 邮件通道关闭
}
```

**配套的失败容忍：** 使用固定验证码时，即使路由器发送返回 `false`，也不会抛异常（`shouldIgnoreSendFailure` 直接复用 `shouldUseFixedCode`），保证开发环境流程不被发送失败阻塞。

## 六、短信策略（AliSmsServiceStrategy）

`AliSmsServiceStrategy` 实现 `ICaptchaSenderStrategy`，标注 `@Component` 和 `@RefreshScope`，通过阿里云短信 SDK（`com.aliyun.dysmsapi20170525.Client`）发送短信验证码。

### 6.1 supports

```java
@Override
public boolean supports(String account) {
    return VerifyUtil.checkPhone(account);
}
```

使用 `VerifyUtil.checkPhone` 校验中国大陆手机号格式（正则：`^1[3-9]\d{9}$`）。

### 6.2 sendCode

```java
@Override
public boolean sendCode(String phone, String code) {
    log.info("开始发送短信验证码, 账号：{}", phone);
    Map<String, String> params = new HashMap<>();
    params.put("code", code);
    return sendTemMessage(phone, templateCode, params);
}
```

### 6.3 sendTemMessage（实际发送）

```java
private boolean sendTemMessage(String phone, String templateCode, Map<String, String> params) {
    // 把是否发送线上短信交给 nacos 管理
    if (!sendMessage) {
        log.error("短信发送通道关闭, {}", phone);
        return false;
    }
    SendSmsRequest sendSmsRequest = new SendSmsRequest();
    sendSmsRequest.setPhoneNumbers(phone);
    sendSmsRequest.setSignName(signName);
    sendSmsRequest.setTemplateCode(templateCode);
    sendSmsRequest.setTemplateParam(JsonUtil.classToJson(params));
    try {
        SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);
        SendSmsResponseBody responseBody = sendSmsResponse.getBody();
        if (responseBody.getCode().equals(MessageConstants.CAPTCHA_MSG_OK)) {
            return true;
        }
        log.error("短信: {} 发送失败, 失败原因: {}...", new Gson().toJson(sendSmsRequest), responseBody.getMessage());
        return false;
    } catch (Exception e) {
        log.error("短信: {} 发送失败, 失败原因: {}...", new Gson().toJson(sendSmsRequest), e.getMessage());
        return false;
    }
}
```

**关键行为：**

- `sendMessage = false`（`sms.send-message=false`）时直接返回 `false`，不调用阿里云 API
- 模板参数以 JSON 形式传入（如 `{"code":"123456"}`），模板中通过 `${code}` 占位符引用
- 成功响应码为 `MessageConstants.CAPTCHA_MSG_OK = "OK"`
- 异常捕获后只记录日志、返回 `false`，不向上抛出

### 6.4 阿里云客户端配置（AliSmsConfig）

`AliSmsConfig` 通过 `@Bean("aliClient")` 注册阿里云短信 `Client`：

```java
@RefreshScope
@Configuration
public class AliSmsConfig {
    @Value("${sms.aliyun.accessKeyId:}")
    private String accessKeyId;
    @Value("${sms.aliyun.accessKeySecret:}")
    private String accessKeySecret;
    @Value("${sms.aliyun.endpoint:}")
    private String endpoint;

    @Bean("aliClient")
    public Client client() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }
}
```

## 七、邮件策略（EmailCodeServiceStrategy）

`EmailCodeServiceStrategy` 实现 `ICaptchaSenderStrategy`，标注 `@Component` 和 `@RefreshScope`，通过 `MailUtil.sendHtml` 发送 HTML 邮件。

### 7.1 supports

```java
@Override
public boolean supports(String account) {
    return VerifyUtil.checkEmail(account);
}
```

使用 `VerifyUtil.checkEmail` 校验标准邮箱格式（正则：`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`）。

### 7.2 sendCode（模板随机选择）

```java
@Override
public boolean sendCode(String email, String code) {
    log.info("开始发送邮件验证码, 账号: {}", email);
    // 把是否发送邮件交给 nacos 管理
    if (!sendMessage) {
        log.error("邮件发送通道关闭, {}", email);
        return false;
    }
    // 从列表中随机选择一个标题
    String subject = getRandomItem(mailCodeProperties.getSubject(), "验证码");
    // 从列表中随机选择一个内容模板
    String contentTemplate = getRandomItem(mailCodeProperties.getContent(), "您的验证码是：{code}，请勿泄露给他人。");
    // 构建邮件内容（替换占位符）
    String content = contentTemplate.replace("{code}", code);
    // 发送邮件
    try {
        MailUtil.sendHtml(email, subject, content);
        return true;
    } catch (Exception e) {
        Map<String, Object> logInfo = new HashMap<>();
        logInfo.put("to", email);
        logInfo.put("subject", subject);
        log.error("邮件: {} 发送失败, 失败原因: {}...", new Gson().toJson(logInfo), e.getMessage());
        return false;
    }
}
```

**关键行为：**

- `sendMessage`（配置项 `captcha.send-message`）为 `false` 时直接返回 `false`
- 标题和内容模板均支持 YAML 列表配置，发送时**随机选择一个**，增加多样性、规避垃圾邮件识别
- 内容模板中的 `{code}` 占位符会被实际验证码替换
- 未配置标题/内容时使用默认值（"验证码" 和 "您的验证码是：{code}，请勿泄露给他人。"）
- 发送异常只记录日志、返回 `false`，不向上抛出

### 7.3 getRandomItem（列表随机选择）

```java
private String getRandomItem(List<String> list, String defaultValue) {
    if (list == null || list.isEmpty()) {
        return defaultValue;
    }
    List<String> processedList = list.stream()
            .map(StrUtil::trim)
            .filter(StrUtil::isNotBlank)
            .toList();
    if (processedList.isEmpty()) {
        return defaultValue;
    }
    if (processedList.size() == 1) {
        return processedList.get(0);
    }
    int index = RANDOM.nextInt(processedList.size());
    return processedList.get(index);
}
```

处理逻辑：空列表 → 默认值；trim + 过滤空白；单元素直接返回；多元素 `Random.nextInt` 随机。

### 7.4 MailCodeProperties 配置类

```java
@Data
@Slf4j
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mail.code")
public class MailCodeProperties {
    /** 邮件标题列表（支持 YAML 列表格式） */
    private List<String> subject = new ArrayList<>();
    /** 邮件内容模板列表（支持 {code} 占位符，支持 YAML 列表格式） */
    private List<String> content = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("邮件验证码配置加载 - subject数量: {}, content数量: {}",
                subject != null ? subject.size() : 0,
                content != null ? content.size() : 0);
    }
}
```

## 八、Redis Key 设计

所有验证码相关数据存储在 Redis 中，Key 前缀定义在 `MessageConstants`：

| 常量名 | 值 | 用途 | TTL |
| --- | --- | --- | --- |
| `CAPTCHA_CODE_KEY` | `captcha:code:` | 验证码本体，拼接账号后形如 `captcha:code:13800138000` | `captcha.code-expiration` 分钟（默认 5） |
| `CAPTCHA_CODE_TIMES_KEY` | `captcha:times:` | 当日发送次数，拼接账号后形如 `captcha:times:13800138000` | 当日剩余秒数（次日 0 点过期） |

**其他相关常量：**

| 常量名 | 值 | 含义 |
| --- | --- | --- |
| `CAPTCHA_MSG_OK` | `"OK"` | 阿里云短信发送成功的响应码 |
| `DEFAULT_CAPTCHA_LENGTH` | `6` | 随机验证码默认长度 |
| `DEFAULT_CAPTCHA_CODE` | `"123456"` | 固定验证码（开发环境使用） |

**Key 操作示例：**

```java
// 写入验证码（5 分钟过期）
redisService.setCacheObject("captcha:code:" + account, code, 5L, TimeUnit.MINUTES);

// 读取验证码
String code = redisService.getCacheObject("captcha:code:" + account, String.class);

// 写入当日发送次数（次日 0 点过期）
redisService.setCacheObject("captcha:times:" + account, times + 1, secondsUntilMidnight, TimeUnit.SECONDS);

// 删除验证码（验证成功后调用）
redisService.deleteObject("captcha:code:" + account);
```

## 九、每日次数重置机制

每日发送次数的 TTL 计算**没有使用固定的 24 小时**，而是精确计算「当前时刻到次日 0 点」的秒数，确保每日 0 点准时重置：

```java
long seconds = ChronoUnit.SECONDS.between(
        LocalDateTime.now(),
        LocalDateTime.now()
                .plusDays(1)           // 加一天
                .withHour(0)           // 设置小时为 0
                .withMinute(0)         // 设置分钟为 0
                .withSecond(0)         // 设置秒为 0
                .withNano(0)           // 设置纳秒为 0
);
redisService.setCacheObject(limitCacheKey, times + 1, seconds, TimeUnit.SECONDS);
```

**示例：** 若当前时间为 `2026-07-19 22:30:00`，则 `seconds` 为次日 `2026-07-20 00:00:00` 与当前的差值 = 5400 秒（1.5 小时）。这样无论何时发送验证码，次数缓存都会在次日 0 点准时过期，无需额外的定时清理任务。

## 十、配置项清单

### 10.1 验证码总开关配置（share-captcha-dev.yaml）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `captcha.type` | `2` | 验证码类型：1=纯数字；2=大小写字母+数字；3=大小写字母+数字+特殊字符 |
| `captcha.code-expiration` | `5` | 验证码有效期（分钟） |
| `captcha.send-limit` | `50` | 单个账号每日发送次数上限 |
| `captcha.send-message` | `true` | 验证码发送总开关。false 时使用固定验证码，不调用任何发送渠道 |
| `sms.send-message` | `false` | 短信通道开关。false 时手机号走固定验证码；`AliSmsServiceStrategy` 内部也会再次检查 |
| `sms.sign-name` | `FrameWork-Java` | 阿里云短信签名 |
| `sms.aliyun.accessKeyId` | （空） | 阿里云 AccessKey ID |
| `sms.aliyun.accessKeySecret` | （空） | 阿里云 AccessKey Secret |
| `sms.aliyun.endpoint` | `dysmsapi.aliyuncs.com` | 阿里云短信服务 endpoint |
| `sms.aliyun.templateCode` | （空） | 阿里云短信模板 ID（模板中需包含 `${code}` 占位符） |
| `mail.send-message` | `false` | 邮件通道开关。false 时邮箱走固定验证码 |
| `mail.code.subject` | （列表） | 邮件标题列表，发送时随机选择一个 |
| `mail.code.content` | （列表） | 邮件内容模板列表，发送时随机选择一个，支持 `{code}` 占位符 |

### 10.2 邮件服务配置（share-mail-dev.yaml）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `mail.isEnabled` | `true` | 是否启用邮件功能 |
| `mail.from` | （配置） | 发件人邮箱（可包含显示名称） |
| `mail.user` | （配置） | SMTP 登录用户名（QQ 邮箱为 QQ 号@qq.com） |
| `mail.pass` | （配置） | 邮箱授权码（不是登录密码） |
| `mail.host` | `smtp.qq.com` | SMTP 服务器地址 |
| `mail.port` | `465` | SMTP 端口（465 为 SSL 端口） |
| `mail.ssl-enable` | `true` | 是否开启 SSL |

### 10.3 Nacos 配置示例（share-captcha-dev.yaml 节选）

```yaml
captcha:
  # 验证码类型，1: 纯数字; 2: 大小写字母+数字; 3: 大小写字母+数字+特殊字符
  type: 3
  # 验证码有效期（分钟）
  code-expiration: 5
  # 单个手机号，每日发送短信次数的限制
  send-limit: 50
  sender:
    # 发送类型：sms: 阿里云短信发送，mail: 邮件发送
    type: mail

sms:
  # 是否开启短信发送功能
  send-message: false
  # 短信签名
  sign-name: FrameWork-Java
  aliyun:
    accessKeyId: # 你的 accessKeyId
    accessKeySecret: # 你的 accessKeySecret
    endpoint: dysmsapi.aliyuncs.com
    templateCode: # 你的短信模板 ID

mail:
  # 是否开启邮箱发送功能
  send-message: false
  code:
    # 邮件标题列表（发送时会随机选择一个）
    subject:
      - "scaffold-ai-assistant 验证码"
      - "scaffold-ai-assistant 安全验证码"
      - "scaffold-ai-assistant 登录验证码"
      - "【scaffold-ai-assistant】验证码通知"
      - "【scaffold-ai-assistant】安全验证"
      - "scaffold-ai-assistant 账户安全验证"
      - "【scaffold-ai-assistant】登录验证通知"
      - "scaffold-ai-assistant 系统验证码"
    # 邮件内容模板列表（发送时会随机选择一个，支持 {code} 占位符）
    content:
      - "您的验证码是：{code}，请勿泄露给他人。"
      - "验证码：{code}，5分钟内有效，请勿泄露给他人。"
      - "您正在进行安全验证，验证码为：{code}，请妥善保管。"
      - "您的登录验证码是 {code}，有效期为5分钟，请勿泄露。"
```

## 十一、扩展点：新增发送渠道

frameworkJava 的策略模式让新增发送渠道（如微信、QQ、企业微信等）变得非常简单，**只需两步**，无需修改 `CaptchaService` 和 `CaptchaSenderRouter` 任何代码。

### 11.1 实现步骤

1. **实现 `ICaptchaSenderStrategy` 接口**，编写 `supports` 和 `sendCode` 方法
2. **标注 `@Component`**（必要时加 `@RefreshScope`），让 Spring 自动注入到 `CaptchaSenderRouter` 的策略列表

### 11.2 示例：新增微信渠道

```java
@Slf4j
@Component
@RefreshScope
public class WeChatCaptchaStrategy implements ICaptchaSenderStrategy {

    @Value("${wechat.send-message:false}")
    private boolean sendMessage;

    @Override
    public boolean supports(String account) {
        // 假设微信号以 wx: 开头，或通过其他方式识别
        return account != null && account.startsWith("wx:");
    }

    @Override
    public boolean sendCode(String account, String code) {
        if (!sendMessage) {
            log.error("微信发送通道关闭, {}", account);
            return false;
        }
        try {
            // 调用微信 API 发送验证码
            return weChatApi.send(account, code);
        } catch (Exception e) {
            log.error("微信: {} 发送失败, 失败原因: {}", account, e.getMessage());
            return false;
        }
    }
}
```

### 11.3 设计原则

- **开闭原则**：对扩展开放，对修改关闭。新增渠道不修改既有代码
- **单一职责**：每个策略只负责一种发送渠道
- **自动装配**：Spring 会自动将新的策略实现注入到 `CaptchaSenderRouter.captchaSenderStrategies` 列表
- **顺序敏感**：策略按 Spring 注入顺序遍历，找到第一个 `supports` 返回 true 的即使用。若多个策略可能同时支持同一账号格式，需通过 `@Order` 控制优先级

## 十二、使用示例

### 12.1 注入并调用 CaptchaService

```java
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 发送验证码（支持手机号或邮箱，根据输入格式自动判断）
     */
    @Override
    public String sendCode(String account) {
        return captchaService.sendCode(account);
    }
}
```

### 12.2 验证码登录场景（校验 + 删除）

来自 frameworkJava 内部 `CodeLoginStrategy` 的实际使用方式：

```java
@Slf4j
@Component
public class CodeLoginStrategy implements ILoginStrategy {

    @Autowired
    private CaptchaService captchaService;

    @Override
    public LoginUserDTO login(LoginDTO loginDTO) {
        CodeLoginDTO codeLoginDTO = (CodeLoginDTO) loginDTO;
        String account = codeLoginDTO.getAccount();

        // ① 校验验证码
        if (!captchaService.checkCode(account, codeLoginDTO.getCode())) {
            throw new ServiceException(ResultCode.ERROR_CODE);
        }
        // ② 校验通过后立即删除，保证一次性使用
        if (!captchaService.deleteCode(account)) {
            log.warn("验证码删除失败！手机号/邮箱: {}", account);
        }
        // ③ 后续登录逻辑...
        return loginUserDTO;
    }
}
```

### 12.3 完整调用流程示例

```java
// 1. 发送验证码（用户输入手机号或邮箱，自动路由到对应渠道）
String code = captchaService.sendCode("13800138000");
// 开发环境（sms.send-message=false）→ 返回固定验证码 "123456"
// 生产环境（sms.send-message=true）→ 返回随机验证码并发送短信

// 2. 测试场景：从缓存读取当前验证码
String cachedCode = captchaService.getCode("13800138000");

// 3. 用户输入验证码后校验
boolean valid = captchaService.checkCode("13800138000", userInputCode);

// 4. 校验成功后删除（一次性使用）
if (valid) {
    captchaService.deleteCode("13800138000");
}
```

### 12.4 开发环境快速联调

开发环境推荐如下配置，无需真实短信/邮件服务即可完成全流程联调：

```yaml
captcha:
  send-message: false    # 总开关关闭，所有账号走固定验证码 123456
# 或者只关闭特定通道
# sms.send-message: false   # 手机号走固定验证码
# mail.send-message: false  # 邮箱走固定验证码
```

此时调用 `captchaService.sendCode(account)` 始终返回 `"123456"`，前端直接使用该验证码即可完成登录/注册联调。
