package com.oa.attendance.controller;

import com.oa.attendance.entity.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class FileController {

    @Value("${oa.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip", "rar"
    ));

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过 20MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.error("不支持的文件类型: ." + ext);
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadDir, dateDir);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            return Result.error("创建上传目录失败");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetPath = targetDir.resolve(storedName);
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            return Result.error("文件保存失败");
        }

        String fileUrl = "/uploads/" + dateDir + "/" + storedName;
        Map<String, String> result = new LinkedHashMap<>();
        result.put("url", fileUrl);
        result.put("name", originalName != null ? originalName : storedName);
        result.put("size", String.valueOf(file.getSize()));
        return Result.success("上传成功", result);
    }
}
