import { initCommon, setRefresh } from './common.js';
import { initRoutes, loadRoutes, refreshOpenRoute } from './routes.js';
import { initLocations, loadLocations } from './locations.js';
import { initOperations } from './operations.js';
import { initValidation } from './validation.js';

const errorContainer = document.querySelector('#appError');
function showError(error) {
    const container = document.querySelector('#routesError') ?? errorContainer;
    container.textContent = error.message;
}

let refreshPending = false;
let refreshPromise = null;
function refreshAll() {
    refreshPending = true;
    if (!refreshPromise) {
        refreshPromise = (async () => {
            while (refreshPending) {
                refreshPending = false;
                await loadLocations();
                await loadRoutes();
                await refreshOpenRoute();
            }
            errorContainer.textContent = '';
        })().finally(() => { refreshPromise = null; });
    }
    return refreshPromise;
}

function connectUpdates() {
    const url = new URL('updates', document.baseURI);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(url);
    socket.addEventListener('open', () => refreshAll().catch(showError));
    socket.addEventListener('message', () => refreshAll().catch(showError));
    socket.addEventListener('close', () => setTimeout(connectUpdates, 2000));
}

async function start() {
    const fragments = await Promise.all(['routes', 'locations', 'operations'].map(async name => {
        const response = await fetch(`sections/${name}.html`);
        if (!response.ok) throw new Error(`Не удалось загрузить раздел ${name}.`);
        return response.text();
    }));
    document.querySelector('#views').innerHTML = fragments.join('\n');
    initCommon();
    initValidation();
    initRoutes();
    initLocations();
    initOperations();
    setRefresh(refreshAll);
    document.querySelectorAll('nav button').forEach(button => {
        button.addEventListener('click', () => {
            document.querySelectorAll('nav button').forEach(btn => btn.classList.toggle('active', btn === button));
            document.querySelectorAll('main > section').forEach(section => {
                section.classList.toggle('hidden', section.id !== `${button.dataset.view}View`);
            });
        });
    });
    connectUpdates();
    await refreshAll();
}

start().catch(showError);
