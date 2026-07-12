package com.onmom.chat.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class SafetySignalDetector {

    List<SafetySignal> detect(String message) {
        String text = message == null ? "" : message.replace(" ", "").toLowerCase();
        List<SafetySignal> signals = new ArrayList<>();

        if (containsAny(text, "태동줄", "태동이줄", "태동도줄", "태동줄어", "태동이줄어", "태동감소", "태동이없", "태동없")) {
            signals.add(new SafetySignal(
                    "REDUCED_FETAL_MOVEMENT",
                    "HIGH",
                    "태동 감소 의심",
                    "사용자가 태동이 평소보다 줄어든 것 같다고 표현했습니다.",
                    "태동 감소가 의심되면 혼자 판단하지 말고 주치의, 병원, 응급실 상담을 권장합니다."
            ));
        }

        if (containsAny(text, "출혈", "피가나", "피나", "하혈")) {
            signals.add(new SafetySignal(
                    "BLEEDING",
                    "HIGH",
                    "출혈 의심",
                    "사용자가 출혈 또는 하혈과 관련된 표현을 했습니다.",
                    "출혈이 있다면 병원, 주치의, 응급실 상담이 필요할 수 있습니다."
            ));
        }

        if (containsAny(text, "심한복통", "배가너무아", "복통이심", "참기힘든통증")) {
            signals.add(new SafetySignal(
                    "SEVERE_ABDOMINAL_PAIN",
                    "HIGH",
                    "심한 복통 의심",
                    "사용자가 심한 복통 또는 참기 힘든 통증을 표현했습니다.",
                    "심한 복통은 병원 상담이 필요할 수 있으므로 즉시 의료진과 상담하도록 안내합니다."
            ));
        }

        if (containsAny(text, "숨쉬기힘", "호흡곤란", "숨이차", "의식이흐", "기절")) {
            signals.add(new SafetySignal(
                    "EMERGENCY_SYMPTOM",
                    "HIGH",
                    "응급 증상 의심",
                    "사용자가 호흡곤란, 의식 저하 등 응급 가능성이 있는 증상을 표현했습니다.",
                    "호흡곤란, 의식 저하가 있으면 119 또는 응급실 상담을 권장합니다."
            ));
        }

        if (containsAny(text, "죽고싶", "사라지고싶", "자해", "해치고싶")) {
            signals.add(new SafetySignal(
                    "SELF_HARM_RISK",
                    "HIGH",
                    "자해 위험 신호",
                    "사용자가 자해 또는 극단적 생각과 관련된 표현을 했습니다.",
                    "혼자 있지 말고 즉시 주변 사람, 119, 응급실 또는 정신건강 위기 상담 도움을 받도록 안내합니다."
            ));
        }

        return signals;
    }

    String riskLevel(List<SafetySignal> signals) {
        if (signals.stream().anyMatch(signal -> "HIGH".equals(signal.severity()))) {
            return "긴급";
        }
        if (!signals.isEmpty()) {
            return "주의";
        }
        return "낮음";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
