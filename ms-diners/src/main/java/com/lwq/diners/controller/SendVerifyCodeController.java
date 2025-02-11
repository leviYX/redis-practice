package com.lwq.diners.controller;

import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.utils.ResultInfoUtil;
import com.lwq.diners.service.SendVerifyCodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 发送验证码控制层
 */
@RestController
public class SendVerifyCodeController {

    @Resource
    private SendVerifyCodeService sendVerifyCodeService;

    @Resource
    private HttpServletRequest request;

    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @GetMapping("send")
    public ResultInfo send(String phone) {
        //存入redis即可，不接入短信第三方
        sendVerifyCodeService.send(phone);
        return ResultInfoUtil.buildSuccess("发送成功", request.getServletPath());
    }

}
