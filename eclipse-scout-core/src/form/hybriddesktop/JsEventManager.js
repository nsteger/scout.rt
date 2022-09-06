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
import {arrays, Event, objects, Widget} from '../../index';

export default class JsEventManager extends Widget {

  static DELIMITER = ':';

  static JS_EVENT = 'jsEvent';
  static JS_EVENT_END = 'jsEventEnd';
  static JS_EVENT_END_PREFIX = JsEventManager.JS_EVENT_END + JsEventManager.DELIMITER;
  static JS_EVENT_CUSTOM = 'jsEventCustom';

  static WIDGET_ADDED = 'widgetAdded';
  static WIDGET_ADDED_PREFIX = JsEventManager.WIDGET_ADDED + JsEventManager.DELIMITER;
  static WIDGET_REMOVED = 'widgetRemoved';
  static WIDGET_REMOVED_PREFIX = JsEventManager.WIDGET_REMOVED + JsEventManager.DELIMITER;

  static ELEMENT_ADDED = 'elementAdded';
  static ELEMENT_ADDED_PREFIX = JsEventManager.ELEMENT_ADDED + JsEventManager.DELIMITER;
  static ELEMENT_REMOVED = 'elementRemoved';
  static ELEMENT_REMOVED_PREFIX = JsEventManager.ELEMENT_REMOVED + JsEventManager.DELIMITER;

  static OPEN_FORM = 'openForm';
  static OPEN_FORM_PREFIX = JsEventManager.OPEN_FORM + JsEventManager.DELIMITER;

  static CREATE_PAGE = 'createPage';
  static CREATE_PAGE_PREFIX = JsEventManager.CREATE_PAGE + JsEventManager.DELIMITER;

  static REMOVE_PAGE = 'removePage';

  constructor() {
    super();

    this.widgets = {};
    this.elements = {};

    this._addWidgetProperties(['widgets']);
  }

  // static helpers

  static get(session) {
    return arrays.find(session.desktop.addOns, addOn => {
      return addOn instanceof JsEventManager;
    });
  }

  // widgets

  _setWidgets(widgets) {
    const oldWidgets = $.extend(true, {}, this.widgets),
      oldWidgetIds = Object.keys(oldWidgets);

    this._setProperty('widgets', widgets);

    const widgetIdsAdded = [], widgetIdsRemoved = [...oldWidgetIds];

    Object.keys(this.widgets).forEach(id => {
      if (!arrays.remove(widgetIdsRemoved, id)) {
        widgetIdsAdded.push(id);
      }
    });

    widgetIdsAdded.forEach(id => this._widgetAdded(id, this.widgets[id]));
    widgetIdsRemoved.forEach(id => this._widgetRemoved(id, oldWidgets[id]));
  }

  _widgetAdded(id, widget) {
    this.trigger(JsEventManager.WIDGET_ADDED_PREFIX + id, new Event({widget: widget}));
  }

  _widgetRemoved(id, widget) {
    this.trigger(JsEventManager.WIDGET_REMOVED_PREFIX + id, new Event({widget: widget}));
  }

  _removeWidgets() {
    // nop
  }

  _renderWidgets() {
    // nop
  }

  _getWidgetId(widget) {
    return objects.keyByValue(this.widgets, widget);
  }

  _hasWidget(id) {
    return Object.keys(this.widgets).indexOf(id) > -1;
  }

  _prepareWidgetProperty(propertyName, widgets) {
    if ('widgets' !== propertyName || !objects.isPlainObject(widgets)) {
      return super._prepareWidgetProperty(propertyName, widgets);
    }

    // <customized>
    let result = {};
    Object.keys(widgets).forEach(id => {
      // Create new child widget(s)
      result[id] = this._createChildren(widgets[id]);
    });
    widgets = Object.values(result);
    // </customized>

    let oldWidgets = this[propertyName];
    // <customized>
    oldWidgets = Object.values(oldWidgets);
    // </customized>
    if (oldWidgets && Array.isArray(widgets)) {
      // If new value is an array, old value has to be one as well
      // Only destroy those which are not in the new array
      oldWidgets = arrays.diff(oldWidgets, widgets);
    }

    if (!this.isPreserveOnPropertyChangeProperty(propertyName)) {
      // Destroy old child widget(s)
      this._destroyChildren(oldWidgets);

      // Link to new parent
      this.link(widgets);
    }

    // <customized>
    return result;
    // </customized>
  }

  // elements

  _setElements(elements) {
    const oldElements = $.extend(true, {}, this.elements),
      oldElementIds = Object.keys(oldElements);

    this._setProperty('elements', elements);

    const elementIdsAdded = [], elementIdsRemoved = [...oldElementIds];

    Object.keys(this.elements).forEach(id => {
      if (!arrays.remove(elementIdsRemoved, id)) {
        elementIdsAdded.push(id);
      }
    });

    elementIdsAdded.forEach(id => this._elementAdded(id, this.elements[id]));
    elementIdsRemoved.forEach(id => this._elementRemoved(id, oldElements[id]));
  }

  _elementAdded(id, element) {
    this.trigger(JsEventManager.ELEMENT_ADDED_PREFIX + id, new Event({element: element}));
  }

  _elementRemoved(id, element) {
    this.trigger(JsEventManager.ELEMENT_REMOVED_PREFIX + id, new Event({element: element}));
  }

  _getElementId(element) {
    return objects.keyByValue(this.elements, element);
  }

  _hasElement(id) {
    return Object.keys(this.elements).indexOf(id) > -1;
  }

  // listeners

  oneWidget(widget, type, handler) {
    this._oneId(this._getWidgetId(widget), type, handler);
  }

  onWidget(widget, type, handler) {
    return this._onId(this._getWidgetId(widget), type, handler);
  }

  offWidget(widget, type, handler) {
    this._offId(this._getWidgetId(widget), type, handler);
  }

  _oneId(id, type, handler) {
    if (!this._hasWidget(id)) {
      return;
    }
    type = type + JsEventManager.DELIMITER + id;
    this.one(JsEventManager.WIDGET_REMOVED_PREFIX + id, this.off.bind(this, type, handler));
    this.one(type, handler);
  }

  _onId(id, type, handler) {
    if (!this._hasWidget(id)) {
      return;
    }
    type = type + JsEventManager.DELIMITER + id;
    this.one(JsEventManager.WIDGET_REMOVED_PREFIX + id, this.off.bind(this, type, handler));
    return this.on(type, handler);
  }

  _offId(id, type, handler) {
    if (!this._hasWidget(id)) {
      return;
    }
    type = type + JsEventManager.DELIMITER + id;
    this.off(type, handler);
    this.off(JsEventManager.WIDGET_REMOVED_PREFIX + id, this.off.bind(this, type, handler));
  }

  // event support

  _createEventId() {
    return Date.now();
  }

  _jsEvent(eventType, data) {
    const id = this._createEventId();
    this.trigger(JsEventManager.JS_EVENT, new Event({
      data: {
        id: id,
        eventType: eventType,
        data: data
      }
    }));
    return id;
  }

  _jsEventEnd(id, data) {
    this.trigger(JsEventManager.JS_EVENT_END_PREFIX + id, new Event({data: data}));
  }

  _jsEventCustom(customEventType, id, data) {
    this.trigger(customEventType + JsEventManager.DELIMITER + id, new Event({data: data}));
  }

  /**
   * @return {Promise}
   */
  async jsEvent(eventType, data) {
    const id = this._jsEvent(eventType, data);
    const r = await this.when(JsEventManager.JS_EVENT_END_PREFIX + id);
    return r.data;
  }

  /**
   * @return {Promise}
   */
  async openForm(modelVariant, data) {
    const id = this._jsEvent(JsEventManager.OPEN_FORM_PREFIX + modelVariant, data);
    const r = await this.when(JsEventManager.WIDGET_ADDED_PREFIX + id);
    return r.widget;
  }

  /**
   * @return {Promise}
   */
  async createPage(modelVariant, data, outline) {
    const id = this._jsEvent(JsEventManager.CREATE_PAGE_PREFIX + modelVariant, data);
    const r = await this.when(JsEventManager.ELEMENT_ADDED_PREFIX + id);

    const model = r.element;
    model.detailForm = this.widget(model.detailForm);
    model.detailTable = this.widget(model.detailTable);
    const page = outline._createTreeNode(model);

    page.one('destroying', e => this._removePage(id));

    return page;
  }

  /**
   * @return {Promise}
   */
  async _removePage(pageId) {
    const id = this._jsEvent(JsEventManager.REMOVE_PAGE, {pageId: pageId});
    await this.when(JsEventManager.ELEMENT_REMOVED_PREFIX + id);
  }
}
