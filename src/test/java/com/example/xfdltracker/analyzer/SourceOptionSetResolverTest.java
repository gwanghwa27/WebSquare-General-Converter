package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.semantic.SourceOptionItem;
import com.example.xfdltracker.semantic.SourceOptionOriginKind;
import com.example.xfdltracker.semantic.SourceOptionResolution;
import com.example.xfdltracker.semantic.SourceOptionSetEvidence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

/**
 * {@link SourceOptionSetResolver}의 SEARCH_AREA_EXTERNAL_FORM_LOCAL_LITERAL_DATASET_SUBSET(Slice
 * 102D) narrow subset 판정을 단위 테스트한다. 실제 {@code TargetWebSquarePipeline}을 거치지 않고
 * resolver 하나만 직접 호출한다(패키지 동일 -- {@code SourceOptionSetResolver}는 package-private).
 */
public class SourceOptionSetResolverTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testPlainControlWithNoOptionEvidenceReturnsNull();
        testExternalReferenceComboResolvesInDocumentOrder();
        testExternalReferenceRadioResolvesInDocumentOrder();
        testDatasetMissingFailsClosed();
        testDatasetAmbiguousFailsClosed();
        testCodeColumnAttributeMissingFailsClosed();
        testDataColumnAttributeMissingFailsClosed();
        testCodeColumnNotFoundInSchemaFailsClosed();
        testDataColumnNotFoundInSchemaFailsClosed();
        testCodeColumnAmbiguousInSchemaFailsClosed();
        testRowMissingCodeColFailsClosed();
        testRowMissingDataColFailsClosed();
        testRowDuplicateCodeColFailsClosed();
        testRowDuplicateDataColAmbiguousFailsClosed();
        testDataColumnAmbiguousInSchemaFailsClosed();
        testValueEmptyFailsClosed();
        testLabelEmptyFailsClosed();
        testDuplicateValueAcrossRowsFailsClosed();
        testRowsElementMissingFailsClosed();
        testZeroRowElementsFailsClosed();
        testInlineChildDatasetWithoutInnerDatasetAttributeFailsClosed();
        testMalformedPartialDeclarationNoDatasetReferenceFailsClosed();
        testAtPrefixedInnerDatasetIsOutOfScope();
        testNoTrimAppliedToValueOrLabel();

        // ---- Slice 102D Correction: scope/inline/empty-attribute/simple-ID 보강 ----
        testExternalReferenceDirectFormChildDatasetResolves();
        testOtherControlInlineDatasetNotTreatedAsExternalCandidate();
        testDatasetInUnsupportedNestedContainerFailsClosed();
        testDatasetInSiblingFormNotVisibleAcrossFormBoundary();
        testSimpleDatasetIdRejectsNonSimpleForms();
        testSimpleDatasetIdAcceptsRealisticCorpusIds();
        testEmptyInnerDatasetAttributePresentNotDowngradedToPlain();
        testEmptyCodeColumnAttributePresentFailsClosed();
        testEmptyDataColumnAttributePresentFailsClosed();
        testHybridInnerDatasetWithOwnInlineChildFailsClosed();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testPlainControlWithNoOptionEvidenceReturnsNull() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        form.appendChild(combo);

        assertTrue("plain-control: resolve() returns null", SourceOptionSetResolver.resolve(combo) == null);
    }

    private static void testExternalReferenceComboResolvesInDocumentOrder() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM",
                new String[][] {{"A", "Alpha"}, {"B", "Beta"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        SourceOptionResolution resolution = SourceOptionSetResolver.resolve(combo);
        assertTrue("combo-external: resolved", resolution != null && resolution.isResolved());
        SourceOptionSetEvidence evidence = resolution.getEvidence();
        assertEquals("combo-external: originKind",
                "EXTERNAL_FORM_LOCAL_DATASET_REFERENCE", evidence.getOriginKind().name());
        assertEquals("combo-external: sourceDatasetId", "dsCode", evidence.getSourceDatasetId());
        assertEquals("combo-external: codeColumnId", "CD", evidence.getCodeColumnId());
        assertEquals("combo-external: dataColumnId", "NM", evidence.getDataColumnId());
        List<SourceOptionItem> items = evidence.getItems();
        assertEquals("combo-external: item count", "2", String.valueOf(items.size()));
        assertEquals("combo-external: item0 rowOrdinal", "0", String.valueOf(items.get(0).getRowOrdinal()));
        assertEquals("combo-external: item0 value", "A", items.get(0).getValue());
        assertEquals("combo-external: item0 label", "Alpha", items.get(0).getLabel());
        assertEquals("combo-external: item1 rowOrdinal", "1", String.valueOf(items.get(1).getRowOrdinal()));
        assertEquals("combo-external: item1 value", "B", items.get(1).getValue());
        assertEquals("combo-external: item1 label", "Beta", items.get(1).getLabel());
    }

    private static void testExternalReferenceRadioResolvesInDocumentOrder() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"1", "One"}}));

        Element radio = doc.createElement("Radio");
        radio.setAttribute("id", "rdo1");
        radio.setAttribute("innerdataset", "dsCode");
        radio.setAttribute("codecolumn", "CD");
        radio.setAttribute("datacolumn", "NM");
        form.appendChild(radio);

        SourceOptionResolution resolution = SourceOptionSetResolver.resolve(radio);
        assertTrue("radio-external: resolved", resolution != null && resolution.isResolved());
        assertEquals("radio-external: item count", "1", String.valueOf(resolution.getEvidence().getItems().size()));
    }

    private static void testDatasetMissingFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsNotThere");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("dataset-missing", combo, SourceOptionResolution.REASON_DATASET_MISSING);
    }

    private static void testDatasetAmbiguousFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"B", "Beta"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("dataset-ambiguous", combo, SourceOptionResolution.REASON_DATASET_AMBIGUOUS);
    }

    private static void testCodeColumnAttributeMissingFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("codecolumn-missing", combo, SourceOptionResolution.REASON_CODECOLUMN_MISSING);
    }

    private static void testDataColumnAttributeMissingFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        form.appendChild(combo);

        assertFailedReason("datacolumn-missing", combo, SourceOptionResolution.REASON_DATACOLUMN_MISSING);
    }

    private static void testCodeColumnNotFoundInSchemaFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "NOPE");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("codecolumn-not-found", combo, SourceOptionResolution.REASON_COLUMN_NOT_FOUND);
    }

    private static void testDataColumnNotFoundInSchemaFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NOPE");
        form.appendChild(combo);

        assertFailedReason("datacolumn-not-found", combo, SourceOptionResolution.REASON_COLUMN_NOT_FOUND);
    }

    private static void testCodeColumnAmbiguousInSchemaFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        rows.appendChild(buildRow(doc, "CD", "A", "NM", "Alpha"));
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("codecolumn-ambiguous", combo, SourceOptionResolution.REASON_COLUMN_AMBIGUOUS);
    }

    private static void testRowMissingCodeColFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        Element row = doc.createElement("Row");
        row.appendChild(buildCol(doc, "NM", "Alpha"));
        rows.appendChild(row);
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("row-value-missing", combo, SourceOptionResolution.REASON_ROW_VALUE_MISSING);
    }

    private static void testRowMissingDataColFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        Element row = doc.createElement("Row");
        row.appendChild(buildCol(doc, "CD", "A"));
        rows.appendChild(row);
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("row-label-missing", combo, SourceOptionResolution.REASON_ROW_LABEL_MISSING);
    }

    private static void testRowDuplicateCodeColFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        Element row = doc.createElement("Row");
        row.appendChild(buildCol(doc, "CD", "A"));
        row.appendChild(buildCol(doc, "CD", "A2"));
        row.appendChild(buildCol(doc, "NM", "Alpha"));
        rows.appendChild(row);
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("row-value-duplicate-col", combo, SourceOptionResolution.REASON_ROW_VALUE_AMBIGUOUS);
    }

    /** Correction 항목 10 -- row 안에서 data Col이 2개 이상이면 row_label_missing이 아니라
     *  별도 reason(row_label_ambiguous)으로 구분한다. */
    private static void testRowDuplicateDataColAmbiguousFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        Element row = doc.createElement("Row");
        row.appendChild(buildCol(doc, "CD", "A"));
        row.appendChild(buildCol(doc, "NM", "Alpha"));
        row.appendChild(buildCol(doc, "NM", "Alpha2"));
        rows.appendChild(row);
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("row-label-duplicate-col", combo, SourceOptionResolution.REASON_ROW_LABEL_AMBIGUOUS);
    }

    /** Correction 항목 11 -- codecolumn schema duplicate뿐 아니라 datacolumn schema duplicate도
     *  동일하게 column ambiguous로 fail-closed해야 한다. */
    private static void testDataColumnAmbiguousInSchemaFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        rows.appendChild(buildRow(doc, "CD", "A", "NM", "Alpha"));
        dataset.appendChild(rows);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("datacolumn-schema-ambiguous", combo, SourceOptionResolution.REASON_COLUMN_AMBIGUOUS);
    }

    private static void testValueEmptyFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("value-empty", combo, SourceOptionResolution.REASON_VALUE_EMPTY);
    }

    private static void testLabelEmptyFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", ""}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("label-empty", combo, SourceOptionResolution.REASON_LABEL_EMPTY);
    }

    private static void testDuplicateValueAcrossRowsFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM",
                new String[][] {{"A", "Alpha"}, {"A", "AlphaAgain"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("value-duplicate", combo, SourceOptionResolution.REASON_VALUE_DUPLICATE);
    }

    private static void testRowsElementMissingFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("rows-missing", combo, SourceOptionResolution.REASON_ROWS_MISSING);
    }

    private static void testZeroRowElementsFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "dsCode");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "CD"));
        columnInfo.appendChild(buildColumn(doc, "NM"));
        dataset.appendChild(columnInfo);
        dataset.appendChild(doc.createElement("Rows"));
        objects.appendChild(dataset);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("zero-rows", combo, SourceOptionResolution.REASON_ROWS_EMPTY);
    }

    private static void testInlineChildDatasetWithoutInnerDatasetAttributeFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element radio = doc.createElement("Radio");
        radio.setAttribute("id", "rdoLiteral");
        radio.setAttribute("codecolumn", "codecolumn");
        radio.setAttribute("datacolumn", "datacolumn");
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "innerdataset");
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, "codecolumn"));
        columnInfo.appendChild(buildColumn(doc, "datacolumn"));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        rows.appendChild(buildRow(doc, "codecolumn", "0", "datacolumn", "SOHO"));
        dataset.appendChild(rows);
        radio.appendChild(dataset);
        form.appendChild(radio);

        assertFailedReason("inline-child-dataset-guard", radio, SourceOptionResolution.REASON_INLINE_UNPROVEN);
    }

    private static void testMalformedPartialDeclarationNoDatasetReferenceFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("malformed-partial-no-dataset", combo, SourceOptionResolution.REASON_DATASET_MISSING);
    }

    private static void testAtPrefixedInnerDatasetIsOutOfScope() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "ds_BRcombo", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "@ds_BRcombo");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("at-prefix-scope-unsupported", combo, SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
    }

    /** trim을 통한 의미 변경 금지(항목 2/7) -- 앞뒤 공백이 있는 실제 semantic text는 그대로
     *  보존돼야 한다(빈 문자열로 오판하지 않되, 임의로 다듬지도 않는다). */
    private static void testNoTrimAppliedToValueOrLabel() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element objects = doc.createElement("Objects");
        form.appendChild(objects);
        objects.appendChild(buildDataset(doc, "dsCode", "CD", "NM",
                new String[][] {{" A ", " Alpha "}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        SourceOptionResolution resolution = SourceOptionSetResolver.resolve(combo);
        assertTrue("no-trim: resolved", resolution != null && resolution.isResolved());
        SourceOptionItem item0 = resolution.getEvidence().getItems().get(0);
        assertEquals("no-trim: value preserved verbatim", " A ", item0.getValue());
        assertEquals("no-trim: label preserved verbatim", " Alpha ", item0.getLabel());
    }

    /** Correction 항목 6(positive) -- Objects wrapper 없이 Form 직계 자식 Dataset도 허용
     *  external 위치다. */
    private static void testExternalReferenceDirectFormChildDatasetResolves() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        SourceOptionResolution resolution = SourceOptionSetResolver.resolve(combo);
        assertTrue("form-direct-child-dataset: resolved", resolution != null && resolution.isResolved());
        assertEquals("form-direct-child-dataset: item count",
                "1", String.valueOf(resolution.getEvidence().getItems().size()));
    }

    /** Correction 항목 5/14.A -- 다른 control(Radio)의 inline child Dataset과 같은 id를 별도
     *  Combo가 innerdataset으로 참조해도 그 inline Dataset을 external candidate로 승격하면 안
     *  된다. 허용 위치에 같은 id가 전혀 없으므로 scope_unsupported로 fail-closed해야 한다. */
    private static void testOtherControlInlineDatasetNotTreatedAsExternalCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element otherRadio = doc.createElement("Radio");
        otherRadio.setAttribute("id", "rdoOther");
        otherRadio.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));
        form.appendChild(otherRadio);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("other-control-inline-leakage", combo, SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
    }

    /** Correction 항목 9(unsupported nested location) -- Objects도 아니고 control 자신도 아닌
     *  일반 container(GroupBox) 아래의 Dataset은 허용 external 위치가 아니다. */
    private static void testDatasetInUnsupportedNestedContainerFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element groupBox = doc.createElement("GroupBox");
        groupBox.setAttribute("id", "grp1");
        groupBox.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));
        form.appendChild(groupBox);

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("nested-container-scope-unsupported", combo, SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
    }

    /** Correction 항목 3(CONTROL_NEAREST_ENCLOSING_FORM) -- 다른 sibling Form 안의 같은 id
     *  Dataset은 이 control의 resolution scope 밖이다(Form 경계를 넘지 않는다). */
    private static void testDatasetInSiblingFormNotVisibleAcrossFormBoundary() throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("Root");
        doc.appendChild(root);

        Element otherForm = doc.createElement("Form");
        otherForm.setAttribute("id", "otherForm");
        otherForm.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));
        root.appendChild(otherForm);

        Element form = doc.createElement("Form");
        form.setAttribute("id", "thisForm");
        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);
        root.appendChild(form);

        assertFailedReason("cross-form-boundary-dataset-missing", combo, SourceOptionResolution.REASON_DATASET_MISSING);
    }

    /** Correction 항목 8 -- simple Dataset ID predicate([A-Za-z_][A-Za-z0-9_]*) 위반은 전부
     *  scope_unsupported다(정규화/trim/대소문자무시 없이 그대로 판정). */
    private static void testSimpleDatasetIdRejectsNonSimpleForms() throws Exception {
        String[] nonSimpleIds = {"Base::dsCode", "ds.code", "ds/code", "ds code", "1dsCode"};
        for (String invalidId : nonSimpleIds) {
            Document doc = newDocument();
            Element form = doc.createElement("Form");
            doc.appendChild(form);
            Element combo = doc.createElement("Combo");
            combo.setAttribute("id", "cbo1");
            combo.setAttribute("innerdataset", invalidId);
            combo.setAttribute("codecolumn", "CD");
            combo.setAttribute("datacolumn", "NM");
            form.appendChild(combo);

            assertFailedReason("non-simple-id[" + invalidId + "]", combo,
                    SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
        }
    }

    /** Correction 항목 8 -- 실제 corpus 스타일 simple id(밑줄/숫자 포함, leading underscore)는
     *  predicate를 만족해 정상적으로 external dataset 탐색으로 이어져야 한다. */
    private static void testSimpleDatasetIdAcceptsRealisticCorpusIds() throws Exception {
        String[] validIds = {"dsCode", "ds_Code1", "_dsCode", "DS1"};
        for (String validId : validIds) {
            Document doc = newDocument();
            Element form = doc.createElement("Form");
            doc.appendChild(form);
            form.appendChild(buildDataset(doc, validId, "CD", "NM", new String[][] {{"A", "Alpha"}}));

            Element combo = doc.createElement("Combo");
            combo.setAttribute("id", "cbo1");
            combo.setAttribute("innerdataset", validId);
            combo.setAttribute("codecolumn", "CD");
            combo.setAttribute("datacolumn", "NM");
            form.appendChild(combo);

            SourceOptionResolution resolution = SourceOptionSetResolver.resolve(combo);
            assertTrue("simple-id-corpus[" + validId + "]: resolved",
                    resolution != null && resolution.isResolved());
        }
    }

    /** Correction 항목 7/14.C -- innerdataset attribute가 존재하되 값이 빈 문자열이면 plain
     *  null로 강등되지 않고 dataset missing으로 fail-closed해야 한다. */
    private static void testEmptyInnerDatasetAttributePresentNotDowngradedToPlain() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "");
        form.appendChild(combo);

        assertFailedReason("empty-innerdataset-not-plain", combo, SourceOptionResolution.REASON_DATASET_MISSING);
    }

    /** Correction 항목 7 -- codecolumn attribute가 존재하되 값이 빈 문자열이어도(단순 absent가
     *  아니라 presence 기반) 동일한 codecolumn_missing reason으로 fail-closed해야 한다. */
    private static void testEmptyCodeColumnAttributePresentFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "");
        combo.setAttribute("datacolumn", "NM");
        form.appendChild(combo);

        assertFailedReason("empty-codecolumn-present", combo, SourceOptionResolution.REASON_CODECOLUMN_MISSING);
    }

    /** Correction 항목 7 -- datacolumn attribute presence 기반 empty 처리도 동일하게
     *  datacolumn_missing으로 fail-closed해야 한다. */
    private static void testEmptyDataColumnAttributePresentFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "");
        form.appendChild(combo);

        assertFailedReason("empty-datacolumn-present", combo, SourceOptionResolution.REASON_DATACOLUMN_MISSING);
    }

    /** Correction 항목 4/14.B(hybrid) -- innerdataset이 유효한 external Dataset을 정확히
     *  가리켜도, control 자신의 direct child Dataset이 함께 있으면 RESOLVED로 승격되면 안 된다. */
    private static void testHybridInnerDatasetWithOwnInlineChildFailsClosed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(buildDataset(doc, "dsCode", "CD", "NM", new String[][] {{"A", "Alpha"}}));

        Element combo = doc.createElement("Combo");
        combo.setAttribute("id", "cbo1");
        combo.setAttribute("innerdataset", "dsCode");
        combo.setAttribute("codecolumn", "CD");
        combo.setAttribute("datacolumn", "NM");
        combo.appendChild(buildDataset(doc, "dsInlineOwn", "CD", "NM", new String[][] {{"X", "XLabel"}}));
        form.appendChild(combo);

        assertFailedReason("hybrid-inner-dataset-with-own-inline", combo,
                SourceOptionResolution.REASON_INLINE_UNPROVEN);
    }

    // ---- fixture 생성 도우미 ----------------------------------------------------------------

    private static Element buildDataset(
            Document doc, String datasetId, String codeColId, String dataColId, String[][] rowsData) {
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", datasetId);
        Element columnInfo = doc.createElement("ColumnInfo");
        columnInfo.appendChild(buildColumn(doc, codeColId));
        columnInfo.appendChild(buildColumn(doc, dataColId));
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        for (String[] rowData : rowsData) {
            rows.appendChild(buildRow(doc, codeColId, rowData[0], dataColId, rowData[1]));
        }
        dataset.appendChild(rows);
        return dataset;
    }

    private static Element buildColumn(Document doc, String id) {
        Element column = doc.createElement("Column");
        column.setAttribute("id", id);
        return column;
    }

    private static Element buildRow(Document doc, String codeColId, String codeValue, String dataColId, String dataValue) {
        Element row = doc.createElement("Row");
        row.appendChild(buildCol(doc, codeColId, codeValue));
        row.appendChild(buildCol(doc, dataColId, dataValue));
        return row;
    }

    private static Element buildCol(Document doc, String id, String textContent) {
        Element col = doc.createElement("Col");
        col.setAttribute("id", id);
        col.setTextContent(textContent);
        return col;
    }

    private static void assertFailedReason(String label, Element control, String expectedReason) {
        SourceOptionResolution resolution = SourceOptionSetResolver.resolve(control);
        assertTrue(label + ": resolution non-null", resolution != null);
        assertTrue(label + ": not resolved (failed)", !resolution.isResolved());
        assertEquals(label + ": failure reason", expectedReason, resolution.getFailureReason());
    }

    private static Document newDocument() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("[FAIL] " + label + " -- expected=<" + expected + "> actual=<" + actual + ">");
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            System.out.println("[FAIL] " + label);
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
