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
package io.confluent.kafka.serializers;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

public class KafkaAvroSerializer extends AbstractKafkaAvroSerializer implements Serializer<Object> {

  private boolean isKey;

  /**
   * Constructor used by Kafka producer.
   */
  public KafkaAvroSerializer() {

  }

  public KafkaAvroSerializer(SchemaRegistryClient client, boolean enableAutoSchemaRegistration) {
    schemaRegistry = client;
    this.enableAutoSchemaRegistry = enableAutoSchemaRegistration;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    this.isKey = isKey;
    Object url = configs.get(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG);
    if (url == null) {
      throw new ConfigException("Missing Schema registry url!");
    }
    Object maxSchemaObject = configs.get(
        AbstractKafkaAvroSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_CONFIG);
    if (maxSchemaObject == null) {
      schemaRegistry = new CachedSchemaRegistryClient(
          (String) url, AbstractKafkaAvroSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_DEFAULT);
    } else {
      schemaRegistry = new CachedSchemaRegistryClient(
          (String) url, (Integer) maxSchemaObject);
    }
    enableAutoSchemaRegistry = (Boolean) configs.get(KafkaAvroSerializerConfig.ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG);
  }

  @Override
  public byte[] serialize(String topic, Object record) {
    String subject;
    if (isKey) {
      subject = topic + "-key";
    } else {
      subject = topic + "-value";
    }
    return serializeImpl(subject, record);
  }

  @Override
  public void close() {

  }

}
