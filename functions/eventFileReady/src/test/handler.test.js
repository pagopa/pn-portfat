const { expect } = require('chai');
const lambdaTester = require('lambda-tester');
const { handler } = require('../../index');
const sinon = require('sinon');
const { SQSClient } = require('@aws-sdk/client-sqs');


describe('Lambda EventFileReady Handler', () => {

    afterEach(() => {
        sinon.restore();
    });

    it('Should return status 202 for valid event', async () => {
        sinon.stub(SQSClient.prototype, 'send').resolves({ MessageId: '12345' });

        const mockEvent = {
            httpMethod: 'POST',
            resource: '/file-ready-event',
            body: JSON.stringify({
                downloadUrl: 'https://myaccount.blob.core.windows.net/fatture/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
                fileVersion: '1.0.0'
            })
        };

        await lambdaTester(handler)
            .event(mockEvent)
            .expectResolve((response) => {
                expect(response.statusCode).to.equal(202);
                const body = JSON.parse(response.body);
                expect(body.message).to.equal('Request accepted');
            });
    });

    it('Should return 400 for invalid body', async () => {
        const mockEvent = {
            httpMethod: 'POST',
            resource: '/file-ready-event',
            body: JSON.stringify({})
        };

        await lambdaTester(handler)
            .event(mockEvent)
            .expectResult((response) => {
                expect(response.statusCode).to.equal(400);
                const body = JSON.parse(response.body);
                expect(body.message).to.equal('Bad Request: missing data or not valid');
            });
    });

    it('Should return 500 for internal server error', async () => {
        sinon.stub(SQSClient.prototype, 'send').rejects(new Error('Error sending message to queue'));

        const mockEvent = {
            httpMethod: 'POST',
            resource: '/file-ready-event',
            body: JSON.stringify({
                downloadUrl: 'https://myaccount.blob.core.windows.net/fatture/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
                fileVersion: '1.0.0'
            })
        };

        await lambdaTester(handler)
            .event(mockEvent)
            .expectResolve((response) => {
                expect(response.statusCode).to.equal(500);
                const body = JSON.parse(response.body);
                expect(body.message).to.equal('Error sending message to queue');
            });
    });
});
