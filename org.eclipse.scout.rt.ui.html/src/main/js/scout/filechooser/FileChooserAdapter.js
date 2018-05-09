/*******************************************************************************
 * Copyright (c) 2014-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.FileChooserAdapter = function() {
  scout.FileChooserAdapter.parent.call(this);
};
scout.inherits(scout.FileChooserAdapter, scout.ModelAdapter);

scout.FileChooserAdapter.prototype._onWidgetCancel = function(event) {
  // Do not close the file chooser immediately, server will send the close event
  event.preventDefault();

  this._send('cancel');
};

scout.FileChooserAdapter.prototype._onWidgetEvent = function(event) {
  if (event.type === 'cancel') {
    this._onWidgetCancel(event);
  } else if (event.type === 'upload') {
    this._onUpload(event);
  } else {
    scout.FileChooserAdapter.parent.prototype._onWidgetEvent.call(this, event);
  }
};

scout.FileChooserAdapter.prototype._onUpload = function(event) {
  if (this.widget.rendered) {
    this.widget.$uploadButton.setEnabled(false);
  }

  if (this.widget.files.length === 0) {
    return;
  }

  if (this.widget.fileInput.legacy) {
    this.widget.fileInput.upload();
  } else {
    this.session.uploadFiles(this, this.widget.files, undefined, this.widget.maximumUploadSize);
  }

  this.session.listen().done(function() {
    if (this.widget && this.widget.rendered) {
      this.widget.$uploadButton.setEnabled(true);
    }
  }.bind(this));
};
