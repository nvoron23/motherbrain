package com.zillabyte.motherbrain.api;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.zillabyte.motherbrain.universe.Config;
import com.zillabyte.motherbrain.utils.JSONUtil;
import com.zillabyte.motherbrain.utils.Utils;

public class RestAPIHelper {
  
  protected static Logger log = Logger.getLogger(RestAPIHelper.class);
  public static final int MAX_RETRIES = 5;
  public static final long RETRY_SLEEP_MS = TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS);

  public RestAPIHelper(APIService api) {
  }
  
  public static String getHost(){
    return Config.getOrDefault("api.host", "localhost");
  }
  
  public static int getPort() {
    return Config.getOrDefault("api.port", Utils.valueOf(5000)).intValue();
  }

  public static JSONObject get(String path, String authToken) throws APIException {
    return get(path, authToken, null);
  }
  
  public static JSONObject get(String path, String authToken, JSONObject params) throws APIException {
    String url = "http://" + getHost() + ":" + getPort() + path;
    return getUrl(authToken, url, params);
  }

  public static JSONObject getUrl(String authToken, String url) throws APIException {
    return getUrl(authToken, url, null);
  }
  
  public static JSONObject getUrl(String authToken, String url, JSONObject params) throws APIException {
    Client client = Client.create();
    ClientResponse response;
    int retries = 0;
    
    log.info("get: " + url);
    
    do {
      retries++;
      WebResource webResource = client.resource(url);
      if(params != null) {
        Iterator<?> keys = params.keys();
        while(keys.hasNext()) {
          String key = (String) keys.next();
          Object value = params.get(key);
          if(value instanceof JSONArray) {
            for(Object a : ((JSONArray) value).toArray()) {
              webResource = webResource.queryParam(key, (String) a);
            }
          } else {
            webResource = webResource.queryParam(key, (String) params.get(key));
          }
        }
      }
      
      Builder b = webResource
          .accept("application/json");
      if (authToken != null && !authToken.isEmpty())
          b.header("Authorization", "auth_token " + authToken);
      response = b.get(ClientResponse.class);
      
      if (response.getStatus() == 200) {
        break;
      } else if (response.getStatus() >= 500) {
        log.info("Server responded with 500. Retrying in a few seconds...(" + retries + ")");
        Utils.sleep(RETRY_SLEEP_MS);
      } else {
         throw (APIException) new APIException("Failed : HTTP error code : " + response.getStatus() + " " + response.getEntity(String.class)).adviseRetry();
      }
    } while(retries < MAX_RETRIES);
 
    final String output = response.getEntity(String.class);
    if (retries == MAX_RETRIES) {
      throw (APIException) new APIException("Failed: max retries exceeded: response from API: " + output).adviseRetry();
    }

    log.debug("get returned: " + output);

    JSONObject ret = JSONUtil.parseObj(output);

    if (ret == null) {
      throw (APIException) new APIException("Failed: returned response body was null for output: "+output).adviseRetry();
    }

    log.info("done: " + url);
    return ret;
  }
  
  
  public static JSONObject post(String path, String body, String authToken) throws APIException {
    String url = "http://" + getHost() + ":" + getPort() + path;

    return postUrl(url, body, authToken);
    
  }

  public static JSONObject postUrl(String url, String body, String authToken) throws APIException {
    log.info("post: " + url + " body: " + body);
    Client client = Client.create();
    WebResource webResource = client.resource(url);
    ClientResponse response;
    
    int retries = -1;
    do {
      retries++; 
      Builder b = webResource
          .type("application/json")
          .accept("application/json");
      if (authToken != null && !authToken.isEmpty()) 
          b.header("Authorization", "auth_token " + authToken);
      response = b.post(ClientResponse.class, body);
   
      if (response.getStatus() == 200) {
        break;
      } else if (response.getStatus() >= 500) {
        
        // See if this is a formatted error from the server... 
        String output = response.getEntity(String.class);
        log.info("Server responded with 500. Retrying in a few seconds...(" + retries + ") (" + url + "): " + Utils.truncate(output));
        JSONObject ret; 
        try { 
          
          // It's a formatted message.. pass it on to the user... 
          ret = JSONUtil.parseObj(output);
          if (ret.containsKey("error_message")) {
            throw ((APIException) new APIException("API error").setInternalMessage(ret.getString("error_message")).setUserMessage(ret.getString("error_message")));
          }
          
        } catch(JSONException ex) {
          Utils.sleep(RETRY_SLEEP_MS);
        }
      } else {
        throw (APIException) new APIException("Failed : HTTP error code : " + response.getStatus()).adviseRetry();
      }
    } while (retries < MAX_RETRIES);

    if (retries == MAX_RETRIES) {
      throw (APIException) new APIException("Failed: max retries exceeded").adviseRetry();
    }

    String output = response.getEntity(String.class);
    log.info("post returned: " + output);
    JSONObject ret = JSONUtil.parseObj(output);
    if (ret == null) // Should not be NULL, it's just a string conversion.
      throw (APIException) new APIException("Failed: returned response body was null for output: "+output).adviseRetry();
    return ret;
  }
  
  public static JSONObject put(String path, String body, String authToken) throws APIException, InterruptedException {
    Client client = Client.create();
    String url = "http://" + getHost() + ":" + getPort() + path;

    log.info("put: " + url + " body: " + body);

    WebResource webResource = client.resource(url);

    ClientResponse response;

    int retries = 0;
    do {
      
      Builder b = webResource
          .type("application/json")
          .accept("application/json");
      if (authToken != null && !authToken.isEmpty())
          b.header("Authorization", "auth_token " + authToken);
      response = b.put(ClientResponse.class, body);
   
      if (response.getStatus() == 200) {
        break;
      } else if (response.getStatus() >= 500) {
        log.info("Server responded with 500. Retrying in a few seconds...(" + retries + ")");
        Thread.sleep(RETRY_SLEEP_MS);
      } else {
        throw (APIException) new APIException("Failed : HTTP error code : " + response.getStatus()).adviseRetry();
      }
      retries++;

    } while (retries < MAX_RETRIES);

    if (retries == MAX_RETRIES) {
      throw (APIException) new APIException("Failed: max retries exceeded").adviseRetry();
    }

    String output = response.getEntity(String.class);
    log.debug("post returned: " + output);
    
    JSONObject ret = JSONUtil.parseObj(output);
    if (ret == null) // Should not be NULL
      throw (APIException) new APIException("Failed: returned response body was null for output: "+output).adviseRetry();
    return ret;
    
  }
}
