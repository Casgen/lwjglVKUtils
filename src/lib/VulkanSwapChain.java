package lib;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSwapChain {

    private VkExtent2D extent;
    private long swapchainPtr, oldSwapChainPtr = 0L;
    private SwapChainBuffer swapChainBuffers[];

    public VkExtent2D getExtent() {
        return extent;
    }

    public long getSwapchainPtr() {
        return swapchainPtr;
    }

    public SwapChainBuffer[] getSwapChainBuffers() {
        return swapChainBuffers;
    }

    public int getCurrentBufferIndex() {
        return currentBufferIndex;
    }

    private int currentBufferIndex;

    public class SwapChainBuffer {

        private long image, view = 0L;
        private VkCommandBuffer cmdBuffer;

        public void setImage(long image) {
            this.image = image;
        }

        public void setView(long view) {
            this.view = view;
        }

        public void setCmdBuffer(VkCommandBuffer cmdBuffer) {
            this.cmdBuffer = cmdBuffer;
        }

        public long getImage() {
            return image;
        }

        public long getView() {
            return view;
        }

        public VkCommandBuffer getCmdBuffer() {
            return cmdBuffer;
        }

        public SwapChainBuffer(long image, long view, VkCommandBuffer cmdBuffer) {
            this.image = image;
            this.view = view;
            this.cmdBuffer = cmdBuffer;
        }

        public SwapChainBuffer() {}
    }

    public VulkanSwapChain(VulkanPhysicalDevice physicalDevice, VulkanLogicalDevice logicalDevice, VulkanSurface surface, VulkanWindow window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            SwapChainSupportDetails swapChainSupportDetails = physicalDevice.getSwapChainSupportDetails();

            VkSurfaceFormatKHR.Buffer surfaceFormat = VkSurfaceFormatKHR.malloc(1, stack);
            surfaceFormat.put(0, swapChainSupportDetails.chooseSwapSurfaceFormat());

            IntBuffer presentMode = stack.mallocInt(1);
            presentMode.put(0, swapChainSupportDetails.chooseSwapPresentMode());

            this.extent = swapChainSupportDetails.chooseSwapExtent(window);

            VkExtent2D.Buffer extent = VkExtent2D.malloc(1, stack);
            extent.put(0, this.extent);

            IntBuffer imageCount = stack.mallocInt(1);

            //For good measure, add another image into SwapChain
            imageCount.put(0, swapChainSupportDetails.getCapabilities().minImageCount() + 1);

            if (swapChainSupportDetails.getCapabilities().maxImageCount() > 0 && swapChainSupportDetails.getCapabilities().maxImageCount() < imageCount.get(0)) {
                imageCount.put(0, swapChainSupportDetails.getCapabilities().maxImageCount());
            }

            VkSwapchainCreateInfoKHR swapChainCreateInfo = VkSwapchainCreateInfoKHR.malloc(stack);

            swapChainCreateInfo.sType$Default()
                    .surface(surface.getSurfacePtr())
                    .minImageCount(imageCount.get(0))
                    .imageFormat(surfaceFormat.get(0).format())
                    .imageColorSpace(surfaceFormat.get(0).colorSpace())
                    .imageArrayLayers(1)                            // No Mipmapping
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageExtent(extent.get(0));

            QueueFamilyIndices indices = physicalDevice.getQueueFamilyIndices();

            if (indices.isQueueIndexSame()) {

                // Indicates that the swapchain will be used only by one queue.
                swapChainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            } else {

                IntBuffer indicesBuffer = stack.mallocInt(2);

                if (!indices.isComplete())
                    throw new IllegalStateException("Some queue families are not present!");

                indicesBuffer.put(0, indices.getGraphicsFamily().get());
                indicesBuffer.put(1, indices.getPresentFamily().get());

                // Indicates that the swapchain will be shared between the two queues.
                swapChainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                swapChainCreateInfo.queueFamilyIndexCount(2);
                swapChainCreateInfo.pQueueFamilyIndices(indicesBuffer);
            }

            swapChainCreateInfo.preTransform(swapChainSupportDetails.getCapabilities().currentTransform());
            swapChainCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            swapChainCreateInfo.presentMode(presentMode.get(0));
            swapChainCreateInfo.clipped(true);
            swapChainCreateInfo.oldSwapchain(0L);

            LongBuffer tempPtr = stack.mallocLong(1);

            VulkanUtils.check(vkCreateSwapchainKHR(logicalDevice.getVkDevice(), swapChainCreateInfo, null, tempPtr));

            swapchainPtr = tempPtr.get(0);

            //Get the number of swapchain images
            IntBuffer imgCount = stack.mallocInt(1);
            VulkanUtils.check(vkGetSwapchainImagesKHR(logicalDevice.getVkDevice(), swapchainPtr, imgCount, null));

            // Get the pointers to every swapchain image.
            LongBuffer swapChainImgPtrs = stack.mallocLong(imgCount.get(0));
            VulkanUtils.check(vkGetSwapchainImagesKHR(logicalDevice.getVkDevice(), swapchainPtr, imgCount, swapChainImgPtrs));

            swapChainBuffers = new SwapChainBuffer[imgCount.get(0)];

            LongBuffer imgViewPtr = stack.mallocLong(1);

            for (int i = 0; i < imgCount.get(0); i++) {
                swapChainBuffers[i] = new SwapChainBuffer();
                swapChainBuffers[i].image = swapChainImgPtrs.get(i);

                VkImageViewCreateInfo colorAttachmentView = VkImageViewCreateInfo.malloc(stack);
                colorAttachmentView.sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .image(swapChainImgPtrs.get(i))
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(surfaceFormat.format())
                        .components(it -> it
                                //TODO: Maybe it will have problems when creating the framebuffers.
                                //  the swizzles have to identical to work properly
                                .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                                .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                                .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                                .a(VK_COMPONENT_SWIZZLE_IDENTITY))
                        .subresourceRange(it -> it
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .baseArrayLayer(0)
                                .layerCount(1));

                VulkanUtils.check(vkCreateImageView(logicalDevice.getVkDevice(),colorAttachmentView, null,imgViewPtr));
                swapChainBuffers[i].view = imgViewPtr.get(0);
            }
        }
    }


}
