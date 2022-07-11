package me.silverwolfg11.commentconfig;

import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.node.ValueConfigNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

/**
 * Test serializing config nodes into a YAML file.
 */
public class NodeSerializerTests extends ConfigTesting {

    public NodeSerializerTests() {
        super("nodeserializertests");
    }

    // Serialize a node to YAML, and check the generated YAML against a resource YAML.
    private void serializeNodeAndCheckDiff(ParentConfigNode rootNode, String resourceName) {
        File serializedFile = serializeToFile(rootNode);
        Path expectedFile = getResource(resourceName);
        Assertions.assertNotNull(expectedFile, "Error getting resource file '" + resourceName + "'!");

        checkNoDiff(serializedFile.toPath(), expectedFile);
    }

    @Test
    public void noCommentSerializationTest() {
        ParentConfigNode rootNode = ParentConfigNode.createRoot();
        rootNode.addChild("test", "Hello!");

        serializeNodeAndCheckDiff(rootNode, "string_serialization.yml");
    }

    @Test
    public void stringCommentSerializationTest() {
        ParentConfigNode rootNode = ParentConfigNode.createRoot();
        rootNode.addChild("test", "Hello!");
        rootNode.setComments("This is a test comment!");

        serializeNodeAndCheckDiff(rootNode, "string_comment_serialization.yml");
    }

    @Test
    public void multiChildrenStringTest() {
        ParentConfigNode rootNode = ParentConfigNode.createRoot();

        ConfigNode child1 = rootNode.addChild("test", "Hello!");
        child1.setComments("This is a test comment!");

        ConfigNode child2 = rootNode.addChild("test2", "World");
        child2.setComments("This is another test comment!");

        ConfigNode child3 = rootNode.addChild("test3", "The last one!");
        child3.setComments("This is the final test comment!");

        serializeNodeAndCheckDiff(rootNode, "multi_string_comment_serialization.yml");
    }

    @Test
    public void multiLevelChildrenTest() {
        ParentConfigNode rootNode = ParentConfigNode.createRoot();
        rootNode.setComments("This is a header comment!");

        ValueConfigNode child1 = rootNode.addChild("test", "Hello");
        child1.setComments("This is the first child comment!");

        ParentConfigNode parent1 = rootNode.addSection("testSection");
        parent1.setComments("This is the first section comment!");

        ValueConfigNode child2 = parent1.addChild("test", "Hello");
        child2.setComments("This is the second child comment!");

        ParentConfigNode parent2 = parent1.addSection("testSection");
        parent2.setComments("This is the second section comment!");

        ValueConfigNode child3 = parent1.addChild("test2", "World");
        child3.setComments("This is the third child comment!");

        ValueConfigNode child4 = parent2.addChild("test", "Hello");
        child4.setComments("This is the fourth child comment!");

        ValueConfigNode child5 = rootNode.addChild("test2", "World");
        child5.setComments("This is the fifth child comment!");

        serializeNodeAndCheckDiff(rootNode, "multi_level_comment_serialization.yml");
    }

}
