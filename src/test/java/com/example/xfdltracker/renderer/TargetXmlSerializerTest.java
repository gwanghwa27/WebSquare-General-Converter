package com.example.xfdltracker.renderer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 외부 의존성 없는(non-JUnit) {@link TargetXmlSerializer} 단위 테스트
 * (SAME_DIRECTORY_TEMP_FILE_PLUS_ATOMIC_REPLACE 방식 publication).
 */
public class TargetXmlSerializerTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSuccessfulUtf8Publication();
        testFinalFileUntouchedWhenSerializationFails();
        testAtomicReplaceOfExistingFinalFile();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testSuccessfulUtf8Publication() throws Exception {
        File dir = Files.createTempDirectory("target-xml-serializer-test").toFile();
        File output = new File(dir, "out.xml");
        Document doc = htmlDocument("hello");

        new TargetXmlSerializer().serialize(doc, output);

        assertTrue("serializer: output file exists after successful serialization", output.isFile());
        String content = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue("serializer: output contains expected element text", content.contains("hello"));
        assertTrue("serializer: no leftover staging file in same directory",
                dir.listFiles().length == 1);
    }

    private static void testFinalFileUntouchedWhenSerializationFails() throws Exception {
        File dir = Files.createTempDirectory("target-xml-serializer-test").toFile();
        File output = new File(dir, "existing.xml");
        Files.write(output.toPath(), "PRE_EXISTING_CONTENT".getBytes(StandardCharsets.UTF_8));

        boolean threw = false;
        try {
            new TargetXmlSerializer().serialize(null, output);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("serializer: invalid-argument call throws before touching output", threw);
        String content = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue("serializer: pre-existing final output preserved when the call fails before serialization",
                "PRE_EXISTING_CONTENT".equals(content));
    }

    private static void testAtomicReplaceOfExistingFinalFile() throws Exception {
        File dir = Files.createTempDirectory("target-xml-serializer-test").toFile();
        File output = new File(dir, "replace.xml");
        Files.write(output.toPath(), "OLD".getBytes(StandardCharsets.UTF_8));

        new TargetXmlSerializer().serialize(htmlDocument("new-content"), output);

        String content = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue("serializer: existing final output atomically replaced with new content",
                content.contains("new-content") && !content.contains("OLD"));
    }

    private static Document htmlDocument(String bodyText) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().newDocument();
        Element html = doc.createElementNS("http://www.w3.org/1999/xhtml", "html");
        doc.appendChild(html);
        Element body = doc.createElementNS("http://www.w3.org/1999/xhtml", "body");
        html.appendChild(body);
        body.setTextContent(bodyText);
        return doc;
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
