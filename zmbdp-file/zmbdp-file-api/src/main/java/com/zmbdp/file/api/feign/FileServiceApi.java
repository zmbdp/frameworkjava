package com.zmbdp.file.api.feign;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.file.api.domain.vo.FileVO;
import com.zmbdp.file.api.domain.vo.SignVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务远程调用 Api
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "fileServiceApi", name = "zmbdp-file-service", path = "/file")
public interface FileServiceApi {
    /**
     * 文件上传
     *
     * @param file 用户上传的文件信息
     * @return 访问文件的地址信息
     * <p> consumes = MediaType.MULTIPART_FORM_DATA_VALUE:
     * <p> 默认情况下，Feign 会尝试将参数序列化为 JSON 格式
     * <p> 但对于文件上传，需要使用 multipart/form-data 格式而不是 JSON
     * <p> 指定 consumes = MediaType.MULTIPART_FORM_DATA_VALUE 告诉 Feign 使用正确的编码方式
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<FileVO> upload(MultipartFile file);

    /**
     * 获取上传签名
     *
     * @return 签名信息
     */
    @GetMapping("/sign")
    Result<SignVO> getSign();
}
