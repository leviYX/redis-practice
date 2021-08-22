package com.lwq.oauth2.server.controller;


import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.utils.ResultInfoUtil;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Oauth2 控制器
 */
@RestController
@RequestMapping("oauth")
public class OAuthController {

    @Resource
    private TokenEndpoint tokenEndpoint;

    @Resource
    private HttpServletRequest request;

    /**
     * 生成token令牌，这个就是走的security内部jar包的封装的接口，直接发接口，他生成
     * @param principal
     * @param parameters：client_id、client_secret、grant_type、username、password
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @PostMapping("token")
    public ResultInfo postAccessToken(Principal principal, @RequestParam Map<String, String> parameters)
            throws HttpRequestMethodNotSupportedException {
        return custom(tokenEndpoint.postAccessToken(principal, parameters).getBody());
    }

    /**
     * 自定义 Token 返回对象,比起以前security自己内部的加多一些操作返回，个性化走一波
     *
     * @param accessToken
     * @return
     */
    private ResultInfo custom(OAuth2AccessToken accessToken) {
        DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
        Map<String, Object> data = new LinkedHashMap(token.getAdditionalInformation());
        data.put("accessToken", token.getValue());
        data.put("expireIn", token.getExpiresIn());
        data.put("scopes", token.getScope());
        if (token.getRefreshToken() != null) {
            data.put("refreshToken", token.getRefreshToken().getValue());
        }
        return ResultInfoUtil.buildSuccess(request.getServletPath(), data);
    }

}