package com.project2.BookStore.service;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryService {
    Page<CategoryDTO> getAllCategories(Pageable pageable) throws BadRequestException;
    
    CategoryDTO getCategoryById(String id) throws BadRequestException;
    
    CategoryDTO addCategory(AddCategoryRequest request) throws BadRequestException;
    
    CategoryDTO updateCategory(UpdateCategoryRequest request) throws BadRequestException;
    
    void deleteCategory(String id) throws BadRequestException;
    
    List<CategoryDTO> searchCategories(String keyword) throws BadRequestException;

    List<CategoryCountDTO> countCategories() throws BadRequestException;
} 