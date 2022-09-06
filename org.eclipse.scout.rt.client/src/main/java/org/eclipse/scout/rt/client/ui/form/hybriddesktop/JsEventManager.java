/*
 * Copyright (c) 2010-2022 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.client.ui.form.hybriddesktop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.scout.rt.client.ui.IWidget;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.dataobject.IDoEntity;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.Bean;
import org.eclipse.scout.rt.platform.reflect.AbstractPropertyObserver;
import org.eclipse.scout.rt.platform.util.ObjectUtility;

@Bean
public class JsEventManager extends AbstractPropertyObserver {

  public static final String PROP_WIDGETS = "widgets";
  public static final String PROP_ELEMENTS = "elements";

  private IJsEventManagerUIFacade m_uiFacade;
  private List<JsEventListener> m_listeners;

  private Map<String, IJsEventHandler> m_jsEventHandler = null;

  public JsEventManager() {
    m_uiFacade = createUIFacade();
    setElementsInternal(Map.of());
    setWidgetsInternal(Map.of());
  }

  // static helpers

  public static JsEventManager get() {
    return IDesktop.CURRENT.get().getAddOn(JsEventManager.class);
  }

  // general

  public void clear() {
    clearElements();
    clearWidgets();
  }

  // widgets

  public void clearWidgets() {
    setWidgetsInternal(Map.of());
  }

  public void addWidget(String id, IWidget widget) {
    addWidgets(Map.of(id, widget));
  }

  public void addWidgets(Map<String, IWidget> widgets) {
    Map<String, IWidget> result = new HashMap<>(getWidgets());
    widgets.forEach((id, widget) -> result.putIfAbsent(id, widget));
    setWidgetsInternal(result);
  }

  public void removeWidgetById(String id) {
    removeWidgetsById(List.of(id));
  }

  public void removeWidgetsById(Collection<String> ids) {
    Map<String, IWidget> result = new HashMap<>(getWidgets());
    ids.forEach(result::remove);
    setWidgetsInternal(result);
  }

  public void removeWidget(IWidget widget) {
    removeWidgets(List.of(widget));
  }

  public void removeWidgets(Collection<IWidget> widgets) {
    setWidgetsInternal(getWidgets().entrySet().stream()
        .filter(entry -> !widgets.contains(entry.getValue()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
  }

  protected void setWidgetsInternal(Map<String, IWidget> widgets) {
    propertySupport.setProperty(PROP_WIDGETS, widgets);
  }

  protected String getWidgetId(Object widget) {
    return getWidgets().entrySet().stream()
        .filter(entry -> ObjectUtility.equals(entry.getValue(), widget))
        .map(Entry::getKey)
        .findAny()
        .orElse(null);
  }

  public Object getWidgetById(String id) {
    return getWidgets().get(id);
  }

  public Map<String, IWidget> getWidgets() {
    return Collections.unmodifiableMap(getWidgetsInternal());
  }

  protected Map<String, IWidget> getWidgetsInternal() {
    //noinspection unchecked
    return (Map<String, IWidget>) propertySupport.getProperty(PROP_WIDGETS, Map.class);
  }

  // elements

  public void clearElements() {
    setElementsInternal(Map.of());
  }

  public void addElement(String id, Object element) {
    addElements(Map.of(id, element));
  }

  public void addElements(Map<String, Object> elements) {
    Map<String, Object> result = new HashMap<>(getElements());
    elements.forEach((id, element) -> result.putIfAbsent(id, element));
    setElementsInternal(result);
  }

  public void removeElementById(String id) {
    removeElementsById(List.of(id));
  }

  public void removeElementsById(Collection<String> ids) {
    Map<String, Object> result = new HashMap<>(getElements());
    ids.forEach(result::remove);
    setElementsInternal(result);
  }

  public void removeElement(Object element) {
    removeElements(List.of(element));
  }

  public void removeElements(Collection<Object> elements) {
    setElementsInternal(getElements().entrySet().stream()
        .filter(entry -> !elements.contains(entry.getValue()))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
  }

  protected void setElementsInternal(Map<String, Object> elements) {
    propertySupport.setProperty(PROP_ELEMENTS, elements);
  }

  protected String getElementId(Object element) {
    return getElements().entrySet().stream()
        .filter(entry -> ObjectUtility.equals(entry.getValue(), element))
        .map(Entry::getKey)
        .findAny()
        .orElse(null);
  }

  public Object getElementById(String id) {
    return getElements().get(id);
  }

  public Map<String, Object> getElements() {
    return Collections.unmodifiableMap(getElementsInternal());
  }

  protected Map<String, Object> getElementsInternal() {
    //noinspection unchecked
    return (Map<String, Object>) propertySupport.getProperty(PROP_ELEMENTS, Map.class);
  }

  // listeners

  public void addJsEventListener(JsEventListener listener) {
    if (m_listeners == null) {
      m_listeners = new ArrayList<>();
    }
    m_listeners.add(listener);
  }

  public void removeJsEventListener(JsEventListener listener) {
    if (m_listeners != null) {
      m_listeners.remove(listener);
    }
  }

  // event support

  protected void fireJsEvent(JsEvent event) {
    if (m_listeners != null) {
      m_listeners.forEach(listener -> listener.jsEvent(event));
    }
  }

  private void jsEvent(String id, String eventType, IDoEntity data) {
    if (m_jsEventHandler == null) {
      m_jsEventHandler = BEANS.all(IJsEventHandler.class).stream()
          .collect(Collectors.toMap(IJsEventHandler::getEventType, Function.identity()));
    }
    Optional.ofNullable(m_jsEventHandler.get(eventType))
        .ifPresent(jsEventHandler -> jsEventHandler.handleJsEvent(id, data));
  }

  public void jsEventEnd(String id) {
    jsEventEnd(id, null);
  }

  public void jsEventEnd(String id, IDoEntity data) {
    JsEvent e =
        new JsEvent(this, JsEvent.TYPE_JS_EVENT_END, id, data);
    fireJsEvent(e);
  }

  public void customEvent(String customEventType, String id) {
    customEvent(customEventType, id, null);
  }

  public void customEvent(String customEventType, String id, IDoEntity data) {
    JsEvent e =
        new JsEvent(this, JsEvent.TYPE_JS_EVENT_CUSTOM, customEventType, id, data);
    fireJsEvent(e);
  }

  public IJsEventManagerUIFacade getUIFacade() {
    return m_uiFacade;
  }

  protected P_UIFacade createUIFacade() {
    return new P_UIFacade();
  }

  protected class P_UIFacade implements IJsEventManagerUIFacade {

    @Override
    public void jsEventFromUI(String id, String eventType, IDoEntity data) {
      jsEvent(id, eventType, data);
    }
  }
}
