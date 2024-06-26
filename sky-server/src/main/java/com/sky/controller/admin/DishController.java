package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")

public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("菜品添加接口")
    public Result save(@RequestBody DishDTO dishDTO){
        dishService.save(dishDTO);

        Long categoryId = dishDTO.getCategoryId();
        String key = "dish_" + categoryId;
        redisTemplate.delete(key);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        PageResult result = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(result);
    }

    /**
     * 根据类型查询分类
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){
        dishService.deleteBatch(ids);

        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return Result.success();
    }

    /**
     * 根据Id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getDishWithFlavorById(@PathVariable("id") Long id){
        DishVO dishVO = dishService.getDishWithFlavorById(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        dishService.update(dishDTO);
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return Result.success();
    }

    /**
     * 菜品停售，起售
     * @param status
     * @Param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售，禁售")
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishService.startOrStop(dish);

        DishVO dishVO = dishService.getDishWithFlavorById(id);
        String key = "dish_" + dishVO.getCategoryId();
        redisTemplate.delete(key);

        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(Long categoryId){
        List<Dish> dishes = dishService.getBycategoryId(categoryId);
        return Result.success(dishes);
    }
}
