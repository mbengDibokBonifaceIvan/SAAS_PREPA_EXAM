package com.ivan.backend.application.usecase;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;

public interface OnboardingUseCase {
    OnboardingResponse execute(OnboardingRequest request);
}