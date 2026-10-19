package com.example.live.converter;

import com.example.live.entity.Category;
import com.example.live.vo.CategoryVO;

public final class CategoryConverter {

    private CategoryConverter() {
    }

    public static CategoryVO toVO(Category category) {
        return new CategoryVO(category.getId(), category.getName(), category.getIcon(), category.getSort());
    }
}
