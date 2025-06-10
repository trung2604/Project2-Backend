package com.project2.BookStore.service.impl;

import com.project2.BookStore.model.Book;
import com.project2.BookStore.model.Category;
import com.project2.BookStore.repository.BookRepository;
import com.project2.BookStore.repository.CategoryRepository;
import com.project2.BookStore.service.BookService;
import com.project2.BookStore.service.ImageProcessingService;
import com.project2.BookStore.service.CategoryService;
import com.project2.BookStore.exception.BadRequestException;
import com.project2.BookStore.dto.BookResponseDTO;
import com.project2.BookStore.dto.AddBookRequest;
import com.project2.BookStore.dto.UpdateBookRequest;
import com.project2.BookStore.dto.CategoryDTO;
import com.project2.BookStore.dto.AddCategoryRequest;
import com.project2.BookStore.dto.SearchBookRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.cloudinary.utils.ObjectUtils;
import com.cloudinary.Cloudinary;
import org.hibernate.Hibernate;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

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

    @Autowired
    private CategoryService categoryService;

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

        // Xử lý danh mục
        Category category;
        try {
            // Kiểm tra xem categoryName có phải là UUID không
            if (request.getCategoryName().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                throw new BadRequestException("Tên danh mục không được là UUID");
            }

            category = categoryRepository.findByName(request.getCategoryName())
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(request.getCategoryName());
                    newCategory.setDescription("Danh mục " + request.getCategoryName());
                    return categoryRepository.save(newCategory);
                });
        } catch (Exception e) {
            if (e instanceof BadRequestException) {
                throw e;
            }
            throw new BadRequestException("Lỗi khi xử lý danh mục: " + e.getMessage());
        }

        // Tạo đối tượng Book mới
        Book book = new Book();
        book.setMainText(request.getMainText());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        book.setSold(request.getSold() != null ? request.getSold() : 0);
        book.setQuantity(request.getQuantity());
        book.setCategoryId(category.getId());
        book.setCategory(category);

        // Xử lý ảnh nếu có
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                log.info("Bắt đầu xử lý ảnh sách từ file: {}", request.getImageFile().getOriginalFilename());
                Book.Image processedImage = imageProcessingService.processAndUploadBookImage(request.getImageFile());
                if (processedImage != null && processedImage.isValid()) {
                    book.setImage(processedImage);
                    log.info("Đã xử lý và upload ảnh thành công - Thumbnail: {}, Medium: {}, Original: {}", 
                        processedImage.getThumbnail(), 
                        processedImage.getMedium(), 
                        processedImage.getOriginal());
                } else {
                    log.warn("Không thể xử lý ảnh sách - processedImage không hợp lệ");
                }
            } catch (Exception e) {
                log.error("Lỗi khi xử lý ảnh sách: {}", e.getMessage(), e);
                throw new BadRequestException("Lỗi khi xử lý ảnh sách: " + e.getMessage());
            }
        } else {
            log.info("Không có file ảnh được cung cấp cho sách");
        }

        // Validate trước khi lưu
        if (book.getCategoryId() == null) {
            throw new BadRequestException("CategoryId không được để trống");
        }

        try {
            // Lưu sách và kiểm tra kết quả
            book = bookRepository.save(book);
            log.info("Đã lưu sách thành công. BookId: {}, Image: {}", 
                book.getId(), 
                book.getImage() != null ? "có ảnh" : "không có ảnh");
            
            // Kiểm tra lại thông tin ảnh sau khi lưu
            if (book.getImage() != null) {
                log.info("Thông tin ảnh sau khi lưu - Thumbnail: {}, Medium: {}, Original: {}", 
                    book.getImage().getThumbnail(),
                    book.getImage().getMedium(),
                    book.getImage().getOriginal());
            }
            
            return new BookResponseDTO(book);
        } catch (Exception e) {
            log.error("Lỗi khi lưu sách: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi lưu sách: " + e.getMessage());
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

            // Bổ sung: Ưu tiên categoryName nếu có, nếu không thì dùng categoryId
            if (request.getCategoryName() != null && !request.getCategoryName().trim().isEmpty()) {
                // Không cho phép truyền UUID vào categoryName
                if (request.getCategoryName().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                    throw new BadRequestException("Tên danh mục không được là UUID");
                }
                Category category = categoryRepository.findByName(request.getCategoryName())
                    .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setName(request.getCategoryName());
                        newCategory.setDescription("Danh mục " + request.getCategoryName());
                        return categoryRepository.save(newCategory);
                    });
                book.setCategoryId(category.getId());
                book.setCategory(category);
            } else if (request.getCategoryId() != null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy thể loại với ID: " + request.getCategoryId()));
                book.setCategoryId(request.getCategoryId());
                book.setCategory(category);
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

    @Override
    public Page<BookResponseDTO> searchBooks(
        String keyword,
        String categoryId,
        Long minPrice,
        Long maxPrice,
        Boolean inStock,
        Pageable pageable
    ) throws BadRequestException {
        try {
            log.info("Bắt đầu tìm kiếm sách với các tiêu chí - Keyword: {}, CategoryId: {}, MinPrice: {}, MaxPrice: {}, InStock: {}",
                keyword, categoryId, minPrice, maxPrice, inStock);

            // Tạo specification trực tiếp trong service
            Specification<Book> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Tìm kiếm theo keyword (tên sách hoặc tác giả)
                if (keyword != null && !keyword.trim().isEmpty()) {
                    String likePattern = "%" + keyword.toLowerCase() + "%";
                    predicates.add(cb.or(
                        cb.like(cb.lower(root.get("mainText")), likePattern),
                        cb.like(cb.lower(root.get("author")), likePattern)
                    ));
                }
                
                // Lọc theo category
                if (categoryId != null && !categoryId.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("categoryId"), categoryId));
                }
                
                // Lọc theo giá
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
                }
                
                // Lọc theo trạng thái tồn kho
                if (inStock != null) {
                    if (inStock) {
                        predicates.add(cb.greaterThan(root.get("quantity"), 0));
                    } else {
                        predicates.add(cb.equal(root.get("quantity"), 0));
                    }
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Book> bookPage = bookRepository.findAll(spec, pageable);

            List<BookResponseDTO> bookDTOs = bookPage.getContent().stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());

            log.info("Tìm thấy {} sách phù hợp với tiêu chí tìm kiếm", bookPage.getTotalElements());
            return new PageImpl<>(bookDTOs, pageable, bookPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm sách: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi tìm kiếm sách: " + e.getMessage());
        }
    }

    @Override
    public List<BookResponseDTO> getTopSellingBooks(int limit) throws BadRequestException {
        try {
            log.info("Lấy danh sách {} sách bán chạy nhất", limit);
            Pageable pageable = PageRequest.of(0, limit);
            List<Book> books = bookRepository.findAllByOrderBySoldDesc(pageable);
            return books.stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sách bán chạy: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi lấy danh sách sách bán chạy: " + e.getMessage());
        }
    }

    @Override
    public List<BookResponseDTO> getLatestBooks(int limit) throws BadRequestException {
        try {
            log.info("Lấy danh sách {} sách mới nhất", limit);
            Pageable pageable = PageRequest.of(0, limit);
            List<Book> books = bookRepository.findAllByOrderByCreatedAtDesc(pageable);
            return books.stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sách mới nhất: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi lấy danh sách sách mới nhất: " + e.getMessage());
        }
    }

    @Override
    public List<BookResponseDTO> getLowStockBooks(int threshold) throws BadRequestException {
        try {
            log.info("Lấy danh sách sách sắp hết hàng (ngưỡng: {})", threshold);
            Pageable pageable = PageRequest.of(0, 10); // Mặc định lấy 10 sách
            List<Book> books = bookRepository.findByQuantityLessThanEqualOrderByQuantityAsc(threshold, pageable);
            return books.stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sách sắp hết hàng: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi lấy danh sách sách sắp hết hàng: " + e.getMessage());
        }
    }

    @Override
    public Page<BookResponseDTO> searchBooksByKeyword(String keyword, Pageable pageable) throws BadRequestException {
        try {
            log.info("Tìm kiếm sách theo từ khóa: {}", keyword);
            Page<Book> bookPage = bookRepository.findByMainTextContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                keyword, keyword, pageable
            );
            List<BookResponseDTO> bookDTOs = bookPage.getContent().stream()
                .map(BookResponseDTO::new)
                .collect(Collectors.toList());
            return new PageImpl<>(bookDTOs, pageable, bookPage.getTotalElements());
        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm sách theo từ khóa: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi tìm kiếm sách: " + e.getMessage());
        }
    }
} 