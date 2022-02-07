package com.atguigu.netty.proto;

import java.util.EventObject;

public class UserEvent extends EventObject {

  /**
   * Constructs a prototypical Event.
   *
   * @param source The object on which the Event initially occurred.
   * @throws IllegalArgumentException if source is null.
   */
  public UserEvent(Object source) {
    super(source);
  }
}
