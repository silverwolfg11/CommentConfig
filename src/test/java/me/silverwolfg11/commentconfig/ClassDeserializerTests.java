package me.silverwolfg11.commentconfig;

import me.Silverwolfg11.CommentConfig.annotations.Comment;
import me.Silverwolfg11.CommentConfig.annotations.Node;
import me.Silverwolfg11.CommentConfig.annotations.SerializableConfig;
import me.Silverwolfg11.CommentConfig.serialization.ClassDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Test deserializing YML files based on a given class.
 *
 * Test YML files and classes match those in the ClassSerializerTests
 */
public class ClassDeserializerTests extends ConfigTesting {

    public ClassDeserializerTests() {
        super("classdeserializertests");
    }

    private <T> T deserializeClassFromFile(String resourceName, Class<T> clazz) {
        ClassDeserializer deserializer = new ClassDeserializer();
        File generatedFile = getResourceFile(resourceName);
        return Assertions.assertDoesNotThrow(
                () -> deserializer.deserializeClass(generatedFile, clazz)
        );
    }

    @SerializableConfig
    protected static class SimpleStringClass {
        private String test;
    }

    @SerializableConfig
    protected static class SimpleListClass {
        private List<String> list;
    }

    @SerializableConfig
    protected static class SimpleMapClass {
        private Map<String, String> map;
    }

    @SerializableConfig
    @Comment({"This is a multi-line", "header comment!"})
    protected static class ComplexClass {
        private int test;

        @Node("list")
        private List<String> testList;

        @SerializableConfig
        private static class TestOptions {
            int option1;
            boolean option2;
        }

        private TestOptions options;
    }

    // Test deserializing a simple class with a string member.
    @Test
    protected void simpleClassDeserialization() {
        SimpleStringClass ssc = deserializeClassFromFile("simpleclass_deserialization.yml", SimpleStringClass.class);

        Assertions.assertEquals(ssc.test, "Hello!");
    }

    // Test deserializing a simple class with a list member.
    @Test
    protected void simpleListDeserialization() {
        SimpleListClass slc = deserializeClassFromFile("simplelistclass_deserialization.yml", SimpleListClass.class);

        Assertions.assertEquals(slc.list.size(), 2);
        Assertions.assertEquals(slc.list.get(0), "hello");
        Assertions.assertEquals(slc.list.get(1), "world!");
    }

    // Test deserializing a simple class with a map member.
    @Test
    protected void simpleMapDeserialization() {
        SimpleMapClass smc = deserializeClassFromFile("simplemapclass_deserialization.yml", SimpleMapClass.class);

        Assertions.assertEquals(smc.map.size(), 2);
        Assertions.assertEquals(smc.map.get("the"), "cake is a lie");
        Assertions.assertEquals(smc.map.get("hello"), "world");
    }

    // Test deserializing a complex class with multiple members.
    @Test
    protected void complexClassDeserialization() {
        ComplexClass cc = deserializeClassFromFile("complexclass_deserialization.yml", ComplexClass.class);

        Assertions.assertEquals(cc.test, 10);

        Assertions.assertEquals(cc.testList.size(), 2);
        Assertions.assertEquals(cc.testList.get(0), "hello");
        Assertions.assertEquals(cc.testList.get(1), "world!");

        Assertions.assertNotNull(cc.options);
        Assertions.assertEquals(cc.options.option1, 10);
        Assertions.assertFalse(cc.options.option2);
    }

}
