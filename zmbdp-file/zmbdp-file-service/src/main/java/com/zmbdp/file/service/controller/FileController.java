package com.zmbdp.file.service.controller;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.file.api.domain.dto.FileReqDTO;
import com.zmbdp.file.api.domain.dto.SignReqDTO;
import com.zmbdp.file.api.domain.vo.FileVO;
import com.zmbdp.file.api.domain.vo.SignVO;
import com.zmbdp.file.api.feign.FileServiceApi;
import com.zmbdp.file.service.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/file")
public class FileController implements FileServiceApi {

    /**
     * 文件服务
     */
    @Autowired
    private IFileService fileService;

    /**
     * 文件上传
     *
     * @param file 用户上传的文件
     * @return 访问文件的地址信息
     */
    @Override
    public Result<FileVO> upload(MultipartFile file) {
        FileReqDTO fileReqDTO = fileService.upload(file);
        FileVO fileVO = new FileVO();
        BeanCopyUtil.copyProperties(fileReqDTO, fileVO);
        return Result.success(fileVO);
    }

    /**
     * 获取签名信息
     *
     * @return 签名信息
     */
    @Override
    public Result<SignVO> getSign() {
        SignReqDTO signReqDTO = fileService.getSign();
        SignVO signVO = new SignVO();
        BeanCopyUtil.copyProperties(signReqDTO, signVO);
        return Result.success(signVO);
    }
}
