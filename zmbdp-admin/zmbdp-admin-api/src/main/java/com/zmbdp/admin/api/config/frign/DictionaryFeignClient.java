package com.zmbdp.admin.api.config.frign;


import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "zmbdp-admin-service", path = "/dictionary_type")
public interface DictionaryFeignClient {
}