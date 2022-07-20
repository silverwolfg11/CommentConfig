package me.Silverwolfg11.CommentConfig.serialization;

import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FieldDeserializer {

    private final ClassDeserializer classDeserializer;
    private final Class<?> parentClass;
    private final Object parentObject;

    private final Logger errorLogger;

    FieldDeserializer(ClassDeserializer classDeserializer, Logger errorLogger, Class<?> parentClass, Object parentObject) {
        this.classDeserializer = classDeserializer;
        this.errorLogger = errorLogger;
        this.parentClass = parentClass;
        this.parentObject = parentObject;
    }

    public <T> Object deserializeObject(Field field, Object serializedObj, Class<T> objectClass) {
        if (objectClass.isArray() && serializedObj instanceof List) {
            // Since the field is an array, and YAML loads all iterables as lists,
            // we will have to convert it to an array.
            Class<?> pType = objectClass.getComponentType();;
            return convertListToArray(pType, serializedObj);
        }
        else if (objectClass.isEnum() && serializedObj instanceof String) {
            try {
                Class<? extends Enum> enumClass = (Class<? extends Enum>) objectClass;
                return Enum.valueOf(enumClass, (String) serializedObj);
            } catch (IllegalArgumentException ex) {
                String errorMsg = String.format("Couldn't convert '%s' to enum '%s'", serializedObj, objectClass.getName());
                if (field != null)
                    errorMsg += " for field " + field.getName() + "!";

                printError(errorMsg);
                return null;
            }
        }
        else if (field != null && objectClass.isAssignableFrom(Map.class) && (serializedObj instanceof Map)) {
            List<Class<?>> mapClasses = getParameterizedClasses(field);
            if (mapClasses == null || mapClasses.size() != 2) {
                return serializedObj;
            }

            Class<?> keyClass = mapClasses.get(0);
            Class<?> valueClass = mapClasses.get(1);

            if (!keyClass.isEnum() && keyClass.equals(String.class)) {
                if (!valueClass.isAnnotationPresent(SerializableConfig.class)
                        && !valueClass.isAssignableFrom(Iterable.class)) {
                    return serializedObj;
                }
            }

            Map<Object, Object> deserializedMap = (Map<Object, Object>) getFieldInstance(field);
            if (deserializedMap == null)
                deserializedMap = new HashMap<>();

            Map<String, Object> mapObject = (Map<String, Object>) serializedObj;
            for (Map.Entry<String, Object> entry : mapObject.entrySet()) {
                Object key = deserializeObject(null, entry.getKey(), keyClass);
                Object value = deserializeObject(null, entry.getValue(), valueClass);
                deserializedMap.put(key, value);
            }

            return deserializedMap;
        }
        else if (serializedObj != null && !objectClass.isPrimitive() && !objectClass.isInstance(serializedObj)) {
            if (objectClass.isAnnotationPresent(SerializableConfig.class)
                    && (serializedObj instanceof Map)) {

                // Is an inner member class
                if (objectClass.isMemberClass() && !Modifier.isStatic(objectClass.getModifiers())) {
                    if (!objectClass.getEnclosingClass().equals(parentClass)) {
                        throw new RuntimeException("Cannot deserialize inner member class " + objectClass.getName());
                    }

                    return deserializeMemberInstance(objectClass, parentClass, parentObject, (Map<String, Object>) serializedObj);
                }
                else {
                    return classDeserializer.deserializeClass((Map<String, Object>) serializedObj, objectClass);
                }
            }

            if (field != null && !objectClass.isInstance(serializedObj))  {
                printError("Type mismatch on field '%s'!", field.getName());
                printError("Expected field type: %s. Object type found: %s.", field.getType().getName(), serializedObj.getClass().getName());
                return null;
            }
        }

        return serializedObj;
    }

    private Object getFieldInstance(Field field) {
        if (field == null || parentObject == null)
            return null;

        boolean fieldAccessibility = field.isAccessible();
        if (!fieldAccessibility) {
            field.setAccessible(true);
        }
        Object fieldInstance;
        try {
           fieldInstance = field.get(parentObject);
        } catch (IllegalAccessException e) {
            printError("Error trying to get field member '%s' for class '%s'!", field.getName(), parentClass.getName());
            return null;
        }

        if (!fieldAccessibility)
            field.setAccessible(false);

        return fieldInstance;
    }

    private List<Class<?>> getParameterizedClasses(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType paramType = (ParameterizedType) genericType;
        Type[] types = paramType.getActualTypeArguments();

        return Arrays.stream(types)
                .filter(t -> t instanceof Class<?>)
                .map(t -> (Class<?>) t)
                .collect(Collectors.toList());
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

        return classDeserializer.deserializeClass(objectMap, memberClass, memberInstance);
    }

    private void printError(String error, Object... args) {
        String errorMsg = (args != null && args.length > 0) ? String.format(error, args) : error;
        if (errorLogger != null) {
            errorLogger.severe(errorMsg);
        }
        else {
            System.out.println(errorMsg);
        }
    }

}
