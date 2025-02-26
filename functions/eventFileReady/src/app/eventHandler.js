const { SQSClient, SendMessageCommand } = require("@aws-sdk/client-sqs");
const axios = require("axios");

const sqsClient = new SQSClient({
  region: "us-east-1", // Cambia con la tua regione
  endpoint: "http://localhost:4566"  // Usa l'endpoint di LocalStack o AWS
});

exports.versioning = async (event, context) => {
  const path = "/pn-portfat-in/file-ready-event";
  
  if (event["resource"].indexOf(`${path}`) < 0 
      || !event["path"].startsWith("/pn-portfat-in/") 
      || event["httpMethod"].toUpperCase() !== "POST" ) {
    console.log(
        "ERROR ENDPOINT ERRATO: {resource, path, httpMethod} ",
        event["resource"],
        event["path"],
        event["httpMethod"]
    );

    return {
      statusCode: 400,
      body: JSON.stringify({
        status: 400,
        errors: [
          {
            code: "Endpoint errato o metodo non consentito"
          }
        ]
      })
    };
  }

  try {
    // Recupera il body e convertilo in JSON
    const requestBody = JSON.parse(event.body);
    console.log("Body ricevuto:", requestBody);

    // Controllo se i campi obbligatori sono presenti
    if (!requestBody.downloadUrl || !requestBody.fileVersionString) {
      return {
        statusCode: 400,
        body: JSON.stringify({
          status: 400,
          errors: [
            {
              code: "downloadUrl e fileVersionString sono obbligatori"
            }
          ]
        })
      };
    }

    // Invia il messaggio alla coda SQS
    const params = {
      QueueUrl: "http://localstack:4566/000000000000/local-delivery-push-inputs",
      MessageBody: JSON.stringify(requestBody),
      MessageGroupId: "my-group",
      MessageDeduplicationId: `unique-id-${Date.now()}`,
    };

    // Invia il messaggio
    const sentMs =  new SendMessageCommand(params);
    console.log("SendMessageCommand >> :", sentMs);
    const data = await sqsClient.send(sentMs);
    console.log("await sqsClient >>>>>:", data);
    console.log("Messaggio inviato alla coda SQS:", data.MessageId);

    // Se tutto va bene, restituisci HTTP 202 (Accepted)
    return {
      statusCode: 202,
      body: JSON.stringify({
        message: "Messaggio inviato correttamente alla coda SQS",
        messageId: data.MessageId
      })
    };
  } catch (error) {
    console.error("Errore nel parsing del body o nell'invio del messaggio:", error);
    return {
      statusCode: 500,
      body: JSON.stringify({
        status: 500,
        errors: [
          {
            code: "Errore nel parsing del body o nell'invio del messaggio"
          }
        ]
      })
    };
  }
};
