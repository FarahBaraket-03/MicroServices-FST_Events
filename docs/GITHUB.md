# Put the project in GitHub

No GitHub repository has been created or pushed by this handoff. Choose your account/repository and visibility. Keep it private if it contains coursework you should not publish.

## First push

Create an empty repository named, for example, events-microservices on GitHub. Do not initialize the remote with a second README. From the extracted project root:

```powershell
git init -b main
git add .
git status
# Review staged files: no .env, passwords, node_modules, target, tool volumes.
git commit -m "Prepare Spring Cloud event management project"
git remote add origin https://github.com/YOUR_USERNAME/events-microservices.git
git push -u origin main
```

Replace YOUR_USERNAME. Use Git Credential Manager/browser authentication or SSH. Never put a personal access token in a repository URL, pom.xml or Jenkinsfile. If user.name/user.email are missing, configure your own identity before committing.

## Continue with real incremental work

Use branches such as feature/config-discovery, feature/cqrs-logistics and test/resilience-demo. Commit actual changes and observations, use meaningful messages, and open pull requests if working as a team. Do not invent earlier commits or Jira activity to imply work that did not occur.

Import docs/GIT_JIRA_TASKS.csv into your Jira project or create those tasks manually. Update tasks as you validate each stage. Record the actual local demo results and fix failures in separate commits.

## Build automation

- .github/workflows/ci.yml verifies the Maven reactor and builds Angular on push/PR. These jobs need internet to fetch dependencies; they do not require your local Docker stack.
- Jenkinsfile runs in your existing Jenkins container and can analyze with Sonar and publish to Nexus once tools/credentials are configured.
- No production deployment is performed. The provided application is intended for your local PC.

## Before pushing

Run `mvn verify` and `npm run build` from frontend. Read docs/VALIDATION.md for the delivery-time results. Use `git diff --cached` to review the exact content being pushed. The archive excludes all dependencies and compiled JARs; GitHub/your PC rebuilds them from source.
