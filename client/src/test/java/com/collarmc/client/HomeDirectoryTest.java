package com.collarmc.client;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class HomeDirectoryTest {
    @Test
    public void lock() throws IOException {
        File home = Files.createTempDir();
        HomeDirectory home1 = HomeDirectory.from(home, "localhost");
        HomeDirectory home2 = HomeDirectory.from(home, "localhost");

        Assert.assertTrue(home1.getLock().tryLock());
        Assert.assertFalse(home2.getLock().tryLock());
        home1.getLock().unlock();
        Assert.assertTrue(home2.getLock().tryLock());
        home2.getLock().unlock();
    }
}
