package sexy.tea.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import sexy.tea.model.Coupon;

import java.util.List;

/**
 * author 大大大西西瓜皮🍉
 * date 15:10 2020-09-26
 * description:
 */
@Mapper
public interface CouponMapper extends tk.mybatis.mapper.common.Mapper<Coupon> {
    int updateBatch(List<Coupon> list);

    int updateBatchSelective(List<Coupon> list);

    int batchInsert(@Param("list") List<Coupon> list);

    int insertOrUpdate(Coupon record);

    int insertOrUpdateSelective(Coupon record);
}