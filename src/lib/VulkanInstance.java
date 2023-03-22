package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT;

public class VulkanInstance {

    private VkInstance instance;

    public VulkanInstance(String appName, VkDebugUtilsMessengerCallbackEXT dbgFunc) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (VulkanUtils.enableValidationLayers && !VulkanUtils.checkValidationLayerSupport()) {
                throw new IllegalStateException("Validation layers requested, but some or none of them are available!");
            }

            PointerBuffer required_extensions = glfwGetRequiredInstanceExtensions();
            if (required_extensions == null) {
                throw new IllegalStateException("glfwGetRequiredInstanceExtensions failed to find the platform surface extensions.");
            }

            //TODO: Maybe try changing this to ByteBuffer?
            PointerBuffer extensionNames = memAllocPointer(64);

            for (int i = 0; i < required_extensions.capacity(); i++) {
                extensionNames.put(required_extensions.get(i));
            }


            ByteBuffer nameBuffer = stack.UTF8(appName);
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(stack);
            applicationInfo.sType$Default()
                    .pNext(0L)
                    .pApplicationName(nameBuffer)
                    .applicationVersion(0)
                    .applicationVersion(0)
                    .engineVersion(0)
                    .apiVersion(VK.getInstanceVersionSupported());

            extensionNames.flip();

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(stack);

            instanceCreateInfo.sType$Default()
                    .pNext(0L)
                    .flags(0)
                    .pApplicationInfo(applicationInfo);


            if (!checkInstanceExtensionsSupport(extensionNames))
                throw new IllegalStateException("VK Instance does not support required extensions!");

            PointerBuffer requiredLayers = stack.mallocPointer(VulkanUtils.validationLayers.length);

            for (int i = 0; i < VulkanUtils.validationLayers.length; i++) {
                requiredLayers.put(i, stack.ASCII(VulkanUtils.validationLayers[i]));
            }

            instanceCreateInfo.ppEnabledLayerNames(requiredLayers)
                    .ppEnabledExtensionNames(required_extensions);
            extensionNames.clear();

            VkDebugUtilsMessengerCreateInfoEXT dbgCreateInfo;
            if (VulkanUtils.enableValidationLayers) {

                dbgCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .messageSeverity(
                        /*VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |*/
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageType(
                                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                        )
                        .pfnUserCallback(dbgFunc)
                        .pUserData(NULL);

                instanceCreateInfo.pNext(dbgCreateInfo.address());
            }

            PointerBuffer pBuff = memAllocPointer(1);

            int err = vkCreateInstance(instanceCreateInfo, null, pBuff);

            if (err == VK_ERROR_INCOMPATIBLE_DRIVER) {
                throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD).");
            } else if (err == VK_ERROR_EXTENSION_NOT_PRESENT) {
                throw new IllegalStateException("Cannot find a specified extension library. Make sure your layers path is set appropriately.");
            } else if (err != 0) {
                throw new IllegalStateException("vkCreateInstance failed. Do you have a compatible Vulkan installable client driver (ICD) installed?");
            }

            instance = new VkInstance(pBuff.get(0), instanceCreateInfo);
        }

    }

    public static boolean checkInstanceExtensionsSupport(PointerBuffer extensionNames) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuff = memAllocInt(1);

            VulkanUtils.check(vkEnumerateInstanceExtensionProperties((String) null, intBuff, null));

            if (intBuff.get(0) != 0) {
                VkExtensionProperties.Buffer instance_extensions = VkExtensionProperties.malloc(intBuff.get(0), stack);
                VulkanUtils.check(vkEnumerateInstanceExtensionProperties((String)null, intBuff, instance_extensions));

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

    public VkInstance getVkInstance() {
        return instance;
    }

}
