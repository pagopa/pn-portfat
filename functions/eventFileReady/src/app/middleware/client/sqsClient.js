const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');
const config = require('../../config/config');


const sqsClient = new SQSClient({
    region: config.region,
    ...(config.endpoint && {
        endpoint: config.endpoint,
        useQueueUrlAsEndpoint: true
    }),
    ...(config.credentials && { credentials: config.credentials })
});

exports.sendMessageToQueue = async (message, filePath) => {
    const params = {
        QueueUrl: config.queueUrl,
        MessageBody: JSON.stringify(message),
        MessageGroupId: filePath
    };

    await sqsClient.send(new SendMessageCommand(params));
};