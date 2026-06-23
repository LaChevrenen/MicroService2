# FlightBook — TP Web Services Sécurisés

Application de réservation de vols avec une API REST Java et une SPA JavaScript, sécurisée via Keycloak (OpenID Connect).

## Stack technique

- **Backend** : Java 11, JAX-RS (Jersey 2.41), déployé sur Tomcat 9
- **Frontend** : HTML/CSS/JS vanilla, Keycloak JS adapter
- **Auth** : Keycloak 26 (OIDC), validation JWT via nimbus-jose-jwt
- **Infra** : Docker + Docker Compose

## Lancer le projet

### Avec Docker (recommandé)

```bash
# Démarrer Keycloak + le backend
docker compose up -d

# Créer l'utilisateur de test (1 seule fois, ou après docker compose down)
./keycloak-setup.ps1   # Windows
./keycloak-setup.sh    # Linux/Mac
```

Ensuite ouvrir http://localhost:8080 — tu es redirigé vers Keycloak, connecte-toi avec `testuser / testpass`.

**Commandes utiles :**
```bash
docker compose stop    # mettre en pause (données conservées)
docker compose start   # reprendre
docker compose down    # tout supprimer (nécessite de relancer le setup)
```

### Sans Docker (dev local)

```bash
cd java-rest-server
mvn tomcat7:run
```

Keycloak doit tourner séparément sur le port 8180.

## API REST

Toutes les routes nécessitent un token Bearer Keycloak (`Authorization: Bearer <token>`).

| Méthode | Route | Description |
|---------|-------|-------------|
| GET | `/api/vols` | Liste tous les vols |
| GET | `/api/vols/{id}` | Détail d'un vol |
| GET | `/api/compagnies` | Liste des compagnies |
| GET | `/api/compagnies/{nom}/vols` | Vols d'une compagnie |
| POST | `/api/reservations?volId={id}` | Réserver un vol |

Sans token → `403 Forbidden`.

Pour obtenir un token de test :
```bash
curl -X POST http://localhost:8180/realms/flightbook/protocol/openid-connect/token \
  -d "grant_type=password&client_id=flightbook-app&username=testuser&password=testpass"
```

## Ce qui a été fait

**Étape 1-2 — API REST + Frontend**
API JAX-RS avec 5 routes (vols, compagnies, réservations). SPA en 3 fichiers (HTML/CSS/JS) qui consomme l'API.

**Étape 3 — OAuth 2.1 Google**
Authentification Google via Authorization Code Flow. Les credentials sont dans `oauth.properties` (gitignored — ne pas committer).

**Étape 4 — Protection de l'API par Keycloak**
`KeycloakAuthFilter` (JAX-RS `@Provider`) valide le JWT sur chaque requête via le endpoint JWKS de Keycloak. Utilise `nimbus-jose-jwt` plutôt que l'adapter Keycloak officiel (qui est déprécié).

**Étape 5 — Authentification SPA via Keycloak**
Le frontend utilise le SDK JS Keycloak (`login-required`). Toutes les requêtes API incluent automatiquement le Bearer token. Le profil utilisateur est extrait directement du JWT.

**Étape 6 — Analyse du JWT**
Le token JWT généré par Keycloak est analysable sur https://jwt.io. Il est composé de 3 parties :

- **Header** : algorithme `RS256`, identifiant de la clé (`kid`) utilisée pour la signature
- **Payload** : les claims — `iss` (qui a émis le token), `sub` (ID unique de l'user), `azp` (le client), `name`, `email`, `realm_access` (rôles), `exp` (expiration)
- **Signature** : signée avec la clé privée RSA de Keycloak, vérifiable avec la clé publique exposée sur `/realms/flightbook/protocol/openid-connect/certs`

C'est cette vérification de signature que fait `KeycloakAuthFilter` à chaque appel API.

**Étape 7 — Contrat OpenAPI**
Le contrat est dans `openapi.yaml`. Pour le visualiser, copie le contenu sur https://editor.swagger.io.

Générer la doc et les clients (nécessite Docker) :
```bash
# Documentation HTML → generated/html-docs/index.html
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli generate \
  -i /local/openapi.yaml -g html2 -o /local/generated/html-docs

# Client JavaScript → generated/client-js/
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli generate \
  -i /local/openapi.yaml -g javascript -o /local/generated/client-js
```

Les fichiers générés sont dans `generated/`.

## Comptes

| Utilisateur | Mot de passe | Rôle |
|-------------|-------------|------|
| testuser | testpass | utilisateur |
| admin | admin | admin Keycloak (http://localhost:8180) |
