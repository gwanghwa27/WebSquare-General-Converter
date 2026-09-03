package com.example.xfdltracker.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 불변 Common Runtime Capability 항목. 레거시 {@code uc.*} 이름은 문서화된 별칭일 뿐 capability ID
 * 자체가 아니다. 어떤 필드도 이름만으로 추론하지 않는다.
 */
public final class CommonRuntimeCapabilityDefinition {

    private final String capabilityId;
    private final RuntimeCapabilityCategory category;
    private final List<String> documentedAliases;
    private final RuntimeCapabilitySupportStatus supportStatus;
    private final RuntimeCapabilitySignatureStatus signatureStatus;
    private final RuntimeCapabilityAsyncModel asyncModel;
    private final List<String> environmentRequirements;
    private final RuntimeCapabilityTargetBindingStatus targetBindingStatus;

    public CommonRuntimeCapabilityDefinition(
            String capabilityId, RuntimeCapabilityCategory category, List<String> documentedAliases,
            RuntimeCapabilitySupportStatus supportStatus, RuntimeCapabilitySignatureStatus signatureStatus,
            RuntimeCapabilityAsyncModel asyncModel, List<String> environmentRequirements,
            RuntimeCapabilityTargetBindingStatus targetBindingStatus) {
        if (capabilityId == null || capabilityId.trim().length() == 0) {
            throw new IllegalArgumentException("common_runtime_capability_definition: capabilityId must not be null/blank");
        }
        if (category == null || supportStatus == null || signatureStatus == null
                || asyncModel == null || targetBindingStatus == null) {
            throw new IllegalArgumentException(
                    "common_runtime_capability_definition: category/supportStatus/signatureStatus/asyncModel/"
                            + "targetBindingStatus must not be null -- unknown values must use an explicit "
                            + "UNKNOWN/UNCONFIRMED/UNBOUND state, never a null field");
        }
        this.capabilityId = capabilityId;
        this.category = category;
        this.documentedAliases = Collections.unmodifiableList(
                new ArrayList<String>(documentedAliases == null ? Collections.<String>emptyList() : documentedAliases));
        this.supportStatus = supportStatus;
        this.signatureStatus = signatureStatus;
        this.asyncModel = asyncModel;
        this.environmentRequirements = Collections.unmodifiableList(new ArrayList<String>(
                environmentRequirements == null ? Collections.<String>emptyList() : environmentRequirements));
        this.targetBindingStatus = targetBindingStatus;
    }

    public String getCapabilityId() { return capabilityId; }
    public RuntimeCapabilityCategory getCategory() { return category; }
    public List<String> getDocumentedAliases() { return documentedAliases; }
    public RuntimeCapabilitySupportStatus getSupportStatus() { return supportStatus; }
    public RuntimeCapabilitySignatureStatus getSignatureStatus() { return signatureStatus; }
    public RuntimeCapabilityAsyncModel getAsyncModel() { return asyncModel; }
    public List<String> getEnvironmentRequirements() { return environmentRequirements; }
    public RuntimeCapabilityTargetBindingStatus getTargetBindingStatus() { return targetBindingStatus; }
}
