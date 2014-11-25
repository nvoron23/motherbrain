package com.zillabyte.motherbrain.api;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import com.zillabyte.motherbrain.flow.MapTuple;
import com.zillabyte.motherbrain.utils.JSONUtil;
import com.zillabyte.motherbrain.utils.Utils;

public class LocalAPIService implements APIService {

  @Override
  public JSONObject getFlowSettings(String flowName, String authToken) throws APIException {
    JSONObject json = new JSONObject();
    json.put("id", flowName);
    json.put("version", 0);
    return json;
  }

  @Override
  public JSONObject getRelationSettings(String relationName, String authToken) throws APIException {
    return new JSONObject();
  }

  @Override
  public JSONObject getRelationConcretified(String sql, String authToken) throws APIException {
    JSONObject params = new JSONObject();
    params.put("relation", sql);
    return RestAPIHelper.post("/relation_backend/concretify_anonymous", params.toString(), authToken);
  }

  @Override
  public JSONObject postRelationSettingsForNextVersion(String flow_id, String relationName, JSONArray jsonSchema, String bufferType, String authToken) throws APIException {
    JSONBuilder builder = new JSONStringer();
    builder.object()
      .key("buffer_settings").object()
        .key("topic").value(relationName)
        .key("source").object()
          .key("type").value("s3")
          .key("retry").value(0)
          .key("config").object()
            .key("shard_path").value(relationName)
            .key("shard_prefix").value("shard_")
            .key("bucket").value("local")
            .key("credentials").object()
              .key("secret").value("")
              .key("access").value("")
            .endObject()
          .endObject()
        .endObject()
      .endObject()
    .endObject();
    
    return JSONUtil.parseObj(builder.toString()); // Builder to JSONObject?
  }

  @Override
  public JSONObject postFlowState(String id, String newState, String authToken) throws APIException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JSONObject postFlowRegistration(String id, JSONObject schema, String authToken) throws APIException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JSONObject appendRelation(String relationId, Collection<MapTuple> buffer, String authToken) throws APIException {
    return Utils.TODO("This is a good place to capture data generated by a sink, and then present it back to the user.  This method should get called if you instantiate the LocalBufferService in the universe");
  }

}
