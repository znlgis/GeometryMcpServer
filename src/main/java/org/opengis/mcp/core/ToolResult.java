package org.opengis.mcp.core;

/**
 * Result wrapper for MCP tool operations.
 */
public class ToolResult<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;

    private ToolResult(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static <T> ToolResult<T> success(T data) {
        return new ToolResult<>(true, data, null, null);
    }

    public static <T> ToolResult<T> success(T data, String message) {
        return new ToolResult<>(true, data, message, null);
    }

    public static <T> ToolResult<T> error(String message) {
        return new ToolResult<>(false, null, message, "ERROR");
    }

    public static <T> ToolResult<T> error(String message, String errorCode) {
        return new ToolResult<>(false, null, message, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (success) {
            return message != null ? message : (data != null ? data.toString() : "Success");
        } else {
            return "Error: " + message + (errorCode != null ? " (" + errorCode + ")" : "");
        }
    }
}
