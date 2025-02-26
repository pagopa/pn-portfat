const axios = require("axios");

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

        // Se tutto va bene, restituisci HTTP 202 (Accepted)
        return {
            statusCode: 202,
            body: JSON.stringify({})
        };
    } catch (error) {
        console.error("Errore nel parsing del body:", error);
        return {
            statusCode: 500,
            body: JSON.stringify({
                status: 500,
                errors: [
                    {
                        code: "Errore nel parsing del body"
                    }
                ]
            })
        };
    }
};


  