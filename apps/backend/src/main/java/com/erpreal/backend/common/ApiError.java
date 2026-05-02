package com.erpreal.backend.common;

public record ApiError(String code, String message, String traceId) {
}
