package com.zmbdp.file.service.service;

import com.zmbdp.file.api.domain.dto.FileReqDTO;
import com.zmbdp.file.api.domain.dto.SignReqDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author 稚名不带撇
 */
public interface IFileService {

    /**
     * 文件上传
     *
     * @param file 上传的文件
     * @return 文件上传后的 DTO
     */
    FileReqDTO upload(MultipartFile file);

    /**
     * 获取文件上传签名
     *
     * @return 文件上传签名
     */
    SignReqDTO getSign();
}
