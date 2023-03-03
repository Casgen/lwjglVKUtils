package exc;

public class MemoryAllocationException extends RuntimeException {

    MemoryAllocationException() {
        super("Failed to allocate memory!");
    }

    public MemoryAllocationException(String message) {
        super(message);
    }
}
