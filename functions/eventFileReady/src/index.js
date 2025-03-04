const apiRouter = require('./app/router/apiRouter');


exports.handler = async (event) => {
    console.log('Event received:', JSON.stringify(event));

    try {
      return await apiRouter.route(event);
    } catch (error) {
      console.error('Unhandled error:', error);
      const statusCode = error.statusCode || 500;
      const message = error.message || 'Internal Server Error';
      return {
        statusCode,
        body: JSON.stringify({ message })
      };
    }
};