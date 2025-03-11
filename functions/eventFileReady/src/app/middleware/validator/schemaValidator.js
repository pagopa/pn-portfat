const { z } = require('zod');
const AppError = require('../../utils/appError');


const schema = z.object({
    downloadUrl: z.string().url(),
    fileVersion: z.string().nonempty()
});

exports.validateBody = (body) => {
    console.log('VALIDATING BODY:', JSON.stringify(body));
    const result = schema.safeParse(body);
    if (!result.success) {
        console.log('Bad Request: missing data or not valid:', result.error);
        throw new AppError(400, 'Bad Request: missing data or not valid', result.error.issues);
    }
};
