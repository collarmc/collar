package com.collarmc.sdht;

public interface DistributedHashTableListener {
    void onAdd(Key key, Content content);
    void onRemove(Key key, Content content);
}
