/*
 * Copyright (c) 2010-2019 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
// Eclipse Scout module: re-exports
// The modules exported here will be available when someone imports from 'eclipse-scout'

import * as scout from './scout/dummy-scout';
import * as self from './index.js';

export { scout as default };
export { default as DummyApp } from './scout/DummyApp';

window.scout = Object.assign(window.scout || {}, self);
