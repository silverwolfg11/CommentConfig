package me.Silverwolfg11.CommentConfig.serialization;

import javafx.scene.Parent;
import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.ConfigVersion;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.annotations.SnakeSerialize;
import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.node.ValueConfigNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClassSerializer {

    // Private constructor since it's a utility class
    private ClassSerializer() {
    }

    public static ParentConfigNode serializeClass(Object obj) {
        Class<?> clazz = obj.getClass();

        // Make sure class is marked to be serializable
        if (!clazz.isAnnotationPresent(SerializableConfig.class)) {
            throw new UnsupportedOperationException("Object must have the `SerializableConfig` annotation!");
        }

        ParentConfigNode root = ParentConfigNode.createRoot();
        serializeFields(obj, Arrays.asList(clazz.getDeclaredFields()), root);

        // Check if the class has any header comments
        if (clazz.isAnnotationPresent(Comment.class)) {
            Comment comment = clazz.getAnnotation(Comment.class);
            root.setComments(comment.value());
        }

        if (clazz.isAnnotationPresent(ConfigVersion.class)) {
            ConfigVersion versionAnnot = clazz.getAnnotation(ConfigVersion.class);
            double configVersion = versionAnnot.value();
            root.addChild("config-version", configVersion, "Do not touch!!!");
        }

        return root;
    }

    private static void serializeFields(Object obj, Collection<Field> fields, ParentConfigNode root) {
        for (Field field : fields) {
            // Avoid compiler-generated or transient fields
            if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()))
                continue;

            Object fieldValue = null;

            field.setAccessible(true);
            try {
                fieldValue = field.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            field.setAccessible(false);

            // If the field value is null do not add it to the node
            if (fieldValue == null)
                continue;

            // Handle custom serialization
            if (!field.isAnnotationPresent(SnakeSerialize.class)) {
                // Convert enums to strings otherwise SnakeYAML will serialize the enum class too which looks ugly
                if (fieldValue.getClass().isEnum()) {
                    fieldValue = ((Enum) fieldValue).name();
                }
            }

            ParentConfigNode currParent;
            String childName;

            if (field.isAnnotationPresent(Node.class)) {
                Node nodeAnnotation = field.getAnnotation(Node.class);
                String[] key = nodeAnnotation.value();
                currParent = getParentNodeFromKey(key, 0, root);
                childName = key[key.length - 1];
            }
            else {
                currParent = root;
                childName = field.getName();
            }

            ConfigNode newNode = null;

            List<String> commentsList = new ArrayList<>();

            if (fieldValue.getClass().isAnnotationPresent(SerializableConfig.class)) {
                ParentConfigNode subSection = serializeClass(fieldValue);
                if (!subSection.hasChildren()) {
                    continue;
                }

                ParentConfigNode mergedSection = currParent.addSection(childName);
                transferSectionNodes(subSection, mergedSection, true);

                if (subSection.hasComments()) {
                    commentsList = lazyAdd(commentsList, subSection.getComments());
                }

                newNode = mergedSection;
            }

            // Allow parsing maps
            else if (fieldValue instanceof Map) {
                Map<?, ?> mapFieldValue = (Map<?, ?>) fieldValue;
                // Don't serialize empty maps
                if (mapFieldValue.isEmpty())
                    continue;

                Map.Entry<?, ?> firstEntry = mapFieldValue.entrySet().stream().findFirst().get();
                Class<?> mapValueClass = firstEntry.getValue().getClass();

                // Only handle complex value types for right now
                if (mapValueClass.isAnnotationPresent(SerializableConfig.class)) {
                    ParentConfigNode mapSection = currParent.addSection(childName);
                    for (Map.Entry<?, ?> mapEntry : mapFieldValue.entrySet()) {
                        Object key = mapEntry.getKey();
                        Object value = mapEntry.getValue();

                        String sectionName = key.toString();
                        ParentConfigNode serializedValue = serializeClass(value);
                        // Only add to section if serialized value has any member properties.
                        if (serializedValue.hasChildren()) {
                            ParentConfigNode entrySection = mapSection.addSection(sectionName);
                            transferSectionNodes(serializedValue, entrySection, true);
                        }
                    }

                    newNode = mapSection;
                }
            }

            // Only serialize if node hasn't been initialized yet.
            // Serves as a default serialization
            if (newNode == null) {
                newNode = currParent.addChild(childName, fieldValue);
            }

            Comment comments = field.getAnnotation(Comment.class);

            if (comments != null) {
                String[] commentsArray = comments.value();
                // Merge existing comments list
                if (commentsList != null) {
                    Collections.addAll(commentsList, commentsArray);
                    commentsArray = commentsList.toArray(new String[0]);
                }

                newNode.setComments(commentsArray);
            }

        }
    }
    private static <T> List<T> lazyAdd(List<T> list, T... elements) {
        if (elements == null || elements.length == 0)
            return list;

        if (list == null)
            list = new ArrayList<>(elements.length);

        Collections.addAll(list, elements);

        return list;
    }
    private static void transferSectionNodes(ParentConfigNode from, ParentConfigNode to, boolean overwriteComments) {
        for (ConfigNode child : from.getChildren()) {
            to.addChild(child);
        }

        if (from.hasComments()) {
            if (!to.hasComments() || overwriteComments) {
                to.setComments(from.getComments());
            }
            else {
                // Merge comments
                List<String> comments = lazyAdd(null, to.getComments());
                comments = lazyAdd(comments, from.getComments());
                to.setComments(comments.toArray(new String[0]));
            }
        }

    }

    private static ParentConfigNode getParentNodeFromKey(String[] key, int currIndex, ParentConfigNode parent) {
        if (key.length == 0 || currIndex == key.length - 1) {
            return parent;
        }

        String childName = key[currIndex];
        ParentConfigNode newParent = parent.addSection(childName);

        return getParentNodeFromKey(key, ++currIndex, newParent);
    }



}
