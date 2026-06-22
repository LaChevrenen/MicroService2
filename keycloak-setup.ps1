# Setup Keycloak pour FlightBook
# Usage : .\keycloak-setup.ps1
# Prerequis : Docker installe et en cours d'execution

Write-Host "=== FlightBook - Setup Keycloak ===" -ForegroundColor Cyan

# 1. Lancer Keycloak si pas deja lance
$existing = docker ps --filter "name=keycloak-flightbook" --format "{{.Names}}" 2>$null
if ($existing -eq "keycloak-flightbook") {
    Write-Host "[OK] Keycloak deja en cours d'execution" -ForegroundColor Green
} else {
    Write-Host "[...] Demarrage de Keycloak..." -ForegroundColor Yellow
    docker run -d --name keycloak-flightbook -p 8180:8080 `
        -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
        -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
        quay.io/keycloak/keycloak:26.2.5 start-dev | Out-Null

    # Attendre que Keycloak soit pret
    $elapsed = 0
    do {
        Start-Sleep -Seconds 5; $elapsed += 5
        $ready = docker logs keycloak-flightbook 2>&1 | Select-String "started in"
        Write-Host "  Attente... ${elapsed}s"
    } while (-not $ready -and $elapsed -lt 120)
    Write-Host "[OK] Keycloak pret" -ForegroundColor Green
}

# 2. Token admin
$adminToken = (Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8180/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin").access_token
$h = @{ Authorization = "Bearer $adminToken"; "Content-Type" = "application/json" }

# 3. Importer le realm
$realmJson = Get-Content "$PSScriptRoot\keycloak-realm-export.json" -Raw
try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8180/admin/realms" -Headers $h -Body $realmJson | Out-Null
    Write-Host "[OK] Realm 'flightbook' importe" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[OK] Realm 'flightbook' existe deja" -ForegroundColor Green
    } else {
        Write-Host "[ERR] Import realm : $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 4. Creer l'utilisateur de test
$userBody = @{
    username = "testuser"
    email = "testuser@flightbook.local"
    firstName = "Test"
    lastName = "User"
    emailVerified = $true
    enabled = $true
    credentials = @(@{ type = "password"; value = "testpass"; temporary = $false })
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Method Post -Uri "http://localhost:8180/admin/realms/flightbook/users" -Headers $h -Body $userBody | Out-Null
    Write-Host "[OK] Utilisateur testuser cree (mot de passe : testpass)" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "[OK] Utilisateur testuser existe deja" -ForegroundColor Green
    } else {
        Write-Host "[ERR] Creation user : $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Setup termine ===" -ForegroundColor Cyan
Write-Host "Admin Keycloak : http://localhost:8180  (admin/admin)"
Write-Host "Lancer le backend : cd java-rest-server && mvn tomcat7:run"
