package com.project2.BookStore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "books")
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "thumbnail", column = @Column(name = "image_thumbnail")),
        @AttributeOverride(name = "medium", column = @Column(name = "image_medium")),
        @AttributeOverride(name = "original", column = @Column(name = "image_original")),
        @AttributeOverride(name = "format", column = @Column(name = "image_format")),
        @AttributeOverride(name = "size", column = @Column(name = "image_size")),
        @AttributeOverride(name = "publicId", column = @Column(name = "image_public_id"))
    })
    private Image image;

    @NotBlank(message = "Tên sách không được để trống")
    @Column(nullable = false, columnDefinition = "text")
    private String mainText;

    @NotBlank(message = "Tác giả không được để trống")
    @Column(nullable = false, columnDefinition = "text")
    private String author;

    @NotNull(message = "Giá sách không được để trống")
    @Min(value = 0, message = "Giá sách phải lớn hơn hoặc bằng 0")
    @Column(nullable = false)
    private Long price;

    @NotNull(message = "Số lượng đã bán không được để trống")
    @Min(value = 0, message = "Số lượng đã bán phải lớn hơn hoặc bằng 0")
    @Column(nullable = false)
    private Integer sold;

    @NotNull(message = "Số lượng trong kho không được để trống")
    @Min(value = 0, message = "Số lượng trong kho phải lớn hơn hoặc bằng 0")
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank(message = "Thể loại không được để trống")
    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String thumbnail;
        private String medium;
        private String original;
        private String format;
        private Long size;
        private String publicId;

        public boolean isValid() {
            return thumbnail != null && medium != null && original != null;
        }

        public static Image fromUrl(String url) {
            if (url == null || url.trim().isEmpty()) {
                return null;
            }
            Image image = new Image();
            image.setOriginal(url);
            image.setMedium(url);
            image.setThumbnail(url);
            image.setFormat("jpg");
            image.setSize(0L);
            image.setPublicId("books/external_" + System.currentTimeMillis());
            return image;
        }
    }
} 