package me.Silverwolfg11.CommentConfig.serialization;

import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDeserializer {

    private final Yaml yaml;
    private Map<Class<?>, SerializableObject> deserializers;

    public ClassDeserializer() {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public void addDeserializer(Class<?> clazz, SerializableObject deserializable) {
        if (deserializers == null)
            deserializers = new HashMap<>();

        deserializers.put(clazz, deserializable);
    }

    private void validateSerializable(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(SerializableConfig.class)) {
            throw new RuntimeException("Class must be annotated with `SerializableConfig`!");
        }
    }

    public <T> T deserializeClass(File file, Class<T> clazz) throws FileNotFoundException {
        final FileInputStream stream = new FileInputStream(file);
        return deserializeClass(new InputStreamReader(stream, StandardCharsets.UTF_8), clazz);
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
            objInstance = clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            for (Constructor<?> declaredConstructor : clazz.getDeclaredConstructors()) {
                System.out.println("Constructor: " + declaredConstructor);
            }

            e.printStackTrace();
            return null;
        }

        return deserializeClass(objMap, clazz, objInstance);
    }

    private <T> T deserializeClass(Map<String, Object> objectMap, Class<T> clazz, T objInstance) {
        System.out.println("Deserializing Class " + clazz.getName());

        for (Field field : clazz.getDeclaredFields()) {
            // Skip compiler generated or transient fields
            if (field.isSynthetic() || Modifier.isTransient(field.getModifiers()))
                continue;

            // We need to get the associated object for the field
            Object fieldObject = getFieldObject(objectMap, field);

            if (fieldObject == null)
                continue;

            Class<?> fieldType = field.getType();

            if (fieldType.isArray()) {
                // Since the field is an array, and YAML loads all iterables as lists,
                // we will have to convert it to an array.
                Class<?> pType = fieldType.getComponentType();;

                if (pType.isPrimitive()) {
                    fieldObject = convertListToPrimitiveArray(fieldObject, pType);
                }
                else {
                    fieldObject = convertListToArray(fieldObject);
                }
            }
            else if (!fieldType.isPrimitive() && !fieldType.isInstance(fieldObject)) {
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
                else {
                    if (deserializers != null && deserializers.containsKey(fieldType)) {
                        fieldObject = deserializers.get(fieldType).deserializeObject(fieldObject);
                    }
                    else {
                        System.out.println("Mismatch on field " + field.getName() + "!!!");
                        System.out.println("FIeld Type: " + field.getType().getName() + ". Object Type: " + fieldObject.getClass().getName());
                        continue;
                    }
                }
            }

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

                try {
                    currentMap = (Map<String, Object>) currMapObj;
                } catch (Throwable e) {
                    return null;
                }
            }

            return currentMap.get(nodePath[nodePath.length - 1]);
        }
        else {
            return objectMap.get(field.getName());
        }
    }

    private Object convertListToArray(Object object) {
        if (!(object instanceof List))
            throw new RuntimeException("Not a list!!!");

        return ((List) object).toArray();
    }

    private Object convertListToPrimitiveArray(Object object, Class<?> type) {
        if (!(object instanceof List))
            throw new RuntimeException("Not a list!!!");

        List objList = (List) object;

        Object array = Array.newInstance(type, objList.size());

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

}
