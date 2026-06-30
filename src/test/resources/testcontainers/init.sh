#!/bin/bash
echo "### CREATE QUEUES FIFO ###"

queues_fifo="local-pn-portfat-inputs-requests.fifo, local-pn-portfat-inputs-requests-mock.fifo"

for qn in  $( echo $queues_fifo | tr " " "\n" ) ; do

    echo creating queue fifo $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
        --queue-name $qn
done

echo "### CREATE QUEUES ###"
queues="local-pn-safestorage-to-portfat"
for qn in $(echo $queues | tr " " "\n"); do
  echo creating queue $qn ...
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
    sqs create-queue \
    --attributes '{"DelaySeconds":"2"}' \
    --queue-name $qn
done

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name PortFatDownload \
    --attribute-definitions \
        AttributeName=downloadId,AttributeType=S \
        AttributeName=archiveFileKey,AttributeType=S \
    --key-schema \
        AttributeName=downloadId,KeyType=HASH \
    --global-secondary-indexes '[
        {
            "IndexName": "archiveFileKey-index",
            "KeySchema": [
                {"AttributeName": "archiveFileKey", "KeyType": "HASH"}
            ],
            "Projection": {
                "ProjectionType": "ALL"
            },
            "ProvisionedThroughput": {
                "ReadCapacityUnits": 10,
                "WriteCapacityUnits": 5
            }
        }
    ]' \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5
echo "Initialization terminated"