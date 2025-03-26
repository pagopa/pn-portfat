const { SQSClient, SendMessageCommand } = require('@aws-sdk/client-sqs');
const { fromEnv } = require('@aws-sdk/credential-providers');
const config = require('../../config/config');
const AppError = require('../../utils/appError');

const isLocalStack = config.isLocal;


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

    try {
        return await sqsClient.send(new SendMessageCommand(params));
    } catch (error) {
        console.error('Error sending message to queue:', error);
        throw new AppError(500, 'Error sending message to queue', error.message);
    }
};