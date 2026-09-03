package com.example.xfdltracker.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 정규 Common Runtime Capability 카탈로그. 리뷰 승인된 인벤토리만 포함하며 그 이상 추가하지 않는다.
 * 표준 JS 원시 기능(setTimeout/Promise 등)과 WebSquare 컴포넌트 API(tabControl.* 등)는 의도적으로
 * 카탈로그 항목이 아니다.
 */
public final class CommonRuntimeCapabilityCatalog {

    private final Map<String, CommonRuntimeCapabilityDefinition> byCapabilityId;
    private final Map<String, String> capabilityIdByAlias;

    public CommonRuntimeCapabilityCatalog(List<CommonRuntimeCapabilityDefinition> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("common_runtime_capability_catalog: definitions must not be null");
        }
        Map<String, CommonRuntimeCapabilityDefinition> byId = new LinkedHashMap<String, CommonRuntimeCapabilityDefinition>();
        Map<String, String> byAlias = new LinkedHashMap<String, String>();
        for (CommonRuntimeCapabilityDefinition def : definitions) {
            if (byId.containsKey(def.getCapabilityId())) {
                throw new IllegalArgumentException(
                        "common_runtime_capability_catalog: duplicate capabilityId=" + def.getCapabilityId());
            }
            byId.put(def.getCapabilityId(), def);
            for (String alias : def.getDocumentedAliases()) {
                if (byAlias.containsKey(alias)) {
                    throw new IllegalArgumentException(
                            "common_runtime_capability_catalog: duplicate documented alias=" + alias);
                }
                byAlias.put(alias, def.getCapabilityId());
            }
        }
        this.byCapabilityId = Collections.unmodifiableMap(byId);
        this.capabilityIdByAlias = Collections.unmodifiableMap(byAlias);
    }

    /** 정의를 반환하거나, 카탈로그에 없으면 null. */
    public CommonRuntimeCapabilityDefinition get(String capabilityId) {
        return byCapabilityId.get(capabilityId);
    }

    /** 문서화된 별칭(예: {@code "uc.msg"})을 정규 capabilityId로 해석한다. 미문서 별칭은 null을 반환하며,
     * 이름만으로 capability를 추정하지 않는다. */
    public String resolveAliasToCapabilityId(String alias) {
        return capabilityIdByAlias.get(alias);
    }

    public Map<String, CommonRuntimeCapabilityDefinition> getAllByCapabilityId() {
        return byCapabilityId;
    }

    // ---- 승인된 시드 인벤토리 ----------------------------------------------

    public static CommonRuntimeCapabilityCatalog createSeeded() {
        List<CommonRuntimeCapabilityDefinition> defs = new ArrayList<CommonRuntimeCapabilityDefinition>();

        // UI_NAVIGATION_RUNTIME_CAPABILITY 항목
        defs.add(nameOnly("MESSAGE_DIALOG", RuntimeCapabilityCategory.UI_NAVIGATION_RUNTIME_CAPABILITY, "uc.msg"));
        defs.add(env("EXTERNAL_PROCESS_EXECUTION", RuntimeCapabilityCategory.UI_NAVIGATION_RUNTIME_CAPABILITY,
                alias("uc.shellExecute"), "NATIVE_PROCESS_EXECUTION"));
        defs.add(nameOnly("SCREEN_OPEN", RuntimeCapabilityCategory.UI_NAVIGATION_RUNTIME_CAPABILITY, "uc.openScreen"));
        defs.add(nameOnly("POPUP_OPEN", RuntimeCapabilityCategory.UI_NAVIGATION_RUNTIME_CAPABILITY, "uc.openPopup"));

        // TRANSACTION_RUNTIME_CAPABILITY 항목
        defs.add(nameOnly("TRANSACTION_SEND", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSend"));
        defs.add(nameOnly("TRANSACTION_SEND_MULTI_EACH", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendMultiEach"));
        defs.add(nameOnly("MULTI_TRANSACTION_WAITING_POPUP", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.multiEachWaitingPopup"));
        defs.add(nameOnly("MULTI_TRANSACTION_CANCEL", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.cancelMultiEachTran"));
        defs.add(nameOnly("MULTI_TRANSACTION_GET_CANCELED_ROWS", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.getCanceledMultiEachRows"));
        defs.add(nameOnly("TRANSACTION_SEND_LIST", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendList"));
        defs.add(nameOnly("TRANSACTION_SEND_LIST_NEXT", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendListNext"));
        defs.add(nameOnly("TRANSACTION_LIST_HAS_NEXT", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.isExistListNext"));
        defs.add(nameOnly("TRANSACTION_BIND_PAGING", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.bindTranPaging"));
        defs.add(nameOnly("TRANSACTION_SEND_PAGING", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendPaging"));
        defs.add(nameOnly("TRANSACTION_SEND_PORTLET", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendPortlet"));
        defs.add(nameOnly("TRANSACTION_SEND_CUSTOMER_INFO", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendCustomerInfo"));
        defs.add(nameOnly("TRANSACTION_SEND_CUSTOMER_INFO_EXCEL", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendCustomerInfoExcel"));
        defs.add(nameOnly("TRANSACTION_SEND_CUSTOMER_INFO_CODE", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendCustomerInfoCode"));
        defs.add(nameOnly("TRANSACTION_OPEN_CUSTOMER_INFO_CODE", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.openCustomerInfoCode"));
        defs.add(nameOnly("TRANSACTION_BULK_INPUT", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranBulkInput"));
        defs.add(nameOnly("TRANSACTION_BULK_OUTPUT_REQUEST", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranBulkOutputRequest"));
        defs.add(nameOnly("TRANSACTION_SEND_UPLOAD_FILE_ID", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.tranSendUploadFileId"));
        defs.add(nameOnly("TRANSACTION_SHOW_ERROR_POPUP", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.showTranErrorPopup"));
        defs.add(nameOnly("TRANSACTION_SHOW_WAITING_POPUP", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.showWaitingPopup"));
        defs.add(nameOnly("TRANSACTION_GET_HEADER_FILE_ID", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.getHeaderFileId"));
        defs.add(nameOnly("TRANSACTION_IS_SUCCESS", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.isTranSuccess"));
        defs.add(nameOnly("TRANSACTION_IS_FAILURE", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.isTranFailure"));
        defs.add(nameOnly("TRANSACTION_STOP_WAITING_POPUP", RuntimeCapabilityCategory.TRANSACTION_RUNTIME_CAPABILITY, "uc.stopTranWaitingPopup"));

        // COMMON_CODE_RUNTIME_CAPABILITY 항목
        defs.add(nameOnly("COMMON_CODE_GET", RuntimeCapabilityCategory.COMMON_CODE_RUNTIME_CAPABILITY, "uc.getCommonCode"));
        defs.add(nameOnly("COMMON_CODE_SET", RuntimeCapabilityCategory.COMMON_CODE_RUNTIME_CAPABILITY, "uc.setCommonCode"));

        // FILE_RUNTIME_CAPABILITY 항목
        defs.add(env("FILE_SELECT_DIALOG_OPEN", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.Open_fileSelectDialog"), "NATIVE_DIALOG"));
        defs.add(env("FILE_MULTI_SELECT_DIALOG_OPEN", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.Open_fileMultiSelectDialog"), "NATIVE_DIALOG"));
        defs.add(env("FILE_SAVE_DIALOG_OPEN", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.Open_fileSaveDialog"), "NATIVE_DIALOG"));
        defs.add(env("FILE_DIALOG_EX_OPEN", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.Open_fileDialogEx"), "NATIVE_DIALOG"));
        defs.add(env("DIRECTORY_EXISTS_CHECK", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.existDirectory"), "FILESYSTEM_ACCESS"));
        defs.add(env("DIRECTORY_CREATE", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.createDirectory"), "FILESYSTEM_ACCESS"));
        defs.add(external("FILE_UPLOAD", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.fileUpload"), "SERVER_SIDE_SERVICE"));
        defs.add(external("FILE_DOWNLOAD", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.fileDownLoad"), "SERVER_SIDE_SERVICE"));
        defs.add(nameOnly("FILE_JSP_FILE_NAME_GET", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, "uc.getJSPfileName"));
        defs.add(nameOnly("FILE_DOWNLOAD_ORIGINAL_NAME_GET", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, "uc.getDownload_OrgName"));
        defs.add(external("FILE_UPLOAD_EMAIL", RuntimeCapabilityCategory.FILE_RUNTIME_CAPABILITY, alias("uc.fileUploadEmail"), "SERVER_SIDE_SERVICE"));

        // EXCEL_RUNTIME_CAPABILITY 항목
        defs.add(nameOnly("EXCEL_FILE_NAME_GET", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, "uc.getExcelFileName"));
        defs.add(external("EXCEL_DOWNLOAD", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, alias("uc.excelDownload"), "SERVER_SIDE_SERVICE"));
        defs.add(external("EXCEL_DOWNLOAD_PAGE", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, alias("uc.excelDownloadPage"), "SERVER_SIDE_SERVICE"));
        defs.add(external("EXCEL_MULTI_DOWNLOAD", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, alias("uc.excelMultiExcelDownload"), "SERVER_SIDE_SERVICE"));
        defs.add(nameOnly("EXCEL_UPLOAD_URL_GET", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, "uc.getExcelUploadURL"));
        defs.add(external("EXCEL_UPLOAD", RuntimeCapabilityCategory.EXCEL_RUNTIME_CAPABILITY, alias("uc.excelUpload"), "SERVER_SIDE_SERVICE"));

        // CONTEXT_RUNTIME_CAPABILITY 항목
        defs.add(nameOnly("CONTEXT_SCOPE_OBJECT_GET", RuntimeCapabilityCategory.CONTEXT_RUNTIME_CAPABILITY, "uc.getConScopeObj"));
        defs.add(nameOnly("SCREEN_POLICY_GET", RuntimeCapabilityCategory.CONTEXT_RUNTIME_CAPABILITY, "uc.getScreenPolicy"));

        return new CommonRuntimeCapabilityCatalog(defs);
    }

    private static List<String> alias(String a) {
        return Collections.singletonList(a);
    }

    private static CommonRuntimeCapabilityDefinition nameOnly(String id, RuntimeCapabilityCategory category, String documentedAlias) {
        return new CommonRuntimeCapabilityDefinition(
                id, category, alias(documentedAlias), RuntimeCapabilitySupportStatus.DOCUMENTED_NAME_ONLY,
                RuntimeCapabilitySignatureStatus.UNCONFIRMED, RuntimeCapabilityAsyncModel.UNKNOWN,
                Collections.<String>emptyList(), RuntimeCapabilityTargetBindingStatus.UNBOUND);
    }

    private static CommonRuntimeCapabilityDefinition env(
            String id, RuntimeCapabilityCategory category, List<String> aliases, String environmentRequirement) {
        return new CommonRuntimeCapabilityDefinition(
                id, category, aliases, RuntimeCapabilitySupportStatus.ENVIRONMENT_SPECIFIC,
                RuntimeCapabilitySignatureStatus.UNCONFIRMED, RuntimeCapabilityAsyncModel.UNKNOWN,
                Arrays.asList(environmentRequirement), RuntimeCapabilityTargetBindingStatus.UNBOUND);
    }

    private static CommonRuntimeCapabilityDefinition external(
            String id, RuntimeCapabilityCategory category, List<String> aliases, String environmentRequirement) {
        return new CommonRuntimeCapabilityDefinition(
                id, category, aliases, RuntimeCapabilitySupportStatus.EXTERNAL_SERVICE_REQUIRED,
                RuntimeCapabilitySignatureStatus.UNCONFIRMED, RuntimeCapabilityAsyncModel.UNKNOWN,
                Arrays.asList(environmentRequirement), RuntimeCapabilityTargetBindingStatus.UNBOUND);
    }
}
