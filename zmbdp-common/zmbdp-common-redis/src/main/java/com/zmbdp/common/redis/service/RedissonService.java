package com.zmbdp.common.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 集合操作服务类
 * <p>
 * 基于 Redisson 对常用集合结构进行统一封装，当前支持：
 * <ul>
 *     <li><b>Map</b>：字段写入、字段读取、整表写入与整表读取</li>
 *     <li><b>List</b>：尾部追加、头部弹出、区间读取</li>
 *     <li><b>Set</b>：成员添加、成员判断、全量读取</li>
 *     <li><b>Queue</b>：入队、出队、长度查询</li>
 * </ul>
 * <p>
 * <b>设计说明：</b>
 * <ul>
 *     <li>接口风格尽量与 {@code RedisService} 保持一致，便于在项目内统一使用</li>
 *     <li>支持普通类型直接读取，也支持通过 {@link TypeReference} 读取复杂泛型</li>
 *     <li>对 Redisson 原生集合对象进行轻量包装，降低业务层直接操作底层 API 的心智负担</li>
 *     <li>所有方法均做异常兜底，不向上抛出运行时异常，适合作为脚手架通用组件复用</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. Map 场景
 * redissonService.setCacheMapValue("user:profile", "u1", userDTO);
 * UserDTO user = redissonService.getCacheMapValue("user:profile", "u1", UserDTO.class);
 *
 * // 2. List 场景
 * redissonService.rightPushForList("order:list", "order-1");
 * String orderNo = redissonService.leftPopForList("order:list");
 *
 * // 3. Set 场景
 * redissonService.addMemberForSet("tag:java", "spring", "redis");
 * Boolean exists = redissonService.isMemberForSet("tag:java", "spring");
 *
 * // 4. Queue 场景
 * redissonService.enqueue("queue:email", mailTask);
 * MailTask task = redissonService.dequeue("queue:email");
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>Map 的 field 固定为 {@link String}</li>
 *     <li>复杂嵌套对象读取时，建议优先使用 {@link TypeReference} 重载方法</li>
 *     <li>当前封装偏向便捷调用，不直接暴露批量管道、阻塞队列等更复杂的 Redisson 能力</li>
 *     <li>发生异常时返回空值、空集合、0 或 false，调用方需自行区分业务语义</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonService {

    /**
     * Redisson 客户端实例
     */
    private final RedissonClient redissonClient;

    /**
     * 判断指定 key 是否存在。
     * <p>
     * 底层通过 {@code countExists} 判断键是否已创建。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Boolean exists = redissonService.hasKey("user:1");
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>写入前存在性判断</li>
     *     <li>缓存预热前检查</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>本方法只判断 key 是否存在，不关心底层数据结构类型</li>
     *     <li>异常时返回 false，若业务上需要区分“确实不存在”和“访问异常”，请自行补充上层日志或监控</li>
     * </ul>
     *
     * @param key Redis 键
     * @return true-存在；false-不存在或异常
     */
    public Boolean hasKey(final String key) {
        try {
            return redissonClient.getKeys().countExists(key) > 0;
        } catch (Exception e) {
            log.warn("RedissonService.hasKey error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除指定 key。
     * <p>
     * 删除 key 及其关联值，适用于当前类封装的所有 Redisson 数据结构。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Boolean deleted = redissonService.deleteObject("user:1");
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>缓存主动清理</li>
     *     <li>业务数据失效后删除</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>删除的是整个 key，不是局部字段或局部元素</li>
     *     <li>当 key 不存在、删除失败或出现异常时统一返回 false</li>
     * </ul>
     *
     * @param key Redis 键
     * @return true-删除成功；false-失败、不存在或异常
     */
    public Boolean deleteObject(final String key) {
        try {
            return redissonClient.getKeys().delete(key) > 0;
        } catch (Exception e) {
            log.warn("RedissonService.deleteObject error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 为指定 key 设置过期时间。
     * <p>
     * 常用于集合缓存场景的 TTL 控制。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * redissonService.expire("user:1", 30, TimeUnit.MINUTES);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>缓存对象统一过期控制</li>
     *     <li>临时集合数据自动淘汰</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只有 key 已存在时该操作才有意义</li>
     *     <li>如果业务上要求“写入即带过期时间”，建议在写入后紧接着调用本方法</li>
     * </ul>
     *
     * @param key     Redis 键
     * @param timeout 过期时长
     * @param unit    时间单位
     * @return true-设置成功；false-设置失败或异常
     */
    public Boolean expire(final String key, final long timeout, final TimeUnit unit) {
        try {
            return redissonClient.getKeys().expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("RedissonService.expire error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定 key 的剩余过期时间（秒）。
     * <p>
     * 返回值语义与 Redis TTL 保持一致：
     * <ul>
     *     <li>{@code > 0}：剩余秒数</li>
     *     <li>{@code -1}：永久有效</li>
     *     <li>{@code -2}：key 不存在或发生异常</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Long ttl = redissonService.getExpire("user:1");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>底层 Redisson 返回毫秒值，这里已统一转换为秒返回</li>
     *     <li>如果需要更精细的毫秒级控制，需直接使用底层 API</li>
     * </ul>
     *
     * @param key Redis 键
     * @return 剩余秒数；-1 表示永久；-2 表示不存在或异常
     */
    public Long getExpire(final String key) {
        try {
            long ttlMs = redissonClient.getKeys().remainTimeToLive(key);
            return ttlMs > 0 ? TimeUnit.MILLISECONDS.toSeconds(ttlMs) : ttlMs;
        } catch (Exception e) {
            log.warn("RedissonService.getExpire error: {}", e.getMessage());
            return -2L;
        }
    }

    /**
     * 设置 Map 中指定字段的值。
     * <p>
     * 适用于按字段拆分缓存对象的场景，例如用户信息、配置项、统计数据等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * redissonService.setCacheMapValue("user:profile", "u1", userDTO);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>对象属性分字段缓存</li>
     *     <li>配置项按名称分组存储</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>该方法执行的是单字段写入，不会覆盖整个 Map</li>
     *     <li>value 支持任意可被 Redisson 正常序列化的对象类型</li>
     * </ul>
     *
     * @param key     Map 键
     * @param hashKey 字段键
     * @param value   字段值
     * @param <T>     字段值类型
     * @return true-写入成功；false-写入失败或异常
     */
    public <T> Boolean setCacheMapValue(final String key, final String hashKey, final T value) {
        try {
            redissonClient.<String, T>getMap(key).put(hashKey, value);
            return true;
        } catch (Exception e) {
            log.warn("RedissonService.setCacheMapValue error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Map 中指定字段的值（直接泛型）。
     * <p>
     * 当调用方能够明确接收类型，且 Redisson 反序列化结果可直接使用时，可优先使用该方法。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * UserDTO user = redissonService.getCacheMapValue("user:profile", "u1");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>该方法依赖调用方自行保证泛型接收类型与实际存储类型一致</li>
     *     <li>若存在类型转换不确定性，建议改用 {@link #getCacheMapValue(String, String, Class)} 或 {@link #getCacheMapValue(String, String, TypeReference)}</li>
     * </ul>
     *
     * @param key     Map 键
     * @param hashKey 字段键
     * @param <T>     返回值类型
     * @return 字段值；不存在或异常返回 null
     */
    public <T> T getCacheMapValue(final String key, final String hashKey) {
        try {
            return redissonClient.<String, T>getMap(key).get(hashKey);
        } catch (Exception e) {
            log.warn("RedissonService.getCacheMapValue error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Map 中指定字段的值（普通类型）。
     * <p>
     * 若底层返回对象与目标类型不一致，会先转为 JSON 再反序列化为目标类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * UserDTO user = redissonService.getCacheMapValue("user:profile", "u1", UserDTO.class);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>读取 DTO、VO 等普通对象</li>
     *     <li>调用方希望显式声明目标类型时</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>若存储值本身已是目标类型，则直接返回，不重复转换</li>
     *     <li>若存储值结构与目标类型不兼容，转换阶段会失败并返回 null</li>
     * </ul>
     *
     * @param key     Map 键
     * @param hashKey 字段键
     * @param clazz   目标类型
     * @param <T>     返回类型
     * @return 字段值；不存在、转换失败或异常返回 null
     * @see #getCacheMapValue(String, String, TypeReference)
     */
    public <T> T getCacheMapValue(final String key, final String hashKey, final Class<T> clazz) {
        try {
            Object value = redissonClient.getMap(key).get(hashKey);
            if (value == null) return null;
            if (clazz.isInstance(value)) return clazz.cast(value);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(value), clazz);
        } catch (Exception e) {
            log.warn("RedissonService.getCacheMapValue class error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Map 中指定字段的值（复杂泛型）。
     * <p>
     * 适用于集合嵌套对象、Map/List 组合等复杂结构读取。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * TypeReference<List<Map<String, UserDTO>>> ref = new TypeReference<>() {};
     * List<Map<String, UserDTO>> value = redissonService.getCacheMapValue("user:profile", "u1", ref);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>读取嵌套集合对象</li>
     *     <li>读取复杂业务结构缓存</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>该方法会先将对象序列化为 JSON，再按目标泛型结构反序列化</li>
     *     <li>泛型定义越准确，读取结果越稳定</li>
     * </ul>
     *
     * @param key     Map 键
     * @param hashKey 字段键
     * @param ref     泛型类型引用
     * @param <T>     返回值类型
     * @return 字段值；不存在或异常返回 null
     */
    public <T> T getCacheMapValue(final String key, final String hashKey, final TypeReference<T> ref) {
        try {
            Object value = redissonClient.getMap(key).get(hashKey);
            return value == null ? null : JsonUtil.jsonToClass(JsonUtil.classToJson(value), ref);
        } catch (Exception e) {
            log.warn("RedissonService.getCacheMapValue typeRef error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量写入整个 Map。
     * <p>
     * 将给定字段集合一次性写入指定 Map 键。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * redissonService.setCacheMap("user:profile", Map.of("u1", userDTO, "u2", userDTO2));
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>初始化整张字段表</li>
     *     <li>批量刷新局部缓存快照</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>本方法会逐项写入给定 map 中的所有字段</li>
     *     <li>若某些字段已存在，会被同名字段新值覆盖</li>
     * </ul>
     *
     * @param key Map 键
     * @param map 待写入的字段集合
     * @param <T> 字段值类型
     * @return true-写入成功；false-写入失败或异常
     */
    public <T> Boolean setCacheMap(final String key, final Map<String, T> map) {
        try {
            redissonClient.<String, T>getMap(key).putAll(map);
            return true;
        } catch (Exception e) {
            log.warn("RedissonService.setCacheMap error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取整个 Map（直接泛型）。
     * <p>
     * 直接读取指定 key 下的全部字段数据，并以 {@code Map<String, T>} 形式返回。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Map<String, UserDTO> profileMap = redissonService.getCacheMap("user:profile");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>异常时返回空 Map，而不是 null</li>
     *     <li>如果调用方对 value 类型有严格要求，需自行保证泛型接收类型正确</li>
     * </ul>
     *
     * @param key Map 键
     * @param <T> 字段值类型
     * @return Map 全量数据；异常返回空 Map
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        try {
            return redissonClient.<String, T>getMap(key).readAllMap();
        } catch (Exception e) {
            log.warn("RedissonService.getCacheMap error: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取整个 Map（复杂泛型）。
     * <p>
     * 适用于 value 本身仍包含复杂泛型结构的场景，例如 {@code Map<String, List<UserDTO>>}。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * TypeReference<Map<String, List<UserDTO>>> ref = new TypeReference<>() {};
     * Map<String, List<UserDTO>> result = redissonService.getCacheMap("user:group", ref);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>会先读取整张 Map，再通过 JSON 转换为目标泛型结构</li>
     *     <li>若目标泛型定义与实际缓存结构不一致，返回结果可能为 null</li>
     * </ul>
     *
     * @param key Map 键
     * @param ref 泛型类型引用
     * @param <T> 返回值类型
     * @return 转换后的 Map 结果；异常返回 null
     */
    public <T> T getCacheMap(final String key, final TypeReference<T> ref) {
        try {
            return JsonUtil.jsonToClass(JsonUtil.classToJson(redissonClient.getMap(key).readAllMap()), ref);
        } catch (Exception e) {
            log.warn("RedissonService.getCacheMap typeRef error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 List 右侧追加元素。
     * <p>
     * 追加成功后返回最新长度，行为上类似 Redis 的 RPUSH。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Long size = redissonService.rightPushForList("order:list", "order-1");
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>简单队列尾插</li>
     *     <li>按写入顺序累积业务记录</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回值是追加完成后的最新长度，而不是固定返回 1</li>
     *     <li>若 key 原本不存在，会由 Redisson 在写入时自动创建对应 List 结构</li>
     * </ul>
     *
     * @param key   List 键
     * @param value 待追加元素
     * @param <T>   元素类型
     * @return 追加后的 List 长度；异常返回 0
     */
    public <T> Long rightPushForList(final String key, final T value) {
        try {
            RList<T> list = redissonClient.getList(key);
            list.add(value);
            return (long) list.size();
        } catch (Exception e) {
            log.warn("RedissonService.rightPushForList error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 从 List 左侧弹出元素。
     * <p>
     * 行为上类似 Redis 的 LPOP；若 List 为空则返回 null。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * String value = redissonService.leftPopForList("order:list");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>本实现通过移除下标 0 元素完成左弹出</li>
     *     <li>适合轻量 List/队列场景，若有复杂阻塞消费需求建议使用更专业的队列组件</li>
     * </ul>
     *
     * @param key List 键
     * @param <T> 元素类型
     * @return 被弹出的元素；为空或异常返回 null
     */
    public <T> T leftPopForList(final String key) {
        try {
            RList<T> list = redissonClient.getList(key);
            return list.isEmpty() ? null : list.remove(0);
        } catch (Exception e) {
            log.warn("RedissonService.leftPopForList error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 List 指定区间的数据（直接泛型）。
     * <p>
     * 按给定起止下标读取 List 区间数据，适合分页预览、窗口读取等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * List<String> list = redissonService.getListRange("order:list", 0, 9);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>{@code start} 和 {@code end} 均为包含边界</li>
     *     <li>异常时返回空 List，调用方可直接安全遍历</li>
     * </ul>
     *
     * @param key   List 键
     * @param start 起始下标（包含）
     * @param end   结束下标（包含）
     * @param <T>   元素类型
     * @return 区间数据；异常返回空 List
     */
    public <T> List<T> getListRange(final String key, final int start, final int end) {
        try {
            return redissonClient.<T>getList(key).range(start, end);
        } catch (Exception e) {
            log.warn("RedissonService.getListRange error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 List 指定区间的数据（复杂泛型）。
     * <p>
     * 当 List 中元素本身包含嵌套结构时，可通过该方法一次性完成区间读取与泛型转换。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * TypeReference<List<Map<String, UserDTO>>> ref = new TypeReference<>() {};
     * List<Map<String, UserDTO>> rows = redissonService.getListRange("order:list", 0, 9, ref);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>底层先读取区间列表，再执行 JSON 泛型转换</li>
     *     <li>若区间内容结构与目标泛型定义不匹配，转换结果可能为 null</li>
     * </ul>
     *
     * @param key   List 键
     * @param start 起始下标（包含）
     * @param end   结束下标（包含）
     * @param ref   泛型类型引用
     * @param <T>   返回值类型
     * @return 转换后的区间数据；异常返回 null
     */
    public <T> T getListRange(final String key, final int start, final int end, final TypeReference<T> ref) {
        try {
            return JsonUtil.jsonToClass(JsonUtil.classToJson(redissonClient.getList(key).range(start, end)), ref);
        } catch (Exception e) {
            log.warn("RedissonService.getListRange typeRef error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 向 Set 中添加一个或多个成员。
     * <p>
     * 返回本次真正新增的成员数量；已存在成员不会重复计数。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Long count = redissonService.addMemberForSet("tag:java", "spring", "redis");
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>标签去重存储</li>
     *     <li>业务标识集合维护</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只有真正新增成功的成员才会计入返回值</li>
     *     <li>已存在元素再次添加不会报错，也不会重复计数</li>
     * </ul>
     *
     * @param key    Set 键
     * @param values 待添加成员
     * @param <T>    成员类型
     * @return 新增成功的成员数量；异常返回 0
     */
    @SafeVarargs
    public final <T> Long addMemberForSet(final String key, final T... values) {
        try {
            RSet<T> set = redissonClient.getSet(key);
            long count = 0;
            for (T value : values) if (set.add(value)) count++;
            return count;
        } catch (Exception e) {
            log.warn("RedissonService.addMemberForSet error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 判断 Set 是否包含指定成员。
     * <p>
     * 用于快速判断某个元素是否已加入目标集合。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Boolean exists = redissonService.isMemberForSet("tag:java", "spring");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Set 天然具备去重语义，本方法适合标签、权限标识、白名单等判重场景</li>
     *     <li>异常时统一返回 false</li>
     * </ul>
     *
     * @param key   Set 键
     * @param value 待判断成员
     * @param <T>   成员类型
     * @return true-存在；false-不存在或异常
     */
    public <T> Boolean isMemberForSet(final String key, final T value) {
        try {
            return redissonClient.<T>getSet(key).contains(value);
        } catch (Exception e) {
            log.warn("RedissonService.isMemberForSet error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Set 的全部成员。
     * <p>
     * 读取指定 Set 中的全部元素，适合在元素规模可控的场景下使用。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Set<String> tags = redissonService.getMembersForSet("tag:java");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Set 为无序集合，返回结果不保证顺序</li>
     *     <li>若集合数据量较大，一次性全量读取可能带来额外网络和内存开销</li>
     * </ul>
     *
     * @param key Set 键
     * @param <T> 成员类型
     * @return 全量成员集合；异常返回空 Set
     */
    public <T> Set<T> getMembersForSet(final String key) {
        try {
            return redissonClient.<T>getSet(key).readAll();
        } catch (Exception e) {
            log.warn("RedissonService.getMembersForSet error: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 向队列尾部入队。
     * <p>
     * 行为上类似 Redis 的 RPUSH/offer。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Boolean success = redissonService.enqueue("queue:email", mailTask);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>轻量任务入队</li>
     *     <li>业务异步处理前的简单缓存排队</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>该方法只负责简单入队，不提供延迟队列、阻塞队列等增强语义</li>
     *     <li>适合作为基础队列封装，复杂消息系统建议交由 MQ 组件处理</li>
     * </ul>
     *
     * @param key   队列键
     * @param value 待入队元素
     * @param <T>   元素类型
     * @return true-入队成功；false-入队失败或异常
     */
    public <T> Boolean enqueue(final String key, final T value) {
        try {
            return redissonClient.<T>getQueue(key).offer(value);
        } catch (Exception e) {
            log.warn("RedissonService.enqueue error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从队列头部出队。
     * <p>
     * 若队列为空则返回 null。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * MailTask task = redissonService.dequeue("queue:email");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>本方法为非阻塞获取，队列为空时立即返回 null</li>
     *     <li>若业务需要阻塞消费、确认机制或重试机制，建议使用 MQ 或更完整的队列能力</li>
     * </ul>
     *
     * @param key 队列键
     * @param <T> 元素类型
     * @return 队头元素；为空或异常返回 null
     */
    public <T> T dequeue(final String key) {
        try {
            return redissonClient.<T>getQueue(key).poll();
        } catch (Exception e) {
            log.warn("RedissonService.dequeue error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取队列当前长度。
     * <p>
     * 用于观察当前队列积压量，常配合入队/出队逻辑进行简单监控。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Long size = redissonService.queueSize("queue:email");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是当前时刻的近实时长度，不应将其视为强事务保证下的精确并发指标</li>
     *     <li>异常时返回 0，业务层若需要区分真实空队列与异常情况，应结合日志判断</li>
     * </ul>
     *
     * @param key 队列键
     * @return 队列长度；异常返回 0
     */
    public Long queueSize(final String key) {
        try {
            return (long) redissonClient.getQueue(key).size();
        } catch (Exception e) {
            log.warn("RedissonService.queueSize error: {}", e.getMessage());
            return 0L;
        }
    }
}
