package com.jzo2o.market.service.impl;

import cn.hutool.db.DbRuntimeException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.market.dto.request.CouponUseBackReqDTO;
import com.jzo2o.api.market.dto.request.CouponUseReqDTO;
import com.jzo2o.api.market.dto.response.AvailableCouponsResDTO;
import com.jzo2o.api.market.dto.response.CouponUseResDTO;
import com.jzo2o.common.expcetions.BadRequestException;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.expcetions.DBException;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.*;
import com.jzo2o.market.enums.ActivityStatusEnum;
import com.jzo2o.market.enums.CouponStatusEnum;
import com.jzo2o.market.mapper.CouponMapper;
import com.jzo2o.market.model.domain.Activity;
import com.jzo2o.market.model.domain.Coupon;
import com.jzo2o.market.model.domain.CouponWriteOff;
import com.jzo2o.market.model.dto.request.CouponOperationPageQueryReqDTO;
import com.jzo2o.market.model.dto.request.SeizeCouponReqDTO;
import com.jzo2o.market.model.dto.response.ActivityInfoResDTO;
import com.jzo2o.market.model.dto.response.CouponInfoResDTO;
import com.jzo2o.market.service.IActivityService;
import com.jzo2o.market.service.ICouponService;
import com.jzo2o.market.service.ICouponUseBackService;
import com.jzo2o.market.service.ICouponWriteOffService;
import com.jzo2o.market.utils.CouponUtils;
import com.jzo2o.mvc.utils.UserContext;
import com.jzo2o.mysql.utils.PageUtils;
import com.jzo2o.redis.utils.RedisSyncQueueUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.jzo2o.common.constants.ErrorInfo.Code.SEIZE_COUPON_FAILD;
import static com.jzo2o.market.constants.RedisConstants.RedisKey.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2023-09-16
 */
@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    @Resource(name = "seizeCouponScript")
    private DefaultRedisScript<String> seizeCouponScript;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private IActivityService activityService;

    @Resource
    private ICouponUseBackService couponUseBackService;

    @Resource
    private ICouponWriteOffService couponWriteOffService;

    //抢券
    @Override
    public void seizeCoupon(SeizeCouponReqDTO seizeCouponReqDTO) {
        // 1.校验当前时间是否在活动期间
        ActivityInfoResDTO activity = activityService.getActivityInfoByIdFromCache(seizeCouponReqDTO.getId());
        if (activity == null || activity.getDistributeStartTime().isAfter(LocalDateTime.now())) {
            throw new CommonException(SEIZE_COUPON_FAILD, "活动不存在或者未开始");
        }
        if (activity.getDistributeEndTime().isBefore(LocalDateTime.now())) {
            throw new CommonException(SEIZE_COUPON_FAILD, "活动不存在或者已结束");
        }

        //2. 抢券
        //2-1 准备参数
        int index = (int) (seizeCouponReqDTO.getId() % 10);
        String couponSeizeSyncRedisKey = RedisSyncQueueUtils.getQueueRedisKey(COUPON_SEIZE_SYNC_QUEUE_NAME, index);// 同步队列redisKey
        String resourceStockRedisKey = String.format(COUPON_RESOURCE_STOCK, index);// 资源库存redisKey
        String couponSeizeListRedisKey = String.format(COUPON_SEIZE_LIST, activity.getId(), index);// 抢券列表
        log.debug("seize coupon keys -> couponSeizeListRedisKey->{},resourceStockRedisKey->{},couponSeizeListRedisKey->{},seizeCouponReqDTO.getId()->{},UserContext.currentUserId():{}",
                couponSeizeListRedisKey, resourceStockRedisKey, couponSeizeListRedisKey, seizeCouponReqDTO.getId(), UserContext.currentUserId());

        //2-2 执行抢券脚本
        Object executeResult = redisTemplate.execute(
                seizeCouponScript, //脚本
                Arrays.asList(couponSeizeSyncRedisKey, resourceStockRedisKey, couponSeizeListRedisKey),//键
                seizeCouponReqDTO.getId(), UserContext.currentUserId()//参数
        );

        //3. 返回结果
        if (executeResult == null) {
            throw new CommonException(SEIZE_COUPON_FAILD, "抢券失败");
        }
        long result = NumberUtils.parseLong(executeResult.toString());
        if (result > 0) {
            return; //成功
        }else if (result == -1) {
            throw new CommonException(SEIZE_COUPON_FAILD, "限领一张");
        }else if (result == -2 || result == -4) {
            throw new CommonException(SEIZE_COUPON_FAILD, "已抢光!");
        }else{
            throw new CommonException(SEIZE_COUPON_FAILD, "抢券失败");
        }
    }
    @Override
    public void processExpireCoupon() {
        lambdaUpdate()
                .eq(Coupon::getStatus, CouponStatusEnum.NO_USE.getStatus())//未使用
                .le(Coupon::getValidityTime, DateUtils.now())//有效期小于当前时间
                .set(Coupon::getStatus, CouponStatusEnum.INVALID.getStatus())//已失效
                .update();
    }

    @Override
    public PageResult<CouponInfoResDTO> findByPage(CouponOperationPageQueryReqDTO dto) {
        //0. 校验
        if (dto.getActivityId() == null) {
            throw new ForbiddenOperationException("请指定活动id");
        }

        //1. 设置分页条件
        Page<Coupon> page = PageUtils.parsePageQuery(dto, Coupon.class);

        //2. 设置业务条件
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getActivityId, dto.getActivityId());

        //3. 执行分页
        page = this.page(page, wrapper);

        //4. 组装返回结果
        return PageUtils.toPage(page, CouponInfoResDTO.class);
    }

}
