#!/bin/bash

# FreightFox Document Storage - Quick Docker Run

echo "FreightFox Document Storage - Quick Docker Run"
echo "=============================================="

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running! Please start Docker and try again."
    exit 1
fi

# Get AWS credentials
echo "Please provide your AWS credentials:"
read -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -s -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
echo ""

# Get S3 bucket name
echo "Please provide your S3 bucket name:"
echo "Format: freight-fox-doc-storage"
echo "Example: freight-fox-doc-storage"
read -p "S3 Bucket Name: " S3_BUCKET_NAME

if [ -z "$S3_BUCKET_NAME" ]; then
    echo "ERROR: S3 bucket name is required"
    exit 1
fi

# Configuration
AWS_REGION="ap-south-1"
ECR_IMAGE="830325870732.dkr.ecr.ap-south-1.amazonaws.com/freight-fox:docs"

echo ""
echo "Configuration:"
echo "   AWS Region: $AWS_REGION"
echo "   S3 Bucket: $S3_BUCKET_NAME"
echo "   Docker Image: $ECR_IMAGE"
echo ""

# Pull and run
echo "Pulling Docker image..."
docker pull $ECR_IMAGE

echo "Starting application..."
docker stop freight-fox-docs 2>/dev/null || true
docker rm freight-fox-docs 2>/dev/null || true

docker run -d \
    --name freight-fox-docs \
    -p 8080:8080 \
    -e AWS_ACCESS_KEY="$AWS_ACCESS_KEY_ID" \
    -e AWS_SECRET_KEY="$AWS_SECRET_ACCESS_KEY" \
    -e AWS_REGION="$AWS_REGION" \
    -e S3_BUCKET_NAME="$S3_BUCKET_NAME" \
    $ECR_IMAGE

echo ""
echo "Waiting for application to start..."
sleep 10

if docker ps | grep -q freight-fox-docs; then
    echo "SUCCESS: Application started successfully!"
    echo ""
    echo "Access your application at:"
    echo "   Main API: http://localhost:8080"
    echo "   Swagger UI: http://localhost:8080/swagger-ui/html"
    echo ""
    echo "Management commands:"
    echo "   View logs: docker logs freight-fox-docs"
    echo "   Stop app: docker stop freight-fox-docs"
    echo "   Remove app: docker rm freight-fox-docs"
else
    echo "ERROR: Failed to start application"
    echo "Logs:"
    docker logs freight-fox-docs
fi