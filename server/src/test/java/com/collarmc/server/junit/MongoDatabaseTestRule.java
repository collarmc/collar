package com.collarmc.server.junit;

import com.mongodb.client.MongoDatabase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import com.collarmc.server.mongo.Mongo;

public class MongoDatabaseTestRule implements TestRule {

    public MongoDatabase db;

    @Override
    public Statement apply(Statement base, Description description) {
        Mongo.getTestingDatabase().drop();
        db = Mongo.getTestingDatabase();
        try {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                }
            };
        } finally {
            db.drop();
        }
    }
}
