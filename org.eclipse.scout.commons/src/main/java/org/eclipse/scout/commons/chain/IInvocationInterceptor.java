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
package org.eclipse.scout.commons.chain;

import java.util.concurrent.Callable;

import org.eclipse.scout.commons.chain.InvocationChain.Chain;

/**
 * An <code>Interceptor</code> is an object to be used in {@link InvocationChain} to intercept the execution of a
 * {@link Callable}.
 * <p>
 * In contrast to a decorator, an interceptor wraps execution of subsequent handlers and of the {@link Callable}.
 * Inherently, each interceptor in the chain blows up the stack, which is why to prefer decorators over interceptors
 * whenever possible.
 *
 * @see IInvocationDecorator
 * @since 5.2
 */
public interface IInvocationInterceptor<RESULT> extends IChainable {

  /**
   * Method invoked prior to executing the {@link Callable}. The {@link Chain} passed in to this method allows the
   * interceptor to continue execution with the next handler in the chain. If not continuing the chain, the
   * {@link Callable} is never invoked and this interceptor's result returned to the invoker.
   *
   * @param chain
   *          The chain to invoke the next handler in the chain, or if this interceptor is the last handler in the
   *          chain, to invoke the Callable at the end of the chain.
   * @return the result to pass along to the invoker.
   * @throws Exception
   *           the exception to pass along to the invoker.
   */
  RESULT intercept(Chain<RESULT> chain) throws Exception;
}
