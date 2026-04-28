package com.jzo2o.foundations.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2023-07-03
 */
public interface ServeMapper extends BaseMapper<Serve> {
    /**
     * 根据区域id查询服务信息
     *
     * @param regionId 区域id
     * @return 服务信息
     */
    List<ServeResDTO> queryListByRegionId(Long regionId);

    /**
     * 首页服务分类及项目
     *
     * @param regionId 区域id
     * @return 服务分类及项目
     */
    List<ServeCategoryResDTO> findListByRegionId(Long regionId);

    /**
     * 精选推荐
     *
     * @param regionId 区域id
     * @return 精选推荐
     */
    List<ServeAggregationSimpleResDTO> findServeListByRegionId(Long regionId);

    /**
     * 服务详情
     *
     * @param id 服务id
     * @return 服务详情
     */
    ServeAggregationSimpleResDTO serveDetail(Long id);

    /**
     * 服务类型
     *
     * @param regionId 区域id
     * @return 服务类型
     */
    List<ServeAggregationTypeSimpleResDTO> findServeTypeListByRegionId(Long regionId);

    /**
     * 根据ID查询服务详情
     *
     * @param id 服务id
     * @return 服务详情
     */
    ServeAggregationResDTO findServeDetailById(Long id);
}
