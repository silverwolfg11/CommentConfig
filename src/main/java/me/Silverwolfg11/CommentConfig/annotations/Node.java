package me.Silverwolfg11.CommentConfig.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represent the path of a YAML node
 * that the field is mapped to during class serialization.
 * <br><br>
 * The last element of the array is the serialized name of the node.
 * Any elements before are considered parent sections.
 * If no parent sections exist, then they will be created.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Node {

    String[] value();
}
