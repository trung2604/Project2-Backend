package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Category;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.CategoryRepository;
import com.project2.BookStore.service.BookService;
import com.project2.BookStore.service.ImageProcessingService;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.dto.BookResponseDTO;
import com.project2.BookStore.dto.AddBookRequest;
import com.project2.BookStore.dto.UpdateBookRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.cloudinary.utils.ObjectUtils;
import com.cloudinary.Cloudinary;
import org.hibernate.Hibernate;

@Slf4j
@Service
public class BookServiceImpl implements BookService {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public List<BookResponseDTO> getAllBooks() throws BadRequestException {
        try {
            List<Book> books = bookRepository.findAll();
            return books.stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi lấy danh sách sách: " + e.getMessage());
        }
    }

    @Override
    public Page<BookResponseDTO> getBooksPaged(Pageable pageable) throws BadRequestException {
        try {
            log.info("Bắt đầu lấy danh sách sách phân trang");
            Page<Book> bookPage = bookRepository.findAll(pageable);
            
            // Log thông tin về số lượng sách và trạng thái ảnh
            log.info("Tìm thấy {} sách", bookPage.getTotalElements());
            bookPage.getContent().forEach(book -> {
                if (book.getImage() != null) {
                    log.info("Sách ID: {} có ảnh - Thumbnail: {}, Medium: {}, Original: {}", 
                        book.getId(), 
                        book.getImage().getThumbnail(),
                        book.getImage().getMedium(),
                        book.getImage().getOriginal());
                } else {
                    log.info("Sách ID: {} không có ảnh", book.getId());
                }
            });
            
            // Chuyển đổi sang DTO
            List<BookResponseDTO> bookDTOs = bookPage.getContent().stream()
                .map(book -> {
                    BookResponseDTO dto = new BookResponseDTO(book);
                    if (dto.getImage() != null) {
                        log.info("Đã chuyển đổi ảnh cho sách ID: {} - Thumbnail: {}", 
                            dto.getId(), dto.getImage().getThumbnail());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
            
            log.info("Đã chuyển đổi {} sách sang DTO", bookDTOs.size());
            return new PageImpl<>(bookDTOs, pageable, bookPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sách phân trang: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi lấy danh sách sách: " + e.getMessage());
        }
    }

    @Override
    public BookResponseDTO getBookById(String id) throws BadRequestException {
        try {
            Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách với ID: " + id));
            return new BookResponseDTO(book);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi lấy thông tin sách: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookResponseDTO addBook(AddBookRequest request) {
        log.info("Bắt đầu thêm sách mới: {}", request.getMainText());
        
        // Validate request data
        if (request.getMainText() == null || request.getMainText().trim().isEmpty()) {
            throw new BadRequestException("Tên sách không được để trống");
        }
        if (request.getAuthor() == null || request.getAuthor().trim().isEmpty()) {
            throw new BadRequestException("Tên tác giả không được để trống");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new BadRequestException("Giá sách không hợp lệ");
        }
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new BadRequestException("Số lượng sách không hợp lệ");
        }
        if (request.getCategoryName() == null || request.getCategoryName().trim().isEmpty()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
        
        try {
            // Xử lý category
            Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    log.info("Tạo danh mục mới: {}", request.getCategoryName());
                    Category newCategory = new Category();
                    newCategory.setName(request.getCategoryName());
                    newCategory.setDescription("Danh mục: " + request.getCategoryName());
                    return categoryRepository.save(newCategory);
                });
            log.info("Đã xử lý category: {} (ID: {})", category.getName(), category.getId());
            
            // Tạo đối tượng Book
            Book book = new Book();
            book.setMainText(request.getMainText().trim());
            book.setAuthor(request.getAuthor().trim());
            book.setPrice(request.getPrice());
            book.setSold(request.getSold() != null ? request.getSold() : 0);
            book.setQuantity(request.getQuantity());
            book.setCategory(category);
            book.setCategoryId(category.getId());
            log.info("Đã set category và categoryId cho sách: {}", category.getId());
            
            // Xử lý ảnh
            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                log.info("Bắt đầu xử lý ảnh từ file: {}", request.getImageFile().getOriginalFilename());
                try {
                    Book.Image image = imageProcessingService.processAndUploadBookImage(request.getImageFile());
                    if (image == null || !image.isValid()) {
                        throw new BadRequestException("Không thể xử lý ảnh sách");
                    }
                    book.setImage(image);
                    log.info("Đã xử lý và upload ảnh thành công: {}", image.getOriginal());
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý ảnh: {}", e.getMessage());
                    throw new BadRequestException("Lỗi khi xử lý ảnh: " + e.getMessage());
                }
            } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
                log.info("Sử dụng URL ảnh: {}", request.getImageUrl());
                Book.Image image = Book.Image.fromUrl(request.getImageUrl());
                if (image == null) {
                    throw new BadRequestException("URL ảnh không hợp lệ");
                }
                book.setImage(image);
            } else {
                log.info("Không có ảnh được cung cấp cho sách");
                book.setImage(null);
            }
            
            // Verify book data before saving
            if (book.getCategoryId() == null) {
                throw new BadRequestException("CategoryId không được để trống");
            }
            
            // Lưu sách
            Book savedBook = bookRepository.save(book);
            log.info("Đã lưu sách thành công với ID: {}", savedBook.getId());
            
            // Verify saved data
            if (savedBook.getId() == null) {
                throw new BadRequestException("Không thể lưu sách: ID không được tạo");
            }
            
            if (savedBook.getImage() != null) {
                log.info("Sách có ảnh: {}", savedBook.getImage().getOriginal());
            } else {
                log.info("Sách không có ảnh");
            }
            
            // Tạo response
            BookResponseDTO response = new BookResponseDTO(savedBook);
            log.info("Đã tạo response với thông tin ảnh: {}", 
                response.getImage() != null ? "có ảnh" : "không có ảnh");
            return response;
            
        } catch (BadRequestException e) {
            log.error("Lỗi khi thêm sách: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi thêm sách: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lưu sách: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BookResponseDTO updateBook(UpdateBookRequest request) throws BadRequestException {
        try {
            Book book = bookRepository.findById(request.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy sách với ID: " + request.getId()));

            if (request.getMainText() != null) book.setMainText(request.getMainText());
            if (request.getAuthor() != null) book.setAuthor(request.getAuthor());
            if (request.getPrice() != null) book.setPrice(request.getPrice());
            if (request.getSold() != null) book.setSold(request.getSold());
            if (request.getQuantity() != null) book.setQuantity(request.getQuantity());
            
            if (request.getCategoryId() != null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy thể loại với ID: " + request.getCategoryId()));
                book.setCategoryId(request.getCategoryId());
            }

            // Xử lý ảnh
            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                log.info("Bắt đầu xử lý ảnh mới từ file cho sách ID: {}", book.getId());
                // Xóa ảnh cũ nếu có
                if (book.getImage() != null) {
                    log.info("Xóa ảnh cũ của sách: {}", book.getId());
                    imageProcessingService.deleteOldBookImage(book.getImage());
                }
                
                // Upload ảnh mới
                try {
                    Book.Image image = imageProcessingService.processAndUploadBookImage(request.getImageFile());
                    if (image == null || !image.isValid()) {
                        throw new BadRequestException("Không thể xử lý ảnh sách");
                    }
                    book.setImage(image);
                    log.info("Đã xử lý và upload ảnh mới thành công: {}", image.getOriginal());
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý ảnh: {}", e.getMessage());
                    throw new BadRequestException("Lỗi khi xử lý ảnh: " + e.getMessage());
                }
            } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
                log.info("Cập nhật ảnh sách ID: {} với URL: {}", book.getId(), request.getImageUrl());
                // Xóa ảnh cũ nếu có
                if (book.getImage() != null) {
                    log.info("Xóa ảnh cũ của sách: {}", book.getId());
                    imageProcessingService.deleteOldBookImage(book.getImage());
                }
                
                // Tạo ảnh mới với URL được cung cấp
                Book.Image image = Book.Image.fromUrl(request.getImageUrl());
                if (image == null) {
                    throw new BadRequestException("URL ảnh không hợp lệ");
                }
                book.setImage(image);
                log.info("Đã cập nhật ảnh sách với URL: {}", request.getImageUrl());
            }

            Book updatedBook = bookRepository.save(book);
            log.info("Đã cập nhật sách thành công - ID: {}, Category: {}", 
                    updatedBook.getId(), 
                    updatedBook.getCategoryId());
                    
            return new BookResponseDTO(updatedBook);
        } catch (BadRequestException e) {
            log.error("Lỗi khi cập nhật sách: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật sách: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi cập nhật sách: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteBook(String id) throws BadRequestException {
        try {
            if (!bookRepository.existsById(id)) {
                throw new BadRequestException("Không tìm thấy sách với ID: " + id);
            }
            bookRepository.deleteById(id);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi xóa sách: " + e.getMessage());
        }
    }

    @Override
    public Page<BookResponseDTO> getBooksByCategoryPaged(String categoryId, Pageable pageable) throws BadRequestException {
        try {
            if (!categoryRepository.existsById(categoryId)) {
                throw new BadRequestException("Không tìm thấy thể loại với ID: " + categoryId);
            }

            Page<Book> bookPage = bookRepository.findByCategoryId(categoryId, pageable);
            List<BookResponseDTO> bookDTOs = bookPage.getContent().stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
            return new PageImpl<>(bookDTOs, pageable, bookPage.getTotalElements());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Lỗi khi lấy danh sách sách theo thể loại: " + e.getMessage());
        }
    }
} 