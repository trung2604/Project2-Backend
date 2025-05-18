package com.project2.BookStore.dto;

import com.project2.BookStore.model.User;
import com.project2.BookStore.model.User.Avatar;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private Avatar avatar;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    
    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.avatar = user.getAvatar();
        this.isActive = user.isActive();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
