package com.example.xfdltracker.renderer;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 완성된 target {@link Document}와 최종 output {@link File}만 받는다(source state 없음). {@link Transformer}
 * 직렬화만 쓴다. 발행은 staging 파일에 쓴 뒤 {@link Files#move}(ATOMIC_MOVE+REPLACE_EXISTING)로 하며,
 * 실패 시 non-atomic fallback 없이 fail-closed한다.
 */
public final class TargetXmlSerializer {

    public void serialize(Document document, File finalOutput) {
        if (document == null || finalOutput == null) {
            throw new IllegalArgumentException("target_xml_serializer: document/finalOutput must not be null");
        }

        byte[] payload = toUtf8Bytes(document);

        File parentDir = finalOutput.getAbsoluteFile().getParentFile();
        if (parentDir != null && !parentDir.isDirectory() && !parentDir.mkdirs() && !parentDir.isDirectory()) {
            throw new IllegalStateException(
                    "target_xml_serializer: failed to create output directory=" + parentDir.getAbsolutePath());
        }

        File staging;
        try {
            staging = File.createTempFile(finalOutput.getName() + ".", ".staging", parentDir);
        } catch (Exception e) {
            throw new IllegalStateException("target_xml_serializer: failed to create staging file", e);
        }

        boolean stagingWriteSucceeded = false;
        try {
            OutputStream out = new FileOutputStream(staging);
            try {
                out.write(payload);
                out.flush();
            } finally {
                out.close();
            }
            stagingWriteSucceeded = true;

            Path stagingPath = staging.toPath();
            Path finalPath = finalOutput.toPath();
            try {
                Files.move(stagingPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                throw new IllegalStateException(
                        "target_xml_serializer: atomic move unavailable on this filesystem -- fail-closed, "
                                + "no non-atomic overwrite fallback permitted", amnse);
            } catch (Exception e) {
                throw new IllegalStateException("target_xml_serializer: atomic publish failed", e);
            }
        } catch (RuntimeException e) {
            if (!stagingWriteSucceeded || staging.exists()) {
                staging.delete();
            }
            throw e;
        } catch (Exception e) {
            staging.delete();
            throw new IllegalStateException("target_xml_serializer: staging write failed", e);
        }
    }

    private byte[] toUtf8Bytes(Document document) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(buffer));
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("target_xml_serializer: in-memory serialization failed", e);
        }
    }
}
