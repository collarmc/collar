package com.collarmc.client.sdht.event;

import com.collarmc.client.Collar;
import com.collarmc.client.events.AbstractCollarEvent;
import com.collarmc.sdht.Content;
import com.collarmc.sdht.Key;

public final class SDHTRecordRemovedEvent extends AbstractCollarEvent {
    public final Key key;
    public final Content content;

    public SDHTRecordRemovedEvent(Collar collar, Key key, Content content) {
        super(collar);
        this.key = key;
        this.content = content;
    }
}
