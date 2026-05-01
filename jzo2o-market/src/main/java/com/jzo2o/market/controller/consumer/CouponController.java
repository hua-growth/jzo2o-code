package com.jzo2o.market.controller.consumer;

import com.jzo2o.market.model.dto.request.SeizeCouponReqDTO;
import com.jzo2o.market.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("consumerCouponController")
@RequestMapping("/consumer/coupon")
@Api(tags = "用户端-优惠券相关接口")
public class CouponController {

    @Autowired
    private ICouponService couponService;

    @ApiOperation("抢券")
    @PostMapping("/seize")
    public void seizeCoupon(@RequestBody SeizeCouponReqDTO seizeCouponReqDTO) {
        couponService.seizeCoupon(seizeCouponReqDTO);
    }
}