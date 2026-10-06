// Запуск: node --experimental-default-type=module --test src/test/js/validation.test.js
import test from 'node:test';
import assert from 'node:assert/strict';
import { state } from '../../main/webapp/js/common.js';
import {
    clearValidation, finiteNumber, positiveInteger, resourceId,
    locationId, routePayload, validateNumber, formField
} from '../../main/webapp/js/validation.js';

function input(value) {
    return {
        value, validity: { badInput: false }, message: '', invalid: false,
        setCustomValidity(message) { this.message = message; },
        setAttribute() { this.invalid = true; },
        removeAttribute() { this.invalid = false; },
        focus() {}, reportValidity() {}
    };
}

function form(values) {
    const fields = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, input(value)]));
    return {
        elements: { namedItem: name => fields[name] ?? null },
        querySelectorAll: () => Object.values(fields)
    };
}

function rejects(action, pattern) {
    assert.throws(action, error => error instanceof Error && !(error instanceof TypeError)
        && pattern.test(error.message));
}

test('отсутствующие поля, неверные типы и неполные объекты дают понятную ошибку', () => {
    for (const value of [undefined, null, false, 5, '12', {}, { value: '12' }, input(12)]) {
        rejects(() => finiteNumber(value, 'X'), /поле ввода/);
    }
});

test('числа проверяются без неявного преобразования мусора', () => {
    for (const value of ['', '   ', 'abc', 'NaN', 'Infinity', '0x10', '1,5', '12px', '1e309']) {
        const field = input(value);
        assert.throws(() => finiteNumber(field, 'X'));
        assert.equal(field.invalid, true);
        assert.notEqual(field.message, '');
    }
    for (const value of ['-12.5', '+2', '.5', '1e3', ' 0 ']) {
        assert.equal(finiteNumber(input(value), 'X'), Number(value));
    }
    const field = input('123');
    field.validity.badInput = true;
    rejects(() => finiteNumber(field, 'X'), /формате/);
});

test('целые положительные числа и ID не теряют точность', () => {
    for (const value of ['0', '-1', '1.5', '1e2', '9007199254740992']) {
        assert.throws(() => positiveInteger(input(value), 'ID'));
    }
    for (const value of [null, undefined, true, {}, [], NaN, Infinity, 0, -1, 1.5, '']) {
        assert.throws(() => resourceId(value));
    }
    assert.equal(resourceId('9007199254740991'), Number.MAX_SAFE_INTEGER);
    assert.equal(positiveInteger(input('12'), 'ID'), 12);
    rejects(() => validateNumber(input('0'), 'Дистанция', { positive: true }), /больше 0/);
});

test('параметры проверок требуют объект и логические значения', () => {
    for (const options of [null, [], 'yes', true, 1]) {
        rejects(() => validateNumber(input('1'), 'X', options), /объект/);
        rejects(() => locationId(input('1'), 'Локация', options), /объект/);
    }
    rejects(() => validateNumber(input('1'), 'X', { integer: 'false' }), /логическое/);
    rejects(() => validateNumber(input('1'), 'X', { positive: 1 }), /логическое/);
    rejects(() => locationId(input('1'), 'Локация', { required: 'false' }), /логическое/);
});

test('список локаций проверяется до поиска', () => {
    for (const locations of [null, {}, '1', [null], [{}], [{ id: '1' }], [{ id: 0 }], [{ id: Infinity }]]) {
        rejects(() => locationId(input('1'), 'Локация', { locations }), /список/);
    }
    rejects(() => locationId(input('2'), 'Локация', { locations: [{ id: 1 }] }), /не найдена/);
    rejects(() => locationId(input('1'), 'Локация', {
        locations: [{ id: 1 }], excludeId: '1'
    }), /другую/);
    assert.equal(locationId(input('1'), 'Локация', { locations: [{ id: 1 }] }), 1);
});

test('необязательная локация допускает пустое значение, но требует существующее поле', () => {
    assert.equal(locationId(input('  '), 'From', { required: false, locations: [] }), null);
    rejects(() => locationId(null, 'From', { required: false, locations: [] }), /поле ввода/);
    rejects(() => locationId(input(''), 'To', { locations: [] }), /обязательное/);
});

test('отсутствующая форма или поле, в том числе дубли, не вызывают TypeError', () => {
    for (const value of [undefined, null, {}, { elements: {} }]) {
        rejects(() => routePayload(value), /форма/);
        rejects(() => clearValidation(value), /форма/);
    }
    rejects(() => routePayload(form({})), /name/);
    rejects(() => formField(form({}), ''), /имя поля/);
    const duplicate = form({});
    duplicate.elements.namedItem = () => ({ value: '1', length: 2 });
    rejects(() => formField(duplicate, 'rating'), /поле ввода/);
});

test('маршрут собирается только после проверки всех обязательных полей', () => {
    state.locations = [{ id: 1 }];
    const values = {
        name: ' Маршрут ', coordX: '-1.5', coordY: '0', fromLocationId: '',
        toLocationId: '1', distance: '2.5', rating: '3'
    };
    assert.deepEqual(routePayload(form(values)), {
        name: 'Маршрут', coordinates: { x: -1.5, y: 0 },
        fromLocationId: null, toLocationId: 1, distance: 2.5, rating: 3
    });
    for (const key of Object.keys(values)) {
        const incomplete = { ...values };
        delete incomplete[key];
        rejects(() => routePayload(form(incomplete)), new RegExp(key));
    }
    for (const name of [' ', 'a'.repeat(256)]) assert.throws(() => routePayload(form({ ...values, name })));
    for (const distance of ['0', '-1', 'Infinity']) {
        assert.throws(() => routePayload(form({ ...values, distance })));
    }
});

test('исправление поля убирает предыдущую ошибку валидации', () => {
    const field = input('abc');
    assert.throws(() => finiteNumber(field, 'X'));
    field.value = '2';
    assert.equal(finiteNumber(field, 'X'), 2);
    assert.equal(field.invalid, false);
    assert.equal(field.message, '');
});
