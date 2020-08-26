package me.Silverwolfg11.CommentConfig.hacks;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

public class CommentYAML extends Yaml {

    final KeyRepresenter keyRepresenter;

    public CommentYAML(DumperOptions options) {
        super(new KeyRepresenter(options), options);
        keyRepresenter = (KeyRepresenter) this.representer;
    }

    public void addSerializer(Class<?> clazz, Represent representer) {
        keyRepresenter.addRepresenter(clazz, representer);
    }

    // Override the dump method to use our custom one
    @Override
    public String dumpAll(Iterator<? extends Object> data) {
        StringWriter buffer = new StringWriter();
        this.dumpAll(data, buffer, (Tag)null);
        return buffer.toString();
    }

    private void dumpAll(Iterator<? extends Object> data, Writer output, Tag rootTag) {
        // This dump method is exactly like SnakeYAML's but this method allows us to
        // capture the emitter for use.
        EmitterProxy emitterProxy = new EmitterProxy(output, this.dumperOptions);
        keyRepresenter.setEmitterProxy(emitterProxy);
        Serializer serializer = new Serializer(emitterProxy.getEmitter(), this.resolver, this.dumperOptions, rootTag);

        try {
            serializer.open();

            while(data.hasNext()) {
                Node node = this.representer.represent(data.next());
                serializer.serialize(node);
            }

            serializer.close();
        } catch (IOException var6) {
            throw new YAMLException(var6);
        }
    }



}
