package com.project2.BookStore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project2.BookStore.dto.AvatarDTO;
import com.project2.BookStore.dto.UserResponseDTO;
import com.project2.BookStore.model.User;
import com.project2.BookStore.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.time.LocalDateTime;
import com.project2.BookStore.model.Book;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageProcessingService {
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif"
    );
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    @Autowired
    private Cloudinary cloudinary;
    
    private static final List<String> ALLOWED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final int THUMBNAIL_SIZE = 150;
    private static final int MEDIUM_SIZE = 300;
    
    public AvatarDTO processAndUploadAvatar(MultipartFile file, UserResponseDTO user) throws IOException {
        log.info("Processing avatar for user: {}", user.getId());
        
        // Validate file
        validateFile(file);
        
        // Delete old avatar if exists
        if (user.getAvatar() != null) {
            deleteOldAvatar(user.getAvatar());
        }
        
        File tempFile = null;
        try {
            // Create temporary file
            tempFile = File.createTempFile("avatar-", "." + getFormat(file.getOriginalFilename()));
            file.transferTo(tempFile);
            
            String basePublicId = "avatars/user_" + user.getId();
            String format = getFormat(file.getOriginalFilename());
            
            // Upload original image
            Map<String, String> uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                "folder", "avatars",
                "public_id", basePublicId + "_original",
                "format", format
            ));

            String originalUrl = uploadResult.get("secure_url");

            // Generate and upload thumbnail
            File thumbnailFile = createThumbnail(tempFile, THUMBNAIL_SIZE, format);
            Map<String, String> thumbnailResult = cloudinary.uploader().upload(thumbnailFile, ObjectUtils.asMap(
                "folder", "avatars",
                "public_id", basePublicId + "_thumbnail",
                "format", format
            ));
            String thumbnailUrl = thumbnailResult.get("secure_url");

            // Generate and upload medium size
            File mediumFile = createThumbnail(tempFile, MEDIUM_SIZE, format);
            Map<String, String> mediumResult = cloudinary.uploader().upload(mediumFile, ObjectUtils.asMap(
                "folder", "avatars",
                "public_id", basePublicId + "_medium",
                "format", format
            ));
            String mediumUrl = mediumResult.get("secure_url");

            // Clean up temp files
            thumbnailFile.delete();
            mediumFile.delete();

            return AvatarDTO.builder()
                    .thumbnail(thumbnailUrl)
                    .medium(mediumUrl)
                    .original(originalUrl)
                    .publicId(basePublicId)
                    .format(format)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (IOException e) {
            log.error("Lỗi khi xử lý ảnh avatar: {}", e.getMessage());
            throw new BadRequestException("Không thể xử lý ảnh: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.error("Lỗi khi xóa file tạm: {}", e.getMessage());
                }
            }
        }
    }
    
    private String getFormat(String filename) {
        if (filename == null) return "jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "jpg";
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn file ảnh để upload");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Kích thước file không được vượt quá 5MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Định dạng file không được hỗ trợ. Chỉ chấp nhận: JPG, JPEG, PNG, GIF");
        }
        
        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
            throw new BadRequestException("Định dạng file không được hỗ trợ. Chỉ chấp nhận: JPG, JPEG, PNG, GIF");
        }
    }
    
    private File createThumbnail(File sourceFile, int size, String format) throws IOException {
        BufferedImage originalImage = ImageIO.read(sourceFile);
        BufferedImage thumbnail = Thumbnails.of(originalImage)
            .size(size, size)
            .asBufferedImage();

        File thumbnailFile = File.createTempFile("thumbnail-", "." + format);
        ImageIO.write(thumbnail, format, thumbnailFile);
        return thumbnailFile;
    }
    
    private String uploadToCloudinary(byte[] imageData, String publicId, String format) throws IOException {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                "public_id", publicId,  // publicId already includes the folder
                "format", format,
                "resource_type", "image",
                "overwrite", true
            );
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(imageData, uploadParams);
            if (uploadResult == null || !uploadResult.containsKey("secure_url")) {
                throw new BadRequestException("Upload không thành công: không nhận được URL");
            }
            
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isEmpty()) {
                throw new BadRequestException("Upload không thành công: URL trống");
            }
            
            log.info("Upload ảnh thành công - public_id: {}, url: {}", publicId, secureUrl);
            return secureUrl;
        } catch (Exception e) {
            log.error("Lỗi khi upload ảnh lên Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
    
    public void deleteOldAvatar(User.Avatar oldAvatar) {
        if (oldAvatar == null || oldAvatar.getPublicId() == null) {
            return;
        }

        try {
            String basePublicId = oldAvatar.getPublicId();
            // Xóa cả 3 phiên bản của avatar
            String[] versions = {"_original", "_thumbnail", "_medium"};
            for (String version : versions) {
                try {
                    cloudinary.uploader().destroy(basePublicId + version, ObjectUtils.asMap("invalidate", true));
                    log.info("Đã xóa avatar {} thành công", basePublicId + version);
                } catch (IOException e) {
                    log.error("Lỗi khi xóa avatar {}: {}", basePublicId + version, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa avatar cũ: {}", e.getMessage());
        }
    }
    
    private String extractPublicId(String url) {
        if (url == null) return null;
        try {
            // Extract public ID from Cloudinary URL
            // Example URL: https://res.cloudinary.com/cloud-name/image/upload/v1234567890/avatars/user_123_thumb.jpg
            String[] parts = url.split("/");
            if (parts.length >= 2) {
                // Get the last part without extension
                String lastPart = parts[parts.length - 1];
                return lastPart.substring(0, lastPart.lastIndexOf("."));
            }
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất public ID từ URL: {}", e.getMessage());
        }
        return null;
    }

    public String uploadImageToCloudinary(MultipartFile file) {
        try {
            validateFile(file);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            log.error("Lỗi khi upload ảnh lên Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Lỗi upload ảnh: " + e.getMessage());
        }
    }

    public void deleteOldBookImage(Book.Image oldImage) {
        if (oldImage == null || oldImage.getPublicId() == null) {
            return;
        }

        try {
            String basePublicId = oldImage.getPublicId();
            // Xóa cả 3 phiên bản của ảnh sách
            String[] versions = {"_original", "_thumb", "_medium"};
            for (String version : versions) {
                try {
                    cloudinary.uploader().destroy(basePublicId + version, ObjectUtils.asMap("invalidate", true));
                    log.info("Đã xóa ảnh sách {} thành công", basePublicId + version);
                } catch (IOException e) {
                    log.error("Lỗi khi xóa ảnh sách {}: {}", basePublicId + version, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa ảnh sách cũ: {}", e.getMessage());
        }
    }

    public Book.Image processAndUploadBookImage(MultipartFile file) {
        validateFile(file);
        File tempFile = null;
        try {
            tempFile = File.createTempFile("book_", "." + getFormat(file.getOriginalFilename()));
            file.transferTo(tempFile);
            String outputFormat = getFormat(file.getOriginalFilename());
            
            // Tạo publicId duy nhất cho mỗi ảnh sách
            String timestamp = String.valueOf(System.currentTimeMillis());
            String random = java.util.UUID.randomUUID().toString().substring(0, 8);
            String basePublicId = String.format("books/book_%s_%s", timestamp, random);
            
            log.info("Bắt đầu xử lý ảnh sách với publicId: {}", basePublicId);
            
            // Validate image can be read
            BufferedImage originalImage = ImageIO.read(tempFile);
            if (originalImage == null) {
                throw new BadRequestException("Không thể đọc file ảnh. Vui lòng kiểm tra lại định dạng file.");
            }
            
            // Validate image dimensions
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            if (width < 150 || height < 150) {
                throw new BadRequestException("Kích thước ảnh quá nhỏ. Yêu cầu tối thiểu 150x150 pixels.");
            }
            
            log.info("Bắt đầu xử lý ảnh sách - Kích thước gốc: {}x{}, Format: {}, PublicId: {}", 
                    width, height, outputFormat, basePublicId);
            
            // Process and upload thumbnail
            ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(150, 150)
                    .outputFormat(outputFormat)
                    .toOutputStream(thumbnailStream);
            String thumbnailUrl = uploadToCloudinary(thumbnailStream.toByteArray(), basePublicId + "_thumb", outputFormat);
            log.info("Đã upload thumbnail: {}", thumbnailUrl);
            
            // Process and upload medium size
            ByteArrayOutputStream mediumStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(300, 300)
                    .outputFormat(outputFormat)
                    .toOutputStream(mediumStream);
            String mediumUrl = uploadToCloudinary(mediumStream.toByteArray(), basePublicId + "_medium", outputFormat);
            log.info("Đã upload medium: {}", mediumUrl);
            
            // Process and upload original size (max 800x800)
            ByteArrayOutputStream originalStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(Math.min(800, width), Math.min(800, height))
                    .outputFormat(outputFormat)
                    .toOutputStream(originalStream);
            String originalUrl = uploadToCloudinary(originalStream.toByteArray(), basePublicId + "_original", outputFormat);
            log.info("Đã upload original: {}", originalUrl);

            // Create and validate image object
            Book.Image image = new Book.Image();
            image.setThumbnail(thumbnailUrl);
            image.setMedium(mediumUrl);
            image.setOriginal(originalUrl);
            image.setFormat(outputFormat);
            image.setSize(file.getSize());
            image.setPublicId(basePublicId);
            
            // Validate all URLs are present
            if (image.getThumbnail() == null || image.getMedium() == null || image.getOriginal() == null) {
                throw new BadRequestException("Không thể tạo đối tượng ảnh: thiếu URL");
            }
            
            log.info("Xử lý ảnh sách thành công - publicId: {}, Thumbnail: {}, Medium: {}, Original: {}", 
                    basePublicId, thumbnailUrl, mediumUrl, originalUrl);
            return image;
        } catch (IOException e) {
            log.error("Lỗi khi xử lý ảnh sách: {}", e.getMessage());
            throw new BadRequestException("Không thể xử lý ảnh: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    log.info("Đã xóa file tạm: {}", tempFile.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Lỗi khi xóa file tạm: {}", e.getMessage());
                }
            }
        }
    }
} 