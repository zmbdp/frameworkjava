package com.zmbdp.file.service.service.impl;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.file.api.domain.dto.FileDTO;
import com.zmbdp.file.api.domain.dto.SignDTO;
import com.zmbdp.file.service.config.OSSProperties;
import com.zmbdp.file.service.constants.OSSCustomConstants;
import com.zmbdp.file.service.service.IFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
public class OSSFileServiceImpl implements IFileService {

    @Autowired
    private OSSClient ossClient;

    @Autowired
    private OSSProperties ossProperties;

    /**
     * 上传文件
     * @param file 用户上传的文件
     * @return 文件上传后返回的DTO
     */
    @Override
    public FileDTO upload(MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            //获取原始的文件名
            String originalFilename = file.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf(".")+1);
            //在 oss 中存储名字就是 UUID + 文件的后缀名
            String objectName = ossProperties.getPathPrefix() + UUID.randomUUID()+"."+extName;

            ObjectMetadata objectMetadata = new ObjectMetadata();
            // set public read
            objectMetadata.setObjectAcl(CannedAccessControlList.PublicRead);

            // 创建 PutObjectRequest 对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), objectName, inputStream, objectMetadata);

            // 创建 PutObject 请求。
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);

            if (putObjectResult == null || StringUtils.isBlank(putObjectResult.getRequestId())) {
                log.error("上传oss异常putObjectResult未正常返回: {}", putObjectRequest);
                throw new ServiceException(ResultCode.OSS_UPLOAD_FAILED);
            }
            FileDTO sysFileDTO = new FileDTO();
            sysFileDTO.setUrl(ossProperties.getBaseUrl() + objectName);
            sysFileDTO.setKey(objectName);
            sysFileDTO.setName(new File(objectName).getName());
            return sysFileDTO;
        } catch (Exception e) {
            log.error("上传oss异常: {}", e.getMessage(), e);
            throw new ServiceException(ResultCode.OSS_UPLOAD_FAILED);
        }
    }

    /**
     * 获取签名
     * @return 获取到的签名信息
     */
    @Override
    public SignDTO getSign() {
        try {
            //获取 ak sk
            String accesskeyid = ossProperties.getAccessKeyId();
            String accesskeysecret = ossProperties.getAccessKeySecret();
            // 获取当前时间
            Instant now = Instant.now();
            //构建返回数据
            SignDTO signDTO = new SignDTO();
            signDTO.setHost(ossProperties.getBaseUrl());
            signDTO.setPathPrefix(ossProperties.getPathPrefix());

            // 步骤 1：创建 policy。
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> policy = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_EXPIRE_TIME_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);
            String expiration = formatter.format(now.plusSeconds(ossProperties.getExpre()));
            policy.put("expiration", expiration);

            List<Object> conditions = new ArrayList<>();

            Map<String, String> bucketCondition = new HashMap<>();
            bucketCondition.put("bucket", ossProperties.getBucketName());
            conditions.add(bucketCondition);

            Map<String, String> signatureVersionCondition = new HashMap<>();
            signatureVersionCondition.put("x-oss-signature-version", "OSS4-HMAC-SHA256");
            conditions.add(signatureVersionCondition);

            Map<String, String> credentialCondition = new HashMap<>();
            formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_DATE_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);
            String dateStr = formatter.format(now);
            String xOSSCredential = accesskeyid + "/" + dateStr + "/" + ossProperties.getRegion() + "/oss/aliyun_v4_request";
            signDTO.setXOSSCredential(xOSSCredential);
            credentialCondition.put("x-oss-credential", xOSSCredential); // 替换为实际的 access key id
            conditions.add(credentialCondition);

            Map<String, String> dateCondition = new HashMap<>();

            // 定义日期时间格式化器
            formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_REQUEST_TIME_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);

            // 格式化时间
            String xOSSDate = formatter.format(now);
            signDTO.setXOSSDate(xOSSDate);
            dateCondition.put("x-oss-date", xOSSDate);

            conditions.add(dateCondition);

            conditions.add(Arrays.asList("content-length-range", ossProperties.getMinLen(), ossProperties.getMaxLen()));
            conditions.add(Arrays.asList("eq", "$success_action_status", "200"));

            policy.put("conditions", conditions);

            String jsonPolicy = mapper.writeValueAsString(policy);

            // 步骤 2：构造待签名字符串（StringToSign）。
            String policyBase64 = new String(Base64.encodeBase64(jsonPolicy.getBytes()));
            signDTO.setPolicy(policyBase64);

            // 步骤 3：计算 SigningKey。
            byte[] dateKey = hmacSha256(("aliyun_v4" + accesskeysecret).getBytes(), dateStr);
            byte[] dateRegionKey = hmacSha256(dateKey, ossProperties.getRegion());
            byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, "oss");
            byte[] signingKey = hmacSha256(dateRegionServiceKey, "aliyun_v4_request");

            // 步骤 4：计算 Signature。
            byte[] result = hmacSha256(signingKey, policyBase64);
            String signature = BinaryUtil.toHex(result);
            signDTO.setSignature(signature);
            return signDTO;
        } catch (Exception e) {
            log.error("生成直传签名失败", e);
            throw new ServiceException(ResultCode.PRE_SIGN_URL_FAILED);
        }
    }

    /**
     * 生成签名
     *
     * @param key 文件存储路径信息
     * @param data 文件内容
     * @return 签名
     */
    public static byte[] hmacSha256(byte[] key, String data) {
        try {
            // 初始化 HMAC 密钥规格，指定算法为 HMAC-SHA256 并使用提供的密钥。
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");

            // 获取 Mac 实例，并通过 getInstance 方法指定使用HMAC-SHA256算法。
            Mac mac = Mac.getInstance("HmacSHA256");
            // 使用密钥初始化 Mac 对象。
            mac.init(secretKeySpec);

            // 执行 HMAC 计算，通过 doFinal 方法接收需要计算的数据并返回计算结果的数组。
            byte[] hmacBytes = mac.doFinal(data.getBytes());

            return hmacBytes;
        } catch (Exception e) {
            log.error("生成直传签名失败", e);
            throw new ServiceException(ResultCode.PRE_SIGN_URL_FAILED);
        }
    }
}
