# FlightBook — TP Web Services Sécurisés

Application de réservation de vols. API REST Java + SPA JS, auth déléguée à Keycloak.

## Stack

- Java 11, JAX-RS (Jersey 2.41), Tomcat 9
- HTML/CSS/JS vanilla, Keycloak JS SDK
- Keycloak 26, nimbus-jose-jwt
- Docker + Docker Compose

## Lancer

```bash
docker compose up -d
./keycloak-setup.ps1   # Windows — crée testuser dans Keycloak
./keycloak-setup.sh    # Linux/Mac
```

Ouvrir http://localhost:8080, se connecter avec `testuser / testpass`.

```bash
docker compose stop / start   # pause sans perdre les données
docker compose down           # tout supprimer
```

Sans Docker : `cd java-rest-server && mvn tomcat7:run` (Keycloak doit tourner sur 8180).

## API

Toutes les routes nécessitent `Authorization: Bearer <token>`, sinon `403`.

| Méthode | Route | Réponse |
|---------|-------|---------|
| GET | `/api/vols` | 200 liste |
| GET | `/api/vols/{id}` | 200 ou 404 |
| GET | `/api/compagnies` | 200 liste |
| GET | `/api/compagnies/{nom}/vols` | 200 liste |
| POST | `/api/reservations?volId={id}` | 201 ou 404 |

Token de test :
```bash
curl -X POST http://localhost:8180/realms/flightbook/protocol/openid-connect/token \
  -d "grant_type=password&client_id=flightbook-app&username=testuser&password=testpass"
```

## Étapes du TP

**1-2 — API REST + Frontend**
5 routes JAX-RS, SPA en 3 fichiers (HTML/CSS/JS).

**3 — OAuth 2.1 Google**
Authorization Code Flow via les servlets `OAuthLoginServlet` / `OAuthCallbackServlet`. Credentials dans `oauth.properties` (gitignored). Ces endpoints restent dans le code mais sont remplacés par Keycloak à l'étape 5.

**4 — API sécurisée par Keycloak**
`KeycloakAuthFilter` vérifie le JWT sur chaque requête via JWKS. On utilise `nimbus-jose-jwt` (l'adapter officiel Keycloak est déprécié depuis Keycloak 17).

**5 — SPA authentifiée par Keycloak**
SDK JS Keycloak en mode `login-required`. Le token est attaché à chaque appel API, le profil vient du JWT.

**6 — Analyse JWT**
Token récupérable via curl ou DevTools (onglet Network). À coller sur https://jwt.io.

3 parties :
- **Header** : algo de signature (`RS256`), `kid` = identifiant du certificat public utilisé pour signer
- **Payload** : les claims — `iss` (Keycloak, vérifié côté API), `sub` (ID opaque de l'user), `typ: Bearer`, `azp` (le client), `realm_access` (rôles du royaume), `iat`/`exp` (émission/expiration)
- **Signature** : vérifiable avec la clé publique Keycloak (`/realms/flightbook/protocol/openid-connect/certs`)

`KeycloakAuthFilter` vérifie la signature + l'`iss` + l'expiration à chaque appel. Si l'un des trois est invalide → 403.

**7 — Contrat OpenAPI**
Contrat dans `openapi.yaml`, à visualiser sur https://editor.swagger.io. Doc HTML et client JS générés dans `generated/` via `openapitools/openapi-generator-cli`.

---

## Déploiement Kubernetes (k3s)

Suite du TP — déploiement sur un cluster k3s local via WSL2.

### 1. Installer k3s dans WSL2

```powershell
# Windows — mettre à jour WSL2
wsl.exe --update
```

Ajouter dans `C:\Users\<user>\.wslconfig` :
```ini
[wsl2]
networkingMode=mirrored
```

Redémarrer WSL (`wsl --shutdown`), puis dans WSL :

```bash
sudo su -
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" sh -
systemctl disable k3s   # ne pas démarrer au boot
export KUBECONFIG="/etc/rancher/k3s/k3s.yaml"
kubectl get node        # doit afficher Ready
```

### 2. Registry privée

```bash
# Faire pointer registry.infres.fr vers l'IP locale
sudo su -
echo "$(hostname -I | awk '{print $1}') registry.infres.fr" >> /etc/hosts

# Déployer la registry dans le cluster
kubectl apply -f k8s/DockerRegistry.yaml

# Autoriser k3s à puller depuis cette registry (HTTP)
cat <<EOF >/etc/rancher/k3s/registries.yaml
mirrors:
  registry.infres.fr:
    endpoint:
      - "http://registry.infres.fr"
EOF
systemctl restart k3s
```

Configurer Docker Engine (dans `/etc/docker/daemon.json`) :
```json
{
  "insecure-registries": ["registry.infres.fr"]
}
```
```bash
systemctl restart docker
```

### 3. Build et push

```bash
docker-compose build
docker-compose push
# Vérifier : wget http://registry.infres.fr/v2/flightbook/tags/list
```

### 4. Déployer sur k3s

```bash
kubectl apply -f k8s/flightbook.yaml
```

## Comptes

| | Mot de passe | |
|--|--|--|
| testuser | testpass | utilisateur |
| admin | admin | admin Keycloak (http://localhost:8180) |
