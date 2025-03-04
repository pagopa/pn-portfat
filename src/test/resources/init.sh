echo "### CREATE QUEUES ###"

queues_fifo="local-pn-portfact-inputs-requests.fifo"

for qn in  $( echo $queues_fifo | tr " " "\n" ) ; do

    echo creating queue fifo $qn ...

    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2","FifoQueue": "true","ContentBasedDeduplication": "true"}' \
        --queue-name $qn
done

echo " - Create pn-delivery-push TABLES"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name PortFatDownload \
    --attribute-definitions \
        AttributeName=downloadId,AttributeType=S \
        AttributeName=downloadUrl,AttributeType=S \
        AttributeName=fileVersion,AttributeType=S \
        AttributeName=sha256,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=createdAt,AttributeType=S \
       AttributeName=updatedAt,AttributeType=S \
    --key-schema \
        AttributeName=downloadId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

echo "Initialization terminated"