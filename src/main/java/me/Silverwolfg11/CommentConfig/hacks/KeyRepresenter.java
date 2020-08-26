package me.Silverwolfg11.CommentConfig.hacks;

import me.Silverwolfg11.CommentConfig.node.CommentKey;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

/**
 * This is the representer that will convert {@link CommentKey} to a {@link CommentScalarNode}
 * via SnakeYAML. Also handles the callback function to add the {@link CommentEvent} to the
 * event queue.
 */
public class KeyRepresenter extends Representer {

    protected EmitterProxy emitterProxy;

    public KeyRepresenter(DumperOptions options) {
        super(options);
        this.representers.put(CommentKey.class, new RepresentNode());
    }

    public void setEmitterProxy(EmitterProxy proxy) {
        this.emitterProxy = proxy;
    }

    public void addRepresenter(Class<?> clazz, Represent representer) {
        this.representers.put(clazz, representer);
    }

    private class RepresentNode implements Represent {

        private void addCommentEvent(String[] comments) {
            KeyRepresenter.this.emitterProxy.getEventQueue().add(new CommentEvent(comments));
        }

        @Override
        public Node representData(Object o) {
            CommentKey obj = (CommentKey) o;
            String val = obj.getKey();

            if (obj.hasComments()) {
                return new CommentScalarNode(Tag.STR, val, null, null, DumperOptions.ScalarStyle.PLAIN)
                        .onFirstCall(() -> addCommentEvent(obj.getComments()));
            }
            else {
                return representScalar(Tag.STR, val);
            }
        }
    }
}
