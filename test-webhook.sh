curl -v -X POST http://localhost:8083/api/webhooks/register \
-H "Content-Type: application/json" \
-d '{
  "webhookUrl": "https://webhook.site/98e1fdc6-5983-4bde-8769-08cd5b822000",
  "secret": "webhook",
  "events": "TRANSACTION_APPROVED"
}'
