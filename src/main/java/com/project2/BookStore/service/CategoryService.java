package com.project2.BookStore.service;

import com.project2.BookStore.dto.CategoryDTO;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryService {
    CategoryDTO createCategory(CategoryDTO categoryDTO) throws BadRequestException;
    CategoryDTO updateCategory(String id, CategoryDTO categoryDTO) throws BadRequestException;
    void deleteCategory(String id) throws BadRequestException;
    CategoryDTO getCategoryById(String id) throws BadRequestException;
    List<CategoryDTO> getAllCategories();
    Page<CategoryDTO> getCategoriesPaged(Pageable pageable);
    List<CategoryDTO> searchCategories(String keyword);
    boolean existsByName(String name);
} 