/**
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator.peweb;

import com.google.inject.Inject;
import java.util.HashSet;
import org.eclipse.emf.mwe2.runtime.Mandatory;
import org.eclipse.xtend.lib.annotations.AccessorType;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.util.DisposableRegistry;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xtext.generator.AbstractXtextGeneratorFragment;
import org.eclipse.xtext.xtext.generator.CodeConfig;
import org.eclipse.xtext.xtext.generator.Issues;
import org.eclipse.xtext.xtext.generator.XtextGeneratorNaming;
import org.eclipse.xtext.xtext.generator.model.FileAccessFactory;
import org.eclipse.xtext.xtext.generator.model.TypeReference;
import org.eclipse.xtext.xtext.generator.util.BooleanGeneratorOption;
import org.eclipse.xtext.xtext.generator.util.GeneratorOption;
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector;

/**
 * Main generator fragment for projectional editing web integration.
 */
@SuppressWarnings("all")
public class PeWebIntegrationFragment extends AbstractXtextGeneratorFragment {
  public enum Framework {
    ORION,
    
    ACE,
    
    CODEMIRROR;
  }
  
  private final static String REQUIREJS_VERSION = "2.3.2";
  
  private final static String REQUIREJS_TEXT_VERSION = "2.0.15";
  
  private final static String JQUERY_VERSION = "2.2.4";
  
  private final static String ACE_VERSION = "1.2.3";
  
  private final static String CODEMIRROR_VERSION = "5.13.2";
  
  @Inject
  private FileAccessFactory fileAccessFactory;
  
  @Inject
  private CodeConfig codeConfig;
  
  @Inject
  @Extension
  private XtextGeneratorNaming _xtextGeneratorNaming;
  
  @Inject
  @Extension
  private XbaseUsageDetector _xbaseUsageDetector;
  
  private final HashSet<String> enabledPatterns = new HashSet<String>();
  
  private final HashSet<String> suppressedPatterns = new HashSet<String>();
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final GeneratorOption<PeWebIntegrationFragment.Framework> framework = new GeneratorOption<PeWebIntegrationFragment.Framework>();
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final BooleanGeneratorOption generateJsHighlighting = new BooleanGeneratorOption(true);
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final BooleanGeneratorOption generateServlet = new BooleanGeneratorOption(false);
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final BooleanGeneratorOption generateJettyLauncher = new BooleanGeneratorOption(false);
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final BooleanGeneratorOption generateWebXml = new BooleanGeneratorOption(false);
  
  @Accessors(AccessorType.PUBLIC_GETTER)
  private final BooleanGeneratorOption generateHtmlExample = new BooleanGeneratorOption(false);
  
  private String highlightingModuleName;
  
  private String highlightingPath;
  
  private String keywordsFilter = "\\w+";
  
  private boolean useServlet3Api = true;
  
  private boolean ignoreCase = false;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private String requireJsVersion = PeWebIntegrationFragment.REQUIREJS_VERSION;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private String requireJsTextVersion = PeWebIntegrationFragment.REQUIREJS_TEXT_VERSION;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private String jQueryVersion = PeWebIntegrationFragment.JQUERY_VERSION;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private String aceVersion = PeWebIntegrationFragment.ACE_VERSION;
  
  @Accessors(AccessorType.PUBLIC_SETTER)
  private String codeMirrorVersion = PeWebIntegrationFragment.CODEMIRROR_VERSION;
  
  /**
   * Choose one of the supported frameworks: {@code "Orion"}, {@code "Ace"}, or {@code "CodeMirror"}
   */
  @Mandatory
  public void setFramework(final String frameworkName) {
    this.framework.set(PeWebIntegrationFragment.Framework.valueOf(frameworkName.toUpperCase()));
  }
  
  /**
   * Whether JavaScript-based syntax highlighting should be generated. The default is {@code true}.
   */
  public void setGenerateJsHighlighting(final boolean generateJsHighlighting) {
    this.generateJsHighlighting.set(generateJsHighlighting);
  }
  
  /**
   * Name of the syntax highlighting RequireJS module to be generated.
   */
  public void setHighlightingModuleName(final String moduleName) {
    this.highlightingModuleName = moduleName;
  }
  
  /**
   * The path of the syntax highlighting JavaScript file to be generated. The default is to
   * derive the path from the {@code highlightingModuleName} property.
   */
  public void setHighlightingPath(final String path) {
    this.highlightingPath = path;
  }
  
  /**
   * Regular expression for filtering those language keywords that should be highlighted. The default
   * is {@code \w+}, i.e. keywords consisting only of letters and digits.
   */
  public void setKeywordsFilter(final String keywordsFilter) {
    this.keywordsFilter = keywordsFilter;
  }
  
  /**
   * Whether a servlet for DSL-specific services should be generated. The default is {@code false}.
   */
  public void setGenerateServlet(final boolean generateServlet) {
    this.generateServlet.set(generateServlet);
  }
  
  /**
   * Whether a web.xml file should be generated. The default is {@code false} (not necessary for Servlet 3 compatible containers).
   */
  public void setGenerateWebXml(final boolean generateWebXml) {
    this.generateWebXml.set(generateWebXml);
  }
  
  /**
   * Whether the Servlet 3 API ({@code WebServlet} annotation) should be used for the generated servlet.
   * The default is {@code true}.
   */
  public void setUseServlet3Api(final boolean useServlet3Api) {
    this.useServlet3Api = useServlet3Api;
  }
  
  /**
   * Whether the generated syntax highlighting should ignore case for language keywords.
   */
  public void setIgnoreCase(final boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }
  
  /**
   * Whether a Java main-class for launching a local Jetty server should be generated. The default
   * is {@code false}.
   */
  public void setGenerateJettyLauncher(final boolean generateJettyLauncher) {
    this.generateJettyLauncher.set(generateJettyLauncher);
  }
  
  /**
   * Whether an example {@code index.html} file for testing the web-based editor should be generated.
   * The default is {@code false}.
   */
  public void setGenerateHtmlExample(final boolean generateHtmlExample) {
    this.generateHtmlExample.set(generateHtmlExample);
  }
  
  /**
   * Enable a default pattern for syntax highlighting. See the documentation of the chosen
   * framework for details.
   */
  public void addEnablePattern(final String pattern) {
    this.enabledPatterns.add(pattern);
  }
  
  /**
   * Suppress a default pattern for syntax highlighting. See the documentation of the chosen
   * framework for details.
   */
  public void addSuppressPattern(final String pattern) {
    this.suppressedPatterns.add(pattern);
  }
  
  protected TypeReference getServerLauncherClass(final Grammar grammar) {
    String _peWebBasePackage = this._xtextGeneratorNaming.getPeWebBasePackage(grammar);
    String _plus = (_peWebBasePackage + ".");
    String _plus_1 = (_plus + "ServerLauncher");
    return new TypeReference(_plus_1);
  }
  
  protected TypeReference getServletClass(final Grammar grammar) {
    String _peWebBasePackage = this._xtextGeneratorNaming.getPeWebBasePackage(grammar);
    String _plus = (_peWebBasePackage + ".");
    String _simpleName = GrammarUtil.getSimpleName(grammar);
    String _plus_1 = (_plus + _simpleName);
    String _plus_2 = (_plus_1 + "PeServlet");
    return new TypeReference(_plus_2);
  }
  
  @Override
  public void checkConfiguration(final Issues issues) {
    super.checkConfiguration(issues);
    boolean _isSet = this.framework.isSet();
    boolean _not = (!_isSet);
    if (_not) {
      issues.addError("The property \'framework\' is required.");
    }
    final Function1<String, Boolean> _function = (String it) -> {
      return Boolean.valueOf(this.suppressedPatterns.contains(it));
    };
    Iterable<String> _filter = IterableExtensions.<String>filter(this.enabledPatterns, _function);
    for (final String pattern : _filter) {
      issues.addError((("The pattern \'" + pattern) + "\' cannot be enabled and suppressed."));
    }
  }
  
  @Override
  public void generate() {
    final String langId = IterableExtensions.<String>head(this.getLanguage().getFileExtensions());
    if ((this.generateServlet.get() && (this.getProjectConfig().getPeWeb().getSrc() != null))) {
      this.generateServlet();
    }
    if ((this.generateJettyLauncher.get() && (this.getProjectConfig().getPeWeb().getSrc() != null))) {
      this.generateServerLauncher();
    }
    if ((this.generateWebXml.get() && (this.getProjectConfig().getPeWeb().getAssets() != null))) {
      this.generateWebXml();
    }
  }
  
  protected void generateServerLauncher() {
    TypeReference _serverLauncherClass = this.getServerLauncherClass(this.getGrammar());
    StringConcatenationClient _client = new StringConcatenationClient() {
      @Override
      protected void appendTo(StringConcatenationClient.TargetStringConcatenation _builder) {
        _builder.append("/**");
        _builder.newLine();
        _builder.append(" ");
        _builder.append("* This program starts an HTTP server for testing the web integration of your DSL.");
        _builder.newLine();
        _builder.append(" ");
        _builder.append("* Just execute it and point a web browser to http://localhost:8080/");
        _builder.newLine();
        _builder.append(" ");
        _builder.append("*/");
        _builder.newLine();
        _builder.append("class ");
        String _simpleName = PeWebIntegrationFragment.this.getServerLauncherClass(PeWebIntegrationFragment.this.getGrammar()).getSimpleName();
        _builder.append(_simpleName);
        _builder.append(" {");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("def static void main(String[] args) {");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("val server = new ");
        TypeReference _typeRef = TypeReference.typeRef("org.eclipse.jetty.server.Server");
        _builder.append(_typeRef, "\t\t");
        _builder.append("(new ");
        TypeReference _typeRef_1 = TypeReference.typeRef("java.net.InetSocketAddress");
        _builder.append(_typeRef_1, "\t\t");
        _builder.append("(\'localhost\', 8080))");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t");
        _builder.append("server.handler = new ");
        TypeReference _typeRef_2 = TypeReference.typeRef("org.eclipse.jetty.webapp.WebAppContext");
        _builder.append(_typeRef_2, "\t\t");
        _builder.append(" => [");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("resourceBase = \'");
        String _path = PeWebIntegrationFragment.this.getProjectConfig().getPeWeb().getAssets().getPath();
        String _path_1 = PeWebIntegrationFragment.this.getProjectConfig().getPeWeb().getRoot().getPath();
        String _plus = (_path_1 + "/");
        String _replace = _path.replace(_plus, "");
        _builder.append(_replace, "\t\t\t");
        _builder.append("\'");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("welcomeFiles = #[\"index.html\"]");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("contextPath = \"/\"");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("configurations = #[");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("new ");
        TypeReference _typeRef_3 = TypeReference.typeRef("org.eclipse.jetty.annotations.AnnotationConfiguration");
        _builder.append(_typeRef_3, "\t\t\t\t");
        _builder.append(",");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t\t");
        _builder.append("new ");
        TypeReference _typeRef_4 = TypeReference.typeRef("org.eclipse.jetty.webapp.WebXmlConfiguration");
        _builder.append(_typeRef_4, "\t\t\t\t");
        _builder.append(",");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t\t");
        _builder.append("new ");
        TypeReference _typeRef_5 = TypeReference.typeRef("org.eclipse.jetty.webapp.WebInfConfiguration");
        _builder.append(_typeRef_5, "\t\t\t\t");
        _builder.append(",");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t\t");
        _builder.append("new ");
        TypeReference _typeRef_6 = TypeReference.typeRef("org.eclipse.jetty.webapp.MetaInfConfiguration");
        _builder.append(_typeRef_6, "\t\t\t\t");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("]");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("setAttribute(");
        TypeReference _typeRef_7 = TypeReference.typeRef("org.eclipse.jetty.webapp.WebInfConfiguration");
        _builder.append(_typeRef_7, "\t\t\t");
        _builder.append(".CONTAINER_JAR_PATTERN, \'.*/");
        String _replace_1 = PeWebIntegrationFragment.this.getProjectConfig().getPeWeb().getName().replace(".", "\\\\.");
        _builder.append(_replace_1, "\t\t\t");
        _builder.append("/.*,.*\\\\.jar\')");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("setInitParameter(\"org.mortbay.jetty.servlet.Default.useFileMappedBuffer\", \"false\")");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("]");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("val log = new ");
        TypeReference _typeRef_8 = TypeReference.typeRef("org.eclipse.jetty.util.log.Slf4jLog");
        _builder.append(_typeRef_8, "\t\t");
        _builder.append("(");
        String _simpleName_1 = PeWebIntegrationFragment.this.getServerLauncherClass(PeWebIntegrationFragment.this.getGrammar()).getSimpleName();
        _builder.append(_simpleName_1, "\t\t");
        _builder.append(".name)");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t");
        _builder.append("try {");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("server.start");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("log.info(\'Server started \' + server.getURI + \'...\')");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("new Thread[");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("log.info(\'Press enter to stop the server...\')");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("val key = System.in.read");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("if (key != -1) {");
        _builder.newLine();
        _builder.append("\t\t\t\t\t");
        _builder.append("server.stop");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("} else {");
        _builder.newLine();
        _builder.append("\t\t\t\t\t");
        _builder.append("log.warn(\'Console input is not available. In order to stop the server, you need to cancel process manually.\')");
        _builder.newLine();
        _builder.append("\t\t\t\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("].start");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("server.join");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("} catch (Exception exception) {");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("log.warn(exception.message)");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("System.exit(1)");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("}");
        _builder.newLine();
      }
    };
    this.fileAccessFactory.createXtendFile(_serverLauncherClass, _client).writeTo(this.getProjectConfig().getPeWeb().getSrc());
  }
  
  protected void generateServlet() {
    final TypeReference injector = TypeReference.typeRef("com.google.inject.Injector");
    TypeReference _servletClass = this.getServletClass(this.getGrammar());
    StringConcatenationClient _client = new StringConcatenationClient() {
      @Override
      protected void appendTo(StringConcatenationClient.TargetStringConcatenation _builder) {
        _builder.append("/**");
        _builder.newLine();
        _builder.append(" ");
        _builder.append("* Deploy this class into a servlet container to enable DSL-specific services.");
        _builder.newLine();
        _builder.append(" ");
        _builder.append("*/");
        _builder.newLine();
        {
          if (PeWebIntegrationFragment.this.useServlet3Api) {
            _builder.append("@");
            TypeReference _typeReference = new TypeReference("javax.servlet.annotation.WebServlet");
            _builder.append(_typeReference);
            _builder.append("(name = \'PeServices\', urlPatterns = \'/pe-service/*\')");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("class ");
        String _simpleName = PeWebIntegrationFragment.this.getServletClass(PeWebIntegrationFragment.this.getGrammar()).getSimpleName();
        _builder.append(_simpleName);
        _builder.append(" extends ");
        TypeReference _typeRef = TypeReference.typeRef("org.eclipse.xtext.peweb.servlet.PeServlet");
        _builder.append(_typeRef);
        _builder.append(" {");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append(DisposableRegistry.class, "\t");
        _builder.append(" disposableRegistry");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append(injector, "\t");
        _builder.append(" injector");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("override getInjector() {");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("if(injector == null){");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("injector =\tnew ");
        TypeReference _peWebSetup = PeWebIntegrationFragment.this._xtextGeneratorNaming.getPeWebSetup(PeWebIntegrationFragment.this.getGrammar());
        _builder.append(_peWebSetup, "\t\t\t");
        _builder.append("().createInjectorAndDoEMFRegistration()\t");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t");
        _builder.append("}else{");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("return injector");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("}\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("override init() {");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("super.init()");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("disposableRegistry = getInjector().getInstance(");
        _builder.append(DisposableRegistry.class, "\t\t");
        _builder.append(")");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("override destroy() {");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("if (disposableRegistry !== null) {");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("disposableRegistry.dispose()");
        _builder.newLine();
        _builder.append("\t\t\t");
        _builder.append("disposableRegistry = null");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t\t");
        _builder.append("super.destroy()");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
        _builder.append("\t");
        _builder.newLine();
        _builder.append("}");
        _builder.newLine();
      }
    };
    this.fileAccessFactory.createXtendFile(_servletClass, _client).writeTo(this.getProjectConfig().getPeWeb().getSrc());
  }
  
  protected void generateWebXml() {
    throw new UnsupportedOperationException("Generation of Web Xmls for a pe editor is not yet supported");
  }
  
  @Pure
  public GeneratorOption<PeWebIntegrationFragment.Framework> getFramework() {
    return this.framework;
  }
  
  @Pure
  public BooleanGeneratorOption getGenerateJsHighlighting() {
    return this.generateJsHighlighting;
  }
  
  @Pure
  public BooleanGeneratorOption getGenerateServlet() {
    return this.generateServlet;
  }
  
  @Pure
  public BooleanGeneratorOption getGenerateJettyLauncher() {
    return this.generateJettyLauncher;
  }
  
  @Pure
  public BooleanGeneratorOption getGenerateWebXml() {
    return this.generateWebXml;
  }
  
  @Pure
  public BooleanGeneratorOption getGenerateHtmlExample() {
    return this.generateHtmlExample;
  }
  
  public void setRequireJsVersion(final String requireJsVersion) {
    this.requireJsVersion = requireJsVersion;
  }
  
  public void setRequireJsTextVersion(final String requireJsTextVersion) {
    this.requireJsTextVersion = requireJsTextVersion;
  }
  
  public void setJQueryVersion(final String jQueryVersion) {
    this.jQueryVersion = jQueryVersion;
  }
  
  public void setAceVersion(final String aceVersion) {
    this.aceVersion = aceVersion;
  }
  
  public void setCodeMirrorVersion(final String codeMirrorVersion) {
    this.codeMirrorVersion = codeMirrorVersion;
  }
}
