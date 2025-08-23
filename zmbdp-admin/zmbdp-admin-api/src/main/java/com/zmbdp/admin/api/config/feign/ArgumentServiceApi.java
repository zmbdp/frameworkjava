package com.zmbdp.admin.api.config.feign;

import com.zmbdp.admin.api.config.domain.dto.ArgumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 参数服务远程调用 Api
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "argumentServiceApi", name = "zmbdp-admin-service", path = "/argument")
public interface ArgumentServiceApi {

    /**
     * 根据参数键查询参数对象
     *
     * @param configKey 参数键
     * @return 参数对象
     */
    @GetMapping("/key")
    ArgumentDTO getByConfigKey(@RequestParam("configKey") String configKey);

    /**
     * 根据多个参数键查询多个参数对象
     *
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    @GetMapping("/keys")
    List<ArgumentDTO> getByConfigKeys(@RequestParam List<String> configKeys);
}
