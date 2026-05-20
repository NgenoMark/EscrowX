package com.example.escbackend.user.controller;

import com.example.escbackend.user.dto.UserDetailsResponse;
import com.example.escbackend.user.dto.UserRoleStatusUpdateResponse;
import com.example.escbackend.user.dto.UserRoleUpdateRequest;
import com.example.escbackend.user.dto.UserStatusUpdateRequest;
import com.example.escbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserDetailsResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping("/by-phone/{phone}")
    public UserDetailsResponse getByPhone(@PathVariable String phone) {
        return userService.getByPhone(phone);
    }

    @GetMapping("by-email/{email}")
    public UserDetailsResponse getByEmail(@PathVariable String email){
        return userService.getByEmail(email);
    }

    @GetMapping
    public Page<UserDetailsResponse> list(
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.list(phone, role, status, page, size);
    }

    @PatchMapping("/{id}/role")
    public UserRoleStatusUpdateResponse updateRole(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return userService.updateRole(id, actorUserId, request);
    }

    @PatchMapping("/{id}/status")
    public UserRoleStatusUpdateResponse updateStatus(
        @PathVariable UUID id,
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return userService.updateStatus(id, actorUserId, request);
    }
}
