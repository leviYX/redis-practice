package com.lwq.feeds.controller;

import com.lwq.commons.model.domain.ResultInfo;
import com.lwq.commons.model.pojo.Feeds;
import com.lwq.commons.model.vo.FeedsVO;
import com.lwq.commons.utils.ResultInfoUtil;
import com.lwq.feeds.service.FeedsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class FeedsController {

    @Resource
    private FeedsService feedsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 分页获取关注的 Feed 数据
     *
     * @param page  1
     * @param access_token  XXX
     * @return
     */
    @GetMapping("{page}")
    public ResultInfo selectForPage(@PathVariable Integer page, String access_token) {
        List<FeedsVO> feedsVOS = feedsService.selectForPage(page, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), feedsVOS);
    }

    /**
     * 变更 Feed，这个是暴露给follow服务的，那边取关或者关注，调用这边的操作
     *
     * @return
     */
    @PostMapping("updateFollowingFeeds/{followingDinerId}")
    public ResultInfo addFollowingFeeds(@PathVariable Integer followingDinerId,
                                        String access_token, @RequestParam int type) {
        feedsService.addFollowingFeed(followingDinerId, access_token, type);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "操作成功");
    }

    /**
     * 删除 Feed
     *
     * @param id
     * @param access_token
     * @return
     */
    @DeleteMapping("{id}")
    public ResultInfo delete(@PathVariable Integer id, String access_token) {
        feedsService.delete(id, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "删除成功");
    }

    /**
     * 添加 Feed
     *
     * @param feeds
     * @param access_token
     * @return
     */
    @PostMapping
    public ResultInfo<String> create(@RequestBody Feeds feeds, String access_token) {
        feedsService.create(feeds, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "添加成功");
    }

}