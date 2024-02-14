package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    public void save(DishDTO dishDTO);

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);
    public void deleteBatch(List<Long> ids);
    public DishVO getDishWithFlavorById(Long id);
    public void update(DishDTO dishDTO);
    public List<DishVO> listWithFlavor(Dish dish);
    public void startOrStop(Dish dish);

    List<Dish> getBycategoryId(Long categoryId);
}
