const apiRouter = require('./src/app/router/apiRouter');
const AppError = require('./src/app/utils/appError');

exports.handler = async (event) => {
    console.log('EVENT RECEIVED:', JSON.stringify(event));
    try {
        return await apiRouter.route(event);
    } catch (error) {
        if (error instanceof AppError) {
            return {
                statusCode: error.statusCode,
                body: JSON.stringify({ message: error.message, error: error.details || error.message })
            };
        }
        return {
            statusCode: 500,
            body: JSON.stringify({ message: 'Internal Server Error', error: error.message })
        };
    }
};