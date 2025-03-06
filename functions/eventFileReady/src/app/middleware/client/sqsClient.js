const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');
const { fromEnv } = require('@aws-sdk/credential-providers');
const config = require('../../config/config');

const isLocalStack = process.env.LOCALSTACK === 'true';


const sqsClient = new SQSClient({
    region: config.region,
    ...(config.endpoint && {
        endpoint: config.endpoint,
        useQueueUrlAsEndpoint: true
    }),
    credentials: isLocalStack ? fromEnv() : config.credentials
});

exports.sendMessageToQueue = async (message, filePath) => {
    console.log(`SQS Client initialized with ${isLocalStack ? 'fromEnv()' : 'explicit credentials'}`);

    const params = {
        QueueUrl: config.queueUrl,
        MessageBody: JSON.stringify(message),
        MessageGroupId: filePath
    };

    console.log('Sending message to queue:', params);
    await sqsClient.send(new SendMessageCommand(params));
};