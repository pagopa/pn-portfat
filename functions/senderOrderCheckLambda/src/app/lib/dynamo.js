const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const {DynamoDBDocumentClient, QueryCommand} = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient();
const docClient = DynamoDBDocumentClient.from(client);

const TABLE_NAME =
    process.env.PORTFAT_DOWNLOAD_TABLE_NAME || 'pn-PortFatDownload';

const STATUS_MONTH_SENT_INDEX = 'statusMonthSent-index';

/**
 * Retrieves COMPLETED records for the specified monthSent.
 *
 * @param {string} monthSent - Month in yyyy-MM format
 * @returns {Promise<Array>} Matching records
 */
async function getCompletedRecordsByMonthSent(monthSent) {
    const command = new QueryCommand({
        TableName: TABLE_NAME,
        IndexName: STATUS_MONTH_SENT_INDEX,

        KeyConditionExpression: '#status = :status AND #monthSent = :monthSent',

        ExpressionAttributeNames: {
            '#status': 'status',
            '#monthSent': 'monthSent'
        },

        ExpressionAttributeValues: {
            ':status': 'COMPLETED',
            ':monthSent': monthSent
        }
    });

    const response = await docClient.send(command);
    return response.Items ?? [];
}

module.exports = {
    getCompletedRecordsByMonthSent
};