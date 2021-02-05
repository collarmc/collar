package team.catgirl.collar.tests.junit;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class CollarAssert {

    public static void waitForCondition(String name, Supplier<Boolean> condition) throws InterruptedException {
        long future = TimeUnit.MINUTES.toMillis(1) + System.currentTimeMillis();
        while (System.currentTimeMillis() < future) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(200);
        }
        Assert.fail("waitForCondition '" + name + "' failed");
    }

    public CollarAssert() {}
}
