package lib;

import exc.MemoryAllocationException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanDevices {

    private VulkanLogicalDevice logicalDevice;
    private VulkanPhysicalDevice physicalDevice;

    public VulkanDevices(VulkanInstance instance, VulkanSurface surface) {
        physicalDevice = new VulkanPhysicalDevice(instance, surface);
        logicalDevice = new VulkanLogicalDevice(physicalDevice);
    }

    public VulkanLogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public VulkanPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getVkDevice() {
        return logicalDevice.getVkDevice();
    }

    public VkPhysicalDevice getVkPhysicalDevice() {
        return physicalDevice.getVkPhysicalDevice();
    }


    /**
     * Creates a VkBuffer, binds the device memory to it and fills it up with the given data.
     * @implNote The size is determined by the ByteBuffer capacity.
     * @param usage What purpose will the buffer be used for.
     * @param sharingMode Sets how the buffer will be shared. That means if it's going to be exclusive to only one
     *                       family queue or be shared between multiple queues.
     * @param memoryProperty Sets the properties of the memory (How it should behave)
     * @param srcData A buffer of pre-filled data
     * @return A pointer to the newly created buffer;
     */
    public VulkanBuffers.Buffer createBuffer(int usage, int sharingMode, int memoryProperty, ByteBuffer srcData) {
        // DON'T FORGET TO REWIND TO SET THE POSITION TO 0!
        srcData.rewind();

        VulkanBuffers.Buffer vertexBuffer = createBuffer(usage, sharingMode, srcData.capacity(), memoryProperty);
        vertexBuffer.setData(logicalDevice, srcData);

        return vertexBuffer;
    }

    /**
     * Creates a VkBuffer and binds the device memory to it.
     *
     * @param usage What purpose will the buffer be used for.
     * @param sharingMode Sets how the buffer will be shared. That means if it's going to be exclusive to only one
     *                       family queue or be shared between multiple queues.
     * @param size Size of the data in bytes
     * @param memoryProperty Sets the properties of the memory (How it should behave)
     * @return A pointer to the newly created buffer;
     */
        public VulkanBuffers.Buffer createBuffer(int usage, int sharingMode, int size, int memoryProperty) {
            try (MemoryStack stack = MemoryStack.stackPush()) {

                VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
                bufferCreateInfo.sType$Default()
                        .usage(usage)
                        .sharingMode(sharingMode)
                        .size(size);

                LongBuffer pBuffer = stack.callocLong(1);

                VulkanUtils.check(vkCreateBuffer(getVkDevice(), bufferCreateInfo, null, pBuffer));

                VkMemoryRequirements memoryRequirements = VkMemoryRequirements.calloc(stack);

                vkGetBufferMemoryRequirements(getVkDevice(), pBuffer.get(0), memoryRequirements);

                VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack);

                allocateInfo.sType$Default()
                        .allocationSize(memoryRequirements.size())
                        .memoryTypeIndex(physicalDevice.findMemoryType(memoryRequirements.memoryTypeBits(), memoryProperty));


                LongBuffer pBufferMemory = stack.mallocLong(1);

                VulkanUtils.check(vkAllocateMemory(getVkDevice(), allocateInfo, null, pBufferMemory));

                if (pBufferMemory.get(0) == MemoryUtil.NULL)
                    throw new MemoryAllocationException("Failed to allocate Memory! vkAllocateMemory returned a null pointer!");

                vkBindBufferMemory(getVkDevice(), pBuffer.get(0), pBufferMemory.get(0), 0);

                VulkanBuffers.Buffer vertexBuffer;
                vertexBuffer = new VulkanBuffers.Buffer(pBuffer.get(0), pBufferMemory.get(0), bufferCreateInfo.size());

                return vertexBuffer;
            }
    }


}
