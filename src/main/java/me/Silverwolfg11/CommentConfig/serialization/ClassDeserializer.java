package me.Silverwolfg11.CommentConfig.serialization;

import me.Silverwolfg11.CommentConfig.annotations.ConfigVersion;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ClassDeserializer {

    private final Yaml yaml;
    private Map<Class<?>, DeserializableObject> deserializers;
    private Logger errorLogger;

    public ClassDeserializer() {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public void addDeserializer(Class<?> clazz, DeserializableObject deserializable) {
        if (deserializers == null)
            deserializers = new HashMap<>();

        deserializers.put(clazz, deserializable);
    }

    public void setErrorLogger(Logger logger) {
        this.errorLogger = logger;
    }

    private void validateSerializable(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(SerializableConfig.class)) {
            throw new RuntimeException("Class must be annotated with `SerializableConfig`!");
        }
    }

    public <T> T deserializeClass(File file, Class<T> clazz) throws IOException {
        try (FileInputStream stream = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return deserializeClass(isr, clazz);
        }
    }

    public <T> T deserializeClassAndUpdate(File file, Class<T> clazz, NodeSerializer reserializer) throws IOException {
        validateSerializable(clazz);
        boolean reSaveConfig = false;
        T deserializedClass;
        try (FileInputStream stream = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, Object> objectMap = yaml.load(isr);

            if (clazz.isAnnotationPresent(ConfigVersion.class)) {
                double latestVersion = clazz.getAnnotation(ConfigVersion.class).value();

                if ((double) objectMap.getOrDefault("config-version", latestVersion + 1) < latestVersion) {
                    reSaveConfig = true;
                }
            }

            deserializedClass = deserializeClass(objectMap, clazz);
        }

        if (reSaveConfig && deserializedClass != null) {
            ConfigNode serializedNode = ClassSerializer.serializeClass(deserializedClass);
            reserializer.serializeToFile(file, serializedNode);
        }

        return deserializedClass;
    }

    public <T> T deserializeClass(InputStreamReader reader, Class<T> clazz) {
        validateSerializable(clazz);
        Map<String, Object> objectMap = yaml.load(reader);
        return deserializeClass(objectMap, clazz);
    }

    public <T> T deserializeClass(String producedYaml, Class<T> clazz) {
        validateSerializable(clazz);
        Map<String, Object> objectMap = yaml.load(producedYaml);
        return deserializeClass(objectMap, clazz);
    }

    <T> T deserializeClass(Map<String, Object> objMap, Class<T> clazz) {
        T objInstance;
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            objInstance = constructor.newInstance();
            constructor.setAccessible(false);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }

        return deserializeClass(objMap, clazz, objInstance);
    }

    <T> T deserializeClass(Map<String, Object> serializedMap, Class<T> clazz, T clazzInstance) {
        FieldDeserializer fieldDeserializer = new FieldDeserializer(this, errorLogger, clazz, clazzInstance);
        for (Field field : clazz.getDeclaredFields()) {
            // Skip compiler generated or transient fields
            if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()))
                continue;

            // We need to get the associated serialized object for the field
            Object serializedObject = getFieldObject(serializedMap, field);

            if (serializedObject == null)
                continue;

            Class<?> fieldType = field.getType();

            if (deserializers != null && deserializers.containsKey(fieldType)) {
                serializedObject = deserializers.get(fieldType).deserializeObject(serializedObject);
            }

            serializedObject = fieldDeserializer.deserializeObject(field, serializedObject, fieldType);

            // After all the modifications to field object, double check that it's not null
            if (serializedObject == null)
                continue;

            field.setAccessible(true);
            try {
                field.set(clazzInstance, serializedObject);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            field.setAccessible(false);
        }

        return clazzInstance;
    }

    private Object getFieldObject(Map<String, Object> objectMap, Field field) {
        if (field.isAnnotationPresent(Node.class)) {
            Node nodeAnnotation = field.getAnnotation(Node.class);
            String[] nodePath = nodeAnnotation.value();

            Map<String, Object> currentMap = objectMap;
            for (int i = 0; i < nodePath.length - 1; ++i) {
                Object currMapObj = currentMap.get(nodePath[i]);

                if (currMapObj instanceof Map) {
                    currentMap = (Map<String, Object>) currMapObj;
                }
                else {
                    return null;
                }
            }

            return currentMap.get(nodePath[nodePath.length - 1]);
        }
        else {
            return objectMap.get(field.getName());
        }
    }

    private void displayError(String errorMessage) {
        if (errorLogger != null) {
            errorLogger.severe(errorMessage);
        }
        else {
            System.out.println(errorMessage);
        }
    }

}
