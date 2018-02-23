/*******************************************************************************
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext.generator.peweb

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.inject.Inject
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.regex.Pattern
import org.eclipse.emf.mwe2.runtime.Mandatory
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.GrammarUtil
import org.eclipse.xtext.util.DisposableRegistry
import org.eclipse.xtext.xtext.generator.AbstractXtextGeneratorFragment
import org.eclipse.xtext.xtext.generator.CodeConfig
import org.eclipse.xtext.xtext.generator.Issues
import org.eclipse.xtext.xtext.generator.XtextGeneratorNaming
import org.eclipse.xtext.xtext.generator.model.FileAccessFactory
import org.eclipse.xtext.xtext.generator.model.TypeReference
import org.eclipse.xtext.xtext.generator.util.BooleanGeneratorOption
import org.eclipse.xtext.xtext.generator.util.GeneratorOption
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector

import static extension org.eclipse.xtext.GrammarUtil.*
import static extension org.eclipse.xtext.xtext.generator.model.TypeReference.*
import static extension org.eclipse.xtext.xtext.generator.util.GrammarUtil2.*
import static extension org.eclipse.xtext.xtext.generator.web.RegexpExtensions.*

/**
 * Main generator fragment for projectional editing web integration.
 */
class PeWebIntegrationFragment  extends AbstractXtextGeneratorFragment {
	
	public static enum Framework {
		ORION, ACE, CODEMIRROR
	}
	
	static val REQUIREJS_VERSION = '2.3.2'
	static val REQUIREJS_TEXT_VERSION = '2.0.15'
	static val JQUERY_VERSION = '2.2.4'
	static val ACE_VERSION = '1.2.3'
	static val CODEMIRROR_VERSION = '5.13.2'
	
	@Inject FileAccessFactory fileAccessFactory
	@Inject CodeConfig codeConfig
	@Inject extension XtextGeneratorNaming
	@Inject extension XbaseUsageDetector
	
	val enabledPatterns = new HashSet<String>
	val suppressedPatterns = new HashSet<String>
	
	@Accessors(PUBLIC_GETTER)
	val framework = new GeneratorOption<Framework>
	
	@Accessors(PUBLIC_GETTER)
	val generateJsHighlighting = new BooleanGeneratorOption(true)
	
	@Accessors(PUBLIC_GETTER)
	val generateServlet = new BooleanGeneratorOption(false)
	
	@Accessors(PUBLIC_GETTER)
	val generateJettyLauncher = new BooleanGeneratorOption(false)
	
	@Accessors(PUBLIC_GETTER)
	val generateWebXml = new BooleanGeneratorOption(false)
	
	@Accessors(PUBLIC_GETTER)
	val generateHtmlExample = new BooleanGeneratorOption(false)
	
	String highlightingModuleName
	String highlightingPath
	String keywordsFilter = '\\w+'
	boolean useServlet3Api = true
	boolean ignoreCase = false
	
	@Accessors(PUBLIC_SETTER)
	String requireJsVersion = REQUIREJS_VERSION
	
	@Accessors(PUBLIC_SETTER)
	String requireJsTextVersion = REQUIREJS_TEXT_VERSION
	
	@Accessors(PUBLIC_SETTER)
	String jQueryVersion = JQUERY_VERSION
	
	@Accessors(PUBLIC_SETTER)
	String aceVersion = ACE_VERSION
	
	@Accessors(PUBLIC_SETTER)
	String codeMirrorVersion = CODEMIRROR_VERSION
	
	/**
	 * Choose one of the supported frameworks: {@code "Orion"}, {@code "Ace"}, or {@code "CodeMirror"}
	 */
	@Mandatory
	def void setFramework(String frameworkName) {
		this.framework.set(Framework.valueOf(frameworkName.toUpperCase))
	}
	
	/**
	 * Whether JavaScript-based syntax highlighting should be generated. The default is {@code true}.
	 */
	def void setGenerateJsHighlighting(boolean generateJsHighlighting) {
		this.generateJsHighlighting.set(generateJsHighlighting)
	}
	
	/**
	 * Name of the syntax highlighting RequireJS module to be generated.
	 */
	def void setHighlightingModuleName(String moduleName) {
		this.highlightingModuleName = moduleName
	}
	
	/**
	 * The path of the syntax highlighting JavaScript file to be generated. The default is to
	 * derive the path from the {@code highlightingModuleName} property.
	 */
	def void setHighlightingPath(String path) {
		this.highlightingPath = path
	}
	
	/**
	 * Regular expression for filtering those language keywords that should be highlighted. The default
	 * is {@code \w+}, i.e. keywords consisting only of letters and digits.
	 */
	def void setKeywordsFilter(String keywordsFilter) {
		this.keywordsFilter = keywordsFilter
	}
	
	/**
	 * Whether a servlet for DSL-specific services should be generated. The default is {@code false}.
	 */
	def void setGenerateServlet(boolean generateServlet) {
		this.generateServlet.set(generateServlet)
	}
	
	/**
	 * Whether a web.xml file should be generated. The default is {@code false} (not necessary for Servlet 3 compatible containers).
	 */
	def void setGenerateWebXml(boolean generateWebXml) {
		this.generateWebXml.set(generateWebXml)
	}
	
	/**
	 * Whether the Servlet 3 API ({@code WebServlet} annotation) should be used for the generated servlet.
	 * The default is {@code true}.
	 */
	def void setUseServlet3Api(boolean useServlet3Api) {
		this.useServlet3Api = useServlet3Api
	}
	
	/**
	 * Whether the generated syntax highlighting should ignore case for language keywords.
	 */
	def void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase
	}
	
	/**
	 * Whether a Java main-class for launching a local Jetty server should be generated. The default
	 * is {@code false}.
	 */
	def void setGenerateJettyLauncher(boolean generateJettyLauncher) {
		this.generateJettyLauncher.set(generateJettyLauncher)
	}
	
	/**
	 * Whether an example {@code index.html} file for testing the web-based editor should be generated.
	 * The default is {@code false}.
	 */
	def void setGenerateHtmlExample(boolean generateHtmlExample) {
		this.generateHtmlExample.set(generateHtmlExample)
	}
	
	/**
	 * Enable a default pattern for syntax highlighting. See the documentation of the chosen
	 * framework for details.
	 */
	def void addEnablePattern(String pattern) {
		enabledPatterns += pattern
	}
	
	/**
	 * Suppress a default pattern for syntax highlighting. See the documentation of the chosen
	 * framework for details. 
	 */
	def void addSuppressPattern(String pattern) {
		suppressedPatterns += pattern
	}
	
	protected def TypeReference getServerLauncherClass(Grammar grammar) {
		new TypeReference(grammar.peWebBasePackage + '.' + 'ServerLauncher')
	}
	
	protected def TypeReference getServletClass(Grammar grammar) {
		new TypeReference(grammar.peWebBasePackage + '.' + GrammarUtil.getSimpleName(grammar) + 'PeServlet')
	}
	
	override checkConfiguration(Issues issues) {
		super.checkConfiguration(issues)
		if (!framework.isSet)
			issues.addError('The property \'framework\' is required.')
		for (pattern : enabledPatterns.filter[suppressedPatterns.contains(it)]) {
			issues.addError('The pattern \'' + pattern + '\' cannot be enabled and suppressed.')
		}
	}
	
	override generate() {

		val langId = language.fileExtensions.head
		
		if (generateServlet.get && projectConfig.peWeb.src !== null) {
			generateServlet()
		}
		if (generateJettyLauncher.get && projectConfig.peWeb.src !== null) {
			generateServerLauncher()
		}
		if (generateWebXml.get && projectConfig.peWeb.assets !== null) {
			generateWebXml()
		}
	}
	
	
	protected def void generateServerLauncher() {
		fileAccessFactory.createXtendFile(grammar.serverLauncherClass, '''
			/**
			 * This program starts an HTTP server for testing the web integration of your DSL.
			 * Just execute it and point a web browser to http://localhost:8080/
			 */
			class «grammar.serverLauncherClass.simpleName» {
				def static void main(String[] args) {
					val server = new «'org.eclipse.jetty.server.Server'.typeRef»(new «'java.net.InetSocketAddress'.typeRef»('localhost', 8080))
					server.handler = new «'org.eclipse.jetty.webapp.WebAppContext'.typeRef» => [
						resourceBase = '«projectConfig.peWeb.assets.path.replace(projectConfig.peWeb.root.path + "/", "")»'
						welcomeFiles = #["index.html"]
						contextPath = "/"
						configurations = #[
							new «'org.eclipse.jetty.annotations.AnnotationConfiguration'.typeRef»,
							new «'org.eclipse.jetty.webapp.WebXmlConfiguration'.typeRef»,
							new «'org.eclipse.jetty.webapp.WebInfConfiguration'.typeRef»,
							new «'org.eclipse.jetty.webapp.MetaInfConfiguration'.typeRef»
						]
						setAttribute(«'org.eclipse.jetty.webapp.WebInfConfiguration'.typeRef».CONTAINER_JAR_PATTERN, '.*/«projectConfig.peWeb.name.replace('.', '\\\\.')»/.*,.*\\.jar')
						setInitParameter("org.mortbay.jetty.servlet.Default.useFileMappedBuffer", "false")
					]
					val log = new «'org.eclipse.jetty.util.log.Slf4jLog'.typeRef»(«grammar.serverLauncherClass.simpleName».name)
					try {
						server.start
						log.info('Server started ' + server.getURI + '...')
						new Thread[
							log.info('Press enter to stop the server...')
							val key = System.in.read
							if (key != -1) {
								server.stop
							} else {
								log.warn('Console input is not available. In order to stop the server, you need to cancel process manually.')
							}
						].start
						server.join
					} catch (Exception exception) {
						log.warn(exception.message)
						System.exit(1)
					}
				}
			}
		''').writeTo(projectConfig.peWeb.src)
	}
	
	protected def void generateServlet() {
		val injector = 'com.google.inject.Injector'.typeRef;
		fileAccessFactory.createXtendFile(grammar.servletClass, '''
			/**
			 * Deploy this class into a servlet container to enable DSL-specific services.
			 */
			«IF useServlet3Api»
				@«new TypeReference("javax.servlet.annotation.WebServlet")»(name = 'PeServices', urlPatterns = '/pe-service/*')
			«ENDIF»
			class «grammar.servletClass.simpleName» extends «'org.eclipse.xtext.peweb.servlet.PeServlet'.typeRef» {
				
				«DisposableRegistry» disposableRegistry
				
				«injector» injector
				
				override getInjector() {
					if(injector == null){
						injector =	new «grammar.peWebSetup»().createInjectorAndDoEMFRegistration()	
					}else{
						return injector
					}	
				}
				
				override init() {
					super.init()
					disposableRegistry = getInjector().getInstance(«DisposableRegistry»)
				}
				
				override destroy() {
					if (disposableRegistry !== null) {
						disposableRegistry.dispose()
						disposableRegistry = null
					}
					super.destroy()
				}
				
			}
		''').writeTo(projectConfig.peWeb.src)
	}
	
	protected def void generateWebXml() {
		throw new UnsupportedOperationException("Generation of Web Xmls for a pe editor is not yet supported");
	}
}
