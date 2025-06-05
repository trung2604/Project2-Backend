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
    public Page<CategoryResponseDTO> getAllCategories(Pageable pageable) throws BadRequestException {
        try {
            Page<Category> categoryPage = categoryRepository.findAll(pageable);
            return categoryPage.map(CategoryResponseDTO::new);
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi lấy danh sách danh mục: " + e.getMessage());
        }
    }

    @Override
    public CategoryResponseDTO getCategoryById(String id) throws BadRequestException {
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + id));
            return new CategoryResponseDTO(category);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi lấy thông tin danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CategoryResponseDTO addCategory(AddCategoryRequest request) throws BadRequestException {
        try {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Danh mục với tên '" + request.getName() + "' đã tồn tại");
            }

            Category category = new Category(request.getName(), request.getDescription());
            Category savedCategory = categoryRepository.save(category);
            return new CategoryResponseDTO(savedCategory);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi thêm danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(UpdateCategoryRequest request) throws BadRequestException {
        log.info("Bắt đầu cập nhật danh mục. CategoryId: {}", request.getId());
        try {
            // Validate request
            if (request.getName() != null && request.getName().trim().isEmpty()) {
                throw new BadRequestException("Tên danh mục không được để trống");
            }

            // Find category
            Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + request.getId()));

            // Check name uniqueness if name is being updated
            if (request.getName() != null && !request.getName().equals(category.getName())) {
                String trimmedName = request.getName().trim();
                if (categoryRepository.existsByName(trimmedName)) {
                    throw new BadRequestException("Danh mục với tên '" + trimmedName + "' đã tồn tại");
                }
                category.setName(trimmedName);
                log.info("Đã cập nhật tên danh mục từ '{}' thành '{}'", category.getName(), trimmedName);
            }

            // Update description if provided
            if (request.getDescription() != null) {
                String trimmedDescription = request.getDescription().trim();
                category.setDescription(trimmedDescription);
                log.info("Đã cập nhật mô tả danh mục");
            }

            Category updatedCategory = categoryRepository.save(category);
            log.info("Cập nhật danh mục thành công. CategoryId: {}", updatedCategory.getId());
            return new CategoryResponseDTO(updatedCategory);
        } catch (BadRequestException e) {
            log.warn("Lỗi khi cập nhật danh mục: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật danh mục: {}", e.getMessage());
            throw new BadRequestException("Lỗi khi cập nhật danh mục: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteCategory(String id) throws BadRequestException {
        try {
            Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với ID: " + id));

            if (!category.getBooks().isEmpty()) {
                throw new BadRequestException("Không thể xóa danh mục này vì đang có sách thuộc danh mục");
            }

            categoryRepository.deleteById(id);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi xóa danh mục: " + e.getMessage());
        }
    }

    @Override
    public List<CategoryResponseDTO> searchCategories(String keyword) throws BadRequestException {
        try {
            List<Category> categories = categoryRepository.findByNameContainingIgnoreCase(keyword);
            return categories.stream()
                .map(CategoryResponseDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi tìm kiếm danh mục: " + e.getMessage());
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
            throw new BadRequestException("Lỗi khi đếm số lượng sách theo danh mục: " + e.getMessage());
        }
    }
} 