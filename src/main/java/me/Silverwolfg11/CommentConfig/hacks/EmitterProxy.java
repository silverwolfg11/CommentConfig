package me.Silverwolfg11.CommentConfig.hacks;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.Event;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
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
        Arrays.fill(data, ' ');
        return data;
    }

    private void writeComments(String[] comments) {
        int indentation = getIndent();
        char[] indentArray = buildIndentation(indentation);
        boolean writeNewLine = true;

        // Check if the writer has been written to already.
        // Otherwise we will have to add a new line after the first comment
        if (firstLine) {
            int col = getColumn();
            if (col == 0)
                writeNewLine = false;

            firstLine = false;
        }

        for (String comment : comments) {
            try {
                if (writeNewLine)
                    writeNewLine();

                writeCharArray(indentArray);
                if (!comment.isEmpty())
                    EmitterProxy.this.writer.append("# ").append(comment);

                if (!writeNewLine) {
                    writeNewLine();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        @Override
        public T poll() {
            T el = super.poll();

            if (el instanceof CommentEvent) {
                CommentEvent commentEvent = (CommentEvent) el;
                if (commentEvent.hasComments()) {
                    writeComments(commentEvent.getComments());
                }

                el = super.poll();
            }
            return el;
        }
    }

}
