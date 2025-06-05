package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDTO {
    private String id;
    private ImageDTO image;
    private String mainText;
    private String author;
    private long price;
    private int sold;
    private int quantity;
    private String categoryId;
    private CategoryDTO category;

    private static final Logger log = LoggerFactory.getLogger(BookResponseDTO.class);

    public BookResponseDTO(Book book) {
        this.id = book.getId();
        if (book.getImage() != null) {
            this.image = new ImageDTO(
                book.getImage().getThumbnail(),
                book.getImage().getMedium(),
                book.getImage().getOriginal(),
                book.getImage().getFormat(),
                book.getImage().getSize()
            );
            log.info("Đã chuyển đổi thông tin ảnh - Thumbnail: {}, Medium: {}, Original: {}", 
                    this.image.getThumbnail(), this.image.getMedium(), this.image.getOriginal());
        } else {
            log.warn("Không có thông tin ảnh cho sách ID: {}", book.getId());
            this.image = null;
        }
        this.mainText = book.getMainText();
        this.author = book.getAuthor();
        this.price = book.getPrice();
        this.sold = book.getSold();
        this.quantity = book.getQuantity();
        this.categoryId = book.getCategoryId();
        
        // Ensure category is properly loaded
        Category category = book.getCategory();
        if (category != null) {
            // Verify category name is not an ID
            if (category.getName() == null || category.getName().equals(category.getId())) {
                throw new IllegalStateException("Category name is invalid or equals to ID: " + category.getId());
            }
            this.category = new CategoryDTO(category);
        } else {
            throw new IllegalStateException("Category is null for book: " + book.getId());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDTO {
        private String id;
        private String name;
        private String description;

        public CategoryDTO(Category category) {
            if (category == null) {
                throw new IllegalArgumentException("Category cannot be null");
            }
            this.id = category.getId();
            // Ensure name is not an ID
            if (category.getName() == null || category.getName().equals(category.getId())) {
                throw new IllegalArgumentException("Category name is invalid or equals to ID: " + category.getId());
            }
            this.name = category.getName();
            this.description = category.getDescription();
        }
    }
} 