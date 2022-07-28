package me.Silverwolfg11.CommentConfig.node;

import java.util.Objects;

/**
 * A general node representing a YAML element like
 * a scalar value or a YAML section.
 */
public abstract class ConfigNode {

    private String[] comments;
    private ParentConfigNode parent;
    private String key;

    // Should only be used for root nodes
    protected ConfigNode() {}

    protected ConfigNode(ParentConfigNode parent, String key) {
        // Check nullity
        Objects.requireNonNull(parent);
        Objects.requireNonNull(key);
        this.parent = parent;
        this.key = key;
    }

    /**
     * Check if the node has comments associated with it.
     *
     * @return if the node has comments associated with it.
     */
    public boolean hasComments() {
        return comments != null;
    }

    /**
     * Get any comments associated with the node.
     *
     * @return comments associated with the node
     * or {@code null} if none exist.
     */
    public String[] getComments() {
        return comments;
    }

    /**
     * Set the comments associated with the node.
     *
     * @param comments Array of comments to associate with the node.
     *                 The array <b>cannot</b> be {@code null}.
     */
    public void setComments(String... comments) {
        Objects.requireNonNull(comments);
        this.comments = comments;
    }

    /**
     * Check if this node has a key associated with it.
     * Any nodes that do not have keys are most likely root nodes.
     *
     * @return if the node has a key associated with it.
     */
    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }

    /**
     * Get the key associated with the node.
     *
     * @return the key associated with the node or {@code null}.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Set the key associated with the node.
     * The key can be {@code null}, but that is not recommended.
     *
     * @param key Key to associate with the node.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Check if this node has a parent.
     *
     * If the node does not have a parent, then it is
     * most likely a root node.
     *
     * @return if the node has a parent.
     */
    public boolean hasParent() {
        return parent != null;
    }

    /**
     * Get the parent node of this node.
     *
     * @return the parent node or {@code null} if there isn't one.
     */
    public ParentConfigNode getParent() {
        return this.parent;
    }

    protected void setParent(ParentConfigNode parent) {
        this.parent = parent;
    }
}
