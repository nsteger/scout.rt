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
package org.eclipse.scout.rt.dataobject.migration.fixture.house;

import org.eclipse.scout.rt.dataobject.DataObjectHelper;
import org.eclipse.scout.rt.dataobject.ITypeVersion;
import org.eclipse.scout.rt.dataobject.migration.AbstractDoValueMigrationHandler;
import org.eclipse.scout.rt.dataobject.migration.DoStructureMigrationContext;
import org.eclipse.scout.rt.dataobject.migration.DoValueMigrationId;
import org.eclipse.scout.rt.dataobject.migration.fixture.version.DeltaFixtureTypeVersions.DeltaFixture_2;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IgnoreBean;
import org.eclipse.scout.rt.platform.util.NumberUtility;

/**
 * This is an example for a non-idempotent value migration.<br>
 * <b>Only used for testing!</b><br>
 * Real-world value migrations must always be idempotent.
 */
@IgnoreBean
public class RoomSizeFixtureDoValueMigrationHandler_2 extends AbstractDoValueMigrationHandler<RoomFixtureDo> {

  public static final DoValueMigrationId ID = DoValueMigrationId.of("ce318ac4-8b6e-4022-8de0-a0becb7358e5");

  @Override
  public DoValueMigrationId id() {
    return ID;
  }

  @Override
  public Class<? extends ITypeVersion> typeVersionClass() {
    return DeltaFixture_2.class; // after RoomTypeValueMigrationHandler
  }

  @Override
  public RoomFixtureDo migrate(DoStructureMigrationContext ctx, RoomFixtureDo value) {
    return BEANS.get(DataObjectHelper.class).clone(value) // clone provided value to allow change detection by caller
        // increase area by 10
        // non-idempotent migration for testing purposes only, real-world value migrations must be idempotent!
        .withAreaInSquareMeter(NumberUtility.nvl(value.getAreaInSquareMeter(), 0) + 10);
  }
}
