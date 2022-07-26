# Sqs Connector

Camunda SQS Connector

## Build

```bash
mvn clean package
```

## API

### Input

```json
{
  "queueUrl": "https://sqs.your.url",
  "queueRegion": "ap-north-5",
  "accessKey": "my access key",
  "secretKey": "my secrets key",
  "messageBody": "{\"myKey\":\"myVal\"}"
}
```

### Output

```json
{
  "result": {
    "messageId": "c158a652-c3e3-5511-a565-fd01a05c0c45"
  }
}
```

## Test locally

Run unit tests

```bash
mvn clean verify
```

### Test as local Google Cloud Function

Build as Google Cloud Function

```bash
mvn function:run -Pcloud-function
```

See also the [Camunda Cloud Connector Run-Time](https://github.com/camunda/connector-runtime-cloud) on how your function
is run as a Google Cloud Function.

### Test as local Job Worker

Use
the [Camunda Job Worker Connector Run-Time](https://github.com/camunda/connector-framework/tree/main/runtime-job-worker)
to run your function as a local Job Worker.

## Element Template

The element templates can be found in
the [element-templates/template-connector.json](element-templates/template-connector.json) file.

## Build a release

Checkout the repo and branch to build the release from. Run the release script with the version to release and the next
development version. The release script requires git and maven to be setup correctly, and that the user has push rights
to the repository.

The release artifacts are deployed to Google Cloud Function by a GitHub workflow.

```bash
./release.sh 0.3.0 0.4.0
```
