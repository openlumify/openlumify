package org.openlumify.core.model.properties.types;

import org.openlumify.core.exception.OpenLumifyException;
import org.openlumify.core.ingest.video.VideoTranscript;
import org.vertexium.property.StreamingPropertyValue;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VideoTranscriptProperty extends OpenLumifyProperty<VideoTranscript, StreamingPropertyValue> {
    public VideoTranscriptProperty(String inKey) {
        super(inKey);
    }

    @Override
    public StreamingPropertyValue wrap(VideoTranscript value) {
        InputStream in = new ByteArrayInputStream(value.toJson().toString().getBytes());
        StreamingPropertyValue result = StreamingPropertyValue.create(in, byte[].class);
        result.searchIndex(false);
        return result;
    }

    @Override
    public VideoTranscript unwrap(Object value) {
        String strValue = null;
        if (value instanceof StreamingPropertyValue) {
            try {
                strValue = IOUtils.toString(((StreamingPropertyValue) value).getInputStream());
            } catch (IOException e) {
                throw new OpenLumifyException("Could not read propery value", e);
            }
        } else if (value != null) {
            strValue = value.toString();
        }
        JSONObject json = new JSONObject(strValue);
        return new VideoTranscript(json);
    }
}
