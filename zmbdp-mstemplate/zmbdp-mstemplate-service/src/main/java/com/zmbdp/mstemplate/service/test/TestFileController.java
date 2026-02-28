package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.file.api.domain.vo.FileVO;
import com.zmbdp.file.api.domain.vo.SignVO;
import com.zmbdp.file.api.feign.FileServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务测试控制器
 * 测试文件上传、签名获取等功能
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/test/file")
public class TestFileController {

    @Autowired
    private FileServiceApi fileServiceApi;

    /**
     * 文件上传测试
     *
     * @param file 上传的文件
     * @return 文件信息
     */
    @PostMapping(value = "/upload")
    public Result<FileVO> upload(MultipartFile file) {
        return fileServiceApi.upload(file);
    }

    /**
     * 获取文件上传签名
     *
     * @return 签名信息
     */
    @RequestMapping("/sign")
    public Result<SignVO> sign() {
        return fileServiceApi.getSign();
    }
}