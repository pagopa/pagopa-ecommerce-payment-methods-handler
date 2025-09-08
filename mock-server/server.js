const express = require('express');
const cors = require('cors');
const app = express();
const port = 9090;

app.use(cors());
app.use(express.json());


function createPaymentMethod(id) {
    return {
        paymentMethodID: id || "pm-001",
        paymentMethodName: "Carta di credito",
        paymentMethodDescription: "Pagamento con carta di credito",
        paymentMethodStatus: "ACTIVE",
        paymentMethodAsset: "asset.png",
        paymentMethodRanges: [
            { start: 0, end: 1000 },
            { start: 1001, end: 5000 }
        ],
        paymentMethodTypeCode: "CARD",
        clientId: "client-123",
        methodManagement: "AUTOMATIC",
        paymentMethodsBrandAssets: {
            VISA: "visa.png",
            MC: "mc.png"
        }
    };
}

app.get('/payment-method', (req, res) => {
    const data = [
        createPaymentMethod("pm-001"),
        createPaymentMethod("pm-002")
    ];
    res.json(data);
});


app.get('/payment-method/:id', (req, res) => {
    const { id } = req.params;
    const data = [createPaymentMethod(id)];
    res.json(data);
});

app.post('/payment-methods/search', (req, res) => {
    const request = req.body;
    const xRequestId = req.headers['x-request-id'];

    console.log(`Richiesta ricevuta con X-Request-Id: ${xRequestId}`);
    console.log('Body:', request);


    const response = {
        paymentMethods: [
            {
                paymentMethodId: "pm-001",
                name: {
                    it: "Carta di credito",
                    en: "Credit Card"
                },
                description: {
                    it: "Pagamento con carta di credito",
                    en: "Pay with credit card"
                },
                status: "ENABLED",
                validityDateFrom: "2025-01-01",
                group: "CP",
                paymentMethodTypes: ["CARTE"],
                paymentMethodAsset: "asset.png",
                methodManagement: "ONBOARDABLE",
                disabledReason: null,
                paymentMethodsBrandAssets: {
                    VISA: "visa.png",
                    MC: "mc.png"
                },
                metadata: {
                    priority: "high"
                }
            }
        ]
    };

    res.json(response);
});



app.listen(port, () => {
    console.log(`Server PaymentMethod mock in ascolto su http://localhost:${port}`);
});



