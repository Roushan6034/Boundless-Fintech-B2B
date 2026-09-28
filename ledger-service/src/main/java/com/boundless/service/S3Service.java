package com.boundless.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final Cloudinary cloudinary;

    public String uploadFile(UUID transactionId, MultipartFile file) throws IOException {
        String fileName = transactionId.toString() + "_" + file.getOriginalFilename();

        log.info("Uploading receipt to Cloudinary: {}", fileName);
        
        try {
            // Upload to Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", "boundless-receipts/" + fileName,
                "resource_type", "auto" // Automatically handles images, PDFs, SVGs, etc.
            ));
            
            // Get the secure HTTPS URL from Cloudinary
            String cloudinaryUrl = uploadResult.get("secure_url").toString();
            
            log.info("Successfully uploaded receipt! URL: {}", cloudinaryUrl);
            return cloudinaryUrl;
            
        } catch (Exception e) {
            log.error("Cloudinary upload failed! Exception: {}", e.getMessage());
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }
}
