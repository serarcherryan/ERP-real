package com.erpreal.backend.common;

public record ApiResponse<T>(T data, String traceId) {
}
