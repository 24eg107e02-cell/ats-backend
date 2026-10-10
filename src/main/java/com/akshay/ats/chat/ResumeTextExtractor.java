package com.akshay.ats.chat;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads plain text out of an uploaded PDF or DOCX resume. Nothing is written to disk. */
@Component
public class ResumeTextExtractor {

    private static final int MAX_XML_BYTES = 10 * 1024 * 1024;

    public String extract(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a resume file.");
        }
        byte[] bytes = file.getBytes();

        if (startsWith(bytes, '%', 'P', 'D', 'F')) {
            return fromPdf(bytes);
        }
        if (startsWith(bytes, 'P', 'K')) {
            return fromDocx(bytes);
        }
        throw new IllegalArgumentException("Please upload a PDF or DOCX resume (old .doc files are not supported).");
    }

    private String fromPdf(byte[] bytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read this PDF. It may be password-protected or damaged.");
        }
    }

    private String fromDocx(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = zip.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        if (out.size() > MAX_XML_BYTES) {
                            throw new IllegalArgumentException("This document is too large to read.");
                        }
                    }
                    return xmlToText(out.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        throw new IllegalArgumentException("This does not look like a valid DOCX resume.");
    }

    private String xmlToText(String xml) {
        return xml.replace("</w:p>", "\n")
                .replace("<w:tab/>", " ")
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'");
    }

    private boolean startsWith(byte[] b, char... prefix) {
        if (b.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (b[i] != (byte) prefix[i]) return false;
        }
        return true;
    }
}
