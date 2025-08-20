# FreightFox Document Storage Service

A Spring Boot REST API service for managing document storage using AWS S3 with comprehensive search capabilities.

## Features

- Upload documents to AWS S3
- Search documents by user and metadata
- List documents with pagination
- Download documents with presigned URLs
- Delete documents
- Health check endpoints
- Comprehensive error handling
- Input validation

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- AWS Account with S3 bucket
- AWS CLI configured (optional)

### S3 Bucket Requirement

You must create an S3 bucket before running the application. Create a bucket with any name you prefer (example: `freight-fox-doc-storage`) in the `ap-south-1` region through the AWS Console or AWS CLI.

Make sure your AWS credentials have the following S3 permissions for the bucket:
- `s3:GetObject`
- `s3:PutObject`
- `s3:DeleteObject`
- `s3:ListBucket`

Example bucket creation:
```bash
aws s3 mb s3://freight-fox-doc-storage --region ap-south-1
```

## Quick Start (Recommended)

### One-Command Setup
```bash
# Download and run the quick-start script
curl -sSL https://raw.githubusercontent.com/your-repo/f-fox-doc-storage-service/main/quick-start.sh | bash

# Or if you have the repository:
./quick-start.sh
```

### Docker Only (Super Quick)
```bash
# Just Docker with AWS credentials
./docker-run.sh
```

**Note:** Both scripts will ask for your S3 bucket name. Make sure you have created the S3 bucket first as mentioned in the prerequisites.

## Local Development Setup

### 1. Clone and Setup

```bash
git clone <repository-url>
cd f-fox-doc-storage-service
```

### 2. Configure AWS S3

#### Option A: Environment Variables
```bash
export AWS_ACCESS_KEY=your-access-key
export AWS_SECRET_KEY=your-secret-key
export AWS_REGION=ap-south-1
export S3_BUCKET_NAME=your-bucket-name
```

#### Option B: Update application.properties
```properties
# Edit src/main/resources/application.properties
aws.access.key=your-access-key
aws.secret.key=your-secret-key
aws.s3.region=ap-south-1
aws.s3.bucket.name=your-bucket-name
```

### 3. Create S3 Bucket

```bash
# Create bucket (replace with your bucket name)
aws s3 mb s3://your-bucket-name --region ap-south-1

# Set bucket policy for public read (optional)
aws s3api put-bucket-policy --bucket your-bucket-name --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    }
  ]
}'
```

### 4. Build and Run

```bash
# Build the application
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR file
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 5. API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui/html`

## API Endpoints

### Document Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/freight-fox/s3-bucket/upload` | Upload document |
| GET | `/api/freight-fox/s3-bucket/search` | Search documents |
| GET | `/api/freight-fox/s3-bucket/list` | List all documents |
| GET | `/api/freight-fox/s3-bucket/download/{fileName}` | Download document |
| DELETE | `/api/freight-fox/s3-bucket/delete/{fileName}` | Delete document |

### Health Check
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/freight-fox/s3-bucket/search?userName=healthcheck` | Health check |

## Usage Examples

### Upload Document
```bash
curl -X POST "http://localhost:8080/api/freight-fox/s3-bucket/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf" \
  -F "userName=john.doe"
```

### Search Documents
```bash
# Search by username only
curl "http://localhost:8080/api/freight-fox/s3-bucket/search?userName=john.doe&page=0&size=10"

# Search by username and search term
curl "http://localhost:8080/api/freight-fox/s3-bucket/search?userName=john.doe&searchTerm=invoice&page=0&size=10"
```

### Download Document
```bash
curl "http://localhost:8080/api/freight-fox/s3-bucket/download/document.pdf?userName=john.doe"
```

### Delete Document
```bash
curl -X DELETE "http://localhost:8080/api/freight-fox/s3-bucket/delete/document.pdf?userName=john.doe"
```

## Configuration

### Application Properties

```properties
# Application Configuration
app.file.max-size=50MB
app.search.default-page-size=100
app.search.max-page-size=1000
app.download.url-expiry-seconds=900

# AWS S3 Configuration
aws.access.key=${AWS_ACCESS_KEY:your-access-key}
aws.secret.key=${AWS_SECRET_KEY:your-secret-key}
aws.s3.region=${AWS_REGION:ap-south-1}
aws.s3.bucket.name=${S3_BUCKET_NAME:your-bucket-name}
```

## Development

### Running Tests
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### Code Style
```bash
# Format code
mvn fmt:format

# Check style
mvn fmt:check
```

### Building for Production
```bash
# Build optimized JAR
mvn clean package -Dmaven.test.skip=true

# Build Docker image
docker build -t freight-fox-docs .
```

## Docker Support

### Build and Run with Docker
```bash
# Build image
docker build -t freight-fox-docs .

# Run container (replace with your actual AWS credentials and S3 bucket name)
docker run -p 8080:8080 \
  -e AWS_ACCESS_KEY=your-access-key \
  -e AWS_SECRET_KEY=your-secret-key \
  -e AWS_REGION=ap-south-1 \
  -e S3_BUCKET_NAME=your-bucket-name \
  freight-fox-docs
```

## Troubleshooting

### Common Issues

1. **S3 Access Denied**
   - Verify AWS credentials
   - Check bucket permissions
   - Ensure bucket exists in correct region

2. **Application Won't Start**
   - Check Java version (requires Java 21+)
   - Verify all required environment variables
   - Check port 8080 availability

3. **File Upload Fails**
   - Check file size limits (default 50MB)
   - Verify S3 bucket write permissions
   - Check network connectivity to AWS

### Logs
```bash
# View application logs
tail -f logs/application.log

# Enable debug logging
export LOGGING_LEVEL_ROOT=DEBUG
mvn spring-boot:run
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.