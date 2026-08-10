# Contributing to PureHub

Thank you for helping build practical tools that remain free, ad-free, privacy-first, and open source.

## Before opening an issue

1. Search existing issues and recent releases.
2. Try the latest production PWA or Android prerelease.
3. Do not include passwords, tokens, private documents, personal QR content, or other sensitive data.
4. Report security vulnerabilities privately according to [SECURITY.md](SECURITY.md).

## A useful bug report

Include:

- the affected mini app;
- web or Android, including browser/app version;
- device model and OS version when relevant;
- exact steps to reproduce;
- expected and actual behavior;
- a redacted screenshot or recording when it materially helps.

## Proposing a feature

Prefer one clear workflow improvement over a broad feature list. Explain the problem, who experiences it, how the proposed change helps, and whether it affects offline behavior or device permissions.

## Development workflow

1. Fork the repository and create a focused branch.
2. Keep unrelated formatting or generated-file changes out of the pull request.
3. Add or update tests for behavioral changes.
4. Run the relevant checks locally.
5. Open a pull request using the repository template.

Web checks:

```bash
cd pwa
npm install
npm run lint
npm run build
```

Android checks:

```bash
./gradlew testStandardDebugUnitTest testFdroidDebugUnitTest lintStandardDebug lintFdroidDebug assembleStandardDebug assembleFdroidDebug
```

Command Center checks:

```bash
python -m unittest discover -s command_center/tests -p "test_*.py"
```

## Product principles

Contributions should preserve these boundaries:

- no advertising or tracking-driven design;
- no mandatory account for utility workflows;
- local processing and storage whenever practical;
- clear permission requests tied to a user action;
- honest wording about security, privacy, precision, and platform limitations;
- accessible UI and reduced-motion support;
- English-first product copy with maintained Vietnamese and Chinese routes.

## Pull-request scope

Small pull requests are easier to review. Describe the user-visible outcome, test evidence, privacy or permission impact, screenshots for UI changes, and any migration or release considerations.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
