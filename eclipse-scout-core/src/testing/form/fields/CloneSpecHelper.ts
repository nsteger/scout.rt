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
import {Session, Widget} from '../../../index';
import 'jasmine';

export default class CloneSpecHelper {
  session: Session;

  constructor(session: Session) {
    this.session = session;
  }

  validateClone(original: Widget, clone: Widget) {
    // @ts-expect-error
    let properties = original._cloneProperties.filter(prop => {
        // @ts-expect-error
        return original._widgetProperties.indexOf(prop) < 0;
      }),
      // @ts-expect-error
      widgetProperties = original._cloneProperties.filter(prop => {
        // @ts-expect-error
        return original._widgetProperties.indexOf(prop) > -1;
      });

    // simple properties to be cloned
    properties.forEach(prop => {
      expect(clone).definedProperty(original, prop);
      expect(original).sameProperty(clone, prop);
    });

    // widget properties to be cloned
    widgetProperties.forEach(prop => {
      expect(clone).definedProperty(original, prop);

      expect(original).widgetCloneProperty(clone, prop);
    });
  }
}
