import { request, formatLocation, formatDateTime, renderRouteDetails, deleteRoute } from './common.js';
import { routePayload, validateRoute, positiveInteger, clearValidation} from './validation.js';
let page = 0;
let sortBy = 'id';
let direction = 'asc';
let routes = [];
let detailsRouteId = null;
let routesBody, routeRowTemplate, routeDialog, routeForm, detailsDialog;
export async function loadRoutes() {
    const size = Number(document.querySelector('#pageSize').value); 
    const filter = encodeURIComponent(document.querySelector('#filterInput').value);
    const data = await request(`/routes?page=${page}&size=${size}&filter=${filter}&sortBy=${sortBy}&direction=${direction}`);
    routes = data.items;
    
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

    const totalPages = Math.ceil(data.total / size) || 1;
    const currentPage = data.page; 

    document.querySelector('#pageInfo').textContent = `Страница ${currentPage + 1} из ${totalPages}`;
    document.querySelector('#routesStatus').textContent = `Всего: ${data.total}`;

    const prevBtn = document.querySelector('#prevPage');
    const nextBtn = document.querySelector('#nextPage');

    prevBtn.disabled = (currentPage === 0);
    nextBtn.disabled = (currentPage + 1 >= totalPages);
}

const editRoute = async id => {
    const route = routes.find(item => item.id === id) ?? await request(`/routes/${id}`);
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
    routeDialog.showModal();
};

const showRoute = async id => {
    const route = await request(`/routes/${id}`);
    detailsRouteId = route.id;
    renderRouteDetails(document.querySelector('#routeDetails'), route);
    detailsDialog.showModal();
};

export function initRoutes() {
    routesBody = document.querySelector('#routesBody');
    routeRowTemplate = document.querySelector('#routeRowTemplate');
    routeDialog = document.querySelector('#routeDialog');
    routeForm = document.querySelector('#routeForm');
    detailsDialog = document.querySelector('#detailsDialog');
    routesBody.addEventListener('click', async event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const id = Number(button.dataset.id);
        try {
            if (button.dataset.action === 'edit') await editRoute(id);
            if (button.dataset.action === 'details') await showRoute(id);
            if (button.dataset.action === 'delete') deleteRoute(id);
        } catch (error) { showError(error); }
    });
    document.querySelector('#newRouteButton').addEventListener('click', () => {
        routeForm.reset();
        clearValidation(routeForm);
        routeForm.elements.id.value = '';
        document.querySelector('#dialogTitle').textContent = 'Новый маршрут';
        document.querySelector('#routeError').textContent = '';
        routeDialog.showModal();
    });

    routeForm.addEventListener('submit', async event => {
        event.preventDefault();
        const id = routeForm.elements.id.value;
        try {
            validateRoute(routeForm);
            await request(id ? `/routes/${id}` : '/routes', { method: id ? 'PUT' : 'POST', body: JSON.stringify(routePayload(routeForm)) });
            routeDialog.close();
            await loadRoutes().catch(showError);
        } catch (error) { document.querySelector('#routeError').textContent = error.message; }
    });

    document.querySelector('#findRouteById').addEventListener('click', async () => {
        try {
            const id = positiveInteger(document.querySelector('#routeIdInput'), 'ID маршрута');
            await showRoute(id);
        } catch (error) { showError(error); }
    });

    document.querySelector('#editFromDetails').addEventListener('click', async () => { detailsDialog.close(); await editRoute(detailsRouteId).catch(showError); });

    document.querySelectorAll('th[data-sort]').forEach(th => th.addEventListener('click', () => {
        direction = sortBy === th.dataset.sort && direction === 'asc' ? 'desc' : 'asc';
        sortBy = th.dataset.sort;
        loadRoutes().catch(showError);
    }));

    document.querySelector('#filterInput').addEventListener('input', () => { page = 0; loadRoutes().catch(showError); });
    document.querySelector('#pageSize').addEventListener('change', () => { page = 0; loadRoutes().catch(showError); });
    document.querySelector('#prevPage').addEventListener('click', () => { page = Math.max(0, page - 1); loadRoutes().catch(showError); });
    document.querySelector('#nextPage').addEventListener('click', () => { page += 1; loadRoutes().catch(showError); });
}
function showError(error) {
    document.querySelector('#routesStatus').textContent = error.message;
}
