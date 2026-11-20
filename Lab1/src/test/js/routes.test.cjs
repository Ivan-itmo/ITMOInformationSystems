const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup() {
    const elements = new Map();
    function element(selector) {
        if (!elements.has(selector)) elements.set(selector, {
            value: selector === '#pageSize' ? '10' : '',
            textContent: '', hidden: false, disabled: false, open: false,
            listeners: {},
            addEventListener(event, handler) { this.listeners[event] = handler; },
            querySelectorAll() { return []; },
            replaceChildren(...rows) { this.rows = rows; },
            showModal() { this.open = true; },
            close() { this.open = false; return this.listeners.close?.(); }
        });
        return elements.get(selector);
    }
    element('#routeRowTemplate').content = { cloneNode: () => ({ querySelectorAll: () => [] }) };
    let current = { id: 1, name: 'Исходный', coordinates: { x: 1, y: 2 }, to: { id: 2 }, distance: 3, rating: 4 };
    const context = vm.createContext({
        document: { querySelector: element, querySelectorAll: () => [] },
        request: async url => {
            if (url.startsWith('/routes?')) return { items: current ? [current] : [], page: 0, total: current ? 1 : 0, size: 10 };
            if (!current) throw Object.assign(new Error('Not found'), { status: 404 });
            return structuredClone(current);
        },
        renderRouteDetails: (container, route) => { container.route = route; },
        positiveInteger: input => Number(input.value), resourceId: Number,
        formatLocation: () => '', formatDateTime: () => '',
    });
    const source = readFileSync(path.join(__dirname, '../../main/webapp/js/routes.js'), 'utf8')
        .replace(/^import .*;\n/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context);
    context.initRoutes();
    return { context, element, change: value => { current = value; }, current: () => structuredClone(current) };
}

test('Инфо сохраняет снимок до нажатия Обновить и замечает следующие изменения', async () => {
    const ui = setup();
    await vm.runInContext('showRoute(1)', ui.context);
    ui.change({ ...ui.current(), name: 'Изменённый' });
    await ui.context.refreshOpenRoute();
    assert.equal(ui.element('#routeDetails').route.name, 'Исходный');
    assert.match(ui.element('#detailsNotice').textContent, /изменён/);
    assert.equal(ui.element('#reloadDetailsButton').hidden, false);
    assert.equal(ui.element('#editFromDetails').disabled, true);
    await ui.element('#reloadDetailsButton').listeners.click();
    assert.equal(ui.element('#routeDetails').route.name, 'Изменённый');
    assert.equal(ui.element('#detailsNotice').textContent, '');
    assert.equal(ui.element('#editFromDetails').disabled, false);
    assert.equal(ui.element('#reloadDetailsButton').hidden, true);
    ui.change({ ...ui.current(), rating: 5 });
    await ui.context.refreshOpenRoute();
    assert.equal(ui.element('#routeDetails').route.rating, 4);
    assert.equal(ui.element('#reloadDetailsButton').hidden, false);
});

test('Удаление сохраняет открытый снимок и строку до закрытия окна', async () => {
    const ui = setup();
    await ui.context.loadRoutes();
    await vm.runInContext('showRoute(1)', ui.context);
    ui.change(null);
    await ui.context.refreshOpenRoute();
    await ui.context.loadRoutes();
    assert.match(ui.element('#detailsNotice').textContent, /удалён/);
    assert.equal(ui.element('#routeDetails').route.name, 'Исходный');
    assert.equal(ui.element('#editFromDetails').disabled, true);
    assert.equal(ui.element('#reloadDetailsButton').hidden, true);
    assert.equal(ui.element('#routesBody').rows.length, 1);
    await ui.element('#detailsDialog').close();
    await ui.context.loadRoutes();
    assert.equal(ui.element('#routesBody').rows.length, 0);
});

test('Удаление между уведомлением об изменении и нажатием Обновить', async () => {
    const ui = setup();
    await vm.runInContext('showRoute(1)', ui.context);
    ui.change({ ...ui.current(), name: 'Изменённый' });
    await ui.context.refreshOpenRoute();
    ui.change(null);
    await ui.element('#reloadDetailsButton').listeners.click();
    assert.match(ui.element('#detailsNotice').textContent, /удалён/);
    assert.equal(ui.element('#routeDetails').route.name, 'Исходный');
    assert.equal(ui.element('#reloadDetailsButton').hidden, true);
});
