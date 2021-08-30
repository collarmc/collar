package com.collarmc.server.services.friends;

import com.collarmc.api.friends.Friend;
import com.collarmc.api.friends.Status;
import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.HttpException.BadRequestException;
import com.collarmc.api.http.HttpException.ServerErrorException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.server.services.profiles.ProfileCache;
import com.collarmc.server.session.SessionManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ArrayListMultimap;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class FriendsService {

    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_FRIEND = "friend";

    private final MongoCollection<Document> docs;
    private final ProfileCache profiles;
    private final SessionManager sessions;

    public FriendsService(MongoDatabase db, ProfileCache profiles, SessionManager sessions) {
        this.docs = db.getCollection("friends");
        this.profiles = profiles;
        Map<String, Object> index = Map.of(
            FIELD_OWNER, 1,
            FIELD_FRIEND, 1
        );
        this.docs.createIndex(new Document(index));
        this.sessions = sessions;
    }

    public AddFriendResponse createFriend(RequestContext context, CreateFriendRequest request) {
        context.assertCallerIs(context.owner);
        if (request.owner == null) {
            throw new BadRequestException("owner");
        }
        if (request.friend == null) {
            throw new BadRequestException("friend");
        }
        Map<String, Object> state = Map.of(
            FIELD_OWNER, request.owner,
            FIELD_FRIEND, request.friend
        );
        InsertOneResult result = docs.insertOne(new Document(state));
        if (!result.wasAcknowledged()) {
            throw new ServerErrorException("could not create friend");
        }
        BsonValue insertedId = result.getInsertedId();
        if (insertedId == null || insertedId.asObjectId() == null) {
            throw new ServerErrorException("could not get upsert id");
        }
        ObjectId value = insertedId.asObjectId().getValue();
        Friend friend = docs.find(eq("_id", value)).map(this::mapFriend).first();
        if (friend == null) {
            throw new HttpException.NotFoundException("couldn't find friend");
        }
        return new AddFriendResponse(friend);
    }

    public DeleteFriendResponse deleteFriend(RequestContext context, DeleteFriendRequest request) {
        context.assertCallerIs(request.owner);
        DeleteResult result = docs.deleteOne(and(eq(FIELD_OWNER, request.owner), eq(FIELD_FRIEND, request.friend)));
        if (!result.wasAcknowledged()) {
            throw new HttpException.NotFoundException("couldn't find friend to delete");
        }
        return new DeleteFriendResponse(request.friend);
    }

    public GetFriendsResponse getFriends(RequestContext context, GetFriendsRequest request) {
        context.assertNotAnonymous();
        FindIterable<Document> documents;
        if (request.byFriend != null) {
            documents = docs.find(eq(FIELD_FRIEND, request.byFriend));
        } else if (request.byOwner != null) {
            documents = docs.find(eq(FIELD_OWNER, request.byOwner));
        } else {
            throw new BadRequestException("use either byOwner or byFriend");
        }
        ArrayListMultimap<UUID, Friend> results = ArrayListMultimap.create();
        documents.forEach(document -> {
            UUID owner = document.get(FIELD_OWNER, UUID.class);
            Friend friend = mapFriend(document);
            results.put(owner, friend);
        });
        return new GetFriendsResponse(results.asMap());
    }

    @Nonnull
    private Friend mapFriend(Document document) {
        UUID owner = document.get(FIELD_OWNER, UUID.class);
        UUID friend = document.get(FIELD_FRIEND, UUID.class);
        PublicProfile profile = profiles.getById(friend).orElseThrow(() -> new IllegalStateException("could not find profile " + friend)).toPublic();
        return sessions.getSessionStateByOwner(friend)
                .filter(sessionState -> sessionState.minecraftPlayer != null)
                .map(sessionState -> new Friend(profile, Status.ONLINE, Set.of(sessionState.minecraftPlayer.id)))
                .orElse(new Friend(profile, Status.OFFLINE, Set.of()));
    }

    public static final class CreateFriendRequest {
        @JsonProperty("owner")
        public final UUID owner;
        @JsonProperty("friend")
        public final UUID friend;

        public CreateFriendRequest(@JsonProperty("owner") UUID owner, @JsonProperty("friend") UUID friend) {
            this.owner = owner;
            this.friend = friend;
        }
    }

    public static final class AddFriendResponse {
        @JsonProperty("friend")
        public final Friend friend;

        public AddFriendResponse(@JsonProperty("friend") Friend friend) {
            this.friend = friend;
        }
    }

    public static final class DeleteFriendRequest {
        @JsonProperty("owner")
        public final UUID owner;
        @JsonProperty("friend")
        public final UUID friend;

        public DeleteFriendRequest(@JsonProperty("owner") UUID owner, @JsonProperty("friend") UUID friend) {
            this.owner = owner;
            this.friend = friend;
        }
    }

    public static final class DeleteFriendResponse {
        @JsonProperty("friend")
        public final UUID friend;

        public DeleteFriendResponse(@JsonProperty("friend") UUID friend) {
            this.friend = friend;
        }
    }

    public static final class GetFriendsRequest {
        @JsonProperty("byOwner")
        public final UUID byOwner;
        @JsonProperty("byFriend")
        public final UUID byFriend;

        public GetFriendsRequest(@JsonProperty("byOwner") UUID byOwner, @JsonProperty("byFriend") UUID byFriend) {
            this.byOwner = byOwner;
            this.byFriend = byFriend;
        }
    }

    public static final class GetFriendsResponse {
        @JsonProperty("friends")
        public final Map<UUID, Collection<Friend>> friends;

        public GetFriendsResponse(@JsonProperty("friends") Map<UUID, Collection<Friend>> friends) {
            this.friends = friends;
        }
    }
}
