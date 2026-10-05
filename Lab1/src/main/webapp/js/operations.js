import { request, renderRouteDetails } from './common.js';
import { routePayload, positiveInteger, locationId, clearValidation } from './validation.js';
import { loadRoutes } from './routes.js';
export function initOperations() {
    document.querySelector('#avgRating').addEventListener('click', () => showOperation('/operations/avg-rating', 'Средний рейтинг'));
    document.querySelector('#countBelow').addEventListener('click', () => {
        try {
            const rating = positiveInteger(document.querySelector('#belowRating'), 'Рейтинг');
            showOperation(`/operations/count-below-rating?rating=${rating}`, 'Количество маршрутов с рейтингом ниже указанного');
        } catch (error) { showResult({ error: error.message }); }
    });
    document.querySelector('#uniqueRatings').addEventListener('click', () => showOperation('/operations/unique-ratings', 'Уникальные рейтинги'));
    document.querySelector('#shortestRoute').addEventListener('click', async () => {
        const fromInput = document.querySelector('#shortestFrom');
        const toInput = document.querySelector('#shortestTo');
        try {
            const ids = [locationId(fromInput, 'From ID'), locationId(toInput, 'To ID')];
            showResult(await request(`/operations/shortest-route?fromLocationId=${ids[0]}&toLocationId=${ids[1]}`), 'Самый короткий маршрут');
        } catch (error) {
            showResult({ error: error.message });
        }
    });

    document.querySelector('#functionRouteForm').addEventListener('submit', async event => {
        event.preventDefault();
        const form = event.currentTarget;
        try {
            const route = await request('/operations/add-between-locations', { method: 'POST', body: JSON.stringify(routePayload(form)) });
            form.reset();
            clearValidation(form);
            showResult(route, 'Маршрут добавлен');
            await loadRoutes();
        } catch (error) { showResult({ error: error.message }); }
    });
}
async function showOperation(path, label) {
    try {
        showResult(await request(path), label);
    } catch (error) {
        showResult({ error: error.message });
    }
}

function showResult(value, label = 'Результат') {
    const container = document.querySelector('#operationResult');
    if (value?.error) {
        container.textContent = `Ошибка: ${value.error}`;
    } else if (value?.id != null) {
        renderRouteDetails(container, value);
        const title = document.createElement('p');
        title.textContent = label;
        container.prepend(title);
    } else {
        const result = value?.value;
        const text = Array.isArray(result) ? (result.join(', ') || 'Нет данных') : (result ?? 'Нет данных');
        container.textContent = `${label}: ${text}`;
    }
}
