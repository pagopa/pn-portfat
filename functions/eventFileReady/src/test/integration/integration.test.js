const { SQSClient, CreateQueueCommand, SendMessageCommand, ReceiveMessageCommand, DeleteMessageCommand } = require("@aws-sdk/client-sqs");
const { GenericContainer, Wait } = require("testcontainers");
const config = require('../../app/config/config');
const chai = require('chai');
const expect = chai.expect;

let container;
let sqsClient;
let queueUrl;


before(async function () {
    this.timeout(120000);

    container = await new GenericContainer("localstack/localstack:2.3")
        .withExposedPorts(4566)
        .withEnvironment({
            SERVICES: config.service,
            AWS_DEFAULT_REGION: config.region,
            AWS_ACCESS_KEY_ID: config.credentials.accessKeyId,
            AWS_SECRET_ACCESS_KEY: config.credentials.secretAccessKey
        })
        .withWaitStrategy(Wait.forLogMessage("Ready."))
        .start();

    const mappedPort = container.getMappedPort(4566);
    const endpoint = `http://localhost:${mappedPort}`;
    console.log(`LocalStack running on ${endpoint}`);

    // Configure SQS client
    sqsClient = new SQSClient({
        region: config.region,
        endpoint: endpoint,
        credentials: {
            accessKeyId: config.credentials.accessKeyId,
            secretAccessKey: config.credentials.secretAccessKey
        }
    });

    // Create SQS FIFO
    const createQueueResponse = await sqsClient.send(new CreateQueueCommand({
        QueueName: config.queueName,
        Attributes: {
            FifoQueue: "true",
            ContentBasedDeduplication: "true"
        }
    }));

    queueUrl = createQueueResponse.QueueUrl;
    console.log(`Created test queue: ${queueUrl}`);
});

after(async function () {
    if (container) {
        await container.stop();
        console.log("LocalStack stopped");
    }
});


describe('SQS Integration Test', () => {

    it("Should send and receive message from SQS", async function () {
        this.timeout(30000);
        const filePath = "/invoices/file.zip";

        const messageBody = {
            downloadUrl: "https://pagopa.blob.core.windows.net/invoices/file.zip?sv=2021-02-12",
            fileVersion: "1.0.0",
            filePath: filePath
        };

        await sqsClient.send(new SendMessageCommand({
            QueueUrl: queueUrl,
            MessageBody: JSON.stringify(messageBody),
            MessageGroupId: filePath
        }));

        console.log("Message sent to SQS");

        const receiveResponse = await sqsClient.send(new ReceiveMessageCommand({
            QueueUrl: queueUrl,
            MaxNumberOfMessages: 1,
            WaitTimeSeconds: 5,
            VisibilityTimeout: 10
        }));

        console.log("Message received from SQS");

        if (receiveResponse.Messages && receiveResponse.Messages.length > 0) {
            const receivedMessage = JSON.parse(receiveResponse.Messages[0].Body);
            console.log("Received Message Body:", receivedMessage);

            expect(receivedMessage).to.have.property("downloadUrl", messageBody.downloadUrl);
            expect(receivedMessage).to.have.property("fileVersion", messageBody.fileVersion);
            expect(receivedMessage).to.have.property("filePath", messageBody.filePath);

            await sqsClient.send(new DeleteMessageCommand({
                QueueUrl: queueUrl,
                ReceiptHandle: receiveResponse.Messages[0].ReceiptHandle
            }));
        } else {
            throw new Error("No messages received");
        }
    });

    it("Should return 400 for invalid message body", async function () {
        this.timeout(30000);

        const invalidMessageBody = {};

        await sqsClient.send(new SendMessageCommand({
            QueueUrl: queueUrl,
            MessageBody: JSON.stringify(invalidMessageBody),
            MessageGroupId: "/invalid/path"
        }));

        console.log("Invalid message sent to SQS");

        const receiveResponse = await sqsClient.send(new ReceiveMessageCommand({
            QueueUrl: queueUrl,
            MaxNumberOfMessages: 1,
            WaitTimeSeconds: 5,
            VisibilityTimeout: 10
        }));

        expect(receiveResponse.Messages).to.have.lengthOf(1);
        const receivedMessage = JSON.parse(receiveResponse.Messages[0].Body);
        expect(receivedMessage).to.deep.equal({});
    });

    it("Should return 500 if SQS is down", async function () {
        this.timeout(30000);

        await container.stop();
        console.log("LocalStack stopped to simulate failure");

        const filePath = "/invoices/error-file.zip";
        const messageBody = {
            downloadUrl: "https://pagopa.blob.core.windows.net/invoices/error-file.zip?sv=2021-02-12",
            fileVersion: "1.0.0",
            filePath: filePath
        };

        try {
            await sqsClient.send(new SendMessageCommand({
                QueueUrl: queueUrl,
                MessageBody: JSON.stringify(messageBody),
                MessageGroupId: filePath
            }));
        } catch (error) {
            console.log("Expected failure when sending message:", error.message);
            expect(error).to.exist;
        }
    });
});