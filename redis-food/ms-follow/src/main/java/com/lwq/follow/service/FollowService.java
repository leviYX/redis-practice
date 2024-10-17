package com.lwq.follow.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.lwq.commons.constant.ApiConstant;
import com.lwq.commons.constant.RedisKeyConstant;
import com.lwq.commons.exception.ParameterException;
import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.model.pojo.Follow;
import com.lwq.commons.model.vo.ShortDinerInfo;
import com.lwq.commons.model.vo.SignInDinerInfo;
import com.lwq.commons.utils.AssertUtil;
import com.lwq.commons.utils.ResultInfoUtil;
import com.lwq.follow.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注/取关业务逻辑层
 */
@Service
public class FollowService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinersServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 共同关注列表
     *
     * @param dinerId
     * @param accessToken
     * @param path
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultInfo findCommonsFriends(Integer dinerId, String accessToken, String path) {
        // 是否选择了查看对象
        AssertUtil.isTrue(dinerId == null || dinerId < 1, "请选择要查看的人");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取登录用户的关注信息
        String loginDinerKey = RedisKeyConstant.following.getKey() + dinerInfo.getId();
        // 获取登录用户查看对象的关注信息
        String dinerKey = RedisKeyConstant.following.getKey() + dinerId;
        // 计算交集
        Set<Integer> dinerIds = redisTemplate.opsForSet().intersect(loginDinerKey, dinerKey);
        // 没有
        if (dinerIds == null || dinerIds.isEmpty()) {
            return ResultInfoUtil.buildSuccess(path, new ArrayList<ShortDinerInfo>());
        }
        // 调用食客服务根据 ids 查询食客信息
        ResultInfo resultInfo = restTemplate.getForObject(dinersServerName + "findByIds?access_token={accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", dinerIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }
        // 处理结果集
        List<LinkedHashMap> dinnerInfoMaps = (ArrayList) resultInfo.getData();
        List<ShortDinerInfo> dinerInfos = dinnerInfoMaps.stream()
                .map(diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true))
                .collect(Collectors.toList());

        return ResultInfoUtil.buildSuccess(path, dinerInfos);
    }

    /**
     * 关注/取关
     *
     * @param followDinerId 关注的食客ID
     * @param isFoolowed    是否关注 1=关注 0=取关
     * @param accessToken   登录用户token，就是当前登录用户
     * @param path          访问地址
     * @return
     */
    public ResultInfo follow(Integer followDinerId, int isFoolowed, String accessToken, String path) {
        // 是否选择了关注对象，followDinerId是自增的，所以要判断<1
        AssertUtil.isTrue(followDinerId == null || followDinerId < 1, "请选择要关注的人");
        // 获取登录用户信息 (封装方法)
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取当前登录用户与需要关注用户的关注信息
        Follow follow = followMapper.selectFollow(dinerInfo.getId(), followDinerId);

        // 如果没有关注信息，且要进行关注操作 -- 添加关注
        if (follow == null && isFoolowed == 1) {
            // 添加关注信息
            int count = followMapper.save(dinerInfo.getId(), followDinerId);
            // 添加关注列表到 Redis
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "关注成功", path, "关注成功");
        }

        // 如果有关注信息，且目前处于关注状态，且要进行取关操作 -- 取关关注
        if (follow != null && follow.getIsValid() == 1 && isFoolowed == 0) {
            // 取关
            int count = followMapper.update(follow.getId(), isFoolowed);
            // 移除 Redis 关注列表
            if (count == 1) {
                removeFromRedisSet(dinerInfo.getId(), followDinerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "成功取关", path, "成功取关");
        }

        // 如果有关注信息，且目前处于取关状态，且要进行关注操作 -- 重新关注
        if (follow != null && follow.getIsValid() == 0 && isFoolowed == 1) {
            // 重新关注
            int count = followMapper.update(follow.getId(), isFoolowed);
            // 添加关注列表到 Redis
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE, "关注成功", path, "关注成功");
        }

        return ResultInfoUtil.buildSuccess(path, "操作成功");
    }

    /**
     * 添加关注列表到 Redis
     *
     * @param dinerId
     * @param followDinerId
     */
    private void addToRedisSet(Integer dinerId, Integer followDinerId) {
        //添加关注集合：就是这个用户关注了哪些人
        redisTemplate.opsForSet().add(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        //添加粉丝集合：就是谁关注了这个用户
        redisTemplate.opsForSet().add(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param dinerId
     * @param followDinerId
     */
    private void removeFromRedisSet(Integer dinerId, Integer followDinerId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
    }

    /**
     * 获取登录用户信息
     *
     * @param accessToken
     * @return
     */
    private SignInDinerInfo loadSignInDinerInfo(String accessToken) {
        // 必须登录
        AssertUtil.mustLogin(accessToken);
        //调用认证中心的security的内部方法，从token解析出用户信息
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getMessage());
        }
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInDinerInfo(), false);
        return dinerInfo;
    }
}
