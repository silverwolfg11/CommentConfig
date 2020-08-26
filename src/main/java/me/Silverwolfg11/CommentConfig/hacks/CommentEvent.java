package me.Silverwolfg11.CommentConfig.hacks;

import org.yaml.snakeyaml.events.Event;

/**
 * A mock event that is nothing but a placeholder in SnakeYAML's event queue.
 * When this event gets polled, our injected modified Queue will then write the comments
 * specified to the stream writer.
 *
 * This event is injected into the event queue by {@link CommentScalarNode}.
 */
public final class CommentEvent extends Event {

    protected String[] comment;

    public CommentEvent(String... comments) {
        super(null, null);
        this.comment = comments;
    }

    public boolean hasComments() {
        return comment != null;
    }

    public String[] getComments() {
        return comment;
    }

    // Doesn't really matter what event
    @Override
    public ID getEventId() {
        return ID.Scalar;
    }
}
