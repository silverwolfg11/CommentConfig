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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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

    private <T> T deserializeClass(Map<String, Object> objMap, Class<T> clazz) {
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

    private <T> T deserializeClass(Map<String, Object> objectMap, Class<T> clazz, T objInstance) {
        for (Field field : clazz.getDeclaredFields()) {
            // Skip compiler generated or transient fields
            if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()))
                continue;

            // We need to get the associated object for the field
            Object fieldObject = getFieldObject(objectMap, field);

            if (fieldObject == null)
                continue;

            Class<?> fieldType = field.getType();

            if (deserializers != null && deserializers.containsKey(fieldType)) {
                fieldObject = deserializers.get(fieldType).deserializeObject(fieldObject);
            }

            if (fieldType.isArray() && fieldObject instanceof List) {
                // Since the field is an array, and YAML loads all iterables as lists,
                // we will have to convert it to an array.
                Class<?> pType = fieldType.getComponentType();;
                fieldObject = convertListToArray(pType, fieldObject);
            }
            else if (fieldType.isEnum() && fieldObject instanceof String) {
                try {
                    Class<? extends Enum> enumClass = (Class<? extends Enum>) fieldType;
                    fieldObject = Enum.valueOf(enumClass, (String) fieldObject);
                } catch (IllegalArgumentException ex) {
                    displayError("Could not convert `" + fieldObject + "` to enum " + fieldType.getName() + " for field " + field.getName());
                    continue;
                }
            }
            else if (fieldObject != null && !fieldType.isPrimitive() && !fieldType.isInstance(fieldObject)) {
                if (fieldType.isAnnotationPresent(SerializableConfig.class)
                    && (fieldObject instanceof Map)) {

                    // Is an inner member class
                    if (fieldType.isMemberClass() && !Modifier.isStatic(fieldType.getModifiers())) {
                        if (!fieldType.getEnclosingClass().equals(clazz)) {
                            throw new RuntimeException("Cannot deserialize inner member class " + fieldType.getName());
                        }

                        fieldObject = deserializeMemberInstance(fieldType, clazz, objInstance, (Map<String, Object>) fieldObject);
                    }
                    else {
                        fieldObject = deserializeClass((Map<String, Object>) fieldObject, fieldType);
                    }
                }

                if (!fieldType.isInstance(fieldObject))  {
                    displayError("Type mismatch on field " + field.getName() + "!");
                    displayError("Expected field type: " + field.getType().getName() + ". Object type found: " + fieldObject.getClass().getName());
                    continue;
                }
            }

            // After all the modifications to field object, double check that it's not null
            if (fieldObject == null)
                continue;

            field.setAccessible(true);
            try {
                field.set(objInstance, fieldObject);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            field.setAccessible(false);
        }

        return objInstance;
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

    private Object convertListToArray(Class<?> arrayType, Object object) {
        List objList = (List) object;

        Object array = Array.newInstance(arrayType, objList.size());

        for (int i = 0; i < objList.size(); i++) {
            Array.set(array, i, objList.get(i));
        }

        return array;
    }

    private <T> Object deserializeMemberInstance(Class<T> memberClass, Class<?> enclosingClass, Object enclosingInstance, Map<String, Object> objectMap) {
        T memberInstance;

        try {
            Constructor<?> constructor =  memberClass.getDeclaredConstructor(enclosingClass);
            constructor.setAccessible(true);
            memberInstance = (T) constructor.newInstance(enclosingInstance);
            constructor.setAccessible(false);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }

        return deserializeClass(objectMap, memberClass, memberInstance);
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
