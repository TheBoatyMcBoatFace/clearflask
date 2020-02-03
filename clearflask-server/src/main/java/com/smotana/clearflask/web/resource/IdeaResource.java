package com.smotana.clearflask.web.resource;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Funnels;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.smotana.clearflask.api.IdeaAdminApi;
import com.smotana.clearflask.api.IdeaApi;
import com.smotana.clearflask.api.model.Idea;
import com.smotana.clearflask.api.model.IdeaCreate;
import com.smotana.clearflask.api.model.IdeaCreateAdmin;
import com.smotana.clearflask.api.model.IdeaSearch;
import com.smotana.clearflask.api.model.IdeaSearchAdmin;
import com.smotana.clearflask.api.model.IdeaSearchResponse;
import com.smotana.clearflask.api.model.IdeaUpdate;
import com.smotana.clearflask.api.model.IdeaUpdateAdmin;
import com.smotana.clearflask.api.model.IdeaWithVote;
import com.smotana.clearflask.api.model.IdeaWithVoteSearchResponse;
import com.smotana.clearflask.api.model.Vote;
import com.smotana.clearflask.api.model.VoteOption;
import com.smotana.clearflask.core.push.NotificationService;
import com.smotana.clearflask.security.limiter.Limit;
import com.smotana.clearflask.store.CommentStore;
import com.smotana.clearflask.store.IdeaStore;
import com.smotana.clearflask.store.IdeaStore.IdeaModel;
import com.smotana.clearflask.store.IdeaStore.SearchResponse;
import com.smotana.clearflask.store.UserStore;
import com.smotana.clearflask.store.UserStore.UserModel;
import com.smotana.clearflask.store.UserStore.UserSession;
import com.smotana.clearflask.store.VoteStore;
import com.smotana.clearflask.store.dynamo.DefaultDynamoDbProvider;
import com.smotana.clearflask.util.BloomFilters;
import com.smotana.clearflask.web.ErrorWithMessageException;
import com.smotana.clearflask.web.security.ExtendedSecurityContext;
import com.smotana.clearflask.web.security.Role;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Singleton
@Path("/v1")
public class IdeaResource extends AbstractResource implements IdeaApi, IdeaAdminApi {

    @Inject
    private NotificationService notificationService;
    @Inject
    private IdeaStore ideaStore;
    @Inject
    private CommentStore commentStore;
    @Inject
    private VoteStore voteStore;
    @Inject
    private UserStore userStore;

    @RolesAllowed({Role.PROJECT_USER})
    @Limit(requiredPermits = 30, challengeAfter = 10)
    @Override
    public Idea ideaCreate(String projectId, IdeaCreate ideaCreate) {
        UserModel user = getExtendedPrincipal()
                .flatMap(ExtendedSecurityContext.ExtendedPrincipal::getUserSessionOpt)
                .map(UserSession::getUserId)
                .flatMap(userId -> userStore.getUser(projectId, userId))
                .orElseThrow(() -> new ErrorWithMessageException(Response.Status.NOT_FOUND, "User not found"));
        IdeaModel ideaModel = new IdeaModel(
                projectId,
                ideaStore.genIdeaId(ideaCreate.getTitle()),
                user.getUserId(),
                user.getName(),
                Instant.now(),
                ideaCreate.getTitle(),
                Strings.emptyToNull(ideaCreate.getDescription()),
                null,
                ideaCreate.getCategoryId(),
                null,
                ImmutableSet.copyOf(ideaCreate.getTagIds()),
                0L,
                0L,
                null,
                null,
                ImmutableSet.of(),
                null,
                null,
                null,
                ImmutableMap.of());
        ideaStore.createIdea(ideaModel);
        return ideaModel.toIdea();
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public Idea ideaCreateAdmin(String projectId, IdeaCreateAdmin ideaCreateAdmin) {
        UserModel user = userStore.getUser(projectId, ideaCreateAdmin.getAuthorUserId())
                .orElseThrow(() -> new ErrorWithMessageException(Response.Status.NOT_FOUND, "User not found"));
        IdeaModel ideaModel = new IdeaModel(
                projectId,
                ideaStore.genIdeaId(ideaCreateAdmin.getTitle()),
                user.getUserId(),
                user.getName(),
                Instant.now(),
                ideaCreateAdmin.getTitle(),
                Strings.emptyToNull(ideaCreateAdmin.getDescription()),
                Strings.emptyToNull(ideaCreateAdmin.getResponse()),
                ideaCreateAdmin.getCategoryId(),
                ideaCreateAdmin.getStatusId(),
                ImmutableSet.copyOf(ideaCreateAdmin.getTagIds()),
                0L,
                0L,
                null,
                ideaCreateAdmin.getFundGoal(),
                ImmutableSet.of(),
                null,
                null,
                null,
                ImmutableMap.of());
        ideaStore.createIdea(ideaModel);
        return ideaModel.toIdea();
    }

    @PermitAll
    @Limit(requiredPermits = 1)
    @Override
    public IdeaWithVote ideaGet(String projectId, String ideaId) {
        Optional<UserModel> userOpt = getExtendedPrincipal()
                .flatMap(ExtendedSecurityContext.ExtendedPrincipal::getUserSessionOpt)
                .map(UserSession::getUserId)
                .flatMap(userId -> userStore.getUser(projectId, userId));
        return ideaStore.getIdea(projectId, ideaId)
                .map(ideaModel -> userOpt.map(user -> addVote(user, ideaModel))
                        .orElseGet(() -> ideaModel.toIdeaWithVote(new Vote(null, null, null))))
                .orElseThrow(() -> new ErrorWithMessageException(Response.Status.NOT_FOUND, "Idea not found"));
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public Idea ideaGetAdmin(String projectId, String ideaId) {
        return ideaStore.getIdea(projectId, ideaId)
                .map(IdeaModel::toIdea)
                .orElseThrow(() -> new ErrorWithMessageException(Response.Status.NOT_FOUND, "Idea not found"));
    }

    @PermitAll
    @Limit(requiredPermits = 10)
    @Override
    public IdeaWithVoteSearchResponse ideaSearch(String projectId, IdeaSearch ideaSearch, String cursor) {
        Optional<UserModel> userOpt = getExtendedPrincipal()
                .flatMap(ExtendedSecurityContext.ExtendedPrincipal::getUserSessionOpt)
                .map(UserSession::getUserId)
                .flatMap(userId -> userStore.getUser(projectId, userId));
        SearchResponse searchResponse = ideaStore.searchIdeas(
                projectId,
                ideaSearch,
                userOpt.map(UserModel::getUserId),
                Optional.ofNullable(Strings.emptyToNull(cursor)));

        ImmutableMap<String, IdeaModel> ideasById = ideaStore.getIdeas(projectId, searchResponse.getIdeaIds());

        ImmutableList<IdeaModel> ideaModels = searchResponse.getIdeaIds().stream()
                .map(ideasById::get)
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());

        return new IdeaWithVoteSearchResponse(
                searchResponse.getCursorOpt().orElse(null),
                userOpt.map(user -> addVotes(user, ideaModels))
                        .orElseGet(() -> ideaModels.stream()
                                .map(ideaModel -> ideaModel.toIdeaWithVote(new Vote(null, null, null)))
                                .collect(ImmutableList.toImmutableList())));
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 10)
    @Override
    public IdeaSearchResponse ideaSearchAdmin(String projectId, IdeaSearchAdmin ideaSearchAdmin, String cursor) {
        SearchResponse searchResponse = ideaStore.searchIdeas(
                projectId,
                ideaSearchAdmin,
                false,
                Optional.ofNullable(Strings.emptyToNull(cursor)));

        ImmutableMap<String, IdeaModel> ideasById = ideaStore.getIdeas(projectId, searchResponse.getIdeaIds());

        return new IdeaSearchResponse(
                searchResponse.getCursorOpt().orElse(null),
                searchResponse.getIdeaIds().stream()
                        .map(ideasById::get)
                        .filter(Objects::nonNull)
                        .map(IdeaModel::toIdea)
                        .collect(ImmutableList.toImmutableList()));
    }

    @RolesAllowed({Role.IDEA_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public Idea ideaUpdate(String projectId, String ideaId, IdeaUpdate ideaUpdate) {
        return ideaStore.updateIdea(projectId, ideaId, ideaUpdate).getIdea().toIdea();
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public Idea ideaUpdateAdmin(String projectId, String ideaId, IdeaUpdateAdmin ideaUpdateAdmin) {
        IdeaModel idea = ideaStore.updateIdea(projectId, ideaId, ideaUpdateAdmin).getIdea();
        if (ideaUpdateAdmin.getSuppressNotifications() != Boolean.TRUE) {
            boolean statusChanged = !Strings.isNullOrEmpty(ideaUpdateAdmin.getStatusId());
            boolean responseChanged = !Strings.isNullOrEmpty(ideaUpdateAdmin.getResponse());
            if (statusChanged || responseChanged) {
                notificationService.onStatusOrResponseChanged(idea, statusChanged, responseChanged);
            }
        }
        return idea.toIdea();
    }

    @RolesAllowed({Role.IDEA_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public void ideaDelete(String projectId, String ideaId) {
        ideaStore.deleteIdea(projectId, ideaId);
        commentStore.deleteCommentsForIdea(projectId, ideaId);
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public void ideaDeleteAdmin(String projectId, String ideaId) {
        ideaStore.deleteIdea(projectId, ideaId);
        commentStore.deleteCommentsForIdea(projectId, ideaId);
    }

    @RolesAllowed({Role.PROJECT_OWNER})
    @Limit(requiredPermits = 1)
    @Override
    public void ideaDeleteBulkAdmin(String projectId, IdeaSearchAdmin ideaSearchAdmin) {
        SearchResponse searchResponse = null;
        do {
            searchResponse = ideaStore.searchIdeas(
                    projectId,
                    // TODO handle the limit somehow better here
                    ideaSearchAdmin.toBuilder().limit(Math.min(
                            ideaSearchAdmin.getLimit(),
                            DefaultDynamoDbProvider.DYNAMO_WRITE_BATCH_MAX_SIZE)).build(),
                    true,
                    searchResponse == null ? Optional.empty() : searchResponse.getCursorOpt());
            ideaStore.deleteIdeas(projectId, searchResponse.getIdeaIds());
            searchResponse.getIdeaIds().forEach(ideaId -> commentStore.deleteCommentsForIdea(projectId, ideaId));
        } while (!searchResponse.getCursorOpt().isPresent());
    }

    private IdeaWithVote addVote(UserModel user, IdeaModel idea) {
        Optional<VoteOption> voteOptionOpt = Optional.empty();
        if (user.getVoteBloom() != null
                && BloomFilters.fromByteArray(user.getVoteBloom(), Funnels.stringFunnel(Charsets.UTF_8))
                .mightContain(idea.getIdeaId())) {
            voteOptionOpt = Optional.ofNullable(voteStore.voteSearch(user.getProjectId(), user.getUserId(), ImmutableSet.of(idea.getIdeaId()))
                    .get(idea.getIdeaId()))
                    .map(voteModel -> VoteStore.VoteValue.fromValue(voteModel.getVote()).toVoteOption());
        }
        Optional<List<String>> expressionOpt = Optional.empty();
        if (user.getExpressBloom() != null
                && BloomFilters.fromByteArray(user.getExpressBloom(), Funnels.stringFunnel(Charsets.UTF_8))
                .mightContain(idea.getIdeaId())) {
            expressionOpt = Optional.ofNullable(voteStore.expressSearch(user.getProjectId(), user.getUserId(), ImmutableSet.of(idea.getIdeaId()))
                    .get(idea.getIdeaId()))
                    .map(expressModel -> expressModel.getExpressions().asList());
        }
        Optional<Long> fundAmountOpt = Optional.empty();
        if (user.getFundBloom() != null
                && BloomFilters.fromByteArray(user.getFundBloom(), Funnels.stringFunnel(Charsets.UTF_8))
                .mightContain(idea.getIdeaId())) {
            fundAmountOpt = Optional.ofNullable(voteStore.fundSearch(user.getProjectId(), user.getUserId(), ImmutableSet.of(idea.getIdeaId()))
                    .get(idea.getIdeaId()))
                    .map(VoteStore.FundModel::getFundAmount);
        }

        return idea.toIdeaWithVote(new Vote(
                voteOptionOpt.orElse(null),
                expressionOpt.orElse(null),
                fundAmountOpt.orElse(null)
        ));
    }

    private ImmutableList<IdeaWithVote> addVotes(UserModel user, ImmutableList<IdeaModel> ideas) {
        ImmutableMap<String, VoteStore.VoteModel> voteResults = Optional.ofNullable(user.getVoteBloom())
                .map(bytes -> BloomFilters.fromByteArray(bytes, Funnels.stringFunnel(Charsets.UTF_8)))
                .map(bloomFilter -> ideas.stream()
                        .map(IdeaModel::getIdeaId)
                        .filter(bloomFilter::mightContain)
                        .collect(ImmutableSet.toImmutableSet()))
                .map(ideaIds -> voteStore.voteSearch(user.getProjectId(), user.getUserId(), ideaIds))
                .orElse(ImmutableMap.of());

        ImmutableMap<String, VoteStore.ExpressModel> expressResults = Optional.ofNullable(user.getExpressBloom())
                .map(bytes -> BloomFilters.fromByteArray(bytes, Funnels.stringFunnel(Charsets.UTF_8)))
                .map(bloomFilter -> ideas.stream()
                        .map(IdeaModel::getIdeaId)
                        .filter(bloomFilter::mightContain)
                        .collect(ImmutableSet.toImmutableSet()))
                .map(ideaIds -> voteStore.expressSearch(user.getProjectId(), user.getUserId(), ideaIds))
                .orElse(ImmutableMap.of());

        ImmutableMap<String, VoteStore.FundModel> fundResults = Optional.ofNullable(user.getFundBloom())
                .map(bytes -> BloomFilters.fromByteArray(bytes, Funnels.stringFunnel(Charsets.UTF_8)))
                .map(bloomFilter -> ideas.stream()
                        .map(IdeaModel::getIdeaId)
                        .filter(bloomFilter::mightContain)
                        .collect(ImmutableSet.toImmutableSet()))
                .map(ideaIds -> voteStore.fundSearch(user.getProjectId(), user.getUserId(), ideaIds))
                .orElse(ImmutableMap.of());

        return ideas.stream()
                .map(idea -> {
                    Vote.VoteBuilder voteBuilder = Vote.builder();
                    VoteStore.VoteModel voteModel = voteResults.get(idea.getIdeaId());
                    if (voteModel != null) {
                        voteBuilder.vote(VoteStore.VoteValue.fromValue(voteModel.getVote()).toVoteOption());
                    }
                    VoteStore.ExpressModel expressModel = expressResults.get(idea.getIdeaId());
                    if (expressModel != null) {
                        voteBuilder.expression(expressModel.getExpressions().asList());
                    }
                    VoteStore.FundModel fundModel = fundResults.get(idea.getIdeaId());
                    if (fundModel != null) {
                        voteBuilder.fundAmount(fundModel.getFundAmount());
                    }
                    return idea.toIdeaWithVote(voteBuilder.build());
                })
                .collect(ImmutableList.toImmutableList());
    }

    public static Module module() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(IdeaResource.class);
            }
        };
    }
}
