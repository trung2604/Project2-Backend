package com.project2.BookStore.dto;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Category;

public class BookWithCategoryDTO {
    private String id;
    private Book.Image image;
    private String mainText;
    private String author;
    private long price;
    private int sold;
    private int quantity;
    private String categoryId;
    private Category category; // Thêm thông tin thể loại

    public BookWithCategoryDTO(String id, Book.Image image, String mainText, String author, 
                             long price, int sold, int quantity, String categoryId, Category category) {
        this.id = id;
        this.image = image;
        this.mainText = mainText;
        this.author = author;
        this.price = price;
        this.sold = sold;
        this.quantity = quantity;
        this.categoryId = categoryId;
        this.category = category;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Book.Image getImage() { return image; }
    public void setImage(Book.Image image) { this.image = image; }

    public String getMainText() { return mainText; }
    public void setMainText(String mainText) { this.mainText = mainText; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
} 