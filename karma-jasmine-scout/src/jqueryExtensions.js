/*
 * Copyright (c) 2010-2021 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */

/* eslint-disable new-cap */

// JQuery extensions for testing purpose

$.fn.triggerBlur = function() {
  let event = jQuery.Event('blur', {
    originalEvent: jQuery.Event('dummy') // create dummy object
  });
  this.trigger(event);
};

$.fn.triggerRightClick = function() {
  this.trigger(jQuery.Event('mousedown', {which: 3}));
  this.trigger(jQuery.Event('mouseup', {which: 3}));
  return this;
};

$.fn.triggerKeyUp = function(key, modifier) {
  let event = jQuery.Event('keyup', {
    originalEvent: jQuery.Event('dummy'), // create dummy object
    which: key
  });
  extendEventWithModifier(event, modifier);
  this.trigger(event);
};

$.fn.triggerKeyDown = function(key, modifier) {
  let event = jQuery.Event('keydown', {
    originalEvent: jQuery.Event('dummy'), // create dummy object
    which: key
  });
  extendEventWithModifier(event, modifier);
  this.trigger(event);
};

function extendEventWithModifier(event, modifier) {
  if (!modifier) {
    return event;
  }
  event.altKey = modifier.includes('alt');
  event.ctrlKey = modifier.includes('ctrl');
  event.shiftKey = modifier.includes('shift');
  event.metaKey = modifier.includes('meta');
  return event;
}

$.fn.triggerMouseEnter = function(opts) {
  return this.triggerMouseAction('mouseenter', opts);
};

$.fn.triggerMouseLeave = function(opts) {
  return this.triggerMouseAction('mouseleave', opts);
};

$.fn.triggerMouseDown = function(opts) {
  return this.triggerMouseAction('mousedown', opts);
};

/**
 * Does not use jQuery to create the event to make sure capture phase listeners are notified as well.
 */
$.fn.triggerMouseDownCapture = function(opts) {
  let event = new MouseEvent('mousedown', {
    view: window,
    bubbles: true,
    cancelable: true
  });
  this[0].dispatchEvent(event);
  return this;
};

$.fn.triggerKeyDownCapture = function(which, modifier) {
  return this.triggerKeyCapture('keydown', which, modifier);
};

$.fn.triggerKeyUpCapture = function(which, modifier) {
  return this.triggerKeyCapture('keyup', which, modifier);
};

/**
 * Triggers key down and key up events.
 */
$.fn.triggerKeyInputCapture = function(which, modifier) {
  this.triggerKeyCapture('keydown', which, modifier);
  this.triggerKeyCapture('keyup', which, modifier);
  return this;
};

$.fn.triggerKeyCapture = function(eventType, which, modifier) {
  let eventObj = new KeyboardEvent(eventType, extendEventWithModifier({
    bubbles: true,
    cancelable: true,
    which: which,
    keyCode: which
  }, modifier));

  this[0].dispatchEvent(eventObj);
  return this;
};

$.fn.triggerMouseUp = function(opts) {
  return this.triggerMouseAction('mouseup', opts);
};

$.fn.triggerMouseMove = function(position) {
  return this.triggerWithPosition('mousemove', {
    position: position
  });
};

$.fn.triggerMouseAction = function(eventType, opts) {
  let event;
  opts = opts || {};

  if (!opts.position) {
    opts.position = this.offset();
  }
  if (!opts.clicks) {
    opts.clicks = 1;
  }
  if (!opts.which) {
    opts.which = 1;
  }
  event = jQuery.Event(eventType, {
    which: opts.which,
    originalEvent: jQuery.Event(eventType, {
      detail: opts.clicks
    }),
    pageX: opts.position.left,
    pageY: opts.position.top
  });
  if (opts.modifier) {
    extendEventWithModifier(event, opts.modifier);
  }

  this.trigger(event);
  return this;
};

$.fn.triggerWithPosition = function(event, position) {
  if (!position) {
    position = this.offset();
  }

  this.trigger({
    type: event,
    pageX: position.left,
    pageY: position.right
  });
  return this;
};

$.fn.triggerContextMenu = function() {
  let opts = {
    position: this.offset(),
    which: 3
  };

  this.triggerMouseDown(opts);
  this.triggerMouseUp(opts);
  this.trigger(jQuery.Event('contextmenu', {
    pageX: opts.position.left,
    pageY: opts.position.top
  }));
  return this;
};

/**
 * Triggers mouse down, mouse up and click events. <br>
 * Also sets the detail property of the originalEvent which contains the numbers of clicks.
 * @param opts options object passed to triggerMouse* functions
 */
$.fn.triggerClick = function(opts) {
  opts = opts || {};

  if (!opts.click) {
    opts.click = 1;
  }

  this.triggerMouseDown(opts);
  this.triggerMouseUp(opts);
  this.triggerMouseAction('click', opts);

  return this;
};

$.fn.triggerDoubleClick = function() {
  this.triggerClick();
  this.triggerClick({click: 2});
  this.trigger(jQuery.Event('dblclick', {
    originalEvent: jQuery.Event('dummy', {
      detail: 2
    })
  }));
  return this;
};

$.fn.triggerImageLoadCapture = function(opts) {
  let event = new Event('load', {
    bubbles: true,
    cancelable: true
  });
  this[0].dispatchEvent(event);
  return this;
};

$.fn.whenAnimationEnd = function() {
  let def = $.Deferred();
  this.oneAnimationEnd(() => {
    def.resolve();
  });
  return def.promise();
};
