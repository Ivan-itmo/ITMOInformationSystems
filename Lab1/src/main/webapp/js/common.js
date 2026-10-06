import { clearValidation, positiveInteger, locationId, resourceId, formField } from './validation.js';

const api = 'api';
export const state = { locations: [] };
let refresh = async () => {};
export function setRefresh(handler) { refresh = handler; }
export async function refreshAll() { await refresh(); }
let deleteKind = 'routes';
let deleteDialog;
let deleteForm;
let replacementLocations = [];
export function initCommon() {
    deleteDialog = document.querySelector('#deleteDialog');
    deleteForm = document.querySelector('#deleteForm');
    document.querySelectorAll('[data-close]').forEach(button => {
        button.addEventListener('click', () => document.getElementById(button.dataset.close).close());
    });
    deleteForm.addEventListener('submit', async event => {
    event.preventDefault();
    try {
        clearValidation(deleteForm);
        const id = positiveInteger(formField(deleteForm, 'id'), 'ID удаляемого объекта');
        const select = formField(deleteForm, 'replacementLocationId');
        const replacement = deleteKind === 'locations' ? locationId(select, 'Локация для замены', {
            required: select.required, locations: replacementLocations, excludeId: id
        }) : null;
        if (!deleteForm.reportValidity()) return;
        
        const options = { method: 'DELETE' };
        if (deleteKind === 'locations') {
            options.body = JSON.stringify({ 
                replacementLocationId: replacement
            });
        }
        
        await request(`/${deleteKind}/${id}`, options);
        deleteDialog.close();
        await refreshAll();
    } catch (error) { 
        document.querySelector('#deleteError').textContent = error.message; 
    }
});
}
export async function request(path, options = {}) {
    const fetchConfig = { 
        headers: { 'Content-Type': 'application/json' } 
    };
    Object.assign(fetchConfig, options);

    const response = await fetch(`${api}${path}`, fetchConfig);
    
    if (!response.ok) {
        switch (response.status) {
            case 400: throw new Error('Ошибка 400: некорректные данные запроса.');
            case 404: throw new Error('Ошибка 404: объект не найден.');
            case 405: throw new Error('Ошибка 405: метод запроса не поддерживается.');
            case 409: throw new Error('Ошибка 409: конфликт данных.');
            case 500: throw new Error('Ошибка 500: внутренняя ошибка сервера.');
            case 503: throw new Error('Ошибка 503: сервер временно недоступен.');
            default: throw new Error(`Ошибка запроса: HTTP ${response.status}.`);
        }
    }
    return response.status === 204 ? null : response.json();
}

export function formatLocation(location) {
    return location ? `#${location.id}` : '—';
}

const dateTimeFormatter = new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
});

export function formatDateTime(value) {
    if (value == null || value === '') return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date);
}

export function fillLocationSelects() {
    const opts = '<option value="">—</option>' + state.locations.map(l => `<option value="${l.id}">#${l.id}</option>`).join('');
    document.querySelectorAll('select[name="fromLocationId"], select[name="toLocationId"]').forEach(select => {
        const selected = select.value;
        select.innerHTML = opts;
        select.value = selected;
    });
}

export const deleteRoute = async id => {
    try {
        id = resourceId(id, 'ID маршрута');
        await request(`/routes/${id}`, { method: 'DELETE' });
        await refreshAll();
    } catch (error) {
        document.querySelector('#routesStatus').textContent = error.message;
    }
};

export const deleteLocation = async id => {
    id = resourceId(id, 'ID локации');
    deleteKind = 'locations';
    replacementLocations = [];
    clearValidation(deleteForm);
    deleteForm.elements.id.value = id;
    const select = deleteForm.elements.replacementLocationId;
    const submit = deleteForm.querySelector('button.danger');
    
    select.innerHTML = '';
    select.required = false;
    submit.disabled = true;
    document.querySelector('#replacementLabel').style.display = 'none';
    document.querySelector('#deleteTitle').textContent = `Удаление локации #${id}`;
    document.querySelector('#deleteDescription').textContent = 'Загрузка…';
    document.querySelector('#deleteError').textContent = '';
    deleteDialog.showModal();
    
    try {
        const [locations, usage] = await Promise.all([
            request('/locations'), 
            request(`/locations/${id}/usage`)
        ]);
        
        if (!deleteDialog.open) return;
        
        const others = locations.filter(l => l.id !== id);
        replacementLocations = others;
        select.add(new Option(usage.routeCount ? 'Выберите' : 'Без замены', ''));
        others.forEach(l => select.add(new Option(`#${l.id}`, l.id)));
        select.required = usage.routeCount > 0;
        document.querySelector('#replacementLabel').style.display = 'flex';
        document.querySelector('#deleteDescription').textContent = 
            usage.routeCount ? `Связано маршрутов: ${usage.routeCount}` : 'Нет связанных маршрутов';
        
        if (usage.routeCount && !others.length) {
            document.querySelector('#deleteError').textContent = 'Нет локаций для замены';
        } else {
            submit.disabled = false;
        }
    } catch (error) {
        document.querySelector('#deleteError').textContent = error.message;
    }
};

export function renderRouteDetails(container, route) {
    const fields = [
        ['ID', route.id],
        ['Название', route.name],
        ['Координаты', `X: ${route.coordinates.x}, Y: ${route.coordinates.y}`],
        ['Дата создания', formatDateTime(route.creationDate)],
        ['Откуда', route.from ? `#${route.from.id} (X: ${route.from.x}, Y: ${route.from.y}, Z: ${route.from.z})` : '—'],
        ['Куда', `#${route.to.id} (X: ${route.to.x}, Y: ${route.to.y}, Z: ${route.to.z})`],
        ['Дистанция', route.distance],
        ['Рейтинг', route.rating]
    ];
    
    const list = document.createElement('dl');
    fields.forEach(([label, value]) => {
        const dt = document.createElement('dt');
        const dd = document.createElement('dd');
        dt.textContent = label;
        dd.textContent = value; 
        list.append(dt, dd);
    });
    
    container.replaceChildren(list);
}
