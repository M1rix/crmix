# Contributing

## Commit discipline

Each implementation task is exactly one commit. CRMIX milestone commits follow `M0X - imperative task`, for example `M03 - prevent appointment overlap`.

Do not combine unrelated fixes with a feature commit. Follow-up corrections use `M0X - fix ...`.

## Required checks

Backend: `spotlessCheck`, `test`, `bootJar`.
Frontend: `lint`, `format:check`, `test`, `build`.

GitHub Actions is CI-only. It must not upload build artifacts or release archives.
