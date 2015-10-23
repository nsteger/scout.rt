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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

/**
 * Invocation chain according to the design pattern 'Chain-of-responsibility'.
 * <p>
 * This chain allows to perform a series of actions prior running a {@link Callable} command. This implementation allows
 * both, the interception by decorators and interceptors. A decorator can run some actions before or after the execution
 * of the command, but cannot wrap execution. That is where interceptors come into play, because they continue the chain
 * themselves by invoking the next element in the chain.
 * <p>
 * In contrast to a decorator, an interceptor wraps execution of subsequent handlers and the {@link Callable}.
 * Inherently, each interceptor in the chain blows up the stack, which is why to prefer decorators over interceptors
 * whenever possible.
 *
 * @since 5.2
 */
public class InvocationChain<RESULT> {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(InvocationChain.class);

  private final LinkedList<IChainable> m_chainables = new LinkedList<>();

  /**
   * Registers the given decorator at the beginning of this chain to decorate the execution of a command.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> registerFirst(final IInvocationDecorator decorator) {
    m_chainables.addFirst(decorator);
    return this;
  }

  /**
   * Registers the given interceptor at the beginning of this chain to wrap the execution of a command.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> registerFirst(final IInvocationInterceptor<RESULT> interceptor) {
    m_chainables.addFirst(interceptor);
    return this;
  }

  /**
   * Registers the given decorator at the end of this chain to decorate the execution of a command.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> registerLast(final IInvocationDecorator decorator) {
    m_chainables.addLast(decorator);
    return this;
  }

  /**
   * Registers the given interceptor at the end of this chain to wrap the execution of a command.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> registerLast(final IInvocationInterceptor<RESULT> interceptor) {
    m_chainables.add(interceptor);
    return this;
  }

  /**
   * Registers the given decorator at the end of this chain to decorate the execution of a command.
   * <p>
   * This method is equivalent to {@link #registerLast(IInvocationDecorator))}.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> register(final IInvocationDecorator decorator) {
    registerLast(decorator);
    return this;
  }

  /**
   * Registers the given interceptor at the end of this chain to wrap the execution of a command.
   * <p>
   * This method is equivalent to {@link #registerLast(IInvocationInterceptor)}.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> register(final IInvocationInterceptor<RESULT> interceptor) {
    registerLast(interceptor);
    return this;
  }

  /**
   * Removes all of the decorators and interceptors from this chain. The chain will be empty after this call returns.
   *
   * @return <code>this</code> in order to support method chaining.
   */
  public InvocationChain<RESULT> clear() {
    m_chainables.clear();
    return this;
  }

  /**
   * Invokes {@link Callable#call()} and allows all registered chain elements to participate in the invocation.
   *
   * @param command
   *          the command to be executed.
   * @return the result of the command.
   * @throws Exception
   *           thrown during execution of the command.
   */
  public RESULT invoke(final Callable<RESULT> command) throws Exception {
    return new Chain<RESULT>(m_chainables, command).continueChain();
  }

  /**
   * A Chain is an object provided by {@link InvocationChain} used to invoke the next handler in the chain, or if the
   * calling handler is the last handler in the chain, to invoke the {@link Callable} at the end of the chain.
   *
   * @param <RESULT>
   *          the result type of the {@link Callable} to be invoked.
   */
  public static class Chain<RESULT> {

    private final Iterator<IChainable> m_iterator;
    private final Callable<RESULT> m_command;

    public Chain(final List<IChainable> chainables, final Callable<RESULT> command) {
      m_iterator = chainables.iterator();
      m_command = command;
    }

    /**
     * Causes the next handler in the chain to be invoked, or if the calling handler is the last handler in the chain,
     * causes the {@link Callable} at the end of the chain to be invoked.
     *
     * @return the {@link Callable}'s return value to pass along to the invoker.
     * @throws Exception
     *           the {@link Callable}'s exception to pass along to the invoker.
     */
    public RESULT continueChain() throws Exception {
      // List of decorators invoked in this round.
      final List<IInvocationDecorator> invokedDecorators = new ArrayList<>();

      try {
        while (m_iterator.hasNext()) {
          final IChainable next = m_iterator.next();

          // Let the decorator to perform some 'before-execution' actions.
          if (next instanceof IInvocationDecorator) {
            final IInvocationDecorator decorator = (IInvocationDecorator) next;
            invokeOnBeforeSafe(decorator);
            invokedDecorators.add(decorator);
          }

          // Let the interceptor to continue the chain.
          if (next instanceof IInvocationInterceptor) {
            @SuppressWarnings("unchecked")
            final IInvocationInterceptor<RESULT> interceptor = ((IInvocationInterceptor<RESULT>) next);
            return interceptor.intercept(this);
          }
        }

        // Invoke the command because all handlers participated in the processing.
        return m_command.call();
      }
      finally {
        // Let the decorators to perform some 'after-execution' actions in reverse order.
        for (int i = invokedDecorators.size() - 1; i >= 0; i--) {
          invokeOnAfterSafe(invokedDecorators.get(i));
        }
      }
    }

    private void invokeOnBeforeSafe(final IInvocationDecorator decorator) {
      try {
        decorator.onBefore();
      }
      catch (final RuntimeException e) {
        LOG.error(String.format("Unexpected error during the 'before-decoration' of a command's execution. [decorator=%s, command=%s]", decorator.getClass().getSimpleName(), m_command), e);
      }
    }

    private void invokeOnAfterSafe(final IInvocationDecorator decorator) {
      try {
        decorator.onAfter();
      }
      catch (final RuntimeException e) {
        LOG.error(String.format("Unexpected error during the 'after-decoration' of a command's execution. [decorator=%s, command=%s]", decorator.getClass().getSimpleName(), m_command), e);
      }
    }
  }
}
