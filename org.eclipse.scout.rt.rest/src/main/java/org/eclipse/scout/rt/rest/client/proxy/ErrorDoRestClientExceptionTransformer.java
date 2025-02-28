/*
 * Copyright (c) 2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.rest.client.proxy;

import java.util.function.BiFunction;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.eclipse.scout.rt.dataobject.exception.AccessForbiddenException;
import org.eclipse.scout.rt.dataobject.exception.ResourceNotFoundException;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.exception.VetoException;
import org.eclipse.scout.rt.rest.error.ErrorDo;
import org.eclipse.scout.rt.rest.error.ErrorResponse;

/**
 * REST client exception handler that extracts {@link ErrorDo} from the error {@link Response}.
 */
@ApplicationScoped
public class ErrorDoRestClientExceptionTransformer extends AbstractEntityRestClientExceptionTransformer {

  @Override
  protected RuntimeException transformByResponseStatus(Response.Status status, RuntimeException e, Response response) {
    switch (status) {
      case FORBIDDEN:
        return transformClientError(e, response, AccessForbiddenException::new);
      case NOT_FOUND:
        return transformClientError(e, response, ResourceNotFoundException::new);
    }
    return super.transformByResponseStatus(status, e, response);
  }

  @Override
  protected RuntimeException transformByResponseStatusFamily(Response.Status.Family family, RuntimeException e, Response response) {
    switch (family) {
      case CLIENT_ERROR:
        return transformClientError(e, response, VetoException::new);
    }
    return super.transformByResponseStatusFamily(family, e, response);
  }

  protected RuntimeException transformClientError(RuntimeException e, Response response, BiFunction<String, RuntimeException, VetoException> vetoExceptionFactory) {
    return safeTransformEntityErrorResponse(e, response, () -> {
      ErrorDo error = response.readEntity(ErrorResponse.class).getError();
      return vetoExceptionFactory.apply(error.getMessage(), e)
          .withTitle(error.getTitle())
          .withCode(error.getErrorCodeAsInt())
          .withSeverity(error.getSeverityAsInt());
    }, () -> {
      StatusType statusInfo = response.getStatusInfo();
      return vetoExceptionFactory.apply(statusInfo.getReasonPhrase(), e);
    });
  }

  @Override
  protected RuntimeException defaultTransform(RuntimeException e, Response response) {
    return safeTransformEntityErrorResponse(e, response, () -> {
      ErrorDo error = response.readEntity(ErrorResponse.class).getError();
      return new ProcessingException(error.getMessage(), e)
          .withTitle(error.getTitle())
          .withCode(error.getErrorCodeAsInt())
          .withSeverity(error.getSeverityAsInt());
    }, () -> {
      StatusType statusInfo = response.getStatusInfo();
      return new ProcessingException("REST call failed: {} {}", statusInfo.getStatusCode(), statusInfo.getReasonPhrase(), e);
    });
  }
}
