package com.example.xfdltracker.runtime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Common Runtime Capability 기반({@link CommonRuntimeCapabilityCatalog}/{@link RuntimeRequirementSet}/
 * {@link TargetRuntimeProfile}/{@link RuntimeCapabilityResolver})에 대한 독립 실행형 unit test(JUnit 미사용).
 */
public class RuntimeFoundationTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testCatalogSeededCount();
        testAliasResolvesToCanonicalId();
        testUnknownAliasDoesNotInventCapability();
        testDuplicateRequirementDeduplicates();
        testUnavailableRequiredCapabilityFailsClosed();
        testUnsupportedCapabilityFailsClosed();
        testUnknownCapabilityFailsClosed();
        testExplicitlyAvailableCapabilityValidates();
        testStandardJsPrimitiveIsNotCatalogEntry();
        testDirectWebSquareComponentApiIsNotCatalogEntry();
        testShellExecuteIsEnvironmentSpecific();
        testBusinessSpecificAliasIsExtensionClassified();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testCatalogSeededCount() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        assertTrue("runtime_catalog: seeded catalog has exactly 49 accepted 98BF entries",
                catalog.getAllByCapabilityId().size() == 49);
    }

    private static void testAliasResolvesToCanonicalId() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        assertTrue("runtime_catalog: uc.msg resolves to MESSAGE_DIALOG",
                "MESSAGE_DIALOG".equals(catalog.resolveAliasToCapabilityId("uc.msg")));
        assertTrue("runtime_catalog: uc.getCommonCode resolves to COMMON_CODE_GET",
                "COMMON_CODE_GET".equals(catalog.resolveAliasToCapabilityId("uc.getCommonCode")));
    }

    private static void testUnknownAliasDoesNotInventCapability() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        assertTrue("runtime_catalog: unrecognized alias resolves to null, never a fabricated id",
                catalog.resolveAliasToCapabilityId("uc.totallyUnknownFunction") == null);
    }

    private static void testDuplicateRequirementDeduplicates() {
        Set<String> ids = new LinkedHashSet<String>();
        ids.add("MESSAGE_DIALOG");
        ids.add("MESSAGE_DIALOG");
        RuntimeRequirementSet set = new RuntimeRequirementSet(ids);
        assertTrue("runtime_requirement_set: duplicate insertion collapses to one requirement",
                set.getRequiredCapabilityIds().size() == 1);
    }

    private static void testUnavailableRequiredCapabilityFailsClosed() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        RuntimeRequirementSet requirements = new RuntimeRequirementSet(Collections.singleton("MESSAGE_DIALOG"));
        TargetRuntimeProfile profile = TargetRuntimeProfile.empty();
        boolean threw = false;
        try {
            new RuntimeCapabilityResolver().validate(requirements, profile, catalog);
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("runtime_capability_resolver: capability absent from profile fails closed", threw);
    }

    private static void testUnsupportedCapabilityFailsClosed() {
        CommonRuntimeCapabilityDefinition unsupported = new CommonRuntimeCapabilityDefinition(
                "TEST_UNSUPPORTED", RuntimeCapabilityCategory.CONTEXT_RUNTIME_CAPABILITY,
                Collections.<String>emptyList(), RuntimeCapabilitySupportStatus.UNSUPPORTED,
                RuntimeCapabilitySignatureStatus.UNCONFIRMED, RuntimeCapabilityAsyncModel.UNKNOWN,
                Collections.<String>emptyList(), RuntimeCapabilityTargetBindingStatus.UNBOUND);
        CommonRuntimeCapabilityCatalog catalog =
                new CommonRuntimeCapabilityCatalog(java.util.Arrays.asList(unsupported));
        RuntimeRequirementSet requirements = new RuntimeRequirementSet(Collections.singleton("TEST_UNSUPPORTED"));
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("TEST_UNSUPPORTED"));
        boolean threw = false;
        try {
            new RuntimeCapabilityResolver().validate(requirements, profile, catalog);
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("runtime_capability_resolver: UNSUPPORTED status fails closed even if profile lists it available", threw);
    }

    private static void testUnknownCapabilityFailsClosed() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        RuntimeRequirementSet requirements = new RuntimeRequirementSet(Collections.singleton("NOT_IN_CATALOG_AT_ALL"));
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("NOT_IN_CATALOG_AT_ALL"));
        boolean threw = false;
        try {
            new RuntimeCapabilityResolver().validate(requirements, profile, catalog);
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("runtime_capability_resolver: capability unknown to canonical catalog fails closed", threw);
    }

    private static void testExplicitlyAvailableCapabilityValidates() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        RuntimeRequirementSet requirements = new RuntimeRequirementSet(Collections.singleton("MESSAGE_DIALOG"));
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("MESSAGE_DIALOG"));
        boolean threw = false;
        try {
            new RuntimeCapabilityResolver().validate(requirements, profile, catalog);
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("runtime_capability_resolver: explicitly available capability validates without throwing", !threw);
    }

    private static void testStandardJsPrimitiveIsNotCatalogEntry() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        assertTrue("runtime_catalog: window.setTimeout is not a catalog alias",
                catalog.resolveAliasToCapabilityId("window.setTimeout") == null);
        assertTrue("runtime_catalog: Promise is not a catalog alias",
                catalog.resolveAliasToCapabilityId("Promise") == null);
    }

    private static void testDirectWebSquareComponentApiIsNotCatalogEntry() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        assertTrue("runtime_catalog: tabControl.addTab is not a catalog alias",
                catalog.resolveAliasToCapabilityId("tabControl.addTab") == null);
    }

    private static void testShellExecuteIsEnvironmentSpecific() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        CommonRuntimeCapabilityDefinition def = catalog.get("EXTERNAL_PROCESS_EXECUTION");
        assertTrue("runtime_catalog: uc.shellExecute (EXTERNAL_PROCESS_EXECUTION) is ENVIRONMENT_SPECIFIC",
                def != null && def.getSupportStatus() == RuntimeCapabilitySupportStatus.ENVIRONMENT_SPECIFIC);
    }

    private static void testBusinessSpecificAliasIsExtensionClassified() {
        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        String id = catalog.resolveAliasToCapabilityId("uc.tranSendCustomerInfo");
        assertTrue("runtime_catalog: uc.tranSendCustomerInfo resolves under TRANSACTION_RUNTIME_CAPABILITY, "
                        + "not silently promoted to a generic universal core id",
                "TRANSACTION_SEND_CUSTOMER_INFO".equals(id));
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
