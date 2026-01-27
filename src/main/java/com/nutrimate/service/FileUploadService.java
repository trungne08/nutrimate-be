package com.nutrimate.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        // Upload file lên Cloudinary
        // "public_id" để đặt tên file ngẫu nhiên tránh trùng lặp
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("public_id", UUID.randomUUID().toString()));

        // Trả về đường dẫn ảnh (secure_url là link https)
        return uploadResult.get("secure_url").toString();
    }
}