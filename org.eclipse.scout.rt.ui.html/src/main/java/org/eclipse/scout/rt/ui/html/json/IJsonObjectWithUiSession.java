package org.eclipse.scout.rt.ui.html.json;

import org.eclipse.scout.rt.ui.html.IUiSession;

public interface IJsonObjectWithUiSession extends IJsonObject {
  IUiSession getUiSession();

  void setUiSession(IUiSession uiSession);
}
