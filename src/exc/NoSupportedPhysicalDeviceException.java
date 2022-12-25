package exc;

public class NoSupportedPhysicalDeviceException extends RuntimeException {

    public NoSupportedPhysicalDeviceException(String message) {
        super(message);
    }

    public NoSupportedPhysicalDeviceException() {
        super("None of the physical devices (GPUs) support Vulkan!");
    }
}
