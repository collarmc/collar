package team.catgirl.collar.server.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;

public final class Mongo {
    private static MongoDatabase database;

    public static MongoDatabase database() {
        if (database == null) {
            String mongoUrl = System.getenv("MONGODB_URI");
            database = mongoUrl == null ? getDevelopmentDatabase() : database(mongoUrl);
        }
        return database;
    }

    public static MongoDatabase database(String mongoUrl) {
        ConnectionString uri = new ConnectionString(mongoUrl);
        MongoClient mongoClient = MongoClients.create(settings(uri));
        String db = uri.getDatabase();
        if (db == null) {
            throw new IllegalStateException("MONGODB_URI did not include database name");
        }
        return mongoClient.getDatabase(db);
    }

    public static MongoDatabase getTestingDatabase() {
        return MongoClients.create(settings(null)).getDatabase("collar-testing");
    }
    private static MongoDatabase getDevelopmentDatabase() {
        return MongoClients.create(settings(null)).getDatabase("collar-dev");
    }

    private static MongoClientSettings settings(ConnectionString uri) {
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .uuidRepresentation(UuidRepresentation.STANDARD);
        if (uri != null) {
            builder.applyConnectionString(uri);
        }
        return builder.build();
    }

    private Mongo() {}
}
