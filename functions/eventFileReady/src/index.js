const apiRouter = require('./app/router/apiRouter');


exports.handler = async (event) => {
    console.log('EVENT RECEIVED:', JSON.stringify(event));
    console.log('ENV VARIABLES:', JSON.stringify(process.env, null, 2));

    try {
      return await apiRouter.route(event);
    } catch (error) {
        console.log('UNHANDLED ERROR');
        return {
            statusCode: 500,
            body: JSON.stringify({ message: 'Internal Server Error', error: error.message })
        };
    }
};