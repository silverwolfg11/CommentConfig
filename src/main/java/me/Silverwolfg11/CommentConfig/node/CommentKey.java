package me.Silverwolfg11.CommentConfig.node;

/**
 * This class is the object that the SnakeYAML injectors look for in order
 * to add comments. All comments are added before the key.
 * <br><br>
 * <b>ONLY MEANT FOR INTERNAL USAGE.</b>
 */
public class CommentKey {
    private String[] comments;
    private String key;

    public CommentKey(String key, String[] comments) {
        this.comments = comments;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean hasComments() {
        return comments != null;
    }

    public String[] getComments() {
        return comments;
    }
}
