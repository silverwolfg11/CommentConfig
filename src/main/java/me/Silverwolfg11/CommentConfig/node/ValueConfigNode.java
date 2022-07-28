package me.Silverwolfg11.CommentConfig.node;

import java.util.Objects;

/**
 * An abstraction of a YAML key-value pair.
 * <br><br>
 * In most cases, an instance of this class
 * should not be explicitly created. Instead,
 * use {@link ParentConfigNode#addChild(String, Object)} or a variant
 * to get an instance of this class.
 */
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

    /**
     * Get the value associated with this node.
     *
     * @return value associated with the node.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the value associated with the node.
     *
     * @param value value to set.
     *        The value <b>cannot</b> be {@code null}.
     */
    public void setValue(Object value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    /**
     * Returns if the value is a primitive.
     * A value is a primitive if {@link Class#isPrimitive()}
     * is true.
     *
     * @return if the value is a primitive.
     */
    public boolean isPrimitive() {
        return value.getClass().isPrimitive();
    }

    /**
     * Get the value as an integer.
     *
     * @return the value as an integer.
     * @throws ClassCastException if the value isn't an integer.
     */
    public int getAsInt() {
        return (int) value;
    }

    /**
     * Get the value as an integer.
     * If the value is not an integer, return a default value.
     *
     * @param def default value to return if the value isn't an integer.
     *
     * @return value as an integer or the default value.
     */
    public int getAsInt(int def) {
        try {
            return (int) value;
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Get the value as a specific type.
     *
     * @param clazz The class to represent the value as.
     *
     * @return value as a class.
     * @param <T> The type to return the value as.
     * @throws ClassCastException if the value cannot be casted as the specified type.
     */
    public <T> T getAs(Class<T> clazz) {
        return (T) value;
    }

    /**
     * Get a parent-less and key-less value node.
     * The node must have a valid key before it
     * can actually be used.
     *
     * <b>This method is not recommended for use unless you
     * know what you're doing.</b>
     *
     * @param value The value of the node.
     * @return parent-less and key-less value node.
     */
    public static ValueConfigNode leaf(Object value) {
        return new ValueConfigNode(value);
    }
}
