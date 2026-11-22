function updateClock() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    document.getElementById('time').textContent = `${hours}:${minutes}`;

    const options = { weekday: 'long', day: 'numeric', month: 'short' };
    document.getElementById('date').textContent = now.toLocaleDateString('uk-UA', options);
}
setInterval(updateClock, 1000);
updateClock();

function updateSpeed(kmh) {
    const speedEl = document.getElementById('speedValue');
    if (!speedEl) return;

    const safeSpeed = Number.isFinite(Number(kmh)) ? Math.max(0, Math.round(Number(kmh))) : 0;
    speedEl.textContent = String(safeSpeed);
}

const appPackages = {
    navigation: "com.google.android.apps.maps",
    radio: "com.ts.MainUI",
    music: "com.android.music",
    bluetooth: "com.ts.bt",
    settings: "com.android.settings",
    camera: "com.ts.dvr"
};

function openApp(appKey) {
    const pkg = appPackages[appKey];
    if (window.Android && pkg) {
        window.Android.launchApp(pkg);
    } else {
        console.log("Запуск додатку: " + appKey);
    }
}

function mediaControl(action) {
    if (window.Android) {
        window.Android.sendMediaKey(action);
    }
}

function mediaAction(action) {
    mediaControl(action);
}

let currentFreq = 103.5;
function radioAction(action) {
    if (action === 'seek_up' || action === 'step_up') {
        currentFreq = +(currentFreq + 0.1).toFixed(1);
        if (currentFreq > 108.0) currentFreq = 87.5;
    } else if (action === 'seek_down' || action === 'step_down') {
        currentFreq = +(currentFreq - 0.1).toFixed(1);
        if (currentFreq < 87.5) currentFreq = 108.0;
    }
    document.getElementById('radioFreq').textContent = currentFreq.toFixed(1);

    if (window.Android) {
        window.Android.controlRadio(action, currentFreq);
    }
}

function openAllAppsModal() {
    document.getElementById('appsModal').style.display = 'flex';
    loadInstalledAppsList();
}

function closeAllAppsModal() {
    document.getElementById('appsModal').style.display = 'none';
}

function loadInstalledAppsList() {
    const grid = document.getElementById('appsGrid');
    grid.innerHTML = '<p style="grid-column: span 4; text-align:center; color:#94a3b8;">Завантаження...</p>';

    try {
        if (window.Android) {
            const jsonString = window.Android.getInstalledApps();
            const apps = JSON.parse(jsonString);

            grid.innerHTML = '';
            apps.forEach(app => {
                const tile = document.createElement('div');
                tile.className = 'app-tile';
                tile.innerHTML = `<div style="font-size: 1.8rem;">📦</div><span>${app.name}</span>`;
                tile.onclick = () => {
                    window.Android.launchApp(app.package);
                    closeAllAppsModal();
                };
                grid.appendChild(tile);
            });
        }
    } catch (e) {
        grid.innerHTML = '<p style="grid-column: span 4; text-align:center; color:#ef4444;">Помилка</p>';
    }
}
