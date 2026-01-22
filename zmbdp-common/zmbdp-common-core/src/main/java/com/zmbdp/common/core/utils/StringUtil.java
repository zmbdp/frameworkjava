package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 字符串工具类
 * <p>
 * 继承自 Apache Commons Lang 的 StringUtils，提供字符串匹配相关的扩展功能。
 * 支持 URL 路径匹配、模式匹配等场景。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>继承 Apache Commons Lang StringUtils 的所有方法</li>
 *     <li>URL 路径匹配（支持 Ant 风格路径匹配）</li>
 *     <li>多模式匹配（支持匹配规则列表）</li>
 *     <li>支持通配符匹配（?、*、**）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // URL 路径匹配
 * boolean match = StringUtil.isMatch("/api/**", "/api/user/list");
 * // 结果：true
 *
 * // 多模式匹配
 * List<String> patterns = Arrays.asList("/api/**", "/admin/**");
 * boolean matched = StringUtil.matches("/api/user", patterns);
 * // 结果：true
 *
 * // 使用父类方法
 * boolean empty = StringUtil.isEmpty(""); // true
 * String trimmed = StringUtil.trim("  test  "); // "test"
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>继承自 Apache Commons Lang StringUtils，可直接使用父类的所有方法</li>
 *     <li>URL 匹配使用 Spring 的 AntPathMatcher 实现</li>
 *     <li>匹配规则支持 ?（单个字符）、*（单层路径）、**（多层路径）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see org.apache.commons.lang3.StringUtils
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class StringUtil extends StringUtils {

    /**
     * 判断字符串是否与匹配规则列表中的任意一个匹配
     * <p>
     * 检查指定字符串是否与匹配规则列表中的任意一个规则匹配。
     * 只要有一个规则匹配成功，就返回 true。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查 URL 是否匹配任意一个规则
     * List<String> patterns = Arrays.asList("/api/**", "/admin/**", "/public/**");
     * boolean matched = StringUtil.matches("/api/user/list", patterns);
     * // 结果：true（匹配 /api/**）
     *
     * // 用于权限控制
     * List<String> publicPaths = Arrays.asList("/login", "/register", "/public/**");
     * if (StringUtil.matches(requestPath, publicPaths)) {
     *     // 允许访问
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 str 为 null 或空字符串，返回 false</li>
     *     <li>如果 patternList 为 null 或空列表，返回 false</li>
     *     <li>只要有一个规则匹配成功，立即返回 true（短路求值）</li>
     *     <li>匹配规则支持 Ant 风格路径匹配（?、*、**）</li>
     *     <li>内部调用 {@link #isMatch(String, String)} 进行匹配</li>
     * </ul>
     *
     * @param str         需要匹配的字符串（通常是 URL 路径），不能为 null 或空字符串
     * @param patternList 匹配规则列表（Ant 风格路径），不能为 null 或空列表
     * @return 如果与任意一个规则匹配则返回 true，否则返回 false
     * @see #isMatch(String, String)
     */
    public static boolean matches(String str, List<String> patternList) {
        if (isEmpty(str) || CollectionUtils.isEmpty(patternList)) {
            return false;
        }
        for (String pattern : patternList) {
            if (isMatch(pattern, str)) {
                // 如果说 当前字符 和我们设定的匹配列表中有一个匹配上了，直接返回 true
                return true;
            }
        }
        // 说明一个匹配上的都没有
        return false;
    }


    /**
     * 判断 URL 是否与规则匹配（Ant 风格路径匹配）
     * <p>
     * 使用 Spring 的 AntPathMatcher 进行路径匹配，支持通配符匹配。
     * 适用于 URL 路径匹配、权限控制等场景。
     * <p>
     * <b>匹配规则说明：</b>
     * <ul>
     *     <li>精确匹配：完全相同的路径</li>
     *     <li>?：匹配任意单个字符（不包括路径分隔符 /）</li>
     *     <li>*：匹配一层路径内的任意字符串，不可跨层级（不包括路径分隔符 /）</li>
     *     <li>**：匹配任意层路径的任意字符，可跨层级（包括路径分隔符 /）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 精确匹配
     * boolean match1 = StringUtil.isMatch("/api/user", "/api/user"); // true
     *
     * // 单层路径匹配
     * boolean match2 = StringUtil.isMatch("/api/*", "/api/user"); // true
     * boolean match3 = StringUtil.isMatch("/api/*", "/api/user/list"); // false（跨层级）
     *
     * // 多层路径匹配
     * boolean match4 = StringUtil.isMatch("/api/**", "/api/user/list"); // true
     * boolean match5 = StringUtil.isMatch("/api/**", "/api/user/list/detail"); // true
     *
     * // 单个字符匹配
     * boolean match6 = StringUtil.isMatch("/api/user?", "/api/user1"); // true
     *
     * // 用于权限控制
     * if (StringUtil.isMatch("/api/**", requestPath)) {
     *     // 需要认证
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 url 为 null 或空字符串，返回 false</li>
     *     <li>如果 pattern 为 null 或空字符串，返回 false</li>
     *     <li>使用 Spring 的 AntPathMatcher 实现，匹配规则遵循 Ant 风格</li>
     *     <li>* 和 ? 不能匹配路径分隔符 /，** 可以匹配路径分隔符</li>
     *     <li>匹配是大小写敏感的</li>
     * </ul>
     *
     * @param pattern 匹配规则（Ant 风格路径，支持 ?、*、** 通配符），不能为 null 或空字符串
     * @param url     需要匹配的 URL 路径，不能为 null 或空字符串
     * @return 如果匹配则返回 true，否则返回 false
     * @see org.springframework.util.AntPathMatcher
     */
    public static boolean isMatch(String pattern, String url) {
        if (isEmpty(url) || isEmpty(pattern)) {
            return false;
        }
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }
}