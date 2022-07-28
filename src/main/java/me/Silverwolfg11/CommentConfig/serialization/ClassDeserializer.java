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
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A deserializer to deserialize YAML to an instance of a class.
 */
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

    /**
     * Add a deserializer to handle deserializing a specific class type.
     *
     * @param clazz Class type that this deserializer handles.
     *              Class type <b>cannot</b> be {@code null}.
     * @param deserializable Deserializer interface.
     *                       Interface <b>cannot</b> be {@code null}.
     */
    public void addDeserializer(Class<?> clazz, DeserializableObject deserializable) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(deserializable);

        if (deserializers == null)
            deserializers = new HashMap<>();

        deserializers.put(clazz, deserializable);
    }

    /**
     * Set the error logger that the class deserializer
     * will use to log error messages.
     *
     * @param logger Logger to use for errors.
     */
    public void setErrorLogger(Logger logger) {
        this.errorLogger = logger;
    }

    private void validateSerializable(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(SerializableConfig.class)) {
            throw new RuntimeException("Class must be annotated with `SerializableConfig`!");
        }
    }

    /**
     * Deserialize a YAML file to a class.
     * The class must have the {@link SerializableConfig} annotation,
     * and a default constructor.
     * <br><br>
     * <b>None of the arguments can be {@code null}.</b>
     *
     * @param file YAML file to read from.
     * @param clazz Class to deserialize to.
     *
     * @return the deserialized object instance.
     *
     * @param <T> Type to deserialize to.
     * @throws IOException if there is an error reading the file.
     */
    public <T> T deserializeClass(File file, Class<T> clazz) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(clazz);

        try (FileInputStream stream = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return deserializeClass(isr, clazz);
        }
    }

    /**
     * Deserialize an object instance using a class from a YAML file, but update
     * the YAML file if it does not match the current config version.
     * The class must have a {@link SerializableConfig} annotation and a default constructor.
     * In order to check the version, the class must also have the {@link ConfigVersion} annotation.
     * <br><br>
     * <b>None of the arguments can be {@code null}.</b>
     *
     * @param file YAML file to read from.
     * @param clazz Class to deserializer to.
     * @param reserializer NodeSerializer instance to serialize the config if the config is out of date.
     *
     * @return an instance of the deserialized class.
     * @param <T> Class type to deserialize to.
     * @throws IOException if there is an error reading the file or writing to the file.
     */
    public <T> T deserializeClassAndUpdate(File file, Class<T> clazz, NodeSerializer reserializer) throws IOException {
        validateSerializable(Objects.requireNonNull(clazz));
        boolean reSaveConfig = false;
        T deserializedClass;
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
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
            Objects.requireNonNull(reserializer).serializeToFile(file, serializedNode);
        }

        return deserializedClass;
    }


    /**
     * Deserialize a YAML input stream to a class.
     * The class must have the {@link SerializableConfig} annotation,
     * and a default constructor.
     * <br><br>
     * <b>None of the arguments can be {@code null}.</b>
     *
     * @param reader Reader to read YAML file from.
     * @param clazz Class to deserialize to.
     *
     * @return the deserialized object instance.
     *
     * @param <T> Type to deserialize to.
     */
    public <T> T deserializeClass(InputStreamReader reader, Class<T> clazz) {
        Objects.requireNonNull(reader);
        Objects.requireNonNull(clazz);

        validateSerializable(clazz);
        Map<String, Object> objectMap = yaml.load(reader);
        return deserializeClass(objectMap, clazz);
    }

    /**
     * Deserialize a YAML string to a class instance.
     * The class must have the {@link SerializableConfig} annotation,
     * and a default constructor.
     * <br><br>
     * <b>None of the arguments can be {@code null}.</b>
     *
     * @param producedYaml YAML string to use.
     * @param clazz Class to deserialize to.
     *
     * @return the deserialized object instance.
     *
     * @param <T> Type to deserialize to.
     */
    public <T> T deserializeClass(String producedYaml, Class<T> clazz) {
        Objects.requireNonNull(producedYaml);
        Objects.requireNonNull(clazz);

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

            Class<?> fieldClass = field.getClass();

            if (deserializers != null && deserializers.containsKey(fieldClass)) {
                serializedObject = deserializers.get(fieldClass).deserializeObject(serializedObject);
            }

            serializedObject = fieldDeserializer.deserializeObject(field, serializedObject, field.getGenericType());

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
