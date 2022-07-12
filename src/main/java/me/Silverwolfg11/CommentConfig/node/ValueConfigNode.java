package me.Silverwolfg11.CommentConfig.node;

import java.util.Objects;

public class ValueConfigNode extends ConfigNode {
    private Object value;

    protected ValueConfigNode(ParentConfigNode parent, String key, Object value) {
        super(parent, key);
        Objects.requireNonNull(value);
        this.value = value;
    }

    protected ValueConfigNode(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isPrimitive() {
        return value.getClass().isPrimitive();
    }

    public int getAsInt() {
        return (int) value;
    }

    public int getAsInt(int def) {
        try {
            return (int) value;
        } catch (Exception e) {
            return def;
        }
    }

    public <T> T getAs(Class<T> clazz) {
        return (T) value;
    }

    // Returns a parent-less, key-less node
    // The node must set a parent and key after use!
    public static ValueConfigNode leaf(Object value) {
        return new ValueConfigNode(value);
    }
}
