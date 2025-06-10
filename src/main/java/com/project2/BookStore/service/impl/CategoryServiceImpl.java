package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Category;
import com.project2.BookStore.repository.CategoryRepository;
import com.project2.BookStore.service.CategoryService;
import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Page<CategoryDTO> getAllCategories(Pageable pageable) throws BadRequestException {
        try {
            Page<Category> categoryPage = categoryRepository.findAll(pageable);
            return categoryPage.map(CategoryDTO::new);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy danh sách danh mục: " + e.getMessage());
        }
    }

    @Override
    public CategoryDTO getCategoryById(String id) throws BadRequestException {
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + id));
            return new CategoryDTO(category);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể lấy thông tin danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CategoryDTO addCategory(AddCategoryRequest request) throws BadRequestException {
        try {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Danh mục với tên này đã tồn tại");
            }

            Category category = new Category(request.getName(), request.getDescription());
            Category savedCategory = categoryRepository.save(category);
            log.info("Đã thêm danh mục mới: {}", savedCategory.getName());
            return new CategoryDTO(savedCategory);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi thêm danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể thêm danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(UpdateCategoryRequest request) throws BadRequestException {
        try {
            Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + request.getId()));

            // Kiểm tra nếu tên mới khác tên cũ và đã tồn tại
            if (!category.getName().equals(request.getName()) && 
                categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Danh mục với tên này đã tồn tại");
            }

            category.setName(request.getName());
            category.setDescription(request.getDescription());
            
            Category updatedCategory = categoryRepository.save(category);
            log.info("Đã cập nhật danh mục: {}", updatedCategory.getName());
            return new CategoryDTO(updatedCategory);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể cập nhật danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteCategory(String id) throws BadRequestException {
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + id));

            if (!category.getBooks().isEmpty()) {
                throw new BadRequestException("Không thể xóa danh mục đang có sách");
            }

            categoryRepository.delete(category);
            log.info("Đã xóa danh mục: {}", category.getName());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xóa danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể xóa danh mục: " + e.getMessage());
        }
    }

    @Override
    public List<CategoryDTO> searchCategories(String keyword) throws BadRequestException {
        try {
            List<Category> categories = categoryRepository.findByNameContainingIgnoreCase(keyword);
            return categories.stream()
                .map(CategoryDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể tìm kiếm danh mục: " + e.getMessage());
        }
    }

    @Override
    public List<CategoryCountDTO> countCategories() throws BadRequestException {
        try {
            List<Category> categories = categoryRepository.findAllOrderByNameAsc();
            List<CategoryCountDTO> result = new ArrayList<>();
            
            for (Category category : categories) {
                CategoryCountDTO dto = new CategoryCountDTO(
                    category.getId(),
                    category.getName(),
                    category.getBooks().size()
                );
                result.add(dto);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi đếm số lượng sách theo danh mục: {}", e.getMessage());
            throw new BadRequestException("Không thể đếm số lượng sách theo danh mục: " + e.getMessage());
        }
    }
} 