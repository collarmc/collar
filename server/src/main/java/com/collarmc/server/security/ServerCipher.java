package com.collarmc.server.security;

import org.whispersystems.libsignal.state.SignalProtocolStore;
import com.collarmc.api.groups.Group;
import com.collarmc.security.Identity;
import com.collarmc.security.cipher.AbstractCipher;
import com.collarmc.security.cipher.CipherException;
import com.collarmc.security.cipher.CipherException.UnavailableCipherException;

public final class ServerCipher extends AbstractCipher {
    public ServerCipher(SignalProtocolStore signalProtocolStore) {
        super(signalProtocolStore);
    }

    @Override
    public byte[] crypt(byte[] bytes) throws CipherException {
        throw new UnavailableCipherException();
    }

    @Override
    public byte[] decrypt(byte[] bytes) throws CipherException {
        throw new UnavailableCipherException();
    }

    @Override
    public byte[] crypt(Identity sender, Group recipient, byte[] bytes) throws CipherException {
        throw new UnavailableCipherException();
    }

    @Override
    public byte[] decrypt(Identity sender, Group group, byte[] bytes) throws CipherException {
        throw new UnavailableCipherException();
    }
}
