package team.catgirl.collar.server.services.profiles;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.bson.BsonObjectId;
import org.bson.Document;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ConflictException;
import team.catgirl.collar.api.http.HttpException.NotFoundException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.server.http.RequestContext;
import team.catgirl.collar.server.security.hashing.PasswordHashing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class ProfileService {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_PROFILE_ID = "profileId";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HASHED_PASSWORD = "hashedPassword";

    private final MongoCollection<Document> docs;
    private final PasswordHashing passwordHashing;

    public ProfileService(MongoDatabase db, PasswordHashing passwordHashing) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_PROFILE_ID, 1);
        index.put(FIELD_EMAIL, 1);
        docs.createIndex(new Document(index));
        this.passwordHashing = passwordHashing;
    }

    public CreateProfileResponse createProfile(RequestContext context, CreateProfileRequest req) {
        context.assertAnonymous();
        if (docs.find(eq(FIELD_EMAIL, req.email)).iterator().hasNext()) {
            throw new ConflictException("profile with this email already exists");
        }
        if (req.name == null) {
            throw new BadRequestException("name missing");
        }
        if (req.email == null) {
            throw new BadRequestException("email missing");
        }
        if (req.password == null) {
            throw new BadRequestException("password missing");
        }
        if (EmailValidator.getInstance().isValid(req.email)) {
            throw new BadRequestException("email address is invalid");
        }
        String hashedPassword = passwordHashing.hash(req.password);
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_PROFILE_ID, UUID.randomUUID());
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_EMAIL, req.email.toLowerCase());
        state.put(FIELD_HASHED_PASSWORD, hashedPassword);
        InsertOneResult insertOneResult = docs.insertOne(new Document(state));
        if (insertOneResult.wasAcknowledged()) {
            BsonObjectId id = Objects.requireNonNull(insertOneResult.getInsertedId()).asObjectId();
            MongoCursor<Document> cursor = docs.find(eq(FIELD_ID, id.getValue())).iterator();
            if (cursor.hasNext()) {
                return new CreateProfileResponse(map(cursor.next()));
            } else {
                throw new ServerErrorException("could not find created profile");
            }
        } else {
            throw new ServerErrorException("could not create new profile");
        }
    }

    public GetProfileResponse getProfile(RequestContext context, GetProfileRequest req) {
        context.assertNotAnonymous();
        MongoCursor<Document> cursor;
        if (req.byId != null) {
            cursor = docs.find(eq(FIELD_PROFILE_ID, req.byId)).iterator();
        } else if (req.byEmail != null) {
            cursor = docs.find(eq(FIELD_EMAIL, req.byEmail.toLowerCase())).iterator();
        } else {
            throw new BadRequestException("empty request");
        }
        if (!cursor.hasNext()) {
            throw new NotFoundException("profile not found");
        }
        Document doc = cursor.next();
        return new GetProfileResponse(map(doc));
    }

    public static class CreateProfileRequest {
        public final String email;
        public final String password;
        public final String name;

        public CreateProfileRequest(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    public static class CreateProfileResponse {
        public final Profile profile;

        public CreateProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    public static class GetProfileRequest {
        public final UUID byId;
        public final String byEmail;

        public GetProfileRequest(UUID byId, String byEmail) {
            this.byId = byId;
            this.byEmail = byEmail;
        }

        public static GetProfileRequest byId(UUID uuid) {
            return new GetProfileRequest(uuid, null);
        }

        public static GetProfileRequest byEmail(String email) {
            return new GetProfileRequest(null, email);
        }
    }

    public static class GetProfileResponse {
        public final Profile profile;

        public GetProfileResponse(Profile profile) {
            this.profile = profile;
        }
    }

    private static Profile map(Document doc) {
        UUID profileId = doc.get(FIELD_PROFILE_ID, UUID.class);
        String email = doc.getString(FIELD_EMAIL);
        String name = doc.getString(FIELD_NAME);
        String hashedPassword = doc.getString(FIELD_HASHED_PASSWORD);
        return new Profile(profileId, email, name, hashedPassword);
    }
}
