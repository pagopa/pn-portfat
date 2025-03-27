const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');
const AppError = require('../../app/utils/appError');

const processFileReadyEventStub = sinon.stub().resolves({ success: true });
const validateBodyStub = sinon.stub().returns(true);
const sendMessageToQueueStub = sinon.stub().resolves();

const routerModule = proxyquire('../../app/router/apiRouter', {
    '../../app/service/messageService': { processFileReadyEvent: processFileReadyEventStub },
    '../../app/middleware/validator/schemaValidator': { validateBody: validateBodyStub },
    '../../app/middleware/client/sqsClient': { sendMessageToQueue: sendMessageToQueueStub }
});

const { route } = routerModule;

describe('apiRouter route handler', () => {

    afterEach(() => {
        sinon.restore();
    });

    it('Should throw 400 for invalid JSON', async () => {
        const event = {
            httpMethod: 'POST',
            resource: '/file-ready-event',
            body: '{invalid-json}'
        };

        try {
            await route(event);
            throw new Error('Expected route() to throw an error');
        } catch (error) {
            expect(error).to.be.instanceOf(AppError);
            expect(error.statusCode).to.equal(400);
            expect(error.message).to.contain('Invalid JSON:');
        }
    });

    it('Should call validateBody and processFileReadyEvent for valid event', async () => {
        const mockBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
            fileVersion: '1.0.0'
        };
        const event = {
            httpMethod: 'POST',
            resource: '/file-ready-event',
            body: JSON.stringify(mockBody)
        };

        const response = await route(event);

        expect(validateBodyStub.calledOnceWith(mockBody)).to.be.true;
        expect(processFileReadyEventStub.calledOnceWith(mockBody)).to.be.true;
        expect(response.statusCode).to.equal(202);
        expect(JSON.parse(response.body)).to.deep.equal({ message: 'Request accepted' });
    });

    it('Should return 200 for GET /status', async () => {
        const event = {
            httpMethod: 'GET',
            resource: '/status'
        };

        const response = await route(event);

        expect(response.statusCode).to.equal(200);
        expect(JSON.parse(response.body)).to.deep.equal({ status: 'Ok' });
    });

    it('Should throw 404 for unknown routes', async () => {
        const event = {
            httpMethod: 'GET',
            resource: '/wrong-route'
        };

        try {
            await route(event);
            throw new Error('Expected route() to throw an error');
        } catch (error) {
            expect(error).to.be.instanceOf(AppError);
            expect(error.statusCode).to.equal(404);
            expect(error.message).to.equal('Route not found');
        }
    });
});