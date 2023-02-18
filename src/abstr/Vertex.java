package abstr;


import org.joml.*;

public class Vertex {

    private Vector2f pos;
    private Vector4f color;

    static public int SIZE_OF = 24;

    public Vertex(Vector2f pos, Vector4f color) {
        this.pos = pos;
        this.color = color;
    }

    public Vector2f getPos() {
        return pos;
    }

    public void setPos(Vector2f pos) {
        this.pos = pos;
    }

    public Vector4f getColor() {
        return color;
    }

    public void setColor(Vector4f color) {
        this.color = color;
    }
}
