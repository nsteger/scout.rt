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
package org.eclipse.scout.rt.dataobject.migration.fixture.house;

import java.util.List;
import java.util.Set;

import org.eclipse.scout.rt.dataobject.DoEntityBuilder;
import org.eclipse.scout.rt.dataobject.IDoEntity;
import org.eclipse.scout.rt.dataobject.ITypeVersion;
import org.eclipse.scout.rt.dataobject.migration.AbstractDoStructureMigrationHandler;
import org.eclipse.scout.rt.dataobject.migration.DoStructureMigrationContext;
import org.eclipse.scout.rt.dataobject.migration.fixture.version.CharlieFixtureTypeVersions.CharlieFixture_3;
import org.eclipse.scout.rt.dataobject.migration.fixture.version.CharlieFixtureTypeVersions.CharlieFixture_5;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.IgnoreBean;
import org.eclipse.scout.rt.platform.util.CollectionUtility;

/**
 * Structure migration used for value migration tests. Inserts two rooms with different room types, which will be
 * migrated.
 */
@IgnoreBean
public class HouseFixtureDoStructureMigrationHandler_3 extends AbstractDoStructureMigrationHandler {

  @Override
  public Class<? extends ITypeVersion> toTypeVersionClass() {
    return CharlieFixture_3.class;
  }

  @Override
  public Set<String> getTypeNames() {
    return CollectionUtility.hashSet("charlieFixture.HouseFixture");
  }

  @Override
  protected boolean migrate(DoStructureMigrationContext ctx, IDoEntity doEntity) {
    List<IDoEntity> rooms = doEntity.getList("rooms", IDoEntity.class);

    rooms.add(BEANS.get(DoEntityBuilder.class)
        .put("_type", "charlieFixture.RoomFixture")
        .put("_typeVersion", CharlieFixture_5.VERSION.unwrap()) // latest version, no structure migration will be applied
        .put("name", "example room 1")
        .put("roomType", "room") // valid room type (already migrated), will not be migrated by RoomTypeFixtureDoValueMigrationHandler_2
        .build());

    rooms.add(BEANS.get(DoEntityBuilder.class)
        .put("_type", "charlieFixture.RoomFixture")
        .put("_typeVersion", CharlieFixture_5.VERSION.unwrap()) // latest version, no structure migration will be applied
        .put("name", "example room 2")
        .put("roomType", "standard-room") // old room type value, will be migrated by RoomTypeFixtureDoValueMigrationHandler_2
        .build());

    return true;
  }
}
