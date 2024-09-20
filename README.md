# hl7-to-kafka
[![MegaLinter](https://github.com/diz-unimr/hl7-to-kafka/actions/workflows/mega-linter.yml/badge.svg?branch=main)](https://github.com/diz-unimr/hl7-to-kafka/actions/workflows/mega-linter.yml?query=branch%3Amain) ![java](https://github.com/diz-unimr/hl7-to-kafka/actions/workflows/build.yml/badge.svg) ![docker](https://github.com/diz-unimr/hl7-to-kafka/actions/workflows/release.yml/badge.svg) [![codecov](https://codecov.io/gh/diz-unimr/hl7-to-kafka/graph/badge.svg?token=6vQBSuFCH3)](https://codecov.io/gh/diz-unimr/hl7-to-kafka)

> HL7 v2 to Kafka producer

This project consist of a HL7v2 MLLP listener, which transfers received
messages to a Kafka topic.

Messages are passed to a Kafka producer and send to a configured topic
without any mapping. The sender receives an acknowledgement message (ACK) of
each message sent.

## Kafka headers

The resulting Kafka record is produced with a timestamp form the HL7v2 
message header, i.e. from the `MSH-7` segment.

The key is set to the Message control id (`MSH-10`) by default. However,
this can be set to an arbitrary message segment value by providing a
[Terser](https://hapifhir.github.io/hapi-hl7v2/base/apidocs/ca/uhn/hl7v2/util/Terser.html)
expression with the `endpoint.hl7.key-expression` application property (e.g.: 
_ORC-2_)

## <a name="deploy_config"></a> Configuration

The following environment variables can be set:

| Variable                       | Default        | Description                                                    |
|--------------------------------|----------------|----------------------------------------------------------------|
| KAFKA_BOOTSTRAP_SERVERS        | localhost:9092 | Kafka brokers                                                  |
| KAFKA_SECURITY_PROTOCOL        | PLAINTEXT      | Kafka communication protocol                                   |
| KAFKA_SSL_TRUST_STORE_PASSWORD |                | Truststore password (if using `SECURITY_PROTOCOL=SSL`)         |
| KAFKA_SSL_KEY_STORE_PASSWORD   |                | Keystore password (if using `SECURITY_PROTOCOL=SSL`)           |
| ENDPOINT_KAFKA_TOPIC           | hl7-topic      | Kafka output topic to send HL7 messages to                     |
| ENDPOINT_HL7_PORT              | 2575           | HL7 MLLP listener port                                         |
| ENDPOINT_HL7_ENCODING          | iso-8859-1     | Default encoding to use if not specified in the message header |
| ENDPOINT_KAFKA_KEYEXPRESSION   |                | Key selector for the Kafka record                              |
| LOG_LEVEL                      | info           | Log level (error, warn, info, debug)                           |

Additional application properties can be set by overriding values form the [application.yaml](src/main/resources/application.yaml) by using environment variables.

## Error handling

In general, the service expects incoming messages to conform to the HL7 2.x
standard and the MLLP protocol.

In case of errors due to missing mandatory message header fields (id and
timestamp), formatting errors or unavailable Kafka broker connections (after
a timeout), an ACK message with the Application error type (`AE`) is returned.

## Development

A [test setup](dev/compose.yaml) and a [script](dev/send-hl7.sh) to send an example
message is available for development purposes.

### Builds

Available image tags can be found at the [Container Registry](https://github.com/orgs/diz-unimr/packages?repo_name=hl7-to-kafka) or under
[Releases](https://github.com/diz-unimr/hl7-to-kafka/releases).

## License

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
