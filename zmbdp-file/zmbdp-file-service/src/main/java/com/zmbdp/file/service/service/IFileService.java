package com.zmbdp.file.service.service;

import com.zmbdp.file.api.domain.dto.FileDTO;
import com.zmbdp.file.api.domain.dto.SignDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    FileDTO upload(MultipartFile file);

    SignDTO getSign();
}
