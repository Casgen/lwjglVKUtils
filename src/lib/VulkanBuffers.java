package lib;

import abstr.Vertex;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanBuffers {

    static public class Buffer implements VulkanResource {
        public long pBuffer;
        public long pDeviceMemory;
        public long size;

        @Override
        public void destroy(VulkanLogicalDevice device) {
            vkDestroyBuffer(device.getVkDevice(), pBuffer, null);
            vkFreeMemory(device.getVkDevice(), pDeviceMemory, null);

            pBuffer = MemoryUtil.NULL;
            pDeviceMemory = MemoryUtil.NULL;
            size = 0L;
        }

        public Buffer(long pBuffer, long pDeviceMemory, long size) {
            this.pBuffer = pBuffer;
            this.pDeviceMemory = pDeviceMemory;
            this.size = size;
        }

        public Buffer() {

        }


        public void setData(VulkanLogicalDevice device, ByteBuffer srcData) {

            try (MemoryStack stack = MemoryStack.stackPush()){

                PointerBuffer dest = stack.mallocPointer(1);

                if (pDeviceMemory == MemoryUtil.NULL || pBuffer == MemoryUtil.NULL)
                    throw new NullPointerException("VkDeviceMemory or VkBuffer is null!");

                vkMapMemory(device.getVkDevice(), pDeviceMemory, 0, srcData.capacity(), 0, dest);
                {
                    dest.getByteBuffer(0, srcData.capacity()).put(srcData);
                }

                vkUnmapMemory(device.getVkDevice(), pDeviceMemory);

            }

        }

    }

    static public class Attribute {

        /**
         * Tells the attribute to which index will be the attribute bound. (Corresponds to the binding property in the
         * VKVertexInputBindingDescription).
         */
        private int binding = 0;

        /**
         * Defines the location of the attribute (index) to the property of a Vertex which is defined in the
         * vertex shader.
         */
        private int location = 0;

        /**
         * Defines the format of the attribute of a Vertex. Mostly defined with the VK_FORMAT value.
         */
        private int format = 0;

        /**
         * Defines the size of an attribute in bytes.
         */
        private int size = 0;

        /**
         * Defines the offset of this attribute from the beginning.
         */
        private int offsetof = 0;

        public Attribute(int location, int format, int offsetof, int size) {
            this.location = location;
            this.format = format;
            this.offsetof = offsetof;
            this.size = size;
        }

        public VkVertexInputAttributeDescription.Buffer createAttributeDescription() {

            VkVertexInputAttributeDescription.Buffer attributeDescription = VkVertexInputAttributeDescription.create(1);

            attributeDescription.binding(binding);
            attributeDescription.format(this.format);
            attributeDescription.offset(this.offsetof);
            attributeDescription.location(this.location);

            return attributeDescription;
        }
    }

    static public class VertexBuffer {

        int stride = 0;
        int binding = 0;
        Attribute[] attributes;

        public VertexBuffer(int binding, Attribute[] attributes) {
            this.binding = binding;
            this.attributes = attributes;

            //TODO: Maybe set stride to -1 if there is not attribute to register, and throw an exception... Maybe.
            for (Attribute attribute : attributes) {
                stride += attribute.size;
            }
        }

        public VkVertexInputAttributeDescription.Buffer createAttributeDescriptions() {

            VkVertexInputAttributeDescription.Buffer descriptions = VkVertexInputAttributeDescription.create(this.attributes.length);

            for (Attribute attribute : attributes) {
                descriptions.put(attribute.createAttributeDescription());
            }

            return descriptions.rewind();
        }

        public VkVertexInputBindingDescription.Buffer createBindingDescription() {
            return createBindingDescription(VK_VERTEX_INPUT_RATE_VERTEX);
        }

        public VkVertexInputBindingDescription.Buffer createBindingDescription(int inputRate) {

            VkVertexInputBindingDescription.Buffer description = VkVertexInputBindingDescription.create(1);

            description.binding(this.binding);
            description.stride(stride);
            description.inputRate(inputRate);

            return description;
        }
    }
}