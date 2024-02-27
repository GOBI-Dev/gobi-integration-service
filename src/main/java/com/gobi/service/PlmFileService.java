package com.gobi.service;


import com.amazonaws.HttpMethod;
import com.gobi.exceptions.FileDownloadException;
import com.gobi.exceptions.FileUploadException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface PlmFileService {
    String uploadFile(MultipartFile multipartFile) throws FileUploadException, IOException;

    Object downloadFile(String fileName) throws FileDownloadException, IOException;

    boolean delete(String fileName);

    String generateUrl(String filename, HttpMethod http) throws FileDownloadException, IOException;
}