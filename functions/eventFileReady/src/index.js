const apiRouter = require('./app/router/apiRouter');

exports.handler = async (event) => {
  console.log('Event received:', JSON.stringify(event));

  try {
    const response = await apiRouter.route(event);
    return response;
  } catch (error) {
    console.error('Unhandled error:', error);
    // Se l'errore ha una proprietà statusCode, la usiamo; altrimenti, 500.
    const statusCode = error.statusCode || 500;
    const message = error.message || 'Internal Server Error';
    return {
      statusCode,
      body: JSON.stringify({ message })
    };
  }
};
