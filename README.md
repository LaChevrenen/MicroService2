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

### Pièges courants

**`kubectl` connection refused** — `KUBECONFIG` n'est pas exporté dans le terminal courant. À mettre dans chaque terminal, ou une fois pour toutes dans `~/.bashrc` :
```bash
echo 'export KUBECONFIG="/etc/rancher/k3s/k3s.yaml"' >> ~/.bashrc
source ~/.bashrc
```

**Fedora/RHEL — pods injoignables (502)** — `firewalld` bloque le réseau entre pods. Ajouter les interfaces k3s à la zone trusted :
```bash
sudo firewall-cmd --permanent --zone=trusted --add-interface=cni0
sudo firewall-cmd --permanent --zone=trusted --add-interface=flannel.1
sudo firewall-cmd --reload
```

**Traefik démarre avant que k3s soit prêt** — Si Traefik a des erreurs RBAC au démarrage, redémarrer le déploiement suffit :
```bash
kubectl rollout restart deployment/traefik -n kube-system
```

**Windows (Docker Desktop) — `docker compose push` échoue en HTTPS** — Docker Desktop ignore `insecure-registries` pour les pushs et tente HTTPS même sur une registry HTTP. Contournement via `crane` :
```bash
curl -LO "https://github.com/google/go-containerregistry/releases/latest/download/go-containerregistry_Linux_x86_64.tar.gz"
tar -xzf go-containerregistry_Linux_x86_64.tar.gz crane
docker save registry.infres.fr/flightbook:latest -o flightbook.tar
./crane push --insecure flightbook.tar registry.infres.fr/flightbook:latest
```

**Windows (WSL) — le terminal se ferme après `systemctl restart k3s`** — Normal, k3s redémarre WSL. Rouvrir un terminal et relancer `sudo systemctl start k3s` + `export KUBECONFIG`.

**Windows — `registry.infres.fr` non résolu par Docker Desktop** — Ajouter l'IP WSL dans le fichier hosts Windows (PowerShell admin) :
```powershell
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "$(wsl hostname -I) registry.infres.fr"
```

---

### 1. Installer k3s

```bash
sudo su -
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" sh -
systemctl disable k3s
exit

export KUBECONFIG="/etc/rancher/k3s/k3s.yaml"
kubectl get node   # Ready
```

### 2. Registry privée

```bash
# Résolution DNS locale
sudo sh -c 'echo "$(hostname -I | awk '"'"'{print $1}'"'"') registry.infres.fr" >> /etc/hosts'

# Déployer la registry dans le cluster
kubectl apply -f k8s/DockerRegistry.yaml

# Autoriser k3s à puller en HTTP
sudo su -
cat <<EOF >/etc/rancher/k3s/registries.yaml
mirrors:
  registry.infres.fr:
    endpoint:
      - "http://registry.infres.fr"
EOF
systemctl restart k3s
exit

# Autoriser Docker à pusher en HTTP
sudo sh -c 'echo '"'"'{"insecure-registries":["registry.infres.fr"]}'"'"' > /etc/docker/daemon.json'
sudo systemctl restart docker

wget -q -O- http://registry.infres.fr/v2/_catalog   # {"repositories":[]}
```

### 3. Build et push

```bash
docker compose build
docker compose push

wget -q -O- http://registry.infres.fr/v2/_catalog   # {"repositories":["flightbook"]}
```

### 4. Déployer sur k3s

```bash
sudo sh -c 'echo "$(hostname -I | awk '"'"'{print $1}'"'"') flightbook.infres.fr" >> /etc/hosts'

kubectl apply -f k8s/flightbook.yaml
kubectl get pods -w   # attendre 2x Running

curl http://flightbook.infres.fr/   # retourne le HTML de l'app
```

### 5. HPA — autoscaling

```bash
kubectl apply -f k8s/flightbook-hpa.yaml
kubectl get hpa -w
```

Stress test pour valider le scaling :
```bash
# Terminal 1 — watch
kubectl get hpa -w

# Terminal 2 — charge CPU sur tous les pods
for pod in $(kubectl get pod -l app=flightbook -o name); do
  kubectl exec $pod -- /bin/sh -c "while true; do cat /dev/urandom | md5sum; done" &
done
wait
```

Résultat observé : CPU 2% → 501% → scale up **2 → 4 replicas** en ~30s. Ctrl+C pour arrêter.

## Comptes

| Utilisateur | Mot de passe | Rôle |
|-------------|--------------|------|
| testuser | testpass | utilisateur |
| admin | admin | admin Keycloak (http://localhost:8180) |
