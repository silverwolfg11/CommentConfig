package me.Silverwolfg11.CommentConfig.serialization;

import javafx.scene.Parent;
import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.ConfigVersion;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.annotations.SnakeSerialize;
import me.Silverwolfg11.CommentConfig.node.CommentKey;
import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.node.ValueConfigNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private static ConfigNode serializeChild(Object obj) {
        if (obj == null)
            return null;

        Class<?> clazz = obj.getClass();
        if (clazz.isAnnotationPresent(SerializableConfig.class)) {
            ParentConfigNode classNode = serializeClass(obj);
            // If class node is empty then skip serialization
            return classNode.hasChildren() ? classNode : null;
        }
        else if (obj instanceof Map) {
            Map<?, ?> mapFieldValue = (Map<?, ?>) obj;

            // Don't serialize empty maps
            if (mapFieldValue.isEmpty())
                return null;

            ParentConfigNode mapSection = ParentConfigNode.createRoot();
            for (Map.Entry<?, ?> mapEntry : mapFieldValue.entrySet()) {
                Object key = mapEntry.getKey();
                Object value = mapEntry.getValue();

                String nodeKey = key.toString();

                if ((!(key instanceof String)) && (!key.getClass().isEnum())) {
                    // If the key is not easily converted to a string
                    // then check if we can just let snakeyaml serialize the map.
                    Class<?> valueClass = value.getClass();
                    if (!valueClass.isAnnotationPresent(SerializableConfig.class)
                            && (!(value instanceof Iterable))) {
                        return ValueConfigNode.leaf(obj);
                    }

                    // If not, throw an exception
                    throw new UnsupportedOperationException("Cannot serialize map that does not have a string or enum key");
                }

                ConfigNode valueNode = serializeChild(value);

                if (valueNode != null) {
                    valueNode.setKey(nodeKey);
                    mapSection.addChild(valueNode);
                }
            }

            return mapSection;
        }
        else if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            // Don't serialize empty collections
            if (collection.isEmpty())
                return null;
            Class<?> collectionClass = collection.stream().findFirst().get().getClass();
            // Only specially serialize complex objects
            if (!collectionClass.isAnnotationPresent(SerializableConfig.class))
                return ValueConfigNode.leaf(obj);
            
            List<Map<CommentKey, Object>> serializedList = new ArrayList<>();
            for (Object el : collection) {
                ParentConfigNode objectNode = serializeClass(el);
                if (!objectNode.hasChildren())
                    continue;

                // Serialize the node to a comment key map in order to preserve comments on the serialized object.
                Map<CommentKey, Object> objectMap = new LinkedHashMap<>();
                NodeSerializer.serializeToCommentMap(objectNode, objectMap);

                serializedList.add(objectMap);
            }

            return ValueConfigNode.leaf(serializedList);
        }
        else {
            // Return parent-less, key-less value node
            // This only works because the parent and key are set after.
            return ValueConfigNode.leaf(obj);
        }
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

            ConfigNode newNode = serializeChild(fieldValue);
            if (newNode == null)
                continue;

            newNode.setKey(childName);
            currParent.addChild(newNode);

            Comment comments = field.getAnnotation(Comment.class);

            if (comments != null) {
                String[] commentsArray = comments.value();
                // Merge existing comments list
                if (newNode.hasComments()) {
                    List<String> mergedComments = new ArrayList<>(commentsArray.length + newNode.getComments().length);
                    Collections.addAll(mergedComments, commentsArray);
                    Collections.addAll(mergedComments, newNode.getComments());

                    commentsArray = mergedComments.toArray(new String[0]);
                }

                newNode.setComments(commentsArray);
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
