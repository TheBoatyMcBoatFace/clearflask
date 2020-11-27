package com.smotana.clearflask.web.security;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.util.Modules;
import com.kik.config.ice.ConfigSystem;
import com.smotana.clearflask.testutil.AbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SanitizerTest extends AbstractTest {

    @Inject
    private Sanitizer sanitizer;

    @Override
    protected void configure() {
        super.configure();

        install(Modules.override(
                Sanitizer.module()
        ).with(new AbstractModule() {
            @Override
            protected void configure() {
                install(ConfigSystem.overrideModule(Sanitizer.Config.class, om -> {
                    om.override(om.id().htmlSanitizerEnabled()).withValue(true);
                }));
            }
        }));
    }

    @Test(timeout = 10_000L)
    public void testAllFormats() throws Exception {
        String html = "<div><strong>  a</strong></div><div><s>b</s></div><div><em>c</em></div><div><u>d</u></div><div><br></div><div><br></div><ul><li>e</li><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>f</u></s></em></strong></a></li></ul><ol><li>g</li><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>h</u></s></em></strong></a></li></ol><ul data-checked=\"false\"><li>i</li></ul><ul data-checked=\"true\"><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>j</u></s></em></strong></a></li></ul><blockquote>k</blockquote><blockquote><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>l</u></s></em></strong></a></blockquote><pre class=\"ql-syntax\" spellcheck=\"false\">m\nn\n</pre><div><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a></div><div><br></div><div><br></div>";
        String expectedHtml = html
                // Quill is strongly for keeping HTML5 tag without trailing slash
                // while OWASP is strongly for interoperability with XHTML
                .replace("<br>", "<br />");
        assertSanitize("Expected same", expectedHtml, html);
    }


    @Test(timeout = 10_000L)
    public void testA() throws Exception {
        assertSanitize("Should be no change",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>");
        assertSanitize("Expected mailto protocol to be allowed and @ symbol to be escaped",
                "<a href=\"mailto:example&#64;example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"mailto:example@example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>");
        assertSanitize("Should be no change with tel protocol",
                "<a href=\"tel:12345678\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"tel:12345678\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>");

        assertSanitize("Uppercase element name should be lowercased",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<A href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>");

        assertSanitize("Expected protocol mismatch to drop a",
                "o",
                "<a href=\"javascript://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>");

        assertSanitize("Expected target to be replaced",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_not_blank\">o</a>");

        assertSanitize("Expected rel to be unchanged",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noopener ugc noreferrer\" target=\"_blank\">o</a>");
        assertSanitize("Expected rel to be updated",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener\" target=\"_blank\">o</a>");
        assertSanitize("Expected rel to be updated",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener  ugc\" target=\"_blank\">o</a>");
        assertSanitize("Expected rel to be updated",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a>",
                "<a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc nofollow\" target=\"_blank\">o</a>");
    }

    @Test(timeout = 10_000L)
    public void testToPlaintext() throws Exception {
        String html = "<div><strong>  a</strong></div><div><s>b</s></div><div><em>c</em></div><div><u>d</u></div><div><br></div><div><br></div><ul><li>e</li><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>f</u></s></em></strong></a></li></ul><ol><li>g</li><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>h</u></s></em></strong></a></li></ol><ul data-checked=\"false\"><li>i</li></ul><ul data-checked=\"true\"><li class=\"ql-indent-1\"><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>j</u></s></em></strong></a></li></ul><blockquote>k</blockquote><blockquote><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\"><strong><em><s><u>l</u></s></em></strong></a></blockquote><pre class=\"ql-syntax\" spellcheck=\"false\">m\nn\n</pre><div><a href=\"http://mock.localhost.com:3000/example.com\" rel=\"noreferrer noopener ugc\" target=\"_blank\">o</a></div><div><br></div><div><br></div>";
        String expectedPlaintext = "  abcdefghijklm\nn\no";
        assertEquals(expectedPlaintext, sanitizer.richHtmlToPlaintext(html));
    }

    @Test(timeout = 10_000L)
    public void testMigratePToDiv() throws Exception {
        assertSanitize("Should be no change",
                "<div>a</div>",
                "<p>a</p>");
    }

    void assertSanitize(String message, String expHtml, String inpHtml) {
        assertEquals(message, expHtml, sanitizer.richHtml(inpHtml, "msg", "'" + message + "'"));
    }
}