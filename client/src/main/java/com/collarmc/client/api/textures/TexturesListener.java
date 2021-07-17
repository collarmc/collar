package com.collarmc.client.api.textures;

import com.collarmc.client.Collar;
import com.collarmc.client.api.ApiListener;

public interface TexturesListener extends ApiListener {
    void onTextureReceived(Collar collar, TexturesApi texturesApi, Texture texture);
}
