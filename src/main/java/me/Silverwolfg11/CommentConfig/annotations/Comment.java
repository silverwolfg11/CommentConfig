package me.Silverwolfg11.CommentConfig.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Comment annotation that should be placed on fields
 * for adding comments during class serialization.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {

    String[] value();

}
