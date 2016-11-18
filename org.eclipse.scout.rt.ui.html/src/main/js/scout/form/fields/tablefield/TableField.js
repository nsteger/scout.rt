/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.TableField = function() {
  scout.TableField.parent.call(this);
  this._addAdapterProperties(['table']);

  this._tableChangedHandler;
  this._deletedRows = [];
  this._insertedRows = [];
  this._updatedRows = [];
  this._checkedRows = [];
};
scout.inherits(scout.TableField, scout.FormField);

scout.TableField.TABLE_CHANGE_EVENTS = 'rowsInserted rowsDeleted allRowsDeleted rowsUpdated rowsChecked';

scout.TableField.prototype._init = function(model) {
  scout.TableField.parent.prototype._init.call(this, model);

  this._delegatePropertyChange('enabled');
  this._delegatePropertyChange('disabledStyle');

  this._tableChangedHandler = this._onTableChanged.bind(this);

  if (this.table) {
    this.table.on(scout.TableField.TABLE_CHANGE_EVENTS, this._tableChangedHandler);
  }
};

scout.TableField.prototype._delegatePropertyChange = function(propertyName) {
  this.on('propertyChange', function(event) {
    if (event.newProperties.hasOwnProperty(propertyName)) {
      this.table.setProperty(propertyName, event.newProperties[propertyName]);
    }
  }.bind(this));
};

scout.TableField.prototype._render = function($parent) {
  this.addContainer($parent, 'table-field');
  this.addLabel();
  this.addMandatoryIndicator();
  this.addStatus();
  this._renderTable();
};

scout.TableField.prototype._syncTable = function(table) {
  if (this.table) {
    this.table.off(scout.TableField.TABLE_CHANGE_EVENTS, this._tableChangedHandler);
  }
  this._setProperty('table', table);
  if (table) {
    table.on(scout.TableField.TABLE_CHANGE_EVENTS, this._tableChangedHandler);
  }
};

scout.TableField.prototype.setTable = function(table) {
  this.setProperty('table', table);
};

scout.TableField.prototype._renderTable = function() {
  if (this.table) {
    this.table.render(this.$container);
    this.addField(this.table.$container);
  }
};

scout.TableField.prototype._removeTable = function() {
  if (this.table) {
    this.table.remove();
  }
  this._removeField();
};

scout.TableField.prototype.computeRequiresSave = function() {
  return this._deletedRows.length > 0 ||
      this._insertedRows.length > 0 ||
      this._updatedRows.length > 0 ||
      this._checkedRows.length > 0;
};

scout.TableField.prototype._onTableChanged = function(event) {
  if (scout.isOneOf(event.type, 'rowsDeleted', 'allRowsDeleted')) {
    scout.arrays.pushAll(this._deletedRows, event.rows);
    this._updateInsertedRows();
    return;
  }
  if (event.type === 'rowsInserted') {
    scout.arrays.pushAll(this._insertedRows, event.rows);
    return;
  }
  if (event.type === 'rowsUpdated') {
    scout.arrays.pushAll(this._updatedRows, event.rows);
    return;
  }
  if (event.type === 'rowsChecked') {
    this._toggleCheckedRows(event.rows);
  }
};

/**
 * If a row is contained in both arrays (_deletedRows, _insertedRows) an inserted row has been
 * deleted again. In that case we can remove that row from both arrays.
 */
scout.TableField.prototype._updateInsertedRows = function () {
  var insertedAndDeletedRows = [];
  this._deletedRows.forEach(function(deletedRow) {
    if (this._insertedRows.indexOf(deletedRow) !== -1) {
      insertedAndDeletedRows.push(deletedRow);
    }
  }, this);

  insertedAndDeletedRows.forEach(function(row) {
    scout.arrays.remove(this._deletedRows, row);
    scout.arrays.remove(this._insertedRows, row);
  }, this);
};

/**
 * If a row already exists in the _checkedRows array, remove it (row was checked/unchecked again, which
 * means it is no longer changed). Add it to the array otherwise.
 */
scout.TableField.prototype._toggleCheckedRows = function (rows) {
  rows.forEach(function(row) {
    var removed = scout.arrays.remove(this._checkedRows, row);
    if (!removed) {
      this._checkedRows.push(row);
    }
  }, this);
};

scout.TableField.prototype.markAsSaved = function() {
  scout.TableField.parent.prototype.markAsSaved.call(this);
  this._deletedRows = [];
  this._insertedRows = [];
  this._updatedRows = [];
  this._checkedRows = [];
};
