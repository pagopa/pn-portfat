const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');


describe('messageService - processFileReadyEvent', () => {
    let sendMessageToQueueStub;
    let messageService;

    beforeEach(() => {
        sendMessageToQueueStub = sinon.stub().resolves();

        messageService = proxyquire('../../app/service/messageService', {
            '../middleware/client/sqsClient': { sendMessageToQueue: sendMessageToQueueStub }
        });
    });

    afterEach(() => {
        sinon.restore();
    });

    it('Should extract filePath, build message and send it to SQS', async () => {
        const inputBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
            fileVersion: '1.0.0'
        };

        const expectedFilePath = '/invoices/file.zip';
        const expectedMessagePayload = {
            downloadUrl: inputBody.downloadUrl,
            fileVersion: inputBody.fileVersion,
            filePath: expectedFilePath
        };

        const result = await messageService.processFileReadyEvent(inputBody);

        expect(sendMessageToQueueStub.calledOnceWithExactly(expectedMessagePayload, expectedFilePath)).to.be.true;

        expect(result).to.deep.equal({ success: true });
    });

    it('Should throw an error if sendMessageToQueue fails', async () => {
        const inputBody = {
            downloadUrl: 'https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2012-02-12&st=2009-02-09&se=2009-02-10&sr=c&sp=r&si=YWJjZGVmZw%3d%3d&sig=dD80ihBh5jfNpymO5Hg1IdiJIEvHcJpCMiCMnN%2fRnbI%3d',
            fileVersion: '1.0.0'
        };

        sendMessageToQueueStub.rejects(new Error('SQS Error'));

        try {
            await messageService.processFileReadyEvent(inputBody);
            throw new Error('Test should have failed');
        } catch (error) {
            expect(error.message).to.equal('SQS Error');
        }
    });
});
