package com.boundless.controller;

import com.boundless.entity.User;
import com.boundless.repository.UserRepository;
import com.boundless.security.JwtUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        
        // 1. Look up the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 2. Cryptographically verify the password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // 3. Security Check: First-time employee login
        if (user.isRequiresPasswordChange()) {
            return ResponseEntity.status(403).body("You must change your temporary password before accessing the system.");
        }

        // 4. Generate the signed JWT
        String token = jwtUtils.generateToken(user.getEmail(), user.getId().toString(), user.getRole());
        
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        // Automatically issue token after password change so employee doesn't have to re-login!
        String token = jwtUtils.generateToken(user.getEmail(), user.getId().toString(), user.getRole());
        return ResponseEntity.ok(new PasswordChangeResponse("Password updated successfully!", token));
    }

    // INTERNAL: Used by other microservices to validate JWTs and get User info
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        try {
            String token = authHeader.substring(7);
            String email = jwtUtils.extractAllClaims(token).getSubject();
            User user = userRepository.findByEmail(email).orElseThrow();
            
            return ResponseEntity.ok(java.util.Map.of(
                "email", user.getEmail(),
                "role", user.getRole(),
                "companyName", user.getCompanyName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}

// DTOs for the JSON request/response
@Data
class LoginRequest {
    private String email;
    private String password;
}

@Data
class JwtResponse {
    private String token;
    public JwtResponse(String token) { this.token = token; }
}

@Data
class ChangePasswordRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}

@Data
class PasswordChangeResponse {
    private String message;
    private String token;
    public PasswordChangeResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }
}
