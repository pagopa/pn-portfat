if (!process.env.AWS_LAMBDA_FUNCTION_NAME && !process.env.LOCALSTACK) {
    require('dotenv').config();
    console.log('Dotenv loaded (local execution)');
} else {
    console.log('Dotenv NOT loaded (LocalStack or AWS Lambda execution)');
}

const isLocal = process.env.LOCALSTACK === 'true';

module.exports = {
    region: process.env.AWS_REGION,
    queueUrl: process.env.SQS_QUEUE_URL,
    queueName: process.env.SQS_QUEUE_NAME,
    endpoint: isLocal ? process.env.SQS_ENDPOINT : undefined,
    service: process.env.AWS_SERVICE,
    credentials: isLocal ? {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID,
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY
    } : undefined
};