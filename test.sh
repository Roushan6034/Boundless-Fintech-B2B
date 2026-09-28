echo '{"webhookUrl": "https://webhook.site/abc", "secret": "webhook", "events": "TRANSACTION_APPROVED"}' > payload.json
curl -v -X POST http://localhost:8083/api/webhooks/register -H "Content-Type: application/json" -d @payload.json
