#!/bin/bash
# Setup Keycloak pour FlightBook
# Usage : ./keycloak-setup.sh
# Prerequis : Docker installe

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== FlightBook - Setup Keycloak ==="

# 1. Lancer Keycloak si pas deja lance
if docker ps --filter "name=keycloak-flightbook" --format "{{.Names}}" | grep -q keycloak-flightbook; then
    echo "[OK] Keycloak deja en cours d'execution"
else
    echo "[...] Demarrage de Keycloak..."
    docker run -d --name keycloak-flightbook -p 8180:8080 \
        -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
        -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
        quay.io/keycloak/keycloak:26.2.5 start-dev

    echo "  Attente du demarrage..."
    until docker logs keycloak-flightbook 2>&1 | grep -q "started in"; do
        sleep 5
        echo -n "."
    done
    echo ""
    echo "[OK] Keycloak pret"
fi

# 2. Token admin
ADMIN_TOKEN=$(curl -s -X POST \
    "http://localhost:8180/realms/master/protocol/openid-connect/token" \
    -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
    | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

AUTH="Authorization: Bearer $ADMIN_TOKEN"

# 3. Importer le realm
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "http://localhost:8180/admin/realms" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d @"$SCRIPT_DIR/keycloak-realm-export.json")

if [ "$HTTP_CODE" = "201" ]; then
    echo "[OK] Realm 'flightbook' importe"
elif [ "$HTTP_CODE" = "409" ]; then
    echo "[OK] Realm 'flightbook' existe deja"
else
    echo "[ERR] Import realm (HTTP $HTTP_CODE)"
fi

# 4. Creer l'utilisateur de test
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "http://localhost:8180/admin/realms/flightbook/users" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d '{
        "username":"testuser",
        "email":"testuser@flightbook.local",
        "firstName":"Test",
        "lastName":"User",
        "emailVerified":true,
        "enabled":true,
        "credentials":[{"type":"password","value":"testpass","temporary":false}]
    }')

if [ "$HTTP_CODE" = "201" ]; then
    echo "[OK] Utilisateur testuser cree (mot de passe : testpass)"
elif [ "$HTTP_CODE" = "409" ]; then
    echo "[OK] Utilisateur testuser existe deja"
else
    echo "[ERR] Creation user (HTTP $HTTP_CODE)"
fi

echo ""
echo "=== Setup termine ==="
echo "Admin Keycloak : http://localhost:8180  (admin/admin)"
echo "Lancer le backend : cd java-rest-server && mvn tomcat7:run"
