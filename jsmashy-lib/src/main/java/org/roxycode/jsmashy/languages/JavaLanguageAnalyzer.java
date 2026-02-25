package org.roxycode.jsmashy.languages;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.roxycode.jsmashy.languages.java.Java20Lexer;
import org.roxycode.jsmashy.languages.java.Java20Parser;
import org.roxycode.jsmashy.languages.java.Java20ParserBaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes Java source code using ANTLR4 to detect syntax errors and provide skeletonization.
 */
public class JavaLanguageAnalyzer implements LanguageAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(JavaLanguageAnalyzer.class);

    @Override
    public boolean supports(String fileName) {
        return fileName.endsWith(".java");
    }

    @Override
    public AnalysisResult analyze(String sourceCode) {
        logger.debug("Analyzing Java source code...");
        List<String> errors = new ArrayList<>();
        
        CharStream charStream = CharStreams.fromString(sourceCode);
        Java20Lexer lexer = new Java20Lexer(charStream);
        
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add("Lexer error at " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java20Parser parser = new Java20Parser(tokens);
        
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add("Parser error at " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        ParseTree tree = parser.start_();
        
        String skeleton = "";
        if (errors.isEmpty()) {
            skeleton = skeletonize(tokens, tree);
        }

        return new AnalysisResult(skeleton, errors);
    }

    private String skeletonize(CommonTokenStream tokens, ParseTree tree) {
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();
        
        walker.walk(new Java20ParserBaseListener() {
            private ParserRuleContext activeOmission = null;

            @Override
            public void enterMethodBody(Java20Parser.MethodBodyContext ctx) {
                if (activeOmission == null && ctx.block() != null) {
                    activeOmission = ctx;
                    rewriter.replace(ctx.block().getStart(), ctx.block().getStop(), " { /* implementation omitted */ }");
                }
            }

            @Override
            public void exitMethodBody(Java20Parser.MethodBodyContext ctx) {
                if (activeOmission == ctx) {
                    activeOmission = null;
                }
            }

            @Override
            public void enterConstructorBody(Java20Parser.ConstructorBodyContext ctx) {
                if (activeOmission == null) {
                    activeOmission = ctx;
                    rewriter.replace(ctx.getStart(), ctx.getStop(), " { /* implementation omitted */ }");
                }
            }

            @Override
            public void exitConstructorBody(Java20Parser.ConstructorBodyContext ctx) {
                if (activeOmission == ctx) {
                    activeOmission = null;
                }
            }
            
            @Override
            public void enterStaticInitializer(Java20Parser.StaticInitializerContext ctx) {
                if (activeOmission == null && ctx.block() != null) {
                    activeOmission = ctx;
                    rewriter.replace(ctx.block().getStart(), ctx.block().getStop(), " { /* implementation omitted */ }");
                }
            }

            @Override
            public void exitStaticInitializer(Java20Parser.StaticInitializerContext ctx) {
                if (activeOmission == ctx) {
                    activeOmission = null;
                }
            }
            
            @Override
            public void enterInstanceInitializer(Java20Parser.InstanceInitializerContext ctx) {
                if (activeOmission == null && ctx.block() != null) {
                    activeOmission = ctx;
                    rewriter.replace(ctx.block().getStart(), ctx.block().getStop(), " { /* implementation omitted */ }");
                }
            }

            @Override
            public void exitInstanceInitializer(Java20Parser.InstanceInitializerContext ctx) {
                if (activeOmission == ctx) {
                    activeOmission = null;
                }
            }
        }, tree);

        return rewriter.getText();
    }
}
