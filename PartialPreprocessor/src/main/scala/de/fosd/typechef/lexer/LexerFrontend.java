/*
 * TypeChef Variability-Aware Lexer.
 */

package de.fosd.typechef.lexer;

import com.mbeddr.core.importer.PartialCodeChecker;
import de.fosd.typechef.LexerToken;
import de.fosd.typechef.VALexer;
import de.fosd.typechef.VALexer.FileSource;
import de.fosd.typechef.VALexer.LexerInput;
import de.fosd.typechef.VALexer.StreamSource;
import de.fosd.typechef.VALexer.TextSource;
import de.fosd.typechef.conditional.Choice;
import de.fosd.typechef.conditional.Conditional;
import de.fosd.typechef.conditional.One;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.FeatureModel;
import de.fosd.typechef.lexer.macrotable.MacroFilter;
import de.fosd.typechef.lexer.options.ILexerOptions;
import de.fosd.typechef.lexer.options.PartialConfiguration;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Frontend/Facade for the lexer subsystem. All external communication with the lexer (for xtc and the original lexer)
 * should go through this class
 */
public class LexerFrontend {

    private PartialCodeChecker codeChecker;
    private SourceIdentifier identifier;
    private TokenSelector tokenSelector;
    private String encoding;

    public LexerFrontend(PartialCodeChecker codeChecker, SourceIdentifier identifier, TokenSelector tokenSelector, String encoding) {
        this.codeChecker = codeChecker;
        this.identifier = identifier;
        this.tokenSelector = tokenSelector;
    }

    public LexerFrontend(PartialCodeChecker codeChecker, SourceIdentifier identifier, TokenSelector tokenSelector) {
        this.codeChecker = codeChecker;
        this.identifier = identifier;
        this.tokenSelector = tokenSelector;
    }

    public LexerFrontend(PartialCodeChecker codeChecker, SourceIdentifier identifier) {
        this(codeChecker, identifier, DefaultTokenSelector.INSTANCE);
    }

    public LexerFrontend() {
        this(null, SourceIdentifier.BASE_SOURCE, DefaultTokenSelector.INSTANCE);
    }

    /**
     * shorthand with few default options, avoiding all command-line parsing
     */
    public Conditional<LexerResult> run(final File targetFile,
                                        final boolean returnTokenList,
                                        final boolean printToStdOutput,
                                        final FeatureModel featureModel) throws LexerException, IOException {

        return run(new VALexer.FileSource(targetFile), returnTokenList, printToStdOutput, featureModel);
    }

    public Conditional<LexerResult> run(final VALexer.LexerInput input,
                                        final boolean returnTokenList,
                                        final boolean printToStdOutput,
                                        final FeatureModel featureModel) throws LexerException, IOException {

        return run(new DefaultLexerOptions(input, printToStdOutput, featureModel), returnTokenList);
    }

    public static abstract class LexerResult {
    }

    public static class LexerSuccess extends LexerResult {
        private List<LexerToken> tokens;
        private List<Pair<FeatureExpr, LexerWarning>> warnings;

        public LexerSuccess(List<LexerToken> tokens, List<Pair<FeatureExpr, LexerWarning>> warnings) {
            this.tokens = tokens;
            this.warnings = warnings;
        }

        public List<LexerToken> getTokens() {
            return tokens;
        }

        public List<Pair<FeatureExpr, LexerWarning>> getWarnings() {
            return warnings;
        }
    }

    public static abstract class LexerIssue extends LexerResult {
        protected int col;
        protected String msg;
        protected String file;
        protected int line;

        public LexerIssue(String msg, String file, int line, int col) {
            this.msg = msg;
            this.file = file;
            this.line = line;
            this.col = col;
        }

        public int getColumn() {
            return col;
        }

        public String getMessage() {
            return msg;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public String getPositionStr() {
            return getFile() + ":" + getLine() + ":" + getColumn();
        }
    }

    public static class LexerError extends LexerIssue {

        public LexerError(String msg, String file, int line, int col) {
            super(msg, file, line, col);
        }

        @Override
        public String toString() {
            return "Lexer Error: " + getMessage();
        }
    }

    public static class LexerWarning extends LexerIssue {

        public LexerWarning(String msg, String file, int line, int col) {
            super(msg, file, line, col);
        }

        @Override
        public String toString() {
            return "Lexer Warning: " + getMessage();
        }
    }

    public Conditional<LexerResult> run(final ILexerOptions options, boolean returnTokenList) throws LexerException, IOException {
        return run(new VALexer.LexerFactory() {
            @Override
            public VALexer create(FeatureModel featureModel) {
                return new Preprocessor(options.getMacroFilter(), featureModel, codeChecker, identifier);
            }
        }, options, returnTokenList);
    }

    public Conditional<LexerResult> run(VALexer.LexerFactory lexerFactory, ILexerOptions options, boolean returnTokenList) throws LexerException, IOException {
        VALexer pp = lexerFactory.create(options.getSmallFeatureModel());

        for (Warning w : options.getWarnings())
            pp.addWarning(w);
        for (Feature f : options.getFeatures())
            pp.addFeature(f);

        PreprocessorListener listener = new PreprocessorListener(pp, options);
        pp.setListener(listener);
        pp.addMacro("__TYPECHEF__", FeatureExprLib.True());

        PrintWriter output = null;
        if (options.getLexOutputFile() != null && options.getLexOutputFile().length() > 0) {
            output = new PrintWriter(new BufferedWriter(new FileWriter(options.getLexOutputFile())));
            pp.openDebugFiles(options.getLexOutputFile());
        } else if (options.isLexPrintToStdout())
            output = new PrintWriter(new OutputStreamWriter(System.out));

        if (options.getLexerPartialConfiguration() != null) {
            for (String def : options.getLexerPartialConfiguration().getDefinedFeatures())
                pp.addMacro(def, FeatureExprLib.True(), "1");
            for (String undef : options.getLexerPartialConfiguration().getUndefinedFeatures())
                pp.removeMacro(undef, FeatureExprLib.True());
        }

        // @mbeddr
        // collect all predefined macros and add them as a separate input
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> macro : options.getDefinedMacros().entrySet()) {
            buffer.append(macro.getKey() + " " + macro.getValue() + System.getProperty("line.separator"));
        }
        if (buffer.length() != 0) {
            pp.addInput(new VALexer.TextSource(buffer.toString(), true));
        }

        for (String undef : options.getUndefMacros())
            pp.removeMacro(undef, FeatureExprLib.True());

        for (String sysInclPath : options.getIncludePaths())
            pp.addSystemIncludePath(sysInclPath);
        for (String quoInclPath : options.getQuoteIncludePath())
            pp.addQuoteIncludePath(quoInclPath);


        for (String include : options.getIncludedHeaders())
            pp.addInput(new VALexer.FileSource(new File(include)), options.getEncoding());


        for (VALexer.LexerInput input : options.getInput())
            pp.addInput(input, options.getEncoding());
        if (options.getInput().isEmpty())
            pp.addInput(new VALexer.StreamSource(System.in, "<console>"));

        if (options.getFeatures().contains(Feature.DEBUG_INCLUDEPATH)) {
            System.err.println("#" + "include \"...\" search starts here:");
            for (String dir : pp.getQuoteIncludePath())
                System.err.println("  " + dir);
            System.err.println("#" + "include <...> search starts here:");
            for (String dir : pp.getSystemIncludePath())
                System.err.println("  " + dir);
            System.err.println("End of search list.");
        }

        LexerError crash = null;
        List<LexerToken> resultTokenList = new ArrayList<LexerToken>();
        List<LexerToken> attachableTokens = new ArrayList<LexerToken>();
        LexerToken lastLanguageToken = null;

        int outputLine = 1;
        try {
            // TokenFilter tokenFilter = new TokenFilter();
            for (; ; ) {
                LexerToken tok = pp.getNextToken();
                if (tok == null)
                    break;
                if (tok.isEOF())
                    break;

                if (!tok.getAttachedTokens().isEmpty()) {
                    // ensure that any attached token is cleared and later re-added to preserve the order
                    scala.collection.Iterator<LexerToken> iterator = tok.getAttachedTokens().iterator();
                    while (iterator.hasNext()) {
                        attachableTokens.add(iterator.next());
                    }
                    tok.clearAttachedTokens();
                }

                if (returnTokenList && (!options.isReturnLanguageTokensOnly() || (tokenSelector != null && tokenSelector.isLanguageToken(tok)))) {
                    lastLanguageToken = tok;
                    for (LexerToken token : attachableTokens) {
                        tok.attachToken(token);
                    }
                    attachableTokens.clear();
                    resultTokenList.add(tok);
                } else if (tokenSelector != null && tokenSelector.isAttachableToken(tok)) {

                    boolean c1 = lastLanguageToken != null;
                    boolean c2 = c1 && lastLanguageToken.getLine() == tok.getLine();
                    boolean c3 = c1 && lastLanguageToken.getSource() == tok.getSource();

                    if (c1 && c2 && c3) {
                        assert attachableTokens.isEmpty();
                        lastLanguageToken.attachToken(tok);
                    } else {
                        attachableTokens.add(tok);
                    }
                }

                if (output != null) {
                    if (options.isAdjustLineNumbers()) {
                        //adjust line numbers to .pi file for debugging
                        String image = tok.getText();
                        while (image.indexOf('\n') >= 0) {
                            outputLine++;
                            image = image.substring(image.indexOf('\n') + 1);
                        }
                        tok.setLine(outputLine);
                        if (options.getLexOutputFile() != null)
                            tok.setSourceName(options.getLexOutputFile());
                    }

                    //write to .pi file
                    tok.lazyPrint(output);
                }
            }
        } catch (Throwable e) {
            if (options.printLexerErrorsToStdErr()) {
                Preprocessor.logger.severe(e.toString());
                e.printStackTrace(System.err);
                pp.printSourceStack(System.err);
            }
            crash = new LexerError(e.toString(), "", -1, -1);
        } finally {
            if (lastLanguageToken != null) {
                for (LexerToken token : attachableTokens) {
                    lastLanguageToken.attachToken(token);
                }
            }

            pp.debugPreprocessorDone();
            if (output != null)
                output.flush();
            if (output != null && !options.isLexPrintToStdout())
                output.close();
        }

        // creating conditional result by nesting the result with all the errors received so far
        Conditional<LexerResult> result = createResult(listener.getLexerErrorList(), listener.getLexerWarningList(), resultTokenList, options.getFullFeatureModel());
        if (crash != null)
            result = new One<LexerResult>(crash);

        if (options.isPrintLexingSuccess())
            System.out.println(printLexingResult(result, FeatureExprFactory.True()));

        return result;
    }

    private Conditional<LexerResult> createResult(List<Pair<FeatureExpr, LexerError>> lexerErrorList, List<Pair<FeatureExpr, LexerWarning>> lexerWarningList, List<LexerToken> resultTokenList, FeatureModel fm) {
        LexerSuccess success = new LexerSuccess(resultTokenList, lexerWarningList);
        Conditional<LexerResult> result = new One<LexerResult>(success);

        List<Pair<FeatureExpr, LexerError>> errorList = new ArrayList<Pair<FeatureExpr, LexerError>>(lexerErrorList);
        Collections.reverse(errorList);
        for (Pair<FeatureExpr, LexerError> error : errorList)
            if (error._1.isSatisfiable(fm)) {
                result = new Choice<LexerResult>(error._1, new One<LexerResult>(error._2), result);
            }

        return result;
    }


    private String printLexingResult(Conditional<LexerResult> result, FeatureExpr feature) {
        if (result instanceof One) {
            LexerResult aresult = ((One<LexerResult>) result).value();
            if (aresult instanceof LexerSuccess)
                return (feature.toString() + "\tlexing succeeded\n");
            else {
                LexerError error = (LexerError) aresult;
                return (feature.toString() + "\tfailed: " + error.msg + " at " + error.getPositionStr() + "\n");
            }
        } else if (result instanceof Choice) {
            Choice<LexerResult> choice = (Choice<LexerResult>) result;
            FeatureExpr leftCond = feature.and(choice.condition());
            boolean left = leftCond.isSatisfiable();
            FeatureExpr rightCond = feature.andNot(choice.condition());
            boolean right = rightCond.isSatisfiable();
            String sresult = "";
            if (left)
                sresult += printLexingResult(choice.thenBranch(), leftCond);
            if (left && right)
                sresult += "\n";
            if (right)
                sresult += printLexingResult(choice.elseBranch(), feature.andNot(choice.condition()));
            return sresult;
        }
        throw new UnsupportedOperationException("cannot be called with this parameter " + result);
    }

    /**
     * helper functions (for debugging purposes) to turn a conditional parse result into a list
     * of all the tokens of successful results
     */
    public static List<LexerToken> conditionalResultToList(Conditional<LexerResult> result, FeatureExpr feature) {
        if (result instanceof One) {
            LexerResult aresult = ((One<LexerResult>) result).value();
            if (aresult instanceof LexerSuccess) {
                List<LexerToken> r = ((LexerSuccess) aresult).getTokens();
                for (LexerToken t : r)
                    t.setFeature(t.getFeature().and(feature));
                return r;
            } else {
                return Collections.emptyList();
            }
        } else if (result instanceof Choice) {
            List<LexerToken> r = new ArrayList<LexerToken>();
            Choice<LexerResult> choice = (Choice<LexerResult>) result;
            r.addAll(conditionalResultToList(choice.thenBranch(), feature.and(choice.condition())));
            r.addAll(conditionalResultToList(choice.elseBranch(), feature.andNot(choice.condition())));
            return r;
        }
        throw new UnsupportedOperationException("cannot be called with this parameter " + result);
    }

    public static FeatureExpr getErrorCondition(Conditional<LexerResult> lexerResult) {
        if (lexerResult instanceof One) {
            LexerResult aresult = ((One<LexerResult>) lexerResult).value();
            if (aresult instanceof LexerSuccess) {
                return FeatureExprFactory.False();
            } else {
                return FeatureExprFactory.True();
            }
        } else if (lexerResult instanceof Choice) {
            Choice<LexerResult> choice = (Choice<LexerResult>) lexerResult;
            return getErrorCondition(choice.thenBranch()).and(choice.condition()).or(
                    getErrorCondition(choice.elseBranch()).andNot(choice.condition())
            );
        }
        throw new UnsupportedOperationException("cannot be called with this parameter " + lexerResult);
    }


    /**
     * helper function, mostly for internal test cases
     * <p/>
     * return only a single token stream and will stop on lexer errors;
     * has very limited view of include paths
     * <p/>
     * prefer the run methods where possible
     */
    public List<LexerToken> parse(String code) throws LexerException,
            IOException {
        return parse(code, Collections.<String>emptyList(), FeatureExprFactory.empty());
    }

    public List<LexerToken> parse(String code, List<String> systemIncludePath, FeatureModel featureModel) throws LexerException,
            IOException {
        return parse(new TextSource(code), systemIncludePath, featureModel);
    }

    public List<LexerToken> parseFile(String fileName, List<String> systemIncludePath, FeatureModel featureModel)
            throws LexerException, IOException {
        return parse(new FileSource(new File(fileName)), systemIncludePath, featureModel);
    }

    public List<LexerToken> parseStream(InputStream stream, String filePath, List<String> systemIncludePath, FeatureModel featureModel)
            throws LexerException, IOException {
        return parse(new StreamSource(stream, filePath), systemIncludePath, featureModel);
    }

    public List<LexerToken> parse(LexerInput source, List<String> systemIncludePath, FeatureModel featureModel)
            throws LexerException, IOException {
        Conditional<LexerResult> result = run(new DebugLexerOptions(source, systemIncludePath, featureModel), true);
        if (result instanceof One) {
            LexerResult lexerResult = ((One<LexerResult>) result).value();
            if (lexerResult instanceof LexerSuccess)
                return ((LexerSuccess) lexerResult).getTokens();
        }
        throw new LexerException("could not get a single successful lexer result: " + result);
    }
//        Preprocessor pp = new Preprocessor(new MacroFilter(), featureModel);
//        pp.addFeature(Feature.DIGRAPHS);
//        pp.addFeature(Feature.TRIGRAPHS);
//        pp.addFeature(Feature.LINEMARKERS);
//        pp.addWarnings(Warning.allWarnings());
//        pp.setListener(new PreprocessorListener(pp) {
//            // @Override
//            // public void handleWarning(Source source, int line, int column,
//            // String msg) throws LexerException {
//            // super.handleWarning(source, line, column, msg);
//            // throw new LexerException(msg);
//            // }
//        });
//        pp.addMacro("__JCPP__", FeatureExprLib.True());
//
//        // include path
//        if (systemIncludePath != null)
//            pp.getSystemIncludePath().addAll(systemIncludePath);
//
//        pp.addInput(source);
//
//        ArrayList<LexerToken> result = new ArrayList<LexerToken>();
//        PrintWriter stdOut = new PrintWriter(new OutputStreamWriter(System.out));
//        for (; ; ) {
//            Token tok = pp.getNextToken();
//            if (tok == null)
//                break;
//            if (tok.getType() == Token.EOF)
//                break;
//
//            if (tok.getType() == Token.INVALID)
//                System.err.println("Invalid token: " + tok);
//            // throw new LexerException(...)
//
//            if (tok.isLanguageToken())
//                result.add(tok);
//            if (debug)
//                tok.lazyPrint(stdOut);
//        }
//        return result;
//    }


    public static class DefaultLexerOptions implements ILexerOptions {
        final VALexer.LexerInput input;
        final boolean printToStdOutput;
        final FeatureModel featureModel;
        private String encoding;

        public DefaultLexerOptions(final VALexer.LexerInput input,
                                   final boolean printToStdOutput,
                                   final FeatureModel featureModel,
                                   final String encoding) {
            this.input = input;
            this.printToStdOutput = printToStdOutput;
            this.featureModel = featureModel;
            this.encoding = encoding;
        }

        public DefaultLexerOptions(final VALexer.LexerInput input,
                                   final boolean printToStdOutput,
                                   final FeatureModel featureModel) {
            this(input, printToStdOutput, featureModel, null);
        }

        @Override
        public String getEncoding() {
            if (this.encoding == null) {
                return Charset.defaultCharset().name();
            } else {
                return this.encoding;
            }
        }

        @Override
        public void setEncoding(String value) {
            this.encoding = value;
        }

        private Map<String, String> definedMacros = new HashMap<String, String>();

        @Override
        public Map<String, String> getDefinedMacros() {
            return definedMacros;
        }

        private Set<String> undefMacros = new HashSet<String>();

        @Override
        public Set<String> getUndefMacros() {
            return undefMacros;
        }

        // @mbeddr - make the list unique by using a linked hashset
        private Set<String> includePaths = new LinkedHashSet<String>();

        @Override
        public List<String> getIncludePaths() {
            return Collections.unmodifiableList(new ArrayList<String>(this.includePaths));
        }

        @Override
        public void addIncludePath(String path) {
            includePaths.add(SourceIdentifier.normalize(path));
        }

        @Override
        public void addQuoteIncludePath(String path) {
            quoteIncludePaths.add(SourceIdentifier.normalize(path));
        }

        private Set<String> quoteIncludePaths = new LinkedHashSet<String>();

        @Override
        public List<String> getQuoteIncludePath() {
            return Collections.unmodifiableList(new ArrayList<String>(this.quoteIncludePaths));
        }

        @Override
        public MacroFilter getMacroFilter() {
            return new MacroFilter();
        }

        @Override
        public List<String> getIncludedHeaders() {
            return Collections.emptyList();
        }

        @Override
        public String getLexOutputFile() {
            return null;
        }

        @Override
        public Set<Warning> getWarnings() {
            return Collections.emptySet();
        }

        @Override
        public boolean isPrintWarnings() {
            return true;
        }

        @Override
        public boolean isPrintLexingSuccess() {
            return false;
        }

        @Override
        public Set<Feature> getFeatures() {
            return Collections.emptySet();
        }

        @Override
        public List<VALexer.LexerInput> getInput() {
            return Collections.singletonList(input);
        }


        @Override
        public boolean isLexPrintToStdout() {
            return printToStdOutput;
        }

        @Override
        public boolean printLexerErrorsToStdErr() {
            return true;
        }

        @Override
        public boolean useXtcLexer() {
            return false;
        }

        @Override
        public FeatureModel getSmallFeatureModel() {
            return featureModel;
        }

        @Override
        public FeatureModel getFullFeatureModel() {
            return featureModel;
        }

        @Override
        public PartialConfiguration getLexerPartialConfiguration() {
            return null;
        }

        @Override
        public boolean isAdjustLineNumbers() {
            return true;
        }

        @Override
        public boolean isReturnLanguageTokensOnly() {
            return true;
        }

        @Override
        public boolean isHandleWarningsAsErrors() {
            return false;
        }
    }


    public static class DebugLexerOptions implements ILexerOptions {
        private final VALexer.LexerInput source;
        private final List<String> systemIncludePath;
        private final FeatureModel featureModel;
        private final Set<Feature> features = new HashSet<Feature>();

        {
            features.add(Feature.DIGRAPHS);
            features.add(Feature.TRIGRAPHS);
            features.add(Feature.LINEMARKERS);
            features.add(Feature.GNUCEXTENSIONS);

        }

        @Override
        public String getEncoding() {
            return Charset.defaultCharset().name();
        }

        @Override
        public void setEncoding(String value) {

        }

        protected DebugLexerOptions(VALexer.LexerInput source, List<String> systemIncludePath, FeatureModel featureModel) {
            this.source = source;
            this.systemIncludePath = systemIncludePath;
            this.featureModel = featureModel;
        }

        @Override
        public Map<String, String> getDefinedMacros() {
            return Collections.emptyMap();
        }

        @Override
        public Set<String> getUndefMacros() {
            return Collections.emptySet();
        }

        @Override
        public List<String> getIncludePaths() {
            return systemIncludePath;
        }

        @Override
        public List<String> getQuoteIncludePath() {
            return Collections.emptyList();
        }

        @Override
        public MacroFilter getMacroFilter() {
            return new MacroFilter();
        }

        @Override
        public List<String> getIncludedHeaders() {
            return Collections.emptyList();
        }

        @Override
        public String getLexOutputFile() {
            return null;
        }


        @Override
        public Set<Warning> getWarnings() {
            return new HashSet<Warning>(Warning.allWarnings());
        }

        @Override
        public boolean isPrintWarnings() {
            return false;
        }

        @Override
        public boolean isPrintLexingSuccess() {
            return false;
        }

        @Override
        public Set<Feature> getFeatures() {
            return features;
        }

        @Override
        public List<VALexer.LexerInput> getInput() {
            return Collections.singletonList(source);
        }


        @Override
        public boolean isLexPrintToStdout() {
            return false;
        }

        @Override
        public boolean printLexerErrorsToStdErr() {
            return false;
        }

        @Override
        public boolean useXtcLexer() {
            return false;
        }

        @Override
        public FeatureModel getSmallFeatureModel() {
            return featureModel;
        }

        @Override
        public FeatureModel getFullFeatureModel() {
            return featureModel;
        }

        @Override
        public PartialConfiguration getLexerPartialConfiguration() {
            return null;
        }

        @Override
        public boolean isAdjustLineNumbers() {
            return true;
        }

        @Override
        public boolean isReturnLanguageTokensOnly() {
            return true;
        }

        @Override
        public boolean isHandleWarningsAsErrors() {
            return false;
        }

        @Override
        public void addIncludePath(String path) {

        }

        @Override
        public void addQuoteIncludePath(String path) {

        }
    }
}
