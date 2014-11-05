package grandmotherbrain.universe;

import grandmotherbrain.api.APIService;
import grandmotherbrain.benchmarking.BenchmarkFactory;
import grandmotherbrain.container.ContainerFactory;
import grandmotherbrain.coordination.CoordinationServiceWrapper;
import grandmotherbrain.flow.FlowService;
import grandmotherbrain.flow.aggregation.AggregationStoreFactory;
import grandmotherbrain.flow.buffer.BufferClientFactory;
import grandmotherbrain.flow.buffer.BufferService;
import grandmotherbrain.flow.error.strategies.ErrorStrategyFactory;
import grandmotherbrain.flow.operations.multilang.builder.FlowBuilderFactory;
import grandmotherbrain.flow.rpc.queues.QueueFactory;
import grandmotherbrain.metrics.Metrics;
import grandmotherbrain.relational.RelationDefFactory;
import grandmotherbrain.shell.ShellFactory;
import grandmotherbrain.top.TopService;
import grandmotherbrain.utils.dfs.DFSService;
import grandmotherbrain.utils.dfs.DFSServiceWrapper;

import java.io.Serializable;

import org.apache.log4j.Logger;


/****
 * The universe object is a singleton (i.e. it only has one instance in the entire JVM).  It is meant to 
 * capture the state of the entire GrandmotherBrain system.  I.e. it should be used to track which Flows 
 * are running, their underlying buffers, provisions. etc.  The goal is to be able to terminate the 
 * entire system, then restart in the same state.  
 *
 */
public class Universe implements Serializable {

  private static final long serialVersionUID = -1384275399314055348L;
  private static final Logger log = Logger.getLogger(Universe.class);
  
  // Master Services  
  transient FlowService _flowService = null;
  transient TopService _topService = null;
  
  // Support Services/Factories
  APIService _api = null;
  RelationDefFactory _relFactory = null;
  SSHFactory _sshFactory = null;
  FileFactory _fileFactory = null;
  LoggerFactory _loggerFactory = null;
  Metrics _metrics = null;
  ExceptionHandler _exceptionHandler = null;
  QueueFactory _queueFactory = null;
  BenchmarkFactory _benchmarkFactory = new BenchmarkFactory.Noop();
  ShellFactory _shellFactory = null; 
  DFSService _dfsService = null;
  CoordinationServiceWrapper _state = null;
  AggregationStoreFactory _aggregationStoreFactory = null;  
  FlowBuilderFactory _flowBuilderFactory = null;
  ContainerFactory _containerFactory = null;
  BufferClientFactory _bufferClientFactory = null;
  BufferService _bufferService = null;
  
  // Misc 
  Config _config = new Config();
  Environment _env = null;
  
  // Singleton 
  private static Universe _instance = null;
  
  
  // Package-private constructor.  Only UniverseBuilder should build a universe.
  Universe() {
  }
  

  
  public SSHFactory sshFactory() {
    return this._sshFactory;
  }
  
  public FileFactory fileFactory() {
    return this._fileFactory;
  }
  
  public FlowService flowService() {
    return this._flowService;
  }



  /**
   * 
   */
  public TopService topService() {
    return this._topService;
  }
  
  

  /***
   * 
   */
  public RelationDefFactory relationFactory() {
    return this._relFactory;
  }

  
  public LoggerFactory loggerFactory() {
    return this._loggerFactory;
  }
  
  public Metrics metrics() {
    return this._metrics;
  }

  public BufferClientFactory bufferClientFactory(){
    return this._bufferClientFactory;
  }
  
  public BufferService bufferService(){
    return this._bufferService;
  }
  
  public DFSServiceWrapper dfsService() {
    return new DFSServiceWrapper(this._dfsService);
  }

  
  public Environment env() {
    return this._env;
  }

  
  public CoordinationServiceWrapper state() {
    return _state;
  }



  public APIService api() {
    return _api;
  }



  
  
  public Config config() {
    return _config;
  }




  public AggregationStoreFactory aggregationStoreFactory() {
    return _aggregationStoreFactory;
  }



  public QueueFactory rpcQueueFactory() {
    return _queueFactory;
  }
  
  
  public ErrorStrategyFactory errorStrategyFactory() {
    // don't worry about making this a proper factory until we have more error strategies. 
    return new ErrorStrategyFactory.Strict();
  }


  public BenchmarkFactory benchmarkFactory() {
    return this._benchmarkFactory;
  }

  
  public ContainerFactory containerFactory() {
    return _containerFactory;
  }
  
  public FlowBuilderFactory flowBuilderFactory() {
    return _flowBuilderFactory;
  }
    

  public ShellFactory shellFactory() {
    return this._shellFactory;
  }

  public static Universe instance() {
    if (_instance == null) throw new IllegalStateException("Universe has not been created!");
    return _instance;
  }

  static synchronized void setInstance(Universe uni) {
    _instance = uni;
  }


  public synchronized static void maybeCreate(Universe uni) {
    if (_instance == null) {
      if (uni == null) {
        throw new NullPointerException("given universe is null");
      }
      log.info("Initializing state machine...");
      setInstance(uni);
      log.info("Universe successfully instantiated.");
    }
  }

  
  public synchronized static boolean hasInstance() {
    return _instance != null;
  }
  
}
