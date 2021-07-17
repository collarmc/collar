package com.collarmc.sdht.events;

public interface Publisher {
    void publish(AbstractSDHTEvent event);
}
