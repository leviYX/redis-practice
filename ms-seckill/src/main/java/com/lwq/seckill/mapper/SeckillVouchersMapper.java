package com.lwq.seckill.mapper;

import com.lwq.commons.model.pojo.SeckillVouchers;
import org.apache.ibatis.annotations.*;

/**
 * 秒杀代金券 Mapper，一个人只能抢一次
 */
public interface SeckillVouchersMapper {

    // 新增秒杀活动
    @Insert("insert into t_seckill_vouchers (fk_voucher_id, amount, start_time, end_time, is_valid, create_date, update_date) " +
            " values (#{fkVoucherId}, #{amount}, #{startTime}, #{endTime}, 1, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(SeckillVouchers seckillVouchers);
    
    // 根据代金券 ID 查询该代金券是否参与抢购活动
    @Select("select id, fk_voucher_id, amount, start_time, end_time, is_valid " +
            " from t_seckill_vouchers where fk_voucher_id = #{voucherId}")
    SeckillVouchers selectVoucher(Integer voucherId);

    // 用户下单之后，生成订单并且减库存
    @Update("update t_seckill_vouchers set amount = amount - 1 " +
            " where id = #{seckillId}")
    int stockDecrease(@Param("seckillId") int seckillId);
}