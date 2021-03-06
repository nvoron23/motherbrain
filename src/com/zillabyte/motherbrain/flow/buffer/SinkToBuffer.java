package com.zillabyte.motherbrain.flow.buffer;

import java.util.List;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.zillabyte.motherbrain.api.APIException;
import com.zillabyte.motherbrain.api.RelationsHelper;
import com.zillabyte.motherbrain.coordination.CoordinationException;
import com.zillabyte.motherbrain.flow.Fields;
import com.zillabyte.motherbrain.flow.FlowCompilationException;
import com.zillabyte.motherbrain.flow.MapTuple;
import com.zillabyte.motherbrain.flow.buffer.mock.LocalBufferProducer;
import com.zillabyte.motherbrain.flow.config.FlowConfig;
import com.zillabyte.motherbrain.flow.operations.Operation;
import com.zillabyte.motherbrain.flow.operations.OperationException;
import com.zillabyte.motherbrain.flow.operations.Sink;
import com.zillabyte.motherbrain.relational.ColumnDef;
import com.zillabyte.motherbrain.relational.RelationDef;
import com.zillabyte.motherbrain.universe.Universe;
import com.zillabyte.motherbrain.utils.Utils;

public class SinkToBuffer extends Sink {


  private static final long serialVersionUID = 4920057982004108977L;
  private static Logger _log = Logger.getLogger(SinkToBuffer.class);

  BufferProducer _producer;
  private String _topicName;
  private RelationDef _relation;
  private FlowConfig _config = FlowConfig.createEmpty();
  
  

  public SinkToBuffer(String id, List<ColumnDef> columns) {
    super(id);
    _relation = new RelationDef(id, id, columns);
    _topicName = id;
    _log.info("Instantiating the sink");
  }
  
  
  @SuppressWarnings("unchecked")
  public SinkToBuffer(JSONObject node, FlowConfig config) throws FlowCompilationException, InterruptedException {
    super(node.getString("name"));
     
    String relationName = node.containsKey("relation") ?  node.getString("relation") : node.getString("name");
    if (relationName == null) {
      throw (FlowCompilationException) new FlowCompilationException().setAllMessages("Could not get relation name from flow meta data: "+node);
    }
    _relation = new RelationDef(relationName);
    _topicName = null;  // We don't know it yet
    _config = config;
    
    List<JSONObject> columns = node.getJSONArray("columns");

    int index = 0;
    for(JSONObject col : columns) {
      String curName = (String) col.keys().next();
      String curType = col.getString(curName);
      ColumnDef cd = new ColumnDef(index++, ColumnDef.convertStringToDataType(curType), curName);
      _relation.addColumn(cd);
    }
  }
  
  /***
   * 
   * @param command
   * @throws OperationException
   * @throws InterruptedException 
   * @throws CoordinationException 
   */
  @Override
  protected void handleFlowCommand(String command) throws Exception {
    if (command.equalsIgnoreCase("cycle_acknowledged")) {

    }
    super.handleFlowCommand(command);
  }
  
  
  public RelationDef getRelation() {
    return _relation;
  }

  /****
   * 
   */
  @Override 
  public void onSetExpectedFields() throws OperationException {
    for(ColumnDef c : this._relation.allColumns()) {
      for(String s : c.getAliases()) {
        Fields f = new Fields(s);
        final String streamName = this.prevNonLoopConnection().streamName();
        this.prevNonLoopOperation().addExpectedFields(streamName, f);
      }
    }
    super.onSetExpectedFields();
  }

  @Override
  protected void process(MapTuple t) throws OperationException, InterruptedException {
    if(_producer == null)
      initProducer();
    // Sanity check:
    // The expected fields for each operation are defined based on what the following operation expects. During emit, the
    // output collector checks if the emitted tuple contains the fields expected by the following operation, however, the
    // tuple might contain extra fields that the following operation may or may not expect. In the case of a sink, we know
    // exactly which fields to expect, so we can make an extra sanity check here.
    for(String field : t.getValueKeys()) {
      if(field.contains(Operation.COMPONENT_CARRY_FIELD_PREFIX)) continue; // ignore the carry fields
      Boolean expectField = Boolean.FALSE;
      for(ColumnDef c : this._relation.allColumns()) {
        for(String alias: c.getAliases()) {
          if(alias.equalsIgnoreCase(field)) {
            expectField = Boolean.TRUE;
            break;
          }
        }
        if(expectField) break;
      }
      if(!expectField) {
        throw new OperationException(this, "Unexpected field '"+field+"' in tuple '"+t.toString()+"'. Data will not be sunk.");
      }
    }
    

    _producer.pushTuple(t);
  }

  public String getTopicName(){
    return _topicName;
  }
  
  @Override
  public int getMaxParallelism() {
    if(!Universe.instance().env().isTestOrProd()) {
      return 1; // Local buffer sinks will overwrite each other otherwise
    } else {
      return super.getMaxParallelism();
    }
  }
  
  @Override
  public void onFinalizeDeclare() throws OperationException, InterruptedException { 

    //RelationDefFactory relationFactory = Universe.instance().relationFactory();
    JSONObject bufferSettings;
    try {
      
      _config.set("buffer_type", "s3"); // TODO: Generic sink types?
      JSONObject apiResult = RelationsHelper.instance().postRelationConfigForNextVersion(_config, _relation.name(), _relation.valueColumns());
            
      if(apiResult.containsKey("buffer_settings")){
        bufferSettings = apiResult.getJSONObject("buffer_settings");
      }else{
        throw (OperationException) new OperationException(this, "Did not get bufferSettings json from API: " + apiResult.toString()).setUserMessage("Could not retrieve relation meta data from API. Please delete relations associated with this app, then try re-pushing. If problem persists, please contact support@zillabyte.com.");
      }

    } catch (APIException e) {
      throw (OperationException)new OperationException(this, e).setAllMessages("Could not get relation '" + _relation.name() + "' from API!");
    }
    _log.info("Got bufferSettings from API: " + bufferSettings.toString());
    JSONObject sourceSettings = bufferSettings.getJSONObject("source");
    JSONObject sourceConfig =  sourceSettings.getJSONObject("config");
    String shardPath = sourceConfig.getString("shard_path");
    String shardPrefix = sourceConfig.getString("shard_prefix");
    String bucket = sourceConfig.getString("bucket");
    //TODO: Credentials
    
    String concreteTableName = bufferSettings.getString("topic");
    _relation.setConcreteName(concreteTableName);
    _log.info("setting concrete name to : " + concreteTableName);
    
    // Have to make the JSON for sending to the sink.
    // Write to schloss
    final BufferService bufferService = Universe.instance().bufferService();
    
    _topicName = bufferSettings.getString("topic");
    bufferService.sinkBuffer(_topicName, shardPath, shardPrefix, bucket);
    int waitRetry = 1;
    while(!bufferService.hasTopic(_topicName)){
      waitRetry += 1;
      if(waitRetry > 30){
        throw (OperationException) new OperationException(this, "Kafka topic wasn't created in 30 seconds. It's likely that metamorphosis screwed up.").setUserMessage("Something is wrong with our internal data-writing system. Please report this error to support@zillabyte.com. Thanks and sorry for the inconvenience!");
      }
      _log.info("Waiting for the topic " + _topicName + " to be created. Attempt #" + waitRetry);
      Utils.sleep(1000); // This is before we deploy the topology to storm. 
    }
    
  }

  private void initProducer() throws OperationException {
    _producer = Universe.instance().bufferClientFactory().createProducer(this);
  }
  
  @Override
  public void onThisBatchCompleted(final Object batchId)  {
    super.onThisBatchCompleted(batchId);
    if(_producer instanceof LocalBufferProducer && Universe.instance().env().isLocal()) {
      ((LocalBufferProducer) _producer).closeFile();
    }
  }

}
