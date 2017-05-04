package org.eclipse.scout.rt.client.ui.form.fields.smartfield2;

import java.util.List;

import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTree;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.AbstractSmartField;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.ContentAssistFieldListener;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.IContentAssistFieldTable;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.IContentAssistFieldUIFacade;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.IContentAssistSearchParam;
import org.eclipse.scout.rt.client.ui.form.fields.smartfield.TreeProposalChooser;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.util.TriState;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRowFetchedCallback;

// FIXME [awe] 7.0 - SF2: lookup/fetch schicht entrümpeln, prüfen ob wir den in-background bzw. den synchronous teil entfernen können.

/**
 * Generic type V: value of the SmartField2, which is also the key used in lookup-rows.
 */
public interface ISmartField2<VALUE> extends IValueField<VALUE> {

  String PROP_RESULT = "result";
  String PROP_ACTIVE_FILTER_ENABLED = "activeFilterEnabled";
  String PROP_ACTIVE_FILTER = "activeFilter";
  String PROP_ACTIVE_FILTER_LABELS = "activeFilterLabels";

  /**
   * Hint to mark the {@link IFuture} used to load the field's initial lookup rows. Typically, this future must not be
   * cancelled.
   * <p>
   * e.g {@link TreeProposalChooser} requires data to apply tree filter.
   */
  String EXECUTION_HINT_INITIAL_LOOKUP = "initialLookup";

  String PROP_BROWSE_ICON_ID = "browseIconId";
  String PROP_ICON_ID = "iconId";
  String PROP_MULTILINE_TEXT = "multilineText";
  String PROP_BROWSE_MAX_ROW_COUNT = "browseMaxRowCount";

  void lookupAll();

  void lookupByText(String text);

  SmartField2Result getResult();

  void addSmartFieldListener(ContentAssistFieldListener listener);

  void removeSmartFieldListener(ContentAssistFieldListener listener);

  /**
   * true: inactive rows are display and can be also be parsed using the UI facade according to
   * {@link #getActiveFilter()} false: inactive rows are only display when the smart field valid is set by the model.
   * The UI facade cannot choose such a value.
   */
  boolean isActiveFilterEnabled();

  /**
   * see {@link #isActiveFilterEnabled()}
   */
  void setActiveFilterEnabled(boolean enabled);

  /**
   * Changes the default-label text for the active-filter radio-button with the given state.
   */
  void setActiveFilterLabel(TriState state, String label);

  /**
   * Returns the label-texts of the active-filter radio-button in this order:
   * <ol>
   * <li>UNDEFINED</li>
   * <li>FALSE</li>
   * <li>TRUE</li>
   * </ol>
   */
  String[] getActiveFilterLabels();

  /**
   * This has only an effect if {@link #isActiveFilterEnabled()} is set to true. true: include only active values false:
   * include only inactive values undefined: include active and inactive values
   */
  TriState getActiveFilter();

  /**
   * see {@link #getActiveFilter()}
   */
  void setActiveFilter(TriState state);

  String getBrowseIconId();

  void setBrowseIconId(String s);

  int getBrowseMaxRowCount();

  void setBrowseMaxRowCount(int n);

  String getIconId();

  void setIconId(String s);

  /**
   * @since 5.1
   */
  void setMultilineText(boolean b);

  /**
   * @since 5.1
   */
  boolean isMultilineText();

  /**
   * For performance optimization, style loading is done lazily. However, sometimes it is useful to apply these changes
   * immediately.
   * <p>
   * This method is called automatically by {@link #getDisplayText()}, {@link #getTooltipText()},
   * {@link #getBackgroundColor()}, {@link #getForegroundColor()} and {@link #getFont()}
   */
  void applyLazyStyles();

  boolean isBrowseAutoExpandAll();

  void setBrowseAutoExpandAll(boolean b);

  boolean isBrowseHierarchy();

  void setBrowseHierarchy(boolean b);

  boolean isBrowseLoadIncremental();

  void setBrowseLoadIncremental(boolean b);

  boolean isLoadParentNodes();

  void setLoadParentNodes(boolean b);

  /**
   * see {@link AbstractSmartField#execBrowseNew(String)}
   */
  String getBrowseNewText();

  /**
   * see {@link AbstractSmartField#execBrowseNew(String)}
   */
  void setBrowseNewText(String s);

  /**
   * see {@link AbstractSmartField#execBrowseNew(String)}
   */
  void doBrowseNew(String newText);

  /**
   * Filter selection of hierarchy browse tree. The level reported here is different than the one used in
   * {@link AbstractTree#execAcceptSelection(org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode, int)} such as this
   * level is one smaller. This is because a tree smart field assumes its tree to have multiple roots, but the ITree
   * model is built as single-root tree with invisible root node. level=-1 is the invisible (anonymous) root level=0 are
   * the multiple roots of the smart tree ...
   */
  boolean acceptBrowseHierarchySelection(VALUE value, int level, boolean leaf);

  /**
   * variant A
   */
  Class<? extends ICodeType<?, VALUE>> getCodeTypeClass();

  void setCodeTypeClass(Class<? extends ICodeType<?, VALUE>> codeType);

  /**
   * variant B
   */
  ILookupCall<VALUE> getLookupCall();

  void setLookupCall(ILookupCall<VALUE> call);

  void prepareKeyLookup(ILookupCall<VALUE> call, VALUE key);

  void prepareTextLookup(ILookupCall<VALUE> call, String text);

  void prepareBrowseLookup(ILookupCall<VALUE> call, String browseHint, TriState activeState);

  void prepareRecLookup(ILookupCall<VALUE> call, VALUE parentKey, TriState activeState);

  /**
   * If the browse lookup call yields exactly one value, assign it to the smartfield, otherwise do nothing.
   *
   * @param background
   *          true (default) if assignment should be done later which allows for one batch call for all smartfields.
   *          Using background=false assigns the value immediately, which results in an immediate call to the data
   *          provider. Whenever possible, background=true should be used to allow for batch calls to the backend.
   * @since 22.05.2009
   */
  void setUniquelyDefinedValue(boolean background);

  /**
   * Sets the current lookup-row to null and also the accepted proposal from the proposal chooser (if available).
   */
  void clearProposal();

  /**
   * This method is normally used by a {@link IContentAssistFieldProposalForm#acceptProposal()}
   */
  void acceptProposal(ILookupRow<VALUE> row);

  IContentAssistFieldUIFacade getUIFacade();

  VALUE getValueAsLookupKey();

  /**
   * Sets the height of the proposal form in pixel. (The proposal form is smaller if there are not enought values to
   * fill the proposal.)
   *
   * @param proposalFormHeight
   *          height in pixel
   */
  void setProposalFormHeight(int proposalFormHeight);

  /**
   * @return the height of the proposal form in pixel
   */
  int getProposalFormHeight();

  /**
   * @return
   */
  Class<? extends IContentAssistFieldTable<VALUE>> getContentAssistFieldTableClass();

  void acceptProposal();

  void setWildcard(String wildcard);

  String getWildcard();

  //search and update the field with the result

  /**
   * updates the lookup rows with the same search text as last time.
   *
   * @param selectCurrentValue
   * @param synchronous
   */
  void doSearch(boolean selectCurrentValue, boolean synchronous);

  /**
   * @param searchText
   * @param selectCurrentValue
   * @param synchronous
   */
  void doSearch(String searchText, boolean selectCurrentValue, boolean synchronous);

  void doSearch(IContentAssistSearchParam<VALUE> param, boolean synchronous);

  // blocking lookups
  /**
   * Lookup rows by key using {@link ILookupCall#getDataByKey()}. Blocks until the result is available.
   *
   * @param key
   *          lookup key
   * @return rows not <code>null</code>
   */
  List<? extends ILookupRow<VALUE>> callKeyLookup(VALUE key);

  /**
   * Lookup rows by text {@link ILookupCall#getDataByText()}. Blocks until the result is available.
   *
   * @param text
   *          search text
   * @return rows not <code>null</code>
   */
  List<? extends ILookupRow<VALUE>> callTextLookup(String text, int maxRowCount);

  /**
   * Lookup all rows using {@link ILookupCall#getDataByAll()}. Blocks until the result is available.
   *
   * @return rows not <code>null</code>
   */
  List<? extends ILookupRow<VALUE>> callBrowseLookup(String browseHint, int maxRowCount);

  /**
   * Lookup all rows using {@link ILookupCall#getDataByAll()}. Blocks until the result is available.
   *
   * @return rows not <code>null</code>
   */
  List<? extends ILookupRow<VALUE>> callBrowseLookup(String browseHint, int maxRowCount, TriState activeState);

  /**
   * Lookup rows of a parent key using {@link ILookupCall#getDataByRec()}. Blocks until the result is available.
   *
   * @return rows not <code>null</code>
   */
  List<ILookupRow<VALUE>> callSubTreeLookup(VALUE parentKey);

  /**
   * Lookup rows of a parent key using {@link ILookupCall#getDataByRec()}. Blocks until the result is available.
   *
   * @return rows not <code>null</code>
   */
  List<ILookupRow<VALUE>> callSubTreeLookup(VALUE parentKey, TriState activeState);

  // non-blocking lookups

  IFuture<List<ILookupRow<VALUE>>> callKeyLookupInBackground(final VALUE key, boolean cancelRunningJobs);

  /**
   * Lookup rows asynchronously by text {@link ILookupCall#getDataByText()}.
   *
   * @param cancelRunningJobs
   *          if <code>true</code> it automatically cancels already running lookup jobs of this field, before starting
   *          the new lookup job.
   * @return {@link IFuture} to cancel data fetching.
   */
  IFuture<List<ILookupRow<VALUE>>> callTextLookupInBackground(String text, boolean cancelRunningJobs);

  /**
   * Lookup rows asynchronously by all {@link ILookupCall#getDataByAll()}. Automatically cancels already running lookup
   * jobs of this field, before starting the lookup job.
   *
   * @param cancelRunningJobs
   *          if <code>true</code> it automatically cancels already running lookup jobs of this field, before starting
   *          the new lookup job.
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<List<ILookupRow<VALUE>>> callBrowseLookupInBackground(boolean cancelRunningJobs);

  /**
   * Lookup rows asynchronously by all {@link ILookupCall#getDataByAll()}. Automatically cancels already running lookup
   * jobs of this field, before starting the lookup job.
   *
   * @param cancelRunningJobs
   *          if <code>true</code> it automatically cancels already running lookup jobs of this field, before starting
   *          the new lookup job.
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<List<ILookupRow<VALUE>>> callBrowseLookupInBackground(String browseHint, boolean cancelRunningJobs);

  /**
   * Lookup child rows of a given parent key asynchronously using {@link ILookupCall#getDataByRec()}.
   *
   * @param cancelRunningJobs
   *          if <code>true</code> it automatically cancels already running lookup jobs of this field, before starting
   *          the new lookup job.
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<List<ILookupRow<VALUE>>> callSubTreeLookupInBackground(final VALUE parentKey, boolean cancelRunningJobs);

  /**
   * Lookup child rows of a given parent key asynchronously using {@link ILookupCall#getDataByRec()}.
   *
   * @param cancelRunningJobs
   *          if <code>true</code> it automatically cancels already running lookup jobs of this field, before starting
   *          the new lookup job.
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<List<ILookupRow<VALUE>>> callSubTreeLookupInBackground(final VALUE parentKey, final TriState activeState, boolean cancelRunningJobs);

  // non-blocking lookups using callbacks (legacy)
  /**
   * Loads lookup rows asynchronously, and notifies the specified callback upon loading completed.
   * <p>
   * The methods of {@link ILookupRowFetchedCallback} are invoked in the model thread.
   *
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<Void> callKeyLookupInBackground(VALUE key, ILookupRowFetchedCallback<VALUE> callback);

  /**
   * Loads lookup rows asynchronously, and notifies the specified callback upon loading completed.
   * <p>
   * The methods of {@link ILookupRowFetchedCallback} are invoked in the model thread.
   *
   * @return {@link IFuture} to cancel data fetching.
   */
  IFuture<Void> callTextLookupInBackground(String text, int maxRowCount, ILookupRowFetchedCallback<VALUE> callback);

  /**
   * Loads lookup rows asynchronously, and notifies the specified callback upon loading completed.
   * <p>
   * The methods of {@link ILookupRowFetchedCallback} are invoked in the model thread.
   *
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<Void> callBrowseLookupInBackground(String browseHint, int maxRowCount, ILookupRowFetchedCallback<VALUE> callback);

  /**
   * Loads lookup rows asynchronously, and notifies the specified callback upon loading completed.
   * <p>
   * The methods of {@link ILookupRowFetchedCallback} are invoked in the model thread.
   *
   * @return {@link IFuture} to cancel data fetching
   */
  IFuture<Void> callBrowseLookupInBackground(String browseHint, int maxRowCount, TriState activeState, ILookupRowFetchedCallback<VALUE> callback);

}
