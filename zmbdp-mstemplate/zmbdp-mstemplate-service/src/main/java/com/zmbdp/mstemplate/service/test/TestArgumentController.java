package com.zmbdp.mstemplate.service.test;

import com.zmbdp.admin.api.config.domain.dto.ArgumentDTO;
import com.zmbdp.admin.api.config.frign.ArgumentServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/test/argument")
public class TestArgumentController {

    @Autowired
    private ArgumentServiceApi argumentServiceApi;

    /**
     * 根据参数键查询参数对象
     *
     * @param configKey 参数键
     * @return 参数对象
     */
    @GetMapping("/key")
    ArgumentDTO getByConfigKey(@RequestParam String configKey) {
        return argumentServiceApi.getByConfigKey(configKey);
    }

    /**
     * 根据多个参数键查询多个参数对象
     *
     * @return 多个参数对象
     */
    @GetMapping("/keys")
    List<ArgumentDTO> getByConfigKeys() {
        List<String> configKeys = new ArrayList<>();
        configKeys.add("admin");
        configKeys.add("super_admin");
        configKeys.add("sys_hot_city");
        configKeys.add("configKey99999");
        return argumentServiceApi.getByConfigKeys(configKeys);
    }
}
