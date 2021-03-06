package org.openlumify.core.ingest.graphProperty;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Graph;
import org.vertexium.property.StreamingPropertyValue;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.workQueue.WorkQueueRepository;
import org.openlumify.core.user.User;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class PostMimeTypeWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(PostMimeTypeWorker.class);
    private Graph graph;
    private WorkQueueRepository workQueueRepository;
    private File localFileForRaw;
    private GraphPropertyWorkerPrepareData workerPrepareData;

    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.workerPrepareData = workerPrepareData;
    }

    protected abstract void execute(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception;

    public void executeAndCleanup(String mimeType, GraphPropertyWorkData data, Authorizations authorizations) throws Exception {
        try {
            execute(mimeType, data, authorizations);
        } finally {
            if (localFileForRaw != null) {
                if (!localFileForRaw.delete()) {
                    LOGGER.warn("Could not delete local file: %s", localFileForRaw.getAbsolutePath());
                }
                localFileForRaw = null;
            }
        }
    }

    protected File getLocalFileForRaw(Element element) throws IOException {
        if (localFileForRaw != null) {
            return localFileForRaw;
        }
        StreamingPropertyValue rawValue = OpenLumifyProperties.RAW.getPropertyValue(element);
        try (InputStream in = rawValue.getInputStream()) {
            String suffix = "-" + element.getId().replaceAll("\\W", "_");
            localFileForRaw = File.createTempFile(PostMimeTypeWorker.class.getName() + "-", suffix);
            try (FileOutputStream out = new FileOutputStream(localFileForRaw)) {
                IOUtils.copy(in, out);
                return localFileForRaw;
            }
        }
    }

    protected User getUser() {
        return this.workerPrepareData.getUser();
    }

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    protected Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    protected WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }
}
