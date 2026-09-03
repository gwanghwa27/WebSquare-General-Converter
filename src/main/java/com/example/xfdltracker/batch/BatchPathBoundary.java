package com.example.xfdltracker.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * batch input/output root의 real-path 경계를 계산한다. output root처럼 아직 없는 경로는 존재하는
 * 조상까지만 real-path로 resolve하고 나머지 구간은 이름 그대로 이어붙이며, 포함관계는 문자열
 * 접두사가 아니라 {@link Path#startsWith(Path)} component 비교로 판정한다.
 */
final class BatchPathBoundary {

    private BatchPathBoundary() {}

    static Path resolveRealPathAllowingMissingTail(File f) throws IOException {
        Path p = f.toPath().toAbsolutePath().normalize();
        List<Path> missingTail = new ArrayList<Path>();
        Path existing = p;
        while (existing != null && !Files.exists(existing)) {
            Path name = existing.getFileName();
            if (name == null) {
                break;
            }
            missingTail.add(0, name);
            existing = existing.getParent();
        }
        if (existing == null || !Files.exists(existing)) {
            throw new IOException(
                    "no existing ancestor directory found to resolve a real path for -- " + f.getAbsolutePath());
        }
        Path result = existing.toRealPath();
        for (Path segment : missingTail) {
            result = result.resolve(segment);
        }
        return result;
    }

    /**
     * {@code relativePath} 부모 구간을 순서대로, 존재 여부는 {@code NOFOLLOW_LINKS}로(dangling
     * symlink도 "존재") 판정한다. 심볼릭 링크/real-path 해석 실패/junction 별칭을 각각 구분해
     * 반환하고, 진짜 없는 구간을 만나야만 {@code NONE}으로 멈춘다(디렉터리 생성 없음).
     */
    static IntermediateAliasFinding findFirstIntermediateAliasIssue(Path realRoot, String relativePath)
            throws IOException {
        String[] segments = relativePath.split("/");
        Path lexical = realRoot;
        for (int i = 0; i < segments.length - 1; i++) {
            lexical = lexical.resolve(segments[i]);
            if (!Files.exists(lexical, LinkOption.NOFOLLOW_LINKS)) {
                return IntermediateAliasFinding.NONE;
            }
            if (Files.isSymbolicLink(lexical)) {
                return IntermediateAliasFinding.symbolicLink();
            }
            Path actualReal;
            try {
                actualReal = lexical.toRealPath();
            } catch (IOException e) {
                return IntermediateAliasFinding.unresolvable();
            }
            if (!actualReal.equals(lexical)) {
                return IntermediateAliasFinding.alias(actualReal);
            }
        }
        return IntermediateAliasFinding.NONE;
    }

    /** {@link #findFirstIntermediateAliasIssue}의 판정 결과 -- 종류별로 구분되는 불변 값 객체. */
    static final class IntermediateAliasFinding {
        static final IntermediateAliasFinding NONE = new IntermediateAliasFinding(Kind.NONE, null);

        enum Kind { NONE, SYMBOLIC_LINK, UNRESOLVABLE, ALIAS }

        final Kind kind;
        final Path aliasRealPath;

        private IntermediateAliasFinding(Kind kind, Path aliasRealPath) {
            this.kind = kind;
            this.aliasRealPath = aliasRealPath;
        }

        static IntermediateAliasFinding symbolicLink() {
            return new IntermediateAliasFinding(Kind.SYMBOLIC_LINK, null);
        }

        static IntermediateAliasFinding unresolvable() {
            return new IntermediateAliasFinding(Kind.UNRESOLVABLE, null);
        }

        static IntermediateAliasFinding alias(Path realPath) {
            return new IntermediateAliasFinding(Kind.ALIAS, realPath);
        }
    }
}
