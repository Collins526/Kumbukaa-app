# Environment Configuration for Render Deployment

## Setup Instructions for Render

### Prerequisites
1. Docker image built successfully
2. PostgreSQL database URL from Neon or Render
3. Email credentials (if using Gmail notifications)
4. Generated JWT secret

### Environment Variables Required
Set these in Render dashboard under "Environment":

```
SPRING_DATASOURCE_URL=jdbc:postgresql://your-host:5432/your-db?sslmode=require
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=your-secure-jwt-secret-key-min-32-chars
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

### Deployment Steps

1. **Connect your GitHub repository to Render**
   - Go to render.com and sign up/login
   - Create a new "Web Service"
   - Connect your GitHub repository

2. **Configure the service**
   - Name: `kumbuka` (or your preferred name)
   - Runtime: `Docker`
   - Branch: `main` (or your default branch)
   - Build Command: Leave empty (Dockerfile handles it)
   - Start Command: Leave empty (ENTRYPOINT in Dockerfile)

3. **Set Environment Variables**
   - Add all required environment variables in the "Environment" tab
   - Use Sync to GitHub if you want to version control (not recommended for secrets)

4. **Configure Database**
   - Use Neon PostgreSQL database (free tier available)
   - Update SPRING_DATASOURCE_URL with your connection string
   - Ensure SSL is enabled (sslmode=require)

5. **Set Auto-Deploy** (Optional)
   - Enable "Auto-Deploy" to deploy on git push
   - Alternatively, deploy manually from Render dashboard

### Health Check
- The Dockerfile includes a health check: `/health`
- Ensure your Spring Boot app exposes this endpoint
- If not available, add one or remove/update the health check URL

### Important Notes
- **Never commit credentials** to git - use environment variables only
- **JWT_SECRET** should be a strong, random string (minimum 32 characters recommended)
- **Database** must be accessible from Render servers (allow public access or setup private network)
- **Port** is set to 8080 and exposed in Dockerfile
- **Memory & CPU** defaults are usually sufficient for Spring Boot apps

### Monitoring & Logs
- View logs in Render dashboard under "Logs" tab
- Use `render logs` CLI command if installed
- Monitor performance metrics in dashboard

### Troubleshooting

If deployment fails:
1. Check Render logs for error messages
2. Verify all environment variables are set correctly
3. Test database connectivity with provided credentials
4. Ensure JWT_SECRET and mail passwords are correct
5. Check if JDBC URL format is correct for your database

### Database Connection Best Practices
- Use SSL/TLS for database connections (sslmode=require)
- Consider using connection pooling (HikariCP is default in Spring Boot)
- Set timeouts to prevent hanging connections

### Security Recommendations
1. Store secrets in Render's secure environment variables
2. Use strong, random JWT_SECRET
3. Enable HTTPS (automatic with Render)
4. Regularly rotate credentials
5. Monitor logs for suspicious activity

---

## Using Neon / PostgreSQL with Render

Follow these steps to use a Neon (or any managed PostgreSQL) database and validate connectivity from Render and locally.

- Provision the database (Neon or Render Postgres) and copy the connection details (host, port, database name, username, password). Neon often provides a connection string which includes SSL parameters.

- Set environment variables in the Render service (Environment tab):

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require&channel_binding=require
SPRING_DATASOURCE_USERNAME=<your-db-user>
SPRING_DATASOURCE_PASSWORD=<your-db-password>
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgresDialect
SPRING_DATASOURCE_HIKARI_MAXIMUM-POOL-SIZE=10
SPRING_DATASOURCE_HIKARI_CONNECTION-TIMEOUT=30000
SPRING_DATASOURCE_HIKARI_IDLE-TIMEOUT=600000
SPRING_PROFILES_ACTIVE=prod
```

- Important: remove any hard-coded credentials from `src/main/resources/application.properties` and rely on environment variables instead.

### Recommended JDBC URL examples

- Neon example (requires SSL and channel binding):
```
jdbc:postgresql://<your-neon-host>/<your-database>?sslmode=require&channel_binding=require
```

- Standard Postgres with SSL:
```
jdbc:postgresql://db.example.com:5432/mydb?sslmode=require
```

### Validate connectivity from your local machine

Option A — using `psql` (locally installed PostgreSQL client):

```bash
export PGPASSWORD="<your-db-password>"
psql "host=<host> port=<port> dbname=<database> user=<your-db-user> sslmode=require" -c "SELECT 1;"
```

Option B — using Docker `postgres` image (no local client required):

```bash
docker run --rm -e PGPASSWORD="<your-db-password>" postgres:15 psql -h <host> -p <port> -U <your-db-user> -d <database> -c "SELECT 1;"
```

If either returns `1`, the DB is reachable and authentication succeeds.

### Validate connectivity from Render (after deploy)

- Deploy the service on Render. In the Render dashboard, open the service's "Shell" (or create a one-off job) and run the same `psql` command from inside Render to verify network access.

- Alternatively, after deployment, check application startup logs in Render. Successful JDBC connection and Hikari pool initialization looks like:

```
com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Starting...
```

- Also verify the application health endpoint (configured at `/health`) from the public service URL:

```bash
curl -f https://<your-render-service>.onrender.com/health
```

A 200 response indicates the application is running and can reach the database (if your health endpoint checks DB connectivity). If your health endpoint does not check DB, verify DB-specific checks in logs or add a DB health check endpoint.

### Troubleshooting connection timeouts

- Ensure the database allows connections from Render; if using a VPC or private networking, configure accordingly.
- Double-check host, port, username, and password and that SSL parameters are correct.
- Increase Hikari `connectionTimeout` if necessary for cold-start connections to remote DBs.
- Temporarily enable more detailed JDBC logging by setting `logging.level.org.springframework=DEBUG` in Render environment (remove or lower after debugging).

### Production notes

- Use long, random `JWT_SECRET` stored in Render secrets.
- Rotate DB credentials regularly and avoid embedding them in source.
- Consider using a managed connection pool or read replicas for scaling.
