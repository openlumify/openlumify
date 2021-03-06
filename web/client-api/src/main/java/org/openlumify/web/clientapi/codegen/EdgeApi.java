package org.openlumify.web.clientapi.codegen;

import org.openlumify.web.clientapi.codegen.ApiException;
import org.openlumify.web.clientapi.ApiInvoker;

import org.openlumify.web.clientapi.model.ClientApiEdgesExistsResponse;
import org.openlumify.web.clientapi.model.ClientApiElementAcl;
import org.openlumify.web.clientapi.model.ClientApiHistoricalPropertyResults;
import org.openlumify.web.clientapi.model.ClientApiEdgeWithVertexData;
import com.sun.jersey.multipart.FormDataMultiPart;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;

public class EdgeApi {
  protected String basePath = "http://openlumify-dev:8889";
  protected ApiInvoker apiInvoker = ApiInvoker.getInstance();

  public ApiInvoker getInvoker() {
    return apiInvoker;
  }
  
  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }
  
  public String getBasePath() {
    return basePath;
  }

  //error info- code: 404 reason: "Edge not found" model: <none>
  public ClientApiEdgeWithVertexData getByEdgeId (String graphEdgeId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphEdgeId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/properties".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphEdgeId)))
      queryParams.put("graphEdgeId", String.valueOf(graphEdgeId));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiEdgeWithVertexData) ApiInvoker.deserialize(response, "", ClientApiEdgeWithVertexData.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public ClientApiHistoricalPropertyResults getPropertyHistory (String graphEdgeId, String propertyKey, String propertyName, Long startTime, Long endTime) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(graphEdgeId == null || propertyKey == null || propertyName == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/property/history".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(graphEdgeId)))
      queryParams.put("graphEdgeId", String.valueOf(graphEdgeId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
    if(!"null".equals(String.valueOf(propertyName)))
      queryParams.put("propertyName", String.valueOf(propertyName));
    if(!"null".equals(String.valueOf(startTime)))
      queryParams.put("startTime", String.valueOf(startTime));
    if(!"null".equals(String.valueOf(endTime)))
      queryParams.put("endTime", String.valueOf(endTime));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiHistoricalPropertyResults) ApiInvoker.deserialize(response, "", ClientApiHistoricalPropertyResults.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public ClientApiEdgeWithVertexData create (String outVertexId, String inVertexId, String predicateLabel, String visibilitySource, String justificationText, String sourceInfo, String edgeId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(outVertexId == null || inVertexId == null || predicateLabel == null || visibilitySource == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/create".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(outVertexId)))
      queryParams.put("outVertexId", String.valueOf(outVertexId));
    if(!"null".equals(String.valueOf(inVertexId)))
      queryParams.put("inVertexId", String.valueOf(inVertexId));
    if(!"null".equals(String.valueOf(predicateLabel)))
      queryParams.put("predicateLabel", String.valueOf(predicateLabel));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(justificationText)))
      queryParams.put("justificationText", String.valueOf(justificationText));
    if(!"null".equals(String.valueOf(sourceInfo)))
      queryParams.put("sourceInfo", String.valueOf(sourceInfo));
    if(!"null".equals(String.valueOf(edgeId)))
      queryParams.put("edgeId", String.valueOf(edgeId));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiEdgeWithVertexData) ApiInvoker.deserialize(response, "", ClientApiEdgeWithVertexData.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public void setProperty (String edgeId, String propertyKey, String propertyName, String value, String visibilitySource, String justificationString, String sourceInfo, String metadata) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(edgeId == null || propertyKey == null || propertyName == null || value == null || visibilitySource == null || justificationString == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/property".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(edgeId)))
      queryParams.put("edgeId", String.valueOf(edgeId));
    if(!"null".equals(String.valueOf(propertyKey)))
      queryParams.put("propertyKey", String.valueOf(propertyKey));
    if(!"null".equals(String.valueOf(propertyName)))
      queryParams.put("propertyName", String.valueOf(propertyName));
    if(!"null".equals(String.valueOf(value)))
      queryParams.put("value", String.valueOf(value));
    if(!"null".equals(String.valueOf(visibilitySource)))
      queryParams.put("visibilitySource", String.valueOf(visibilitySource));
    if(!"null".equals(String.valueOf(justificationString)))
      queryParams.put("justificationString", String.valueOf(justificationString));
    if(!"null".equals(String.valueOf(sourceInfo)))
      queryParams.put("sourceInfo", String.valueOf(sourceInfo));
    if(!"null".equals(String.valueOf(metadata)))
      queryParams.put("metadata", String.valueOf(metadata));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return ;
      }
      else {
        return ;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return ;
      }
      else {
        throw ex;
      }
    }
  }
  public void deleteEdge (String edgeId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(edgeId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(edgeId)))
      queryParams.put("edgeId", String.valueOf(edgeId));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return ;
      }
      else {
        return ;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return ;
      }
      else {
        throw ex;
      }
    }
  }
  public ClientApiEdgesExistsResponse doExist (List<String> edgeIds) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(edgeIds == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/exists".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    String[] contentTypes = {
      "multipart/form-data"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      hasFields = true;
      if(edgeIds != null) { for(String edgeId:edgeIds) { mp.field("edgeIds[]", edgeId, MediaType.MULTIPART_FORM_DATA_TYPE); } }
      if(hasFields && !mp.getFields().isEmpty())
        postBody = mp;
    }
    else {
      throw new java.lang.RuntimeException("invalid content type");}

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiEdgesExistsResponse) ApiInvoker.deserialize(response, "", ClientApiEdgesExistsResponse.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  public ClientApiElementAcl getAcl (String elementId) throws ApiException {
    Object postBody = null;
    // verify required params are set
    if(elementId == null ) {
       throw new ApiException(400, "missing required params");
    }
    // create path and map variables
    String path = "/edge/acl".replaceAll("\\{format\\}","json");

    // query params
    Map<String, String> queryParams = new HashMap<String, String>();
    Map<String, String> headerParams = new HashMap<String, String>();
    Map<String, String> formParams = new HashMap<String, String>();

    if(!"null".equals(String.valueOf(elementId)))
      queryParams.put("elementId", String.valueOf(elementId));
    String[] contentTypes = {
      "application/json"};

    String contentType = contentTypes.length > 0 ? contentTypes[0] : "application/json";

    if(contentType.startsWith("multipart/form-data")) {
      boolean hasFields = false;
      FormDataMultiPart mp = new FormDataMultiPart();
      if(hasFields)
        postBody = mp;
    }
    else {
      }

    try {
      String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, formParams, contentType);
      if(response != null){
        return (ClientApiElementAcl) ApiInvoker.deserialize(response, "", ClientApiElementAcl.class);
      }
      else {
        return null;
      }
    } catch (ApiException ex) {
      if(ex.getCode() == 404) {
      	return null;
      }
      else {
        throw ex;
      }
    }
  }
  }

