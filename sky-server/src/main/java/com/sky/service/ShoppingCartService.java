package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
    public List<ShoppingCart> getByUserId();
    public void clean();
    public void deleteOneObject(ShoppingCartDTO shoppingCartDTO);
}
