package com.example.xfdltracker.batch;

import com.example.xfdltracker.io.TextFileUtil;
import com.example.xfdltracker.runtime.CommonRuntimeCapabilityCatalog;
import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 호출자가 명시적으로 제공한 profile 파일을 {@link TargetRuntimeProfile}로 파싱한다. 문법: 줄마다
 * 정규 capability ID 하나, 빈 줄 무시, {@code #}로 시작하는 줄은 전체가 주석. 카탈로그에 없는 ID는
 * 변환 시작 전 fail-closed한다(alias/이름 추론 없음, 암묵적 기본 profile 없음).
 */
public final class BatchRuntimeProfileLoader {

    private BatchRuntimeProfileLoader() {}

    public static TargetRuntimeProfile load(File profileFile, CommonRuntimeCapabilityCatalog catalog) throws Exception {
        if (profileFile == null) {
            throw new IllegalArgumentException("batch_runtime_profile_loader: profileFile must not be null");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("batch_runtime_profile_loader: catalog must not be null");
        }
        if (!profileFile.isFile()) {
            throw new IllegalArgumentException(
                    "batch_runtime_profile_loader: runtime profile file not found -- "
                            + profileFile.getAbsolutePath() + " (batch_runtime_profile_file_missing)");
        }

        String content = TextFileUtil.read(profileFile, "UTF-8");
        String[] rawLines = content.split("\r\n|\r|\n", -1);

        Set<String> ids = new LinkedHashSet<String>();
        List<String> unknown = new ArrayList<String>();
        for (int i = 0; i < rawLines.length; i++) {
            String trimmed = rawLines[i].trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            if (catalog.get(trimmed) == null) {
                unknown.add("line " + (i + 1) + ":" + trimmed);
                continue;
            }
            ids.add(trimmed);
        }

        if (!unknown.isEmpty()) {
            throw new IllegalStateException(
                    "batch_runtime_profile_loader: unknown capability id(s) not present in the canonical "
                            + "catalog, refusing to guess -- " + unknown
                            + " (batch_runtime_profile_unknown_capability_id)");
        }

        return new TargetRuntimeProfile(ids);
    }
}
