package com.example.xfdltracker.behavior;

/**
 * {@code resolveStaticTabPage(...)}의 불변 결과. {@code status == RESOLVED}일 때만
 * {@code tabControlStructuralId}/{@code pageOrdinal}이 non-null이며, 그 외 상태는 이 필드들을
 * 절대 발명하지 않는다(둘 다 null). {@code reason}은 진단 전용이며 semantic 판정 authority가 아니다.
 */
public final class TabStaticReceiverResolution {

    private final TabStaticReceiverResolutionStatus status;
    private final String tabControlStructuralId;
    private final Integer pageOrdinal;
    private final String reason;

    private TabStaticReceiverResolution(
            TabStaticReceiverResolutionStatus status, String tabControlStructuralId, Integer pageOrdinal,
            String reason) {
        this.status = status;
        this.tabControlStructuralId = tabControlStructuralId;
        this.pageOrdinal = pageOrdinal;
        this.reason = reason;
    }

    public static TabStaticReceiverResolution resolved(String tabControlStructuralId, int pageOrdinal) {
        if (tabControlStructuralId == null || tabControlStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "tab_static_receiver_resolution: tabControlStructuralId must not be null/blank");
        }
        if (pageOrdinal < 0) {
            throw new IllegalArgumentException(
                    "tab_static_receiver_resolution: pageOrdinal must be >= 0, but was " + pageOrdinal);
        }
        return new TabStaticReceiverResolution(
                TabStaticReceiverResolutionStatus.RESOLVED, tabControlStructuralId, Integer.valueOf(pageOrdinal),
                null);
    }

    public static TabStaticReceiverResolution missing(String reason) {
        return new TabStaticReceiverResolution(
                TabStaticReceiverResolutionStatus.MISSING, null, null, requireReason(reason));
    }

    public static TabStaticReceiverResolution ambiguous(String reason) {
        return new TabStaticReceiverResolution(
                TabStaticReceiverResolutionStatus.AMBIGUOUS, null, null, requireReason(reason));
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.trim().length() == 0) {
            throw new IllegalArgumentException("tab_static_receiver_resolution: reason must not be null/blank");
        }
        return reason;
    }

    public TabStaticReceiverResolutionStatus getStatus() { return status; }
    public String getTabControlStructuralId() { return tabControlStructuralId; }
    public Integer getPageOrdinal() { return pageOrdinal; }
    public String getReason() { return reason; }
}
