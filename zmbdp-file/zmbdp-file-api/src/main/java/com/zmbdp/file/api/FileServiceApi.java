package com.zmbdp.file.api;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.file.api.domain.vo.FileVO;
import com.zmbdp.file.api.domain.vo.SignVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author 稚名不带撇
 */
@FeignClient(name = "zmbdp-file-service", path = "/file")
public interface FileServiceApi {
    /**
     * 文件上传
     *
     * @param file 用户上传的文件信息
     * @return 访问文件的地址信息
     */
    @PostMapping("/upload")
    Result<FileVO> upload(@RequestBody MultipartFile file);

    /**
     * 获取上传签名
     *
     * @return 签名信息
     */
    @GetMapping("/sign")
    Result<SignVO> getSign();
}
