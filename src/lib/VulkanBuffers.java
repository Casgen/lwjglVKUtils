package lib;

import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;

public class VulkanBuffers {

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