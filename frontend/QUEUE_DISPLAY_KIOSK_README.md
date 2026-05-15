# Queue Display Kiosk

Large factory queue screen for the public queue endpoint.

## Local URLs

- Production queue: `http://localhost:5173/queue-display?queue=PRODUCTION`
- Accounting queue: `http://localhost:5173/queue-display?queue=ACCOUNTING`
- Force Arabic: `http://localhost:5173/queue-display?queue=PRODUCTION&lang=ar`
- Force English: `http://localhost:5173/queue-display?queue=PRODUCTION&lang=en`

## Local Run

Start the queue backend through the normal OPS services. The queue service is configured on port `9040`, and Vite proxies `/api/public/queues` to it.

```powershell
cd frontend
npm install
npm run dev
```

Open one of the URLs above on the display PC.

## Production Build

```powershell
cd frontend
npm ci
npm run build
```

Serve `frontend/dist` with nginx and use `frontend/kiosk/kiosk-nginx.conf` as the static/proxy config. The nginx config proxies:

```text
/api/public/queues/* -> http://queue-service:9040/api/public/queues/*
```

If your backend host is not named `queue-service`, edit `proxy_pass` in `frontend/kiosk/kiosk-nginx.conf`.

## Chrome Kiosk

Windows:

```powershell
"C:\Program Files\Google\Chrome\Application\chrome.exe" --kiosk "http://localhost:5173/queue-display?queue=PRODUCTION&lang=ar" --no-first-run --disable-infobars
```

Linux:

```bash
google-chrome --kiosk "http://localhost:5173/queue-display?queue=PRODUCTION&lang=ar"
```
