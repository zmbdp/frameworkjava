package com.zmbdp.file.service.service;

import com.zmbdp.file.api.domain.dto.FileReqDTO;
import com.zmbdp.file.api.domain.dto.SignReqDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    FileReqDTO upload(MultipartFile file);

    SignReqDTO getSign();
}
