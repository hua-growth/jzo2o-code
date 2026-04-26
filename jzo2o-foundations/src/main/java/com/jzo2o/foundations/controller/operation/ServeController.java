package com.jzo2o.foundations.controller.operation;


import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.model.Result;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.request.RegionPageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.RegionUpsertReqDTO;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.RegionResDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import com.jzo2o.foundations.service.IServeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 运营端 - 区域服务相关接口
 */
@RestController("operationServeController")
@RequestMapping("/operation/serve")
@Api(tags = "运营端 - 区域服务相关接口")
public class ServeController {

    @Autowired
    private IServeService serveService;

    @GetMapping("/page")
    @ApiOperation("区域服务分页查询")
    public PageResult<ServeResDTO> page(ServePageQueryReqDTO servePageQueryReqDTO) {
        return serveService.findByPage(servePageQueryReqDTO);
    }

    @PostMapping("/batch")
    @ApiOperation("区域服务新增")
    public Result add(@RequestBody List<ServeUpsertReqDTO> dtoList) {
        serveService.add(dtoList);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @ApiOperation("区域服务价格修改")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class),
            @ApiImplicitParam(name = "price", value = "区域服务修改后的价格", required = true, dataTypeClass = BigDecimal.class),
    })
    public void update(@NotNull(message = "id不能为空") @PathVariable("id") Long id,
                       @RequestParam("price") BigDecimal price) {
        Serve serve = new Serve();
        serve.setId(id);
        serve.setPrice(price);
        serveService.updateById(serve);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("区域服务删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void delete(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        serveService.deleteById(id);
    }

    @PutMapping("/onSale/{id}")
    @ApiOperation("区域服务上架")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void onSale(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        serveService.onSale(id);
    }

    @PutMapping("/offSale/{id}")
    @ApiOperation("区域服务下架")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void offSale(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        serveService.offSale(id);
    }

    @PutMapping("/onHot/{id}")
    @ApiOperation("区域服务设为热门")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void onHot(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        serveService.onHot(id);
    }

    @PutMapping("/offHot/{id}")
    @ApiOperation("区域服务取消热门")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void offHot(@NotNull(message = "id不能为空") @PathVariable("id") Long id) {
        serveService.offHot(id);
    }
}