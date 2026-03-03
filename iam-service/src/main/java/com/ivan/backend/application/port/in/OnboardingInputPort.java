package com.ivan.backend.application.port.in;

import com.ivan.backend.application.dto.OnboardingRequest;
import com.ivan.backend.application.dto.OnboardingResponse;

public interface OnboardingInputPort {
    OnboardingResponse execute(OnboardingRequest request);
}
