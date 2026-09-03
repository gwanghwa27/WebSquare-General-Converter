package com.example.xfdltracker;

import com.example.xfdltracker.analyzer.CallTreePrinter;
import com.example.xfdltracker.analyzer.FunctionCallAnalyzer;
import com.example.xfdltracker.analyzer.UnusedFunctionAnalyzer;
import com.example.xfdltracker.model.FunctionInfo;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlFunctionParser;
import com.example.xfdltracker.parser.XfdlReader;
import org.w3c.dom.Document;

import java.io.File;
import java.util.Set;

public class XfdlFunctionTracker {
    public XfdlAnalysisResult analyze(File xfdlFile) throws Exception {
        return analyze(xfdlFile, null);
    }

    /**
     * Phase 3 통합 스크립트 분석. 이벤트 바인딩은 여전히 XFDL DOM에서 가져오며,
     * function index/call graph는 선택적으로 import된 XJS 심볼을 포함할 수 있다.
     */
    public XfdlAnalysisResult analyze(File xfdlFile, String scriptOverride) throws Exception {
        XfdlReader reader = new XfdlReader();
        Document document = reader.read(xfdlFile);
        XfdlAnalysisResult result = new XfdlAnalysisResult();
        reader.extractEvents(document, result);
        String script = scriptOverride == null ? reader.extractScript(document) : scriptOverride;
        new XfdlFunctionParser().parse(script, result);
        new FunctionCallAnalyzer().analyze(result);
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("사용법: java -cp bin com.example.xfdltracker.XfdlFunctionTracker <screen.xfdl>");
            System.exit(1);
        }
        XfdlFunctionTracker tracker = new XfdlFunctionTracker();
        XfdlAnalysisResult result = tracker.analyze(new File(args[0]));

        System.out.println("=== 함수 목록 ===");
        for (FunctionInfo fn : result.getFunctions().values()) {
            System.out.println(fn.getName() + ", 시작행=" + fn.getStartLine() + ", 호출함수=" + fn.getCalledFunctions());
        }

        System.out.println();
        System.out.println("=== 이벤트 호출 트리 ===");
        new CallTreePrinter().print(result);

        Set<String> unused = new UnusedFunctionAnalyzer().findUnused(result);
        System.out.println("=== 미사용 후보 함수 ===");
        for (String name : unused) System.out.println(name);
    }
}
