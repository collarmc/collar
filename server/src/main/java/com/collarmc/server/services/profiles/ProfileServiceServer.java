package com.collarmc.server.services.profiles;

import com.collarmc.api.groups.Group;
import com.collarmc.api.http.HttpException.BadRequestException;
import com.collarmc.api.http.HttpException.ConflictException;
import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.HttpException.ServerErrorException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.Role;
import com.collarmc.api.profiles.TexturePreference;
import com.collarmc.api.security.PublicKey;
import com.collarmc.server.security.hashing.PasswordHashing;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.push;

public class ProfileServiceServer implements ProfileService {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_PROFILE_ID = "profileId";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_HASHED_PASSWORD = "hashedPassword";
    private static final String FIELD_EMAIL_VERIFIED = "emailVerified";
    private static final String FIELD_CAPE_TEXTURE = "capeTexture";
    private static final String FIELD_CAPE_TEXTURE_ID = "texture";
    private static final String FIELD_KNOWN_ACCOUNTS = "knownAccounts";
    private static final String FIELD_ROLES = "roles";
    private static final String FIELD_PUBLIC_KEY = "publicKey";

    private final MongoCollection<Document> docs;
    private final PasswordHashing passwordHashing;

    public ProfileServiceServer(MongoDatabase db, PasswordHashing passwordHashing) {
        this.docs = db.getCollection("profiles");
        Map<String, Object> index = new HashMap<>();
        index.put(FIELD_PROFILE_ID, 1);
        index.put(FIELD_EMAIL, 1);
        docs.createIndex(new Document(index));
        this.passwordHashing = passwordHashing;
    }

    @Override
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
        if (!EmailValidator.getInstance().isValid(req.email)) {
            throw new BadRequestException("email address is invalid");
        }
        String hashedPassword = passwordHashing.hash(req.password);
        Map<String, Object> state = new HashMap<>();
        state.put(FIELD_PROFILE_ID, UUID.randomUUID());
        state.put(FIELD_NAME, req.name);
        state.put(FIELD_EMAIL, req.email.toLowerCase());
        state.put(FIELD_HASHED_PASSWORD, hashedPassword);
        state.put(FIELD_EMAIL_VERIFIED, false);
        state.put(FIELD_ROLES, List.of(Role.PLAYER.name()));
        InsertOneResult insertOneResult = docs.insertOne(new Document(state));
        if (insertOneResult.wasAcknowledged()) {
            BsonValue insertedId = insertOneResult.getInsertedId();
            if (insertedId == null || insertedId.asObjectId() == null) {
                throw new ServerErrorException("could not get upsert id");
            }
            BsonObjectId id = insertedId.asObjectId();
            MongoCursor<Document> cursor = docs.find(eq(FIELD_ID, id.getValue())).iterator();
            if (cursor.hasNext()) {
                return new CreateProfileResponse(mapToProfile(cursor.next()));
            } else {
                throw new ServerErrorException("could not find created profile");
            }
        } else {
            throw new ServerErrorException("could not create new profile");
        }
    }

    @Override
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
        return new GetProfileResponse(mapToProfile(doc));
    }

    @Override
    public UpdateProfileResponse updateProfile(RequestContext context, UpdateProfileRequest req) {
        context.assertNotAnonymous();
        UpdateResult result;
        if (req.emailVerified != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_EMAIL_VERIFIED, req.emailVerified)));
        } else if (req.hashedPassword != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_HASHED_PASSWORD, req.hashedPassword)));
        } else if (req.publicKey != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(Map.of(FIELD_PUBLIC_KEY, new Binary(req.publicKey.key)))));
        } else if (req.cape != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), new Document("$set", new Document(FIELD_CAPE_TEXTURE, map(req.cape))));
        } else if (req.addMinecraftAccount != null) {
            result = docs.updateOne(eq(FIELD_PROFILE_ID, req.profile), push(FIELD_KNOWN_ACCOUNTS, req.addMinecraftAccount));
        } else {
            throw new BadRequestException("bad request");
        }
        if (result.wasAcknowledged()) {
            Document first = docs.find(eq(FIELD_PROFILE_ID, req.profile)).first();
            if (first == null) {
                throw new NotFoundException("could not find profile");
            }
            return new UpdateProfileResponse(mapToProfile(first));
        } else {
            throw new ServerErrorException("could not update profile");
        }
    }

    @Override
    public PlayerCountResponse playerCount(RequestContext context, PlayerCountRequest req) {
        return new PlayerCountResponse(docs.countDocuments());
    }

    @Override
    public GetProfilesResponse getProfiles(RequestContext context, GetProfilesRequest req) {
        context.assertNotAnonymous();
        if (req.byMinecraftId != null) {
            MongoCursor<Profile> profiles = docs.find(elemMatch(FIELD_KNOWN_ACCOUNTS, eq(req.byMinecraftId))).map(ProfileServiceServer::mapToProfile).cursor();
            return new GetProfilesResponse(StreamSupport.stream(Spliterators.spliteratorUnknownSize(profiles, Spliterator.ORDERED), false).collect(Collectors.toList()));
        } else {
            throw new BadRequestException("did not specify byMinecraftId");
        }
    }

    private static Profile mapToProfile(Document doc) {
        UUID profileId = doc.get(FIELD_PROFILE_ID, UUID.class);
        String email = doc.getString(FIELD_EMAIL);
        String name = doc.getString(FIELD_NAME);
        String hashedPassword = doc.getString(FIELD_HASHED_PASSWORD);
        boolean emailVerified = doc.getBoolean(FIELD_EMAIL_VERIFIED, false);
        TexturePreference capeTexture = mapTexturePreference(doc.get(FIELD_CAPE_TEXTURE, Document.class));
        Binary publicKeyBinary = doc.get(FIELD_PUBLIC_KEY, Binary.class);
        List<UUID> knownAccounts = doc.getList(FIELD_KNOWN_ACCOUNTS, UUID.class);
        Set<Role> roles = doc.getList(FIELD_ROLES, String.class, List.of()).stream().map(Role::valueOf).collect(Collectors.toSet());
        PublicKey publicKey = publicKeyBinary == null || publicKeyBinary.getData().length == 0 ? null : new PublicKey(publicKeyBinary.getData());
        return new Profile(
                profileId,
                roles,
                email,
                name,
                hashedPassword,
                emailVerified,
                capeTexture,
                knownAccounts == null ? Set.of() : Set.copyOf(knownAccounts),
                publicKey
        );
    }

    private static TexturePreference mapTexturePreference(Document doc) {
        if (doc == null) {
            return null;
        }
        return new TexturePreference(doc.get(FIELD_CAPE_TEXTURE_ID, UUID.class));
    }

    private Document map(TexturePreference capeTexture) {
        return new Document(Map.of(
            FIELD_CAPE_TEXTURE_ID, capeTexture.texture
        ));
    }
}
