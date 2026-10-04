import { initCommon, setRefresh } from './common.js';
import { initRoutes, loadRoutes } from './routes.js';
import { initLocations, loadLocations } from './locations.js';
import { initOperations } from './operations.js';
import { initValidation } from './validation.js';

const errorContainer = document.querySelector('#appError');
function showError(error) { errorContainer.textContent = error.message; }

async function refreshAll() {
    await loadLocations();
    await loadRoutes();
    errorContainer.textContent = '';
}

function connectUpdates() {
    const socket = new WebSocket(`ws://${location.host}/updates`);
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
