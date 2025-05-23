const { expect } = require('chai');
const { validateBody } = require('../../../app/middleware/validator/schemaValidator');


describe('schemaValidator - validateBody', () => {

    it('Should pass for valid body', () => {
        const validBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
            fileVersion: '1.0.0'
        };

        expect(() => validateBody(validBody)).to.not.throw();
    });

    it('Should throw error for missing downloadUrl', () => {
        const invalidBody = {
            fileVersion: '1.0.0'
        };

        try {
            validateBody(invalidBody);
            throw new Error('Test should have thrown');
        } catch (err) {
            expect(err.statusCode).to.equal(400);
            expect(err.message).to.equal('Bad Request: missing data or not valid');
            expect(err.details).to.be.an('array');
            expect(err.details).to.deep.include({
                code: 'invalid_type',
                expected: 'string',
                received: 'undefined',
                path: ['downloadUrl'],
                message: 'Required'
            });
        }
    });

    it('Should throw error for invalid downloadUrl', () => {
        const invalidBody = {
            downloadUrl: 'invalid-url',
            fileVersion: '1.0.0'
        };

        try {
            validateBody(invalidBody);
            throw new Error('Test should have thrown');
        } catch (err) {
            expect(err.statusCode).to.equal(400);
            expect(err.message).to.equal('Bad Request: missing data or not valid');
            expect(err.details).to.be.an('array');
            expect(err.details).to.deep.include({
                code: 'invalid_string',
                validation: 'url',
                path: ['downloadUrl'],
                message: 'Invalid url'
            });
        }
    });

    it('Should throw error for missing fileVersion', () => {
        const invalidBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d'
        };

        try {
            validateBody(invalidBody);
            throw new Error('Test should have thrown');
        } catch (err) {
            expect(err.statusCode).to.equal(400);
            expect(err.message).to.equal('Bad Request: missing data or not valid');
            expect(err.details).to.be.an('array');
            expect(err.details).to.deep.include({
                code: 'invalid_type',
                expected: 'string',
                received: 'undefined',
                path: ['fileVersion'],
                message: 'Required'
            });
        }
    });

    it('Should throw error for empty fileVersion', () => {
        const invalidBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
            fileVersion: ''
        };

        try {
            validateBody(invalidBody);
            throw new Error('Test should have thrown');
        } catch (err) {
            expect(err.statusCode).to.equal(400);
            expect(err.message).to.equal('Bad Request: missing data or not valid');
            expect(err.details).to.be.an('array');
            expect(err.details).to.deep.include({
                code: 'too_small',
                minimum: 1,
                type: 'string',
                inclusive: true,
                exact: false,
                path: ['fileVersion'],
                message: 'String must contain at least 1 character(s)'
            });
        }
    });
});