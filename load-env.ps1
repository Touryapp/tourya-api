# Script para cargar variables de entorno desde archivo .env
# Uso: .\load-env.ps1

$envFile = ".env"

if (Test-Path $envFile) {
    Write-Host "Cargando variables de entorno desde $envFile..." -ForegroundColor Green
    
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remover comillas si existen
            $value = $value -replace '^[""'']|[""'']$', ''
            
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  Configurado: $name" -ForegroundColor Cyan
        }
    }
    
    Write-Host "`nVariables de entorno cargadas exitosamente!" -ForegroundColor Green
    Write-Host "Ahora puedes ejecutar: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
} else {
    Write-Host "Error: No se encontro el archivo .env" -ForegroundColor Red
    Write-Host "Por favor, copia .env.example a .env y configura tus valores:" -ForegroundColor Yellow
    Write-Host "  Copy-Item .env.example .env" -ForegroundColor Cyan
}
