package com.example.escbackend.user.controller;

import com.example.escbackend.user.dto.*;
import com.example.escbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", allowedHeaders = "*") // <-- Add this line here
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserDetailsResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @GetMapping("/{id}/rider-profile")
    public RiderProfileResponse getRiderProfile(@PathVariable UUID id) {
        return userService.getRiderProfileByUserId(id);
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
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.listMarketplaceUsers(actorUserId, phone, status, page, size);
    }

    @GetMapping("/buyers")
    public Page<UserDetailsResponse> listBuyers(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.listBuyers(actorUserId, phone, status, page, size);
    }

    @GetMapping("/sellers")
    public Page<UserDetailsResponse> listSellers(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.listSellers(actorUserId, phone, status, page, size);
    }


    @GetMapping("/riders")
    public Page<UserDetailsResponse> listRiders(
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return userService.listRiders(actorUserId, phone, status, page , size);
    }

    @GetMapping("/employees")
    public Page<UserDetailsResponse> listEmployees(
        @RequestHeader("X-Actor-User-Id") UUID actorUserId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return userService.listEmployees(actorUserId, phone, status, page, size);
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

    @PatchMapping("admin/{id}/blacklist")
    public BlacklistUpdateResponse updateBlacklistStatus(
            @PathVariable UUID id,
            @RequestHeader("X-Actor-User-Id") UUID actorUserId,
            @Valid @RequestBody BlacklistUpdateRequest request
    ) {
        return userService.updateBlacklistStatus(id, actorUserId, request);
    }

    @PatchMapping("/{id}/update_profile")
    public UpdateUserResponse updateUserProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ){
        return userService.updateUserDetails(id, request);
    }
}
