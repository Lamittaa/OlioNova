

# Customer Tracking Page Plan

This is the later external customer page plan for olive batch tracking.

## Goal

Build a public, simple page where a customer enters an order ID or secure tracking code and sees:

- batch status: registered, in progress, done
- approximate progress percent and remaining minutes
- tank location: A, B, C, or D
- friendly pickup message when done

## Backend Contract To Reuse

Current internal endpoints:

- `GET /api/tracking/orders/{orderId}`
- `GET /api/tracking/batches/{batchId}`

Response already includes the public-friendly fields:

- `statusLabel`
- `progressPercent`
- `estimatedRemainingMinutes`
- `tankLabel`
- `friendlyMessage`
- `tanks`

Before making it public, add a customer-safe lookup token so customers do not expose sequential order IDs. Example:

- Add `trackingCode` to `batch_tracking`
- Return it to reception after the batch is created
- Public endpoint: `GET /api/public/tracking/{trackingCode}`
- Permit only that public endpoint in tracking-service security

## Frontend Shape

Create a separate route like `/track` outside the authenticated app shell.

Recommended UI:

- one centered lookup field for tracking code
- progress bar with the three steps
- four tank tiles A/B/C/D, with the active tank highlighted
- short customer message from `friendlyMessage`

Keep the customer page read-only. Operators should continue using the authenticated production page to change status or tank.
