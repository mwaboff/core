package com.aboff.core.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for sanitizing free-text markdown content submitted by users.
 *
 * <p>Sanitization is performed in two passes:
 * <ol>
 *   <li><b>Pass 1 — Markdown URI-scheme neutralization:</b> Rewrites dangerous URI schemes
 *       ({@code javascript:}, {@code data:}, {@code vbscript:}, {@code file:}) found in markdown
 *       link syntax, autolinks, and reference definitions to the inert prefix {@code unsafe:}.
 *       This prevents markdown renderers on the frontend from turning these constructs into
 *       executable links before the browser's HTML parser can see them.</li>
 *   <li><b>Pass 2 — JSoup HTML sanitization:</b> Parses the result as HTML with a strict
 *       allowlist of safe inline and block tags. Any tag not in the allowlist (including
 *       {@code <script>}, {@code <img>}, {@code <a>}, {@code <iframe>}) is stripped entirely.
 *       All HTML attributes on allowed tags are also stripped.</li>
 * </ol>
 *
 * <p><b>Known caveat:</b> Raw comparison expressions like {@code 5 < 10} written outside a
 * backtick code span may lose the {@code < 10>} portion because JSoup interprets it as an
 * unrecognized HTML tag and removes it. Users should use HTML entities ({@code &lt;}) or
 * backtick code spans for literal angle brackets in prose.</p>
 */
public final class MarkdownSanitizerUtil {

    private static final Logger log = LoggerFactory.getLogger(MarkdownSanitizerUtil.class);

    /**
     * Matches a dangerous URI scheme inside a markdown inline or image link:
     * {@code ](optional-ws scheme optional-ws :}.
     * Capture group 1 = any leading whitespace before the scheme;
     * capture group 2 = the scheme keyword itself.
     */
    private static final Pattern INLINE_LINK_SCHEME = Pattern.compile(
            "\\]\\((\\s*)(javascript|data|vbscript|file)(\\s*:)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches a dangerous URI scheme inside a markdown autolink:
     * {@code < optional-ws scheme optional-ws :}.
     * Capture group 1 = any leading whitespace before the scheme.
     */
    private static final Pattern AUTOLINK_SCHEME = Pattern.compile(
            "<(\\s*)(javascript|data|vbscript|file)(\\s*:)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches a dangerous URI scheme in a markdown link reference definition line:
     * {@code ^optional-ws [label]: optional-ws scheme optional-ws :}.
     * Applied with {@link Pattern#MULTILINE}.
     */
    private static final Pattern REFERENCE_DEF_SCHEME = Pattern.compile(
            "^(\\s*\\[[^\\]]+\\]:\\s*)(javascript|data|vbscript|file)(\\s*:)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    /** Replacement token used in place of neutralized dangerous schemes. */
    private static final String SAFE_SCHEME = "unsafe";

    private MarkdownSanitizerUtil() {
        // Utility class — prevent instantiation.
    }

    /**
     * Sanitizes raw markdown text to remove XSS vectors and unsafe URI schemes.
     *
     * <p>Input goes through pre-normalization (line-ending normalization and ASCII
     * control-character stripping), then Pass 1 (URI-scheme neutralization in markdown
     * syntax), then Pass 2 (JSoup HTML allowlist sanitization).
     *
     * @param rawMarkdown the raw markdown string provided by the user; must not be null
     * @return the sanitized string, or {@code ""} if the input is empty
     * @throws IllegalArgumentException if {@code rawMarkdown} is null
     */
    public static String sanitize(String rawMarkdown) {
        if (rawMarkdown == null) {
            throw new IllegalArgumentException("rawMarkdown must not be null");
        }
        if (rawMarkdown.isEmpty()) {
            return "";
        }

        log.debug("Sanitizing markdown input of length {}", rawMarkdown.length());

        String normalized = preNormalize(rawMarkdown);
        String schemeNeutralized = neutralizeSchemes(normalized);
        String sanitized = jsoupSanitize(schemeNeutralized);

        log.debug("Markdown sanitization complete; output length {}", sanitized.length());
        return sanitized;
    }

    /**
     * Normalizes line endings and strips ASCII control characters that are not
     * legitimate whitespace ({@code \t} 0x09 and {@code \n} 0x0A are preserved).
     *
     * @param input the raw input string
     * @return the normalized string
     */
    private static String preNormalize(String input) {
        // Normalize Windows and legacy Mac line endings to Unix.
        String lineNormalized = input.replace("\r\n", "\n").replace('\r', '\n');

        // Strip C0 controls (0x00–0x1F) except TAB (0x09) and LF (0x0A), and DEL (0x7F).
        StringBuilder sb = new StringBuilder(lineNormalized.length());
        for (int i = 0; i < lineNormalized.length(); i++) {
            char c = lineNormalized.charAt(i);
            boolean isControlChar = (c <= 0x1F && c != '\t' && c != '\n') || c == 0x7F;
            if (!isControlChar) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Rewrites dangerous URI schemes in markdown link syntax to the inert {@code unsafe:} prefix.
     * Handles inline links/images, autolinks, and reference definitions.
     *
     * @param input pre-normalized markdown text
     * @return markdown with dangerous schemes replaced
     */
    private static String neutralizeSchemes(String input) {
        String result = replaceScheme(input, INLINE_LINK_SCHEME, "](");
        result = replaceScheme(result, AUTOLINK_SCHEME, "<");
        result = replaceScheme(result, REFERENCE_DEF_SCHEME, null);
        return result;
    }

    /**
     * Applies a scheme-replacement pattern to the input, preserving any surrounding whitespace
     * captured in the match groups.
     *
     * <p>Each pattern must have three capture groups:
     * <ol>
     *   <li>Prefix (the literal opening token: {@code ](} or {@code <}) — or for reference
     *       definitions, the label+colon prefix up to the scheme.</li>
     *   <li>The scheme keyword (e.g. {@code javascript}).</li>
     *   <li>Any whitespace before the final {@code :} (may be empty).</li>
     * </ol>
     *
     * @param input   the text to process
     * @param pattern the compiled pattern to apply
     * @param prefix  the fixed literal to emit before group 1, or {@code null} when group 1
     *                itself contains the variable prefix (reference-definition case)
     * @return the text with dangerous schemes replaced
     */
    private static String replaceScheme(String input, Pattern pattern, String prefix) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String group1 = matcher.group(1);
            String group3 = matcher.group(3);
            String replacement;
            if (prefix != null) {
                // Inline link / autolink: emit the fixed literal, then whitespace, then safe scheme.
                replacement = Matcher.quoteReplacement(prefix + group1 + SAFE_SCHEME + group3);
            } else {
                // Reference definition: group 1 is the variable label+colon prefix.
                replacement = Matcher.quoteReplacement(group1 + SAFE_SCHEME + group3);
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Strips disallowed HTML using a JSoup allowlist that permits only a curated set of
     * safe inline and block formatting tags with no attributes.
     *
     * @param input the markdown text after URI-scheme neutralization
     * @return HTML-sanitized text
     */
    private static String jsoupSanitize(String input) {
        Safelist safelist = Safelist.none()
                .addTags(
                        "b", "i", "em", "strong", "u", "s", "del", "ins",
                        "code", "kbd", "samp", "var", "mark", "sub", "sup",
                        "small", "br", "p"
                );

        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(input, "", safelist, outputSettings);
    }
}
