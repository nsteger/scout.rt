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

import java.util.EventObject;

import org.eclipse.scout.rt.client.ui.IModelEvent;
import org.eclipse.scout.rt.dataobject.IDoEntity;

public class JsEvent extends EventObject implements IModelEvent {

  private static final long serialVersionUID = 1L;

  public static final int TYPE_JS_EVENT_CUSTOM = 13;
  public static final int TYPE_JS_EVENT_END = 42;

  private int m_type;

  private String m_customEventType;
  private String m_id;
  private IDoEntity m_data;

  public JsEvent(Object source, int type) {
    super(source);
    m_type = type;
  }

  public JsEvent(Object source, int type, String id, IDoEntity data) {
    this(source, type);
    m_id = id;
    m_data = data;
  }

  public JsEvent(Object source, int type, String customEventType, String id, IDoEntity data) {
    this(source, type, id, data);
    m_customEventType = customEventType;
  }

  @Override
  public int getType() {
    return m_type;
  }

  public String getCustomEventType() {
    return m_customEventType;
  }

  public String getId() {
    return m_id;
  }

  public IDoEntity getData() {
    return m_data;
  }
}
