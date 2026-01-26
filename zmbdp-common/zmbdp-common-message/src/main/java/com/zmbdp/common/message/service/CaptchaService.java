package com.zmbdp.common.message.service;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.MessageConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.message.router.CaptchaSenderRouter;
import com.zmbdp.common.message.strategy.ICaptchaSenderStrategy;
import com.zmbdp.common.ratelimit.annotation.RateLimit;
import com.zmbdp.common.ratelimit.enums.RateLimitDimension;
import com.zmbdp.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * <p>
 * 提供验证码的生成、发送、存储、校验等功能。
 * 支持短信和邮件两种发送方式，根据账号格式自动选择发送器。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>发送验证码：支持短信和邮件两种方式</li>
 *     <li>验证码存储：使用 Redis 存储验证码和发送次数</li>
 *     <li>发送限制：每日发送次数限制、频繁发送限制</li>
 *     <li>固定验证码：开发/测试环境可使用固定验证码，避免发送真实短信/邮件</li>
 *     <li>验证码校验：验证用户输入的验证码是否正确</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 *
 * <pre>{@code
 * // 发送验证码
 * String code = captchaService.sendCode("13800138000");
 *
 * // 校验验证码
 * boolean valid = captchaService.checkCode("13800138000", "123456");
 *
 * // 获取验证码（用于测试）
 * String cachedCode = captchaService.getCode("13800138000");
 *
 * // 删除验证码
 * captchaService.deleteCode("13800138000");
 * }</pre>
 * <p>
 * <b>配置说明：</b>
 * <ul>
 *     <li>captcha.send-limit：每日发送次数限制（默认 50）</li>
 *     <li>captcha.code-expiration：验证码有效期，单位分钟（默认 5）</li>
 *     <li>captcha.send-message：是否发送验证码（默认 true）</li>
 *     <li>captcha.type：验证码类型（默认 2，数字+字母）</li>
 *     <li>sms.send-message：短信通道是否开启（默认 false）</li>
 *     <li>mail.send-message：邮件通道是否开启（默认 false）</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>支持配置热更新（@RefreshScope）</li>
 *     <li>验证码存储在 Redis 中，有效期可配置</li>
 *     <li>发送次数限制按账号（手机号/邮箱）统计，每日重置</li>
 *     <li>频繁发送限制：1 分钟内只能发送一次</li>
 *     <li>如果使用固定验证码，发送失败不会抛出异常</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see CaptchaSenderRouter
 * @see ICaptchaSenderStrategy
 */
@Service
@RefreshScope
public class CaptchaService {

    /**
     * Redis 服务类
     * <p>
     * 用于存储验证码和发送次数限制。
     */
    @Autowired
    private RedisService redisService;

    /**
     * 单个手机号/邮箱，每日发送验证码次数的限制
     * <p>
     * 配置项：captcha.send-limit，默认值为 50。
     * 超过限制后，当日无法再发送验证码，需要等到次日重置。
     */
    @Value("${captcha.send-limit:50}")
    private Integer sendLimit;

    /**
     * 验证码的有效期，单位是分钟
     * <p>
     * 配置项：captcha.code-expiration，默认值为 5 分钟。
     * 验证码过期后无法使用，需要重新发送。
     */
    @Value("${captcha.code-expiration:5}")
    private Long accountCodeExpiration;

    /**
     * 验证码发送总开关
     * <p>
     * 配置项：captcha.send-message，默认值为 true。
     * <ul>
     *     <li>true：发送验证码（根据通道配置决定发送短信或邮件）</li>
     *     <li>false：不发送验证码，使用固定验证码</li>
     * </ul>
     */
    @Value("${captcha.send-message:true}")
    private boolean sendMessage;

    /**
     * 判断生成什么难度的验证码
     */
    @Value("${captcha.type:2}")
    private Integer captChaType;

    /**
     * 短信服务是否发送线上短信
     * <p>
     * 配置项：sms.send-message，默认值为 false。
     * <ul>
     *     <li>true：发送真实短信（生产环境）</li>
     *     <li>false：不发送短信，使用固定验证码（开发/测试环境）</li>
     * </ul>
     * 此配置对应 AliSmsServiceStrategy 的 sendMessage 配置。
     */
    @Value("${sms.send-message:false}")
    private boolean smsSendMessage;

    /**
     * 邮件服务是否发送邮件（EmailCodeServiceStrategy 的 sendMessage 配置）
     */
    @Value("${mail.send-message:false}")
    private boolean mailSendMessage;

    /**
     * 验证码发送器路由器
     * <p>
     * 根据账号格式（手机号或邮箱）自动选择对应的发送器：
     * <ul>
     *     <li>手机号：使用短信发送器（AliSmsServiceStrategy）</li>
     *     <li>邮箱：使用邮件发送器（EmailCodeServiceStrategy）</li>
     * </ul>
     */
    @Autowired
    private CaptchaSenderRouter captchaSenderRouter;

    /**
     * 发送验证码
     * <p>
     * <b>限流说明：</b>
     * <ul>
     *     <li>双维度限流（IP + 账号），任一维度超限即拒绝</li>
     *     <li>限流阈值：每分钟 3 次</li>
     *     <li>未登录时自动退化为 IP 限流</li>
     *     <li><b>内部调用行为：</b>
     * <ul>
     *     <li>使用 AspectJ 切面，可以拦截内部调用（如 {@code this.sendCode()} 或 {@code captchaService.sendCode()}）</li>
     *     <li>但如果没有 HTTP 请求上下文（如定时任务、异步任务、单元测试），会跳过限流，直接放行</li>
     *     <li>只有通过 HTTP 请求调用（Controller -> Service）时，限流才会生效</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param account 手机号 / 邮箱
     * @return 验证码
     */
    @RateLimit(
            limit = 1, // windowSec 时间内 阈值
            windowSec = 60, // 限流时间窗口，单位秒
            dimensions = RateLimitDimension.BOTH, // 限流维度
            ipHeaderName = "X-Send-Code-IP", // IP 限流时使用的 HTTP 头名称
            message = "操作过于频繁，请稍后重试" // 限流提示语
    )
    public String sendCode(String account) {
        // 先校验是否超过每日的发送限制（针对每个手机号/邮箱）
        String limitCacheKey = MessageConstants.CAPTCHA_CODE_TIMES_KEY + account;
        Integer times = redisService.getCacheObject(limitCacheKey, Integer.class);
        times = times == null ? 0 : times;
        if (times >= sendLimit) {
            throw new ServiceException(ResultCode.SEND_MSG_FAILED);
        }

        // 然后判断是否在 1 分钟内频繁发送
        String codeKey = MessageConstants.CAPTCHA_CODE_KEY + account;
        // 如果不想用注解的话用下面注释起来的这个也行，
        // 如果就只是限制验证码发送频率的话，强行用注解有点过度设计了
//        String cacheValue = redisService.getCacheObject(codeKey, String.class);
//        long expireTime = redisService.getExpire(codeKey);
//        if (!StringUtil.isEmpty(cacheValue) && expireTime > accountCodeExpiration * 60 - 60) {
//            long time = expireTime - accountCodeExpiration * 60 + 60;
//            throw new ServiceException("操作频繁, 请在 " + time + " 秒之后重试", ResultCode.INVALID_PARA.getCode());
//        }

        // 生成验证码（根据配置决定使用固定验证码还是随机验证码）
        String verifyCode = shouldUseFixedCode(account)
                ? MessageConstants.DEFAULT_CAPTCHA_CODE
                : VerifyUtil.generateVerifyCode(MessageConstants.DEFAULT_CAPTCHA_LENGTH, captChaType);

        // 发送线上短信/邮件（根据账号格式自动选择发送器）
        if (sendMessage) {
            boolean result = captchaSenderRouter.sendCode(account, verifyCode);
            // 发送失败时，如果使用了固定验证码，则不抛异常（允许失败）
            if (!result && !shouldIgnoreSendFailure(account)) {
                throw new ServiceException(ResultCode.SEND_MSG_FAILED);
            }
        }
        // 设置验证码的缓存
        redisService.setCacheObject(codeKey, verifyCode, accountCodeExpiration, TimeUnit.MINUTES);
        // 设置发送次数限制的缓存 （无法预先设置缓存，只能先读后写）
        long seconds = ChronoUnit.SECONDS.between(
                LocalDateTime.now(),
                LocalDateTime.now()
                        .plusDays(1).withHour(0).withMinute(0)
                        .withSecond(0).withNano(0)
        );
        redisService.setCacheObject(limitCacheKey, times + 1, seconds, TimeUnit.SECONDS);
        return verifyCode;
    }

    /**
     * 从缓存中获取手机号/邮箱的验证码
     * <p>
     * 从 Redis 中获取指定账号的验证码。适用于测试场景或验证码查询。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取验证码（用于测试）
     * String code = captchaService.getCode("13800138000");
     * if (code != null) {
     *     log.info("当前验证码: {}", code);
     * } else {
     *     log.info("验证码不存在或已过期");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果验证码不存在或已过期，返回 null</li>
     *     <li>如果 account 为 null，可能抛出异常</li>
     *     <li>验证码存储在 Redis 中，有过期时间</li>
     *     <li>主要用于测试和调试场景</li>
     * </ul>
     *
     * @param account 手机号或邮箱，不能为 null
     * @return 验证码，如果不存在或已过期返回 null
     * @see #sendCode(String)
     * @see #checkCode(String, String)
     * @see #deleteCode(String)
     */
    public String getCode(String account) {
        String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + account;
        return redisService.getCacheObject(cacheKey, String.class);
    }

    /**
     * 从缓存中删除手机号/邮箱的验证码
     *
     * @param account 手机号/邮箱
     * @return 验证码
     */
    public boolean deleteCode(String account) {
        String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + account;
        return redisService.deleteObject(cacheKey);
    }

    /**
     * 校验手机号/邮箱与验证码是否匹配
     * <p>
     * 验证用户输入的验证码是否正确。从 Redis 中获取存储的验证码，与用户输入的验证码进行比较。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 校验验证码
     * boolean valid = captchaService.checkCode("13800138000", "123456");
     * if (valid) {
     *     log.info("验证码正确");
     *     // 验证成功后删除验证码
     *     captchaService.deleteCode("13800138000");
     * } else {
     *     log.warn("验证码错误");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 account 或 code 为 null 或空字符串，抛出 ServiceException（ResultCode.INVALID_CODE）</li>
     *     <li>如果验证码不存在或已过期，抛出 ServiceException（ResultCode.INVALID_CODE）</li>
     *     <li>验证码比较区分大小写</li>
     *     <li>验证成功后建议立即删除验证码，防止重复使用</li>
     *     <li>验证码只能使用一次（验证成功后应删除）</li>
     * </ul>
     *
     * @param account 手机号或邮箱，不能为 null 或空字符串
     * @param code    用户输入的验证码，不能为 null 或空字符串
     * @return true 表示验证码正确，false 表示验证码错误
     * @throws ServiceException 如果 account 或 code 为空，或验证码不存在
     * @see #sendCode(String)
     * @see #getCode(String)
     * @see #deleteCode(String)
     */
    public boolean checkCode(String account, String code) {
        String cacheCode = getCode(account);
        if (code == null || StringUtil.isEmpty(code) || cacheCode == null || StringUtil.isEmpty(cacheCode)) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        return cacheCode.equals(code);
    }

    /**
     * 判断是否应该使用固定验证码
     * <p>
     * 根据配置判断是否使用固定验证码。如果总开关关闭或对应通道关闭，则使用固定验证码。
     * <p>
     * <b>使用固定验证码的情况：</b>
     * <ul>
     *     <li>sendMessage 为 false（总开关关闭）</li>
     *     <li>sendMessage 为 true，但手机号的 sms.send-message 为 false（短信通道关闭）</li>
     *     <li>sendMessage 为 true，但邮箱的 mail.send-message 为 false（邮件通道关闭）</li>
     * </ul>
     *
     * @param account 账号（手机号或邮箱），不能为 null
     * @return true 表示使用固定验证码，false 表示生成随机验证码
     * @see #shouldIgnoreSendFailure(String)
     * @see com.zmbdp.common.domain.constants.MessageConstants#DEFAULT_CAPTCHA_CODE
     */
    private boolean shouldUseFixedCode(String account) {
        // 总开关关闭，使用固定验证码
        if (!sendMessage) {
            return true;
        }
        // 用户输入的是手机号且短信通道关闭，使用固定验证码
        if (VerifyUtil.checkPhone(account) && !smsSendMessage) {
            return true;
        }
        // 用户输入的是邮箱且邮件通道关闭，使用固定验证码
        return VerifyUtil.checkEmail(account) && !mailSendMessage;
    }

    /**
     * 判断发送失败时是否应该忽略异常
     * <p>
     * 如果使用了固定验证码，则发送失败时应该忽略异常（因为验证码已经固定，用户可以直接使用）。
     * 此方法直接调用 {@link #shouldUseFixedCode(String)} 方法。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>在开发/测试环境使用固定验证码时，发送失败不影响功能</li>
     *     <li>在生产环境使用随机验证码时，发送失败必须抛出异常</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果使用固定验证码，返回 true（忽略发送失败）</li>
     *     <li>如果使用随机验证码，返回 false（发送失败时抛出异常）</li>
     *     <li>此方法用于 sendCode 方法中的异常处理逻辑</li>
     * </ul>
     *
     * @param account 账号（手机号或邮箱），不能为 null
     * @return true 表示忽略发送失败，false 表示发送失败时抛出异常
     * @see #shouldUseFixedCode(String)
     * @see #sendCode(String)
     */
    private boolean shouldIgnoreSendFailure(String account) {
        return shouldUseFixedCode(account);
    }
}