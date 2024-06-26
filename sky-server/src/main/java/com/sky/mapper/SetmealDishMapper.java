package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    List<Long> getSetmealIdByDishIds(List<Long> ids);

    void deleteByIds(List<Long> ids);

    void save(SetmealDish setmealDish);
}
