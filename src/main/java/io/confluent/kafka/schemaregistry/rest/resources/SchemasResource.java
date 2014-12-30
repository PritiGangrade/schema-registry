/**
 * Copyright 2014 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.kafka.schemaregistry.rest.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import io.confluent.kafka.schemaregistry.rest.RegisterSchemaForwardingAgent;
import io.confluent.kafka.schemaregistry.rest.Versions;
import io.confluent.kafka.schemaregistry.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.rest.entities.requests.RegisterSchemaRequest;
import io.confluent.kafka.schemaregistry.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.storage.SchemaRegistry;
import io.confluent.kafka.schemaregistry.storage.exceptions.SchemaRegistryException;

@Produces({Versions.SCHEMA_REGISTRY_V1_JSON_WEIGHTED,
           Versions.SCHEMA_REGISTRY_DEFAULT_JSON_WEIGHTED,
           Versions.JSON_WEIGHTED})
@Consumes({Versions.SCHEMA_REGISTRY_V1_JSON,
           Versions.SCHEMA_REGISTRY_DEFAULT_JSON,
           Versions.JSON, Versions.GENERIC_REQUEST})
public class SchemasResource {

  public final static String MESSAGE_SCHEMA_NOT_FOUND = "Schema not found.";
  private static final Logger log = LoggerFactory.getLogger(SchemasResource.class);

  private final String topic;
  private final boolean isKey;
  private final SchemaRegistry schemaRegistry;

  public SchemasResource(SchemaRegistry registry, String topic, boolean isKey) {
    this.schemaRegistry = registry;
    this.topic = topic;
    this.isKey = isKey;
  }

  @GET
  @Path("/{id}")
  public Schema getSchema(@PathParam("id") Integer id) {
    Schema schema = null;
    try {
      schema = schemaRegistry.get(this.topic, id);
    } catch (SchemaRegistryException e) {
      log.debug("Error while retrieving schema with id " + id + " from the schema registry",
                e);
      throw new NotFoundException(MESSAGE_SCHEMA_NOT_FOUND, e);
    }
    if (schema == null) {
      throw new NotFoundException(MESSAGE_SCHEMA_NOT_FOUND);
    }
    return schema;
  }

  @GET
  public List<Integer> list() {
    Iterator<Schema> allSchemasForThisTopic = null;
    List<Integer> allVersions = new ArrayList<Integer>();
    try {
      allSchemasForThisTopic = schemaRegistry.getAllVersions(this.topic);
    } catch (SchemaRegistryException e) {
      throw new ClientErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
    }
    while (allSchemasForThisTopic.hasNext()) {
      Schema schema = allSchemasForThisTopic.next();
      allVersions.add(schema.getVersion());
    }
    return allVersions;
  }

  @POST
  public void register(final @Suspended AsyncResponse asyncResponse,
                       final @HeaderParam("Content-Type") String contentType,
                       final @HeaderParam("Accept") String accept,
                       @PathParam("topic") String topicName, RegisterSchemaRequest request) {
    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put("Content-Type", contentType);
    requestProperties.put("Accept", accept);
    RegisterSchemaForwardingAgent forwardingAgent =
        new RegisterSchemaForwardingAgent(requestProperties, topicName, isKey, request);

    Schema schema = new Schema(topicName, 0, request.getSchema(), false);
    int version = -1;
    try {
      version = schemaRegistry.register(topicName, schema, forwardingAgent);
    } catch (SchemaRegistryException e) {
      throw new ClientErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
    }
    RegisterSchemaResponse registerSchemaResponse = new RegisterSchemaResponse();
    registerSchemaResponse.setVersion(version);
    asyncResponse.resume(registerSchemaResponse);
  }
}