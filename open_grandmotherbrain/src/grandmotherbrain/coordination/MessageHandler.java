package grandmotherbrain.coordination;

import grandmotherbrain.flow.operations.OperationException;

public interface MessageHandler {

  /***
   * handles new message
   * @param key
   * @throws OperationException 
   * @throws InterruptedException 
   * @throws CoordinationException 
   */
  public void handleNewMessage(String key, Object payload) throws Exception;
 
  
}
