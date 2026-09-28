# Public intelligence hub

Endpoint: `https://scrying-public-hub.walkermejames.workers.dev`

The public hub is optional and accepts only these aggregate categories: BLE pattern, Wi-Fi posture, magnetic anomaly, and motion anomaly. A client must first derive a coarse cell and six-hour time bucket locally, create a fresh rotating token, and submit no raw radio identifier, SSID, device name, exact coordinate, audio, image, contact, or stable installation identifier.

Insights remain hidden until at least five tokens contribute to the same category/cell/bucket. Counts are rounded down to a multiple of five. Both raw aggregate submissions and aggregates expire after 30 days.

The deployment has no authentication because it is a public opt-in aggregate API. This means it is susceptible to data poisoning until anti-abuse controls, app attestation, and a privacy review are added. Its output must be displayed as low-confidence community context, never a security verdict or evidence about a person.
