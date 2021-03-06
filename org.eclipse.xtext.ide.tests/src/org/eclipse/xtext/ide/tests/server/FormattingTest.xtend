/*******************************************************************************
 * Copyright (c) 2016 itemis AG (http://www.itemis.com) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ide.tests.server

import org.eclipse.xtext.ide.server.formatting.FormattingService
import org.junit.Test
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.Position

/**
 * Tests for {@link FormattingService}
 * 
 * @author Christian Dietrich - Initial contribution and API
 */
class FormattingTest extends AbstractTestLangLanguageServerTest {
	
	@Test def void testFormattingService() {
		testFormatting [
			model = '''type Foo{int bar} type Bar{Foo foo}'''
			expectedText = '''
			type Foo{
				int bar
			}
			type Bar{
				Foo foo
			}
			'''
		]
	}
	
	@Test def void testFormattingClosedFile() {
		testFormatting [
		    it.filesInScope = #{
		        'foo.testlang' -> '''type Foo{int bar} type Bar{Foo foo}'''
		    }
			expectedText = '''
			type Foo{
				int bar
			}
			type Bar{
				Foo foo
			}
			'''
		]
	}
	
	@Test def void testRangeFormattingService() {
		testRangeFormatting [
			model = '''type Foo{int bar} type Bar{Foo foo}'''
			range = new Range => [
				start = new Position(0,0)
				end = new Position(0,17)
			]
			expectedText = '''
			type Foo{
				int bar
			} type Bar{Foo foo}'''
		]
	}
	
}