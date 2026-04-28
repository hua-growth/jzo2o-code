package com.jzo2o.customer.controller.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.customer.model.domain.AddressBook;
import com.jzo2o.customer.model.dto.request.AddressBookPageQueryReqDTO;
import com.jzo2o.customer.model.dto.request.AddressBookUpsertReqDTO;
import com.jzo2o.customer.service.IAddressBookService;
import com.jzo2o.customer.service.ICommonUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController("consumerAddressBookController")
@RequestMapping("/consumer/address-book")
@Api(tags = "用户端 - 用户地址相关接口")
public class AddressBookController {
    @Resource
    private ICommonUserService commonUserService;

    @Autowired
    private IAddressBookService addressBookService;

    @GetMapping("/defaultAddress")
    @ApiOperation("获取用户默认地址")
    public AddressBookResDTO obtainDefaultAddress(){
        return addressBookService.obtainDefaultAddress();
    }

    @PostMapping()
    @ApiOperation("新增用户地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "addressBookUpsertReqDTO", value = "新增地址参数", required = true, dataType = "AddressBookUpsertReqDTO")
    })
    public void addAddress(@RequestBody AddressBookUpsertReqDTO addressBookUpsertReqDTO){
        addressBookService.addAddress(addressBookUpsertReqDTO);
    }

    @ApiOperation("地址薄分页查询")
    @GetMapping("/page")
    public PageResult<AddressBookResDTO> page(AddressBookPageQueryReqDTO addressBookPageQueryReqDTO) {
        return addressBookService.page(addressBookPageQueryReqDTO);
    }

    @ApiOperation("地址薄详情")
    @GetMapping("/{id}")
    public AddressBookResDTO detail(@PathVariable("id") Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        return BeanUtil.toBean(addressBook, AddressBookResDTO.class);
    }

    @ApiOperation("地址薄修改")
    @PutMapping("/{id}")
    public void update(@PathVariable("id") Long id, @RequestBody AddressBookUpsertReqDTO addressBookUpsertReqDTO) {
        addressBookService.updateAddressBook(id, addressBookUpsertReqDTO);
    }

    @ApiOperation("地址薄批量删除")
    @DeleteMapping("/batch")
    public void logicallyDelete(@RequestBody List<Long> ids) {
        addressBookService.removeByIds(ids);
    }

    @ApiOperation("地址薄设为默认/取消默认")
    @PutMapping("/default")
    public void updateDefaultStatus(Long id, Integer flag) {
        addressBookService.updateDefaultStatus(id, flag);
    }
}
