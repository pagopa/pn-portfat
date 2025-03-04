const { sendMessageToQueue } = require('../middleware/client/sqsClient');


exports.processFileReadyEvent = async (body) => {

    const filePath = new URL(body.downloadUrl).pathname;

    const messagePayload = {
        downloadUrl: body.downloadUrl,
        fileVersionString: body.fileVersionString,
        filePath: filePath
    };

    await sendMessageToQueue(messagePayload, filePath);
    return { success: true };
};