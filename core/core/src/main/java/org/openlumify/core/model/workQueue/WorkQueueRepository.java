package org.openlumify.core.model.workQueue;

import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vertexium.*;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.config.Configuration;
import org.openlumify.core.exception.OpenLumifyAccessDeniedException;
import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.externalResource.ExternalResourceWorker;
import org.openlumify.core.externalResource.QueueExternalResourceWorker;
import org.openlumify.core.ingest.WorkerSpout;
import org.openlumify.core.ingest.graphProperty.ElementOrPropertyStatus;
import org.openlumify.core.ingest.graphProperty.GraphPropertyMessage;
import org.openlumify.core.ingest.graphProperty.GraphPropertyRunner;
import org.openlumify.core.model.FlushFlag;
import org.openlumify.core.model.WorkQueueNames;
import org.openlumify.core.model.notification.SystemNotification;
import org.openlumify.core.model.notification.UserNotification;
import org.openlumify.core.model.properties.MediaOpenLumifyProperties;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdate;
import org.openlumify.core.model.properties.types.OpenLumifyPropertyUpdateRemove;
import org.openlumify.core.model.user.AuthorizationRepository;
import org.openlumify.core.model.user.UserRepository;
import org.openlumify.core.model.workspace.Workspace;
import org.openlumify.core.model.workspace.WorkspaceRepository;
import org.openlumify.core.model.workspace.WorkspaceUser;
import org.openlumify.core.user.SystemUser;
import org.openlumify.core.user.User;
import org.openlumify.core.util.ClientApiConverter;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.ClientApiWorkspace;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WorkQueueRepository {
    protected static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(WorkQueueRepository.class);
    private final Configuration configuration;
    private final WorkQueueNames workQueueNames;
    private final Graph graph;
    private GraphPropertyRunner graphPropertyRunner;
    private AuthorizationRepository authorizationRepository;
    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;

    protected WorkQueueRepository(
            Graph graph,
            WorkQueueNames workQueueNames,
            Configuration configuration
    ) {
        this.graph = graph;
        this.workQueueNames = workQueueNames;
        this.configuration = configuration;
    }

    public void pushGraphPropertyQueue(Element element, Property property, Priority priority) {
        checkNotNull(property, "property cannot be null");
        pushGraphPropertyQueue(element, property.getKey(), property.getName(), priority);
    }

    public void pushGraphPropertyQueueHiddenOrDeleted(
            Element element,
            Property property,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            String workspaceId,
            Priority priority
    ) {
        checkNotNull(property, "property cannot be null");
        pushGraphPropertyQueue(element, property, workspaceId, null, priority, status, beforeActionTimestamp);
    }

    public void pushGraphOpenLumifyPropertyQueue(
            Element element,
            Iterable<OpenLumifyPropertyUpdate> properties,
            Priority priority
    ) {
        pushGraphOpenLumifyPropertyQueue(element, properties, null, null, priority);
    }

    public void pushGraphOpenLumifyPropertyQueue(
            Element element,
            Iterable<OpenLumifyPropertyUpdate> properties,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        GraphPropertyMessage data = new GraphPropertyMessage();
        data.setPriority(priority);

        List<GraphPropertyMessage.Property> messageProperties = new ArrayList<>();
        for (OpenLumifyPropertyUpdate propertyUpdate : properties) {
            String propertyKey = propertyUpdate.getPropertyKey();
            String propertyName = propertyUpdate.getPropertyName();

            if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
                broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
            }

            ElementOrPropertyStatus status = ElementOrPropertyStatus.getStatus(propertyUpdate);
            if (canHandle(element, propertyKey, propertyName, status)) {
                Long beforeDeleteTimestamp = propertyUpdate instanceof OpenLumifyPropertyUpdateRemove
                        ? ((OpenLumifyPropertyUpdateRemove) propertyUpdate).getBeforeDeleteTimestamp()
                        : null;
                GraphPropertyMessage.Property property = new GraphPropertyMessage.Property();
                property.setPropertyKey(propertyKey);
                property.setPropertyName(propertyName);
                property.setStatus(status);
                property.setBeforeActionTimestamp(beforeDeleteTimestamp);
                messageProperties.add(property);
            }
        }
        if (messageProperties.size() == 0) {
            return;
        }
        data.setProperties(messageProperties.toArray(new GraphPropertyMessage.Property[messageProperties.size()]));

        addElementTypeToJson(data, element);

        if (workspaceId != null && !workspaceId.equals("")) {
            data.setWorkspaceId(workspaceId);
            data.setVisibilitySource(visibilitySource);
        }

        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);
    }

    private void addElementTypeToJson(GraphPropertyMessage data, Element element) {
        if (element instanceof Vertex) {
            data.setGraphVertexId(new String[]{element.getId()});
        } else if (element instanceof Edge) {
            data.setGraphEdgeId(new String[]{element.getId()});
        } else {
            throw new OpenLumifyException("Unexpected element type: " + element.getClass().getName());
        }
    }

    public void pushGraphPropertyQueue(
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        pushGraphPropertyQueue(
                element,
                property.getKey(),
                property.getName(),
                workspaceId,
                visibilitySource,
                ElementOrPropertyStatus.UPDATE,
                null,
                priority
        );
    }

    public void pushGraphPropertyQueue(
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            FlushFlag flushFlag
    ) {
        pushGraphPropertyQueue(
                element,
                property.getKey(),
                property.getName(),
                workspaceId,
                visibilitySource,
                priority,
                ElementOrPropertyStatus.UPDATE,
                null,
                flushFlag
        );
    }

    public void pushGraphPropertyQueue(
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            FlushFlag flushFlag
    ) {
        pushGraphPropertyQueue(
                element,
                property.getKey(),
                property.getName(),
                workspaceId,
                visibilitySource,
                priority,
                status,
                beforeActionTimestamp,
                flushFlag
        );
    }

    public void pushElementImageQueue(Element element, Property property, Priority priority) {
        pushElementImageQueue(element, property.getKey(), property.getName(), priority);
    }

    public void pushElementImageQueue(
            Element element,
            String propertyKey,
            String propertyName,
            Priority priority
    ) {
        getGraph().flush();
        checkNotNull(element);
        JSONObject data = new JSONObject();
        if (element instanceof Vertex) {
            data.put("graphVertexId", element.getId());
        } else if (element instanceof Edge) {
            data.put("graphEdgeId", element.getId());
        } else {
            throw new OpenLumifyException("Unexpected element type: " + element.getClass().getName());
        }
        data.put("propertyKey", propertyKey);
        data.put("propertyName", propertyName);
        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), FlushFlag.DEFAULT, data, priority);

        broadcastEntityImage(element, propertyKey, propertyName);
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            Priority priority
    ) {
        pushGraphPropertyQueue(element, propertyKey, propertyName, ElementOrPropertyStatus.UPDATE, null, priority);
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            Long beforeActionTimestamp,
            Priority priority
    ) {
        pushGraphPropertyQueue(
                element, propertyKey, propertyName, ElementOrPropertyStatus.UPDATE, beforeActionTimestamp, priority);
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            Priority priority
    ) {
        pushGraphPropertyQueue(
                element,
                propertyKey,
                propertyName,
                null,
                null,
                status,
                beforeActionTimestamp,
                priority
        );
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            String workspaceId,
            String visibilitySource,
            Priority priority
    ) {
        pushGraphPropertyQueue(
                element,
                propertyKey,
                propertyName,
                workspaceId,
                visibilitySource,
                priority,
                ElementOrPropertyStatus.UPDATE,
                null,
                FlushFlag.DEFAULT
        );
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            String workspaceId,
            String visibilitySource,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            Priority priority
    ) {
        pushGraphPropertyQueue(
                element,
                propertyKey,
                propertyName,
                workspaceId,
                visibilitySource,
                priority,
                status,
                beforeActionTimestamp,
                FlushFlag.DEFAULT
        );
    }


    public void pushMultipleGraphPropertyQueue(
            Iterable<? extends Element> elements,
            String propertyKey,
            String propertyName,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            FlushFlag flushFlag
    ) {
        checkNotNull(elements);
        if (!elements.iterator().hasNext()) {
            return;
        }

        getGraph().flush();

        GraphPropertyMessage data = createPropertySpecificMessage(
                propertyKey,
                propertyName,
                workspaceId,
                visibilitySource,
                status,
                beforeActionTimestamp,
                priority
        );

        List<String> vertices = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        for (Element element : elements) {
            if (!canHandle(element, propertyKey, propertyName, status)) {
                continue;
            }

            if (element instanceof Vertex) {
                vertices.add(element.getId());
            } else if (element instanceof Edge) {
                edges.add(element.getId());
            } else {
                throw new OpenLumifyException("Unexpected element type: " + element.getClass().getName());
            }
        }

        data.setGraphVertexId(vertices.toArray(new String[vertices.size()]));
        data.setGraphEdgeId(edges.toArray(new String[edges.size()]));

        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);

        for (Element element : elements) {
            if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
                broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
            }
        }
    }

    public void pushGraphPropertyQueue(
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            ElementOrPropertyStatus status,
            Long beforeDeleteTimestamp
    ) {
        getGraph().flush();
        checkNotNull(element);
        String propertyKey = property.getKey();
        String propertyName = property.getName();

        GraphPropertyMessage data = createPropertySpecificMessage(
                propertyKey,
                propertyName,
                workspaceId,
                visibilitySource,
                status,
                beforeDeleteTimestamp,
                priority
        );

        addElementTypeToJson(data, element);

        if (canHandle(element, property, status)) {
            pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);
        }

        if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
            broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
        }
    }

    public void pushGraphPropertyQueue(
            Element element,
            String propertyKey,
            String propertyName,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            ElementOrPropertyStatus status,
            Long beforeDeleteTimestamp,
            @Deprecated FlushFlag flushFlag
    ) {
        getGraph().flush();
        checkNotNull(element);

        GraphPropertyMessage data = createPropertySpecificMessage(
                propertyKey,
                propertyName,
                workspaceId,
                visibilitySource,
                status,
                beforeDeleteTimestamp,
                priority
        );

        addElementTypeToJson(data, element);

        if (canHandle(element, propertyKey, propertyName, status)) {
            pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);
        }

        if (shouldBroadcastGraphPropertyChange(element, propertyKey, propertyName, workspaceId, priority)) {
            broadcastPropertyChange(element, propertyKey, propertyName, workspaceId);
        }
    }

    private boolean canHandle(Element element, Property property, ElementOrPropertyStatus status) {
        String propertyKey = property == null ? null : property.getKey();
        String propertyName = property == null ? null : property.getName();
        return canHandle(element, propertyKey, propertyName, status);
    }

    private boolean canHandle(Element element, String propertyKey, String propertyName, ElementOrPropertyStatus status) {
        if (this.graphPropertyRunner == null) {
            return true;
        }
        if (propertyKey == null && propertyName == null) {
            return true;
        }

        return this.graphPropertyRunner.canHandle(element, propertyKey, propertyName, status);
    }


    private GraphPropertyMessage createPropertySpecificMessage(
            String propertyKey,
            String propertyName,
            String workspaceId,
            String visibilitySource,
            ElementOrPropertyStatus status,
            Long beforeActionTimestamp,
            Priority priority
    ) {
        GraphPropertyMessage data = new GraphPropertyMessage();

        if (workspaceId != null && !workspaceId.equals("")) {
            data.setWorkspaceId(workspaceId);
            data.setVisibilitySource(visibilitySource);
        }
        data.setPropertyKey(propertyKey);
        data.setPropertyName(propertyName);
        data.setStatus(status);
        data.setPriority(priority);
        if (status == ElementOrPropertyStatus.DELETION || status == ElementOrPropertyStatus.HIDDEN) {
            checkNotNull(beforeActionTimestamp, "Timestamp before " + status + " cannot be null");
        }
        data.setBeforeActionTimestamp(beforeActionTimestamp);
        return data;
    }

    public void pushGraphPropertyQueue(Element element, Priority priority) {
        pushGraphPropertyQueue(element, null, null, priority, FlushFlag.DEFAULT);
    }

    public void pushGraphPropertyQueue(
            Element element,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            FlushFlag flushFlag
    ) {
        getGraph().flush();
        checkNotNull(element);
        GraphPropertyMessage data = new GraphPropertyMessage();
        data.setPriority(priority);
        addElementTypeToJson(data, element);

        if (workspaceId != null && !workspaceId.equals("")) {
            data.setWorkspaceId(workspaceId);
            data.setVisibilitySource(visibilitySource);
        }

        if (canHandle(element, null, null)) {
            pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);
        }
    }

    public void pushVertexIds(Iterable<String> vertexIds, Priority priority, FlushFlag flushFlag) {
        for (String vertexId : vertexIds) {
            pushVertexId(vertexId, priority, flushFlag);
        }
    }

    private void pushVertexId(String vertexId, Priority priority, FlushFlag flushFlag) {
        GraphPropertyMessage data = new GraphPropertyMessage();
        data.setPriority(priority);
        data.setGraphVertexId(new String[]{vertexId});
        pushOnQueue(workQueueNames.getGraphPropertyQueueName(), data.toBytes(), priority);
    }

    protected boolean shouldBroadcastGraphPropertyChange(
            Element element,
            String propertyKey,
            String propertyName,
            String workspaceId,
            Priority priority
    ) {
        return shouldBroadcast(priority) &&
                !MediaOpenLumifyProperties.VIDEO_FRAME.getPropertyName().equals(propertyName);
    }

    protected boolean shouldBroadcastTextUpdate(String vertexId, Priority priority) {
        return shouldBroadcast(priority);
    }

    protected boolean shouldBroadcast(Priority priority) {
        return priority != Priority.LOW;
    }

    public void pushLongRunningProcessQueue(JSONObject queueItem) {
        pushLongRunningProcessQueue(queueItem, Priority.NORMAL);
    }

    public void pushLongRunningProcessQueue(JSONObject queueItem, Priority priority) {
        broadcastLongRunningProcessChange(queueItem);
        pushOnQueue(workQueueNames.getLongRunningProcessQueueName(), FlushFlag.DEFAULT, queueItem, priority);
    }

    public void pushLongRunningProcessDeadLetterQueue(JSONObject queueItem) {
        broadcastLongRunningProcessChange(queueItem);
        pushOnDeadLetterQueue(workQueueNames.getLongRunningProcessQueueName(), queueItem);
    }

    public void broadcast(String type, JSONObject data, JSONObject permissions) {
        checkNotNull(type);

        JSONObject json = new JSONObject();
        json.putOpt("permissions", permissions);
        json.putOpt("data", data);
        json.put("type", type);
        broadcastJson(json);
    }

    public void broadcastLongRunningProcessDeleted(JSONObject longRunningProcessQueueItem) {
        String userId = longRunningProcessQueueItem.optString("userId");
        checkNotNull(userId, "userId cannot be null");
        JSONObject json = new JSONObject();
        json.put("type", "longRunningProcessDeleted");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        JSONObject data = new JSONObject();
        data.put("processId", longRunningProcessQueueItem.get("id"));
        json.put("data", data);
        broadcastJson(json);
    }

    public void broadcastLongRunningProcessChange(JSONObject longRunningProcessQueueItem) {
        String userId = longRunningProcessQueueItem.optString("userId");
        checkNotNull(userId, "userId cannot be null");
        JSONObject json = new JSONObject();
        json.put("type", "longRunningProcessChange");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        JSONObject dataJson = new JSONObject(longRunningProcessQueueItem.toString());

        /// because results can get quite large we don't want this going on in a web socket message
        if (dataJson.has("results")) {
            dataJson.remove("results");
        }

        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void broadcastWorkProductAncillaryChange(String workProductId, String workspaceId, String ancillaryId, User user, String skipSourceGuid) {
        JSONObject json = new JSONObject();
        json.put("type", "workProductAncillaryChange");
        json.put("permissions", getPermissionsWithWorkspace(workspaceId));
        JSONObject dataJson = new JSONObject();
        dataJson.put("id", ancillaryId);
        dataJson.put("workspaceId", workspaceId);
        dataJson.put("productId", workProductId);
        dataJson.putOpt("sourceGuid", skipSourceGuid);
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void broadcastWorkProductChange(String workProductId, String workspaceId, User user, String skipSourceGuid) {
        broadcastWorkProductChange(workProductId, skipSourceGuid, workspaceId, getPermissionsWithWorkspace(workspaceId));
    }

    public void broadcastWorkProductChange(String workProductId, ClientApiWorkspace workspace, User user, String skipSourceGuid) {
        broadcastWorkProductChange(workProductId, skipSourceGuid, workspace.getWorkspaceId(), getPermissionsWithUsers(workspace, null));
    }

    public void broadcastWorkProductPreviewChange(String workProductId, String workspaceId, User user, String md5) {
        JSONObject json = new JSONObject();
        json.put("type", "workProductPreviewChange");

        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(user.getUserId());
        permissions.put("users", users);
        json.put("permissions", permissions);

        JSONObject dataJson = new JSONObject();
        dataJson.put("id", workProductId);
        dataJson.put("workspaceId", workspaceId);
        dataJson.putOpt("md5", md5);
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void broadcastWorkProductDelete(String workProductId, ClientApiWorkspace workspace) {
        JSONObject json = new JSONObject();
        json.put("type", "workProductDelete");
        json.put("permissions", getPermissionsWithUsers(workspace, null));
        JSONObject dataJson = new JSONObject();
        dataJson.put("id", workProductId);
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void broadcastElement(Element element, String workspaceId) {
        broadcastPropertyChange(element, null, null, workspaceId);
    }

    public void pushElement(Element element, Priority priority) {
        pushGraphPropertyQueue(element, null, null, priority);
    }

    public void pushElement(Element element, long beforeDeletionTimestamp, Priority priority) {
        pushGraphPropertyQueue(element, (String) null, null, beforeDeletionTimestamp, priority);
    }

    public void pushElements(Iterable<? extends Element> elements, Priority priority) {
        pushMultipleGraphPropertyQueue(
                elements,
                null,
                null,
                null,
                null,
                priority,
                ElementOrPropertyStatus.UPDATE,
                null,
                FlushFlag.DEFAULT
        );
    }

    public void pushElement(Element element) {
        pushElement(element, Priority.NORMAL);
    }

    public void pushEdgeDeletion(Edge edge, long beforeDeletionTimestamp, Priority priority) {
        broadcastEdgeDeletion(edge);
        pushGraphPropertyQueue(edge, null, null, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushPublishedEdgeDeletion(Edge edge, long beforeDeletionTimestamp, Priority priority) {
        broadcastPublishEdgeDelete(edge);
        pushGraphPropertyQueue(edge, null, null, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushPublishedVertexDeletion(Vertex vertex, long beforeDeletionTimestamp, Priority priority) {
        broadcastPublishVertexDelete(vertex);
        pushGraphPropertyQueue(vertex, null, null, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushPublishedPropertyDeletion(
            Element element,
            String key,
            String name,
            long beforeDeletionTimestamp,
            Priority priority
    ) {
        broadcastPublishPropertyDelete(element, key, name);
        pushGraphPropertyQueue(element, key, name, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushUndoPublicPropertyDeletion(Element element, String key, String name, Priority priority) {
        broadcastUndoPropertyDelete(element, key, name);
        pushGraphPropertyQueue(element, key, name, ElementOrPropertyStatus.UNHIDDEN, null, priority);
    }

    public void pushUndoSandboxProperty(
            Element element,
            String key,
            String name,
            long beforeDeletionTimestamp,
            Priority priority
    ) {
        broadcastUndoPropertyDelete(element, key, name);
        pushGraphPropertyQueue(element, key, name, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushEdgeHidden(Edge edge, long beforeHiddenTimestamp, Priority priority) {
        broadcastEdgeDeletion(edge);
        pushGraphPropertyQueue(edge, null, null, ElementOrPropertyStatus.HIDDEN, beforeHiddenTimestamp, priority);
    }

    public void pushEdgeUnhidden(Edge edge, Priority priority) {
        broadcastUndoEdgeDelete(edge);
        pushGraphPropertyQueue(edge, null, null, ElementOrPropertyStatus.UNHIDDEN, null, priority);
    }

    public void pushVertexUnhidden(Vertex vertex, Priority priority) {
        broadcastUndoVertexDelete(vertex);
        pushGraphPropertyQueue(vertex, null, null, ElementOrPropertyStatus.UNHIDDEN, null, priority);
    }

    protected void broadcastEdgeDeletion(Edge edge) {
        JSONObject dataJson = new JSONObject();
        if (edge != null) {
            dataJson.put("edgeId", edge.getId());
            dataJson.put("outVertexId", edge.getVertexId(Direction.OUT));
            dataJson.put("inVertexId", edge.getVertexId(Direction.IN));
        }

        JSONObject json = new JSONObject();
        json.put("type", "edgeDeletion");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushVertexDeletion(Vertex vertex, long beforeDeletionTimestamp, Priority priority) {
        pushVertexDeletion(vertex.getId());
        pushGraphPropertyQueue(vertex, null, null, ElementOrPropertyStatus.DELETION, beforeDeletionTimestamp, priority);
    }

    public void pushVertexHidden(Vertex vertex, long beforeHiddenTimestamp, Priority priority) {
        pushVertexDeletion(vertex.getId());
        pushGraphPropertyQueue(vertex, null, null, ElementOrPropertyStatus.HIDDEN, beforeHiddenTimestamp, priority);
    }

    public void pushVertexDeletion(String vertexId) {
        JSONArray verticesDeleted = new JSONArray();
        verticesDeleted.put(vertexId);
        broadcastVerticesDeletion(verticesDeleted);
    }

    public void pushVerticesDeletion(JSONArray verticesDeleted) {
        broadcastVerticesDeletion(verticesDeleted);
    }

    protected void broadcastVerticesDeletion(JSONArray verticesDeleted) {
        JSONObject dataJson = new JSONObject();
        if (verticesDeleted != null) {
            dataJson.put("vertexIds", verticesDeleted);
        }

        JSONObject json = new JSONObject();
        json.put("type", "verticesDeleted");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushOntologyChange(String workspaceId) {
        pushOntologyChange(workspaceId, null, null, null);
    }

    public void pushOntologyChange(String workspaceId, Iterable<String> conceptIds, Iterable<String> relationshipIds, Iterable<String> propertyIds) {
        pushOntologyChange(workspaceId, OntologyAction.Update, conceptIds, relationshipIds, propertyIds);
    }

    public void pushOntologyChange(String workspaceId, OntologyAction action, Iterable<String> conceptIds, Iterable<String> relationshipIds, Iterable<String> propertyIds) {
        JSONObject json = new JSONObject();
        json.put("type", "ontologyChange");

        JSONObject data = new JSONObject();
        data.put("action", action.toString());
        data.put("idType", action.equals(OntologyAction.Update) ? "id" : "iri");

        if (workspaceId != null) {
            data.put("workspaceId", workspaceId);
            json.put("permissions", getPermissionsWithWorkspace(workspaceId));
        }
        if (conceptIds != null || relationshipIds != null || propertyIds != null) {
            data.put("conceptIds", conceptIds == null ? new JSONArray() : new JSONArray(Sets.newHashSet(conceptIds)));
            data.put("propertyIds", propertyIds == null ? new JSONArray() : new JSONArray(Sets.newHashSet(propertyIds)));
            data.put("relationshipIds", relationshipIds == null ? new JSONArray() : new JSONArray(Sets.newHashSet(relationshipIds)));
        }
        json.put("data", data);
        broadcastJson(json);
    }

    public void pushOntologyConceptsChange(String workspaceId, List<String> ids) {
        pushOntologyChange(workspaceId, ids, null, null);
    }

    public void pushOntologyConceptsChange(String workspaceId, String... ids) {
        pushOntologyChange(workspaceId, Arrays.asList(ids), null, null);
    }

    public void pushOntologyPropertiesChange(String workspaceId, String... ids) {
        pushOntologyChange(workspaceId, null, null, Arrays.asList(ids));
    }

    public void pushOntologyPropertiesChange(String workspaceId, List<String> ids) {
        pushOntologyChange(workspaceId, null, null, ids);
    }

    public void pushOntologyRelationshipsChange(String workspaceId, List<String> ids) {
        pushOntologyChange(workspaceId, null, ids, null);
    }

    public void pushOntologyRelationshipsChange(String workspaceId, String... ids) {
        pushOntologyChange(workspaceId, null, Arrays.asList(ids), null);
    }

    public void pushTextUpdated(String vertexId) {
        pushTextUpdated(vertexId, Priority.NORMAL);
    }

    public void pushTextUpdated(String vertexId, Priority priority) {
        if (shouldBroadcastTextUpdate(vertexId, priority)) {
            broadcastTextUpdated(vertexId);
        }
    }

    protected void broadcastTextUpdated(String vertexId) {
        JSONObject dataJson = new JSONObject();
        if (vertexId != null) {
            dataJson.put("graphVertexId", vertexId);
        }

        JSONObject json = new JSONObject();
        json.put("type", "textUpdated");
        json.put("data", dataJson);
        broadcastJson(json);
    }

    public void pushUserAccessChange(User user) {
        JSONObject json = new JSONObject();
        json.put("type", "userAccessChange");
        json.put("permissions", getPermissionsWithUserIds(user.getUserId()));
        json.put("data", getUserRepository().toJsonWithAuths(user));
        broadcastJson(json);
    }

    public void pushUserCurrentWorkspaceChange(User user, String workspaceId) {
        broadcastUserWorkspaceChange(user, workspaceId);
    }

    public void pushWorkspaceChange(
            ClientApiWorkspace workspace,
            List<ClientApiWorkspace.User> previousUsers,
            String changedByUserId,
            String changedBySourceGuid
    ) {
        broadcastWorkspace(workspace, previousUsers, changedByUserId, changedBySourceGuid);
    }


    protected void broadcastWorkProductChange(String workProductId, String skipSourceGuid, String workspaceId, JSONObject permissions) {
        JSONObject json = new JSONObject();
        json.put("type", "workProductChange");
        json.put("permissions", permissions);
        JSONObject dataJson = new JSONObject();
        dataJson.put("id", workProductId);
        dataJson.put("workspaceId", workspaceId);
        dataJson.putOpt("sourceGuid", skipSourceGuid);
        json.put("data", dataJson);
        broadcastJson(json);
    }

    protected void broadcastUserWorkspaceChange(User user, String workspaceId) {
        JSONObject json = new JSONObject();
        json.put("type", "userWorkspaceChange");
        JSONObject data = UserRepository.toJson(user);
        data.put("workspaceId", workspaceId);
        json.put("data", data);
        broadcastJson(json);
    }

    protected void broadcastWorkspace(
            ClientApiWorkspace workspace,
            List<ClientApiWorkspace.User> previousUsers,
            String changedByUserId,
            String changedBySourceGuid
    ) {
        User changedByUser = getUserRepository().findById(changedByUserId);
        Workspace ws = getWorkspaceRepository().findById(workspace.getWorkspaceId(), changedByUser);

        previousUsers.forEach(workspaceUser -> {
            boolean isChangingUser = workspaceUser.getUserId().equals(changedByUserId);

            User user = isChangingUser ? changedByUser : getUserRepository().findById(workspaceUser.getUserId());
            Authorizations authorizations = getAuthorizationRepository().getGraphAuthorizations(user, workspace.getWorkspaceId());

            // No need to regenerate client api if changing user
            try {
                ClientApiWorkspace userWorkspace = isChangingUser ? workspace : getWorkspaceRepository().toClientApi(ws, user, authorizations);
                JSONObject json = new JSONObject();
                json.put("type", "workspaceChange");
                json.put("modifiedBy", changedByUserId);
                json.put("permissions", getPermissionsWithUsers(null, Arrays.asList(workspaceUser)));
                json.put("data", new JSONObject(ClientApiConverter.clientApiToString(userWorkspace)));
                json.putOpt("sourceGuid", changedBySourceGuid);
                broadcastJson(json);
            } catch (OpenLumifyAccessDeniedException e) {
                /* Ignore push message if lost access */
            }
        });
    }

    public void pushWorkspaceDelete(ClientApiWorkspace workspace) {
        JSONObject json = new JSONObject();
        json.put("type", "workspaceDelete");
        json.put("permissions", getPermissionsWithUsers(workspace, null));
        json.put("workspaceId", workspace.getWorkspaceId());
        broadcastJson(json);
    }

    public void pushWorkspaceDelete(String workspaceId, String userId) {
        JSONObject json = new JSONObject();
        json.put("type", "workspaceDelete");
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(userId);
        permissions.put("users", users);
        json.put("permissions", permissions);
        json.put("workspaceId", workspaceId);
        broadcastJson(json);
    }

    private JSONObject getPermissionsWithUserIds(String... userIds) {
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        for (String userId : userIds) {
            users.put(userId);
        }
        permissions.put("users", users);
        return permissions;
    }

    private JSONObject getPermissionsWithWorkspace(String workspaceId) {
        List<WorkspaceUser> users = getWorkspaceRepository().findUsersWithAccess(workspaceId, new SystemUser());
        return getPermissionsWithUsers(users);
    }

    private JSONObject getPermissionsWithUsers(List<WorkspaceUser> workspaceUsers) {
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        if (workspaceUsers != null) {
            for (WorkspaceUser workspaceUser : workspaceUsers) {
                users.put(workspaceUser.getUserId());
            }
        }
        permissions.put("users", users);
        return permissions;
    }

    private JSONObject getPermissionsWithUsers(
            ClientApiWorkspace workspace,
            List<ClientApiWorkspace.User> previousUsers
    ) {
        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        if (previousUsers != null) {
            for (ClientApiWorkspace.User user : previousUsers) {
                users.put(user.getUserId());
            }
        }
        if (workspace != null) {
            for (ClientApiWorkspace.User user : workspace.getUsers()) {
                users.put(user.getUserId());
            }
        }
        permissions.put("users", users);
        return permissions;
    }

    public void pushUserNotification(UserNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "notification");

        JSONObject permissions = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(notification.getUserId());
        permissions.put("users", users);
        json.put("permissions", permissions);

        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotification(SystemNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "notification");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotificationUpdate(SystemNotification notification) {
        JSONObject json = new JSONObject();
        json.put("type", "systemNotificationUpdated");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notification", notification.toJSONObject());
        broadcastJson(json);
    }

    public void pushSystemNotificationEnded(String notificationId) {
        JSONObject json = new JSONObject();
        json.put("type", "systemNotificationEnded");
        JSONObject data = new JSONObject();
        json.put("data", data);
        data.put("notificationId", notificationId);
        broadcastJson(json);
    }

    protected void broadcastPropertyChange(
            Element element,
            String propertyKey,
            String propertyName,
            String workspaceId
    ) {
        try {
            JSONObject json;
            if (element instanceof Vertex) {
                json = getBroadcastPropertyChangeJson((Vertex) element, propertyKey, propertyName, workspaceId);
            } else if (element instanceof Edge) {
                json = getBroadcastPropertyChangeJson((Edge) element, propertyKey, propertyName, workspaceId);
            } else {
                throw new OpenLumifyException("Unexpected element type: " + element.getClass().getName());
            }
            broadcastJson(json);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not broadcast property change", ex);
        }
    }

    protected void broadcastEntityImage(Element element, String propertyKey, String propertyName) {
        try {
            JSONObject json = getBroadcastEntityImageJson((Vertex) element);
            broadcastJson(json);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not broadcast property change", ex);
        }
    }

    protected abstract void broadcastJson(JSONObject json);

    protected JSONObject getBroadcastEntityImageJson(Vertex graphVertex) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "entityImageUpdated");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());

        json.put("data", dataJson);
        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(
            Vertex graphVertex,
            String propertyKey,
            String propertyName,
            String workspaceId
    ) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "propertyChange");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());
        dataJson.putOpt("workspaceId", workspaceId);

        json.put("data", dataJson);

        return json;
    }

    protected JSONObject getBroadcastPropertyChangeJson(
            Edge edge,
            String propertyKey,
            String propertyName,
            String workspaceId
    ) {
        // TODO: only broadcast to workspace users if sandboxStatus is PRIVATE
        JSONObject json = new JSONObject();
        json.put("type", "propertyChange");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphEdgeId", edge.getId());
        dataJson.put("outVertexId", edge.getVertexId(Direction.OUT));
        dataJson.put("inVertexId", edge.getVertexId(Direction.IN));
        dataJson.putOpt("workspaceId", workspaceId);

        json.put("data", dataJson);

        return json;
    }

    public final void pushOnQueue(
            String queueName,
            @Deprecated FlushFlag flushFlag,
            JSONObject json,
            Priority priority
    ) {
        if (priority != null) {
            json.put("priority", priority.name());
        }
        pushOnQueue(queueName, json.toString().getBytes(), priority);
    }

    public abstract void pushOnQueue(
            String queueName,
            byte[] data,
            Priority priority
    );

    public final void pushOnDeadLetterQueue(String queueName, JSONObject json) {
        pushOnDeadLetterQueue(queueName, json.toString().getBytes());
    }

    public abstract void pushOnDeadLetterQueue(String queueName, byte[] data);

    public void init(Map map) {

    }

    public abstract void flush();

    public void format() {
        for (String queueName : getQueueNames()) {
            LOGGER.info("deleting queue: %s", queueName);
            deleteQueue(queueName);
        }
    }

    protected abstract void deleteQueue(String queueName);

    protected Iterable<String> getQueueNames() {
        List<String> queueNames = new ArrayList<>();
        queueNames.add(getWorkQueueNames().getGraphPropertyQueueName());
        queueNames.add(getWorkQueueNames().getLongRunningProcessQueueName());

        Collection<ExternalResourceWorker> externalResourceWorkers =
                InjectHelper.getInjectedServices(ExternalResourceWorker.class, getConfiguration());
        for (ExternalResourceWorker externalResourceWorker : externalResourceWorkers) {
            if (!(externalResourceWorker instanceof QueueExternalResourceWorker)) {
                continue;
            }
            String queueName = ((QueueExternalResourceWorker) externalResourceWorker).getQueueName();
            queueNames.add(queueName);
        }
        return queueNames;
    }

    public Graph getGraph() {
        return graph;
    }

    public abstract void subscribeToBroadcastMessages(BroadcastConsumer broadcastConsumer);

    public abstract void unsubscribeFromBroadcastMessages(BroadcastConsumer broadcastConsumer);

    public abstract WorkerSpout createWorkerSpout(String queueName);

    public void broadcastPublishVertexDelete(Vertex vertex) {
        broadcastPublish(vertex, PublishType.DELETE);
    }

    public void broadcastPublishVertex(Vertex vertex) {
        broadcastPublish(vertex, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoVertexDelete(Vertex vertex) {
        broadcastPublish(vertex, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoVertex(Vertex vertex) {
        broadcastPublish(vertex, PublishType.UNDO);
    }

    public void broadcastPublishPropertyDelete(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.DELETE);
    }

    public void broadcastPublishProperty(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoPropertyDelete(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoProperty(Element element, String key, String name) {
        broadcastPublish(element, key, name, PublishType.UNDO);
    }

    public void broadcastPublishEdgeDelete(Edge edge) {
        broadcastPublish(edge, PublishType.DELETE);
    }

    public void broadcastPublishEdge(Edge edge) {
        broadcastPublish(edge, PublishType.TO_PUBLIC);
    }

    public void broadcastUndoEdgeDelete(Edge edge) {
        broadcastPublish(edge, PublishType.UNDO_DELETE);
    }

    public void broadcastUndoEdge(Edge edge) {
        broadcastPublish(edge, PublishType.UNDO);
    }

    private void broadcastPublish(Element element, PublishType publishType) {
        broadcastPublish(element, null, null, publishType);
    }

    private void broadcastPublish(Element element, String propertyKey, String propertyName, PublishType publishType) {
        try {
            JSONObject json;
            if (element instanceof Vertex) {
                json = getBroadcastPublishJson((Vertex) element, propertyKey, propertyName, publishType);
            } else if (element instanceof Edge) {
                json = getBroadcastPublishJson((Edge) element, propertyKey, propertyName, publishType);
            } else {
                throw new OpenLumifyException("Unexpected element type: " + element.getClass().getName());
            }
            broadcastJson(json);
        } catch (Exception ex) {
            throw new OpenLumifyException("Could not broadcast publish", ex);
        }
    }

    protected JSONObject getBroadcastPublishJson(
            Vertex graphVertex,
            String propertyKey,
            String propertyName,
            PublishType publishType
    ) {
        JSONObject json = new JSONObject();
        json.put("type", "publish");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphVertexId", graphVertex.getId());
        dataJson.put("publishType", publishType.getJsonString());
        if (propertyName == null) {
            dataJson.put("objectType", "vertex");
        } else {
            dataJson.put("objectType", "property");
        }
        json.put("data", dataJson);

        return json;
    }

    protected JSONObject getBroadcastPublishJson(
            Edge edge,
            String propertyKey,
            String propertyName,
            PublishType publishType
    ) {
        JSONObject json = new JSONObject();
        json.put("type", "publish");

        JSONObject dataJson = new JSONObject();
        dataJson.put("graphEdgeId", edge.getId());
        dataJson.put("publishType", publishType.getJsonString());
        if (propertyName == null) {
            dataJson.put("objectType", "edge");
        } else {
            dataJson.put("objectType", "property");
        }
        json.put("data", dataJson);

        return json;
    }

    public void setGraphPropertyRunner(GraphPropertyRunner graphPropertyRunner) {
        this.graphPropertyRunner = graphPropertyRunner;
    }

    private enum PublishType {
        TO_PUBLIC("toPublic"),
        DELETE("delete"),
        UNDO_DELETE("undoDelete"),
        UNDO("undo");

        private final String jsonString;

        PublishType(String jsonString) {
            this.jsonString = jsonString;
        }

        public String getJsonString() {
            return jsonString;
        }
    }

    public static abstract class BroadcastConsumer {
        private String consumerKey;

        public abstract void broadcastReceived(JSONObject json);

        public String getConsumerKey() {
            return consumerKey;
        }

        public void setConsumerKey(String consumerKey) {
            this.consumerKey = consumerKey;
        }
    }

    protected WorkQueueNames getWorkQueueNames() {
        return workQueueNames;
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected AuthorizationRepository getAuthorizationRepository() {
        if (authorizationRepository == null) {
            authorizationRepository = InjectHelper.getInstance(AuthorizationRepository.class);
        }
        return authorizationRepository;
    }

    public void setAuthorizationRepository(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    protected WorkspaceRepository getWorkspaceRepository() {
        if (workspaceRepository == null) {
            workspaceRepository = InjectHelper.getInstance(WorkspaceRepository.class);
        }
        return workspaceRepository;
    }

    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    protected UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = InjectHelper.getInstance(UserRepository.class);
        }
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public enum OntologyAction {
        Update,
        Delete
    }
}
