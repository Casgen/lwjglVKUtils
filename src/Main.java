import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.VK_FALSE;

public class Main {


    public static void main(String[] args) {
        VulkanUtils.initVk();
        VulkanInstance instance = new VulkanInstance("First Vulkan app", true,
                VkDebugUtilsMessengerCallbackEXT.create(
                        (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                            String severity;
                            if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
                                severity = "VERBOSE";
                            } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                                severity = "INFO";
                            } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                                severity = "WARNING";
                            } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                                severity = "ERROR";
                            } else {
                                severity = "UNKNOWN";
                            }

                            String type;
                            if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
                                type = "GENERAL";
                            } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
                                type = "VALIDATION";
                            } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
                                type = "PERFORMANCE";
                            } else {
                                type = "UNKNOWN";
                            }

                            VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                            System.err.format(
                                    "%s %s: [%s]\n\t%s\n",
                                    type, severity, data.pMessageIdNameString(), data.pMessageString()
                            );

                            /*
                             * false indicates that layer should not bail-out of an
                             * API call that had validation failures. This may mean that the
                             * app dies inside the driver due to invalid parameter(s).
                             * That's what would happen without validation layers, so we'll
                             * keep that behavior here.
                             */
                            return VK_FALSE;
                        }
                ));
    }
}
