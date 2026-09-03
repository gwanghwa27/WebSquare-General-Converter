package com.example.xfdltracker.tab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JavaScriptCleaner에 의해 인용된(quoted) page ID가 가려지는 Tab 표현식을 위한 소규모 어휘 분석 helper. */
final class TabScriptTextSupport {
    private static final Pattern QUOTED_PAGE = Pattern.compile(
            "tabpages\\s*\\[\\s*([\\\"'])([A-Za-z_$][A-Za-z0-9_$.-]*)\\1\\s*\\]");

    private TabScriptTextSupport() {}

    static String restoreQuotedTabpageSelectors(String original, String cleaned) {
        if (original == null || cleaned == null || original.length() != cleaned.length()) return cleaned;
        char[] out = cleaned.toCharArray();
        Matcher m = QUOTED_PAGE.matcher(original);
        while (m.find()) {
            int tabEnd = Math.min(m.start() + 8, cleaned.length());
            if (tabEnd <= m.start() || !"tabpages".equals(cleaned.substring(m.start(), tabEnd))) continue;
            for (int i = m.start(); i < m.end(); i++) out[i] = original.charAt(i);
        }
        return new String(out);
    }
}
