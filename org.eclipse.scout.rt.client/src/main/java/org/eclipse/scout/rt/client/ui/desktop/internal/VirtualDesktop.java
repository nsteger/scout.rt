/*
 * Copyright (c) 2010-2023 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.client.ui.desktop.internal;

import java.beans.PropertyChangeListener;
import java.security.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.client.job.ModelJobs.WrongThreadException;
import org.eclipse.scout.rt.client.ui.Coordinates;
import org.eclipse.scout.rt.client.ui.IDisplayParent;
import org.eclipse.scout.rt.client.ui.IEventHistory;
import org.eclipse.scout.rt.client.ui.IWidget;
import org.eclipse.scout.rt.client.ui.ScrollOptions;
import org.eclipse.scout.rt.client.ui.WidgetListeners;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.action.menu.root.IContextMenu;
import org.eclipse.scout.rt.client.ui.action.view.IViewButton;
import org.eclipse.scout.rt.client.ui.basic.filechooser.IFileChooser;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.BrowserHistoryEntry;
import org.eclipse.scout.rt.client.ui.desktop.DesktopEvent;
import org.eclipse.scout.rt.client.ui.desktop.DesktopListeners;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.IDesktopUIFacade;
import org.eclipse.scout.rt.client.ui.desktop.IOpenUriAction;
import org.eclipse.scout.rt.client.ui.desktop.bench.layout.BenchLayoutData;
import org.eclipse.scout.rt.client.ui.desktop.datachange.DataChangeEvent;
import org.eclipse.scout.rt.client.ui.desktop.datachange.IDataChangeManager;
import org.eclipse.scout.rt.client.ui.desktop.notification.IDesktopNotification;
import org.eclipse.scout.rt.client.ui.desktop.notification.NativeNotificationDefaults;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.IFormHandler;
import org.eclipse.scout.rt.client.ui.messagebox.IMessageBox;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.classid.ClassId;
import org.eclipse.scout.rt.platform.context.PropertyMap;
import org.eclipse.scout.rt.platform.reflect.BasicPropertySupport;
import org.eclipse.scout.rt.platform.resource.BinaryResource;
import org.eclipse.scout.rt.platform.util.visitor.IBreadthFirstTreeVisitor;
import org.eclipse.scout.rt.platform.util.visitor.IDepthFirstTreeVisitor;
import org.eclipse.scout.rt.platform.util.visitor.TreeVisitResult;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;

/**
 * This class is used as a placeholder of virtual desktop while the desktop is loading until the desktop is set onto the
 * {@link IClientSession}. Reasons for that are observer attachments and data change registration in the init block of
 * forms, fields, pages that must be done while the desktop is loading. This pattern solves the bird/egg problem in
 * initialization of an object with self-references.
 */
@ClassId("3184965f-010b-4d1b-9c08-d5cd35eedf2b")
public class VirtualDesktop implements IDesktop {

  private DesktopListeners m_listeners;
  private BasicPropertySupport m_propertyChangeListeners;
  private IDataChangeManager m_dataChangeListeners;
  private IDataChangeManager m_dataChangeDesktopInForegroundListeners;
  private WidgetListeners m_widgetListeners;
  private IDesktop m_realDesktop; // holds the "real" Desktop set to the ClientSession as soon as available.

  public VirtualDesktop() {
    m_listeners = new DesktopListeners();
    m_propertyChangeListeners = new BasicPropertySupport(this);
    m_dataChangeListeners = BEANS.get(IDataChangeManager.class);
    m_dataChangeDesktopInForegroundListeners = BEANS.get(IDataChangeManager.class);
    m_widgetListeners = new WidgetListeners();
  }

  /**
   * After calling this function this {@link VirtualDesktop} only acts as proxy for the given real one. Listeners
   * already registered to this {@link VirtualDesktop} are transferred and future listeners will be added to the real
   * desktop directly.
   *
   * @param realDesktop
   *          Must not be {@code null} and must not be a {@link VirtualDesktop}.
   * @throws IllegalArgumentException
   *           if the realDesktop is null or a {@link VirtualDesktop}.
   * @throws IllegalStateException
   *           if a different real desktop has already been set
   * @throws WrongThreadException
   *           if this method is not called from a model thread.
   */
  public void setRealDesktop(IDesktop realDesktop) {
    if (realDesktop == null) {
      throw new IllegalArgumentException("Real desktop must not be null");
    }
    if (realDesktop instanceof VirtualDesktop) {
      throw new IllegalArgumentException("Real desktop must not be virtual");
    }
    if (m_realDesktop == realDesktop) {
      return; // already the correct value
    }
    if (m_realDesktop != null) {
      throw new IllegalStateException("real desktop has already been set");
    }
    ModelJobs.assertModelThread();

    // copy listeners over to real desktop
    realDesktop.desktopListeners().addAll(desktopListeners());
    m_listeners = null;

    m_propertyChangeListeners.getPropertyChangeListeners().forEach(l -> realDesktop.addPropertyChangeListener(l));
    m_propertyChangeListeners.getSpecificPropertyChangeListeners()
        .forEach((propName, listeners) -> listeners
            .forEach(listener -> realDesktop.addPropertyChangeListener(propName, listener)));
    m_propertyChangeListeners = null;

    realDesktop.dataChangeListeners().addAll(dataChangeListeners());
    m_dataChangeListeners = null;

    realDesktop.dataChangeDesktopInForegroundListeners().addAll(dataChangeDesktopInForegroundListeners());
    m_dataChangeDesktopInForegroundListeners = null;

    realDesktop.widgetListeners().addAll(widgetListeners());
    m_widgetListeners = null;

    // assign real desktop so that from now on calls to this virtual instance are delegated to the real one
    m_realDesktop = realDesktop;
  }

  public Optional<IDesktop> getRealDesktop() {
    return Optional.ofNullable(m_realDesktop);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    forwardToRealDesktopOrElse(d -> d.addPropertyChangeListener(listener), () -> m_propertyChangeListeners.addPropertyChangeListener(listener));
  }

  @Override
  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    forwardToRealDesktopOrElse(d -> d.addPropertyChangeListener(propertyName, listener), () -> m_propertyChangeListeners.addPropertyChangeListener(propertyName, listener));
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    forwardToRealDesktopOrElse(d -> d.removePropertyChangeListener(listener), () -> m_propertyChangeListeners.removePropertyChangeListener(listener));
  }

  @Override
  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    forwardToRealDesktopOrElse(d -> d.removePropertyChangeListener(propertyName, listener), () -> m_propertyChangeListeners.removePropertyChangeListener(propertyName, listener));
  }

  @Override
  public List<PropertyChangeListener> getPropertyChangeListeners() {
    return getFromRealDesktopOrElseGet(d -> d.getPropertyChangeListeners(), () -> m_propertyChangeListeners.getPropertyChangeListeners());
  }

  @Override
  public Map<String, List<PropertyChangeListener>> getSpecificPropertyChangeListeners() {
    return getFromRealDesktopOrElseGet(d -> d.getSpecificPropertyChangeListeners(), () -> m_propertyChangeListeners.getSpecificPropertyChangeListeners());
  }

  @Override
  public DesktopListeners desktopListeners() {
    return getFromRealDesktopOrElse(d -> d.desktopListeners(), m_listeners);
  }

  @Override
  public IDataChangeManager dataChangeListeners() {
    return getFromRealDesktopOrElse(d -> d.dataChangeListeners(), m_dataChangeListeners);
  }

  @Override
  public IDataChangeManager dataChangeDesktopInForegroundListeners() {
    return getFromRealDesktopOrElse(d -> d.dataChangeDesktopInForegroundListeners(), m_dataChangeDesktopInForegroundListeners);
  }

  @Override
  public WidgetListeners widgetListeners() {
    return getFromRealDesktopOrElse(d -> d.widgetListeners(), m_widgetListeners);
  }

  public void forwardToRealDesktopOrThrow(Consumer<IDesktop> realDesktopTask) {
    getFromRealDesktopOrThrow(d -> {
      realDesktopTask.accept(d);
      return Boolean.TRUE;
    });
  }

  public <T> T getFromRealDesktopOrThrow(Function<IDesktop, T> realDesktopTask) {
    IDesktop realDesktop = getRealDesktop()
        .orElseThrow(() -> new UnsupportedOperationException("The desktop is currently loading. This method must be called after the desktop has loaded and is set onto the session"));
    return realDesktopTask.apply(realDesktop);
  }

  public void forwardToRealDesktopIfAvailable(Consumer<IDesktop> realDesktopTask) {
    getFromRealDesktopOrElse(d -> {
      realDesktopTask.accept(d);
      return Boolean.TRUE;
    }, null);
  }

  public <T> T getFromRealDesktopOrElse(Function<IDesktop, T> realDesktopTask, T orElse) {
    return getFromRealDesktopOrElseGet(realDesktopTask, () -> orElse);
  }

  public void forwardToRealDesktopOrElse(Consumer<IDesktop> realDesktopTask, Runnable virtualDesktopTask) {
    getFromRealDesktopOrElseGet(d -> {
      realDesktopTask.accept(d);
      return Boolean.TRUE;
    }, () -> {
      virtualDesktopTask.run();
      return Boolean.TRUE;
    });
  }

  public <T> T getFromRealDesktopOrElseGet(Function<IDesktop, T> realDesktopTask, Supplier<? extends T> orElse) {
    return getRealDesktop()
        .map(realDesktopTask)
        .orElseGet(orElse);
  }

  /*
   * Not implemented methods (forward to real desktop if available)
   */

  @Override
  public boolean isAutoPrefixWildcardForTextSearch() {
    return getFromRealDesktopOrThrow(d -> d.isAutoPrefixWildcardForTextSearch());
  }

  @Override
  public void setAutoPrefixWildcardForTextSearch(boolean b) {
    forwardToRealDesktopOrThrow(d -> d.setAutoPrefixWildcardForTextSearch(b));
  }

  @Override
  public boolean isSelectViewTabsKeyStrokesEnabled() {
    return getFromRealDesktopOrThrow(IDesktop::isSelectViewTabsKeyStrokesEnabled);
  }

  @Override
  public void setSelectViewTabsKeyStrokesEnabled(boolean selectViewTabsKeyStrokesEnabled) {
    forwardToRealDesktopOrThrow(d -> d.setSelectViewTabsKeyStrokesEnabled(selectViewTabsKeyStrokesEnabled));
  }

  @Override
  public String getSelectViewTabsKeyStrokeModifier() {
    return getFromRealDesktopOrThrow(d -> d.getSelectViewTabsKeyStrokeModifier());
  }

  @Override
  public void setSelectViewTabsKeyStrokeModifier(String selectViewTabsKeyStrokeModifier) {
    forwardToRealDesktopOrThrow(d -> d.setSelectViewTabsKeyStrokeModifier(selectViewTabsKeyStrokeModifier));
  }

  @Override
  public void doLogoAction() {
    forwardToRealDesktopOrThrow(d -> d.doLogoAction());
  }

  @Override
  public void activateBookmark(Bookmark bm) {
    forwardToRealDesktopOrThrow(d -> d.activateBookmark(bm));
  }

  @Override
  public void activateBookmark(Bookmark bm, boolean activateOutline) {
    forwardToRealDesktopOrThrow(d -> d.activateBookmark(bm, activateOutline));
  }

  @Override
  public boolean isShowing(IFileChooser fileChooser) {
    return getFromRealDesktopOrThrow(d -> d.isShowing(fileChooser));
  }

  @Override
  public List<IFileChooser> getFileChoosers() {
    return getFromRealDesktopOrThrow(d -> d.getFileChoosers());
  }

  @Override
  public List<IFileChooser> getFileChoosers(IDisplayParent displayParent) {
    return getFromRealDesktopOrThrow(d -> d.getFileChoosers(displayParent));
  }

  @Override
  public void showFileChooser(IFileChooser fileChooser) {
    forwardToRealDesktopOrThrow(d -> d.showFileChooser(fileChooser));
  }

  @Override
  public void hideFileChooser(IFileChooser fileChooser) {
    forwardToRealDesktopOrThrow(d -> d.hideFileChooser(fileChooser));
  }

  @Override
  public void openUri(String url, IOpenUriAction openUriAction) {
    forwardToRealDesktopOrThrow(d -> d.openUri(url, openUriAction));
  }

  @Override
  public void openUri(BinaryResource binaryResource) {
    forwardToRealDesktopOrThrow(d -> d.openUri(binaryResource));
  }

  @Override
  public void openUri(BinaryResource binaryResource, IOpenUriAction openUriAction) {
    forwardToRealDesktopOrThrow(d -> d.openUri(binaryResource, openUriAction));
  }

  @Override
  public void showForm(IForm form) {
    forwardToRealDesktopOrThrow(d -> d.showForm(form));
  }

  @Override
  public void hideForm(IForm form) {
    forwardToRealDesktopOrThrow(d -> d.hideForm(form));
  }

  @Override
  public void addKeyStrokes(IKeyStroke... keyStrokes) {
    forwardToRealDesktopOrThrow(d -> d.addKeyStrokes(keyStrokes));
  }

  @Override
  public void showMessageBox(IMessageBox messageBox) {
    forwardToRealDesktopOrThrow(d -> d.showMessageBox(messageBox));
  }

  @Override
  public void hideMessageBox(IMessageBox messageBox) {
    forwardToRealDesktopOrThrow(d -> d.hideMessageBox(messageBox));
  }

  @Override
  public void closeInternal() {
    forwardToRealDesktopOrThrow(d -> d.closeInternal());
  }

  @Override
  public Bookmark createBookmark() {
    return getFromRealDesktopOrThrow(d -> d.createBookmark());
  }

  @Override
  public Bookmark createBookmark(IPage<?> page) {
    return getFromRealDesktopOrThrow(d -> d.createBookmark(page));
  }

  @Override
  public void dataChanged(Object... dataTypes) {
    forwardToRealDesktopOrThrow(d -> d.dataChanged(dataTypes));
  }

  @Override
  public void fireDataChangeEvent(DataChangeEvent event) {
    forwardToRealDesktopOrThrow(d -> d.fireDataChangeEvent(event));
  }

  @Override
  public void setDataChanging(boolean b) {
    forwardToRealDesktopOrThrow(d -> d.setDataChanging(b));
  }

  @Override
  public boolean isDataChanging() {
    return getFromRealDesktopOrThrow(d -> d.isDataChanging());
  }

  @Override
  public void ensureViewStackVisible() {
    forwardToRealDesktopOrThrow(d -> d.ensureViewStackVisible());
  }

  @Override
  public void activateForm(IForm form) {
    forwardToRealDesktopOrThrow(d -> d.activateForm(form));
  }

  @Override
  public void activateOutline(IOutline outline) {
    forwardToRealDesktopOrThrow(d -> d.activateOutline(outline));
  }

  @Override
  public void activateFirstPage() {
    forwardToRealDesktopOrThrow(d -> d.activateFirstPage());
  }

  @Override
  public <T extends IForm> T findForm(Class<T> formType) {
    return getFromRealDesktopOrThrow(d -> d.findForm(formType));
  }

  @Override
  public <T extends IForm> List<T> findForms(Class<T> formType) {
    return getFromRealDesktopOrThrow(d -> d.findForms(formType));
  }

  @Override
  public <T extends IOutline> T findOutline(Class<T> outlineType) {
    return getFromRealDesktopOrThrow(d -> d.findOutline(outlineType));
  }

  @Override
  public <T extends IAction> T findAction(Class<T> actionType) {
    return getFromRealDesktopOrThrow(d -> d.findAction(actionType));
  }

  @Override
  public <T extends IViewButton> T findViewButton(Class<T> viewButtonType) {
    return getFromRealDesktopOrThrow(d -> d.findViewButton(viewButtonType));
  }

  @Override
  public List<IOutline> getAvailableOutlines() {
    return getFromRealDesktopOrThrow(d -> d.getAvailableOutlines());
  }

  @Override
  public void setAvailableOutlines(List<? extends IOutline> availableOutlines) {
    forwardToRealDesktopOrThrow(d -> d.setAvailableOutlines(availableOutlines));
  }

  @Override
  public List<IForm> getDialogs() {
    return getFromRealDesktopOrThrow(d -> d.getDialogs());
  }

  @Override
  public List<IForm> getDialogs(IDisplayParent displayParent, boolean includeChildDialogs) {
    return getFromRealDesktopOrThrow(d -> d.getDialogs(displayParent, includeChildDialogs));
  }

  @Override
  public Set<IKeyStroke> getKeyStrokes() {
    return getFromRealDesktopOrThrow(d -> d.getKeyStrokes());
  }

  @Override
  public List<IMenu> getMenus() {
    return getFromRealDesktopOrThrow(d -> d.getMenus());
  }

  @Override
  public <T extends IMenu> T getMenuByClass(Class<T> menuType) {
    return getFromRealDesktopOrThrow(d -> d.getMenuByClass(menuType));
  }

  @Override
  public IContextMenu getContextMenu() {
    return getFromRealDesktopOrThrow(d -> d.getContextMenu());
  }

  @Override
  public boolean isShowing(IMessageBox messageBox) {
    return getFromRealDesktopOrThrow(d -> d.isShowing(messageBox));
  }

  @Override
  public List<IMessageBox> getMessageBoxes() {
    return getFromRealDesktopOrThrow(d -> d.getMessageBoxes());
  }

  @Override
  public List<IDesktopNotification> getNotifications() {
    return getFromRealDesktopOrThrow(d -> d.getNotifications());
  }

  @Override
  public List<IMessageBox> getMessageBoxes(IDisplayParent displayParent) {
    return getFromRealDesktopOrThrow(d -> d.getMessageBoxes(displayParent));
  }

  @Override
  public IOutline getOutline() {
    return getFromRealDesktopOrThrow(d -> d.getOutline());
  }

  @Override
  public IForm getPageSearchForm() {
    return getFromRealDesktopOrThrow(d -> d.getPageSearchForm());
  }

  @Override
  public void setPageSearchForm(IForm f) {
    forwardToRealDesktopOrThrow(d -> d.setPageSearchForm(f));
  }

  @Override
  public IForm getPageDetailForm() {
    return getFromRealDesktopOrThrow(d -> d.getPageDetailForm());
  }

  @Override
  public void setPageDetailForm(IForm f) {
    forwardToRealDesktopOrThrow(d -> d.setPageDetailForm(f));
  }

  @Override
  public ITable getPageDetailTable() {
    return getFromRealDesktopOrThrow(d -> d.getPageDetailTable());
  }

  @Override
  public void setPageDetailTable(ITable t) {
    forwardToRealDesktopOrThrow(d -> d.setPageDetailTable(t));
  }

  @Override
  public List<IForm> getSimilarForms(IForm form) {
    return getFromRealDesktopOrThrow(d -> d.getSimilarForms(form));
  }

  @Override
  public String getTitle() {
    return getFromRealDesktopOrThrow(d -> d.getTitle());
  }

  @Override
  public List<IAction> getActions() {
    return getFromRealDesktopOrThrow(d -> d.getActions());
  }

  @Override
  public <T extends IViewButton> T getViewButton(Class<? extends T> searchType) {
    return getFromRealDesktopOrThrow(d -> d.getViewButton(searchType));
  }

  @Override
  public List<IViewButton> getViewButtons() {
    return getFromRealDesktopOrThrow(d -> d.getViewButtons());
  }

  @Override
  public IDesktopUIFacade getUIFacade() {
    return getFromRealDesktopOrThrow(d -> d.getUIFacade());
  }

  @Override
  public boolean isShowing(IForm form) {
    return getFromRealDesktopOrThrow(d -> d.isShowing(form));
  }

  @Override
  public List<IForm> getForms(IDisplayParent displayParent) {
    return getFromRealDesktopOrThrow(d -> d.getForms(displayParent));
  }

  @Override
  public List<IForm> getViews() {
    return getFromRealDesktopOrThrow(d -> d.getViews());
  }

  @Override
  public Collection<IForm> getSelectedViews(IDisplayParent displayParent) {
    return getFromRealDesktopOrThrow(d -> d.getSelectedViews(displayParent));
  }

  @Override
  public <F extends IForm, H extends IFormHandler> List<F> findAllOpenViews(Class<? extends F> formClass, Class<? extends H> handlerClass, Object exclusiveKey) {
    return getFromRealDesktopOrThrow(d -> d.findAllOpenViews(formClass, handlerClass, exclusiveKey));
  }

  @Override
  public <F extends IForm, H extends IFormHandler> F findOpenView(Class<? extends F> formClass, Class<? extends H> handlerClass, Object exclusiveKey) {
    return getFromRealDesktopOrThrow(d -> d.findOpenView(formClass, handlerClass, exclusiveKey));
  }

  @Override
  public List<IForm> getViews(IDisplayParent displayParent) {
    return getFromRealDesktopOrThrow(d -> d.getViews(displayParent));
  }

  @Override
  public List<? extends IWidget> getChildren() {
    return getFromRealDesktopOrThrow(d -> d.getChildren());
  }

  @Override
  public void visit(Consumer<IWidget> visitor) {
    forwardToRealDesktopOrThrow(d -> d.visit(visitor));
  }

  @Override
  public <T extends IWidget> void visit(Consumer<T> visitor, Class<T> type) {
    forwardToRealDesktopOrThrow(d -> d.visit(visitor, type));
  }

  @Override
  public TreeVisitResult visit(Function<IWidget, TreeVisitResult> visitor) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor));
  }

  @Override
  public <T extends IWidget> TreeVisitResult visit(Function<T, TreeVisitResult> visitor, Class<T> type) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor, type));
  }

  @Override
  public TreeVisitResult visit(IDepthFirstTreeVisitor<IWidget> visitor) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor));
  }

  @Override
  public <T extends IWidget> TreeVisitResult visit(IDepthFirstTreeVisitor<T> visitor, Class<T> type) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor, type));
  }

  @Override
  public <T extends IWidget> TreeVisitResult visit(IBreadthFirstTreeVisitor<T> visitor, Class<T> type) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor, type));
  }

  @Override
  public TreeVisitResult visit(IBreadthFirstTreeVisitor<IWidget> visitor) {
    return getFromRealDesktopOrThrow(d -> d.visit(visitor));
  }

  @Override
  public void init() {
    forwardToRealDesktopOrThrow(d -> d.init());
  }

  @Override
  public void reinit() {
    forwardToRealDesktopOrThrow(d -> d.reinit());
  }

  @Override
  public boolean isInitConfigDone() {
    return getFromRealDesktopOrThrow(d -> d.isInitConfigDone());
  }

  @Override
  public boolean isInitDone() {
    return getFromRealDesktopOrThrow(d -> d.isInitDone());
  }

  @Override
  public boolean isDisposeDone() {
    return getFromRealDesktopOrThrow(d -> d.isDisposeDone());
  }

  @Override
  public void dispose() {
    forwardToRealDesktopOrThrow(d -> d.dispose());
  }

  @Override
  public boolean isOpened() {
    return getFromRealDesktopOrThrow(d -> d.isOpened());
  }

  @Override
  public boolean isGuiAvailable() {
    return getFromRealDesktopOrThrow(d -> d.isGuiAvailable());
  }

  @Override
  public void refreshPages(List<Class<? extends IPage<?>>> pages) {
    forwardToRealDesktopOrThrow(d -> d.refreshPages(pages));
  }

  @Override
  public void refreshPages(Class... pageTypes) {
    forwardToRealDesktopOrThrow(d -> d.refreshPages(pageTypes));
  }

  @Override
  public void releaseUnusedPages() {
    forwardToRealDesktopOrThrow(d -> d.releaseUnusedPages());
  }

  @Override
  public void afterTablePageLoaded(IPageWithTable<?> page) {
    forwardToRealDesktopOrThrow(d -> d.afterTablePageLoaded(page));
  }

  @Override
  public void removeKeyStrokes(IKeyStroke... keyStrokes) {
    forwardToRealDesktopOrThrow(d -> d.removeKeyStrokes(keyStrokes));
  }

  @Override
  public void setKeyStrokes(Collection<? extends IKeyStroke> ks) {
    forwardToRealDesktopOrThrow(d -> d.setKeyStrokes(ks));
  }

  @Override
  public void setOutline(Class<? extends IOutline> outlineType) {
    forwardToRealDesktopOrThrow(d -> d.setOutline(outlineType));
  }

  @Override
  public void addNotification(IDesktopNotification notification) {
    // desktop notifications are silently ignored
  }

  @Override
  public void removeNotification(IDesktopNotification notification) {
    // desktop notifications are silently ignored
  }

  @Override
  public void reloadGui() {
    forwardToRealDesktopOrThrow(d -> d.reloadGui());
  }

  @Override
  public void setTitle(String s) {
    forwardToRealDesktopOrThrow(d -> d.setTitle(s));
  }

  @Override
  public String getCssClass() {
    return getFromRealDesktopOrThrow(d -> d.getCssClass());
  }

  @Override
  public void setCssClass(String cssClass) {
    forwardToRealDesktopOrThrow(d -> d.setCssClass(cssClass));
  }

  @Override
  public boolean has(IWidget child) {
    return false;
  }

  @Override
  public boolean doBeforeClosingInternal() {
    return getFromRealDesktopOrThrow(d -> d.doBeforeClosingInternal());
  }

  @Override
  public List<IForm> getUnsavedForms() {
    return getFromRealDesktopOrThrow(d -> d.getUnsavedForms());
  }

  @Override
  public IForm getActiveForm() {
    return getFromRealDesktopOrThrow(d -> d.getActiveForm());
  }

  @Override
  public IWidget getFocusedElement() {
    return getFromRealDesktopOrThrow(d -> d.getFocusedElement());
  }

  @Override
  public void setTrackFocus(boolean trackFocus) {
  }

  @Override
  public boolean isTrackFocus() {
    return false;
  }

  @Override
  public void addAddOn(Object addOn) {
    forwardToRealDesktopOrThrow(d -> d.addAddOn(addOn));
  }

  @Override
  public void removeAddOn(Object addOn) {
    forwardToRealDesktopOrThrow(d -> d.removeAddOn(addOn));
  }

  @Override
  public Collection<Object> getAddOns() {
    return getFromRealDesktopOrThrow(d -> d.getAddOns());
  }

  @Override
  public <T> T getAddOn(Class<T> addOnClass) {
    return getFromRealDesktopOrThrow(d -> d.getAddOn(addOnClass));
  }

  @Override
  public boolean isOutlineChanging() {
    return getFromRealDesktopOrThrow(d -> d.isOutlineChanging());
  }

  @Override
  public String getDisplayStyle() {
    return DISPLAY_STYLE_DEFAULT;
  }

  @Override
  public void setDisplayStyle(String displayStyle) {
    // always DISPLAY_STYLE_DEFAULT is used
  }

  @Override
  public String getLogoId() {
    return null;
  }

  @Override
  public void setLogoId(String id) {
    // NOP
  }

  @Override
  public boolean isCacheSplitterPosition() {
    return false;
  }

  @Override
  public void setCacheSplitterPosition(boolean b) {
    // NOP
  }

  @Override
  public String getTheme() {
    return null;
  }

  @Override
  public void setTheme(String theme) {
    // NOP
  }

  @Override
  public NativeNotificationDefaults getNativeNotificationDefaults() {
    return null;
  }

  @Override
  public void reloadPageFromRoot(IPage<?> page) {
    // NOP
  }

  @Override
  public void setNativeNotificationDefaults(NativeNotificationDefaults nativeNotificationDefaults) {
    // NOP
  }

  @Override
  public BrowserHistoryEntry getBrowserHistoryEntry() {
    return null;
  }

  @Override
  public void setBrowserHistoryEntry(BrowserHistoryEntry browserHistory) {
    // NOP
  }

  @Override
  public void setNavigationVisible(boolean visible) {
    // NOP
  }

  @Override
  public boolean isNavigationVisible() {
    return false;
  }

  @Override
  public void setBenchVisible(boolean visible) {
    // NOP
  }

  @Override
  public boolean isBenchVisible() {
    return false;
  }

  @Override
  public BenchLayoutData getBenchLayoutData() {
    return null;
  }

  @Override
  public void setBenchLayoutData(BenchLayoutData layoutData) {
    // NOP
  }

  @Override
  public void setHeaderVisible(boolean visible) {
    // NOP
  }

  @Override
  public boolean isHeaderVisible() {
    return false;
  }

  @Override
  public void setNavigationHandleVisible(boolean visible) {
    // NOP
  }

  @Override
  public boolean isNavigationHandleVisible() {
    return false;
  }

  @Override
  public boolean isInBackground() {
    return false;
  }

  @Override
  public IEventHistory<DesktopEvent> getEventHistory() {
    return null;
  }

  @Override
  public boolean isGeolocationServiceAvailable() {
    return false;
  }

  @Override
  public void setGeolocationServiceAvailable(boolean available) {
    // NOP
  }

  @Override
  public PropertyMap getStartupRequestParams() {
    return null;
  }

  @Override
  public String getStartupUrl() {
    return null;
  }

  @Override
  public <VALUE> VALUE getStartupRequestParam(String propertyName) {
    return null;
  }

  @Override
  public Future<Coordinates> requestGeolocation() {
    return getFromRealDesktopOrThrow(d -> d.requestGeolocation());
  }

  @Override
  public void setLogoActionEnabled(boolean logoActionEnabled) {
    // NOP
  }

  @Override
  public boolean isLogoActionEnabled() {
    return false;
  }

  @Override
  public boolean cancelForms(Set<IForm> formSet) {
    return getFromRealDesktopOrThrow(d -> d.cancelForms(formSet));
  }

  @Override
  public boolean cancelForms(Set<IForm> formSet, boolean alwaysShowUnsavedChangesForm) {
    return getFromRealDesktopOrThrow(d -> d.cancelForms(formSet, alwaysShowUnsavedChangesForm));
  }

  @Override
  public void closeForms(Set<IForm> formSet) {
    // NOP
  }

  @Override
  public Object getProperty(String name) {
    return null;
  }

  @Override
  public void setProperty(String name, Object value) {
    // NOP
  }

  @Override
  public boolean hasProperty(String name) {
    return false;
  }

  @Override
  public <T extends IWidget> T getWidgetByClass(Class<T> widgetClassToFind) {
    return getFromRealDesktopOrThrow(d -> d.getWidgetByClass(widgetClassToFind));
  }

  @Override
  public void setDense(boolean dense) {
    // NOP
  }

  @Override
  public boolean isDense() {
    return false;
  }

  @Override
  public boolean isLoading() {
    return false;
  }

  @Override
  public void setLoading(boolean loading) {
    // NOP
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public void setEnabled(boolean enabled) {
    // NOP
  }

  @Override
  public void setEnabledPermission(Permission permission) {
    // NOP
  }

  @Override
  public boolean isEnabledGranted() {
    return false;
  }

  @Override
  public void setEnabledGranted(boolean enabledGranted) {
    // NOP
  }

  @Override
  public void setEnabled(boolean enabled, String dimension) {
    // NOP
  }

  @Override
  public boolean isEnabled(String dimension) {
    return false;
  }

  @Override
  public void setEnabled(boolean enabled, boolean updateParents) {
    // NOP
  }

  @Override
  public void setEnabled(boolean enabled, boolean updateParents, boolean updateChildren) {
    // NOP
  }

  @Override
  public void setEnabledGranted(boolean enabled, boolean updateParents) {
    // NOP
  }

  @Override
  public void setEnabledGranted(boolean enabled, boolean updateParents, boolean updateChildren) {
    // NOP
  }

  @Override
  public boolean isEnabled(Predicate<String> filter) {
    return false;
  }

  @Override
  public void setEnabled(boolean enabled, boolean updateParents, String dimension) {
    // NOP
  }

  @Override
  public void setEnabled(boolean enabled, boolean updateParents, boolean updateChildren, String dimension) {
    // NOP
  }

  @Override
  public IWidget getParent() {
    return null;
  }

  @Override
  public boolean setParentInternal(IWidget w) {
    return false;
  }

  @Override
  public boolean isEnabledIncludingParents() {
    return false;
  }

  @Override
  public boolean visitParents(Consumer<IWidget> visitor) {
    return false;
  }

  @Override
  public <T extends IWidget> boolean visitParents(Consumer<T> visitor, Class<T> typeFilter) {
    return false;
  }

  @Override
  public boolean visitParents(Predicate<IWidget> visitor) {
    return false;
  }

  @Override
  public <T extends IWidget> boolean visitParents(Predicate<T> visitor, Class<T> typeFilter) {
    return false;
  }

  @Override
  public <T extends IWidget> T getParentOfType(Class<T> type) {
    return null;
  }

  @Override
  public String classId() {
    return null;
  }

  @Override
  public boolean isInheritAccessibility() {
    return false;
  }

  @Override
  public void setInheritAccessibility(boolean inheritAccessibility) {
    // NOP
  }

  @Override
  public void scrollToTop() {
    // NOP
  }

  @Override
  public void scrollToTop(ScrollOptions options) {
    // NOP
  }

  @Override
  public void reveal() {
    // NOP
  }

  @Override
  public void reveal(ScrollOptions options) {
    // NOP
  }
}
