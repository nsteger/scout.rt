/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.jaxws.consumer;

/**
 * Listener to participate in <code>2-phase-commit-protocol (2PC)</code>.
 *
 * @since 5.1
 */
public interface ICommitListener {

  /**
   * Invoked once the transaction is about to commit.
   *
   * @return <code>true</code> to continue committing the transaction, <code>false</code> to abort.
   */
  boolean onCommitPhase1();

  /**
   * Invoked to finally commit the transaction.
   */
  void onCommitPhase2();
}
