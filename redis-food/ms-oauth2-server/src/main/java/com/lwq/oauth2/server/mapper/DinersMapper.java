package com.lwq.oauth2.server.mapper;

import com.lwq.commons.model.pojo.Diners;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 食客 Mapper
 */
public interface DinersMapper {
    // 根据用户名 or 手机号 or 邮箱查询用户信息，这三个加上密码都能做登录校验
    @Select("select id, username, nickname, phone, email, " +
            "password, avatar_url, roles, is_valid from t_diners where " +
            "(username = #{account} or phone = #{account} or email = #{account})")
    Diners selectByAccountInfo(@Param("account") String account);
}
