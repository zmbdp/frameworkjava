package com.zmbdp.common.security.domain.dto;

import com.zmbdp.common.domain.domain.vo.TokenVO;
import lombok.Data;

/**
 * token 信息
 *
 * @author 稚名不带撇
 */
@Data
public class TokenDTO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 过期时间
     */
    private Long expires;

    /**
     * 转换 tokenVo
     *
     * @return tokenVo
     */
    public TokenVO convertToVo() {
        TokenVO tokenVO = new TokenVO();
        tokenVO.setAccessToken(this.accessToken);
        tokenVO.setExpires(this.expires);
        return tokenVO;
    }
}
