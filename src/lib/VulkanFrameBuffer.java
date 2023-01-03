package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanFrameBuffer {


    private long[] pFrameBuffers;

    public VulkanFrameBuffer(VulkanLogicalDevice device, VulkanRenderPass renderPass, VkExtent2D extent2D, long[] swapChainImageViews) {

        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass.getRenderPassPtr());
            framebufferInfo.width(extent2D.width());
            framebufferInfo.height(extent2D.height());
            framebufferInfo.layers(1);

            pFrameBuffers = new long[swapChainImageViews.length];

            for(int i = 0; i < swapChainImageViews.length; i++) {

                attachments.put(0, swapChainImageViews[i]);

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device.getVkDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                pFrameBuffers[i] = (pFramebuffer.get(0));
            }
        }
    }

    public VulkanFrameBuffer(VulkanLogicalDevice logicalDevice, VulkanRenderPass renderPass, VulkanSwapChain swapChain) {
        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass.getRenderPassPtr());
            framebufferInfo.width(swapChain.getExtent().width());
            framebufferInfo.height(swapChain.getExtent().height());
            framebufferInfo.layers(1);

            VulkanSwapChain.SwapChainBuffer[] imageViews = swapChain.getSwapChainBuffers();

            pFrameBuffers = new long[imageViews.length];

            for(int i = 0; i < imageViews.length; i++) {

                attachments.put(0, swapChain.getSwapChainBuffers()[i].getView());

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(logicalDevice.getVkDevice(), framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                pFrameBuffers[i] = (pFramebuffer.get(0));
            }
        }
    }

    public long[] getFrameBuffers() {
        return pFrameBuffers;
    }
}
