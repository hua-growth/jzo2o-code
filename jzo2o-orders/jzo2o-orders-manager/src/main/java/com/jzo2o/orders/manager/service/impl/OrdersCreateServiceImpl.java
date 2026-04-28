package com.jzo2o.orders.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.customer.AddressBookApi;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.api.foundations.ServeApi;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.utils.DateUtils;
import com.jzo2o.mvc.utils.UserContext;
import com.jzo2o.orders.base.constants.RedisConstants;
import com.jzo2o.orders.base.mapper.OrdersMapper;
import com.jzo2o.orders.base.model.domain.Orders;
import com.jzo2o.orders.manager.model.dto.request.PlaceOrderReqDTO;
import com.jzo2o.orders.manager.model.dto.response.PlaceOrderResDTO;
import com.jzo2o.orders.manager.service.IOrdersCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 下单服务类
 * </p>
 *
 * @author itcast
 * @since 2023-07-10
 */
@Slf4j
@Service
public class OrdersCreateServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersCreateService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ServeApi serveApi;

    @Autowired
    private AddressBookApi addressBookApi;

    @Autowired
    private IOrdersCreateService owner;

    @Override
    public PlaceOrderResDTO placeOrder(PlaceOrderReqDTO placeOrderReqDTO) {
        //1. 调用运营微服务, 根据服务id查询
        ServeAggregationResDTO serveDto = serveApi.findById(placeOrderReqDTO.getServeId());
        if (ObjectUtil.isNull(serveDto) || serveDto.getSaleStatus() != 2) {
            throw new ForbiddenOperationException("服务不存在或者状态有误");
        }

        //2. 调用customer微服务, 根据地址id查询信息
        AddressBookResDTO addressDto = addressBookApi.detail(placeOrderReqDTO.getAddressBookId());
        if (ObjectUtil.isNull(addressDto)) {
            throw new ForbiddenOperationException("服务地址有误");
        }

        //3. 准备Orders实体类对象
        Orders orders = new Orders();
        orders.setId(generateOrderId());//订单id
        orders.setUserId(UserContext.currentUserId());//下单人id
        orders.setServeId(placeOrderReqDTO.getServeId());//服务id

        //运营数据微服务
        orders.setServeTypeId(serveDto.getServeTypeId());//服务类型id
        orders.setServeTypeName(serveDto.getServeTypeName());//服务类型名称
        orders.setServeItemId(serveDto.getServeItemId());//服务项id
        orders.setServeItemName(serveDto.getServeItemName());//服务项名称
        orders.setServeItemImg(serveDto.getServeItemImg());//服务项图片
        orders.setUnit(serveDto.getUnit());//服务单位
        orders.setPrice(serveDto.getPrice());//服务单价
        orders.setCityCode(serveDto.getCityCode());//城市编码

        orders.setOrdersStatus(0);//订单状态: 待支付
        orders.setPayStatus(2);//支付状态: 待支付

        orders.setPurNum(placeOrderReqDTO.getPurNum());//购买数量
        orders.setTotalAmount(serveDto.getPrice().multiply(new BigDecimal(placeOrderReqDTO.getPurNum())));//总金额: 价格 * 购买数量
        orders.setDiscountAmount(new BigDecimal(0));//优惠金额
        orders.setRealPayAmount(orders.getTotalAmount().subtract(orders.getDiscountAmount()));//实付金额 订单总金额 - 优惠金额

        //地址
        orders.setServeAddress(addressDto.getAddress());//服务详细地址
        orders.setContactsPhone(addressDto.getPhone());//联系人手机号
        orders.setContactsName(addressDto.getName());//联系人名字
        orders.setLon(addressDto.getLon());//经度
        orders.setLat(addressDto.getLat());//纬度

        orders.setServeStartTime(placeOrderReqDTO.getServeStartTime());//服务开始时间
        orders.setDisplay(1);//用户端是否展示 1 展示
        orders.setSortBy(DateUtils.toEpochMilli(placeOrderReqDTO.getServeStartTime()) + orders.getId() % 100000);//排序字段


        //4. 保存到数据表
        owner.saveOrders(orders);

        //5.返回
        return new PlaceOrderResDTO(orders.getId());
    }

    @Transactional
    public void saveOrders(Orders orders) {
        this.save(orders);
    }

    /**
     * 生成订单id
     *
     * @return 订单id 19位：2位年+2位月+2位日+13位序号(自增)
     */
    private Long generateOrderId() {
        //1. 2位年+2位月+2位日
        Long yyMMdd = DateUtils.getFormatDate(LocalDateTime.now(), "yyMMdd");

        //2. 自增数字  1 2
        Long num = redisTemplate.opsForValue().increment(RedisConstants.Lock.ORDERS_SHARD_KEY_ID_GENERATOR, 1);//1 代表的是每次增长量为1

        //3. 组装返回
        return yyMMdd * 10000000000000L + num;
    }
}
