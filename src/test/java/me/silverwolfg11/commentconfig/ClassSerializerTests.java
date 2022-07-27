package me.silverwolfg11.commentconfig;

import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.annotations.SnakeSerialize;
import me.Silverwolfg11.CommentConfig.node.ParentConfigNode;
import me.Silverwolfg11.CommentConfig.serialization.ClassSerializer;
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
    protected static class EnumClass {
        private enum TestEnum {
            TEST1,
            TEST2,
            TEST3
        }

        @Comment("This is a test comment!")
        private TestEnum test = TestEnum.TEST1;
    }

    @Test
    protected void enumClassSerialization() {
        serializeClassAndCheckDiff(new EnumClass(), "enumclass_serialization.yml");
    }

    @SerializableConfig
    protected static class RawEnumClass {
        private enum TestEnum {
            TEST1,
            TEST2,
            TEST3
        }

        @Comment("This is a test comment!")
        @SnakeSerialize
        private TestEnum test = TestEnum.TEST1;
    }

    @Test
    protected void rawEnumClassSerialization() {
        serializeClassAndCheckDiff(new RawEnumClass(), "rawenumclass_serialization.yml");
    }

    @SerializableConfig
    protected static class RawListSerializationClass {
        @Comment("This is a test list!")
        private List<String> list = Arrays.asList("hello", "world!");
    }

    @Test
    protected void rawListClassSerialization() {
        serializeClassAndCheckDiff(new RawListSerializationClass(), "rawlistclass_serialization.yml");
    }

    @SerializableConfig
    protected static class SimpleListSerializationClass {
        @SerializableConfig
        private static class TestClass {
            @Comment("This is a test string!")
            private String test = "TestStr";
        }

        @Comment("This is a test list!")
        private List<TestClass> list = Arrays.asList(new TestClass(), new TestClass());
    }


    @Test
    protected void simpleListClassSerialization() {
        serializeClassAndCheckDiff(new SimpleListSerializationClass(), "simplelistclass_serialization.yml");
    }

    @SerializableConfig
    protected static class EnumListClass {
        private enum TestEnum {
            TEST1,
            TEST2,
            TEST3
        }

        @Comment("This is a test list!")
        private List<TestEnum> list = Arrays.asList(TestEnum.TEST1, TestEnum.TEST2, TestEnum.TEST3);
    }


    @Test
    protected void enumListClassSerialization() {
        serializeClassAndCheckDiff(new EnumListClass(), "enumlistclass_serialization.yml");
    }

    @SerializableConfig
    protected static class ComplexListSerializationClass {

        @SerializableConfig
        private static class Types {
            @Comment("A special type")
            private String type = "TYPE1";
        }

        @SerializableConfig
        private static class Options {
            @Comment("This is a list in a list!")
            List<String> options = Arrays.asList("option1", "option2", "option3");
            Types types = new Types();
        }

        @Comment("This is a complex list!")
        private List<List<Options>> list = Arrays.asList(
                Arrays.asList(new Options(), new Options()),
                Arrays.asList(new Options(), new Options())
        );
    }


    @Test
    protected void complexListClassSerialization() {
        serializeClassAndCheckDiff(new ComplexListSerializationClass(), "complexlistclass_serialization.yml");
    }

    @SerializableConfig
    protected static class RawMapSerializationClass {

        @Comment("This is a serialized map!")
        private Map<String, String> map = new HashMap<>();

        RawMapSerializationClass() {
            map.put("hello", "world");
            map.put("the", "cake is a lie");
        }
    }

    @Test
    protected void rawMapClassSerialization() {
        serializeClassAndCheckDiff(new RawMapSerializationClass(), "rawmapclass_serialization.yml");
    }

    @SerializableConfig
    protected static class SimpleMapSerializationClass {

        @SerializableConfig
        private static class Options {
            @Comment("This is a comment on option1!")
            String option1 = "Hello";
            @Comment("This is a comment on option2")
            String option2 = "World";
        }

        @Comment("This is a simple serialized map!")
        private Map<String, Options> map = new HashMap<>();

        SimpleMapSerializationClass() {
            map.put("hello", new Options());
            map.put("world", new Options());
        }
    }

    @Test
    protected void simpleMapSerialization() {
        serializeClassAndCheckDiff(new SimpleMapSerializationClass(), "simplemapclass_serialization.yml");
    }

    @SerializableConfig
    protected static class ComplexMapSerializationClass {

        @SerializableConfig
        @Comment("Test root comment!")
        private static class Options {
            @Comment("This is a comment on option1!")
            Map<TestEnum2, Integer> option1 = new HashMap<>();
            @Comment("This is a comment on option2")
            String option2 = "World";

            private Options() {
                option1.put(TestEnum2.TEST21, 1);
                option1.put(TestEnum2.TEST22, 2);
                option1.put(TestEnum2.TEST23, 3);
            }

        }

        private enum TestEnum1 {
            TEST11,
            TEST12,
            TEST13
        }

        private enum TestEnum2 {
            TEST21,
            TEST22,
            TEST23
        }

        @Comment("This is a complex serialized map!")
        private Map<TestEnum1, Options> map = new HashMap<>();

        ComplexMapSerializationClass() {
            map.put(TestEnum1.TEST11, new Options());
            map.put(TestEnum1.TEST13, new Options());
        }
    }

    @Test
    protected void complexMapSerialization() {
        serializeClassAndCheckDiff(new ComplexMapSerializationClass(), "complexmapclass_serialization.yml");
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
