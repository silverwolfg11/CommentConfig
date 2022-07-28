package me.Silverwolfg11.CommentConfig.serialization;

/**
 * Interface to handle deserialization of specific objects from YAML.
 */
public interface DeserializableObject {
    /**
     * Deserialize an object to a specific object.
     *
     * @param o SnakeYAML deserialized object.
     *
     * @return Actual object representation.
     */
    Object deserializeObject(Object o);
}
