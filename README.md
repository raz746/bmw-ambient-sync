# BMW Ambient Sync

Experimental music-reactive ambient lighting project for a 2023 BMW M2 (G87) with iDrive 8 and a Samsung Galaxy S26 playing Pandora via Android Auto.

## Status

**Research and prototype stage. No vehicle-control functionality is implemented.** Installing code from this repository does not enable beat-synchronized BMW lighting.

## Development plan

1. **Android companion:** Experiment with Android's user-authorized MediaProjection / AudioPlaybackCapture for Pandora. Playback capture works only if the source app permits it. Otherwise, evaluate microphone-based beat detection with appropriate consent and privacy safeguards.
2. **Beat estimation:** Process incoming audio locally to estimate bass energy and produce a low-bandwidth intensity signal. Do not record or upload raw audio.
3. **Simulator:** Test audio-to-light behavior off-vehicle using a visual lighting mock-up.
4. **iDrive receiver:** Explore the MGU Store packaging format and a reliable phone-to-head-unit transport. Compatibility is not yet confirmed.
5. **Vehicle feasibility:** Assess manufacturer-supported control interfaces, rate limits, and fail-safe behavior before considering any hardware integration. No high-frequency body-controller commands without validation.

## Proposed structure

- `android-companion/` — Android test app for capture permission and beat detection
- `idrive-app/` — iDrive UI / receiver research and prototypes
- `simulator/` — off-car lighting visualizer
- `docs/` — compatibility findings and design notes

## Safety and privacy

Test only while parked. Audio capture requires explicit user permission and may be blocked by Pandora. Avoid recording conversations, storing raw audio, or transmitting raw audio. Vehicle control is out of scope until supported safe operating limits are understood. Do not copy source from third-party repositories without checking license terms and obtaining permission where required.

## Next step

Implement and build a standalone Android beat-meter proof of concept, without connecting it to the car.
