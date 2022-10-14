/*
 * Copyright (c) 2010-2020 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {Menu, MenuModel, scout, Session} from '../../index';

export default class MenuSpecHelper {
  session: Session;

  constructor(session: Session) {
    this.session = session;
  }

  createModel(text: string, iconId: string, menuTypes: string[]): MenuModel {
    return {
      objectType: Menu,
      parent: this.session.desktop,
      text: text,
      iconId: iconId,
      menuTypes: menuTypes,
      visible: true
    };
  }

  createMenu(model: MenuModel): Menu {
    model = model || {} as MenuModel;
    model.objectType = model.objectType || Menu;
    model.session = this.session;
    model.parent = this.session.desktop;
    return scout.create(Menu, model);
  }
}

