const appConfig = require('config');

const isLocal = appConfig.has('LOCALSTACK') && appConfig.get('LOCALSTACK') === 'true';

module.exports = {
    region: appConfig.get('PN_PORTFAT_AWS_REGION'),
    queueUrl: appConfig.get('PN_PORTFAT_SQS_QUEUE_URL'),
    queueName: appConfig.get('PN_PORTFAT_SQS_QUEUE_NAME'),
    endpoint: isLocal ? appConfig.get('SQS_ENDPOINT') : undefined,
    credentials: isLocal ? {
        accessKeyId: appConfig.get('AWS_ACCESS_KEY_ID'),
        secretAccessKey: appConfig.get('AWS_SECRET_ACCESS_KEY')
    } : undefined,
    isLocal
};
