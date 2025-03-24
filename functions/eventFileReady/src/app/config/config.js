if (process.env.LOCALSTACK !== 'true') {
    require('dotenv').config();
    console.log('Dotenv loaded');
} else {
    console.log('Dotenv NOT loaded (AWS Lambda execution)');
}

const isLocal = process.env.LOCALSTACK === 'true';

module.exports = {
    region: process.env.PN_PORTFAT_AWS_REGION,
    queueUrl: process.env.PN_PORTFAT_SQS_QUEUE_URL,
    queueName: process.env.PN_PORTFAT_SQS_QUEUE_NAME,
    endpoint: isLocal ? process.env.SQS_ENDPOINT : undefined,
    service: process.env.PN_PORTFAT_AWS_SERVICE,
    credentials: isLocal ? {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID,
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY
    } : undefined
};