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
    private static final int TOP_N = 5;

    /**
     * Exports a PDF report based on process snapshot and alert history.
     * @param processes current live snapshot from masterData
     * @param alerts full alert history from HistoryStorage
     * @return path of the written PDF
     * @throws IOException if file writing fails
     */
    public static Path export(List<ProcessInfo> processes,
                              List<AlertEvent> alerts) throws IOException {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path dir = Paths.get(System.getProperty("user.home"), "Desktop", "ProcessGuardReports");
        Files.createDirectories(dir);
        Path out = dir.resolve("report_" + timestamp + ".pdf");

        byte[] pdf = buildPdf(processes, alerts);
        Files.write(out, pdf);
        return out;
    }

    /**
     * Builds raw PDF bytes for the report.
     * @param processes process snapshot
     * @param alerts alert history
     * @return PDF file as byte array
     * @throws IOException if generation fails
     */
    private static byte[] buildPdf(List<ProcessInfo> processes,
                                   List<AlertEvent> alerts) throws IOException {

        PdfWriter w = new PdfWriter();

        String now = LocalDateTime.now().format(DT);

        w.text(50, 800, 18, true,  "ProcessGuard - Session Report");
        w.text(50, 778, 9,  false, "Generated: " + now);
        w.line(50, 770, 545, 770);

        int y = 752;

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

        y = section(w, y, "2. Heaviest Processes (current snapshot)");

        if (processes.isEmpty()) {
            y = bodyLine(w, y, "  No process data available.");
        } else {

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

        w.line(50, 50, 545, 50);
        w.text(50, 38, 8, false, "ProcessGuard  |  " + now + "  |  processguard.local");

        return w.finish();
    }

    /**
     * Renders a section header.
     * @param w pdf writer
     * @param y current y position
     * @param title section title
     * @return updated y position
     */
    private static int section(PdfWriter w, int y, String title) {
        y -= 6;
        w.rect(50, y - 4, 495, 18, 0.85f);
        w.text(54, y + 2, 11, true, title);
        return y - 22;
    }

    /**
     * Renders a subsection header.
     * @param w pdf writer
     * @param y current y position
     * @param title header text
     * @return updated y position
     */
    private static int subHeader(PdfWriter w, int y, String title) {
        w.text(54, y, 9, true, title);
        return y - 14;
    }

    /**
     * Renders a body line.
     * @param w pdf writer
     * @param y current y position
     * @param text content
     * @return updated y position
     */
    private static int bodyLine(PdfWriter w, int y, String text) {
        w.text(54, y, 9, false, text);
        return y - 14;
    }

    /**
     * Renders table header row.
     * @param w pdf writer
     * @param y current y position
     * @param cols column labels
     * @param widths column widths
     * @return updated y position
     */
    private static int tableHeader(PdfWriter w, int y, String[] cols, int[] widths) {
        w.rect(50, y - 4, 495, 14, 0.25f);
        int x = 54;
        for (int i = 0; i < cols.length; i++) {
            w.textColour(x, y, 8, true, cols[i], 1f, 1f, 1f);
            x += widths[i];
        }
        return y - 16;
    }

    /**
     * Renders a table row.
     * @param w pdf writer
     * @param y current y position
     * @param shade whether row is shaded
     * @param vals row values
     * @param widths column widths
     * @return updated y position
     */
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

    /**
     * Converts severity string into ranking value.
     * @param sev severity string
     * @return severity rank
     */
    private static int sevOrd(String sev) {
        return switch (sev) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /**
     * Truncates string to max length.
     * @param s input string
     * @param max max length
     * @return truncated string
     */
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    /**
     * Minimal PDF writer for generating raw PDF 1.4 output.
     */
    private static class PdfWriter {

        private final StringBuilder cs = new StringBuilder();
        private final List<byte[]> objects = new ArrayList<>();
        private final List<Integer> offsets = new ArrayList<>();

        /**
         * Writes left-aligned text.
         * @param x x position
         * @param y y position
         * @param size font size
         * @param bold bold flag
         * @param str text content
         */
        void text(int x, int y, int size, boolean bold, String str) {
            String font = bold ? "F2" : "F1";
            cs.append("BT /").append(font).append(" ").append(size)
                    .append(" Tf ").append(x).append(" ").append(y)
                    .append(" Td (").append(escape(str)).append(") Tj ET\n");
        }

        /**
         * Writes colored text.
         * @param x x position
         * @param y y position
         * @param size font size
         * @param bold bold flag
         * @param str text content
         * @param r red
         * @param g green
         * @param b blue
         */
        void textColour(int x, int y, int size, boolean bold,
                        String str, float r, float g, float b) {
            String font = bold ? "F2" : "F1";
            cs.append("BT /").append(font).append(" ").append(size)
                    .append(" Tf ").append(r).append(" ").append(g).append(" ").append(b)
                    .append(" rg ").append(x).append(" ").append(y)
                    .append(" Td (").append(escape(str)).append(") Tj ET\n");
        }

        /**
         * Draws a line.
         * @param x1 start x
         * @param y1 start y
         * @param x2 end x
         * @param y2 end y
         */
        void line(int x1, int y1, int x2, int y2) {
            cs.append("0.5 w ")
                    .append(x1).append(" ").append(y1).append(" m ")
                    .append(x2).append(" ").append(y2).append(" l S\n");
        }

        /**
         * Draws a filled rectangle.
         * @param x x position
         * @param y y position
         * @param w width
         * @param h height
         * @param grey fill intensity
         */
        void rect(int x, int y, int w, int h, float grey) {
            cs.append(grey).append(" g ")
                    .append(x).append(" ").append(y).append(" ")
                    .append(w).append(" ").append(h).append(" re f\n")
                    .append("0 g\n");
        }

        byte[] finish() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            write(out, "%PDF-1.4\n");

            addObj(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            addObj(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            addObj(out,
                    "3 0 obj\n<< /Type /Page /Parent 2 0 R\n" +
                            " /MediaBox [0 0 595 842]\n" +
                            " /Contents 4 0 R\n" +
                            " /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\n" +
                            "endobj\n");

            byte[] streamBytes = cs.toString().getBytes(StandardCharsets.ISO_8859_1);
            String obj4 = "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
            offsets.add(out.size());
            write(out, obj4);
            out.write(streamBytes);
            write(out, "\nendstream\nendobj\n");

            addObj(out,
                    "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            addObj(out,
                    "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            int xrefPos = out.size();
            int totalObjs = offsets.size() + 1;

            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 ").append(totalObjs).append("\n");
            xref.append("0000000000 65535 f \n");

            for (int off : offsets) {
                xref.append(String.format("%010d 00000 n \n", off));
            }

            write(out, xref.toString());
            write(out, "trailer\n<< /Size " + totalObjs + " /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF\n");

            return out.toByteArray();
        }

        private void addObj(ByteArrayOutputStream out, String obj) throws IOException {
            offsets.add(out.size());
            write(out, obj);
        }

        private static void write(ByteArrayOutputStream out, String s) throws IOException {
            out.write(s.getBytes(StandardCharsets.ISO_8859_1));
        }

        /**
         * Escapes PDF special characters.
         * @param s input string
         * @return escaped string
         */
        private static String escape(String s) {
            if (s == null) return "";
            s = s.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", "?");
            return s.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }
    }
}