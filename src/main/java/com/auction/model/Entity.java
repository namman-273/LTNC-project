package com.auction.model;
import java.io.Serializable;

public abstract class Entity implements java.io.Serializable {
    protected String id;

    public Entity(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public void displayInfo(){};
}
