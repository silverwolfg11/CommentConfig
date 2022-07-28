package me.Silverwolfg11.CommentConfig.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represent that a field should be natively serialized by SnakeYAML.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SnakeSerialize {
}
