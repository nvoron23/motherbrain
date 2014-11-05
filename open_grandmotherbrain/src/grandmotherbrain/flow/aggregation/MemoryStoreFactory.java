package grandmotherbrain.flow.aggregation;

import grandmotherbrain.flow.operations.AggregationOperation;

public class MemoryStoreFactory implements AggregationStoreFactory {

  /**
   * 
   */
  private static final long serialVersionUID = 518330658805872632L;

  @Override
  public AggregationStoreWrapper getStore(AggregationOperation op, String prefix) {
    return new AggregationStoreWrapper(new MemoryStore());
  }

}
