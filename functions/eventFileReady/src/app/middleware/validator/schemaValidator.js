const { z } = require('zod');


const schema = z.object({
    downloadUrl: z.string().url(),
    fileVersionString: z.string().nonempty()
});

exports.validateBody = (body) => {
    const result = schema.safeParse(body);
    if (!result.success) {
        console.error('Invalid request body:', result.error);
        throw {
            statusCode: 400,
            message: 'Invalid request body',
            details: result.error.issues
        };
    }
};