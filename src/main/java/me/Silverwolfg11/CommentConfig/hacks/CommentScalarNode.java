package me.Silverwolfg11.CommentConfig.hacks;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * This is a modified scalar node that will
 * run some code when node value is fetched for the first time.
 *
 * The node value is fetched right before the mapping event is added to the SnakeYAML event queue,
 * thus a {@link CommentEvent} can be injected right before the mapping event to indicate that comments
 * should be added before this scalar key is written to the output stream.
 */
public class CommentScalarNode extends ScalarNode {
    private boolean firstCall = true;
    protected Runnable run;

    public CommentScalarNode(Tag tag, String value, Mark startMark, Mark endMark, DumperOptions.ScalarStyle style) {
        super(tag, value, startMark, endMark, style);
    }

    public CommentScalarNode onFirstCall(Runnable run) {
        this.run = run;
        return this;
    }

    @Override
    public String getValue() {
        // Only run the function the first time getValue() is called
        if (firstCall) {
            // Insert the mark event
            if (run != null) {
                run.run();
            }
            firstCall = false;
        }
        return super.getValue();
    }
}
