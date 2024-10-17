package com.lwq.commons.constant;

import lombok.Getter;

/**
 * 各种枚举类，验证码的，分布式锁的等等
 */
@Getter
public enum RedisKeyConstant {

    verify_code("verify_code:", "验证码"),
    seckill_vouchers("seckill_vouchers:", "秒杀券的key"),
    lock_key("lockby:", "分布式锁的key"),
    following("following:", "关注集合Key"),
    followers("followers:", "粉丝集合key"),
    following_feeds("following_feeds:", "我关注的好友的FeedsKey"),//推送时候的key，就是给你关注的好友推
    diner_points("diner:points", "diner用户的积分Key"),
    diner_location("diner:location", "diner地理位置Key"),
    restaurants("restaurants:", "餐厅的Key"),
    restaurant_new_reviews("restaurant:new:reviews:", "餐厅评论Key"),
    ;
    private String key;
    private String desc;

    RedisKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

}