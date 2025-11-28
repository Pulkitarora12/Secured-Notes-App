package com.notes.notes.service;

import com.notes.notes.dto.UserDTO;
import com.notes.notes.entity.User;

import java.util.List;

public interface UserService {
    void updateUserRole(Long userId, String roleName);

    List<User> getAllUsers();

    UserDTO getUserById(Long id);

    User findByUsername(String username);
}
