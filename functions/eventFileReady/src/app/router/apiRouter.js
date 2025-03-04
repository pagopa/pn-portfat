const { processFileReadyEvent } = require('../service/messageService');
const { validateBody } = require('../middleware/validator/schemaValidator');


exports.route = async (event) => {
    const { httpMethod, resource, body } = event;

    if (httpMethod === 'POST' && resource === '/pn-portfat-in/file-ready-event') {
        let parsedBody;
        try {
            parsedBody = JSON.parse(body ?? '{}');
        } catch (err) {
            console.log('Invalid JSON received:', body, err.message);
            return {
                statusCode: 400,
                body: JSON.stringify({ message: `Invalid JSON: ${err.message}` })
            };
        }
        validateBody(parsedBody);

        const result = await processFileReadyEvent(parsedBody);

        if (result.success) {
            return {
                statusCode: 202,
                body: JSON.stringify({ message: 'Request accepted' })
            };
        } else {
            return {
                statusCode: 500,
                body: JSON.stringify({ message: 'Internal error' })
            };
        }
    }

    if (httpMethod === 'GET' && resource === '/status') {
        return {
            statusCode: 200,
            body: JSON.stringify({ status: 'Ok' })
        };
    }

    return {
        statusCode: 404,
        body: JSON.stringify({ message: 'Route not found' })
    };
};