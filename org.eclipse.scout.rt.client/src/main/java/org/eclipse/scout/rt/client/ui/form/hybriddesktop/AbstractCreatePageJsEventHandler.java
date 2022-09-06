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

public abstract class AbstractCreatePageJsEventHandler<PAGE extends IPage<?>> implements IJsEventHandler {

  private final static String ID = "createPage";
  protected final static String ID_PREFIX = ID + DELIMITER;

  @Override
  public void handleJsEvent(String id, IDoEntity data) {
    PAGE page = createPage(data);

    page.nodeAddedNotify();
    page.pageActivatedNotify();

    jsEventManager().addWidget(id + "detailForm", page.getDetailForm());
    jsEventManager().addWidget(id + "detailTable", page.getTable());
    jsEventManager().addElement(id, page);
  }

  protected abstract PAGE createPage(IDoEntity data);
}
