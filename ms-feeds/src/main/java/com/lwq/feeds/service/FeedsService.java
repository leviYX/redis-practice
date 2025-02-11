package com.lwq.feeds.service;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.lwq.commons.constant.ApiConstant;
import com.lwq.commons.constant.RedisKeyConstant;
import com.lwq.commons.exception.ParameterException;
import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.model.pojo.Feeds;
import com.lwq.commons.model.vo.FeedsVO;
import com.lwq.commons.model.vo.ShortDinerInfo;
import com.lwq.commons.model.vo.SignInDinerInfo;
import com.lwq.commons.utils.AssertUtil;
import com.lwq.feeds.mapper.FeedsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedsService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-follow-server}")
    private String followServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinersServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private FeedsMapper feedsMapper;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 根据时间由近至远，每次查询 20 条 Feed
     *
     * @param page
     * @param accessToken
     * @return
     */
    public List<FeedsVO> selectForPage(Integer page, String accessToken) {
        if (page == null) {
            page = 1;
        }
        // 获取登录用户
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 我关注的好友的 Feedkey
        String key = RedisKeyConstant.following_feeds.getKey() + dinerInfo.getId();
        // SortedSet 的 ZREVRANGE 命令是闭区间，所以要减一，0-19就是20条了。其实可以自己试试，看看效果
        long start = (page - 1) * ApiConstant.PAGE_SIZE;
        long end = page * ApiConstant.PAGE_SIZE - 1;
        //按照时间排序由近至远，反转出来
        Set<Integer> feedIds = redisTemplate.opsForZSet().reverseRange(key, start, end);
        //好友圈没有发过feed，直接返回
        if (feedIds == null || feedIds.isEmpty()) {
            return Lists.newArrayList();
        }
        // 根据多主键查询 Feed
        List<Feeds> feeds = feedsMapper.findFeedsByIds(feedIds);
        // 初始化关注好友 ID 集合
        List<Integer> followingDinerIds = new ArrayList<>();
        // 添加用户 ID 至集合，顺带将 Feeds 转为 Vo 对象
        List<FeedsVO> feedsVOS = feeds.stream().map(feed -> {
            FeedsVO feedsVO = new FeedsVO();
            BeanUtil.copyProperties(feed, feedsVO);
            // 添加用户 ID
            followingDinerIds.add(feed.getFkDinerId());
            return feedsVO;
        }).collect(Collectors.toList());
        // 远程调用dinners服务获取 Feed 中用户信息
        ResultInfo resultInfo = restTemplate.getForObject(dinersServerName + "findByIds?access_token=${accessToken}&ids={ids}",
                ResultInfo.class, accessToken, followingDinerIds);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        //取出dinner用户信息
        List<LinkedHashMap> dinerInfoMaps = (ArrayList) resultInfo.getData();
        // 构建一个 key 为用户 ID，value 为 ShortDinerInfo（关注的食客用户） 的 Map，后面直接从map获取使用
        Map<Integer, ShortDinerInfo> dinerInfos = dinerInfoMaps.stream()
                .collect(Collectors.toMap(
                        // key
                        diner -> (Integer) diner.get("id"),
                        // value
                        diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true)
                ));
        // 循环 VO 集合，根据用户 ID 从 Map 中获取用户信息并设置至 VO 对象
        feedsVOS.forEach(feedsVO -> {
            feedsVO.setDinerInfo(dinerInfos.get(feedsVO.getFkDinerId()));
        });
        return feedsVOS;
    }

    /**
     * 变更 Feed
     *
     * @param followingDinerId 关注的好友 ID
     * @param accessToken      登录用户token
     * @param type             1 关注 0 取关
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFollowingFeed(Integer followingDinerId, String accessToken, int type) {
        // 请选择关注的好友
        AssertUtil.isTrue(followingDinerId == null || followingDinerId < 1,
                "请选择关注的好友");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取关注/取关的食客的所有 Feed
        List<Feeds> feedsList = feedsMapper.findByDinerId(followingDinerId);
        String key = RedisKeyConstant.following_feeds.getKey() + dinerInfo.getId();
        if (type == 0) {
            // 取关操作，此时把你取关的这个人以前发的feed，从你维护的集合里删除。
            List<Integer> feedIds = feedsList.stream()
                    .map(feed -> feed.getId())
                    .collect(Collectors.toList());
            redisTemplate.opsForZSet().remove(key, feedIds.toArray(new Integer[]{}));
        } else {
            // 关注操作，你就要把这个人以前发的feed，加到你关注的这个维护的集合里面，就像你加了一个人
            //他以前发的朋友圈你也能看到了，这个zset的score就是他发的时间
            Set<ZSetOperations.TypedTuple> typedTuples =
                    feedsList.stream()
                            .map(feed -> new DefaultTypedTuple<>(feed.getId(), (double) feed.getUpdateDate().getTime()))
                            .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(key, typedTuples);
        }
    }

    /**
     * 删除 Feed
     *
     * @param id
     * @param accessToken，登录了才能删，所以要传过来
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id, String accessToken) {
        // 请选择要删除的 Feed
        AssertUtil.isTrue(id == null || id < 1, "请选择要删除的Feed");
        // 获取登录用户
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取 Feed 内容
        Feeds feeds = feedsMapper.findById(id);
        // 判断 Feed 是否已经被删除且只能删除自己的 Feed
        AssertUtil.isTrue(feeds == null, "该Feed已被删除");//也是种幂等性吧，网络延迟，多删了几次，在这里拦截
        AssertUtil.isTrue(!feeds.getFkDinerId().equals(dinerInfo.getId()), "只能删除自己的Feed");
        // 删除
        int count = feedsMapper.delete(id);
        if (count == 0) {
            return;
        }
        // 将内容从粉丝的集合中删除 -- 异步消息队列优化
        // 先获取我的粉丝
        List<Integer> followers = findFollowers(dinerInfo.getId());
        // 移除 Feed
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            redisTemplate.opsForZSet().remove(key, feeds.getId());
        });
    }

    /**
     * 添加 Feed
     *
     * @param feeds
     * @param accessToken
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Feeds feeds, String accessToken) {
        // 校验 Feed 内容不能为空，不能太长
        AssertUtil.isNotEmpty(feeds.getContent(), "请输入内容");
        AssertUtil.isTrue(feeds.getContent().length() > 255, "输入内容太多，请重新输入");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // Feed 关联用户信息
        feeds.setFkDinerId(dinerInfo.getId());
        // 添加 Feed
        int count = feedsMapper.save(feeds);
        AssertUtil.isTrue(count == 0, "添加失败");
        // 推送到粉丝的列表中 -- 后续这里应该采用异步消息队列解决性能问题
        // 先获取当前登录这哥们的粉丝 id 集合
        List<Integer> followers = findFollowers(dinerInfo.getId());
        // 推送 Feed
        long now = System.currentTimeMillis();
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            //每一个feed都发给他所有的粉丝，他的粉丝都维护一个set,其实就是每个人都有一个set，里面放着你关注的人推得feed，按时间排的
            //谁发了，你要是他的粉丝就会推给你
            //使用zset存放这个dinner的feed，至于分数就用推送的时间，因为我们是按时间大小展示的，zset很方便
            redisTemplate.opsForZSet().add(key, feeds.getId(), now);
        });
    }

    /**
     * 走食客followers服务，获取粉丝 id 集合
     *
     * @param dinerId
     * @return
     */
    private List<Integer> findFollowers(Integer dinerId) {
        String url = followServerName + "followers/" + dinerId;
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        List<Integer> followers = (List<Integer>) resultInfo.getData();
        return followers;
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
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInDinerInfo(), false);
        if (dinerInfo == null) {
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE, ApiConstant.NO_LOGIN_MESSAGE);
        }
        return dinerInfo;
    }

}
