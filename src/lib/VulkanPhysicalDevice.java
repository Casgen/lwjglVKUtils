package lib;

import exc.NoSupportedPhysicalDeviceException;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanPhysicalDevice {

    private VkPhysicalDevice vkPhysicalDevice;
    private QueueFamilyIndices queueFamilyIndices;
    private SwapChainSupportDetails swapChainSupportDetails;

    final static String[] deviceExtensions = new String[]{
            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
    };

    public QueueFamilyIndices getQueueFamilyIndices() {
        return queueFamilyIndices;
    }

    public VkPhysicalDevice getVkPhysicalDevice() {
        return vkPhysicalDevice;
    }

    public SwapChainSupportDetails getSwapChainSupportDetails() {
        return swapChainSupportDetails;
    }

    /**
     * Constructs a VkPhysicalDevice.
     * @param instance - A VulkanInstance object that is needed for discovering the available GPUs (physical devices)
     * @param surface - A VulkanSurface object that is needed for determining each device's capabilities.
     */
    public VulkanPhysicalDevice(VulkanInstance instance, VulkanSurface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);

            VulkanUtils.check(vkEnumeratePhysicalDevices(instance.getVkInstance(), deviceCount, null));

            PointerBuffer availablePhysicalDevices = stack.mallocPointer(deviceCount.get(0));

            if (deviceCount.get(0) == 0) {
                throw new NoSupportedPhysicalDeviceException();
            }

            VulkanUtils.check(vkEnumeratePhysicalDevices(instance.getVkInstance(),deviceCount,availablePhysicalDevices));

            VkPhysicalDevice device;

            IntBuffer queueFamilyCount = stack.mallocInt(1);

            for (int i = 0; i < availablePhysicalDevices.capacity(); i++) {
                availablePhysicalDevices.position(i);

                device = new VkPhysicalDevice(availablePhysicalDevices.get(i),instance.getVkInstance());
                QueueFamilyIndices indices = new QueueFamilyIndices(device, surface.getSurfacePtr());

                if (isDeviceSuitable(device, surface, indices)) {
                    vkPhysicalDevice = device;
                    //TODO: Maybe optimize this further to not call the constructor twice.
                    swapChainSupportDetails = new SwapChainSupportDetails(device,surface);
                    queueFamilyIndices = indices;
                }

            }

            if (vkPhysicalDevice == null)
                throw new NullPointerException("Failed to find suitable GPU!");

        }
    }


    private boolean isDeviceSuitable(VkPhysicalDevice physicalDevice, VulkanSurface surface, QueueFamilyIndices indices) {
        try (MemoryStack stack = stackPush()) {
            boolean extensionsSupported = checkDeviceExtensionSupport(physicalDevice);

            boolean swapChainAdequate = false;
            if (extensionsSupported) {
                SwapChainSupportDetails swapChainSupport = new SwapChainSupportDetails(physicalDevice, surface);
                if (swapChainSupport.getFormats() != null && swapChainSupport.getPresentModes() != null) {
                    swapChainAdequate = swapChainSupport.getFormats().capacity() > 0 && swapChainSupport.getPresentModes().capacity() > 0;
                }
            }

            return indices.isComplete() && extensionsSupported && swapChainAdequate;
        }
    }
    public static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.mallocInt(1);
            VulkanUtils.check(vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null));

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0),stack);
            VulkanUtils.check(vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions));

            List<String> requiredExtensions = new ArrayList<>(List.of(deviceExtensions));

            for (int i = 0; i < availableExtensions.capacity(); i++) {
                availableExtensions.position(i);

                //System.out.println(availableExtensions.extensionNameString());
                requiredExtensions.remove(availableExtensions.extensionNameString());
            }

            return requiredExtensions.isEmpty();
        }
    }

    public int findMemoryType(int typeFilter, int properties) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.mallocStack();
        vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }
}
