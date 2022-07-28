package me.Silverwolfg11.CommentConfig.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that represents the current version
 * of the serialized class.
 * <br><br>
 * Used to update the config during serialization.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigVersion {
    double value();
}
