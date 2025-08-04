package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.file.api.feign.FileServiceApi;
import com.zmbdp.file.api.domain.vo.FileVO;
import com.zmbdp.file.api.domain.vo.SignVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/test/file")
public class TestFileController {

    @Autowired
    private FileServiceApi fileServiceApi;

    @PostMapping(value = "/upload")
    public Result<FileVO> upload(MultipartFile file) {
        return fileServiceApi.upload(file);
    }

    @RequestMapping("/sign")
    public Result<SignVO> sign() {
        return fileServiceApi.getSign();
    }
}
