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

import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.dataobject.IDoEntity;

public class RemovePageJsEventHandler implements IJsEventHandler {

  private final static String ID = "removePage";

  @Override
  public String getEventType() {
    return ID;
  }

  @Override
  public void handleJsEvent(String id, IDoEntity data) {
    String pageId = String.valueOf(data.get("pageId"));

    Object element = jsEventManager().getElementById(pageId);
    if (element instanceof IPage) {
      IPage<?> page = (IPage<?>) element;
      jsEventManager().removeElement(page);
      jsEventManager().removeWidget(page.getDetailForm());
      jsEventManager().removeWidget(page.getTable(false));
    }
  }
}
