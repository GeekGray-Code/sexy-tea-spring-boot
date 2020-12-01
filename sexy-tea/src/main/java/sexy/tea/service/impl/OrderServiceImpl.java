package sexy.tea.service.impl;

import cn.hutool.core.lang.UUID;
import com.alipay.easysdk.factory.Factory.Payment;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sexy.tea.exception.BusinessException;
import sexy.tea.mapper.OrderGoodsMapper;
import sexy.tea.mapper.OrderMapper;
import sexy.tea.mapper.OrderShippingMapper;
import sexy.tea.model.Order;
import sexy.tea.model.OrderGoods;
import sexy.tea.model.ShoppingRecord;
import sexy.tea.model.User;
import sexy.tea.model.common.Pager;
import sexy.tea.model.common.Result;
import sexy.tea.model.dto.OrderDto;
import sexy.tea.service.OrderService;
import sexy.tea.service.ShoppingRecordService;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * author 大大大西西瓜皮🍉
 * date 15:10 2020-09-26
 * description:
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrderMapper orderMapper;

    private final OrderShippingMapper orderShippingMapper;

    private final OrderGoodsMapper orderGoodsMapper;

    private final ShoppingRecordService shoppingRecordService;

    @Value("${order.gen}")
    private String genKey;

    @Value("${order.default")
    private String defaultKey;

    @Value("${order.item}")
    private String itemKey;

    private final StringRedisTemplate template;

    @Autowired
    public OrderServiceImpl(OrderMapper orderMapper, OrderShippingMapper orderShippingMapper, OrderGoodsMapper orderGoodsMapper, ShoppingRecordService shoppingRecordService, StringRedisTemplate template) {
        this.orderMapper = orderMapper;
        this.orderShippingMapper = orderShippingMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.shoppingRecordService = shoppingRecordService;
        this.template = template;
    }

    @Override
    public int updateBatch(List<Order> list) {
        return orderMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<Order> list) {
        return orderMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<Order> list) {
        return orderMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(Order record) {
        return orderMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(Order record) {
        return orderMapper.insertOrUpdateSelective(record);
    }

    @Override
    public Result find(int pageNum, int pageSize) {

        final Page<Order> page = PageHelper.startPage(pageNum, pageSize);
        final Example example = Example.builder(Order.class).build();
        example.createCriteria().andNotEqualTo("status", -1);
        example.setOrderByClause("update_time DESC");
        orderMapper.selectByExample(example);

        return Result.success("订单查询成功", Pager.<Order>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(page.getTotal())
                .result(page.getResult())
                .build());
    }

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public Result createOrder(OrderDto orderDto) {
        if (orderDto == null || orderDto.getUser() == null || orderDto.getRecord() == null) {
            return Result.business("参数错误", Optional.empty());
        }

        // 获取参数
        final User user = orderDto.getUser();
        ShoppingRecord record = orderDto.getRecord();

        log.info("uid = {}, record = {}", user, record);

        // 生成订单号
        String orderId = UUID.fastUUID().toString().replace("-", "");
        log.info("orderId = {}", orderId);

        // 创建订单对象
        Order order = Order.builder()
                .id(orderId)
                .uid(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .total(record.getTotal())
                .isPay(0)
                .nickname(user.getNickname())
                .createTime(new Date())
                .build();

        log.info("order = {}", order);

        // 插入订单对象
        orderMapper.insertSelective(order);

        // 生成订单商品项
        OrderGoods orderGoods = OrderGoods.builder()
                .orderId(orderId)
                .items(record.getItems())
                .build();
        orderGoodsMapper.insertSelective(orderGoods);

        // 删除购物车项
        shoppingRecordService.updateShoppingRecordByUid(user.getId());

        return Result.success("生成订单成功", orderId);
    }

    @Override
    public Order findByOrderId(String orderId) {
        log.info("根据订单ID查询订单, orderId = {}", orderId);

        final Example example = Example.builder(Order.class).build();
        example.createCriteria()
                .andEqualTo("id", orderId)
                .andEqualTo("status", 1);
        example.setOrderByClause("update_time DESC");

        return orderMapper.selectOneByExample(example);
    }

    @Override
    public Result findOrderGoodsByOrderId(String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return Result.business("参数错误", Optional.empty());
        }

        final Example example = Example.builder(OrderGoods.class).build();
        example.createCriteria().andEqualTo("orderId", orderId).andEqualTo("status", 1);
        example.setOrderByClause("update_time DESC");

        final OrderGoods orderGoods = orderGoodsMapper.selectOneByExample(example);
        return Result.success("查询订单项成功", orderGoods);
    }

    @Transactional
    @Override
    public Result deleteByOrderId(String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return Result.business("删除订单参数错误", Optional.empty());
        }
        log.info("删除订单, orderId = {}", orderId);
        // 根据订单号删除订单和订单项(逻辑删除)
        orderMapper.deleteByOrderId(orderId);
        orderGoodsMapper.deleteByOrderId(orderId);
        return Result.success("删除订单成功", orderId);
    }

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public void fallbackUpdateOrder(String orderId) {

        final AlipayTradeQueryResponse response;
        try {
            response = Payment.Common().query(orderId);
            orderMapper.fallbackUpdateOrder(orderId);
            log.info("alipay response: {}", response);
        } catch (Exception e) {
            log.error("支付回调出错, error = {}", e.getMessage());
        }
    }

    @Override
    public Result findByUid(Long uid) {
        if (uid == null || uid <= 0) {
            return Result.business("参数错误", Optional.empty());
        }
        final Example example = Example.builder(Order.class).build();
        example.createCriteria().andEqualTo("uid", uid).andNotEqualTo("status", -1);
        example.setOrderByClause("update_time DESC");
        final List<Order> orderList = orderMapper.selectByExample(example);
        return Result.success("查询订单成功", orderList);
    }
}

