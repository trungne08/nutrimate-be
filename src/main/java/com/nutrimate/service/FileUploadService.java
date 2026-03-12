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
        // Thêm option "resource_type", "auto" cực kỳ quan trọng ở đây
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                com.cloudinary.utils.ObjectUtils.asMap(
                        "resource_type", "auto" // 👈 Ép Cloudinary tự nhận diện Video để tăng giới hạn lên 100MB
                ));
        return uploadResult.get("url").toString();
    }
}