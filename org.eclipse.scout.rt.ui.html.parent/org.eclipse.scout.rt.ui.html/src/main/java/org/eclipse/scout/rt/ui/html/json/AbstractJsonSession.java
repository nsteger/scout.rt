/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json;

import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientJob;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.ui.IUiDeviceType;
import org.eclipse.scout.rt.shared.ui.IUiLayer;
import org.eclipse.scout.rt.shared.ui.UiDeviceType;
import org.eclipse.scout.rt.shared.ui.UiLayer;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class AbstractJsonSession implements IJsonSession, HttpSessionBindingListener {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractJsonSession.class);
  private static final long ROOT_ID = 1;

  private final JsonAdapterFactory m_jsonAdapterFactory;
  private final JsonAdapterRegistry m_jsonAdapterRegistry;
  private final Set<String> m_unregisterAdapterSet = new HashSet<String>();
  private final ICustomHtmlRenderer m_customHtmlRenderer;
  private final P_RootAdapter m_rootJsonAdapter;

  private JsonClientSession<? extends IClientSession> m_jsonClientSession;
  private String m_jsonSessionId;
  private long m_jsonAdapterSeq = ROOT_ID;
  private JsonResponse m_currentJsonResponse;
  private JsonRequest m_currentJsonRequest;
  private HttpServletRequest m_currentHttpRequest;
  private JsonEventProcessor m_jsonEventProcessor;
  private boolean m_disposing;

  public AbstractJsonSession() {
    m_currentJsonResponse = createJsonResponse();
    m_jsonAdapterFactory = createJsonAdapterFactory();
    m_jsonAdapterRegistry = createJsonAdapterRegistry();
    m_rootJsonAdapter = new P_RootAdapter(this);
    m_customHtmlRenderer = createCustomHtmlRenderer();
  }

  protected JsonResponse createJsonResponse() {
    return new JsonResponse();
  }

  protected JsonAdapterFactory createJsonAdapterFactory() {
    return new JsonAdapterFactory();
  }

  protected JsonAdapterRegistry createJsonAdapterRegistry() {
    return new JsonAdapterRegistry();
  }

  protected ICustomHtmlRenderer createCustomHtmlRenderer() {
    return new CustomHtmlRenderer();
  }

  /**
   * Returns a list of text keys (which must exist in a NLS property file) that are sent to the HTML client on start-up.
   * The texts returned by this method should be used as static UI texts. All other texts are sent as regular
   * (form-)data and must not be added here.
   */
  protected List<String> getTextKeys() {
    return Arrays.asList(
        // From org.eclipse.scout.rt.client
        "ResetTableColumns",
        "ColumnSorting",
        "Column",
        // From org.eclipse.scout.rt.ui.html
        "LoadOptions_",
        "NoOptions",
        "OneOption",
        "NumOptions",
        "InvalidDateFormat",
        "FilterBy_",
        "SearchFor_",
        "TableRowCount0",
        "TableRowCount1",
        "TableRowCount",
        "NumRowsSelected",
        "SelectAll",
        "SelectNone",
        "NumRowsFiltered",
        "NumRowsFilteredBy",
        "RemoveFilter",
        "NumRowsLoaded",
        "ReloadData",
        "Reload",
        "showEveryDate",
        "groupedByWeekday",
        "groupedByMonth",
        "groupedByYear",
        "Count",
        "ConnectionInterrupted",
        "ConnectionReestablished",
        "Reconnecting_",
        "ServerError",
        "SessionTimeout",
        "SessionExpiredMsg",
        "Move",
        "toBegin",
        "forward",
        "backward",
        "toEnd",
        "ascending",
        "descending",
        "ascendingAdditionally",
        "descendingAdditionally",
        "Sum",
        "overEverything",
        "grouped",
        "ColorCells",
        "fromRedToGreen",
        "fromGreenToRed",
        "withBarGraph",
        "remove",
        "add",
        "FilterBy",
        "Reconnecting",
        "Show",
        "Up",
        "Back",
        "Continue",
        "Ignore",
        "UiProcessingErrorTitle",
        "UiProcessingErrorText",
        "UiProcessingErrorAction"
        );
  }

  protected JSONObject getTextMap(Locale locale) {
    JSONObject map = new JSONObject();
    for (String textKey : getTextKeys()) {
      JsonObjectUtility.putProperty(map, textKey, TEXTS.get(locale, textKey));
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void init(HttpServletRequest request, JsonStartupRequest jsonStartupRequest) {
    m_currentHttpRequest = request;
    m_currentJsonRequest = jsonStartupRequest;
    m_jsonSessionId = jsonStartupRequest.getJsonSessionId();
    if (currentSubject() == null) {
      throw new SecurityException("/json request is not authenticated with a Subject");
    }

    IClientSession clientSession;
    synchronized (this) {
      HttpSession httpSession = m_currentHttpRequest.getSession();
      // Lookup the requested client session
      String clientSessionId = jsonStartupRequest.getClientSessionId();
      if (clientSessionId == null) {
        throw new IllegalStateException("Missing clientSessionId in JSON request");
      }
      String clientSessionAttributeName = "scout.htmlui.session.client." + clientSessionId;
      clientSession = (IClientSession) httpSession.getAttribute(clientSessionAttributeName);
      if (clientSession != null) {
        // Found existing client session
        LOG.info("Using cached client session [clientSessionId=" + clientSessionId + "]");
      }
      else {
        // No client session for the requested ID was found, so create one and store it in the map
        LOG.info("Creating new client session [clientSessionId=" + clientSessionId + "]");
        //FIXME CGU session must be started later, see JsonClientSession
        //return SERVICES.getService(IClientSessionRegistryService.class).newClientSession(clientSessionClass(), subject, UUID.randomUUID().toString(), userAgent);
        Locale oldLocale = LocaleThreadLocal.get(false);
        try {
          LocaleThreadLocal.set(request.getLocale());
          //
          clientSession = createClientSession();
          initClientSession(clientSession, jsonStartupRequest);
        }
        finally {
          LocaleThreadLocal.set(oldLocale);
        }
        httpSession.setAttribute(clientSessionAttributeName, clientSession);
        httpSession.setAttribute(clientSessionAttributeName + ".cleanup", new P_ClientSessionCleanupHandler(clientSessionId, clientSession));
      }
      // Handle detach
      String parentJsonSessionId = jsonStartupRequest.getParentJsonSessionId();
      if (parentJsonSessionId != null) {
        IJsonSession parentJsonSession = (IJsonSession) httpSession.getAttribute("scout.htmlui.session.json." + parentJsonSessionId);
        if (parentJsonSession != null) {
          LOG.info("Attaching jsonSession '" + m_jsonSessionId + "' to parentJsonSession '" + parentJsonSessionId + "'");
          // TODO BSH Detach | Actually do something
        }
      }
    }

    m_jsonClientSession = (JsonClientSession) createJsonAdapter(clientSession, m_rootJsonAdapter, null);
    m_jsonEventProcessor = createJsonEventProcessor();
    startUpClientSession(clientSession);

    JSONObject jsonEvent = new JSONObject();
    JsonObjectUtility.putProperty(jsonEvent, "clientSession", m_jsonClientSession.getId());
    JsonObjectUtility.putProperty(jsonEvent, "textMap", getTextMap(request.getLocale()));
    m_currentJsonResponse.addActionEvent(m_jsonSessionId, "initialized", jsonEvent);
    LOG.info("JsonSession with ID " + m_jsonSessionId + " initialized");
  }

  /**
   * @return a new {@link IClientSession} that is not yet initialized, so
   *         {@link IClientSession#startSession(org.osgi.framework.Bundle)} was not yet called
   */
  protected abstract IClientSession createClientSession();

  protected JsonEventProcessor createJsonEventProcessor() {
    return new JsonEventProcessor(this);
  }

  /**
   * initialize the properties of the {@link IClientSession} but does not yet start it
   * {@link IClientSession#startSession(org.osgi.framework.Bundle)} was not yet called
   */
  protected void initClientSession(IClientSession clientSession, JsonStartupRequest jsonStartupRequest) {
    UserAgent userAgent = createUserAgent(jsonStartupRequest);
    clientSession.setUserAgent(userAgent);
    clientSession.setSubject(currentSubject());
    clientSession.setVirtualSessionId(UUID.randomUUID().toString());
    //custom props
    HashMap<String, String> customProps = new HashMap<String, String>();
    JSONObject obj = jsonStartupRequest.getCustomParams();
    if (obj != null) {
      JSONArray names = jsonStartupRequest.getCustomParams().names();
      for (int i = 0; i < names.length(); i++) {
        customProps.put(names.optString(i), obj.optString(names.optString(i)));
      }
    }
    clientSession.initCustomParams(customProps);
  }

  protected void startUpClientSession(IClientSession clientSession) {
    ClientSyncJob job = new ClientSyncJob("AbstractJsonSession#startClientSession", clientSession) {
      @Override
      protected void runVoid(IProgressMonitor monitor) throws Throwable {
        m_jsonClientSession.startUp();
      }
    };
    job.runNow(new NullProgressMonitor());
    try {
      job.throwOnError();
    }
    catch (ProcessingException e) {
      throw new JsonException(e);
    }
    if (!clientSession.isActive()) {
      throw new JsonException("ClientSession is not active, there must have been a problem with loading or starting");
    }
  }

  protected UserAgent createUserAgent(JsonStartupRequest jsonStartupRequest) {
    IUiLayer uiLayer = UiLayer.HTML;
    IUiDeviceType uiDeviceType = UiDeviceType.DESKTOP;
    String browserId = m_currentHttpRequest.getHeader("User-Agent");
    JSONObject userAgent = jsonStartupRequest.getUserAgent();
    if (userAgent != null) {
      // FIXME CGU: it would be great if UserAgent could be changed dynamically, to switch from mobile to tablet mode on the fly, should be done as event in JsonClientSession
      String uiDeviceTypeStr = userAgent.optString("deviceType", null);
      if (uiDeviceTypeStr != null) {
        uiDeviceType = UiDeviceType.createByIdentifier(uiDeviceTypeStr);
      }
      String uiLayerStr = userAgent.optString("uiLayer", null);
      if (uiLayerStr != null) {
        uiLayer = UiLayer.createByIdentifier(uiLayerStr);
      }
    }
    return UserAgent.create(uiLayer, uiDeviceType, browserId);
  }

  protected Subject currentSubject() {
    return Subject.getSubject(AccessController.getContext());
  }

  @Override
  public void dispose() {
    if (m_currentHttpRequest != null) {
      // If there is a request in progress just mark the session as being disposed.
      // The actual disposing happens before returning to the client, see processRequest.
      m_disposing = true;
      return;
    }

    m_jsonAdapterRegistry.disposeAllJsonAdapters();
    m_currentJsonResponse = null;
    flush();
    // "Leak detection". After disposing all adapters and flushing the session, no adapters should be remaining.
    if (!m_jsonAdapterRegistry.isEmpty() && !m_unregisterAdapterSet.isEmpty()) {
      throw new IllegalStateException("JsonAdapterRegistry should be empty, but is not!");
    }
  }

  protected JsonAdapterFactory jsonAdapterFactory() {
    return m_jsonAdapterFactory;
  }

  protected JsonAdapterRegistry jsonAdapterRegistry() {
    return m_jsonAdapterRegistry;
  }

  @Override
  public IJsonAdapter<?> getRootJsonAdapter() {
    return m_rootJsonAdapter;
  }

  @Override
  public String getJsonSessionId() {
    return m_jsonSessionId;
  }

  @Override
  public IClientSession getClientSession() {
    return m_jsonClientSession.getModel();
  }

  @Override
  public JsonClientSession<? extends IClientSession> getJsonClientSession() {
    return m_jsonClientSession;
  }

  public long getJsonAdapterSeq() {
    return m_jsonAdapterSeq;
  }

  @Override
  public ICustomHtmlRenderer getCustomHtmlRenderer() {
    return m_customHtmlRenderer;
  }

  @Override
  public String createUniqueIdFor(IJsonAdapter jsonAdapter) {
    //FIXME CGU create id based on scout object for automatic gui testing, use @classId? or CustomWidgetIdGenerator from scout.ui.rwt bundle?
    return "" + (++m_jsonAdapterSeq);
  }

  @Override
  public IJsonAdapter<?> getJsonAdapter(String id) {
    return m_jsonAdapterRegistry.getJsonAdapter(id);
  }

  @Override
  public List<IJsonAdapter<?>> getJsonChildAdapters(IJsonAdapter<?> parent) {
    return m_jsonAdapterRegistry.getJsonAdapters(parent);
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A getJsonAdapter(M model, IJsonAdapter<?> parent) {
    return getJsonAdapter(model, parent, true);
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A getJsonAdapter(M model, IJsonAdapter<?> parent, boolean checkRoot) {
    A jsonAdapter = m_jsonAdapterRegistry.getJsonAdapter(model, parent);
    if (jsonAdapter == null && checkRoot) {
      jsonAdapter = m_jsonAdapterRegistry.getJsonAdapter(model, getRootJsonAdapter());
    }
    return jsonAdapter;
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A getOrCreateJsonAdapter(M model, IJsonAdapter<?> parent) {
    return getOrCreateJsonAdapter(model, parent, null);
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A getOrCreateJsonAdapter(M model, IJsonAdapter<?> parent, IJsonAdapterFactory adapterFactory) {
    A jsonAdapter = getJsonAdapter(model, parent);
    if (jsonAdapter != null) {
      return jsonAdapter;
    }
    return createJsonAdapter(model, parent, adapterFactory);
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A createJsonAdapter(M model, IJsonAdapter<?> parent) {
    return createJsonAdapter(model, parent, null);
  }

  @Override
  public <M, A extends IJsonAdapter<? super M>> A createJsonAdapter(M model, IJsonAdapter<?> parent, IJsonAdapterFactory adapterFactory) {
    A jsonAdapter = newJsonAdapter(model, parent, adapterFactory);

    // because it's a new adapter we must add it to the response
    m_currentJsonResponse.addAdapter(jsonAdapter);
    return jsonAdapter;
  }

  /**
   * Creates an adapter instance for the given model using the given factory and calls the <code>init()</code> method
   * on the created instance.
   */
  @SuppressWarnings("unchecked")
  public <M, A extends IJsonAdapter<? super M>> A newJsonAdapter(M model, IJsonAdapter<?> parent, IJsonAdapterFactory adapterFactory) {
    if (adapterFactory == null) {
      adapterFactory = m_jsonAdapterFactory;
    }
    String id = createUniqueIdFor(null); // FIXME CGU
    A adapter = (A) adapterFactory.createJsonAdapter(model, this, id, parent);
    adapter.init();
    return adapter;
  }

  @Override
  public void registerJsonAdapter(IJsonAdapter<?> jsonAdapter) {
    m_jsonAdapterRegistry.addJsonAdapter(jsonAdapter, jsonAdapter.getParent());
  }

  @Override
  public void unregisterJsonAdapter(String id) {
    boolean createdInCurrentRequest = m_currentJsonResponse.adapterMap().containsKey(id);
    if (createdInCurrentRequest) {
      // If adapter was not yet sent to the client, we can safely remove it from the registry
      // (as if it was never registered) and remove it completely from the response (including
      // events targeting the adapter).
      m_jsonAdapterRegistry.removeJsonAdapter(id);
      m_currentJsonResponse.removeJsonAdapter(id);
    }
    else {
      // Because the client might be interested in events for the disposed adapter, we cannot
      // remove it yet from the registry. Therefore, we add it to a temporary list which will
      // be processed after the response has been sent (see flush()).
      // Example case where this is relevant:
      //   0) Form was previously sent to the client
      //   1) Form is closed and disposed
      //   2) Desktop sends a "formRemoved" event
      // If the form adapter was removed in step 2), the JsonDesktop could not resolve the
      // adapter ID anymore in step 3).
      m_unregisterAdapterSet.add(id);
    }
  }

  @Override
  public void flush() {
    // Response has been sent, it is now safe to remove all adapters from the registry
    // that were previously only remembered in m_unregisterAdapterSet.
    if (LOG.isDebugEnabled()) {
      LOG.debug("Flush. Remove these adapter IDs from registry: " + m_unregisterAdapterSet);
    }
    for (String id : m_unregisterAdapterSet) {
      m_jsonAdapterRegistry.removeJsonAdapter(id);
    }
    m_unregisterAdapterSet.clear();
  }

  @Override
  public JsonResponse currentJsonResponse() {
    return m_currentJsonResponse;
  }

  protected JsonRequest currentJsonRequest() {
    return m_currentJsonRequest;
  }

  @Override
  public HttpServletRequest currentHttpRequest() {
    return m_currentHttpRequest;
  }

  public JsonEventProcessor getJsonEventProcessor() {
    return m_jsonEventProcessor;
  }

  @Override
  public JSONObject processRequest(HttpServletRequest httpRequest, JsonRequest jsonRequest) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adapter count before request: " + m_jsonAdapterRegistry.getJsonAdapterCount());
    }

    try {
      m_currentHttpRequest = httpRequest;
      m_currentJsonRequest = jsonRequest;
      if (jsonRequest.isStartupRequest()) {
        getJsonClientSession().processRequestLocale(httpRequest.getLocale());
      }
      JsonResponse jsonResponse = currentJsonResponse();
      getJsonEventProcessor().processEvents(m_currentJsonRequest, jsonResponse);

      return jsonResponse.toJson();
    }
    finally {
      // FIXME CGU really finally? what if exception occurs and some events are already delegated to the model?
      // reset event map (aka jsonResponse) when response has been sent to client
      flush();
      m_currentJsonResponse = createJsonResponse();
      m_currentHttpRequest = null;

      if (m_disposing) {
        dispose();
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adapter count after request: " + m_jsonAdapterRegistry.getJsonAdapterCount());
      }
    }
  }

  @Override
  public void logout() {
    LOG.info("Logging out...");
    currentHttpRequest().getSession(false).invalidate();
    currentJsonResponse().addActionEvent(getJsonSessionId(), "logout", new JSONObject());
    LOG.info("Logged out");
  }

  @Override
  public void valueBound(HttpSessionBindingEvent event) {
  }

  @Override
  public void valueUnbound(HttpSessionBindingEvent event) {
    dispose();
    LOG.info("JSON session with ID " + m_jsonSessionId + " unbound from HTTP session.");
  }

  /**
   * An instance of this class should be added to the HTTP session for each
   * client session. If the HTTP session is invalidated, this listener is
   * called and can shutdown the client session model.
   */
  protected static class P_ClientSessionCleanupHandler implements HttpSessionBindingListener {

    private final String m_clientSessionId;
    private final IClientSession m_clientSession;

    public P_ClientSessionCleanupHandler(String clientSessionId, IClientSession clientSession) {
      m_clientSessionId = clientSessionId;
      m_clientSession = clientSession;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
      LOG.info("Shutting down client session with ID " + m_clientSessionId + " due to invalidation of HTTP session...");

      // Dispose model (if session was not already stopped earlier by itself)
      if (m_clientSession.isActive()) {
        ClientJob job = new ClientSyncJob("Disposing client session", m_clientSession) {
          @Override
          protected void runVoid(IProgressMonitor monitor) throws Throwable {
            m_clientSession.getDesktop().getUIFacade().fireDesktopClosingFromUI(true);
          }
        };
        job.runNow(new NullProgressMonitor());
        try {
          job.throwOnError();
        }
        catch (ProcessingException e) {
          throw new JsonException(e);
        }
      }

      LOG.info("Client session with ID " + m_clientSessionId + " terminated.");
    }
  }

  private static class P_RootAdapter extends AbstractJsonAdapter<Object> {

    public P_RootAdapter(IJsonSession jsonSession) {
      super(new Object(), jsonSession, ROOT_ID + "", null);
    }

    @Override
    public String getObjectType() {
      return null;
    }

  }
}
