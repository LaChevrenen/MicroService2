// ─── Keycloak ─────────────────────────────────────────────────────────────────
const keycloak = new Keycloak('keycloak.json');

window.addEventListener('load', () => {
    keycloak.init({ onLoad: 'login-required' })
        .then(authenticated => {
            if (!authenticated) return;
            afficherProfil();
            chargerCompagnies();
        })
        .catch(() => {
            document.getElementById('container').innerHTML =
                '<p class="message error">Impossible de contacter Keycloak (http://localhost:8180).</p>';
        });
});

// ─── Profil depuis le JWT Keycloak ────────────────────────────────────────────
function afficherProfil() {
    const p = keycloak.tokenParsed;
    document.getElementById('userZone').innerHTML =
        '<div class="user-info">' +
        '<span>Bonjour, ' + (p.name || p.preferred_username) + '</span>' +
        '<button class="btn-login" onclick="keycloak.logout()">Déconnexion</button>' +
        '</div>';
}

// ─── fetch avec token Bearer (rafraîchi automatiquement) ──────────────────────
async function authFetch(url, options = {}) {
    try { await keycloak.updateToken(30); } catch (_) { keycloak.login(); return; }
    options.headers = { ...(options.headers || {}), Authorization: 'Bearer ' + keycloak.token };
    return fetch(url, options);
}

// ─── Route 3 : charge les compagnies pour le filtre ───────────────────────────
async function chargerCompagnies() {
    try {
        const res = await authFetch('api/compagnies');
        if (!res.ok) throw new Error();
        const compagnies = await res.json();
        const sel = document.getElementById('filtreCompagnie');
        compagnies.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c;
            opt.textContent = c;
            sel.appendChild(opt);
        });
        sel.disabled = false;
    } catch (_) {}
}

// ─── Route 1 : GET /api/vols ──────────────────────────────────────────────────
async function chargerTousLesVols() {
    document.getElementById('filtreCompagnie').value = '';
    setStatus('Chargement…');
    try {
        const res = await authFetch('api/vols');
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const vols = await res.json();
        afficherVols(vols);
        setStatus(vols.length + ' vol(s) trouvé(s)');
    } catch (e) {
        afficherErreur(e.message);
    }
}

// ─── Route 4 : GET /api/compagnies/{nom}/vols ─────────────────────────────────
async function filtrerParCompagnie() {
    const nom = document.getElementById('filtreCompagnie').value;
    if (!nom) { chargerTousLesVols(); return; }
    setStatus('Chargement…');
    try {
        const res = await authFetch('api/compagnies/' + encodeURIComponent(nom) + '/vols');
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const vols = await res.json();
        afficherVols(vols);
        setStatus(vols.length + ' vol(s) pour ' + nom);
    } catch (e) {
        afficherErreur(e.message);
    }
}

// ─── Route 2 : GET /api/vols/{id} ────────────────────────────────────────────
async function voirDetail(id) {
    const panel = document.getElementById('detail-' + id);
    if (panel.classList.contains('open')) {
        panel.classList.remove('open');
        return;
    }
    panel.textContent = 'Chargement…';
    panel.classList.add('open');
    try {
        const res = await authFetch('api/vols/' + id);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const v = await res.json();
        panel.innerHTML =
            '<p><strong>ID :</strong> ' + v.id + '</p>' +
            '<p><strong>Compagnie :</strong> ' + v.compagnie + '</p>' +
            '<p><strong>Numéro :</strong> ' + v.numero + '</p>' +
            '<p><strong>Place :</strong> ' + v.place + '</p>' +
            '<p><strong>Prix :</strong> ' + v.prix.toFixed(2) + ' €</p>' +
            '<p><strong>Date :</strong> ' + formatDate(v.date) + '</p>';
    } catch (e) {
        panel.textContent = 'Erreur : ' + e.message;
    }
}

// ─── Route 5 : POST /api/reservations?volId={id} ─────────────────────────────
async function reserver(id) {
    try {
        const res = await authFetch('api/reservations?volId=' + id, { method: 'POST' });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const resa = await res.json();
        afficherToast('Réservation confirmée !\nRéf. ' + resa.reference + ' — ' + resa.compagnie + ' ' + resa.numero);
    } catch (e) {
        afficherToast('Erreur : ' + e.message);
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function afficherVols(vols) {
    const container = document.getElementById('container');
    if (vols.length === 0) {
        container.innerHTML = '<p class="message">Aucun vol trouvé.</p>';
        return;
    }
    container.innerHTML = '<div class="grid">' + vols.map(volCard).join('') + '</div>';
}

function volCard(vol) {
    return `
    <div class="card">
        <div class="card-header">
            <span class="compagnie">${vol.compagnie}</span>
            <span class="numero">${vol.numero}</span>
        </div>
        <div class="card-info">
            <div class="info-item">
                <label>Date</label>
                <span>${formatDate(vol.date)}</span>
            </div>
            <div class="info-item">
                <label>Place</label>
                <span>${vol.place}</span>
            </div>
        </div>
        <div class="card-footer">
            <span class="prix">${vol.prix.toFixed(2)} €</span>
            <div style="display:flex;gap:0.4rem">
                <button class="btn-detail" onclick="voirDetail(${vol.id})">Détails</button>
                <button class="btn-reserver" onclick="reserver(${vol.id})">Réserver</button>
            </div>
        </div>
        <div class="detail-panel" id="detail-${vol.id}"></div>
    </div>`;
}

function formatDate(dateStr) {
    return new Date(dateStr).toLocaleDateString('fr-FR', {
        day: '2-digit', month: 'short', year: 'numeric'
    });
}

function setStatus(msg) {
    document.getElementById('status').textContent = msg;
}

function afficherErreur(msg) {
    document.getElementById('container').innerHTML =
        '<p class="message error">Erreur : ' + msg + '</p>';
    setStatus('');
}

function afficherToast(msg) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 3500);
}
