const { processFileReadyEvent } = require('../service/messageService');
const { validateBody } = require('../middleware/validator/schemaValidator');
const AppError = require('../utils/appError');


exports.route = async (event) => {
    console.log('ROUTING EVENT:', JSON.stringify(event));
    const { httpMethod, resource, body } = event;

    if (httpMethod === 'POST' && resource === 'file-ready-event') {
        let parsedBody;
        try {
            parsedBody = JSON.parse(body ?? '{}');
        } catch (err) {
            console.log('Invalid JSON received:', body, err.message);
            throw new AppError(400, `Invalid JSON: ${err.message}`);
        }

        validateBody(parsedBody);

        const result = await processFileReadyEvent(parsedBody);
        if (result.success) {
            return {
                statusCode: 202,
                body: JSON.stringify({ message: 'Request accepted' })
            };
        } else {
            throw new AppError(500, 'Internal error while processing request');
        }
    }

    if (httpMethod === 'GET' && resource === '/status') {
        return {
            statusCode: 200,
            body: JSON.stringify({ status: 'Ok' })
        };
    }

    throw new AppError(404, 'Route not found');
};