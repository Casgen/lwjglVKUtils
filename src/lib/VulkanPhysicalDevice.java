package lib;

import exc.NoSupportedPhysicalDeviceException;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanPhysicalDevice {

    private QueueFamilyIndices queueFamilyIndices;
    private VkPhysicalDevice vkPhysicalDevice;

    public QueueFamilyIndices getQueueFamilyIndices() {
        return queueFamilyIndices;
    }

    public VkPhysicalDevice getVkPhysicalDevice() {
        return vkPhysicalDevice;
    }

    /**
     * Constructs a VkPhysicalDevice.
     * @param instance - A VulkanInstance object that is needed for discovering the available GPUs (physical devices)
     * @param surface - A VulkanSurface object that is needed for determining each device's capabilities.
     */
    public VulkanPhysicalDevice(VulkanInstance instance, VulkanSurface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);

            VulkanUtils.check(vkEnumeratePhysicalDevices(instance.getInstance(), deviceCount, null));

            PointerBuffer availablePhysicalDevices = stack.mallocPointer(deviceCount.get(0));

            if (deviceCount.get(0) == 0) {
                throw new NoSupportedPhysicalDeviceException();
            }

            VulkanUtils.check(vkEnumeratePhysicalDevices(instance.getInstance(),deviceCount,availablePhysicalDevices));

            VkPhysicalDevice device;

            IntBuffer queueFamilyCount = stack.mallocInt(1);

            for (int i = 0; i < availablePhysicalDevices.capacity(); i++) {
                availablePhysicalDevices.position(i);

                device = new VkPhysicalDevice(availablePhysicalDevices.get(i),instance.getInstance());
                QueueFamilyIndices indices = new QueueFamilyIndices(device, surface);

                if (VulkanUtils.isDeviceSuitable(device, surface, indices)) {
                    vkPhysicalDevice = device;
                    queueFamilyIndices = indices;
                }

            }

            if (vkPhysicalDevice == null)
                throw new NullPointerException("Failed to find suitable GPU!");

        }
    }
}
