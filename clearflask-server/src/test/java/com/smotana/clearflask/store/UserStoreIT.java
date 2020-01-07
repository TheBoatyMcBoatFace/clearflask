package com.smotana.clearflask.store;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.smotana.clearflask.api.model.UserSearchAdmin;
import com.smotana.clearflask.api.model.UserUpdate;
import com.smotana.clearflask.store.UserStore.User;
import com.smotana.clearflask.store.UserStore.UserSession;
import com.smotana.clearflask.store.dynamo.InMemoryDynamoDbProvider;
import com.smotana.clearflask.store.dynamo.mapper.DynamoMapperImpl;
import com.smotana.clearflask.store.impl.DynamoElasticUserStore;
import com.smotana.clearflask.testutil.AbstractIT;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.Assert.*;

@Slf4j
public class UserStoreIT extends AbstractIT {

    @Inject
    private UserStore store;

    @Override
    protected void configure() {
        super.configure();

        install(InMemoryDynamoDbProvider.module());
        install(DynamoMapperImpl.module());
        install(DynamoElasticUserStore.module());

    }

    @Test(timeout = 5_000L)
    public void testUser() throws Exception {
        User user = new User(
                "myProjectId",
                "myUserId",
                "john",
                "john.doe@example.com",
                "password",
                true,
                BigDecimal.ONE,
                "myIosPushToken",
                "myAndroidPushToken",
                "myBrowserPushToken");

        store.createUser(user).getIndexingFuture().get();
        assertTrue(store.getUser(user.getProjectId(), user.getUserId()).isPresent());
        assertEquals(user, store.getUser(user.getProjectId(), user.getUserId()).get());
        assertTrue(store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.EMAIL, user.getEmail()).isPresent());
        assertEquals(user, store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.EMAIL, user.getEmail()).get());
        assertTrue(store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.ANDROID_PUSH, user.getAndroidPushToken()).isPresent());
        assertEquals(user, store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.ANDROID_PUSH, user.getAndroidPushToken()).get());
        assertTrue(store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.BROWSER_PUSH, user.getBrowserPushToken()).isPresent());
        assertEquals(user, store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.BROWSER_PUSH, user.getBrowserPushToken()).get());
        assertTrue(store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.IOS_PUSH, user.getIosPushToken()).isPresent());
        assertEquals(user, store.getUserByIdentifier(user.getProjectId(), UserStore.IdentifierType.IOS_PUSH, user.getIosPushToken()).get());

        assertEquals(ImmutableList.of(user), store.getUsers(user.getProjectId(), user.getUserId()));

        User userUpdated = user.toBuilder()
                .name("joe")
                .email("joe.doe@example.com")
                .password("password2")
                .emailNotify(false)
                .iosPushToken("myIosPushToken2")
                .androidPushToken("myAndroidPushToken2")
                .browserPushToken("myBrowserPushToken2")
                .build();
        store.updateUser(user.getProjectId(), user.getUserId(), UserUpdate.builder()
                .name(userUpdated.getName())
                .email(userUpdated.getEmail())
                .password(userUpdated.getPassword())
                .emailNotify(userUpdated.isEmailNotify())
                .iosPushToken(userUpdated.getIosPushToken())
                .androidPushToken(userUpdated.getAndroidPushToken())
                .browserPushToken(userUpdated.getBrowserPushToken())
                .build());
        assertEquals(userUpdated, store.getUser(userUpdated.getProjectId(), userUpdated.getUserId()).get());

        store.deleteUsers(userUpdated.getProjectId(), userUpdated.getUserId());
        assertFalse(store.getUser(userUpdated.getProjectId(), userUpdated.getUserId()).isPresent());
    }

    @Test(timeout = 5_000L)
    public void testSearchUsers() throws Exception {
        String projectId = UUID.randomUUID().toString();
        User user1 = new User(
                projectId,
                "myUserId1",
                "john",
                "john.doe@example.com",
                "password",
                true,
                BigDecimal.ONE,
                "myIosPushToken1",
                "myAndroidPushToken1",
                "myBrowserPushToken1");
        User user2 = new User(
                projectId,
                "myUserId2",
                "matt",
                "matt@example.com",
                "jilasjdklad",
                true,
                BigDecimal.ONE,
                "myIosPushToken2",
                "myAndroidPushToken2",
                "myBrowserPushToken2");
        User user3 = new User(
                projectId,
                "myUserId3",
                "Bobby",
                "bobby@example.com",
                "fawferfva",
                true,
                BigDecimal.ONE,
                "myIosPushToken3",
                "myAndroidPushToken3",
                "myBrowserPushToken3");

        store.createUser(user1).getIndexingFuture().get();
        store.createUser(user2).getIndexingFuture().get();
        store.createUser(user3).getIndexingFuture().get();

        log.info("DEBUG searchUsers {}", store.searchUsers(projectId, UserSearchAdmin.builder()
                .searchText("john").build()).size());
        log.info("DEBUG searchUsers {}", store.searchUsers(projectId, UserSearchAdmin.builder()
                .searchText("bobby@example.com").build()).size());
        log.info("DEBUG searchUsers {}", store.searchUsers(projectId, UserSearchAdmin.builder()
                .searchText("matt@example").build()).size());
        log.info("DEBUG searchUsers {}", store.searchUsers(projectId, UserSearchAdmin.builder()
                .searchText("myUserId1").build()).size());
        assertEquals(1, store.searchUsers(projectId, UserSearchAdmin.builder().searchText("bobby").build()).size());
        // TODO
    }

    @Test(timeout = 5_000L)
    public void testUserSession() throws Exception {
        User user = new User(
                "myProjectId",
                "myUserId",
                "john",
                "john.doe@example.com",
                "password",
                true,
                BigDecimal.ONE,
                "myIosPushToken",
                "myAndroidPushToken",
                "myBrowserPushToken");
        store.createUser(user).getIndexingFuture().get();

        UserSession session1 = store.createSession(user.getProjectId(), user.getUserId(), Instant.ofEpochMilli(System.currentTimeMillis()).plus(1, ChronoUnit.DAYS));
        log.info("Created session 1 {}", session1);
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session1.getSessionId()).isPresent());
        assertEquals(session1, store.getSession(user.getProjectId(), user.getUserId(), session1.getSessionId()).get());

        UserSession session2 = store.createSession(user.getProjectId(), user.getUserId(), Instant.ofEpochMilli(System.currentTimeMillis()).plus(1, ChronoUnit.DAYS));
        log.info("Created session 2 {}", session2);
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session2.getSessionId()).isPresent());
        assertEquals(session2, store.getSession(user.getProjectId(), user.getUserId(), session2.getSessionId()).get());
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session1.getSessionId()).isPresent());

        store.revokeSession(user.getProjectId(), user.getUserId(), session1.getSessionId());
        assertFalse(store.getSession(user.getProjectId(), user.getUserId(), session1.getSessionId()).isPresent());
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session2.getSessionId()).isPresent());

        UserSession session3 = store.createSession(user.getProjectId(), user.getUserId(), Instant.ofEpochMilli(System.currentTimeMillis()).plus(1, ChronoUnit.DAYS));
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).isPresent());
        assertEquals(session3, store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).get());
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session2.getSessionId()).isPresent());

        store.revokeSessions(user.getProjectId(), user.getUserId(), session3.getSessionId());
        assertFalse(store.getSession(user.getProjectId(), user.getUserId(), session2.getSessionId()).isPresent());
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).isPresent());

        Instant refreshedExpiry = Instant.ofEpochMilli(System.currentTimeMillis()).plus(2, ChronoUnit.DAYS);
        store.refreshSession(user.getProjectId(), user.getUserId(), session3.getSessionId(), refreshedExpiry);
        assertTrue(store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).isPresent());
        assertEquals(refreshedExpiry, store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).get().getExpiry());

        store.revokeSessions(user.getProjectId(), user.getUserId());
        assertFalse(store.getSession(user.getProjectId(), user.getUserId(), session3.getSessionId()).isPresent());
    }
}