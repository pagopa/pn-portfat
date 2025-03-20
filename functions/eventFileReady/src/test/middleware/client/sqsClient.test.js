const { expect } = require('chai');
const sinon = require('sinon');
const { SendMessageCommand } = require('@aws-sdk/client-sqs');
const proxyquire = require('proxyquire');

const mockSend = sinon.stub();

const sqsClientModule = proxyquire('../../../app/middleware/client/sqsClient', {
    '@aws-sdk/client-sqs': {
        SQSClient: sinon.stub().returns({ send: mockSend }),
        SendMessageCommand
    }
});


describe('sqsClient - sendMessageToQueue', () => {

    afterEach(() => {
        sinon.restore();
        mockSend.reset();
    });

    it('Should send message with correct params', async () => {
        mockSend.resolves();

        const message = { key: 'value' };
        const filePath = '/path/to/file';

        await sqsClientModule.sendMessageToQueue(message, filePath);

        expect(mockSend.calledOnce).to.be.true;

        const sendCommand = mockSend.firstCall.args[0];

        expect(sendCommand).to.be.instanceOf(SendMessageCommand);
        expect(sendCommand.input.QueueUrl).to.equal(process.env.PN_PORTFAT_SQS_QUEUE_URL);
        expect(sendCommand.input.MessageBody).to.equal(JSON.stringify(message));
        expect(sendCommand.input.MessageGroupId).to.equal(filePath);
    });
});