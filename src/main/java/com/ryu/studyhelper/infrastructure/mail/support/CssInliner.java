package com.ryu.studyhelper.infrastructure.mail.support;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * CSS를 HTML에 인라인으로 변환하는 유틸
 * 이메일 클라이언트(특히 Gmail)는 외부 CSS를 지원하지 않으므로
 * 모든 스타일을 인라인으로 변환해야 함
 */
@Component
@Slf4j
public class CssInliner {

    /**
     * HTML과 CSS 파일 경로를 받아서 CSS를 인라인으로 적용한 HTML 반환
     *
     * @param html HTML 콘텐츠
     * @param cssPath CSS 파일 경로 (classpath 기준)
     * @return 인라인 CSS가 적용된 HTML
     */
    public String inline(String html, String cssPath) {
        try {
            String css = loadCssFile(cssPath);
            Document doc = Jsoup.parse(html);
            Map<String, String> cssRules = parseCss(css);
            applyCssRules(doc, cssRules);
            return doc.html();
        } catch (Exception e) {
            log.error("CSS 인라인 변환 실패: {}", e.getMessage(), e);
            return html;
        }
    }

    private String loadCssFile(String cssPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(cssPath);
        if (!resource.exists()) {
            throw new IOException("CSS 파일을 찾을 수 없습니다: " + cssPath);
        }
        try (var stream = resource.getInputStream()) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseCss(String css) {
        Map<String, String> rules = new HashMap<>();
        css = css.replaceAll("/\\*.*?\\*/", "");

        String[] blocks = css.split("}");
        for (String block : blocks) {
            if (!block.contains("{")) continue;

            String[] parts = block.split("\\{", 2);
            if (parts.length != 2) continue;

            String selector = parts[0].trim();
            String properties = parts[1].trim();

            if (selector.startsWith(".")) {
                String className = selector.substring(1).trim();
                rules.put(className, properties);
            }
        }
        return rules;
    }

    private void applyCssRules(Document doc, Map<String, String> cssRules) {
        for (Map.Entry<String, String> entry : cssRules.entrySet()) {
            String className = entry.getKey();
            String properties = entry.getValue();

            Elements elements = doc.getElementsByClass(className);
            for (Element element : elements) {
                String existingStyle = element.attr("style");
                String newStyle = existingStyle.isEmpty()
                        ? properties
                        : existingStyle + " " + properties;
                element.attr("style", newStyle);
            }
        }
    }
}