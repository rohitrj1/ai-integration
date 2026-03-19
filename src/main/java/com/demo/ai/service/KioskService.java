package com.demo.ai.service;

import com.demo.ai.model.KioskReportRequest;

public interface KioskService {
    String submitReport(KioskReportRequest request);
}