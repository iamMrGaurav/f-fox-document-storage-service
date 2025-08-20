#!/bin/bash

# FreightFox Document Storage Service - Quick Start
# This script sets up and runs the application locally

set -e

echo "FreightFox Document Storage Service - Quick Start"
echo "================================================="
echo ""

# Configuration
ECR_IMAGE="830325870732.dkr.ecr.ap-south-1.amazonaws.com/freight-fox:docs"
APP_PORT="8080"
AWS_REGION="ap-south-1"

# Function to print output
print_status() {
    echo "SUCCESS: $1"
}

print_warning() {
    echo "WARNING: $1"
}

print_error() {
    echo "ERROR: $1"
}

print_info() {
    echo "INFO: $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to get user choice
get_user_choice() {
    echo ""
    echo "Choose how to run the application:"
    echo "1) Docker (Recommended - No local setup needed)"
    echo "2) Local Java (Build and run with Maven)"
    echo ""
    read -p "Enter your choice (1 or 2): " choice
    echo ""
}

# Function to setup AWS credentials
setup_aws_credentials() {
    echo "AWS Configuration Setup"
    echo "========================"
    
    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
        print_warning "AWS credentials not found in environment variables"
        echo ""
        echo "Please provide your AWS credentials:"
        read -p "AWS Access Key ID: " aws_access_key
        read -s -p "AWS Secret Access Key: " aws_secret_key
        echo ""
        read -p "AWS Region (default: ap-south-1): " aws_region
        aws_region=${aws_region:-ap-south-1}
        
        export AWS_ACCESS_KEY_ID="$aws_access_key"
        export AWS_SECRET_ACCESS_KEY="$aws_secret_key"
        export AWS_DEFAULT_REGION="$aws_region"
        
        print_status "AWS credentials configured"
    else
        print_status "Using existing AWS credentials"
    fi
    echo ""
}

# Function to get S3 bucket name
get_s3_bucket() {
    echo "S3 Bucket Configuration"
    echo "======================="
    
    echo "Please provide your S3 bucket name:"
    echo "Format: freight-fox-doc-storage"
    echo "Example: freight-fox-doc-storage"
    read -p "S3 Bucket Name: " s3_bucket_name
    
    if [ -z "$s3_bucket_name" ]; then
        print_error "S3 bucket name is required"
        exit 1
    fi
    
    export S3_BUCKET_NAME="$s3_bucket_name"
    print_info "Using S3 bucket: $s3_bucket_name"
    echo ""
}

# Function to run with Docker
run_with_docker() {
    echo "Running with Docker"
    echo "=================="
    
    # Check if Docker is installed
    if ! command_exists docker; then
        print_error "Docker is not installed!"
        echo "Please install Docker from: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running!"
        echo "Please start Docker and try again"
        exit 1
    fi
    
    print_info "Pulling latest image from ECR..."
    if docker pull $ECR_IMAGE; then
        print_status "Image pulled successfully"
    else
        print_error "Failed to pull image. Please check your internet connection."
        exit 1
    fi
    
    # Stop any existing container
    docker stop freight-fox-docs 2>/dev/null || true
    docker rm freight-fox-docs 2>/dev/null || true
    
    print_info "Starting container..."
    
    # Run container
    docker run -d \
        --name freight-fox-docs \
        -p $APP_PORT:8080 \
        -e AWS_ACCESS_KEY="$AWS_ACCESS_KEY_ID" \
        -e AWS_SECRET_KEY="$AWS_SECRET_ACCESS_KEY" \
        -e AWS_REGION="$AWS_REGION" \
        -e S3_BUCKET_NAME="$S3_BUCKET_NAME" \
        $ECR_IMAGE
    
    print_status "Application started in Docker container"
    
    # Wait for application to start
    print_info "Waiting for application to start..."
    sleep 10
    
    # Check if container is running
    if docker ps | grep -q freight-fox-docs; then
        print_status "Container is running successfully"
        
        # Test health endpoint
        if curl -s http://localhost:$APP_PORT/api/freight-fox/s3-bucket/search?userName=healthcheck >/dev/null; then
            print_status "Application health check passed"
        else
            print_warning "Application may still be starting..."
        fi
    else
        print_error "Container failed to start"
        echo "Container logs:"
        docker logs freight-fox-docs
        exit 1
    fi
}

# Function to run locally with Java
run_with_java() {
    echo "Running with Local Java"
    echo "======================="
    
    # Check Java version
    if command_exists java; then
        java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_version" -ge 21 ] 2>/dev/null; then
            print_status "Java $java_version found"
        else
            print_error "Java 21 or higher is required. Current version: $java_version"
            echo "Please install Java 21 from: https://adoptium.net/"
            exit 1
        fi
    else
        print_error "Java is not installed!"
        echo "Please install Java 21 from: https://adoptium.net/"
        exit 1
    fi
    
    # Check Maven
    if command_exists mvn; then
        print_status "Maven found"
    else
        print_error "Maven is not installed!"
        echo "Please install Maven from: https://maven.apache.org/install.html"
        exit 1
    fi
    
    # Set environment variables
    export AWS_ACCESS_KEY="$AWS_ACCESS_KEY_ID"
    export AWS_SECRET_KEY="$AWS_SECRET_ACCESS_KEY"
    
    print_info "Building application..."
    if mvn clean package -DskipTests; then
        print_status "Application built successfully"
    else
        print_error "Build failed"
        exit 1
    fi
    
    print_info "Starting application..."
    
    # Start application in background
    nohup mvn spring-boot:run > application.log 2>&1 &
    APP_PID=$!
    
    print_status "Application starting with PID: $APP_PID"
    
    # Wait for application to start
    print_info "Waiting for application to start..."
    sleep 15
    
    # Test health endpoint
    max_attempts=12
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:$APP_PORT/api/freight-fox/s3-bucket/search?userName=healthcheck >/dev/null; then
            print_status "Application started successfully"
            break
        else
            if [ $attempt -eq $max_attempts ]; then
                print_error "Application failed to start"
                echo "Check application.log for details"
                exit 1
            fi
            print_info "Attempt $attempt/$max_attempts - waiting..."
            sleep 5
            attempt=$((attempt + 1))
        fi
    done
}

# Function to display final information
show_final_info() {
    echo ""
    echo "Setup Complete!"
    echo "==============="
    echo ""
    print_status "Application is running successfully!"
    echo ""
    echo "Application URLs:"
    echo "=================="
    echo "Main API: http://localhost:$APP_PORT"
    echo "Swagger UI: http://localhost:$APP_PORT/swagger-ui/html"
    echo "Health Check: http://localhost:$APP_PORT/api/freight-fox/s3-bucket/search?userName=healthcheck"
    echo ""
    echo "Configuration:"
    echo "=============="
    echo "S3 Bucket: $S3_BUCKET_NAME"
    echo "AWS Region: $AWS_REGION"
    echo ""
    echo "Quick Test Commands:"
    echo "==================="
    echo "# Upload a file"
    echo "curl -X POST \"http://localhost:$APP_PORT/api/freight-fox/s3-bucket/upload\" \\"
    echo "  -H \"Content-Type: multipart/form-data\" \\"
    echo "  -F \"file=@your-file.pdf\" \\"
    echo "  -F \"userName=test-user\""
    echo ""
    echo "# Search files"
    echo "curl \"http://localhost:$APP_PORT/api/freight-fox/s3-bucket/search?userName=test-user\""
    echo ""
    if [ "$choice" = "1" ]; then
        echo "Docker Management:"
        echo "=================="
        echo "# View logs: docker logs freight-fox-docs"
        echo "# Stop app: docker stop freight-fox-docs"
        echo "# Start app: docker start freight-fox-docs"
        echo "# Remove app: docker rm freight-fox-docs"
    else
        echo "Java Management:"
        echo "==============="
        echo "# View logs: tail -f application.log"
        echo "# Stop app: kill $APP_PID"
    fi
    echo ""
    print_info "Application is ready to use."
}

# Main execution
main() {
    # Get user choice
    get_user_choice
    
    # Setup AWS credentials
    setup_aws_credentials
    
    # Get S3 bucket name
    get_s3_bucket
    
    # Run based on user choice
    case $choice in
        1)
            run_with_docker
            ;;
        2)
            run_with_java
            ;;
        *)
            print_error "Invalid choice. Please run the script again."
            exit 1
            ;;
    esac
    
    # Show final information
    show_final_info
}

# Handle Ctrl+C gracefully
trap 'echo ""; print_info "Setup interrupted by user"; exit 1' INT

# Run main function
main