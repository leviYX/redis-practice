package com.lwq.diners.service;

import cn.hutool.core.bean.BeanUtil;

import com.lwq.commons.constant.ApiConstant;
import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.utils.AssertUtil;
import com.lwq.commons.utils.ResultInfoUtil;
import com.lwq.diners.config.OAuth2ClientConfiguration;
import com.lwq.diners.domain.OAuthDinerInfo;
import com.lwq.diners.vo.LoginDinerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

/**
 * 食客服务业务逻辑层
 */
@Service
public class DinersService {

    @Resource
    private RestTemplate restTemplate;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private OAuth2ClientConfiguration oAuth2ClientConfiguration;

    /**
     * 登录
     *
     * @param account  帐号：用户名或手机或邮箱
     * @param password 密码
     * @param path     请求路径
     * @return
     */
    public ResultInfo signIn(String account, String password, String path) {
        // 参数校验
        AssertUtil.isNotEmpty(account, "请输入登录帐号");
        AssertUtil.isNotEmpty(password, "请输入登录密码");
        /*-----------------------发送请求去security验证中心去校验，拿令牌-----------------------------------*/
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 构建请求体（请求参数）
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("username", account);
        body.add("password", password);
        body.setAll(BeanUtil.beanToMap(oAuth2ClientConfiguration));
        //组合请求头和请求体，构建请求对象
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        // 设置 Authorization
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(oAuth2ClientConfiguration.getClientId(),
                oAuth2ClientConfiguration.getSecret()));
        // 发送请求去验证服务，返回类型为ResultInfo
        ResponseEntity<ResultInfo> result = restTemplate.postForEntity(oauthServerName + "oauth/token", entity, ResultInfo.class);
        // 处理返回结果，既然是请求，就有返回码，可以用http状态返回
        AssertUtil.isTrue(result.getStatusCode() != HttpStatus.OK, "登录失败");
        ResultInfo resultInfo = result.getBody();
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            // 登录失败
            resultInfo.setData(resultInfo.getMessage());
            return resultInfo;
        }
        // 这里的 Data 是一个 LinkedHashMap 转成了域对象 OAuthDinerInfo，BeanUtil.fillBeanWithMap可以把map映射到实体类里
        OAuthDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new OAuthDinerInfo(), false);//忽略错误
        // 根据业务需求返回视图对象
        LoginDinerInfo loginDinerInfo = new LoginDinerInfo();
        loginDinerInfo.setToken(dinerInfo.getAccessToken());
        loginDinerInfo.setAvatarUrl(dinerInfo.getAvatarUrl());
        loginDinerInfo.setNickname(dinerInfo.getNickname());
        return ResultInfoUtil.buildSuccess(path, loginDinerInfo);
    }

}
