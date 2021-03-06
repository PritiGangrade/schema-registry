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

import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.SerializationException;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

/**
 * At present, this only works for value not key and it only supports IndexedRecord. To register for
 * a topic, user will need to provide a list of record_name:topic_name map as a CSV format. By
 * default, the encoder will use record name as topic.
 */
public class KafkaAvroEncoder extends AbstractKafkaAvroSerializer implements Encoder<Object> {

  public KafkaAvroEncoder(SchemaRegistryClient schemaRegistry, boolean enableAutoSchemaRegistration) {
    this.schemaRegistry = schemaRegistry;
    this.enableAutoSchemaRegistry = enableAutoSchemaRegistration;
  }

  /**
   * Constructor used by Kafka producer.
   */
  public KafkaAvroEncoder(VerifiableProperties props) {
    if (props == null) {
      throw new ConfigException("Missing schema registry url!");
    }
    String url = props.getProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG);
    if (url == null) {
      throw new ConfigException("Missing schema registry url!");
    }
    enableAutoSchemaRegistry = props.getBoolean(KafkaAvroSerializerConfig.ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG);
    int maxSchemaObject = props.getInt(
            AbstractKafkaAvroSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_CONFIG,
            AbstractKafkaAvroSerDeConfig.MAX_SCHEMAS_PER_SUBJECT_DEFAULT);
    schemaRegistry = new CachedSchemaRegistryClient(url, maxSchemaObject);
  }

  @Override
  public byte[] toBytes(Object object) {
    if (object instanceof IndexedRecord) {
      String subject = ((IndexedRecord) object).getSchema().getName() + "-value";
      return serializeImpl(subject, object);
    } else {
      throw new SerializationException("Primitive types are not supported yet");
    }
  }

}
