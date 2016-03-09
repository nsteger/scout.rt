package org.eclipse.scout.rt.client.deeplink;

import java.util.regex.Matcher;

import org.eclipse.scout.rt.client.session.ClientSessionProvider;
import org.eclipse.scout.rt.client.ui.desktop.BrowserHistory;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Order(1000)
public class OutlineHandler extends AbstractDeepLinkHandler {

  private static final String HANDLER_PREFIX = "outline";

  private static final Logger LOG = LoggerFactory.getLogger(OutlineHandler.class);

  public OutlineHandler() {
    super(defaultPattern(HANDLER_PREFIX));
  }

  @Override
  public void handleImpl(Matcher matcher) throws DeepLinkException {
    String outlineId = matcher.group(1);
    String outlineName = matcher.group(2);
    LOG.info("Handling deep-link request for outline id=" + outlineId + " name=" + outlineName);

    IOutline selectedOutline = null;
    IDesktop desktop = ClientSessionProvider.currentSession().getDesktop();
    for (IOutline outline : desktop.getAvailableOutlines()) {
      String tmpOutlineId = outlineId(outline);
      if (tmpOutlineId.equals(outlineId)) {
        selectedOutline = outline;
        break;
      }
    }

    if (selectedOutline == null) {
      throw new DeepLinkException();
    }

    if (!selectedOutline.isVisible() || !selectedOutline.isEnabled()) {
      throw new DeepLinkException();
    }

    desktop.activateOutline(selectedOutline);
    LOG.info("Activate outline " + selectedOutline);
  }

  public BrowserHistory createBrowserHistory(IDesktop desktop, IOutline outline) {
    String path = getPathPrefix() + HANDLER_PREFIX + "/" + outlineId(outline) + "/" + toSlug(outline.getTitle());
    String title = desktop.getTitle() + " - " + outline.getTitle();
    return new BrowserHistory(path, title);
  }

  private static String outlineId(IOutline outline) {
    int nameChecksum = fletcher16(outline);
    nameChecksum = Math.abs(nameChecksum);
    return StringUtility.lpad(String.valueOf(nameChecksum), "0", 5);
  }

  private static short fletcher16(IOutline outline) {
    short sum1 = 0;
    short sum2 = 0;
    short modulus = 255;
    String data = outline.getClass().getName();

    for (int i = 0; i < data.length(); i++) {
      sum1 = (short) ((sum1 + data.charAt(i)) % modulus);
      sum2 = (short) ((sum2 + sum1) % modulus);
    }
    return (short) ((sum2 << 8) | sum1);
  }

}
