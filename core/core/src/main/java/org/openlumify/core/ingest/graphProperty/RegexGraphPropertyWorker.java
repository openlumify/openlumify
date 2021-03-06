package org.openlumify.core.ingest.graphProperty;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.openlumify.core.model.ontology.Concept;
import org.openlumify.core.model.properties.OpenLumifyProperties;
import org.openlumify.core.model.termMention.TermMentionBuilder;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;
import org.openlumify.web.clientapi.model.VisibilityJson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RegexGraphPropertyWorker extends GraphPropertyWorker {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(RegexGraphPropertyWorker.class);
    private final Pattern pattern;

    public RegexGraphPropertyWorker(String regEx) {
        this.pattern = Pattern.compile(regEx, Pattern.MULTILINE);
    }

    protected abstract Concept getConcept();

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        LOGGER.debug("Extractor prepared for entity type [%s] with regular expression: %s", getConcept().getIRI(), this.pattern.toString());
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting pattern [%s] from provided text", pattern);

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        final Matcher matcher = pattern.matcher(text);

        Vertex outVertex = (Vertex) data.getElement();

        List<Vertex> termMentions = new ArrayList<>();
        while (matcher.find()) {
            final String patternGroup = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            VisibilityJson visibilityJson = VisibilityJson.updateVisibilitySourceAndAddWorkspaceId(null, null, data.getWorkspaceId());
            Vertex termMention = new TermMentionBuilder()
                    .outVertex(outVertex)
                    .propertyKey(data.getProperty().getKey())
                    .propertyName(data.getProperty().getName())
                    .start(start)
                    .end(end)
                    .title(patternGroup)
                    .conceptIri(getConcept().getIRI())
                    .visibilityJson(visibilityJson)
                    .process(getClass().getName())
                    .save(getGraph(), getVisibilityTranslator(), getUser(), getAuthorizations());
            termMentions.add(termMention);
        }
        applyTermMentionFilters(outVertex, termMentions);
        pushTextUpdated(data);
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(OpenLumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = (String) property.getMetadata().getValue(OpenLumifyProperties.MIME_TYPE.getPropertyName());
        return !(mimeType == null || !mimeType.startsWith("text"));
    }
}
