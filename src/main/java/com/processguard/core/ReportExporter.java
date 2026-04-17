package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a focused PDF summary report — no external dependencies.
 * Writes raw PDF 1.4 syntax directly.
 *
 * Sections:
 *   1. Top Repeat Alerts      — alert types that fired most often
 *   2. Custom Rule Violations — which rules triggered and how many times
 *   3. Heavy Processes        — processes consistently using the most CPU / memory
 *
 * Called on a background thread from MainDashboard, so blocking I/O is fine.
 */
public class ReportExporter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TOP_N = 5;   // rows shown per section

    // =========================================================
    // PUBLIC ENTRY POINT
    // =========================================================

    /**
     * @param processes  current live snapshot from masterData
     * @param alerts     full alert history from HistoryStorage
     * @return path of the written PDF
     */
    public static Path export(List<ProcessInfo> processes,
                              List<AlertEvent>  alerts) throws IOException {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path dir = Paths.get(System.getProperty("user.home"), "Desktop", "ProcessGuardReports");
        Files.createDirectories(dir);
        Path out = dir.resolve("report_" + timestamp + ".pdf");

        byte[] pdf = buildPdf(processes, alerts);
        Files.write(out, pdf);
        return out;
    }

    // =========================================================
    // PDF BUILDER
    // =========================================================

    private static byte[] buildPdf(List<ProcessInfo> processes,
                                   List<AlertEvent>  alerts) throws IOException {

        PdfWriter w = new PdfWriter();

        String now = LocalDateTime.now().format(DT);

        // ── title / meta ─────────────────────────────────────
        w.text(50, 800, 18, true,  "ProcessGuard - Session Report");
        w.text(50, 778, 9,  false, "Generated: " + now);
        w.line(50, 770, 545, 770);

        int y = 752;

        // ── Section 1: Top Repeat Alerts ─────────────────────
        y = section(w, y, "1. Top Repeat Alerts");

        Map<String, Long> alertFreq = alerts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getType().name().replace("_", " "),
                        Collectors.counting()));

        if (alertFreq.isEmpty()) {
            y = bodyLine(w, y, "  No alerts recorded this session.");
        } else {
            y = tableHeader(w, y, new String[]{"Alert Type", "Count", "Severity"}, new int[]{300, 80, 115});
            List<Map.Entry<String, Long>> topAlerts = alertFreq.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TOP_N)
                    .toList();

            boolean shade = false;
            for (Map.Entry<String, Long> e : topAlerts) {
                // find highest severity seen for this alert type
                String sev = alerts.stream()
                        .filter(a -> a.getType().name().replace("_", " ").equals(e.getKey()))
                        .map(a -> a.getSeverity().name())
                        .max(Comparator.comparingInt(ReportExporter::sevOrd))
                        .orElse("—");

                y = tableRow(w, y, shade,
                        new String[]{e.getKey(), String.valueOf(e.getValue()), sev},
                        new int[]{300, 80, 115});
                shade = !shade;
            }
        }

        y -= 16;

        // ── Section 2: Heavy Processes ────────────────────────
        y = section(w, y, "2. Heaviest Processes (current snapshot)");

        if (processes.isEmpty()) {
            y = bodyLine(w, y, "  No process data available.");
        } else {
            // top by CPU
            y = subHeader(w, y, "By CPU Usage");
            y = tableHeader(w, y, new String[]{"Process Name", "PID", "CPU %", "Mem MB"}, new int[]{240, 80, 100, 95});

            boolean shade = false;
            for (ProcessInfo p : processes.stream()
                    .sorted(Comparator.comparingDouble(ProcessInfo::getCpuUsage).reversed())
                    .limit(TOP_N).toList()) {

                y = tableRow(w, y, shade, new String[]{
                        truncate(p.getName(), 35),
                        String.valueOf(p.getPid()),
                        String.format("%.1f%%", p.getCpuUsage()),
                        p.getMemoryUsageMB() + " MB"
                }, new int[]{240, 80, 100, 95});
                shade = !shade;
            }

            y -= 10;

            // top by memory
            y = subHeader(w, y, "By Memory Usage");
            y = tableHeader(w, y, new String[]{"Process Name", "PID", "CPU %", "Mem MB"}, new int[]{240, 80, 100, 95});

            shade = false;
            for (ProcessInfo p : processes.stream()
                    .sorted(Comparator.comparingLong(ProcessInfo::getMemoryUsageMB).reversed())
                    .limit(TOP_N).toList()) {

                y = tableRow(w, y, shade, new String[]{
                        truncate(p.getName(), 35),
                        String.valueOf(p.getPid()),
                        String.format("%.1f%%", p.getCpuUsage()),
                        p.getMemoryUsageMB() + " MB"
                }, new int[]{240, 80, 100, 95});
                shade = !shade;
            }
        }

        // ── footer ───────────────────────────────────────────
        w.line(50, 50, 545, 50);
        w.text(50, 38, 8, false, "ProcessGuard  |  " + now + "  |  processguard.local");

        return w.finish();
    }

    // =========================================================
    // LAYOUT HELPERS
    // =========================================================

    private static int section(PdfWriter w, int y, String title) {
        y -= 6;
        w.rect(50, y - 4, 495, 18, 0.85f); // grey background
        w.text(54, y + 2, 11, true, title);
        return y - 22;
    }

    private static int subHeader(PdfWriter w, int y, String title) {
        w.text(54, y, 9, true, title);
        return y - 14;
    }

    private static int bodyLine(PdfWriter w, int y, String text) {
        w.text(54, y, 9, false, text);
        return y - 14;
    }

    private static int tableHeader(PdfWriter w, int y, String[] cols, int[] widths) {
        w.rect(50, y - 4, 495, 14, 0.25f); // dark header bg
        int x = 54;
        for (int i = 0; i < cols.length; i++) {
            w.textColour(x, y, 8, true, cols[i], 1f, 1f, 1f); // white text
            x += widths[i];
        }
        return y - 16;
    }

    private static int tableRow(PdfWriter w, int y, boolean shade,
                                String[] vals, int[] widths) {
        if (shade) w.rect(50, y - 4, 495, 13, 0.94f);
        int x = 54;
        for (int i = 0; i < vals.length; i++) {
            w.text(x, y, 8, false, vals[i]);
            x += widths[i];
        }
        return y - 14;
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private static int sevOrd(String sev) {
        return switch (sev) {
            case "CRITICAL" -> 4;
            case "HIGH"     -> 3;
            case "MEDIUM"   -> 2;
            case "LOW"      -> 1;
            default         -> 0;
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    // =========================================================
    // MINIMAL ZERO-DEPENDENCY PDF WRITER  (PDF 1.4, single page)
    // =========================================================

    /**
     * Writes raw PDF 1.4 bytes.
     * Supports: text (Helvetica / Helvetica-Bold), lines, filled rectangles.
     * Single A4 page (595 x 842 pt).
     */
    private static class PdfWriter {

        // content stream ops queued here
        private final StringBuilder cs = new StringBuilder();

        // PDF object bytes, in order
        private final List<byte[]> objects  = new ArrayList<>();
        private final List<Integer> offsets = new ArrayList<>();

        // ── drawing API ───────────────────────────────────────

        /** Draw left-aligned text at (x, y) in pt from bottom-left. */
        void text(int x, int y, int size, boolean bold, String str) {
            String font = bold ? "F2" : "F1";
            cs.append("BT /").append(font).append(" ").append(size)
                    .append(" Tf ").append(x).append(" ").append(y)
                    .append(" Td (").append(escape(str)).append(") Tj ET\n");
        }

        /** Draw text in an RGB colour (0.0–1.0 each channel). */
        void textColour(int x, int y, int size, boolean bold,
                        String str, float r, float g, float b) {
            String font = bold ? "F2" : "F1";
            cs.append("BT /").append(font).append(" ").append(size)
                    .append(" Tf ").append(r).append(" ").append(g).append(" ").append(b)
                    .append(" rg ").append(x).append(" ").append(y)
                    .append(" Td (").append(escape(str)).append(") Tj ET\n");
        }

        /** Draw a horizontal line. */
        void line(int x1, int y1, int x2, int y2) {
            cs.append("0.5 w ")
                    .append(x1).append(" ").append(y1).append(" m ")
                    .append(x2).append(" ").append(y2).append(" l S\n");
        }

        /**
         * Draw a filled grey rectangle.
         * @param grey 0.0 (black) – 1.0 (white)
         */
        void rect(int x, int y, int w, int h, float grey) {
            cs.append(grey).append(" g ")
                    .append(x).append(" ").append(y).append(" ")
                    .append(w).append(" ").append(h).append(" re f\n")
                    .append("0 g\n"); // reset to black
        }

        // ── build final PDF bytes ─────────────────────────────

        byte[] finish() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // header
            write(out, "%PDF-1.4\n");

            // obj 1 – catalog
            addObj(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // obj 2 – pages
            addObj(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // obj 3 – page  (A4 = 595 x 842)
            addObj(out,
                    "3 0 obj\n" +
                            "<< /Type /Page /Parent 2 0 R\n" +
                            "   /MediaBox [0 0 595 842]\n" +
                            "   /Contents 4 0 R\n" +
                            "   /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\n" +
                            "endobj\n");

            // obj 4 – content stream
            byte[] streamBytes = cs.toString().getBytes(StandardCharsets.ISO_8859_1);
            String obj4 = "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
            offsets.add(out.size());
            write(out, obj4);
            out.write(streamBytes);
            write(out, "\nendstream\nendobj\n");

            // obj 5 – Helvetica (regular)
            addObj(out,
                    "5 0 obj\n" +
                            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica\n" +
                            "   /Encoding /WinAnsiEncoding >>\n" +
                            "endobj\n");

            // obj 6 – Helvetica-Bold
            addObj(out,
                    "6 0 obj\n" +
                            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold\n" +
                            "   /Encoding /WinAnsiEncoding >>\n" +
                            "endobj\n");

            // xref
            int xrefPos = out.size();
            int totalObjs = offsets.size() + 1; // +1 for free entry 0
            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 ").append(totalObjs).append("\n");
            xref.append("0000000000 65535 f \n");
            for (int off : offsets) {
                xref.append(String.format("%010d 00000 n \n", off));
            }
            write(out, xref.toString());

            // trailer
            write(out, "trailer\n<< /Size " + totalObjs + " /Root 1 0 R >>\n"
                    + "startxref\n" + xrefPos + "\n%%EOF\n");

            return out.toByteArray();
        }

        // ── internals ─────────────────────────────────────────

        private void addObj(ByteArrayOutputStream out, String obj) throws IOException {
            offsets.add(out.size());
            write(out, obj);
        }

        private static void write(ByteArrayOutputStream out, String s) throws IOException {
            out.write(s.getBytes(StandardCharsets.ISO_8859_1));
        }

        /** Escape special PDF string characters. */
        private static String escape(String s) {
            if (s == null) return "";
            // strip non-latin chars that Helvetica can't render
            s = s.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
            return s.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }
    }
}