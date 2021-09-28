package com.smotana.clearflask.store.github;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.jayway.jsonpath.JsonPath;
import com.kik.config.ice.ConfigSystem;
import com.kik.config.ice.annotations.DefaultValue;
import com.smotana.clearflask.api.model.AvailableRepo;
import com.smotana.clearflask.api.model.AvailableRepos;
import com.smotana.clearflask.api.model.Category;
import com.smotana.clearflask.api.model.CommentUpdate;
import com.smotana.clearflask.api.model.ConfigAdmin;
import com.smotana.clearflask.api.model.GitHubStatusSync;
import com.smotana.clearflask.api.model.IdeaStatus;
import com.smotana.clearflask.api.model.IdeaUpdateAdmin;
import com.smotana.clearflask.core.ManagedService;
import com.smotana.clearflask.store.CommentStore;
import com.smotana.clearflask.store.CommentStore.CommentAndIndexingFuture;
import com.smotana.clearflask.store.CommentStore.CommentModel;
import com.smotana.clearflask.store.GitHubStore;
import com.smotana.clearflask.store.IdeaStore;
import com.smotana.clearflask.store.IdeaStore.IdeaAndIndexingFuture;
import com.smotana.clearflask.store.IdeaStore.IdeaModel;
import com.smotana.clearflask.store.ProjectStore;
import com.smotana.clearflask.store.ProjectStore.Project;
import com.smotana.clearflask.store.UserStore;
import com.smotana.clearflask.store.UserStore.UserModel;
import com.smotana.clearflask.store.dynamo.DynamoUtil;
import com.smotana.clearflask.store.dynamo.mapper.DynamoMapper;
import com.smotana.clearflask.store.dynamo.mapper.DynamoMapper.TableSchema;
import com.smotana.clearflask.store.impl.DynamoElasticUserStore;
import com.smotana.clearflask.util.Extern;
import com.smotana.clearflask.util.MarkdownAndQuillUtil;
import com.smotana.clearflask.web.ApiException;
import com.smotana.clearflask.web.Application;
import com.smotana.clearflask.web.resource.GitHubResource;
import com.smotana.clearflask.web.security.Sanitizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload.Issue;
import org.kohsuke.github.GHEventPayload.IssueComment;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.smotana.clearflask.store.dynamo.DefaultDynamoDbProvider.DYNAMO_WRITE_BATCH_MAX_SIZE;

@Slf4j
@Singleton
public class GitHubStoreImpl extends ManagedService implements GitHubStore {

    public final static String USER_GUID_GITHUB_PREFIX = "gh-";

    public interface Config {
        @DefaultValue("true")
        boolean enabled();

        @DefaultValue("Iv1.4c1c98e9e6c71cae")
        String clientId();

        @DefaultValue("")
        String clientSecret();

        @DefaultValue("P1D")
        Duration authExpiry();
    }

    @Inject
    private Config config;
    @Inject
    private GitHubResource.Config configGitHubResource;
    @Inject
    private Application.Config configApp;
    @Inject
    private Gson gson;
    @Inject
    private GitHubClientProvider gitHubClientProvider;
    @Inject
    private MarkdownAndQuillUtil markdownAndQuillUtil;
    @Inject
    private AmazonDynamoDB dynamo;
    @Inject
    private DynamoDB dynamoDoc;
    @Inject
    private DynamoMapper dynamoMapper;
    @Inject
    private DynamoUtil dynamoUtil;
    @Inject
    private IdeaStore ideaStore;
    @Inject
    private CommentStore commentStore;
    @Inject
    private ProjectStore projectStore;
    @Inject
    private UserStore userStore;
    @Inject
    private Sanitizer sanitizer;

    private final JsonPath changesBodyJsonPath = JsonPath.compile("changes.body");
    private TableSchema<GitHubAuthorization> gitHubAuthorizationSchema;
    private ListeningExecutorService executor;

    @Override
    protected void serviceStart() throws Exception {
        gitHubAuthorizationSchema = dynamoMapper.parseTableSchema(GitHubAuthorization.class);

        executor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(
                2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("GitHubStoreImpl-worker-%d").build()));
    }

    @Override
    protected void serviceStop() throws Exception {
        executor.shutdownNow();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Extern
    @Override
    public AvailableRepos getReposForUser(String accountId, String code) {
        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            throw new ApiException(Response.Status.SERVICE_UNAVAILABLE, "GitHub integration is disabled");
        }
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost reqAuthorize = new HttpPost("https://github.com/login/oauth/access_token");
            reqAuthorize.setHeader("Accept", "application/json");
            reqAuthorize.setEntity(new UrlEncodedFormEntity(ImmutableList.of(
                    new BasicNameValuePair("grant_type", "authorization_code"),
                    new BasicNameValuePair("client_id", config.clientId()),
                    new BasicNameValuePair("client_secret", config.clientSecret()),
                    new BasicNameValuePair("redirect_uri", "https://" + configApp.domain() + "/dashboard/settings/project/github"),
                    new BasicNameValuePair("code", code)),
                    Charsets.UTF_8));
            DynamoElasticUserStore.OAuthAuthorizationResponse oAuthAuthorizationResponse;
            try (CloseableHttpResponse res = client.execute(reqAuthorize)) {
                if (res.getStatusLine().getStatusCode() < 200
                        || res.getStatusLine().getStatusCode() > 299) {
                    log.info("GitHub provider failed authorization for repos, url {} response status {}",
                            reqAuthorize.getURI(), res.getStatusLine().getStatusCode());
                    throw new ApiException(Response.Status.FORBIDDEN, "Failed to authorize");
                }
                try {
                    oAuthAuthorizationResponse = gson.fromJson(new InputStreamReader(res.getEntity().getContent(), StandardCharsets.UTF_8), DynamoElasticUserStore.OAuthAuthorizationResponse.class);
                } catch (JsonSyntaxException | JsonIOException ex) {
                    log.warn("GitHub provider authorization response cannot parse, url {} response status {}",
                            reqAuthorize.getURI(), res.getStatusLine().getStatusCode(), ex);
                    throw new ApiException(Response.Status.SERVICE_UNAVAILABLE, "Failed to fetch", ex);
                }
            }
            GitHub userClient = gitHubClientProvider.getOauthClient(oAuthAuthorizationResponse.getAccessToken());
            ImmutableMap.Builder<Long, Long> repositoryAndInstallationIdsBuilder = ImmutableMap.builder();
            ImmutableList.Builder<AvailableRepo> availableReposBuilder = ImmutableList.builder();
            for (GHAppInstallation installation : userClient.getMyself().getAppInstallations()) {
                GitHub installationClient = gitHubClientProvider.getInstallationClient(installation.getId()).getClient();
                for (GHRepository repository : installationClient.getApp().getInstallationById(installation.getId()).listRepositories()) {
                    availableReposBuilder.add(new AvailableRepo(
                            repository.getId(),
                            repository.getFullName()));
                    repositoryAndInstallationIdsBuilder.put(
                            repository.getId(),
                            installation.getId());
                }
            }
            authorizeAccountForRepos(accountId, repositoryAndInstallationIdsBuilder.build());
            return new AvailableRepos(availableReposBuilder.build());
        } catch (IOException ex) {
            throw new ApiException(Response.Status.FORBIDDEN, "Failed to authorize", ex);
        }
    }

    private void authorizeAccountForRepos(String accountId, ImmutableMap<Long, Long> repositoryAndInstallationIds) {
        if (repositoryAndInstallationIds.isEmpty()) {
            return;
        }
        Iterables.partition(repositoryAndInstallationIds.entrySet(), DYNAMO_WRITE_BATCH_MAX_SIZE).forEach(batch -> {
            dynamoUtil.retryUnprocessed(dynamoDoc.batchWriteItem(new TableWriteItems(gitHubAuthorizationSchema.tableName())
                    .withItemsToPut(batch.stream()
                            .map(entry -> new GitHubAuthorization(
                                    accountId,
                                    entry.getValue(),
                                    entry.getKey(),
                                    Instant.now().plus(config.authExpiry()).getEpochSecond()))
                            .map(gitHubAuthorizationSchema::toItem)
                            .collect(ImmutableList.toImmutableList()))));
        });
    }

    @Override
    public void setupConfigGitHubIntegration(String accountId, Optional<ConfigAdmin> configPrevious, ConfigAdmin configAdmin) {
        Optional<Long> repositoryIdOpt = Optional.ofNullable(configAdmin.getGithub()).map(com.smotana.clearflask.api.model.GitHub::getRepositoryId);
        Optional<Long> repositoryIdPreviousOpt = configPrevious.flatMap(c -> Optional.ofNullable(c.getGithub())).map(com.smotana.clearflask.api.model.GitHub::getRepositoryId);

        if (repositoryIdOpt.equals(repositoryIdPreviousOpt)) {
            return;
        }

        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            throw new ApiException(Response.Status.SERVICE_UNAVAILABLE, "GitHub integration is disabled");
        }

        // Install new
        if (repositoryIdOpt.isPresent()) {
            GitHubAuthorization authorization = getAccountAuthorizationForRepo(accountId, repositoryIdOpt.get())
                    .orElseThrow(() -> new ApiException(Response.Status.UNAUTHORIZED, "Your access to this repository is expired, please refresh."));
            linkRepo(
                    configAdmin.getProjectId(),
                    authorization);
        }

        // Uninstall old
        if (repositoryIdPreviousOpt.isPresent()) {
            unlinkRepository(
                    configAdmin.getProjectId(),
                    repositoryIdPreviousOpt.get(),
                    false,
                    true);
        }
    }

    private void linkRepo(String projectId, GitHubAuthorization authorization) {
        GitHub installationClient;
        try {
            installationClient = gitHubClientProvider.getInstallationClient(authorization.getInstallationId()).getClient();
        } catch (IOException ex) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "Access denied on GitHub Installation", ex);
        }
        GHRepository repository;
        try {
            repository = installationClient.getRepositoryById(authorization.getRepositoryId());
        } catch (IOException ex) {
            log.warn("Linking repo failed, could not get repository by id. projectId {}, authorization {}",
                    projectId, authorization);
            throw new ApiException(Response.Status.BAD_REQUEST, "Could not access GitHub repository");
        }
        try {
            repository.createHook(
                    "web",
                    ImmutableMap.of(
                            "url", getWebhookUrl(projectId).toExternalForm(),
                            "content_type", "json",
                            // You cannot retrieve a secret after it is set,
                            // it is safe to use the same secret as App webhook
                            "secret", configGitHubResource.webhookSecret()),
                    ImmutableSet.of(
                            GHEvent.ISSUES,
                            GHEvent.ISSUE_COMMENT),
                    true);
            repository.createWebHook(getWebhookUrl(projectId),
                    ImmutableSet.of(
                            GHEvent.ISSUES,
                            GHEvent.ISSUE_COMMENT));
        } catch (IOException ex) {
            log.warn("Linking repo failed, could not create webhook. projectId {}, authorization {}",
                    projectId, authorization);
            throw new ApiException(Response.Status.BAD_REQUEST, "Could not create GitHub repository webhook");
        }
    }

    @Extern
    @Override
    public void unlinkRepository(String projectId, long repositoryId, boolean removeFromConfig, boolean removeWebhook) {
        if (removeWebhook) {
            try {
                GHRepository repository = gitHubClientProvider.getAppClient()
                        .getRepositoryById(repositoryId);
                URL webhookUrl = getWebhookUrl(projectId);
                ImmutableList<GHHook> hooksToDelete = repository.getHooks()
                        .stream()
                        .filter(GHHook::isActive)
                        .filter(hook -> webhookUrl.equals(hook.getUrl()))
                        .collect(ImmutableList.toImmutableList());
                for (GHHook hook : hooksToDelete) {
                    hook.delete();
                }
            } catch (IOException ex) {
                log.warn("Unlinking repo failed, could not delete webhooks, ignoring. projectId {}, repositoryId {} removeFromConfig {}",
                        projectId, repositoryId, removeFromConfig);
            }
        }
        if (removeFromConfig) {
            projectStore.getProject(projectId, false)
                    .map(Project::getVersionedConfigAdmin)
                    .map(versionedConfigAdmin -> versionedConfigAdmin.toBuilder()
                            .config(versionedConfigAdmin.getConfig().toBuilder()
                                    .github(null)
                                    .build())
                            .build())
                    .ifPresent(config -> projectStore.updateConfig(
                            projectId,
                            Optional.empty(),
                            config,
                            true));
        }
    }

    private URL getWebhookUrl(String projectId) throws MalformedURLException {
        return new URL("https://" + configApp.domain() + GitHubResource.REPO_WEBHOOK_PATH.replace("{projectId}", projectId));
    }

    private Optional<GitHubAuthorization> getAccountAuthorizationForRepo(String accountId, long repositoryId) {
        return Optional.ofNullable(gitHubAuthorizationSchema.fromItem(gitHubAuthorizationSchema.table().getItem(new GetItemSpec()
                        .withPrimaryKey(gitHubAuthorizationSchema.primaryKey(Map.of(
                                "accountId", accountId,
                                "repositoryId", repositoryId))))))
                .filter(auth -> {
                    if (auth.getTtlInEpochSec() < Instant.now().getEpochSecond()) {
                        log.debug("DynamoDB has an expired auth session with expiry {}", auth.getTtlInEpochSec());
                        return false;
                    }
                    return true;
                });
    }

    @Override
    public Optional<IdeaAndIndexingFuture> ghIssueEvent(Project project, Issue ghIssue) throws IOException {
        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            return Optional.empty();
        }
        com.smotana.clearflask.api.model.GitHub integration = project.getGitHubIntegration().get();
        String ideaId = ideaStore.genDeterministicIdeaIdForGithubIssue(ghIssue.getIssue().getNumber(), ghIssue.getIssue().getId(), ghIssue.getRepository().getId());
        switch (ghIssue.getAction()) {
            case "opened":
                UserModel user = getCfUserFromGhUser(project.getProjectId(), ghIssue.getIssue().getUser());
                return Optional.of(ideaStore.createIdeaAndUpvote(new IdeaModel(
                        project.getProjectId(),
                        ideaId,
                        user.getUserId(),
                        user.getName(),
                        user.getIsMod(),
                        Instant.now(),
                        ghIssue.getIssue().getTitle(),
                        markdownAndQuillUtil.markdownToQuill(project.getProjectId(), "gh-new-post", ideaId, ghIssue.getIssue().getBody()),
                        null,
                        null,
                        null,
                        null,
                        integration.getCreateWithCategoryId(),
                        Optional.ofNullable(Strings.emptyToNull(integration.getInitialStatusId()))
                                .or(() -> project.getCategory(integration.getCreateWithCategoryId())
                                        .map(Category::getWorkflow)
                                        .flatMap(workflow -> Optional.ofNullable(workflow.getEntryStatus())))
                                .orElse(null),
                        integration.getCreateWithTags() != null
                                ? ImmutableSet.copyOf(integration.getCreateWithTags())
                                : ImmutableSet.of(),
                        0L,
                        0L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        null,
                        null,
                        ImmutableSet.of(),
                        null,
                        ghIssue.getIssue().getHtmlUrl().toExternalForm())));
            case "reopened":
            case "closed":
                Optional<String> switchToStatusOpt = Optional.ofNullable(integration.getStatusSync())
                        .map("reopened".equals(ghIssue.getAction())
                                ? GitHubStatusSync::getOpenStatus
                                : GitHubStatusSync::getClosedStatus);
                if (switchToStatusOpt.isPresent()) {
                    Optional<IdeaModel> ideaOpt = ideaStore.getIdea(project.getProjectId(), ideaId);
                    if (ideaOpt.isPresent() && !switchToStatusOpt.get().equals(ideaOpt.get().getStatusId())) {
                        return Optional.of(ideaStore.updateIdea(project.getProjectId(), ideaId, IdeaUpdateAdmin.builder()
                                .statusId(switchToStatusOpt.get())
                                .build(), Optional.empty()));
                    }
                }
                break;
            case "edited":
                boolean updated = false;
                IdeaUpdateAdmin.IdeaUpdateAdminBuilder updateBuilder = IdeaUpdateAdmin.builder();
                if (ghIssue.getChanges().getTitle() != null) {
                    updateBuilder.description(ghIssue.getIssue().getTitle());
                    updated = true;
                }
                if (ghIssue.getChanges().getBody() != null) {
                    updateBuilder.description(markdownAndQuillUtil.markdownToQuill(project.getProjectId(), "gh-post", ideaId, ghIssue.getIssue().getBody()));
                    updated = true;
                }
                if (updated) {
                    return Optional.of(ideaStore.updateIdea(project.getProjectId(), ideaId, updateBuilder.build(), Optional.empty()));
                }
                break;
            case "deleted":
                try {
                    ideaStore.deleteIdea(project.getProjectId(), ideaId, true);
                } catch (ConditionalCheckFailedException ex) {
                    // Issue was probably created before integration was setup and doesn't exist
                }
                break;
        }

        return Optional.empty();
    }

    @Override
    public Optional<CommentAndIndexingFuture<?>> ghIssueCommentEvent(Project project, IssueComment ghIssueComment, String payload) throws IOException {
        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            return Optional.empty();
        }

        com.smotana.clearflask.api.model.GitHub integration = project.getGitHubIntegration().get();
        if (integration.getCommentSync() != Boolean.TRUE) {
            return Optional.empty();
        }

        String postId = ideaStore.genDeterministicIdeaIdForGithubIssue(ghIssueComment.getIssue().getNumber(), ghIssueComment.getIssue().getId(), ghIssueComment.getRepository().getId());
        String commentId = commentStore.genDeterministicCommentIdForGithubIssueComment(ghIssueComment.getComment().getId());
        switch (ghIssueComment.getAction()) {
            case "created":
                Optional<IdeaModel> ideaOpt = ideaStore.getIdea(project.getProjectId(), postId);
                if (ideaOpt.isPresent()) {
                    UserModel user = getCfUserFromGhUser(project.getProjectId(), ghIssueComment.getComment().getUser());
                    return Optional.of(commentStore.createCommentAndUpvote(new CommentModel(
                            project.getProjectId(),
                            postId,
                            commentId,
                            ImmutableList.of(),
                            0,
                            0L,
                            user.getUserId(),
                            user.getName(),
                            user.getIsMod(),
                            Instant.now(),
                            null,
                            markdownAndQuillUtil.markdownToQuill(project.getProjectId(), "gh-new-comment", commentId, ghIssueComment.getComment().getBody()),
                            0,
                            0)));
                }
                break;
            case "edited":
                // GitHub client is missing "changes" parsing so we cannot do:
                // ghIssueComment.getChanges().getBody()
                // https://github.com/hub4j/github-api/issues/1243
                // Need to extract it ourselves here
                boolean bodyChanged = changesBodyJsonPath.read(payload) != null;
                if (bodyChanged) {
                    commentStore.updateComment(project.getProjectId(), postId, commentId, ghIssueComment.getComment().getUpdatedAt().toInstant(), CommentUpdate.builder()
                            .content(markdownAndQuillUtil.markdownToQuill(project.getProjectId(), "gh-comment", commentId, ghIssueComment.getComment().getBody())).build());
                }
                break;
            case "deleted":
                try {
                    return Optional.of(commentStore.markAsDeletedComment(project.getProjectId(), postId, commentId));
                } catch (ConditionalCheckFailedException ex) {
                    // Issue comment was probably created before integration was setup and doesn't exist
                }
                break;
        }

        return Optional.empty();
    }

    @Override
    public ListenableFuture<Optional<GHIssueComment>> cfCommentCreatedAsync(Project project, IdeaModel idea, CommentModel comment, UserModel user) {
        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            return Futures.immediateFuture(Optional.empty());
        }

        Optional<com.smotana.clearflask.api.model.GitHub> integration = project.getGitHubIntegration();
        if (integration.isEmpty() || integration.get().getCommentSync() != Boolean.TRUE) {
            return Futures.immediateFuture(Optional.empty());
        }

        Optional<IdeaStore.GitHubIssueMetadata> gitHubIssueMetadataOpt = getMetadataFromLinkedIdea(project, idea);
        if (gitHubIssueMetadataOpt.isEmpty()) {
            return Futures.immediateFuture(Optional.empty());
        }

        return submit(() -> {
            Optional<CommentModel> parentCommentOpt = comment.getParentCommentIds().isEmpty() ? Optional.empty()
                    : commentStore.getComment(project.getProjectId(), idea.getIdeaId(),
                    comment.getParentCommentIds().get(comment.getParentCommentIds().size() - 1));

            GitHub appClient = gitHubClientProvider.getAppClient();
            GHRepository repository;
            try {
                repository = appClient.getRepositoryById(gitHubIssueMetadataOpt.get().getRepositoryId());
            } catch (HttpException ex) {
                if (ex.getResponseCode() == 403) {
                    // Turns out we don't have permission anymore, unlink this repo
                    unlinkRepository(project.getProjectId(), gitHubIssueMetadataOpt.get().getRepositoryId(), true, false);
                }
                throw ex;
            }

            GHIssueComment ghIssueComment = repository.getIssue((int) gitHubIssueMetadataOpt.get().getIssueId())
                    .comment(markdownAndQuillUtil.quillToMarkdown(quoteComment(comment, parentCommentOpt)));
            return Optional.of(ghIssueComment);
        });
    }

    @Override
    public ListenableFuture<Optional<StatusAndOrResponse>> cfStatusAndOrResponseChangedAsync(Project project, IdeaModel idea, boolean statusChanged, boolean responseChanged) {
        if (!config.enabled()) {
            log.debug("Not enabled, skipping");
            return Futures.immediateFuture(Optional.empty());
        }

        if (!statusChanged && !responseChanged) {
            return Futures.immediateFuture(Optional.empty());
        }

        Optional<com.smotana.clearflask.api.model.GitHub> integration = project.getGitHubIntegration();
        if (integration.isEmpty()) {
            return Futures.immediateFuture(Optional.empty());
        }
        boolean syncStatus = integration.get().getStatusSync() != null;
        boolean syncResponse = integration.get().getResponseSync() == Boolean.TRUE;
        if ((!statusChanged || !syncStatus)
                && (!responseChanged || !syncResponse)) {
            return Futures.immediateFuture(Optional.empty());
        }

        Optional<IdeaStore.GitHubIssueMetadata> gitHubIssueMetadataOpt = getMetadataFromLinkedIdea(project, idea);
        if (gitHubIssueMetadataOpt.isEmpty()) {
            return Futures.immediateFuture(Optional.empty());
        }

        return submit(() -> {
            GitHub appClient = gitHubClientProvider.getAppClient();
            GHRepository repository;
            try {
                repository = appClient.getRepositoryById(gitHubIssueMetadataOpt.get().getRepositoryId());
            } catch (HttpException ex) {
                if (ex.getResponseCode() == 403) {
                    // Turns out we don't have permission anymore, unlink this repo
                    unlinkRepository(project.getProjectId(), gitHubIssueMetadataOpt.get().getRepositoryId(), true, false);
                }
                throw ex;
            }

            GHIssue ghIssue = repository.getIssue((int) gitHubIssueMetadataOpt.get().getIssueId());
            Optional<GHIssueComment> responseCommentOpt = Optional.empty();
            if (responseChanged
                    && syncResponse
                    && !Strings.isNullOrEmpty(idea.getResponseAsUnsafeHtml())
                    && !Strings.isNullOrEmpty(idea.getResponseAuthorName())) {
                GHIssueComment ghComment = ghIssue.comment(signComment(
                        idea.getResponseSanitized(sanitizer),
                        idea.getResponseAuthorName(),
                        true));
                responseCommentOpt = Optional.of(ghComment);
            }
            if (statusChanged
                    && syncStatus
                    && !Strings.isNullOrEmpty(idea.getStatusId())) {
                IdeaStatus statusToSet = project.getStatus(idea.getCategoryId(), idea.getStatusId()).get();
                Optional<GHLabel> labelOpt = repository.listLabels().toList().stream()
                        .filter(label -> statusToSet.getName().equals(label.getName()))
                        .findAny();
                GHLabel labelToAdd;
                if (labelOpt.isEmpty()) {
                    labelToAdd = repository.createLabel(
                            statusToSet.getName(),
                            statusToSet.getColor() != null && statusToSet.getColor().startsWith("#")
                                    ? statusToSet.getColor().substring(1)
                                    : "000000",
                            "Managed by ClearFlask");
                } else {
                    labelToAdd = labelOpt.get();
                }
                List<GHLabel> existingLabels = ghIssue.addLabels(labelToAdd);
                Set<String> categoryAllStatusNames = project.getCategory(idea.getCategoryId()).get().getWorkflow().getStatuses().stream()
                        .map(IdeaStatus::getName)
                        .collect(Collectors.toSet());
                Set<GHLabel> labelsToDelete = existingLabels.stream()
                        .filter(label -> labelToAdd.getId() != label.getId())
                        .filter(label -> categoryAllStatusNames.contains(label.getName()))
                        .collect(Collectors.toSet());
                ghIssue.removeLabels(labelsToDelete);

                List<String> closedStatuses = Optional.ofNullable(integration.get().getStatusSync().getClosedStatuses()).orElse(ImmutableList.of());
                if (!closedStatuses.isEmpty()) {
                    boolean shouldBeClosed = closedStatuses.contains(statusToSet.getStatusId());
                    boolean isClosed = ghIssue.getState().equals(GHIssueState.CLOSED);
                    if (shouldBeClosed != isClosed) {
                        if (shouldBeClosed) {
                            ghIssue.close();
                        } else {
                            ghIssue.reopen();
                        }
                    }
                }
            }
            return Optional.of(new StatusAndOrResponse(ghIssue, responseCommentOpt));
        });
    }

    private String quoteComment(CommentModel comment, Optional<CommentModel> parentCommentOpt) {
        String html = signComment(comment.getContentSanitized(sanitizer), comment.getAuthorName(), comment.getAuthorIsMod());
        if (parentCommentOpt.isPresent() && parentCommentOpt.get().getAuthorUserId() != null) {
            html = signComment("<blockquote>" +
                    parentCommentOpt.get().getContentSanitized(sanitizer) +
                    "</blockquote>", parentCommentOpt.get().getAuthorName(), parentCommentOpt.get().getAuthorIsMod()) +
                    html;
        }
        return html;
    }

    private String signComment(String html, String name, boolean isMod) {
        name = "<b>" + name + "</b>";
        return (isMod
                ? "Moderator " + name + ":"
                : name + ":")
                + html;
    }

    private Optional<IdeaStore.GitHubIssueMetadata> getMetadataFromLinkedIdea(Project project, IdeaModel idea) {
        Optional<IdeaStore.GitHubIssueMetadata> gitHubIssueMetadataOpt = ideaStore.extractGitHubIssueFromIdeaId(idea.getIdeaId());
        if (gitHubIssueMetadataOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<com.smotana.clearflask.api.model.GitHub> gitHubOpt = project.getGitHubIntegration();
        if (gitHubOpt.isEmpty() || gitHubOpt.get().getRepositoryId() != gitHubIssueMetadataOpt.get().getRepositoryId()) {
            return Optional.empty();
        }
        return gitHubIssueMetadataOpt;

    }

    private UserModel getCfUserFromGhUser(String projectId, GHUser ghUser) {
        return userStore.createOrGet(
                projectId,
                USER_GUID_GITHUB_PREFIX + ghUser.getId(),
                () -> {
                    try {
                        return Optional.ofNullable(Strings.emptyToNull(ghUser.getEmail()));
                    } catch (IOException ex) {
                        log.warn("Failed to fetch email from GH user, ghUser ID {} projectId {}", ghUser.getId(), projectId);
                        return Optional.empty();
                    }
                },
                () -> {
                    try {
                        return Optional.ofNullable(Strings.emptyToNull(ghUser.getName()));
                    } catch (IOException e) {
                        log.warn("Failed to fetch name from GH user, ghUser ID {} projectId {}", ghUser.getId(), projectId);
                        return Optional.empty();
                    }
                },
                false);
    }

    private <T> ListenableFuture<T> submit(Callable<T> task) {
        return executor.submit(() -> {
            try {
                return task.call();
            } catch (Throwable th) {
                log.warn("Failed to complete GitHub Integration task", th);
                throw th;
            }
        });
    }

    public static Module module() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(GitHubStore.class).to(GitHubStoreImpl.class).asEagerSingleton();
                install(ConfigSystem.configModule(Config.class));
                Multibinder.newSetBinder(binder(), ManagedService.class).addBinding().to(GitHubStoreImpl.class);
            }
        };
    }
}
