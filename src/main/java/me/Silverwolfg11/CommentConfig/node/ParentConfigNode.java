package me.Silverwolfg11.CommentConfig.node;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ParentConfigNode extends ConfigNode {
    private Map<String, ConfigNode> children;

    // Only used for root node
    private ParentConfigNode() {}

    private ParentConfigNode(ParentConfigNode parent, String key) {
        super(parent, key);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean hasChild(ConfigNode childNode) {
        Objects.requireNonNull(childNode);
        return hasChild(childNode.getKey());
    }

    public boolean hasChild(String key) {
        Objects.requireNonNull(key);
        return hasChildren() && children.containsKey(key);
    }

    public ValueConfigNode addChild(String key, Object value) {
        return addChild(key, value, (String[]) null);
    }

    public ValueConfigNode addChild(String key, Object value, String... comments) {
        ValueConfigNode valueNode = new ValueConfigNode(this, key, value);

        if (comments != null && comments.length > 0)
            valueNode.setComments(comments);

        addChild(valueNode);
        return valueNode;
    }

    public ParentConfigNode addSection(String sectionName) {
        return addSection(sectionName, (String[]) null);
    }

    public ParentConfigNode addSection(String sectionName, String... comments) {
        ParentConfigNode parentNode = new ParentConfigNode(this, sectionName);

        // Don't add the section if it already exists
        if (hasChild(parentNode)) {
            ConfigNode childNode = getChild(parentNode.getKey());

            if (childNode instanceof ParentConfigNode) {
                return (ParentConfigNode) childNode;
            }
        }

        if (comments != null && comments.length > 0) {
            parentNode.setComments(comments);
        }

        addChild(parentNode);
        return parentNode;
    }

    public void addChild(ConfigNode childNode) {
        Objects.requireNonNull(childNode);
        Objects.requireNonNull(childNode.getKey());

        if (children == null)
            children = new LinkedHashMap<>();

        children.put(childNode.getKey(), childNode);
    }

    public boolean removeChild(ConfigNode configNode) {
        if (!hasChild(configNode))
            return false;

        children.remove(configNode.getKey());
        return true;
    }

    public ConfigNode getChild(String key) {
        Objects.requireNonNull(key);

        if (children != null)
            return children.get(key);

        return null;
    }

    public Collection<ConfigNode> getChildren() {
        if (children == null || children.isEmpty())
            return Collections.emptyList();

        return Collections.unmodifiableCollection(children.values());
    }

    public static ParentConfigNode createRoot() {
        return new ParentConfigNode();
    }
}
