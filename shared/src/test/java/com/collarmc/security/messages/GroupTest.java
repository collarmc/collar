package com.collarmc.security.messages;

public class GroupTest {

//    @Test
//    public void testGroupChat() throws Exception {
//        // Create some identities
//        IdentityStoreImpl aliceStore = new IdentityStoreImpl();
//        IdentityStoreImpl bobStore = new IdentityStoreImpl();
//        IdentityStoreImpl eveStore = new IdentityStoreImpl();
//
//        // Bob and Alice
//        ClientIdentity bob = bobStore.identity();
//        ClientIdentity alice = aliceStore.identity();
//        ClientIdentity eve = eveStore.identity();
//
//        // Trust each other
//        aliceStore.trustIdentity(bob);
//        aliceStore.trustIdentity(eve);
//        bobStore.trustIdentity(alice);
//        bobStore.trustIdentity(eve);
//        eveStore.trustIdentity(bob);
//        eveStore.trustIdentity(alice);
//
//        UUID group = UUID.randomUUID();
//
//        Set<ClientIdentity> members = new HashSet<>();
//        members.add(alice);
//        members.add(bob);
//
//        GroupSession aliceGroupSession = new GroupSession(aliceStore.identity(), group, aliceStore, aliceStore.messagingPublicKey, aliceStore.identityPublicKey, members);
//        GroupSession bobGroupSession = new GroupSession(bobStore.identity(), group, bobStore, bobStore.messagingPublicKey, bobStore.identityPublicKey, members);
//        GroupSession eveGroupSession = new GroupSession(eveStore.identity(), group, eveStore, eveStore.messagingPublicKey, eveStore.identityPublicKey, members);
//
//        List<GroupMessageEnvelope> envelopes = aliceGroupSession.encrypt("hello world!!!".getBytes(StandardCharsets.UTF_8));
//        GroupMessageEnvelope bobsMessage = envelopes.stream().filter(envelope -> envelope.recipient.id().equals(bob.id())).findFirst().orElseThrow(IllegalStateException::new);
//
//        // Bob can receive message from alice
//        GroupMessage decodedMessage = bobGroupSession.decrypt(bobsMessage.message, alice);
//        Assert.assertEquals(group, decodedMessage.group);
//        Assert.assertEquals(GroupSession.createSessionId(group, members), decodedMessage.sessionId);
//        Assert.assertEquals("hello world!!!", new String(decodedMessage.contents, StandardCharsets.UTF_8));
//
//        // Eve should not be able to read message from bob
//        try {
//            GroupMessageEnvelope aliceMessage = envelopes.stream().filter(envelope -> envelope.recipient.id().equals(bob.id())).findFirst().orElseThrow(IllegalStateException::new);
//            eveGroupSession.decrypt(aliceMessage.message, alice);
//            Assert.fail("bob should not be able to read message intended for alice");
//        } catch (CipherException ignored) {}
//    }
}
