const { expect } = require('chai');
const sinon = require('sinon');
const { SendMessageCommand } = require('@aws-sdk/client-sqs');
const proxyquire = require('proxyquire');
const { queueUrl, mockQueueUrl } = require('../../../app/config/config');

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

    [
        {
            name: 'mock assente',
            message: { key: 'value' },
            expectedQueueUrl: queueUrl
        },
        {
            name: 'mock false boolean',
            message: { key: 'value', mock: false },
            expectedQueueUrl: queueUrl
        },
        {
            name: 'mock false string',
            message: { key: 'value', mock: 'false' },
            expectedQueueUrl: queueUrl
        },
        {
            name: 'mock true boolean',
            message: { key: 'value', mock: true },
            expectedQueueUrl: mockQueueUrl
        },
        {
            name: 'mock true string',
            message: { key: 'value', mock: 'true' },
            expectedQueueUrl: mockQueueUrl
        }
    ].forEach(({ name, message, expectedQueueUrl }) => {
        it(`Should send message to correct queue when ${name}`, async () => {
            mockSend.resolves();

            const filePath = '/path/to/file';

            await sqsClientModule.sendMessageToQueue(message, filePath);

            expect(mockSend.calledOnce).to.be.true;

            const sendCommand = mockSend.firstCall.args[0];

            expect(sendCommand).to.be.instanceOf(SendMessageCommand);
            expect(sendCommand.input.QueueUrl).to.equal(expectedQueueUrl);
            expect(sendCommand.input.MessageBody).to.equal(JSON.stringify(message));
            expect(sendCommand.input.MessageGroupId).to.equal(filePath);
        });
    });
});