package com.auction.model.entities;

import java.io.Serializable;

/**
 * .
 */
public abstract class Entity implements Serializable {
  private String id;
  private static final long serialVersionUID = 1L;

  public Entity(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void displayInfo() {
  }
}
