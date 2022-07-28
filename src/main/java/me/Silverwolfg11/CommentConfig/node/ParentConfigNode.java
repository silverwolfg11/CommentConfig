package me.Silverwolfg11.CommentConfig.node;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An abstraction of a YAML section in node format.
 * <br>
 * To create the very top section, the "root" node, use
 * {@link ParentConfigNode#createRoot()}.
 */
public class ParentConfigNode extends ConfigNode {
    private Map<String, ConfigNode> children;

    // Only used for root node
    private ParentConfigNode() {}

    private ParentConfigNode(ParentConfigNode parent, String key) {
        super(parent, key);
    }

    /**
     * Check if this section has any child nodes.
     *
     * @return if this section has child nodes.
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * Check if this section has a specific child node.
     *
     * The child node is checked base on its key.
     *
     * @param childNode The child node to check.
     *                  The specified node <b>cannot</b> be {@code null}.
     *
     * @return if the section has the child node.
     */
    public boolean hasChild(ConfigNode childNode) {
        Objects.requireNonNull(childNode);
        return hasChild(childNode.getKey());
    }

    /**
     * Check if this section has a specific child node
     * with the passed-in key.
     *
     * @param key The key to check against.
     *            The key <b>cannot</b> be {@code null}.
     *
     * @return if the section has a child node with the key.
     */
    public boolean hasChild(String key) {
        Objects.requireNonNull(key);
        return hasChildren() && children.containsKey(key);
    }

    /**
     * Add a child value to this section.
     *
     * @param key The key to associate with the child.
     *            The key <b>cannot</b> be {@code null}.
     * @param value The value to associate with the child.
     *              The value <b>cannot</b> be {@code null}.
     *
     * @return the child node added.
     */
    public ValueConfigNode addChild(String key, Object value) {
        return addChild(key, value, (String[]) null);
    }

    /**
     * Add a child value with comments to this section.
     * The comments will be associated with the child, not the section.
     *
     * @param key The key to associate with the child.
     *            The key <b>cannot</b> be {@code null}.
     *
     * @param value The value to associate with the child.
     *              The value <b>cannot</b> be {@code null}.
     *
     * @param comments The comments to associate with the child.
     *
     * @return the child node added to the section.
     */
    public ValueConfigNode addChild(String key, Object value, String... comments) {
        ValueConfigNode valueNode = new ValueConfigNode(this, key, value);

        if (comments != null && comments.length > 0)
            valueNode.setComments(comments);

        addChild(valueNode);
        return valueNode;
    }

    /**
     * Add a child section to this section.
     *
     * @param sectionName The name of the child section.
     *                    The child section's name <b>cannot</b> be {@code null}.
     *
     * @return the child section added.
     */
    public ParentConfigNode addSection(String sectionName) {
        return addSection(sectionName, (String[]) null);
    }

    /**
     * Add a child section with comments to this section.
     * The comments are associated with the child section.
     *
     * @param sectionName The name of the child section.
     *                    The child section's name <b>cannot</b> be {@code null}.
     * @param comments Comments to associate with the child section.
     *
     * @return the child section added.
     */
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

    /**
     * Add a child node to this section.
     *
     * @param childNode The child node to add to the section.
     *                  The child node and the child node's key <b>cannot</b>
     *                  be {@code null}.
     */
    public void addChild(ConfigNode childNode) {
        Objects.requireNonNull(childNode);
        Objects.requireNonNull(childNode.getKey());

        if (children == null)
            children = new LinkedHashMap<>();

        // Force-fully re-parent to maintain correct tree
        if (!childNode.hasParent() || childNode.getParent() != this)
            childNode.setParent(this);

        children.put(childNode.getKey(), childNode);
    }

    /**
     * Remove the child node associated with the section.
     *
     * The child node is removed base on its key.
     *
     * @param configNode Child node to remove.
     *
     * @return if a child node was removed.
     */
    public boolean removeChild(ConfigNode configNode) {
        if (!hasChild(configNode))
            return false;

        children.remove(configNode.getKey());
        configNode.setParent(null);

        return true;
    }

    /**
     * Get a specific child node from a key.
     *
     * @param key Key of child node.
     *            The key <b>cannot</b> be {@code null}.
     *
     * @return the child node with the key or {@code null} if none matched.
     */
    public ConfigNode getChild(String key) {
        Objects.requireNonNull(key);

        if (children != null)
            return children.get(key);

        return null;
    }

    /**
     * Get all child nodes associated with the section.
     *
     * @return an <b>immutable</b> list of the child nodes.
     */
    public Collection<ConfigNode> getChildren() {
        if (children == null || children.isEmpty())
            return Collections.emptyList();

        return Collections.unmodifiableCollection(children.values());
    }

    /**
     * Create a root section.
     *
     * @return the root section.
     */
    public static ParentConfigNode createRoot() {
        return new ParentConfigNode();
    }
}
