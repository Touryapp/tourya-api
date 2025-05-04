#!/bin/bash

echo "Iniciando despliegue en EC2..."

# Iniciar sesión en Amazon ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Descargar la última imagen desde Amazon ECR
docker pull <AWS_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/tourya-api:latest

# Detener y eliminar el contenedor existente
docker stop tourya-api || true
docker rm tourya-api || true

# Ejecutar el nuevo contenedor
docker run -d --name tourya-api -p 8088:8088 \
  --env DB_USER=${DB_USER} \
  --env DB_PASSWORD=${DB_PASSWORD} \
  --env DB_HOST=${DB_HOST} \
  --env DB_PORT=${DB_PORT} \
  <AWS_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/tourya-api:latest

echo "Despliegue completado."