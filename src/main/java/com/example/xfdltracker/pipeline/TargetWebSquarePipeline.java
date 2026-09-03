package com.example.xfdltracker.pipeline;

import com.example.xfdltracker.XfdlFunctionTracker;
import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.binding.SourceBindingAnalyzer;
import com.example.xfdltracker.binding.SourceBindingReference;
import com.example.xfdltracker.behavior.SourceAnalysisStatus;
import com.example.xfdltracker.behavior.SourceScriptAnalysisResult;
import com.example.xfdltracker.behavior.SourceScriptAnalyzer;
import com.example.xfdltracker.behavior.TargetScriptArtifact;
import com.example.xfdltracker.behavior.TargetScriptDocumentIntegrator;
import com.example.xfdltracker.behavior.TargetScriptTranslationResult;
import com.example.xfdltracker.behavior.TargetScriptTranslator;
import com.example.xfdltracker.behavior.TargetTranslationStatus;
import com.example.xfdltracker.composition.CompositionDecision;
import com.example.xfdltracker.composition.CompositionEvaluator;
import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetCompositionPlanBuilder;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.payload.PayloadBehaviorFinalizationStatus;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadBehaviorFinalizationResult;
import com.example.xfdltracker.payload.TargetPayloadBehaviorFinalizer;
import com.example.xfdltracker.payload.TargetPayloadExtractor;
import com.example.xfdltracker.renderer.AtomicRenderResult;
import com.example.xfdltracker.renderer.AtomicWebSquareRenderer;
import com.example.xfdltracker.renderer.CompositionRenderResult;
import com.example.xfdltracker.renderer.CompositionRenderer;
import com.example.xfdltracker.renderer.TargetDocumentAssembler;
import com.example.xfdltracker.renderer.TargetXmlSerializer;
import com.example.xfdltracker.runtime.CommonRuntimeCapabilityCatalog;
import com.example.xfdltracker.runtime.RuntimeCapabilityResolver;
import com.example.xfdltracker.runtime.RuntimeFunctionCallAnalyzer;
import com.example.xfdltracker.runtime.RuntimeRequirementSet;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 단독 프로덕션 진입점: 원본 XFDL 파싱/분석을 직접 소유(레거시 오케스트레이터 미사용). 런타임 검증
 * 레인과 스크립트 번역 레인은 서로 호출하지 않으며, BUTTON_GROUP payload만 Plan node identity로
 * finalize한다. 실패 시 즉시 예외, {@link TargetXmlSerializer}가 원자적으로 발행하므로 부분 기록 없음.
 */
public final class TargetWebSquarePipeline {

    public void convert(File sourceXfdl, File targetWebSquareXml, TargetPipelineConfig config) {
        if (sourceXfdl == null) {
            throw new IllegalArgumentException("target_web_square_pipeline: sourceXfdl must not be null");
        }
        if (targetWebSquareXml == null) {
            throw new IllegalArgumentException("target_web_square_pipeline: targetWebSquareXml must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("target_web_square_pipeline: config must not be null");
        }
        if (!sourceXfdl.isFile()) {
            throw new IllegalArgumentException(
                    "target_web_square_pipeline: sourceXfdl is not a file: " + sourceXfdl.getAbsolutePath());
        }
        try {
            XfdlReader reader = new XfdlReader();
            Document sourceDocument = reader.read(sourceXfdl);
            Element sourceRoot = sourceDocument.getDocumentElement();
            XfdlAnalysisResult analysis = new XfdlFunctionTracker().analyze(sourceXfdl);

            List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(sourceRoot, analysis);

            CompositionEvaluator evaluator = new CompositionEvaluator();
            List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
            for (SemanticRegionResult region : regions) {
                decisions.add(evaluator.evaluate(region));
            }

            TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
            // 원본 XFDL -> binding evidence 분석(SourceBindingAnalyzer, accepted-path 유일한 raw
            // source binding 스캔 지점) -> payload extraction 순서를 그대로 지킨다.
            List<SourceBindingReference> bindingReferences = new SourceBindingAnalyzer().analyze(sourceRoot);
            List<TargetNodePayload> payloads =
                    new TargetPayloadExtractor().extract(sourceRoot, plan, regions, bindingReferences);

            String script = reader.extractScript(sourceDocument);

            // (A) 공통 런타임 요구사항 레인 -- fail-closed, 런타임 지원 리소스는 절대 emit하지 않음.
            CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
            RuntimeRequirementSet requirements = new RuntimeFunctionCallAnalyzer().analyze(script, catalog);
            new RuntimeCapabilityResolver().validate(requirements, config.getRuntimeProfile(), catalog);

            // (B) 일반 소스 스크립트 behavior 레인 -- (A)와 완전히 분리, RuntimeFunctionCallAnalyzer를
            // 호출하지 않고 uc.*도 번역하지 않는다(SourceScriptAnalyzer가 자체 계약으로 fail-closed).
            SourceScriptAnalysisResult sourceAnalysisResult = new SourceScriptAnalyzer().analyze(script);
            if (sourceAnalysisResult.getStatus() != SourceAnalysisStatus.ANALYZED) {
                throw new IllegalStateException(
                        "target_web_square_pipeline: general source-script behavior analysis failed -- status="
                                + sourceAnalysisResult.getStatus() + " reason=" + sourceAnalysisResult.getReason());
            }
            TargetScriptTranslationResult translationResult =
                    new TargetScriptTranslator().translate(sourceAnalysisResult.getAnalysis());
            if (translationResult.getStatus() != TargetTranslationStatus.TRANSLATED) {
                throw new IllegalStateException(
                        "target_web_square_pipeline: general source-script behavior translation failed -- status="
                                + translationResult.getStatus() + " reason=" + translationResult.getReason());
            }
            TargetScriptArtifact scriptArtifact = translationResult.getArtifact();

            // family-scoped behavior finalization -- 정확한 Plan node identity로만 상관, BUTTON_GROUP만 적용.
            Map<String, String> familyByPlanNodeId = new LinkedHashMap<String, String>();
            for (TargetCompositionNode node : plan.getNodes()) {
                familyByPlanNodeId.put(node.getNodeId(), node.getFamily());
            }
            List<TargetNodePayload> finalizedPayloads = new ArrayList<TargetNodePayload>();
            TargetPayloadBehaviorFinalizer behaviorFinalizer = new TargetPayloadBehaviorFinalizer();
            for (TargetNodePayload payload : payloads) {
                if (!"BUTTON_GROUP".equals(familyByPlanNodeId.get(payload.getPlanNodeId()))) {
                    finalizedPayloads.add(payload);
                    continue;
                }
                TargetPayloadBehaviorFinalizationResult finalizationResult =
                        behaviorFinalizer.finalize(payload, scriptArtifact);
                if (finalizationResult.getStatus() != PayloadBehaviorFinalizationStatus.FINALIZED) {
                    throw new IllegalStateException(
                            "target_web_square_pipeline: BUTTON_GROUP behavior finalization failed -- planNodeId="
                                    + payload.getPlanNodeId() + " status=" + finalizationResult.getStatus()
                                    + " reason=" + finalizationResult.getFailureReason());
                }
                finalizedPayloads.add(finalizationResult.getFinalizedPayload());
            }

            List<AtomicRenderResult> atomicResults = new AtomicWebSquareRenderer().render(plan, finalizedPayloads);
            List<CompositionRenderResult> compositionResults = new CompositionRenderer().render(plan, atomicResults);

            Document targetDocument = new TargetDocumentAssembler().assemble(plan, compositionResults);
            new TargetScriptDocumentIntegrator().integrate(targetDocument, scriptArtifact);
            new TargetXmlSerializer().serialize(targetDocument, targetWebSquareXml);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("target_web_square_pipeline: conversion failed", e);
        }
    }
}
