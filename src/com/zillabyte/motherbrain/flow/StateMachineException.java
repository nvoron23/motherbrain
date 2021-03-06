package com.zillabyte.motherbrain.flow;

import com.zillabyte.motherbrain.top.MotherbrainException;

public class StateMachineException extends MotherbrainException {

  private static final long serialVersionUID = -8809690626288066286L;

  public StateMachineException() {
    super();
  }
  
  public StateMachineException(String msg) {
    super(msg);
  }
  
  public StateMachineException(Throwable ex) {
    super(ex);
  }
  
}
