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
import {JsEventManager, ModelAdapter} from '../../index';

/**
 * @typedef {Event} scout.JsEvent
 * @property {string} customEventType
 * @property {string} id
 * @property {object} data
 */

export default class JsEventManagerAdapter extends ModelAdapter {

  constructor() {
    super();
  }

  /**
   * @param {scout.JsEvent} event
   */
  onModelAction(event) {
    if (event.type === JsEventManager.JS_EVENT_END) {
      this._onJsEventEnd(event);
    }
    if (event.type === JsEventManager.JS_EVENT_CUSTOM) {
      this._onJsEventCustom(event);
    } else {
      super.onModelAction(event);
    }
  }

  /**
   * @param {scout.JsEvent} event
   */
  _onJsEventEnd(event) {
    this.widget._jsEventEnd(event.id, event.data);
  }

  /**
   * @param {scout.JsEvent} event
   */
  _onJsEventCustom(event) {
    this.widget._jsEventCustom(event.customEventType, event.id, event.data);
  }

  _onWidgetEvent(event) {
    if (event.type === JsEventManager.JS_EVENT) {
      this._onJsEvent(event);
    } else {
      super._onWidgetEvent(event);
    }
  }

  _onJsEvent(event) {
    this._send(JsEventManager.JS_EVENT, event.data);
  }
}
