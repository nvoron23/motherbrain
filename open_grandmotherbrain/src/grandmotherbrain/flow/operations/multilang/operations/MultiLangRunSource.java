package grandmotherbrain.flow.operations.multilang.operations;

import grandmotherbrain.benchmarking.Benchmark;
import grandmotherbrain.container.ContainerWrapper;
import grandmotherbrain.coordination.CoordinationException;
import grandmotherbrain.flow.EndCyclePolicy;
import grandmotherbrain.flow.MapTuple;
import grandmotherbrain.flow.StateMachineException;
import grandmotherbrain.flow.collectors.OutputCollector;
import grandmotherbrain.flow.operations.OperationException;
import grandmotherbrain.flow.operations.OperationLogger;
import grandmotherbrain.flow.operations.Source;
import grandmotherbrain.flow.operations.multilang.MultiLangException;
import grandmotherbrain.flow.operations.multilang.MultiLangProcessTupleObserver;

import java.util.concurrent.TimeoutException;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.javatuples.Pair;

public class MultiLangRunSource extends Source implements MultiLangOperation {
  
  private static final long serialVersionUID = 2067987097123293403L;
  private static final Logger _log = Logger.getLogger(MultiLangRunSource.class);
  
  private MultilangHandler _handler;
  private EndCyclePolicy _endCyclePolicy = EndCyclePolicy.NULL_EMIT;
  
  
  public MultiLangRunSource(JSONObject nodeSettings, ContainerWrapper container) {
    super(MultilangHandler.getName(nodeSettings), MultilangHandler.getConfig(nodeSettings));
    _handler = new MultilangHandler(this, nodeSettings, container);
    
    if (nodeSettings.containsKey("end_cycle_policy")) {
      if (nodeSettings.getString("end_cycle_policy").equalsIgnoreCase("explicit") ) {
        _endCyclePolicy = EndCyclePolicy.EXPLICIT;
      } else if (nodeSettings.getString("end_cycle_policy").equalsIgnoreCase("infinite") ) {
        _endCyclePolicy = EndCyclePolicy.INFINITE;
      }
    }
    
  }
  
  
  
  @Override
  public void prepare() throws MultiLangException, InterruptedException {
    _handler.prepare();
  }
  

  @Override
  public final void cleanup() throws MultiLangException, InterruptedException {
    _handler.cleanup();
  }

  
  public MultiLangRunSource setEndCyclePolicy(EndCyclePolicy v) {
    _endCyclePolicy = v;
    return this;
  }
  
  
  @Override
  public void onBeginCycle(OutputCollector output) throws InterruptedException, OperationException, CoordinationException, StateMachineException, TimeoutException {
    super.onBeginCycle(output);
    _handler.writeMessage("{\"command\": \"begin_cycle\"}");
    _handler.waitForDoneMessageWithoutCollecting();
    _handler.maybeThrowNextError();
  }

  
  @Override
  public void onEndCycle(OutputCollector output) throws OperationException, InterruptedException, CoordinationException, StateMachineException, TimeoutException {
    super.onEndCycle(output);
    _handler.maybeThrowNextError();
  }

  
  @Override
  protected boolean nextTuple(OutputCollector collector) throws InterruptedException, OperationException {

    Benchmark.markBegin("multilang.source.next_tuple");
    try {
      
      // INIT
      int emitted = 0;
      boolean nullEmitted = false;
      Object output = null;
  
      // Sanity
      _handler.ensureAlive();
      if (_handler.tupleObserver().queue().size() != 0) {
        throw (OperationException) new OperationException(this).setInternalMessage("attempt to get nextTuple when queue is not empty: " + _handler.tupleObserver().queue());
      }
      
      // Issue the command
      Benchmark.markBegin("multilang.source.next_tuple.command_next");
      _handler.writeMessage("{\"command\": \"next\"}");
      _handler.maybeThrowNextError();
      Benchmark.markEnd("multilang.source.next_tuple.command_next");
      
      // Iterate until signaled to end, or exception
      do {
      
        // Collect the next tuple/execption/signal
        Benchmark.markBegin("multilang.source.next_tuple.take_next_tuple");
        output = _handler.takeNextTuple();
        Benchmark.markEnd("multilang.source.next_tuple.take_next_tuple");
        _handler.maybeThrowNextError();
        
        if (nullEmitted) {
          // Once nullEmitted is set, just ignore everything until we see a DONE_MARKER
          continue;
          
        } else if (output == MultiLangProcessTupleObserver.END_CYCLE_MARKER) {
          // Done with this cycle
          nullEmitted = true;
          continue;
          
        } else if (output == MultiLangProcessTupleObserver.DONE_MARKER) {
          // We've recieved the 'done' message. This is handled in the loop condition
          continue;
          
        } else if (output instanceof Pair<?, ?>) {
          
          // Valid message
          Pair<String, MapTuple> pair = (Pair<String, MapTuple>) output;
          String stream = pair.getValue0();
          MapTuple tuple = pair.getValue1();
          
          // Depending on our policy, this may be the end of the cycle!
          if (_endCyclePolicy == EndCyclePolicy.NULL_EMIT) {
            // Check for any null values.  If they exist, then end the cycle
            for(Object o : tuple.values().values()) {
              if (o == null || o.equals("null")) {   // <-- weird JS thing
                // Set the nullEmitted flag and continue collecting tuples until we see a DONE
                _log.info("null value detected; ending cycle");
                nullEmitted = true;
              }
            }
          }
    
          // Emit the tuple, continue looking for the DONE mark
          emitted++;
          Benchmark.markBegin("multilang.source.next_tuple.emit");
          collector.emit(stream, tuple);
          Benchmark.markEnd("multilang.source.next_tuple.emit");
          continue;
          
        } else if (output instanceof Throwable) {
          throw new OperationException(this, (Throwable) output);
          
        } else { 
          throw new OperationException(this, "unknown type");
          
        }
      } while(output != MultiLangProcessTupleObserver.DONE_MARKER);
      
      // Shall we end the cycle? 
      if (emitted == 0) {
        if (_endCyclePolicy == EndCyclePolicy.NULL_EMIT) {
          _operationLogger.writeLog("No tuples were emitted from the source '" + this.instanceName() + "' and the cycle will now end.  If you wish to change this behavior, set the 'end_cycle_policy' to 'explicit'.", OperationLogger.LogPriority.SYSTEM);
          _log.info("no tuples emitted; ending cycle");
          return false;
        }
      }
      
      // Was a nullEmitted? If so, end the cycle
      if (nullEmitted) {
        return false;
      }
      
      // We've made it here, so continue the cycle
      return true;
      
    } finally {
      Benchmark.markEnd("multilang.source.next_tuple");
    }
  }

  
  @Override 
  public boolean isAlive() {
    return _handler.isAlive();
  }


  @Override
  public ContainerWrapper getContainer() {
    return _handler.getContainer();
  }

  
  @Override
  public MultilangHandler getMultilangHandler() {
    return _handler;
  }
  
}
