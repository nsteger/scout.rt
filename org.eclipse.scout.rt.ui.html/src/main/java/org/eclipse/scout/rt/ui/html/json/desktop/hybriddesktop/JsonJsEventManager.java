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
package org.eclipse.scout.rt.ui.html.json.desktop.hybriddesktop;

import java.util.Map;

import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.client.ui.IWidget;
import org.eclipse.scout.rt.client.ui.form.hybriddesktop.JsEvent;
import org.eclipse.scout.rt.client.ui.form.hybriddesktop.JsEventListener;
import org.eclipse.scout.rt.client.ui.form.hybriddesktop.JsEventManager;
import org.eclipse.scout.rt.dataobject.IDoEntity;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.ui.html.IUiSession;
import org.eclipse.scout.rt.ui.html.json.AbstractJsonPropertyObserver;
import org.eclipse.scout.rt.ui.html.json.IJsonAdapter;
import org.eclipse.scout.rt.ui.html.json.IJsonObject;
import org.eclipse.scout.rt.ui.html.json.IJsonObjectWithParentAdapter;
import org.eclipse.scout.rt.ui.html.json.IJsonObjectWithUiSession;
import org.eclipse.scout.rt.ui.html.json.JsonAdapterUtility;
import org.eclipse.scout.rt.ui.html.json.JsonDataObjectHelper;
import org.eclipse.scout.rt.ui.html.json.JsonEvent;
import org.eclipse.scout.rt.ui.html.json.JsonProperty;
import org.eclipse.scout.rt.ui.html.json.MainJsonObjectFactory;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterProperty;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterPropertyConfig;
import org.eclipse.scout.rt.ui.html.json.form.fields.JsonAdapterPropertyConfigBuilder;
import org.json.JSONObject;

public class JsonJsEventManager<T extends JsEventManager> extends AbstractJsonPropertyObserver<T> {

  public static final String EVENT_JS_EVENT = "jsEvent";
  public static final String EVENT_JS_EVENT_END = "jsEventEnd";
  public static final String EVENT_JS_EVENT_CUSTOM = "jsEventCustom";

  private JsEventListener m_listener;

  private final JsonDataObjectHelper m_jsonDoHelper = BEANS.get(JsonDataObjectHelper.class); // cached instance

  public JsonJsEventManager(T model, IUiSession uiSession, String id, IJsonAdapter<?> parent) {
    super(model, uiSession, id, parent);
  }

  @Override
  public String getObjectType() {
    return "scout.JsEventManager";
  }

  protected JsonDataObjectHelper jsonDoHelper() {
    return m_jsonDoHelper;
  }

  @Override
  protected void attachModel() {
    super.attachModel();
    if (m_listener != null) {
      throw new IllegalStateException();
    }
    m_listener = new P_JsEventListener();
    getModel().addJsEventListener(m_listener);
  }

  @Override
  protected void detachModel() {
    super.detachModel();
    getModel().removeJsEventListener(m_listener);
  }

  @Override
  protected void initJsonProperties(T model) {
    super.initJsonProperties(model);
    putJsonProperty(new JsonAdapterProperty<>(JsEventManager.PROP_WIDGETS, model, getUiSession()) {
      @Override
      protected JsonAdapterPropertyConfig createConfig() {
        return new JsonAdapterPropertyConfigBuilder().disposeOnChange(false).build();
      }

      @Override
      protected Map<String, IWidget> modelValue() {
        return getModel().getWidgets();
      }

      @Override
      protected void createAdapters(Object modelValue) {
        if (modelValue instanceof Map) {
          ((Map<?, ?>) modelValue).values().forEach(this::createAdapter);
        }
        else {
          super.createAdapters(modelValue);
        }
      }

      @Override
      public Object prepareValueForToJson(Object value) {
        if (value instanceof Map) {
          JSONObject json = new JSONObject();
          //noinspection unchecked
          ((Map<String, Object>) value).forEach((id, widget) -> json.put(id, JsonAdapterUtility.getAdapterIdForModel(getUiSession(), widget, getParentJsonAdapter(), getFilter())));
          return json;
        }
        return super.prepareValueForToJson(value);
      }
    });
    putJsonProperty(new JsonProperty<>(JsEventManager.PROP_ELEMENTS, model) {
      @Override
      protected Map<String, Object> modelValue() {
        return getModel().getElements();
      }

      @Override
      public Object prepareValueForToJson(Object value) {
        if (value instanceof Map) {
          JSONObject json = new JSONObject();
          //noinspection unchecked
          ((Map<String, Object>) value).forEach((id, element) -> {
            IJsonObject jsonObject = MainJsonObjectFactory.get().createJsonObject(element);
            if (jsonObject instanceof IJsonObjectWithUiSession) {
              ((IJsonObjectWithUiSession) jsonObject).setUiSession(getUiSession());
            }
            if (jsonObject instanceof IJsonObjectWithParentAdapter) {
              ((IJsonObjectWithParentAdapter) jsonObject).setParentAdapter(JsonJsEventManager.this);
            }
            if (jsonObject != null) {
              json.put(id, jsonObject.toJson());
            }
          });
          return json;
        }
        return super.prepareValueForToJson(value);
      }
    });
  }

  protected void handleModelJsEvent(JsEvent event) {
    switch (event.getType()) {
      case JsEvent.TYPE_JS_EVENT_END:
        handleModelJsEventEnd(event.getId(), event.getData());
        break;
      case JsEvent.TYPE_JS_EVENT_CUSTOM:
        handleModelJsEventCustom(event.getCustomEventType(), event.getId(), event.getData());
        break;
      default:
        // NOP
    }
  }

  protected void handleModelJsEventEnd(String id, IDoEntity data) {
    addActionEvent(EVENT_JS_EVENT_END, new JSONObject()
        .put("id", id)
        .put("data", jsonDoHelper().dataObjectToJson(data)));
  }

  protected void handleModelJsEventCustom(String customEventType, String id, IDoEntity data) {
    addActionEvent(EVENT_JS_EVENT_CUSTOM, new JSONObject()
        .put("customEventType", customEventType)
        .put("id", id)
        .put("data", jsonDoHelper().dataObjectToJson(data)));
  }

  @Override
  public void handleUiEvent(JsonEvent event) {
    if (EVENT_JS_EVENT.equals(event.getType())) {
      handleUiJsEvent(event);
    }
    else {
      super.handleUiEvent(event);
    }
  }

  protected void handleUiJsEvent(JsonEvent event) {
    JSONObject eventData = event.getData();
    String id = eventData.getString("id");
    String eventType = eventData.getString("eventType");
    IDoEntity data = jsonDoHelper().jsonToDataObject(eventData.optJSONObject("data"), IDoEntity.class);

    getModel().getUIFacade().jsEventFromUI(id, eventType, data);
  }

  protected class P_JsEventListener implements JsEventListener {
    @Override
    public void jsEvent(JsEvent e) {
      ModelJobs.assertModelThread();
      handleModelJsEvent(e);
    }
  }
}
