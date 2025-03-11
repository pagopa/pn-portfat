const { sendMessageToQueue } = require('../middleware/client/sqsClient');


exports.processFileReadyEvent = async (body) => {
    const filePath = new URL(body.downloadUrl).pathname;
    const messagePayload = {
        downloadUrl: body.downloadUrl,
        fileVersion: body.fileVersion,
        filePath: filePath
    };

    console.log('SENDING MESSAGE:', JSON.stringify(messagePayload));

    await sendMessageToQueue(messagePayload, filePath);
    return { success: true };
};
