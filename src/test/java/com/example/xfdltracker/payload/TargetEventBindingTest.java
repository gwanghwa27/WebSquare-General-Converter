package com.example.xfdltracker.payload;

/**
 * 외부 의존성 없는(no JUnit) offline 단위 테스트다. 관련 대상: {@link TargetEventBinding}.
 */
public class TargetEventBindingTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testValidConstruction();
        testNegativeOrdinalRejected();
        testBlankEventNameRejected();
        testNullEventNameRejected();
        testBlankFunctionIdentifierRejected();
        testNullFunctionIdentifierRejected();
        testEqualsAndHashCodeExact();
        testEqualsDiffersOnAnyField();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testValidConstruction() {
        TargetEventBinding binding = new TargetEventBinding(0, "onclick", "fn_save");
        assertEquals("valid: buttonOrdinal", "0", String.valueOf(binding.getButtonOrdinal()));
        assertEquals("valid: targetEventLocalName", "onclick", binding.getTargetEventLocalName());
        assertEquals("valid: targetFunctionIdentifier", "fn_save", binding.getTargetFunctionIdentifier());
    }

    private static void testNegativeOrdinalRejected() {
        try {
            new TargetEventBinding(-1, "onclick", "fn_save");
            fail("negative_ordinal: expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass("negative_ordinal: rejected");
        }
    }

    private static void testBlankEventNameRejected() {
        try {
            new TargetEventBinding(0, "   ", "fn_save");
            fail("blank_event_name: expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass("blank_event_name: rejected");
        }
    }

    private static void testNullEventNameRejected() {
        try {
            new TargetEventBinding(0, null, "fn_save");
            fail("null_event_name: expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass("null_event_name: rejected");
        }
    }

    private static void testBlankFunctionIdentifierRejected() {
        try {
            new TargetEventBinding(0, "onclick", "   ");
            fail("blank_function_identifier: expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass("blank_function_identifier: rejected");
        }
    }

    private static void testNullFunctionIdentifierRejected() {
        try {
            new TargetEventBinding(0, "onclick", null);
            fail("null_function_identifier: expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass("null_function_identifier: rejected");
        }
    }

    private static void testEqualsAndHashCodeExact() {
        TargetEventBinding a = new TargetEventBinding(2, "onclick", "fn_save");
        TargetEventBinding b = new TargetEventBinding(2, "onclick", "fn_save");
        assertTrue("equals_exact: equal instances", a.equals(b));
        assertTrue("equals_exact: symmetric", b.equals(a));
        assertEquals("equals_exact: hashCode", String.valueOf(a.hashCode()), String.valueOf(b.hashCode()));
    }

    private static void testEqualsDiffersOnAnyField() {
        TargetEventBinding base = new TargetEventBinding(2, "onclick", "fn_save");
        assertFalse("differs: ordinal", base.equals(new TargetEventBinding(3, "onclick", "fn_save")));
        assertFalse("differs: eventName", base.equals(new TargetEventBinding(2, "onblur", "fn_save")));
        assertFalse("differs: functionIdentifier", base.equals(new TargetEventBinding(2, "onclick", "fn_other")));
        assertFalse("differs: null", base.equals(null));
        assertFalse("differs: other type", base.equals("not a binding"));
    }

    // ---- assertion 도우미 ----

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(label + " -- expected=<" + expected + "> actual=<" + actual + ">");
        } else {
            pass(label);
        }
    }

    private static void assertTrue(String label, boolean actual) {
        if (!actual) {
            fail(label + " -- expected true");
        } else {
            pass(label);
        }
    }

    private static void assertFalse(String label, boolean actual) {
        if (actual) {
            fail(label + " -- expected false");
        } else {
            pass(label);
        }
    }

    private static void pass(String label) {
        System.out.println("[PASS] " + label);
    }

    private static void fail(String label) {
        System.out.println("[FAIL] " + label);
        failures++;
    }
}
