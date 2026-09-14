package uz.mirix.crmix.billing.application;

public class PaymeRpcException extends RuntimeException {
    private final int code;
    private final String data;

    public PaymeRpcException(int code, String message) {
        this(code, message, null);
    }

    public PaymeRpcException(int code, String message, String data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int code() { return code; }
    public String data() { return data; }
}
