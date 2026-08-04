# Contributing

This repository uses a simple workflow and conventions to keep history clean and collaboration predictable.

Branching
- `main` : production-ready
- `dev` : integration branch
- `feat/<ticket>-short-desc` : feature branches
- `fix/<ticket>-short-desc` : bugfix branches
- `refactor/<short-desc>` : refactors

Commits
- Use Conventional Commit prefixes where possible: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`
- Write present-tense, imperative, descriptive messages (e.g. `fix: avoid NPE when user is null`).

Author identity
- Configure your git identity before contributing:
```
git config user.name "Your Name"
git config user.email "you@example.com"
```

Hooks
- To enable the provided pre-commit checks locally run:
```
git config core.hooksPath .githooks
```

Pull Requests / Reviews
- Open a pull request to merge into `dev` or `main` depending on the type of change.
- At least one approving review is required before merging.

Sensitive data
- Do not commit secrets (.env, credentials, keystores). If you think a secret was committed, rotate it immediately and contact the maintainers.

