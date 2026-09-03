package com.example.xfdltracker.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * conversion 시작 전 계획된 모든 output에 대해 real path가 output root 아래인지, 그 부모 경로에
 * 심볼릭 링크/broken alias/junction 별칭이 없는지, 최종 대상 경로 자체가 이미(종류 무관, NOFOLLOW
 * 기준) 점유되어 있는지 미리 검증한다. 위반 시 conversion 전에 fail-closed하고 기존 entry는 보존한다.
 */
final class BatchOutputPreflight {

    private BatchOutputPreflight() {}

    static void verify(Path realOutputRoot, List<BatchOutputPlanner.PlannedConversion> plans) throws IOException {
        List<String> escapes = new ArrayList<String>();
        List<String> symbolicLinkIntermediates = new ArrayList<String>();
        List<String> unresolvableIntermediates = new ArrayList<String>();
        List<String> aliasedIntermediates = new ArrayList<String>();
        List<String> preexisting = new ArrayList<String>();
        for (BatchOutputPlanner.PlannedConversion plan : plans) {
            Path realTarget = BatchPathBoundary.resolveRealPathAllowingMissingTail(plan.getTargetFile());
            if (!realTarget.startsWith(realOutputRoot)) {
                escapes.add(plan.getRelativeTargetPath() + " -> " + realTarget);
                continue;
            }
            BatchPathBoundary.IntermediateAliasFinding finding = BatchPathBoundary.findFirstIntermediateAliasIssue(
                    realOutputRoot, plan.getRelativeTargetPath());
            if (finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.SYMBOLIC_LINK) {
                symbolicLinkIntermediates.add(plan.getRelativeTargetPath());
                continue;
            }
            if (finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.UNRESOLVABLE) {
                unresolvableIntermediates.add(plan.getRelativeTargetPath());
                continue;
            }
            if (finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.ALIAS) {
                aliasedIntermediates.add(plan.getRelativeTargetPath() + " -> " + finding.aliasRealPath);
                continue;
            }
            if (Files.exists(plan.getTargetFile().toPath(), LinkOption.NOFOLLOW_LINKS)) {
                preexisting.add(plan.getRelativeTargetPath());
            }
        }
        if (!escapes.isEmpty()) {
            throw new IllegalStateException(
                    "batch_output_preflight: planned output resolves outside output root, refusing to publish -- "
                            + escapes + " (batch_output_root_escape)");
        }
        if (!symbolicLinkIntermediates.isEmpty()) {
            throw new IllegalStateException(
                    "batch_output_preflight: planned output publication path passes through an intermediate "
                            + "symbolic link, refusing to publish regardless of what that link resolves to -- "
                            + symbolicLinkIntermediates + " (batch_output_intermediate_symbolic_link_rejected)");
        }
        if (!unresolvableIntermediates.isEmpty()) {
            throw new IllegalStateException(
                    "batch_output_preflight: an intermediate publication path component exists but its real "
                            + "path cannot be resolved (broken/dangling alias), refusing to treat it as a "
                            + "not-yet-created directory -- " + unresolvableIntermediates
                            + " (batch_output_intermediate_path_unresolvable)");
        }
        if (!aliasedIntermediates.isEmpty()) {
            throw new IllegalStateException(
                    "batch_output_preflight: planned output publication path crosses an intermediate "
                            + "junction/reparse alias, refusing to publish even though the final real path "
                            + "remains inside the output root -- " + aliasedIntermediates
                            + " (batch_output_intermediate_alias_rejected)");
        }
        if (!preexisting.isEmpty()) {
            throw new IllegalStateException(
                    "batch_output_preflight: planned output already exists, refusing to overwrite -- "
                            + preexisting + " (batch_preexisting_output_rejected)");
        }
    }
}
