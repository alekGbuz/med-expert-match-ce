package com.berdachuk.medexpertmatch.web.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import com.berdachuk.medexpertmatch.web.service.ChatMarkdownRenderer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight CommonMark subset renderer with an HTML allowlist for SSR chat history.
 */
@Service("chatMarkdownRenderer")
public class ChatMarkdownRendererImpl implements ChatMarkdownRenderer {

    private static final String DETAILS_CLOSE = "</details>";
    private static final Pattern FENCED_CODE = Pattern.compile("```(?:\\w+)?\\n([\\s\\S]*?)```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)([^*]+)\\*(?!\\*)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(((?:[^()]|\\([^()]*\\))*)\\)");
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z0-9]+)(\\s[^>]*)?>");
    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern LLM_ANSWER_WRAPPER = Pattern.compile(
            "<div\\s+class=\"llm-answer-label\"[^>]*>\\s*Response\\s*</div>|<div\\s+class=\"llm-answer\"[^>]*>|</div>",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> ALLOWED_TAGS = List.of(
            "p", "strong", "em", "code", "pre", "ul", "ol", "li", "a", "br",
            "details", "summary", "div");

    @Override
    public String renderSafe(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String prepared = markdown.contains("llm-thinking")
                ? markdown.trim()
                : LlmResponseSanitizer.formatForChatDisplay(markdown.trim());
        return renderPreparedAssistant(prepared);
    }

    private String renderPreparedAssistant(String prepared) {
        int detailsStart = prepared.indexOf("<details");
        int detailsEnd = prepared.indexOf(DETAILS_CLOSE);
        if (detailsStart >= 0 && detailsEnd > detailsStart) {
            String beforeDetails = prepared.substring(0, detailsStart).trim();
            String detailsHtml = prepared.substring(detailsStart, detailsEnd + DETAILS_CLOSE.length());
            String afterDetails = unwrapAnswerMarkdown(prepared.substring(detailsEnd + DETAILS_CLOSE.length()));
            StringBuilder sb = new StringBuilder();
            if (!beforeDetails.isEmpty()) {
                sb.append("<div class=\"llm-answer\">").append(convertMarkdown(beforeDetails)).append("</div>");
            }
            sb.append(detailsHtml);
            if (!afterDetails.isEmpty()) {
                sb.append("<div class=\"llm-answer-label\">Response</div><div class=\"llm-answer\">")
                        .append(convertMarkdown(afterDetails)).append("</div>");
            }
            return sanitizeHtml(sb.toString());
        }
        return sanitizeHtml(convertMarkdown(LlmResponseSanitizer.stripLlmReasoning(prepared)));
    }

    private static String unwrapAnswerMarkdown(String remainder) {
        return LLM_ANSWER_WRAPPER.matcher(remainder.trim()).replaceAll("").trim();
    }

    private String convertMarkdown(String text) {
        String processed = replaceAll(FENCED_CODE, text, m -> {
            String code = escapeHtml(m.group(1));
            return "\u0000FENCED\u0000" + code + "\u0000ENDFENCED\u0000";
        });
        processed = applyInlineFormatting(processed);
        processed = processed
                .replace("<strong>", "\u0000B\u0000").replace("</strong>", "\u0000/B\u0000")
                .replace("<em>", "\u0000I\u0000").replace("</em>", "\u0000/I\u0000")
                .replace("<code>", "\u0000C\u0000").replace("</code>", "\u0000/C\u0000")
                .replace("<a ", "\u0000A ").replace("</a>", "\u0000/A\u0000")
                .replace("\">", "\u0000Q\u0000");
        processed = escapeHtml(processed);
        processed = processed
                .replace("\u0000B\u0000", "<strong>").replace("\u0000/B\u0000", "</strong>")
                .replace("\u0000I\u0000", "<em>").replace("\u0000/I\u0000", "</em>")
                .replace("\u0000C\u0000", "<code>").replace("\u0000/C\u0000", "</code>")
                .replace("\u0000A ", "<a ").replace("\u0000/A\u0000", "</a>")
                .replace("\u0000Q\u0000", "\">");
        processed = processed.replace("\u0000FENCED\u0000", "<pre><code>")
                .replace("\u0000ENDFENCED\u0000", "</code></pre>");

        StringBuilder html = new StringBuilder();
        boolean inList = false;
        for (String line : processed.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(trimmed.substring(2).trim()).append("</li>");
                continue;
            }
            if (inList) {
                html.append("</ul>");
                inList = false;
            }
            if (!trimmed.isEmpty()) {
                html.append("<p>").append(trimmed).append("</p>");
            }
        }
        if (inList) {
            html.append("</ul>");
        }
        return html.toString();
    }

    private String applyInlineFormatting(String text) {
        String result = replaceAll(INLINE_CODE, text, m -> "<code>" + m.group(1) + "</code>");
        for (int i = 0; i < 3; i++) {
            String next = replaceAll(BOLD, result, m -> "<strong>" + m.group(1) + "</strong>");
            next = replaceAll(LINK, next, m -> {
                String href = m.group(2).trim();
                if (!href.startsWith("http://") && !href.startsWith("https://")) {
                    return m.group(1);
                }
                return "<a href=\"" + escapeAttribute(href) + "\">" + m.group(1) + "</a>";
            });
            next = replaceAll(ITALIC, next, m -> "<em>" + m.group(1) + "</em>");
            if (next.equals(result)) {
                break;
            }
            result = next;
        }
        return result;
    }

    private String sanitizeHtml(String html) {
        Matcher matcher = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(html, last, matcher.start());
            String slash = matcher.group(1);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String attrs = matcher.group(3) == null ? "" : matcher.group(3);
            if (ALLOWED_TAGS.contains(tag)) {
                if ("a".equals(tag) && slash.isEmpty()) {
                    out.append(buildSafeAnchor(attrs));
                } else if ("a".equals(tag)) {
                    out.append("</a>");
                } else if ("details".equals(tag) || "summary".equals(tag) || "div".equals(tag)) {
                    if (slash.isEmpty()) {
                        out.append(buildSafeContainerOpen(tag, attrs));
                    } else {
                        out.append("</").append(tag).append('>');
                    }
                } else if (slash.isEmpty()) {
                    out.append('<').append(tag).append('>');
                } else {
                    out.append("</").append(tag).append('>');
                }
            }
            last = matcher.end();
        }
        out.append(html.substring(last));
        return out.toString();
    }

    private String buildSafeContainerOpen(String tag, String attrs) {
        if ("div".equals(tag) && attrs != null) {
            String lower = attrs.toLowerCase(Locale.ROOT);
            if (lower.contains("llm-thinking")
                    || lower.contains("llm-answer")
                    || lower.contains("llm-answer-label")) {
                return "<" + tag + attrs + ">";
            }
            return "<div>";
        }
        if ("details".equals(tag)) {
            return "<details class=\"llm-thinking\">";
        }
        if ("summary".equals(tag)) {
            return "<summary>";
        }
        return "<" + tag + ">";
    }

    private String buildSafeAnchor(String attrs) {
        Matcher hrefMatcher = HREF.matcher(attrs);
        if (!hrefMatcher.find()) {
            return "";
        }
        String href = hrefMatcher.group(1).trim();
        if (!href.startsWith("http://") && !href.startsWith("https://")) {
            return "";
        }
        return "<a href=\"" + escapeAttribute(href) + "\">";
    }

    private static String replaceAll(Pattern pattern, String input, java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacer.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escapeAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }
}
