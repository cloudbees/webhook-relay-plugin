package org.jenkinsci.plugins.webhookrelay;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class JSONObjectHelper {

  static final String ENCODING = "UTF-8";

  public static @Nonnull JSONObject getJSONObjectDecodeSafe(@Nonnull String body) {
    try {
      return JSONObject.fromObject(body);
    } catch (JSONException e) {
      return JSONObject.fromObject(urlDecode(body));
    }
  }

  private static @Nonnull String urlDecode(@Nonnull String pathToken) {
    try {
      return URLDecoder.decode(pathToken, ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(String.format("Unexpected URL decode exception. %s not supported on this system.", ENCODING), e);
    }
  }
}