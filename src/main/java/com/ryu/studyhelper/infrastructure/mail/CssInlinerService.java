package com.ryu.studyhelper.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * CSS를 HTML에 인라인으로 변환하는 서비스
 * 이메일 클라이언트(특히 Gmail)는 외부 CSS를 지원하지 않으므로
 * 모든 스타일을 인라인으로 변환해야 함
 */
@Service
@Slf4j
public class CssInlinerService {

    /**
     * HTML과 CSS 파일 경로를 받아서 CSS를 인라인으로 적용한 HTML 반환
     *
     * @param html HTML 콘텐츠
     * @param cssPath CSS 파일 경로 (classpath 기준)
     * @return 인라인 CSS가 적용된 HTML
     */
    public String inlineCss(String html, String cssPath) {
        try {
            // CSS 파일 로드
            String css = loadCssFile(cssPath);

            // HTML 파싱
            Document doc = Jsoup.parse(html);

            // CSS 파싱 및 적용
            Map<String, String> cssRules = parseCss(css);
            applyCssRules(doc, cssRules);

            return doc.html();
        } catch (Exception e) {
            log.error("CSS 인라인 변환 실패: {}", e.getMessage(), e);
            return html; // 실패 시 원본 반환
        }
    }

    /**
     * CSS 파일 로드
     */
    private String loadCssFile(String cssPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(cssPath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * 간단한 CSS 파서 (클래스 선택자 중심)
     * 형식: .className { property: value; property2: value2; }
     */
    private Map<String, String> parseCss(String css) {
        Map<String, String> rules = new HashMap<>();

        // 주석 제거
        css = css.replaceAll("/\\*.*?\\*/", "");

        // CSS 규칙 파싱 (간단한 정규식 기반)
        String[] blocks = css.split("}");
        for (String block : blocks) {
            if (!block.contains("{")) continue;

            String[] parts = block.split("\\{", 2);
            if (parts.length != 2) continue;

            String selector = parts[0].trim();
            String properties = parts[1].trim();

            // 클래스 선택자만 처리 (.)
            if (selector.startsWith(".")) {
                String className = selector.substring(1).trim();
                rules.put(className, properties);
            }
        }

        return rules;
    }

    /**
     * CSS 규칙을 HTML 요소에 적용
     */
    private void applyCssRules(Document doc, Map<String, String> cssRules) {
        for (Map.Entry<String, String> entry : cssRules.entrySet()) {
            String className = entry.getKey();
            String properties = entry.getValue();

            // 해당 클래스를 가진 모든 요소 찾기
            Elements elements = doc.getElementsByClass(className);
            for (Element element : elements) {
                // 기존 스타일에 추가 (기존 인라인 스타일 유지)
                String existingStyle = element.attr("style");
                String newStyle = existingStyle.isEmpty()
                    ? properties
                    : existingStyle + " " + properties;
                element.attr("style", newStyle);
            }
        }
    }
}