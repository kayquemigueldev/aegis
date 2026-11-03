
package dev.kayquemiguel.aegis.detection.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record DetectionAlertPageResponse(
        List<DetectionAlertResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public DetectionAlertPageResponse {
        content = List.copyOf(content);
    }

    public static DetectionAlertPageResponse from(
            Page<DetectionAlertResponse> result
    ) {
        return new DetectionAlertPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }
}
