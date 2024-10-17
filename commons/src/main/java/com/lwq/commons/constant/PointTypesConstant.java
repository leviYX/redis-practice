package com.lwq.commons.constant;

import lombok.Getter;

/**
 * 积分类型
 */
@Getter
public enum PointTypesConstant {
    //不做字典表了，直接做个枚举
    sign(0),//签到的
    follow(1),//关注的
    feed(2),//发feed的
    review(3)//餐厅的
    ;

    private int type;

    PointTypesConstant(int key) {
        this.type = key;
    }

}