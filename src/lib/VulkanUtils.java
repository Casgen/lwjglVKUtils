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


    public static void initVk() {
        GLFWErrorCallback.createPrint().set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
        }
    }



    //TODO: Add a function for enabling GPU Features
    public static void addGpuFeature(int feature, boolean bool) {
    }
}
