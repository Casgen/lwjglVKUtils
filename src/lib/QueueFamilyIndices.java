package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.util.Optional;

public class QueueFamilyIndices {

    private Optional<Integer> graphicsFamily = Optional.empty();
    private Optional<Integer> presentFamily = Optional.empty();

    public QueueFamilyIndices(VkPhysicalDevice device, long surface) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer queueFamilyCount = stack.mallocInt(1);

            //Get how many Queue families exist
            vkGetPhysicalDeviceQueueFamilyProperties(device,queueFamilyCount, null);

            //Allocated given buffer with defined size and obtain the QueueProperties
            VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0));
            vkGetPhysicalDeviceQueueFamilyProperties(device,queueFamilyCount, queueProps);

            if (queueFamilyCount.get(0) == 0) {
                throw new IllegalStateException();
            }

            //Acts as a boolean for checking if the queue supports presenting.
            IntBuffer presentSupport = stack.mallocInt(1);

            for (int i = 0; i < queueProps.capacity(); i++) {

                if ((queueProps.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsFamily = Optional.of(i);
                }

                vkGetPhysicalDeviceSurfaceSupportKHR(device,i,surface,presentSupport);

                /* If the queue supports presentation, get the index of that queue and store it.
                   NOTE: The queueFamilyIndex member of each element of pQueueCreateInfos must be
                   unique within pQueueCreateInfos, except that two members can share the same
                   queueFamilyIndex if one describes protected-capable queues and one describes
                   queues that are not protected-capable
                 */
                if (presentSupport.get(0) == VK_TRUE) {
                    presentFamily = Optional.of(i);
                }
                if (isComplete()) break;
            }
        }

    }
    public boolean isComplete() {
        return graphicsFamily.isPresent() && presentFamily.isPresent();
    }

    public boolean isQueueIndexSame() {
        return isComplete() && graphicsFamily.get() == presentFamily.get();
    }

    public Optional<Integer> getGraphicsFamily() {
        return graphicsFamily;
    }

    public Optional<Integer> getPresentFamily() {
        return presentFamily;
    }
}
