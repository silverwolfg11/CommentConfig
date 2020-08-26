package me.Silverwolfg11.CommentConfig.serialization;

import org.yaml.snakeyaml.representer.Represent;

public interface SerializableObject extends Represent {
    Object deserializeObject(Object o);
}
