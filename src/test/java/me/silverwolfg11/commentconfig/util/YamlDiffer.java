package me.silverwolfg11.commentconfig.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlDiffer {

    private static class YamlFile {
        private final List<String> lines;

        YamlFile(List<String> lines) {
            this.lines = lines;
        }

        YamlFile(Path filePath) throws IOException {
            List<String> lines;
            try (Stream<String> linesStream = Files.lines(filePath)) {
                lines = linesStream
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
            this.lines = lines;
        }
    }

    public static class LineDiff {
        private final String leftLine;
        private final String rightLine;

        LineDiff(String leftLine, String rightLine) {
            this.leftLine = leftLine;
            this.rightLine = rightLine;
        }

        public String getLeftLine() {
            return leftLine;
        }

        public String getRightLine() {
            return rightLine;
        }
    }

    public static List<LineDiff> findDifference(Path leftFilePath, Path rightFilePath) throws IOException {
        YamlFile leftFile = new YamlFile(leftFilePath);
        YamlFile rightFile = new YamlFile(rightFilePath);

        List<LineDiff> lineDiffs = new ArrayList<>();

        // The minimum number of lines in both files
        int numLines = Math.min(leftFile.lines.size(), rightFile.lines.size());

        for (int lineIndex = 0; lineIndex < numLines; ++lineIndex) {
            String leftLine = leftFile.lines.get(lineIndex);
            String rightLine = rightFile.lines.get(lineIndex);

            if (!leftLine.equals(rightLine)) {
                lineDiffs.add(new LineDiff(leftLine, rightLine));
            }
        }

        // Handle the case when one file has more lines than the other
        List<String> remainingLines = Collections.emptyList();
        boolean leftRemaining = false;
        if (leftFile.lines.size() > numLines) {
            remainingLines = leftFile.lines;
            leftRemaining = true;
        } else if (rightFile.lines.size() > numLines) {
            remainingLines = rightFile.lines;
        }

        for (int lineIdx = numLines; lineIdx < remainingLines.size(); lineIdx++) {
            String remainingLine = remainingLines.get(lineIdx);
            lineDiffs.add(new LineDiff(leftRemaining ? remainingLine : "", leftRemaining ? "" : remainingLine));
        }

        return lineDiffs;
    }

}
