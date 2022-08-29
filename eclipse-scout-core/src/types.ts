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

export type Predicate<T> = (obj: T) => boolean;

// Type that makes some properties optional and some required. TODO CGU move to separate file in util
export type PartialAndRequired<T, OPTIONAL extends keyof T, REQUIRED extends keyof T> = Omit<T, OPTIONAL | REQUIRED> & Partial<Pick<T, OPTIONAL>> & Required<Pick<T, REQUIRED>>;

export type EnumObject<TYPE> = TYPE[keyof TYPE];

export type EnumObject<TYPE> = TYPE[keyof TYPE];
