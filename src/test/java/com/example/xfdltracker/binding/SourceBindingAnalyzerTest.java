package com.example.xfdltracker.binding;

import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

/**
 * {@link SourceBindingAnalyzer}의 exact structural identity 기반 resolution 계약을 검증하는
 * offline 단위 테스트(no JUnit). id 문자열 매칭은 이 analyzer 내부 정확히 한 곳에서만 일어나며,
 * 0개/2개 이상 매치는 첫 값을 고르지 않고 후보 전체를 보존한 명시적 상태로 남는다.
 */
public class SourceBindingAnalyzerTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSingleResolvedBindItemFieldsPreservedExactly();
        testUnresolvedNoComponentMatch();
        testAmbiguousDuplicateIdNotFirstPicked();
        testAmbiguousThreeCandidatesAllRetainedDeterministicOrder();
        testMultiplicityPreservationTwoBindItemsSameTarget();
        testMissingCompidStillProducesUnresolvedReference();
        testComponentAgnosticEditTargetDoesNotBecomeCheckBox();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** {@code DatasetBinding.xfdl}과 동일한 구조 -- compid가 정확히 한 Element로 resolve되면
     *  propid/datasetid/columnid 원본 값이 그대로 보존되고 structural identity가 정확히 일치한다. */
    private static void testSingleResolvedBindItemFieldsPreservedExactly() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element bind = doc.createElement("Bind");
        Element bindItem = bindItem(doc, "b1", "edtName", "value", "dsMain", "NAME");
        bind.appendChild(bindItem);
        form.appendChild(bind);
        Element edit = doc.createElement("Edit");
        edit.setAttribute("id", "edtName");
        form.appendChild(edit);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("single-resolved: reference count", "1", String.valueOf(refs.size()));
        SourceBindingReference ref = refs.get(0);
        assertEquals("single-resolved: compid preserved", "edtName", ref.getCompid());
        assertEquals("single-resolved: propid preserved", "value", ref.getPropid());
        assertEquals("single-resolved: datasetid preserved", "dsMain", ref.getDatasetid());
        assertEquals("single-resolved: columnid preserved", "NAME", ref.getColumnid());
        assertEquals("single-resolved: resolution state",
                "RESOLVED_EXACT_ONE_COMPONENT", ref.getResolution().name());
        assertEquals("single-resolved: resolvedComponentStructuralIdentity matches the Edit exactly",
                SourceStructuralIdentity.build(edit), ref.getResolvedComponentStructuralIdentity());
        assertEquals("single-resolved: bindingStructuralIdentity matches the BindItem exactly",
                SourceStructuralIdentity.build(bindItem), ref.getBindingStructuralIdentity());
    }

    /** compid가 문서 어디에도 없는 id를 가리키면 unresolved -- 임의 컴포넌트에 배정하지 않는다. */
    private static void testUnresolvedNoComponentMatch() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(wrapInBind(doc, bindItem(doc, "b1", "doesNotExist", "value", "ds", "COL")));

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("unresolved-no-match: reference count", "1", String.valueOf(refs.size()));
        assertEquals("unresolved-no-match: resolution state",
                "UNRESOLVED_NO_COMPONENT_MATCH", refs.get(0).getResolution().name());
        assertTrue("unresolved-no-match: resolvedComponentStructuralIdentity is null",
                refs.get(0).getResolvedComponentStructuralIdentity() == null);
        assertEquals("unresolved-no-match: candidate list is empty",
                "0", String.valueOf(refs.get(0).getCandidateComponentStructuralIdentities().size()));
    }

    /**
     * SOURCE_BINDING_AMBIGUOUS_TWO_CANDIDATE_TEST -- 같은 id를 가진 Element가 문서 안에 2개
     * 있으면 ambiguous -- 첫 값을 고르지 않고 두 후보 structural identity를 모두 보존한다.
     */
    private static void testAmbiguousDuplicateIdNotFirstPicked() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(wrapInBind(doc, bindItem(doc, "b1", "dup", "checked", "ds", "COL")));
        Element scopeA = doc.createElement("Div");
        Element checkBoxA = doc.createElement("CheckBox");
        checkBoxA.setAttribute("id", "dup");
        scopeA.appendChild(checkBoxA);
        Element scopeB = doc.createElement("Div");
        Element checkBoxB = doc.createElement("CheckBox");
        checkBoxB.setAttribute("id", "dup");
        scopeB.appendChild(checkBoxB);
        form.appendChild(scopeA);
        form.appendChild(scopeB);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("ambiguous-duplicate-id: reference count", "1", String.valueOf(refs.size()));
        assertEquals("ambiguous-duplicate-id: resolution state",
                "UNRESOLVED_AMBIGUOUS_COMPONENT_MATCH", refs.get(0).getResolution().name());
        assertTrue("ambiguous-duplicate-id: resolvedComponentStructuralIdentity is null (no first-pick)",
                refs.get(0).getResolvedComponentStructuralIdentity() == null);
        List<String> candidates = refs.get(0).getCandidateComponentStructuralIdentities();
        assertEquals("ambiguous-duplicate-id: exactly 2 candidates retained", "2", String.valueOf(candidates.size()));
        assertTrue("ambiguous-duplicate-id: candidate A present",
                candidates.contains(SourceStructuralIdentity.build(checkBoxA)));
        assertTrue("ambiguous-duplicate-id: candidate B present",
                candidates.contains(SourceStructuralIdentity.build(checkBoxB)));
    }

    /**
     * SOURCE_BINDING_AMBIGUOUS_THREE_CANDIDATE_TEST -- 같은 id를 가진 Element가 3개 있으면
     * 셋 다 candidate identity로 보존되며(중복 없음, 결정적 순서), 승자를 고르지 않는다.
     */
    private static void testAmbiguousThreeCandidatesAllRetainedDeterministicOrder() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(wrapInBind(doc, bindItem(doc, "b1", "triple", "checked", "ds", "COL")));
        Element c1 = doc.createElement("CheckBox");
        c1.setAttribute("id", "triple");
        Element c2 = doc.createElement("CheckBox");
        c2.setAttribute("id", "triple");
        Element c3 = doc.createElement("CheckBox");
        c3.setAttribute("id", "triple");
        form.appendChild(c1);
        form.appendChild(c2);
        form.appendChild(c3);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("ambiguous-three: reference count", "1", String.valueOf(refs.size()));
        assertEquals("ambiguous-three: resolution state",
                "UNRESOLVED_AMBIGUOUS_COMPONENT_MATCH", refs.get(0).getResolution().name());
        List<String> candidates = refs.get(0).getCandidateComponentStructuralIdentities();
        assertEquals("ambiguous-three: exactly 3 candidates retained (no dedup loss)",
                "3", String.valueOf(candidates.size()));
        String id1 = SourceStructuralIdentity.build(c1);
        String id2 = SourceStructuralIdentity.build(c2);
        String id3 = SourceStructuralIdentity.build(c3);
        assertEquals("ambiguous-three: deterministic order matches document order (candidate[0])",
                id1, candidates.get(0));
        assertEquals("ambiguous-three: deterministic order matches document order (candidate[1])",
                id2, candidates.get(1));
        assertEquals("ambiguous-three: deterministic order matches document order (candidate[2])",
                id3, candidates.get(2));
    }

    /**
     * BIND_ITEM_MULTIPLICITY_PRESERVATION_TEST -- 같은 compid를 가리키는 서로 다른 BindItem 2개가
     * 하나의 Set entry로 뭉개지지 않고 각각 별개 record로, 서로 다른 propid/datasetid/columnid와
     * 함께 보존된다.
     */
    private static void testMultiplicityPreservationTwoBindItemsSameTarget() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element bind = doc.createElement("Bind");
        bind.appendChild(bindItem(doc, "b1", "chkAgree", "checked", "dsAgree", "AGREE"));
        bind.appendChild(bindItem(doc, "b2", "chkAgree", "title", "dsLabel", "LABEL"));
        form.appendChild(bind);
        Element checkBox = doc.createElement("CheckBox");
        checkBox.setAttribute("id", "chkAgree");
        form.appendChild(checkBox);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("multiplicity: both declarations retained (not collapsed to one)",
                "2", String.valueOf(refs.size()));
        boolean sawChecked = false;
        boolean sawTitle = false;
        String expectedResolvedId = SourceStructuralIdentity.build(checkBox);
        for (SourceBindingReference ref : refs) {
            assertEquals("multiplicity: each entry resolves to the same CheckBox",
                    expectedResolvedId, ref.getResolvedComponentStructuralIdentity());
            if ("checked".equals(ref.getPropid())) {
                sawChecked = true;
                assertEquals("multiplicity: checked entry datasetid", "dsAgree", ref.getDatasetid());
                assertEquals("multiplicity: checked entry columnid", "AGREE", ref.getColumnid());
            }
            if ("title".equals(ref.getPropid())) {
                sawTitle = true;
                assertEquals("multiplicity: title entry datasetid", "dsLabel", ref.getDatasetid());
                assertEquals("multiplicity: title entry columnid", "LABEL", ref.getColumnid());
            }
        }
        assertTrue("multiplicity: propid=checked declaration present", sawChecked);
        assertTrue("multiplicity: propid=title declaration present", sawTitle);
    }

    /** compid attribute 자체가 없는 malformed BindItem도 조용히 버리지 않고 unresolved record로 남긴다. */
    private static void testMissingCompidStillProducesUnresolvedReference() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element bind = doc.createElement("Bind");
        Element malformed = doc.createElement("BindItem");
        malformed.setAttribute("id", "bMalformed");
        malformed.setAttribute("propid", "value");
        bind.appendChild(malformed);
        form.appendChild(bind);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("missing-compid: reference count", "1", String.valueOf(refs.size()));
        assertEquals("missing-compid: compid preserved as empty", "", refs.get(0).getCompid());
        assertEquals("missing-compid: resolution state",
                "UNRESOLVED_NO_COMPONENT_MATCH", refs.get(0).getResolution().name());
        assertEquals("missing-compid: candidate list is empty",
                "0", String.valueOf(refs.get(0).getCandidateComponentStructuralIdentities().size()));
    }

    /** component-agnostic 확인 -- Edit를 가리키는 BindItem은 그 Edit로만 resolve되고 옆에 있는
     *  무관한 CheckBox와는 아무 상관이 없다(잘못된 correlation의 반례). */
    private static void testComponentAgnosticEditTargetDoesNotBecomeCheckBox() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(wrapInBind(doc, bindItem(doc, "b1", "edtOnly", "value", "ds", "COL")));
        Element edit = doc.createElement("Edit");
        edit.setAttribute("id", "edtOnly");
        form.appendChild(edit);
        Element unrelatedCheckBox = doc.createElement("CheckBox");
        unrelatedCheckBox.setAttribute("id", "chkUnrelated");
        form.appendChild(unrelatedCheckBox);

        List<SourceBindingReference> refs = new SourceBindingAnalyzer().analyze(form);
        assertEquals("component-agnostic: reference count", "1", String.valueOf(refs.size()));
        assertEquals("component-agnostic: resolves to the Edit, not the unrelated CheckBox",
                SourceStructuralIdentity.build(edit), refs.get(0).getResolvedComponentStructuralIdentity());
    }

    private static Element bindItem(
            Document doc, String id, String compid, String propid, String datasetid, String columnid) {
        Element bindItem = doc.createElement("BindItem");
        bindItem.setAttribute("id", id);
        bindItem.setAttribute("compid", compid);
        bindItem.setAttribute("propid", propid);
        bindItem.setAttribute("datasetid", datasetid);
        bindItem.setAttribute("columnid", columnid);
        return bindItem;
    }

    private static Element wrapInBind(Document doc, Element bindItem) {
        Element bind = doc.createElement("Bind");
        bind.appendChild(bindItem);
        return bind;
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    // ---- assertion 검증(no JUnit) ----

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + label);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
