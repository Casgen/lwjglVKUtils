package lib;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanUtils {


    final static String[] validationLayers = new String[]{
            "VK_LAYER_KHRONOS_validation",
    };

    final static String[] deviceExtensions = new String[]{
            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
    };

    public static boolean enableValidationLayers = true;
    final private static VkPhysicalDeviceFeatures gpuFeatures = VkPhysicalDeviceFeatures.malloc();


    public static boolean checkValidationLayerSupport() {

        try (MemoryStack stack = stackPush()) {
            IntBuffer intBuff = memAllocInt(1);

            PointerBuffer requiredLayers = null;
            check(vkEnumerateInstanceLayerProperties(intBuff, null));


            if (intBuff.get(0) > 0) {
                VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(intBuff.get(0), stack);
                check(vkEnumerateInstanceLayerProperties(intBuff, availableLayers));

                for (int i = 0; i < validationLayers.length; i++) {
                    boolean layerFound = false;
                    for (int j = 0; j < availableLayers.capacity(); j++) {
                        availableLayers.position(j);
                        //System.out.println(availableLayers.layerNameString());
                        if (validationLayers[i].equals(availableLayers.layerNameString())) {
                            layerFound = true;
                            break;
                        }
                    }

                    if (!layerFound) {
                        System.err.format("Cannot find layer: %s\n", validationLayers[i]);
                        throw new IllegalStateException("vkEnumerateInstanceLayerProperties failed to find required validation layer.");
                    }
                }
            }
        }
        return true;
    }

    public static void check(int errcode) {
        if (errcode != 0) {
            throw new IllegalStateException(String.format("Vulkan error [0x%X]", errcode));
        }
    }

    public static boolean checkInstanceExtensionsSupport(PointerBuffer extensionNames) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuff = memAllocInt(1);

            VulkanUtils.check(vkEnumerateInstanceExtensionProperties((String) null, intBuff, null));

            if (intBuff.get(0) != 0) {
                    VkExtensionProperties.Buffer instance_extensions = VkExtensionProperties.malloc(intBuff.get(0), stack);
                    check(vkEnumerateInstanceExtensionProperties((String)null, intBuff, instance_extensions));

                    for (int i = 0; i < intBuff.get(0); i++) {
                        instance_extensions.position(i);
                        if (VK_EXT_DEBUG_UTILS_EXTENSION_NAME.equals(instance_extensions.extensionNameString()) &&
                        VulkanUtils.enableValidationLayers) {
                                extensionNames.put(memASCII(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
                            }
                    }
            }

            return true;
        }
    }

    public static void initVk() {
        GLFWErrorCallback.createPrint().set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
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

    public static boolean isDeviceSuitable(VkPhysicalDevice physicalDevice, VulkanSurface surface, QueueFamilyIndices indices) {
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

    //TODO: Add a function for enabling GPU Features
    public static void addGpuFeature(int feature, boolean bool) {
    }
}
