package me.Silverwolfg11.CommentConfig.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to represent classes that can be
 * serialized to YAML through the {@link me.Silverwolfg11.CommentConfig.serialization.ClassSerializer}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializableConfig {
}
