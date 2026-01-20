# DevOps and Deployment Specification

## Purpose
This document specifies the deployment architecture, continuous integration (CI) pipeline, and operational procedures for the Meer platform.

## Scope
- CI/CD Pipeline (GitHub Actions)
- Deployment Strategy (SSH/Systemd)
- Environment and Secret Management
- Monitoring and Health Checks

## CI/CD Pipeline
The project uses GitHub Actions to automate testing, database migrations, and deployments.

### Main Workflow: `deploy-prod.yml`
1. **Environment Setup**: 
   - Decodes a base64 encoded `.env` file from GitHub Secrets.
   - Injects environment-specific vars (e.g., `FIREBASE_PROJECT_ID`).
2. **Database Migration**: 
   - Executes `./gradlew flywayMigrate` on the production database before the application build.
3. **Build**: 
   - Compiles the application using JDK 17 and Gradle.
   - Generates a versioned BootJar (e.g., `meer-abc1234.jar`).
4. **Delivery**: 
   - Copies the JAR and `.env` file to the production host via SCP.
5. **Restart**: 
   - Updates the Systemd service definition and restarts the application.
   - Injects Firebase service account JSON from secrets during the restart phase.

## Production Environment
- **Host**: Linux (Ubuntu-based).
- **Process Manager**: `Systemd` (Service named via `SERVICE` env var).
- **Execution**: `java -jar /opt/meer-prod/meer.jar` (invoked via `scripts/run-prod.sh`).
- **Secret Management**:
  - Application secrets are stored in `/opt/meer-prod/.env`.
  - Firebase credentials are stored in `/opt/meer-prod/firebase-service-account.json`.

## Monitoring and Health
- **Actuator**: The system uses Spring Boot Actuator for health monitoring.
- **Health Check**: `GET /actuator/health` (or configured `HEALTH_URL`).
- **Smoke Test**: The deployment pipeline includes a 200-second window (40 attempts) to verify the application reports status `UP` after a restart.
- **Disk Monitoring**: A dedicated `.github/workflows/disk-monitor.yml` checks for storage pressure on the host.

## Invariants
- Migrations must succeed before the new application code is deployed.
- Every deployment must be followed by a successful smoke test.
- Production environment variables are strictly isolated from source control.

## Deployment Scripts
- `scripts/run-local.sh`: Environment setup and execution for developers.
- `scripts/run-prod.sh`: Entry point for production execution.
