package com.aboff.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownSanitizerUtilTest {

    // -------------------------------------------------------------------------
    // Null / empty guards
    // -------------------------------------------------------------------------

    @Test
    void sanitize_NullInput_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> MarkdownSanitizerUtil.sanitize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sanitize_EmptyString_ReturnsEmptyString() {
        // Act
        String result = MarkdownSanitizerUtil.sanitize("");

        // Assert
        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Plain markdown round-trip
    // -------------------------------------------------------------------------

    @Test
    void sanitize_PlainMarkdownHeadingAndBold_RoundTrips() {
        // Arrange
        String input = "# Heading\n\n**bold**";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — JSoup does not strip markdown syntax; the text survives intact.
        assertThat(result).isEqualTo(input);
    }

    // -------------------------------------------------------------------------
    // Allowed HTML tags preserved
    // -------------------------------------------------------------------------

    @Test
    void sanitize_AllowedInlineTagStrong_IsPreserved() {
        // Arrange
        String input = "<strong>x</strong>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).isEqualTo("<strong>x</strong>");
    }

    // -------------------------------------------------------------------------
    // Disallowed / dangerous HTML stripped
    // -------------------------------------------------------------------------

    @Test
    void sanitize_ScriptTag_IsStripped() {
        // Arrange
        String input = "<script>alert(1)</script>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).doesNotContain("script");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void sanitize_ImgWithOnerror_IsStripped() {
        // Arrange
        String input = "<img src=x onerror=alert(1)>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — img is not in the safelist; the whole tag must be gone.
        assertThat(result).doesNotContain("img");
        assertThat(result).doesNotContain("onerror");
    }

    @Test
    void sanitize_StrongWithOnclickAttribute_AttributeIsStripped() {
        // Arrange
        String input = "<strong onclick=\"x\">y</strong>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — tag allowed, attribute must be removed.
        assertThat(result).isEqualTo("<strong>y</strong>");
    }

    @Test
    void sanitize_AnchorTag_IsStripped() {
        // Arrange
        String input = "<a href=\"https://ok\">x</a>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — <a> is not in the safelist; only the text content survives.
        assertThat(result).doesNotContain("<a");
        assertThat(result).contains("x");
    }

    // -------------------------------------------------------------------------
    // Pass 1 — markdown URI-scheme neutralization
    // -------------------------------------------------------------------------

    @Test
    void sanitize_InlineLinkWithJavascriptScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "[x](javascript:alert(1))";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).contains("[x](unsafe:alert(1))");
    }

    @Test
    void sanitize_InlineLinkWithJavascriptSchemeAndLeadingWhitespace_WhitespacePreservedSchemeNeutralized() {
        // Arrange
        String input = "[x](  JavaScript:alert(1))";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — two spaces before scheme must be preserved; scheme replaced.
        assertThat(result).contains("[x](  unsafe:alert(1))");
    }

    @Test
    void sanitize_ImageWithDataScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "![alt](data:text/html,<script>)";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).contains("unsafe:");
        assertThat(result).doesNotContain("data:text/html");
        // JSoup will also strip the literal <script> text from within the URL
        assertThat(result).doesNotContain("<script>");
    }

    @Test
    void sanitize_AutolinkWithJavascriptScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "<javascript:alert(1)>";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — Pass 1 rewrites the scheme to "unsafe:", and then JSoup strips the
        // resulting unknown-tag token entirely. Either way the javascript: scheme is gone.
        assertThat(result).doesNotContain("javascript:");
        assertThat(result).doesNotContain("alert(1)");
    }

    @Test
    void sanitize_ReferenceLinkDefinitionWithJavascriptScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "[x][1]\n\n[1]: javascript:alert(1)";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).contains("[1]: unsafe:");
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    void sanitize_InlineLinkWithVbscriptScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "[x](vbscript:foo)";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).contains("unsafe:");
        assertThat(result).doesNotContain("vbscript:");
    }

    @Test
    void sanitize_InlineLinkWithFileScheme_SchemeIsNeutralized() {
        // Arrange
        String input = "[x](file:///etc/passwd)";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert
        assertThat(result).contains("unsafe:");
        assertThat(result).doesNotContain("file:");
    }

    @Test
    void sanitize_SafeSchemes_ArePreservedIntact() {
        // Arrange
        String input = "[a](https://example.com) [b](http://x) [c](mailto:a@b) [d](/rel) [e](#anchor)";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — none of the safe schemes/paths should be rewritten.
        assertThat(result).contains("https://example.com");
        assertThat(result).contains("http://x");
        assertThat(result).contains("mailto:a@b");
        assertThat(result).contains("/rel");
        assertThat(result).contains("#anchor");
    }

    // -------------------------------------------------------------------------
    // Pre-normalization
    // -------------------------------------------------------------------------

    @Test
    void sanitize_NulByte_IsStripped() {
        // Arrange — embed a NUL byte (0x00).
        String input = "hello\u0000world";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — NUL byte stripped, surrounding text intact.
        assertThat(result).isEqualTo("helloworld");
        assertThat(result).doesNotContain("\u0000");
    }

    @Test
    void sanitize_CrLfAndBareCr_NormalizedToLf() {
        // Arrange
        String input = "line1\r\nline2\rline3";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — no carriage returns remain; line feeds present.
        assertThat(result).doesNotContain("\r");
        assertThat(result).contains("\n");
    }

    // -------------------------------------------------------------------------
    // Code fence survival
    // -------------------------------------------------------------------------

    @Test
    void sanitize_TripleBacktickCodeFence_ContentSurvivesWithoutReflow() {
        // Arrange
        String input = "```\nx\n```";

        // Act
        String result = MarkdownSanitizerUtil.sanitize(input);

        // Assert — markdown code fence syntax must pass through unchanged.
        assertThat(result).isEqualTo(input);
    }
}
