package com.project2.BookStore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project2.BookStore.dto.AvatarDTO;
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
import com.project2.BookStore.model.Book;

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
    
    public AvatarDTO processAndUploadAvatar(MultipartFile file, User user) throws IOException {
        // Validate file
        validateFile(file);
        
        // Delete old avatar if exists
        if (user.getAvatar() != null) {
            deleteOldAvatar(user.getAvatar());
        }
        
        File tempFile = null;
        try {
            // Create temporary file
            tempFile = File.createTempFile("avatar_", getFileExtension(file.getOriginalFilename()));
            file.transferTo(tempFile);
            
            // Get original image format
            String originalFormat = getImageFormat(file);
            String outputFormat = "jpg"; // Default to jpg for better compression
            if ("png".equalsIgnoreCase(originalFormat)) {
                outputFormat = "png"; // Keep PNG format for PNG images
            }
            
            // Use userId as base filename to ensure consistent public_id
            String baseFilename = "user_" + user.getId();
            
            // Read the original image
            BufferedImage originalImage = ImageIO.read(tempFile);
            if (originalImage == null) {
                throw new BadRequestException("Không thể đọc file ảnh. Vui lòng kiểm tra lại định dạng file.");
            }
            
            // Process thumbnail (150x150)
            ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(150, 150)
                    .outputFormat(outputFormat)
                    .toOutputStream(thumbnailStream);
            String thumbnailUrl = uploadToCloudinary(thumbnailStream.toByteArray(), baseFilename + "_thumb", outputFormat);
            
            // Process medium size (300x300)
            ByteArrayOutputStream mediumStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(300, 300)
                    .outputFormat(outputFormat)
                    .toOutputStream(mediumStream);
            String mediumUrl = uploadToCloudinary(mediumStream.toByteArray(), baseFilename + "_medium", outputFormat);
            
            // Process original size (max 800px width/height)
            ByteArrayOutputStream originalStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(800, 800)
                    .outputFormat(outputFormat)
                    .toOutputStream(originalStream);
            String originalUrl = uploadToCloudinary(originalStream.toByteArray(), baseFilename + "_original", outputFormat);
            
            return AvatarDTO.builder()
                    .thumbnail(thumbnailUrl)
                    .medium(mediumUrl)
                    .original(originalUrl)
                    .format(outputFormat)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Không thể xử lý ảnh: " + e.getMessage());
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    System.err.println("Error deleting temporary file: " + e.getMessage());
                }
            }
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return ".tmp";
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex == -1 ? ".tmp" : filename.substring(lastDotIndex);
    }
    
    private String getImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                return "jpg";
            } else if (contentType.equals("image/png")) {
                return "png";
            } else if (contentType.equals("image/gif")) {
                return "gif";
            }
        }
        // Try to get format from filename
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
                return extension;
            }
        }
        return "jpg"; // Default to jpg
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
    
    private String uploadToCloudinary(byte[] imageData, String publicId, String format) throws IOException {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                "public_id", "avatars/" + publicId,
                "format", format,
                "resource_type", "image",
                "overwrite", true
            );
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(imageData, uploadParams);
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            throw new BadRequestException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
    
    private void deleteOldAvatar(User.Avatar oldAvatar) {
        try {
            // Extract public IDs from URLs
            String thumbnailPublicId = extractPublicId(oldAvatar.getThumbnail());
            String mediumPublicId = extractPublicId(oldAvatar.getMedium());
            String originalPublicId = extractPublicId(oldAvatar.getOriginal());
            
            // Delete all versions of the old avatar
            if (thumbnailPublicId != null) {
                cloudinary.uploader().destroy(thumbnailPublicId, ObjectUtils.asMap("invalidate", true));
            }
            if (mediumPublicId != null) {
                cloudinary.uploader().destroy(mediumPublicId, ObjectUtils.asMap("invalidate", true));
            }
            if (originalPublicId != null) {
                cloudinary.uploader().destroy(originalPublicId, ObjectUtils.asMap("invalidate", true));
            }
        } catch (IOException e) {
            // Log the error but don't throw it to prevent upload failure
            System.err.println("Error deleting old avatar: " + e.getMessage());
        }
    }
    
    private String extractPublicId(String url) {
        if (url == null) return null;
        // Extract public ID from Cloudinary URL
        // Example URL: https://res.cloudinary.com/cloud-name/image/upload/v1234567890/avatars/user_123_thumb.jpg
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            // Get the last part without extension
            String lastPart = parts[parts.length - 1];
            return lastPart.substring(0, lastPart.lastIndexOf("."));
        }
        return null;
    }

    public String uploadImageToCloudinary(org.springframework.web.multipart.MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }
    }

    public Book.Image processAndUploadBookImage(MultipartFile file) {
        validateFile(file);
        File tempFile = null;
        try {
            tempFile = File.createTempFile("book_", getFileExtension(file.getOriginalFilename()));
            file.transferTo(tempFile);
            String outputFormat = getImageFormat(file);
            String baseFilename = "book_" + System.currentTimeMillis();
            BufferedImage originalImage = ImageIO.read(tempFile);
            if (originalImage == null) {
                throw new BadRequestException("Không thể đọc file ảnh. Vui lòng kiểm tra lại định dạng file.");
            }
            // Thumbnail (150x150)
            ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(150, 150)
                    .outputFormat(outputFormat)
                    .toOutputStream(thumbnailStream);
            String thumbnailUrl = uploadToCloudinary(thumbnailStream.toByteArray(), baseFilename + "_thumb", outputFormat);
            // Medium (300x300)
            ByteArrayOutputStream mediumStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(300, 300)
                    .outputFormat(outputFormat)
                    .toOutputStream(mediumStream);
            String mediumUrl = uploadToCloudinary(mediumStream.toByteArray(), baseFilename + "_medium", outputFormat);
            // Original (800x800)
            ByteArrayOutputStream originalStream = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(800, 800)
                    .outputFormat(outputFormat)
                    .toOutputStream(originalStream);
            String originalUrl = uploadToCloudinary(originalStream.toByteArray(), baseFilename + "_original", outputFormat);
            return new Book.Image(thumbnailUrl, mediumUrl, originalUrl, outputFormat, file.getSize());
        } catch (IOException e) {
            throw new BadRequestException("Không thể xử lý ảnh: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    System.err.println("Error deleting temporary file: " + e.getMessage());
                }
            }
        }
    }
} 