const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('Sender Order Check Lambda', () => {
  let handler;
  let docClientStub;
  let sendStub;

  beforeEach(() => {
    sendStub = sinon.stub();
    docClientStub = {
      send: sendStub
    };

    const dynamodbStub = {
      DynamoDBDocumentClient: {
        from: sinon.stub().returns(docClientStub)
      }
    };

    handler = proxyquire('../../index.js', {
      '@aws-sdk/lib-dynamodb': dynamodbStub
    }).handler;

    process.env.PORTFAT_DOWNLOAD_TABLE_NAME = 'pn-PortFatDownload';
    process.env.AWS_REGION = 'eu-south-1';
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should return 200 when both X+1 and X+4 records exist', async () => {
    sendStub.onFirstCall().resolves({ Items: [{ downloadId: '1' }] });
    sendStub.onSecondCall().resolves({ Items: [{ downloadId: '2' }] });

    const result = await handler();

    expect(result.statusCode).to.equal(200);
    expect(JSON.parse(result.body).success).to.be.true;
  });

  it('should return 400 when X+1 record is missing', async () => {
    sendStub.onFirstCall().resolves({ Items: [] });
    sendStub.onSecondCall().resolves({ Items: [{ downloadId: '2' }] });

    const result = await handler();

    expect(result.statusCode).to.equal(400);
    expect(JSON.parse(result.body).success).to.be.false;
  });

  it('should return 400 when X+4 record is missing', async () => {
    sendStub.onFirstCall().resolves({ Items: [{ downloadId: '1' }] });
    sendStub.onSecondCall().resolves({ Items: [] });

    const result = await handler();

    expect(result.statusCode).to.equal(400);
    expect(JSON.parse(result.body).success).to.be.false;
  });

  it('should return 400 when both records are missing', async () => {
    sendStub.onFirstCall().resolves({ Items: [] });
    sendStub.onSecondCall().resolves({ Items: [] });

    const result = await handler();

    expect(result.statusCode).to.equal(400);
    expect(JSON.parse(result.body).success).to.be.false;
  });

  it('should return 500 on DynamoDB error', async () => {
    sendStub.rejects(new Error('DynamoDB error'));

    const result = await handler();

    expect(result.statusCode).to.equal(500);
    expect(JSON.parse(result.body).success).to.be.false;
  });

  it('should log result message when check passes', async () => {
    const consoleLogSpy = sinon.spy(console, 'log');
    sendStub.onFirstCall().resolves({ Items: [{ downloadId: '1' }] });
    sendStub.onSecondCall().resolves({ Items: [{ downloadId: '2' }] });

    await handler();

    expect(consoleLogSpy.calledWith(sinon.match(/Check PASSED/))).to.be.true;
  });

  it('should log error when check fails', async () => {
    const consoleErrorSpy = sinon.spy(console, 'error');
    sendStub.onFirstCall().resolves({ Items: [] });
    sendStub.onSecondCall().resolves({ Items: [] });

    await handler();

    expect(consoleErrorSpy.calledWith(sinon.match(/Check FAILED/))).to.be.true;
  });
});

