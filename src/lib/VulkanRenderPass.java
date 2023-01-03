package lib;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK12.vkCreateRenderPass2;

public class VulkanRenderPass {


    private long renderPassPtr;

    public VulkanRenderPass(VulkanLogicalDevice logicalDevice, List<VkAttachmentDescription> attachDescs,
                            List<VkSubpassDescription> subpassDescs, List<VkSubpassDependency> subpassDependencies) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkAttachmentDescription.Buffer attachmentBuffer = VkAttachmentDescription.malloc(attachDescs.size(), stack);

            for (VkAttachmentDescription attachment : attachDescs) {
                attachmentBuffer.put(attachment);
            }
            attachmentBuffer.position(0);

            VkSubpassDependency.Buffer dependenciesBuffer = VkSubpassDependency.malloc(subpassDependencies.size(), stack);

            for (VkSubpassDependency dependency : subpassDependencies) {
                dependenciesBuffer.put(dependency);
            }
            dependenciesBuffer.position(0);

            VkSubpassDescription.Buffer subpassBuffer = VkSubpassDescription.malloc(subpassDescs.size(), stack);

            for (VkSubpassDescription desc : subpassDescs) {
                subpassBuffer.put(desc);
            }
            subpassBuffer.position(0);

            new VulkanRenderPass(logicalDevice,attachmentBuffer, subpassBuffer, dependenciesBuffer);
        }
    }

    public VulkanRenderPass(VulkanLogicalDevice logicalDevice, VkAttachmentDescription.Buffer attachmentBuffer,
                            VkSubpassDescription.Buffer subpassBuffer, VkSubpassDependency.Buffer subpassDependencyBuffer) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.malloc(stack);
            createInfo.sType$Default()
                    .flags(0)
                    .pAttachments(attachmentBuffer)
                    .pNext(NULL)
                    .pDependencies(subpassDependencyBuffer)
                    .pSubpasses(subpassBuffer);


            LongBuffer ptr = stack.mallocLong(1);

            VulkanUtils.check(vkCreateRenderPass(logicalDevice.getVkDevice(),createInfo,null,ptr));

            renderPassPtr = ptr.get(0);
        }
    }

    public VulkanRenderPass(VulkanLogicalDevice logicalDevice, VkAttachmentDescription.Buffer attachmentBuffer,
                            VkSubpassDescription.Buffer subpassBuffer) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.malloc(stack);
            createInfo.sType$Default()
                    .flags(0)
                    .pAttachments(attachmentBuffer)
                    .pNext(NULL)
                    .pSubpasses(subpassBuffer);


            LongBuffer ptr = stack.mallocLong(1);

            VulkanUtils.check(vkCreateRenderPass(logicalDevice.getVkDevice(),createInfo,null,ptr));

            renderPassPtr = ptr.get(0);
        }
    }

    public long getRenderPassPtr() {
        return renderPassPtr;
    }
}
