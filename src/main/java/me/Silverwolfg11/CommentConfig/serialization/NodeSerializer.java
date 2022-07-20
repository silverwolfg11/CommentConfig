package me.Silverwolfg11.CommentConfig.serialization;

import me.Silverwolfg11.CommentConfig.hacks.CommentYAML;
import me.Silverwolfg11.CommentConfig.node.CommentKey;
import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.node.ValueConfigNode;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.representer.Represent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class NodeSerializer {

    private final CommentYAML yaml;

    public NodeSerializer() {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new CommentYAML(options);
    }

    public void addSerializer(Class<?> clazz, Represent representer) {
        yaml.addSerializer(clazz, representer);
    }

    public String serializeToString(ConfigNode node) {
        Map<CommentKey, Object> commentMap = new LinkedHashMap<>();
        serializeToCommentMap(node, commentMap);
        String dump = yaml.dump(commentMap);

        // Handle root comments a.k.a the header
        if (!node.hasKey() && node.hasComments()) {
            StringBuilder builder = new StringBuilder();
            for (String comment : node.getComments()) {
                if (!comment.isEmpty())
                    builder.append("# ").append(comment);

                builder.append("\n");
            }

            dump = builder + dump;
        }

        return dump;
    }


    public void serializeToFile(File file, ConfigNode node) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist!");
        }

        String producedYAML = serializeToString(node);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(producedYAML);
        }
    }

    static void serializeToCommentMap(ConfigNode node, Map<CommentKey, Object> parentMap) {
        Object nodeVal = null;

        if (node instanceof ParentConfigNode) {
            // May be a root node
            ParentConfigNode parentNode = (ParentConfigNode) node;
            if (parentNode.hasChildren()) {
                Map<CommentKey, Object> levelMap;

                if (node.hasKey()) {
                    nodeVal = levelMap = new LinkedHashMap<>();
                }
                // Handle root nodes
                else {
                    levelMap = parentMap;
                }

                for (ConfigNode child : parentNode.getChildren()) {
                    serializeToCommentMap(child, levelMap);
                }
            }
        }
        else if (node instanceof ValueConfigNode) {
            ValueConfigNode vConfig = (ValueConfigNode) node;
            nodeVal = vConfig.getValue();
        }

        if (nodeVal != null && node.hasKey()) {
            CommentKey key = new CommentKey(node.getKey(), node.getComments());
            parentMap.put(key, nodeVal);
        }
    }

}
