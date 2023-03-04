package lib;

import abstr.Vertex;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanLogicalDevice {

    private VkDevice vkDevice;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;


    public VulkanLogicalDevice(VulkanPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            //Create a Device queue
            FloatBuffer queuePriority = stack.mallocFloat(1);
            queuePriority.put(0, 1.f);

            // TODO: This can change in further development as more queue types will be needed
            VkDeviceQueueCreateInfo.Buffer deviceQueueCreateInfos = VkDeviceQueueCreateInfo.malloc(
                    physicalDevice.getQueueFamilyIndices().isQueueIndexSame() ? 1 : 2
            );


            if (!physicalDevice.getQueueFamilyIndices().isComplete())
                throw new IllegalStateException("No Presentation or Graphics queue family exists!");

            IntBuffer queueIndices;
            if (physicalDevice.getQueueFamilyIndices().isQueueIndexSame()) {
                queueIndices = stack.mallocInt(1);

                VkDeviceQueueCreateInfo createInfo = VkDeviceQueueCreateInfo.calloc();
                createInfo.sType$Default()
                        .pNext(NULL)
                        .pQueuePriorities(queuePriority)
                        .flags(0)
                        // Since the queue families indices are the same (at least for now, it doesn't matter
                        // which one we choose to pass in.
                        // TODO: This can change in further development as more queue types will be needed
                        // No need for isPresent. The isComplete function does this already for us
                        .queueFamilyIndex(physicalDevice.getQueueFamilyIndices().getGraphicsFamily().get());

                deviceQueueCreateInfos.put(0, createInfo);
            } else {
                // TODO: This can change in further development as more queue types will be needed
                queueIndices = stack.mallocInt(2);

                // No need for isPresent. The isComplete function does this already for us
                queueIndices.put(physicalDevice.getQueueFamilyIndices().getGraphicsFamily().get());
                queueIndices.put(physicalDevice.getQueueFamilyIndices().getPresentFamily().get());

                for (int i = 0; i < deviceQueueCreateInfos.capacity(); i++) {
                    deviceQueueCreateInfos.position(i);
                    queueIndices.position(i);

                    VkDeviceQueueCreateInfo createInfo = VkDeviceQueueCreateInfo.calloc();
                    createInfo.sType$Default()
                            .pNext(NULL)
                            .pQueuePriorities(queuePriority)
                            .flags(0)
                            .queueFamilyIndex(queueIndices.get());

                    deviceQueueCreateInfos.put(createInfo);
                }

            }

            deviceQueueCreateInfos.position(0);

            // TODO: Add a function for enabling certain GPU features. also make a handler for setting all of not wanted
            //  feature to false (Vulkan doesn't like the NULL value).
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack);

            PointerBuffer extensionProperties = stack.mallocPointer(VulkanPhysicalDevice.deviceExtensions.length);

            // TODO: Careful here, there could be some other extension that maybe have been added before this.
            //  This needs to be maybe addressed later.
            for (int i = 0; i < VulkanPhysicalDevice.deviceExtensions.length; i++) {
                extensionProperties.position(i);
                extensionProperties.put(memASCII(VulkanPhysicalDevice.deviceExtensions[i]));
            }

            extensionProperties.flip();

            deviceCreateInfo.sType$Default()
                    .pNext(NULL)
                    .pQueueCreateInfos(deviceQueueCreateInfos)
                    .pEnabledFeatures(features)
                    .ppEnabledExtensionNames(extensionProperties);

            System.out.println(deviceCreateInfo.queueCreateInfoCount());

            if (VulkanUtils.enableValidationLayers) {
                PointerBuffer requiredLayers = stack.mallocPointer(VulkanUtils.validationLayers.length);

                for (int i = 0; i < VulkanUtils.validationLayers.length; i++) {
                    requiredLayers.put(i, stack.ASCII(VulkanUtils.validationLayers[i]));
                }

                deviceCreateInfo.ppEnabledLayerNames(requiredLayers);
            }

            // Used as a storage for a pointer to the device. vkCreateDevice returns a new pointer to the VkDevice
            // With this you then construct the VkDevice with the 'new' keyword.
            PointerBuffer devicePtr = stack.mallocPointer(1);

            VulkanUtils.check(vkCreateDevice(physicalDevice.getVkPhysicalDevice(), deviceCreateInfo, null, devicePtr));
            this.vkDevice = new VkDevice(devicePtr.get(0), physicalDevice.getVkPhysicalDevice(), deviceCreateInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);

            QueueFamilyIndices indices = physicalDevice.getQueueFamilyIndices();

            vkGetDeviceQueue(vkDevice, indices.getGraphicsFamily().get(), 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), vkDevice);

            vkGetDeviceQueue(vkDevice, indices.getPresentFamily().get(), 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), vkDevice);


        }
    }


    public VkDevice getVkDevice() {
        return vkDevice;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue getPresentQueue() {
        return presentQueue;
    }

}
