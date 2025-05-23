package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Category;
import com.project2.BookStore.repository.CategoryRepository;
import com.project2.BookStore.service.CategoryService;
import com.project2.BookStore.dto.CategoryDTO;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) throws BadRequestException {
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new BadRequestException("Danh mục với tên '" + categoryDTO.getName() + "' đã tồn tại");
        }

        Category category = new Category(categoryDTO.getName(), categoryDTO.getDescription());
        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(String id, CategoryDTO categoryDTO) throws BadRequestException {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với id: " + id));

        // Kiểm tra nếu tên mới khác tên cũ và đã tồn tại
        if (!category.getName().equals(categoryDTO.getName()) && 
            categoryRepository.existsByName(categoryDTO.getName())) {
            throw new BadRequestException("Danh mục với tên '" + categoryDTO.getName() + "' đã tồn tại");
        }

        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setUpdatedAt(java.time.LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(String id) throws BadRequestException {
        if (!categoryRepository.existsById(id)) {
            throw new BadRequestException("Không tìm thấy danh mục với id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryDTO getCategoryById(String id) throws BadRequestException {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy danh mục với id: " + id));
        return convertToDTO(category);
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllOrderByNameAsc().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Page<CategoryDTO> getCategoriesPaged(Pageable pageable) {
        return categoryRepository.findAll(pageable)
            .map(this::convertToDTO);
    }

    @Override
    public List<CategoryDTO> searchCategories(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    private CategoryDTO convertToDTO(Category category) {
        return new CategoryDTO(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
} 