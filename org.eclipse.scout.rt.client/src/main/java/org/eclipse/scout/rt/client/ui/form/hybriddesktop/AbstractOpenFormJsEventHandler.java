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

import org.eclipse.scout.rt.client.ui.form.FormEvent;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.dataobject.DoEntity;
import org.eclipse.scout.rt.dataobject.IDoEntity;
import org.eclipse.scout.rt.platform.BEANS;

public abstract class AbstractOpenFormJsEventHandler<FORM extends IForm> implements IJsEventHandler {

  private final static String ID = "openForm";
  protected final static String ID_PREFIX = ID + DELIMITER;

  @Override
  public void handleJsEvent(String id, IDoEntity data) {
    FORM form = createForm(data);

    form.setShowOnStart(false);

    form.addFormListener(e -> {
      if (FormEvent.TYPE_STORE_AFTER == e.getType()) {
        DoEntity result = BEANS.get(DoEntity.class);
        exportResult(form, result);
        jsEventManager().customEvent("store", id, result);
      }
      else if (FormEvent.TYPE_RESET_COMPLETE == e.getType()) {
        DoEntity result = BEANS.get(DoEntity.class);
        exportResult(form, result);
        jsEventManager().customEvent("reset", id, result);
      }
      else if (FormEvent.TYPE_CLOSED == e.getType()) {
        jsEventManager().customEvent("close", id);
        jsEventManager().removeWidget(form);
      }
    });

    startForm(form);

    jsEventManager().addWidget(id, form);
  }

  protected abstract FORM createForm(IDoEntity data);

  protected void startForm(FORM form) {
    form.start();
  }

  protected void exportResult(FORM form, IDoEntity result) {
    // nop
  }
}
