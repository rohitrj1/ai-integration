package com.demo.ai.service;

import com.demo.ai.model.FranchiseRequest;

public interface FranchiseService {
    String submitEnquiry(FranchiseRequest request);
}