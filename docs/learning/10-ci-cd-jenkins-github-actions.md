# CI/CD — Jenkins + GitHub Actions in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## Two CI/CD Pipelines
This project has both because:
- **Jenkins**: standard in large enterprises/banks (on-prem, full control)
- **GitHub Actions**: modern cloud-native (most companies moving toward this)

Being fluent in both is a differentiator on senior tech lead job descriptions.

## Jenkins Pipeline
**File:** `Jenkinsfile`

### Pipeline Stages

```
Checkout
  └─> Code Quality (PARALLEL: Checkstyle + SpotBugs)
        └─> Unit Tests (JUnit + JaCoCo coverage)
              └─> Build (mvn package)
                    └─> SonarQube Analysis + Quality Gate
                          └─> Docker Build & Push
                                └─> [develop branch] Deploy to Dev
                                      └─> [develop branch] Integration Tests
                                            └─> [main branch] Deploy to Staging (MANUAL APPROVAL)
```

### Key Patterns

**Parallel quality stages** (mirrors Agile sprint Definition of Done):
```groovy
stage('Code Quality') {
    parallel {
        stage('Checkstyle') { ... }
        stage('SpotBugs')   { ... }
    }
}
```

**Branch-conditional deployment:**
```groovy
stage('Deploy to Dev') {
    when { branch 'develop' }   // Only on develop branch
    steps { sh 'docker-compose up -d' }
}
```

**Manual approval gate** (change management for staging):
```groovy
stage('Deploy to Staging') {
    when { branch 'main' }
    steps {
        input(message: "Deploy to staging?", submitter: 'tech-lead,release-manager')
        // Only proceeds after human approval
    }
}
```

### Environment Variables
```groovy
environment {
    JWT_SECRET = credentials('nexabank-jwt-secret')  // Jenkins credential store
}
```

Never hardcode secrets in Jenkinsfile — use Jenkins Credentials.

## GitHub Actions Workflows
**Files:** `.github/workflows/`

### Three Workflows

| File | Trigger | Purpose |
|---|---|---|
| `ci.yml` | Pull Request | Build + test + coverage |
| `cd-dev.yml` | Push to `develop` | Build images → push to ghcr.io → deploy dev |
| `cd-staging.yml` | Push to `main` | Manual approval → build → staging deploy |

### GitHub Environment Protection (replaces Jenkins `input`)
In `cd-staging.yml`:
```yaml
environment: staging    # Requires approval from required reviewers
                        # Configured in: GitHub → Settings → Environments → staging
```

This is the GitHub Actions equivalent of Jenkins `input{}` — a human must click "Approve" in the GitHub UI before the staging deployment proceeds.

### Caching Maven Dependencies
```yaml
- uses: actions/setup-java@v4
  with:
    cache: maven   # Caches ~/.m2/repository between runs
```

Without caching: Maven downloads all dependencies on every run (~3-5 min). With caching: ~30 seconds.

## Agile/SDLC Connection
The pipeline reflects Agile engineering practices:
1. Every PR triggers CI (quality gate before merge)
2. Merge to `develop` auto-deploys to dev environment (continuous delivery)
3. Integration tests on every dev deployment (catches integration bugs early)
4. Staging requires approval (change management — mirrors CAB approval process in enterprises)
5. `post.failure` emails the team (proactive communication — matches job description)

## Interview Talking Points
- **Jenkins vs. GitHub Actions?** Jenkins is self-hosted (more control, more maintenance). GitHub Actions is managed (easier setup, integrates with GitHub PRs, no server to maintain). Most companies are migrating to GitHub Actions or similar.
- **What is the purpose of the quality gate?** Ensures code meets minimum quality standards (coverage %, no critical bugs) before merging. Enforces team standards consistently.
- **What is SonarQube?** Static code analysis tool — detects bugs, vulnerabilities, code smells. The `waitForQualityGate abortPipeline: true` blocks the pipeline until SonarQube returns a pass/fail.
- **How do you handle secrets in pipelines?** Jenkins: Credentials plugin. GitHub Actions: Repository Secrets (`${{ secrets.JWT_SECRET }}`). Never commit secrets to code.

## Questions to Ask Your AI
- "What is the difference between continuous delivery and continuous deployment?"
- "How would you add automated database rollback to the pipeline if deployment fails?"
- "What is a quality gate in SonarQube and how do you configure thresholds?"
- "How do GitHub Actions matrix builds work for testing against multiple Java versions?"
- "What is the Gitflow branching strategy and how does it relate to this pipeline?"
