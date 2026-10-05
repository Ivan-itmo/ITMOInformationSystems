import { request, state, fillLocationSelects, deleteLocation } from './common.js';
import { finiteNumber, clearValidation } from './validation.js';
let locationsBody;
export async function loadLocations() {
    state.locations = await request('/locations');
    locationsBody.innerHTML = state.locations.map(location => (
        `<tr><td>${location.id}</td><td>${location.x}</td><td>${location.y}</td><td>${location.z}</td><td><button class="table-btn danger" data-id="${location.id}">Удалить</button></td></tr>`
    )).join('');
    fillLocationSelects();
}

export function initLocations() {
    locationsBody = document.querySelector('#locationsBody');
    locationsBody.addEventListener('click', async event => {
        const button = event.target.closest('button[data-id]');
        if (!button) return;
        try {
            await deleteLocation(button.dataset.id);
        } catch (error) {
            document.querySelector('#locationMessage').className = 'error';
            document.querySelector('#locationMessage').textContent = error.message;
        }
    });
    const form = document.querySelector('#locationForm');
    const message = document.querySelector('#locationMessage');
    form.addEventListener('submit', async event => {
        event.preventDefault();
        message.textContent = '';
        try {
            const payload = {};
            for (const axis of ['X', 'Y', 'Z']) {
                payload[axis.toLowerCase()] = finiteNumber(document.querySelector(`#loc${axis}`), `Координата ${axis}`);
            }
            await request('/locations', { method: 'POST', body: JSON.stringify(payload) });
            form.reset();
            clearValidation(form);
            await loadLocations();
            message.className = 'ok';
            message.textContent = 'Сделано';
        } catch (error) {
            message.className = 'error';
            message.textContent = error.message;
        }
    });
}
