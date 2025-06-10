package com.project2.BookStore.service;

import com.project2.BookStore.dto.*;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface BookService {
    List<BookResponseDTO> getAllBooks() throws BadRequestException;
    
    Page<BookResponseDTO> getBooksPaged(Pageable pageable) throws BadRequestException;
    
    BookResponseDTO getBookById(String id) throws BadRequestException;
    
    /**
     * Thêm sách mới
     * @param request Thông tin sách cần thêm
     * @return Thông tin sách đã thêm
     */
    BookResponseDTO addBook(AddBookRequest request);
    
    BookResponseDTO updateBook(UpdateBookRequest request) throws BadRequestException;
    
    void deleteBook(String id) throws BadRequestException;
    
    Page<BookResponseDTO> getBooksByCategoryPaged(String categoryId, Pageable pageable) throws BadRequestException;

    /**
     * Tìm kiếm và lọc sách theo nhiều tiêu chí
     * @param keyword Từ khóa tìm kiếm (tên sách hoặc tác giả)
     * @param categoryId ID danh mục
     * @param minPrice Giá tối thiểu
     * @param maxPrice Giá tối đa
     * @param inStock Trạng thái tồn kho
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Trang kết quả sách
     * @throws BadRequestException nếu có lỗi xảy ra
     */
    Page<BookResponseDTO> searchBooks(
        String keyword,
        String categoryId,
        Long minPrice,
        Long maxPrice,
        Boolean inStock,
        Pageable pageable
    ) throws BadRequestException;

    /**
     * Lấy danh sách sách bán chạy nhất
     * @param limit Số lượng sách cần lấy
     * @return Danh sách sách bán chạy
     */
    List<BookResponseDTO> getTopSellingBooks(int limit) throws BadRequestException;

    /**
     * Lấy danh sách sách mới nhất
     * @param limit Số lượng sách cần lấy
     * @return Danh sách sách mới nhất
     */
    List<BookResponseDTO> getLatestBooks(int limit) throws BadRequestException;

    /**
     * Lấy danh sách sách sắp hết hàng
     * @param threshold Ngưỡng số lượng còn lại
     * @return Danh sách sách sắp hết hàng
     */
    List<BookResponseDTO> getLowStockBooks(int threshold) throws BadRequestException;

    /**
     * Tìm kiếm sách theo tên hoặc tác giả
     * @param keyword Từ khóa tìm kiếm
     * @param pageable Thông tin phân trang
     * @return Trang kết quả sách
     */
    Page<BookResponseDTO> searchBooksByKeyword(String keyword, Pageable pageable) throws BadRequestException;
} 