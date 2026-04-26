package com.jzo2o.foundations.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.mapper.RegionMapper;
import com.jzo2o.foundations.mapper.ServeItemMapper;
import com.jzo2o.foundations.mapper.ServeMapper;
import com.jzo2o.foundations.model.domain.Region;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.domain.ServeItem;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Override
    public void offHot(Long id) {
        //取消热门0
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getIsHot()== 0) {
            throw new ForbiddenOperationException("取消热门失败,服务已经是非热门");
        }
        serve.setIsHot(0);
        baseMapper.updateById(serve);
    }
    @Override
    public void onHot(Long id) {
        //设为热门1
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getIsHot()== 1) {
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


}