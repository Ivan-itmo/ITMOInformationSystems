import { request, formatLocation, formatDateTime, renderRouteDetails, deleteRoute } from './common.js';
import { routePayload, positiveInteger, resourceId, clearValidation, formField } from './validation.js';
let page = 0;
let sortBy = 'id';
let direction = 'asc';
let routes = [];
let detailsState = null;
let detailsDeleted = false;
let detailsSequence = 0;
let editingState = null;
let loadSequence = 0;
let editSequence = 0;
let routesBody, routeRowTemplate, routeDialog, routeForm, detailsDialog;
export async function loadRoutes() {
    const sequence = ++loadSequence;
    const size = positiveInteger(document.querySelector('#pageSize'), 'Размер страницы');
    if (![5, 10, 20].includes(size)) throw new Error('Выберите размер страницы: 5, 10 или 20.');
    const filter = encodeURIComponent(document.querySelector('#filterInput').value);
    const data = await request(`/routes?page=${page}&size=${size}&filter=${filter}&sortBy=${sortBy}&direction=${direction}`);
    if (sequence !== loadSequence) return;
    const retainedIndex = detailsDialog.open && detailsDeleted
        ? routes.findIndex(route => route.id === detailsState.id) : -1;
    const retainedRoute = routes[retainedIndex];
    routes = [...data.items];
    if (retainedRoute && !routes.some(route => route.id === retainedRoute.id)) {
        routes.splice(Math.min(retainedIndex, routes.length), 0, retainedRoute);
    }
    page = data.page;
    const totalPages = Math.ceil(data.total / data.size) || 1;
    
    const rows = routes.map(route => {
        const row = routeRowTemplate.content.cloneNode(true);
        const fields = {
            id: route.id,
            name: route.name,
            coordinates: `${route.coordinates.x}, ${route.coordinates.y}`,
            creationDate: formatDateTime(route.creationDate),
            from: formatLocation(route.from),
            to: formatLocation(route.to),
            distance: route.distance,
            rating: route.rating
        };
        row.querySelectorAll('[data-field]').forEach(cell => {
            cell.textContent = fields[cell.dataset.field];
        });
        row.querySelectorAll('button[data-action]').forEach(button => {
            button.dataset.id = route.id;
        });
        return row;
    });
    routesBody.replaceChildren(...rows);

    document.querySelector('#pageInfo').textContent = `Страница ${page + 1} из ${totalPages}`;
    document.querySelector('#routesStatus').textContent = `Всего: ${data.total}`;
    document.querySelector('#routesError').textContent = '';

    const prevBtn = document.querySelector('#prevPage');
    const nextBtn = document.querySelector('#nextPage');

    prevBtn.disabled = (page === 0);
    nextBtn.disabled = (page + 1 >= totalPages);
}

const editRoute = async id => {
    id = resourceId(id, 'ID маршрута');
    const sequence = ++editSequence;
    const route = await request(`/routes/${id}`);
    if (sequence !== editSequence) return;
    editingState = route;
    clearValidation(routeForm);
    routeForm.elements.id.value = route.id;
    routeForm.elements.name.value = route.name;
    routeForm.elements.coordX.value = route.coordinates.x;
    routeForm.elements.coordY.value = route.coordinates.y;
    routeForm.elements.fromLocationId.value = route.from?.id ?? '';
    routeForm.elements.toLocationId.value = route.to?.id ?? '';
    routeForm.elements.distance.value = route.distance;
    routeForm.elements.rating.value = route.rating;
    document.querySelector('#dialogTitle').textContent = `Маршрут #${route.id}`;
    document.querySelector('#routeError').textContent = '';
    document.querySelector('#saveRouteButton').disabled = false;
    document.querySelector('#reloadRouteButton').hidden = true;
    if (!routeDialog.open) routeDialog.showModal();
};

function showEditConflict(deleted) {
    document.querySelector('#routeError').textContent = deleted
        ? 'Объект удалён другим пользователем. Сохранение невозможно.'
        : 'Объект изменён другим пользователем. Загрузите актуальные данные перед сохранением.';
    document.querySelector('#saveRouteButton').disabled = true;
    document.querySelector('#reloadRouteButton').hidden = deleted;
}

function showDetailsStatus(message, { changed = false, deleted = false } = {}) {
    detailsDeleted = deleted;
    if (deleted) ++detailsSequence;
    document.querySelector('#detailsNotice').textContent = message;
    document.querySelector('#reloadDetailsButton').hidden = !changed;
    document.querySelector('#editFromDetails').disabled = changed || deleted;
}

function sameState(left, right) {
    if (left === right) return true;
    if (!left || !right || typeof left !== 'object' || typeof right !== 'object') return false;
    const keys = Object.keys(left);
    return keys.length === Object.keys(right).length && keys.every(key => Object.hasOwn(right, key) && sameState(left[key], right[key]));
}

export async function refreshOpenRoute() {
    const snapshot = editingState;
    if (routeDialog.open && snapshot) {
        try {
            const current = await request(`/routes/${snapshot.id}`);
            if (routeDialog.open && editingState === snapshot && !sameState(current, snapshot)) {
                showEditConflict(false);
            }
        } catch (error) {
            if (routeDialog.open && editingState === snapshot) {
                if (error.status === 404) showEditConflict(true);
                else throw error;
            }
        }
    }
    const detailsSnapshot = detailsState;
    if (detailsDialog.open && detailsSnapshot && !detailsDeleted) {
        const isCurrent = () => detailsDialog.open && detailsState === detailsSnapshot && !detailsDeleted;
        try {
            const current = await request(`/routes/${detailsSnapshot.id}`);
            if (isCurrent()) {
                const changed = !sameState(current, detailsSnapshot);
                showDetailsStatus(changed ? 'Объект изменён другим пользователем. Можно обновить данные.' : '', { changed });
            }
        } catch (error) {
            if (!isCurrent()) return;
            if (error.status === 404) showDetailsStatus('Объект удалён другим пользователем.', { deleted: true });
            else throw error;
        }
    }
}

const showRoute = async id => {
    id = resourceId(id, 'ID маршрута');
    const sequence = ++detailsSequence;
    const route = await request(`/routes/${id}`);
    if (sequence !== detailsSequence) return;
    detailsState = route;
    document.querySelector('#routesError').textContent = '';
    showDetailsStatus('');
    renderRouteDetails(document.querySelector('#routeDetails'), route);
    if (!detailsDialog.open) detailsDialog.showModal();
};

export function initRoutes() {
    routesBody = document.querySelector('#routesBody');
    routeRowTemplate = document.querySelector('#routeRowTemplate');
    routeDialog = document.querySelector('#routeDialog');
    routeForm = document.querySelector('#routeForm');
    detailsDialog = document.querySelector('#detailsDialog');
    routeDialog.addEventListener('close', () => { editingState = null; ++editSequence; });
    detailsDialog.addEventListener('close', () => {
        const needsRefresh = detailsDeleted;
        detailsState = null;
        detailsDeleted = false;
        ++detailsSequence;
        if (needsRefresh) loadRoutes().catch(showError);
    });
    document.querySelector('#reloadDetailsButton').addEventListener('click', async () => {
        const snapshot = detailsState;
        try {
            await showRoute(snapshot.id);
        } catch (error) {
            if (!detailsDialog.open || detailsState !== snapshot || detailsDeleted) return;
            if (error.status === 404) {
                showDetailsStatus('Объект удалён другим пользователем.', { deleted: true });
            } else {
                document.querySelector('#detailsNotice').textContent = error.message;
            }
        }
    });
    document.querySelector('#reloadRouteButton').addEventListener('click', async () => {
        try {
            await editRoute(editingState.id);
        } catch (error) {
            if (error.status === 404) showEditConflict(true);
            else document.querySelector('#routeError').textContent = error.message;
        }
    });
    routesBody.addEventListener('click', async event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        try {
            const id = resourceId(button.dataset.id, 'ID маршрута');
            if (button.dataset.action === 'edit') await editRoute(id);
            if (button.dataset.action === 'details') await showRoute(id);
            if (button.dataset.action === 'delete') deleteRoute(id);
        } catch (error) { showError(error); }
    });
    document.querySelector('#newRouteButton').addEventListener('click', () => {
        editingState = null;
        ++editSequence;
        routeForm.reset();
        clearValidation(routeForm);
        routeForm.elements.id.value = '';
        document.querySelector('#dialogTitle').textContent = 'Новый маршрут';
        document.querySelector('#routeError').textContent = '';
        document.querySelector('#saveRouteButton').disabled = false;
        document.querySelector('#reloadRouteButton').hidden = true;
        routeDialog.showModal();
    });

    routeForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (document.querySelector('#saveRouteButton').disabled) return;
        let id = null;
        try {
            const idInput = formField(routeForm, 'id');
            id = idInput.value === '' ? null : positiveInteger(idInput, 'ID маршрута');
            const payload = routePayload(routeForm);
            if (id) payload.expectedState = editingState;
            document.querySelector('#saveRouteButton').disabled = true;
            await request(id ? `/routes/${id}` : '/routes', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
            routeDialog.close();
            await loadRoutes().catch(showError);
        } catch (error) {
            if (id && (error.status === 404 || error.status === 409)) showEditConflict(error.status === 404);
            else {
                document.querySelector('#saveRouteButton').disabled = false;
                document.querySelector('#routeError').textContent = error.message;
            }
        }
    });

    document.querySelector('#findRouteById').addEventListener('click', async () => {
        try {
            const id = positiveInteger(document.querySelector('#routeIdInput'), 'ID маршрута');
            await showRoute(id);
        } catch (error) { showError(error); }
    });

    document.querySelector('#editFromDetails').addEventListener('click', async () => {
        if (document.querySelector('#editFromDetails').disabled) return;
        const id = detailsState.id;
        detailsDialog.close();
        await editRoute(id).catch(showError);
    });

    document.querySelectorAll('th[data-sort]').forEach(th => th.addEventListener('click', () => {
        direction = sortBy === th.dataset.sort && direction === 'asc' ? 'desc' : 'asc';
        sortBy = th.dataset.sort;
        page = 0;
        loadRoutes().catch(showError);
    }));

    document.querySelector('#filterInput').addEventListener('input', () => { page = 0; loadRoutes().catch(showError); });
    document.querySelector('#pageSize').addEventListener('change', () => { page = 0; loadRoutes().catch(showError); });
    document.querySelector('#prevPage').addEventListener('click', () => { page = Math.max(0, page - 1); loadRoutes().catch(showError); });
    document.querySelector('#nextPage').addEventListener('click', () => { page += 1; loadRoutes().catch(showError); });
}
function showError(error) {
    document.querySelector('#routesError').textContent = error.message;
}
