package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
//import io.swagger.v3.oas.annotations.servers.Server;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sky.mapper.ShoppingCartMapper;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO){
        //判断购物车是否存在相应的菜品或套餐
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        ShoppingCart cart;
        //如果已存在，则数量加以一
        if(shoppingCarts!=null && shoppingCarts.size()!=0){
            cart = shoppingCarts.get(0);
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.update(cart);
            return ;
        }
        //如果不存在插入一条新数据
        else{
            cart = new ShoppingCart();
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId!=null) {
                Dish dish = dishMapper.getById(dishId);
                cart.setName(dish.getName());
                cart.setAmount(dish.getPrice());
                cart.setImage(dish.getImage());
                cart.setDishId(dishId);
            }
            else{
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                cart.setName(setmeal.getName());
                cart.setAmount(setmeal.getPrice());
                cart.setImage(setmeal.getImage());
                cart.setSetmealId(setmeal.getId());
            }
            cart.setUserId(BaseContext.getCurrentId());
            cart.setNumber(1);
            cart.setCreateTime(LocalDateTime.now());
            cart.setDishFlavor(shoppingCartDTO.getDishFlavor());
            shoppingCartMapper.insert(cart);
        }
    }
    public List<ShoppingCart> getByUserId(){
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        return shoppingCartMapper.list(shoppingCart);
    }
    public void clean(){
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.clean(userId);
    }

    @Override
    public void deleteOneObject(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .setmealId(shoppingCartDTO.getSetmealId())
                .dishId(shoppingCartDTO.getDishId())
                .dishFlavor(shoppingCartDTO.getDishFlavor())
                .build();
        ShoppingCart cart = shoppingCartMapper.list(shoppingCart).get(0);
        if(cart.getNumber()-1==0){
            shoppingCartMapper.deleteById(cart.getId());
        }
        else{
            cart.setNumber(cart.getNumber()-1);
            shoppingCartMapper.update(cart);
        }
    }
}
