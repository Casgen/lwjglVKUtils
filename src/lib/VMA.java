package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;

public class VMA {

    private long vmaAllocPtr;

    public VMA(VulkanInstance instance, VulkanPhysicalDevice physicalDevice, VulkanLogicalDevice logicalDevice) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VmaAllocatorCreateInfo createInfo = VmaAllocatorCreateInfo.calloc(stack);

            createInfo.device(logicalDevice.getVkDevice());
            createInfo.physicalDevice(physicalDevice.getVkPhysicalDevice());
            createInfo.instance(instance.getVkInstance());
            createInfo.flags();
            createInfo.vulkanApiVersion();

            PointerBuffer ptr = stack.mallocPointer(1);
            VulkanUtils.check(Vma.vmaCreateAllocator(createInfo,ptr));

            if (ptr.get(0) == MemoryUtil.NULL)
                throw new NullPointerException("Couldn't construct VMA instance! the VMA " +
                        "Allocator returned a null pointer!");

            vmaAllocPtr = ptr.get(0);
        }
    }
}
