package com.jzo2o.foundations.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.constants.RedisConstants;
import com.jzo2o.foundations.mapper.*;
import com.jzo2o.foundations.model.domain.*;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.*;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 区域服务管理
 *
 * @author itcast
 * @create 2023/7/17 16:50
 **/
@Service
public class ServeServiceImpl extends ServiceImpl<ServeMapper, Serve> implements IServeService {

    @Autowired
    private ServeItemMapper serveItemMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private ServeMapper serveMapper;

    @Autowired
    private ServeSyncMapper serveSyncMapper;

    @Autowired
    private ServeTypeMapper serveTypeMapper;

    @Autowired
    private RestHighLevelClient client;

    @Override
    public ServeAggregationResDTO findServeDetailById(Long id) {
        return baseMapper.findServeDetailById(id);
    }
    @Override
    public List<ServeSimpleResDTO> search(String cityCode, String keyword, Long serveTypeId) {
        //1. 创建请求对象
        SearchRequest request = new SearchRequest("serve_aggregation");

        //2. 封装请求参数
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //城市编码
        boolQuery.must(QueryBuilders.termQuery("city_code", cityCode));
        //服务类型id
        if (serveTypeId != null) {
            boolQuery.must(QueryBuilders.termQuery("serve_type_id", serveTypeId));
        }
        //关键词
        if (StrUtil.isNotEmpty(keyword)) {
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, "serve_item_name", "serve_type_name"));
        }
        request.source().query(boolQuery);//查询
        request.source().sort("serve_item_sort_num", SortOrder.ASC);//排序

        //3. 执行请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //4. 处理返回结果   List<ServeSimpleResDTO>
        if (response.getHits().getTotalHits().value == 0) {
            return List.of();
        }
        return Arrays.stream(response.getHits().getHits())
                .map(e -> JSONUtil.toBean(e.getSourceAsString(), ServeSimpleResDTO.class))
                .collect(Collectors.toList());
    }
    @Override
    public List<ServeAggregationTypeSimpleResDTO> serveTypeList(Long regionId) {
        //1 对区域进行校验
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2 查询当前区域下上架服务对应的分类
        return baseMapper.findServeTypeListByRegionId(regionId);
    }

    public ServeAggregationSimpleResDTO serveDetail(Long id) {
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve)) {
            throw new ForbiddenOperationException("服务详情查询失败,服务不存在");
        }
        return serveMapper.serveDetail(id);
    }

    @Caching(
            cacheable = {
                    //返回数据为空，则缓存空值30分钟，这样可以避免缓存穿透
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE, key = "#regionId",
                            unless = "#result.size() > 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),

                    //返回值不为空，则永久缓存数据
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE, key = "#regionId",
                            unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeAggregationSimpleResDTO> hotServeList(Long regionId) {
        //1 对区域进行校验
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2 查询指定区域下上架且热门的服务项目信息
        return baseMapper.findServeListByRegionId(regionId);
    }

    @Caching(
            cacheable = {
                    //返回数据为空，则缓存空值30分钟，这样可以避免缓存穿透
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key = "#regionId",
                            unless = "#result.size() > 0", cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),

                    //返回值不为空，则永久缓存数据
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key = "#regionId",
                            unless = "#result.size() == 0", cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeCategoryResDTO> firstPageServeList(Long regionId) {
        //1 对区域进行校验-区域是否被启用
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2. 查询指定区域下上架的服务分类及项目信息
        List<ServeCategoryResDTO> list = baseMapper.findListByRegionId(regionId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        //3. 截取
        list = CollUtil.sub(list, 0, Math.min(list.size(), 2));//服务类型截取
        list.forEach(e ->
                //服务项目截取
                e.setServeResDTOList(CollUtil.sub(e.getServeResDTOList(), 0, Math.min(e.getServeResDTOList().size(), 4)))
        );
        return list;
    }


    @Override
    public void offHot(Long id) {
        //取消热门0
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getIsHot() == 0) {
            throw new ForbiddenOperationException("取消热门失败,服务已经是非热门");
        }
        serve.setIsHot(0);
        baseMapper.updateById(serve);
    }


    @Override
    public void onHot(Long id) {
        //设为热门1
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getIsHot() == 1) {
            throw new ForbiddenOperationException("设为热门失败,服务已经是热门");
        }
        serve.setIsHot(1);
        serve.setHotTimeStamp(System.currentTimeMillis());
        baseMapper.updateById(serve);
    }


    @Override
    public void offSale(Long id) {
        //1) 区域服务当前上架状态
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getSaleStatus() != 2) {
            throw new ForbiddenOperationException("下架区域服务失败,服务不是上架状态");
        }
        //2) 下架1
        serve.setSaleStatus(1);
        baseMapper.updateById(serve);
        //3） 删除同步表数据
        serveSyncMapper.deleteById(id);
    }


    @Override
    public void onSale(Long id) {
        //1) 区域服务当前非上架状态
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getSaleStatus() == 2) {
            throw new ForbiddenOperationException("上架区域服务失败,服务已是上架状态");
        }
        //2) 服务项目是启用状态2
        ServeItem serveItem = serveItemMapper.selectById(serve.getServeItemId());
        if (ObjectUtil.isNull(serveItem) || serveItem.getActiveStatus() != 2) {
            throw new ForbiddenOperationException("上架区域服务失败,服务项目不是启用状态");
        }
        //3) 上架
        serve.setSaleStatus(2);
        baseMapper.updateById(serve);
        //4）添加同步表数据
        addServeSync(id);
    }


    @Override
    public void deleteById(Long id) {
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getSaleStatus() != 0) {
            throw new ForbiddenOperationException("删除区域服务失败,服务不是草稿状态");
        }
        baseMapper.deleteById(id);
    }


    @Override
    public PageResult<ServeResDTO> findByPage(ServePageQueryReqDTO servePageQueryReqDTO) {
        return PageHelperUtils.selectPage(servePageQueryReqDTO,
                () -> baseMapper.queryListByRegionId(servePageQueryReqDTO.getRegionId()));
    }

    //这个业务方法中是多次对数据库进行保存操作,务必进行事务处理
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(List<ServeUpsertReqDTO> dtoList) {
        //遍历集合 拿到每一个区域服务(服务项目id  区域id 价格)
        for (ServeUpsertReqDTO dto : dtoList) {
            //1. 服务项目必须是启用状态的才能添加到区域
            //根据服务项目id去serve_item查询记录
            ServeItem serveItem = serveItemMapper.selectById(dto.getServeItemId());
            if (ObjectUtil.isNull(serveItem) || serveItem.getActiveStatus() != 2) {
                throw new ForbiddenOperationException("添加失败,服务项目状态有误");
            }

            //2. 一个服务项目对于一个区域，只能添加一次
            //根据指定的服务项目id和区域id进行统计,如果统计结果是>0 代表当前项目在当前区域已经存在,就不能再次添加了
            //lambdaQuery()是条件构造器 select count(*) from serve where serve_item_id = ? and region_id = ?
            Integer count = this.lambdaQuery()
                    .eq(Serve::getServeItemId, dto.getServeItemId())
                    .eq(Serve::getRegionId, dto.getRegionId())
                    .count();
            if (count > 0) {
                throw new ForbiddenOperationException("添加失败,当前服务项目已经存在");
            }

            //3.向serve数据表进行保存
            //Serve serve = new Serve();
            //serve.setServeItemId(dto.getServeItemId());//serve_item_id
            //serve.setRegionId(dto.getRegionId());//region_id
            //serve.setPrice(dto.getPrice());//price
            Serve serve = BeanUtil.copyProperties(dto, Serve.class);
            //city_code: 需要根据regionId从region表查询
            Region region = regionMapper.selectById(dto.getRegionId());
            if (ObjectUtil.isNotNull(region)) {
                serve.setCityCode(region.getCityCode());
            }
            baseMapper.insert(serve);
        }
    }

    /**
     * 新增服务同步数据
     *
     * @param serveId 服务id
     */
    private void addServeSync(Long serveId) {
        //服务信息
        Serve serve = baseMapper.selectById(serveId);
        //区域信息
        Region region = regionMapper.selectById(serve.getRegionId());
        //服务项信息
        ServeItem serveItem = serveItemMapper.selectById(serve.getServeItemId());
        //服务类型
        ServeType serveType = serveTypeMapper.selectById(serveItem.getServeTypeId());

        ServeSync serveSync = new ServeSync();
        serveSync.setServeTypeId(serveType.getId());
        serveSync.setServeTypeName(serveType.getName());
        serveSync.setServeTypeIcon(serveType.getServeTypeIcon());
        serveSync.setServeTypeImg(serveType.getImg());
        serveSync.setServeTypeSortNum(serveType.getSortNum());

        serveSync.setServeItemId(serveItem.getId());
        serveSync.setServeItemIcon(serveItem.getServeItemIcon());
        serveSync.setServeItemName(serveItem.getName());
        serveSync.setServeItemImg(serveItem.getImg());
        serveSync.setServeItemSortNum(serveItem.getSortNum());
        serveSync.setUnit(serveItem.getUnit());
        serveSync.setDetailImg(serveItem.getDetailImg());
        serveSync.setPrice(serve.getPrice());

        serveSync.setCityCode(region.getCityCode());
        serveSync.setId(serve.getId());
        serveSync.setIsHot(serve.getIsHot());
        serveSyncMapper.insert(serveSync);
    }

}