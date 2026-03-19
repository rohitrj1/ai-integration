package com.demo.ai.service;

import com.demo.ai.model.RefundResult;
import org.springframework.web.multipart.MultipartFile;

public interface RefundService {
    RefundResult processRefund(String mobile, String reason, MultipartFile screenshot);
}