package com.example.xfdltracker.composition;

import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadCategory;
import com.example.xfdltracker.renderer.AtomicRenderResult;
import com.example.xfdltracker.renderer.AtomicWebSquareRenderer;
import com.example.xfdltracker.renderer.RenderStatus;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Java 8, dependency-free, offline mandatory conformance verifier for the Slice 85
 * evidence-scoped runtime exception design (Slice 86 implementation). Not part of the
 * Production converter -- this is a standalone offline verification tool, placed in the
 * {@code com.example.xfdltracker.composition} package only so it can construct minimal
 * {@link TargetCompositionNode}/{@link TargetCompositionPlan} fixtures directly (package-private
 * constructors), exactly the same access pattern the existing {@code AtomicWebSquareRendererTest}
 * already uses for its own tamper fixtures.
 *
 * <p>This tool is a CONSUMER, never an authority. It checks three things against each other and
 * fails closed on any mismatch -- it does not itself decide what is lawful:
 * <ol>
 *   <li>the canonical authorization declaration (a {@code .properties} file in the sanctioned
 *       catalog/design repository) against the four exact records this Slice accepted;</li>
 *   <li>each declared evidence locator (root identity + root-relative path + SHA256) against the
 *       actual file content at that exact, explicitly-configured root binding;</li>
 *   <li>the renderer's actual emitted target scaffolding (via real
 *       {@link AtomicWebSquareRenderer#render}, not string search on its source) against the
 *       declaration.</li>
 * </ol>
 *
 * <p>Renderer runtime input contract is not touched by this tool: the renderer is exercised only
 * through its existing public {@code render(TargetCompositionPlan, List<TargetNodePayload>)}
 * entry point, exactly as any other caller would use it. This verifier does not add any new
 * runtime input to the renderer and does not read the declaration/evidence into the renderer
 * itself -- the renderer never sees this tool.
 */
public final class RuntimeExceptionConformanceVerifier {

    private static final String KNOWN_ROOT_IDENTITY = "XPLATFORM_WEBSQUARE_VALIDATION_PROJECT_ROOT";

    private static final class ExpectedRecord {
        final String family, variant, locus, token, authorityKind, evidenceRelativePath, evidenceSha256;

        ExpectedRecord(String family, String variant, String locus, String token, String authorityKind,
                String evidenceRelativePath, String evidenceSha256) {
            this.family = family;
            this.variant = variant;
            this.locus = locus;
            this.token = token;
            this.authorityKind = authorityKind;
            this.evidenceRelativePath = evidenceRelativePath;
            this.evidenceSha256 = evidenceSha256;
        }

        String key() {
            return family + "." + variant + "." + locus + "." + token;
        }
    }

    /** Exactly the four records ACCEPTED in Slice 85/86. Nothing else is ever lawful. */
    private static final List<ExpectedRecord> EXPECTED_RECORDS = Collections.unmodifiableList(Arrays.asList(
            new ExpectedRecord("TITLE_BAR", "title_only", "ROOT_WRAPPER", "dfbox",
                    "CURRENT_PROJECT_TARGET_RUNTIME_STYLING_DEPENDENCY",
                    "work/websquare-devpack-copy/tomcat/webapps/ROOT/cm/css/base.css",
                    "f2ac13f63211d943709b4599a51ff572c17c608ea4fdf9f895cd8b4109fbfb2b"),
            new ExpectedRecord("BUSINESS_TABLE", "horizontal", "ROOT_WRAPPER", "tbbox",
                    "CURRENT_PROJECT_TARGET_RUNTIME_STYLING_DEPENDENCY",
                    "work/websquare-devpack-copy/tomcat/webapps/ROOT/cm/css/base.css",
                    "f2ac13f63211d943709b4599a51ff572c17c608ea4fdf9f895cd8b4109fbfb2b"),
            new ExpectedRecord("BUSINESS_TABLE", "horizontal", "TABLE_WRAPPER", "w2tb",
                    "WEBSQUARE_FRAMEWORK_RUNTIME_REQUIREMENT",
                    "work/websquare-devpack-copy/tomcat/webapps/ROOT/websquare/_websquare_/wbd_B5170_babel_main.js",
                    "f9343fbf345c6cf867149f9a85d846554000bca9032fbafd9851d991d3c07ecf"),
            new ExpectedRecord("BUSINESS_TABLE", "horizontal", "TABLE_WRAPPER", "tb",
                    "CURRENT_PROJECT_TARGET_RUNTIME_STYLING_DEPENDENCY",
                    "work/websquare-devpack-copy/tomcat/webapps/ROOT/cm/css/base.css",
                    "f2ac13f63211d943709b4599a51ff572c17c608ea4fdf9f895cd8b4109fbfb2b")
    ));

    /** Frozen forbidden classes never authorized by this or any current declaration. */
    private static final Set<String> FORBIDDEN_TOKENS = Collections.unmodifiableSet(new LinkedHashSet<String>(
            Arrays.asList("shbox", "shbox_inner", "tbbox", "w2tb", "tb", "dfbox")));

    private static final List<String> FAILURES = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("usage: java RuntimeExceptionConformanceVerifier "
                    + "<runtime_exception_authorizations.properties> <" + KNOWN_ROOT_IDENTITY + "_concrete_root_dir>");
            System.exit(2);
            return;
        }
        File declarationFile = new File(args[0]);
        File sanctionedRootDir = new File(args[1]);

        Properties declaration = loadDeclaration(declarationFile);
        if (declaration != null) {
            verifyDeclaration(declaration);
            verifyEvidence(declaration, sanctionedRootDir);
        }
        verifyRendererConformance();

        if (FAILURES.isEmpty()) {
            System.out.println("[PASS] RuntimeExceptionConformanceVerifier: all checks passed "
                    + "(" + EXPECTED_RECORDS.size() + " authorized records).");
            System.exit(0);
        } else {
            for (String f : FAILURES) {
                System.out.println("[FAIL] " + f);
            }
            System.out.println(FAILURES.size() + " CONFORMANCE FAILURE(S)");
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------------------------------
    // 1. Canonical declaration validation
    // ---------------------------------------------------------------------------------------

    private static Properties loadDeclaration(File declarationFile) {
        if (!declarationFile.isFile()) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: canonical declaration missing at "
                    + declarationFile.getPath());
            return null;
        }
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(declarationFile)) {
            p.load(in);
        } catch (Exception e) {
            FAILURES.add("canonical declaration unreadable: " + declarationFile.getPath() + " (" + e + ")");
            return null;
        }
        return p;
    }

    private static void verifyDeclaration(Properties declaration) {
        String recordsRaw = declaration.getProperty("exception.records");
        if (recordsRaw == null || recordsRaw.trim().length() == 0) {
            FAILURES.add("declaration missing required key exception.records (or it is blank -- "
                    + "no wildcard/empty value is accepted as a valid record list)");
            return;
        }
        String[] declaredKeys = recordsRaw.split(",");
        List<String> declaredKeyList = new ArrayList<String>();
        for (String k : declaredKeys) {
            String trimmed = k.trim();
            if (trimmed.length() == 0) {
                FAILURES.add("declaration exception.records contains an empty entry -- fake/empty "
                        + "sentinel values are not accepted");
                continue;
            }
            declaredKeyList.add(trimmed);
        }

        // duplicate applicability
        Set<String> seen = new LinkedHashSet<String>();
        for (String k : declaredKeyList) {
            if (!seen.add(k)) {
                FAILURES.add("declaration lists duplicate applicability key: " + k);
            }
        }

        // exactly the four expected records, no unknown extras, none missing
        Set<String> expectedKeys = new LinkedHashSet<String>();
        for (ExpectedRecord r : EXPECTED_RECORDS) {
            expectedKeys.add(r.key());
        }
        for (String k : declaredKeyList) {
            if (!expectedKeys.contains(k)) {
                FAILURES.add("declaration authorizes unknown extra record (not part of the accepted "
                        + "four-record scope): " + k);
            }
        }
        for (String expected : expectedKeys) {
            if (!declaredKeyList.contains(expected)) {
                FAILURES.add("declaration is missing an accepted authorized record: " + expected);
            }
        }

        // exact field-by-field check for each expected record actually present
        for (ExpectedRecord r : EXPECTED_RECORDS) {
            String prefix = "exception." + r.key() + ".";
            checkField(prefix, "family", r.family, declaration);
            checkField(prefix, "variant", r.variant, declaration);
            checkField(prefix, "locus", r.locus, declaration);
            checkField(prefix, "token", r.token, declaration);
            checkField(prefix, "evidenceAuthorityKind", r.authorityKind, declaration);
            checkField(prefix, "evidenceRootIdentity", KNOWN_ROOT_IDENTITY, declaration);
            checkField(prefix, "evidenceRelativePath", r.evidenceRelativePath, declaration);
            checkField(prefix, "evidenceSha256", r.evidenceSha256, declaration);
            checkField(prefix, "authorizationStatus", "EXCEPTION_AUTHORIZED", declaration);
        }

        // forbidden tokens never listed as authorized under any other family/variant/locus
        for (String token : FORBIDDEN_TOKENS) {
            boolean isOneOfTheFour = false;
            for (ExpectedRecord r : EXPECTED_RECORDS) {
                if (r.token.equals(token)) { isOneOfTheFour = true; break; }
            }
            if (isOneOfTheFour) continue;
            for (String k : declaredKeyList) {
                if (k.endsWith("." + token)) {
                    FAILURES.add("declaration authorizes forbidden token outside the accepted scope: " + k);
                }
            }
        }
    }

    private static void checkField(String prefix, String field, String expected, Properties declaration) {
        String actual = declaration.getProperty(prefix + field);
        if (actual == null || actual.trim().length() == 0) {
            FAILURES.add("declaration " + prefix + field + " is missing/empty (expected exact value \""
                    + expected + "\"; wildcard/empty is never accepted)");
            return;
        }
        if (!expected.equals(actual.trim())) {
            FAILURES.add("declaration " + prefix + field + " mismatch: expected=\"" + expected
                    + "\" actual=\"" + actual.trim() + "\"");
        }
    }

    // ---------------------------------------------------------------------------------------
    // 2. Evidence source validation (root-anchored relative path + SHA256, fail-closed)
    // ---------------------------------------------------------------------------------------

    private static void verifyEvidence(Properties declaration, File sanctionedRootDir) {
        File canonicalRoot;
        try {
            canonicalRoot = sanctionedRootDir.getCanonicalFile();
        } catch (Exception e) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: sanctioned root binding "
                    + "unavailable/unreadable: " + sanctionedRootDir.getPath());
            return;
        }
        if (!canonicalRoot.isDirectory()) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: sanctioned root binding is "
                    + "not a directory: " + canonicalRoot.getPath());
            return;
        }

        // Deduplicate identical (relativePath, sha256) pairs so the same evidence file is not
        // hashed three times, but every declared record is still individually checked.
        for (ExpectedRecord r : EXPECTED_RECORDS) {
            String prefix = "exception." + r.key() + ".";
            String declaredRootIdentity = declaration.getProperty(prefix + "evidenceRootIdentity");
            if (declaredRootIdentity == null || !KNOWN_ROOT_IDENTITY.equals(declaredRootIdentity.trim())) {
                FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: " + r.key()
                        + " declares an evidence root identity that is not the known sanctioned root ("
                        + KNOWN_ROOT_IDENTITY + "): " + declaredRootIdentity);
                continue;
            }
            String declaredRelativePath = declaration.getProperty(prefix + "evidenceRelativePath");
            String declaredSha256 = declaration.getProperty(prefix + "evidenceSha256");
            if (declaredRelativePath == null || declaredSha256 == null) {
                continue; // already reported by verifyDeclaration
            }
            resolveAndVerify(r.key(), canonicalRoot, declaredRelativePath.trim(), declaredSha256.trim());
        }
    }

    private static void resolveAndVerify(String recordKey, File canonicalRoot, String relativePath, String expectedSha256) {
        // No filename search, no recursive search, no CWD-relative resolution: exactly
        // root + relativePath, then verify the canonicalized result did not escape the root.
        File candidate = new File(canonicalRoot, relativePath);
        File canonicalCandidate;
        try {
            canonicalCandidate = candidate.getCanonicalFile();
        } catch (Exception e) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: " + recordKey
                    + " evidence path could not be canonicalized: " + candidate.getPath());
            return;
        }
        String rootPath = canonicalRoot.getPath() + File.separator;
        if (!canonicalCandidate.getPath().startsWith(rootPath)) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: " + recordKey
                    + " evidence path resolves outside the sanctioned root (rejected as root escape / "
                    + "path traversal): " + canonicalCandidate.getPath());
            return;
        }
        if (!canonicalCandidate.isFile()) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: " + recordKey
                    + " required evidence source missing: " + canonicalCandidate.getPath());
            return;
        }
        String actualSha256;
        try {
            actualSha256 = sha256(canonicalCandidate);
        } catch (Exception e) {
            FAILURES.add(recordKey + " could not compute SHA256 for " + canonicalCandidate.getPath() + " (" + e + ")");
            return;
        }
        if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
            FAILURES.add("EXCEPTION_EVIDENCE_CONTEXT_STALE_OR_MISMATCH: " + recordKey
                    + " evidence SHA256 mismatch (evidence context is stale and needs re-confirmation, "
                    + "NOT treated as rejection): expected=" + expectedSha256 + " actual=" + actualSha256
                    + " path=" + canonicalCandidate.getPath());
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) >= 0) {
                digest.update(buf, 0, n);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    // ---------------------------------------------------------------------------------------
    // 3. Renderer implementation conformance (real render(), DOM inspection, no source reanalysis)
    // ---------------------------------------------------------------------------------------

    private static void verifyRendererConformance() {
        verifyTitleBar();
        verifyBusinessTable();
    }

    private static void verifyTitleBar() {
        TargetCompositionNode node = new TargetCompositionNode(
                "verifier:title_bar_root", "TITLE_BAR", "title_only", "HIGH",
                Collections.<String, Object>emptyMap(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "verifier:title_bar_root",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "verifier:title_bar_root"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());
        TargetNodePayload payload = new TargetNodePayload(
                node.getIdentityKind(), "verifier:title_bar_root", Collections.<TargetLeafPayload>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer()
                .render(plan, Collections.singletonList(payload));
        if (results.size() != 1 || results.get(0).getStatus() != RenderStatus.RENDERED) {
            FAILURES.add("renderer conformance: TITLE_BAR.title_only minimal fixture did not render "
                    + "(drift from accepted renderer behavior) -- status="
                    + (results.isEmpty() ? "NONE" : results.get(0).getStatus()));
            return;
        }
        Element root = results.get(0).getTargetElement();
        checkExactClassSet("TITLE_BAR.title_only root wrapper", root, singleton("dfbox"));
        checkNoForbiddenTokenBeyondAuthorized(root, singleton("dfbox"));
    }

    private static void verifyBusinessTable() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("row_count", Integer.valueOf(1));
        params.put("column_pair_count", Integer.valueOf(1));
        TargetCompositionNode node = new TargetCompositionNode(
                "verifier:business_table_root", "BUSINESS_TABLE", "horizontal", "HIGH",
                params, null, CompositionDecision.Origin.SOURCE_SEMANTIC, "verifier:business_table_root",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "verifier:business_table_root"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());

        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", Integer.valueOf(0));
        labelData.put("cellIndexInRow", Integer.valueOf(0));
        labelData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "verifier-label", labelData,
                "verifier_fixture", null);

        Map<String, Object> controlData = new LinkedHashMap<String, Object>();
        controlData.put("rowIndex", Integer.valueOf(0));
        controlData.put("cellIndexInRow", Integer.valueOf(1));
        controlData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload control = new TargetLeafPayload(
                TargetPayloadCategory.CONTROL_TYPE, "Edit", controlData,
                "verifier_fixture", null);

        TargetNodePayload payload = new TargetNodePayload(
                node.getIdentityKind(), "verifier:business_table_root", Arrays.asList(label, control));

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer()
                .render(plan, Collections.singletonList(payload));
        if (results.size() != 1 || results.get(0).getStatus() != RenderStatus.RENDERED) {
            FAILURES.add("renderer conformance: BUSINESS_TABLE.horizontal minimal 1x1 fixture did not "
                    + "render (drift from accepted renderer behavior) -- status="
                    + (results.isEmpty() ? "NONE" : results.get(0).getStatus()));
            return;
        }
        Element root = results.get(0).getTargetElement();
        checkExactClassSet("BUSINESS_TABLE.horizontal root wrapper", root, singleton("tbbox"));

        Element tableWrapper = findFirstDescendantWithTagname(root, "table");
        if (tableWrapper == null) {
            FAILURES.add("renderer conformance: BUSINESS_TABLE.horizontal table wrapper "
                    + "(tagname=table) not found in emitted structure");
        } else {
            Set<String> expectedTableClasses = new LinkedHashSet<String>(Arrays.asList("w2tb", "tb"));
            checkExactClassSet("BUSINESS_TABLE.horizontal table wrapper", tableWrapper, expectedTableClasses);
        }
        checkNoForbiddenTokenBeyondAuthorized(root, new LinkedHashSet<String>(Arrays.asList("tbbox", "w2tb", "tb")));
    }

    private static Set<String> singleton(String s) {
        return Collections.singleton(s);
    }

    private static void checkExactClassSet(String label, Element element, Set<String> expected) {
        String classAttr = element.getAttribute("class");
        Set<String> actual = new LinkedHashSet<String>();
        for (String token : classAttr.trim().split("\\s+")) {
            if (token.length() > 0) actual.add(token);
        }
        if (!actual.equals(expected)) {
            FAILURES.add("renderer conformance: " + label + " class set mismatch: expected=" + expected
                    + " actual=" + actual);
        }
    }

    /** Confirms no forbidden token beyond the ones authorized for this fragment appears anywhere
     * in the emitted subtree's class attributes -- structural conformance only, not a re-analysis
     * of source/semantic meaning. */
    private static void checkNoForbiddenTokenBeyondAuthorized(Element root, Set<String> authorizedForThisFragment) {
        List<String> allClassTokens = new ArrayList<String>();
        collectAllClassTokens(root, allClassTokens);
        for (String token : allClassTokens) {
            if (FORBIDDEN_TOKENS.contains(token) && !authorizedForThisFragment.contains(token)) {
                FAILURES.add("renderer conformance: unauthorized forbidden token emitted: " + token);
            }
        }
    }

    private static void collectAllClassTokens(Node node, List<String> out) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap attrs = node.getAttributes();
            Node classAttr = attrs == null ? null : attrs.getNamedItem("class");
            if (classAttr != null) {
                for (String token : classAttr.getNodeValue().trim().split("\\s+")) {
                    if (token.length() > 0) out.add(token);
                }
            }
        }
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectAllClassTokens(children.item(i), out);
        }
    }

    private static Element findFirstDescendantWithTagname(Element root, String tagname) {
        if (tagname.equals(root.getAttribute("tagname"))) {
            return root;
        }
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element found = findFirstDescendantWithTagname((Element) child, tagname);
                if (found != null) return found;
            }
        }
        return null;
    }

    private RuntimeExceptionConformanceVerifier() {
    }
}
