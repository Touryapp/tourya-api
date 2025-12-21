# Script para ejecutar tourya-api con variables de entorno
# Uso: .\run.ps1

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "   Iniciando TourYa API Server" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Cargar variables de entorno
if (Test-Path ".env") {
    Write-Host "Cargando configuracion..." -ForegroundColor Cyan
    & .\load-env.ps1
    Write-Host ""
} else {
    Write-Host "Advertencia: No se encontro archivo .env" -ForegroundColor Yellow
    Write-Host "Creando desde plantilla..." -ForegroundColor Yellow
    
    if (Test-Path ".env.example") {
        Copy-Item .env.example .env
        Write-Host "Archivo .env creado" -ForegroundColor Green
        Write-Host "Por favor, edita .env con tus credenciales antes de continuar" -ForegroundColor Yellow
        Write-Host ""
        Read-Host "Presiona Enter despues de configurar .env"
        & .\load-env.ps1
    } else {
        Write-Host "No se encontro .env.example" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Iniciando servidor Spring Boot..." -ForegroundColor Green
Write-Host "Puerto: 8088" -ForegroundColor Gray
Write-Host "Contexto: /api/v1/" -ForegroundColor Gray
Write-Host ""

# Ejecutar Maven
.\mvnw.cmd spring-boot:run
