package me.Silverwolfg11.CommentConfig.hacks;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class EmitterProxy {

    protected final Queue<Event> eventQueue;
    protected final Emitter emitter;
    protected Writer writer;
    protected final char[] bestLineBreak;
    protected boolean firstLine = true;

    // Reflection
    private Field indent;

    public EmitterProxy(Writer writer, DumperOptions options) {
        this.emitter = new Emitter(writer, options);
        this.eventQueue = new CheckedABQ<>(100);
        this.writer = writer;
        this.bestLineBreak = options.getLineBreak().getString().toCharArray();
        setIndentField();
        insertCustomQueue();
    }

    private void setIndentField() {
        try {
            indent = emitter.getClass().getDeclaredField("indent");
            indent.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    private int getIndent() {
        try {
            return (int) indent.get(emitter);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private int getColumn() {
        try {
            Field column = emitter.getClass().getDeclaredField("column");
            column.setAccessible(true);
            int col = (int) column.get(emitter);
            column.setAccessible(false);
            return col;
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }

        return -1;
    }

    private void insertCustomQueue() {
        try {
            Field field = emitter.getClass().getDeclaredField("events");
            field.setAccessible(true);
            field.set(emitter, eventQueue);
            field.setAccessible(false);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public Emitter getEmitter() {
        return emitter;
    }

    public Queue<Event> getEventQueue() {
        return eventQueue;
    }

    public Writer getWriter() {
        return writer;
    }

    private void writeNewLine() {
        try {
            writer.write(bestLineBreak);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCharArray(char... chars) {
        try {
            writer.write(chars);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private char[] buildIndentation(int length) {
        char[] data = new char[length];
        if (length > 0)
            Arrays.fill(data, ' ');
        return data;
    }

    private enum CommentLocation {
        IN_SEQUENCE,
        IN_MAP
    }

    private void writeComments(String[] comments, CommentLocation commentLoc) {
        if (comments == null || comments.length == 0)
            return;

        int indentation = getIndent();
        // By default, always add a newline at the beginning
        // because a scalar value will also prepend a newline.
        boolean putNewlineAtStart = true;
        boolean putNewlineAtEnd = false;
        int newLineIndent = 0;

        // Add a newline to the end.
        // if the writer has not been written to yet.
        if (firstLine) {
            int col = getColumn();
            if (col == 0) {
                putNewlineAtStart = false;
                putNewlineAtEnd = true;
            }

            firstLine = false;
        }

        // Add a newline only to the end
        // if writing comments directly in a sequence (YAML sequence).
        if (commentLoc == CommentLocation.IN_SEQUENCE) {
            putNewlineAtStart = false;
            putNewlineAtEnd = true;
            // Correctly indent the scalar after the comment.
            newLineIndent = indentation - 1;
            // Add the comment directly next to the "-" character of the sequence.
            indentation = 1;
        }

        if (putNewlineAtStart)
            writeNewLine();

        char[] indentArray = buildIndentation(indentation);
        for (int i = 0; i < comments.length; i++) {
            // Add a newline starting from the second line of comments.
            if (i > 0)
                writeNewLine();

            String comment = comments[i];
            try {
                if (indentArray.length > 0)
                    writeCharArray(indentArray);

                // Prevent comments with newlines from getting written
                if (comment.indexOf('\n') != -1)
                    comment = comment.substring(0, comment.indexOf('\n'));

                if (!comment.isEmpty()) {
                    if (comment.charAt(0) != '#')
                        comment = "# " + comment;

                    EmitterProxy.this.writer.append(comment);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (putNewlineAtEnd)
            writeNewLine();

        if (newLineIndent > 0) {
            writeCharArray(buildIndentation(newLineIndent));
        }
    }

    private enum CollectionType {
        MAPPING,
        SEQUENCE
    }

    /**
     * A class that extends the ArrayBlockingQueue to be injected into the Emitter field.
     * This modifies the poll method to check polls for CommentEvents and
     * write the comment to the output.
     */
    private class CheckedABQ<T> extends ArrayBlockingQueue<T> {

        private CheckedABQ(int capacity) {
            super(capacity);
        }

        // Use the collection stack to indicate whether
        // an element is being written within a sequence
        // or a map.
        private final Deque<CollectionType> collectionStack = new ArrayDeque<>();

        private CollectionType secondElementInStack() {
            if (collectionStack.size() < 2)
                return null;

            Iterator<CollectionType> stackIter = collectionStack.iterator();
            stackIter.next(); // First element

            return stackIter.next(); // Second Element
        }

        private CommentLocation getCommentLocation() {
            if (collectionStack.isEmpty()) {
                return null;
            }

            // Comment location determined by this algorithm:
            // If the last start event was for a mapping,
            // (since comment events happen after scalar mapping events)
            // and the second to last event was a sequence, then
            // the comment is being written is a sequence. Otherwise,
            // it's being written in a normal mapping.
            if (collectionStack.peek() == CollectionType.MAPPING) {
                CollectionType secondEl = secondElementInStack();
                if (secondEl == null || secondEl == CollectionType.MAPPING)
                    return CommentLocation.IN_MAP;
                else if (secondEl == CollectionType.SEQUENCE) {
                    return CommentLocation.IN_SEQUENCE;
                }
            }

            return CommentLocation.IN_SEQUENCE;
        }

        @Override
        public T poll() {
            T el = super.poll();

            if (el instanceof CommentEvent) {
                CommentEvent commentEvent = (CommentEvent) el;
                // Don't let the normal emitter process the comment event.
                el = super.poll();

                if (commentEvent.hasComments()) {
                    writeComments(commentEvent.getComments(), getCommentLocation());
                }
            }

            // Keep track of whether we're in a sequence or in a map.
            if (el instanceof MappingStartEvent) {
                collectionStack.push(CollectionType.MAPPING);
            }
            else if (el instanceof SequenceStartEvent) {
                collectionStack.push(CollectionType.SEQUENCE);
            }
            else if (el instanceof CollectionEndEvent) {
                collectionStack.pop();
            }

            return el;
        }
    }

}
