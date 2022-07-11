package me.silverwolfg11.commentconfig;

import me.Silverwolfg11.CommentConfig.node.ConfigNode;
import me.Silverwolfg11.CommentConfig.serialization.NodeSerializer;
import me.silverwolfg11.commentconfig.util.YamlDiffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class ConfigTesting {
    @TempDir
    Path tempDir;

    protected final String resourceFolder;

    public ConfigTesting(String resourceFolder) {
        this.resourceFolder = resourceFolder;
    }

    protected File getResourceFile(String resourceName) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceURL = classLoader.getResource(resourceFolder + "/" + resourceName);
        if (resourceURL == null)
            return null;

        return new File(resourceURL.getFile());
    }

    protected Path getResource(String resourceName) {
        File file = getResourceFile(resourceName);
        return file != null ? file.toPath() : null;
    }

    protected File getTempFile() {
        Path tempFilePath = tempDir.resolve(UUID.randomUUID().toString().replace("-", "") + "yml");
        return tempFilePath.toFile();
    }


    protected File serializeToFile(ConfigNode node) {
        File tempFile = getTempFile();
        Assertions.assertDoesNotThrow(tempFile::createNewFile, "Error creating temporary file!");
        NodeSerializer serializer = new NodeSerializer();
        Assertions.assertDoesNotThrow(() -> serializer.serializeToFile(tempFile, node), "Error serializing node to file!");

        return tempFile;
    }

    protected void checkNoDiff(Path generatedFilePath, Path expectedFilePath) {

        List<YamlDiffer.LineDiff> diffs = Assertions.assertDoesNotThrow(
                () -> YamlDiffer.findDifference(generatedFilePath, expectedFilePath)
                , "Error checking diff!");

        Assertions.assertTrue(diffs.isEmpty(),
                () -> "Difference between YAML Files:\nGenerated Output | Expected Output\n" + diffs.stream()
                        .map(ld -> ld.getLeftLine() + " | " + ld.getRightLine())
                        .collect(Collectors.joining("\n"))
        );
    }
}
