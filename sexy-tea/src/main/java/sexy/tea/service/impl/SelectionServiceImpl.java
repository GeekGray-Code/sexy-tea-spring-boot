package sexy.tea.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sexy.tea.exception.BusinessException;
import sexy.tea.mapper.SelectionMapper;
import sexy.tea.model.Selection;
import sexy.tea.model.common.Pager;
import sexy.tea.model.common.Result;
import sexy.tea.model.dto.MinioDto;
import sexy.tea.service.SelectionService;
import sexy.tea.utils.MinioUtils;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * author 大大大西西瓜皮🍉
 * date 15:10 2020-09-26
 * description:
 */
@Service
@Slf4j
public class SelectionServiceImpl implements SelectionService {

    private final SelectionMapper selectionMapper;

    @Autowired
    public SelectionServiceImpl(SelectionMapper selectionMapper) {
        this.selectionMapper = selectionMapper;
    }

    @Value("${minio.prefix}")
    private String prefix;

    @Value("${minio.defaultBucketName}")
    private String defaultBucketName;

    @Override
    public int updateBatch(List<Selection> list) {
        return selectionMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<Selection> list) {
        return selectionMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<Selection> list) {
        return selectionMapper.batchInsert(list);
    }

    @Override
    public int insertOrUpdate(Selection record) {
        return selectionMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(Selection record) {
        return selectionMapper.insertOrUpdateSelective(record);
    }

    @Override
    public Result find(int pageNum, int pageSize) {
        Page<Selection> page = PageHelper.startPage(pageNum, pageSize);
        Example example = Example.builder(Selection.class).build();
        example.createCriteria().andNotEqualTo("status", -1);
        List<Selection> selectionList = selectionMapper.selectByExample(example);
        return Result.success("列表查询", Pager.<Selection>builder()
                .pageNum(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .result(selectionList)
                .build());
    }

    @Override
    public Result findByPrimaryKey(Integer primaryKey) {
        Selection selection = selectionMapper.selectByPrimaryKey(primaryKey);
        if (selection == null || primaryKey <= 0) {
            return Result.notFound();
        }
        return Result.success("主键: " + primaryKey, selection);
    }

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public Result saveOrUpdate(Selection selection) {
        if (selection == null) {
            // 异常
            return Result.business("参数异常!", Optional.empty());
        }
        if (selection.getId() == null || selection.getId() <= 0) {
            // 插入数据
            selection.setProductId(UUID.randomUUID().toString().replace("-", ""));
            selectionMapper.insertSelective(selection);
        } else {
            // 更新数据
            selectionMapper.updateByPrimaryKeySelective(selection);
        }
        return Result.success("更改成功", selection);
    }

    @Override
    public Result uploadImage(MinioDto dto, Long id) {
        // 根据 product_id 查询实体记录
        Example example = Example.builder(Selection.class).build();
        example.createCriteria()
                .andEqualTo("id", id)
                .andEqualTo("status", 1);
        Selection selection = selectionMapper.selectOneByExample(example);
        // 校验
        if (selection == null) {
            return Result.business("参数错误, id: " + id, Optional.empty());
        }
        String name = selection.getProductId() + dto.getSuffix();
        // 图片
        try {
            InputStream is = dto.getFile().getInputStream();
            MinioUtils.upload(defaultBucketName, name, is, dto.getContentType());
        } catch (IOException e) {
            log.error("上传失败, 错误信息：{}", e.getMessage());
        }
        String url = prefix + name;
        // 更新图片地址
        selection.setProductImage(url);
        selectionMapper.updateByPrimaryKey(selection);
        return Result.success("图片上传成功, 地址为： " + url, Optional.empty());
    }

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public Result delete(Integer id) {
        if (id == null || id <= 0) {
            // 校验
            return Result.business("参数错误", Optional.empty());
        }
        int row = selectionMapper.deleteByPrimaryKey(id);
        return Result.success("删除成功, 受影响的行数: " + row, Optional.empty());
    }

    @Override
    public Result findByName(String name, int pageNum, int pageSize) {
        if (StringUtils.isEmpty(name)) {
            return Result.business("参数错误", Optional.empty());
        }
        name += "%";
        Page<Selection> page = PageHelper.startPage(pageNum, pageSize);
        List<Selection> selections = selectionMapper.findByName(name);
        if (selections == null) {
            return Result.business("查询的饮品不存在", Optional.empty());
        }
        return Result.success("关键词: " + name, Pager.<Selection>builder()
                .pageSize(page.getPageSize())
                .pageNum(page.getPageNum())
                .total(page.getTotal())
                .result(selections)
                .build());
    }
}
