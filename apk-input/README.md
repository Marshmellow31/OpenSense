# Private APK input

Place lawfully obtained, authorized original APKs here. APKs and provenance sidecars are ignored by Git.
Do not install or execute them on your personal phone or primary machine.

Preferred files, keeping the package/version in the filename:

- `com.contus.mahindra.xuv500-2.8.2.apk` (2018-era comparison candidate)
- `com.contus.mahindra.xuv500-3.5.apk` (later archival candidate; actual identity must be checked)
- `com.visteon-0.46.apk` (legacy comparison)
- The exact APK that previously connected successfully to your vehicle, if available.

The 3.5 and 0.46 APKs have now been downloaded and statically inspected; see `docs/research.md` for their exact hashes and provenance. The 2.8.2 APK and a known-original device export remain optional comparison inputs.
If an app export contains split APKs, provide the entire original set in a private folder and explain its source; do not rename an XAPK/ZIP as an APK. The current inventory handles standalone `.apk` files only.

For each APK optionally add `<filename>.apk.provenance.json`:

```json
{
  "source_url_or_device": "Describe privately; never include credentials",
  "obtained_at": "YYYY-MM-DD",
  "method": "Original device export or exact archival download URL",
  "authorization": "Describe your permission to analyze",
  "previously_connected_vehicle": "Unknown"
}
```

Run `python tools/inspect_apks.py --android-sdk <SDK> --java <JDK17-or-newer-java>`.
Output goes to ignored `private-analysis/`. Review and redact before sharing any findings.
