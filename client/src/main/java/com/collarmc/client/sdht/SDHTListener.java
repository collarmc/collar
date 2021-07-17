package com.collarmc.client.sdht;

import com.collarmc.client.Collar;
import com.collarmc.client.api.ApiListener;
import com.collarmc.sdht.Content;
import com.collarmc.sdht.Key;

public interface SDHTListener extends ApiListener {
    default void onRecordAdded(Collar collar, SDHTApi sdhtApi, Key key, Content content) {}

    default void onRecordRemoved(Collar collar, SDHTApi sdhtApi, Key key, Content content) {}
}
