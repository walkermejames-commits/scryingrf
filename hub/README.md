# Scrying Public Intelligence Hub

This Cloudflare Worker accepts only opt-in, aggregate environmental reports. It deliberately rejects raw MAC addresses, SSIDs, names, coordinates, audio, images, contact data, and stable device identifiers.

Each accepted report contains a category, coarse cell, six-hour time bucket, and a client-generated rotating token. Results are returned only once at least five unique tokens have contributed. Counts are rounded down to the nearest five and records expire after 30 days.

The public hub is not a person-tracking, live-location, or device-identification service. It is vulnerable to aggregation poisoning until platform attestation and abuse controls are added; its results must therefore be presented as low-confidence community context, never a threat verdict.

Deploy with:

```sh
npm install
npx wrangler d1 migrations apply scrying-public-intelligence --remote
npx wrangler deploy
```
