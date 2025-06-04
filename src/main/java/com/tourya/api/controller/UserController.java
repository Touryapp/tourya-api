package com.tourya.api.controller;


import com.tourya.api.common.PageResponse;
import com.tourya.api.models.request.ChangePasswordRequest;
import com.tourya.api.models.responses.UserResponse;
import com.tourya.api.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {
    private final UserService userService;

    @PatchMapping()
    public ResponseEntity<?>  changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication connectedUser
            ){
        userService.changePassword(changePasswordRequest, connectedUser);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/admin/findAll")
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            @RequestParam(name = "firstName", required = false) String firstName,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(userService.findAll(page, size, firstName, connectedUser));
    }

    @GetMapping("/admin/consultDataById/{userId}")
    public ResponseEntity<UserResponse> consultDataById(@PathVariable Integer userId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(userService.consultDataById(userId, connectedUser));
    }

    @PutMapping("/admin/blockById/{userId}")
    public ResponseEntity<?>  blockById(@PathVariable Integer userId,
            Authentication connectedUser
    ){
        userService.blockById(userId, connectedUser);
        return ResponseEntity.accepted().build();
    }
    @PutMapping("/admin/unBlockById/{userId}")
    public ResponseEntity<?>  unBlockById(@PathVariable Integer userId,
                                    Authentication connectedUser
    ){
        userService.unBlockById(userId, connectedUser);
        return ResponseEntity.accepted().build();
    }
}
