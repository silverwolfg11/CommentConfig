package me.silverwolfg11.commentconfig;

import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.serialization.ClassSerializer;
import me.Silverwolfg11.CommentConfig.serialization.NodeSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test serializing classes to a YAML file.
 */
public class ClassSerializerTests extends ConfigTesting {

    public ClassSerializerTests() {
        super("classserializertests");
    }

    // Serialize an object to a YAML file, and check the generated YAML against an existing YAML.
    private void serializeClassAndCheckDiff(Object obj, String resourceName) {
        ParentConfigNode rootNode = ClassSerializer.serializeClass(obj);
        File serializedFile = serializeToFile(rootNode);
        Path expectedFilePath = getResource(resourceName);
        checkNoDiff(serializedFile.toPath(), expectedFilePath);
    }

    @SerializableConfig
    protected static class SimpleStringClass {
        @Comment("This is a test comment!")
        private String test = "Hello!";
    }

    @Test
    protected void simpleClassSerialization() {
        serializeClassAndCheckDiff(new SimpleStringClass(), "simpleclass_serialization.yml");
    }

    @SerializableConfig
    protected static class SimpleListSerializationClass {
        @Comment("This is a test list!")
        private List<String> list = Arrays.asList("hello", "world!");
    }


    @Test
    protected void simpleListClassSerialization() {
        serializeClassAndCheckDiff(new SimpleListSerializationClass(), "simplelistclass_serialization.yml");
    }

    @SerializableConfig
    protected static class SimpleMapSerializationClass {

        @Comment("This is a serialized map!")
        private Map<String, String> map = new HashMap<>();

        SimpleMapSerializationClass() {
            map.put("hello", "world");
            map.put("the", "cake is a lie");
        }
    }

    @Test
    protected void simpleMapClassSerialization() {
        serializeClassAndCheckDiff(new SimpleMapSerializationClass(), "simplemapclass_serialization.yml");
    }

    @SerializableConfig
    protected static class ComplexMapSerializationClass {

        @SerializableConfig
        private static class Options {
            @Comment("This is a comment on option1!")
            String option1 = "Hello";
            @Comment("This is a comment on option2")
            String option2 = "World";
        }

        @Comment("This is a complex serialized map!")
        private Map<String, Options> map = new HashMap<>();

        ComplexMapSerializationClass() {
            map.put("hello", new Options());
            map.put("world", new Options());
        }
    }

    @Test
    protected void complexMapSerialization() {
        ParentConfigNode rootNode = ClassSerializer.serializeClass(new ComplexMapSerializationClass());

        File tempFile = getTempFile();
        Assertions.assertDoesNotThrow(tempFile::createNewFile, "Error creating temporary file!");
        
        NodeSerializer serializer = new NodeSerializer();
        Assertions.assertDoesNotThrow( () -> serializer.serializeToFile(tempFile, rootNode) );
        checkNoDiff(tempFile.toPath(), getResource("complexmapclass_serialization.yml"));
    }

    @SerializableConfig
    @Comment({"This is a multi-line", "header comment!"})
    protected static class ComplexSerializationClass {
        @Comment("This is a test integer!")
        private int test = 10;

        @Comment("This is a renamed node's list!")
        @Node("list")
        private List<String> testList = Arrays.asList("hello", "world!");

        @SerializableConfig
        private static class TestOptions {
            int option1 = 10;
            boolean option2 = false;
        }

        @Comment("This is an inner options class!")
        private TestOptions options = new TestOptions();
    }

    @Test
    protected void complexClassSerialization() {
        serializeClassAndCheckDiff(new ComplexSerializationClass(), "complexclass_serialization.yml");
    }

}
