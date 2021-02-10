package team.catgirl.collar.server.services.friends;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import team.catgirl.collar.api.friends.Friend;
import team.catgirl.collar.api.friends.Status;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.http.HttpException.BadRequestException;
import team.catgirl.collar.api.http.HttpException.ServerErrorException;
import team.catgirl.collar.server.session.SessionManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class FriendsService {

    public static final String FIELD_OWNER = "owner";
    public static final String FIELD_FRIEND = "friend";

    private final MongoCollection<Document> docs;
    private final SessionManager sessions;

    public FriendsService(MongoDatabase db, SessionManager sessions) {
        this.docs = db.getCollection("friends");
        Map<String, Object> index = Map.of(
            FIELD_OWNER, 1,
            FIELD_FRIEND, 1
        );
        this.docs.createIndex(new Document(index));
        this.sessions = sessions;
    }

    public AddFriendResponse createFriend(CreateFriendRequest request) {
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
        ObjectId value = Objects.requireNonNull(result.getInsertedId()).asObjectId().getValue();
        Friend friend = docs.find(eq("_id", value)).map(this::mapFriend).first();
        if (friend == null) {
            throw new HttpException.NotFoundException("couldn't find friend");
        }
        return new AddFriendResponse(friend);
    }

    public DeleteFriendResponse deleteFriend(DeleteFriendRequest request) {
        DeleteResult result = docs.deleteOne(and(eq(FIELD_OWNER, request.owner), eq(FIELD_FRIEND, request.friend)));
        if (!result.wasAcknowledged()) {
            throw new HttpException.NotFoundException("couldn't find friend to delete");
        }
        return new DeleteFriendResponse(request.friend);
    }

    public GetFriendsResponse getFriends(GetFriendsRequest request) {
        FindIterable<Document> documents;
        if (request.byFriend != null) {
            // TODO: use distinct on owner here?
            documents = docs.find(eq(FIELD_FRIEND, request.byFriend));
        } else if (request.byOwner != null) {
            documents = docs.find(eq(FIELD_OWNER, request.byOwner));
        } else {
            throw new BadRequestException("use either byOwner or byFriend");
        }
        return new GetFriendsResponse(StreamSupport.stream(documents.map(this::mapFriend).spliterator(), false).collect(Collectors.toList()));
    }

    private Friend mapFriend(Document document) {
        UUID friend = document.get(FIELD_FRIEND, UUID.class);
        return sessions.getSessionStateByOwner(friend)
                .map(sessionState -> new Friend(friend, Status.ONLINE, List.of(sessionState.player.id)))
                .orElse(new Friend(friend, Status.OFFLINE, List.of()));
    }

    public static final class CreateFriendRequest {
        public final UUID owner;
        public final UUID friend;

        public CreateFriendRequest(UUID owner, UUID friend) {
            this.owner = owner;
            this.friend = friend;
        }
    }

    public static final class AddFriendResponse {
        public final Friend friend;

        public AddFriendResponse(Friend friend) {
            this.friend = friend;
        }
    }

    public static final class DeleteFriendRequest {
        public final UUID owner;
        public final UUID friend;

        public DeleteFriendRequest(UUID owner, UUID friend) {
            this.owner = owner;
            this.friend = friend;
        }
    }

    public static final class DeleteFriendResponse {
        public final UUID friend;

        public DeleteFriendResponse(UUID friend) {
            this.friend = friend;
        }
    }

    public static final class GetFriendsRequest {
        public final UUID byOwner;
        public final UUID byFriend;

        public GetFriendsRequest(UUID byOwner, UUID byFriend) {
            this.byOwner = byOwner;
            this.byFriend = byFriend;
        }
    }

    public static final class GetFriendsResponse {
        public final List<Friend> friends;

        public GetFriendsResponse(List<Friend> friends) {
            this.friends = friends;
        }
    }
}
