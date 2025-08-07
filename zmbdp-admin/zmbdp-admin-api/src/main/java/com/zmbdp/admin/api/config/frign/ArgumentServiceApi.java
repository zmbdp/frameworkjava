package com.zmbdp.admin.api.config.frign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(contextId = "argumentServiceApi", name = "zmbdp-admin-service", path = "/argument")
public interface ArgumentServiceApi {
}
