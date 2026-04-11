# Translating ProxMoxOpen

ProxMoxOpen uses standard Android string resources and the Weblate platform for community translations. Please do **not** submit translation pull requests directly — all translations go through Weblate so that existing work is not overwritten.

## Weblate project

Translations live at:

```
https://hosted.weblate.org/projects/proxmoxopen/
```

(The exact URL will be filled in once the project is registered.)

Weblate hosts the free tier for FOSS projects, so joining is free and only requires an account on `hosted.weblate.org`.

## Source strings

The source-of-truth string file is `app/src/main/res/values/strings.xml`. Weblate picks this up automatically. Translations land in `app/src/main/res/values-<lang>/strings.xml`.

Target languages for the MVP:

- English (`values/`) — source
- German (`values-de/`)

Community-contributed languages will be added through Weblate as soon as they reach a reasonable completion percentage.

## Style guide

- Match the tone of the English source: concise, technical, polite.
- Do not invent new technical terms. When a term exists in the Proxmox VE UI, reuse it (for example "Knoten" for "node" in German).
- Keep button labels short (max 2 words where possible).
- Do not translate placeholders like `%1$s` or `%1$d`.

## Reporting translation issues

If a string reads wrong in your language, open an issue tagged `translation`. Include the string ID and the proposed fix.
