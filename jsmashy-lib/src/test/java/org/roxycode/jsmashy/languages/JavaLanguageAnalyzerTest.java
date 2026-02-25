package org.roxycode.jsmashy.languages;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JavaLanguageAnalyzerTest {

    private final JavaLanguageAnalyzer analyzer = new JavaLanguageAnalyzer();

    @Test
    void testAnalyzeValidJavaCode() {
        String sourceCode = "public class HelloWorld { " +
                "  public static void main(String[] args) { " +
                "    System.out.println(\"Hello, world!\"); " +
                "  } " +
                "}";
        
        AnalysisResult result = analyzer.analyze(sourceCode);
        
        assertFalse(result.hasErrors(), "Should not have errors: " + result.errors());
        assertTrue(result.skeleton().replace(" ", "").contains("publicclassHelloWorld"), "Skeleton should contain class name");
        assertTrue(result.skeleton().replace(" ", "").contains("publicstaticvoidmain(String[]args)"), "Skeleton should contain method signature");
        assertFalse(result.skeleton().contains("System.out.println"), "Skeleton should NOT contain method body content");
        assertTrue(result.skeleton().contains("{ /* implementation omitted */ }"), "Skeleton should contain omitted comment");
    }

    @Test
    void testAnalyzeInvalidJavaCode() {
        String sourceCode = "public class HelloWorld { " +
                "  public static void main(String[] args) { " +
                "    System.out.println(\"Hello, world!\") " + // Missing semicolon
                "  } " +
                "}";
        
        AnalysisResult result = analyzer.analyze(sourceCode);
        
        assertTrue(result.hasErrors(), "Should have syntax errors");
        assertTrue(result.errors().get(0).contains("Parser error"), "Error should be a parser error");
    }

    @Test
    void testSkeletonizeComplexClass() {
        String sourceCode = "package com.example;\n" +
                "public class Complex {\n" +
                "    private String name;\n" +
                "    static { System.out.println(\"static init\"); }\n" +
                "    { System.out.println(\"instance init\"); }\n" +
                "    public Complex(String name) { this.name = name; }\n" +
                "    public String getName() { return name; }\n" +
                "}";
        
        AnalysisResult result = analyzer.analyze(sourceCode);
        
        assertFalse(result.hasErrors());
        String skeleton = result.skeleton();
        
        assertTrue(skeleton.replace(" ", "").contains("packagecom.example;"));
        assertTrue(skeleton.replace(" ", "").contains("privateStringname;"));
        assertTrue(skeleton.replace(" ", "").contains("static{/*implementationomitted*/}"));
        assertTrue(skeleton.replace(" ", "").contains("{/*implementationomitted*/}"));
        assertTrue(skeleton.replace(" ", "").contains("publicComplex(Stringname){/*implementationomitted*/}"));
        assertTrue(skeleton.replace(" ", "").contains("publicStringgetName(){/*implementationomitted*/}"));
        
        assertFalse(skeleton.contains("this.name = name;"));
        assertFalse(skeleton.contains("return name;"));
    }

    @Test
    void testNestedStructures() {
        String sourceCode = "public class Outer { " +
                "  public void method() { " +
                "    Runnable r = new Runnable() { " +
                "      public void run() { System.out.println(\"inner\"); } " +
                "    }; " +
                "  } " +
                "}";
        
        // This should not throw Exception
        AnalysisResult result = analyzer.analyze(sourceCode);
        assertFalse(result.hasErrors());
        assertTrue(result.skeleton().contains("{ /* implementation omitted */ }"));
    }


    @Test
    void testPreserveWhitespace() {
        String sourceCode = "public class WS {\n" +
                "    public void test() {\n" +
                "        System.out.println(\"hi\");\n" +
                "    }\n" +
                "}";
        AnalysisResult result = analyzer.analyze(sourceCode);
        String skeleton = result.skeleton();
        
        // Check for preserved newlines and indentation
        assertTrue(skeleton.contains("public class WS {\n"), "Should preserve newline after class declaration");
        assertTrue(skeleton.contains("    public void test()"), "Should preserve indentation for method");
        assertTrue(skeleton.contains(" { /* implementation omitted */ }"), "Should contain omission block");
        // System.out.println("DEBUG SKELETON:\n" + skeleton);
    }

}