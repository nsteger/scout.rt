scout.MenuBarKeyStrokeAdapter = function(menuBar) {
  scout.MenuBarKeyStrokeAdapter.parent.call(this, menuBar);

  this.registerKeyStroke(new scout.MenuBarLeftKeyStroke(menuBar));
  this.registerKeyStroke(new scout.MenuBarRightKeyStroke(menuBar));
};
scout.inherits(scout.MenuBarKeyStrokeAdapter, scout.AbstractKeyStrokeAdapter);

/* --- MenuBarKeyStroke --- */

scout.MenuBarKeyStroke = function(menuBar, keyStroke) {
  scout.MenuBarKeyStroke.parent.call(this);
  this.menuBar = menuBar;
  this.keyStroke = keyStroke;
  this.drawHint = true;
  this.bubbleUp = false;
  this.initKeyStrokeParts();
};
scout.inherits(scout.MenuBarKeyStroke, scout.KeyStroke);

scout.MenuBarKeyStroke.prototype._drawKeyBox = function($container) {
  // NOP
};

/* --- MenuBarLeftKeyStroke --- */

scout.MenuBarLeftKeyStroke = function(menuBar) {
  scout.MenuBarLeftKeyStroke.parent.call(this, menuBar, 'LEFT');
};
scout.inherits(scout.MenuBarLeftKeyStroke, scout.MenuBarKeyStroke);

/**
 * @Override scout.KeyStroke
 */
scout.MenuBarLeftKeyStroke.prototype.handle = function(event) {
  var menuItems = this.menuBar.visibleMenuItems,
    $menuItemFocused = this.menuBar.$container.find(':focus'),
    i, menuItem, lastValidItem, elementToFocus;

  for (i = 0; i < menuItems.length; i++) {
    menuItem = menuItems[i];
    if ($menuItemFocused[0] === menuItem.$container[0]) {
      if (lastValidItem) {
        this.menuBar.setTabbableMenu(lastValidItem);
        lastValidItem.$container.focus();
      }
      break;
    }
    if (menuItem.isTabTarget()) {
      lastValidItem = menuItem;
    }
  }
};

/* --- MenuBarRightKeyStroke --- */

scout.MenuBarRightKeyStroke = function(menuBar) {
  scout.MenuBarRightKeyStroke.parent.call(this, menuBar, 'RIGHT');
};
scout.inherits(scout.MenuBarRightKeyStroke, scout.MenuBarKeyStroke);

/**
 * @Override scout.KeyStroke
 */
scout.MenuBarRightKeyStroke.prototype.handle = function(event) {
  var menuItems = this.menuBar.visibleMenuItems,
    $menuItemFocused = this.menuBar.$container.find(':focus'),
    i, menuItem, elementToFocus, focusNext = false;

  for (i = 0; i < menuItems.length; i++) {
    menuItem = menuItems[i];
    if (focusNext && menuItem.isTabTarget()) {
      this.menuBar.setTabbableMenu(menuItem);
      menuItem.$container.focus();
      break;
    }
    if ($menuItemFocused[0] === menuItem.$container[0]) {
      focusNext = true;
    }
  }
};
