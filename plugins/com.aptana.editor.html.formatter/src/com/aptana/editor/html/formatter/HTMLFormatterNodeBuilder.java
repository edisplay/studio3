/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.formatter;

import java.util.Arrays;
import java.util.HashSet;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.html.formatter.nodes.FormatterDefaultElementNode;
import com.aptana.editor.html.formatter.nodes.FormatterHTMLCommentNode;
import com.aptana.editor.html.formatter.nodes.FormatterSpecialElementNode;
import com.aptana.editor.html.formatter.nodes.FormatterVoidElementNode;
import com.aptana.editor.html.parsing.ast.HTMLElementNode;
import com.aptana.editor.html.parsing.ast.HTMLNode;
import com.aptana.editor.html.parsing.ast.HTMLNodeTypes;
import com.aptana.formatter.FormatterDocument;
import com.aptana.formatter.nodes.AbstractFormatterNodeBuilder;
import com.aptana.formatter.nodes.FormatterBlockNode;
import com.aptana.formatter.nodes.FormatterBlockWithBeginEndNode;
import com.aptana.formatter.nodes.FormatterBlockWithBeginNode;
import com.aptana.formatter.nodes.FormatterCommentNode;
import com.aptana.formatter.nodes.IFormatterContainerNode;
import com.aptana.parsing.ast.INameNode;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.lexer.IRange;

/**
 * HTML formatter node builder.<br>
 * This builder generates the formatter nodes that will then be processed by the {@link HTMLFormatterNodeRewriter} to
 * produce the output for the code formatting process.
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class HTMLFormatterNodeBuilder extends AbstractFormatterNodeBuilder
{

	/**
	 * Void Elements are elements that can <b>only</b> have a start tag.<br>
	 * 
	 * @see http://dev.w3.org/html5/spec/Overview.html#void-elements
	 */
	@SuppressWarnings("nls")
	protected static final HashSet<String> VOID_ELEMENTS = new HashSet<String>(Arrays.asList("area", "base", "br",
			"col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track",
			"wbr"));
	@SuppressWarnings("nls")
	protected static final HashSet<String> OPTIONAL_ENDING_TAGS = new HashSet<String>(Arrays.asList(""));
	private static final String INLINE_TAG_CLOSING = "/>"; //$NON-NLS-1$

	private FormatterDocument document;

	/**
	 * @param parseResult
	 * @param document
	 * @return
	 */
	public IFormatterContainerNode build(IParseNode parseResult, FormatterDocument document)
	{
		this.document = document;
		final IFormatterContainerNode rootNode = new FormatterBlockNode(document);
		start(rootNode);
		IParseNode[] children = parseResult.getChildren();
		addNodes(children);
		checkedPop(rootNode, document.getLength());
		return rootNode;
	}

	/**
	 * @param children
	 * @param rootNode
	 */
	private void addNodes(IParseNode[] children)
	{
		if (children == null || children.length == 0)
		{
			return;
		}
		for (IParseNode child : children)
		{
			addNode(child);
		}
	}

	/**
	 * @param node
	 * @param rootNode
	 */
	private void addNode(IParseNode node)
	{
		if (node instanceof HTMLNode)
		{
			// DEBUG
			// System.out.println(elementNode.getName() + "[" + elementNode.getStartingOffset() + ", "
			// + elementNode.getEndingOffset() + "]");

			HTMLNode htmlNode = (HTMLNode) node;
			if (htmlNode.getNodeType() == HTMLNodeTypes.COMMENT)
			{
				// We got a HTMLCommentNode
				FormatterCommentNode commentNode = new FormatterHTMLCommentNode(document, htmlNode.getStartingOffset(),
						htmlNode.getEndingOffset() + 1);
				// We just need to add a child here. We cannot 'push', since the comment node is not a container node.
				addChild(commentNode);
			}
			else if (htmlNode.getNodeType() == HTMLNodeTypes.ELEMENT || htmlNode.getNodeType() == HTMLNodeTypes.SPECIAL)
			{
				// Check if we need to create a formatter node with a begin and end node, or just begin node.
				HTMLElementNode elementNode = (HTMLElementNode) node;
				String name = elementNode.getName().toLowerCase();
				if (VOID_ELEMENTS.contains(name) || !hasInlineClosingTag(elementNode))
				{
					FormatterBlockWithBeginNode formatterNode = new FormatterVoidElementNode(document, name);
					formatterNode.setBegin(createTextNode(document, elementNode.getStartingOffset(), elementNode
							.getEndingOffset() + 1));
					push(formatterNode);
					checkedPop(formatterNode, -1);
				}
				else
				{
					pushFormatterNode(elementNode);
				}
			}
		}
		else
		{
			// it's a node that was generated from a different language parser, such as the RHTMLParser
			FormatterSpecialElementNode specialNode = new FormatterSpecialElementNode(document, StringUtil.EMPTY);
			int startingOffset = node.getStartingOffset();
			int endingOffset = node.getEndingOffset() + 1;
			specialNode.setBegin(createTextNode(document, startingOffset, endingOffset));
			specialNode.setEnd(createTextNode(document, endingOffset, endingOffset)); // empty end
			push(specialNode);
			checkedPop(specialNode, -1);
		}
	}

	/**
	 * @param elementNode
	 * @return
	 */
	private boolean hasInlineClosingTag(HTMLElementNode elementNode)
	{
		int startingOffset = elementNode.getStartingOffset();
		int endingOffset = elementNode.getEndingOffset();
		if (endingOffset - startingOffset > 1 && document.getLength() >= endingOffset + 1)
		{
			String text = document.get(endingOffset - 1, endingOffset + 1);
			if (INLINE_TAG_CLOSING.equals(text))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine the type of the node and return a formatter node that should represent it while rewriting the doc.<br>
	 * Ant HTMLElementNode is acceptable here, even the special nodes. These special node just represents the wrapping
	 * nodes around the 'foreign' nodes that exist as their children (nodes produced from the RHTML parser and JS
	 * parser, for example).<br>
	 * This behavior allows the inner child of these HTMLSpecialNodes to be processed in the
	 * {@link #addNode(IParseNode)} method and produce a FormatterSpecialElementNode.<br>
	 * 
	 * @param node
	 * @return FormatterBlockWithBeginEndNode sub-classing instance
	 */
	private FormatterBlockWithBeginEndNode pushFormatterNode(HTMLElementNode node)
	{
		String type = node.getName().toLowerCase();
		FormatterBlockWithBeginEndNode formatterNode;
		IRange beginNodeRange = node.getNameNode().getNameRange();
		INameNode endNode = node.getEndNode();
		int endOffset = node.getEndingOffset() + 1;
		if (endNode != null)
		{
			IRange endNodeRange = endNode.getNameRange();
			endOffset = endNodeRange.getStartingOffset();
		}

		formatterNode = new FormatterDefaultElementNode(document, type);
		formatterNode.setBegin(createTextNode(document, beginNodeRange.getStartingOffset(), beginNodeRange
				.getEndingOffset() + 1));
		push(formatterNode);
		if (node.getNodeType() == HTMLNodeTypes.SPECIAL)
		{
			// Everything under this HTMLSpecialNode should be wrapped with a
			// FormatterSpecialElementNode, and no need to visit its children.
			// The assumption here is that the wrapping HTMLElementNode of this special node
			// always have start and end tags.
			FormatterSpecialElementNode specialNode = new FormatterSpecialElementNode(document, StringUtil.EMPTY);
			int endSpecial = getEndWithoutWhiteSpaces(endNode.getNameRange().getStartingOffset() - 1, document) + 1;
			int beginSpecial = getBeginWithoutWhiteSpaces(beginNodeRange.getEndingOffset() + 1, document);
			specialNode.setBegin(createTextNode(document, beginSpecial, endSpecial));
			specialNode.setEnd(createTextNode(document, endSpecial, endSpecial)); // empty end
			push(specialNode);
			checkedPop(specialNode, -1);
		}
		else
		{
			// Recursively call this method till we are done with all the children under this node.
			addNodes(node.getChildren());
		}
		checkedPop(formatterNode, endOffset);
		formatterNode.setEnd(createTextNode(document, endOffset, node.getEndingOffset() + 1));
		return formatterNode;
	}

	/**
	 * @param i
	 * @param document2
	 * @return
	 */
	private int getBeginWithoutWhiteSpaces(int offset, FormatterDocument document)
	{
		int length = document.getLength();
		while (offset < length)
		{
			if (!Character.isWhitespace(document.charAt(offset)))
			{
				break;
			}
			offset++;
		}
		return offset;
	}

	/**
	 * @param startingOffset
	 * @param document2
	 * @return
	 */
	private int getEndWithoutWhiteSpaces(int offset, FormatterDocument document)
	{
		while (offset > 0)
		{
			if (!Character.isWhitespace(document.charAt(offset)))
			{
				break;
			}
			offset--;
		}
		return offset;
	}
}
