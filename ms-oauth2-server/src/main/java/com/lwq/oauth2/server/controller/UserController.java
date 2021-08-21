package com.lwq.oauth2.server.controller;


import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.model.domain.SignInIdentity;
import com.lwq.commons.model.vo.SignInDinerInfo;
import com.lwq.commons.utils.ResultInfoUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户中心
 */
@RestController
public class UserController {

    @Resource
    private HttpServletRequest request;

    @Resource
    private RedisTokenStore redisTokenStore;

    /**
     * 生成令牌的时候用户信息已经在Authentication，取出来即可.取出来的时候你要把令牌传进来，他解析令牌，取出你的用户信息
     * @param authentication
     * @return
     */
    @GetMapping("user/me")
    public ResultInfo getCurrentUser(Authentication authentication) {
        // 获取登录用户的信息，然后设置
        SignInIdentity signInIdentity = (SignInIdentity) authentication.getPrincipal();
        // 转为前端可用的视图对象
        SignInDinerInfo dinerInfo = new SignInDinerInfo();
        BeanUtils.copyProperties(signInIdentity, dinerInfo);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), dinerInfo);
    }
    /**
     * 获取登录用户信息有两种方式：
     * 1、localhost:8082/user/me?access_token=0f861120-39b9-4110-82ce-6e4855da0620
     * 通过这个请求传进去令牌，security会解析令牌，你要是登录成功了就会有用户返回，因为生成令牌用的就是登录用户的信息
     * 他会逆向解析出来的。也就是你解析这个就会
     * 2、请求就是localhost:8082/user/me，但是Authorization的type设置为Bearer Token设置为拿到的返回的ToKen令牌，也可以获取当前登录用户
     * 这种的请求，浏览器发过来其实是localhost:8082/user/me?access_token=bearer:0f861120-39b9-4110-82ce-6e4855da0620,这种，所以需要判断你的access_token里面有没有
     * bearer。有就干掉。我们这里用的postman显示不出，具体这个登录这块以后研究security的时候研究吧
     */

    /**
     * 安全退出
     * 因为这个令牌存在两个地方都能生效，所以这两个地方都要检查，只要有就删除，才会真的退出，不然你不知道谁里面有
     * @param access_token
     * @param authorization
     * @return
     */
    @GetMapping("user/logout")
    public ResultInfo logout(String access_token, String authorization) {
        // 判断 access_token 是否为空，为空将 authorization 赋值给 access_token
        if (StringUtils.isBlank(access_token)) {
            access_token = authorization;
        }
        // 判断 authorization 是否为空
        if (StringUtils.isBlank(access_token)) {
            //都没有，说明已经退出了
            return ResultInfoUtil.buildSuccess(request.getServletPath(), "退出成功");
        }
        // 判断 bearer token 是否为空
        if (access_token.toLowerCase().contains("bearer ".toLowerCase())) {
            access_token = access_token.toLowerCase().replace("bearer ", "");
        }
        // 到这里说明有，取出来然后清除 redis token 信息
        OAuth2AccessToken oAuth2AccessToken = redisTokenStore.readAccessToken(access_token);
        if (oAuth2AccessToken != null) {
            //清除AccessToken
            redisTokenStore.removeAccessToken(oAuth2AccessToken);
            OAuth2RefreshToken refreshToken = oAuth2AccessToken.getRefreshToken();
            //清除RefreshToken
            redisTokenStore.removeRefreshToken(refreshToken);
        }
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "退出成功");
    }
}
