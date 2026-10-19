package com.example.live.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.live.converter.CategoryConverter;
import com.example.live.entity.Category;
import com.example.live.mapper.CategoryMapper;
import com.example.live.service.CategoryService;
import com.example.live.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryVO> listAll() {
        List<Category> categories = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort).orderByAsc(Category::getId));
        return categories.stream().map(CategoryConverter::toVO).toList();
    }
}
