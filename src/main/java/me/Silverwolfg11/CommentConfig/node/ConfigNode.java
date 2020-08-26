package me.Silverwolfg11.CommentConfig.node;

import java.util.Objects;

public abstract class ConfigNode {

    private String[] comments;
    private ParentConfigNode parent;
    private String key;

    // Should only be used for root nodes
    protected ConfigNode() {}

    public ConfigNode(ParentConfigNode parent, String key) {
        // Check nullity
        Objects.requireNonNull(parent);
        Objects.requireNonNull(key);
        this.parent = parent;
        this.key = key;
    }

    public boolean hasComments() {
        return comments != null;
    }

    public String[] getComments() {
        return comments;
    }

    public void setComments(String... comments) {
        Objects.requireNonNull(comments);
        this.comments = comments;
    }

    public boolean hasKey() {
        return key != null;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public ParentConfigNode getParent() {
        return this.parent;
    }

    public void setParent(ParentConfigNode parent) {
        this.parent = parent;
    }
}
