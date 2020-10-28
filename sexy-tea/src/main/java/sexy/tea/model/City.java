package sexy.tea.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * author 大大大西西瓜皮🍉
 * date 15:10 2020-09-26
 * description:
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "city")
public class City implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 城市表id
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;
    /**
     * 城市名
     */
    @Column(name = "city")
    private String city;
    /**
     * 城市经度
     */
    @Column(name = "center_longitude")
    private String centerLongitude;
    /**
     * 城市纬度
     */
    @Column(name = "center_latitude")
    private String centerLatitude;
    /**
     * 门店数量
     */
    @Column(name = "`count`")
    private Integer count;
    /**
     * 记录状态：1 正常，0 禁用，-1 删除
     */
    @Column(name = "`status`")
    private Integer status;
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}