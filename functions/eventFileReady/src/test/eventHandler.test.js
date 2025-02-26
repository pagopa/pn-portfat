const { versioning } = require("../app/eventHandler.js");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);

// Simula un evento API Gateway
const event = {
    resource: "/pn-portfat-in/file-ready-event",
    path: "/pn-portfat-in/file-ready-event",
    httpMethod: "POST",
    body: JSON.stringify({
        downloadUrl: "https://example.com/file",
        fileVersionString: "2024-02-26T12:00:00Z"
    })
};

// Simula un context vuoto (non serve per ora)
const context = {};

// Esegue la Lambda e stampa il risultato
versioning(event, context).then(response => {
    console.log("Risultato:", response);
}).catch(error => {
    console.error("Errore:", error);
});