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
package com.aptana.editor.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.aptana.editor.common.contentassist.ContentAssistant;
import com.aptana.editor.common.contentassist.InformationControl;
import com.aptana.editor.common.hover.CommonAnnotationHover;
import com.aptana.editor.common.internal.formatter.CommonMultiPassContentFormatter;
import com.aptana.editor.common.preferences.IPreferenceConstants;
import com.aptana.editor.common.text.CommonDoubleClickStrategy;
import com.aptana.editor.common.text.RubyRegexpAutoIndentStrategy;
import com.aptana.editor.common.text.reconciler.CommonCompositeReconcilingStrategy;
import com.aptana.editor.common.text.reconciler.CommonReconciler;
import com.aptana.formatter.ScriptFormatterManager;
import com.aptana.theme.IThemeManager;
import com.aptana.theme.ThemePlugin;

@SuppressWarnings("restriction")
public abstract class CommonSourceViewerConfiguration extends TextSourceViewerConfiguration implements
		ITopContentTypesProvider
{
	private AbstractThemeableEditor fTextEditor;
	private CommonDoubleClickStrategy fDoubleClickStrategy;
	private IPreferenceChangeListener fThemeChangeListener;
	private IPreferenceChangeListener fAutoActivationListener;
	protected static final String CONTENTTYPE_HTML_PREFIX = "com.aptana.contenttype.html"; //$NON-NLS-1$
	public static final int DEFAULT_CONTENT_ASSIST_DELAY = 200;
	public static final int LONG_CONTENT_ASSIST_DELAY = 1000;
	
	/**
	 * CommonSourceViewerConfiguration
	 * 
	 * @param preferenceStore
	 * @param editor
	 */
	public CommonSourceViewerConfiguration(IPreferenceStore preferenceStore, AbstractThemeableEditor editor)
	{
		super(preferenceStore);

		fTextEditor = editor;
	}

	/**
	 * dispose
	 */
	public void dispose()
	{
		fTextEditor = null;
		fDoubleClickStrategy = null;
		if (fAutoActivationListener != null)
		{
			new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).removePreferenceChangeListener(fAutoActivationListener);
			fAutoActivationListener = null;
		}
		if (fThemeChangeListener != null)
		{
			new InstanceScope().getNode(ThemePlugin.PLUGIN_ID).removePreferenceChangeListener(fThemeChangeListener);
			fThemeChangeListener = null;
		}
	}

	/**
	 * getAbstractThemeableEditor
	 * 
	 * @return
	 */
	protected AbstractThemeableEditor getAbstractThemeableEditor()
	{
		return fTextEditor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.
	 * ISourceViewer)
	 */
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer)
	{
		return new CommonAnnotationHover(false)
		{
			protected boolean isIncluded(Annotation annotation)
			{
				return isShowInVerticalRuler(annotation);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.
	 * ISourceViewer, java.lang.String)
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType)
	{
		return new IAutoEditStrategy[] { new RubyRegexpAutoIndentStrategy(contentType, this, sourceViewer) };
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.
	 * ISourceViewer)
	 */
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
	{
		final ContentAssistant assistant = new ContentAssistant();
		assistant.setProposalSelectorBackground(getThemeBackground());
		assistant.setProposalSelectorForeground(getThemeForeground());
		assistant.setProposalSelectorSelectionColor(getThemeSelection());

		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		String[] contentTypes = getConfiguredContentTypes(sourceViewer);

		for (String type : contentTypes)
		{
			IContentAssistProcessor processor = getContentAssistProcessor(sourceViewer, type);

			if (processor != null)
			{
				assistant.setContentAssistProcessor(processor, type);
			}
		}

		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		if (fPreferenceStore != null)
		{
			setAutoActivationOptions(assistant);
		}

		fAutoActivationListener = new IPreferenceChangeListener()
		{
			public void preferenceChange(PreferenceChangeEvent event)
			{
				setAutoActivationOptions(assistant);
			}
		};
		new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).addPreferenceChangeListener(fAutoActivationListener);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);

		fThemeChangeListener = new IPreferenceChangeListener()
		{

			public void preferenceChange(PreferenceChangeEvent event)
			{
				if (event.getKey().equals(IThemeManager.THEME_CHANGED))
				{
					assistant.setProposalSelectorBackground(getThemeBackground());
					assistant.setProposalSelectorForeground(getThemeForeground());
					assistant.setProposalSelectorSelectionColor(getThemeSelection());
				}
			}
		};
		new InstanceScope().getNode(ThemePlugin.PLUGIN_ID).addPreferenceChangeListener(fThemeChangeListener);

		
		return assistant;
	}

	private void setAutoActivationOptions(final ContentAssistant assistant)
	{
		int delay = fPreferenceStore.getInt(IPreferenceConstants.CONTENT_ASSIST_DELAY);
		if(delay >= 0) {
			assistant.enableAutoActivation(true);
			assistant.setAutoActivationDelay(delay);				
		}
		else {
			assistant.enableAutoActivation(false);
		}
	}

	/**
	 * Returns the content assist processor that will be used for content assist in the given source viewer and for the
	 * given partition type.
	 * 
	 * @param sourceViewer
	 *            the source viewer to be configured by this configuration
	 * @param contentType
	 *            the partition type for which the content assist processor is applicable
	 * @return IContentAssistProcessor or null if the content type is not supported
	 */
	protected IContentAssistProcessor getContentAssistProcessor(ISourceViewer sourceViewer, String contentType)
	{
		return new CommonContentAssistProcessor(getAbstractThemeableEditor());
	}

	/**
	 * Collects the code formatters by the supported content-types and returns a new {@link import
	 * org.eclipse.jface.text.formatter.MultiPassContentFormatter} that holds them.<br>
	 * The returned content formatter is computed from the result of {@link #getTopContentTypes()}. The first element in
	 * the returned array should define the 'master' formatter. While the rest of the elements should contain the
	 * 'slave' formatter. <br>
	 * Note that each slave formatter is located in the last element of each inner-array that was returned from the
	 * getTopContentTypes call.
	 */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer)
	{
		final String[][] contentTypes = getTopContentTypes();
		final CommonMultiPassContentFormatter formatter = new CommonMultiPassContentFormatter(
				getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE);
		boolean masterSet = false;
		Set<String> addedFormatters = new HashSet<String>();
		for (String contentTypeArr[] : contentTypes)
		{
			// The first item in the array should contain the master formatter strategy
			// In case it starts with the HTML prefix (like in PHP, ERB etc.), we try to set
			// the master to the HTML formatter.
			if (!masterSet && contentTypeArr[0].startsWith(CONTENTTYPE_HTML_PREFIX))
			{
				if (ScriptFormatterManager.hasFormatterFor(CONTENTTYPE_HTML_PREFIX))
				{
					formatter.setMasterStrategy(CONTENTTYPE_HTML_PREFIX);
					masterSet = true;
					addedFormatters.add(CONTENTTYPE_HTML_PREFIX);
				}
				else
				{
					CommonEditorPlugin
							.logWarning("Could not located an expected code formatter for '" + CONTENTTYPE_HTML_PREFIX + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			String contentType = contentTypeArr[contentTypeArr.length - 1];
			if (!addedFormatters.contains(contentType) && ScriptFormatterManager.hasFormatterFor(contentType))
			{
				if (!masterSet)
				{
					formatter.setMasterStrategy(contentType);
					masterSet = true;
				}
				else
				{
					formatter.setSlaveStrategy(contentType);
				}
			}
		}
		return formatter;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.source.SourceViewerConfiguration#getDoubleClickStrategy(org.eclipse.jface.text.source.
	 * ISourceViewer, java.lang.String)
	 */
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType)
	{
		if (fDoubleClickStrategy == null)
		{
			fDoubleClickStrategy = new CommonDoubleClickStrategy();
		}

		return fDoubleClickStrategy;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getHyperlinkDetectorTargets(org.eclipse.jface.text.
	 * source.ISourceViewer)
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" })
	@Override
	protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer)
	{
		Map targets = super.getHyperlinkDetectorTargets(sourceViewer);

		targets.put("com.aptana.editor.ui.hyperlinkTarget", fTextEditor); //$NON-NLS-1$

		return targets;
	}

	/**
	 * @return the default indentation string (either tab or spaces which represents a tab)
	 */
	public String getIndent()
	{
		boolean useSpaces = fPreferenceStore
				.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);

		if (useSpaces)
		{
			int tabWidth = fPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
			StringBuilder buf = new StringBuilder();

			for (int i = 0; i < tabWidth; ++i)
			{
				buf.append(" "); //$NON-NLS-1$
			}

			return buf.toString();
		}

		return "\t"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationControlCreator(org.eclipse.jface.text.source
	 * .ISourceViewer)
	 */
	@Override
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer)
	{
		return new IInformationControlCreator()
		{
			public IInformationControl createInformationControl(Shell parent)
			{
				return new InformationControl(parent, SWT.NONE, new HTMLTextPresenter(false))
				{
					@Override
					protected Color getBackground()
					{
						return getThemeBackground();
					}

					@Override
					protected Color getForeground()
					{
						return getThemeForeground();
					}

					@Override
					protected Color getBorderColor()
					{
						return getForeground();
					}
				};
			}
		};
	}

	protected Color getThemeBackground()
	{
		RGB bg = ThemePlugin.getDefault().getThemeManager().getCurrentTheme().getBackground();
		return ThemePlugin.getDefault().getColorManager().getColor(bg);
	}

	protected Color getThemeForeground()
	{
		RGB bg = ThemePlugin.getDefault().getThemeManager().getCurrentTheme().getForeground();
		return ThemePlugin.getDefault().getColorManager().getColor(bg);
	}

	protected Color getThemeSelection()
	{
		RGB bg = ThemePlugin.getDefault().getThemeManager().getCurrentTheme().getSelectionAgainstBG();
		return ThemePlugin.getDefault().getColorManager().getColor(bg);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationPresenter(org.eclipse.jface.text.source
	 * .ISourceViewer)
	 */
	@Override
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer)
	{
		InformationPresenter presenter = new InformationPresenter(getInformationPresenterControlCreator(sourceViewer));

		presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setSizeConstraints(60, 10, true, true);

		return presenter;
	}

	/**
	 * getInformationPresenterControlCreator
	 * 
	 * @param sourceViewer
	 * @return
	 */
	private IInformationControlCreator getInformationPresenterControlCreator(ISourceViewer sourceViewer)
	{
		return new IInformationControlCreator()
		{
			public IInformationControl createInformationControl(Shell parent)
			{
				return new DefaultInformationControl(parent, true);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getOverviewRulerAnnotationHover(org.eclipse.jface.text
	 * .source.ISourceViewer)
	 */
	@Override
	public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer)
	{
		return new CommonAnnotationHover(true)
		{
			protected boolean isIncluded(Annotation annotation)
			{
				return isShowInOverviewRuler(annotation);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.editors.text.TextSourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer
	 * )
	 */
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer)
	{
		if (fTextEditor != null && fTextEditor.isEditable())
		{
			CommonCompositeReconcilingStrategy strategy = new CommonCompositeReconcilingStrategy(fTextEditor,
					getConfiguredDocumentPartitioning(sourceViewer));
			CommonReconciler reconciler = new CommonReconciler(fTextEditor, strategy, false);

			reconciler.setIsIncrementalReconciler(false);
			reconciler.setIsAllowedToModifyDocument(false);
			reconciler.setProgressMonitor(new NullProgressMonitor());
			reconciler.setDelay(500);

			return reconciler;
		}

		return null;
	}

	protected AbstractThemeableEditor getEditor()
	{
		return fTextEditor;
	}
}
