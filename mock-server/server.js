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




app.listen(port, () => {
    console.log(`Server PaymentMethod mock in ascolto su http://localhost:${port}`);
});
