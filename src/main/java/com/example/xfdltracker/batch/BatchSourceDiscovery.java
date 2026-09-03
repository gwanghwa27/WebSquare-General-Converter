package com.example.xfdltracker.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * input root 아래에서 확장자가 정확히 {@code .xfdl}인 파일만 {@link Files#walkFileTree}(FOLLOW_LINKS
 * 없이)로 재귀 탐색한다. 심볼릭 링크 항목은 확장자/대상 종류와 무관하게 항상 fail-closed하며, real
 * path 재확인으로 junction 별칭/순환(같은 real 디렉터리를 두 경로로 재방문)도 거부한다.
 */
public final class BatchSourceDiscovery {

    private static final String EXTENSION = ".xfdl";

    private BatchSourceDiscovery() {}

    public static List<File> discover(File inputRoot) {
        if (inputRoot == null) {
            throw new IllegalArgumentException("batch_source_discovery: inputRoot must not be null");
        }
        if (!inputRoot.isDirectory()) {
            throw new IllegalArgumentException(
                    "batch_source_discovery: input root is not a directory -- " + inputRoot.getAbsolutePath()
                            + " (batch_input_root_not_directory)");
        }
        Path realInputRoot = resolveRealPath(inputRoot, "batch_input_root_real_path_unresolvable");

        List<Path> foundRelative = new ArrayList<Path>();
        Set<String> visitedRealDirs = new HashSet<String>();
        try {
            Files.walkFileTree(realInputRoot, new DiscoveryVisitor(realInputRoot, visitedRealDirs, foundRelative));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "batch_source_discovery: traversal failed under " + realInputRoot + " -- " + e.getMessage(), e);
        }

        Collections.sort(foundRelative, new Comparator<Path>() {
            public int compare(Path a, Path b) {
                return a.toString().replace('\\', '/').compareTo(b.toString().replace('\\', '/'));
            }
        });

        List<File> result = new ArrayList<File>();
        for (Path relative : foundRelative) {
            result.add(realInputRoot.resolve(relative).toFile());
        }
        return Collections.unmodifiableList(result);
    }

    /** input root 기준 real-path 상대경로 문자열(항상 {@code /} 구분자, 결정적, 경계 이탈 시 fail-closed). */
    public static String relativePath(File inputRoot, File file) {
        Path realInputRoot = resolveRealPath(inputRoot, "batch_input_root_real_path_unresolvable");
        Path realFile = resolveRealPath(file, "batch_discovered_file_real_path_unresolvable");
        if (!realFile.startsWith(realInputRoot)) {
            throw new IllegalStateException(
                    "batch_source_discovery: discovered file is not under input root -- " + realFile
                            + " (batch_input_root_escape)");
        }
        return realInputRoot.relativize(realFile).toString().replace('\\', '/');
    }

    private static Path resolveRealPath(File file, String reasonCode) {
        try {
            return file.toPath().toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "batch_source_discovery: failed to resolve real path -- " + file.getAbsolutePath()
                            + " (" + reasonCode + ")", e);
        }
    }

    private static final class DiscoveryVisitor extends SimpleFileVisitor<Path> {
        private final Path realInputRoot;
        private final Set<String> visitedRealDirs;
        private final List<Path> found;

        DiscoveryVisitor(Path realInputRoot, Set<String> visitedRealDirs, List<Path> found) {
            this.realInputRoot = realInputRoot;
            this.visitedRealDirs = visitedRealDirs;
            this.found = found;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path realDir = dir.toRealPath();
            if (!realDir.startsWith(realInputRoot)) {
                throw new IllegalStateException(
                        "batch_source_discovery: directory resolves outside input root -- " + realDir
                                + " (batch_input_root_escape)");
            }
            if (!visitedRealDirs.add(realDir.toString())) {
                throw new IllegalStateException(
                        "batch_source_discovery: a real directory was reached through two different lexical "
                                + "paths under the input tree (alias or cycle, not distinguished) -- " + realDir
                                + " (batch_input_directory_alias_or_cycle_rejected)");
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isSymbolicLink()) {
                throw new IllegalStateException(
                        "batch_source_discovery: refusing to treat a symbolic link as a normal input entry "
                                + "regardless of its extension or target -- " + file
                                + " (batch_input_symbolic_link_entry_rejected)");
            }
            String name = file.getFileName().toString();
            if (!name.endsWith(EXTENSION)) {
                return FileVisitResult.CONTINUE;
            }
            Path realFile = file.toRealPath();
            if (!realFile.startsWith(realInputRoot)) {
                throw new IllegalStateException(
                        "batch_source_discovery: discovered file resolves outside input root -- " + realFile
                                + " (batch_input_root_escape)");
            }
            found.add(realInputRoot.relativize(realFile));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw new IllegalStateException(
                    "batch_source_discovery: failed to visit path during traversal -- " + file
                            + " (batch_traversal_visit_failed)", exc);
        }
    }
}
