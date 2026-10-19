package com.example.live.controller;

import com.example.live.common.result.Result;
import com.example.live.service.CategoryService;
import com.example.live.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listAll());
    }
}
