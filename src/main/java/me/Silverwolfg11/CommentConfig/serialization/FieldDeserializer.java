package me.Silverwolfg11.CommentConfig.serialization;

import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
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

    private boolean isSpeciallyDeserialized(Class<?> clazz) {
        if (clazz == null)
            return false;

        return clazz.isAnnotationPresent(SerializableConfig.class) ||
                clazz.isEnum() ||
                Map.class.isAssignableFrom(clazz) ||
                Collection.class.isAssignableFrom(clazz) ||
                clazz.isArray();
    }

    private Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            return getClassFromType(pType.getRawType());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Object deserializeObject(Field field, Object serializedObj, Type objectType) {
        Class<?> objectClass = getClassFromType(objectType);

        if (objectClass == null)
            return serializedObj;

        if (objectClass.isArray() && serializedObj instanceof List) {
            // Since the field is an array, and YAML loads all iterables as lists,
            // we will have to convert it to an array.
            Class<?> pType = objectClass.getComponentType();
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
        else if (Collection.class.isAssignableFrom(objectClass) && (serializedObj instanceof List)) {
            List<Type> typeParameters = getParameterizedTypes(objectType);
            if (typeParameters == null || typeParameters.size() != 1) {
                return serializedObj;
            }

            Type listParamType = typeParameters.get(0);
            Class<?> listParameter = getClassFromType(listParamType);

            if (!isSpeciallyDeserialized(listParameter)) {
                return serializedObj;
            }


            Collection<Object> deserializedList = null;
            if (field != null) {
                deserializedList = (Collection<Object>) getFieldInstance(field);
            }

            if (deserializedList == null) {
                deserializedList = (Collection<Object>) defaultConstructObject(objectClass);
            }

            // If no default constructor, then can't deserialize
            if (deserializedList == null)
                deserializedList = (Collection<Object>) createCommonInstancesOf(objectClass);

            // Exhausted all options, fast-fail
            if (deserializedList == null)
                return serializedObj;

            // Make sure the deserialized list is empty
            deserializedList.clear();

            List<Object> serializedList = (List<Object>) serializedObj;
            for (Object listEl : serializedList) {
                Object deserializedListEl = deserializeObject(null, listEl, listParamType);
                deserializedList.add(deserializedListEl);
            }

            return deserializedList;
        }
        else if (Map.class.isAssignableFrom(objectClass) && (serializedObj instanceof Map)) {
            List<Type> mapTypes = getParameterizedTypes(objectType);
            if (mapTypes == null || mapTypes.size() != 2) {
                return serializedObj;
            }

            Type keyType = mapTypes.get(0);
            Class<?> keyClass = getClassFromType(keyType);

            Type valueType = mapTypes.get(1);
            Class<?> valueClass = getClassFromType(valueType);

            if (!keyClass.isEnum() && keyClass.equals(String.class)) {
                if (!valueClass.isAnnotationPresent(SerializableConfig.class)
                        && !valueClass.isAssignableFrom(Iterable.class)) {
                    return serializedObj;
                }
            }

            Map<Object, Object> deserializedMap = null;
            if (field != null) {
                deserializedMap = (Map<Object, Object>) getFieldInstance(field);
            }

            if (deserializedMap == null)
                deserializedMap = new HashMap<>();

            deserializedMap.clear();

            Map<String, Object> mapObject = (Map<String, Object>) serializedObj;
            for (Map.Entry<String, Object> entry : mapObject.entrySet()) {
                Object key = deserializeObject(null, entry.getKey(), keyType);
                Object value = deserializeObject(null, entry.getValue(), valueType);
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

    private List<Type> getParameterizedTypes(Type objectType) {
        if (!(objectType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType paramType = (ParameterizedType) objectType;
        Type[] types = paramType.getActualTypeArguments();

        return Arrays.stream(types)
                .collect(Collectors.toList());
    }

    private Object convertListToArray(Class<?> arrayType, Object object) {
        List<?> objList = (List<?>) object;

        Object array = Array.newInstance(arrayType, objList.size());

        for (int i = 0; i < objList.size(); i++) {
            Array.set(array, i, objList.get(i));
        }

        return array;
    }

    private <T> T defaultConstructObject(Class<T> clazz) {
        if (clazz.isInterface())
            return null;

        try {
            Constructor<?> constructor =  clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = (T) constructor.newInstance();
            constructor.setAccessible(false);
            return instance;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Create common instances of some iterable interfaces
    private Collection<?> createCommonInstancesOf(Class<?> clazz) {
        if (clazz == Collection.class || clazz == List.class) {
            return new ArrayList<>();
        }
        else if (clazz == Deque.class) {
            return new ArrayDeque<>();
        }

        return null;
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
