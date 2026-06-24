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

## Rapport

### 1-2 — API REST et frontend

On a mis en place 5 routes JAX-RS sur Jersey 2.41 avec des données statiques en mémoire. L'objectif n'était pas la persistance mais la structure REST : comprendre comment mapper des ressources (`/vols`, `/compagnies`, `/reservations`) sur des verbes HTTP et des codes de retour cohérents. La configuration Maven pour faire tourner Jersey sur Tomcat est verbeuse pour peu de résultat visible, mais ça force à comprendre ce que fait chaque dépendance. Le passage XML→JSON (étapes 1c→1d) s'est fait en ajoutant Jackson par-dessus le binding JaxB existant — les deux coexistent ce qui n'est pas idéal, mais ça montre bien que JSON est devenu la norme alors que JaxB était pensé XML à l'origine. La SPA appelle l'API via `fetch` et est servie directement par Tomcat sans serveur web séparé.

### 3 — Délégation OAuth 2.1 vers Google

Cette étape a permis de comprendre concrètement le flow Authorization Code : l'utilisateur est redirigé vers Google, s'authentifie là-bas, Google renvoie un code d'autorisation, on l'échange côté serveur contre un access token, et on s'en sert pour appeler l'API Google. On a utilisé `google-oauth-client-servlet` qui abstrait les deux étapes via `OAuthLoginServlet` et `OAuthCallbackServlet`. Ce qui est moins évident au premier abord : l'URI de redirection configurée dans la Google Cloud Console doit correspondre exactement à ce que le serveur envoie — port inclus, sinon Google refuse. Les credentials sont dans `oauth.properties` (gitignored). Ces endpoints restent dans le code pour illustrer l'approche, mais sont remplacés fonctionnellement par Keycloak à l'étape 5 — ce qui montre bien que Keycloak est un OAuth provider comme un autre, juste hébergé localement.

### 4 — Sécurisation de l'API avec Keycloak

C'est l'étape où on a vraiment compris le fonctionnement de la vérification JWT. `KeycloakAuthFilter` intercepte chaque requête, parse le Bearer token, récupère les JWKS publics de Keycloak pour vérifier la signature RSA, puis contrôle l'issuer et l'expiration. On a d'abord voulu utiliser l'adapter officiel Keycloak, mais il est déprécié depuis Keycloak 17 et génère des erreurs à la compilation — on est donc passés à `nimbus-jose-jwt` en implémentant la vérification manuellement. C'est plus de code, mais on comprend exactement ce qui se passe à chaque étape plutôt que de déléguer à une boîte noire. Les JWKS sont mis en cache 10 minutes pour ne pas appeler Keycloak à chaque requête. Ce qu'on ferait en plus : vérifier les rôles dans `realm_access.roles` pour du RBAC par route.

### 5 — Authentification de la SPA

Le SDK JS Keycloak s'intègre en quelques lignes mais cache beaucoup de complexité. En mode `login-required`, toute visite sans session active redirige automatiquement vers Keycloak — l'utilisateur ne voit jamais de formulaire custom. La partie la moins évidente est la gestion du refresh : `keycloak.updateToken(30)` avant chaque appel API renouvelle silencieusement le token s'il expire dans moins de 30 secondes, ce qui évite des déconnexions en cours de navigation. Le profil vient directement du JWT décodé côté client (`keycloak.tokenParsed`) sans appel `/userinfo` supplémentaire. `keycloak.js` est un fichier lourd (~100 Ko) qui génère des avertissements dans l'IDE, et le SDK est lui-même déprécié depuis Keycloak 26 au profit des librairies OIDC standard comme `oidc-client-ts` — on l'a gardé parce qu'il était le plus simple à intégrer rapidement.

### 6 — Analyse du token JWT

En décodant le token sur jwt.io, on voit concrètement ce que transportent les trois parties. Le header contient `alg: RS256` et un `kid` — cet identifiant de clé permet à Keycloak de faire tourner ses certificats sans invalider les sessions en cours, le client sait quelle clé publique utiliser pour vérifier. Le payload contient les claims utiles : `iss` (l'URL du realm, que notre filtre compare), `sub` (l'ID opaque de l'utilisateur), `realm_access.roles`, et `exp` en UNIX timestamp. Ce qui est intéressant c'est que le token est entièrement auto-porteur — le serveur n'a aucun état à maintenir, il vérifie juste la signature et l'expiration. La signature elle-même n'est pas lisible mais vérifiable via les JWKS publics de Keycloak. Si l'un des trois contrôles échoue dans `KeycloakAuthFilter`, la requête est rejetée avec 403.

### 7 — Contrat OpenAPI

On a défini le contrat dans `openapi.yaml` (OpenAPI 3.0) à la main, puis généré la doc HTML et un client JS via `openapitools/openapi-generator-cli`. L'exercice montre bien la valeur d'un contrat écrit explicitement : le client JS généré est directement utilisable sans connaître l'implémentation, et la documentation HTML donne une vue claire de l'API pour un consommateur externe. La limite de l'approche manuelle est la synchronisation — si une route change dans le code, le YAML ne se met pas à jour tout seul. La solution propre serait de générer le contrat depuis les annotations JAX-RS avec `swagger-core` pour avoir une seule source de vérité.

---

## Déploiement Kubernetes (k3s)

### Pièges courants

**`kubectl` connection refused** — `KUBECONFIG` n'est pas exporté dans le terminal courant. À ajouter une fois dans `~/.bashrc` :
```bash
echo 'export KUBECONFIG="/etc/rancher/k3s/k3s.yaml"' >> ~/.bashrc && source ~/.bashrc
```

**Fedora/RHEL — pods injoignables (502)** — `firewalld` bloque le réseau entre pods :
```bash
sudo firewall-cmd --permanent --zone=trusted --add-interface=cni0
sudo firewall-cmd --permanent --zone=trusted --add-interface=flannel.1
sudo firewall-cmd --reload
```

**Traefik démarre avant que k3s soit prêt** — erreurs RBAC au démarrage, un restart suffit :
```bash
kubectl rollout restart deployment/traefik -n kube-system
```

**Windows — `docker compose push` échoue en HTTPS** — Docker Desktop ignore `insecure-registries` pour les pushs. Contournement via `crane` :
```bash
curl -LO "https://github.com/google/go-containerregistry/releases/latest/download/go-containerregistry_Linux_x86_64.tar.gz"
tar -xzf go-containerregistry_Linux_x86_64.tar.gz crane
docker save registry.infres.fr/flightbook:latest -o flightbook.tar
./crane push --insecure flightbook.tar registry.infres.fr/flightbook:latest
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
sudo sh -c 'echo "$(hostname -I | awk '"'"'{print $1}'"'"') registry.infres.fr" >> /etc/hosts'

kubectl apply -f k8s/DockerRegistry.yaml

sudo su -
cat <<EOF >/etc/rancher/k3s/registries.yaml
mirrors:
  registry.infres.fr:
    endpoint:
      - "http://registry.infres.fr"
EOF
systemctl restart k3s
exit

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

Stress test :
```bash
for pod in $(kubectl get pod -l app=flightbook -o name); do
  kubectl exec $pod -- /bin/sh -c "while true; do cat /dev/urandom | md5sum; done" &
done
wait
```

Résultat observé : CPU 2% → 501% → scale up **2 → 4 replicas** en ~30s.

---

## Comptes

| Utilisateur | Mot de passe | Rôle |
|-------------|--------------|------|
| testuser | testpass | utilisateur Keycloak |
| admin | admin | admin Keycloak (port 8180) |
