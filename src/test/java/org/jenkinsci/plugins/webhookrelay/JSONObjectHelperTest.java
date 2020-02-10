package org.jenkinsci.plugins.webhookrelay;

import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jenkinsci.plugins.webhookrelay.JSONObjectHelper.ENCODING;
import static org.jenkinsci.plugins.webhookrelay.JSONObjectHelper.getJSONObjectDecodeSafe;
import static org.junit.Assert.assertThat;

public class JSONObjectHelperTest {
  
  @Test
  public void testGetJSONObjectDecodeSafe() throws Exception {
    String body = IOUtils.toString(this.getClass().getResourceAsStream("encoded_payload.json"), ENCODING);
    
    JSONObject jsonObject = getJSONObjectDecodeSafe(body);
    
    assertThat(jsonObject, is(notNullValue()));
  }

}
