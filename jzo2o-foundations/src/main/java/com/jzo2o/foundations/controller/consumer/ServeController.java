package com.jzo2o.foundations.controller.consumer;

import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;
import com.jzo2o.foundations.model.dto.response.ServeSimpleResDTO;
import com.jzo2o.foundations.service.IServeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerServeController")
@RequestMapping("/customer/serve")
@Api(tags = "用户端 - 首页服务相关接口")
public class ServeController {
    @Resource
    private IServeService serveService;

    @GetMapping("/firstPageServeList")
    @ApiOperation("首页服务分类及项目")
    public List<ServeCategoryResDTO> firstPageServeList(@RequestParam("regionId") Long regionId) {
        return serveService.firstPageServeList(regionId);
    }

    @GetMapping("/hotServeList")
    @ApiOperation("精选推荐")
    public List<ServeAggregationSimpleResDTO> hotServeList(Long regionId) {
        return serveService.hotServeList(regionId);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询服务详情")
    public ServeAggregationSimpleResDTO serveDetail(@PathVariable("id") Long id) {
        return serveService.serveDetail(id);
    }

    @GetMapping("/serveTypeList")
    @ApiOperation("获取全部服务分类")
    public List<ServeAggregationTypeSimpleResDTO> serveTypeList(@RequestParam("regionId") Long regionId) {
        return serveService.serveTypeList(regionId);
    }
    @GetMapping("/search")
    @ApiOperation("服务搜索")
    public List<ServeSimpleResDTO> search(String cityCode, String keyword, Long serveTypeId) {
        return serveService.search(cityCode, keyword, serveTypeId);
    }
}